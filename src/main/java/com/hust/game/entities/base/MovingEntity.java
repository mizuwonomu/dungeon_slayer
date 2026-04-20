package com.hust.game.entities.base;

import com.hust.game.entities.BaseEntity;
import javafx.scene.image.Image;

public abstract class MovingEntity extends BaseEntity {
    
    protected double speed;

    public MovingEntity(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight);
    }
}
