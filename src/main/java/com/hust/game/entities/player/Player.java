package com.hust.game.entities.player;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.base.MovingEntity;
import com.hust.game.entities.interfaces.Attackable;
import com.hust.game.entities.interfaces.Collidable;
import com.hust.game.entities.interfaces.Damageable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import lombok.Getter;

/**
 * Player - nhân vật do người chơi điều khiển.
 *
 * Kế thừa MovingEntity (có speed, direction, state, move*())
 * Implement 3 interface:
 * - Collidable : xử lý khi va chạm vào tường/enemy
 * - Damageable : nhận sát thương, kiểm tra HP
 * - Attackable : tấn công enemy (Member C dùng)
 */
@Getter
public class Player extends MovingEntity implements Collidable, Damageable, Attackable {

    // -------------------------------------------------------
    // SPRITE SHEETS — mỗi trạng thái + hướng = 1 file ảnh riêng
    // Lý do tách file: sy luôn = 0, chỉ cần dịch sx theo frameIndex
    // -------------------------------------------------------
    private final Image idleUp, idleDown, idleLeft, idleRight; // đứng yên 4 hướng
    private final Image runUp, runDown, runLeft, runRight; // chạy 4 hướng

    // Ảnh combat (đang chém)
    private final Image combatUp, combatDown, combatLeft, combatRight;
    
    // Thêm field bật skill
    private final Image rageHitImg;

    // Trạng thái tấn công
    private boolean isAttacking = false;
    private AttackEffect attackEffect;

    // -------------------------------------------------------
    // COLLISION ROLLBACK
    // Trước mỗi frame di chuyển, lưu lại vị trí cũ.
    // Nếu va chạm xảy ra → restore về lastX, lastY
    // -------------------------------------------------------
    private double lastX, lastY;

    // -------------------------------------------------------
    // ANIMATION TIMER
    // animationTimer đếm số frame game đã trôi qua.
    // Khi đủ animationDelay thì mới chuyển sang frame sprite tiếp theo.
    // Tách riêng khỏi game loop để kiểm soát tốc độ animation độc lập.
    // -------------------------------------------------------
    private int animationTimer = 0;

    // -------------------------------------------------------
    // DAMAGE EFFECT
    // Thời gian nhấp nháy khi nhận sát thương
    // -------------------------------------------------------
    private int flashTimer = 0;

    // -------------------------------------------------------
    // HP — máu nhân vật
    // -------------------------------------------------------
    private int currentHp; // máu hiện tại
 //   private final int maxHp = GameConstants.PLAYER_MAX_HP;// máu tối đa lấy từ constants
    private final int maxHp = 1000;
    private int currentMana;
    private final int maxMana = GameConstants.PLAYER_MAX_MANA; // máu tối đa lấy từ constants

    // -------------------------------------------------------
    // ATTACK COOLDOWN
    // Sau mỗi lần tấn công, phải chờ ATTACK_COOLDOWN_MAX frame
    // mới được tấn công lại → tránh spam attack.
    // -------------------------------------------------------
    private int attackCooldown = 0;
        private static final int ATTACK_COOLDOWN_MAX =
            (8 * 4); // 8 frames attack delay giữa các lần đánh
    private final int attackDamage = 10; // sát thương mỗi đòn

    // -------------------------------------------------------
    // CONSTRUCTOR
    // Truyền idleDown làm spriteSheet mặc định ban đầu (nhân vật nhìn xuống)
    // numFrames và renderSize lấy từ GameConstants → không hardcode
    // -------------------------------------------------------
    public Player(double x, double y,
            Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
            Image runDown, Image runUp, Image runLeft, Image runRight,
            Image combatDown, Image combatUp, Image combatLeft, Image combatRight,
            Image swordHitImg, Image rageHitImg) {

        // Gọi constructor MovingEntity: truyền vị trí, spriteSheet mặc định,
        // số frame, kích thước render, và tốc độ di chuyển
        super(x, y,
                idleDown,
                GameConstants.PLAYER_NUM_FRAMES,
                GameConstants.TILE_SIZE, GameConstants.TILE_SIZE,
                GameConstants.PLAYER_SPEED);

        // Lưu tất cả 8 sprite sheet vào field
        this.idleDown = idleDown;
        this.idleUp = idleUp;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.runDown = runDown;
        this.runUp = runUp;
        this.runLeft = runLeft;
        this.runRight = runRight;
        
        this.combatDown = combatDown;
        this.combatUp = combatUp;
        this.combatLeft = combatLeft;
        this.combatRight = combatRight;

        // Khởi tạo máu đầy
        this.currentHp = maxHp;
        this.currentMana = maxMana;
        // Tạo entity chứa hiệu ứng kiếm
        this.rageHitImg = rageHitImg;
        this.attackEffect = new AttackEffect(swordHitImg, rageHitImg, this);
    }

