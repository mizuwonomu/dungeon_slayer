package com.hust.game.enemy;

import com.hust.game.entities.base.MovingEntity;
import com.hust.game.entities.Player;
import javafx.scene.image.Image;

public class Enemy extends MovingEntity {

    protected double speed = 1.0;

    // Constructor tạm thời ở Giai đoạn 1
    public Enemy(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight);

    }

    @Override
    public void update() {
        // Giai đoạn 1: Quái tự động trôi chéo xuống dưới để test chuyển động
        this.x += speed;
        this.y += speed;
    }
}
