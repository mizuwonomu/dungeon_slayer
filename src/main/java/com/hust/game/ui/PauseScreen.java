package com.hust.game.ui;

import com.hust.game.audio.SoundManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class PauseScreen {
    private final StackPane root;

    public PauseScreen(Runnable onResume, Runnable onMenu) {
        Image buttonSheet = loadImg("/assets/button.png");

        root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        root.setVisible(false);
        root.setManaged(false);

        Text title = new Text("PAUSED");
        Font titleFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 90);
        if (titleFont == null) {
            titleFont = Font.font("Arial", FontWeight.BOLD, 90);
        }
        title.setFont(titleFont);
        title.setFill(Color.WHITE);
        title.setStroke(Color.BLACK);
        title.setStrokeWidth(3.0);

        StackPane resumeBtn = createSpriteBtn("RESUME", buttonSheet, 3, 0.7, onResume);
        StackPane menuBtn = createSpriteBtn("MAIN MENU", buttonSheet, 3, 0.7, onMenu);

        VBox uiBox = new VBox(25, title, resumeBtn, menuBtn);
        uiBox.setAlignment(Pos.CENTER);
        uiBox.setTranslateY(20);

        root.getChildren().add(uiBox);
        root.setPickOnBounds(true);
    }

    public StackPane getRoot() {
        return root;
    }

    public void setVisible(boolean visible) {
        root.setVisible(visible);
        root.setManaged(visible);
    }

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

        Text textNode = new Text(btnText);
        Font pixelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 34);
        if (pixelFont == null) {
            pixelFont = Font.font("Arial", FontWeight.BOLD, 34);
        }
        textNode.setFont(pixelFont);
        textNode.setFill(Color.RED);
        textNode.setStroke(Color.BLACK);
        textNode.setStrokeWidth(2.5);

        pane.getChildren().addAll(view, textNode);

        int[] currentFrame = {0};
        Timeline[] timeline = {null};

        pane.setOnMouseEntered(e -> {
            SoundManager.playButtonHoverSound();
            pane.setScaleX(1.10);
            pane.setScaleY(1.10);
            textNode.setFill(Color.ORANGE);
            textNode.setStroke(Color.WHITE);
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
            textNode.setFill(Color.RED);
            textNode.setStroke(Color.BLACK);
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
            SoundManager.playButtonClickSound();
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

    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}
