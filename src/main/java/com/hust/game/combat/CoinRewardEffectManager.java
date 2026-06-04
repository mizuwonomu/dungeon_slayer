package com.hust.game.combat;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CoinRewardEffectManager {
    public enum RewardIcon {
        COIN,
        HEALTH_POTION,
        MANA_POTION
    }

    private static final int COIN_FRAMES = 6;
    private static final int POTION_FRAMES = 8;
    private static final int FRAME_DELAY = 10;
    private static final int EFFECT_LIFETIME = 60;
    private static final double RENDER_SIZE = 32.0;

    private static class CoinRewardEffect {
        double x;
        double y;
        RewardIcon icon;
        int timer = EFFECT_LIFETIME;
        int animTimer = 0;
        int frameIndex = 0;

        CoinRewardEffect(double x, double y, RewardIcon icon) {
            this.x = x;
            this.y = y;
            this.icon = icon;
        }
    }

    private final List<CoinRewardEffect> effects = new ArrayList<>();
    private final Image coinSprite;
    private final Image healthPotionSprite;
    private final Image manaPotionSprite;

    public CoinRewardEffectManager() {
        this.coinSprite = loadImage("/assets/items/coin.png");
        this.healthPotionSprite = loadImage("/assets/items/health_potion.png");
        this.manaPotionSprite = loadImage("/assets/items/mana_potion.png");
    }

    public void spawn(double x, double y) {
        spawn(x, y, RewardIcon.COIN);
    }

    public void spawn(double x, double y, RewardIcon icon) {
        effects.add(new CoinRewardEffect(x, y, icon));
    }

    public void update() {
        Iterator<CoinRewardEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            CoinRewardEffect effect = iterator.next();
            effect.timer--;
            effect.y -= 0.35;

            effect.animTimer++;
            if (effect.animTimer >= FRAME_DELAY) {
                effect.animTimer = 0;
                effect.frameIndex = (effect.frameIndex + 1) % getFrameCount(effect.icon);
            }

            if (effect.timer <= 0) {
                iterator.remove();
            }
        }
    }

    public void render(GraphicsContext gc) {
        for (CoinRewardEffect effect : effects) {
            Image sprite = getSprite(effect.icon);
            if (sprite == null) {
                continue;
            }

            int frameCount = getFrameCount(effect.icon);
            double frameWidth = sprite.getWidth() / frameCount;
            double frameHeight = sprite.getHeight();
            double alpha = Math.min(1.0, effect.timer / 20.0);
            gc.save();
            gc.setGlobalAlpha(alpha);
            gc.drawImage(
                    sprite,
                    effect.frameIndex * frameWidth, 0, frameWidth, frameHeight,
                    effect.x - RENDER_SIZE / 2.0, effect.y - RENDER_SIZE / 2.0,
                    RENDER_SIZE, RENDER_SIZE
            );
            gc.restore();
        }
    }

    private Image loadImage(String path) {
        var stream = getClass().getResourceAsStream(path);
        return stream != null ? new Image(stream, 0, 0, true, false) : null;
    }

    private Image getSprite(RewardIcon icon) {
        return switch (icon) {
            case HEALTH_POTION -> healthPotionSprite;
            case MANA_POTION -> manaPotionSprite;
            case COIN -> coinSprite;
        };
    }

    private int getFrameCount(RewardIcon icon) {
        return icon == RewardIcon.COIN ? COIN_FRAMES : POTION_FRAMES;
    }
}
