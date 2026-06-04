package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.player.Player;
import com.hust.game.main.App;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Queue;

public class Witch extends Enemy {

    private static final int CIRCLE_DAMAGE_COOLDOWN_FRAMES = 30;
    private static final double SUMMON_KNIGHT_RENDER_WIDTH = 96.0;
    private static final double SUMMON_KNIGHT_RENDER_HEIGHT = 96.0;
    private static final double SUMMON_KNIGHT_COLLISION_WIDTH_RATIO = 0.4;
    private static final double SUMMON_KNIGHT_COLLISION_HEIGHT_RATIO = 0.2;
    private static final int SUMMON_SEARCH_RADIUS_TILES = 18;
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
    private Image dieSprite;
    private boolean isDying = false;

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
            this.dieSprite = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/assets/enemy/witch_die.png"));
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

    // Hàm trợ giúp để cập nhật Frame và Tỷ lệ khi chuyển đổi kỹ năng
    private void updateSpriteAndDimensions(Image sprite, int totalFramesInSheet, int animationFrames) {
        double oldCenterX = this.x + this.renderWidth / 2.0;
        
        this.spriteSheet = sprite;
        this.numFrames = animationFrames;
        if (sprite != null) {
            this.frameWidth = sprite.getWidth() / totalFramesInSheet;
            this.frameHeight = sprite.getHeight();
            
            // Fix cứng chiều cao Witch là 150 để upscale ảnh rõ nét, chiều ngang tự tính theo tỷ lệ
            this.renderHeight = 150;
            this.renderWidth = this.renderHeight * (this.frameWidth / this.frameHeight);
        }
        this.frameIndex = 0;
        
        // Căn giữa lại tọa độ X để Witch không bị giật sang bên trái/phải khi đổi ảnh
        this.x = oldCenterX - this.renderWidth / 2.0;
    }

    private void resetToIdle() {
        isCastingCircle = false;
        isSummoning = false;
        circleTimer = 0;
        
        // IDLE chỉ lấy 7 frame đầu của witch_atk (tổng 20 frame)
        updateSpriteAndDimensions(castSprite, 20, 7);
    }

