package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.collision.CollisionChecker;
import java.util.ArrayList;

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
    ArrayList<Node> openList = new ArrayList<>();
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
        openList.add(currentNode);
        
        // Đặt lại trạng thái và tính toán Heuristic (Chi phí quãng đường) cho tất cả các Node
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                node[row][col].open = false;
                node[row][col].checked = false;
                
                int xDistance = Math.abs(col - startCol);
                int yDistance = Math.abs(row - startRow);
                node[row][col].gCost = xDistance + yDistance;
                
                xDistance = Math.abs(col - goalCol);
                yDistance = Math.abs(row - goalRow);
                node[row][col].hCost = xDistance + yDistance;
                
                node[row][col].fCost = node[row][col].gCost + node[row][col].hCost;
            }
        }
    }
    
    public boolean search() {
        while (!goalReached && step < 500) { // Giới hạn số bước tìm kiếm để tránh tràn bộ nhớ/giật lag
            int col = currentNode.col;
            int row = currentNode.row;
            
            currentNode.checked = true;
            openList.remove(currentNode);
            
            // Quét 4 hướng: Lên, Trái, Xuống, Phải
            if (row - 1 >= 0) openNode(node[row - 1][col]);
            if (col - 1 >= 0) openNode(node[row][col - 1]);
            if (row + 1 < GameConstants.MAX_WORLD_ROW) openNode(node[row + 1][col]);
            if (col + 1 < GameConstants.MAX_WORLD_COL) openNode(node[row][col + 1]);
            
            int bestNodeIndex = -1;
            int bestNodefCost = 9999;
            
            for (int i = 0; i < openList.size(); i++) {
                if (openList.get(i).fCost < bestNodefCost) {
                    bestNodeIndex = i;
                    bestNodefCost = openList.get(i).fCost;
                } else if (openList.get(i).fCost == bestNodefCost) {
                    if (openList.get(i).gCost < openList.get(bestNodeIndex).gCost) {
                        bestNodeIndex = i;
                    }
                }
            }
            if (bestNodeIndex == -1) break; // Kẹt cứng không tìm được đường
            currentNode = openList.get(bestNodeIndex);
            
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