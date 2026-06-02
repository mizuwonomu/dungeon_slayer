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
import javafx.scene.shape.Rectangle;

public class Minimap {

    private Group root;

    private ImageView mapView;
    private ImageView playerDot;
    private Group mapContainer;

    private double zoom = 1.0;

    private double panX = 0;
    private double panY = 0;

    private void applyTransform() {

        mapContainer.setScaleX(zoom);
        mapContainer.setScaleY(zoom);

        mapContainer.setTranslateX(panX);
        mapContainer.setTranslateY(panY);
    }

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
        mapView = new ImageView(minimapImage);

        // Fit map to screen width
        mapView.setFitWidth(width);
        mapView.setPreserveRatio(true);
        mapView.setSmooth(false);

        // ===== PLAYER ICON =====
        playerDot = new ImageView(
                imageLoader.apply("/assets/skill_berserk_box.png")
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
                worldWidth = 256 * GameConstants.TILE_SIZE;
                worldHeight = 32 * GameConstants.TILE_SIZE;

                miniXMultiplier = 2.0;
                break;

            // ===== LEVEL 2 =====
            case 2:
                mapView.setFitWidth(width * 0.9);
                mapView.setPreserveRatio(true);

                worldWidth = 5820;
                worldHeight = 3023;
                break;

            default:
                worldWidth = 256 * GameConstants.TILE_SIZE;
                worldHeight = 32 * GameConstants.TILE_SIZE;
        }

        // ===== DISPLAYED MINIMAP SIZE =====
        double minimapWidth = mapView.getFitWidth();

        double minimapHeight =
                minimapImage.getHeight()
                        * (mapView.getFitWidth() / minimapImage.getWidth());

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

        // ===== APPLY POSITION =====
        playerDot.setLayoutX(miniX);
        playerDot.setLayoutY(miniY);

        // ===== ROOT OVERLAY =====
        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefSize(width, height);
        overlay.setMinSize(width, height);
        overlay.setMaxSize(width, height);

        Rectangle clip = new Rectangle(width, height);
        overlay.setClip(clip);

        mapContainer = new Group(
                mapView,
                playerDot
        );

        overlay.getChildren().addAll(
                background,
                mapContainer
        );

        root = new Group(overlay);
    }

    public void zoomIn() {
        zoom *= 1.1;
        applyTransform();
    }

    public void zoomOut() {
        zoom /= 1.1;

        if (zoom < 0.5) {
            zoom = 0.5;
        }

        applyTransform();
    }

    public void moveUp() {
        panY += 30;
        applyTransform();
    }

    public void moveDown() {
        panY -= 30;
        applyTransform();
    }

    public void moveLeft() {
        panX += 30;
        applyTransform();
    }

    public void moveRight() {
        panX -= 30;
        applyTransform();
    }

    public Group getRoot() {
        return root;
    }
}