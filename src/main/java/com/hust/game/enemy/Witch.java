package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import com.hust.game.main.App;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Witch extends Enemy {

    private int skillCooldownTimer = 0;
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
        this.castSprite = skillImg; // witch_atk.png
        this.summonSprite = idleImg; // witch_summon.png

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
        
        // Thiết lập trạng thái mặc định (Idle)
        resetToIdle();
    }

    private void resetToIdle() {
        isCastingCircle = false;
        isSummoning = false;
        circleTimer = 0;
        this.spriteSheet = castSprite;
        this.numFrames = 7; // IDLE chỉ lấy 7 frame đầu của witch_atk
        this.frameWidth = castSprite.getWidth() / 20.0; // Phải chia cứng 20 vì ảnh thực tế dài 20 frame
        this.frameHeight = castSprite.getHeight();
        this.frameIndex = 0;
    }

    private void decideNextSkill() {
        int knightCount = 0;
        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0) knightCount++;
        }

        if (knightCount < 2 && circleCountSinceLastSummon >= 2) {
            isSummoning = true;
            circleCountSinceLastSummon = 0;
            this.spriteSheet = summonSprite;
            this.numFrames = 25;
            this.frameWidth = summonSprite.getWidth() / 25.0;
            this.frameHeight = summonSprite.getHeight();
            this.frameIndex = 0;
        } else {
            isCastingCircle = true;
            circleCountSinceLastSummon++;
            circleTimer = 0;
            this.spriteSheet = castSprite;
            this.numFrames = 20;
            this.frameWidth = castSprite.getWidth() / 20.0;
            this.frameHeight = castSprite.getHeight();
            this.frameIndex = 0;
        }
    }

    private void spawnKnightsSafely() {
        int knightCount = 0;
        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0) knightCount++;
        }

        // Giới hạn y cho Knight không bị dính vào viền tường (Tile=48, Knight=96)
        double safeY = Math.max(48, Math.min(this.y, App.getGameHeight() - 144));

        if (knightCount == 0) {
            double safeX = Math.max(48, this.x - 80);
            enemyManager.spawnEnemy("Knight", safeX, safeY, knightIdle, 8, 96, 96, targetPlayer, knightAtk);
        } else if (knightCount == 1) {
            double safeX = Math.min(App.getGameWidth() - 144, this.x + 80);
            enemyManager.spawnEnemy("Knight", safeX, safeY, knightIdle, 8, 96, 96, targetPlayer, knightAtk);
        }
    }

    @Override
    public void update() {
        this.lastX = this.x;
        this.lastY = this.y;

        if (this.flashTimer > 0) {
            this.flashTimer--;
        }
        if (this.hitStunTimer > 0) {
            this.hitStunTimer--;
        }

        // Bổ sung Knockback cho Phù thủy để tự nhiên hơn khi bị đánh trúng
        if (this.kbTimer > 0) {
            this.kbTimer--;
            this.x += kbVectorX;
            this.y += kbVectorY;
        }

        if (this.hp <= 0) {
            return;
        }

        // 1. CƠ CHẾ DỊCH CHUYỂN (Chỉ kích hoạt 1 lần khi HP <= 50%)
        if (this.hp <= this.maxHp / 2 && !hasTeleported) {
            hasTeleported = true;
            if (targetPlayer.getX() < App.getGameWidth() / 2) {
                this.x = App.getGameWidth() - 150;
            } else {
                this.x = 100;
            }
            this.y = App.getGameHeight() / 2 - 50;
            return;
        }

        // 2. MÁY TRẠNG THÁI
        // Tính toán khoảng cách bằng TÂM của nhân vật để không bị giật (jitter)
        double pCenterX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0;
        double pCenterY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0;
        double wCenterX = this.x + this.renderWidth / 2.0;
        double wCenterY = this.y + this.renderHeight / 2.0;
        
        double diffX = pCenterX - wCenterX;
        double diffY = pCenterY - wCenterY;

        // Đảo ngược logic lật ảnh: Giả định ảnh Witch gốc quay sang TRÁI (ngược với Slime)
        // Có khoảng trễ 5 pixel để nhân vật không bị lật mặt liên tục khi đứng thẳng hàng
        if (diffX > 5) {
            this.isFlipped = true;  // Quay sang phải
        } else if (diffX < -5) {
            this.isFlipped = false; // Quay sang trái
        }

        if (!isCastingCircle && !isSummoning) {
            double dist = Math.sqrt(diffX * diffX + diffY * diffY);

            // Tầm chiêu là 1/3 màn hình (~250 pixel)
            if (dist > 250) {
                this.moveX = (diffX / dist) * this.speed;
                this.moveY = (diffY / dist) * this.speed;
                this.x += this.moveX;
                this.y += this.moveY;
            } else {
                this.moveX = 0;
                this.moveY = 0;
                skillCooldownTimer++;
                if (skillCooldownTimer > 90) { // Đợi 1.5 giây giữa các lần ra chiêu
                    skillCooldownTimer = 0;
                    decideNextSkill();
                    return;
                }
            }

            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex >= 7) {
                    this.frameIndex = 0;
                }
            }
        } 
        else if (isCastingCircle) {
            circleTimer++;
            
            // 1. Giai đoạn gồng phép (1 giây đầu)
            if (circleTimer <= 60) {
                this.frameIndex = 0;
            } else {
                // Chạy animation cast phép của Witch
                this.animationTimer++;
                if (this.animationTimer >= this.animationDelay) {
                    this.animationTimer = 0;
                    this.frameIndex++;
                    if (this.frameIndex >= this.numFrames) {
                        this.frameIndex = 0;
                    }
                }

                if (circleTimer == 61) { // Vừa gồng xong 1 giây, vòng bắt đầu đuổi
                    com.hust.game.audio.SoundManager.playWitchCircleFollowSound();
                }
            }
            
            // 2. Giai đoạn xử lý Vòng lửa
            if (circleTimer <= 180) {
                // Vòng lửa bám đuôi Player
                circleX = targetPlayer.getX() - 24;
                circleY = targetPlayer.getY() - 15;
            } else if (circleTimer <= 210) {
                // Vòng dừng lại khóa mục tiêu (chuẩn bị nổ)
            } else if (circleTimer == 211) {
                com.hust.game.audio.SoundManager.playWitchCircleExplodeSound();
                // Phát nổ gây sát thương
                double cDiffX = (targetPlayer.getX() + targetPlayer.getRenderWidth() / 2) - (circleX + 48);
                double cDiffY = (targetPlayer.getY() + targetPlayer.getRenderHeight() / 2) - (circleY + 48);
                if (Math.sqrt(cDiffX * cDiffX + cDiffY * cDiffY) <= 60) {
                    targetPlayer.takeDamage(this.damage);
                }
            } else if (circleTimer > 230) {
                resetToIdle();
            }
        } 
        else if (isSummoning) {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                
                if (this.frameIndex == 8) { // Frame số 9 (đếm từ 1) bắt đầu đọc chú
                    com.hust.game.audio.SoundManager.playWitchSummonSound();
                }
                if (this.frameIndex == 18) {
                    spawnKnightsSafely(); // Đạt mốc frame 18 thì quái xuất hiện
                } 
                else if (this.frameIndex >= 25) {
                    resetToIdle();
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

        // Render Vòng lửa dựa theo bộ đếm circleTimer
        if (isCastingCircle && circleTimer > 60) {
            Image currentCircle;
            int cFrames;
            int cIndex;

            if (circleTimer > 210) {
                // Vòng nổ
                currentCircle = circleAtkSprite;
                cFrames = 8;
                cIndex = ((circleTimer - 210) / 2) % cFrames;
            } else {
                // Vòng đang xoay
                currentCircle = circleSprite;
                cFrames = 1;
                cIndex = ((circleTimer - 60) / 5) % cFrames;
            }

            double cWidth = currentCircle.getWidth() / cFrames;
            gc.drawImage(currentCircle, cIndex * cWidth, 0, cWidth, currentCircle.getHeight(),
                    circleX, circleY, 96, 96);
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
