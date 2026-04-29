package com.hust.game.ui;

import com.hust.game.combat.CombatManager;
import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class HUD {

    private Image avatarImg;
    private Image healthImg;
    private Image manaImg;
    private Image berserkIcon;

    private static final double FRAME_WIDTH = 1152.0;
    private static final double FRAME_HEIGHT = 384.0;
    private static final double SCALE = 0.2; // Giảm thêm 20% so với mức 40% cũ

    private static final double RENDER_WIDTH = FRAME_WIDTH * SCALE;
    private static final double RENDER_HEIGHT = FRAME_HEIGHT * SCALE;

    private final Player player;
    private final CombatManager combatManager;

    // ONE constructor that takes BOTH
    public HUD(Player player, CombatManager combatManager) {
        this.player = player;
        this.combatManager = combatManager;
        loadAssets();
    }

    private void loadAssets() {
        avatarImg = loadImg("/assets/avatar.png");
        healthImg = loadImg("/assets/player_health.png");
        manaImg = loadImg("/assets/player_mana.png");

        // Skill icon
        berserkIcon = new Image(
                getClass().getResourceAsStream("/assets/berserk.png"),
                32, 32, true, false
        );
    }

    public void render(GraphicsContext gc) {
        double drawX = 2.0; // Sát lề trái 2px
        double drawY = 2.0; // Sát lề trên 2px

        gc.save();

        // Draw base UI
        gc.drawImage(avatarImg, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        // HP
        int hpIndex = getFrameIndex(player.getCurrentHp(), player.getMaxHp());
        double hpSourceX = hpIndex * FRAME_WIDTH;
        gc.drawImage(healthImg, hpSourceX, 0, FRAME_WIDTH, FRAME_HEIGHT, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        // Mana
        int manaIndex = getFrameIndex(player.getCurrentMana(), player.getMaxMana());
        double manaSourceX = manaIndex * FRAME_WIDTH;
        gc.drawImage(manaImg, manaSourceX, 0, FRAME_WIDTH, FRAME_HEIGHT, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        gc.restore();

        gc.setGlobalAlpha(1.0);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12)); // Chữ nhỏ lại một chút

        if (combatManager != null) {

            // Căn chỉnh vị trí icon Skill tỷ lệ thuận với khung Avatar mới
            double x = drawX + 336;
            double y = drawY + 6;

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
            gc.drawImage(berserkIcon, x, y, 48, 27); // Thu nhỏ icon tương ứng

            gc.setGlobalAlpha(1.0);

            // Timer display
            if (active) {
                int seconds = combatManager.getSkillDurationRemaining() / 60;
                gc.fillText("Time: " + seconds + "s", x, y + 40);
            } else if (onCooldown) {
                int seconds = combatManager.getSkillCooldownRemaining() / 60;
                gc.fillText("CD: " + seconds + "s", x, y + 40);
            }
        }
    }

    private int getFrameIndex(int current, int max) {
        if (current <= 0) return 10;
        if (current >= max) return 0;
        int index = 10 - (int) Math.ceil(((double) current / max) * 10.0);
        return Math.max(0, Math.min(10, index));
    }

    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}