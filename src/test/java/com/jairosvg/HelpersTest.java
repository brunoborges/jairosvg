package com.jairosvg;

import com.jairosvg.util.Helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HelpersTest {

    @Test
    void testNormalize() {
        assertEquals("10 20 30 40", Helpers.normalize("10,20,30,40"));
        assertEquals("10 -20 30", Helpers.normalize("10-20 30"));
    }
}
