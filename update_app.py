import sys, re

path = 'd:/Project/dungeon_slayer/src/main/java/com/hust/game/main/App.java'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

# 1. Add variables
vars_str = """
    private int realLoadingTimer = 0;
    private int realLoadingHintTimer = 0;
    private int realLoadingHintIndex = 0;
    private boolean isLoadingPhase = false;
    private String[] realLoadingHints = {
        "Sử dụng WASD để di chuyển, J để tấn công, L để bật cuồng nộ!",
        "Sử dụng cuồng nộ có thể x2 sát thương đó!",
        "Phát triển bởi nhóm 27 Ộ Ô Pi!",
        "Slime là sinh vật chạm vào thôi là mất máu!",
        "Cái cây có tầm đánh y như player! Cẩn thận!",
        "Quyền năng hắc ám của phù thủy có thể triệu hồi ra hiệp sĩ!"
    };
    private Image loadingBgSheet;
    private Image loadingBallSheet;
    private javafx.scene.text.Font loadingHintFont;
    private int nextLevelToLoadMusic = -1;
    
    private void startLoadingPhase(int level) {
        if (DevSettings.shouldSkipLoadingScreens()) {
            playInGameMusic(level);
            isLoadingPhase = false;
            fadeInTimer = 120;
            return;
        }
        isLoadingPhase = true;
        realLoadingTimer = 0;
        realLoadingHintTimer = 0;
        realLoadingHintIndex = (int) (Math.random() * realLoadingHints.length);
        nextLevelToLoadMusic = level;
        
        if (loadingBgSheet == null) {
            loadingBgSheet = loadImg("/assets/menu_background.png");
            loadingBallSheet = loadImg("/assets/loading_ball.png");
            loadingHintFont = javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 28);
            if (loadingHintFont == null) {
                loadingHintFont = javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 28);
            }
        }
    }
"""

c = c.replace('private int fadeInTimer = 0; // Bộ đếm thời gian fade-in khi vào màn (120 frame = 2s)', 'private int fadeInTimer = 0; // Bộ đếm thời gian fade-in khi vào màn (120 frame = 2s)\n' + vars_str)

# 2. update createGameScene music call
c = c.replace('playInGameMusic(startLevel);', 'startLoadingPhase(startLevel);')

# 3. Handle inputs
c = c.replace('if (fadeInTimer > 0) return; // Khoá input khi đang chuyển cảnh', 'if (isLoadingPhase || fadeInTimer > 0) return; // Khoá input khi đang chuyển cảnh')

# mouse pressed logic
old_mouse = '''        scene.setOnMousePressed(e -> {
            if (fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = true;
        });
        scene.setOnMouseReleased(e -> {
            if (fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = false;
        });'''
new_mouse = '''        scene.setOnMousePressed(e -> {
            if (isLoadingPhase) {
                realLoadingHintTimer = 0;
                realLoadingHintIndex = (realLoadingHintIndex + 1) % realLoadingHints.length;
                return;
            }
            if (fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = true;
        });
        scene.setOnMouseReleased(e -> {
            if (isLoadingPhase || fadeInTimer > 0) return;
            if (!isPaused) isMousePressed = false;
        });'''
c = c.replace(old_mouse, new_mouse)

# 4. game loop pause logic
old_gameloop = '''                if (fadeInTimer > 0) {
                    fadeInTimer--;
                    updateCamera(); // Chỉ tính toán camera theo người chơi, bỏ qua logic game
                    pauseBtn.setMouseTransparent(true); // Khóa tương tác nút Pause
                } else if (!isVictory && !isGameOver && !isPaused && !isMinimapOpen) {'''
