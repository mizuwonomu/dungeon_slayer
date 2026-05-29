package com.hust.game.combat;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CoinRewardEffectManager {
    private static final int COIN_FRAMES = 6;
    private static final int FRAME_DELAY = 10;
    private static final int EFFECT_LIFETIME = 60;
    private static final double RENDER_SIZE = 32.0;

    private static class CoinRewardEffect {
        double x;
        double y;
        int timer = EFFECT_LIFETIME;
        int animTimer = 0;
        int frameIndex = 0;

        CoinRewardEffect(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private final List<CoinRewardEffect> effects = new ArrayList<>();
    private final Image coinSprite;

    public CoinRewardEffectManager() {
        var stream = getClass().getResourceAsStream("/assets/items/coin.png");
        this.coinSprite = stream != null ? new Image(stream, 0, 0, true, false) : null;
    }

    public void spawn(double x, double y) {
        effects.add(new CoinRewardEffect(x, y));
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
                effect.frameIndex = (effect.frameIndex + 1) % COIN_FRAMES;
            }

            if (effect.timer <= 0) {
                iterator.remove();
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (coinSprite == null) {
            return;
        }

        double frameWidth = coinSprite.getWidth() / COIN_FRAMES;
        double frameHeight = coinSprite.getHeight();

        for (CoinRewardEffect effect : effects) {
            double alpha = Math.min(1.0, effect.timer / 20.0);
            gc.save();
            gc.setGlobalAlpha(alpha);
            gc.drawImage(
                    coinSprite,
                    effect.frameIndex * frameWidth, 0, frameWidth, frameHeight,
                    effect.x - RENDER_SIZE / 2.0, effect.y - RENDER_SIZE / 2.0,
                    RENDER_SIZE, RENDER_SIZE
            );
            gc.restore();
        }
    }
}
