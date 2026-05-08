package com.hust.game.combat;

import com.hust.game.enemy.Enemy;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.player.PlayerCombat;
import javafx.geometry.Rectangle2D;

import java.util.List;
import java.util.ArrayList;

/**
 * CombatManager - trung gian xử lý combat giữa Player và Enemy.
 *
 * Trách nhiệm:
 *   1. Khi player chém → lấy hitbox → kiểm tra enemy nào bị dính
 *   2. Gây damage cho enemy bị dính đòn
 *   3. Xử lý skill cuồng nộ của player
 *
 * Lý do cần class này:
 *   Player không nên biết danh sách enemy (coupling cao).
 *   Enemy tự xử lý chiều nó tấn công player (như Slime.update()).
 *   CombatManager đứng giữa để xử lý chiều player → enemy.
 */
public class CombatManager {

    // Tham chiếu đến player và danh sách enemy
    private final Player player;
    private final List<Enemy> enemyList;

    // -------------------------------------------------------
    // SKILL: CUỒNG NỘ
    // Khi kích hoạt: rút 10 máu, nhân đôi damage trong 10 giây
    // Cooldown: 30 giây = 1800 frame (60fps x 30)
    // -------------------------------------------------------
    private boolean skillActive    = false;   // skill có đang bật không
    private int skillDuration      = 0;       // đếm ngược thời gian skill còn lại (frame)
    private int skillCooldown      = 0;       // đếm ngược cooldown (frame)

    private static final int SKILL_DURATION_FRAMES  = 600;  // 10 giây x 60fps
    private static final int SKILL_COOLDOWN_FRAMES  = 1800; // 30 giây x 60fps
    private static final int SKILL_HP_COST          = 10;   // máu bị rút khi dùng skill
    private static final int SKILL_MANA_COST        = 10;
    private static final int SKILL_DAMAGE_MULTIPLIER = 2;   // nhân đôi damage

    // -------------------------------------------------------
    // COMBO SYSTEM
    // -------------------------------------------------------
    private int comboCount = 0;
    private int comboTimer = 0;
    private static final int COMBO_WINDOW_FRAMES = 60; // 1 giây (60 fps)

    // -------------------------------------------------------
    // CRIT TEXT SYSTEM (Hiệu ứng sát thương chí mạng)
    // -------------------------------------------------------
    private class CritText {
        double x, y;
        String text;
        javafx.scene.paint.Color color;
        double sizeMultiplier;
        int timer = 60; // Tồn tại 1 giây

        public CritText(double x, double y, String text, javafx.scene.paint.Color color, double sizeMultiplier) {
            this.x = x; this.y = y; this.text = text; this.color = color; this.sizeMultiplier = sizeMultiplier;
        }
    }
    private List<CritText> critTexts = new ArrayList<>();

    private ParticleManager particleManager = new ParticleManager();
    private int hitStopFrames = 0;
    private int shakeTimer = 0;
    private double shakeAmplitude = 0;
    private boolean hasDealtDamageThisAttack = true;
    private boolean hasIncreasedComboThisAttack = false;

    public CombatManager(Player player, List<Enemy> enemyList) {
        this.player    = player;
        this.enemyList = enemyList;
    }