new_gameloop = '''                if (isLoadingPhase) {
                    updateCamera(); // Chỉ tính toán camera theo người chơi, bỏ qua logic game
                    pauseBtn.setMouseTransparent(true); // Khóa tương tác nút Pause
                } else if (fadeInTimer > 0) {
                    fadeInTimer--;
                    updateCamera(); // Chỉ tính toán camera theo người chơi, bỏ qua logic game
                    pauseBtn.setMouseTransparent(true); // Khóa tương tác nút Pause
                } else if (!isVictory && !isGameOver && !isPaused && !isMinimapOpen) {'''
c = c.replace(old_gameloop, new_gameloop)

# 5. render loading overlay
old_fadein = '''                // --- VẼ HIỆU ỨNG FADE-IN TỪ ĐEN SANG GAME ---
                if (fadeInTimer > 0) {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.setGlobalAlpha((double) fadeInTimer / 120.0);
                    gc.fillRect(0, 0, WIDTH, HEIGHT);
                    gc.setGlobalAlpha(1.0);
                }'''
new_loading = '''                // --- VẼ HIỆU ỨNG FADE-IN TỪ ĐEN SANG GAME ---
                if (isLoadingPhase) {
                    realLoadingTimer++;
                    realLoadingHintTimer++;
                    if (realLoadingHintTimer >= 300) {
                        realLoadingHintTimer = 0;
                        realLoadingHintIndex = (realLoadingHintIndex + 1) % realLoadingHints.length;
                    }

                    double currentBlackAlpha = 1.0;
                    double currentBgAlpha = 0.3;
                    double currentLoadingAlpha = 1.0;

                    if (realLoadingTimer < 60) {
                        currentLoadingAlpha = realLoadingTimer / 60.0;
                    }

                    if (realLoadingTimer > 540) {
                        currentBlackAlpha = Math.max(0, 1.0 - (realLoadingTimer - 540) * 0.02);
                        currentBgAlpha = Math.max(0, 0.3 - (realLoadingTimer - 540) * 0.006);
                        currentLoadingAlpha = Math.max(0, currentBlackAlpha); // fade out together
                    }

                    // Fill black to cover game
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.setGlobalAlpha(currentBlackAlpha);
                    gc.fillRect(0, 0, WIDTH, HEIGHT);

                    // Draw background faint
                    if (currentBgAlpha > 0 && loadingBgSheet != null) {
                        gc.save();
                        gc.setGlobalAlpha(currentBgAlpha);
                        int bgFrameIndex = (realLoadingTimer / 64) % 4;
                        double sx = bgFrameIndex * 1838.0;
                        double scale = Math.max((double) GameConstants.WINDOW_WIDTH / 1838.0, (double) GameConstants.WINDOW_HEIGHT / 1079.0);
                        double drawW = 1838.0 * scale;
                        double drawH = 1079.0 * scale;
                        double drawX = (GameConstants.WINDOW_WIDTH - drawW) / 2.0;
                        double drawY = (GameConstants.WINDOW_HEIGHT - drawH) / 2.0;
                        gc.drawImage(loadingBgSheet, sx, 0, 1838.0, 1079.0, drawX, drawY, drawW, drawH);
                        gc.restore();
                    }

                    // Draw loading balls & hint
                    if (currentLoadingAlpha > 0 && loadingBallSheet != null) {
                        gc.setGlobalAlpha(currentLoadingAlpha);
                        double ballW = 80, ballH = 80, spacing = 20;
                        double startX = GameConstants.WINDOW_WIDTH - 50 - (4 * ballW + 3 * spacing);
                        double startY = GameConstants.WINDOW_HEIGHT - 50 - ballH;

                        int t = realLoadingTimer;
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

                        if (loadingHintFont != null) {
                            gc.setFont(loadingHintFont);
                            gc.setFill(javafx.scene.paint.Color.WHITE);
                            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                            gc.setTextBaseline(javafx.geometry.VPos.CENTER);
                            double textCenterX = (20 + (startX + 20)) / 2.0;
                            double textCenterY = startY + ballH / 2.0;
                            gc.fillText(realLoadingHints[realLoadingHintIndex], textCenterX, textCenterY);
                            gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
                            gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
                        }
                    }

                    gc.setGlobalAlpha(1.0);

                    // Stop loading phase
                    if (realLoadingTimer > 540 && currentBlackAlpha <= 0) {
                        isLoadingPhase = false;
                        playInGameMusic(nextLevelToLoadMusic);
                    }
                } else if (fadeInTimer > 0) {
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.setGlobalAlpha((double) fadeInTimer / 120.0);
                    gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
                    gc.setGlobalAlpha(1.0);
                }'''
