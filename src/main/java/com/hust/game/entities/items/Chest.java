package com.hust.game.entities.items;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.StaticEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Chest extends StaticEntity {
    private static final int FRAME_COUNT = 4;
    private static final int OPEN_ANIM_DELAY = 8;
    private static final int FADE_FRAMES = 30;

    private boolean opened = false;
    private boolean opening = false;
    private boolean fading = false;
    private int animTimer = 0;
    private int fadeTimer = FADE_FRAMES;

    public Chest(double x, double y, Image spriteSheet) {
        super(x, y, spriteSheet, FRAME_COUNT, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
    }

    public boolean open() {
        if (opened || opening) {
            return false;
        }

        opening = true;
        frameIndex = 0;
        animTimer = 0;
        fadeTimer = FADE_FRAMES;
        return true;
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean isSolid() {
        return !opening && !opened && !fading;
    }

    public boolean isReadyToRemove() {
        return fading && fadeTimer <= 0;
    }

    @Override
    public void update() {
        if (fading) {
            fadeTimer--;
            return;
        }

        if (!opening) {
            return;
        }

        animTimer++;
        if (animTimer < OPEN_ANIM_DELAY) {
            return;
        }

        animTimer = 0;
        frameIndex++;
        if (frameIndex >= FRAME_COUNT - 1) {
            frameIndex = FRAME_COUNT - 1;
            opening = false;
            opened = true;
            fading = true;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (!fading) {
            super.render(gc);
            return;
        }

        double alpha = Math.max(0.0, fadeTimer / (double) FADE_FRAMES);
        gc.save();
        gc.setGlobalAlpha(alpha);
        super.render(gc);
        gc.restore();
    }
}
