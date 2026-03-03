package io.brunoborges.jairosvg;

import io.brunoborges.jairosvg.util.UrlHelper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlHelperTest {

    @Test
    void testUrlParsing() {
        var url = UrlHelper.parseUrl("#myId");
        assertEquals("myId", url.fragment());

        var url2 = UrlHelper.parseUrl("url(#grad1)");
        assertEquals("grad1", url2.fragment());
    }
}
