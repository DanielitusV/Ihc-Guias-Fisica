package com.litus.guias.ui;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppIconTest {

    @Test
    void officialUmssIconIsSquareTransparentAndLargeEnoughForWindows() throws Exception {
        var resource = AppIconTest.class.getResourceAsStream("/com/litus/guias/icon/umss.png");
        assertNotNull(resource);
        var image = ImageIO.read(resource);

        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
    }
}