    /**
     * update() — gọi mỗi frame từ App.java.
     * Đếm ngược duration và cooldown của skill.
     */
    public void update() {
        // Nếu skill đang chạy → đếm ngược duration
        if (skillActive) {
            skillDuration--;

            // Hết thời gian skill → tắt skill, bắt đầu cooldown
            if (skillDuration <= 0) {
                skillActive   = false;
                skillCooldown = SKILL_COOLDOWN_FRAMES;
                player.setRageMode(false, 0); // về sprite bình thường
            }
        }

        // Đếm ngược cooldown
        if (skillCooldown > 0) {
            skillCooldown--;
        }

        // Đếm ngược Combo
        if (comboTimer > 0) {
            comboTimer--;
            if (comboTimer <= 0) {
                comboCount = 0; // Hết 1 giây không chém trúng ai -> Reset Combo
            }
        }
        
        particleManager.update();

        // Kiểm tra xem Player có đang chém và hoạt ảnh đã đến frame thứ 4 (index 3) chưa
        if (player.isAttacking() && player.getFrameIndex() == 3 && !hasDealtDamageThisAttack) {
            processAttackHits(player.getAttackDamage(), false, true);
            hasDealtDamageThisAttack = true;
        } else if (player.isThrusting()) {
            int t = player.getThrustTimer();
            // Tung 3 phát chọc ở các frame 15, 9, 3 (ngay khoảnh khắc lưỡi kiếm đâm ra xa nhất)
            if (t == 15 || t == 9 || t == 3) {
                processAttackHits(10, true, t == 3); // Mỗi nhát chọc cơ bản 10 sát thương, chỉ knockback ở đòn cuối
            }
        }
    }

    /**
     * playerAttack() — gọi khi player bấm J hoặc LeftMouse.
     *
     * Luồng:
     *   1. Kiểm tra player có thể tấn công không (cooldown)
     *   2. Lấy hitbox chém từ PlayerCombat
     *   3. Duyệt enemyList, enemy nào hitbox dính vào thì takeDamage
     *   4. Reset cooldown của player
     * @return Số đòn Combo hiện tại (0 nếu chém trượt)
     */
    public void playerAttack() {
        // Kiểm tra cooldown tấn công — nếu chưa hết thì bỏ qua
        if (!player.canAttack()) return;

        // Nếu đánh đến đòn thứ 3 -> Bật chế độ Chọc (Thrust)
        boolean isThrust = (comboCount > 0 && comboCount % 3 == 2);

        // Kích hoạt Animation chém và vẽ hiệu ứng
        player.triggerAttackVisuals(isThrust);

        // Reset cooldown tấn công của player ngay lúc vung kiếm
        player.resetAttackCooldown(isThrust ? 18 : 16);
        
        hasDealtDamageThisAttack = false; // Đánh dấu là chưa gây sát thương cho nhát chém này
        hasIncreasedComboThisAttack = false; // Reset cờ cộng dồn combo
    }

