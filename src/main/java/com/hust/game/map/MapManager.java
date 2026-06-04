package com.hust.game.map;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.items.Chest;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class MapManager {
    private static final int[][] LEVEL_1_CHEST_TILES = {
            {8, 16}, {20, 22}, {42, 17}, {62, 28}, {92, 22}
    };
    private static final int[][] LEVEL_2_CHEST_TILES = {
            {7, 22}, {22, 16}, {42, 21}, {62, 18}, {96, 24}
    };
    public Map<Integer, Tile> tiles;
    public int[][] mapTileNum;
    public int[][] mapTileNumLayer2;
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
        mapTileNumLayer2 = new int[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
        loadTiles();
        
        String mapPath = "/assets/maps/level" + level + ".txt";
        loadMap(mapPath);
        
        String layer2Path = "/assets/maps/level" + level + "_layer2.txt";
        loadMapLayer2(layer2Path);
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
            tile.collision = tileType.isSolid();
            if (tileType.getImageName() == null) {
                tiles.put(index, tile);
                return;
            }
            // Sử dụng chữ thường 'tiles' để tránh lỗi case-sensitive khi build bằng Maven
            java.io.InputStream is = getClass().getResourceAsStream("/assets/tiles/" + tileType.getImageName());
            if (is != null) {
                tile.image = new Image(is);
            } else {
                System.err.println("Cảnh báo: Không tìm thấy ảnh " + tileType.getImageName());
            }
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

            spawnChests();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadMapLayer2(String filePath) {
        try {
            java.io.InputStream is = getClass().getResourceAsStream(filePath);
            if (is == null) {
                System.out.println("Không tìm thấy file map layer 2 " + filePath + ", bỏ qua.");
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
                String line = br.readLine();
                if (line == null) break;
                
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) { row--; continue; } 
                
                String[] numbers = line.split("\\s+");
                for (int col = 0; col < GameConstants.MAX_WORLD_COL && col < numbers.length; col++) {
                    int val = Integer.parseInt(numbers[col].trim());
                    // Chỉ ghi đè lên ô trống (0), không ghi đè lên các ô đã được đánh dấu (-15)
                    if (val != 0 && mapTileNumLayer2[row][col] == 0) {
                        mapTileNumLayer2[row][col] = val;
                    }
                }
            }
            br.close();
            
            // Tạo các Entity tĩnh từ những ô map layer 2 cần hiệu ứng 3D
            for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
                for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                    int tileId = mapTileNumLayer2[row][col];
                    if (tileId <= 0) continue; // Bỏ qua ô trống hoặc ô đã xử lý (-15)
                    
                    Tile t = tiles.get(tileId);
                    if (t == null || t.image == null) continue;

                    // Vật cản (nhà, hàng rào) hoặc bụi cây (cần vẽ đè player) đều là StaticEntity
                    if (t.collision || tileId == 14) { // 14 là BUSH
                        if (tileId == 15) { // HOUSE là 2x2, (row, col) là góc DƯỚI-TRÁI
                            int w = GameConstants.TILE_SIZE * 2;
                            int h = GameConstants.TILE_SIZE * 2;
                            // Đánh dấu 3 ô còn lại là vật cản ảo để không sinh thêm entity
                            if (row - 1 >= 0) mapTileNumLayer2[row-1][col] = -15;
                            if (col + 1 < GameConstants.MAX_WORLD_COL) mapTileNumLayer2[row][col+1] = -15;
                            if (row - 1 >= 0 && col + 1 < GameConstants.MAX_WORLD_COL) mapTileNumLayer2[row-1][col+1] = -15;

                            // Tạo StaticEntity, vị trí vẽ là góc TRÊN-TRÁI
                            mapEntities.add(new com.hust.game.entities.base.StaticEntity(col * GameConstants.TILE_SIZE, (row - 1) * GameConstants.TILE_SIZE, t.image, 1, w, h));
                        } else { // Các vật thể 1x1 khác (VD: Fence, Bush)
                            mapEntities.add(new com.hust.game.entities.base.StaticEntity(col * GameConstants.TILE_SIZE, row * GameConstants.TILE_SIZE, t.image, 1, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public int getLoadedRowCount() {
        return loadedRowCount;
    }

    public int getLoadedColCount() {
        return loadedColCount;
    }

    private void spawnChests() {
        int[][] chestTiles = getChestTilesForLevel();
        if (chestTiles.length == 0) {
            return;
        }

        Image chestSprite = loadItemSprite("/assets/items/chest.png");
        if (chestSprite == null) {
            return;
        }

        for (int[] tile : chestTiles) {
            double x = tile[0] * GameConstants.TILE_SIZE;
            double y = tile[1] * GameConstants.TILE_SIZE;
            mapEntities.add(new Chest(x, y, chestSprite));
        }
    }

    private int[][] getChestTilesForLevel() {
        if (level == 1) {
            return LEVEL_1_CHEST_TILES;
        }
        if (level == 2) {
            return LEVEL_2_CHEST_TILES;
        }
        return new int[0][0];
    }

    private Image loadItemSprite(String path) {
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
                
                // Vẽ layer 2: Chỉ vẽ các tile trang trí không phải là entity 3D
                int tileId2 = mapTileNumLayer2[row][col];
                if (tileId2 > 0) {
                    Tile t2 = tiles.get(tileId2);
                    // Chỉ vẽ nếu nó không phải vật cản VÀ không phải là bụi cây (vì chúng đã là entity)
                    if (t2 != null && !t2.collision && tileId2 != 14 && t2.image != null) {
                        gc.drawImage(t2.image, col * GameConstants.TILE_SIZE, row * GameConstants.TILE_SIZE, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
                    }
                }
            }
        }
    }
}
