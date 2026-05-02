package com.hust.game.ui;

import com.hust.game.constants.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.Cursor;

import java.util.function.Consumer;

public class MenuScreen {

    // -------------------------------------------------------
    // CALLBACK — gọi khi người dùng bấm nút
    // -------------------------------------------------------
    private final Consumer<Void> onStart;
    private final Consumer<Void> onSettings;

    // -------------------------------------------------------
    // ANIMATION BACKGROUND
    // Sprite sheet 4 frame nằm ngang, mỗi frame 1838x1079
    // -------------------------------------------------------
    private static final int    BG_NUM_FRAMES  = 4;      // Tổng số frame sprite sheet
    private static final double BG_FRAME_W     = 1838.0; // Chiều rộng 1 frame gốc (7352 / 4)
    private static final double BG_FRAME_H     = 1079.0; // Chiều cao gốc sprite sheet
    private static final int    BG_ANIM_DELAY  = 64;     // Số frame game giữa mỗi lần đổi ảnh
    private static boolean hasPlayedIntro = false; //Chỉ false lần đầu tiên mở game, nếu đã từ sound button hay victory -> không load anim nữa
    // -------------------------------------------------------
    // PHASE SYSTEM
    // Phase 0: Chạy animation background, chưa hiện nút
    //          Thời gian = BG_ANIM_DELAY * BG_NUM_FRAMES (chạy đúng 1 vòng)
    // Phase 1: Hiện nút bình thường, background vẫn chạy animation
    // Phase 2: Người dùng bấm Play → background fade out dần
    //          Khi alpha xuống đủ thấp → chuyển scene
    // -------------------------------------------------------
    // Thời gian Phase 0: đủ để chạy đúng 1 vòng animation (4 frame x 64 delay)
    private static final int    PHASE0_DURATION = BG_ANIM_DELAY * BG_NUM_FRAMES;

    // Tốc độ fade out khi bấm Play (giảm bao nhiêu alpha mỗi frame)
    private static final double FADE_SPEED      = 0.02; // 1.0 / 0.02 = 50 frame = ~0.8 giây

    // Ngưỡng alpha để chuyển scene (gần như trong suốt hoàn toàn)
    private static final double FADE_THRESHOLD  = 0.05;

    public MenuScreen(Consumer<Void> onStart, Consumer<Void> onSettings) {
        this.onStart    = onStart;
        this.onSettings = onSettings;
    }

