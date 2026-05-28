package com.hust.game.ui;

import com.hust.game.combat.CombatManager;
import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class HUD {

    private Image avatarImg;
    private Image healthImg;
    private Image manaImg;
    private Image healthPotionSheet;
    private Image manaPotionSheet;
    private Image berserkIcon;
    private javafx.scene.text.Font pixelFont;

    private static final double FRAME_WIDTH = 720.0;
    private static final double FRAME_HEIGHT = 120.0;
    private static final double SCALE = 0.60; // Tỉ lệ 60%

    private static final double RENDER_WIDTH = FRAME_WIDTH * SCALE;
    private static final double RENDER_HEIGHT = FRAME_HEIGHT * SCALE;

    private static final int POTION_FRAMES = 8;
    private static final double POTION_ICON_SIZE = 32.0;
    private static final double POTION_TEXT_GAP = 6.0;
    private static final double POTION_GROUP_GAP = 20.0;
    private static final double POTION_OFFSET_X = -48.0;

    private final Player player;
    private final CombatManager combatManager;

    // ONE constructor that takes BOTH
    public HUD(Player player, CombatManager combatManager) {
        this.player = player;
        this.combatManager = combatManager;
        loadAssets();
    }

    private void loadAssets() {
        // Khắc phục lỗi NullPointerException do kích thước ảnh vượt giới hạn Texture của GPU (> 16384px).
        // Load ảnh và thu nhỏ trực tiếp xuống 17% ngay từ đầu để lưu vào RAM/VRAM.
        avatarImg = loadImg("/assets/avatar.png", RENDER_WIDTH, RENDER_HEIGHT);
        healthImg = loadImg("/assets/player_health.png", RENDER_WIDTH * 21, RENDER_HEIGHT);
        manaImg = loadImg("/assets/player_mana.png", RENDER_WIDTH * 6, RENDER_HEIGHT);
        healthPotionSheet = loadRawImg("/assets/items/health_potion.png");
        manaPotionSheet = loadRawImg("/assets/items/mana_potion.png");

        // Skill icon
        berserkIcon = new Image(
                getClass().getResourceAsStream("/assets/berserk.png"),
                32, 32, true, false
        );

    pixelFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 14);
    if (pixelFont == null) {
        pixelFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14);
    }
    }

    public void render(GraphicsContext gc) {
        double drawX = 2.0; // Sát lề trái 2px
        double drawY = 2.0; // Sát lề trên 2px

        gc.setGlobalAlpha(1.0);
        
        // Vẽ Avatar ở lớp dưới cùng
        gc.drawImage(avatarImg, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        gc.save();

        // HP
        int hpIndex = getFrameIndex(player.getCurrentHp(), player.getMaxHp());
        double hpSourceX = hpIndex * RENDER_WIDTH;
        gc.drawImage(healthImg, hpSourceX, 0, RENDER_WIDTH, RENDER_HEIGHT, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        // Mana
        int manaIndex = getManaFrameIndex(player.getCurrentMana(), player.getMaxMana());
        double manaSourceX = manaIndex * RENDER_WIDTH;
        gc.drawImage(manaImg, manaSourceX, 0, RENDER_WIDTH, RENDER_HEIGHT, drawX, drawY, RENDER_WIDTH, RENDER_HEIGHT);

        // Text hiển thị máu (HP) đè lên trên tất cả
        gc.setFont(pixelFont);
        gc.setFill(javafx.scene.paint.Color.RED);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);

        String hpText = player.getCurrentHp() + "  /  " + player.getMaxHp();
        double textX = drawX + RENDER_WIDTH - 30; // Dịch sang trái 10px
        double textY = drawY + (RENDER_HEIGHT / 2.0) - 3; // Cao hơn đường xích đạo 3px

        gc.setTextAlign(javafx.scene.text.TextAlignment.RIGHT); // Đảm bảo số luôn đổ về trái, thẳng hàng mép phải
        gc.strokeText(hpText, textX, textY);
        gc.fillText(hpText, textX, textY);

        renderPotionCounts(gc, drawX, drawY);

        gc.restore();

        gc.setGlobalAlpha(1.0);
        
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12)); // Chữ nhỏ lại một chút

        if (combatManager != null) {

            // Vị trí icon skill: dưới avatar, cách lề trái 5px
            double iconX = drawX + 5;
            double iconY = drawY + RENDER_HEIGHT; // Ngay dưới avatar
            double iconSize = 32.0; // Kích thước icon 32x32

            boolean active = combatManager.isSkillActive();
            boolean onCooldown = combatManager.getSkillCooldownRemaining() > 0;

            // 🎯 Set opacity
            if (active) {
                gc.setGlobalAlpha(1.0);
            } else if (onCooldown) {
                gc.setGlobalAlpha(0.4); // Mờ đi khi đang cooldown
            } else {
                gc.setGlobalAlpha(1.0); // Sẵn sàng
            }
            gc.drawImage(berserkIcon, iconX, iconY, iconSize, iconSize);

            gc.setGlobalAlpha(1.0);

            // Vị trí text cooldown: bên phải icon
            double cdTextX = iconX + iconSize + 5; // Cách icon 5px
            double cdTextY = iconY + iconSize / 2 + 4; // Căn giữa theo chiều dọc với icon

            // Timer display
            if (active) {
                int seconds = combatManager.getSkillDurationRemaining() / 60;
                gc.fillText(seconds + "s", cdTextX, cdTextY);
            } else if (onCooldown) {
                int seconds = combatManager.getSkillCooldownRemaining() / 60;
                gc.fillText(seconds + "s", cdTextX, cdTextY);
            }
        }

        renderMergeStatus(gc, drawX, drawY);
    }

    private void renderMergeStatus(GraphicsContext gc, double drawX, double drawY) {
        if (!player.isMergeActive() && !player.hasStoredMergeForm()) {
            return;
        }

        double textX = drawX + 5;
        double textY = drawY + RENDER_HEIGHT + 54;
        String text = player.isMergeActive()
                ? "TREE " + Math.max(1, (int) Math.ceil(player.getMergeDurationRemaining() / 60.0)) + "s"
                : "TREE READY [K]";

        gc.setGlobalAlpha(1.0);
        gc.setFont(pixelFont);
        gc.setFill(javafx.scene.paint.Color.LIGHTGREEN);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.strokeText(text, textX, textY);
        gc.fillText(text, textX, textY);
    }

    private int getFrameIndex(int current, int max) {
        if (current <= 0) return 20;
        if (current >= max) return 0;
        int index = 20 - (int) Math.ceil(((double) current / max) * 20.0);
        return Math.max(0, Math.min(20, index));
    }

    private int getManaFrameIndex(int current, int max) {
        if (current <= 0) return 5;
        if (current >= max) return 0;
        int index = 5 - (int) Math.ceil(((double) current / max) * 5.0);
        return Math.max(0, Math.min(5, index));
    }

    private void renderPotionCounts(GraphicsContext gc, double drawX, double drawY) {
        if (healthPotionSheet == null || manaPotionSheet == null) {
            return;
        }

        double manaBarRightX = drawX + RENDER_WIDTH + POTION_OFFSET_X;
        double iconY = drawY + (RENDER_HEIGHT / 2.0) + 6.0;

        double frameW = healthPotionSheet.getWidth() / POTION_FRAMES;
        double frameH = healthPotionSheet.getHeight();

        double healthIconX = manaBarRightX;
        gc.drawImage(healthPotionSheet, 0, 0, frameW, frameH,
                healthIconX, iconY, POTION_ICON_SIZE, POTION_ICON_SIZE);

        gc.setFont(pixelFont);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        String healthCount = "x" + player.getHealthPotionCount();
        double healthTextX = healthIconX + POTION_ICON_SIZE + POTION_TEXT_GAP;
        double textY = iconY + POTION_ICON_SIZE / 2.0;
        gc.strokeText(healthCount, healthTextX, textY);
        gc.fillText(healthCount, healthTextX, textY);

        double manaIconX = healthTextX + 24.0 + POTION_GROUP_GAP;
        double manaFrameW = manaPotionSheet.getWidth() / POTION_FRAMES;
        double manaFrameH = manaPotionSheet.getHeight();
        gc.drawImage(manaPotionSheet, 0, 0, manaFrameW, manaFrameH,
                manaIconX, iconY, POTION_ICON_SIZE, POTION_ICON_SIZE);

        String manaCount = "x" + player.getManaPotionCount();
        double manaTextX = manaIconX + POTION_ICON_SIZE + POTION_TEXT_GAP;
        gc.strokeText(manaCount, manaTextX, textY);
        gc.fillText(manaCount, manaTextX, textY);
    }

    private Image loadImg(String path, double requestedWidth, double requestedHeight) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, requestedWidth, requestedHeight, false, true); // true cho việc bật làm mịn ảnh (smooth scaling)
    }

    private Image loadRawImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream);
    }
}