package com.hust.game.ui;

import com.hust.game.audio.SoundManager;
import com.hust.game.constants.GameConstants;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

import java.util.function.Consumer;

public class SettingsScreen {

    private final Consumer<Void> onBack;

    private int soundLevel = 50;

    public SettingsScreen(Consumer<Void> onBack) {
        this.onBack = onBack;
    }

    public Scene createScene() {

        // ===== TITLE =====
        Text title = new Text("SETTINGS");
        title.setStyle("-fx-font-size: 48px; -fx-fill: white;");

        // ===== LOAD STATIC IMAGES =====
        Image menuImg = loadImg("/assets/menu.png");
        Image menuHover = loadImg("/assets/menuselect.png");

        Image minusImg = loadImg("/assets/sound/minus.png");
        Image minusHover = loadImg("/assets/sound/minusselect.png");

        Image plusImg = loadImg("/assets/sound/plus.png");
        Image plusHover = loadImg("/assets/sound/plusselect.png");

        // ===== SOUND IMAGE (dynamic) =====
        ImageView soundView = new ImageView(loadSoundImage());
        soundView.setFitWidth(300); // size of sound display
        soundView.setPreserveRatio(true);

        // ===== MINUS BUTTON (BIGGER) =====
        ImageView minusView = new ImageView(minusImg);
        minusView.setFitWidth(150); // 🔥 bigger
        minusView.setPreserveRatio(true);

        Button minusBtn = new Button();
        minusBtn.setGraphic(minusView);
        minusBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        minusBtn.setOnMouseEntered(e -> {
            minusView.setImage(minusHover);
            minusView.setScaleX(1.15);
            minusView.setScaleY(1.15);
        });

        minusBtn.setOnMouseExited(e -> {
            minusView.setImage(minusImg);
            minusView.setScaleX(1.0);
            minusView.setScaleY(1.0);
        });

        minusBtn.setOnAction(e -> {
            if (soundLevel > 0) {
                soundLevel -= 10;
                soundView.setImage(loadSoundImage());

                SoundManager.setMasterVolume(soundLevel / 100.0);
            }
        });

        // ===== PLUS BUTTON (BIGGER) =====
        ImageView plusView = new ImageView(plusImg);
        plusView.setFitWidth(150); // 🔥 bigger
        plusView.setPreserveRatio(true);

        Button plusBtn = new Button();
        plusBtn.setGraphic(plusView);
        plusBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        plusBtn.setOnMouseEntered(e -> {
            plusView.setImage(plusHover);
            plusView.setScaleX(1.15);
            plusView.setScaleY(1.15);
        });

        plusBtn.setOnMouseExited(e -> {
            plusView.setImage(plusImg);
            plusView.setScaleX(1.0);
            plusView.setScaleY(1.0);
        });

        plusBtn.setOnAction(e -> {
            if (soundLevel < 100) {
                soundLevel += 10;
                soundView.setImage(loadSoundImage());

                SoundManager.setMasterVolume(soundLevel / 100.0);
            }
        });

        // ===== SOUND ROW =====
        HBox soundRow = new HBox(-100, minusBtn, soundView, plusBtn);
        soundRow.setAlignment(Pos.CENTER);
        soundRow.setTranslateY(20);

        // ===== MENU BUTTON =====
        ImageView menuView = new ImageView(menuImg);
        menuView.setFitWidth(300);
        menuView.setPreserveRatio(true);

        Button menuBtn = new Button();
        menuBtn.setGraphic(menuView);
        menuBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        menuBtn.setTranslateY(-20);

        menuBtn.setOnMouseEntered(e -> {
            menuView.setImage(menuHover);
            menuView.setScaleX(1.1);
            menuView.setScaleY(1.1);
        });

        menuBtn.setOnMouseExited(e -> {
            menuView.setImage(menuImg);
            menuView.setScaleX(1.0);
            menuView.setScaleY(1.0);
        });

        menuBtn.setOnAction(e -> onBack.accept(null));

        // ===== LAYOUT =====
        VBox layout = new VBox(60, title, soundRow, menuBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: black;");

        return new Scene(
                layout,
                GameConstants.WINDOW_WIDTH,
                GameConstants.WINDOW_HEIGHT
        );


    }

    // ===== LOAD SOUND IMAGE BASED ON VALUE =====
    private Image loadSoundImage() {
        String path = "/assets/sound/sound" + soundLevel + ".png";
        return loadImg(path);
    }

    // ===== HELPER =====
    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}