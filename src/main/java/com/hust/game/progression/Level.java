package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.npc.Npc;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.hust.game.entities.environment.Gate;


public class Level {

    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;
    private static final int SMART_ROOM_SIZE = 7;
    private static final int SMART_ROOM_MIN_WALKABLE_TILES = 28;
    private static final int LEVEL1_SMART_ROOM_LIMIT = 8;
    private static final int LEVEL1_TEST_ENEMY_LIMIT = LEVEL1_SMART_ROOM_LIMIT * 2;
    private static final int LEVEL2_SMART_ROOM_LIMIT = 7;
    private static final int LEVEL2_WITCH_COUNT = 5;
    private static final double ENEMY_FOOT_COLLISION_WIDTH_RATIO = 0.4;
    private static final double ENEMY_FOOT_COLLISION_HEIGHT_RATIO = 0.2;
    private static final double LEVEL2_WITCH_RENDER_HEIGHT = 150.0;
    private static final long LEVEL1_NPC_SEED = 1001L;

    private int lvlID;

    private EnemyManager enemyManager;
    private MapManager map;
    Player player;

    Image treeImg;
    Image treeSkillImg;
    Image slimeImg;
    Image knightImg;
    Image knightSkillImg;
    Image witchImg;
    Image witchSkillImg;
    Image bossImg;
    
    private List<Gate> gates = new ArrayList<>();
    private Image gateImg;
    private Image hangingTreeImg;
    private Npc npc;

