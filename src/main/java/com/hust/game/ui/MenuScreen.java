package com.hust.game.ui;

import com.hust.game.constants.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

import java.util.function.Consumer;

public class MenuScreen {

    // -------------------------------------------------------
    // CALLBACK — gọi khi người dùng bấm nút
    // -------------------------------------------------------
    private final Consumer<Void> onStart;       // Bấm Play → chuyển sang game
    private final Consumer<Void> onSettings;    // Bấm Settings → chuyển sang settings

    // -------------------------------------------------------
    // ANIMATION BACKGROUND
    // Sprite sheet 4 frame nằm ngang, mỗi frame 510x300
    // -------------------------------------------------------
    private static final int BG_NUM_FRAMES     = 4;      // Tổng số frame trong sprite sheet
    private static final double BG_FRAME_W     = 510.0;  // Chiều rộng 1 frame gốc (2040 / 4)
    private static final double BG_FRAME_H     = 300.0;  // Chiều cao gốc của sprite sheet
    private static final int BG_ANIM_DELAY     = 64;     // Cứ 12 frame game thì đổi 1 frame background (~5fps)

    // -------------------------------------------------------
    // PHASE SYSTEM
    // Phase 0: Background hiện rõ nét, chưa có nút (180 frame = 3 giây)
    // Phase 1: Background mờ dần, nút fade in (60 frame = 1 giây)
    // Phase 2: Ổn định — background mờ cố định, nút hiện đầy đủ
    // -------------------------------------------------------
    private static final int PHASE0_DURATION   = 180;    // 3 giây x 60fps
    private static final int PHASE1_DURATION   = 60;     // 1 giây x 60fps
    private static final double BG_MIN_ALPHA   = 0.35;   // Độ mờ tối thiểu của background khi vào Phase 2

    public MenuScreen(Consumer<Void> onStart, Consumer<Void> onSettings) {
        this.onStart    = onStart;
        this.onSettings = onSettings;
    }

