package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;

public class Slime extends Enemy {
    private int damageTick = 0;
    private Image dieSprite;
    private boolean isDying = false;

    private Rectangle2D[] frameHitboxes = new Rectangle2D[8]; // Lưu cache hitbox riêng cho từng frame

    public Slime(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 2.0;
        this.maxHp = 50;
        this.hp = maxHp;
        this.damage = 5;
        this.knockback = 3;
        
        try {
            this.dieSprite = new Image(getClass().getResourceAsStream("/assets/enemy/slime_die.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy slime_die.png");
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
            gc.save();
            double renderX = this.x;
            double renderY = this.y;
            if (this.isFlipped) renderX += this.renderWidth;
            double renderW = this.isFlipped ? -this.renderWidth : this.renderWidth;

            gc.drawImage(this.spriteSheet,
                this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight,
                renderX, renderY, renderW, this.renderHeight);
            gc.restore();

            // Chớp trắng lúc vừa nhận đòn kết liễu — draw white sprite đè lên, pixel trong suốt không bị ảnh hưởng
            if (this.flashTimer > 54) {
                applyWhiteFlash(gc, 0.9);
            }
        } else {
            super.render(gc); // Lúc sống dùng Enemy.render() có sẵn flash handling
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        // Bỏ qua khi đã chết
        if (this.hp <= 0) {
            return new Rectangle2D(x, y, renderWidth, renderHeight);
        }
        
        int idx = this.frameIndex;
        if (idx < 0 || idx >= frameHitboxes.length) return new Rectangle2D(x, y, renderWidth, renderHeight);
        
        // Cắt hitbox động: Quét pixel loại bỏ điểm ảnh rỗng (trong suốt) khi Slime nhảy lên
        if (frameHitboxes[idx] == null) {
            javafx.scene.image.PixelReader pr = this.spriteSheet.getPixelReader();
            if (pr != null) {
                int fw = (int) this.frameWidth;
                int fh = (int) this.frameHeight;
                int sx = idx * fw;

                int minX = fw, maxX = 0, minY = fh, maxY = 0;
                for (int py = 0; py < fh; py++) {
                    for (int px = 0; px < fw; px++) {
                        if (sx + px >= this.spriteSheet.getWidth() || py >= this.spriteSheet.getHeight()) continue;
                        if (pr.getColor(sx + px, py).getOpacity() > 0.1) {
                            if (px < minX) minX = px;
                            if (px > maxX) maxX = px;
                            if (py < minY) minY = py;
                            if (py > maxY) maxY = py;
                        }
                    }
                }
                if (minX <= maxX && minY <= maxY) {
                    double scaleX = this.renderWidth / fw;
                    double scaleY = this.renderHeight / fh;
                    
                    double boxW = (maxX - minX + 1) * scaleX;
                    double boxH = (maxY - minY + 1) * scaleY;
                    frameHitboxes[idx] = new Rectangle2D(minX * scaleX, minY * scaleY, boxW, boxH);
                } else {
                    frameHitboxes[idx] = new Rectangle2D(0, 0, renderWidth, renderHeight);
                }
            } else {
                frameHitboxes[idx] = new Rectangle2D(0, 0, renderWidth, renderHeight);
            }
        }
        
        Rectangle2D box = frameHitboxes[idx];
        if (this.isFlipped) {
            double flippedX = this.renderWidth - (box.getMinX() + box.getWidth());
            return new Rectangle2D(this.x + flippedX, this.y + box.getMinY(), box.getWidth(), box.getHeight());
        } else {
            return new Rectangle2D(this.x + box.getMinX(), this.y + box.getMinY(), box.getWidth(), box.getHeight());
        }
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