package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;

public class Slime extends Enemy {
    private int damageTick = 0;
    private Image shadowSprite;
    private Image dieSprite;
    private boolean isDying = false;
    private static final javafx.scene.effect.ColorAdjust WHITE_EFFECT = new javafx.scene.effect.ColorAdjust(0, 0, 1.0, 0);

    public Slime(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 2.0;
        this.maxHp = 50;
        this.hp = maxHp;
        this.damage = 5;
        this.knockback = 3;
        
        try {
            this.shadowSprite = new Image(getClass().getResourceAsStream("/assets/enemy/slime_shadow.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy slime_shadow.png");
        }
        
        try {
            this.dieSprite = new Image(getClass().getResourceAsStream("/assets/enemy/slime_die.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy slime_die.png");
        }
    }

    @Override
    protected void drawShadow(GraphicsContext gc) {
        if (shadowSprite != null) {
            double sx = this.frameIndex * this.frameWidth;
            // Vẽ bóng trùng khớp toạ độ của Slime nhưng không đi qua logic lật chiều ảnh
            gc.drawImage(shadowSprite, sx, 0, this.frameWidth, this.frameHeight, this.x, this.y, this.renderWidth, this.renderHeight);
        }
    }

    @Override
    public void update() {
        if (!isActive) return; // Bất động khi ra ngoài camera

        // Lưu lại vị trí hiện tại trước khi update
        double prevX = this.x;
        double prevY = this.y;

        // Gọi update của Enemy để tự động lật mặt, chạy animation và tính toán toạ độ
        // mới
        super.update(); // Gọi hàm của Enemy để di chuyển và cập nhật frameIndex

        if (this.hp <= 0) {
            this.hitStunTimer = 0; // Xóa hitStun để class Enemy không tự động ép đè frameIndex = 0
            
            // Đợi hết 6 frame lóe trắng đầu tiên mới đổi sang animation chết
            if (this.flashTimer <= 54) {
                if (!isDying) {
                    isDying = true;
                    if (dieSprite != null) {
                        this.spriteSheet = dieSprite;
                        this.numFrames = 6; // Chuyển về 6 frame
                        this.frameWidth = dieSprite.getWidth() / 6.0;
                        this.frameHeight = dieSprite.getHeight();
                    }
                    this.frameIndex = 0;
                    this.animationTimer = 0;
                }
                
                // Chạy animation chết dần trong 54 frames còn lại (54 / 6 = 9 frames mỗi ảnh)
                this.animationTimer++;
                if (this.animationTimer >= 9) {
                    this.animationTimer = 0;
                    if (this.frameIndex < 5) {
                        this.frameIndex++;
                    }
                }
            }
            return;
        }

        // Slime có 8 frame (index 0 -> 7).
        // Frame 1, 2 (index 0, 1): Đứng im lấy đà.
        // --- SỬA LỖI: Chỉ khôi phục vị trí (đứng im) nếu KHÔNG BỊ KNOCKBACK VÀ KHÔNG BỊ KHỰNG ---
        if (this.kbTimer <= 0 && this.frameIndex < 3 && this.hitStunTimer <= 0) {
            this.x = prevX; 
            this.y = prevY;
        }

        // Xử lý logic tấn công (chạm vào người chơi)
        handleTouchDamage();

        // Phát âm thanh đúng vào khoảnh khắc Slime bắt đầu bật nhảy (frameIndex == 3)
        // vì theo logic phía trên, frame 0, 1, 2 nó đang đứng im lấy đà.
        if (this.animationTimer == 0 && this.frameIndex == 7) {
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
        if (!this.isHarmless && this.intersects(targetPlayer) && damageTick <= 0) {
            targetPlayer.takeDamage(this.damage, this);
            damageTick = 30; // Cooldown 0.5s để tránh gây damage mỗi frame
            
            // Tính toán hướng để đẩy Slime lùi ra xa
            double pCenterX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0;
            double pCenterY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0;
            double sCenterX = this.x + this.renderWidth / 2.0;
            double sCenterY = this.y + this.renderHeight / 2.0;
            
            double diffX = sCenterX - pCenterX;
            double diffY = sCenterY - pCenterY;
            double dist = Math.sqrt(diffX * diffX + diffY * diffY);
            if (dist == 0) { diffX = 1; dist = 1; }
            
            this.kbVectorX = (diffX / dist) * 4.0; // Lực lùi lại nhẹ 
            this.kbVectorY = (diffY / dist) * 4.0;
            this.kbTimer = 6; // Lùi trong 6 frames (0.1 giây)
            
            this.hitStunTimer = 18; // Khựng đóng băng 0.3s (18 frames)
            this.frameIndex = 0; // Đưa về animation lấy đà ban đầu
            this.animationTimer = 0;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (this.hp <= 0) {
            drawShadow(gc);
            gc.save();
            if (this.flashTimer > 54) gc.setEffect(WHITE_EFFECT); // Lóe trắng lúc vừa nhận đòn kết liễu
            
            double renderX = this.x;
            double renderY = this.y;
            if (this.isFlipped) renderX += this.renderWidth;
            double renderW = this.isFlipped ? -this.renderWidth : this.renderWidth;
            
            gc.drawImage(this.spriteSheet, this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight, renderX, renderY, renderW, this.renderHeight);
            gc.restore();
        } else {
            super.render(gc); // Lúc sống vẫn xài render gốc có chớp trắng khi nhận sát thương bình thường
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        // Thu nhỏ hitbox của Slime lại 35% mỗi bên (Tránh việc Slime quá dễ chạm trúng Player)
        double paddingX = this.renderWidth * 0.35;
        double paddingY = this.renderHeight * 0.35;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        double w = renderWidth * 0.6; 
        double h = renderHeight * 0.4;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }
}
