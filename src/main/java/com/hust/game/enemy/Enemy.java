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

        // Lưu vị trí cũ CHỈ KHI được phép di chuyển (Giúp knockback không bị kẹt tường)
        this.lastX = this.x;
        this.lastY = this.y;

        // Nếu đang bị knockback thì ưu tiên trượt lùi và KHÔNG thực hiện AI rượt đuổi
        if (kbTimer > 0) {
            kbTimer--;
            this.x += kbVectorX;
            this.y += kbVectorY;
            return;
        }

        // Nếu cạn máu, hoặc trong thời gian nghỉ sau đòn đánh -> Đứng im
        if (hp <= 0 || attackPauseTimer > 0) {
            return; // Thoát sớm sau khi đã tính knockback
        }

        // Không có mục tiêu -> đứng
        if (targetPlayer == null)
            return;

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

        double nextX = this.x + this.moveX;
        double nextY = this.y + this.moveY;

        int TILE_SIZE = 48;

        int nextCol = (int) (nextX / TILE_SIZE);
        int nextRow = (int) (nextY / TILE_SIZE);

        this.x = nextX;
        this.y = nextY;

        animationTimer++;
        if (animationTimer >= animationDelay) {
            animationTimer = 0;
            frameIndex = (frameIndex + 1) % numFrames;
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

    // Tạo sẵn hiệu ứng tĩnh (static) để GPU không bị quá tải và rớt khung hình
    private static final javafx.scene.effect.ColorAdjust RED_EFFECT = new javafx.scene.effect.ColorAdjust(-0.15, 0.8, 0.1, 0);
    private static final javafx.scene.effect.ColorAdjust SLIME_RED_EFFECT = new javafx.scene.effect.ColorAdjust(0.8, 0.8, 0.1, 0);

    @Override
    public void render(GraphicsContext gc) {
        gc.save(); // Lưu trạng thái GraphicsContext hiện tại

        // Hiệu ứng đổi màu đỏ khi bị chém trúng (Sử dụng lại ColorAdjust)
        if (flashTimer > 0) {
            if (this instanceof Slime) {
                gc.setEffect(SLIME_RED_EFFECT); // Slime màu xanh dương -> Dịch hue +0.8
            } else {
                gc.setEffect(RED_EFFECT);
            }
        }

        super.render(gc); // Gọi render của lớp cha để vẽ sprite của quái vật

        gc.restore(); // Khôi phục trạng thái GraphicsContext (gỡ bỏ hiệu ứng ColorAdjust)
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
        if (this.hp < 0)
            this.hp = 0;
        this.flashTimer = 15; // Kích hoạt hiệu ứng đỏ trong 15 frames (~0.25 giây) cho dứt khoát
        System.out.println("Quái vật bị chém trúng! Máu còn: " + this.hp + "/" + this.maxHp);
    }

    // Hàm xử lý đẩy lùi quái vật (Knockback)
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        this.kbTimer = 6; // Bị đẩy lùi trượt đi trong 6 frames liên tiếp
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
