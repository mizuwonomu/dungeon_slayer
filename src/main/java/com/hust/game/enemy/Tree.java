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
    private final int SKILL_NUM_FRAMES = 4;
    private boolean hasDealtSkillDamage = false;
    private Image shadowSprite;
    private Image dieSprite;
    private boolean isDying = false;
    private static final javafx.scene.effect.ColorAdjust WHITE_EFFECT = new javafx.scene.effect.ColorAdjust(0, 0, 1.0, 0);

    private double[] normalOffsets = new double[8];
    private double[] skillOffsets = new double[4];
    private double[] dieOffsets = new double[8];

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
            this.shadowSprite = new Image(getClass().getResourceAsStream("/assets/enemy/tree_shadow.png"));
            this.dieSprite = new Image(getClass().getResourceAsStream("/assets/enemy/tree_die.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh shadow hoặc die của Tree!");
        }
        
        java.util.Arrays.fill(normalOffsets, Double.NaN);
        java.util.Arrays.fill(skillOffsets, Double.NaN);
        java.util.Arrays.fill(dieOffsets, Double.NaN);
    }

    public Tree(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        this(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer, null);
    }

    private double getAlignOffset() {
        Image currentImg = this.spriteSheet;
        if (currentImg == null) return 0;
        
        double[] targetArray = null;
        if (currentImg == normalSprite) targetArray = normalOffsets;
        else if (currentImg == skillSprite) targetArray = skillOffsets;
        else if (currentImg == dieSprite) targetArray = dieOffsets;
        
        if (targetArray == null || this.frameIndex >= targetArray.length) return 0;
        
        // Chỉ quét ảnh 1 lần duy nhất cho mỗi frame để lưu vào bộ nhớ cache, tiết kiệm tối đa CPU
        if (Double.isNaN(targetArray[this.frameIndex])) {
            javafx.scene.image.PixelReader pr = currentImg.getPixelReader();
            if (pr != null) {
                int fw = (int) (currentImg.getWidth() / targetArray.length);
                int fh = (int) currentImg.getHeight();
                int sx = this.frameIndex * fw;
                
                int minX = fw, maxX = 0;
                for (int y = 0; y < fh; y++) {
                    for (int x = 0; x < fw; x++) {
                        if (sx + x >= currentImg.getWidth() || y >= currentImg.getHeight()) continue;
                        javafx.scene.paint.Color color = pr.getColor(sx + x, y);
                        if (color.getOpacity() > 0.1) { // Chỉ lấy phần pixel có vẽ hình (Không trong suốt)
                            if (x < minX) minX = x;
                            if (x > maxX) maxX = x;
                        }
                    }
                }
                if (minX <= maxX) {
                    double center = (minX + maxX) / 2.0;
                    targetArray[this.frameIndex] = fw - 2 * center; // Tính ra khoảng lùi cần thiết
                } else {
                    targetArray[this.frameIndex] = 0;
                }
            } else {
                targetArray[this.frameIndex] = 0;
            }
        }
        return targetArray[this.frameIndex];
    }

    @Override
    protected void drawShadow(GraphicsContext gc) {
        if (this.hp <= 0) return; // Ẩn bóng khi chết

        if (shadowSprite != null) {
            double sx = this.frameIndex * this.frameWidth;
            double renderX = this.x;
            
            // Bóng KHÔNG BỊ LẬT, nhưng dịch chuyển toạ độ X sao cho phần pixel không trong suốt trùng khớp với Tree
            if (this.isFlipped) {
                double offset = getAlignOffset();
                renderX += offset * (this.renderWidth / this.frameWidth);
            }
            
            gc.drawImage(shadowSprite, sx, 0, this.frameWidth, this.frameHeight, renderX, this.y, this.renderWidth, this.renderHeight);
        }
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
                
                if (skillFrameIndex == 1) { // Frame thứ 2 khi tấn công (index đếm từ 0)
                    com.hust.game.audio.SoundManager.playTreeAtkSound();
                }

                // Tính sát thương vào frame thứ 3 (index 2) thay vì đợi hoạt ảnh kết thúc
                if (skillFrameIndex == 2 && !hasDealtSkillDamage) {
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
        double attackRange = 30.0; // Tầm đánh mở rộng ra bằng với Player
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
        
        // CHỐNG CHÉM XUYÊN TƯỜNG (Raycast từ tâm Quái tới tâm Player)
        if (collisionChecker != null) {
            double pCenterX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0;
            double pCenterY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0;
            double eCenterX = this.x + this.renderWidth / 2.0;
            double eCenterY = this.y + this.renderHeight / 2.0;
            
            for (int i = 1; i <= 5; i++) {
                int checkX = (int) (eCenterX + (pCenterX - eCenterX) * i / 5.0);
                int checkY = (int) (eCenterY + (pCenterY - eCenterY) * i / 5.0);
                if (collisionChecker.checkTile(checkX, checkY)) {
                    return false; // Có tường chắn ở giữa
                }
            }
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
            drawShadow(gc);
            gc.save();
            if (this.flashTimer > 54) gc.setEffect(WHITE_EFFECT); // Lóe trắng lúc vừa nhận đòn
            
            double renderX = this.x;
            double renderY = this.y;
            if (this.isFlipped) renderX += this.renderWidth;
            double renderW = this.isFlipped ? -this.renderWidth : this.renderWidth;
            
            gc.drawImage(this.spriteSheet, this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight, renderX, renderY, renderW, this.renderHeight);
            gc.restore();
        } else {
            super.render(gc);
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        // Cắt hitbox lại 30% bề ngang, 20% bề dọc vì frame mới 104x96 khá rộng
        double paddingX = this.renderWidth * 0.3;
        double paddingY = this.renderHeight * 0.2;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }
}
