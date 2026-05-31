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

        // Fit map to screen width
        mapView.setFitWidth(width);
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

        // ===== LEVEL SETTINGS =====
        double offsetX = 0;
        double offsetY = 0;

        double miniXMultiplier = 1.0;
        double miniYMultiplier = 1.0;

        switch (mapManager.level) {

            // ===== LEVEL 1 =====
            case 1:
                offsetX = 0;
                offsetY = 0;

                miniXMultiplier = 2.0;
                break;

            // ===== LEVEL 2 =====
            case 2:
                double scaleX = (double) width / minimapImage.getWidth();
                double scaleY = (double) height / minimapImage.getHeight();

                double scale = Math.min(scaleX, scaleY);

                mapView.setFitWidth(minimapImage.getWidth() * scale);
                mapView.setFitHeight(minimapImage.getHeight() * scale);
                break;
        }

        // ===== DISPLAYED MINIMAP SIZE =====
        double minimapWidth = mapView.getFitWidth();
        double minimapHeight = mapView.getFitHeight();

        // ===== PLAYER POSITION =====
        double miniX =
                (player.getX() / worldWidth)
                        * minimapWidth
                        * miniXMultiplier
                        - offsetX;

        double miniY =
                (player.getY() / worldHeight)
                        * minimapHeight
                        * miniYMultiplier
                        - offsetY;

        // Convert into StackPane-centered coordinates
        miniX -= minimapWidth / 2;
        miniY -= minimapHeight / 2;

        // ===== APPLY POSITION =====
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