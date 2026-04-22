package com.hust.game.combat;

import com.hust.game.enemy.Enemy;
import com.hust.game.entities.Direction;
import com.hust.game.entities.player.Player;
import javafx.geometry.Rectangle2D;

import java.util.List;

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
    private static final int SKILL_DAMAGE_MULTIPLIER = 2;   // nhân đôi damage

    // -------------------------------------------------------
    // COMBO SYSTEM
    // -------------------------------------------------------
    private int comboCount = 0;
    private int comboTimer = 0;
    private static final int COMBO_WINDOW_FRAMES = 60; // 1 giây (60 fps)

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
    public int playerAttack() {
        // Kiểm tra cooldown tấn công — nếu chưa hết thì bỏ qua
        if (!player.canAttack()) return 0;

        // Tính damage thực tế — nhân đôi nếu skill đang bật
        int actualDamage = player.getAttackDamage();
        if (skillActive) {
            actualDamage *= SKILL_DAMAGE_MULTIPLIER;
        }

        // Kích hoạt Animation chém và vẽ hiệu ứng
        player.triggerAttackVisuals();

        // Tính toán hitbox tấn công trực tiếp để tránh lỗi thiếu class PlayerCombat
        double attackRange = 30.0; // Tầm chém (khoảng cách hitbox mở rộng ra phía trước)
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

        // Duyệt tất cả enemy, kiểm tra hitbox chém có dính vào không
        for (Enemy enemy : enemyList) {
            // Lấy boundary của enemy (Rectangle2D)
            Rectangle2D enemyBox = enemy.getBoundary();

            // Nếu hitbox chém intersects với boundary của enemy → gây damage
            if (attackBox.intersects(enemyBox)) {
                enemy.takeDamage(actualDamage);

                // Bật hiệu ứng knockback
                enemy.applyKnockback(player.getDirection());
                hitAny = true; // Xác nhận đã chém trúng
            }
        }

        // Xử lý tăng Combo nếu chém trúng
        if (hitAny) {
            if (comboTimer > 0) comboCount++; // Đang trong thời gian Combo -> Cộng dồn
            else comboCount = 1; // Khởi đầu Combo mới
            comboTimer = COMBO_WINDOW_FRAMES; // Reset lại đồng hồ 1 giây
        }

        // Reset cooldown tấn công của player
        player.resetAttackCooldown();

        return hitAny ? comboCount : 0;
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

        // Không cho dùng nếu máu không đủ để rút
        if (player.getCurrentHp() <= SKILL_HP_COST) return;

        // Rút máu player
        player.takeDamage(SKILL_HP_COST);

        // Bật skill + set duration
        skillActive   = true;
        skillDuration = SKILL_DURATION_FRAMES;

        System.out.println("Skill CUỒNG NỘ kích hoạt! Damage x2 trong 10 giây");
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
}