    public Level(int lvlID,
                 EnemyManager enemyManager,
                 Player player,
                 Image treeImg, Image treeSkillImg,
                 Image slimeImg,
                 Image knightImg, Image knightSkillImg,
                 Image witchImg, Image witchSkillImg,
                 Image bossImg) {

        this.lvlID = lvlID;
        this.enemyManager = enemyManager;
        this.player = player;

        this.treeImg = treeImg;
        this.treeSkillImg = treeSkillImg;
        this.slimeImg = slimeImg;
        this.knightImg = knightImg;
        this.knightSkillImg = knightSkillImg;
        this.witchImg = witchImg;
        this.witchSkillImg = witchSkillImg;
        this.bossImg = bossImg;

        map = new MapManager(lvlID);
        
        try {
            gateImg = new Image(getClass().getResourceAsStream("/assets/tiles/gate.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy gate.png");
        }
        try {
            hangingTreeImg = new Image(getClass().getResourceAsStream("/assets/tiles/hanging_tree.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy hanging_tree.png");
        }
    }
    public void init(){
        spawnEnemy();
        spawnGates();
        spawnDecorations();
        spawnNpc();
    }

    void draw(GraphicsContext gc) {
        map.draw(gc);
        // Không vẽ Gate ở đây nữa, Gate sẽ được nhúng vào renderList bên App.java để đè hình 3D
    }
    
    public void update() {
        for (Gate gate : gates) {
            gate.update();
        }
        // Chạy update cho các vật thể trên map để kích hoạt animation (nếu có)
        for (com.hust.game.entities.base.BaseEntity entity : map.mapEntities) {
            entity.update();
        }
        
        // Logic mở cổng Tutorial
        if (lvlID == 0) {
            List<Enemy> enemies = enemyManager.getEnemyList();
            boolean slimeDead = true;
            boolean knightDead = true;
            
            for (Enemy e : enemies) {
                if (e instanceof Slime && e.getHp() > 0) slimeDead = false;
                if (e instanceof Knight && e.getHp() > 0) knightDead = false;
            }
            
            for (Gate g : gates) {
                if (g.getX() < 15 * TILE_SIZE) {
                    g.open(); // Cổng phòng 1->2 (Cột 9) luôn mở
                } else if (g.getX() < 30 * TILE_SIZE && slimeDead) {
                    g.open(); // Cổng phòng 2->3 (Cột 22) mở khi Slime chết
                } else if (g.getX() > 30 * TILE_SIZE && knightDead) {
                    g.open(); // Cổng phòng 3->4 (Cột 36) mở khi Knight chết
                }
            }
            
            // Đánh thức quái vật ở phòng cuối (Tree) khi cổng phòng 4 được mở (Knight chết)
            if (knightDead) {
                for (Enemy e : enemies) {
                    if (e instanceof Tree) {
                        e.setImmobile(false);
                        e.setHarmless(false);
                    }
                }
            }
        }
    }
    
    private void spawnGates() {
        if (gateImg != null) {
            for (int[] pos : map.gatePositions) {
                Gate gate = new Gate(pos[0] * TILE_SIZE, pos[1] * TILE_SIZE, gateImg);
                gates.add(gate);
            }

            // Tự động sinh Gate lấp kín các hành lang cho Tutorial nếu map chưa đánh dấu -100
            if (lvlID == 0 && map.gatePositions.isEmpty()) {
                // Cửa 1 (Phòng 1 -> 2) tại cột 9
                gates.add(new Gate(9 * TILE_SIZE, 4 * TILE_SIZE, gateImg));
                gates.add(new Gate(9 * TILE_SIZE, 5 * TILE_SIZE, gateImg));
                
                // Cửa 2 (Phòng 2 -> 3) tại cột 22
                gates.add(new Gate(22 * TILE_SIZE, 4 * TILE_SIZE, gateImg));
                gates.add(new Gate(22 * TILE_SIZE, 5 * TILE_SIZE, gateImg));
                gates.add(new Gate(22 * TILE_SIZE, 6 * TILE_SIZE, gateImg));
                
                // Cửa 3 (Phòng 3 -> 4) tại cột 36
                gates.add(new Gate(36 * TILE_SIZE, 4 * TILE_SIZE, gateImg));
                gates.add(new Gate(36 * TILE_SIZE, 5 * TILE_SIZE, gateImg));
                gates.add(new Gate(36 * TILE_SIZE, 6 * TILE_SIZE, gateImg));
                gates.add(new Gate(36 * TILE_SIZE, 7 * TILE_SIZE, gateImg));
            }
        }
    }

    private void spawnDecorations() {
        if (lvlID == 0 && hangingTreeImg != null) {
            // Tọa độ tính bằng TILE_SIZE (48px). Ảnh 512x256 có 4 frame => 1 frame kích thước 128x256
            
            // Tạm thời bỏ cây vào phòng đầu tiên (Tutorial)
            // map.mapEntities.add(new com.hust.game.entities.base.StaticEntity(
            //    4 * TILE_SIZE, 1 * TILE_SIZE, hangingTreeImg, 4, 128, 256));
            
            // Tạm thời bỏ cây vào phòng cuối cùng (Tutorial)
            // map.mapEntities.add(new com.hust.game.entities.base.StaticEntity(
            //    55 * TILE_SIZE, 1 * TILE_SIZE, hangingTreeImg, 4, 128, 256));
        }
    }

    public List<Gate> getGates() {
        return gates;
    }

    public Npc getNpc() {
        return npc;
    }

    void spawnEnemy(){
        if (lvlID == 0) {
            // Phòng 2: Slime bất động
            enemyManager.spawnEnemy("Slime", 15 * TILE_SIZE, 5 * TILE_SIZE, slimeImg, 8, 51, 31.5, player);
            // Phòng 3: Knight bất động
            enemyManager.spawnEnemy("Knight", 29 * TILE_SIZE, 4 * TILE_SIZE, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, player, knightSkillImg);
            // Phòng 4: 3 Tree bình thường
            enemyManager.spawnEnemy("Tree", 45 * TILE_SIZE, 3 * TILE_SIZE, treeImg, 8, 96, 96, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 47 * TILE_SIZE, 6 * TILE_SIZE, treeImg, 8, 96, 96, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 49 * TILE_SIZE, 4 * TILE_SIZE, treeImg, 8, 96, 96, player, treeSkillImg);
            
            for (Enemy e : enemyManager.getEnemyList()) {
                // Khóa toàn bộ quái vật ban đầu (Bao gồm cả Tree ở phòng cuối)
                e.setImmobile(true);
                e.setHarmless(true);
            }
        } else if(lvlID == 1){

            // --- THUẬT TOÁN SINH QUÁI THÔNG MINH CHO LEVEL 1 ---
            
            // 1. Flood Fill (BFS) để tìm tất cả các ô có thể đi tới được từ vị trí spawn của Player
            // Giúp tự động bỏ qua các phòng bị đóng kín hoàn toàn bởi tường (-1)
            boolean[][] reachable = new boolean[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
            java.util.Queue<int[]> queue = new java.util.LinkedList<>();
            
            int startCol = 4;
            int startRow = 16;
            queue.add(new int[]{startCol, startRow});
            reachable[startRow][startCol] = true;
            
            int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
            while (!queue.isEmpty()) {
                int[] curr = queue.poll();
                int cx = curr[0];
                int cy = curr[1];
                
                for (int[] d : dirs) {
                    int nx = cx + d[0];
                    int ny = cy + d[1];
                    if (nx >= 0 && nx < GameConstants.MAX_WORLD_COL && ny >= 0 && ny < GameConstants.MAX_WORLD_ROW) {
                        if (!reachable[ny][nx]) {
                            int tileId = map.mapTileNum[ny][nx];
                            com.hust.game.map.Tile t = map.tiles.get(tileId);
                            if (t != null && !t.collision) {
                                reachable[ny][nx] = true;
                                queue.add(new int[]{nx, ny});
                            }
                        }
                    }
                }
            }
            
            int spawnedForTest = 0;

            // 2. Quét map theo các vùng 7x7 để phát hiện "Phòng" và bỏ qua "Hành lang"
            for (int r = 0; r < GameConstants.MAX_WORLD_ROW && spawnedForTest < LEVEL1_SMART_ROOM_LIMIT * 2; r += 7) {
                for (int c = 15; c < GameConstants.MAX_WORLD_COL && spawnedForTest < LEVEL1_TEST_ENEMY_LIMIT; c += 7) { // c >= 15 để chừa trống phòng spawn đầu tiên
                    int reachableCount = 0;
                    List<int[]> roomTiles = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        for (int j = 0; j < 7; j++) {
                            if (r + i < GameConstants.MAX_WORLD_ROW && c + j < GameConstants.MAX_WORLD_COL) {
                                if (reachable[r + i][c + j]) {
                                    reachableCount++;
                                    roomTiles.add(new int[]{c + j, r + i});
                                }
                            }
                        }
                    }
                    
                    // Nếu vùng 7x7 có >= 28 ô đi được -> Đủ rộng để coi là Phòng (Hành lang 3 ô chỉ có tối đa ~21 ô)
                    if (reachableCount >= 28) {
                        int numEnemies = Math.min(2, LEVEL1_SMART_ROOM_LIMIT * 2 - spawnedForTest);
                        for (int k = 0; k < numEnemies && !roomTiles.isEmpty(); k++) {
                            int randIdx = (int)(Math.random() * roomTiles.size());
                            int[] pos = roomTiles.get(randIdx);
                            roomTiles.remove(randIdx); // Xóa khỏi danh sách để tránh sinh 2 con quái đè lên cùng 1 ô
                            
                            double spawnX = pos[0] * TILE_SIZE;
                            double spawnY = pos[1] * TILE_SIZE;
                            
                            // Tỉ lệ 50% ra Cây, 50% ra Slime
                            if (Math.random() > 0.5) {
                                // Ảnh Tree cao 96px (2 ô), mà điểm xét tường là dưới chân. 
                                // Phải lùi Y lên 1 ô (-48) để chân Tree đáp đúng vào ô trống đã quét được
                                double[] safeTree = findSafeEnemySpawn(spawnX, spawnY - 48, 96, 96);
                                enemyManager.spawnEnemy("Tree", safeTree[0], safeTree[1], treeImg, 8, 96, 96, player, treeSkillImg);
                            } else {
                                double[] safeSlime = findSafeEnemySpawn(spawnX, spawnY, 51, 31.5);
                                enemyManager.spawnEnemy("Slime", safeSlime[0], safeSlime[1], slimeImg, 8, 51, 31.5, player);
                            }
                            spawnedForTest++;
                        }
                    }
                }
            }
        }
        else if(lvlID == 2){
            spawnLevel2Enemies();
        }
        else if (lvlID == 3) {
            // Spawn boss ở ô (31, 20)
            enemyManager.spawnEnemy("FinalBoss", 30 * TILE_SIZE, 19 * TILE_SIZE, bossImg, 5, TILE_SIZE * 3, TILE_SIZE * 3, player);
        }
    }

    private void spawnLevel2Enemies() {
        double knightW = TILE_SIZE * 2.0;
        double knightH = TILE_SIZE * 2.0;
        double witchH = LEVEL2_WITCH_RENDER_HEIGHT;
        double witchW = calculateWitchRenderWidth(witchH);

        boolean[][] reachable = buildReachableTilesFromPlayer();
        List<int[]> anchors = new ArrayList<>();
        addLevel2Anchor(anchors, reachable, 0.30, 0.20); // khu tren
        addLevel2Anchor(anchors, reachable, 0.32, 0.78); // khu duoi
        addLevel2Anchor(anchors, reachable, 0.66, 0.32); // ben phai, cum 1
        addLevel2Anchor(anchors, reachable, 0.82, 0.68); // ben phai, cum 2
        addLevel2Anchor(anchors, reachable, 0.52, 0.52); // cum giua de level khong bi trong

        for (int i = 0; i < anchors.size(); i++) {
            int[] anchor = anchors.get(i);

            if (i < LEVEL2_WITCH_COUNT) {
                spawnEnemyAtTile("Witch", anchor[0], anchor[1], witchW, witchH, witchImg, 25, witchSkillImg);
            }

            if (i < 2) {
                spawnEnemyAtTile("Knight", anchor[0], anchor[1] + 2, knightW, knightH, knightImg, 8, knightSkillImg);
            }
        }
    }

    private void addLevel2Anchor(List<int[]> anchors, boolean[][] reachable, double colRatio, double rowRatio) {
        int[] tile = findReachableTileNear(reachable, colRatio, rowRatio, 8, anchors, 10);
        if (tile == null) {
            tile = findReachableTileNear(reachable, colRatio, rowRatio, 8, anchors, 4);
        }
        if (tile == null) {
            tile = findReachableTileNear(reachable, colRatio, rowRatio, 8, anchors, 0);
        }
        if (tile != null) {
            anchors.add(tile);
        }
    }

    private int[] findReachableTileNear(boolean[][] reachable, double colRatio, double rowRatio,
            int minPlayerDistance, List<int[]> anchors, int minAnchorDistance) {
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return null;
        }

        int rows = reachable.length;
        int cols = reachable[0].length;
        int targetCol = clampToRange((int) Math.round((cols - 1) * colRatio), 0, cols - 1);
        int targetRow = clampToRange((int) Math.round((rows - 1) * rowRatio), 0, rows - 1);
        int playerCol = player != null ? (int) (player.getCollisionBoundary().getMinX() / TILE_SIZE) : 4;
        int playerRow = player != null ? (int) (player.getCollisionBoundary().getMinY() / TILE_SIZE) : 16;
        int maxRadius = Math.max(rows, cols);

        for (int radius = 0; radius <= maxRadius; radius++) {
            int[] best = null;
            int bestDistance = Integer.MAX_VALUE;

            for (int row = targetRow - radius; row <= targetRow + radius; row++) {
                for (int col = targetCol - radius; col <= targetCol + radius; col++) {
                    if (Math.abs(row - targetRow) != radius && Math.abs(col - targetCol) != radius) {
                        continue;
                    }
                    if (row < 0 || row >= rows || col < 0 || col >= cols || !reachable[row][col]) {
                        continue;
                    }
                    if (Math.abs(col - playerCol) + Math.abs(row - playerRow) < minPlayerDistance) {
                        continue;
                    }
                    if (!isFarFromAnchors(col, row, anchors, minAnchorDistance)) {
                        continue;
                    }

                    int distance = Math.abs(col - targetCol) + Math.abs(row - targetRow);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new int[]{col, row};
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    private boolean isFarFromAnchors(int col, int row, List<int[]> anchors, int minDistance) {
        for (int[] anchor : anchors) {
            if (Math.abs(col - anchor[0]) + Math.abs(row - anchor[1]) < minDistance) {
                return false;
            }
        }
        return true;
    }

    private boolean[][] buildReachableTilesFromPlayer() {
        int startCol = 4;
        int startRow = 16;

        if (player != null) {
            Rectangle2D playerBox = player.getCollisionBoundary();
            startCol = clampToRange((int) ((playerBox.getMinX() + playerBox.getWidth() / 2.0) / TILE_SIZE),
                    0, Math.max(0, map.getLoadedColCount() - 1));
            startRow = clampToRange((int) ((playerBox.getMinY() + playerBox.getHeight() / 2.0) / TILE_SIZE),
                    0, Math.max(0, map.getLoadedRowCount() - 1));
        }

        return buildReachableTiles(startCol, startRow);
    }

    private boolean[][] buildReachableTiles(int startCol, int startRow) {
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        boolean[][] reachable = new boolean[rows][cols];

        if (!isWalkableSpawnTile(startCol, startRow)) {
            int[] nearest = findNearestWalkableTile(startCol, startRow);
            if (nearest == null) {
                return reachable;
            }
            startCol = nearest[0];
            startRow = nearest[1];
        }

        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{startCol, startRow});
        reachable[startRow][startCol] = true;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            for (int[] dir : dirs) {
                int nextCol = current[0] + dir[0];
                int nextRow = current[1] + dir[1];
                if (nextRow < 0 || nextRow >= rows || nextCol < 0 || nextCol >= cols
                        || reachable[nextRow][nextCol] || !isWalkableSpawnTile(nextCol, nextRow)) {
                    continue;
                }

                reachable[nextRow][nextCol] = true;
                queue.add(new int[]{nextCol, nextRow});
            }
        }

        return reachable;
    }

    private List<List<int[]>> findReachableRooms(boolean[][] reachable, int minCol, int roomLimit, int minPlayerDistance) {
        List<List<int[]>> rooms = new ArrayList<>();
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        int playerCol = player != null ? (int) (player.getCollisionBoundary().getMinX() / TILE_SIZE) : 4;
        int playerRow = player != null ? (int) (player.getCollisionBoundary().getMinY() / TILE_SIZE) : 16;

        for (int r = 0; r < rows && rooms.size() < roomLimit; r += SMART_ROOM_SIZE) {
            for (int c = minCol; c < cols && rooms.size() < roomLimit; c += SMART_ROOM_SIZE) {
                List<int[]> roomTiles = new ArrayList<>();

                for (int i = 0; i < SMART_ROOM_SIZE; i++) {
                    for (int j = 0; j < SMART_ROOM_SIZE; j++) {
                        int row = r + i;
                        int col = c + j;
                        if (row >= rows || col >= cols || !reachable[row][col]) {
                            continue;
                        }
                        int distance = Math.abs(col - playerCol) + Math.abs(row - playerRow);
                        if (distance < minPlayerDistance) {
                            continue;
                        }
                        roomTiles.add(new int[]{col, row});
                    }
                }

                if (roomTiles.size() >= SMART_ROOM_MIN_WALKABLE_TILES) {
                    rooms.add(roomTiles);
                }
            }
        }

        return rooms;
    }

    private void spawnEnemyFromRoom(String enemyType, List<int[]> roomTiles, int tileOffset,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        if (roomTiles == null || roomTiles.isEmpty()) {
            return;
        }

        int index = Math.floorMod((tileOffset + 1) * roomTiles.size() / 3, roomTiles.size());
        int[] tile = roomTiles.get(index);
        spawnEnemyAtTile(enemyType, tile[0], tile[1], renderWidth, renderHeight, sprite, frames, skillSprite);
    }

    private void spawnEnemyAtTile(String enemyType, int col, int row,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
        double preferredX = centerX - renderWidth / 2.0;
        double preferredY = centerY - renderHeight + (renderHeight * ENEMY_FOOT_COLLISION_HEIGHT_RATIO) / 2.0;
        double[] safe = findSafeEnemySpawn(preferredX, preferredY, renderWidth, renderHeight);

        enemyManager.spawnEnemy(enemyType, safe[0], safe[1], sprite, frames,
                renderWidth, renderHeight, player, skillSprite);
    }

    private int[] findNearestWalkableTile(int startCol, int startRow) {
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        int maxRadius = Math.max(rows, cols);

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int row = startRow - radius; row <= startRow + radius; row++) {
                for (int col = startCol - radius; col <= startCol + radius; col++) {
                    if (Math.abs(row - startRow) != radius && Math.abs(col - startCol) != radius) {
                        continue;
                    }
                    if (isWalkableSpawnTile(col, row)) {
                        return new int[]{col, row};
                    }
                }
            }
        }

        return null;
    }

    private boolean isWalkableSpawnTile(int col, int row) {
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return false;
        }

        int centerX = col * TILE_SIZE + TILE_SIZE / 2;
        int centerY = row * TILE_SIZE + TILE_SIZE / 2;
        return !isBlockedByMap(centerX, centerY);
    }

