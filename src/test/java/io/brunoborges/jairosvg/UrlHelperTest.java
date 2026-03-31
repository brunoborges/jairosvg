package io.brunoborges.jairosvg;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
}
