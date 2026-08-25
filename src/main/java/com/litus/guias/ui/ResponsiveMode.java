package com.litus.guias.ui;

public enum ResponsiveMode {
    COMPACT,
    MEDIUM,
    WIDE;

    public static ResponsiveMode forWidth(double width) {
        if (width < 900) {
            return COMPACT;
        }
        if (width < 1280) {
            return MEDIUM;
        }
        return WIDE;
    }
}
