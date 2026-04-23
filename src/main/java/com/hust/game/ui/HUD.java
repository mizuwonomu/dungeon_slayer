package com.hust.game.ui;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class HUD {

    private Image statusBar;
    private Image[] healthBars;
    private Image[] manaBars;

    private final Player player;

    public HUD(Player player) {
        this.player = player;
        loadAssets();
    }

    private void loadAssets() {
        statusBar = loadImg("/assets/avatar.png");

        healthBars = new Image[11];
        for (int i = 0; i <= 10; i++) {
            int hp = i * 10;
            healthBars[i] = loadImg("/assets/health/health_" + hp + ".png");
        }

        manaBars = new Image[6];
        for (int i = 0; i <= 5; i++) {
            int mana = i * 10;
            manaBars[i] = loadImg("/assets/mana/mana_" + mana + ".png");
        }
    }

    public void render(GraphicsContext gc) {

        gc.drawImage(statusBar, 0, 0, 300, 130);

        // ✅ HP from player
        int indexhealth = player.getCurrentHp() / 10;
        indexhealth = Math.max(0, Math.min(10, indexhealth));
        gc.drawImage(healthBars[indexhealth], 0, 1, 300, 125);

        // Mana (still local for now)
        int indexmana = player.getCurrentMana() / 10;
        indexmana = Math.max(0, Math.min(5, indexmana));
        gc.drawImage(manaBars[indexmana], 0, 1, 300, 125);
    }

    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}