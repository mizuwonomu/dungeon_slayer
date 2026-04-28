package com.hust.game.enemy;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.base.MovingEntity;
import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public abstract class Enemy extends MovingEntity {

    protected Player targetPlayer;
    protected int maxHp;
    protected int hp;
    protected int damage;
    protected int knockback;
    protected int animationTimer = 0;
    protected int animationDelay = 10;
    protected double lastX, lastY;
    protected double moveX, moveY;
    protected int flashTimer = 0; // Bộ đếm nhấp nháy khi dính đòn
    protected int attackPauseTimer = 0; // Bộ đếm thời gian nghỉ sau khi tấn công
    protected int hitStunTimer = 0; // Thời gian khựng lại khi bị đánh trúng

    // Các biến phục vụ thuật toán lách vật cản (Wall Sliding)
    protected int dodgeTimer = 0;
    protected boolean dodgeAxisX = true;
    protected double dodgeDirX = 0;
    protected double dodgeDirY = 0;

    // Các biến phục vụ Knockback mượt (Smooth Knockback)
    protected int kbTimer = 0;
    protected double kbVectorX = 0;
    protected double kbVectorY = 0;

    // Constructor tạm thời ở Giai đoạn 1
    public Enemy(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, 1.0);
        this.targetPlayer = targetPlayer; // Chốt mục tiêu
    }

    @Override
    public void update() {
        if (flashTimer > 0)
            flashTimer--; // Giảm dần thời gian nháy đỏ
        if (attackPauseTimer > 0)
            attackPauseTimer--; // Giảm thời gian nghỉ
        if (hitStunTimer > 0)
            hitStunTimer--; // Giảm thời gian khựng

        this.lastX = this.x;
        this.lastY = this.y;

        // --- Logic di chuyển & AI ---
        if (kbTimer > 0) {
            // Trạng thái: Bị đẩy lùi
            kbTimer--;
            this.x += kbVectorX;
            this.y += kbVectorY;
        } else if (hp > 0 && attackPauseTimer <= 0 && hitStunTimer <= 0 && targetPlayer != null) {
            // Trạng thái: AI hoạt động (đuổi theo player)
            double playerX = targetPlayer.getX();
            double playerY = targetPlayer.getY();

            // Thuật toán nội suy tìm tọa độ Player
            double diffX = playerX - this.x;
            double diffY = playerY - this.y;

            double distance = Math.sqrt(diffX * diffX + diffY * diffY);

            this.moveX = 0;
            this.moveY = 0;

            if (distance > 0) {
                // Nếu đang trong trạng thái lách vật cản, ưu tiên đi men theo 1 trục
                if (dodgeTimer > 0) {
                    dodgeTimer--;
                    this.moveX = dodgeDirX * speed;
                    this.moveY = dodgeDirY * speed;
                } else {
                    this.moveX = (diffX / distance) * speed;
                    this.moveY = (diffY / distance) * speed;
                }

                // Xoay mặt nhìn theo Player (Mirror ảnh ngang)
                if (diffX < 0) {
                    this.isFlipped = true; // Đi sang trái thì lật mặt
                } else if (diffX > 0) {
                    this.isFlipped = false; // Đi sang phải thì giữ nguyên
                }
            }
            this.x += this.moveX;
            this.y += this.moveY;
        }
        // else: Trạng thái Chết, Nghỉ, hoặc không có mục tiêu -> Đứng im, không làm gì cả.

        // --- Logic animation ---
        // Chỉ tiếp tục chạy hoạt ảnh nếu quái vật còn sống
        if (this.hp > 0 && hitStunTimer <= 0) {
            animationTimer++;
            if (animationTimer >= animationDelay) {
                animationTimer = 0;
                frameIndex = (frameIndex + 1) % numFrames;
            }
        } else if (hitStunTimer > 0) {
            this.frameIndex = 0; // Khựng lại, ép hiển thị ở frame đầu tiên
        }
    }

    public void onCollision(BaseEntity other) {
        this.x = lastX;
        this.y = lastY;

        // Thuật toán trượt tường (Sliding) và lách nhau
        dodgeAxisX = !dodgeAxisX; // Đổi trục di chuyển để thử lách hướng khác
        dodgeTimer = (other == null) ? 20 : 10; // Đập tường thì trượt lâu hơn (20 frame), đụng nhau thì lách nhanh (10
                                                // frame)

        if (targetPlayer == null)
            return;

        double pX = targetPlayer.getX() - this.x;
        double pY = targetPlayer.getY() - this.y;

        if (dodgeAxisX) {
            // Cố định hướng trượt theo trục X
            dodgeDirX = (Math.abs(pX) > 1) ? Math.signum(pX) : (Math.random() > 0.5 ? 1 : -1);
            dodgeDirY = 0;
        } else {
            // Cố định hướng trượt theo trục Y
            dodgeDirX = 0;
            dodgeDirY = (Math.abs(pY) > 1) ? Math.signum(pY) : (Math.random() > 0.5 ? 1 : -1);
        }
    }

    // --- GETTERS DÀNH CHO TRƯỢT TƯỜNG (SLIDING) TẠI APP.JAVA ---
    public double getLastX() {
        return lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public double getMoveX() {
        return moveX;
    }

    public double getMoveY() {
        return moveY;
    }

    public int getDamage() {
        return this.damage;
    }

    public int getHp() {
        return this.hp;
    }

    // Hàm nhận sát thương từ Player
    public void takeDamage(int amount) {
        if (this.hp <= 0)
            return; // Nếu đã chết thì không nhận thêm sát thương nữa
        this.hp -= amount;
        
        if (this.hp <= 0) {
            this.hp = 0;
            this.flashTimer = 60; // Quái chết -> Tồn tại thêm 60 frames (1 giây) để chạy hiệu ứng mờ dần
        } else {
            this.flashTimer = 15; // Quái còn sống -> Chỉ nháy đỏ 15 frames (~0.25 giây)
        }
        
        com.hust.game.ui.DamageTextManager.addText(this, this.x + renderWidth / 2 - 10, this.y, "-" + amount, javafx.scene.paint.Color.WHITE);
        System.out.println("Quái vật bị chém trúng! Máu còn: " + this.hp + "/" + this.maxHp);
    }

    @Override
    public void render(GraphicsContext gc) {
        // Nếu quái vật đã chết, kích hoạt hiệu ứng mờ dần (Fade out)
        if (this.hp <= 0) {
            gc.save();
            double alpha = (double) this.flashTimer / 60.0;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;
            
            gc.setGlobalAlpha(alpha);
            super.render(gc); // Vẽ quái vật với độ mờ giảm dần
            gc.restore();
        } else {
            super.render(gc); // Quái còn sống thì vẽ bình thường
        }
    }

    // Hàm xử lý đẩy lùi quái vật (Knockback)
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        this.kbTimer = 6; // Bị đẩy lùi trượt đi trong 6 frames liên tiếp
        this.hitStunTimer = 30; // Bị khựng lại (stun) trong 0.5s (30 frame)
        this.frameIndex = 0; // Reset về frame animation đầu tiên
        double kbSpeed = this.knockback * 2.5; // Vận tốc đẩy mỗi frame
        this.kbVectorX = 0;
        this.kbVectorY = 0;
        switch (dir) {
            case UP: this.kbVectorY = -kbSpeed; break;
            case DOWN: this.kbVectorY = kbSpeed; break;
            case LEFT: this.kbVectorX = -kbSpeed; break;
            case RIGHT: this.kbVectorX = kbSpeed; break;
        }
    }

    // Kiểm tra xem quái vật đã chết và chạy xong hiệu ứng nhấp nháy báo tử chưa
    public boolean isReadyToRemove() {
        return this.hp <= 0 && this.flashTimer <= 0;
    }
}
