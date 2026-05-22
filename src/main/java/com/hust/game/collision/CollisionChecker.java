package com.hust.game.collision;

import com.hust.game.constants.GameConstants;
import com.hust.game.map.MapManager;

public class CollisionChecker {
    MapManager mapM;

    public CollisionChecker(MapManager mapM) {
        this.mapM = mapM;
    }

    // Kiểm tra xem tọa độ (x, y) tiếp theo có chạm tường/ao không
    public boolean checkTile(int nextX, int nextY) {
        // Chuyển tọa độ pixel sang tọa độ mảng (row, col)
        int col = nextX / GameConstants.TILE_SIZE;
        int row = nextY / GameConstants.TILE_SIZE;

        // Tránh lỗi index out of bounds
        if (row < 0 || row >= GameConstants.MAX_WORLD_ROW || col < 0 || col >=  GameConstants.MAX_WORLD_COL) {
            return true; // Coi như va chạm nếu ra ngoài map
        }

        int tileId1 = mapM.mapTileNum[row][col];
        com.hust.game.map.Tile tile1 = mapM.tiles.get(tileId1);
        
        // Nếu layer 1 là null hoặc solid thì trả về va chạm
        if (tile1 == null || tile1.collision) {
            return true;
        }

        // Kiểm tra tiếp layer 2
        if (mapM.mapTileNumLayer2 != null) {
            int tileId2 = mapM.mapTileNumLayer2[row][col];
            if (tileId2 == -15) return true; // Vật cản ảo tạo ra từ object to như House (2x2)
            if (tileId2 > 0) {
                com.hust.game.map.Tile tile2 = mapM.tiles.get(tileId2);
                if (tile2 != null && tile2.collision) return true;
            }
        }
        return false;
    }
}