c = c.replace(old_fadein, new_loading)

# 6. Replace usages of showLoadingScreen
# In Tutorial complete screen
old_tut_screen = '''                                showLoadingScreen(stage, scene, () -> {
                                    gameManager.loadLevel(1);
                                    collisionChecker = new CollisionChecker(gameManager.getMap());
                                    combatManager.setCollisionChecker(collisionChecker);
                                    combatManager.setCurrentLevelIndex(gameManager.getCurrentLevelIndex());
                                    enemyManager.setCollisionChecker(collisionChecker);
                                    combatManager.resetSkill();
                                    if (allyManager != null) allyManager.reset();
                                    input.clear(); // Chống kẹt nút
                                    fadeInTimer = 120;
                                updateCameraZoom(1);
                                    playInGameMusic(1);
                                    gameLoop.start();
                                });'''
new_tut_screen = '''                                gameManager.loadLevel(1);
                                collisionChecker = new CollisionChecker(gameManager.getMap());
                                combatManager.setCollisionChecker(collisionChecker);
                                combatManager.setCurrentLevelIndex(gameManager.getCurrentLevelIndex());
                                enemyManager.setCollisionChecker(collisionChecker);
                                combatManager.resetSkill();
                                if (allyManager != null) allyManager.reset();
                                input.clear(); // Chống kẹt nút
                                updateCameraZoom(1);
                                startLoadingPhase(1);
                                gameLoop.start();'''
c = c.replace(old_tut_screen, new_tut_screen)

# In Level clear screen
old_lvl_screen = '''                                showLoadingScreen(stage, scene, () -> {
                                    gameManager.loadNextLevel();
                                    collisionChecker = new CollisionChecker(gameManager.getMap());
                                    combatManager.setCollisionChecker(collisionChecker);
                                    combatManager.setCurrentLevelIndex(gameManager.getCurrentLevelIndex());
                                    enemyManager.setCollisionChecker(collisionChecker);
                                    combatManager.resetSkill();
                                    if (allyManager != null) allyManager.reset();
                                    input.clear(); // Xóa các phím đang đè để tránh kẹt nút
                                    fadeInTimer = 120; // Kích hoạt lại hiệu ứng mờ dần sáng lên khi vào màn mới
                                updateCameraZoom(gameManager.getCurrentLevelIndex());
                                    playInGameMusic(gameManager.getCurrentLevelIndex());
                                    gameLoop.start();
                                });'''
new_lvl_screen = '''                                gameManager.loadNextLevel();
                                collisionChecker = new CollisionChecker(gameManager.getMap());
                                combatManager.setCollisionChecker(collisionChecker);
                                combatManager.setCurrentLevelIndex(gameManager.getCurrentLevelIndex());
                                enemyManager.setCollisionChecker(collisionChecker);
                                combatManager.resetSkill();
                                if (allyManager != null) allyManager.reset();
                                input.clear(); // Xóa các phím đang đè để tránh kẹt nút
                                updateCameraZoom(gameManager.getCurrentLevelIndex());
                                startLoadingPhase(gameManager.getCurrentLevelIndex());
                                gameLoop.start();'''
c = c.replace(old_lvl_screen, new_lvl_screen)

# Remove showLoadingScreen definition to clean up
c = re.sub(r'private void showLoadingScreen\(Stage stage, Scene nextScene, Runnable onLoaded\) \{.*?\n    \}\n\n    private Image loadImg\(String path\) \{', '    private Image loadImg(String path) {', c, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
