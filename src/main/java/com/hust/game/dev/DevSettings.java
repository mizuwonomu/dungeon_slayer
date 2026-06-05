package com.hust.game.dev;

public final class DevSettings {
    private static final String DEV_MODE_PROPERTY = "game.devMode";

    private DevSettings() {
    }

    public static boolean isDevMode() {
        return Boolean.getBoolean(DEV_MODE_PROPERTY);
    }

    public static boolean shouldSkipMenuIntro() {
        return isDevMode();
    }

    public static boolean shouldSkipLoadingScreens() {
        return isDevMode();
    }
}
