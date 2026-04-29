package com.hust.game.main;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.EnemyManager;
import com.hust.game.enemy.Knight;
import com.hust.game.enemy.Enemy;
import com.hust.game.map.MapManager;
import com.hust.game.collision.CollisionChecker;

import com.hust.game.ui.GameFinish;
import com.hust.game.ui.HUD;
import com.hust.game.ui.MenuScreen;
import com.hust.game.ui.PauseScreen;
import com.hust.game.ui.SettingsScreen;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class App extends Application {
    // kích thước cửa sổ
    // Cập nhật lại kích thước khớp chuẩn tỷ lệ 1.7 (17 cột x 10 hàng, TILE_SIZE = 48)
    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 480; // 10 * 48

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

    private boolean isPaused = false;
    private PauseScreen pauseScreen;

    // ... (everything above stays EXACTLY the same)

    @Override
    public void start(Stage stage) {
        stage.setTitle("GHOULITE");

        // Gọi tải toàn bộ âm thanh ngay từ đầu để MenuScreen và Settings có thể dùng được tiếng click/hover
        SoundManager.loadSounds();

        MenuScreen menu = new MenuScreen(

                // START
                v -> {
                    Scene gameScene = createGameScene(stage);
                    stage.setScene(gameScene);
                    gameLoop.start();
                },

                // SETTINGS
                v -> showSettings(stage)
        );

        stage.setScene(menu.createScene());
        stage.show();
    }
    private void showMenu(Stage stage) {
        MenuScreen menu = new MenuScreen(

                v -> {
                    Scene gameScene = createGameScene(stage);
                    stage.setScene(gameScene);
                    gameLoop.start();
                },

                v -> showSettings(stage)
        );

        stage.setScene(menu.createScene());
    }

    private void showSettings(Stage stage) {
        SettingsScreen settings = new SettingsScreen(
                v -> showMenu(stage)
        );

        stage.setScene(settings.createScene());
    }
    private Scene createMenuScene(Stage stage) {
        Image startImg = loadImg("/assets/start.png");
        Image exitImg = loadImg("/assets/exit.png");
        Image startSelectImg = loadImg("/assets/startselect.png");
        Image exitselectImg = loadImg("/assets/exitselect.png");

        javafx.scene.control.Button startBtn = new javafx.scene.control.Button();
        javafx.scene.control.Button exitBtn = new javafx.scene.control.Button();

        // Set images into buttons
        startBtn.setGraphic(new javafx.scene.image.ImageView(startImg));
        exitBtn.setGraphic(new javafx.scene.image.ImageView(exitImg));

        startBtn.setOnMouseEntered(e -> startBtn.setOpacity(0.7));
        startBtn.setOnMouseExited(e -> startBtn.setOpacity(1.0));

        exitBtn.setOnMouseEntered(e -> exitBtn.setOpacity(0.7));
        exitBtn.setOnMouseExited(e -> exitBtn.setOpacity(1.0));

        ImageView startView = new ImageView(startImg);
        startView.setFitWidth(250);
        startView.setPreserveRatio(true);

        ImageView exitView = new ImageView(exitImg);
        exitView.setFitWidth(250);
        exitView.setPreserveRatio(true);

        // START button hover
        startBtn.setOnMouseEntered(e -> startView.setImage(startSelectImg));
        startBtn.setOnMouseExited(e -> startView.setImage(startImg));

        // QUIT button hover
        exitBtn.setOnMouseEntered(e -> exitView.setImage(exitselectImg));
        exitBtn.setOnMouseExited(e -> exitView.setImage(exitImg));

        startBtn.setGraphic(startView);
        exitBtn.setGraphic(exitView);

        startBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        exitBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        startBtn.setOnAction(e -> {
            Scene gameScene = createGameScene(stage);
            stage.setScene(gameScene);
            gameLoop.start();
        });

        exitBtn.setOnAction(e -> {
            javafx.application.Platform.exit();
        });

        javafx.scene.text.Text title = new javafx.scene.text.Text("Dungeon Slayer");
        title.setStyle("-fx-font-size: 36px; -fx-fill: white;");

        javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(30, title, startBtn, exitBtn);

        layout.setStyle("-fx-alignment: center; -fx-background-color: black;");

        return new Scene(layout, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
    }

    private AnimationTimer gameLoop;
    private HUD hud;

    private GameFinish gameOverUI;
    private boolean isEndUIShown = false;


    private Scene createGameScene(Stage stage) {
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        Group gameLayer = new Group(canvas);
        StackPane root = new StackPane(gameLayer);
        Scene scene = new Scene(root);

        pauseScreen = new PauseScreen(
            () -> setPaused(false),
            () -> {
                setPaused(false);
                    if (gameLoop != null) {
                        gameLoop.stop();
                    }
                showMenu(stage);
            }
        );
        root.getChildren().add(pauseScreen.getRoot());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause(stage);
                return;
            }
            if (!isPaused) {
                input.add(e.getCode());
            }
        });
        scene.setOnKeyReleased(e -> {
            if (!isPaused) {
                input.remove(e.getCode());
            }
        });

        initializeEntities();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                boolean isVictory = enemyManager.getEnemyList().isEmpty();
                boolean isGameOver = player.isDead();

                if (!isVictory && !isGameOver && !isPaused) {
                    handleInput();
                    player.update();
                    combatManager.update();
                    enemyManager.updateAll();
                    checkCollisions();
                    com.hust.game.ui.DamageTextManager.update(); // Cập nhật toạ độ và thời gian tồn tại của chữ bay
                }

                gc.clearRect(0, 0, WIDTH, HEIGHT);

                gc.save();

                // Screen shake
                if (screenShakeTimer > 0 && !isPaused) {
                    screenShakeTimer--;
                    double dx = (Math.random() - 0.5) * screenShakeAmplitude * 20;
                    double dy = (Math.random() - 0.5) * screenShakeAmplitude * 20;
                    gc.translate(dx, dy);
                }

                // Draw map
                mapManager.draw(gc);

                obstacles.forEach(e -> e.render(gc));

                gc.restore();

                player.render(gc);
                enemyManager.renderAll(gc);
                com.hust.game.ui.DamageTextManager.render(gc); // Vẽ các số sát thương (vẽ sau quái để đè lên trên cùng)

                hud.render(gc);

                // End screen
                if (isVictory || isGameOver) {
                    gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.7));
                    gc.fillRect(0, 0, WIDTH, HEIGHT);

                    // Load font chữ pixel
                    javafx.scene.text.Font finishFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 100);
                    if (finishFont == null) {
                        finishFont = new javafx.scene.text.Font("Arial", 100); // Fallback
                    }
                    gc.setFont(finishFont);
                    gc.setStroke(javafx.scene.paint.Color.WHITE);
                    gc.setLineWidth(4);

                    if (isVictory) {
                        String text = "VICTORY!";
                        javafx.scene.text.Text tempText = new javafx.scene.text.Text(text);
                        tempText.setFont(gc.getFont());
                        double textWidth = tempText.getLayoutBounds().getWidth();
                        double textHeight = tempText.getLayoutBounds().getHeight();
                        double x = (WIDTH - textWidth) / 2;
                        double y = (HEIGHT + textHeight) / 2 - 50;

                        gc.setFill(javafx.scene.paint.Color.GOLD);
                        gc.strokeText(text, x, y); // Vẽ viền trắng
                        gc.fillText(text, x, y);   // Vẽ chữ màu vàng gold
                    } else { // isGameOver
                        String text = "DEFEAT";
                        javafx.scene.text.Text tempText = new javafx.scene.text.Text(text);
                        tempText.setFont(gc.getFont());
                        double textWidth = tempText.getLayoutBounds().getWidth();
                        double textHeight = tempText.getLayoutBounds().getHeight();
                        double x = (WIDTH - textWidth) / 2;
                        double y = (HEIGHT + textHeight) / 2 - 50;

                        gc.setFill(javafx.scene.paint.Color.GRAY);
                        gc.strokeText(text, x, y); // Vẽ viền trắng
                        gc.fillText(text, x, y);   // Vẽ chữ màu xám
                    }

                    if (!isEndUIShown) {
                        gameLoop.stop();

                        gameOverUI = new GameFinish(

                                // Retry
                                () -> {
                                    isEndUIShown = false;
                                    Scene newGame = createGameScene(stage);
                                    stage.setScene(newGame);
                                    gameLoop.start();
                                },

                                // Menu
                                () -> {
                                    isEndUIShown = false;

                                    showMenu(stage);
                                });

                        root.getChildren().add(gameOverUI.getRoot());
                        isEndUIShown = true;
                    }
                }
            }
        };
        return scene;
    }

    private void togglePause(Stage stage) {
        if (player == null || enemyManager == null || pauseScreen == null) {
            return;
        }
        boolean isVictory = enemyManager.getEnemyList().isEmpty();
        boolean isGameOver = player.isDead();
        if (isVictory || isGameOver || isEndUIShown) {
            return;
        }
        setPaused(!isPaused);
    }

    private void setPaused(boolean paused) {
        isPaused = paused;
        pauseScreen.setVisible(paused);
        if (paused) {
            input.clear();
            SoundManager.stopGameplaySounds();
        }
    }

    private void initializeEntities() {
        try {
            Image iDown = loadImg("/assets/player/idle_down.png");
            Image iUp = loadImg("/assets/player/idle_up.png");
            Image iLeft = loadImg("/assets/player/idle_left.png");
            Image iRight = loadImg("/assets/player/idle_right.png");

            Image rDown = loadImg("/assets/player/run_down.png");
            Image rUp = loadImg("/assets/player/run_up.png");
            Image rLeft = loadImg("/assets/player/run_left.png");
            Image rRight = loadImg("/assets/player/run_right.png");

            Image cDown = loadImg("/assets/player/combatdown.png");
            Image cUp = loadImg("/assets/player/combatup.png");
            Image cLeft = loadImg("/assets/player/combatleft.png");
            Image cRight = loadImg("/assets/player/combatright.png");

            Image swordHit = loadImg("/assets/player/wswordhit.png");
            Image rageHit = loadImg("/assets/player/bswordhit.png");

            Image wallImg = loadImg("/assets/tiles/wall.png", TILE_SIZE, TILE_SIZE);
            Image treeImg = loadImg("/assets/enemy/tree.png");
            Image treeSkillImg = loadImg("/assets/enemy/Tree_skill.png");
            Image slimeImg = loadImg("/assets/enemy/slime.png");
            Image knightImg = loadImg("/assets/enemy/knight_idle.png");
            Image knightSkillImg = loadImg("/assets/enemy/knight_attack.png");
            Image witchImg = loadImg("/assets/enemy/witch_summon.png");
            Image witchSkillImg = loadImg("/assets/enemy/witch_atk.png");

            Image powerUpImg = loadImg("/assets/player/player_power_up.png");
            Image thunderImg = loadImg("/assets/player/lightning.png");

            // Khai báo Player trước khi đưa cho Quái
            player = new Player(WIDTH / 2, HEIGHT / 2,
                    iDown, iUp, iLeft, iRight, rDown, rUp, rLeft, rRight,
                    cDown, cUp, cLeft, cRight, swordHit, rageHit, powerUpImg, thunderImg);

            // Sinh quái vật để test di chuyển
            enemyManager = new EnemyManager();
            enemyManager.spawnEnemy("Tree", WIDTH / 2 + 100, HEIGHT / 2, treeImg, 8,
            TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Slime", WIDTH / 2 - 100, HEIGHT / 2, slimeImg, 8,
            TILE_SIZE, TILE_SIZE, player);
            // enemyManager.spawnEnemy("Knight", WIDTH / 2, HEIGHT / 2 - 100, knightImg, 8,
            // TILE_SIZE * 2, TILE_SIZE * 2,
            // player,
            // knightSkillImg);
            enemyManager.spawnEnemy("Witch", WIDTH / 2, HEIGHT / 2 + 100, witchImg, 25, TILE_SIZE, TILE_SIZE,
                    player, witchSkillImg);
            // tạo combat manager
            combatManager = new CombatManager(player, enemyManager.getEnemyList());

        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy file ảnh!");
            e.printStackTrace();
            System.exit(1);
        }
        mapManager = new MapManager();
        collisionChecker = new CollisionChecker(mapManager);
        hud = new HUD(player, combatManager);
    }

    private void handleInput() {
        player.savePosition();

        boolean isAnyKeyPressed = false;

        // Khóa di chuyển khi đang chém để animation hiển thị rõ ràng và uy lực hơn
        if (!player.isAttacking()) {
            boolean up    = input.contains(KeyCode.W) || input.contains(KeyCode.UP);
            boolean down  = input.contains(KeyCode.S) || input.contains(KeyCode.DOWN);
            boolean left  = input.contains(KeyCode.A) || input.contains(KeyCode.LEFT);
            boolean right = input.contains(KeyCode.D) || input.contains(KeyCode.RIGHT);

            // Xử lý direction — ưu tiên trục đơn, đi chéo thì giữ direction cũ
            if      (up && !down && !left && !right) player.setDirection(Direction.UP);
            else if (down && !up && !left && !right) player.setDirection(Direction.DOWN);
            else if (left && !right && !up && !down) player.setDirection(Direction.LEFT);
            else if (right && !left && !up && !down) player.setDirection(Direction.RIGHT);

            // Di chuyển độc lập từng trục
            double dx = 0, dy = 0;
            if (up)    dy -= 1;
            if (down)  dy += 1;
            if (left)  dx -= 1;
            if (right) dx += 1;

            // Normalize vector chéo để tốc độ không bị nhanh hơn
            if (dx != 0 && dy != 0) {
                dx *= 0.7071; // 1 / √2
                dy *= 0.7071;
            }

            // Apply movement
            if (dx != 0) player.setX(player.getX() + dx * GameConstants.PLAYER_SPEED);
            if (dy != 0) player.setY(player.getY() + dy * GameConstants.PLAYER_SPEED);

            isAnyKeyPressed = (up || down || left || right);
        }

        if (input.contains(KeyCode.J)) {
            if (!isJHeld) { // Yêu cầu phải nhả phím J ra rồi bấm lại mới chém tiếp được
                if (player.canAttack()) {
                    int combo = combatManager.playerAttack();
                    if (combo > 0) {
                        screenShakeTimer = 3; // Tăng lên 3 frame để độ rung rõ ràng hơn một chút
                        // Phân tầng độ rung theo Combo
                        if (combo <= 2)
                            screenShakeAmplitude = 0.0; // Hit 1, 2: Không rung
                        else if (combo == 3)
                            screenShakeAmplitude = 0.3; // Hit 3: Rung nhẹ
                        else
                            screenShakeAmplitude = 0.5;
                    }
                }
                isJHeld = true; // Đánh dấu là đang đè phím
            }
        } else {
            isJHeld = false; // Nhả phím J ra thì reset cờ cho phép chém tiếp
        }

        if (input.contains(KeyCode.L)) {
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
                        // Soft collision: Đẩy nhẹ 2 quái vật ra xa nhau thay vì giật lùi (tránh bị kẹt
                        // thành 1 cục)
                        double cx1 = enemy.getX() + enemy.getRenderWidth() / 2.0;
                        double cy1 = enemy.getY() + enemy.getRenderHeight() / 2.0;
                        double cx2 = otherEnemy.getX() + otherEnemy.getRenderWidth() / 2.0;
                        double cy2 = otherEnemy.getY() + otherEnemy.getRenderHeight() / 2.0;

                        double dx = cx1 - cx2;
                        double dy = cy1 - cy2;
                        double dist = Math.sqrt(dx * dx + dy * dy);

                        if (dist == 0) { // Xử lý góc lách ngẫu nhiên nếu 2 quái đè khít lên nhau từ lúc spawn
                            dx = Math.random() - 0.5;
                            dy = Math.random() - 0.5;
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
            // Việc này đảm bảo nếu quái bị xô đẩy văng vào tường, nó sẽ ngay lập tức bị
            // giật ngược lại vị trí an toàn
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

        // 2. Kiểm tra va chạm với các vật cản khác (nếu list obstacles vẫn còn dùng sau
        // này)
        for (BaseEntity wall : obstacles) {
            if (player.intersects(wall)) {
                player.onCollision(wall);

                break;
            }
        }

        if (enemyManager != null) {
            List<Enemy> enemies = enemyManager.getEnemyList();
            for (Enemy enemy : enemies) {
                // Phải soi xem nó có đụng Player không đã!
                if (enemy.intersects(player)) {

                    if (enemy instanceof Knight && ((Knight) enemy).isDealingDamage()) {
                        player.takeDamage(enemy.getDamage());
                    } else {
                        enemy.onCollision(player);
                    }

                }
            }
        }
    }

    private Image loadImg(String path) {
        return loadImg(path, 0, 0);
    }

    private Image loadImg(String path, double w, double h) {
        java.io.InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("❌ LỖI KHÔNG TÌM THẤY ẢNH: " + path);
            throw new IllegalArgumentException("Missing image file: " + path);
        }
        return new Image(is, w, h, true, false);
    }

    public static int getGameWidth() {
        return WIDTH;
    }

    public static int getGameHeight() {
        return HEIGHT;
    }

    public static void main(String[] args) {
        launch(args);
    }
}