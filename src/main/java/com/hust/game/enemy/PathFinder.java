package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.collision.CollisionChecker;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class PathFinder {
    public class Node {
        public int col, row;
        public int gCost, hCost, fCost;
        public Node parent;
        public boolean open, checked;
        
        public Node(int col, int row) {
            this.col = col; 
            this.row = row;
        }
    }
    
    Node[][] node;
    PriorityQueue<Node> openList = new PriorityQueue<>((n1, n2) -> {
        int result = Integer.compare(n1.fCost, n2.fCost);
        if (result == 0) {
            return Integer.compare(n1.gCost, n2.gCost);
        }
        return result;
    });
    public ArrayList<Node> pathList = new ArrayList<>();
    Node startNode, goalNode, currentNode;
    boolean goalReached = false;
    int step = 0;
    CollisionChecker collisionChecker;
    
    public PathFinder(CollisionChecker collisionChecker) {
        this.collisionChecker = collisionChecker;
        instantiateNodes();
    }
    
    public void instantiateNodes() {
        node = new Node[GameConstants.MAX_WORLD_ROW][GameConstants.MAX_WORLD_COL];
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                node[row][col] = new Node(col, row);
            }
        }
    }
    
    public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
        openList.clear();
        pathList.clear();
        goalReached = false;
        step = 0;
        
        startCol = Math.max(0, Math.min(GameConstants.MAX_WORLD_COL - 1, startCol));
        startRow = Math.max(0, Math.min(GameConstants.MAX_WORLD_ROW - 1, startRow));
        goalCol = Math.max(0, Math.min(GameConstants.MAX_WORLD_COL - 1, goalCol));
        goalRow = Math.max(0, Math.min(GameConstants.MAX_WORLD_ROW - 1, goalRow));

        startNode = node[startRow][startCol];
        currentNode = startNode;
        goalNode = node[goalRow][goalCol];
        
        // Đặt lại trạng thái cho tất cả các Node (Chỉ thiết lập cờ, KHÔNG tính toán toán học ở đây)
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                node[row][col].open = false;
                node[row][col].checked = false;
            }
        }
        
        startNode.gCost = 0;
        startNode.hCost = Math.abs(startCol - goalCol) + Math.abs(startRow - goalRow);
        startNode.fCost = startNode.gCost + startNode.hCost;
        
        openList.add(currentNode);
        currentNode.open = true;
    }
    
    public boolean search() {
        while (!goalReached && step < 500) { // Giới hạn số bước tìm kiếm để tránh tràn bộ nhớ/giật lag
            int col = currentNode.col;
            int row = currentNode.row;
            
            currentNode.checked = true;
            
            // Quét 4 hướng: Lên, Trái, Xuống, Phải
            if (row - 1 >= 0) openNode(node[row - 1][col]);
            if (col - 1 >= 0) openNode(node[row][col - 1]);
            if (row + 1 < GameConstants.MAX_WORLD_ROW) openNode(node[row + 1][col]);
            if (col + 1 < GameConstants.MAX_WORLD_COL) openNode(node[row][col + 1]);
            
            currentNode = openList.poll();
            if (currentNode == null) break; // Kẹt cứng không tìm được đường
            
            if (currentNode == goalNode) {
                goalReached = true;
                trackThePath();
            }
            step++;
        }
        return goalReached;
    }
    
    private void openNode(Node node) {
        if (!node.open && !node.checked) {
            // Kiểm tra xem ô tiếp theo có phải tường hay không bằng cách lấy tọa độ tâm ô đó
            int pixelX = node.col * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;
            int pixelY = node.row * GameConstants.TILE_SIZE + GameConstants.TILE_SIZE / 2;
            
            if (!collisionChecker.checkTile(pixelX, pixelY)) {
                node.open = true;
                node.parent = currentNode;
                
                node.gCost = currentNode.gCost + 1;
                node.hCost = Math.abs(node.col - goalNode.col) + Math.abs(node.row - goalNode.row);
                node.fCost = node.gCost + node.hCost;
                
                openList.add(node);
            } else {
                node.checked = true; // Là tường thì chốt lại luôn, không xét lại nữa
            }
        }
    }
    
    private void trackThePath() {
        Node current = goalNode;
        while (current != startNode) {
            pathList.add(0, current);
            current = current.parent;
        }
    }
}