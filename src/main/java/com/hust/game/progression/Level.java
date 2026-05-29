package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.npc.Npc;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.hust.game.entities.environment.Gate;


public class Level {

    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;
    private static final int LEVEL1_TEST_ENEMY_LIMIT = 2;
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
            for (int r = 0; r < GameConstants.MAX_WORLD_ROW && spawnedForTest < LEVEL1_TEST_ENEMY_LIMIT; r += 7) {
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
                        int numEnemies = LEVEL1_TEST_ENEMY_LIMIT - spawnedForTest;
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
                                enemyManager.spawnEnemy("Tree", spawnX, spawnY - 48, treeImg, 8, 96, 96, player, treeSkillImg);
                            } else {
                                enemyManager.spawnEnemy("Slime", spawnX, spawnY, slimeImg, 8, 51, 31.5, player);
                            }
                            spawnedForTest++;
                        }
                    }
                }
            }
        }
        else if(lvlID == 2){
            enemyManager.spawnEnemy("Knight", WIDTH / 2 - 200, HEIGHT / 2 - 200, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, 
                                    player, knightSkillImg);
            enemyManager.spawnEnemy("Knight", WIDTH / 2 + 50 , HEIGHT / 2 - 200, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, 
                                    player, knightSkillImg);
            enemyManager.spawnEnemy("Witch", WIDTH / 2 - 100, HEIGHT / 2 - 100, witchImg, 25, TILE_SIZE, TILE_SIZE,
                                    player, witchSkillImg);
        }
        else if (lvlID == 3) {
            // Render ảnh boss giảm nửa kích thước (146.25 x 144)
            // Dịch boss lên trên một chút để không bị dính vào tường
            enemyManager.spawnEnemy("FinalBoss", WIDTH / 2 - 73, HEIGHT / 2 - 250, bossImg, 8, 146.25, 144, player);
        }
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
