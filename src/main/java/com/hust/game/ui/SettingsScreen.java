package com.hust.game.ui;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.function.Consumer;

public class SettingsScreen {

    private final Consumer<Void> onBack;

    // -------------------------------------------------------
    // BACKGROUND — dùng lại sprite sheet của menu
    // -------------------------------------------------------
    private static final int    BG_NUM_FRAMES = 4;
    private static final double BG_FRAME_W    = 1838.0;
    private static final double BG_FRAME_H    = 1079.0;
    private static final int    BG_ANIM_DELAY = 64;

    // Độ mờ cố định của background (0.0 = đen hoàn toàn, 1.0 = rõ hoàn toàn)
    private static final double BG_DIM_ALPHA  = 0.3;

    // -------------------------------------------------------
    // SOUND LEVEL — bước nhảy 10, từ 0 đến 100
    // -------------------------------------------------------
    private int bgmLevel;
    private int sfxLevel;

    public SettingsScreen(Consumer<Void> onBack) {
        this.onBack = onBack;
        // Đồng bộ mức âm lượng hiện tại khi mở cài đặt
        this.bgmLevel = (int) Math.round(SoundManager.getBgmVolume() * 100);
        this.sfxLevel = (int) Math.round(SoundManager.getSfxVolume() * 100);
    }

    public Scene createScene() {
        // -------------------------------------------------------
        // CANVAS — vẽ background mờ phía dưới
        // -------------------------------------------------------
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // -------------------------------------------------------
        // LOAD ẢNH
        // -------------------------------------------------------
        Image bgSheet    = loadImg("/assets/menu_background.png");
        Image buttonSheet = loadImg("/assets/button.png"); // Dùng sprite sheet mới thay cho menu.png cũ

        // -------------------------------------------------------
        // TITLE
        // -------------------------------------------------------
        javafx.scene.text.Text title = new javafx.scene.text.Text("SETTINGS");
        Font titleFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 80);
        if (titleFont == null) {
            titleFont = Font.font("Arial", FontWeight.BOLD, 80);
        }
        title.setFont(titleFont);
        title.setFill(Color.WHITE);
        title.setStroke(Color.BLACK);
        title.setStrokeWidth(2.0);