    private void processAttackHits(int baseDamage, boolean isThrust, boolean applyKnockback) {
        // Tính tỷ lệ Crit dựa trên Combo
        int currentChance = 1;
        if (comboCount == 4) currentChance = 5;
        else if (comboCount == 5) currentChance = 10;
        else if (comboCount == 6) currentChance = 20;
        else if (comboCount >= 7) currentChance = 30;
        
        boolean isCrit = (Math.random() * 100) < currentChance;

        // Tính damage thực tế — nhân đôi nếu skill đang bật
        int actualDamage = baseDamage;
        if (skillActive) {
            actualDamage *= SKILL_DAMAGE_MULTIPLIER;
        }
        if (isCrit) {
            actualDamage *= 2; // Chí mạng x2 sát thương tổng
        }

        // Tính toán hitbox tấn công trực tiếp để tránh lỗi thiếu class PlayerCombat
        double attackRange = isThrust ? 45.0 : 30.0; // Tầm chọc dài hơn chém thường 1 chút
        double px = player.getX();
        double py = player.getY();
        double pw = player.getRenderWidth();
        double ph = player.getRenderHeight();
        
        Rectangle2D attackBox;
        switch (player.getDirection()) {
            case UP:
                attackBox = new Rectangle2D(px, py - attackRange, pw, attackRange); break;
            case DOWN:
                attackBox = new Rectangle2D(px, py + ph, pw, attackRange); break;
            case LEFT:
                attackBox = new Rectangle2D(px - attackRange, py, attackRange, ph); break;
            case RIGHT:
                attackBox = new Rectangle2D(px + pw, py, attackRange, ph); break;
            default:
                attackBox = new Rectangle2D(px, py, pw, ph); break;
        }

        boolean hitAny = false;
        boolean hitSlime = false;
        boolean hitKnight = false;
        boolean hitTree = false;
        boolean hitWitch = false;
        boolean isWitchDead = false;
        boolean hitBoss = false;
        boolean isFinalHit = false;
        boolean isBossDefeated = false;

        // Duyệt tất cả enemy, kiểm tra hitbox chém có dính vào không
        for (Enemy enemy : enemyList) {
            // Bỏ qua nếu quái vật đã chết (đang trong trạng thái mờ dần)
            if (enemy.getHp() <= 0) continue;

            // Lấy boundary của enemy (Rectangle2D)
            Rectangle2D enemyBox = enemy.getBoundary();

            // Nếu hitbox chém intersects với boundary của enemy → gây damage
            if (attackBox.intersects(enemyBox)) {
                enemy.takeDamage(actualDamage);

                // Bật hiệu ứng knockback (với đòn chọc thì chỉ đẩy lùi ở phát cuối cùng)
                if (applyKnockback) {
                    enemy.applyKnockback(player.getDirection());
                }
                hitAny = true; // Xác nhận đã chém trúng
                
                // Bắn hạt máu văng ra
                double bloodX = enemy.getX() + enemy.getRenderWidth() / 2;
                double bloodY = enemy.getY() + enemy.getRenderHeight() / 2;
                particleManager.spawnBlood(bloodX, bloodY, player.getDirection());

                // Phân loại quái vật và xác định đòn kết liễu
                if (enemy instanceof com.hust.game.enemy.Slime) hitSlime = true;
                if (enemy instanceof com.hust.game.enemy.Knight) hitKnight = true;
                if (enemy instanceof com.hust.game.enemy.Tree) hitTree = true;
                if (enemy instanceof com.hust.game.enemy.Witch) hitWitch = true;
                if (enemy instanceof com.hust.game.enemy.FinalBoss) hitBoss = true;
                
                if (enemy.getHp() <= 0) {
                    isFinalHit = true;
                    if (enemy instanceof com.hust.game.enemy.Witch) isWitchDead = true;
                if (enemy instanceof com.hust.game.enemy.FinalBoss) isBossDefeated = true;
                }

                // Tính toạ độ hiển thị text ngay trên đầu quái vật
                double textX = enemy.getX() + enemy.getRenderWidth() / 2 - 15;
                double textY = enemy.getY();
                // Thêm Text CRIT nếu chém chí mạng, hoặc phủ màu Cyan nếu đang cuồng nộ
                if (isCrit) {
                    javafx.scene.paint.Color critColor = skillActive ? javafx.scene.paint.Color.CYAN : javafx.scene.paint.Color.RED;
                    double sizeMult = skillActive ? 2.0 : 1.5;
                    String text = "CRIT DAMAGE!\n-" + actualDamage;
                    critTexts.add(new CritText(textX - 20, textY - 20, text, critColor, sizeMult));
                    
                    // Xóa số sát thương trắng mặc định sinh ra từ hàm takeDamage
                    com.hust.game.ui.DamageTextManager.removeLastText(enemy);
                } else if (skillActive) {
                    String text = "-" + actualDamage;
                    critTexts.add(new CritText(textX, textY, text, javafx.scene.paint.Color.CYAN, 1.0));
                    
                    // Xóa số sát thương trắng mặc định sinh ra từ hàm takeDamage
                    com.hust.game.ui.DamageTextManager.removeLastText(enemy);
                }
            }
        }
        
        if (isBossDefeated) {
            hitStopFrames = 30; // Khựng lại 0.5s (30 frame) khi kết liễu Final Boss
        } else if (hitAny && !isThrust) { // Đòn chọc diễn ra quá nhanh nên tắt hit-stop để tránh màn hình bị giật cục
            hitStopFrames = 6;  // Khựng lại 0.1s (6 frame) cho mọi đòn chém trúng thông thường
        }
        
        if (hitAny) {
            // Đẩy lùi Player lại một khoảng nhỏ khi chém trúng quái (Recoil)
            if (applyKnockback) {
                player.applyRecoil(15.0);
            }

            // Rung màn hình khi chém trúng
            if (isCrit) {
                shakeTimer = 5;
                shakeAmplitude = isThrust ? 0.4 : 0.8; // Chọc liên tiếp nên giảm độ rung lại để không nhức mắt
            } else {
                shakeTimer = 4;
                shakeAmplitude = isThrust ? 0.2 : 0.4; // Rung nhẹ mọi nhát chém thường
            }
        }

        // Xử lý âm thanh
        if (skillActive) {
            if (!hitAny) {
                com.hust.game.audio.SoundManager.playSwordPowerUpSound(0.5); // Chém trượt
            } else {
                com.hust.game.audio.SoundManager.playSwordPowerUpSound(1.0); // Chém trúng
                if (isWitchDead) {
                    com.hust.game.audio.SoundManager.playWitchDiedSound(); // Phát âm kết liễu phù thủy
                } else if (isFinalHit) {
                    com.hust.game.audio.SoundManager.playNsFinalHitSound(); // Phát thêm âm kết liễu
                }
                if (hitWitch && !isWitchDead) {
                    com.hust.game.audio.SoundManager.playWitchDmgTakenSound(); // Âm thanh phù thủy nhận dmg
                }
            }
        } else {
            if (!hitAny) {
                com.hust.game.audio.SoundManager.playNsMissSound(); // Chém trượt
            } else if (isWitchDead) {
                com.hust.game.audio.SoundManager.playWitchDiedSound(); // Đòn kết liễu phù thủy thay âm mặc định
            } else if (isFinalHit) {
                com.hust.game.audio.SoundManager.playNsFinalHitSound(); // Đòn kết liễu
            } else {
                // Chém thường trúng quái
                if (hitSlime) com.hust.game.audio.SoundManager.playNsHitSlimeSound();
                if (hitKnight) com.hust.game.audio.SoundManager.playNsHitKnightSound();
                if (hitTree) {
                    com.hust.game.audio.SoundManager.playNsHitTreeSound();
                    com.hust.game.audio.SoundManager.playNsMissSound();
                } // Cây dùng chung âm thanh với Knight
                if (hitWitch) {
                    com.hust.game.audio.SoundManager.playWitchDmgTakenSound();
                }
                if (hitBoss) com.hust.game.audio.SoundManager.playNsHitKnightSound();
            }
        }

        // Xử lý tăng Combo nếu chém trúng
        if (hitAny) {
            if (!hasIncreasedComboThisAttack) { // Cả 3 phát chọc chỉ tăng 1 combo tổng
                if (comboTimer > 0) comboCount++; 
                else comboCount = 1; 
                hasIncreasedComboThisAttack = true;
            }
            comboTimer = COMBO_WINDOW_FRAMES; // Reset lại đồng hồ 1 giây
            
            // Hiển thị chữ Combo bay lên (Tận dụng luôn DamageTextManager)
            // Truyền `this` làm target để mỗi khi Combo tăng, số cũ sẽ biến mất nhường chỗ cho số mới
            if (comboCount >= 3) {
                javafx.scene.paint.Color comboColor;
                if (comboCount == 3) {
                    comboColor = javafx.scene.paint.Color.DEEPSKYBLUE; // Combo 3: Xanh nước biển sáng
                } else if (comboCount == 4) {
                    comboColor = javafx.scene.paint.Color.ORANGE; // Combo 4: Cam
                } else {
                    comboColor = javafx.scene.paint.Color.RED; // Combo 5 trở lên: Đỏ
                }
                com.hust.game.ui.DamageTextManager.addText(this, player.getX() - 10, player.getY() - 15, "Combo x" + comboCount, comboColor);
            }
        }
    }

