package com.litus.guias.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiContrastTest {

    @Test
    void stylesheetUsesReadableContrastForCommonControls() throws IOException {
        var resource = UiContrastTest.class.getResourceAsStream("/com/litus/guias/ui/aero.css");
        assertNotNull(resource);
        String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(css.contains(".header-date"));
        assertTrue(css.contains("-fx-text-fill: #123f5d;"));
        assertTrue(css.contains(".muted { -fx-text-fill: #4f6f85; }"));
        assertTrue(css.contains("-fx-prompt-text-fill: #607789;"));
        assertTrue(css.contains(".primary-button:hover"));
        assertTrue(css.contains(".danger-button:hover"));
        assertTrue(css.contains(".button:disabled { -fx-opacity: .85;"));
        assertTrue(css.contains("-fx-background-color: #176d9f;"));

        assertTrue(contrast("#123f5d", "#eaf7ff") >= 4.5);
        assertTrue(contrast("#4f6f85", "#ffffff") >= 4.5);
        assertTrue(contrast("#ffffff", "#176d9f") >= 4.5);
    }

    private static double contrast(String first, String second) {
        double light = Math.max(luminance(first), luminance(second));
        double dark = Math.min(luminance(first), luminance(second));
        return (light + 0.05) / (dark + 0.05);
    }

    private static double luminance(String hex) {
        double red = channel(hex.substring(1, 3));
        double green = channel(hex.substring(3, 5));
        double blue = channel(hex.substring(5, 7));
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double channel(String hex) {
        double value = Integer.parseInt(hex, 16) / 255.0;
        return value <= 0.04045
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
