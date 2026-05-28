package com.hust.game.main;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import com.hust.game.dev.DevSettings;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.npc.Npc;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;
import com.hust.game.entities.interfaces.Interactable;
import com.hust.game.combat.CombatManager;
import com.hust.game.enemy.EnemyManager;
import com.hust.game.enemy.Knight;
import com.hust.game.enemy.Enemy;
import com.hust.game.map.MapManager;
import com.hust.game.collision.CollisionChecker;
import com.hust.game.progression.GameManager;
import com.hust.game.progression.TutorialManager;

import com.hust.game.ui.GameFinish;
import com.hust.game.ui.HUD;
import com.hust.game.ui.MenuScreen;
import com.hust.game.ui.LevelClearScreen;
import com.hust.game.ui.TutorialCompleteScreen;
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
import java.util.Iterator;

public class App extends Application {
    // kích thước cửa sổ
    // Cập nhật lại kích thước khớp chuẩn tỷ lệ 1.7 (17 cột x 10 hàng, TILE_SIZE = 48)
    private static final int WIDTH = GameConstants.WINDOW_WIDTH; 
    private static final int HEIGHT = GameConstants.WINDOW_HEIGHT; 

    // kích thước 1 ô trong game 8-bit sau khi upscale (ví dụ 16x16 -> 48x48)
    private static final int TILE_SIZE = 48;
    private AnimationTimer timer;

    // Độ zoom camera (1.0 = gốc)
    private static final double ZOOM = 1.0;

    private GraphicsContext gc;
    private Player player;
    private CombatManager combatManager;

    private EnemyManager enemyManager; // Gọi quản lý quái vật
    private List<BaseEntity> obstacles = new ArrayList<>(); // danh sách vật cản
    private MapManager mapManager;
    private CollisionChecker collisionChecker;
    private GameManager gameManager;
    private TutorialManager tutorialManager;

    // dùng set để lưu những phím đang được giữ để di chuyển chéo
    private Set<KeyCode> input = new HashSet<>();

    private boolean isJHeld = false; // Ngăn chặn đè phím J (buộc phải nhấp nhả)
    private boolean isEHeld = false;
    private boolean isRHeld = false;
    private boolean isHHeld = false;
    private boolean isOHeld = false;
    private boolean is1Held = false;
    private boolean is2Held = false;
    private int screenShakeTimer = 0; // Bộ đếm rung màn hình
    private double screenShakeAmplitude = 0.0; // Độ rung (0.0, 0.5, 1.0)
    private int hitStopTimer = 0;
    private boolean isMousePressed = false;

    private boolean isPaused = false;
    private PauseScreen pauseScreen;

    // Camera position
    private double cameraX = 0;
    private double cameraY = 0;
    private int fadeInTimer = 0; // Bộ đếm thời gian fade-in khi vào màn (120 frame = 2s)

    // ... (everything above stays EXACTLY the same)

    private javafx.scene.media.MediaPlayer menuMusicPlayer;
    private javafx.scene.media.MediaPlayer inGameMusicPlayer;
    private static App instance;

    private Image healthPotionImg;
    private Image manaPotionImg;

