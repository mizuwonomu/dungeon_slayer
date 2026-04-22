package com.hust.game.enemy;

import com.hust.game.entities.base.MovingEntity;
import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;

public abstract class Enemy extends MovingEntity {

    // Đã xóa dòng protected double speed = 1.0;
    protected Player targetPlayer;
    protected int maxHp;
    protected int hp;
    protected int damage;
    protected int knockback;

    // Constructor tạm thời ở Giai đoạn 1
    public Enemy(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, 1.0);
        this.targetPlayer = targetPlayer; // Chốt mục tiêu
    }
    /**
     * Nhận sát thương từ player.
     * Không cho hp xuống dưới 0.
     * TODO: thêm hiệu ứng chết (drop item, xóa khỏi enemyList) sau
     */
    public void takeDamage(int amount) {
        this.hp = Math.max(0, this.hp - amount);

        // In ra console để test logic trước khi có asset
        System.out.println(this.getClass().getSimpleName()
            + " nhận " + amount + " damage! HP còn: " + this.hp + "/" + this.maxHp);
    }

    /** Kiểm tra enemy đã chết chưa */
    public boolean isDead() {
        return this.hp <= 0;
    }
    
    @Override
    public void update() {
        // Không có mục tiêu -> đứng
        if (targetPlayer == null)
            return;

        double playerX = targetPlayer.getX();
        double playerY = targetPlayer.getY();

        // Thuật toán nội suy tìm tọa độ Player
        double diffX = playerX - this.x;
        double diffY = playerY - this.y;

        double distance = Math.sqrt(diffX * diffX + diffY * diffY);

        double moveX = 0;
        double moveY = 0;

        if (distance > 0) {
            moveX = (diffX / distance) * speed;
            moveY = (diffY / distance) * speed;
        }

        double nextX = this.x + moveX;
        double nextY = this.y + moveY;

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
    }
}
