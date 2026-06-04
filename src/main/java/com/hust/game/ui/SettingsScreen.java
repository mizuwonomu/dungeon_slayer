package com.hust.game.ui;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.function.Consumer;

public class SettingsScreen {

    private final Consumer<Void> onBack;
    private final Consumer<DisplaySettings.WindowMode> onWindowModeChange;
    private final SettingsTabState tabState = new SettingsTabState();

    // -------------------------------------------------------
    // BACKGROUND — dùng lại sprite sheet của menu
    // -------------------------------------------------------
    private static final int    BG_NUM_FRAMES = 4;
    private static final double BG_FRAME_W    = 1838.0;
    private static final double BG_FRAME_H    = 1079.0;
    private static final int    BG_ANIM_DELAY = 64;
    private static final double TAB_ARROW_SCALE = 0.24;
    private static final double HOTKEY_LIST_WIDTH = 760;

    // Độ mờ cố định của background (0.0 = đen hoàn toàn, 1.0 = rõ hoàn toàn)
    private static final double BG_DIM_ALPHA  = 0.3;

    // -------------------------------------------------------
    // SOUND LEVEL — bước nhảy 10, từ 0 đến 100
    // -------------------------------------------------------
    private int bgmLevel;
    private int sfxLevel;

    public SettingsScreen(Consumer<Void> onBack, Consumer<DisplaySettings.WindowMode> onWindowModeChange) {
        this.onBack = onBack;
        this.onWindowModeChange = onWindowModeChange;
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

        Image leftTabButtonSheet = loadImg("/assets/left_arrow.png");
        Image rightTabButtonSheet = loadImg("/assets/right_arrow.png");

        // -------------------------------------------------------
        // TITLE
        // -------------------------------------------------------
        javafx.scene.text.Text title = createTitle("SETTINGS");

        Font labelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 30);
        if (labelFont == null) {
            labelFont = Font.font("Arial", FontWeight.BOLD, 30);
        }
        Font loadedHotkeyFont = Font.loadFont(getClass().getResourceAsStream("/fonts/SVN-Determination_Sans.ttf"), 39);
        if (loadedHotkeyFont == null) {
            loadedHotkeyFont = Font.font("Arial", 32);
        }
        final Font hotkeyFont = loadedHotkeyFont;

        // -------------------------------------------------------
        // BACKGROUND MUSIC ROW (NHẠC NỀN)
        // -------------------------------------------------------
        javafx.scene.text.Text bgmLabel = createLabel("Nhạc nền", labelFont);

        StackPane bgmBtn = createSpriteBtn(String.valueOf(bgmLevel), buttonSheet, 3, 0.55, () -> {});

        StackPane bgmMinusBtn = createSpriteBtn("-", buttonSheet, 3, 0.35, () -> {
            if (bgmLevel > 0) {
                bgmLevel -= 10;
                ((Text) bgmBtn.getChildren().get(1)).setText(String.valueOf(bgmLevel));
                SoundManager.setBgmVolume(bgmLevel / 100.0);
                com.hust.game.main.App.updateBgmVolume(bgmLevel / 100.0);
            }
        });

