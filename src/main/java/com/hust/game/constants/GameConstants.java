package com.hust.game.constants;

public class GameConstants {
    private GameConstants() {
    }

    //kích thước cửa sổ chơi
    public static final int WINDOW_WIDTH = 816;
    public static final int WINDOW_HEIGHT = 480;

    // Kích thước map (số ô)
    public static final int MAX_SCREEN_COL = 17;
    public static final int MAX_SCREEN_ROW = 10;

    public static final int TILE_SIZE = 48;
    public static final int TARGET_FPS = 60;

    //player stats
    public static final int PLAYER_NUM_FRAMES = 8;
    public static final double PLAYER_SPEED = 3.0;
    public static final int PLAYER_ANIMATION_DELAY = 10;
    public static final int PLAYER_MAX_HP = 100;
    public static final int PLAYER_MAX_MANA = 50;

    // Enemy (Member B tham chiếu)
    public static final double ENEMY_SPEED = 1.5;
    public static final int    ENEMY_MAX_HP = 50;
}
