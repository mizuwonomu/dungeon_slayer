package com.hust.game.ui;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.player.Player;
import com.hust.game.map.MapManager;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class Minimap {

    private Group root;

    public Minimap(
            int width,
            int height,
            Runnable onClose,
            java.util.function.Function<String, Image> imageLoader,
            Player player,
            MapManager mapManager
    ) {

        // ===== DARK OVERLAY =====
        Region background = new Region();
        background.setPrefSize(width, height);
        background.setStyle("-fx-background-color: rgba(0,0,0,0.75);");

        // ===== LOAD MINIMAP IMAGE =====
        Image minimapImage = imageLoader.apply(
                "/assets/maps/level" + mapManager.level + ".png"
        );

        // ===== MAP VIEW =====
        ImageView mapView = new ImageView(minimapImage);

        // Scale map to fully fit screen width
        mapView.setFitWidth(width);

// Height auto-scales from aspect ratio
        mapView.setPreserveRatio(true);

        mapView.setSmooth(false);

        mapView.setPreserveRatio(true);
        mapView.setSmooth(false);

        // ===== PLAYER ICON =====
        ImageView playerDot = new ImageView(
                imageLoader.apply("/assets/berserk.png")
        );

        playerDot.setFitWidth(20);
        playerDot.setFitHeight(20);
        playerDot.setPreserveRatio(true);

        // ===== WORLD SIZE =====
        double worldWidth =
                GameConstants.MAX_WORLD_COL * GameConstants.TILE_SIZE;

        double worldHeight =
                GameConstants.MAX_WORLD_ROW * GameConstants.TILE_SIZE;

// ===== DISPLAYED MINIMAP SIZE =====
        double minimapWidth = mapView.getFitWidth();

        double minimapHeight =
                minimapImage.getHeight()
                        * (mapView.getFitWidth() / minimapImage.getWidth());

// ===== PLAYER POSITION =====
        double miniX =
                (player.getX() / worldWidth)
                        * minimapWidth;

        double miniY =
                (player.getY() / worldHeight)
                        * minimapHeight;

// StackPane center correction
        miniX -= minimapWidth / 2;
        miniY -= minimapHeight / 2;

// Center icon
        miniX += playerDot.getFitWidth() / 2;
        miniY += playerDot.getFitHeight() / 2;

        playerDot.setTranslateX(miniX);
        playerDot.setTranslateY(miniY);

        // ===== ROOT OVERLAY =====
        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(
                background,
                mapView,
                playerDot
        );

        root = new Group(overlay);
    }

    public Group getRoot() {
        return root;
    }
}