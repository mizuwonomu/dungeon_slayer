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
    
    protected boolean isActive = true; // Trạng thái hoạt động (trong màn hình)
    protected boolean isImmobile = false; // Khóa di chuyển (Tutorial)
    protected boolean isHarmless = false; // Khóa sát thương (Tutorial)

    protected com.hust.game.collision.CollisionChecker collisionChecker;

    protected PathFinder pathFinder;
    protected boolean onPath = false;
    protected int pathUpdateTimer = (int)(Math.random() * 30); // Random để các con quái không update A* cùng 1 frame

    private boolean coinRewarded = false;

    // Cache bản "all-white" của spriteSheet hiện tại
    // Pixel có alpha > 0 → trắng cùng opacity; pixel trong suốt → giữ nguyên trong suốt
    // Cách này không đụng blend mode của canvas nên không bị ảnh hưởng bởi background đã vẽ trước đó
    private transient javafx.scene.image.Image cachedWhiteSheet = null;
    private transient javafx.scene.image.Image lastCachedSource = null;

    /**
     * Trả về bản white của spriteSheet hiện tại (tự động cache và regenerate khi sprite đổi).
     */
    protected javafx.scene.image.Image getWhiteSprite() {
        if (cachedWhiteSheet == null || lastCachedSource != this.spriteSheet) {
            lastCachedSource = this.spriteSheet;
            cachedWhiteSheet = buildWhiteSprite(this.spriteSheet);
        }
        return cachedWhiteSheet;
    }

    /**
     * Tạo một bản sao của ảnh gốc: mọi pixel có alpha > 0 đều bị thay bằng trắng cùng opacity.
     * Chạy một lần duy nhất per sprite, kết quả được cache lại.
     */
    private static javafx.scene.image.Image buildWhiteSprite(javafx.scene.image.Image source) {
        int w = (int) source.getWidth();
        int h = (int) source.getHeight();
        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelReader pr = source.getPixelReader();
        javafx.scene.image.PixelWriter pw = wi.getPixelWriter();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                double opacity = pr.getColor(px, py).getOpacity();
                if (opacity > 0.0) {
                    pw.setColor(px, py, javafx.scene.paint.Color.color(1.0, 1.0, 1.0, opacity));
                }
                // opacity == 0 → bỏ qua, pixel giữ nguyên trong suốt
            }
        }
        return wi;
    }

    /**
     * Vẽ lớp phủ trắng đè lên sprite đang hiển thị.
     * Gọi SAU khi đã vẽ sprite gốc.
     * @param gc      GraphicsContext chính
     * @param alpha   Độ mờ của lớp phủ (0.0 – 1.0)
     */
    protected void applyWhiteFlash(GraphicsContext gc, double alpha) {
        javafx.scene.image.Image ws = getWhiteSprite();
        double rx = this.isFlipped ? this.x + this.renderWidth : this.x;
        double rw = this.isFlipped ? -this.renderWidth : this.renderWidth;
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.drawImage(ws,
            this.frameIndex * this.frameWidth, 0, this.frameWidth, this.frameHeight,
            rx, this.y, rw, this.renderHeight);
        gc.restore();
    }

    // Constructor tạm thời ở Giai đoạn 1
    public Enemy(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, 1.0);
        this.targetPlayer = targetPlayer; // Chốt mục tiêu
    }

    @Override
    public void update() {
        if (!isActive) return; // Nếu ngoài camera thì bỏ qua update (Bất động)

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
            double multiplier = kbTimer / 3.5; // Giảm tốc dần đều, bắt đầu nhanh và chậm dần
            kbTimer--;
            this.x += kbVectorX * multiplier;
            this.y += kbVectorY * multiplier;
        } else if (hp > 0 && attackPauseTimer <= 0 && hitStunTimer <= 0 && targetPlayer != null && !isImmobile) {
            // Đồng bộ A* xuống HITBOX DƯỚI CHÂN thay vì tâm của bức ảnh
            javafx.geometry.Rectangle2D pCol = targetPlayer.getCollisionBoundary();
            double pCenterX = pCol.getMinX() + pCol.getWidth() / 2.0;
            double pCenterY = pCol.getMinY() + pCol.getHeight() / 2.0;

            javafx.geometry.Rectangle2D bounds = this.getCollisionBoundary();
            double currentCenterX = bounds.getMinX() + bounds.getWidth() / 2.0;
            double currentCenterY = bounds.getMinY() + bounds.getHeight() / 2.0;

            // Cập nhật đường đi A* mỗi 30 frame (~0.5 giây) để giảm tải CPU
            pathUpdateTimer++;
            if (pathUpdateTimer >= 30) {
                pathUpdateTimer = 0;
                int goalCol = (int) (pCenterX / com.hust.game.constants.GameConstants.TILE_SIZE);
                int goalRow = (int) (pCenterY / com.hust.game.constants.GameConstants.TILE_SIZE);
                searchPath(goalCol, goalRow);
            }

            double diffX = pCenterX - currentCenterX;
            double diffY = pCenterY - currentCenterY;
            double distance = Math.sqrt(diffX * diffX + diffY * diffY);

            this.moveX = 0;
            this.moveY = 0;

            if (distance > 0) {
                if (dodgeTimer > 0) {
                    // Lách vật cản nhẹ khi dính nhau (Soft Collision)
                    dodgeTimer--;
                    this.moveX = dodgeDirX * speed;
                    this.moveY = dodgeDirY * speed;
                    
                    if (dodgeDirX < 0) this.isFlipped = true;
                    else if (dodgeDirX > 0) this.isFlipped = false;
                    
                } else if (onPath && pathFinder.pathList.size() > 0) {
                    // ĐI THEO ĐƯỜNG DẪN A* TÌM ĐƯỢC
                    int nextCol = pathFinder.pathList.get(0).col;
                    int nextRow = pathFinder.pathList.get(0).row;
                    
                    // Điểm đến là tâm của ô lưới tiếp theo
                    double tileCenterX = nextCol * com.hust.game.constants.GameConstants.TILE_SIZE + com.hust.game.constants.GameConstants.TILE_SIZE / 2.0;
                    double tileCenterY = nextRow * com.hust.game.constants.GameConstants.TILE_SIZE + com.hust.game.constants.GameConstants.TILE_SIZE / 2.0;
                    
                    double dx = tileCenterX - currentCenterX;
                    double dy = tileCenterY - currentCenterY;
                    double distToNext = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distToNext > speed) {
                        this.moveX = (dx / distToNext) * speed;
                        this.moveY = (dy / distToNext) * speed;
                    } else {
                        // Đã đến node hiện tại, lấy node tiếp theo
                        pathFinder.pathList.remove(0);
                    }
                    
                    // Hướng mặt
                    if (this.moveX < 0) this.isFlipped = true;
                    else if (this.moveX > 0) this.isFlipped = false;

                } else {
                    // TRƯỜNG HỢP FALLBACK: Di chuyển đường thẳng (Đang rất gần, hoặc đường cụt)
                    this.moveX = (diffX / distance) * speed;
                    this.moveY = (diffY / distance) * speed;
                    
                    if (diffX < 0) this.isFlipped = true;
                    else if (diffX > 0) this.isFlipped = false;
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

    public void setCollisionChecker(com.hust.game.collision.CollisionChecker checker) {
        this.collisionChecker = checker;
        if (checker != null) {
            this.pathFinder = new PathFinder(checker);
        }
    }

    public void searchPath(int goalCol, int goalRow) {
        if (pathFinder == null) return;
        
        javafx.geometry.Rectangle2D bounds = this.getCollisionBoundary();
        int startCol = (int) ((bounds.getMinX() + bounds.getWidth() / 2.0) / com.hust.game.constants.GameConstants.TILE_SIZE);
        int startRow = (int) ((bounds.getMinY() + bounds.getHeight() / 2.0) / com.hust.game.constants.GameConstants.TILE_SIZE);
        
        // Đã đứng chung mâm với Player thì không cần tìm đường A*
        if (startCol == goalCol && startRow == goalRow) {
            onPath = false;
            return;
        }

        pathFinder.setNodes(startCol, startRow, goalCol, goalRow);
        onPath = pathFinder.search(); // Trả về true nếu có đường đi
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

    public int getMaxHp() {
        return this.maxHp;
    }

    // Hàm nhận sát thương từ Player
    public void takeDamage(int amount) {
        if (this.hp <= 0)
            return; // Nếu đã chết thì không nhận thêm sát thương nữa
        this.hp -= amount;
        
        if (this.hp <= 0) {
            this.hp = 0;
            this.flashTimer = 60; // Quái chết -> Tồn tại thêm 60 frames (1 giây) để chạy hiệu ứng mờ dần
            this.kbTimer = 0; // Hủy giật lùi để quái đứng im khi chết
        } else {
            this.flashTimer = 6; // Quái còn sống -> Chỉ nháy trắng 6 frames (~0.1 giây)
        }
        
        com.hust.game.ui.DamageTextManager.addText(this, this.x + renderWidth / 2 - 10, this.y, "-" + amount, javafx.scene.paint.Color.WHITE);
        System.out.println("Quái vật bị chém trúng! Máu còn: " + this.hp + "/" + this.maxHp);
    }

    @Override
    public void render(GraphicsContext gc) {
        if (this.hp <= 0) {
            // Fade out dần theo flashTimer
            gc.save();
            double alpha = (double) this.flashTimer / 60.0;
            if (alpha < 0) alpha = 0;
            if (alpha > 1) alpha = 1;
            gc.setGlobalAlpha(alpha);
            super.render(gc);
            gc.restore();

            // Chớp trắng trong 6 frame đầu tiên khi vừa nhận đòn kết liễu
            if (this.flashTimer > 54) {
                applyWhiteFlash(gc, 0.9);
            }
        } else {
            super.render(gc);
            // Quái còn sống và đang bị thương → chớp trắng
            if (this.flashTimer > 0) {
                applyWhiteFlash(gc, 0.9);
            }
        }
    }

    // Hàm xử lý đẩy lùi quái vật (Knockback)
    public void applyKnockback(com.hust.game.entities.Direction dir) {
        if (this.hp <= 0) return; // Không đẩy lùi nếu quái đã chết
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

    public boolean hasCoinRewarded() {
        return coinRewarded;
    }

    public void markCoinRewarded() {
        coinRewarded = true;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setImmobile(boolean immobile) {
        this.isImmobile = immobile;
    }

    public void setHarmless(boolean harmless) {
        this.isHarmless = harmless;
    }
}
