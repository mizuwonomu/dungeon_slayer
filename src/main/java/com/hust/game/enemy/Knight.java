package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;

public class Knight extends Enemy {

    private int dashCooldownTimer = 120;
    private final int MAX_DASH_COOLDOWN = 180;
    private static final int DASH_DAMAGE_COOLDOWN_FRAMES = 30;
    private int MAX_DASH_DURATION = 30;
    private Image normalSprite;
    private Image skillSprite;
    private Image walkSprite;
    private boolean isDashing = false;
    private Image dieSprite;
    private boolean isDying = false;
    private double dashVectorX, dashVectorY;

    public Knight(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer, Image skillSprite) {
        super(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 1.2;
        this.maxHp = 150;
        this.hp = maxHp;
        this.damage = 30;
        this.knockback = 5;

        this.normalSprite = sprSheet;
        this.skillSprite = skillSprite;
        
        try {
            this.dieSprite = new Image(getClass().getResourceAsStream("/assets/enemy/knight_die.png"));
            this.walkSprite = new Image(getClass().getResourceAsStream("/assets/enemy/knight_walk.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh die/walk của Knight!");
        }
    }

    public Knight(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        this(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer, null);
    }

    // Hàm kiểm tra vùng lướt (hình bình hành) không có tường cản giữa Knight và Player
    private boolean hasClearDashPath(double startX, double startY, double startH, double endX, double endY, double endH) {
        if (collisionChecker == null) return true;
        
        // Số lượng đường thẳng nằm ngang cần quét từ đỉnh tới đáy
        double maxHeight = Math.max(startH, endH);
        // Quét mỗi khoảng TILE_SIZE/4 (12 pixel) để đảm bảo quét kín hình bình hành, không lọt khe tường
        int lineSteps = (int) (maxHeight / (com.hust.game.constants.GameConstants.TILE_SIZE / 4.0));
        lineSteps = Math.max(2, lineSteps); // Ít nhất 2 đường: trên đỉnh và dưới chân

        for (int j = 0; j <= lineSteps; j++) {
            double currentStartY = startY + (startH * j) / lineSteps;
            double currentEndY = endY + (endH * j) / lineSteps;

            double dx = endX - startX;
            double dy = currentEndY - currentStartY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance == 0) continue;

            // Dọc theo mỗi đường thẳng, quét các điểm cách nhau nửa ô TILE
            int pointSteps = (int) (distance / (com.hust.game.constants.GameConstants.TILE_SIZE / 2.0));
            pointSteps = Math.max(1, pointSteps);

            for (int i = 0; i <= pointSteps; i++) {
                double checkX = startX + (dx * i) / pointSteps;
                double checkY = currentStartY + (dy * i) / pointSteps;
                
                // Nếu bất kỳ phần nào của hình bình hành này cắt vào tường -> Bị cản
                if (collisionChecker.checkTile((int) checkX, (int) checkY)) {
                    return false; 
                }
            }
        }
        return true;
    }

    // Hàm tính toán trước khoảng cách dịch chuyển để xem có chạm được Player hay không
    private boolean canDashHitPlayer(double distance) {
        // Yêu cầu Player phải ở gần hơn một chút so với quãng đường lướt thực tế (300)
        // Điều này giúp Knight khi lướt sẽ bay xuyên qua người Player thay vì dừng lại ngay trước mặt
        double CHECK_DISTANCE = this.speed * 220; 
        // Nới rộng vòng tròn tính khoảng cách vì hitbox gây damage giờ là toàn bộ hình ảnh
        double hitRange = this.renderWidth * 1.5; 
        return distance <= (CHECK_DISTANCE + hitRange);
    }

    @Override
    public void update() {
        updatePlayerDamageCooldown();

        if (this.hp > 0) {
            if (this.flashTimer > 0) {
                this.flashTimer--;
            }
            if (this.hitStunTimer > 0) {
                this.hitStunTimer--;
            }
        }

        // --- BỔ SUNG LOGIC KNOCKBACK MÀ TRƯỚC ĐÓ BỊ THIẾU ---
        this.lastX = this.x;
        this.lastY = this.y;

        if (this.hp <= 0) {
            this.hitStunTimer = 0;
            this.isDashing = false;
            
            if (!isDying) {
                isDying = true;
                if (dieSprite != null) {
                    this.spriteSheet = dieSprite;
                    this.numFrames = 6;
                    this.frameWidth = dieSprite.getWidth() / 6.0;
                    this.frameHeight = dieSprite.getHeight();
                }
                this.frameIndex = 0;
                this.animationTimer = 0;
            }
            
            // Chớp trắng 6 frame đầu tiên
            if (this.flashTimer > 54) {
                this.flashTimer--;
            }

            // Chạy animation chết đến frame cuối
            if (this.frameIndex < 5) {
                this.animationTimer++;
                if (this.animationTimer >= 9) { // 54 / 6 = 9 frames game mỗi ảnh
                    this.animationTimer = 0;
                    this.frameIndex++;
                }
            } else {
                // Đã đến frame cuối cùng, bắt đầu mờ dần (~1s)
                if (this.flashTimer > 0 && this.flashTimer <= 54) {
                    this.flashTimer--;
                }
            }
            return;
        }
        
        if (this.kbTimer > 0) {
            double multiplier = this.kbTimer / 3.5;
            this.kbTimer--;
            this.x += kbVectorX * multiplier;
            this.y += kbVectorY * multiplier;
        }
        if (this.kbTimer > 0 || this.hitStunTimer > 0) {
            return; // Còn sống nhưng đang bị knockback hoặc choáng thì ngắt AI lướt/đi bộ
        }
        // ----------------------------------------------------

        if (!isDashing && !this.isImmobile && !isPlayerWithinDetectionRange()) {
            this.moveX = 0;
            this.moveY = 0;
            if (normalSprite != null && this.spriteSheet != normalSprite) {
                this.spriteSheet = normalSprite;
                this.numFrames = 8;
                this.frameWidth = normalSprite.getWidth() / this.numFrames;
                this.frameHeight = normalSprite.getHeight();
                this.frameIndex = 0;
            }
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex = (this.frameIndex + 1) % this.numFrames;
            }
            return;
        }

        if (dashCooldownTimer > 0) {
            dashCooldownTimer--;
        }
        if (!isDashing) {
            // Lấy tọa độ Tâm của quái vật và tâm của người chơi để ngắm chuẩn xác
            double kVisCenterX = this.x + this.renderWidth / 2.0;
            double kVisCenterY = this.y + this.renderHeight / 2.0;
            double pVisCenterX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0;
            double pVisCenterY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0;

            double diffX = pVisCenterX - kVisCenterX;
            double diffY = pVisCenterY - kVisCenterY;
            double distance = Math.sqrt(diffX * diffX + diffY * diffY);
            
            boolean canSeePlayer = false;
            boolean canHitPlayer = false;
            if (distance > 0 && distance < 600) { // Knight có thể phóng ở một khoảng cách hợp lý
                canSeePlayer = hasClearDashPath(kVisCenterX, this.y, this.renderHeight, pVisCenterX, targetPlayer.getY(), targetPlayer.getRenderHeight());
                canHitPlayer = canDashHitPlayer(distance);
            }

            // NẾU CÓ THỂ PHÓNG TRÚNG VÀ KHÔNG BỊ CẢN -> LƯỚT
            if (dashCooldownTimer == 0 && canSeePlayer && canHitPlayer && !this.isImmobile) {
                if (distance > 0) {
                    double TOTAL_DASH_DISTANCE = this.speed * 300; // Quãng đường lướt thực tế dài hơn để đâm xuyên qua
                    // Chia cho animationDelay vì hành động lướt lặp lại trong 10 frame game
                    dashVectorX = (diffX / distance) * (TOTAL_DASH_DISTANCE / this.animationDelay);
                    dashVectorY = (diffY / distance) * (TOTAL_DASH_DISTANCE / this.animationDelay);
                }
                this.isFlipped = diffX < 0;
                isDashing = true;
                com.hust.game.audio.SoundManager.playKnightReadySound();
                this.spriteSheet = skillSprite;
                this.numFrames = 14;
                this.frameWidth = skillSprite.getWidth() / this.numFrames;
                this.frameHeight = skillSprite.getHeight();
                this.frameIndex = 0;
                this.animationTimer = 0;
            } else {
                // NẾU TRONG TẦM LƯỚT VÀ KHÔNG BỊ TƯỜNG CẢN NHƯNG CHƯA HỒI CHIÊU -> ĐỨNG IM CHỜ ĐỢI
                if (canSeePlayer && canHitPlayer) {
                    this.moveX = 0;
                    this.moveY = 0;
                    if (diffX < 0) this.isFlipped = true;
                    else if (diffX > 0) this.isFlipped = false;
                } 
                // NẾU NGOÀI TẦM LƯỚT HOẶC BỊ TƯỜNG CẢN -> ĐI BỘ LẠI GẦN THEO PATHFINDER
                else if (!this.isImmobile) {
                    javafx.geometry.Rectangle2D pCol = targetPlayer.getCollisionBoundary();
                    double pCenterX = pCol.getMinX() + pCol.getWidth() / 2.0;
                    double pCenterY = pCol.getMinY() + pCol.getHeight() / 2.0;
                    javafx.geometry.Rectangle2D bounds = this.getCollisionBoundary();
                    double currentCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
                    double currentCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;

                    pathUpdateTimer++;
                    if (pathUpdateTimer >= 30) {
                        pathUpdateTimer = 0;
                        int goalCol = (int) (pCenterX / com.hust.game.constants.GameConstants.TILE_SIZE);
                        int goalRow = (int) (pCenterY / com.hust.game.constants.GameConstants.TILE_SIZE);
                        searchPath(goalCol, goalRow);
                    }

                    this.moveX = 0;
                    this.moveY = 0;
                    if (distance > 0) {
                        if (dodgeTimer > 0) {
                            dodgeTimer--;
                            this.moveX = dodgeDirX * speed;
                            this.moveY = dodgeDirY * speed;
                            if (dodgeDirX < 0) this.isFlipped = true;
                            else if (dodgeDirX > 0) this.isFlipped = false;
                        } else if (onPath && pathFinder != null && pathFinder.pathList.size() > 0) {
                            int nextCol = pathFinder.pathList.get(0).col;
                            int nextRow = pathFinder.pathList.get(0).row;
                            double tileCenterX = nextCol * com.hust.game.constants.GameConstants.TILE_SIZE + com.hust.game.constants.GameConstants.TILE_SIZE / 2.0;
                            double tileCenterY = nextRow * com.hust.game.constants.GameConstants.TILE_SIZE + com.hust.game.constants.GameConstants.TILE_SIZE / 2.0;
                            double dx = tileCenterX - currentCenterX;
                            double dy = tileCenterY - currentCenterY;
                            double distToNext = Math.sqrt(dx * dx + dy * dy);
                            if (distToNext > speed) {
                                this.moveX = (dx / distToNext) * speed;
                                this.moveY = (dy / distToNext) * speed;
                            } else {
                                pathFinder.pathList.remove(0);
                            }
                            if (this.moveX < 0) this.isFlipped = true;
                            else if (this.moveX > 0) this.isFlipped = false;
                        } else {
                            this.moveX = (diffX / distance) * speed;
                            this.moveY = (diffY / distance) * speed;
                            if (diffX < 0) this.isFlipped = true;
                            else if (diffX > 0) this.isFlipped = false;
                        }
                    }
                    this.x += this.moveX;
                    this.y += this.moveY;
                }

                // Cập nhật Animation Đi bộ hoặc Đứng im
                Image targetSprite = (this.moveX != 0 || this.moveY != 0) && walkSprite != null ? walkSprite : normalSprite;
                if (this.spriteSheet != targetSprite) {
                    this.spriteSheet = targetSprite;
                    this.numFrames = 8;
                    this.frameWidth = targetSprite.getWidth() / this.numFrames;
                    this.frameHeight = targetSprite.getHeight();
                    this.frameIndex = 0;
                }

                this.animationTimer++;
                if (this.animationTimer >= this.animationDelay) {
                    this.animationTimer = 0;
                    this.frameIndex = (this.frameIndex + 1) % this.numFrames;
                }
            }
        } else {
            this.animationTimer++;
            if (this.animationTimer >= this.animationDelay) {
                this.animationTimer = 0;
                this.frameIndex++;
                if (this.frameIndex == 8) { // Khoảnh khắc chuẩn bị lao đi
                    com.hust.game.audio.SoundManager.playKnightAtkSound();
                }
            }
            if (this.frameIndex == 8) { // Lướt diễn ra liên tục trong animationDelay (10) frame game
                this.lastX = this.x;
                this.lastY = this.y;

                this.x += dashVectorX;
                this.y += dashVectorY;
            }
            
            // Tự động kiểm tra sát thương bằng Hitbox toàn thân (getBoundary) 
            // thay vì phụ thuộc vào Hitbox vật lý nhỏ dưới chân
            if (this.isDealingDamage()) {
                if (this.getBoundary().intersects(targetPlayer.getBoundary())) {
                    tryDamagePlayer(targetPlayer, this.damage, DASH_DAMAGE_COOLDOWN_FRAMES);
                }
            }

            if (this.frameIndex >= this.numFrames) {
                isDashing = false;
                this.spriteSheet = normalSprite;
                this.numFrames = 8;
                this.frameWidth = normalSprite.getWidth() / this.numFrames;
                this.frameHeight = normalSprite.getHeight();
                this.frameIndex = 0;
                dashCooldownTimer = MAX_DASH_COOLDOWN;
            }
        }
    }

