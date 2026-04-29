package com.hust.game.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class GameFinish {

    private StackPane root;

    public GameFinish(Runnable onRetry, Runnable onMenu) {
        // Load sprite sheet
        Image buttonSheet = load("/assets/button.png");

        // Create buttons
        StackPane retryBtn = createSpriteBtn("RETRY", buttonSheet, 3, 0.5, onRetry);
        StackPane menuBtn = createSpriteBtn("MENU", buttonSheet, 3, 0.5, onMenu);

        // Horizontal layout
        HBox buttons = new HBox(60, retryBtn, menuBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        // Move slightly down (below text)
        buttons.setTranslateY(100);

        // Center everything
        root = new StackPane(buttons);
        root.setPrefSize(816, 624); // match your game window size
    }

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
            Font pixelFont = Font.loadFont(getClass().getResourceAsStream("/fonts/PixelFont.ttf"), 28);
            if (pixelFont == null) {
                pixelFont = Font.font("Arial", FontWeight.BOLD, 28); // fallback nếu ko tìm thấy
            }
            textNode.setFont(pixelFont);
            textNode.setFill(Color.RED);
            textNode.setStroke(Color.BLACK);
            textNode.setStrokeWidth(1.5);
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
            int framesToAnimate = numFrames - 1 - currentFrame[0];
            if (framesToAnimate > 0) {
                timeline[0] = new Timeline(new KeyFrame(Duration.millis(50), evt -> {
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
        pane.setOnMouseReleased(e -> { pane.setScaleX(1.0); pane.setScaleY(1.0); });
        
        pane.setOnMouseClicked(e -> action.run());
        pane.setCursor(Cursor.HAND);
        
        return pane;
    }


    private Image load(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }

    public StackPane getRoot() {
        return root;
    }
}