    /**
     * activateSkill() — gọi khi player bấm L.
     *
     * Điều kiện kích hoạt:
     *   - Skill không đang bật
     *   - Cooldown đã về 0
     *   - Player còn đủ máu để rút (> SKILL_HP_COST)
     */
    public void activateSkill() {
        // Không cho dùng khi skill đang chạy hoặc còn cooldown
        if (skillActive || skillCooldown > 0) return;
        // Sửa lỗi: Phải kiểm tra với MANA_COST và điều kiện là < (thiếu mana mới return)
        if (player.getCurrentMana() < SKILL_MANA_COST) return;

        player.takeMana(SKILL_MANA_COST);

        // Bật skill + set duration
        skillActive   = true;
        skillDuration = SKILL_DURATION_FRAMES;
        
        player.setRageMode(true, skillDuration);

        // Phát âm thanh Power Up
        com.hust.game.audio.SoundManager.playPlayerPowerUpSound();
        com.hust.game.audio.SoundManager.playThunderSound();

        System.out.println("Skill CUỒNG NỘ kích hoạt! Damage x2 trong 10 giây");
    }

    /**
     * Vẽ các số Crit và hiệu ứng Cuồng nộ đè lên giao diện
     */
    public void renderCrits(javafx.scene.canvas.GraphicsContext gc) {
        javafx.scene.text.Font baseFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 16);
        if (baseFont == null) baseFont = new javafx.scene.text.Font("Arial", 16);
        
