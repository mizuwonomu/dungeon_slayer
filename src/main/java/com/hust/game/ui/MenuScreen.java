package com.hust.game.ui;

import com.hust.game.constants.GameConstants;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

import java.util.function.Consumer;

public class MenuScreen {

    private final Consumer<Void> onStart;
    private final Consumer<Void> onSettings;

    public MenuScreen(Consumer<Void> onStart, Consumer<Void> onSettings) {
        this.onStart = onStart;
        this.onSettings = onSettings;
    }

    public Scene createScene() {

        Image startImg = loadImg("/assets/start.png");
        Image startHover = loadImg("/assets/startselect.png");

        Image settingsImg = loadImg("/assets/setting.png");
        Image settingsHover = loadImg("/assets/settingselect.png");

        Image exitImg = loadImg("/assets/exit.png");
        Image exitHover = loadImg("/assets/exitselect.png");

        // ImageViews
        ImageView startView = new ImageView(startImg);
        startView.setFitWidth(300);
        startView.setPreserveRatio(true);

        ImageView settingsView = new ImageView(settingsImg);
        settingsView.setFitWidth(300);
        settingsView.setPreserveRatio(true);

        ImageView exitView = new ImageView(exitImg);
        exitView.setFitWidth(300);
        exitView.setPreserveRatio(true);

        // Buttons
        Button startBtn = new Button();
        Button settingsBtn = new Button();
        Button exitBtn = new Button();

        startBtn.setGraphic(startView);
        settingsBtn.setGraphic(settingsView);
        exitBtn.setGraphic(exitView);

        // Remove default button styling
        startBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        exitBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Hover effects
        startBtn.setOnMouseEntered(e -> {
            startView.setImage(startHover);
            startView.setScaleX(1.15);
            startView.setScaleY(1.15);
        });
        startBtn.setOnMouseExited(e -> {
            startView.setImage(startImg);
            startView.setScaleX(1.0);
            startView.setScaleY(1.0);
        });

        settingsBtn.setOnMouseEntered(e -> {
            settingsView.setImage(settingsHover);
            settingsView.setScaleX(1.1);
            settingsView.setScaleY(1.1);
        });
        settingsBtn.setOnMouseExited(e -> {
            settingsView.setImage(settingsImg);
            settingsView.setScaleX(1.0);
            settingsView.setScaleY(1.0);
        });

        exitBtn.setOnMouseEntered(e -> {
            exitView.setImage(exitHover);
            exitView.setScaleX(1.15);
            exitView.setScaleY(1.15);
        });
        exitBtn.setOnMouseExited(e -> {
            exitView.setImage(exitImg);
            exitView.setScaleX(1.0);
            exitView.setScaleY(1.0);
        });

        // Actions
        startBtn.setOnAction(e -> onStart.accept(null));
        settingsBtn.setOnAction(e -> onSettings.accept(null));
        exitBtn.setOnAction(e -> Platform.exit());

        // Title
        Text title = new Text("GHOULITE");
        title.setStyle("-fx-font-size: 48px; -fx-fill: white;");

        VBox buttonColumn = new VBox(-30, startBtn, settingsBtn, exitBtn);
        buttonColumn.setAlignment(Pos.CENTER);

        VBox layout = new VBox(-30, title, buttonColumn);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: black;");

        return new Scene(
                layout,
                GameConstants.WINDOW_WIDTH,
                GameConstants.WINDOW_HEIGHT
        );
    }

    // Helper
    private Image loadImg(String path) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new RuntimeException("Missing asset: " + path);
        }
        return new Image(stream, 0, 0, true, false);
    }
}