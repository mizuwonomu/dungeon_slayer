package com.hust.game.entities.player.merge;

import com.hust.game.entities.Direction;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class PlayerMergeController {
    private static final int TREE_FORM_DURATION_FRAMES = 300;
    private static final int MERGE_COOLDOWN_FRAMES = 360;
    private static final int TREE_IDLE_FRAMES = 8;
    private static final int TREE_ATTACK_FRAMES = 8;
    private static final int TREE_IDLE_DELAY = 10;
    private static final int TREE_ATTACK_DELAY = 6;
    private static final int TREE_DAMAGE_FRAME = 5;
    private static final int TREE_DAMAGE = 10;
    private static final int TREE_ATTACK_COOLDOWN = TREE_ATTACK_FRAMES * TREE_ATTACK_DELAY;
    private static final double TREE_RENDER_SIZE = 96.0;
    private static final double TREE_ATTACK_RANGE = 45.0;

    private MergeFormType storedForm;
    private MergeFormType activeForm;
    private int activeTimer;
    private int cooldownTimer;

    private Image treeIdleSprite;
    private Image treeSkillSprite;
    private Image transformSprite;
    private int transformMode = 0; // 0=None, 1=In, 2=Out
    private int transformFrameIndex;
    private int transformAnimTimer;
    private static final int TRANSFORM_DELAY = 4;

    private int treeFrameIndex;
    private int treeAnimTimer;
    private boolean treeAttacking;

    public void configureTreeSprites(Image treeIdleSprite, Image treeSkillSprite, Image transformSprite) {
        this.treeIdleSprite = treeIdleSprite;
        this.treeSkillSprite = treeSkillSprite;
        this.transformSprite = transformSprite;
    }

    public void grantForm(MergeFormType formType) {
        if (activeForm != null || cooldownTimer > 0) {
            return;
        }
        storedForm = formType;
    }

    public boolean activateStoredForm() {
        if (storedForm == null || activeForm != null) {
            return false;
        }

        activeForm = storedForm;
        storedForm = null;
        activeTimer = TREE_FORM_DURATION_FRAMES;
        treeFrameIndex = 0;
        treeAnimTimer = 0;
        treeAttacking = false;
        
        transformMode = 1;
        transformFrameIndex = 0;
        transformAnimTimer = 0;
        return true;
    }

    public void update() {
        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        if (activeForm == null) {
            return;
        }

        if (transformMode > 0) {
            transformAnimTimer++;
            if (transformAnimTimer >= TRANSFORM_DELAY) {
                transformAnimTimer = 0;
                transformFrameIndex++;
                int totalFrames = (transformSprite != null) ? (int) (transformSprite.getWidth() / 288.0) : 12;
                if (transformFrameIndex >= totalFrames) {
                    if (transformMode == 2) {
                        finishActiveForm();
                        return; // Ended OUT
                    }
                    transformMode = 0;
                }
            }
        }

        if (transformMode != 2) {
            activeTimer--;
            if (activeTimer <= 0) {
                transformMode = 2; // Start OUT
                transformFrameIndex = 0;
                transformAnimTimer = 0;
                com.hust.game.audio.SoundManager.playTransformSound();
                return;
            }
            if (activeForm == MergeFormType.TREE && (transformMode == 0 || transformFrameIndex >= 12)) {
                updateTreeAnimation();
            }
        }
    }

    private void updateTreeAnimation() {
        treeAnimTimer++;
        int delay = treeAttacking ? TREE_ATTACK_DELAY : TREE_IDLE_DELAY;
        if (treeAnimTimer < delay) {
            return;
        }

        treeAnimTimer = 0;
        treeFrameIndex++;

        if (treeAttacking) {
            if (treeFrameIndex >= TREE_ATTACK_FRAMES) {
                treeAttacking = false;
                treeFrameIndex = 0;
            }
        } else if (treeFrameIndex >= TREE_IDLE_FRAMES) {
            treeFrameIndex = 0;
        }
    }

    private void clearActiveForm() {
        activeForm = null;
        activeTimer = 0;
        treeAttacking = false;
        treeFrameIndex = 0;
        treeAnimTimer = 0;
        transformMode = 0;
    }

    private void finishActiveForm() {
        clearActiveForm();
        cooldownTimer = MERGE_COOLDOWN_FRAMES;
    }

    public void clearAll() {
        storedForm = null;
        clearActiveForm();
        cooldownTimer = 0;
    }

    public boolean startTreeAttack() {
        if (!isTreeActive() || treeAttacking) {
            return false;
        }

        treeAttacking = true;
        treeFrameIndex = 0;
        treeAnimTimer = 0;
        return true;
    }

    public boolean isTreeActive() {
        return activeForm == MergeFormType.TREE;
    }

    public boolean hasStoredForm() {
        return storedForm != null;
    }

    public boolean isActive() {
        return activeForm != null;
    }

    public boolean isTreeAttacking() {
        return isTreeActive() && treeAttacking;
    }

    public int getTreeFrameIndex() {
        return treeFrameIndex;
    }

    public int getTreeDamageFrame() {
        return TREE_DAMAGE_FRAME;
    }

    public int getTreeDamage() {
        return TREE_DAMAGE;
    }

    public int getTreeAttackCooldown() {
        return TREE_ATTACK_COOLDOWN;
    }

    public int getActiveTimer() {
        return activeTimer;
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public Rectangle2D getTreeAttackBox(double playerX, double playerY, double playerW, double playerH) {
        Rectangle2D treeBounds = getTreeRenderBounds(playerX, playerY, playerW, playerH);
        return new Rectangle2D(
                treeBounds.getMinX() - TREE_ATTACK_RANGE,
                treeBounds.getMinY() - TREE_ATTACK_RANGE,
                treeBounds.getWidth() + TREE_ATTACK_RANGE * 2,
                treeBounds.getHeight() + TREE_ATTACK_RANGE * 2
        );
    }

    public boolean isTransforming() {
        return transformMode > 0;
    }

    public boolean shouldRenderPlayer() {
        if (activeForm == null) return true;
        if (transformMode == 1 && transformFrameIndex < 12) return true;
        if (transformMode == 2 && transformFrameIndex >= 12) return true;
        return false;
    }

    public boolean shouldRenderTree() {
        if (activeForm == MergeFormType.TREE) {
            if (transformMode == 1 && transformFrameIndex < 12) return false;
            if (transformMode == 2 && transformFrameIndex >= 12) return false;
            return true;
        }
        return false;
    }

    public void renderTransform(GraphicsContext gc, double effectX, double effectY, double effectW, double effectH) {
        if (transformMode == 0 || transformSprite == null) {
            return;
        }

        double frameWidth = 288.0;
        double frameHeight = 288.0;
        double sx = transformFrameIndex * frameWidth;

        gc.drawImage(transformSprite,
                sx, 0, frameWidth, frameHeight,
                effectX, effectY,
                effectW, effectH);
    }

    public void render(GraphicsContext gc, double playerX, double playerY, double playerW, double playerH,
            Direction direction) {
        if (!isTreeActive()) {
            return;
        }

        Image activeSprite = treeAttacking && treeSkillSprite != null ? treeSkillSprite : treeIdleSprite;
        if (activeSprite == null) {
            return;
        }

        int frameCount = treeAttacking ? TREE_ATTACK_FRAMES : TREE_IDLE_FRAMES;
        double frameWidth = activeSprite.getWidth() / frameCount;
        double frameHeight = activeSprite.getHeight();
        int frame = Math.min(treeFrameIndex, frameCount - 1);
        Rectangle2D drawBounds = getTreeRenderBounds(playerX, playerY, playerW, playerH);

        boolean flipped = direction == Direction.LEFT;
        if (flipped) {
            gc.drawImage(activeSprite,
                    frame * frameWidth, 0, frameWidth, frameHeight,
                    drawBounds.getMinX() + drawBounds.getWidth(), drawBounds.getMinY(),
                    -drawBounds.getWidth(), drawBounds.getHeight());
        } else {
            gc.drawImage(activeSprite,
                    frame * frameWidth, 0, frameWidth, frameHeight,
                    drawBounds.getMinX(), drawBounds.getMinY(),
                    drawBounds.getWidth(), drawBounds.getHeight());
        }
    }

    private Rectangle2D getTreeRenderBounds(double playerX, double playerY, double playerW, double playerH) {
        double centerX = playerX + playerW / 2.0;
        double bottomY = playerY + playerH;
        return new Rectangle2D(
                centerX - TREE_RENDER_SIZE / 2.0,
                bottomY - TREE_RENDER_SIZE,
                TREE_RENDER_SIZE,
                TREE_RENDER_SIZE
        );
    }
}