    public Scene createScene() {
        // -------------------------------------------------------
        // CANVAS — dùng để vẽ background animation
        // -------------------------------------------------------
        Canvas canvas = new Canvas(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Image startImg    = loadImg("/assets/start.png");
        Image startHover  = loadImg("/assets/startselect.png");
        Image settingsImg = loadImg("/assets/setting.png");
        Image settingsHover = loadImg("/assets/settingselect.png");
        Image exitImg     = loadImg("/assets/exit.png");
        Image exitHover   = loadImg("/assets/exitselect.png");

        // ImageView cho từng nút
        ImageView startView    = new ImageView(startImg);
        ImageView settingsView = new ImageView(settingsImg);
        ImageView exitView     = new ImageView(exitImg);

        startView.setFitWidth(300);    startView.setPreserveRatio(true);
        settingsView.setFitWidth(300); settingsView.setPreserveRatio(true);
        exitView.setFitWidth(300);     exitView.setPreserveRatio(true);

        // Tạo Button và gắn graphic
        Button startBtn    = makeBtn(startView,    startImg,    startHover,    () -> onStart.accept(null));
        Button settingsBtn = makeBtn(settingsView, settingsImg, settingsHover, () -> onSettings.accept(null));
        Button exitBtn     = makeBtn(exitView,     exitImg,     exitHover,     () -> Platform.exit());

        // VBox chứa 3 nút, xếp dọc ở giữa màn hình
        VBox btnBox = new VBox(-30, startBtn, settingsBtn, exitBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setOpacity(0.0); // Ẩn hoàn toàn lúc đầu — sẽ fade in ở Phase 1

        // -------------------------------------------------------
        // STACKPANE — xếp canvas (background) bên dưới, nút bên trên
        // -------------------------------------------------------
        StackPane root = new StackPane(canvas, btnBox);
        root.setStyle("-fx-background-color: black;"); // Nền đen khi background chưa load

        // -------------------------------------------------------
        // LOAD SPRITE SHEET BACKGROUND
        // Đặt file vào: src/main/resources/assets/menu_bg.png
        // -------------------------------------------------------
        Image bgSheet = loadImg("/assets/menu_background.png");

        // -------------------------------------------------------
        // BIẾN TRẠNG THÁI ANIMATION — dùng mảng 1 phần tử để có thể
        // thay đổi bên trong lambda (Java yêu cầu effectively final)
        // -------------------------------------------------------
        int[]    bgFrameIndex = { 0 };   // Frame hiện tại của background
        int[]    bgAnimTimer  = { 0 };   // Đếm frame để đổi ảnh background
        int[]    phaseTimer   = { 0 };   // Đếm frame tổng để chuyển phase
        int[]    phase        = { 0 };   // Phase hiện tại: 0, 1, hoặc 2
        double[] bgAlpha      = { 1.0 }; // Độ trong suốt hiện tại của background

        // -------------------------------------------------------
        // ANIMATION TIMER — chạy mỗi frame, đây là "game loop" của menu
        // -------------------------------------------------------
        AnimationTimer menuLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // --- Xóa canvas trước khi vẽ lại ---
                gc.clearRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillRect(0, 0, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

                // --- Cập nhật frame background (chạy liên tục ở mọi phase) ---
                bgAnimTimer[0]++;
                if (bgAnimTimer[0] >= BG_ANIM_DELAY) {
                    bgAnimTimer[0] = 0;
                    // Vòng lặp: frame 0 → 1 → 2 → 3 → 0 → ...
                    bgFrameIndex[0] = (bgFrameIndex[0] + 1) % BG_NUM_FRAMES;
                }

                // --- Logic chuyển phase ---
                phaseTimer[0]++;

                if (phase[0] == 0) {
                    // Phase 0: Background rõ nét (alpha = 1.0), nút ẩn
                    bgAlpha[0] = 1.0;
                    if (phaseTimer[0] >= PHASE0_DURATION) {
                        phase[0] = 1;      // Chuyển sang Phase 1
                        phaseTimer[0] = 0; // Reset bộ đếm
                    }

                } else if (phase[0] == 1) {
                    // Phase 1: Background mờ dần từ 1.0 xuống BG_MIN_ALPHA
                    // Nút fade in từ 0.0 lên 1.0
                    double progress = (double) phaseTimer[0] / PHASE1_DURATION; // 0.0 → 1.0

                    // Alpha background giảm tuyến tính
                    bgAlpha[0] = 1.0 - progress * (1.0 - BG_MIN_ALPHA);

                    // Nút tăng opacity tuyến tính (JavaFX node property)
                    btnBox.setOpacity(progress);

                    if (phaseTimer[0] >= PHASE1_DURATION) {
                        phase[0] = 2;          // Chuyển sang Phase 2 (ổn định)
                        bgAlpha[0] = BG_MIN_ALPHA;
                        btnBox.setOpacity(1.0); // Chắc chắn nút hiện đầy đủ
                    }

                }
                // Phase 2: Không cần làm gì thêm, giữ nguyên trạng thái

                // --- Vẽ background lên canvas với alpha hiện tại ---
                gc.save();
                gc.setGlobalAlpha(bgAlpha[0]);

                // Tính tọa độ sx để cắt đúng frame từ sprite sheet
                double sx = bgFrameIndex[0] * BG_FRAME_W; // Dịch sang phải theo frameIndex

                // Vẽ frame hiện tại, scale lên đầy màn hình game
                gc.drawImage(
                    bgSheet,
                    sx, 0,                                          // Nguồn: cắt từ (sx, 0)
                    BG_FRAME_W, BG_FRAME_H,                        // Nguồn: kích thước 1 frame gốc
                    0, 0,                                           // Đích: góc trên trái màn hình
                    GameConstants.WINDOW_WIDTH,                     // Đích: scale full width
                    GameConstants.WINDOW_HEIGHT                     // Đích: scale full height
                );

                gc.restore(); // Khôi phục alpha về 1.0 để không ảnh hưởng thứ khác
            }
        };

        menuLoop.start(); // Khởi động vòng lặp menu

        // Dừng loop khi scene bị thay thế (tránh leak memory)
        Scene scene = new Scene(root, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin == null) menuLoop.stop();
        });

        return scene;
    }

    // -------------------------------------------------------
    // HELPER: Tạo Button với hover effect gọn hơn
    // -------------------------------------------------------
    private Button makeBtn(ImageView view, Image normal, Image hover, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(view);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Hover: đổi ảnh + scale nhẹ
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