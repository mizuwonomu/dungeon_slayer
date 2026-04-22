package com.hust.game.main;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.base.StaticEntity;
import com.hust.game.entities.player.Player;
import com.hust.game.enemy.Enemy;
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
import java.util.List;
import java.util.Set;

/**
 * App — điểm khởi chạy của game Dungeon Slayer.
 *
 * Extends Application của JavaFX → start() là hàm chính được gọi khi launch().
 *
 * Trách nhiệm của class này:
 * 1. Setup cửa sổ và canvas
 * 2. Nhận input bàn phím
 * 3. Chạy game loop 60fps
 * 4. Điều phối: input → update → collision → render
 *
 * App KHÔNG chứa logic game cụ thể — chỉ điều phối các entity.
 * Logic chi tiết nằm trong từng entity (Player, Enemy, ...).
 */
public class App extends Application {

    // -------------------------------------------------------
    // JAVAFX RENDERING
    // GraphicsContext là "cây bút" để vẽ lên Canvas.
    // Mọi lệnh render đều gọi qua gc.
    // -------------------------------------------------------
    private GraphicsContext gc;
    private EnemyManager enemyManager;
    // -------------------------------------------------------
    // ENTITIES
    // player: nhân vật chính do người chơi điều khiển
    // obstacles: danh sách vật cản tĩnh (tường, cổng...)
    // Member A sẽ thay thế bằng MapManager sau này
    // -------------------------------------------------------
    private Player player;
    private final List<BaseEntity> obstacles = new ArrayList<>();

    // -------------------------------------------------------
    // INPUT — bàn phím
    // Dùng Set thay vì biến boolean riêng lẻ vì:
    // - Set tự động loại trùng
    // - Hỗ trợ nhiều phím cùng lúc (di chuyển chéo nếu cần)
    // - Thêm/xóa phím O(1)
    // -------------------------------------------------------
    private final Set<KeyCode> input = new HashSet<>();

