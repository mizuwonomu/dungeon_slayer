package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Tree extends Enemy {
    private int skillCoolDown = 0;
    private final int SKILL_TIMING = 120;

    private Image normalSprite;
    private Image skillSprite;
    private boolean isCastingSkill = false;
    private int skillAnimTimer = 0;
    private int skillFrameIndex = 0;
    private final int SKILL_NUM_FRAMES = 8;
    private boolean hasDealtSkillDamage = false;
    private Image dieSprite;
    private boolean isDying = false;

    public Tree(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer, Image skillSprite) {
        super(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 1.0;
        this.maxHp = 100;
        this.hp = maxHp;
        this.damage = 10;
        this.knockback = 1;

        this.normalSprite = sprSheet;
        this.skillSprite = skillSprite;
        
        try {
            this.dieSprite = new Image(getClass().getResourceAsStream("/assets/enemy/tree_die.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh die của Tree!");
        }
    }

    public Tree(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        this(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer, null);
    }

    @Override
    public void update() {
        if (!isActive) return; // Bất động khi ra ngoài camera
        
        if (this.hp <= 0) {
            if (this.flashTimer > 0) this.flashTimer--;

            this.hitStunTimer = 0; 
            this.isCastingSkill = false;
            
            if (this.flashTimer <= 54) {
                if (!isDying) {
                    isDying = true;
                    if (dieSprite != null) {
                        this.spriteSheet = dieSprite;
                        this.numFrames = 8; 
                        this.frameWidth = dieSprite.getWidth() / 8.0;
                        this.frameHeight = dieSprite.getHeight();
                    }
                    this.frameIndex = 0;
                    this.animationTimer = 0;
                }
                
                this.animationTimer++;
                if (this.animationTimer >= 6) { // Chạy 6 frame game mỗi ảnh
                    this.animationTimer = 0;
                    if (this.frameIndex < 7) {
                        this.frameIndex++;
                    }
                }
            }
            return;
        }

        if (isCastingSkill) {
            if (this.flashTimer > 0)
                this.flashTimer--;
            if (this.attackPauseTimer > 0)
                this.attackPauseTimer--;
            if (this.hitStunTimer > 0)
                this.hitStunTimer--;

            // --- BỔ SUNG KNOCKBACK KHI ĐANG CAST SKILL ---
            this.lastX = this.x;
            this.lastY = this.y;
            if (this.kbTimer > 0) {
                double multiplier = this.kbTimer / 3.5;
                this.kbTimer--;
                this.x += kbVectorX * multiplier;
                this.y += kbVectorY * multiplier;
            }

            
            // Nếu bị đánh trúng và đang bị khựng, gián đoạn skill và quay về animation di chuyển
            if (this.hitStunTimer > 0) {
                this.isCastingSkill = false;
                this.spriteSheet = normalSprite;
                this.frameWidth = normalSprite.getWidth() / 8.0;
                this.frameHeight = normalSprite.getHeight();
                this.frameIndex = 0;
                return;
            }

            skillAnimTimer++;
            if (skillAnimTimer >= animationDelay) {
                skillAnimTimer = 0;
                skillFrameIndex++;
                
                if (skillFrameIndex == 4) { // Frame thứ 5 khi bắt đầu chém
                    com.hust.game.audio.SoundManager.playTreeAtkSound();
                }

                // Tính sát thương vào frame thứ 6 (index 5) để khớp với animation mới
                if (skillFrameIndex == 5 && !hasDealtSkillDamage) {
                    if (isInAttackRange()) {
                        targetPlayer.takeDamage(this.damage, this);
                    }
                    hasDealtSkillDamage = true;
                }

                if (skillFrameIndex >= SKILL_NUM_FRAMES) {
                    isCastingSkill = false;
                    this.spriteSheet = normalSprite;
                    this.frameWidth = normalSprite.getWidth() / 8.0;
                    this.frameHeight = normalSprite.getHeight();
                    this.frameIndex = 0;
                } else {
                    this.frameIndex = skillFrameIndex;
                }
            }
            return; // Khóa di chuyển khi đang cast skill
        }

        if (skillCoolDown > 0) {
            skillCoolDown--;
        }
        if (isInAttackRange()) {
            if (skillCoolDown <= 0) {
                castSkill();
                skillCoolDown = SKILL_TIMING;
                this.attackPauseTimer = 60; // Đứng im 1s (60 frame)
            }
        }

        // Gọi super.update() ở cuối cùng. Logic trong lớp Enemy sẽ tự kiểm tra
        // các trạng thái (như attackPauseTimer > 0) để quyết định có di chuyển hay
        // không.
        super.update();

        // tree_moving: phát vào frame đầu của tree mỗi khi di chuyển
   //     if (this.animationTimer == 0 && this.frameIndex == 0 && (this.moveX != 0 || this.moveY != 0)) {
     //       com.hust.game.audio.SoundManager.playTreeMovingSound();
     //   }
    }

    private boolean isInAttackRange() {
        double attackRange = 45.0; // Tầm đánh mở rộng để có thể dễ dàng chạm Player ở cả trên và dưới
        javafx.geometry.Rectangle2D treeBox = this.getBoundary();
        javafx.geometry.Rectangle2D attackBox = new javafx.geometry.Rectangle2D(
            treeBox.getMinX() - attackRange,
            treeBox.getMinY() - attackRange,
            treeBox.getWidth() + attackRange * 2,
            treeBox.getHeight() + attackRange * 2
        );
        if (!attackBox.intersects(targetPlayer.getBoundary())) {
            return false;
        }
        
        return true;
    }

    private void castSkill() {
        System.out.println("Tree is using skill! " + targetPlayer.getX());

        if (skillSprite != null) {
            isCastingSkill = true;
            skillFrameIndex = 0;
            skillAnimTimer = 0;
            hasDealtSkillDamage = false;

            this.spriteSheet = skillSprite;
            this.frameWidth = skillSprite.getWidth() / SKILL_NUM_FRAMES;
            this.frameHeight = skillSprite.getHeight();
            this.frameIndex = 0;
        } else {
            targetPlayer.takeDamage(this.damage, this);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (this.hp <= 0) {
            gc.save();
            double renderX = this.x;
            double renderY = this.y;
            if (this.isFlipped) renderX += this.renderWidth;
            double renderW = this.isFlipped ? -this.renderWidth : this.renderWidth;

            gc.drawImage(this.spriteSheet,
                this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight,
                renderX, renderY, renderW, this.renderHeight);
            gc.restore();

            // Chớp trắng lúc vừa nhận đòn kết liễu — draw white sprite đè lên, pixel trong suốt không bị ảnh hưởng
            if (this.flashTimer > 54) {
                applyWhiteFlash(gc, 0.9);
            }
        } else {
            super.render(gc);
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        // Cắt hitbox lại 30% bề ngang, 20% bề dọc vì frame 96x96 vẫn có viền trống
        // Giảm bớt padding để hitbox lớn hơn, dễ trúng hơn.
        double paddingX = this.renderWidth * 0.15;
        double paddingY = this.renderHeight * 0.1;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        // Thu nhỏ vùng va chạm dưới chân (giống các quái khác) để Tree lọt vừa qua ô gạch (48px)
        // Từ đó thuật toán tìm đường A* mới có thể dẫn nó đi xuyên qua các hành lang hẹp mà không bị kẹt.
        double w = renderWidth * 0.4;
        double h = renderHeight * 0.2;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }
}