    private double calculateWitchRenderWidth(double renderHeight) {
        if (witchSkillImg == null || witchSkillImg.getHeight() <= 0) {
            return TILE_SIZE;
        }
        double frameWidth = witchSkillImg.getWidth() / 20.0;
        return renderHeight * (frameWidth / witchSkillImg.getHeight());
    }

    private double[] findSafeEnemySpawn(double preferredX, double preferredY, double renderWidth, double renderHeight) {
        if (isSafeEnemySpawn(preferredX, preferredY, renderWidth, renderHeight)) {
            return new double[]{preferredX, preferredY};
        }

        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        int startCol = clampToRange((int) Math.round(collisionCenterX(preferredX, renderWidth) / TILE_SIZE), 0, cols - 1);
        int startRow = clampToRange((int) Math.round(collisionCenterY(preferredY, renderHeight) / TILE_SIZE), 0, rows - 1);
        int maxRadius = Math.max(rows, cols);

        for (int radius = 0; radius <= maxRadius; radius++) {
            double[] best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int row = startRow - radius; row <= startRow + radius; row++) {
                for (int col = startCol - radius; col <= startCol + radius; col++) {
                    if (Math.abs(row - startRow) != radius && Math.abs(col - startCol) != radius) {
                        continue;
                    }
                    if (row < 0 || row >= rows || col < 0 || col >= cols) {
                        continue;
                    }

                    double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
                    double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
                    double candidateX = centerX - renderWidth / 2.0;
                    double candidateY = centerY - renderHeight + (renderHeight * ENEMY_FOOT_COLLISION_HEIGHT_RATIO) / 2.0;

                    if (!isSafeEnemySpawn(candidateX, candidateY, renderWidth, renderHeight)) {
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

        System.err.println("Khong tim thay vi tri spawn an toan cho enemy level " + lvlID);
        return new double[]{preferredX, preferredY};
    }

    private boolean isSafeEnemySpawn(double x, double y, double renderWidth, double renderHeight) {
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        if (x < 0 || y < 0 || x + renderWidth > cols * TILE_SIZE || y + renderHeight > rows * TILE_SIZE) {
            return false;
        }

        Rectangle2D collisionBox = spawnCollisionBox(x, y, renderWidth, renderHeight);
        int left = (int) Math.floor(collisionBox.getMinX());
        int right = (int) Math.floor(collisionBox.getMaxX() - 1);
        int top = (int) Math.floor(collisionBox.getMinY());
        int bottom = (int) Math.floor(collisionBox.getMaxY() - 1);
        int centerX = (int) Math.floor(collisionBox.getMinX() + collisionBox.getWidth() / 2.0);
        int centerY = (int) Math.floor(collisionBox.getMinY() + collisionBox.getHeight() / 2.0);

        if (isBlockedByMap(left, top) || isBlockedByMap(right, top)
                || isBlockedByMap(left, bottom) || isBlockedByMap(right, bottom)
                || isBlockedByMap(centerX, centerY)) {
            return false;
        }

        if (player != null && player.getCollisionBoundary().intersects(collisionBox)) {
            return false;
        }

        for (Enemy enemy : enemyManager.getEnemyList()) {
            if (enemy.getHp() > 0 && enemy.getCollisionBoundary().intersects(collisionBox)) {
                return false;
            }
        }

        return true;
    }

    private Rectangle2D spawnCollisionBox(double x, double y, double renderWidth, double renderHeight) {
        double w = renderWidth * ENEMY_FOOT_COLLISION_WIDTH_RATIO;
        double h = renderHeight * ENEMY_FOOT_COLLISION_HEIGHT_RATIO;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }

    private boolean isBlockedByMap(int pixelX, int pixelY) {
        int col = pixelX / TILE_SIZE;
        int row = pixelY / TILE_SIZE;
        if (row < 0 || row >= map.getLoadedRowCount() || col < 0 || col >= map.getLoadedColCount()) {
            return true;
        }

        int tileId1 = map.mapTileNum[row][col];
        com.hust.game.map.Tile tile1 = map.tiles.get(tileId1);
        if (tile1 == null || tile1.collision) {
            return true;
        }

        int tileId2 = map.mapTileNumLayer2[row][col];
        if (tileId2 == -15) {
            return true;
        }
        if (tileId2 > 0) {
            com.hust.game.map.Tile tile2 = map.tiles.get(tileId2);
            return tile2 != null && tile2.collision;
        }
        return false;
    }

    private double collisionCenterX(double x, double renderWidth) {
        return x + renderWidth / 2.0;
    }

    private double collisionCenterY(double y, double renderHeight) {
        return y + renderHeight - (renderHeight * ENEMY_FOOT_COLLISION_HEIGHT_RATIO) / 2.0;
    }

    private int clampToRange(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    boolean isVictory(){
        return enemyManager.getEnemyList().isEmpty();
    }

    public MapManager getMap() {
        return map;
    }

    private void spawnNpc() {
        if (lvlID != 1) {
            npc = null;
            return;
        }

        Image idle1 = loadFirstAvailableImage("/assets/npc/Idle.png", "/assets/npc/Idle.png");
        Image idle2 = loadOptionalImage("/assets/npc/Idle_2.png");
        Image idle3 = loadOptionalImage("/assets/npc/Idle_3.png");
        Image dialogue = loadOptionalImage("/assets/npc/Dialogue.png");
        Image approval = loadOptionalImage("/assets/npc/Approval.png");
        Image manaPotion = loadOptionalImage("/assets/items/mana_potion.png");
        Image healthPotion = loadOptionalImage("/assets/items/health_potion.png");

        if (idle1 == null || idle2 == null || dialogue == null || manaPotion == null || healthPotion == null) {
            System.err.println("Không thể sinh NPC level 1 vì thiếu sprite sheet NPC hoặc item.");
            npc = null;
            return;
        }

        int[] tile = chooseNpcTile();
        double x = tile[0] * TILE_SIZE;
        double y = tile[1] * TILE_SIZE;
        npc = new Npc(x, y, idle1, idle2, idle3, dialogue, approval, manaPotion, healthPotion);
    }

    private int[] chooseNpcTile() {
        List<int[]> candidates = new ArrayList<>();
        int rows = Math.max(1, map.getLoadedRowCount());
        int cols = Math.max(1, map.getLoadedColCount());
        int playerSpawnCol = 4;
        int playerSpawnRow = 16;
        int minDistance = 8;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int tileId = map.mapTileNum[row][col];
                com.hust.game.map.Tile tile = map.tiles.get(tileId);
                if (tile == null || tile.collision) {
                    continue;
                }
                int distance = Math.abs(col - playerSpawnCol) + Math.abs(row - playerSpawnRow);
                if (distance < minDistance) {
                    continue;
                }
                candidates.add(new int[]{col, row});
            }
        }

        if (candidates.isEmpty()) {
            return new int[]{playerSpawnCol + 4, playerSpawnRow};
        }

        Random random = new Random(LEVEL1_NPC_SEED);
        return candidates.get(random.nextInt(candidates.size()));
    }

    private Image loadOptionalImage(String path) {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Không tìm thấy ảnh: " + path);
                return null;
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh: " + path);
            return null;
        }
    }

    private Image loadFirstAvailableImage(String primaryPath, String fallbackPath) {
        Image image = loadOptionalImage(primaryPath);
        return image != null ? image : loadOptionalImage(fallbackPath);
    }
}
