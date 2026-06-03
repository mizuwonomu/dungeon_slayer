package com.hust.game.collision;

import com.hust.game.constants.GameConstants;
import com.hust.game.map.MapManager;
import javafx.geometry.Rectangle2D;

public class CollisionChecker {
    private static final double LINE_SAMPLE_STEP = 4.0;
    private final MapManager mapM;

    public CollisionChecker(MapManager mapM) {
        this.mapM = mapM;
    }

    public boolean checkTile(int nextX, int nextY) {
        int col = (int) Math.floor(nextX / (double) GameConstants.TILE_SIZE);
        int row = (int) Math.floor(nextY / (double) GameConstants.TILE_SIZE);
        return isBlockedTile(col, row);
    }

    public boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        return !isLineBlockedByWall(x1, y1, x2, y2);
    }

    public boolean hasUnblockedAttackLine(double attackerX, double attackerY, Rectangle2D targetBox) {
        if (targetBox == null || targetBox.getWidth() <= 0.0 || targetBox.getHeight() <= 0.0) {
            return false;
        }

        double minX = targetBox.getMinX();
        double maxX = targetBox.getMaxX();
        double minY = targetBox.getMinY();
        double maxY = targetBox.getMaxY();
        double centerX = minX + targetBox.getWidth() / 2.0;
        double centerY = minY + targetBox.getHeight() / 2.0;
        double insetX = Math.min(4.0, targetBox.getWidth() / 4.0);
        double insetY = Math.min(4.0, targetBox.getHeight() / 4.0);

        double[][] targetPoints = {
                {centerX, centerY},
                {centerX, minY + insetY},
                {centerX, maxY - insetY},
                {minX + insetX, centerY},
                {maxX - insetX, centerY}
        };

        for (double[] point : targetPoints) {
            if (hasLineOfSight(attackerX, attackerY, point[0], point[1])) {
                return true;
            }
        }

        return false;
    }

    public boolean isLineBlockedByWall(double x1, double y1, double x2, double y2) {
        if (mapM == null) {
            return false;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0) {
            return false;
        }

        int steps = Math.max(1, (int) Math.ceil(distance / LINE_SAMPLE_STEP));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            int checkX = (int) Math.round(x1 + dx * t);
            int checkY = (int) Math.round(y1 + dy * t);
            if (checkTile(checkX, checkY)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlockedTile(int col, int row) {
        if (mapM == null) {
            return true;
        }

        if (row < 0 || row >= GameConstants.MAX_WORLD_ROW || col < 0 || col >= GameConstants.MAX_WORLD_COL) {
            return true;
        }

        int tileId1 = mapM.mapTileNum[row][col];
        com.hust.game.map.Tile tile1 = mapM.tiles.get(tileId1);
        if (tile1 == null || tile1.collision) {
            return true;
        }

        if (mapM.mapTileNumLayer2 != null) {
            int tileId2 = mapM.mapTileNumLayer2[row][col];
            if (tileId2 == -15) {
                return true;
            }
            if (tileId2 > 0) {
                com.hust.game.map.Tile tile2 = mapM.tiles.get(tileId2);
                if (tile2 != null && tile2.collision) {
                    return true;
                }
            }
        }

        return false;
    }
}
