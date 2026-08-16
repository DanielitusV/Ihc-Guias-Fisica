package com.litus.guias.persistence;

import java.nio.file.Path;

public final class ApplicationDataPath {

    private ApplicationDataPath() {
    }

    public static Path resolve() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "GuiasFisica");
        }
        return Path.of(
                System.getProperty("user.home"),
                "AppData",
                "Local",
                "GuiasFisica"
        );
    }
}
