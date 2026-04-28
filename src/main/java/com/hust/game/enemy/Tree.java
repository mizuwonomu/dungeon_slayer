package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Tree extends Enemy {
    private int skillCoolDown = 0;
    private final int SKILL_TIMING = 120;

    private Image normalSprite;
    private Image skillSprite;
    private boolean isCastingSkill = false;
    private int skillAnimTimer = 0;
    private int skillFrameIndex = 0;
    private final int SKILL_NUM_FRAMES = 8;

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
    }

    public Tree(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        this(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer, null);
    }

    @Override
    public void update() {
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
                this.kbTimer--;
                this.x += kbVectorX;
                this.y += kbVectorY;
            }

            // Chết khi đang cast skill -> giữ nguyên frame hiện tại và không làm gì nữa
            if (this.hp <= 0) {
                return;
            }
            
            // Nếu bị đánh trúng và đang bị khựng, gián đoạn skill và quay về animation di chuyển
            if (this.hitStunTimer > 0) {
                this.isCastingSkill = false;
                this.spriteSheet = normalSprite;
                this.frameWidth = normalSprite.getWidth() / this.numFrames;
                this.frameHeight = normalSprite.getHeight();
                this.frameIndex = 0;
                return;
            }

            skillAnimTimer++;
            if (skillAnimTimer >= animationDelay) {
                skillAnimTimer = 0;
                skillFrameIndex++;
                
                if (skillFrameIndex == 1) { // Frame thứ 2 khi tấn công (index đếm từ 0)
                    com.hust.game.audio.SoundManager.playTreeAtkSound();
                }

                if (skillFrameIndex >= SKILL_NUM_FRAMES) {
                    isCastingSkill = false;
                    this.spriteSheet = normalSprite;
                    this.frameWidth = normalSprite.getWidth() / this.numFrames;
                    this.frameHeight = normalSprite.getHeight();
                    this.frameIndex = 0;

                    // Chỉ gây sát thương nếu Player không kịp né
                    if (this.intersects(targetPlayer)) {
                        targetPlayer.takeDamage(this.damage);
                    }
                } else {
                    this.frameIndex = skillFrameIndex;
                }
            }
            return; // Khóa di chuyển khi đang cast skill
        }

        if (skillCoolDown > 0) {
            skillCoolDown--;
        }
        if (this.intersects(targetPlayer)) {
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

    private void castSkill() {
        System.out.println("Tree is using skill! " + targetPlayer.getX());

        if (skillSprite != null) {
            isCastingSkill = true;
            skillFrameIndex = 0;
            skillAnimTimer = 0;

            this.spriteSheet = skillSprite;
            this.frameWidth = skillSprite.getWidth() / SKILL_NUM_FRAMES;
            this.frameHeight = skillSprite.getHeight();
            this.frameIndex = 0;
        } else {
            targetPlayer.takeDamage(this.damage);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (isCastingSkill) {
            gc.save();

            // Tính tâm của quái vật để phóng to từ giữa ra
            double centerX = this.x + this.getRenderWidth() / 2.0;
            double centerY = this.y + this.getRenderHeight() / 2.0;

            // Scale hình ảnh 2x cho hiển thị
            gc.translate(centerX, centerY);
            gc.scale(2.0, 2.0);
            gc.translate(-centerX, -centerY);

            super.render(gc);

            gc.restore();
        } else {
            super.render(gc);
        }
    }
}
