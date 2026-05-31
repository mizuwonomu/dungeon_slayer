package com.hust.game.ui;

import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.Enemy;
import com.hust.game.enemy.FinalBoss;
import com.hust.game.entities.player.Player;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class HUD {

    private Image avatarImg;
    private Image healthImg;
    private Image manaImg;
    
    private Image moneyBoxImg;
    private Image healthManaBoxImg;
    private Image skillBerserkBoxImg;
    private Image skillNhaphonBoxImg;
    private Image cooldownBoxImg;

    private javafx.scene.text.Font pixelFont;

    private static final double FRAME_WIDTH = 720.0;
    private static final double FRAME_HEIGHT = 120.0;
    private static final double SCALE = 0.60; // Tỉ lệ 60%

    private static final double RENDER_WIDTH = FRAME_WIDTH * SCALE;
    private static final double RENDER_HEIGHT = FRAME_HEIGHT * SCALE;
    private static final double BOSS_BAR_WIDTH = 720.0;
    private static final double BOSS_BAR_HEIGHT = 48.0;
    private static final double BOSS_BAR_GAP = 12.0;

    private final Player player;
    private final CombatManager combatManager;
    private final List<Enemy> enemies;

    // ONE constructor that takes BOTH
    public HUD(Player player, CombatManager combatManager, List<Enemy> enemies) {
        this.player = player;
        this.combatManager = combatManager;
        this.enemies = enemies;
        loadAssets();
    }

    private void loadAssets() {
        avatarImg = loadImg("/assets/avatar.png", RENDER_WIDTH, RENDER_HEIGHT);
        healthImg = loadImg("/assets/player_health.png", RENDER_WIDTH * 21, RENDER_HEIGHT);
        manaImg = loadImg("/assets/player_mana.png", RENDER_WIDTH * 6, RENDER_HEIGHT);

        moneyBoxImg = loadImg("/assets/money_box.png", 149.0 * SCALE, 44.0 * SCALE);
        healthManaBoxImg = loadImg("/assets/health_mana_box.png", 148.0, 44.0);
        skillBerserkBoxImg = loadImg("/assets/skill_berserk_box.png", 50.0, 53.0);
        skillNhaphonBoxImg = loadImg("/assets/skill_nhaphon_box.png", 50.0, 53.0);

        try {
            cooldownBoxImg = loadImg("/assets/cooldown_box.png", 50.0, 53.0);
        } catch (Exception e) {
            cooldownBoxImg = null;
            System.err.println("Could not load cooldown_box.png: " + e.getMessage());
        }

        pixelFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 14);
        if (pixelFont == null) {
            pixelFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14);
        }
    }

    public void render(GraphicsContext gc) {
        double screenWidth = gc.getCanvas().getWidth();
        double screenHeight = gc.getCanvas().getHeight();

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
        
        // ============================================
        // MONEY BOX
        // ============================================
        double scaledMoneyW = 149.0 * SCALE;
        double scaledMoneyH = 44.0 * SCALE;
        double moneyBoxX = 24.0; // Căn lề trái sâu hơn 20px so với 4px cũ
        double moneyBoxY = drawY + RENDER_HEIGHT + 4.0;
        gc.drawImage(moneyBoxImg, moneyBoxX, moneyBoxY);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18));
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(2.5);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        
        String coinText = String.valueOf(player.getCoins());
        double moneyTextX = moneyBoxX + (scaledMoneyW * 2.0 / 3.0);
        double moneyTextY = moneyBoxY + (scaledMoneyH / 2.0);
        gc.strokeText(coinText, moneyTextX, moneyTextY);
        gc.fillText(coinText, moneyTextX, moneyTextY);

        gc.restore();
        gc.setGlobalAlpha(1.0);

        // ============================================
        // 3-BOX CLUSTER (Health/Mana, Berserk, Nhaphon)
        // ============================================
        double clusterW = 148.0 + 4.0 + 50.0 + 4.0 + 50.0; // 256
        double clusterX = (screenWidth - clusterW) / 2.0;
        double clusterMaxH = 53.0;
        double clusterY = screenHeight - 20.0 - clusterMaxH;
        double equatorY = clusterY + clusterMaxH / 2.0;

        renderBossHealthBar(gc, screenWidth, clusterY);

        // 1. Health/Mana Box
        double hmBoxW = 148.0;
        double hmBoxH = 44.0;
        double hmBoxX = clusterX;
        double hmBoxY = equatorY - hmBoxH / 2.0;
        gc.drawImage(healthManaBoxImg, hmBoxX, hmBoxY);

        gc.setFont(pixelFont);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // Left half (Health Potion)
        gc.setFill(javafx.scene.paint.Color.WHITE);
        String hpPotionText = String.valueOf(player.getHealthPotionCount());
        double hpPotionX = hmBoxX + 49.33; // hmBoxX + 74/3 + (74*2/3)/2
        gc.strokeText(hpPotionText, hpPotionX, hmBoxY + hmBoxH / 2.0);
        gc.fillText(hpPotionText, hpPotionX, hmBoxY + hmBoxH / 2.0);

        // Right half (Mana Potion)
        gc.setFill(javafx.scene.paint.Color.WHITE);
        String manaPotionText = String.valueOf(player.getManaPotionCount());
        double manaPotionX = hmBoxX + 74.0 + 49.33;
        gc.strokeText(manaPotionText, manaPotionX, hmBoxY + hmBoxH / 2.0);
        gc.fillText(manaPotionText, manaPotionX, hmBoxY + hmBoxH / 2.0);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 2. Berserk Skill Box
        double berserkBoxX = hmBoxX + hmBoxW + 4.0;
        double berserkBoxY = equatorY - 53.0 / 2.0;

        gc.setGlobalAlpha(1.0);
        gc.drawImage(skillBerserkBoxImg, berserkBoxX, berserkBoxY);

        if (combatManager != null) {
            boolean active = combatManager.isSkillActive();
            boolean onCooldown = combatManager.getSkillCooldownRemaining() > 0;

            if (onCooldown) {
                double ratio = combatManager.getSkillCooldownRemaining() / 1800.0;
                double yOffset = (1.0 - ratio) * 53.0;

                if (cooldownBoxImg != null) {
                    gc.drawImage(cooldownBoxImg, 0, yOffset, 50.0, 53.0 - yOffset, berserkBoxX, berserkBoxY + yOffset, 50.0, 53.0 - yOffset);
                } else {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
                    gc.fillRect(berserkBoxX, berserkBoxY + yOffset, 50.0, 53.0 - yOffset);
                }

                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.setStroke(javafx.scene.paint.Color.BLACK);
                gc.setLineWidth(1.5);
                gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                int seconds = (int) Math.ceil(combatManager.getSkillCooldownRemaining() / 60.0);
                gc.strokeText(seconds + "s", berserkBoxX + 25.0, berserkBoxY + 26.5);
                gc.fillText(seconds + "s", berserkBoxX + 25.0, berserkBoxY + 26.5);
            } else if (active) {
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.setStroke(javafx.scene.paint.Color.BLACK);
                gc.setLineWidth(1.5);
                gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                int seconds = (int) Math.ceil(combatManager.getSkillDurationRemaining() / 60.0);
                gc.strokeText(seconds + "s", berserkBoxX + 25.0, berserkBoxY + 26.5);
                gc.fillText(seconds + "s", berserkBoxX + 25.0, berserkBoxY + 26.5);
            }
        }

        // 3. Nhaphon (Merge) Skill Box
        double nhaphonBoxX = berserkBoxX + 50.0 + 4.0;
        double nhaphonBoxY = equatorY - 53.0 / 2.0;

        gc.setGlobalAlpha(1.0);
        gc.drawImage(skillNhaphonBoxImg, nhaphonBoxX, nhaphonBoxY);

        boolean mergeActive = player.isMergeActive();
        boolean mergeReady = player.hasStoredMergeForm();
        boolean mergeCooldown = player.getMergeCooldownRemaining() > 0;

        if (mergeCooldown) {
            double ratio = player.getMergeCooldownRemaining() / 360.0;
            double yOffset = (1.0 - ratio) * 53.0;

            if (cooldownBoxImg != null) {
                gc.drawImage(cooldownBoxImg, 0, yOffset, 50.0, 53.0 - yOffset, nhaphonBoxX, nhaphonBoxY + yOffset, 50.0, 53.0 - yOffset);
            } else {
                gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
                gc.fillRect(nhaphonBoxX, nhaphonBoxY + yOffset, 50.0, 53.0 - yOffset);
            }

            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setStroke(javafx.scene.paint.Color.BLACK);
            gc.setLineWidth(1.5);
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            int seconds = (int) Math.ceil(player.getMergeCooldownRemaining() / 60.0);
            gc.strokeText(seconds + "s", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
            gc.fillText(seconds + "s", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
        } else if (mergeActive) {
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setStroke(javafx.scene.paint.Color.BLACK);
            gc.setLineWidth(1.5);
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            int seconds = (int) Math.ceil(player.getMergeDurationRemaining() / 60.0);
            gc.strokeText(seconds + "s", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
            gc.fillText(seconds + "s", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
        } else if (mergeReady) {
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setStroke(javafx.scene.paint.Color.BLACK);
            gc.setLineWidth(1.5);
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.strokeText("K", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
            gc.fillText("K", nhaphonBoxX + 25.0, nhaphonBoxY + 26.5);
        } else {
            if (cooldownBoxImg != null) {
                gc.drawImage(cooldownBoxImg, 0, 0, 50.0, 53.0, nhaphonBoxX, nhaphonBoxY, 50.0, 53.0);
            } else {
                gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.5));
                gc.fillRect(nhaphonBoxX, nhaphonBoxY, 50.0, 53.0);
            }
        }
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

    private void renderBossHealthBar(GraphicsContext gc, double screenWidth, double clusterY) {
        FinalBoss boss = getLiveBoss();
        if (boss == null || healthImg == null) {
            return;
        }

        int maxHp = boss.getMaxHp();
        if (maxHp <= 0) {
            return;
        }

        int hp = boss.getHp();
        int hpIndex = getFrameIndex(hp, maxHp);
        double hpSourceX = hpIndex * RENDER_WIDTH;
        double barX = (screenWidth - BOSS_BAR_WIDTH) / 2.0;
        double barY = clusterY - BOSS_BAR_GAP - BOSS_BAR_HEIGHT;

        gc.save();
        gc.drawImage(healthImg,
                hpSourceX, 0, RENDER_WIDTH, RENDER_HEIGHT,
                barX, barY, BOSS_BAR_WIDTH, BOSS_BAR_HEIGHT);

        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18));
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(2.0);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        String bossHpText = "BOSS  " + hp + " / " + maxHp;
        double textX = barX + BOSS_BAR_WIDTH / 2.0;
        double textY = barY + BOSS_BAR_HEIGHT / 2.0;
        gc.strokeText(bossHpText, textX, textY);
        gc.fillText(bossHpText, textX, textY);
        gc.restore();
    }

    private FinalBoss getLiveBoss() {
        if (enemies == null) {
            return null;
        }

        for (Enemy enemy : enemies) {
            if (enemy instanceof FinalBoss boss && boss.getHp() > 0) {
                return boss;
            }
        }
        return null;
    }

    private Image loadImg(String path, double requestedWidth, double requestedHeight) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, requestedWidth, requestedHeight, false, true); // true cho việc bật làm mịn ảnh (smooth scaling)
    }
}
