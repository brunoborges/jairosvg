package com.jairosvg.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL handling utilities. Port of CairoSVG url.py
 */
public final class UrlHelper {

    public static final String VERSION = "1.0.0";
    private static final Pattern URL_PATTERN = Pattern.compile("url\\((.+)\\)");

    private UrlHelper() {
    }

    /** A parsed URL wrapper. */
    public record ParsedUrl(String scheme, String authority, String path, String query, String fragment) {
        public String getUrl() {
            var sb = new StringBuilder();
            if (scheme != null && !scheme.isEmpty()) {
                sb.append(scheme).append(":");
            }
            if (authority != null && !authority.isEmpty()) {
                sb.append("//").append(authority);
            }
            if (path != null) {
                sb.append(path);
            }
            if (query != null && !query.isEmpty()) {
                sb.append("?").append(query);
            }
            if (fragment != null && !fragment.isEmpty()) {
                sb.append("#").append(fragment);
            }
            return sb.toString();
        }

        public boolean hasNonFragmentParts() {
            return (scheme != null && !scheme.isEmpty()) || (authority != null && !authority.isEmpty())
                    || (path != null && !path.isEmpty()) || (query != null && !query.isEmpty());
        }

        public ParsedUrl withoutFragment() {
            return new ParsedUrl(scheme, authority, path, query, null);
        }
    }

    /** Functional interface for URL fetching. */
    @FunctionalInterface
    public interface UrlFetcher {
        byte[] fetch(String url, String resourceType) throws IOException;
    }

    /** Default URL fetcher that fetches from file system or network. */
    public static byte[] fetch(String url, String resourceType) throws IOException {
        if (url == null || url.isEmpty()) {
            return new byte[0];
        }

        if (url.startsWith("data:")) {
            return decodeDataUrl(url);
        }

        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
                Path filePath;
                if (uri.getScheme() == null) {
                    // No scheme: treat as plain file path
                    filePath = Path.of(url);
                } else {
                    // file: URI — strip query/fragment to avoid IllegalArgumentException
                    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                        filePath = Path.of(uri.getPath());
                    } else {
                        filePath = Path.of(uri);
                    }
                }
                return Files.readAllBytes(filePath);
            } else {
                return fetchHttp(url);
            }
        } catch (URISyntaxException e) {
            // Try as file path
            return Files.readAllBytes(Path.of(url));
        }
    }

    /** Safe fetcher that only allows data: URLs. Returns empty SVG otherwise. */
    public static byte[] safeFetch(String url, String resourceType) throws IOException {
        if (url != null && url.startsWith("data:")) {
            return fetch(url, resourceType);
        }
        return "<svg width=\"1\" height=\"1\"></svg>".getBytes();
    }

    /** Parse a URL, potentially extracting it from url() notation. */
    public static ParsedUrl parseUrl(String url, String base) {
        if (url == null || url.isEmpty()) {
            return new ParsedUrl(null, null, null, null, null);
        }

        Matcher m = URL_PATTERN.matcher(url);
        if (m.find()) {
            url = m.group(1).strip();
            // Remove quotes
            if ((url.startsWith("'") && url.endsWith("'")) || (url.startsWith("\"") && url.endsWith("\""))) {
                url = url.substring(1, url.length() - 1);
            }
        }

        if (base != null && !base.isEmpty()) {
            url = resolveUrl(url, base);
        }

        return parseUrlComponents(url.strip());
    }

    /** Parse URL without base. */
    public static ParsedUrl parseUrl(String url) {
        return parseUrl(url, null);
    }

    /** Read bytes from a parsed URL using the given fetcher. */
    public static byte[] readUrl(ParsedUrl url, UrlFetcher fetcher, String resourceType) throws IOException {
        String fullUrl;
        if (url.scheme() != null && !url.scheme().isEmpty()) {
            fullUrl = url.getUrl();
        } else {
            String path = url.getUrl();
            if (path.isEmpty())
                return new byte[0];
            fullUrl = "file://" + Path.of(path).toAbsolutePath();
        }
        return fetcher.fetch(fullUrl, resourceType);
    }

    private static ParsedUrl parseUrlComponents(String url) {
        if (url == null || url.isEmpty()) {
            return new ParsedUrl(null, null, null, null, null);
        }

        String fragment = null;
        int hashIdx = url.indexOf('#');
        if (hashIdx >= 0) {
            fragment = url.substring(hashIdx + 1);
            url = url.substring(0, hashIdx);
        }

        String scheme = null;
        String authority = null;
        String path = url;
        String query = null;

        // Extract scheme
        int colonIdx = url.indexOf(':');
        if (colonIdx > 0 && url.substring(0, colonIdx).matches("[a-zA-Z][a-zA-Z0-9+.-]*")) {
            scheme = url.substring(0, colonIdx);
            path = url.substring(colonIdx + 1);
        }

        // Extract authority
        if (path.startsWith("//")) {
            path = path.substring(2);
            int slashIdx = path.indexOf('/');
            if (slashIdx >= 0) {
                authority = path.substring(0, slashIdx);
                path = path.substring(slashIdx);
            } else {
                authority = path;
                path = "";
            }
        }

        // Extract query
        int qIdx = path.indexOf('?');
        if (qIdx >= 0) {
            query = path.substring(qIdx + 1);
            path = path.substring(0, qIdx);
        }

        return new ParsedUrl(scheme, authority, path.isEmpty() ? null : path, query, fragment);
    }

    private static String resolveUrl(String url, String base) {
        if (url.startsWith("data:") || url.startsWith("http:") || url.startsWith("https:")) {
            return url;
        }

        try {
            URI baseUri = new URI(base);
            URI resolved = baseUri.resolve(url);
            return resolved.toString();
        } catch (URISyntaxException e) {
            // Fall back to path resolution
            if (url.startsWith("#")) {
                return base + url;
            }
            Path basePath = Path.of(base);
            if (Files.isRegularFile(basePath)) {
                basePath = basePath.getParent();
            }
            if (basePath != null && !url.isEmpty()) {
                return basePath.resolve(url).toString();
            }
            return url;
        }
    }

    private static byte[] decodeDataUrl(String dataUrl) {
        // data:[<mediatype>][;base64],<data>
        String data = dataUrl.substring(5); // remove "data:"
        int commaIdx = data.indexOf(',');
        if (commaIdx < 0)
            return new byte[0];

        String meta = data.substring(0, commaIdx);
        String encoded = data.substring(commaIdx + 1);

        if (meta.endsWith(";base64")) {
            return Base64.getDecoder().decode(encoded);
        } else {
            try {
                return java.net.URLDecoder.decode(encoded, "UTF-8").getBytes();
            } catch (Exception e) {
                return encoded.getBytes();
            }
        }
    }

    private static byte[] fetchHttp(String url) throws IOException {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("User-Agent", "JairoSVG " + VERSION)
                    .GET().build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.body();
        } catch (URISyntaxException | InterruptedException e) {
            throw new IOException("Failed to fetch URL: " + url, e);
        }
    }
}
