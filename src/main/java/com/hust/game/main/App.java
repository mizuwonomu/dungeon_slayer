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
import com.hust.game.progression.GameManager;

import com.hust.game.ui.GameFinish;
import com.hust.game.ui.HUD;
import com.hust.game.ui.MenuScreen;
import com.hust.game.ui.LevelClearScreen;
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
    private static final int WIDTH = GameConstants.WINDOW_WIDTH; 
    private static final int HEIGHT = GameConstants.WINDOW_HEIGHT; 

    // kích thước 1 ô trong game 8-bit sau khi upscale (ví dụ 16x16 -> 48x48)
    private static final int TILE_SIZE = 48;
    private AnimationTimer timer;

    // Độ zoom camera (1.5 = phóng to 50%)
    private static final double ZOOM = 1.5;

    private GraphicsContext gc;
    private Player player;
    private CombatManager combatManager;

    private EnemyManager enemyManager; // Gọi quản lý quái vật
    private List<BaseEntity> obstacles = new ArrayList<>(); // danh sách vật cản
    private MapManager mapManager;
    private CollisionChecker collisionChecker;
    private GameManager gameManager;

    // dùng set để lưu những phím đang được giữ để di chuyển chéo
    private Set<KeyCode> input = new HashSet<>();

    private boolean isJHeld = false; // Ngăn chặn đè phím J (buộc phải nhấp nhả)
    private int screenShakeTimer = 0; // Bộ đếm rung màn hình
    private double screenShakeAmplitude = 0.0; // Độ rung (0.0, 0.5, 1.0)

    private boolean isPaused = false;
    private PauseScreen pauseScreen;

    // Camera position
    private double cameraX = 0;
    private double cameraY = 0;
    private int fadeInTimer = 0; // Bộ đếm thời gian fade-in khi vào màn (120 frame = 2s)

    // ... (everything above stays EXACTLY the same)

    @Override
    public void start(Stage stage) {
        stage.setTitle("GHOULITE");

        // Gọi tải toàn bộ âm thanh ngay từ đầu để MenuScreen và Settings có thể dùng được tiếng click/hover
        SoundManager.loadSounds();

        MenuScreen menu = new MenuScreen(

                // START
                v -> {
                    Scene gameScene = createGameScene(stage, 1);
                    stage.setScene(gameScene);
                    gameLoop.start();
                },

                // SETTINGS
                v -> showSettings(stage, () -> showMenu(stage))
        );

        stage.setScene(menu.createScene());
        stage.show();
    }
    private void showMenu(Stage stage) {
        MenuScreen menu = new MenuScreen(

                v -> {
                    Scene gameScene = createGameScene(stage, 1);
                    stage.setScene(gameScene);
                    gameLoop.start();
                },

                v -> showSettings(stage, () -> showMenu(stage))
        );

        stage.setScene(menu.createScene());
    }

    private void showSettings(Stage stage, Runnable onBack) {
        SettingsScreen settings = new SettingsScreen(
                v -> onBack.run()
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
            Scene gameScene = createGameScene(stage, 1);
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
    private boolean isLevelClearUIShown = false;
    private LevelClearScreen levelClearScreen;


    private Scene createGameScene(Stage stage, int startLevel) {
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // Đổ đen ngay từ đầu trên Canvas để tránh nháy khung hình đầu tiên
        gc.setFill(javafx.scene.paint.Color.BLACK);
        gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        Group gameLayer = new Group(canvas);
        StackPane root = new StackPane(gameLayer);
        root.setStyle("-fx-background-color: black;"); // Đảm bảo toàn bộ khung viền cũng hiển thị đen 
        Scene scene = new Scene(root);

        pauseScreen = new PauseScreen(
            () -> setPaused(false),
            // Nút SETTINGS: Tạm tắt logic GameLoop đi để tránh hao CPU, khi quay lại (onBack) thì bật lại GameLoop
            () -> {
                if (gameLoop != null) gameLoop.stop();
                showSettings(stage, () -> {
                    stage.setScene(scene); // Khôi phục lại GameScene đang dở
                    if (gameLoop != null) gameLoop.start();
                });
            },
            () -> {
                setPaused(false);
                    if (gameLoop != null) {
                        gameLoop.stop();
                    }
                showMenu(stage);
            }
        );
        
        Image pauseBtnSheet = loadImg("/assets/pause.png");
        StackPane pauseBtn = createSpriteBtn(pauseBtnSheet, 3, 0.5, () -> togglePause(stage)); // Thu nhỏ nút xuống 50% (còn 64x64)
        StackPane.setAlignment(pauseBtn, javafx.geometry.Pos.TOP_RIGHT); // Căn sát góc trên bên phải
        root.getChildren().add(pauseBtn);

        root.getChildren().add(pauseScreen.getRoot());

        scene.setOnKeyPressed(e -> {
            if (fadeInTimer > 0) return; // Khoá input khi đang chuyển cảnh
            if (e.getCode() == KeyCode.ESCAPE) {
                togglePause(stage);
                return;
            }
            if (!isPaused) {
                input.add(e.getCode());
            }
        });
        scene.setOnKeyReleased(e -> {
            if (fadeInTimer > 0) return; // Khoá input khi đang chuyển cảnh
            if (!isPaused) {
                input.remove(e.getCode());
            }
        });

        initializeEntities(startLevel);
        
        fadeInTimer = 120; // 2 giây fade-in (120 frames ở 60FPS)

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                boolean isVictory = gameManager.isVictory();
                boolean isGameOver = player.isDead();

                if (fadeInTimer > 0) {
                    fadeInTimer--;
                    updateCamera(); // Chỉ tính toán camera theo người chơi, bỏ qua logic game
                    pauseBtn.setMouseTransparent(true); // Khóa tương tác nút Pause
                } else if (!isVictory && !isGameOver && !isPaused) {
                    pauseBtn.setMouseTransparent(false); // Mở khóa nút Pause
                    handleInput();
                    player.update();
                    combatManager.update();
                    
                    updateCamera(); // Tính toán vị trí Camera trước để có toạ độ chuẩn
                    
                    // Logic Culling: Tắt hoạt động của quái vật nằm ngoài màn hình
                    double margin = GameConstants.TILE_SIZE * 2; // Vùng đệm 2 ô để quái kích hoạt mượt mà
                    for (Enemy e : enemyManager.getEnemyList()) {
                        boolean onScreen = e.getX() + e.getRenderWidth() >= cameraX - margin &&
                                           e.getX() <= cameraX + (WIDTH / ZOOM) + margin &&
                                           e.getY() + e.getRenderHeight() >= cameraY - margin &&
                                           e.getY() <= cameraY + (HEIGHT / ZOOM) + margin;
                        e.setActive(onScreen);
                    }

                    enemyManager.updateAll();
                    checkCollisions();
                    com.hust.game.ui.DamageTextManager.update(); // Cập nhật toạ độ và thời gian tồn tại của chữ bay
                }

                gc.clearRect(0, 0, WIDTH, HEIGHT);
                
                // Đổ nền đen cho toàn bộ cửa sổ game để phần viền nằm ngoài map hiển thị màu đen
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillRect(0, 0, WIDTH, HEIGHT);

                gc.save();
                
                gc.scale(ZOOM, ZOOM); // Phóng to camera lên 50%
                
                gc.translate(-cameraX, -cameraY); // Dịch chuyển toạ độ bản đồ theo camera

                // Screen shake
                if (screenShakeTimer > 0 && !isPaused) {
                    screenShakeTimer--;
                    double dx = (Math.random() - 0.5) * screenShakeAmplitude * 20;
                    double dy = (Math.random() - 0.5) * screenShakeAmplitude * 20;
                    gc.translate(dx, dy);
                }

                // Draw map
                gameManager.draw(gc);

                obstacles.forEach(e -> e.render(gc));

                player.render(gc);
                enemyManager.renderAll(gc);
                com.hust.game.ui.DamageTextManager.render(gc); // Vẽ các số sát thương (vẽ sau quái để đè lên trên cùng)

                gc.restore(); // Khôi phục toạ độ nguyên gốc tại đây, để HUD vẽ không bị trượt đi

                hud.render(gc);

                // --- VẼ HIỆU ỨNG FADE-IN TỪ ĐEN SANG GAME ---
                if (fadeInTimer > 0) {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.setGlobalAlpha((double) fadeInTimer / 120.0);
                    gc.fillRect(0, 0, WIDTH, HEIGHT);
                    gc.setGlobalAlpha(1.0);
                }

                // Clear level
                if (isVictory && gameManager.getCurrentLevelIndex() < 2 && !isLevelClearUIShown) {
                    gameLoop.stop();
                    levelClearScreen = new LevelClearScreen(
                            // Next level
                            () -> {
                                isLevelClearUIShown = false;
                                root.getChildren().remove(levelClearScreen.getRoot());
                                gameManager.loadNextLevel();
                                collisionChecker = new CollisionChecker(gameManager.getMap());
                                combatManager.resetSkill();
                                gameLoop.start();
                            },
                            // Retry
                            () -> {
                                isLevelClearUIShown = false;
                                Scene newGame = createGameScene(stage, gameManager.getCurrentLevelIndex());
                                stage.setScene(newGame);
                                gameLoop.start();
                            },
                            // Menu
                            () -> {
                                isLevelClearUIShown = false;
                                showMenu(stage);
                            }
                    );
                    levelClearScreen.setVisible(true);
                    root.getChildren().add(levelClearScreen.getRoot());
                    isLevelClearUIShown = true;
                }

                // End screen
                if ((isVictory && gameManager.getCurrentLevelIndex() >= 2) || isGameOver) {
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
                                    int retryLevel = isVictory ? 1 : gameManager.getCurrentLevelIndex();
                                    Scene newGame = createGameScene(stage, retryLevel);
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

    private void updateCamera() {
        if (player == null) return;
        
        // Tính toạ độ của người chơi trên màn hình hiện tại
        double playerScreenX = player.getX() - cameraX;
        double playerScreenY = player.getY() - cameraY;

        // Tính toán kích thước Viewport thực tế sau khi zoom
        double viewW = WIDTH / ZOOM;
        double viewH = HEIGHT / ZOOM;

        // Xác định vùng Deadzone (40% ở chính giữa màn hình viewport)
        double dzWidth = viewW * 0.4;
        double dzHeight = viewH * 0.4;
        double dzLeft = (viewW - dzWidth) / 2.0;
        double dzRight = dzLeft + dzWidth;
        double dzTop = (viewH - dzHeight) / 2.0;
        double dzBottom = dzTop + dzHeight;

        // Nếu player chạm biên giới hạn Deadzone, trượt camera theo
        if (playerScreenX < dzLeft) {
            cameraX -= (dzLeft - playerScreenX);
        } else if (playerScreenX + player.getRenderWidth() > dzRight) {
            cameraX += (playerScreenX + player.getRenderWidth() - dzRight);
        }
        if (playerScreenY < dzTop) {
            cameraY -= (dzTop - playerScreenY);
        } else if (playerScreenY + player.getRenderHeight() > dzBottom) {
            cameraY += (playerScreenY + player.getRenderHeight() - dzBottom);
        }

        // Phương án 2: Tắt giới hạn Clamp Camera
        // Camera giờ đây sẽ luôn đi theo Player dù chạm tới góc map.
        // Phần ngoài map không có TILE sẽ lộ ra nền đen bọc ngoài (đã vẽ bằng fillRect ở phía trên).
    }

    private void togglePause(Stage stage) {
        if (player == null || enemyManager == null || pauseScreen == null) {
            return;
        }
        boolean isVictory = enemyManager.getEnemyList().isEmpty();
        boolean isGameOver = player.isDead();
        if (isVictory || isGameOver || isEndUIShown || isLevelClearUIShown) {
            return;
        }
        setPaused(!isPaused);
    }

    private void setPaused(boolean paused) {
        isPaused = paused;
        pauseScreen.setVisible(paused);
        if (paused) {
            com.hust.game.audio.SoundManager.playPauseSound(); // Phát âm thanh đặc biệt khi game bắt đầu Pause
            input.clear();
            SoundManager.stopGameplaySounds();
        } else {
            com.hust.game.audio.SoundManager.playUnpauseSound(); // Phát âm thanh khi quay lại game
        }
    }

    private void initializeEntities(int level) {
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
            
            gameManager = new GameManager(
                enemyManager,
                player,
                treeImg, treeSkillImg,
                slimeImg,
                knightImg, knightSkillImg,
                witchImg, witchSkillImg
            );

            gameManager.loadLevel(level);

            // --- TEST MODE: Set vị trí spawn (4, 5) và xóa hết quái ở Level 1 ---
            if (level == 1) {
                player.setX(4 * GameConstants.TILE_SIZE); // Vị trí x (cột 4)
                player.setY(5 * GameConstants.TILE_SIZE); // Vị trí y (hàng 5)
           //     enemyManager.getEnemyList().clear(); // Xóa sạch danh sách quái
            }

            // tạo combat manager
            combatManager = new CombatManager(player, enemyManager.getEnemyList());

        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy file ảnh!");
            e.printStackTrace();
            System.exit(1);
        }
        collisionChecker = new CollisionChecker(gameManager.getMap());
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
        // 1. Kiểm tra va chạm với bề mặt Map (Wall, Pond) - Xử lý trượt tường (Wall Sliding)
        double padding = 12; // Cắt bớt phần viền trong suốt của ảnh để nhân vật đi mượt hơn
        double nextX = player.getX();
        double nextY = player.getY();
        double lastX = player.getLastX();
        double lastY = player.getLastY();

        // Bước 1: Thử di chuyển trục X trước
        player.setX(nextX);
        player.setY(lastY);
        int left = (int) (player.getX() + padding);
        int right = (int) (player.getX() + player.getRenderWidth() - padding);
        int top = (int) (player.getY() + padding);
        int bottom = (int) (player.getY() + player.getRenderHeight() - padding);

        if (collisionChecker.checkTile(left, top) || collisionChecker.checkTile(right, top) ||
            collisionChecker.checkTile(left, bottom) || collisionChecker.checkTile(right, bottom)) {
            player.setX(lastX); // Chạm tường trục X -> Hủy di chuyển X
        }

        // Bước 2: Thử di chuyển trục Y trên cơ sở X đã an toàn
        player.setY(nextY);
        left = (int) (player.getX() + padding);
        right = (int) (player.getX() + player.getRenderWidth() - padding);
        top = (int) (player.getY() + padding);
        bottom = (int) (player.getY() + player.getRenderHeight() - padding);

        if (collisionChecker.checkTile(left, top) || collisionChecker.checkTile(right, top) ||
            collisionChecker.checkTile(left, bottom) || collisionChecker.checkTile(right, bottom)) {
            player.setY(lastY); // Chạm tường trục Y -> Hủy di chuyển Y
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

    private StackPane createSpriteBtn(Image spriteSheet, int numFrames, double scaleMultiplier, Runnable action) {
        StackPane pane = new StackPane();
        
        double frameW = spriteSheet.getWidth() / numFrames;
        double frameH = spriteSheet.getHeight();
        
        ImageView view = new ImageView(spriteSheet);
        view.setViewport(new javafx.geometry.Rectangle2D(0, 0, frameW, frameH));
        
        view.setFitWidth(frameW * scaleMultiplier);
        view.setFitHeight(frameH * scaleMultiplier);

        pane.setPrefSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        pane.setMaxSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        
        pane.getChildren().add(view);
        
        int[] currentFrame = {0};
        javafx.animation.Timeline[] timeline = {null};
        
        pane.setOnMouseEntered(e -> {
            com.hust.game.audio.SoundManager.playButtonHoverSound();
            pane.setScaleX(1.10);
            pane.setScaleY(1.10);
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = numFrames - 1 - currentFrame[0];
            if (framesToAnimate > 0) {
                timeline[0] = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(30), evt -> {
                    currentFrame[0]++;
                    view.setViewport(new javafx.geometry.Rectangle2D(currentFrame[0] * frameW, 0, frameW, frameH));
                }));
                timeline[0].setCycleCount(framesToAnimate);
                timeline[0].play();
            }
        });
        
        pane.setOnMouseExited(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = currentFrame[0];
            if (framesToAnimate > 0) {
                timeline[0] = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), evt -> {
                    currentFrame[0]--;
                    view.setViewport(new javafx.geometry.Rectangle2D(currentFrame[0] * frameW, 0, frameW, frameH));
                }));
                timeline[0].setCycleCount(framesToAnimate);
                timeline[0].play();
            }
        });
        
        pane.setOnMousePressed(e -> {
            // Không phát tiếng click mặc định ở đây nữa, âm thanh sẽ được phát ở hàm setPaused()
            pane.setScaleX(0.9);
            pane.setScaleY(0.9);
        });
        pane.setOnMouseReleased(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
        });
        
        pane.setOnMouseClicked(e -> action.run());
        pane.setCursor(javafx.scene.Cursor.HAND);
        
        return pane;
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