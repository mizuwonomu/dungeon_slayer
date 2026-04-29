package com.hust.game.ui;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.util.function.Consumer;

public class SettingsScreen {

    private final Consumer<Void> onBack;

    // -------------------------------------------------------
    // BACKGROUND — dùng lại sprite sheet của menu
    // -------------------------------------------------------
    private static final int    BG_NUM_FRAMES = 4;
    private static final double BG_FRAME_W    = 510.0;
    private static final double BG_FRAME_H    = 300.0;
    private static final int    BG_ANIM_DELAY = 64;

    // Độ mờ cố định của background (0.0 = đen hoàn toàn, 1.0 = rõ hoàn toàn)
    private static final double BG_DIM_ALPHA  = 0.3;

    // -------------------------------------------------------
    // SOUND LEVEL — bước nhảy 10, từ 0 đến 100
    // -------------------------------------------------------
    private int soundLevel = 50;

    public SettingsScreen(Consumer<Void> onBack) {
        this.onBack = onBack;
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
        Image menuImg    = loadImg("/assets/menu.png");
        Image menuHover  = loadImg("/assets/menuselect.png");
        Image minusImg   = loadImg("/assets/sound/minus.png");
        Image minusHover = loadImg("/assets/sound/minusselect.png");
        Image plusImg    = loadImg("/assets/sound/plus.png");
        Image plusHover  = loadImg("/assets/sound/plusselect.png");

        // -------------------------------------------------------
        // TITLE
        // -------------------------------------------------------
        javafx.scene.text.Text title = new javafx.scene.text.Text("SETTINGS");
        title.setStyle("-fx-font-size: 42px; -fx-fill: white; -fx-font-weight: bold;");

        // -------------------------------------------------------
        // SOUND IMAGE — ảnh 1920x1080, fitWidth 300 → height ~168px
        // -------------------------------------------------------
        ImageView soundView = new ImageView(loadSoundImage());
        soundView.setFitWidth(300);
        soundView.setPreserveRatio(true);

        // -------------------------------------------------------
        // NÚT MINUS và PLUS
        // -------------------------------------------------------
        ImageView minusView = new ImageView(minusImg);
        minusView.setFitWidth(100);
        minusView.setPreserveRatio(true);

        Button minusBtn = makeBtn(minusView, minusImg, minusHover, () -> {
            if (soundLevel > 0) {
                soundLevel -= 10;
                soundView.setImage(loadSoundImage()); // Cập nhật ảnh mức âm lượng
                SoundManager.setMasterVolume(soundLevel / 100.0);
            }
        });

        ImageView plusView = new ImageView(plusImg);
        plusView.setFitWidth(100);
        plusView.setPreserveRatio(true);

        Button plusBtn = makeBtn(plusView, plusImg, plusHover, () -> {
            if (soundLevel < 100) {
                soundLevel += 10;
                soundView.setImage(loadSoundImage()); // Cập nhật ảnh mức âm lượng
                SoundManager.setMasterVolume(soundLevel / 100.0);
            }
        });

        // -------------------------------------------------------
        // SOUND ROW — minus | ảnh sound | plus nằm ngang
        // -------------------------------------------------------
        HBox soundRow = new HBox(10, minusBtn, soundView, plusBtn);
        soundRow.setAlignment(Pos.CENTER);

        // -------------------------------------------------------
        // NÚT BACK VỀ MENU
        // -------------------------------------------------------
        ImageView menuView = new ImageView(menuImg);
        menuView.setFitWidth(200);
        menuView.setPreserveRatio(true);

        Button menuBtn = makeBtn(menuView, menuImg, menuHover, () -> onBack.accept(null));

        // -------------------------------------------------------
        // LAYOUT TỔNG — Title → Sound Row → Back, canh giữa màn hình
        // -------------------------------------------------------
        VBox uiBox = new VBox(30, title, soundRow, menuBtn);
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

                gc.drawImage(
                    bgSheet,
                    sx, 0,                       // Nguồn: cắt từ (sx, 0)
                    BG_FRAME_W, BG_FRAME_H,      // Nguồn: kích thước 1 frame gốc
                    0, 0,                        // Đích: góc trên trái màn hình
                    GameConstants.WINDOW_WIDTH,  // Đích: full width
                    GameConstants.WINDOW_HEIGHT  // Đích: full height
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
    // HELPER: Load ảnh sound theo soundLevel hiện tại
    // -------------------------------------------------------
    private Image loadSoundImage() {
        return loadImg("/assets/sound/sound" + soundLevel + ".png");
    }

    // -------------------------------------------------------
    // HELPER: Tạo Button với hover effect
    // -------------------------------------------------------
    private Button makeBtn(ImageView view, Image normal, Image hover, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(view);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        btn.setOnMouseEntered(e -> {
            view.setImage(hover);
            view.setScaleX(1.15);
            view.setScaleY(1.15);
        });
        btn.setOnMouseExited(e -> {
            view.setImage(normal);
            view.setScaleX(1.0);
            view.setScaleY(1.0);
        });

        btn.setOnAction(e -> action.run());
        return btn;
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