package com.hust.game.ui;

import com.hust.game.constants.GameConstants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class ScaledSceneFactory {
    private ScaledSceneFactory() {
    }

    public static Scene createScene(Parent content) {
        if (content instanceof Region region) {
            region.setMinSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
            region.setPrefSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
            region.setMaxSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        }

        Group scaledContent = new Group(content);
        StackPane viewport = new StackPane(scaledContent);
        viewport.setStyle("-fx-background-color: black;");
        viewport.setPrefSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);

        NumberBinding scale = Bindings.min(
                viewport.widthProperty().divide(GameConstants.WINDOW_WIDTH),
                viewport.heightProperty().divide(GameConstants.WINDOW_HEIGHT)
        );
        scaledContent.scaleXProperty().bind(scale);
        scaledContent.scaleYProperty().bind(scale);

        return new Scene(viewport, GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
    }
}