    // -------------------------------------------------------
    // SPRITE SWITCHING
    // Gọi mỗi khi state hoặc direction thay đổi.
    // Chọn đúng spriteSheet dựa trên tổ hợp (state x direction).
    // Reset frameIndex = 0 để animation bắt đầu lại từ đầu,
    // tránh hiện tượng nhảy frame giữa chừng khi đổi hướng.
    // -------------------------------------------------------
    private void updateSpriteSheet() {
        if (isAttacking) {
            this.spriteSheet = switch (direction) {
                case UP -> combatUp;
                case DOWN -> combatDown;
                case LEFT -> combatLeft;
                case RIGHT -> combatRight;
            };
            // Cập nhật số frame chém là 8
            this.frameWidth = spriteSheet.getWidth() / 8; 
            this.frameHeight = spriteSheet.getHeight(); // Tránh bị sai tỷ lệ ảnh
            return; // Thoát sớm, không dùng idle hay run nữa
        }

        // Dùng switch expression (Java 14+) cho gọn
        this.spriteSheet = switch (state) {
            case IDLE -> switch (direction) {
                case UP -> idleUp;
                case DOWN -> idleDown;
                case LEFT -> idleLeft;
                case RIGHT -> idleRight;
            };
            case RUNNING -> switch (direction) {
                case UP -> runUp;
                case DOWN -> runDown;
                case LEFT -> runLeft;
                case RIGHT -> runRight;
            };
            //tạm thời dùng idle
            case ATTACKING -> switch (direction) {
                case UP -> idleUp;
                case DOWN -> idleDown;
                case LEFT -> idleLeft;
                case RIGHT -> idleRight;
            };
        };
        // Cập nhật lại frameWidth vì spriteSheet mới có thể khác kích thước
        this.frameWidth = spriteSheet.getWidth() / GameConstants.PLAYER_NUM_FRAMES;
        this.frameHeight = spriteSheet.getHeight();

        // Reset về frame đầu tiên khi đổi animation
        this.frameIndex = 0;
    }

    /**
     * Đổi trạng thái (IDLE / RUNNING).
     * Chỉ cập nhật sprite khi state thực sự thay đổi → tránh reset animation liên
     * tục.
     */
    public void setState(EntityState newState) {
        if (this.state != newState) {
            this.state = newState;
            updateSpriteSheet();
        }
    }

    /**
     * Đổi hướng nhìn (UP/DOWN/LEFT/RIGHT).
     * Tương tự setState — chỉ update khi thực sự đổi hướng.
     */
    public void setDirection(Direction newDirection) {
        if (this.direction != newDirection) {
            this.direction = newDirection;
            updateSpriteSheet();
        }
    }

    /**
     * Lưu vị trí hiện tại trước khi di chuyển.
     * App.java gọi hàm này ở đầu mỗi frame, trước handleInput().
     */
    public void savePosition() {
        this.lastX = x;
        this.lastY = y;
    }

