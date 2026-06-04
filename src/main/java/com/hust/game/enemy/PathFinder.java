package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.collision.CollisionChecker;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class PathFinder {
    public class Node {
        public int col, row;
        
        public Node(int col, int row) {
            this.col = col; 
            this.row = row;
        }
    }
    
    // Bộ nhớ đệm Flow Field dùng chung (static) cho tất cả quái vật
    private static int[][] distanceMap;
    private static int cachedGoalCol = -1;
    private static int cachedGoalRow = -1;

    public ArrayList<Node> pathList = new ArrayList<>();
    private CollisionChecker collisionChecker;
    private int startCol, startRow, goalCol, goalRow;
    
    public PathFinder(CollisionChecker collisionChecker) {
        this.collisionChecker = collisionChecker;
        if (distanceMap == null) {
            distanceMap = new int[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
        }
    }
    
    public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
        this.startCol = Math.max(0, Math.min(GameConstants.MAX_WORLD_COL - 1, startCol));
        this.startRow = Math.max(0, Math.min(GameConstants.MAX_WORLD_ROW - 1, startRow));
        this.goalCol = Math.max(0, Math.min(GameConstants.MAX_WORLD_COL - 1, goalCol));
        this.goalRow = Math.max(0, Math.min(GameConstants.MAX_WORLD_ROW - 1, goalRow));
    }
    
    public boolean search() {
        pathList.clear();

        // Chỉ tính toán lại toàn đồ thị Flow Field khi Player đã bước sang 1 ô gạch mới
        if (goalCol != cachedGoalCol || goalRow != cachedGoalRow) {
            updateFlowField(goalCol, goalRow);
            cachedGoalCol = goalCol;
            cachedGoalRow = goalRow;
        }

        // Nếu vị trí hiện tại không thể đến được mục tiêu
        if (distanceMap[startRow][startCol] == Integer.MAX_VALUE) {
            return false;
        }

        // Dò ngược từ vị trí quái vật (Gradient Descent) xuống Player
        int currCol = startCol;
        int currRow = startRow;
        int limit = 100; // Giới hạn max 100 node để tránh lặp vô hạn

        while ((currCol != goalCol || currRow != goalRow) && limit-- > 0) {
            int currentDist = distanceMap[currRow][currCol];
            int nextCol = currCol;
            int nextRow = currRow;
            int bestDist = currentDist;

            // Tìm ô bên cạnh có khoảng cách nhỏ nhất
            int[][] dirs = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}};
            for (int[] d : dirs) {
                int r = currRow + d[1];
                int c = currCol + d[0];
                if (r >= 0 && r < GameConstants.MAX_WORLD_ROW && c >= 0 && c < GameConstants.MAX_WORLD_COL) {
                    if (distanceMap[r][c] < bestDist) {
                        bestDist = distanceMap[r][c];
                        nextCol = c;
                        nextRow = r;
                    }
                }
            }
            
            if (bestDist >= currentDist) break; // Bị kẹt cứng

            currCol = nextCol;
            currRow = nextRow;
            pathList.add(new Node(currCol, currRow));
        }

        return !pathList.isEmpty();
    }
    
    // Loang Dijkstra từ Player ngược ra toàn map (Thuật toán Flow Field)
    private void updateFlowField(int targetCol, int targetRow) {
        for (int r = 0; r < GameConstants.MAX_WORLD_ROW; r++) {
            for (int c = 0; c < GameConstants.MAX_WORLD_COL; c++) {
                distanceMap[r][c] = Integer.MAX_VALUE;
            }
        }

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{targetCol, targetRow});
        distanceMap[targetRow][targetCol] = 0;

        int[][] dirs = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int c = curr[0];
            int r = curr[1];
            int dist = distanceMap[r][c];

            for (int[] d : dirs) {
                int nc = c + d[0];
                int nr = r + d[1];

                if (nc >= 0 && nc < GameConstants.MAX_WORLD_COL && nr >= 0 && nr < GameConstants.MAX_WORLD_ROW) {
                    if (distanceMap[nr][nc] == Integer.MAX_VALUE) {
                        int pixelX = nc * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;
                        int pixelY = nr * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;

                        if (!collisionChecker.checkTile(pixelX, pixelY)) {
                            distanceMap[nr][nc] = dist + 1;
                            queue.offer(new int[]{nc, nr});
                        }
                    }
                }
            }
        }
    }
}