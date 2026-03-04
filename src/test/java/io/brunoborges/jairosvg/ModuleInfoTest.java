package io.brunoborges.jairosvg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleInfoTest {

    @Test
    void testNamedModule() {
        Module module = JairoSVG.class.getModule();
        assertTrue(module.isNamed());
        assertEquals("io.brunoborges.jairosvg", module.getName());
    }
}