        StackPane bgmPlusBtn = createSpriteBtn("+", buttonSheet, 3, 0.35, () -> {
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
        javafx.scene.text.Text sfxLabel = createLabel("Hiệu ứng", labelFont);

        StackPane sfxBtn = createSpriteBtn(String.valueOf(sfxLevel), buttonSheet, 3, 0.55, () -> {});

        StackPane sfxMinusBtn = createSpriteBtn("-", buttonSheet, 3, 0.35, () -> {
            if (sfxLevel > 0) {
                sfxLevel -= 10;
                ((Text) sfxBtn.getChildren().get(1)).setText(String.valueOf(sfxLevel));
                SoundManager.setSfxVolume(sfxLevel / 100.0);
            }
        });

        StackPane sfxPlusBtn = createSpriteBtn("+", buttonSheet, 3, 0.35, () -> {
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
        // DISPLAY MODE ROW
        // -------------------------------------------------------
        javafx.scene.text.Text displayLabel = createLabel("Chế độ màn hình", labelFont);

        StackPane displayModeBtn = createSpriteBtn(DisplaySettings.getWindowModeLabel(), buttonSheet, 3, 0.85, () -> {});
        Text displayModeText = (Text) displayModeBtn.getChildren().get(1);
        displayModeBtn.setOnMouseClicked(e -> {
            DisplaySettings.WindowMode nextMode = DisplaySettings.isFullscreen()
                    ? DisplaySettings.WindowMode.WINDOWED
                    : DisplaySettings.WindowMode.FULLSCREEN;
            DisplaySettings.setWindowMode(nextMode);
            displayModeText.setText(DisplaySettings.getWindowModeLabel());
            onWindowModeChange.accept(nextMode);
        });

        HBox displayRow = new HBox(10, displayModeBtn);
        displayRow.setAlignment(Pos.CENTER);
        VBox displayBox = new VBox(5, displayLabel, displayRow);
        displayBox.setAlignment(Pos.CENTER);

        // -------------------------------------------------------
        // NÚT BACK VỀ MENU
        // -------------------------------------------------------
        StackPane menuBtn = createSpriteBtn("BACK", buttonSheet, 3, 0.7, () -> onBack.accept(null));

        StackPane pageRoot = new StackPane();
        Runnable[] renderPage = new Runnable[1];
        renderPage[0] = () -> {
            pageRoot.getChildren().clear();
            switch (tabState.getActiveTab()) {
                case HOTKEYS -> pageRoot.getChildren().add(createHotkeysPage(
                        buttonSheet,
                        leftTabButtonSheet,
                        hotkeyFont,
                        renderPage[0]
                ));
                case SOUND_DISPLAY -> pageRoot.getChildren().add(createSoundDisplayPage(
                            title,
                            bgmBox,
                            sfxBox,
                            displayBox,
                            menuBtn,
                            rightTabButtonSheet,
                            renderPage[0]
                    ));
            }
        };
        renderPage[0].run();

        // -------------------------------------------------------
        // STACKPANE — canvas background mờ bên dưới, UI bên trên
        // -------------------------------------------------------
        StackPane root = new StackPane(canvas, pageRoot);
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
        Scene scene = ScaledSceneFactory.createScene(root);
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                javafx.application.Platform.runLater(() -> {
                    if (root.getScene() == null) bgLoop.stop();
                });
            }
        });

        return scene;
    }

