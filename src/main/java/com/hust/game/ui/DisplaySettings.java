package com.hust.game.ui;

public final class DisplaySettings {
    public enum WindowMode {
        WINDOWED,
        FULLSCREEN
    }

    private static WindowMode windowMode = WindowMode.WINDOWED;

    private DisplaySettings() {
    }

    public static WindowMode getWindowMode() {
        return windowMode;
    }

    public static void setWindowMode(WindowMode mode) {
        windowMode = mode != null ? mode : WindowMode.WINDOWED;
    }

    public static boolean isFullscreen() {
        return windowMode == WindowMode.FULLSCREEN;
    }

    public static String getWindowModeLabel() {
        return isFullscreen() ? "FULLSCREEN" : "WINDOWED";
    }
}
