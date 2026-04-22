package com.hust.game.map;

import com.hust.game.constants.GameConstants;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MapManager {
    public Tile[] tiles;
    public int[][] mapTileNum;

    public MapManager() {
        tiles = new Tile[TileType.values().length];
        mapTileNum = new int[GameConstants.MAX_SCREEN_ROW][GameConstants.MAX_SCREEN_COL];
        loadTiles();
        loadMap("/assets/maps/level1.txt");
    }

    public void loadTiles() {
        for (TileType type : TileType.values()) {
            setup(type);
        }
    }

    private void setup(TileType tileType) {
        try {
            int index = tileType.getId();
            tiles[index] = new Tile();
            // Sử dụng chữ thường 'tiles' để tránh lỗi case-sensitive khi build bằng Maven
            java.io.InputStream is = getClass().getResourceAsStream("/assets/tiles/" + tileType.getImageName());
            if (is != null) {
                tiles[index].image = new Image(is);
            } else {
                System.err.println("Cảnh báo: Không tìm thấy ảnh " + tileType.getImageName());
            }
            tiles[index].collision = tileType.isSolid();
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
            for (int row = 0; row < GameConstants.MAX_SCREEN_ROW; row++) {
                String line = br.readLine();
                if (line == null) break;
                
                // Fix an toàn: Xóa ký tự tàng hình BOM (UTF-8) ở đầu file txt và dòng trống
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty()) { row--; continue; } // Bỏ qua dòng trống, không tính vào row
                
                String[] numbers = line.split("\\s+");
                for (int col = 0; col < GameConstants.MAX_SCREEN_COL && col < numbers.length; col++) {
                    mapTileNum[row][col] = Integer.parseInt(numbers[col].trim());
                }
            }
            br.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void draw(GraphicsContext gc) {
        for (int row = 0; row < GameConstants.MAX_SCREEN_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_SCREEN_COL; col++) {
                int tileId = mapTileNum[row][col];
                // Tránh lỗi mảng index âm nếu load ID lỗi từ file txt
                if (tileId >= 0 && tileId < tiles.length && tiles[tileId] != null && tiles[tileId].image != null) {
                    gc.drawImage(tiles[tileId].image, col * GameConstants.TILE_SIZE, row * GameConstants.TILE_SIZE,
                                 GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
                }
            }
        }
    }
}