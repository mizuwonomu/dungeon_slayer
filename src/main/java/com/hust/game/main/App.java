package com.hust.game.main;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.EnemyManager;

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
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // kích thước 1 ô trong game 8-bit sau khi upscale (ví dụ 16x16 -> 48x48)
    private static final int TILE_SIZE = 48;
    private AnimationTimer timer;

    private GraphicsContext gc;
    private Player player;
    private CombatManager combatManager;

    private EnemyManager enemyManager; // Gọi quản lý quái vật
    private List<BaseEntity> obstacles = new ArrayList<>(); // danh sách vật cản

    // dùng set để lưu những phím đang được giữ để di chuyển chéo
    private Set<KeyCode> input = new HashSet<>();

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

        initializeEntities();

        // khởi tạo và chạy game loop (60 fps)
        timer = new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                // xử lí input và update logic
                handleInput();
                player.update();
                combatManager.update();

                // Lệnh cho quái vật di chuyển
                if (enemyManager != null)
                    enemyManager.updateAll();

                // kiểm tra va chạm giữa player và toàn bộ vật cản
                checkCollisions();

                if (player.isDead()){
                    timer.stop();   

                    System.out.println("CHẾT CON CỤ RỒI :(");
                    return;
                }

                // bước b: render (xoá màn cũ -> vẽ màn mới)
                gc.clearRect(0, 0, WIDTH, HEIGHT);

                for (BaseEntity wall : obstacles) {
                    wall.render(gc);
                }

                player.render(gc);

                // Vẽ quái vật lên màn hình
                if (enemyManager != null)
                    enemyManager.renderAll(gc);

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

            Image wallImg = new Image(getClass().getResourceAsStream("/assets/wall.png"), TILE_SIZE, TILE_SIZE, true,
                    false);
            Image treeImg = new Image(getClass().getResourceAsStream("/assets/tree.png"), TILE_SIZE, TILE_SIZE, true,
                    false);
            Image slimeImg = new Image(getClass().getResourceAsStream("/assets/slime.png"), TILE_SIZE, TILE_SIZE, true,
                    false);

            // Khai báo Player trước khi đưa cho Quái
            player = new Player(WIDTH / 2, HEIGHT / 2, iDown, iUp, iLeft, iRight, rDown, rUp, rLeft, rRight);

            // Sinh quái vật để test di chuyển
            enemyManager = new EnemyManager();
            enemyManager.spawnEnemy("Tree", 10, 100, treeImg, 1, TILE_SIZE, TILE_SIZE, player);
            enemyManager.spawnEnemy("Slime", 100, 100, slimeImg, 1, TILE_SIZE, TILE_SIZE, player);

            //tạo combat manager
            combatManager = new CombatManager(player, enemyManager.getEnemyList());

            obstacles.add(new BaseEntity(300, 300, wallImg, 1, TILE_SIZE, TILE_SIZE) {
                @Override
                public void update() {
                }
            });
            obstacles.add(new BaseEntity(300, 300 + TILE_SIZE, wallImg, 1, TILE_SIZE, TILE_SIZE) {
                @Override
                public void update() {
                }
            });
            obstacles.add(new BaseEntity(WIDTH / 2 + 100, HEIGHT / 2, wallImg, 1, TILE_SIZE, TILE_SIZE) {
                @Override
                public void update() {
                }
            });

        } catch (NullPointerException e) {
            System.err.println("LỖI: Không tìm thấy file ảnh!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleInput() {
        player.savePosition();

        boolean isAnyKeyPressed = false;

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

        if (input.contains(KeyCode.J)){
            combatManager.playerAttack();
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