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

        // Nếu đang chịu đòn, cạn máu, hoặc trong thời gian nghỉ sau đòn đánh -> Đứng im
        if (flashTimer > 0 || hp <= 0 || attackPauseTimer > 0) {
            return; // Thoát sớm, giữ nguyên lastX, lastY an toàn cũ
        }

        // Lưu vị trí cũ CHỈ KHI được phép di chuyển (Giúp knockback không bị kẹt tường)
        this.lastX = this.x;
        this.lastY = this.y;

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

    @Override
    public void render(GraphicsContext gc) {
        // Hiệu ứng nhấp nháy khi bị chém trúng
        if (flashTimer > 0) {
            // Cứ mỗi 7 frame thì ẩn ảnh 1 lần (Tạo ra 2 nhịp nháy trong khoảng 30 frame)
            if ((flashTimer / 7) % 2 == 0) {
                return; // Không vẽ ảnh ở frame này
            }
        }
        super.render(gc);
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

    // Hàm nhận sát thương từ Player
    public void takeDamage(int amount) {
        if (this.hp <= 0)
            return; // Nếu đã chết thì không nhận thêm sát thương nữa
        this.hp -= amount;
        if (this.hp < 0)
            this.hp = 0;
        this.flashTimer = 30; // Kích hoạt nhấp nháy trong 30 frames (0.5 giây)
        System.out.println("Quái vật bị chém trúng! Máu còn: " + this.hp + "/" + this.maxHp);
    }

    // Hàm xử lý đẩy lùi quái vật (Knockback)
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        double kbDistance = this.knockback * 15.0; // Khoảng cách đẩy lùi
        switch (dir) {
            case UP:
                this.y -= kbDistance;
                break;
            case DOWN:
                this.y += kbDistance;
                break;
            case LEFT:
                this.x -= kbDistance;
                break;
            case RIGHT:
                this.x += kbDistance;
                break;
        }
    }

    // Kiểm tra xem quái vật đã chết và chạy xong hiệu ứng nhấp nháy báo tử chưa
    public boolean isReadyToRemove() {
        return this.hp <= 0 && this.flashTimer <= 0;
    }
}
