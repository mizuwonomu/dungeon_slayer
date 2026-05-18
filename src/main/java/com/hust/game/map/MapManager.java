package com.hust.game.map;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.items.HealthPotion;
import com.hust.game.entities.items.ManaPotion;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class MapManager {
    private static final int POTION_SPAWN_TOTAL = 3;
    private static final int POTION_MANA_COUNT = 2;
    public Map<Integer, Tile> tiles;
    public int[][] mapTileNum;
    public int level;
    public List<int[]> gatePositions;
    public List<com.hust.game.entities.base.BaseEntity> mapEntities; // Danh sách các vật thể cứng trên Map
    private int loadedRowCount;
    private int loadedColCount;

    public MapManager(int level) {
        this.level = level;
        tiles = new HashMap<>();
        gatePositions = new ArrayList<>();
        mapEntities = new ArrayList<>();
        mapTileNum = new int[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
        loadTiles();
        
        // Tái sử dụng map Level 2 cho màn Final Boss (Level 3)
        String mapPath = (level == 3) ? "/assets/maps/level2.txt" : "/assets/maps/level" + level + ".txt";
        loadMap(mapPath);
    }

    public void loadTiles() {
        for (TileType type : TileType.values()) {
            setup(type);
        }
    }

    private void setup(TileType tileType) {
        try {
            int index = tileType.getId();
            Tile tile = new Tile();
            // Sử dụng chữ thường 'tiles' để tránh lỗi case-sensitive khi build bằng Maven
            java.io.InputStream is = getClass().getResourceAsStream("/assets/tiles/" + tileType.getImageName());
            if (is != null) {
                tile.image = new Image(is);
            } else {
                System.err.println("Cảnh báo: Không tìm thấy ảnh " + tileType.getImageName());
            }
            tile.collision = tileType.isSolid();
            tiles.put(index, tile);
        } catch (Exception e) {
            System.err.println("Lỗi load ảnh tile: " + tileType.getImageName());
            e.printStackTrace();
        }
    }

    public void loadMap(String filePath) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(filePath);
            if (is == null) {
                System.err.println("Lỗi: Không tìm thấy file map " + filePath);
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            loadedRowCount = 0;
            loadedColCount = 0;
            for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
                String line = br.readLine();
                if (line == null) break;
                
                // Fix an toàn: Xóa ký tự tàng hình BOM (UTF-8) ở đầu file txt và dòng trống
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) { row--; continue; } // Bỏ qua dòng trống, không tính vào row
                
                String[] numbers = line.split("\\s+");
                loadedRowCount = Math.max(loadedRowCount, row + 1);
                loadedColCount = Math.max(loadedColCount, Math.min(GameConstants.MAX_WORLD_COL, numbers.length));
                for (int col = 0; col < GameConstants.MAX_WORLD_COL && col < numbers.length; col++) {
                    int val = Integer.parseInt(numbers[col].trim());
                    if (val == -100) {
                        gatePositions.add(new int[]{col, row});
                        val = 1; // Đổi thành nền đất để có thể đi qua sau khi cổng mở
                    }
                    mapTileNum[row][col] = val;
                }
            }
            br.close();
            
            // Tạo các Entity tĩnh từ những ô map có va chạm (Tường, Cây...) để xếp chồng 3D
            mapEntities.clear();
            for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
                for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                    int tileId = mapTileNum[row][col];
                    Tile t = tiles.get(tileId);
                    if (t != null && t.collision && t.image != null) {
                        mapEntities.add(new com.hust.game.entities.base.StaticEntity(
                                col * GameConstants.TILE_SIZE,
                                row * GameConstants.TILE_SIZE,
                                t.image, 1,
                                GameConstants.TILE_SIZE, GameConstants.TILE_SIZE));
                    }
                }
            }

            spawnPotions();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int getLoadedRowCount() {
        return loadedRowCount;
    }

    public int getLoadedColCount() {
        return loadedColCount;
    }

    private void spawnPotions() {
        Image healthPotion = loadPotionSprite("/assets/items/health_potion.png");
        Image manaPotion = loadPotionSprite("/assets/items/mana_potion.png");
        if (healthPotion == null || manaPotion == null) {
            return;
        }

        List<int[]> spots = findPotionSpots(POTION_SPAWN_TOTAL);
        for (int i = 0; i < spots.size(); i++) {
            int[] spot = spots.get(i);
            double x = spot[0] * GameConstants.TILE_SIZE
                    + (GameConstants.TILE_SIZE - GameConstants.POTION_RENDER_SIZE) / 2.0;
            double y = spot[1] * GameConstants.TILE_SIZE
                    + (GameConstants.TILE_SIZE - GameConstants.POTION_RENDER_SIZE) / 2.0;

            if (i < POTION_MANA_COUNT) {
                mapEntities.add(new ManaPotion(x, y, manaPotion));
            } else {
                mapEntities.add(new HealthPotion(x, y, healthPotion));
            }
        }
    }

    private List<int[]> findPotionSpots(int count) {
        List<int[]> spots = new ArrayList<>();
        int stride = 7;

        for (int row = 1; row < GameConstants.MAX_WORLD_ROW - 1 && spots.size() < count; row++) {
            for (int col = 1; col < GameConstants.MAX_WORLD_COL - 1 && spots.size() < count; col++) {
                if ((row + col) % stride != 0) {
                    continue;
                }
                if (isWalkableTile(row, col)) {
                    spots.add(new int[]{col, row});
                }
            }
        }

        for (int row = 1; row < GameConstants.MAX_WORLD_ROW - 1 && spots.size() < count; row++) {
            for (int col = 1; col < GameConstants.MAX_WORLD_COL - 1 && spots.size() < count; col++) {
                if (isWalkableTile(row, col)) {
                    spots.add(new int[]{col, row});
                }
            }
        }

        return spots;
    }

    private boolean isWalkableTile(int row, int col) {
        int tileId = mapTileNum[row][col];
        Tile tile = tiles.get(tileId);
        return tile != null && !tile.collision;
    }

    private Image loadPotionSprite(String path) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.err.println("Missing item asset: " + path);
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            System.err.println("Failed to load item asset: " + path);
            return null;
        }
    }

    public void draw(GraphicsContext gc) {
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                int tileId = mapTileNum[row][col];
                Tile t = tiles.get(tileId);
                // Chỉ vẽ nền đất ở dưới (t.collision == false). Tường sẽ được bốc ra vẽ chung với Player ở App.java
                if (t != null && !t.collision && t.image != null) {
                    gc.drawImage(t.image, col * GameConstants.TILE_SIZE, row * GameConstants.TILE_SIZE,
                                 GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
                }
            }
        }
    }
}
