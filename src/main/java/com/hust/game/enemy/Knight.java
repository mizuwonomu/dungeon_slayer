package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Knight extends Enemy {

    private int dashCooldownTimer = 0;
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
                    dashVectorX = (diffX / distance) * (this.speed * 10);
                    dashVectorY = (diffY / distance) * (this.speed * 10);
                }

                if (diffX < 0) {
                    this.isFlipped = true; // Player ở bên trái -> Quay trái
                } else {
                    this.isFlipped = false; // Player ở bên phải -> Quay phải
                }

                isDashing = true;
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
            }
            if (this.frameIndex == 8 || this.frameIndex == 9) {
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

}
