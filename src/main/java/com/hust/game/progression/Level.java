package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.npc.Npc;
import com.hust.game.entities.items.Chest;
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
    private static final int SMART_ROOM_MIN_WALKABLE_TILES = 25;
    private static final int SMART_ENEMIES_PER_ROOM = 5;
    private static final int ROOM_SCAN_RADIUS = SMART_ROOM_SIZE / 2;
    private static final int ROOM_ZONE_COUNT = 3;
    private static final int ROOM_CENTER_MIN_DISTANCE_TILES = 10;
    private static final int ENEMY_SPAWN_MIN_DISTANCE_TILES = 3;
    private static final int ROOM_SPAWN_SEARCH_RADIUS = 6;
    private static final int LEVEL1_EARLY_ZONE_ENEMY_LIMIT = 3;
    private static final int LEVEL1_SMART_ROOM_LIMIT = 8;
    private static final int LEVEL1_TEST_ENEMY_LIMIT = LEVEL1_SMART_ROOM_LIMIT * SMART_ENEMIES_PER_ROOM;
    private static final int LEVEL2_SMART_ROOM_LIMIT = 7;
    private static final int LEVEL2_WITCH_COUNT = 5;
    private static final int LEVEL2_KNIGHTS_PER_WITCH_GROUP = 2;
    private static final long LEVEL2_SMALL_ROOM_RANDOM_SEED = 2002L;
    private static final double ENEMY_FOOT_COLLISION_WIDTH_RATIO = 0.4;
    private static final double ENEMY_FOOT_COLLISION_HEIGHT_RATIO = 0.2;
    private static final double LEVEL2_WITCH_RENDER_HEIGHT = 150.0;
    private static final long LEVEL1_NPC_SEED = 1001L;
    private static final String LEVEL1_NPC_TIP = "Cách đánh quái đơn giản lắm! Bạn chỉ cần canh thời gian đánh trúng!\n"
            + "Nếu thấy quái chuẩn bị đánh, hãy né hoặc chém đúng nhịp để phản đòn.";
    private static final String BOSS_NPC_TIP = "Skill có sự thay đổi đó!\n"
            + "Bấm K để triệu hồi companion hỗ trợ đánh boss.\n"
            + "Hãy tận dụng nó đúng lúc nhé!";

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
        map.mapEntities.removeIf(entity -> entity instanceof Chest chest && chest.isReadyToRemove());
        
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
            spawnSmartLevel1Enemies();
        }
        else if(lvlID == 2){
            spawnLevel2Enemies();
        }
        else if (lvlID == 3) {
            // Spawn boss ở ô (31, 20)
            enemyManager.spawnEnemy("FinalBoss", 30 * TILE_SIZE, 17 * TILE_SIZE, bossImg, 5, TILE_SIZE * 3, TILE_SIZE * 3, player);
        }
    }

    private void spawnLevel2Enemies() {
        double knightW = TILE_SIZE * 2.0;
        double knightH = TILE_SIZE * 2.0;
        double witchH = LEVEL2_WITCH_RENDER_HEIGHT;
        double witchW = calculateWitchRenderWidth(witchH);

        boolean[][] reachable = buildReachableTilesFromPlayer();
        List<int[]> usedSpawnTiles = new ArrayList<>();
        List<int[]> selectedRooms = new ArrayList<>();
        List<int[]> roomStats = new ArrayList<>();
        int spawned = 0;

        spawned += spawnLevel2Group("top-left room",
                findLevel2RoomByRatio(reachable, 0.10, 0.10, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        // Player level 2 starts around tile (7,22); this anchor picks the reachable room directly below it.
        spawned += spawnLevel2HorizontalGroup("below-start corridor room",
                findLevel2RoomByTileAnchor(reachable, 6, 36, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("bottom-left room",
                findLevel2RoomByRatio(reachable, 0.16, 0.78, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("right connector room",
                findLevel2RoomByRatio(reachable, 0.50, 0.48, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("center large room lower",
                findLevel2RoomByRatio(reachable, 0.39, 0.45, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("center large room upper",
                findLevel2RoomByTileAnchor(reachable, 55, 18, selectedRooms, 8),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("cemetery outer upper",
                findLevel2RoomByRatio(reachable, 0.70, 0.28, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("cemetery outer lower",
                findLevel2RoomByRatio(reachable, 0.70, 0.62, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("cemetery inner upper",
                findLevel2RoomByRatio(reachable, 0.86, 0.28, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);
        spawned += spawnLevel2Group("cemetery inner lower",
                findLevel2RoomByRatio(reachable, 0.86, 0.62, selectedRooms, ROOM_CENTER_MIN_DISTANCE_TILES),
                1, reachable, usedSpawnTiles, selectedRooms, roomStats, witchW, witchH, knightW, knightH);

        for (int[] roomCenter : findLevel2SmallBottomRooms(reachable, selectedRooms, 2)) {
            spawned += spawnLevel2Group("small bottom room",
                    roomCenter, 1, reachable, usedSpawnTiles, selectedRooms, roomStats,
                    witchW, witchH, knightW, knightH);
        }

        logLevel2FinalSpawns(roomStats, reachable);
        System.out.println("Level 2 layout spawn: rooms=" + roomStats.size() + ", enemies=" + spawned);
    }

    private int spawnLevel2Group(String label, int[] roomCenter, int witchGroupCount,
            boolean[][] reachable, List<int[]> usedSpawnTiles, List<int[]> selectedRooms, List<int[]> roomStats,
            double witchW, double witchH, double knightW, double knightH) {
        if (roomCenter == null) {
            System.out.println("Level 2 spawn area " + label + ": not found");
            return 0;
        }

        if (isFarFromTiles(roomCenter[0], roomCenter[1], selectedRooms, 1)) {
            selectedRooms.add(new int[]{roomCenter[0], roomCenter[1]});
        }

        int spawned = 0;
        int spawnedWitches = 0;
        int spawnedKnights = 0;

        for (int group = 0; group < witchGroupCount; group++) {
            int witchSlot = getLevel2WitchGroupSlot(group);
            if (spawnSmartEnemyNearRoom("Witch", roomCenter, witchSlot, reachable, usedSpawnTiles,
                    witchW, witchH, witchImg, 25, witchSkillImg)) {
                spawned++;
                spawnedWitches++;

                int guardsSpawned = 0;
                for (int guard = 0; guard < LEVEL2_KNIGHTS_PER_WITCH_GROUP; guard++) {
                    int guardSlot = getLevel2GuardSlot(witchSlot, guard);
                    if (spawnSmartEnemyNearRoom("Knight", roomCenter, guardSlot, reachable, usedSpawnTiles,
                            knightW, knightH, knightImg, 8, knightSkillImg)) {
                        spawned++;
                        spawnedKnights++;
                        guardsSpawned++;
                    }
                }

                if (guardsSpawned < LEVEL2_KNIGHTS_PER_WITCH_GROUP) {
                    System.out.println("Level 2 spawn warning: " + label + " group " + (group + 1)
                            + " only spawned " + guardsSpawned + "/"
                            + LEVEL2_KNIGHTS_PER_WITCH_GROUP + " Knight guards.");
                }
            } else {
                System.out.println("Level 2 spawn warning: " + label + " group " + (group + 1)
                        + " could not spawn Witch, skipped its Knight guards.");
            }
        }

        addLevel2RoomStats(roomStats, roomCenter, spawnedWitches, spawnedKnights);
        int[] totals = findLevel2RoomStats(roomStats, roomCenter);
        int finalWitches = totals == null ? 0 : totals[2];
        int finalKnights = totals == null ? 0 : totals[3];
        int finalTotal = totals == null ? 0 : totals[4];
        int zone = getZoneForCol(roomCenter[0], reachable[0].length);
        int walkableCount = countReachableAround(reachable, roomCenter[0], roomCenter[1], ROOM_SCAN_RADIUS);

        System.out.println("Level 2 spawn area " + label + ": center=(" + roomCenter[0] + "," + roomCenter[1]
                + "), zone=" + zoneName(zone) + ", walkable=" + walkableCount
                + ", added Witch=" + spawnedWitches + ", added Knight=" + spawnedKnights
                + ", final Witch=" + finalWitches + ", final Knight=" + finalKnights
                + ", final total=" + finalTotal);

        return spawned;
    }

    private int spawnLevel2HorizontalGroup(String label, int[] roomCenter,
            boolean[][] reachable, List<int[]> usedSpawnTiles, List<int[]> selectedRooms, List<int[]> roomStats,
            double witchW, double witchH, double knightW, double knightH) {
        if (roomCenter == null) {
            System.out.println("Level 2 spawn area " + label + ": not found");
            return 0;
        }

        if (isFarFromTiles(roomCenter[0], roomCenter[1], selectedRooms, 1)) {
            selectedRooms.add(new int[]{roomCenter[0], roomCenter[1]});
        }

        int[][] witchOffsets = {
                {-6, 0},
                {12, 0},
                {30, 0}
        };
        int[][][] guardOffsets = {
                {{-7, -2}, {-7, 2}},
                {{12, -3}, {13, 3}},
                {{29, -2}, {31, 2}}
        };
        int spawned = 0;
        int spawnedWitches = 0;
        int spawnedKnights = 0;

        for (int group = 0; group < witchOffsets.length; group++) {
            if (spawnSmartEnemyNearRoomAtOffset("Witch", roomCenter,
                    witchOffsets[group][0], witchOffsets[group][1], reachable, usedSpawnTiles,
                    witchW, witchH, witchImg, 25, witchSkillImg)) {
                spawned++;
                spawnedWitches++;

                int guardsSpawned = 0;
                for (int guard = 0; guard < LEVEL2_KNIGHTS_PER_WITCH_GROUP; guard++) {
                    if (spawnSmartEnemyNearRoomAtOffset("Knight", roomCenter,
                            guardOffsets[group][guard][0], guardOffsets[group][guard][1],
                            reachable, usedSpawnTiles, knightW, knightH, knightImg, 8, knightSkillImg)) {
                        spawned++;
                        spawnedKnights++;
                        guardsSpawned++;
                    }
                }

                if (guardsSpawned < LEVEL2_KNIGHTS_PER_WITCH_GROUP) {
                    System.out.println("Level 2 spawn warning: " + label + " horizontal group " + (group + 1)
                            + " only spawned " + guardsSpawned + "/"
                            + LEVEL2_KNIGHTS_PER_WITCH_GROUP + " Knight guards.");
                }
            } else {
                System.out.println("Level 2 spawn warning: " + label + " horizontal group " + (group + 1)
                        + " could not spawn Witch, skipped its Knight guards.");
            }
        }

        addLevel2RoomStats(roomStats, roomCenter, spawnedWitches, spawnedKnights);
        int[] totals = findLevel2RoomStats(roomStats, roomCenter);
        int finalWitches = totals == null ? 0 : totals[2];
        int finalKnights = totals == null ? 0 : totals[3];
        int finalTotal = totals == null ? 0 : totals[4];
        int zone = getZoneForCol(roomCenter[0], reachable[0].length);
        int walkableCount = countReachableAround(reachable, roomCenter[0], roomCenter[1], ROOM_SCAN_RADIUS);

        System.out.println("Level 2 spawn area " + label + ": center=(" + roomCenter[0] + "," + roomCenter[1]
                + "), zone=" + zoneName(zone) + ", walkable=" + walkableCount
                + ", added Witch=" + spawnedWitches + ", added Knight=" + spawnedKnights
                + ", final Witch=" + finalWitches + ", final Knight=" + finalKnights
                + ", final total=" + finalTotal);

        return spawned;
    }

    private int getLevel2WitchGroupSlot(int group) {
        int[] slots = {0, 7, 8, 5, 6, 3, 4};
        return slots[group % slots.length];
    }

    private int getLevel2GuardSlot(int witchSlot, int guardIndex) {
        switch (witchSlot % 9) {
            case 7:
                return guardIndex == 0 ? 4 : 6;
            case 8:
                return guardIndex == 0 ? 3 : 5;
            case 5:
                return guardIndex == 0 ? 1 : 3;
            case 6:
                return guardIndex == 0 ? 2 : 4;
            default:
                return guardIndex == 0 ? 1 : 2;
        }
    }

    private int[] findLevel2RoomByRatio(boolean[][] reachable, double colRatio, double rowRatio,
            List<int[]> excludedRooms, int minRoomDistance) {
        List<int[]> candidates = collectLevel2RoomCandidates(reachable, 3, 8);
        if (candidates.isEmpty()) {
            return null;
        }

        int rows = reachable.length;
        int cols = reachable[0].length;
        int targetCol = clampToRange((int) Math.round((cols - 1) * colRatio), 0, cols - 1);
        int targetRow = clampToRange((int) Math.round((rows - 1) * rowRatio), 0, rows - 1);
        int[] best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int[] candidate : candidates) {
            if (excludedRooms != null
                    && !isFarFromTiles(candidate[0], candidate[1], excludedRooms, minRoomDistance)) {
                continue;
            }

            int distance = Math.abs(candidate[0] - targetCol) + Math.abs(candidate[1] - targetRow);
            int score = distance * 100 - candidate[2];
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best == null ? null : new int[]{best[0], best[1]};
    }

    private int[] findLevel2RoomByTileAnchor(boolean[][] reachable, int targetCol, int targetRow,
            List<int[]> excludedRooms, int minRoomDistance) {
        List<int[]> candidates = collectLevel2RoomCandidates(reachable, 3, 8);
        if (candidates.isEmpty()) {
            return null;
        }

        int[] best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int[] candidate : candidates) {
            if (excludedRooms != null
                    && !isFarFromTiles(candidate[0], candidate[1], excludedRooms, minRoomDistance)) {
                continue;
            }

            int distance = Math.abs(candidate[0] - targetCol) + Math.abs(candidate[1] - targetRow);
            int score = distance * 100 - candidate[2];
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best == null ? null : new int[]{best[0], best[1]};
    }

    private List<int[]> findLevel2SmallBottomRooms(boolean[][] reachable, List<int[]> excludedRooms, int count) {
        List<int[]> candidates = collectLevel2RoomCandidates(reachable, 3, 8);
        List<int[]> filtered = new ArrayList<>();
        List<int[]> selected = new ArrayList<>();
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return selected;
        }

        int minRow = (int) Math.round(reachable.length * 0.70);
        int minCol = (int) Math.round(reachable[0].length * 0.25);
        int maxCol = (int) Math.round(reachable[0].length * 0.70);

        for (int[] candidate : candidates) {
            if (candidate[1] < minRow || candidate[0] < minCol || candidate[0] > maxCol) {
                continue;
            }
            if (excludedRooms != null
                    && !isFarFromTiles(candidate[0], candidate[1], excludedRooms, ROOM_CENTER_MIN_DISTANCE_TILES)) {
                continue;
            }
            filtered.add(candidate);
        }

        Random random = new Random(LEVEL2_SMALL_ROOM_RANDOM_SEED);
        while (!filtered.isEmpty() && selected.size() < count) {
            int index = random.nextInt(filtered.size());
            int[] candidate = filtered.remove(index);
            if (!isFarFromTiles(candidate[0], candidate[1], selected, ROOM_CENTER_MIN_DISTANCE_TILES + 4)) {
                continue;
            }
            selected.add(new int[]{candidate[0], candidate[1]});
        }

        return selected;
    }

    private List<int[]> collectLevel2RoomCandidates(boolean[][] reachable, int minCol, int minPlayerDistance) {
        List<int[]> candidates = new ArrayList<>();
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return candidates;
        }

        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();
        for (int row = ROOM_SCAN_RADIUS; row < reachable.length - ROOM_SCAN_RADIUS; row++) {
            for (int col = Math.max(minCol, ROOM_SCAN_RADIUS);
                    col < reachable[0].length - ROOM_SCAN_RADIUS; col++) {
                if (!reachable[row][col]) {
                    continue;
                }
                if (Math.abs(col - playerCol) + Math.abs(row - playerRow) < minPlayerDistance) {
                    continue;
                }

                int walkableCount = countReachableAround(reachable, col, row, ROOM_SCAN_RADIUS);
                if (walkableCount >= SMART_ROOM_MIN_WALKABLE_TILES) {
                    candidates.add(new int[]{col, row, walkableCount});
                }
            }
        }

        return candidates;
    }

    private void addLevel2RoomStats(List<int[]> roomStats, int[] roomCenter, int witches, int knights) {
        if (roomCenter == null || witches + knights <= 0) {
            return;
        }

        int[] stats = findLevel2RoomStats(roomStats, roomCenter);
        if (stats == null) {
            roomStats.add(new int[]{roomCenter[0], roomCenter[1], witches, knights, witches + knights});
            return;
        }

        stats[2] += witches;
        stats[3] += knights;
        stats[4] += witches + knights;
    }

    private int getLevel2RoomTotal(List<int[]> roomStats, int[] roomCenter) {
        int[] stats = findLevel2RoomStats(roomStats, roomCenter);
        return stats == null ? 0 : stats[4];
    }

    private int[] findLevel2RoomStats(List<int[]> roomStats, int[] roomCenter) {
        if (roomStats == null || roomCenter == null) {
            return null;
        }

        for (int[] stats : roomStats) {
            if (stats[0] == roomCenter[0] && stats[1] == roomCenter[1]) {
                return stats;
            }
        }
        return null;
    }

    private void logLevel2FinalSpawns(List<int[]> roomStats, boolean[][] reachable) {
        System.out.println("Level 2 final spawn list:");
        for (int[] stats : roomStats) {
            int zone = getZoneForCol(stats[0], reachable[0].length);
            int walkableCount = countReachableAround(reachable, stats[0], stats[1], ROOM_SCAN_RADIUS);
            System.out.println("  room center=(" + stats[0] + "," + stats[1] + "), zone=" + zoneName(zone)
                    + ", walkable=" + walkableCount + ", Witch=" + stats[2]
                    + ", Knight=" + stats[3] + ", total=" + stats[4]);
        }
    }

    private void spawnSmartLevel1Enemies() {
        double slimeW = 51;
        double slimeH = 31.5;
        double treeW = 96;
        double treeH = 96;

        boolean[][] reachable = buildReachableTilesFromPlayer();
        List<int[]> roomCenters = findRoomCenters(reachable, 15, LEVEL1_SMART_ROOM_LIMIT, 8);
        List<int[]> usedSpawnTiles = new ArrayList<>();
        List<int[]> level1RoomStats = new ArrayList<>();
        int spawned = 0;
        int earlyZoneSpawned = 0;

        for (int[] roomCenter : roomCenters) {
            int zone = getZoneForCol(roomCenter[0], reachable[0].length);
            int roomSpawnLimit = SMART_ENEMIES_PER_ROOM;
            if (zone == 0) {
                int earlyRemaining = Math.max(0, LEVEL1_EARLY_ZONE_ENEMY_LIMIT - earlyZoneSpawned);
                roomSpawnLimit = Math.min(roomSpawnLimit, earlyRemaining);
            }

            int roomSpawned = 0;
            int roomTrees = 0;
            int roomSlimes = 0;
            for (int slot = 0; slot < roomSpawnLimit; slot++) {
                boolean spawnedSlot;
                if (slot % 2 == 0) {
                    spawnedSlot = spawnSmartEnemyNearRoom("Tree", roomCenter, slot, reachable, usedSpawnTiles,
                            treeW, treeH, treeImg, 8, treeSkillImg);
                } else {
                    spawnedSlot = spawnSmartEnemyNearRoom("Slime", roomCenter, slot, reachable, usedSpawnTiles,
                            slimeW, slimeH, slimeImg, 8, null);
                }
                if (spawnedSlot) {
                    spawned++;
                    roomSpawned++;
                    if (slot % 2 == 0) {
                        roomTrees++;
                    } else {
                        roomSlimes++;
                    }
                    if (zone == 0) {
                        earlyZoneSpawned++;
                    }
                }
            }
            addLevel1RoomStats(level1RoomStats, roomCenter, roomSpawned, roomTrees, roomSlimes);
            logRoomSpawn(1, roomCenter, reachable, roomSpawned);
        }

        spawned += spawnLevel1BonusRooms(reachable, roomCenters, usedSpawnTiles, level1RoomStats,
                treeW, treeH, slimeW, slimeH);
        System.out.println("Level 1 smart spawn: rooms=" + roomCenters.size() + ", enemies=" + spawned);
    }

    private int spawnLevel1BonusRooms(boolean[][] reachable, List<int[]> roomCenters, List<int[]> usedSpawnTiles,
            List<int[]> roomStats, double treeW, double treeH, double slimeW, double slimeH) {
        int spawned = 0;

        int[] upperStartRoom = findLevel1UpperStartRoom(reachable, roomCenters);
        spawned += spawnLevel1BonusGroup("upper-start", upperStartRoom, 2, 1, reachable,
                usedSpawnTiles, roomStats, treeW, treeH, slimeW, slimeH);

        int[] topLeftRoom = findLevel1TopLeftRoom(reachable, upperStartRoom);
        spawned += spawnLevel1BonusGroup("top-left", topLeftRoom, 1, 2, reachable,
                usedSpawnTiles, roomStats, treeW, treeH, slimeW, slimeH);

        return spawned;
    }

    private int spawnLevel1BonusGroup(String label, int[] roomCenter, int treeCount, int slimeCount,
            boolean[][] reachable, List<int[]> usedSpawnTiles, List<int[]> roomStats,
            double treeW, double treeH, double slimeW, double slimeH) {
        if (roomCenter == null) {
            System.out.println("Level 1 bonus room " + label + ": not found");
            return 0;
        }

        int nextSlot = getLevel1RoomTotal(roomStats, roomCenter);
        int spawned = 0;
        int spawnedTrees = 0;
        int spawnedSlimes = 0;

        for (int i = 0; i < treeCount; i++) {
            if (spawnSmartEnemyNearRoom("Tree", roomCenter, nextSlot + spawned, reachable, usedSpawnTiles,
                    treeW, treeH, treeImg, 8, treeSkillImg)) {
                spawned++;
                spawnedTrees++;
            }
        }

        for (int i = 0; i < slimeCount; i++) {
            if (spawnSmartEnemyNearRoom("Slime", roomCenter, nextSlot + spawned, reachable, usedSpawnTiles,
                    slimeW, slimeH, slimeImg, 8, null)) {
                spawned++;
                spawnedSlimes++;
            }
        }

        addLevel1RoomStats(roomStats, roomCenter, spawned, spawnedTrees, spawnedSlimes);
        int[] totals = findLevel1RoomStats(roomStats, roomCenter);
        int finalTrees = totals == null ? 0 : totals[3];
        int finalSlimes = totals == null ? 0 : totals[4];
        int finalTotal = totals == null ? 0 : totals[2];
        int zone = getZoneForCol(roomCenter[0], reachable[0].length);
        int walkableCount = countReachableAround(reachable, roomCenter[0], roomCenter[1], ROOM_SCAN_RADIUS);
        System.out.println("Level 1 bonus room " + label + ": center=(" + roomCenter[0] + "," + roomCenter[1]
                + "), zone=" + zoneName(zone) + ", walkable=" + walkableCount
                + ", added Tree=" + spawnedTrees + ", added Slime=" + spawnedSlimes
                + ", final Tree=" + finalTrees + ", final Slime=" + finalSlimes
                + ", final total=" + finalTotal);

        return spawned;
    }

    private int[] findLevel1UpperStartRoom(boolean[][] reachable, List<int[]> roomCenters) {
        if (roomCenters == null || roomCenters.isEmpty()) {
            return findLevel1UpperStartRoomFallback(reachable, null);
        }

        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();
        int[] best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int[] roomCenter : roomCenters) {
            int zone = getZoneForCol(roomCenter[0], reachable[0].length);
            if (zone != 0 || roomCenter[1] >= playerRow) {
                continue;
            }

            int score = Math.abs(roomCenter[0] - playerCol) * 2 + Math.abs(roomCenter[1] - playerRow);
            if (score < bestScore) {
                bestScore = score;
                best = roomCenter;
            }
        }

        return best != null ? best : findLevel1UpperStartRoomFallback(reachable, null);
    }

    private int[] findLevel1UpperStartRoomFallback(boolean[][] reachable, int[] excludedRoom) {
        List<int[]> candidates = collectLevel1RoomCandidates(reachable, 1, 8);
        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();
        int[] best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int[] candidate : candidates) {
            if (!isDifferentRoom(candidate, excludedRoom)) {
                continue;
            }
            if (candidate[1] >= playerRow) {
                continue;
            }

            int score = Math.abs(candidate[0] - playerCol) * 2 + Math.abs(candidate[1] - playerRow);
            if (score < bestScore) {
                bestScore = score;
                best = new int[]{candidate[0], candidate[1]};
            }
        }

        return best;
    }

    private int[] findLevel1TopLeftRoom(boolean[][] reachable, int[] excludedRoom) {
        List<int[]> candidates = collectLevel1RoomCandidates(reachable, 1, 8);
        int[] best = null;
        int bestScore = Integer.MAX_VALUE;

        for (int[] candidate : candidates) {
            if (!isDifferentRoom(candidate, excludedRoom)) {
                continue;
            }

            int score = candidate[0] * 2 + candidate[1] * 3 - candidate[2];
            if (score < bestScore) {
                bestScore = score;
                best = new int[]{candidate[0], candidate[1]};
            }
        }

        return best;
    }

    private List<int[]> collectLevel1RoomCandidates(boolean[][] reachable, int minCol, int minPlayerDistance) {
        List<int[]> candidates = new ArrayList<>();
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return candidates;
        }

        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();

        for (int row = ROOM_SCAN_RADIUS; row < reachable.length - ROOM_SCAN_RADIUS; row++) {
            for (int col = Math.max(minCol, ROOM_SCAN_RADIUS); col < reachable[0].length - ROOM_SCAN_RADIUS; col++) {
                if (!reachable[row][col]) {
                    continue;
                }
                if (Math.abs(col - playerCol) + Math.abs(row - playerRow) < minPlayerDistance) {
                    continue;
                }

                int walkableCount = countReachableAround(reachable, col, row, ROOM_SCAN_RADIUS);
                if (walkableCount >= SMART_ROOM_MIN_WALKABLE_TILES) {
                    candidates.add(new int[]{col, row, walkableCount});
                }
            }
        }

        return candidates;
    }

    private boolean isDifferentRoom(int[] candidate, int[] roomCenter) {
        if (candidate == null || roomCenter == null) {
            return true;
        }

        return Math.abs(candidate[0] - roomCenter[0]) + Math.abs(candidate[1] - roomCenter[1])
                >= ROOM_CENTER_MIN_DISTANCE_TILES;
    }

    private void addLevel1RoomStats(List<int[]> roomStats, int[] roomCenter, int total, int trees, int slimes) {
        if (total <= 0 || roomCenter == null) {
            return;
        }

        int[] stats = findLevel1RoomStats(roomStats, roomCenter);
        if (stats == null) {
            roomStats.add(new int[]{roomCenter[0], roomCenter[1], total, trees, slimes});
            return;
        }

        stats[2] += total;
        stats[3] += trees;
        stats[4] += slimes;
    }

    private int getLevel1RoomTotal(List<int[]> roomStats, int[] roomCenter) {
        int[] stats = findLevel1RoomStats(roomStats, roomCenter);
        return stats == null ? 0 : stats[2];
    }

    private int[] findLevel1RoomStats(List<int[]> roomStats, int[] roomCenter) {
        if (roomStats == null || roomCenter == null) {
            return null;
        }

        for (int[] stats : roomStats) {
            if (stats[0] == roomCenter[0] && stats[1] == roomCenter[1]) {
                return stats;
            }
        }
        return null;
    }

    private List<int[]> findRoomCenters(boolean[][] reachable, int minCol, int roomLimit, int minPlayerDistance) {
        List<int[]> roomCenters = new ArrayList<>();
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return roomCenters;
        }

        int rows = reachable.length;
        int cols = reachable[0].length;
        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();
        List<List<int[]>> candidatesByZone = new ArrayList<>();
        for (int zone = 0; zone < ROOM_ZONE_COUNT; zone++) {
            candidatesByZone.add(new ArrayList<>());
        }

        for (int row = ROOM_SCAN_RADIUS; row < rows - ROOM_SCAN_RADIUS; row++) {
            for (int col = Math.max(minCol, ROOM_SCAN_RADIUS); col < cols - ROOM_SCAN_RADIUS; col++) {
                if (!reachable[row][col]) {
                    continue;
                }
                if (Math.abs(col - playerCol) + Math.abs(row - playerRow) < minPlayerDistance) {
                    continue;
                }
                if (isNpcZone(col, row)) {
                    continue;
                }
                int walkableCount = countReachableAround(reachable, col, row, ROOM_SCAN_RADIUS);
                if (walkableCount < SMART_ROOM_MIN_WALKABLE_TILES) {
                    continue;
                }

                int zone = getZoneForCol(col, cols);
                candidatesByZone.get(zone).add(new int[]{col, row, walkableCount});
            }
        }

        int baseLimitPerZone = Math.max(1, roomLimit / ROOM_ZONE_COUNT);
        int extraRooms = Math.max(0, roomLimit % ROOM_ZONE_COUNT);
        for (int zone = 0; zone < ROOM_ZONE_COUNT && roomCenters.size() < roomLimit; zone++) {
            int zoneLimit = baseLimitPerZone + (zone >= ROOM_ZONE_COUNT - extraRooms ? 1 : 0);
            List<int[]> zoneCandidates = candidatesByZone.get(zone);
            int selectedInZone = 0;

            while (selectedInZone < zoneLimit && roomCenters.size() < roomLimit) {
                int bestIndex = -1;
                int bestScore = Integer.MIN_VALUE;

                for (int i = 0; i < zoneCandidates.size(); i++) {
                    int[] candidate = zoneCandidates.get(i);
                    int col = candidate[0];
                    int row = candidate[1];
                    if (!isFarFromTiles(col, row, roomCenters, ROOM_CENTER_MIN_DISTANCE_TILES)) {
                        continue;
                    }

                    int score = scoreRoomCandidate(candidate, zone, rows, cols, roomCenters);
                    if (score > bestScore) {
                        bestScore = score;
                        bestIndex = i;
                    }
                }

                if (bestIndex < 0) {
                    break;
                }

                int[] selected = zoneCandidates.remove(bestIndex);
                roomCenters.add(new int[]{selected[0], selected[1]});
                selectedInZone++;
                System.out.println("Level " + lvlID + " room candidate: center=("
                        + selected[0] + "," + selected[1] + "), zone=" + zoneName(zone)
                        + ", walkable=" + selected[2]);
            }
        }

        return roomCenters;
    }

    private int scoreRoomCandidate(int[] candidate, int zone, int rows, int cols, List<int[]> selectedRooms) {
        int col = candidate[0];
        int row = candidate[1];
        int walkableCount = candidate[2];
        int zoneStartCol = zone * cols / ROOM_ZONE_COUNT;
        int zoneEndCol = ((zone + 1) * cols / ROOM_ZONE_COUNT) - 1;
        if (zone == ROOM_ZONE_COUNT - 1) {
            zoneEndCol = cols - 1;
        }

        int zoneCenterCol = (zoneStartCol + zoneEndCol) / 2;
        int zoneCenterRow = rows / 2;
        int distanceFromZoneCenter = Math.abs(col - zoneCenterCol) + Math.abs(row - zoneCenterRow);
        int distanceFromSelected = Math.min(nearestDistanceToTiles(col, row, selectedRooms), 20);

        return walkableCount * 100 + distanceFromSelected * 6 - distanceFromZoneCenter * 2;
    }

    private int nearestDistanceToTiles(int col, int row, List<int[]> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            return 20;
        }

        int nearest = Integer.MAX_VALUE;
        for (int[] tile : tiles) {
            int distance = Math.abs(col - tile[0]) + Math.abs(row - tile[1]);
            if (distance < nearest) {
                nearest = distance;
            }
        }
        return nearest;
    }

    private int getZoneForCol(int col, int cols) {
        if (cols <= 0) {
            return 0;
        }
        int zone = (col * ROOM_ZONE_COUNT) / cols;
        return clampToRange(zone, 0, ROOM_ZONE_COUNT - 1);
    }

    private String zoneName(int zone) {
        switch (zone) {
            case 0:
                return "early";
            case 1:
                return "middle";
            default:
                return "late";
        }
    }

    private void logRoomSpawn(int level, int[] roomCenter, boolean[][] reachable, int spawned) {
        int zone = getZoneForCol(roomCenter[0], reachable[0].length);
        int walkableCount = countReachableAround(reachable, roomCenter[0], roomCenter[1], ROOM_SCAN_RADIUS);
        System.out.println("Level " + level + " room spawn: center=(" + roomCenter[0] + "," + roomCenter[1]
                + "), zone=" + zoneName(zone) + ", walkable=" + walkableCount + ", spawned=" + spawned);
    }

    private boolean spawnSmartEnemyNearRoom(String enemyType, int[] roomCenter, int slot,
            boolean[][] reachable, List<int[]> usedSpawnTiles,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        int[][] offsets = {
                {0, 0},
                {-3, -2},
                {3, 2},
                {-2, 3},
                {2, -3},
                {-3, 2},
                {3, -2},
                {0, -3},
                {0, 3}
        };

        int[] offset = offsets[slot % offsets.length];
        return spawnSmartEnemyNearRoomAtOffset(enemyType, roomCenter, offset[0], offset[1],
                reachable, usedSpawnTiles, renderWidth, renderHeight, sprite, frames, skillSprite);
    }

    private boolean spawnSmartEnemyNearRoomAtOffset(String enemyType, int[] roomCenter, int offsetCol, int offsetRow,
            boolean[][] reachable, List<int[]> usedSpawnTiles,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        int targetCol = roomCenter[0] + offsetCol;
        int targetRow = roomCenter[1] + offsetRow;
        int[] spawnTile = findSpawnTileNear(targetCol, targetRow, reachable, usedSpawnTiles,
                renderWidth, renderHeight);
        if (spawnTile == null) {
            return false;
        }

        if (!trySpawnEnemyAtExactTile(enemyType, spawnTile[0], spawnTile[1],
                renderWidth, renderHeight, sprite, frames, skillSprite)) {
            return false;
        }

        usedSpawnTiles.add(spawnTile);
        return true;
    }

    private int[] findSpawnTileNear(int targetCol, int targetRow, boolean[][] reachable,
            List<int[]> usedSpawnTiles, double renderWidth, double renderHeight) {
        if (reachable == null || reachable.length == 0 || reachable[0].length == 0) {
            return null;
        }

        int rows = reachable.length;
        int cols = reachable[0].length;
        int playerCol = getPlayerTileCol();
        int playerRow = getPlayerTileRow();

        for (int radius = 0; radius <= ROOM_SPAWN_SEARCH_RADIUS; radius++) {
            int[] best = null;
            int bestDistance = Integer.MAX_VALUE;

            for (int row = targetRow - radius; row <= targetRow + radius; row++) {
                for (int col = targetCol - radius; col <= targetCol + radius; col++) {
                    if (Math.abs(row - targetRow) != radius && Math.abs(col - targetCol) != radius) {
                        continue;
                    }
                    if (row < ROOM_SCAN_RADIUS || row >= rows - ROOM_SCAN_RADIUS
                            || col < ROOM_SCAN_RADIUS || col >= cols - ROOM_SCAN_RADIUS) {
                        continue;
                    }
                    if (!reachable[row][col] || !hasRoomClearance(reachable, col, row)) {
                        continue;
                    }
                    if (Math.abs(col - playerCol) + Math.abs(row - playerRow) < 8) {
                        continue;
                    }
                    if (!isFarFromTiles(col, row, usedSpawnTiles, ENEMY_SPAWN_MIN_DISTANCE_TILES)) {
                        continue;
                    }
                    if (!isExactEnemySpawnSafe(col, row, renderWidth, renderHeight)) {
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

    private boolean hasRoomClearance(boolean[][] reachable, int centerCol, int centerRow) {
        return countReachableAround(reachable, centerCol, centerRow, ROOM_SCAN_RADIUS)
                >= SMART_ROOM_MIN_WALKABLE_TILES;
    }

    private int countReachableAround(boolean[][] reachable, int centerCol, int centerRow, int radius) {
        int count = 0;
        for (int row = centerRow - radius; row <= centerRow + radius; row++) {
            for (int col = centerCol - radius; col <= centerCol + radius; col++) {
                if (row >= 0 && row < reachable.length && col >= 0 && col < reachable[0].length
                        && reachable[row][col]) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isExactEnemySpawnSafe(int col, int row, double renderWidth, double renderHeight) {
        double[] position = enemyPositionForTile(col, row, renderWidth, renderHeight);
        return isSafeEnemySpawn(position[0], position[1], renderWidth, renderHeight);
    }

    private boolean trySpawnEnemyAtExactTile(String enemyType, int col, int row,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        double[] position = enemyPositionForTile(col, row, renderWidth, renderHeight);
        if (!isSafeEnemySpawn(position[0], position[1], renderWidth, renderHeight)) {
            return false;
        }

        enemyManager.spawnEnemy(enemyType, position[0], position[1], sprite, frames,
                renderWidth, renderHeight, player, skillSprite);
        return true;
    }

    private double[] enemyPositionForTile(int col, int row, double renderWidth, double renderHeight) {
        double centerX = col * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = row * TILE_SIZE + TILE_SIZE / 2.0;
        double x = centerX - renderWidth / 2.0;
        double y = centerY - renderHeight + (renderHeight * ENEMY_FOOT_COLLISION_HEIGHT_RATIO) / 2.0;
        return new double[]{x, y};
    }

    private boolean isFarFromTiles(int col, int row, List<int[]> tiles, int minDistance) {
        for (int[] tile : tiles) {
            if (Math.abs(col - tile[0]) + Math.abs(row - tile[1]) < minDistance) {
                return false;
            }
        }
        return true;
    }

    private int getPlayerTileCol() {
        if (player == null) {
            return 4;
        }
        Rectangle2D playerBox = player.getCollisionBoundary();
        return (int) ((playerBox.getMinX() + playerBox.getWidth() / 2.0) / TILE_SIZE);
    }

    private int getPlayerTileRow() {
        if (player == null) {
            return 16;
        }
        Rectangle2D playerBox = player.getCollisionBoundary();
        return (int) ((playerBox.getMinY() + playerBox.getHeight() / 2.0) / TILE_SIZE);
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

        // Sau khi thuật toán loang xong, ta đục lỗ khu vực NPC thành tường (false)
        // Để tất cả các logic sinh quái phía sau đều tự động tránh xa khu vực này
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (isNpcZone(c, r)) {
                    reachable[r][c] = false;
                }
            }
        }

        return reachable;
    }

    private boolean isNpcZone(int col, int row) {
        if (lvlID == 1 && col >= 61 && col <= 68 && row >= 6 && row <= 30) {
            return true;
        }
        if (lvlID == 2 && col >= 112 && col <= 119 && row >= 14 && row <= 19) {
            return true;
        }
        return false;
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

    private void spawnEnemyFromRoomSlot(String enemyType, List<int[]> roomTiles, int slot, int totalSlots,
            double renderWidth, double renderHeight, Image sprite, int frames, Image skillSprite) {
        if (roomTiles == null || roomTiles.isEmpty()) {
            return;
        }

        int index = ((slot + 1) * roomTiles.size()) / (totalSlots + 1);
        index = clampToRange(index, 0, roomTiles.size() - 1);
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
        if (!isSafeEnemySpawn(safe[0], safe[1], renderWidth, renderHeight)) {
            return;
        }

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

        int col = centerX / TILE_SIZE;
        int row = centerY / TILE_SIZE;
        if (isNpcZone(col, row)) {
            return false;
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
        if (lvlID < 1 || lvlID > 3) {
            npc = null;
            return;
        }

        Image idle1 = loadFirstAvailableImage("/assets/npc/Idle.png", "/assets/npc/Idle.png");
        Image idle2 = loadOptionalImage("/assets/npc/Idle_2.png");
        Image dialogue = loadOptionalImage("/assets/npc/Dialogue.png");
        Image idle3 = dialogue;
        Image approval = idle2;
        Image manaPotion = loadOptionalImage("/assets/items/mana_potion.png");
        Image healthPotion = loadOptionalImage("/assets/items/health_potion.png");

        if (idle1 == null || idle2 == null || dialogue == null || manaPotion == null || healthPotion == null) {
            System.err.println("Không thể sinh NPC level " + lvlID + " vì thiếu sprite sheet NPC hoặc item.");
            npc = null;
            return;
        }

        int[] tile = new int[]{0, 0};
        if (lvlID == 1) {
            tile = new int[]{66, 19};
        } else if (lvlID == 2) {
            tile = new int[]{115, 19};
        } else if (lvlID == 3) {
            tile = new int[]{8, 16};
        }


        double x = tile[0] * TILE_SIZE;
        double y = tile[1] * TILE_SIZE;
        String tipText = lvlID == 3 ? BOSS_NPC_TIP : LEVEL1_NPC_TIP;
        npc = new Npc(x, y, idle1, idle2, idle3, dialogue, approval, manaPotion, healthPotion, tipText);
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
