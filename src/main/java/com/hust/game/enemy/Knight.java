package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Knight extends Enemy {

    private int dashCooldownTimer = 120;
    private final int MAX_DASH_COOLDOWN = 180;
    private int MAX_DASH_DURATION = 30;
    private Image normalSprite;
    private Image skillSprite;
    private boolean isDashing = false;
    private double dashVectorX, dashVectorY;

    public Knight(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer, Image skillSprite) {
        super(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 1.2;
        this.maxHp = 150;
        this.hp = maxHp;
        this.damage = 30;
        this.knockback = 5;

        this.normalSprite = sprSheet;
        this.skillSprite = skillSprite;
    }

    public Knight(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        this(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer, null);
    }

    @Override
    public void update() {
        if (this.flashTimer > 0) {
            this.flashTimer--;
        }
        if (this.hitStunTimer > 0) {
            this.hitStunTimer--;
        }

        // --- BỔ SUNG LOGIC KNOCKBACK MÀ TRƯỚC ĐÓ BỊ THIẾU ---
        this.lastX = this.x;
        this.lastY = this.y;
        if (this.kbTimer > 0) {
            this.kbTimer--;
            this.x += kbVectorX;
            this.y += kbVectorY;
        }

        // Nếu đã chết, giữ nguyên frame animation cuối và ngắt toàn bộ logic AI
        if (this.hp <= 0) {
            return;
        }
        
        if (this.kbTimer > 0 || this.hitStunTimer > 0) {
            return; // Còn sống nhưng đang bị knockback hoặc choáng thì ngắt AI lướt/đi bộ
        }
        // ----------------------------------------------------

        if (dashCooldownTimer > 0) {
            dashCooldownTimer--;
        }
        if (!isDashing) {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex >= this.numFrames) {
                    this.frameIndex = 0;
                }
            }

            double currentDiffX = targetPlayer.getX() - this.x;
            if (currentDiffX < 0) {
                this.isFlipped = true;
            } else {
                this.isFlipped = false;
            }

            if (dashCooldownTimer == 0) {
                double diffX = targetPlayer.getX() - this.x;
                double diffY = targetPlayer.getY() - this.y;

                double distance = Math.sqrt(diffX * diffX + diffY * diffY);

                if (distance > 0) {
                    dashVectorX = (diffX / distance) * (this.speed * 40); // Tăng mạnh tốc độ lướt
                    dashVectorY = (diffY / distance) * (this.speed * 40);
                }

                if (diffX < 0) {
                    this.isFlipped = true; // Player ở bên trái -> Quay trái
                } else {
                    this.isFlipped = false; // Player ở bên phải -> Quay phải
                }

                isDashing = true;

                com.hust.game.audio.SoundManager.playKnightReadySound();

                this.spriteSheet = skillSprite;
                this.numFrames = 14;
                this.frameWidth = skillSprite.getWidth() / this.numFrames;
                this.frameHeight = skillSprite.getHeight();
                this.frameIndex = 0;
            }
        } else {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex == 8) { // Khoảnh khắc chuẩn bị lao đi
                    com.hust.game.audio.SoundManager.playKnightAtkSound();
                }
            }
            if (this.frameIndex == 8) { // Chỉ lướt trong 1 frame duy nhất để quãng đường ngắn lại
                this.lastX = this.x;
                this.lastY = this.y;

                this.x += dashVectorX;
                this.y += dashVectorY;
            }
            if (this.frameIndex >= this.numFrames) {
                isDashing = false;
                this.spriteSheet = normalSprite;
                this.numFrames = 8;
                this.frameWidth = normalSprite.getWidth() / this.numFrames;
                this.frameHeight = normalSprite.getHeight();
                this.frameIndex = 0;
                dashCooldownTimer = MAX_DASH_COOLDOWN;
            }
        }
    }

    public boolean isDashing() {
        return this.isDashing;
    }

    public boolean isDealingDamage() {
        // Chỉ gây sát thương đúng vào khoảnh khắc lướt (frame 8 và 9)
        return this.isDashing && this.frameIndex >= 8 && this.frameIndex <= 9;
    }

    @Override
    public Rectangle2D getBoundary() {
        // Thu nhỏ hitbox của Knight: Cắt bớt 25% diện tích viền ngoài mỗi bên
        double paddingX = this.renderWidth * 0.25;
        double paddingY = this.renderHeight * 0.25;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }

    @Override
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        // Hiệp sĩ miễn nhiễm với bị đẩy lùi và choáng khi đang dùng chiêu
        if (this.isDashing) {
            return;
        }
        super.applyKnockback(dir);
    }

}
