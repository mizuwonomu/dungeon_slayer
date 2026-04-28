package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import com.hust.game.main.App;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Witch extends Enemy {

    private int summonTimer = 0;
    private int circleCountSinceLastSummon = 3;
    private EnemyManager enemyManager;
    private Image knightIdle;
    private Image knightAtk;

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
            this.knightIdle = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/enemy/knight_idle.png"));
            this.knightAtk = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/enemy/knight_attack.png"));
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
            // Tự động dạt sang đầu kia của căn phòng
            if (targetPlayer.getX() < App.getGameWidth() / 2) {
                this.x = App.getGameWidth() - 150;
            } else {
                this.x = 100;
            }
            this.y = App.getGameHeight() / 2 - 50;
            return;
        }
        // 2. MÁY TRẠNG THÁI TẤN CÔNG
        if (isSummoning) {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex = (this.frameIndex + 1) % this.numFrames;
            }

            summonTimer++;
            if (summonTimer == 60) {
                int count = 0;
                for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
                    if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0)
                        count++;
                }
                if (count == 0) {
                    enemyManager.spawnEnemy("Knight", this.x - 80, this.y, knightIdle, 8, 96, 96, targetPlayer,
                            knightAtk);
                } else if (count == 1) {
                    enemyManager.spawnEnemy("Knight", this.x + 80, this.y, knightIdle, 8, 96, 96, targetPlayer,
                            knightAtk);
                }
            } else if (summonTimer > 90) {
                isSummoning = false;
                summonTimer = 0;
            }
        } else if (isCastingCircle) {
            circleTimer++;
            // 1. Giai đoạn gồng phép
            if (circleTimer <= 60) {
                this.frameIndex = 0;
            } else {
                // Sau 1 giây thì bắt đầu xả phép
                this.animationTimer++;
                if (this.animationTimer >= this.animationDelay) {
                    this.animationTimer = 0;
                    this.frameIndex++;
                    if (this.frameIndex >= this.numFrames) {
                        this.frameIndex = 0;
                    }
                }
            }
            // 2. Giai đoạn xả Vòng lửa
            if (circleTimer <= 180) {
                // Bám đuôi Player
                circleX = targetPlayer.getX() - 24;
                circleY = targetPlayer.getY() - 15;
            } else if (circleTimer <= 210) {
            } else if (circleTimer == 211) {
                // Phát nổ
                double centerCircleX = circleX + 48;
                double centerCircleY = circleY + 48;
                double centerPlayerX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2;
                double centerPlayerY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2;
                double diffX = centerPlayerX - centerCircleX;
                double diffY = centerPlayerY - centerCircleY;
                double dist = Math.sqrt(diffX * diffX + diffY * diffY);
                if (dist <= 60) { // Phạm vi sát thương
                    targetPlayer.takeDamage(this.damage);
                }
            } else if (circleTimer > 230) {
                isCastingCircle = false;
                circleTimer = 0;
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
            if (circleTimer > 240) {
                circleTimer = 0;

                int knightCount = 0;
                for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
                    if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0) {
                        knightCount++;
                    }
                }

                if (knightCount < 2) {
                    if (circleCountSinceLastSummon >= 3) {
                        isSummoning = true;
                        circleCountSinceLastSummon = 0;

                        this.spriteSheet = this.summonSprite;
                        this.numFrames = 25;
                        this.frameWidth = this.summonSprite.getWidth() / this.numFrames;
                        this.frameHeight = this.summonSprite.getHeight();
                        this.frameIndex = 0;
                    } else {
                        isCastingCircle = true;
                        circleCountSinceLastSummon++;

                        this.spriteSheet = castSprite;
                        this.numFrames = 20;
                        this.frameWidth = castSprite.getWidth() / this.numFrames;
                        this.frameHeight = castSprite.getHeight();
                        this.frameIndex = 0;
                    }
                } else {
                    isCastingCircle = true;
                    circleCountSinceLastSummon = 0;

                    this.spriteSheet = castSprite;
                    this.numFrames = 20;
                    this.frameWidth = castSprite.getWidth() / this.numFrames;
                    this.frameHeight = castSprite.getHeight();
                    this.frameIndex = 0;
                }
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

        if (isCastingCircle && circleTimer > 60) {
            Image currentCircle;
            int cFrames;
            int cIndex;

            if (circleTimer > 210) { // Cũ là 150 -> Mới là 210
                // Vòng lửa phát nổ
                currentCircle = circleAtkSprite;
                cFrames = 8;
                cIndex = ((circleTimer - 210) / 2) % cFrames;
            } else {
                // Vòng lửa đang chạy bám theo người
                currentCircle = circleSprite;
                cFrames = 1;
                cIndex = ((circleTimer - 60) / 5) % cFrames;
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