    public static void triggerScreenShake(int timer, double amplitude) {
        if (instance != null) {
            instance.screenShakeTimer = timer;
            instance.screenShakeAmplitude = amplitude;
        }
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        stage.setTitle("GHOULITE");

        // Gọi tải toàn bộ âm thanh ngay từ đầu để MenuScreen và Settings có thể dùng được tiếng click/hover
        SoundManager.loadSounds();
        playMenuMusic();

        MenuScreen menu = new MenuScreen(

                // START
                level -> {
                    stopMenuMusic();
                    Scene gameScene = createGameScene(stage, level);
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
        stopInGameMusic();
        playMenuMusic();
        MenuScreen menu = new MenuScreen(

                level -> {
                    stopMenuMusic();
                    Scene gameScene = createGameScene(stage, level);
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
            stopMenuMusic();
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
    private boolean isTutorialClearUIShown = false;
    private TutorialCompleteScreen tutorialClearScreen;


    private Scene createGameScene(Stage stage, int startLevel) {
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        Image tempBackScreen = null;
        try {
            tempBackScreen = loadImg("/assets/back_screen.png");
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh back_screen.png");
        }
        final Image backScreenImg = tempBackScreen;

        // Đổ hình nền hoặc đổ đen ngay từ đầu trên Canvas để tránh nháy khung hình đầu tiên
        if (backScreenImg != null) {
            gc.drawImage(backScreenImg, 0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        } else {
            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        }

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
        
        scene.setOnMousePressed(e -> {
            if (fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = true;
        });
        scene.setOnMouseReleased(e -> {
            if (fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = false;
        });

        initializeEntities(startLevel);
        playInGameMusic(startLevel);
        
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
                    
                    // Cơ chế Hit Stop: Đóng băng toàn bộ hoạt động (trừ việc vẽ ra hình)
                    if (hitStopTimer > 0) {
                        hitStopTimer--;
                    } else {
                        Npc npc = getActiveNpc();
                        if (npc != null) {
                            npc.update(player);
                            handleNpcInput(npc);
                        }
                        boolean isNpcInteractionOpen = npc != null && npc.isInteractionOpen();

                        if (isNpcInteractionOpen) {
                            player.savePosition();
                            player.setState(EntityState.IDLE);
                        } else {
                            handleInput();
                        }

                        player.update();
                        updateCamera(); // Tính toán vị trí Camera trước để có toạ độ chuẩn

                        if (!isNpcInteractionOpen) {
                            int stopFrames = combatManager.consumeHitStopFrames();
                            if (stopFrames > 0) {
                                hitStopTimer = stopFrames;
                            }

                            int sTimer = combatManager.consumeShakeTimer();
                            if (sTimer > 0) {
                                screenShakeTimer = sTimer;
                                screenShakeAmplitude = combatManager.consumeShakeAmplitude();
                            }

                            if (tutorialManager != null) {
                                tutorialManager.update(player, input, isMousePressed);
                            }

                            combatManager.update();
                            gameManager.update(); // Cập nhật logic của map (ví dụ Gate)

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
                    }
                }

                gc.clearRect(0, 0, WIDTH, HEIGHT);
                
                // Đổ nền cho toàn bộ cửa sổ game để phần viền nằm ngoài map hiển thị ảnh
                if (backScreenImg != null) {
                    gc.drawImage(backScreenImg, 0, 0, WIDTH, HEIGHT);
                } else {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.fillRect(0, 0, WIDTH, HEIGHT);
                }

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

                // --- 2.5D DEPTH SORTING (Z-INDEX) ---
                // Gom tất cả Entity lại để sắp xếp thứ tự vẽ (Y-Sorting)
                List<BaseEntity> renderList = new ArrayList<>();
                renderList.add(player);
                
                // Thêm Quái vật
                for (Enemy e : enemyManager.getEnemyList()) {
                    if (e.isActive()) renderList.add(e);
                }
                
                // Thêm Cổng (Gate)
                if (gameManager.getGates() != null) {
                    renderList.addAll(gameManager.getGates());
                }

                Npc npc = getActiveNpc();
                if (npc != null) {
                    renderList.add(npc);
                }
                
                // Thêm Tường/Vật cản từ Map (áp dụng Culling để tối ưu hiệu năng)
                if (gameManager.getMap() != null && gameManager.getMap().mapEntities != null) {
                    double margin = GameConstants.TILE_SIZE * 2;
                    for (BaseEntity wall : gameManager.getMap().mapEntities) {
                        if (wall.getX() + wall.getRenderWidth() >= cameraX - margin &&
                            wall.getX() <= cameraX + (WIDTH / ZOOM) + margin &&
                            wall.getY() + wall.getRenderHeight() >= cameraY - margin &&
                            wall.getY() <= cameraY + (HEIGHT / ZOOM) + margin) {
                            renderList.add(wall);
                        }
                    }
                }
                
                renderList.addAll(obstacles);
                
                // Sắp xếp sao cho entity nào có tọa độ đáy (Y + Height) thấp hơn sẽ vẽ trước, cao hơn vẽ sau (đè lên)
                renderList.sort((e1, e2) -> {
                    double bottom1 = e1.getY() + e1.getRenderHeight();
                    double bottom2 = e2.getY() + e2.getRenderHeight();
                    return Double.compare(bottom1, bottom2);
                });

                // Vẽ lần lượt theo thứ tự đã sắp xếp
                for (BaseEntity entity : renderList) {
                    entity.render(gc);
                }
                
                combatManager.getParticleManager().render(gc);

                com.hust.game.ui.DamageTextManager.render(gc); // Vẽ các số sát thương (vẽ sau quái để đè lên trên cùng)
                combatManager.renderCrits(gc); // Vẽ text CRIT đè lên trên cùng

                gc.restore(); // Khôi phục toạ độ nguyên gốc tại đây, để HUD vẽ không bị trượt đi

                hud.render(gc);

                if (npc != null) {
                    npc.renderOverlay(gc);
                }
                
                if (tutorialManager != null) {
                    tutorialManager.render(gc);
                }

                // --- VẼ HIỆU ỨNG FADE-IN TỪ ĐEN SANG GAME ---
                if (fadeInTimer > 0) {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.setGlobalAlpha((double) fadeInTimer / 120.0);
                    gc.fillRect(0, 0, WIDTH, HEIGHT);
                    gc.setGlobalAlpha(1.0);
                }

                // Tutorial clear
                if (isVictory && gameManager.getCurrentLevelIndex() == 0 && !isTutorialClearUIShown) {
                    gameLoop.stop();
                    tutorialClearScreen = new TutorialCompleteScreen(
                            // Bấm Start Level 1
                            () -> {
                                isTutorialClearUIShown = false;
                                root.getChildren().remove(tutorialClearScreen.getRoot());
                                showLoadingScreen(stage, scene, () -> {
                                    gameManager.loadLevel(1);
                                    collisionChecker = new CollisionChecker(gameManager.getMap());
                                    combatManager.setCollisionChecker(collisionChecker);
                                    enemyManager.setCollisionChecker(collisionChecker);
                                    combatManager.resetSkill();
                                    input.clear(); // Chống kẹt nút
                                    fadeInTimer = 120;
                                    playInGameMusic(1);
                                    gameLoop.start();
                                });
                            },
                            // Về Menu
                            () -> {
                                isTutorialClearUIShown = false;
                                showMenu(stage);
                            }
                    );
                    tutorialClearScreen.setVisible(true);
                    root.getChildren().add(tutorialClearScreen.getRoot());
                    isTutorialClearUIShown = true;
                }

                // Clear level
                if (isVictory && gameManager.getCurrentLevelIndex() > 0 && gameManager.getCurrentLevelIndex() < 3 && !isLevelClearUIShown) {
                    gameLoop.stop();
                    levelClearScreen = new LevelClearScreen(
                            // Next level
                            () -> {
                                isLevelClearUIShown = false;
                                root.getChildren().remove(levelClearScreen.getRoot());
                                showLoadingScreen(stage, scene, () -> {
                                    gameManager.loadNextLevel();
                                    collisionChecker = new CollisionChecker(gameManager.getMap());
                                    combatManager.setCollisionChecker(collisionChecker);
                                    enemyManager.setCollisionChecker(collisionChecker);
                                    combatManager.resetSkill();
                                    input.clear(); // Xóa các phím đang đè để tránh kẹt nút
                                    fadeInTimer = 120; // Kích hoạt lại hiệu ứng mờ dần sáng lên khi vào màn mới
                                    playInGameMusic(gameManager.getCurrentLevelIndex());
                                    gameLoop.start();
                                });
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
                if ((isVictory && gameManager.getCurrentLevelIndex() >= 3) || isGameOver) {
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

        // Xác định vùng Deadzone thu hẹp sát player (chỉ khoảng = hitbox player + 1/2)
        double dzWidth = player.getRenderWidth() * 1.5;
        double dzHeight = player.getRenderHeight() * 1.5;
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
        if (isVictory || isGameOver || isEndUIShown || isLevelClearUIShown || isTutorialClearUIShown) {
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

            Image dDown = loadImg("/assets/player/dash_down.png");
            Image dUp = loadImg("/assets/player/dash_up.png");
            Image dLeft = loadImg("/assets/player/dash_left.png");
            Image dRight = loadImg("/assets/player/dash_right.png");

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

            healthPotionImg = loadImg("/assets/items/health_potion.png");
            manaPotionImg = loadImg("/assets/items/mana_potion.png");

            Image bossImg = loadImg("/assets/enemy/final_boss_idle.png");

            // Khai báo Player trước khi đưa cho Quái
            player = new Player(WIDTH / 2, HEIGHT / 2,
                    iDown, iUp, iLeft, iRight, rDown, rUp, rLeft, rRight,
                    cDown, cUp, cLeft, cRight, 
                    dDown, dUp, dLeft, dRight, 
                    swordHit, rageHit, powerUpImg, thunderImg);

            // Sinh quái vật để test di chuyển
            enemyManager = new EnemyManager();
            
            gameManager = new GameManager(
                enemyManager,
                player,
                treeImg, treeSkillImg,
                slimeImg,
                knightImg, knightSkillImg,
                witchImg, witchSkillImg,
                bossImg
            );

            gameManager.loadLevel(level);

            if (level == 0) {
                tutorialManager = new TutorialManager();
            } else {
                tutorialManager = null;
            }

            // tạo combat manager
            combatManager = new CombatManager(player, enemyManager.getEnemyList());

        } catch (Exception e) {
            System.err.println("LỖI: Không tìm thấy file ảnh!");
            e.printStackTrace();
            System.exit(1);
        }
        collisionChecker = new CollisionChecker(gameManager.getMap());
        combatManager.setCollisionChecker(collisionChecker);
        enemyManager.setCollisionChecker(collisionChecker);
        hud = new HUD(player, combatManager);
    }

    private void handleInput() {
        player.savePosition();

        boolean isAnyKeyPressed = false;

        // Xử lý phím SHIFT để Dash
        if (input.contains(KeyCode.SHIFT)) {
            if (player.canDash()) {
                double dashDx = 0, dashDy = 0;
                if (input.contains(KeyCode.W) || input.contains(KeyCode.UP)) dashDy -= 1;
                if (input.contains(KeyCode.S) || input.contains(KeyCode.DOWN)) dashDy += 1;
                if (input.contains(KeyCode.A) || input.contains(KeyCode.LEFT)) dashDx -= 1;
                if (input.contains(KeyCode.D) || input.contains(KeyCode.RIGHT)) dashDx += 1;
                
                player.startDash(dashDx, dashDy);
                com.hust.game.audio.SoundManager.playSlimeMoveSound(); // Tạm dùng âm thanh này làm tiếng gió lướt
            }
        }

        if (player.isDashing()) {
            player.setX(player.getX() + player.getDashVectorX());
            player.setY(player.getY() + player.getDashVectorY());
            isAnyKeyPressed = true; // Giữ trạng thái RUNNING để animation chạy bộ nhìn mượt
        } else if (!player.isAttacking() && !player.isThrusting()) {
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
                    combatManager.playerAttack();
                }
                isJHeld = true; // Đánh dấu là đang đè phím
            }
        } else {
            isJHeld = false; // Nhả phím J ra thì reset cờ cho phép chém tiếp
        }

        if (input.contains(KeyCode.L)) {
            combatManager.activateSkill();
        }

        if (input.contains(KeyCode.E)) {
            if (!isEHeld) {
                player.useHealthPotion();
                isEHeld = true;
            }
        } else {
            isEHeld = false;
        }

        if (input.contains(KeyCode.Q)) {
            if (!isRHeld) {
                player.useManaPotion();
                isRHeld = true;
            }
        } else {
            isRHeld = false;
        }

        if (isAnyKeyPressed) {
            player.setState(EntityState.RUNNING);
        } else {
            player.setState(EntityState.IDLE);
        }
    }

    private void handleNpcInput(Npc npc) {
        if (input.contains(KeyCode.H)) {
            if (!isHHeld) {
                npc.startInteraction();
                isHHeld = true;
            }
        } else {
            isHHeld = false;
        }

        boolean onePressed = input.contains(KeyCode.DIGIT1) || input.contains(KeyCode.NUMPAD1);
        if (onePressed) {
            if (!is1Held) {
                if (npc.isInteractionOpen()) {
                    npc.handleOptionOne(player);
                }
                is1Held = true;
            }
        } else {
            is1Held = false;
        }

        boolean twoPressed = input.contains(KeyCode.DIGIT2) || input.contains(KeyCode.NUMPAD2);
        if (twoPressed) {
            if (!is2Held) {
                if (npc.isInteractionOpen()) {
                    npc.handleOptionTwo(player);
                }
                is2Held = true;
            }
        } else {
            is2Held = false;
        }

        if (input.contains(KeyCode.O)) {
            if (!isOHeld) {
                npc.closeInteraction();
                isOHeld = true;
            }
        } else {
            isOHeld = false;
        }
        
        if (input.contains(KeyCode.SPACE) || isMousePressed) {
            npc.skipDialogue();
        }
    }

    private Npc getActiveNpc() {
        if (gameManager == null || gameManager.getCurrentLevelIndex() != 1) {
            return null;
        }
        return gameManager.getNpc();
    }

    private void checkCollisions() {
        // 1. Kiểm tra va chạm với bề mặt Map (Wall, Pond) - Xử lý trượt tường (Wall Sliding)
        double nextX = player.getX();
        double nextY = player.getY();
        double lastX = player.getLastX();
        double lastY = player.getLastY();

        // Bước 1: Thử di chuyển trục X trước
        player.setX(nextX);
        player.setY(lastY);
        javafx.geometry.Rectangle2D pCol = player.getCollisionBoundary();
        int left = (int) pCol.getMinX();
        int right = (int) pCol.getMaxX();
        int top = (int) pCol.getMinY();
        int bottom = (int) pCol.getMaxY();

        boolean hitX = collisionChecker.checkTile(left, top) || collisionChecker.checkTile(right, top) ||
                       collisionChecker.checkTile(left, bottom) || collisionChecker.checkTile(right, bottom);

        if (!hitX && gameManager.getGates() != null) {
            for (com.hust.game.entities.environment.Gate gate : gameManager.getGates()) {
                if (gate.isSolid() && pCol.intersects(gate.getBoundary())) { // Dùng getBoundary() để lấy trọn khối 48x48
                    hitX = true;
                    break;
                }
            }
        }

        if (hitX) {
            player.setX(lastX); // Chạm tường trục X -> Hủy di chuyển X
        }

        // Bước 2: Thử di chuyển trục Y trên cơ sở X đã an toàn
        player.setY(nextY);
        pCol = player.getCollisionBoundary();
        left = (int) pCol.getMinX();
        right = (int) pCol.getMaxX();
        top = (int) pCol.getMinY();
        bottom = (int) pCol.getMaxY();

        boolean hitY = collisionChecker.checkTile(left, top) || collisionChecker.checkTile(right, top) ||
                       collisionChecker.checkTile(left, bottom) || collisionChecker.checkTile(right, bottom);

        if (!hitY && gameManager.getGates() != null) {
            for (com.hust.game.entities.environment.Gate gate : gameManager.getGates()) {
                if (gate.isSolid() && pCol.intersects(gate.getBoundary())) {
                    hitY = true;
                    break;
                }
            }
        }

        if (hitY) {
            player.setY(lastY); // Chạm tường trục Y -> Hủy di chuyển Y
        }

        // Kiểm tra va chạm cho tất cả Enemy với địa hình (Wall, Pond, Gate)
        if (enemyManager != null) {
            List<Enemy> enemies = enemyManager.getEnemyList();

            // Bước 1: Tính toán lực đẩy (Soft Collision) giữa các quái vật trước
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (!enemy.isActive()) continue; // Tối ưu: Bỏ qua quái ngoài màn hình
                
                // Kiểm tra va chạm với các quái vật khác (tránh đè lên nhau)
                for (int j = i + 1; j < enemies.size(); j++) {
                    Enemy otherEnemy = enemies.get(j);
                    if (!otherEnemy.isActive()) continue; // Tối ưu: Bỏ qua quái ngoài màn hình
                    
                    if (enemy.getCollisionBoundary().intersects(otherEnemy.getCollisionBoundary())) {
                        // Soft collision: Đẩy nhẹ 2 quái vật ra xa nhau thay vì giật lùi (tránh bị kẹt
                        // thành 1 cục)
                        javafx.geometry.Rectangle2D eCol1 = enemy.getCollisionBoundary();
                        javafx.geometry.Rectangle2D eCol2 = otherEnemy.getCollisionBoundary();
                        double cx1 = eCol1.getMinX() + eCol1.getWidth() / 2.0;
                        double cy1 = eCol1.getMinY() + eCol1.getHeight() / 2.0;
                        double cx2 = eCol2.getMinX() + eCol2.getWidth() / 2.0;
                        double cy2 = eCol2.getMinY() + eCol2.getHeight() / 2.0;

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
                if (!enemy.isActive()) continue; // Tối ưu: Bỏ qua quái ngoài màn hình
                
                javafx.geometry.Rectangle2D eCol = enemy.getCollisionBoundary();
                int eLeft = (int) eCol.getMinX();
                int eRight = (int) eCol.getMaxX();
                int eTop = (int) eCol.getMinY();
                int eBottom = (int) eCol.getMaxY();

                boolean eHit = collisionChecker.checkTile(eLeft, eTop) || collisionChecker.checkTile(eRight, eTop) ||
                               collisionChecker.checkTile(eLeft, eBottom) || collisionChecker.checkTile(eRight, eBottom);

                if (!eHit && gameManager.getGates() != null) {
                    for (com.hust.game.entities.environment.Gate gate : gameManager.getGates()) {
                        if (gate.isSolid() && eCol.intersects(gate.getBoundary())) {
                            eHit = true;
                            break;
                        }
                    }
                }
                
                if (eHit) {
                    enemy.onCollision(null); // Trở về lastX, lastY ban đầu (Bên ngoài tường)
                }
            }
        }

        if (enemyManager != null) {
            List<Enemy> enemies = enemyManager.getEnemyList();
            for (Enemy enemy : enemies) {
                if (!enemy.isActive()) continue; // Tối ưu: Bỏ qua quái ngoài màn hình
                
                // Phải soi xem nó có đụng Player không đã!
                if (enemy.getCollisionBoundary().intersects(player.getCollisionBoundary())) {

                    // Chặn Player không đi xuyên qua quái
                    player.onCollision(enemy);

                    if (enemy instanceof Knight && ((Knight) enemy).isDealingDamage()) {
                        player.takeDamage(enemy.getDamage(), enemy);
                    } else {
                        enemy.onCollision(player);
                    }

                }
            }
        }

        // KIỂM TRA NHẶT VẬT PHẨM (Interactable)
        if (gameManager.getMap() != null && gameManager.getMap().mapEntities != null) {
            double pX = player.getX();
            double pY = player.getY();
            Iterator<BaseEntity> it = gameManager.getMap().mapEntities.iterator();
            while (it.hasNext()) {
                BaseEntity entity = it.next();
                
                // Tối ưu culling: Chỉ kiểm tra va chạm với những vật thể nằm rất gần Player (Khoảng ~3 ô)
                if (Math.abs(entity.getX() - pX) > 150 || Math.abs(entity.getY() - pY) > 150) {
                    continue;
                }

                if (entity instanceof Interactable) {
                    // Player dẫm lên vật phẩm (so sánh 2 hitbox)
                    if (player.getCollisionBoundary().intersects(entity.getBoundary())) {
                        boolean pickedUp = ((Interactable) entity).onInteract(player);
                        if (pickedUp) {
                            it.remove(); // Xóa vật phẩm khỏi bản đồ nếu nhặt thành công
                            com.hust.game.audio.SoundManager.playButtonHoverSound(); // Tạm dùng âm thanh này làm hiệu ứng nhặt đồ
                        }
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

    private void playMenuMusic() {
        if (menuMusicPlayer == null) {
            try {
                java.net.URL url = getClass().getResource("/sounds/background_music.mp3");
                if (url != null) {
                    javafx.scene.media.Media media = new javafx.scene.media.Media(url.toExternalForm());
                    menuMusicPlayer = new javafx.scene.media.MediaPlayer(media);
                    menuMusicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE); // Lặp vô hạn
                    menuMusicPlayer.setVolume(SoundManager.getBgmVolume());
                }
            } catch (Exception e) {
                System.err.println("Lỗi load nhạc menu: " + e.getMessage());
            }
        }
        if (menuMusicPlayer != null && menuMusicPlayer.getStatus() != javafx.scene.media.MediaPlayer.Status.PLAYING) {
            menuMusicPlayer.play();
        }
    }

    private void stopMenuMusic() {
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stop();
        }
    }

    private void playInGameMusic(int level) {
        stopInGameMusic();
        String musicPath = "";
        double volMultiplier = 1.0;
        
        if (level == 0) {
            musicPath = "/sounds/tutorial.mp3";
            volMultiplier = 0.5;
        } else if (level == 1) {
            musicPath = "/sounds/Music_lvl_1.mp3";
            volMultiplier = 0.7;
        } else if (level == 3) {
            musicPath = "/sounds/boss_fight_music.mp3";
            volMultiplier = 0.8; // Âm lượng nhạc Boss (có thể chỉnh lại nếu thấy quá to/nhỏ)
        }

        if (!musicPath.isEmpty()) {
            try {
                java.net.URL url = getClass().getResource(musicPath);
                if (url != null) {
                    javafx.scene.media.Media media = new javafx.scene.media.Media(url.toExternalForm());
                    inGameMusicPlayer = new javafx.scene.media.MediaPlayer(media);
                    inGameMusicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE); // Lặp vô hạn
                    inGameMusicPlayer.setVolume(volMultiplier * SoundManager.getBgmVolume());
                    inGameMusicPlayer.play();
                }
            } catch (Exception e) {
                System.err.println("Lỗi load nhạc in-game: " + e.getMessage());
            }
        }
    }

    private void stopInGameMusic() {
        if (inGameMusicPlayer != null) {
            inGameMusicPlayer.stop();
            inGameMusicPlayer.dispose();
            inGameMusicPlayer = null;
        }
    }

    public static void updateBgmVolume(double vol) {
        if (instance != null) {
            if (instance.menuMusicPlayer != null) {
                instance.menuMusicPlayer.setVolume(vol);
            }
            if (instance.inGameMusicPlayer != null) {
                double multiplier = (instance.gameManager != null && instance.gameManager.getCurrentLevelIndex() == 0) ? 0.5 : 0.7;
                instance.inGameMusicPlayer.setVolume(multiplier * vol);
            }
        }
    }

    private void showLoadingScreen(Stage stage, Scene nextScene, Runnable onLoaded) {
        if (DevSettings.shouldSkipLoadingScreens()) {
            if (App.this.gc != null) {
                App.this.gc.clearRect(0, 0, WIDTH, HEIGHT);
                App.this.gc.setFill(javafx.scene.paint.Color.BLACK);
                App.this.gc.fillRect(0, 0, WIDTH, HEIGHT);
            }
            stage.setScene(nextScene);
            onLoaded.run();
            return;
        }

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);
        Scene loadingScene = new Scene(root, WIDTH, HEIGHT);

        Image bgSheet = loadImg("/assets/menu_background.png");
        Image loadingBallSheet = loadImg("/assets/loading_ball.png");

        javafx.scene.text.Font hintFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 28);
        if (hintFont == null) {
            hintFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 28);
        }
        final javafx.scene.text.Font finalHintFont = hintFont;

        String[] hints = {
            "Sử dụng WASD để di chuyển, J để tấn công, L để bật cuồng nộ!",
            "Sử dụng cuồng nộ có thể x2 sát thương đó!",
            "Phát triển bởi nhóm 27 Ộ Ô Pi!",
            "Slime là sinh vật chạm vào thôi là mất máu!",
            "Cái cây có tầm đánh y như player! Cẩn thận!",
            "Quyền năng hắc ám của phù thủy có thể triệu hồi ra hiệp sĩ!"
        };
        int[] hintIndex = { (int) (Math.random() * hints.length) };
        String[] selectedHint = { hints[hintIndex[0]] };

        int[] loadingTimer = { 0 };
        int[] hintTimer = { 0 };
        double[] bgAlpha = { 0.0 };

        root.setOnMouseClicked(e -> {
            hintTimer[0] = 0;
            hintIndex[0] = (hintIndex[0] + 1) % hints.length;
            selectedHint[0] = hints[hintIndex[0]];
        });

        AnimationTimer loadingLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                loadingTimer[0]++;
                hintTimer[0]++;

                // Random chữ sau mỗi 5 giây giống ngoài menu
                if (hintTimer[0] >= 300) {
                    hintTimer[0] = 0;
                    hintIndex[0] = (hintIndex[0] + 1) % hints.length;
                    selectedHint[0] = hints[hintIndex[0]];
                }

                // Hiệu ứng mờ dần (Fade In - Fade Out)
                if (loadingTimer[0] < 30) {
                    bgAlpha[0] += 0.01;
                    if (bgAlpha[0] > 0.3) bgAlpha[0] = 0.3; // Nền mờ 0.3 để không bị chói
                } else if (loadingTimer[0] > 540) {
                    bgAlpha[0] -= 0.02;
                    if (bgAlpha[0] < 0) bgAlpha[0] = 0;
                }

                gc.clearRect(0, 0, WIDTH, HEIGHT);
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillRect(0, 0, WIDTH, HEIGHT);

                gc.save();
                gc.setGlobalAlpha(bgAlpha[0]);

                int bgFrameIndex = (loadingTimer[0] / 64) % 4;
                double sx = bgFrameIndex * 1838.0;

                double scale = Math.max((double) WIDTH / 1838.0, (double) HEIGHT / 1079.0);
                double drawW = 1838.0 * scale;
                double drawH = 1079.0 * scale;
                double drawX = (WIDTH - drawW) / 2.0;
                double drawY = (HEIGHT - drawH) / 2.0;

                gc.drawImage(bgSheet, sx, 0, 1838.0, 1079.0, drawX, drawY, drawW, drawH);
                gc.restore();

                // Xử lý vẽ quả bóng Loading Ball và Hints
                double loadingAlpha = 1.0;
                if (loadingTimer[0] < 60) {
                    loadingAlpha = loadingTimer[0] / 60.0;
                } else if (loadingTimer[0] >= 540) {
                    loadingAlpha = Math.max(0.0, bgAlpha[0] / 0.3);
                }
                gc.setGlobalAlpha(loadingAlpha);

                double ballW = 80, ballH = 80, spacing = 20;
                double startX = WIDTH - 50 - (4 * ballW + 3 * spacing);
                double startY = HEIGHT - 50 - ballH;

                int t = loadingTimer[0];
                int b1 = (t < 60) ? (t / 15) : 4;
                int b2 = (t < 60) ? 0 : (t < 120) ? ((t - 60) / 15) : 4;
                int b3 = (t < 120) ? 0 : (t < 240) ? ((t - 120) / 30) : 4;
                int b4 = (t < 240) ? 0 : (t < 300) ? ((t - 240) / 30) : (t < 480) ? 2 : Math.min(4, 3 + (t - 480) / 15);

                int[] ballFrames = {b1, b2, b3, b4};

                for (int i = 0; i < 4; i++) {
                    double drawBallX = startX + i * (ballW + spacing);
                    double frameX = Math.min(4, ballFrames[i]) * 160.0;
                    gc.drawImage(loadingBallSheet, frameX, 0, 160, 160, drawBallX, startY, ballW, ballH);
                }

                gc.setFont(finalHintFont);
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                gc.setTextBaseline(javafx.geometry.VPos.CENTER);
                double textCenterX = (20 + (startX + 20)) / 2.0;
                double textCenterY = startY + ballH / 2.0;
                gc.fillText(selectedHint[0], textCenterX, textCenterY);
                gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
                gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
                
                gc.setGlobalAlpha(1.0);

                // Khi chạy xong 540 + x frame (đã tối đen) -> Hoàn tất Scene
                if (loadingTimer[0] > 540 && bgAlpha[0] <= 0) {
                    this.stop();
                    
                    // Xoá nền Map cũ của Scene trước khi đẩy lại vào Stage để tránh chớp nháy khung hình
                    if (App.this.gc != null) {
                        App.this.gc.clearRect(0, 0, WIDTH, HEIGHT);
                        App.this.gc.setFill(javafx.scene.paint.Color.BLACK);
                        App.this.gc.fillRect(0, 0, WIDTH, HEIGHT);
                    }

                    stage.setScene(nextScene);
                    onLoaded.run();
                }
            }
        };

        loadingLoop.start();
        stage.setScene(loadingScene);
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
