package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;

public class Slime extends Enemy {
    private int damageTick = 0;

    public Slime(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 2.0;
        this.maxHp = 50;
        this.hp = maxHp;
        this.damage = 5;
        this.knockback = 3;
    }

    @Override
    public void update() {
        // Nếu chết hoặc đang bị choáng (nháy đỏ) thì không làm gì cả
        if (this.hp <= 0 || this.flashTimer > 0) {
            if (this.flashTimer > 0)
                this.flashTimer--;
            return;
        }

        // Lưu lại vị trí hiện tại trước khi update
        double prevX = this.x;
        double prevY = this.y;

        // Gọi update của Enemy để tự động lật mặt, chạy animation và tính toán toạ độ
        // mới
        super.update(); // Gọi hàm của Enemy để di chuyển và cập nhật frameIndex

        // Slime có 8 frame (index 0 -> 7).
        // Frame 1, 2 (index 0, 1): Đứng im lấy đà.
        if (this.frameIndex < 3) {
            this.x = prevX; // Khôi phục lại toạ độ cũ để không di chuyển
            this.y = prevY;
        }

        // Xử lý logic tấn công (chạm vào người chơi)
        handleTouchDamage();
    }

    /**
     * Xử lý logic gây sát thương khi chạm vào người chơi.
     * Tách ra thành hàm riêng để có thể gọi cả lúc di chuyển và đứng im.
     */
    private void handleTouchDamage() {
        if (damageTick > 0) {
            damageTick--;
        }
        // Chỉ tấn công nếu đang không trong cooldown nhỏ sau đòn đánh
        if (this.intersects(targetPlayer) && damageTick <= 0) {
            targetPlayer.takeDamage(this.damage);
            damageTick = 30; // Cooldown 0.5s để tránh gây damage mỗi frame
        }
    }
}
