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
        // Lưu lại vị trí hiện tại trước khi update
        double prevX = this.x;
        double prevY = this.y;

        // Gọi update của Enemy để tự động lật mặt, chạy animation và tính toán toạ độ
        // mới
        super.update(); // Gọi hàm của Enemy để di chuyển và cập nhật frameIndex

        // Nếu cạn máu thì dừng các logic đặc thù của Slime (để super.update xử lý knockback và trừ flashTimer)
        if (this.hp <= 0) {
            return;
        }

        // Slime có 8 frame (index 0 -> 7).
        // Frame 1, 2 (index 0, 1): Đứng im lấy đà.
        // --- SỬA LỖI: Chỉ khôi phục vị trí (đứng im) nếu KHÔNG BỊ KNOCKBACK ---
        if (this.kbTimer <= 0 && this.frameIndex < 3) {
            this.x = prevX; 
            this.y = prevY;
        }

        // Xử lý logic tấn công (chạm vào người chơi)
        handleTouchDamage();

        // slime_move: phát ngay sau khi frame di chuyển kết thúc (cycle animation về 0)
        if (this.animationTimer == 0 && this.frameIndex == 0) {
            com.hust.game.audio.SoundManager.playSlimeMoveSound();
        }
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
