package com.hust.game.constants;

public class GameConstants {
    private GameConstants() {
    }

    //kích thước cửa sổ chơi
    public static final int WINDOW_WIDTH = 1440; // 30 cột * 48px
    public static final int WINDOW_HEIGHT = 864; // 18 hàng * 48px

    // Kích thước map (số ô)
    public static final int MAX_SCREEN_COL = 30;
    public static final int MAX_SCREEN_ROW = 18;

    // Kích thước thực tế của Bản đồ Thế giới (Khớp với file level1.txt mới là 256 cột ngang x 32 hàng dọc)
    public static final int MAX_WORLD_COL = 256; 
    public static final int MAX_WORLD_ROW = 32; 

    public static final int TILE_SIZE = 48;

    public static final int WORLD_WIDTH = MAX_WORLD_COL * TILE_SIZE;
    public static final int WORLD_HEIGHT = MAX_WORLD_ROW * TILE_SIZE;

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