    public Scene createScene() {
        // -------------------------------------------------------
        // CANVAS — vẽ background animation
        // -------------------------------------------------------
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // -------------------------------------------------------
        // NÚT BẤM MAIN MENU — Sử dụng sprite sheet (Animation Frame)
        // -------------------------------------------------------
        Image buttonSheet  = loadImg("/assets/button.png");
        Image settingSheet = loadImg("/assets/setting.png");

        int[]    phase   = { hasPlayedIntro ? 1 : 0 };
        double[] bgAlpha = { 1.0 };

        String[] hints = {
            "Sử dụng WASD để di chuyển, J để tấn công, L để bật cuồng nộ!",
            "Sử dụng cuồng nộ có thể x2 sát thương đó!"
        };
        String[] selectedHint = { "" };
        int[] hintIndex = { 0 };
        int[] hintTimer = { 0 };

        // Nút Start
        StackPane startBtn = createSpriteBtn("START", buttonSheet, 3, 1.0, () -> {
            if (phase[0] == 1) {
                phase[0] = 2; // Kích hoạt fade out
                hintIndex[0] = (int) (Math.random() * hints.length);
                selectedHint[0] = hints[hintIndex[0]];
            }
        });

        // Nút Exit
        StackPane exitBtn = createSpriteBtn("EXIT", buttonSheet, 3, 1.0, Platform::exit);

        // Nút Settings (Thu nhỏ thêm 10%, scale = 0.65)
        StackPane settingsBtn = createSpriteBtn(null, settingSheet, 3, 0.65, () -> onSettings.accept(null));

        // Bố cục: Setting nằm bên phải Exit, khoảng cách 5px
        HBox bottomRow = new HBox(5, exitBtn, settingsBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Bố cục: Exit nằm dưới Start (cùng x do canh trái), khoảng cách 5px
        VBox btnBox = new VBox(5, startBtn, bottomRow);
        btnBox.setAlignment(Pos.BOTTOM_LEFT);
        
        btnBox.setOpacity(hasPlayedIntro ? 1.0 : 0.0);
        btnBox.setMouseTransparent(!hasPlayedIntro); // Fix lỗi: chưa hiện nút đã bấm được

        // -------------------------------------------------------
        // STACKPANE — canvas phía dưới, nút phía trên
        // StackPane.setAlignment đặt btnBox dính góc dưới trái
        // -------------------------------------------------------
        StackPane root = new StackPane(canvas, btnBox);
        StackPane.setAlignment(btnBox, Pos.BOTTOM_LEFT);

        // Padding: cách mép dưới cao hơn một chút (60px), cách mép trái 70px
        StackPane.setMargin(btnBox, new Insets(0, 0, 20, 70));

        root.setStyle("-fx-background-color: black;"); // Nền đen khi background chưa load

        // Thêm sự kiện click chuột để đổi hint khi đang ở màn hình loading
        root.setOnMouseClicked(e -> {
            if (phase[0] == 2) {
                hintTimer[0] = 0; // Reset lại thời gian chờ
                hintIndex[0] = (hintIndex[0] + 1) % hints.length;
                selectedHint[0] = hints[hintIndex[0]];
            }
        });

        // -------------------------------------------------------
        // LOAD SPRITE SHEET BACKGROUND
        // Đặt file vào: src/main/resources/assets/menu_bg.png
        // -------------------------------------------------------
        Image bgSheet = loadImg("/assets/menu_background.png");
        Image loadingBallSheet = loadImg("/assets/loading_ball.png");

        Font hintFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 28);
        if (hintFont == null) {
            hintFont = Font.font("Arial", FontWeight.BOLD, 28);
        }
        final Font finalHintFont = hintFont;

        // -------------------------------------------------------
        // BIẾN TRẠNG THÁI ANIMATION
        // -------------------------------------------------------
        int[] bgFrameIndex = { 0 }; // Frame hiện tại của background (0 → 3)
        int[] bgAnimTimer  = { 0 }; // Đếm frame game để biết lúc nào đổi ảnh
        int[] phaseTimer   = { 0 }; // Đếm tổng frame đã trôi qua trong phase hiện tại
        int[] loadingTimer = { 0 }; // Đếm thời gian cho animation loading

        // -------------------------------------------------------
        // ANIMATION TIMER — "game loop" của menu, chạy mỗi frame (~60fps)
        // Dùng mảng holder để lambda bên trong có thể gọi stop()
        // -------------------------------------------------------
        AnimationTimer[] menuLoopHolder = { null };

        AnimationTimer menuLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                // --- Xóa canvas và vẽ nền đen trước ---
                // Nền đen này sẽ lộ ra khi background fade out → không bị flash trắng
                gc.clearRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

                // --- Cập nhật frame animation background ---
                // Chạy liên tục ở mọi phase để background không bị đứng hình
                bgAnimTimer[0]++;
                if (bgAnimTimer[0] >= BG_ANIM_DELAY) {
                    bgAnimTimer[0] = 0;
                    // Vòng lặp tuần hoàn: 0 → 1 → 2 → 3 → 0 → ...
                    bgFrameIndex[0] = (bgFrameIndex[0] + 1) % BG_NUM_FRAMES;
                }

                // --- Xử lý logic từng phase ---
                phaseTimer[0]++;

                if (phase[0] == 0) {
                    // PHASE 0: Background chạy animation đủ 1 vòng, nút chưa hiện
                    bgAlpha[0] = 1.0; // Background rõ nét hoàn toàn

                    if (phaseTimer[0] >= PHASE0_DURATION) {
                        phase[0] = 1;      // Chuyển Phase 1
                        phaseTimer[0] = 0; // Reset bộ đếm
                        btnBox.setOpacity(1.0); // Hiện nút ngay lập tức (không fade in)
                        btnBox.setMouseTransparent(false); // Cho phép thao tác
                        hasPlayedIntro = true; // Đánh dấu đã xem intro rồi, lần sau bỏ qua
                    }

                } else if (phase[0] == 1) {
                    // PHASE 1: Menu bình thường, nút hiển thị, background vẫn chạy
                    bgAlpha[0] = 1.0; // Giữ background rõ nét, chờ người dùng bấm

                } else if (phase[0] == 2) {
                    // PHASE 2: Bấm Play → nút biến mất, fade out background, chạy loading
                    btnBox.setVisible(false); // Nút biến mất ngay lập tức
                    
                    loadingTimer[0]++;
                    hintTimer[0]++;

                    // Tự động chuyển dòng hint tiếp theo sau mỗi 5s (300 frames ở 60 FPS)
                    if (hintTimer[0] >= 300) {
                        hintTimer[0] = 0; // Reset timer
                        hintIndex[0] = (hintIndex[0] + 1) % hints.length;
                        selectedHint[0] = hints[hintIndex[0]];
                    }

                    if (loadingTimer[0] < 540) {
                        // Đang load bóng: giảm độ sáng về 0.3 giống Setting để vẫn nhìn thấy mờ mờ
                        if (bgAlpha[0] > 0.3) {
                            bgAlpha[0] -= FADE_SPEED;
                            if (bgAlpha[0] < 0.3) bgAlpha[0] = 0.3;
                        }
                    } else {
                        // Chờ nửa giây sau khi bóng load xong (510 + 30 = 540) -> Tối hẳn về đen
                        bgAlpha[0] -= FADE_SPEED;
                        if (bgAlpha[0] <= 0) {
                            bgAlpha[0] = 0;
                            menuLoopHolder[0].stop(); // Dừng loop menu trước
                            onStart.accept(null);      // Gọi callback chuyển scene
                            return;                    // Thoát handle() để không vẽ thêm
                        }
                    }
                }

                // --- Vẽ background frame hiện tại lên canvas ---
                gc.save();
                gc.setGlobalAlpha(Math.max(0.0, bgAlpha[0])); // Đảm bảo alpha không âm

                // sx: tọa độ x bắt đầu cắt trong sprite sheet
                // Dịch sang phải theo frameIndex để lấy đúng frame
                double sx = bgFrameIndex[0] * BG_FRAME_W;

                // Tính toán tỷ lệ để giữ nguyên Aspect Ratio (kiểu Cover màn hình)
                double scale = Math.max((double) GameConstants.WINDOW_WIDTH / BG_FRAME_W, (double) GameConstants.WINDOW_HEIGHT / BG_FRAME_H);
                double drawW = BG_FRAME_W * scale;
                double drawH = BG_FRAME_H * scale;
                double drawX = (GameConstants.WINDOW_WIDTH - drawW) / 2.0;
                double drawY = (GameConstants.WINDOW_HEIGHT - drawH) / 2.0;

                gc.drawImage(
                    bgSheet,
                    sx, 0,                       // Nguồn: bắt đầu cắt từ (sx, 0)
                    BG_FRAME_W, BG_FRAME_H,      // Nguồn: kích thước 1 frame gốc
                    drawX, drawY,                // Đích: vẽ căn giữa màn hình
                    drawW, drawH                 // Đích: kích thước đã scale đúng tỷ lệ
                );

                gc.restore(); // Khôi phục globalAlpha về 1.0 để không ảnh hưởng render khác
                
                // --- Vẽ 4 quả bóng Loading khi vào Phase 2 ---
                if (phase[0] == 2) {
                // Fade bóng loading đi cùng background lúc chuyển sang đen tuyền
                if (loadingTimer[0] >= 540) {
                    gc.setGlobalAlpha(Math.max(0.0, bgAlpha[0] / 0.3));
                } else {
                    gc.setGlobalAlpha(1.0);
                }

                    double ballW = 80;
                    double ballH = 80;
                    double spacing = 20;
                    double startX = GameConstants.WINDOW_WIDTH - 50 - (4 * ballW + 3 * spacing);
                    double startY = GameConstants.WINDOW_HEIGHT - 50 - ballH;
                    
                    int t = loadingTimer[0];
                    
                    // Giả lập Game Tải:
                    // Quả 1 & 2 chạy tuần tự trong 2s (mỗi quả 1s ~ 60 frames)
                    int b1 = (t < 60) ? (t / 15) : 4;
                    int b2 = (t < 60) ? 0 : (t < 120) ? ((t - 60) / 15) : 4;
                    // Quả 3 xong trong 2s (60 * 2 = 120 frames)
                    int b3 = (t < 120) ? 0 : (t < 240) ? ((t - 120) / 30) : 4;
                    // Quả 4 tới frame 3 (index 2) trong 1s, nghỉ 3s, chạy hết trong 0.5s
                    int b4 = (t < 240) ? 0 : (t < 300) ? ((t - 240) / 30) : (t < 480) ? 2 : Math.min(4, 3 + (t - 480) / 15);
                    
                    int[] ballFrames = {b1, b2, b3, b4};
                    
                    for (int i = 0; i < 4; i++) {
                        double drawBallX = startX + i * (ballW + spacing);
                        double frameX = Math.min(4, ballFrames[i]) * 160.0; // kích thước 1 frame = 800/5 = 160
                        gc.drawImage(loadingBallSheet, frameX, 0, 160, 160, drawBallX, startY, ballW, ballH);
                    }

                    // Vẽ Text Hướng dẫn/Mẹo
                    gc.setFont(finalHintFont);
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                    gc.setTextBaseline(javafx.geometry.VPos.CENTER);
                    double textCenterX = (20 + (startX + 20)) / 2.0;
                    double textCenterY = startY + ballH / 2.0; // Căn dọc ngang hàng với quả bóng
                    gc.fillText(selectedHint[0], textCenterX, textCenterY);
                    gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
                    gc.setTextBaseline(javafx.geometry.VPos.BASELINE);

                gc.setGlobalAlpha(1.0); // Reset alpha
                }
            }
        };

        menuLoopHolder[0] = menuLoop; // Gán vào holder để lambda stop() được
        menuLoop.start();

        // Dừng loop khi scene bị thay thế (tránh memory leak)
        Scene scene = new Scene(root, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin == null) menuLoop.stop();
        });

        return scene;
    }

    // -------------------------------------------------------
    // HELPER: Tạo Nút tùy chỉnh với Sprite Animation và Text
    // -------------------------------------------------------
    private StackPane createSpriteBtn(String btnText, Image spriteSheet, int numFrames, double scaleMultiplier, Runnable action) {
        StackPane pane = new StackPane();
        
        // Kích thước 1 frame
        double frameW = spriteSheet.getWidth() / numFrames;
        double frameH = spriteSheet.getHeight();
        
        ImageView view = new ImageView(spriteSheet);
        view.setViewport(new Rectangle2D(0, 0, frameW, frameH));
        
        view.setFitWidth(frameW * scaleMultiplier);
        view.setFitHeight(frameH * scaleMultiplier);

        // Trói chặt khu vực nhận diện chuột (Hitbox) vừa khít với ảnh
        pane.setPrefSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        pane.setMaxSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        
        final Text textNode = (btnText != null) ? new Text(btnText) : null;
        
        if (textNode != null) {
            Font pixelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 56);
            if (pixelFont == null) {
                pixelFont = Font.font("Arial", FontWeight.BOLD, 56); // fallback nếu ko tìm thấy
            }
            textNode.setFont(pixelFont);
            textNode.setFill(Color.RED);
            textNode.setStroke(Color.BLACK);
            textNode.setStrokeWidth(3.0); // Tăng lại độ dày viền
        }
        
        if (textNode != null) {
            pane.getChildren().addAll(view, textNode); // Text nằm đè lên hình ảnh
        } else {
            pane.getChildren().add(view);
        }
        
        // Biến trạng thái Animation
        int[] currentFrame = {0};
        Timeline[] timeline = {null};
        
        pane.setOnMouseEntered(e -> {
            com.hust.game.audio.SoundManager.playButtonHoverSound();
            pane.setScaleX(1.10);
            pane.setScaleY(1.10);
            if (textNode != null) {
                textNode.setFill(Color.ORANGE);
                textNode.setStroke(Color.WHITE);
            }
            
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = numFrames - 1 - currentFrame[0]; // Chỉ chạy tiếp số frame còn lại
            if (framesToAnimate > 0) {
                timeline[0] = new Timeline(new KeyFrame(Duration.millis(20), evt -> {
                    currentFrame[0]++;
                    view.setViewport(new Rectangle2D(currentFrame[0] * frameW, 0, frameW, frameH));
                }));
                timeline[0].setCycleCount(framesToAnimate);
                timeline[0].play();
            }
        });
        
        pane.setOnMouseExited(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
            if (textNode != null) {
                textNode.setFill(Color.RED);
                textNode.setStroke(Color.BLACK);
            }
            
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = currentFrame[0];
            if (framesToAnimate > 0) { // Chạy animation giật lùi về frame 0
                timeline[0] = new Timeline(new KeyFrame(Duration.millis(100), evt -> {
                    currentFrame[0]--;
                    view.setViewport(new Rectangle2D(currentFrame[0] * frameW, 0, frameW, frameH));
                }));
                timeline[0].setCycleCount(framesToAnimate);
                timeline[0].play();
            }
        });
        
        // Thu nhỏ còn 0.9 khi nhấn
        pane.setOnMousePressed(e -> {
            com.hust.game.audio.SoundManager.playButtonClickSound();
            pane.setScaleX(0.9);
            pane.setScaleY(0.9);
        });
        
        // Trả về kích thước x1.0 khi nhả chuột
        pane.setOnMouseReleased(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
        });
        
        // Gọi action khi click
        pane.setOnMouseClicked(e -> action.run());
        pane.setCursor(Cursor.HAND);
        
        return pane;
    }

    // -------------------------------------------------------
    // HELPER: Load ảnh từ resources, ném lỗi rõ ràng nếu thiếu file
    // -------------------------------------------------------
    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}