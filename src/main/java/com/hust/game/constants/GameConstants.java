package com.hust.game.constants;

public class GameConstants {
    private GameConstants(){}

    //kích thước cửa sổ chơi
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;

    public static final int TILE_SIZE = 48;
    public static final int TARGET_FPS = 60;

    //player stats
    public static final int PLAYER_NUM_FRAMES = 8;
    public static final double PLAYER_SPEED = 3.0;
    public static final int PLAYER_ANIMATION_DELAY = 10;
    public static final int PLAYER_MAX_HP = 100;

    // Enemy (Member B tham chiếu)
    public static final double ENEMY_SPEED = 1.5;
    public static final int    ENEMY_MAX_HP = 50;
}