    private Text createTitle(String text) {
        javafx.scene.text.Text title = new javafx.scene.text.Text(text);
        Font titleFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 80);
        if (titleFont == null) {
            titleFont = Font.font("Arial", FontWeight.BOLD, 80);
        }
        title.setFont(titleFont);
        title.setFill(Color.WHITE);
        title.setStroke(Color.BLACK);
        title.setStrokeWidth(2.0);
        return title;
    }

    private Text createLabel(String text, Font font) {
        javafx.scene.text.Text label = new javafx.scene.text.Text(text);
        label.setFont(font);
        label.setFill(Color.WHITE);
        label.setStroke(Color.BLACK);
        label.setStrokeWidth(1.5);
        return label;
    }

    private StackPane createSoundDisplayPage(
            Text title,
            VBox bgmBox,
            VBox sfxBox,
            VBox displayBox,
            StackPane menuBtn,
            Image rightTabButtonSheet,
            Runnable renderPage
    ) {
        VBox uiBox = new VBox(12, title, bgmBox, sfxBox, displayBox, menuBtn);
        uiBox.setAlignment(Pos.CENTER);

        StackPane rightBtn = createImageBtn(rightTabButtonSheet, TAB_ARROW_SCALE, () -> {
            tabState.showNextTab();
            renderPage.run();
        });
        StackPane.setAlignment(rightBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightBtn, new Insets(0, 105, 0, 0));

        StackPane page = new StackPane(uiBox, rightBtn);
        page.setPrefSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        return page;
    }

    private StackPane createHotkeysPage(Image buttonSheet, Image leftTabButtonSheet, Font hotkeyFont, Runnable renderPage) {
        Text title = createTitle("HOTKEYS");
        VBox hotkeyList = createHotkeyList(hotkeyFont,
                "WASD / Phím mũi tên: Di chuyển player",
                "J: Tấn công",
                "SHIFT: Lướt",
                "K: Nhập hồn / Companion",
                "L: Cuồng nộ",
                "Q: Dùng bình mana",
                "E: Dùng bình máu",
                "M: Mở / đóng minimap",
                "WASD: Di chuyển minimap",
                "+ / -: Thu phóng to / nhỏ map",
                "ESC: Tạm dừng / đóng minimap",
                "H: Tương tác NPC",
                "O: Thoát hội thoại/shop",
                "1 / 2: Mua vật phẩm NPC",
                "SPACE / Chuột trái: Bỏ qua thoại NPC"
        );

        ScrollPane hotkeyScroll = createHotkeyScrollPane(hotkeyList);
        StackPane menuBtn = createSpriteBtn("BACK", buttonSheet, 3, 0.7, () -> onBack.accept(null));
        VBox uiBox = new VBox(22, title, hotkeyScroll, menuBtn);
        uiBox.setAlignment(Pos.CENTER);

        StackPane leftBtn = createImageBtn(leftTabButtonSheet, TAB_ARROW_SCALE, () -> {
            tabState.showPreviousTab();
            renderPage.run();
        });
        StackPane.setAlignment(leftBtn, Pos.CENTER_LEFT);
        StackPane.setMargin(leftBtn, new Insets(0, 0, 0, 105));

        StackPane page = new StackPane(uiBox, leftBtn);
        page.setPrefSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        return page;
    }

    private ScrollPane createHotkeyScrollPane(VBox hotkeyList) {
        ScrollPane scrollPane = new ScrollPane(hotkeyList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportWidth(HOTKEY_LIST_WIDTH + 40);
        scrollPane.setPrefViewportHeight(500);
        scrollPane.setMaxWidth(HOTKEY_LIST_WIDTH + 60);
        scrollPane.setMaxHeight(500);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;"
        );
        return scrollPane;
    }

    private VBox createHotkeyList(Font font, String... hotkeyLines) {
        VBox hotkeyList = new VBox(5);
        hotkeyList.setAlignment(Pos.CENTER_LEFT);
        hotkeyList.setMaxWidth(HOTKEY_LIST_WIDTH);

        for (int i = 0; i < hotkeyLines.length; i++) {
            hotkeyList.getChildren().add(createHotkeyRow(hotkeyLines[i], font));
            if (i < hotkeyLines.length - 1) {
                hotkeyList.getChildren().add(createHotkeySeparator());
            }
        }

        return hotkeyList;
    }

    private Region createHotkeySeparator() {
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setMaxHeight(2);
        separator.setMinHeight(2);
        separator.setMaxWidth(HOTKEY_LIST_WIDTH);
        separator.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85);");
        return separator;
    }

    private StackPane createHotkeyRow(String text, Font font) {
        StackPane row = new StackPane(createHotkeyText(text, font));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20.0, 0, 20.0, 0));
        row.setMaxWidth(HOTKEY_LIST_WIDTH);
        return row;
    }

    private Text createHotkeyText(String text, Font font) {
        Text hotkey = new Text(text);
        hotkey.setFont(font);
        hotkey.setFill(Color.WHITE);
        hotkey.setStroke(Color.BLACK);
        hotkey.setStrokeWidth(1.3);
        return hotkey;
    }

    private StackPane createImageBtn(Image image, double scaleMultiplier, Runnable action) {
        StackPane pane = new StackPane();
        ImageView view = new ImageView(image);
        view.setFitWidth(image.getWidth() * scaleMultiplier);
        view.setFitHeight(image.getHeight() * scaleMultiplier);

        pane.getChildren().add(view);
        pane.setPrefSize(image.getWidth() * scaleMultiplier, image.getHeight() * scaleMultiplier);
        pane.setMaxSize(image.getWidth() * scaleMultiplier, image.getHeight() * scaleMultiplier);

        pane.setOnMouseEntered(e -> {
            com.hust.game.audio.SoundManager.playButtonHoverSound();
            pane.setScaleX(1.10);
            pane.setScaleY(1.10);
        });
        pane.setOnMouseExited(e -> {
            pane.setScaleX(1.0);
            pane.setScaleY(1.0);
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
        return pane;
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
                textNode.setFont(Font.font("Consolas", FontWeight.BOLD, 46));
                textNode.setFill(Color.BLACK);
                textNode.setStroke(Color.WHITE);
                textNode.setStrokeWidth(2.0);
            } else {
                double fontSize = btnText.length() > 8 ? 30 : 36;
                Font pixelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), fontSize);
                if (pixelFont == null) {
                    pixelFont = Font.font("Arial", FontWeight.BOLD, fontSize);
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
