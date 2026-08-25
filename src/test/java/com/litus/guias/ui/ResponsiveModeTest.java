package com.litus.guias.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponsiveModeTest {

    @Test
    void selectsModeAtExactBreakpoints() {
        assertEquals(ResponsiveMode.COMPACT, ResponsiveMode.forWidth(899));
        assertEquals(ResponsiveMode.MEDIUM, ResponsiveMode.forWidth(900));
        assertEquals(ResponsiveMode.MEDIUM, ResponsiveMode.forWidth(1279));
        assertEquals(ResponsiveMode.WIDE, ResponsiveMode.forWidth(1280));
    }
}