        for (int i = critTexts.size() - 1; i >= 0; i--) {
            CritText ct = critTexts.get(i);
            gc.setFont(javafx.scene.text.Font.font(baseFont.getFamily(), javafx.scene.text.FontWeight.BOLD, 16 * ct.sizeMultiplier));
            gc.setFill(ct.color);
            gc.setStroke(javafx.scene.paint.Color.WHITE);
            gc.setLineWidth(1.5);
            
            String[] lines = ct.text.split("\n");
            double currentY = ct.y;
            for (String line : lines) {
                gc.strokeText(line, ct.x, currentY);
                gc.fillText(line, ct.x, currentY);
                currentY += 18 * ct.sizeMultiplier; // Giãn dòng theo kích thước
            }
            
            ct.y -= 0.5; // Chữ bay lên từ từ
            ct.timer--;
            if (ct.timer <= 0) critTexts.remove(i);
        }
    }
    // -------------------------------------------------------
    // GETTER cho HUD — Member C dùng để hiển thị trạng thái skill
    // -------------------------------------------------------

    /** Skill có đang bật không */
    public boolean isSkillActive() { return skillActive; }

    /** Còn bao nhiêu frame trước khi skill tắt */
    public int getSkillDurationRemaining() { return skillDuration; }

    /** Còn bao nhiêu frame trước khi dùng skill lại được */
    public int getSkillCooldownRemaining() { return skillCooldown; }

    /** Trả về số đòn Combo hiện tại */
    public int getComboCount() { return comboCount; }

    /** Trả về thời gian combo còn lại để áp dụng hiệu ứng mờ dần */
    public int getComboTimer() { return comboTimer; }

    public void resetSkill(){
        this.skillCooldown = 0;
        this.skillDuration = 0;
    }
    
    public ParticleManager getParticleManager() {
        return particleManager;
    }
    
    public int consumeHitStopFrames() {
        int frames = hitStopFrames;
        hitStopFrames = 0;
        return frames;
    }
    
    public int consumeShakeTimer() {
        int timer = shakeTimer;
        shakeTimer = 0;
        return timer;
    }
    
    public double consumeShakeAmplitude() {
        double amp = shakeAmplitude;
        shakeAmplitude = 0;
        return amp;
    }
}