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

        int tileId = mapM.mapTileNum[row][col];

        // Kiểm tra tileId hợp lệ để tránh crash nếu map file có lỗi
        if (tileId < 0 || tileId >= mapM.tiles.length || mapM.tiles[tileId] == null) {
            return true;
        }
        return mapM.tiles[tileId].collision; // Trả về true nếu là Pond hoặc Wall
    }
}