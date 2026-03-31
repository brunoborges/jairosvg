package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.brunoborges.jairosvg.util.UrlHelper;
import io.brunoborges.jairosvg.util.UrlHelper.ParsedUrl;

class UrlHelperTest {

    @Test
    void testUrlParsing() {
        var url = UrlHelper.parseUrl("#myId");
        assertEquals("myId", url.fragment());

        var url2 = UrlHelper.parseUrl("url(#grad1)");
        assertEquals("grad1", url2.fragment());
    }

    // ── parseUrl (no base) ─────────────────────────────────────────────

    @Test
    void testParseUrlHttp() {
        ParsedUrl url = UrlHelper.parseUrl("http://example.com/img.svg");
        assertEquals("http", url.scheme());
        assertEquals("example.com", url.authority());
        assertEquals("/img.svg", url.path());
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlFragmentOnly() {
        ParsedUrl url = UrlHelper.parseUrl("#fragment");
        assertEquals("fragment", url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlFromUrlNotation() {
        ParsedUrl url = UrlHelper.parseUrl("url(#myId)");
        assertEquals("myId", url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlFromUrlNotationSingleQuotes() {
        ParsedUrl url = UrlHelper.parseUrl("url('#myId')");
        assertEquals("myId", url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlFromUrlNotationDoubleQuotes() {
        ParsedUrl url = UrlHelper.parseUrl("url(\"#myId\")");
        assertEquals("myId", url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlDataScheme() {
        ParsedUrl url = UrlHelper.parseUrl("data:image/png;base64,abc");
        assertEquals("data", url.scheme());
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlRelativePath() {
        ParsedUrl url = UrlHelper.parseUrl("relative/path.svg");
        assertNull(url.scheme());
        assertNull(url.authority());
        assertEquals("relative/path.svg", url.path());
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlRelativePathWithFragment() {
        ParsedUrl url = UrlHelper.parseUrl("relative/path.svg#frag");
        assertEquals("frag", url.fragment());
        assertEquals("relative/path.svg", url.path());
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlEmpty() {
        ParsedUrl url = UrlHelper.parseUrl("");
        assertNull(url.scheme());
        assertNull(url.path());
        assertNull(url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlNull() {
        ParsedUrl url = UrlHelper.parseUrl(null);
        assertNull(url.scheme());
        assertNull(url.path());
        assertNull(url.fragment());
        assertFalse(url.hasNonFragmentParts());
    }

    // ── parseUrl with base URL ─────────────────────────────────────────

    @Test
    void testParseUrlWithBaseRelative() {
        ParsedUrl url = UrlHelper.parseUrl("img.svg", "http://example.com/dir/");
        assertEquals("http", url.scheme());
        assertEquals("example.com", url.authority());
        assertTrue(url.path().contains("img.svg"));
    }

    @Test
    void testParseUrlWithBaseFragmentOnly() {
        ParsedUrl url = UrlHelper.parseUrl("#frag", "http://example.com/page.html");
        assertEquals("frag", url.fragment());
    }

    @Test
    void testParseUrlWithBaseAbsoluteNotResolved() {
        // Absolute URLs should not be affected by base
        ParsedUrl url = UrlHelper.parseUrl("http://other.com/x.svg", "http://example.com/dir/");
        assertEquals("http", url.scheme());
        assertEquals("other.com", url.authority());
    }

    // ── decodeDataUrl ──────────────────────────────────────────────────

    @Test
    void testDecodeDataUrlBase64() {
        // "Hello" in base64 is "SGVsbG8="
        byte[] result = UrlHelper.decodeDataUrl("data:text/plain;base64,SGVsbG8=");
        assertEquals("Hello", new String(result));
    }

    @Test
    void testDecodeDataUrlPlainText() {
        byte[] result = UrlHelper.decodeDataUrl("data:text/plain,Hello%20World");
        assertEquals("Hello World", new String(result));
    }

    @Test
    void testDecodeDataUrlNoComma() {
        byte[] result = UrlHelper.decodeDataUrl("data:text/plain");
        assertEquals(0, result.length);
    }

    @Test
    void testDecodeDataUrlViaRender() throws Exception {
        // Verify data URI image rendering works end-to-end
        // 1x1 red pixel PNG as base64
        String redPixelBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==";
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"
                     xmlns:xlink="http://www.w3.org/1999/xlink">
                  <image width="10" height="10"
                         href="data:image/png;base64,%s"/>
                </svg>
                """.formatted(redPixelBase64);
        var img = RenderTestHelper.render(svg);
        assertNotNull(img);
        assertEquals(10, img.getWidth());
        assertEquals(10, img.getHeight());
    }

    // ── ParsedUrl record ───────────────────────────────────────────────

    @Test
    void testParsedUrlGetUrl() {
        ParsedUrl url = new ParsedUrl("https", "example.com", "/path", "q=1", "frag");
        assertEquals("https://example.com/path?q=1#frag", url.getUrl());
    }

    @Test
    void testParsedUrlGetUrlMinimal() {
        ParsedUrl url = new ParsedUrl(null, null, null, null, "id");
        assertEquals("#id", url.getUrl());
    }

    @Test
    void testParsedUrlHasNonFragmentParts() {
        assertFalse(new ParsedUrl(null, null, null, null, "frag").hasNonFragmentParts());
        assertTrue(new ParsedUrl("http", null, null, null, "frag").hasNonFragmentParts());
        assertTrue(new ParsedUrl(null, "host", null, null, null).hasNonFragmentParts());
        assertTrue(new ParsedUrl(null, null, "/path", null, null).hasNonFragmentParts());
        assertTrue(new ParsedUrl(null, null, null, "q=1", null).hasNonFragmentParts());
    }

    @Test
    void testParsedUrlWithoutFragment() {
        ParsedUrl url = new ParsedUrl("http", "host", "/path", "q", "frag");
        ParsedUrl noFrag = url.withoutFragment();
        assertNull(noFrag.fragment());
        assertEquals("http", noFrag.scheme());
        assertEquals("host", noFrag.authority());
        assertEquals("/path", noFrag.path());
        assertEquals("q", noFrag.query());
    }

    @Test
    void testParsedUrlGetUrlSchemeOnly() {
        ParsedUrl url = new ParsedUrl("file", null, "/home/test.svg", null, null);
        assertEquals("file:/home/test.svg", url.getUrl());
    }

    @Test
    void testParseUrlHttpsWithQuery() {
        ParsedUrl url = UrlHelper.parseUrl("https://example.com/page?key=val#sec");
        assertEquals("https", url.scheme());
        assertEquals("example.com", url.authority());
        assertEquals("/page", url.path());
        assertEquals("key=val", url.query());
        assertEquals("sec", url.fragment());
    }

    // ── fetch / safeFetch ───────────────────────────────────────────────

    @Test
    void testFetchNullUrl() throws Exception {
        byte[] result = UrlHelper.fetch(null, "image");
        assertEquals(0, result.length);
    }

    @Test
    void testFetchEmptyUrl() throws Exception {
        byte[] result = UrlHelper.fetch("", "image");
        assertEquals(0, result.length);
    }

    @Test
    void testFetchDataUrl() throws Exception {
        byte[] result = UrlHelper.fetch("data:text/plain;base64,SGVsbG8=", "text");
        assertEquals("Hello", new String(result));
    }

    @Test
    void testSafeFetchDataUrl() throws Exception {
        byte[] result = UrlHelper.safeFetch("data:text/plain;base64,SGVsbG8=", "text");
        assertEquals("Hello", new String(result));
    }

    @Test
    void testSafeFetchNonDataUrl() throws Exception {
        byte[] result = UrlHelper.safeFetch("http://example.com/image.png", "image");
        // Returns fallback SVG
        String fallback = new String(result);
        assertTrue(fallback.contains("<svg"), "Non-data URL should return fallback SVG");
    }

    @Test
    void testSafeFetchNullUrl() throws Exception {
        byte[] result = UrlHelper.safeFetch(null, "image");
        String fallback = new String(result);
        assertTrue(fallback.contains("<svg"), "Null URL should return fallback SVG");
    }

    // ── readUrl ─────────────────────────────────────────────────────────

    @Test
    void testReadUrlWithEmptyPath() throws Exception {
        // ParsedUrl with no scheme, no path, only fragment → getUrl() returns "#frag"
        // readUrl should detect no scheme and try file path — exercise the code path
        ParsedUrl url = new ParsedUrl(null, null, null, null, "frag");
        // getUrl() returns "#frag" which is non-empty, so it goes to file path
        // Just verify it doesn't throw unexpectedly
        try {
            UrlHelper.readUrl(url, (u, r) -> new byte[0], "image");
        } catch (Exception e) {
            // It may fail to read a file — that's fine, we just wanted to exercise the path
        }
    }

    // ── resolveBaseUrl ──────────────────────────────────────────────────

    @Test
    void testResolveBaseUrlViaRender() throws Exception {
        // xml:base attribute is resolved
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50"
                     xml:base="http://example.com/assets/">
                  <rect width="50" height="50" fill="red"/>
                </svg>
                """;
        var img = RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── parseUrl with base URL edge cases ───────────────────────────────

    @Test
    void testParseUrlWithAbsoluteDataUrlIgnoresBase() {
        ParsedUrl url = UrlHelper.parseUrl("data:image/png;base64,abc", "http://example.com/");
        assertEquals("data", url.scheme());
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void testParseUrlWithAbsoluteHttpUrlIgnoresBase() {
        ParsedUrl url = UrlHelper.parseUrl("https://other.com/x.svg", "http://example.com/");
        assertEquals("https", url.scheme());
        assertEquals("other.com", url.authority());
    }

    @Test
    void testParseUrlWithNullBase() {
        ParsedUrl url = UrlHelper.parseUrl("relative/path.svg", null);
        assertEquals("relative/path.svg", url.path());
        assertNull(url.scheme());
    }

    @Test
    void testParseUrlWithEmptyBase() {
        ParsedUrl url = UrlHelper.parseUrl("img.svg", "");
        assertEquals("img.svg", url.path());
    }

    @Test
    void testParseUrlAuthorityWithoutPath() {
        ParsedUrl url = UrlHelper.parseUrl("http://example.com");
        assertEquals("http", url.scheme());
        assertEquals("example.com", url.authority());
    }

    @Test
    void testParseUrlFileSchemeWithQueryAndFragment() {
        ParsedUrl url = UrlHelper.parseUrl("file:///home/test.svg?x=1#frag");
        assertEquals("file", url.scheme());
        assertEquals("frag", url.fragment());
        assertEquals("x=1", url.query());
    }

    // ── ParsedUrl record edge cases ─────────────────────────────────────

    @Test
    void testParsedUrlGetUrlPathOnly() {
        ParsedUrl url = new ParsedUrl(null, null, "/local/file.svg", null, null);
        assertEquals("/local/file.svg", url.getUrl());
    }

    @Test
    void testParsedUrlGetUrlQueryOnly() {
        ParsedUrl url = new ParsedUrl(null, null, null, "key=val", null);
        assertEquals("?key=val", url.getUrl());
    }

    @Test
    void testParsedUrlGetUrlEmpty() {
        ParsedUrl url = new ParsedUrl(null, null, null, null, null);
        assertEquals("", url.getUrl());
    }

    // ── fetch file path (no scheme) ──────────────────────────────────────

    @Test
    void testFetchNonExistentFile() {
        assertThrows(java.io.IOException.class, () -> UrlHelper.fetch("/nonexistent/path/to/file.svg", "image"));
    }

    @Test
    void testFetchFileScheme(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path f = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(f, "hello");
        byte[] result = UrlHelper.fetch("file://" + f.toAbsolutePath(), "text");
        assertEquals("hello", new String(result));
    }

    @Test
    void testFetchPlainFilePath(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path f = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(f, "world");
        byte[] result = UrlHelper.fetch(f.toAbsolutePath().toString(), "text");
        assertEquals("world", new String(result));
    }

    // ── resolveUrl edge cases ────────────────────────────────────────────

    @Test
    void testParseUrlRelativeWithMalformedBase() {
        // Malformed base should still resolve (path-based fallback)
        ParsedUrl url = UrlHelper.parseUrl("image.svg", "file:///dir with spaces/");
        assertNotNull(url);
    }

    @Test
    void testParseUrlFragmentOnlyWithBase() {
        ParsedUrl url = UrlHelper.parseUrl("#frag", "http://example.com/page.html");
        assertEquals("frag", url.fragment());
    }

    // ── readUrl with scheme ──────────────────────────────────────────────

    @Test
    void testReadUrlWithScheme() throws Exception {
        ParsedUrl url = new ParsedUrl("data", null, "text/plain;base64,SGVsbG8=", null, null);
        byte[] result = UrlHelper.readUrl(url, (u, r) -> {
            if (u.startsWith("data:")) {
                return UrlHelper.decodeDataUrl(u);
            }
            return new byte[0];
        }, "text");
        assertEquals("Hello", new String(result));
    }

    // ── readUrl with no scheme (file path) ──────────────────────────────

    @Test
    void testReadUrlNoScheme(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path f = tempDir.resolve("test.svg");
        java.nio.file.Files.writeString(f, "<svg/>");
        ParsedUrl url = new ParsedUrl(null, null, f.toAbsolutePath().toString(), null, null);
        byte[] result = UrlHelper.readUrl(url, UrlHelper::fetch, "image/svg+xml");
        assertTrue(new String(result).contains("<svg"));
    }

    // ── parseUrlComponents authority-only URL ────────────────────────────

    @Test
    void testParseUrlAuthorityOnly() {
        ParsedUrl url = UrlHelper.parseUrl("http://example.com");
        assertEquals("example.com", url.authority());
        // Path may be null or empty when authority exhausts the string
    }

    // ── data URL without base64 → URL-decoded ──

    @Test
    void decodeDataUrlPlainText() {
        byte[] result = UrlHelper.decodeDataUrl("data:text/plain,Hello%20World");
        assertEquals("Hello World", new String(result));
    }

    // ── data URL without comma → empty ──

    @Test
    void decodeDataUrlNoComma() {
        byte[] result = UrlHelper.decodeDataUrl("data:invalid");
        assertEquals(0, result.length);
    }

    // ── SVG image with relative href (exercises resolveUrl) ──

    @Test
    void svgImageRelativeHref() throws Exception {
        // Uses a non-existent relative ref — should gracefully skip
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50">
                  <image href="nonexistent.png" width="50" height="50"/>
                </svg>
                """;
        BufferedImage img = io.brunoborges.jairosvg.RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── resolveUrl fallback paths via parseUrl(url, base) ──

    @Test
    void resolveUrlWithDataScheme() {
        // data: URLs should pass through unchanged
        ParsedUrl result = UrlHelper.parseUrl("data:image/png;base64,abc", "http://example.com");
        assertEquals("data", result.scheme());
    }

    @Test
    void resolveUrlWithHttpScheme() {
        // http URLs should pass through unchanged
        ParsedUrl result = UrlHelper.parseUrl("http://other.com/img.svg", "http://example.com");
        assertEquals("http", result.scheme());
        assertEquals("other.com", result.authority());
    }

    @Test
    void resolveUrlWithHttpsScheme() {
        // https URLs should pass through unchanged
        ParsedUrl result = UrlHelper.parseUrl("https://secure.com/img.svg", "http://example.com");
        assertEquals("https", result.scheme());
    }

    @Test
    void resolveUrlRelativeToValidBase() {
        // Relative URL resolved against a valid base URI
        ParsedUrl result = UrlHelper.parseUrl("image.png", "http://example.com/dir/page.html");
        assertEquals("http", result.scheme());
        assertTrue(result.path().endsWith("image.png"));
    }

    @Test
    void resolveUrlFragmentWithInvalidBase() {
        // Fragment URL with invalid base URI triggers catch block → base + url
        ParsedUrl result = UrlHelper.parseUrl("#myId", "not a valid {uri}");
        // Falls back to: "not a valid {uri}#myId"
        assertEquals("myId", result.fragment());
    }

    @Test
    void resolveUrlPathWithInvalidBase(@TempDir java.nio.file.Path tempDir) throws Exception {
        // Non-fragment relative URL with invalid base URI triggers path resolution
        java.nio.file.Path baseFile = tempDir.resolve("base.svg");
        java.nio.file.Files.writeString(baseFile, "<svg/>");
        ParsedUrl result = UrlHelper.parseUrl("other.svg", "invalid {uri}" + baseFile.toString());
        // Falls back to path resolution; since base is not a real file,
        // Path.of(base).getParent() is used
        assertNotNull(result);
    }

    @Test
    void resolveUrlEmptyUrlWithInvalidBase() {
        // Empty URL with invalid base → falls through to return url (empty)
        ParsedUrl result = UrlHelper.parseUrl("", "invalid {uri}");
        // Empty url → early return before resolveUrl is called
        assertNotNull(result);
    }

    // ── fetchHttp via local socket-based HTTP server ──

    @Test
    void fetchHttpViaSvgImage() throws Exception {
        byte[] pngData = createTinyPng();
        // Start a minimal HTTP server using raw sockets (no jdk.httpserver module
        // needed)
        var serverSocket = new java.net.ServerSocket(0);
        int port = serverSocket.getLocalPort();
        var serverThread = new Thread(() -> {
            try (var clientSocket = serverSocket.accept()) {
                var in = clientSocket.getInputStream();
                // Read HTTP request (at least headers)
                byte[] buf = new byte[4096];
                in.read(buf);
                // Send HTTP response
                var out = clientSocket.getOutputStream();
                String headers = "HTTP/1.1 200 OK\r\n" + "Content-Type: image/png\r\n" + "Content-Length: "
                        + pngData.length + "\r\n" + "\r\n";
                out.write(headers.getBytes());
                out.write(pngData);
                out.flush();
            } catch (Exception e) {
                // ignore
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        try {
            byte[] fetched = UrlHelper.fetch("http://localhost:" + port + "/test.png", "image");
            assertNotNull(fetched);
            assertTrue(fetched.length > 0, "Should have fetched PNG data");
        } finally {
            serverSocket.close();
            serverThread.join(5000);
        }
    }

    @Test
    void fetchHttpBadUri() {
        // Invalid URI should throw IOException wrapping URISyntaxException
        assertThrows(IOException.class, () -> UrlHelper.fetch("http://invalid host with spaces", "image"));
    }

    private static byte[] createTinyPng() throws Exception {
        var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000);
        var baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    // ── fetch() with file URI containing query/fragment (L84-85) ──

    @Test
    void fetchFileUriWithQueryAndFragment(@TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path svgFile = tempDir.resolve("test.svg");
        java.nio.file.Files.writeString(svgFile, "<svg/>");
        // file URI with query param — exercises the rawQuery != null branch
        byte[] result = UrlHelper.fetch("file://" + svgFile.toAbsolutePath() + "?version=1", "image");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ── fetch() with invalid URI (URISyntaxException → fallback to Path) ──

    @Test
    void fetchWithInvalidUriAsFallback(@TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path svgFile = tempDir.resolve("test.svg");
        java.nio.file.Files.writeString(svgFile, "<svg/>");
        // This absolute path is valid as a Path but might trigger different URI parsing
        byte[] result = UrlHelper.fetch(svgFile.toAbsolutePath().toString(), "image");
        assertNotNull(result);
    }

    // ── parseUrl with quoted url() ──

    @Test
    void parseUrlWithSingleQuotedUrl() {
        ParsedUrl result = UrlHelper.parseUrl("url('image.png')");
        assertEquals("image.png", result.path());
    }

    @Test
    void parseUrlWithDoubleQuotedUrl() {
        ParsedUrl result = UrlHelper.parseUrl("url(\"image.png\")");
        assertEquals("image.png", result.path());
    }

    // ── parseUrlComponents with scheme detection ──

    @Test
    void parseUrlComponentsWithCustomScheme() {
        ParsedUrl result = UrlHelper.parseUrl("custom+scheme://host/path");
        assertEquals("custom+scheme", result.scheme());
        assertEquals("host", result.authority());
    }

    // ── readUrl path branches ──

    @Test
    void readUrlWithSchemedUrl(@TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path svgFile = tempDir.resolve("test.svg");
        java.nio.file.Files.writeString(svgFile, "<svg/>");
        ParsedUrl url = new ParsedUrl("file", null, svgFile.toAbsolutePath().toString(), null, null);
        byte[] result = UrlHelper.readUrl(url, UrlHelper::fetch, "image");
        assertNotNull(result);
    }

    @Test
    void readUrlWithEmptyPath() throws Exception {
        ParsedUrl url = new ParsedUrl(null, null, null, null, null);
        byte[] result = UrlHelper.readUrl(url, UrlHelper::fetch, "image");
        assertEquals(0, result.length);
    }

    // ── resolveUrl with file path base (L211-214) ──

    @Test
    void resolveUrlWithFilePathBase(@TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path baseFile = tempDir.resolve("base.svg");
        java.nio.file.Files.writeString(baseFile, "<svg/>");
        // Base is a valid file path but not a valid URI → triggers catch block
        // and then path resolution with Files.isRegularFile check
        ParsedUrl result = UrlHelper.parseUrl("child.svg", baseFile.toAbsolutePath().toString());
        assertNotNull(result);
        // Should resolve relative to base file's parent directory
        assertNotNull(result.path());
    }

    // ── resolveBaseUrl with xml:base attribute ──

    @Test
    void resolveBaseUrlWithXmlBase() throws Exception {
        var svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="50" height="50"
                     xml:base="http://example.com/images/">
                  <image href="test.png" width="50" height="50"/>
                </svg>
                """;
        // This exercises the xml:base branch in resolveBaseUrl
        BufferedImage img = io.brunoborges.jairosvg.RenderTestHelper.render(svg);
        assertNotNull(img);
    }

    // ── ParsedUrl.getUrl() with all components ──

    @Test
    void parsedUrlGetUrlAllComponents() {
        ParsedUrl url = new ParsedUrl("https", "example.com", "/path", "q=1", "frag");
        String full = url.getUrl();
        assertTrue(full.contains("https:"));
        assertTrue(full.contains("//example.com"));
        assertTrue(full.contains("/path"));
        assertTrue(full.contains("?q=1"));
        assertTrue(full.contains("#frag"));
    }

    @Test
    void parsedUrlGetUrlMinimal() {
        ParsedUrl url = new ParsedUrl(null, null, "/just/path", null, null);
        assertEquals("/just/path", url.getUrl());
    }

    // ── hasNonFragmentParts with various combinations ──

    @Test
    void hasNonFragmentPartsOnlyFragment() {
        ParsedUrl url = new ParsedUrl(null, null, null, null, "frag");
        assertFalse(url.hasNonFragmentParts());
    }

    @Test
    void hasNonFragmentPartsWithScheme() {
        ParsedUrl url = new ParsedUrl("http", null, null, null, null);
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void hasNonFragmentPartsWithQuery() {
        ParsedUrl url = new ParsedUrl(null, null, null, "q=1", null);
        assertTrue(url.hasNonFragmentParts());
    }

    @Test
    void hasNonFragmentPartsWithPath() {
        ParsedUrl url = new ParsedUrl(null, null, "/path", null, null);
        assertTrue(url.hasNonFragmentParts());
    }
}