    public boolean isDashing() {
        return this.isDashing;
    }

    public boolean isDealingDamage() {
        // Gây sát thương ở các frame 7, 8 và 9 (Nới rộng hitbox ra xung quanh thời điểm lướt)
        return this.isDashing && this.frameIndex >= 7 && this.frameIndex <= 9;
    }

    @Override
    public void render(GraphicsContext gc) {
        if (this.hp <= 0) {
            gc.save();
            // Tính toán alpha để mờ dần (từ 54 về 0)
            double alpha = this.flashTimer / 54.0;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;
            gc.setGlobalAlpha(alpha);

            double renderX = this.x;
            double renderY = this.y;
            if (this.isFlipped) renderX += this.renderWidth;
            double renderW = this.isFlipped ? -this.renderWidth : this.renderWidth;

            gc.drawImage(this.spriteSheet,
                this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight,
                renderX, renderY, renderW, this.renderHeight);
            gc.restore();

            // Chớp trắng lúc vừa nhận đòn kết liễu
            if (this.flashTimer > 54) {
                applyWhiteFlash(gc, 0.9);
            }
        } else {
            super.render(gc);
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        return new Rectangle2D(x, y, renderWidth, renderHeight);
    }

    @Override
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        // Hiệp sĩ miễn nhiễm với bị đẩy lùi và choáng khi đang dùng chiêu
        if (this.isDashing) {
            return;
        }
        super.applyKnockback(dir);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        double w = renderWidth * 0.4;
        double h = renderHeight * 0.2;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }

    @Override
    public void onCollision(com.hust.game.entities.base.BaseEntity other) {
        super.onCollision(other);
        // Nếu đập mặt vào tường khi đang lướt -> Dừng lướt ngay lập tức
        if (this.isDashing && other == null) {
            this.dashVectorX = 0;
            this.dashVectorY = 0;
        }
    }
}
