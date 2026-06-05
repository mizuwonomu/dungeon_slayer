package com.hust.game.entities.base;

import javafx.scene.image.Image;

//cho wall, tile để định nghĩa map
//kế thừa từ baseentity, nhưng map không cần di chuyển
//map là 1 object tĩnh

/**
 * Entity đứng yên: tường, cổng, item nằm trên map.
 * Member A dùng class này để build tilemap.
 */
public class StaticEntity  extends BaseEntity{
    public StaticEntity(double x, double y, Image spriteSheets,
        int numFrames, double renderWidth, double renderHeight) {
            super(x, y, spriteSheets, numFrames, renderWidth, renderHeight);
        }

    private int animTimer = 0;
    private int animDelay = 15; // Tốc độ chuyển frame (4 hình/giây cho animation môi trường)

    @Override
    public void update() {
        if (numFrames > 1) {
            animTimer++;
            if (animTimer >= animDelay) {
                animTimer = 0;
                frameIndex++;
                if (frameIndex >= numFrames) {
                    frameIndex = 0;
                }
            }
        }
    }
}