    private void decideNextSkill() {
        int knightCount = 0;
        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0) knightCount++;
        }

        if (knightCount < 2 && circleCountSinceLastSummon >= 2) {
            isSummoning = true;
            circleCountSinceLastSummon = 0;
            
            // SUMMON dùng witch_summon.png (tổng 25 frame)
            updateSpriteAndDimensions(summonSprite, 25, 25);
        } else {
            isCastingCircle = true;
            circleCountSinceLastSummon++;
            circleTimer = 0;
            
            // CAST dùng witch_atk.png (tổng 20 frame)
            updateSpriteAndDimensions(castSprite, 20, 20);
        }
    }

    private void spawnKnightsSafely() {
        int knightCount = 0;
        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e instanceof com.hust.game.enemy.Knight && e.getHp() > 0) knightCount++;
        }

        // Tim vi tri summon gan Witch nhung khong de Knight vao tuong hoac ngoai map.
        double[] spawn;

        if (knightCount == 0) {
            spawn = findSafeKnightSpawn(this.x - SUMMON_KNIGHT_RENDER_WIDTH, this.y);
            if (spawn != null) {
                enemyManager.spawnEnemy("Knight", spawn[0], spawn[1], knightIdle, 8,
                        SUMMON_KNIGHT_RENDER_WIDTH, SUMMON_KNIGHT_RENDER_HEIGHT, targetPlayer, knightAtk);
            }
        } else if (knightCount == 1) {
            spawn = findSafeKnightSpawn(this.x + this.renderWidth, this.y);
            if (spawn != null) {
                enemyManager.spawnEnemy("Knight", spawn[0], spawn[1], knightIdle, 8,
                        SUMMON_KNIGHT_RENDER_WIDTH, SUMMON_KNIGHT_RENDER_HEIGHT, targetPlayer, knightAtk);
            }
        }
    }

    private double[] findSafeKnightSpawn(double preferredX, double preferredY) {
        boolean[][] reachable = buildReachableTilesFromPlayer();
        if (isSafeKnightSpawn(preferredX, preferredY, reachable)) {
            return new double[]{preferredX, preferredY};
        }

        int startCol = (int) Math.round(knightCollisionCenterX(preferredX) / GameConstants.TILE_SIZE);
        int startRow = (int) Math.round(knightCollisionCenterY(preferredY) / GameConstants.TILE_SIZE);
        int maxRadius = Math.max(SUMMON_SEARCH_RADIUS_TILES,
                Math.max(GameConstants.MAX_WORLD_ROW, GameConstants.MAX_WORLD_COL));

        for (int radius = 0; radius <= maxRadius; radius++) {
            double[] best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int row = startRow - radius; row <= startRow + radius; row++) {
                for (int col = startCol - radius; col <= startCol + radius; col++) {
                    if (Math.abs(row - startRow) != radius && Math.abs(col - startCol) != radius) {
                        continue;
                    }

                    if (!isReachableTile(col, row, reachable)) {
                        continue;
                    }

                    double centerX = col * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2.0;
                    double centerY = row * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2.0;
                    double candidateX = centerX - SUMMON_KNIGHT_RENDER_WIDTH / 2.0;
                    double candidateY = centerY - SUMMON_KNIGHT_RENDER_HEIGHT
                            + (SUMMON_KNIGHT_RENDER_HEIGHT * SUMMON_KNIGHT_COLLISION_HEIGHT_RATIO) / 2.0;

                    if (!isSafeKnightSpawn(candidateX, candidateY, reachable)) {
                        continue;
                    }

                    double dx = candidateX - preferredX;
                    double dy = candidateY - preferredY;
                    double distance = dx * dx + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new double[]{candidateX, candidateY};
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        System.err.println("Witch khong tim thay vi tri summon Knight an toan.");
        return null;
    }

    private boolean isSafeKnightSpawn(double x, double y) {
        return isSafeKnightSpawn(x, y, null);
    }

    private boolean isSafeKnightSpawn(double x, double y, boolean[][] reachable) {
        Rectangle2D collisionBox = knightSpawnCollisionBox(x, y);
        if (!isCollisionBoxReachable(collisionBox, reachable)) {
            return false;
        }

        int left = (int) Math.floor(collisionBox.getMinX());
        int right = (int) Math.floor(collisionBox.getMaxX() - 1);
        int top = (int) Math.floor(collisionBox.getMinY());
        int bottom = (int) Math.floor(collisionBox.getMaxY() - 1);
        int centerX = (int) Math.floor(collisionBox.getMinX() + collisionBox.getWidth() / 2.0);
        int centerY = (int) Math.floor(collisionBox.getMinY() + collisionBox.getHeight() / 2.0);

        if (isBlockedByCollisionChecker(left, top) || isBlockedByCollisionChecker(right, top)
                || isBlockedByCollisionChecker(left, bottom) || isBlockedByCollisionChecker(right, bottom)
                || isBlockedByCollisionChecker(centerX, centerY)) {
            return false;
        }

        if (targetPlayer != null && targetPlayer.getCollisionBoundary().intersects(collisionBox)) {
            return false;
        }

        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e == this || e.getHp() <= 0) {
                continue;
            }
            if (e.getCollisionBoundary().intersects(collisionBox)) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlockedByCollisionChecker(int pixelX, int pixelY) {
        return collisionChecker != null && collisionChecker.checkTile(pixelX, pixelY);
    }

    private boolean canOccupy(double x, double y) {
        if (collisionChecker == null) {
            return true;
        }

        Rectangle2D collisionBox = new Rectangle2D(
                x + renderWidth * 0.3,
                y + renderHeight * 0.8,
                renderWidth * 0.4,
                renderHeight * 0.2
        );
        int left = (int) Math.floor(collisionBox.getMinX());
        int right = (int) Math.floor(collisionBox.getMaxX() - 1);
        int top = (int) Math.floor(collisionBox.getMinY());
        int bottom = (int) Math.floor(collisionBox.getMaxY() - 1);
        return !isBlockedByCollisionChecker(left, top)
                && !isBlockedByCollisionChecker(right, top)
                && !isBlockedByCollisionChecker(left, bottom)
                && !isBlockedByCollisionChecker(right, bottom);
    }

    private Rectangle2D knightSpawnCollisionBox(double x, double y) {
        double w = SUMMON_KNIGHT_RENDER_WIDTH * SUMMON_KNIGHT_COLLISION_WIDTH_RATIO;
        double h = SUMMON_KNIGHT_RENDER_HEIGHT * SUMMON_KNIGHT_COLLISION_HEIGHT_RATIO;
        double bx = x + (SUMMON_KNIGHT_RENDER_WIDTH - w) / 2.0;
        double by = y + SUMMON_KNIGHT_RENDER_HEIGHT - h;
        return new Rectangle2D(bx, by, w, h);
    }

    private double knightCollisionCenterX(double x) {
        return x + SUMMON_KNIGHT_RENDER_WIDTH / 2.0;
    }

    private double knightCollisionCenterY(double y) {
        return y + SUMMON_KNIGHT_RENDER_HEIGHT
                - (SUMMON_KNIGHT_RENDER_HEIGHT * SUMMON_KNIGHT_COLLISION_HEIGHT_RATIO) / 2.0;
    }

    private double[] findSafeWitchTeleportPosition() {
        if (targetPlayer == null) {
            return new double[]{this.x, this.y};
        }

        Rectangle2D witchBox = getCollisionBoundary();
        Rectangle2D playerBox = targetPlayer.getCollisionBoundary();
        double witchCenterX = witchBox.getMinX() + witchBox.getWidth() / 2.0;
        double witchCenterY = witchBox.getMinY() + witchBox.getHeight() / 2.0;
        double playerCenterX = playerBox.getMinX() + playerBox.getWidth() / 2.0;
        double playerCenterY = playerBox.getMinY() + playerBox.getHeight() / 2.0;

        double awayX = witchCenterX - playerCenterX;
        double awayY = witchCenterY - playerCenterY;
        double distance = Math.sqrt(awayX * awayX + awayY * awayY);
        if (distance < 1.0) {
            double angle = ((int) ((witchCenterX + witchCenterY) / GameConstants.TILE_SIZE) % 8)
                    * Math.PI / 4.0;
            awayX = Math.cos(angle);
            awayY = Math.sin(angle);
            distance = 1.0;
        }

        awayX /= distance;
        awayY /= distance;

        double side = ((int) Math.floor((witchCenterX + witchCenterY) / GameConstants.TILE_SIZE) % 2 == 0)
                ? 1.0 : -1.0;
        double preferredCenterX = witchCenterX
                + awayX * GameConstants.TILE_SIZE * 7.0
                + (-awayY) * side * GameConstants.TILE_SIZE * 4.0;
        double preferredCenterY = witchCenterY
                + awayY * GameConstants.TILE_SIZE * 7.0
                + awayX * side * GameConstants.TILE_SIZE * 4.0;

        double preferredX = preferredCenterX - renderWidth / 2.0;
        double preferredY = preferredCenterY - renderHeight + (renderHeight * 0.2) / 2.0;
        return findSafeWitchPosition(preferredX, preferredY);
    }

    private double[] findSafeWitchPosition(double preferredX, double preferredY) {
        boolean[][] reachable = buildReachableTilesFromPlayer();
        if (isSafeWitchPosition(preferredX, preferredY, reachable)) {
            return new double[]{preferredX, preferredY};
        }

        int startCol = (int) Math.round((preferredX + renderWidth / 2.0) / GameConstants.TILE_SIZE);
        int startRow = (int) Math.round((preferredY + renderHeight - (renderHeight * 0.2) / 2.0)
                / GameConstants.TILE_SIZE);
        int maxRadius = Math.max(SUMMON_SEARCH_RADIUS_TILES,
                Math.max(GameConstants.MAX_WORLD_ROW, GameConstants.MAX_WORLD_COL));

        for (int radius = 0; radius <= maxRadius; radius++) {
            double[] best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int row = startRow - radius; row <= startRow + radius; row++) {
                for (int col = startCol - radius; col <= startCol + radius; col++) {
                    if (Math.abs(row - startRow) != radius && Math.abs(col - startCol) != radius) {
                        continue;
                    }

                    if (!isReachableTile(col, row, reachable)) {
                        continue;
                    }

                    double centerX = col * GameConstants.TILE_SIZE
                            + GameConstants.TILE_SIZE / 2.0;
                    double centerY = row * GameConstants.TILE_SIZE
                            + GameConstants.TILE_SIZE / 2.0;
                    double candidateX = centerX - renderWidth / 2.0;
                    double candidateY = centerY - renderHeight + (renderHeight * 0.2) / 2.0;

                    if (!isSafeWitchPosition(candidateX, candidateY, reachable)) {
                        continue;
                    }

                    double dx = candidateX - preferredX;
                    double dy = candidateY - preferredY;
                    double distance = dx * dx + dy * dy;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new double[]{candidateX, candidateY};
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        System.err.println("Witch khong tim thay vi tri dich chuyen an toan.");
        return new double[]{this.x, this.y};
    }

    private boolean isSafeWitchPosition(double x, double y) {
        return isSafeWitchPosition(x, y, null);
    }

    private boolean isSafeWitchPosition(double x, double y, boolean[][] reachable) {
        Rectangle2D collisionBox = new Rectangle2D(
                x + renderWidth * 0.3,
                y + renderHeight * 0.8,
                renderWidth * 0.4,
                renderHeight * 0.2
        );
        if (!isCollisionBoxReachable(collisionBox, reachable)) {
            return false;
        }

        int left = (int) Math.floor(collisionBox.getMinX());
        int right = (int) Math.floor(collisionBox.getMaxX() - 1);
        int top = (int) Math.floor(collisionBox.getMinY());
        int bottom = (int) Math.floor(collisionBox.getMaxY() - 1);
        int centerX = (int) Math.floor(collisionBox.getMinX() + collisionBox.getWidth() / 2.0);
        int centerY = (int) Math.floor(collisionBox.getMinY() + collisionBox.getHeight() / 2.0);

        return !isBlockedByCollisionChecker(left, top)
                && !isBlockedByCollisionChecker(right, top)
                && !isBlockedByCollisionChecker(left, bottom)
                && !isBlockedByCollisionChecker(right, bottom)
                && !isBlockedByCollisionChecker(centerX, centerY)
                && (targetPlayer == null || !targetPlayer.getCollisionBoundary().intersects(collisionBox))
                && !intersectsOtherEnemy(collisionBox);
    }

    private boolean[][] buildReachableTilesFromPlayer() {
        if (collisionChecker == null) {
            return null;
        }

        boolean[][] reachable = new boolean[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
        Rectangle2D sourceBox = targetPlayer != null ? targetPlayer.getCollisionBoundary() : getCollisionBoundary();
        int startCol = pixelToTile(sourceBox.getMinX() + sourceBox.getWidth() / 2.0);
        int startRow = pixelToTile(sourceBox.getMinY() + sourceBox.getHeight() / 2.0);

        if (!isTileWalkable(startCol, startRow)) {
            int[] nearestWalkable = findNearestWalkableTile(startCol, startRow);
            if (nearestWalkable == null) {
                return reachable;
            }
            startCol = nearestWalkable[0];
            startRow = nearestWalkable[1];
        }

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startCol, startRow});
        reachable[startRow][startCol] = true;

        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            for (int[] direction : directions) {
                int nextCol = current[0] + direction[0];
                int nextRow = current[1] + direction[1];

                if (!isTileInMap(nextCol, nextRow) || reachable[nextRow][nextCol]
                        || !isTileWalkable(nextCol, nextRow)) {
                    continue;
                }

                reachable[nextRow][nextCol] = true;
                queue.add(new int[]{nextCol, nextRow});
            }
        }

        return reachable;
    }

    private int[] findNearestWalkableTile(int startCol, int startRow) {
        int maxRadius = Math.max(GameConstants.MAX_WORLD_ROW, GameConstants.MAX_WORLD_COL);
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int row = startRow - radius; row <= startRow + radius; row++) {
                for (int col = startCol - radius; col <= startCol + radius; col++) {
                    if (Math.abs(row - startRow) != radius && Math.abs(col - startCol) != radius) {
                        continue;
                    }

                    if (isTileWalkable(col, row)) {
                        return new int[]{col, row};
                    }
                }
            }
        }

        return null;
    }

    private boolean isCollisionBoxReachable(Rectangle2D collisionBox, boolean[][] reachable) {
        if (reachable == null) {
            return true;
        }

        int col = pixelToTile(collisionBox.getMinX() + collisionBox.getWidth() / 2.0);
        int row = pixelToTile(collisionBox.getMinY() + collisionBox.getHeight() / 2.0);
        return isReachableTile(col, row, reachable);
    }

    private boolean isReachableTile(int col, int row, boolean[][] reachable) {
        if (reachable == null) {
            return true;
        }

        return isTileInMap(col, row) && reachable[row][col];
    }

    private boolean isTileWalkable(int col, int row) {
        if (!isTileInMap(col, row)) {
            return false;
        }

        int pixelX = col * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;
        int pixelY = row * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;
        return !isBlockedByCollisionChecker(pixelX, pixelY);
    }

    private boolean isTileInMap(int col, int row) {
        return col >= 0 && col < GameConstants.MAX_WORLD_COL
                && row >= 0 && row < GameConstants.MAX_WORLD_ROW;
    }

    private int pixelToTile(double pixel) {
        return (int) Math.floor(pixel / GameConstants.TILE_SIZE);
    }

    private boolean intersectsOtherEnemy(Rectangle2D collisionBox) {
        for (com.hust.game.enemy.Enemy e : enemyManager.getEnemyList()) {
            if (e == this || e.getHp() <= 0) {
                continue;
            }
            if (e.getCollisionBoundary().intersects(collisionBox)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update() {
        updatePlayerDamageCooldown();

        this.lastX = this.x;
        this.lastY = this.y;

        if (this.hp > 0) {
            if (this.flashTimer > 0) {
                this.flashTimer--;
            }
            if (this.hitStunTimer > 0) {
                this.hitStunTimer--;
            }
        }

        if (this.hp <= 0) {
            this.hitStunTimer = 0;
            this.isCastingCircle = false;
            this.isSummoning = false;

            if (!isDying) {
                isDying = true;
                if (dieSprite != null) {
                    this.spriteSheet = dieSprite;
                    this.numFrames = 11;
                    this.frameWidth = dieSprite.getWidth() / 11.0;
                    this.frameHeight = dieSprite.getHeight();
                    
                    // Cập nhật lại tỷ lệ kích thước cho ảnh chết, giữ tâm đứng yên
                    double oldCenterX = this.x + this.renderWidth / 2.0;
                    this.renderHeight = 150;
                    this.renderWidth = this.renderHeight * (this.frameWidth / this.frameHeight);
                    this.x = oldCenterX - this.renderWidth / 2.0;
                }
                this.frameIndex = 0;
                this.animationTimer = 0;
            }

            if (this.flashTimer > 54) {
                this.flashTimer--;
            }

            if (this.frameIndex < 10) {
                this.animationTimer++;
                if (this.animationTimer >= 6) { // Chạy 6 frame game mỗi ảnh
                    this.animationTimer = 0;
                    this.frameIndex++;
                }
            } else {
                if (this.flashTimer > 0 && this.flashTimer <= 54) {
                    this.flashTimer--;
                }
            }
            return;
        }

        // Bổ sung Knockback cho Phù thủy để tự nhiên hơn khi bị đánh trúng
        if (this.kbTimer > 0) {
            double multiplier = this.kbTimer / 3.5;
            this.kbTimer--;
            this.x += kbVectorX * multiplier;
            this.y += kbVectorY * multiplier;
        }

        // 1. CƠ CHẾ DỊCH CHUYỂN (Chỉ kích hoạt 1 lần khi HP <= 50%)
        if (this.hp <= this.maxHp / 2 && !hasTeleported) {
            hasTeleported = true;
            
            // Kích thước phòng Level 2 là 816x480. Dịch chuyển trong vùng an toàn (x: 100->650, y: 200)
            // Dịch chuyển Witch đến vị trí an toàn hơn, tránh bị kẹt vào tường ở rìa màn hình.
            // Các giá trị đã được điều chỉnh để đảm bảo có khoảng trống xung quanh.
            double[] safePos = findSafeWitchTeleportPosition();
            this.x = safePos[0];
            this.y = safePos[1];
            
            // Cập nhật lastX, lastY để cơ chế chống kẹt tường không đẩy ngược Witch về chỗ cũ
            this.lastX = this.x;
            this.lastY = this.y;
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
                double nextX = this.x + this.moveX;
                double nextY = this.y + this.moveY;
                if (canOccupy(nextX, nextY)) {
                    this.x = nextX;
                    this.y = nextY;
                } else {
                    this.moveX = 0;
                    this.moveY = 0;
                }
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
                circleX = targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0 - 32;
                circleY = targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0 - 32;
            } else if (circleTimer <= 210) {
                // Vòng dừng lại khóa mục tiêu (chuẩn bị nổ)
            } else if (circleTimer == 211) {
                com.hust.game.audio.SoundManager.playWitchCircleExplodeSound();
                // Phát nổ gây sát thương
                double cDiffX = (targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0) - (circleX + 32);
                double cDiffY = (targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0) - (circleY + 32);
                if (Math.sqrt(cDiffX * cDiffX + cDiffY * cDiffY) <= 40) {
                    tryDamagePlayer(targetPlayer, this.damage, circleX + 32, circleY + 32, CIRCLE_DAMAGE_COOLDOWN_FRAMES); // Đẩy lùi tính từ tâm vòng lửa
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
        if (this.hp <= 0) {
            gc.save();
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

            if (this.flashTimer > 54) {
                applyWhiteFlash(gc, 0.9);
            }
        } else {
            super.render(gc);

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
                        circleX, circleY + 5, 64, 64);
            }
        }
    }

    @Override
    public Rectangle2D getBoundary() {
        // Cắt bớt hitbox vì sprite kích thước rộng (269x300, 223x300) có rất nhiều viền trống 2 bên
        // Giảm bớt padding để hitbox lớn hơn, dễ trúng hơn.
        double paddingX = this.renderWidth * 0.2;
        double paddingY = this.renderHeight * 0.1;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        double w = renderWidth * 0.4;
        double h = renderHeight * 0.2;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }
}
