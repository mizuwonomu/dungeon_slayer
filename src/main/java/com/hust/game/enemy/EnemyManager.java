package com.hust.game.enemy;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import com.hust.game.entities.player.Player;

public class EnemyManager {
    // Danh sách chứa toàn bộ quái vật đang sống trên bản đồ
    private List<Enemy> enemyList;

    public EnemyManager() {
        this.enemyList = new ArrayList<>();
    }

    // Hàm sinh thái quái vật, nhét vào danh sách
    public void spawnEnemy(String enemyType, double x, double y, Image sprite, int numFrames, double w, double h,
            Player targetPlayer) {
        Enemy newEnemy = null;

        if (enemyType.equals("Slime")) {
            newEnemy = new Slime(x, y, sprite, numFrames, w, h, targetPlayer);
        } else if (enemyType.equals("Tree")) {
            newEnemy = new Tree(x, y, sprite, numFrames, w, h, targetPlayer);
        }

        if (newEnemy != null) {
            enemyList.add(newEnemy);
        }
    }

    // Duyệt qua tất cả để chốt sổ tọa độ mới
    public void updateAll() {
        // Dọn dẹp quái vật đã chết và nhấp nháy xong ra khỏi bản đồ
        enemyList.removeIf(Enemy::isReadyToRemove);

        for (Enemy e : enemyList) {
            e.update();
        }
    }

    // Duyệt qua tất cả để vẽ lên bản đồ
    public void renderAll(GraphicsContext gc) {
        for (Enemy e : enemyList) {
            e.render(gc);
        }
    }

    // Getter lấy danh sách nếu các file khác (ví dụ check va chạm đạn) cần dùng
    public List<Enemy> getEnemyList() {
        return enemyList;
    }
}
