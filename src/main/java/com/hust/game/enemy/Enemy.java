package com.hust.game.enemy;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.base.MovingEntity;
import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;

public abstract class Enemy extends MovingEntity {

    protected Player targetPlayer;
    protected int maxHp;
    protected int hp;
    protected int damage;
    protected int knockback;
    protected int animationTimer = 0;
    protected int animationDelay = 10;
    protected double lastX, lastY;
    protected double moveX, moveY;

    // Constructor tạm thời ở Giai đoạn 1
    public Enemy(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, 1.0);
        this.targetPlayer = targetPlayer; // Chốt mục tiêu
    }

    @Override
    public void update() {
        // Lưu vị trí cũ
        this.lastX = this.x;
        this.lastY = this.y;

        // Không có mục tiêu -> đứng
        if (targetPlayer == null)
            return;

        double playerX = targetPlayer.getX();
        double playerY = targetPlayer.getY();

        // Thuật toán nội suy tìm tọa độ Player
        double diffX = playerX - this.x;
        double diffY = playerY - this.y;

        double distance = Math.sqrt(diffX * diffX + diffY * diffY);

        this.moveX = 0;
        this.moveY = 0;

        if (distance > 0) {
            this.moveX = (diffX / distance) * speed;
            this.moveY = (diffY / distance) * speed;
            
            // Xoay mặt nhìn theo Player (Mirror ảnh ngang)
            if (diffX < 0) {
                this.isFlipped = true;  // Đi sang trái thì lật mặt
            } else if (diffX > 0) {
                this.isFlipped = false; // Đi sang phải thì giữ nguyên
            }
        }

        double nextX = this.x + this.moveX;
        double nextY = this.y + this.moveY;

        int TILE_SIZE = 48;

        int nextCol = (int) (nextX / TILE_SIZE);
        int nextRow = (int) (nextY / TILE_SIZE);

        /*
         * ------------------------------------------------------------------
         * if (MapManager.map[nextRow][nextCol] == 1) {
         * // Mảng số 1 là Tường -> Không cho phép gán nextX, nextY vào x, y
         * // Quái sẽ đứng im đập mặt vào tường
         * } else {
         * // Mảng số 0 là Đường đi hợp lệ
         * this.x = nextX;
         * this.y = nextY;
         * }
         * ------------------------------------------------------------------
         */

        this.x = nextX;
        this.y = nextY;

        animationTimer++;
        if (animationTimer >= animationDelay) {
            animationTimer = 0;
            frameIndex = (frameIndex + 1) % numFrames;
        }
    }

    public void onCollision(BaseEntity other) {
        this.x = lastX;
        this.y = lastY;
    }

    // --- GETTERS DÀNH CHO TRƯỢT TƯỜNG (SLIDING) TẠI APP.JAVA ---
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getMoveX() { return moveX; }
    public double getMoveY() { return moveY; }
}
