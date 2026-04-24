package com.hust.game.ui;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class GameFinish {

    private StackPane root;

    public GameFinish(Runnable onRetry, Runnable onMenu) {

        // Load images
        Image retryImg = load("/assets/retry.png");
        Image retryHover = load("/assets/retryselect.png");

        Image menuImg = load("/assets/menu.png");
        Image menuHover = load("/assets/menuselect.png");

        // Create buttons
        Button retryBtn = createButton(retryImg, retryHover, onRetry);
        Button menuBtn = createButton(menuImg, menuHover, onMenu);

        // Horizontal layout
        HBox buttons = new HBox(60, retryBtn, menuBtn);
        buttons.setStyle("-fx-alignment: center;");

        // Move slightly down (below text)
        buttons.setTranslateY(100);

        // Center everything
        root = new StackPane(buttons);
        root.setPrefSize(816, 624); // match your game window size
    }

    private Button createButton(Image normal, Image hover, Runnable action) {
        ImageView view = new ImageView(normal);

        // Base size
        view.setFitWidth(300);
        view.setPreserveRatio(true);

        // Default scale
        view.setScaleX(1.1);
        view.setScaleY(1.1);

        Button btn = new Button();
        btn.setGraphic(view);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Hover effect
        btn.setOnMouseEntered(e -> {
            view.setImage(hover);
            view.setScaleX(1.25);
            view.setScaleY(1.25);
        });

        btn.setOnMouseExited(e -> {
            view.setImage(normal);
            view.setScaleX(1.1);
            view.setScaleY(1.1);
        });

        // Click action
        btn.setOnAction(e -> action.run());

        return btn;
    }

    private Image load(String path) {
        return new Image(getClass().getResourceAsStream(path));
    }

    public StackPane getRoot() {
        return root;
    }
}