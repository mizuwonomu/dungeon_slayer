package com.hust.game.main;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.EnemyManager;
import com.hust.game.enemy.Enemy;
import com.hust.game.map.MapManager;
import com.hust.game.collision.CollisionChecker;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class App extends Application {
    // kích thước cửa sổ
    // Cập nhật lại kích thước khớp với map level1.txt (17 cột x 13 hàng, TILE_SIZE = 48)
    private static final int WIDTH = 816;  // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48

    // kích thước 1 ô trong game 8-bit sau khi upscale (ví dụ 16x16 -> 48x48)
    private static final int TILE_SIZE = 48;
    private AnimationTimer timer;

    private GraphicsContext gc;
    private Player player;
    private CombatManager combatManager;

    private EnemyManager enemyManager; // Gọi quản lý quái vật
    private List<BaseEntity> obstacles = new ArrayList<>(); // danh sách vật cản
    private MapManager mapManager;
    private CollisionChecker collisionChecker;

    // dùng set để lưu những phím đang được giữ để di chuyển chéo
    private Set<KeyCode> input = new HashSet<>();

    private boolean isJHeld = false; // Ngăn chặn đè phím J (buộc phải nhấp nhả)
    private int screenShakeTimer = 0; // Bộ đếm rung màn hình
    private double screenShakeAmplitude = 0.0; // Độ rung (0.0, 0.5, 1.0)

    @Override
    public void start(Stage stage) {
        stage.setTitle("OOP Roguelike 8-bit Demo");

        // setup javafx ui blank (canvas)
        Group root = new Group();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        root.getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();

        // để cho nhìn 8-bit hơn: tắt chế độ làm mờ ảnh khi upscale
        Scene scene = new Scene(root);
        stage.setScene(scene);

        // nhận sự kiến phím bấm (keyboard input)
        scene.setOnKeyPressed(e -> input.add(e.getCode())); // khi ấn xuống -> cho vào set
        scene.setOnKeyReleased(e -> input.remove(e.getCode())); // khi thả ra -> xoá khỏi set

        // Khởi tạo Map
        mapManager = new MapManager();

        // Khởi tạo bộ kiểm tra va chạm địa hình
        collisionChecker = new CollisionChecker(mapManager);

        initializeEntities();

        // khởi tạo và chạy game loop (60 fps)
        timer = new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                boolean isVictory = enemyManager != null && enemyManager.getEnemyList().isEmpty();
                boolean isGameOver = player.isDead();

                // Nếu chưa chết và chưa thắng thì mới update logic
                if (!isGameOver && !isVictory) {
                    handleInput();
                    player.update();
                    combatManager.update();

                    if (enemyManager != null)
                        enemyManager.updateAll();

                    checkCollisions();
                }

                // bước b: render (xoá màn cũ -> vẽ màn mới)
                gc.clearRect(0, 0, WIDTH, HEIGHT);

                gc.save();
                // Hiệu ứng rung màn hình
                if (screenShakeTimer > 0) {
                    screenShakeTimer--;
                    if (screenShakeAmplitude > 0) {
                        // Hệ số 20 để amplitude 1.0 giật tối đa 10 pixel, amplitude 0.5 giật 5 pixel
                        double dx = (Math.random() - 0.5) * screenShakeAmplitude * 20; 
                        double dy = (Math.random() - 0.5) * screenShakeAmplitude * 20;
                        gc.translate(dx, dy);
                    }
                }

                // Vẽ map trước (để các entity hiển thị đè lên trên)
                mapManager.draw(gc);

                for (BaseEntity wall : obstacles) {
                    wall.render(gc);
                }

                player.render(gc);

                // Hiển thị Combo Text trên đầu Player (chỉ hiện từ hit thứ 3 trở đi)
                int currentCombo = combatManager.getComboCount();
                if (currentCombo >= 3) {
                    gc.setFill(javafx.scene.paint.Color.ORANGE); // Màu cam nổi bật
                    gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14 + Math.min(currentCombo * 2, 20)));
                    gc.fillText("Combo x" + currentCombo, player.getX() - 10, player.getY() - 10);
                }

                // Vẽ quái vật lên màn hình
                if (enemyManager != null)
                    enemyManager.renderAll(gc);

                gc.restore();

                // Vẽ màn hình Game Over / Victory đè lên
                if (isGameOver || isVictory) {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.7)); // Phủ màn đen mờ
                    gc.fillRect(0, 0, WIDTH, HEIGHT);
                    gc.setFill(isVictory ? javafx.scene.paint.Color.YELLOW : javafx.scene.paint.Color.RED);
                    gc.setFont(new javafx.scene.text.Font("Arial", 50));
                    String endText = isVictory ? "VICTORY!" : "GAME OVER";
                    gc.fillText(endText, WIDTH / 2 - (isVictory ? 120 : 140), HEIGHT / 2 - 20);
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.setFont(new javafx.scene.text.Font("Arial", 20));
                    gc.fillText("Press ENTER to restart", WIDTH / 2 - 100, HEIGHT / 2 + 30);

                    if (input.contains(KeyCode.ENTER)) {
                        input.clear();
                        initializeEntities(); // Reset lại toàn bộ map và nhân vật
                    }
                }
            }
        };
        timer.start();

        stage.show();
    }

    private void initializeEntities() {
        try {
            Image iDown = new Image(getClass().getResourceAsStream("/assets/idle_down.png"), 0, 0, true, false);
            Image iUp = new Image(getClass().getResourceAsStream("/assets/idle_up.png"), 0, 0, true, false);
            Image iLeft = new Image(getClass().getResourceAsStream("/assets/idle_left.png"), 0, 0, true, false);
            Image iRight = new Image(getClass().getResourceAsStream("/assets/idle_right.png"), 0, 0, true, false);

            Image rDown = new Image(getClass().getResourceAsStream("/assets/run_down.png"), 0, 0, true, false);
            Image rUp = new Image(getClass().getResourceAsStream("/assets/run_up.png"), 0, 0, true, false);
            Image rLeft = new Image(getClass().getResourceAsStream("/assets/run_left.png"), 0, 0, true, false);
            Image rRight = new Image(getClass().getResourceAsStream("/assets/run_right.png"), 0, 0, true, false);

            Image cDown = new Image(getClass().getResourceAsStream("/assets/combatdown.png"), 0, 0, true, false);
            Image cUp = new Image(getClass().getResourceAsStream("/assets/combatup.png"), 0, 0, true, false);
            Image cLeft = new Image(getClass().getResourceAsStream("/assets/combatleft.png"), 0, 0, true, false);
            Image cRight = new Image(getClass().getResourceAsStream("/assets/combatright.png"), 0, 0, true, false);
            Image swordHit = new Image(getClass().getResourceAsStream("/assets/bswordhit.png"), 0, 0, true, false);

            Image wallImg = new Image(getClass().getResourceAsStream("/assets/tiles/wall.png"), TILE_SIZE, TILE_SIZE, true,
                    false);
            Image treeImg = new Image(getClass().getResourceAsStream("/assets/tree.png"), 0, 0, true,
                    false);
            Image treeSkillImg = new Image(getClass().getResourceAsStream("/assets/tree_skill.png"), 0, 0, true,
                    false);
            Image slimeImg = new Image(getClass().getResourceAsStream("/assets/slime.png"), 0, 0, true,
                    false);

            // Khai báo Player trước khi đưa cho Quái
            player = new Player(WIDTH / 2, HEIGHT / 2, 
                iDown, iUp, iLeft, iRight, rDown, rUp, rLeft, rRight, 
                cDown, cUp, cLeft, cRight, swordHit);

            // Sinh quái vật để test di chuyển
            enemyManager = new EnemyManager();
            enemyManager.spawnEnemy("Tree", WIDTH / 2 + 100, HEIGHT / 2, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Slime", WIDTH / 2 - 100, HEIGHT / 2, slimeImg, 8, TILE_SIZE, TILE_SIZE, player);
            enemyManager.spawnEnemy("Slime", WIDTH / 2, HEIGHT / 2 - 150, slimeImg, 8, TILE_SIZE, TILE_SIZE, player);
            enemyManager.spawnEnemy("Tree", WIDTH / 2, HEIGHT / 2 + 150, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);

            //tạo combat manager
            combatManager = new CombatManager(player, enemyManager.getEnemyList());

        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy file ảnh!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleInput() {
        player.savePosition();

        boolean isAnyKeyPressed = false;

        // Khóa di chuyển khi đang chém để animation hiển thị rõ ràng và uy lực hơn
        if (!player.isAttacking()) {
            if (input.contains(KeyCode.W) || input.contains(KeyCode.UP)) {
                player.setDirection(Direction.UP);
                player.moveUp();
                isAnyKeyPressed = true;
            }
            else if (input.contains(KeyCode.S) || input.contains(KeyCode.DOWN)) {
                player.setDirection(Direction.DOWN);
                player.moveDown();
                isAnyKeyPressed = true;
            }
            else if (input.contains(KeyCode.A) || input.contains(KeyCode.LEFT)) {
                player.setDirection(Direction.LEFT);
                player.moveLeft();
                isAnyKeyPressed = true;
            }
            else if (input.contains(KeyCode.D) || input.contains(KeyCode.RIGHT)) {
                player.setDirection(Direction.RIGHT);
                player.moveRight();
                isAnyKeyPressed = true;
            }
        }

        if (input.contains(KeyCode.J)){
            if (!isJHeld) { // Yêu cầu phải nhả phím J ra rồi bấm lại mới chém tiếp được
                if (player.canAttack()) {
                    int combo = combatManager.playerAttack();
                    if (combo > 0) {
                        screenShakeTimer = 5; // Tăng lên 5 frame để độ rung rõ ràng hơn một chút
                        // Phân tầng độ rung theo Combo
                        if (combo <= 2) screenShakeAmplitude = 0.0; // Hit 1, 2: Không rung
                        else if (combo == 3) screenShakeAmplitude = 0.5; // Hit 3: Rung nhẹ
                        else screenShakeAmplitude = 1.0; // Hit 4+: Rung dứt khoát
                    }
                }
                isJHeld = true; // Đánh dấu là đang đè phím
            }
        } else {
            isJHeld = false; // Nhả phím J ra thì reset cờ cho phép chém tiếp
        }

        if (input.contains(KeyCode.L)){
            combatManager.activateSkill();
        }

        if (isAnyKeyPressed) {
            player.setState(EntityState.RUNNING);
        } else {
            player.setState(EntityState.IDLE);
        }
    }

    private void checkCollisions() {
        // 1. Kiểm tra va chạm với bề mặt Map (Wall, Pond)
        double padding = 12; // Cắt bớt phần viền trong suốt của ảnh để nhân vật đi mượt hơn
        int left = (int) (player.getX() + padding);
        int right = (int) (player.getX() + player.getRenderWidth() - padding);
        int top = (int) (player.getY() + padding);
        int bottom = (int) (player.getY() + player.getRenderHeight() - padding);

        if (collisionChecker.checkTile(left, top) || collisionChecker.checkTile(right, top) ||
            collisionChecker.checkTile(left, bottom) || collisionChecker.checkTile(right, bottom)) {
            player.onCollision(null); // Gọi rollback vị trí nếu bị kẹt tường
        }

        // Kiểm tra va chạm cho tất cả Enemy với địa hình (Wall, Pond)
        if (enemyManager != null) {
            List<Enemy> enemies = enemyManager.getEnemyList();
            
            // Bước 1: Tính toán lực đẩy (Soft Collision) giữa các quái vật trước
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                // Kiểm tra va chạm với các quái vật khác (tránh đè lên nhau)
                for (int j = i + 1; j < enemies.size(); j++) {
                    Enemy otherEnemy = enemies.get(j);
                    if (enemy.intersects(otherEnemy)) {
                        // Soft collision: Đẩy nhẹ 2 quái vật ra xa nhau thay vì giật lùi (tránh bị kẹt thành 1 cục)
                        double cx1 = enemy.getX() + enemy.getRenderWidth() / 2.0;
                        double cy1 = enemy.getY() + enemy.getRenderHeight() / 2.0;
                        double cx2 = otherEnemy.getX() + otherEnemy.getRenderWidth() / 2.0;
                        double cy2 = otherEnemy.getY() + otherEnemy.getRenderHeight() / 2.0;

                        double dx = cx1 - cx2;
                        double dy = cy1 - cy2;
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        if (dist == 0) { // Xử lý góc lách ngẫu nhiên nếu 2 quái đè khít lên nhau từ lúc spawn
                            dx = Math.random() - 0.5; dy = Math.random() - 0.5;
                            dist = Math.sqrt(dx * dx + dy * dy);
                        }
                        double pushStrength = 1.5; // Lực đẩy trượt qua nhau
                        enemy.setX(enemy.getX() + (dx / dist) * pushStrength);
                        enemy.setY(enemy.getY() + (dy / dist) * pushStrength);
                        otherEnemy.setX(otherEnemy.getX() - (dx / dist) * pushStrength);
                        otherEnemy.setY(otherEnemy.getY() - (dy / dist) * pushStrength);
                    }
                }
            }

            // Bước 2: KIỂM TRA VA CHẠM TƯỜNG SAU KHI ĐÃ BỊ ĐẨY
            // Việc này đảm bảo nếu quái bị xô đẩy văng vào tường, nó sẽ ngay lập tức bị giật ngược lại vị trí an toàn
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                int eLeft = (int) (enemy.getX() + padding);
                int eRight = (int) (enemy.getX() + enemy.getRenderWidth() - padding);
                int eTop = (int) (enemy.getY() + padding);
                int eBottom = (int) (enemy.getY() + enemy.getRenderHeight() - padding);

                if (collisionChecker.checkTile(eLeft, eTop) || collisionChecker.checkTile(eRight, eTop) ||
                    collisionChecker.checkTile(eLeft, eBottom) || collisionChecker.checkTile(eRight, eBottom)) {
                    enemy.onCollision(null); // Trở về lastX, lastY ban đầu (Bên ngoài tường)
                }
            }
        }

        // 2. Kiểm tra va chạm với các vật cản khác (nếu list obstacles vẫn còn dùng sau này)
        for (BaseEntity wall : obstacles) {
            if (player.intersects(wall)) {
                player.onCollision(wall);

                break;
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}