        Font labelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 36);
        if (labelFont == null) {
            labelFont = Font.font("Arial", FontWeight.BOLD, 36);
        }

        // -------------------------------------------------------
        // BACKGROUND MUSIC ROW (NHẠC NỀN)
        // -------------------------------------------------------
        javafx.scene.text.Text bgmLabel = new javafx.scene.text.Text("Nhạc nền");
        bgmLabel.setFont(labelFont);
        bgmLabel.setFill(Color.WHITE);
        bgmLabel.setStroke(Color.BLACK);
        bgmLabel.setStrokeWidth(1.5);

        StackPane bgmBtn = createSpriteBtn(String.valueOf(bgmLevel), buttonSheet, 3, 0.8, () -> {});

        StackPane bgmMinusBtn = createSpriteBtn("-", buttonSheet, 3, 0.45, () -> {
            if (bgmLevel > 0) {
                bgmLevel -= 10;
                ((Text) bgmBtn.getChildren().get(1)).setText(String.valueOf(bgmLevel));
                SoundManager.setBgmVolume(bgmLevel / 100.0);
                com.hust.game.main.App.updateBgmVolume(bgmLevel / 100.0);
            }
        });

        StackPane bgmPlusBtn = createSpriteBtn("+", buttonSheet, 3, 0.45, () -> {
            if (bgmLevel < 100) {
                bgmLevel += 10;
                ((Text) bgmBtn.getChildren().get(1)).setText(String.valueOf(bgmLevel));
                SoundManager.setBgmVolume(bgmLevel / 100.0);
                com.hust.game.main.App.updateBgmVolume(bgmLevel / 100.0);
            }
        });

        HBox bgmRow = new HBox(10, bgmMinusBtn, bgmBtn, bgmPlusBtn);
        bgmRow.setAlignment(Pos.CENTER);
        VBox bgmBox = new VBox(5, bgmLabel, bgmRow);
        bgmBox.setAlignment(Pos.CENTER);

        // -------------------------------------------------------
        // SFX ROW (HIỆU ỨNG)
        // -------------------------------------------------------
        javafx.scene.text.Text sfxLabel = new javafx.scene.text.Text("Hiệu ứng");
        sfxLabel.setFont(labelFont);
        sfxLabel.setFill(Color.WHITE);
        sfxLabel.setStroke(Color.BLACK);
        sfxLabel.setStrokeWidth(1.5);

        StackPane sfxBtn = createSpriteBtn(String.valueOf(sfxLevel), buttonSheet, 3, 0.8, () -> {});

        StackPane sfxMinusBtn = createSpriteBtn("-", buttonSheet, 3, 0.45, () -> {
            if (sfxLevel > 0) {
                sfxLevel -= 10;
                ((Text) sfxBtn.getChildren().get(1)).setText(String.valueOf(sfxLevel));
                SoundManager.setSfxVolume(sfxLevel / 100.0);
            }
        });

        StackPane sfxPlusBtn = createSpriteBtn("+", buttonSheet, 3, 0.45, () -> {
            if (sfxLevel < 100) {
                sfxLevel += 10;
                ((Text) sfxBtn.getChildren().get(1)).setText(String.valueOf(sfxLevel));
                SoundManager.setSfxVolume(sfxLevel / 100.0);
            }
        });

        HBox sfxRow = new HBox(10, sfxMinusBtn, sfxBtn, sfxPlusBtn);
        sfxRow.setAlignment(Pos.CENTER);
        VBox sfxBox = new VBox(5, sfxLabel, sfxRow);
        sfxBox.setAlignment(Pos.CENTER);

        // -------------------------------------------------------
        // NÚT BACK VỀ MENU
        // -------------------------------------------------------
        StackPane menuBtn = createSpriteBtn("BACK", buttonSheet, 3, 0.8, () -> onBack.accept(null));

        // -------------------------------------------------------
        // LAYOUT TỔNG — Title → Sound Row → Back, canh giữa màn hình
        // -------------------------------------------------------
        VBox uiBox = new VBox(20, title, bgmBox, sfxBox, menuBtn);
        uiBox.setAlignment(Pos.CENTER);

        // -------------------------------------------------------
        // STACKPANE — canvas background mờ bên dưới, UI bên trên
        // -------------------------------------------------------
        StackPane root = new StackPane(canvas, uiBox);
        root.setStyle("-fx-background-color: black;");

        // -------------------------------------------------------
        // BIẾN ANIMATION BACKGROUND
        // -------------------------------------------------------
        int[] bgFrameIndex = { 0 }; // Frame hiện tại (0 → 3)
        int[] bgAnimTimer  = { 0 }; // Đếm frame để đổi ảnh

        // -------------------------------------------------------
        // ANIMATION TIMER — chạy background mờ liên tục
        // -------------------------------------------------------
        AnimationTimer bgLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Xóa canvas và vẽ nền đen trước
                gc.clearRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

                // Cập nhật frame animation background
                bgAnimTimer[0]++;
                if (bgAnimTimer[0] >= BG_ANIM_DELAY) {
                    bgAnimTimer[0] = 0;
                    // Vòng lặp tuần hoàn: 0 → 1 → 2 → 3 → 0
                    bgFrameIndex[0] = (bgFrameIndex[0] + 1) % BG_NUM_FRAMES;
                }

                // Vẽ background với alpha cố định BG_DIM_ALPHA
                gc.save();
                gc.setGlobalAlpha(BG_DIM_ALPHA);

                // Cắt đúng frame từ sprite sheet bằng cách dịch sx
                double sx = bgFrameIndex[0] * BG_FRAME_W;

                // Tính toán tỷ lệ để giữ nguyên Aspect Ratio (kiểu Cover màn hình)
                double scale = Math.max((double) GameConstants.WINDOW_WIDTH / BG_FRAME_W, (double) GameConstants.WINDOW_HEIGHT / BG_FRAME_H);
                double drawW = BG_FRAME_W * scale;
                double drawH = BG_FRAME_H * scale;
                double drawX = (GameConstants.WINDOW_WIDTH - drawW) / 2.0;
                double drawY = (GameConstants.WINDOW_HEIGHT - drawH) / 2.0;

                gc.drawImage(
                    bgSheet,
                    sx, 0,                       // Nguồn: cắt từ (sx, 0)
                    BG_FRAME_W, BG_FRAME_H,      // Nguồn: kích thước 1 frame gốc
                    drawX, drawY,                // Đích: căn giữa
                    drawW, drawH                 // Đích: kích thước giữ tỷ lệ
                );

                gc.restore(); // Khôi phục globalAlpha về 1.0
            }
        };

        bgLoop.start();

        // Dừng loop khi scene bị thay thế (tránh memory leak)
        Scene scene = new Scene(root, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin == null) bgLoop.stop();
        });

        return scene;
    }

    // -------------------------------------------------------
    // HELPER: Tạo Nút tùy chỉnh với Sprite Animation và Text
    // -------------------------------------------------------
    private StackPane createSpriteBtn(String btnText, Image spriteSheet, int numFrames, double scaleMultiplier, Runnable action) {
        StackPane pane = new StackPane();
        
        double frameW = spriteSheet.getWidth() / numFrames;
        double frameH = spriteSheet.getHeight();
        
        ImageView view = new ImageView(spriteSheet);
        view.setViewport(new Rectangle2D(0, 0, frameW, frameH));
        
        view.setFitWidth(frameW * scaleMultiplier);
        view.setFitHeight(frameH * scaleMultiplier);

        pane.setPrefSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        pane.setMaxSize(frameW * scaleMultiplier, frameH * scaleMultiplier);
        
        final Text textNode = (btnText != null) ? new Text(btnText) : null;
        
        if (textNode != null) {
            if ("+".equals(btnText) || "-".equals(btnText)) {
                textNode.setFont(Font.font("Consolas", FontWeight.BOLD, 60));
                textNode.setFill(Color.BLACK);
                textNode.setStroke(Color.WHITE);
                textNode.setStrokeWidth(2.0);
            } else {
                Font pixelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 46);
                if (pixelFont == null) {
                    pixelFont = Font.font("Arial", FontWeight.BOLD, 46);
                }
                textNode.setFont(pixelFont);
                textNode.setFill(Color.RED);
                textNode.setStroke(Color.BLACK);
                textNode.setStrokeWidth(2.5);
            }
        }
        
        if (textNode != null) {
            pane.getChildren().addAll(view, textNode);
        } else {
            pane.getChildren().add(view);
        }
        
        int[] currentFrame = {0};
        Timeline[] timeline = {null};
        
        pane.setOnMouseEntered(e -> {
            com.hust.game.audio.SoundManager.playButtonHoverSound();
            pane.setScaleX(1.10);
            pane.setScaleY(1.10);
            if (textNode != null) {
                if ("+".equals(btnText) || "-".equals(btnText)) {
                    textNode.setFill(Color.DARKGRAY);
                    textNode.setStroke(Color.WHITE);
                } else {
                    textNode.setFill(Color.ORANGE);
                    textNode.setStroke(Color.WHITE);
                }
            }
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = numFrames - 1 - currentFrame[0];
            if (framesToAnimate > 0) {
                timeline[0] = new Timeline(new KeyFrame(Duration.millis(30), evt -> {
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
                if ("+".equals(btnText) || "-".equals(btnText)) {
                    textNode.setFill(Color.BLACK);
                    textNode.setStroke(Color.WHITE);
                } else {
                    textNode.setFill(Color.RED);
                    textNode.setStroke(Color.BLACK);
                }
            }
            if (timeline[0] != null) timeline[0].stop();
            int framesToAnimate = currentFrame[0];
            if (framesToAnimate > 0) {
                timeline[0] = new Timeline(new KeyFrame(Duration.millis(50), evt -> {
                    currentFrame[0]--;
                    view.setViewport(new Rectangle2D(currentFrame[0] * frameW, 0, frameW, frameH));
                }));
                timeline[0].setCycleCount(framesToAnimate);
                timeline[0].play();
            }
        });
        
        pane.setOnMousePressed(e -> {
            com.hust.game.audio.SoundManager.playButtonClickSound();
            pane.setScaleX(0.9);
            pane.setScaleY(0.9);
        });
        pane.setOnMouseReleased(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
        });
        
        pane.setOnMouseClicked(e -> action.run());
        pane.setCursor(Cursor.HAND);
        
        return pane;
    }

    // -------------------------------------------------------
    // HELPER: Load ảnh từ resources
    // -------------------------------------------------------
    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}