    /**
     * start() — JavaFX gọi hàm này sau launch().
     * Đây là nơi setup toàn bộ UI và bắt đầu game loop.
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Dungeon Slayer");

        // Tạo Canvas — vùng vẽ pixel của game
        // Kích thước lấy từ GameConstants, không hardcode
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        // Lấy GraphicsContext từ canvas để dùng khi render
        gc = canvas.getGraphicsContext2D();

        // Group là container gốc của JavaFX scene graph
        // Scene bọc Group lại và gắn vào cửa sổ (Stage)
        Scene scene = new Scene(new Group(canvas));

        // -------------------------------------------------------
        // KEYBOARD INPUT
        // setOnKeyPressed: khi người dùng nhấn phím → thêm vào Set
        // setOnKeyReleased: khi người dùng thả phím → xóa khỏi Set
        // Kết quả: Set luôn chứa đúng những phím đang được GIỮ
        // -------------------------------------------------------
        scene.setOnKeyPressed(e -> input.add(e.getCode()));
        scene.setOnKeyReleased(e -> input.remove(e.getCode()));

        stage.setScene(scene);

        // Load ảnh và khởi tạo các entity
        initializeEntities();

        // -------------------------------------------------------
        // GAME LOOP — AnimationTimer của JavaFX
        // handle() được gọi mỗi frame, ~60 lần/giây
        // currentNanoTime: thời gian hiện tại tính bằng nanosecond
        // (hiện tại chưa dùng, nhưng có thể dùng để tính delta time sau)
        // -------------------------------------------------------
        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {

                // BƯỚC 1: Xử lý input → di chuyển player, đổi state/direction
                handleInput();

                // BƯỚC 2: Update logic nội tại của player (animation timer, cooldown...)
                player.update();

                // [BƯỚC 2.5 - FIX LỖI QUÁI ĐỨNG IM]: Cập nhật bộ não cho bọn Quái!
                // Nhóm của em vừa dọn dẹp lại code nhưng lỡ tay XÓA MẤT dòng lệnh kích hoạt não
                // bộ cho Quái!
                if (enemyManager != null) {
                    enemyManager.updateAll();
                }

                // BƯỚC 3: Kiểm tra va chạm sau khi đã di chuyển
                // Nếu va chạm → player tự rollback về vị trí cũ
                checkCollisions();

                // BƯỚC 4: Render — xóa frame cũ rồi vẽ frame mới
                // Phải xóa trước, không thì frame trước bị chồng lên
                gc.clearRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

                // Vẽ tất cả vật cản trước (background layer)
                obstacles.forEach(e -> e.render(gc));

                // Vẽ player sau cùng (foreground layer)
                player.render(gc);

                // Vẽ quái vật lên màn hình
                if (enemyManager != null)
                    enemyManager.renderAll(gc);
            }
        }.start();

        stage.show();
    }

    /**
     * initializeEntities() — load ảnh và tạo các entity ban đầu.
     *
     * Tách ra khỏi start() để giữ start() gọn,
     * và dễ thay thế sau khi Member A làm xong MapManager.
     */
    private void initializeEntities() {
        try {
            // Load 8 sprite sheet cho player
            // idle_* : đứng yên 4 hướng
            // run_* : chạy 4 hướng
            Image iDown = loadImg("/assets/idle_down.png");
            Image iUp = loadImg("/assets/idle_up.png");
            Image iLeft = loadImg("/assets/idle_left.png");
            Image iRight = loadImg("/assets/idle_right.png");
            Image rDown = loadImg("/assets/run_down.png");
            Image rUp = loadImg("/assets/run_up.png");
            Image rLeft = loadImg("/assets/run_left.png");
            Image rRight = loadImg("/assets/run_right.png");

            // Load ảnh tường, resize luôn về TILE_SIZE khi load
            Image wallImg = loadImg("/assets/wall.png",
                    GameConstants.TILE_SIZE,
                    GameConstants.TILE_SIZE);

            Image treeImg = loadImg("/assets/tree.png");
            Image slimeImg = loadImg("/assets/slime.png");

            // Khởi tạo player ở giữa màn hình
            // numFrames và renderSize đã được Player tự lấy từ GameConstants
            player = new Player(
                    GameConstants.WINDOW_WIDTH / 2.0,
                    GameConstants.WINDOW_HEIGHT / 2.0,
                    iDown, iUp, iLeft, iRight,
                    rDown, rUp, rLeft, rRight);

            // -------------------------------------------------------
            // TODO: Member A thay đoạn này bằng MapManager.loadMap()
            // Tạm thời hardcode 3 tường để test collision
            // -------------------------------------------------------

            // Sinh quái để test
            enemyManager = new EnemyManager();
            enemyManager.spawnEnemy("Tree", 10, 100, treeImg, 8, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE,
                    player);
            enemyManager.spawnEnemy("Slime", 100, 100, slimeImg, 8, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE,
                    player);

            int ts = GameConstants.TILE_SIZE;
            obstacles.add(new StaticEntity(300, 300, wallImg, 1, ts, ts));
            obstacles.add(new StaticEntity(300, 300 + ts, wallImg, 1, ts, ts));
            obstacles.add(new StaticEntity(
                    GameConstants.WINDOW_WIDTH / 2.0 + 100,
                    GameConstants.WINDOW_HEIGHT / 2.0,
                    wallImg, 1, ts, ts));

        } catch (NullPointerException e) {
            // NullPointerException xảy ra khi getResourceAsStream không tìm thấy file
            // → in lỗi rõ ràng rồi thoát, không để game chạy với asset null
            System.err.println("LỖI: Không tìm thấy file ảnh! Kiểm tra thư mục /assets");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * handleInput() — đọc Set phím đang giữ và ra lệnh cho player.
     *
     * Thứ tự ưu tiên: W/UP → S/DOWN → A/LEFT → D/RIGHT
     * (else-if nên chỉ 1 hướng được xử lý mỗi frame → không di chuyển chéo)
     *
     * Gọi savePosition() TRƯỚC KHI di chuyển để có thể rollback nếu va chạm.
     */
    private void handleInput() {
        // Lưu vị trí hiện tại trước khi di chuyển
        player.savePosition();

        boolean moving = false; // cờ để xác định player có đang di chuyển không

        if (input.contains(KeyCode.W) || input.contains(KeyCode.UP)) {
            player.setDirection(Direction.UP);
            player.moveUp();
            moving = true;
        } else if (input.contains(KeyCode.S) || input.contains(KeyCode.DOWN)) {
            player.setDirection(Direction.DOWN);
            player.moveDown();
            moving = true;
        } else if (input.contains(KeyCode.A) || input.contains(KeyCode.LEFT)) {
            player.setDirection(Direction.LEFT);
            player.moveLeft();
            moving = true;
        } else if (input.contains(KeyCode.D) || input.contains(KeyCode.RIGHT)) {
            player.setDirection(Direction.RIGHT);
            player.moveRight();
            moving = true;
        }

        // Đổi state dựa trên có di chuyển hay không
        // setState() chỉ trigger updateSpriteSheet() khi state thực sự thay đổi
        player.setState(moving ? EntityState.RUNNING : EntityState.IDLE);
    }

    /**
     * checkCollisions() — kiểm tra player có đụng vào vật cản nào không.
     *
     * Dùng AABB (Axis-Aligned Bounding Box) qua intersects() của BaseEntity.
     * Khi phát hiện va chạm → gọi onCollision() để player tự rollback.
     * break ngay sau va chạm đầu tiên → tránh xử lý nhiều collision cùng lúc.
     *
     * TODO: Member B cần thêm collision giữa player và enemy vào đây.
     */
    private void checkCollisions() {
        for (BaseEntity wall : obstacles) {
            if (player.intersects(wall)) {
                player.onCollision(wall); // rollback vị trí
                break;
            }
        }

        if (enemyManager != null) {
            for (Enemy quai : enemyManager.getEnemyList()) {
                // Check quái có chạm tường không
                for (BaseEntity wall : obstacles) {
                    if (quai.intersects(wall)) {
                        quai.setX(quai.getLastX());
                        quai.setY(quai.getLastY());

                        quai.setX(quai.getX() + quai.getMoveX());
                        if (quai.intersects(wall)) {
                            // Kẹt X rồi, rụt chân X lại!
                            quai.setX(quai.getLastX());
                        }

                        quai.setY(quai.getY() + quai.getMoveY());
                        if (quai.intersects(wall)) {
                            // Kẹt Y rồi, rụt chân Y lại!
                            quai.setY(quai.getLastY());
                        }
                        break;
                    }
                }
            }
        }
    }

    // -------------------------------------------------------
    // HELPER — load ảnh từ resources
    // Tách ra để tránh lặp code, đặt false để giữ pixel art sắc nét
    // (smooth = false → không làm mờ khi upscale, giữ phong cách 8-bit)
    // -------------------------------------------------------

    /** Load ảnh với kích thước gốc */
    private Image loadImg(String path) {
        return new Image(getClass().getResourceAsStream(path), 0, 0, true, false);
    }

    /** Load ảnh và resize về kích thước chỉ định */
    private Image loadImg(String path, double w, double h) {
        return new Image(getClass().getResourceAsStream(path), w, h, true, false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}