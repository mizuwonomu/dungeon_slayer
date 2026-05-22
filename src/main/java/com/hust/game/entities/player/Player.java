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
import javafx.geometry.Rectangle2D;
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

    // Ảnh lướt (Dash)
    private final Image dashUp, dashDown, dashLeft, dashRight;

    // Thêm field bật skill
    private final Image rageHitImg;
    private final Image powerUpImg;
    private final Image thunderImg;

    private boolean isRageActive = false;
    private int rageTimer = 0;
    private int powerUpFrameIndex = 0;
    private int powerUpAnimTimer = 0;

    private boolean isThunderActive = false;
    private int thunderFrameIndex = 0;
    private int thunderAnimTimer = 0;

    // Trạng thái tấn công
    private boolean isAttacking = false;
    private AttackEffect attackEffect;

    // Trạng thái đâm/chọc
    private boolean isThrusting = false;
    private int thrustTimer = 0;

    // Trạng thái lướt (Dash)
    private boolean isDashing = false;
    private int dashCooldown = 0;
    private int dashComboTimer = 0; // Bộ đếm thời gian cho phép lướt đúp
    private static final int DASH_COOLDOWN_MAX = 90; // Hồi chiêu 1.5s
    private double dashVectorX = 0;
    private double dashVectorY = 0;

    // Trạng thái giật lùi khi chém (Recoil)
    private int recoilTimer = 0;
    private double recoilVectorX = 0;
    private double recoilVectorY = 0;

    // Trạng thái bị knockback khi nhận sát thương
    private int kbTimer = 0;
    private double kbVectorX = 0;
    private double kbVectorY = 0;

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
    private final int maxHp = GameConstants.PLAYER_MAX_HP; // máu tối đa lấy từ constants
    private int currentMana;
    private final int maxMana = GameConstants.PLAYER_MAX_MANA; // máu tối đa lấy từ constants

    private int healthPotionCount = 0;
    private int manaPotionCount = 0;

    // -------------------------------------------------------
    // ATTACK COOLDOWN
    // Sau mỗi lần tấn công, phải chờ ATTACK_COOLDOWN_MAX frame
    // mới được tấn công lại → tránh spam attack.
    // -------------------------------------------------------
    private int attackCooldown = 0; 
    private static final int ATTACK_COOLDOWN_MAX = (8 * 2); // Giảm thời gian chờ xuống còn một nửa (16 frames)
    private final int attackDamage = 20; // sát thương mỗi đòn

    // -------------------------------------------------------
    // CONSTRUCTOR
    // Truyền idleDown làm spriteSheet mặc định ban đầu (nhân vật nhìn xuống)
    // numFrames và renderSize lấy từ GameConstants → không hardcode
    // -------------------------------------------------------
    public Player(double x, double y,
            Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
            Image runDown, Image runUp, Image runLeft, Image runRight,
            Image combatDown, Image combatUp, Image combatLeft, Image combatRight,
            Image dashDown, Image dashUp, Image dashLeft, Image dashRight,
            Image swordHitImg, Image rageHitImg, Image powerUpImg, Image thunderImg) {

        // Gọi constructor MovingEntity: truyền vị trí, spriteSheet mặc định,
        // số frame, kích thước render, và tốc độ di chuyển
        super(x, y,
                idleDown,
                GameConstants.PLAYER_NUM_FRAMES,
                64, 64,
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

        this.dashDown = dashDown;
        this.dashUp = dashUp;
        this.dashLeft = dashLeft;
        this.dashRight = dashRight;

        // Khởi tạo máu đầy
        this.currentHp = maxHp;
        this.currentMana = maxMana;
        // Tạo entity chứa hiệu ứng kiếm
        this.rageHitImg = rageHitImg;
        this.powerUpImg = powerUpImg;
        this.thunderImg = thunderImg;
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
        if (isDashing) {
            this.spriteSheet = switch (direction) {
                case UP -> dashUp;
                case DOWN -> dashDown;
                case LEFT -> dashLeft;
                case RIGHT -> dashRight;
            };
            this.frameWidth = spriteSheet.getWidth() / 3.0; // Dash có 3 frames
            this.frameHeight = spriteSheet.getHeight();
            return;
        }

        if (isAttacking || isThrusting) {
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
            // tạm thời dùng idle
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
        if (flashTimer > 0)
            flashTimer--; // Giảm dần thời gian nháy đỏ

        if (isThrusting) {
            thrustTimer--;
            if (thrustTimer <= 0) {
                isThrusting = false;
                frameIndex = 0;
                if (attackEffect != null) attackEffect.setActive(false);
                updateSpriteSheet(); 
            }
            if (attackEffect != null) attackEffect.update();
        } else if (isAttacking) {
            animationTimer++;
            // Tăng tốc độ chém lên gấp đôi: chỉ mất 2 frame game để chuyển 1 frame ảnh
            if (animationTimer >= 2) {
                animationTimer = 0;
                frameIndex++;
                if (frameIndex >= 8) { // Kết thúc 8 frame chém
                    isAttacking = false;
                    frameIndex = 0;
                    if (attackEffect != null)
                        attackEffect.setActive(false);
                    updateSpriteSheet(); // Khôi phục ảnh idle/run
                }
            }
            if (attackEffect != null)
                attackEffect.update();
        } else if (isDashing) {
            // Làm mượt Dash: Giảm tốc độ dần đều (Ease-out) bằng ma sát
            dashVectorX *= 0.85;
            dashVectorY *= 0.85;

            animationTimer++;
            // Chạy 3 frame animation trong 12 frame game (0.2s) -> mỗi frame tồn tại 4 frame game
            if (animationTimer >= 4) {
                animationTimer = 0;
                frameIndex++;
                if (frameIndex >= 3) {
                    isDashing = false;
                    frameIndex = 0;
                    updateSpriteSheet(); // Lướt xong thì quay về dáng đứng hoặc chạy
                }
            }
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
            
        // Cập nhật trạng thái lướt
        if (dashCooldown > 0) dashCooldown--;

        // Cập nhật giật lùi (Recoil)
        if (recoilTimer > 0) {
            double multiplier = recoilTimer / 3.0; // Từ 5 đến 1, tổng multiplier = 5.0 (Quãng đường không đổi)
            recoilTimer--;
            this.x += recoilVectorX * multiplier;
            this.y += recoilVectorY * multiplier;
        }
            
        // Cập nhật Knockback khi bị thương
        if (kbTimer > 0) {
            double multiplier = kbTimer / 3.5; // Từ 6 đến 1, tổng multiplier = 6.0 (Quãng đường không đổi)
            kbTimer--;
            this.x += kbVectorX * multiplier;
            this.y += kbVectorY * multiplier;
        }

        // Cập nhật hiệu ứng hào quang cuồng nộ
        if (isRageActive) {
            if (rageTimer > 0) rageTimer--;
            powerUpAnimTimer++;
            if (powerUpAnimTimer >= 8) { // Cứ 8 frames game đổi 1 hình của Power Up
                powerUpAnimTimer = 0;
                powerUpFrameIndex = (powerUpFrameIndex + 1) % 5;
            }
        }
        
        // Cập nhật hiệu ứng sấm sét
        if (isThunderActive) {
            thunderAnimTimer++;
            // Tốc độ: 60 * 3 / 8 = 22.5 => Chạy khoảng 7-8 frames game cho mỗi 1 hình sấm sét
            if (thunderAnimTimer >= 8) {
                thunderAnimTimer = 0;
                thunderFrameIndex++;
                if (thunderFrameIndex >= 3) { // Hoàn thành 3 hình thì tắt sấm sét
                    isThunderActive = false;
                }
            }
        }
    }

    // Thêm method — CombatManager gọi khi bật/tắt skill
    public void setRageMode(boolean active, int duration) {
        if (attackEffect != null) attackEffect.setRageMode(active);
        this.isRageActive = active;
        this.rageTimer = duration;
        if (active) {
            this.isThunderActive = true;
            this.thunderFrameIndex = 0;
            this.thunderAnimTimer = 0;
            this.powerUpFrameIndex = 0;
            this.powerUpAnimTimer = 0;
        } else {
            this.isThunderActive = false;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Hiệu ứng đỏ khi bị thương đã được loại bỏ theo yêu cầu.

        if (isDashing) {
            // Tính toán khung hình lướt sao cho ôm trọn tâm Player dựa trên kích thước gốc 64x64
            double drawW = this.frameWidth * (this.renderWidth / 64.0);
            double drawH = this.frameHeight * (this.renderHeight / 64.0);
            
            double centerX = this.x + this.renderWidth / 2.0;
            double centerY = this.y + this.renderHeight / 2.0;
            
            double drawX = centerX - drawW / 2.0;
            double drawY = centerY - drawH / 2.0;
            
            double sx = this.frameIndex * this.frameWidth;
            
            if (this.isFlipped) {
                gc.drawImage(this.spriteSheet, sx, 0, this.frameWidth, this.frameHeight, drawX + drawW, drawY, -drawW, drawH);
            } else {
                gc.drawImage(this.spriteSheet, sx, 0, this.frameWidth, this.frameHeight, drawX, drawY, drawW, drawH);
            }
        } else {
            super.render(gc);
        }

        // Vẽ hiệu ứng Power Up đè lên Player
        if (isRageActive && powerUpImg != null) {
            gc.save();
            // Ở 60 frame (1 giây) cuối cùng -> giảm dần Alpha để tạo hiệu ứng mờ dần
            double alpha = 1.0;
            if (rageTimer <= 60) {
                alpha = Math.max(0, (double) rageTimer / 60.0);
            }
            gc.setGlobalAlpha(alpha);
            
            double pw = powerUpImg.getWidth() / 5;
            double ph = powerUpImg.getHeight();
            double px = powerUpFrameIndex * pw;
            
            // Kích thước tương đương với Player và đè lên người
            gc.drawImage(powerUpImg, px, 0, pw, ph, x, y, renderWidth, renderHeight);
            gc.restore();
        }
        
        // Vẽ hiệu ứng Thunder sấm sét đè lên tất cả
        if (isThunderActive && thunderImg != null) {
            double tw = thunderImg.getWidth() / 3;
            double th = thunderImg.getHeight();
            double tx = thunderFrameIndex * tw;
            
            double thunderRenderHeight = 256.0;
            // Tính toán tỷ lệ bề ngang để hình không bị méo (hoặc kéo giãn theo đúng tỷ lệ ảnh)
            double thunderRenderWidth = thunderRenderHeight * (tw / th);
            
            // Căn giữa tia sét và đồng bộ phần ĐÁY của sét với ĐÁY của nhân vật
            double thunderX = x + (renderWidth / 2) - (thunderRenderWidth / 2);
            double thunderY = y + renderHeight - thunderRenderHeight;
            
            gc.drawImage(thunderImg, tx, 0, tw, th, thunderX, thunderY, thunderRenderWidth, thunderRenderHeight);
        }

        if ((isAttacking || isThrusting) && attackEffect != null) {
            attackEffect.render(gc); // Vẽ hiệu ứng kiếm đè lên
        }
    }

    public void triggerAttackVisuals(boolean isThrust) {
        if (isThrust) {
            this.isAttacking = false;
            isThrusting = true;
            thrustTimer = 18; // Kéo dài 0.3s cho đòn chọc
            frameIndex = 0;
            updateSpriteSheet();
            if (attackEffect != null) attackEffect.trigger(true);
        } else {
            this.isThrusting = false;
            isAttacking = true;
            frameIndex = 0;
            animationTimer = 0;
            updateSpriteSheet();
            if (attackEffect != null) attackEffect.trigger(false);
        }
    }

    public boolean canDash() {
        return dashCooldown <= 0 && !isDashing && !isAttacking;
    }

    public Rectangle2D getAttackBox() {
        double attackRange = isThrusting ? 45.0 : 30.0;
        double px = this.x;
        double py = this.y;
        double pw = this.renderWidth;
        double ph = this.renderHeight;
        
        switch (direction) {
            case UP:    return new Rectangle2D(px, py - attackRange, pw, attackRange);
            case DOWN:  return new Rectangle2D(px, py + ph, pw, attackRange);
            case LEFT:  return new Rectangle2D(px - attackRange, py, attackRange, ph);
            case RIGHT: return new Rectangle2D(px + pw, py, attackRange, ph);
            default:    return new Rectangle2D(px, py, pw, ph);
        }
    }

    public void startDash(double dx, double dy) {
        isDashing = true;
        
        if (dashComboTimer > 0) {
            dashCooldown = DASH_COOLDOWN_MAX; // Lướt lần 2 -> Phạt hồi chiêu 1.5s
            dashComboTimer = 0;
        } else {
            dashCooldown = 0; // Lướt lần 1 -> Không hồi chiêu ngay
            dashComboTimer = 30; // Cho phép 0.5s (30 frames) để lướt đúp kể từ lúc lướt xong lần 1
        }
        
        frameIndex = 0;
        animationTimer = 0;
        updateSpriteSheet();
        
        // Nếu không bấm phím hướng nào mà vẫn ấn SHIFT -> lướt theo hướng mặt hiện tại
        if (dx == 0 && dy == 0) {
            switch (direction) {
                case UP: dy = -1; break;
                case DOWN: dy = 1; break;
                case LEFT: dx = -1; break;
                case RIGHT: dx = 1; break;
            }
        } else {
            // Chuẩn hóa vector di chuyển chéo
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }
        
        // Tăng mạnh tốc độ ban đầu và giảm dần (Ease-out) trong quá trình update
        // Vận tốc ban đầu cao (9.5) nhân với hệ số suy giảm 0.85 qua 12 frame sẽ cho tổng quãng đường ~160px
        double dashSpeedMultiplier = 9.5;
        dashVectorX = dx * GameConstants.PLAYER_SPEED * dashSpeedMultiplier;
        dashVectorY = dy * GameConstants.PLAYER_SPEED * dashSpeedMultiplier;
    }

    public boolean isDashing() { return isDashing; }
    public double getDashVectorX() { return dashVectorX; }
    public double getDashVectorY() { return dashVectorY; }

    // Đẩy lùi nhẹ khi chém trúng mục tiêu (Recoil)
    public void applyRecoil(double distance) {
        this.recoilTimer = 5; // Dội lùi từ từ trong 5 frames
        double speed = distance / 5.0;
        this.recoilVectorX = 0;
        this.recoilVectorY = 0;
        switch (direction) {
            case UP: this.recoilVectorY = speed; break;
            case DOWN: this.recoilVectorY = -speed; break;
            case LEFT: this.recoilVectorX = speed; break;
            case RIGHT: this.recoilVectorX = -speed; break;
        }
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

    @Override
    public void takeDamage(int amount) {
        takeDamage(amount, this.x, this.y); // Nếu không có nguồn, giật lùi tại chỗ
    }

    public void takeDamage(int amount, BaseEntity source) {
        if (flashTimer > 0 || isDashing) return;
        
        // --- CƠ CHẾ PARRY ---
        // Chỉ tính Parry đúng vào các frame lưỡi kiếm chạm mục tiêu (Khung hình 3 với chém thường, hoặc các nhịp chọc)
        boolean isParryWindow = false;
        if (isAttacking && this.frameIndex == 3) isParryWindow = true;
        if (isAttacking && this.frameIndex >= 3 && this.frameIndex <= 5) isParryWindow = true;
        if (isThrusting && (this.thrustTimer == 15 || this.thrustTimer == 9 || this.thrustTimer == 3)) isParryWindow = true;
        
        // Nếu quái vật tấn công từ phía trước (nằm trong tầm chém) vào đúng Frame này
        if (isParryWindow && source != null) {
            Rectangle2D attackBox = getAttackBox();
            Rectangle2D sourceBox = source.getBoundary();
            
            // Tầm đánh của Tree xa hơn thân hình, nên ta mở rộng hitbox xét Parry để chém trúng "chiêu" của nó
            if (source instanceof com.hust.game.enemy.Tree) {
                sourceBox = new Rectangle2D(sourceBox.getMinX() - 45, sourceBox.getMinY() - 45, sourceBox.getWidth() + 90, sourceBox.getHeight() + 90);
            }
            
            if (attackBox.intersects(sourceBox)) {
                // Đỡ đòn thành công! Không mất máu.
                applyRecoil(25.0); // Giật lùi Player lại để tạo cảm giác chém vào vật cứng
                
                if (source instanceof com.hust.game.enemy.Enemy) {
                    ((com.hust.game.enemy.Enemy) source).applyKnockback(this.direction);
                }

                com.hust.game.main.App.triggerScreenShake(6, 0.4);
                com.hust.game.audio.SoundManager.playNsHitKnightSound(); // Dùng tiếng chém trúng giáp sắt làm tiếng Parry
                com.hust.game.ui.DamageTextManager.addText(this, this.x + renderWidth / 2 - 20, this.y - 15, "PARRY!", javafx.scene.paint.Color.CYAN);
                
                return; // Thoát hàm, miễn nhiễm sát thương
            }
        }

        takeDamage(amount, source.getX() + source.getRenderWidth() / 2.0, source.getY() + source.getRenderHeight() / 2.0);
    }

    /** Trừ máu, bật tử và tính toán góc giật lùi */
    public void takeDamage(int amount, double srcX, double srcY) {
        if (flashTimer > 0 || isDashing)
            return; // Nháy đỏ hoặc Đang lướt (I-frames) -> Miễn nhiễm sát thương
        currentHp = Math.max(0, currentHp - amount);
        flashTimer = 18; // Kích hoạt thời gian nháy đỏ và bất tử (18 frame ~ 0.3s)
        com.hust.game.ui.DamageTextManager.addText(this, this.x + renderWidth / 2 - 10, this.y, "-" + amount,
                javafx.scene.paint.Color.RED);
        com.hust.game.audio.SoundManager.playPlayerHitSound(); // Phát âm thanh bị thương
        com.hust.game.main.App.triggerScreenShake(10, 0.5); // Giảm độ rung màn hình khi nhận sát thương

        // Áp dụng knockback đẩy lùi khỏi nguồn sát thương
        this.kbTimer = 6; // Bị giật lùi trong 6 frames liên tiếp
        double pCenterX = this.x + this.renderWidth / 2.0;
        double pCenterY = this.y + this.renderHeight / 2.0;
        double diffX = pCenterX - srcX;
        double diffY = pCenterY - srcY;
        double dist = Math.sqrt(diffX * diffX + diffY * diffY);
        if (dist == 0) { diffX = 1; dist = 1; } // Tránh lỗi chia cho 0
        
        double kbSpeed = 6.0; // Lực đẩy lùi
        this.kbVectorX = (diffX / dist) * kbSpeed;
        this.kbVectorY = (diffY / dist) * kbSpeed;
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
    
    public int getCurrentMana() {
        return currentMana;
    }

    public int getHealthPotionCount() {
        return healthPotionCount;
    }

    public int getManaPotionCount() {
        return manaPotionCount;
    }

    public int getTotalPotionCount() {
        return healthPotionCount + manaPotionCount;
    }

    public boolean isPotionInventoryFull() {
        return getTotalPotionCount() >= GameConstants.MAX_POTIONS_TOTAL;
    }

    public boolean addHealthPotion() {
        if (isPotionInventoryFull() || healthPotionCount >= GameConstants.MAX_POTIONS_PER_TYPE) {
            return false;
        }
        healthPotionCount++;
        return true;
    }

    public boolean addManaPotion() {
        if (isPotionInventoryFull() || manaPotionCount >= GameConstants.MAX_POTIONS_PER_TYPE) {
            return false;
        }
        manaPotionCount++;
        return true;
    }

    public boolean useHealthPotion() {
        if (healthPotionCount <= 0 || currentHp >= maxHp) {
            return false;
        }
        healthPotionCount--;
        healHp(GameConstants.POTION_HEAL_AMOUNT);
        return true;
    }

    public boolean useManaPotion() {
        if (manaPotionCount <= 0 || currentMana >= maxMana) {
            return false;
        }
        manaPotionCount--;
        restoreMana(GameConstants.POTION_MANA_AMOUNT);
        return true;
    }

    private void healHp(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    private void restoreMana(int amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
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
        return attackCooldown == 0 && !isDashing && !isThrusting; // Không được chém khi đang lướt/chọc
    }

    public void resetAttackCooldown(int cooldown) {
        attackCooldown = cooldown;
    }

    /** Gọi sau khi tấn công để bắt đầu đếm cooldown */
    @Override
    public void resetAttackCooldown() {
        attackCooldown = ATTACK_COOLDOWN_MAX;
    }

    public void reset(double startX, double startY){
        this.x = startX;
        this.y = startY;

        this.currentHp = maxHp;
        this.currentMana = maxMana;
        this.healthPotionCount = 0;
        this.manaPotionCount = 0;
        resetAttackCooldown();
    }
}
