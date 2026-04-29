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
import javafx.geometry.Insets;

import java.util.function.Consumer;

public class MenuScreen {

    // -------------------------------------------------------
    // CALLBACK — gọi khi người dùng bấm nút
    // -------------------------------------------------------
    private final Consumer<Void> onStart;
    private final Consumer<Void> onSettings;

    // -------------------------------------------------------
    // ANIMATION BACKGROUND
    // Sprite sheet 4 frame nằm ngang, mỗi frame 510x300
    // -------------------------------------------------------
    private static final int    BG_NUM_FRAMES  = 4;      // Tổng số frame sprite sheet
    private static final double BG_FRAME_W     = 510.0;  // Chiều rộng 1 frame gốc (2040 / 4)
    private static final double BG_FRAME_H     = 300.0;  // Chiều cao gốc sprite sheet
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
        // NÚT BẤM — 3 nút sát nhau, góc trái dưới màn hình
        // -------------------------------------------------------
        Image startImg      = loadImg("/assets/start.png");
        Image startHover    = loadImg("/assets/startselect.png");
        Image settingsImg   = loadImg("/assets/setting.png");
        Image settingsHover = loadImg("/assets/settingselect.png");
        Image exitImg       = loadImg("/assets/exit.png");
        Image exitHover     = loadImg("/assets/exitselect.png");

        ImageView startView    = new ImageView(startImg);
        ImageView settingsView = new ImageView(settingsImg);
        ImageView exitView     = new ImageView(exitImg);

        // Kích thước nút — nhỏ gọn để vừa góc dưới trái
        startView.setFitWidth(290);    startView.setPreserveRatio(true);
        settingsView.setFitWidth(290); settingsView.setPreserveRatio(true);
        exitView.setFitWidth(290);     exitView.setPreserveRatio(true);

        // -------------------------------------------------------
        // Biến phase dùng mảng để lambda có thể modify
        // (Java yêu cầu biến trong lambda phải effectively final,
        //  dùng mảng 1 phần tử là cách workaround phổ biến)
        // -------------------------------------------------------
        int[]    phase   = { hasPlayedIntro ? 1 : 0 };  // Nếu đã xem intro rồi → bỏ qua Phase 0
        double[] bgAlpha = { 1.0 };

        // Nút Play — khi bấm thì chuyển sang Phase 2 (fade out)
        // Không gọi onStart ngay, chờ fade xong mới gọi trong loop
        Button startBtn = makeBtn(startView, startImg, startHover, () -> {
            if (phase[0] == 1) {
                phase[0] = 2; // Kích hoạt fade out
            }
        });

        // Nút Settings và Exit hoạt động bình thường, không cần fade
        Button settingsBtn = makeBtn(settingsView, settingsImg, settingsHover,
                () -> onSettings.accept(null));
        Button exitBtn     = makeBtn(exitView, exitImg, exitHover,
                () -> Platform.exit());

        // VBox: 3 nút xếp dọc, khoảng cách đều nhau giữa các nút
        VBox btnBox = new VBox(4, startBtn, settingsBtn, exitBtn); // 8px khoảng cách
        btnBox.setAlignment(Pos.BOTTOM_LEFT);
        btnBox.setOpacity(hasPlayedIntro ? 1.0 : 0.0); // Ẩn hoàn toàn lúc đầu — hiện ra sau Phase 0

        // -------------------------------------------------------
        // STACKPANE — canvas phía dưới, nút phía trên
        // StackPane.setAlignment đặt btnBox dính góc dưới trái
        // -------------------------------------------------------
        StackPane root = new StackPane(canvas, btnBox);
        StackPane.setAlignment(btnBox, Pos.BOTTOM_LEFT);

        // Padding: cách mép dưới 30px, cách mép trái 30px
        StackPane.setMargin(btnBox, new Insets(0, 0, 20, 70));

        root.setStyle("-fx-background-color: black;"); // Nền đen khi background chưa load

        // -------------------------------------------------------
        // LOAD SPRITE SHEET BACKGROUND
        // Đặt file vào: src/main/resources/assets/menu_bg.png
        // -------------------------------------------------------
        Image bgSheet = loadImg("/assets/menu_background.png");

        // -------------------------------------------------------
        // BIẾN TRẠNG THÁI ANIMATION
        // -------------------------------------------------------
        int[] bgFrameIndex = { 0 }; // Frame hiện tại của background (0 → 3)
        int[] bgAnimTimer  = { 0 }; // Đếm frame game để biết lúc nào đổi ảnh
        int[] phaseTimer   = { 0 }; // Đếm tổng frame đã trôi qua trong phase hiện tại

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
                        hasPlayedIntro = true; // Đánh dấu đã xem intro rồi, lần sau bỏ qua
                    }

                } else if (phase[0] == 1) {
                    // PHASE 1: Menu bình thường, nút hiển thị, background vẫn chạy
                    bgAlpha[0] = 1.0; // Giữ background rõ nét, chờ người dùng bấm

                } else if (phase[0] == 2) {
                    // PHASE 2: Người dùng bấm Play → fade out background dần dần
                    // Mỗi frame giảm alpha đi FADE_SPEED
                    bgAlpha[0] -= FADE_SPEED;

                    // Nút mờ dần theo cùng tốc độ với background
                    btnBox.setOpacity(Math.max(0.0, bgAlpha[0]));

                    // Khi alpha đủ thấp → chuyển sang game
                    if (bgAlpha[0] <= FADE_THRESHOLD) {
                        menuLoopHolder[0].stop(); // Dừng loop menu trước
                        onStart.accept(null);      // Gọi callback chuyển scene
                        return;                    // Thoát handle() để không vẽ thêm
                    }
                }

                // --- Vẽ background frame hiện tại lên canvas ---
                gc.save();
                gc.setGlobalAlpha(Math.max(0.0, bgAlpha[0])); // Đảm bảo alpha không âm

                // sx: tọa độ x bắt đầu cắt trong sprite sheet
                // Dịch sang phải theo frameIndex để lấy đúng frame
                double sx = bgFrameIndex[0] * BG_FRAME_W;

                // Vẽ frame hiện tại, scale lên full màn hình game
                gc.drawImage(
                    bgSheet,
                    sx, 0,                       // Nguồn: bắt đầu cắt từ (sx, 0)
                    BG_FRAME_W, BG_FRAME_H,      // Nguồn: kích thước 1 frame gốc
                    0, 0,                        // Đích: vẽ từ góc trên trái màn hình
                    GameConstants.WINDOW_WIDTH,  // Đích: kéo rộng full màn hình
                    GameConstants.WINDOW_HEIGHT  // Đích: kéo cao full màn hình
                );

                gc.restore(); // Khôi phục globalAlpha về 1.0 để không ảnh hưởng render khác
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
    // HELPER: Tạo Button với hover effect (đổi ảnh + scale nhẹ)
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