package com.hust.game.ui;

public class SettingsTabState {
    public enum Tab {
        SOUND_DISPLAY,
        HOTKEYS
    }

    private Tab activeTab = Tab.SOUND_DISPLAY;

    public Tab getActiveTab() {
        return activeTab;
    }

    public void showSoundDisplay() {
        activeTab = Tab.SOUND_DISPLAY;
    }

    public void showHotkeys() {
        activeTab = Tab.HOTKEYS;
    }

    public void showNextTab() {
        switch (activeTab) {
            case SOUND_DISPLAY -> activeTab = Tab.HOTKEYS;
            case HOTKEYS -> activeTab = Tab.HOTKEYS;
        }
    }

    public void showPreviousTab() {
        switch (activeTab) {
            case SOUND_DISPLAY -> activeTab = Tab.SOUND_DISPLAY;
            case HOTKEYS -> activeTab = Tab.SOUND_DISPLAY;
        }
    }
}
