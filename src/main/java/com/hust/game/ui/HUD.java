package com.hust.game.ui;

import com.hust.game.combat.CombatManager;
import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class HUD {

    private Image statusBar;
    private Image[] healthBars;
    private Image[] manaBars;
    private Image berserkIcon;

    private final Player player;
    private final CombatManager combatManager;

    // ONE constructor that takes BOTH
    public HUD(Player player, CombatManager combatManager) {
        this.player = player;
        this.combatManager = combatManager;
        loadAssets();
    }

    private void loadAssets() {
        statusBar = loadImg("/assets/avatar.png");

        // HP bars
        healthBars = new Image[11];
        for (int i = 0; i <= 10; i++) {
            int hp = i * 10;
            healthBars[i] = loadImg("/assets/health/health_" + hp + ".png");
        }

        // Mana bars
        manaBars = new Image[6];
        for (int i = 0; i <= 5; i++) {
            int mana = i * 10;
            manaBars[i] = loadImg("/assets/mana/mana_" + mana + ".png");
        }

        // Skill icon
        berserkIcon = new Image(
                getClass().getResourceAsStream("/assets/berserk.png"),
                32, 32, true, false
        );
    }

    public void render(GraphicsContext gc) {

        // Draw base UI
        gc.drawImage(statusBar, 0, 0, 300, 130);

        // HP
        int hpIndex = player.getCurrentHp() / 10;
        hpIndex = Math.max(0, Math.min(10, hpIndex));
        gc.drawImage(healthBars[hpIndex], 0, 1, 300, 125);

        // Mana
        int manaIndex = player.getCurrentMana() / 10;
        manaIndex = Math.max(0, Math.min(5, manaIndex));
        gc.drawImage(manaBars[manaIndex], 0, 1, 300, 125);

        gc.setGlobalAlpha(1.0);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));

        if (combatManager != null) {

            double x = 275;
            double y = 8;

            boolean active = combatManager.isSkillActive();
            boolean onCooldown = combatManager.getSkillCooldownRemaining() > 0;

            // 🎯 Set opacity
            if (active) {
                gc.setGlobalAlpha(1.0); // full brightness
            } else if (onCooldown) {
                gc.setGlobalAlpha(0.4); // faded when on cooldown
            } else {
                gc.setGlobalAlpha(1.0); // ready to use
            }
            gc.drawImage(berserkIcon, x, y, 60, 33.75);

            gc.setGlobalAlpha(1.0);

            // Timer display
            if (active) {
                int seconds = combatManager.getSkillDurationRemaining() / 60;
                gc.fillText("Time: " + seconds + "s", x, y + 50);
            } else if (onCooldown) {
                int seconds = combatManager.getSkillCooldownRemaining() / 60;
                gc.fillText("CD: " + seconds + "s", x, y + 50);
            }
        }
    }

    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}