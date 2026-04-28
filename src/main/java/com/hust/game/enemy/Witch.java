package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Witch extends Enemy {

    private EnemyManager enemyManager;

    // Ảnh các kỹ năng
    private Image summonSprite;
    private Image castSprite;
    private Image circleSprite;
    private Image circleAtkSprite;

    // Các cờ trạng thái
    private boolean isSummoning = false;
    private boolean isCastingCircle = false;
    private boolean hasTeleported = false;

    // Quản lý Vòng lửa
    private double circleX, circleY;
    private int circleTimer = 0;

    public Witch(double x, double y, Image idleImg, int numFrames, double renderWidth, double renderHeight,
            Player player, Image skillImg, EnemyManager manager) {
        super(x, y, idleImg, numFrames, renderWidth, renderHeight, player);
        this.speed = 1.0;
        this.maxHp = 100;
        this.hp = this.maxHp;
        this.damage = 15;

        this.enemyManager = manager;
        this.castSprite = skillImg;
        this.summonSprite = idleImg;

        try {
            this.circleSprite = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/enemy/witch_circle.png"));
            this.circleAtkSprite = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/enemy/witch_circle_atk.png"));
        } catch (Exception e) {
            System.err.println("Lỗi nạp ảnh vòng lửa Phù thủy!");
        }
    }

    @Override
    public void update() {
        this.lastX = this.x;
        this.lastY = this.y;

        if (this.flashTimer > 0) {
            this.flashTimer--;
        }

        if (this.hp <= 0) {
            return;
        }
        // 1. CƠ CHẾ DỊCH CHUYỂN
        if (this.hp <= this.maxHp / 2 && this.flashTimer == 14) {

            double angle = Math.random() * 2 * Math.PI;
            double newX = targetPlayer.getX() + 200 * Math.cos(angle);
            double newY = targetPlayer.getY() + 200 * Math.sin(angle);

            double maxW = com.hust.game.constants.GameConstants.MAX_SCREEN_COL
                    * com.hust.game.constants.GameConstants.TILE_SIZE;
            double maxH = com.hust.game.constants.GameConstants.MAX_SCREEN_ROW
                    * com.hust.game.constants.GameConstants.TILE_SIZE;
            this.x = Math.max(0, Math.min(newX, maxW - this.renderWidth));
            this.y = Math.max(0, Math.min(newY, maxH - this.renderHeight));

            return;
        }
        // 2. MÁY TRẠNG THÁI TẤN CÔNG
        if (isSummoning) {
            // Tạm để trống. Bài sau ta sẽ gọi hàm spawnEnemy ở đây
        } else if (isCastingCircle) {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex >= this.numFrames) {
                    this.frameIndex = 0;
                }
            }
            // 2. KỊCH BẢN VÒNG LỬA
            circleTimer++;

            if (circleTimer <= 120) {
                // 2 giây: Bám đuôi Player liên tục
                circleX = targetPlayer.getX() - 24;
                circleY = targetPlayer.getY() - 15;
            } else if (circleTimer <= 150) {
                // 0.5 giây: Khóa mục tiêu (Không cập nhật X, Y nữa -> Vòng lửa dừng lại)
            } else if (circleTimer == 151) {
                // Nổ
                // Tính khoảng cách từ tâm vòng lửa đến Player
                double centerCircleX = circleX + 48;
                double centerCircleY = circleY + 48;

                // Tìm tâm của Player
                double centerPlayerX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2;
                double centerPlayerY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2;

                // Khoảng cách giữa 2 Tâm
                double diffX = centerPlayerX - centerCircleX;
                double diffY = centerPlayerY - centerCircleY;
                double dist = Math.sqrt(diffX * diffX + diffY * diffY);

                // Bán kính nổ
                if (dist < 45) {
                    targetPlayer.takeDamage(30);
                }
            } else if (circleTimer > 170) {
                // Kết thúc
                isCastingCircle = false;
                circleTimer = 0;

                this.spriteSheet = this.summonSprite;
                this.numFrames = 25;
                this.frameWidth = this.spriteSheet.getWidth() / this.numFrames;
                this.frameHeight = this.spriteSheet.getHeight();
                this.frameIndex = 0;
            }
        } else {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex >= this.numFrames) {
                    this.frameIndex = 0;
                }
            }

            double diffX = targetPlayer.getX() - this.x;
            if (diffX < 0) {
                this.isFlipped = true; // Quay trái
            } else {
                this.isFlipped = false; // Quay phải
            }

            circleTimer++;
            if (circleTimer > 180) {
                isCastingCircle = true;
                circleTimer = 0;
                this.spriteSheet = castSprite;
                this.numFrames = 20;
                this.frameWidth = castSprite.getWidth() / this.numFrames;
                this.frameHeight = castSprite.getHeight();
                this.frameIndex = 0;
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.save();
        double centerX = this.x + this.renderWidth / 2.0;
        double centerY = this.y + this.renderHeight / 2.0;
        gc.translate(centerX, centerY);
        gc.scale(3.0, 3.0);
        gc.translate(-centerX, -centerY);

        super.render(gc);
        gc.restore();

        if (isCastingCircle) {
            Image currentCircle;
            int cFrames;
            int cIndex;
            if (circleTimer > 150) {
                // Dùng ảnh nổ
                currentCircle = circleAtkSprite;
                cFrames = 8;
                cIndex = ((circleTimer - 150) / 2) % cFrames;
            } else {
                // Dùng ảnh vòng bình thường
                currentCircle = circleSprite;
                cFrames = 1;
                cIndex = (circleTimer / 5) % cFrames;
            }
            double cWidth = currentCircle.getWidth() / cFrames;
            double cHeight = currentCircle.getHeight();
            double circleSize = 96;
            gc.drawImage(currentCircle, cIndex * cWidth, 0, cWidth, cHeight,
                    circleX, circleY, circleSize, circleSize);
        }
    }

    @Override
    public void onCollision(com.hust.game.entities.base.BaseEntity other) {
        super.onCollision(other);

        // Nếu đụng trúng Player -> Trừ máu Player bằng damage của Witch
        if (other instanceof Player) {
            ((Player) other).takeDamage(this.damage);
        }
    }
}
