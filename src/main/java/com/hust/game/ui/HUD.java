package com.hust.game.ui;

import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.Enemy;
import com.hust.game.enemy.FinalBoss;
import com.hust.game.entities.ally.AllyManager;
import com.hust.game.entities.player.Player;
import java.util.List;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class HUD {

    private Image avatarImg;
    private Image healthImg;
    private Image manaImg;
    
    private Image moneyBoxImg;
    private Image healthManaBoxImg;
    private Image skillBerserkBoxImg;
    private Image skillNhaphonBoxImg;
    private Image cooldownBoxImg;

    // Boss UI
    private Image bossHpImg;
    private Image bossHpWhiteImg;
    private Image bossHpOutlineImg;
    private int bossHpAnimTimer = 0;
    private boolean hasStartedBossHpAnim = false;

    // Boss White HP Logic
    private int lastBossHp = -1;
    private double whiteBossHp = -1;
    private int whiteHpDelayTimer = 0;
    private double whiteHpDropRate = 0;

    private javafx.scene.text.Font pixelFont;
    private javafx.scene.text.Font bossFont;
    private javafx.scene.text.Font tooltipFont;

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
    private static final double HEALTH_MANA_BOX_WIDTH = 148.0;
    private static final double HEALTH_MANA_BOX_HEIGHT = 44.0;
    private static final double SKILL_BOX_SIZE = 50.0;
    private static final double SKILL_BOX_HEIGHT = 53.0;
    private static final double BOX_GAP = 4.0;
    private static final double TOOLTIP_OFFSET = 18.0;
    private static final double TOOLTIP_WIDTH = 480.0;
    private static final double TOOLTIP_PADDING_X = 18.0;
    private static final double TOOLTIP_PADDING_Y = 14.0;
    private static final double TOOLTIP_LINE_HEIGHT = 23.0;
    private static final double TOOLTIP_BORDER_WIDTH = 3.5;

    private record TooltipSegment(String text, Color color) {}
    private record TooltipLine(TooltipSegment... segments) {}

    private final Player player;
    private final CombatManager combatManager;
    private final List<Enemy> enemies;
    private final AllyManager allyManager;
    private boolean mouseInsideCanvas = false;
    private double mouseX = 0;
    private double mouseY = 0;

    // ONE constructor that takes BOTH
    public HUD(Player player, CombatManager combatManager, List<Enemy> enemies, AllyManager allyManager) {
        this.player = player;
        this.combatManager = combatManager;
        this.enemies = enemies;
        this.allyManager = allyManager;
        loadAssets();
    }

    public void setMousePosition(double mouseX, double mouseY, boolean insideCanvas) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseInsideCanvas = insideCanvas;
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

        try {
            bossHpImg = loadImg("/assets/boss_hp.png", 600.0, 20.0); // To x2
            bossHpWhiteImg = loadImg("/assets/boss_hp_white.png", 600.0, 20.0); // To x2
            bossHpOutlineImg = loadImg("/assets/boss_hp_outline.png", 600.0, 20.0); // To x2
        } catch (Exception e) {
            System.err.println("Could not load boss hp images: " + e.getMessage());
        }

        pixelFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 14);
        if (pixelFont == null) {
            pixelFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14);
        }
        
        bossFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 20);
        if (bossFont == null) {
            bossFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 20);
        }

        tooltipFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 18);
        if (tooltipFont == null) {
            tooltipFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 18);
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
        double clusterW = HEALTH_MANA_BOX_WIDTH + BOX_GAP + SKILL_BOX_SIZE + BOX_GAP + SKILL_BOX_SIZE; // 256
        double clusterX = (screenWidth - clusterW) / 2.0;
        double clusterMaxH = SKILL_BOX_HEIGHT;
        double clusterY = screenHeight - 20.0 - clusterMaxH;
        double equatorY = clusterY + clusterMaxH / 2.0;

        // 1. Health/Mana Box
        double hmBoxW = HEALTH_MANA_BOX_WIDTH;
        double hmBoxH = HEALTH_MANA_BOX_HEIGHT;
        double hmBoxX = clusterX;
        double hmBoxY = equatorY - hmBoxH / 2.0;
        Rectangle2D healthPotionHover = new Rectangle2D(hmBoxX, hmBoxY, hmBoxW / 2.0, hmBoxH);
        Rectangle2D manaPotionHover = new Rectangle2D(hmBoxX + hmBoxW / 2.0, hmBoxY, hmBoxW / 2.0, hmBoxH);
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
        double berserkBoxX = hmBoxX + hmBoxW + BOX_GAP;
        double berserkBoxY = equatorY - SKILL_BOX_HEIGHT / 2.0;
        Rectangle2D berserkHover = new Rectangle2D(berserkBoxX, berserkBoxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);

        gc.setGlobalAlpha(1.0);
        gc.drawImage(skillBerserkBoxImg, berserkBoxX, berserkBoxY);

        if (combatManager != null) {
            boolean active = combatManager.isSkillActive();
            boolean onCooldown = combatManager.getSkillCooldownRemaining() > 0;

            if (onCooldown) {
                double ratio = combatManager.getSkillCooldownRemaining() / 1800.0;
                double yOffset = (1.0 - ratio) * SKILL_BOX_HEIGHT;

                if (cooldownBoxImg != null) {
                    gc.drawImage(cooldownBoxImg, 0, yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset,
                            berserkBoxX, berserkBoxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
                } else {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
                    gc.fillRect(berserkBoxX, berserkBoxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
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
        double nhaphonBoxX = berserkBoxX + SKILL_BOX_SIZE + BOX_GAP;
        double nhaphonBoxY = equatorY - SKILL_BOX_HEIGHT / 2.0;
        Rectangle2D kSkillHover = new Rectangle2D(nhaphonBoxX, nhaphonBoxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);

        gc.setGlobalAlpha(1.0);
        gc.drawImage(skillNhaphonBoxImg, nhaphonBoxX, nhaphonBoxY);

        if (!renderSummonSkillBox(gc, nhaphonBoxX, nhaphonBoxY)) {
            boolean mergeActive = player.isMergeActive();
            boolean mergeReady = player.hasStoredMergeForm();
            boolean mergeCooldown = player.getMergeCooldownRemaining() > 0;

            if (mergeCooldown) {
                double ratio = player.getMergeCooldownRemaining() / 360.0;
                double yOffset = (1.0 - ratio) * SKILL_BOX_HEIGHT;

                if (cooldownBoxImg != null) {
                    gc.drawImage(cooldownBoxImg, 0, yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset,
                            nhaphonBoxX, nhaphonBoxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
                } else {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
                    gc.fillRect(nhaphonBoxX, nhaphonBoxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
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
                    gc.drawImage(cooldownBoxImg, 0, 0, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT,
                            nhaphonBoxX, nhaphonBoxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);
                } else {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.5));
                    gc.fillRect(nhaphonBoxX, nhaphonBoxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);
                }
            }
        }

        // ============================================
        // BOSS HEALTH BAR
        // ============================================
        FinalBoss boss = getLiveBoss();
        if (boss != null && boss.hasStartedCombat() && boss.getHp() > 0) {
            if (!hasStartedBossHpAnim) {
                hasStartedBossHpAnim = true;
                bossHpAnimTimer = 0;
                lastBossHp = boss.getHp();
                whiteBossHp = boss.getHp();
                whiteHpDelayTimer = 0;
                whiteHpDropRate = 0;
            }
            if (bossHpAnimTimer < 60) bossHpAnimTimer++;
            renderBossUI(gc, screenWidth, screenHeight, boss);
        } else if (boss == null || (boss != null && boss.getHp() <= 0)) {
            hasStartedBossHpAnim = false;
            bossHpAnimTimer = 0;
            lastBossHp = -1;
            whiteBossHp = -1;
            whiteHpDelayTimer = 0;
            whiteHpDropRate = 0;
        }

        TooltipLine[] tooltipLines = getHoveredTooltipLines(
                healthPotionHover, manaPotionHover, berserkHover, kSkillHover);
        if (tooltipLines != null) {
            renderTooltip(gc, tooltipLines, screenWidth, screenHeight);
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

    private boolean renderSummonSkillBox(GraphicsContext gc, double boxX, double boxY) {
        if (allyManager == null
                || (!allyManager.hasLiveBoss()
                && !allyManager.hasActiveMinion()
                && allyManager.getSummonCooldownRemaining() <= 0)) {
            return false;
        }

        int cooldown = allyManager.getSummonCooldownRemaining();
        if (cooldown > 0) {
            double ratio = cooldown / 1800.0;
            double yOffset = (1.0 - ratio) * SKILL_BOX_HEIGHT;

            if (cooldownBoxImg != null) {
                gc.drawImage(cooldownBoxImg, 0, yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset,
                        boxX, boxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
            } else {
                gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.75));
                gc.fillRect(boxX, boxY + yOffset, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT - yOffset);
            }

            drawSkillBoxText(gc, (int) Math.ceil(cooldown / 60.0) + "s", boxX, boxY);
        } else if (allyManager.canSummon()) {
            drawSkillBoxText(gc, "K", boxX, boxY);
        } else if (allyManager.hasActiveMinion()) {
            drawSkillBoxText(gc, "ON", boxX, boxY);
        } else {
            if (cooldownBoxImg != null) {
                gc.drawImage(cooldownBoxImg, 0, 0, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT,
                        boxX, boxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);
            } else {
                gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.5));
                gc.fillRect(boxX, boxY, SKILL_BOX_SIZE, SKILL_BOX_HEIGHT);
            }
        }

        return true;
    }

    private void drawSkillBoxText(GraphicsContext gc, String text, double boxX, double boxY) {
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1.5);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.strokeText(text, boxX + 25.0, boxY + 26.5);
        gc.fillText(text, boxX + 25.0, boxY + 26.5);
    }

    private TooltipLine[] getHoveredTooltipLines(Rectangle2D healthPotionHover, Rectangle2D manaPotionHover,
            Rectangle2D berserkHover, Rectangle2D kSkillHover) {
        if (!mouseInsideCanvas) {
            return null;
        }

        if (containsMouse(healthPotionHover)) {
            return new TooltipLine[]{
                    tooltipLine("Vật phẩm: Bình máu (Bấm E)"),
                    tooltipLine(
                            tooltipSegment("Hồi ", Color.WHITE),
                            tooltipSegment("20 HP", Color.RED),
                            tooltipSegment(".", Color.WHITE)),
                    tooltipLine("Số lượng hiện có: " + player.getHealthPotionCount())
            };
        }

        if (containsMouse(manaPotionHover)) {
            return new TooltipLine[]{
                    tooltipLine("Vật phẩm: Bình mana (Bấm Q)"),
                    tooltipLine(
                            tooltipSegment("Hồi ", Color.WHITE),
                            tooltipSegment("10 mana", Color.DODGERBLUE),
                            tooltipSegment(".", Color.WHITE)),
                    tooltipLine("Số lượng hiện có: " + player.getManaPotionCount())
            };
        }

        if (containsMouse(berserkHover)) {
            return new TooltipLine[]{
                    tooltipLine("Skill: Cuồng nộ (Bấm L)"),
                    tooltipLine("Tăng sát thương đòn đánh thường"),
                    tooltipLine("trong thời gian ngắn."),
                    tooltipLine("Thời gian hiệu lực: 10s"),
                    tooltipLine("Hồi chiêu: 30s"),
                    tooltipLine("Mana: 10")
            };
        }

        if (containsMouse(kSkillHover)) {
            if (allyManager != null && (allyManager.hasLiveBoss()
                    || allyManager.hasActiveMinion()
                    || allyManager.getSummonCooldownRemaining() > 0)) {
                return new TooltipLine[]{
                        tooltipLine("Skill: Companion (Bấm K)"),
                        tooltipLine("Triệu hồi đồng minh hỗ trợ"),
                        tooltipLine("tấn công boss."),
                        tooltipLine("Chỉ dùng được trong boss level."),
                        tooltipLine("Hồi chiêu: 30s"),
                        tooltipLine("Mana: 20")
                };
            }

            return new TooltipLine[]{
                    tooltipLine("Skill: Nhập hồn (Bấm K)"),
                    tooltipLine("Khi hạ gục quái vật, bạn có thể"),
                    tooltipLine("nhập hồn để trở thành quái đó"),
                    tooltipLine("trong 1 khoảng thời gian."),
                    tooltipLine("Đặc tính:"),
                    tooltipLine("- Tree: giảm tốc đánh nhưng tăng mạnh phòng thủ"),
                    tooltipLine("Thời gian hiệu lực: 5s"),
                    tooltipLine("Hồi chiêu: 6s"),
                    tooltipLine("Mana: 20")
            };
        }

        return null;
    }

    private TooltipLine tooltipLine(String text) {
        return new TooltipLine(tooltipSegment(text, Color.WHITE));
    }

    private TooltipLine tooltipLine(TooltipSegment... segments) {
        return new TooltipLine(segments);
    }

    private TooltipSegment tooltipSegment(String text, Color color) {
        return new TooltipSegment(text, color);
    }

    private boolean containsMouse(Rectangle2D area) {
        return area.contains(mouseX, mouseY);
    }

    private void renderTooltip(GraphicsContext gc, TooltipLine[] lines, double screenWidth, double screenHeight) {
        gc.save();
        gc.setFont(tooltipFont);

        double width = TOOLTIP_WIDTH;
        double height = lines.length * TOOLTIP_LINE_HEIGHT + TOOLTIP_PADDING_Y * 2;
        double x = mouseX + TOOLTIP_OFFSET;
        double y = mouseY + TOOLTIP_OFFSET;

        if (x + width > screenWidth - 8.0) {
            x = mouseX - width - TOOLTIP_OFFSET;
        }
        if (y + height > screenHeight - 8.0) {
            y = mouseY - height - TOOLTIP_OFFSET;
        }
        x = Math.max(8.0, Math.min(x, screenWidth - width - 8.0));
        y = Math.max(8.0, Math.min(y, screenHeight - height - 8.0));

        gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.78));
        gc.fillRoundRect(x, y, width, height, 12, 12);
        gc.setStroke(javafx.scene.paint.Color.WHITE);
        gc.setLineWidth(TOOLTIP_BORDER_WIDTH);
        gc.strokeRoundRect(x, y, width, height, 12, 12);

        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.TOP);
        double textX = x + TOOLTIP_PADDING_X;
        double textY = y + TOOLTIP_PADDING_Y;
        for (TooltipLine line : lines) {
            drawTooltipText(gc, line, textX, textY);
            textY += TOOLTIP_LINE_HEIGHT;
        }
        gc.restore();
    }

    private void drawTooltipText(GraphicsContext gc, TooltipLine line, double x, double y) {
        double currentX = x;
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2.0);

        for (TooltipSegment segment : line.segments()) {
            gc.setFill(segment.color());
            gc.strokeText(segment.text(), currentX, y);
            gc.fillText(segment.text(), currentX, y);
            currentX += measureTooltipText(segment.text());
        }
    }

    private double measureTooltipText(String text) {
        Text helper = new Text(text);
        helper.setFont(tooltipFont);
        return helper.getLayoutBounds().getWidth();
    }

    private void renderBossUI(GraphicsContext gc, double screenWidth, double screenHeight, FinalBoss boss) {
        if (bossHpImg == null || bossHpOutlineImg == null) return;

        // --- Cập nhật logic thanh máu trắng (White HP) ---
        if (lastBossHp != -1 && boss.getHp() < lastBossHp) {
            lastBossHp = boss.getHp();
            whiteHpDelayTimer = 18; // Delay 0.3s (18 frames ở 60 FPS)
            whiteHpDropRate = (whiteBossHp - boss.getHp()) / 18.0; // Tốc độ giảm để hoàn thành trong 0.3s
        } else if (lastBossHp != -1 && boss.getHp() > lastBossHp) {
            lastBossHp = boss.getHp();
            whiteBossHp = boss.getHp(); // Nếu Boss hồi máu thì cập nhật ngay
        }

        if (whiteHpDelayTimer > 0) {
            whiteHpDelayTimer--;
        } else {
            if (whiteBossHp > boss.getHp()) {
                whiteBossHp -= whiteHpDropRate;
                if (whiteBossHp < boss.getHp()) {
                    whiteBossHp = boss.getHp();
                }
            } else {
                whiteBossHp = boss.getHp();
            }
        }
        // -------------------------------------------------

        double t = bossHpAnimTimer / 60.0;
        // Ease out cubic: Nhanh ban đầu, chậm dần về cuối trong 1s
        double progress = 1.0 - Math.pow(1.0 - t, 3);
        
        double fullWidth = 600.0;
        double barHeight = 20.0;
        double currentWidth = fullWidth * progress;
        
        double centerX = screenWidth / 2.0;
        
        // Đưa xuống dưới màn hình, nằm ngay phía trên cụm Skill Box (cách 30px)
        double clusterY = screenHeight - 20.0 - 53.0; // Tọa độ Y của hộp kỹ năng
        double topY = clusterY - 30.0 - barHeight; 
        
        gc.save();
        
        // 1. Vẽ Tên Boss
        gc.setFont(bossFont);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.BOTTOM);
        gc.setGlobalAlpha(progress); // Mờ dần đồng bộ với thanh máu
        
        // Vẽ viền đen cho tên Boss
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(3.0);
        gc.strokeText("Kẻ lang bạt mất đi linh hồn", centerX, topY - 5);
        gc.fillText("Kẻ lang bạt mất đi linh hồn", centerX, topY - 5); // Tách tên cách thanh máu 5px
        
        gc.setGlobalAlpha(1.0);
        
        // 2. Vẽ Nền thanh máu (Kéo dài từ giữa ra 2 bên)
        double sx = (fullWidth / 2.0) - (currentWidth / 2.0); // Tọa độ X để cắt ảnh gốc
        double dx = centerX - (currentWidth / 2.0); // Tọa độ X để vẽ lên màn hình
        if (currentWidth > 0 && barHeight > 0) {
            gc.drawImage(bossHpOutlineImg, sx, 0, currentWidth, barHeight, dx, topY, currentWidth, barHeight);
        }

        // 3. Vẽ Máu Boss Trắng (Nằm dưới máu đỏ, trên nền outline)
        if (bossHpWhiteImg != null) {
            double whiteHpPercent = whiteBossHp / boss.getMaxHp();
            double whiteFillWidth = fullWidth * whiteHpPercent;

            double drawWhiteLeft = Math.max(sx, 0);
            double drawWhiteRight = Math.min(sx + currentWidth, whiteFillWidth);

            if (drawWhiteLeft < drawWhiteRight) {
                double sw = drawWhiteRight - drawWhiteLeft;
                if (sw > 0 && barHeight > 0) {
                    double destX = centerX - (fullWidth / 2.0) + drawWhiteLeft;
                    gc.drawImage(bossHpWhiteImg, drawWhiteLeft, 0, sw, barHeight, destX, topY, sw, barHeight);
                }
            }
        }
        
        // 4. Vẽ Máu Boss thực tế
        double hpPercent = (double) boss.getHp() / boss.getMaxHp();
        double fillWidth = fullWidth * hpPercent; // Máu cắt từ phải sang trái (Chỉ lấy phần từ 0 đến fillWidth)
        
        // Chỉ vẽ phần máu NẰM TRONG vùng animation đang được mở ra
        double drawLeft = Math.max(sx, 0); // Giới hạn bên trái
        double drawRight = Math.min(sx + currentWidth, fillWidth); // Giới hạn bên phải
        
        if (drawLeft < drawRight) {
            double sw = drawRight - drawLeft;
            if (sw > 0 && barHeight > 0) {
                double destX = centerX - (fullWidth / 2.0) + drawLeft;
                gc.drawImage(bossHpImg, drawLeft, 0, sw, barHeight, destX, topY, sw, barHeight);
            }
        }
        
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
