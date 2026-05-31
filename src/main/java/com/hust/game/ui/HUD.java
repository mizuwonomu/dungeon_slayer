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
    private Image potionBoxImg;
    private Image berserkIcon;
    private Image coinSheet;
    private javafx.scene.text.Font pixelFont;

    private static final double FRAME_WIDTH = 720.0;
    private static final double FRAME_HEIGHT = 120.0;
    private static final double SCALE = 0.60; // Tỉ lệ 60%

    private static final double RENDER_WIDTH = FRAME_WIDTH * SCALE;
    private static final double RENDER_HEIGHT = FRAME_HEIGHT * SCALE;

    private static final int POTION_FRAMES = 8;
    private static final double POTION_PANEL_X = 6.0;
    private static final double POTION_PANEL_Y_OFFSET = 78.0;
    private static final double POTION_PANEL_WIDTH = 58.0;
    private static final double POTION_PANEL_HEIGHT = 116.0;
    private static final double POTION_SLOT_HEIGHT = POTION_PANEL_HEIGHT / 2.0;
    private static final double POTION_ICON_SIZE = 30.0;

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
        potionBoxImg = loadRawImg("/assets/cmt_box.png");
        coinSheet = loadRawImg("/assets/items/coin.png");

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

        renderCoins(gc, drawX, drawY);

        gc.restore();

        gc.setGlobalAlpha(1.0);
        
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12)); // Chữ nhỏ lại một chút

        if (combatManager != null) {

            // Vị trí icon skill: dưới avatar, cách lề trái 5px
            double iconX = drawX + POTION_PANEL_WIDTH + 16;
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

        renderPotionPanel(gc, drawX, drawY);
        renderMergeStatus(gc, drawX, drawY);
    }

    private void renderMergeStatus(GraphicsContext gc, double drawX, double drawY) {
        if (!player.isMergeActive() && !player.hasStoredMergeForm()) {
            return;
        }

        double textX = drawX + POTION_PANEL_WIDTH + 16;
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

    private void renderPotionPanel(GraphicsContext gc, double drawX, double drawY) {
        if (healthPotionSheet == null || manaPotionSheet == null) {
            return;
        }

        double panelX = drawX + POTION_PANEL_X;
        double panelY = drawY + POTION_PANEL_Y_OFFSET;

        gc.save();
        if (potionBoxImg != null) {
            gc.drawImage(potionBoxImg, panelX, panelY, POTION_PANEL_WIDTH, POTION_PANEL_HEIGHT);
        } else {
            gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
            gc.fillRoundRect(panelX, panelY, POTION_PANEL_WIDTH, POTION_PANEL_HEIGHT, 10, 10);
            gc.setStroke(javafx.scene.paint.Color.WHITE);
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(panelX, panelY, POTION_PANEL_WIDTH, POTION_PANEL_HEIGHT, 10, 10);
        }

        double separatorY = panelY + POTION_SLOT_HEIGHT;
        gc.setStroke(javafx.scene.paint.Color.rgb(235, 220, 170, 0.75));
        gc.setLineWidth(1.5);
        gc.strokeLine(panelX + 7.0, separatorY, panelX + POTION_PANEL_WIDTH - 7.0, separatorY);

        renderPotionSlot(gc, healthPotionSheet, player.getHealthPotionCount(), panelX, panelY);
        renderPotionSlot(gc, manaPotionSheet, player.getManaPotionCount(), panelX, panelY + POTION_SLOT_HEIGHT);
        gc.restore();
    }

    private void renderPotionSlot(GraphicsContext gc, Image sheet, int count, double slotX, double slotY) {
        double frameW = sheet.getWidth() / POTION_FRAMES;
        double frameH = sheet.getHeight();
        double iconX = slotX + (POTION_PANEL_WIDTH - POTION_ICON_SIZE) / 2.0;
        double iconY = slotY + 9.0;

        if (count <= 0) {
            gc.setGlobalAlpha(0.45);
        }
        gc.drawImage(sheet, 0, 0, frameW, frameH, iconX, iconY, POTION_ICON_SIZE, POTION_ICON_SIZE);
        gc.setGlobalAlpha(1.0);

        gc.setFont(pixelFont);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        String countText = "x" + count;
        double textX = slotX + POTION_PANEL_WIDTH / 2.0;
        double textY = slotY + POTION_SLOT_HEIGHT - 12.0;
        gc.strokeText(countText, textX, textY);
        gc.fillText(countText, textX, textY);
    }

    private void renderCoins(GraphicsContext gc, double drawX, double drawY) {
        if (coinSheet == null) {
            return;
        }

        double frameW = coinSheet.getWidth() / 6.0;
        double frameH = coinSheet.getHeight();
        double iconSize = 30.0;
        double iconX = drawX + RENDER_WIDTH - 118.0;
        double iconY = drawY + RENDER_HEIGHT + 4.0;

        gc.drawImage(coinSheet, 0, 0, frameW, frameH, iconX, iconY, iconSize, iconSize);

        gc.setFont(pixelFont);
        gc.setFill(javafx.scene.paint.Color.GOLD);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        String coinText = "x" + player.getCoins();
        double textX = iconX + iconSize + 6.0;
        double textY = iconY + iconSize / 2.0;
        gc.strokeText(coinText, textX, textY);
        gc.fillText(coinText, textX, textY);
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
