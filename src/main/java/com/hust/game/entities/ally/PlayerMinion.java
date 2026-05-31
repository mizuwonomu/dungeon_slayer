package com.hust.game.entities.ally;

import com.hust.game.constants.GameConstants;
import com.hust.game.enemy.FinalBoss;
import com.hust.game.entities.Direction;
import com.hust.game.entities.base.BaseEntity;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class PlayerMinion extends BaseEntity {
    private static final int PLAYER_FRAMES = GameConstants.PLAYER_NUM_FRAMES;
    private static final int ANIMATION_DELAY = 8;
    private static final int LIFETIME_FRAMES = 900;
    private static final int ATTACK_FRAMES = 8;
    private static final int ATTACK_FRAME_DELAY = 5;
    private static final int DAMAGE_FRAME = 4;
    private static final int ATTACK_COOLDOWN_FRAMES = 48;
    private static final int ATTACK_DAMAGE = 20;
    private static final double SPEED = 2.25;
    private static final double ATTACK_RANGE = 30.0;
    private static final double RENDER_SIZE = 38.0;
    private static final double SWORD_RENDER_SIZE = 64.0;
    private static final double SWORD_ANCHOR_OFFSET = 16.0;
    private static final double TARGET_HURTBOX_PADDING = 0.0;

    private final Image idleDown;
    private final Image idleUp;
    private final Image idleLeft;
    private final Image idleRight;
    private final Image combatDown;
    private final Image combatUp;
    private final Image combatLeft;
    private final Image combatRight;
    private final Image swordHit;

    private FinalBoss target;
    private Direction direction = Direction.DOWN;
    private boolean attacking = false;
    private boolean damageApplied = false;
    private int animationTimer = 0;
    private int lifetimeTimer = LIFETIME_FRAMES;
    private int attackCooldown = 0;

    public PlayerMinion(double x, double y,
            Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
            Image combatDown, Image combatUp, Image combatLeft, Image combatRight,
            Image swordHit, FinalBoss target) {
        super(x, y, idleDown, PLAYER_FRAMES, RENDER_SIZE, RENDER_SIZE);
        this.idleDown = idleDown;
        this.idleUp = idleUp;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.combatDown = combatDown;
        this.combatUp = combatUp;
        this.combatLeft = combatLeft;
        this.combatRight = combatRight;
        this.swordHit = swordHit;
        this.target = target;
    }

    @Override
    public void update() {
        lifetimeTimer--;
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (!isAlive()) {
            return;
        }

        if (target == null || target.getHp() <= 0) {
            lifetimeTimer = 0;
            return;
        }

        updateDirectionToTarget();
        if (attacking) {
            updateAttack();
        } else if (isTargetInRange()) {
            startAttack();
        } else {
            moveTowardTarget();
            updateIdleAnimation();
        }
    }

    public boolean isAlive() {
        return lifetimeTimer > 0;
    }

    private void updateDirectionToTarget() {
        double dx = getTargetCenterX() - getCenterX();
        double dy = getTargetCenterY() - getCenterY();

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx < 0 ? Direction.LEFT : Direction.RIGHT;
        } else {
            direction = dy < 0 ? Direction.UP : Direction.DOWN;
        }
    }

    private void moveTowardTarget() {
        double dx = getTargetCenterX() - getCenterX();
        double dy = getTargetCenterY() - getCenterY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) {
            return;
        }

        x += (dx / dist) * SPEED;
        y += (dy / dist) * SPEED;
        setIdleSprite();
    }

    private void updateIdleAnimation() {
        animationTimer++;
        if (animationTimer >= ANIMATION_DELAY) {
            animationTimer = 0;
            frameIndex = (frameIndex + 1) % PLAYER_FRAMES;
        }
    }

    private void startAttack() {
        if (attackCooldown > 0) {
            updateIdleAnimation();
            return;
        }

        attacking = true;
        damageApplied = false;
        frameIndex = 0;
        animationTimer = 0;
        setCombatSprite();
    }

    private void updateAttack() {
        animationTimer++;
        if (animationTimer < ATTACK_FRAME_DELAY) {
            return;
        }

        animationTimer = 0;
        frameIndex++;

        if (!damageApplied && frameIndex >= DAMAGE_FRAME && isTargetInRange()) {
            target.takeDamage(ATTACK_DAMAGE);
            damageApplied = true;
        }

        if (frameIndex >= ATTACK_FRAMES) {
            attacking = false;
            attackCooldown = ATTACK_COOLDOWN_FRAMES;
            frameIndex = 0;
            setIdleSprite();
        }
    }

    private boolean isTargetInRange() {
        return getAttackBox().intersects(getTargetHurtBox());
    }

    private Rectangle2D getTargetHurtBox() {
        Rectangle2D bounds = target.getBoundary();
        return new Rectangle2D(
                bounds.getMinX() - TARGET_HURTBOX_PADDING,
                bounds.getMinY() - TARGET_HURTBOX_PADDING,
                bounds.getWidth() + TARGET_HURTBOX_PADDING * 2.0,
                bounds.getHeight() + TARGET_HURTBOX_PADDING * 2.0
        );
    }

    private Rectangle2D getAttackBox() {
        return switch (direction) {
            case UP -> new Rectangle2D(x, y - ATTACK_RANGE, renderWidth, ATTACK_RANGE);
            case DOWN -> new Rectangle2D(x, y + renderHeight, renderWidth, ATTACK_RANGE);
            case LEFT -> new Rectangle2D(x - ATTACK_RANGE, y, ATTACK_RANGE, renderHeight);
            case RIGHT -> new Rectangle2D(x + renderWidth, y, ATTACK_RANGE, renderHeight);
        };
    }

    private void setIdleSprite() {
        spriteSheet = switch (direction) {
            case UP -> idleUp;
            case DOWN -> idleDown;
            case LEFT -> idleLeft;
            case RIGHT -> idleRight;
        };
        isFlipped = direction == Direction.LEFT;
        numFrames = PLAYER_FRAMES;
        frameWidth = spriteSheet.getWidth() / numFrames;
        frameHeight = spriteSheet.getHeight();
    }

    private void setCombatSprite() {
        spriteSheet = switch (direction) {
            case UP -> combatUp;
            case DOWN -> combatDown;
            case LEFT -> combatLeft;
            case RIGHT -> combatRight;
        };
        isFlipped = direction == Direction.LEFT;
        numFrames = ATTACK_FRAMES;
        frameWidth = spriteSheet.getWidth() / numFrames;
        frameHeight = spriteSheet.getHeight();
    }

    @Override
    public void render(GraphicsContext gc) {
        super.render(gc);
        if (attacking) {
            renderSwordHit(gc);
        }
    }

    private void renderSwordHit(GraphicsContext gc) {
        if (swordHit == null) {
            return;
        }

        double swordFrameW = swordHit.getWidth() / ATTACK_FRAMES;
        double swordFrameH = swordHit.getHeight();
        double swordX = x + (renderWidth - SWORD_RENDER_SIZE) / 2.0;
        double swordY = y + (renderHeight - SWORD_RENDER_SIZE) / 2.0;
        double rotation = 0;

        switch (direction) {
            case UP -> {
                swordY = y - SWORD_RENDER_SIZE + SWORD_ANCHOR_OFFSET;
                rotation = -90;
            }
            case DOWN -> {
                swordY = y + renderHeight - SWORD_ANCHOR_OFFSET;
                rotation = 90;
            }
            case LEFT -> {
                swordX = x - SWORD_RENDER_SIZE + SWORD_ANCHOR_OFFSET;
                isFlipped = true;
            }
            case RIGHT -> {
                swordX = x + renderWidth - SWORD_ANCHOR_OFFSET;
                isFlipped = false;
            }
        }

        gc.save();
        gc.translate(swordX + SWORD_RENDER_SIZE / 2.0, swordY + SWORD_RENDER_SIZE / 2.0);
        gc.rotate(rotation);
        if (direction == Direction.LEFT) {
            gc.drawImage(swordHit,
                    frameIndex * swordFrameW, 0, swordFrameW, swordFrameH,
                    SWORD_RENDER_SIZE / 2.0, -SWORD_RENDER_SIZE / 2.0,
                    -SWORD_RENDER_SIZE, SWORD_RENDER_SIZE);
        } else {
            gc.drawImage(swordHit,
                    frameIndex * swordFrameW, 0, swordFrameW, swordFrameH,
                    -SWORD_RENDER_SIZE / 2.0, -SWORD_RENDER_SIZE / 2.0,
                    SWORD_RENDER_SIZE, SWORD_RENDER_SIZE);
        }
        gc.restore();
        isFlipped = direction == Direction.LEFT;
    }

    private double getCenterX() {
        return x + renderWidth / 2.0;
    }

    private double getCenterY() {
        return y + renderHeight / 2.0;
    }

    private double getTargetCenterX() {
        return target.getX() + target.getRenderWidth() / 2.0;
    }

    private double getTargetCenterY() {
        return target.getY() + target.getRenderHeight() / 2.0;
    }
}
