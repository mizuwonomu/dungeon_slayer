package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.List;
import com.hust.game.entities.environment.Gate;


public class Level {

    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;

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

    void spawnEnemy(){
        if (lvlID == 0) {
            // Phòng 2: Slime bất động
            enemyManager.spawnEnemy("Slime", 15 * TILE_SIZE, 5 * TILE_SIZE, slimeImg, 8, TILE_SIZE, TILE_SIZE, player);
            // Phòng 3: Knight bất động
            enemyManager.spawnEnemy("Knight", 29 * TILE_SIZE, 4 * TILE_SIZE, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, player, knightSkillImg);
            // Phòng 4: 3 Tree bình thường
            enemyManager.spawnEnemy("Tree", 45 * TILE_SIZE, 3 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 47 * TILE_SIZE, 6 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 49 * TILE_SIZE, 4 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            
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
            
            // 2. Quét map theo các vùng 7x7 để phát hiện "Phòng" và bỏ qua "Hành lang"
            for (int r = 0; r < GameConstants.MAX_WORLD_ROW; r += 7) {
                for (int c = 15; c < GameConstants.MAX_WORLD_COL; c += 7) { // c >= 15 để chừa trống phòng spawn đầu tiên
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
                        int numEnemies = 4 + (int)(Math.random() * 3); // 4-6 quái (trung bình 5 con)
                        for (int k = 0; k < numEnemies && !roomTiles.isEmpty(); k++) {
                            int randIdx = (int)(Math.random() * roomTiles.size());
                            int[] pos = roomTiles.get(randIdx);
                            roomTiles.remove(randIdx); // Xóa khỏi danh sách để tránh sinh 2 con quái đè lên cùng 1 ô
                            
                            double spawnX = pos[0] * TILE_SIZE;
                            double spawnY = pos[1] * TILE_SIZE;
                            
                            // Tỉ lệ 50% ra Cây, 50% ra Slime
                            if (Math.random() > 0.5) {
                                enemyManager.spawnEnemy("Tree", spawnX, spawnY, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
                            } else {
                                enemyManager.spawnEnemy("Slime", spawnX, spawnY, slimeImg, 8, TILE_SIZE, TILE_SIZE, player);
                            }
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
            enemyManager.spawnEnemy("Witch", WIDTH / 2 - 250 , HEIGHT / 2 , witchImg, 25, TILE_SIZE, TILE_SIZE,
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
}
