package com.hust.game.entities.environment;

import com.hust.game.entities.base.BaseEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Gate extends BaseEntity {
    private boolean isOpen = false;
    private int animTimer = 0;
    private int animDelay = 15; // Tốc độ animation (2s cho 8 frame -> 120 frames / 8 = 15)
    private boolean isDone = false; 

    public Gate(double x, double y, Image spriteSheet) {
        super(x, y, spriteSheet, 8, 48, 48); // Kích thước render bằng 1 ô (TILE_SIZE = 48)
        this.frameWidth = 64;  // Kích thước 1 frame gốc trong file gate.png (512/8)
        this.frameHeight = 64;
        this.frameIndex = 0;
    }

    public void open() {
        if (!isOpen) {
            isOpen = true;
            com.hust.game.audio.SoundManager.playGateBurnSound();
        }
    }

    @Override
    public void update() {
        if (isOpen && !isDone) {
            animTimer++;
            if (animTimer >= animDelay) {
                animTimer = 0;
                frameIndex++;
                if (frameIndex >= numFrames) {
                    isDone = true;
                    frameIndex = numFrames - 1; // Ẩn hoặc giữ ở frame cuối sau khi mở xong
                }
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (!isDone) {
            super.render(gc);
        }
    }

    public boolean isSolid() {
        return !isOpen; // Chỉ xuyên qua được khi cửa bắt đầu mở
    }
    
    @Override
    public Rectangle2D getBoundary() {
        return new Rectangle2D(x, y, renderWidth, renderHeight); // Hitbox đúng bằng 1 ô vuông map
    }
}