    // -------------------------------------------------------
    // UPDATE — gọi mỗi frame bởi game loop trong App.java
    // -------------------------------------------------------
    @Override
    public void update() {
        if (flashTimer > 0) flashTimer--; // Giảm dần thời gian nháy đỏ

        if (isAttacking) {
            animationTimer++;
            // Animation chém diễn ra nhanh hơn đi bộ
            if (animationTimer >= 4) {
                animationTimer = 0;
                frameIndex++;
                if (frameIndex >= 8) { // Kết thúc 8 frame chém
                    isAttacking = false;
                    frameIndex = 0;
                    if (attackEffect != null) attackEffect.setActive(false);
                    updateSpriteSheet(); // Khôi phục ảnh idle/run
                }
            }
            if (attackEffect != null) attackEffect.update();
        } else {
            // Đếm frame game đã trôi qua cho trạng thái đi bộ/đứng
            animationTimer++;
            if (animationTimer >= GameConstants.PLAYER_ANIMATION_DELAY) {
                animationTimer = 0;
                frameIndex = (frameIndex + 1) % GameConstants.PLAYER_NUM_FRAMES;
            }
        }

        // Giảm cooldown tấn công mỗi frame (đếm ngược về 0)
        if (attackCooldown > 0)
            attackCooldown--;
    }

    // Thêm method — CombatManager gọi khi bật/tắt skill
    public void setRageMode(boolean active) {
        if (attackEffect != null) attackEffect.setRageMode(active);
    }

    // Tạo sẵn hiệu ứng tĩnh (static) để GPU không bị quá tải khi load lại mỗi frame
    private static final javafx.scene.effect.ColorAdjust RED_EFFECT = new javafx.scene.effect.ColorAdjust(-0.15, 0.8, 0.1, 0);
    
    @Override
    public void render(GraphicsContext gc) {
       gc.save();
        // Hiệu ứng đỏ khi bị thương đồng bộ với quái vật
        if (flashTimer > 0) {
            gc.setEffect(RED_EFFECT);
        }
        
        super.render(gc); 
        gc.restore();

        
        if (isAttacking && attackEffect != null) {
            attackEffect.render(gc); // Vẽ hiệu ứng kiếm đè lên
        }
    }

    public void triggerAttackVisuals() {
        isAttacking = true;
        frameIndex = 0;
        animationTimer = 0;
        updateSpriteSheet();
        if (attackEffect != null) attackEffect.trigger();
    }

    // -------------------------------------------------------
    // COLLIDABLE — xử lý va chạm
    // -------------------------------------------------------
    /**
     * Khi va chạm với tường hoặc entity khác:
     * khôi phục về vị trí trước khi di chuyển (rollback).
     * App.java gọi hàm này sau khi phát hiện intersects().
     */
    @Override
    public void onCollision(BaseEntity other) {
        this.x = lastX;
        this.y = lastY;
    }

    // -------------------------------------------------------
    // DAMAGEABLE — nhận sát thương (Member C dùng)
    // -------------------------------------------------------

    /** Trừ máu, không cho xuống dưới 0 */
    @Override
    public void takeDamage(int amount) {
        if (flashTimer > 0) return; // Nếu đang trong trạng thái nháy đỏ -> Bất tử (Miễn nhiễm sát thương)
        currentHp = Math.max(0, currentHp - amount);
        flashTimer = 18; // Kích hoạt thời gian nháy đỏ và bất tử (18 frame ~ 0.3s)
        com.hust.game.ui.DamageTextManager.addText(this, this.x + renderWidth / 2 - 10, this.y, "-" + amount, javafx.scene.paint.Color.RED);
        com.hust.game.audio.SoundManager.playPlayerHitSound(); // Phát âm thanh bị thương
    }

    public void takeMana(int amount) {
        currentMana = Math.max(0, currentMana - amount);
    }

    @Override
    public int getCurrentHp() {
        return currentHp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    public int getMaxMana() {
        return maxMana;
    }

    /** Trả về true khi máu = 0 → game over */
    @Override
    public boolean isDead() {
        return currentHp <= 0;
    }

    // -------------------------------------------------------
    // ATTACKABLE — tấn công (Member C dùng)
    // -------------------------------------------------------

    @Override
    public int getAttackDamage() {
        return attackDamage;
    }

    /** Chỉ cho tấn công khi cooldown đã về 0 */
    @Override
    public boolean canAttack() {
        return attackCooldown == 0;
    }

    /** Gọi sau khi tấn công để bắt đầu đếm cooldown */
    @Override
    public void resetAttackCooldown() {
        attackCooldown = ATTACK_COOLDOWN_MAX;
    }
}