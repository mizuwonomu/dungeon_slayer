package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class FinalBoss extends Enemy {

    public FinalBoss(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight, Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.maxHp = 10000;
        this.hp = maxHp;
        this.damage = 0; // Để test combat, không gây sát thương
        this.knockback = 1; // Knockback rất nhẹ
        this.speed = 0;
        this.isImmobile = true; // Chỉ đứng im
        this.animationDelay = 12; // Tốc độ chạy animation nhàn rỗi
    }

    @Override
    public Rectangle2D getBoundary() {
        double paddingX = this.renderWidth * 0.25;
        double paddingY = this.renderHeight * 0.2;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        double w = renderWidth * 0.4;
        double h = renderHeight * 0.2;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }
}