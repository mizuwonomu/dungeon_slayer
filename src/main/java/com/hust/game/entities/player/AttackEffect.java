package com.hust.game.entities.player;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.Direction;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class AttackEffect extends BaseEntity {
    private boolean active = false;
    private Player player;
    private double rotation = 0;

    public AttackEffect(Image spriteSheet, Player player) {
        // Phóng to kích thước render lên x1.5 (90x90)
        super(0, 0, spriteSheet, 8, 90, 90);
        this.player = player;
    }

    public void trigger() {
        this.active = true;
        this.frameIndex = 0;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void update() {
        if (!active) return;

        // 1. Cập nhật vị trí bám theo player
        Direction dir = player.getDirection();
        double px = player.getX();
        double py = player.getY();
        double pw = player.getRenderWidth();
        double ph = player.getRenderHeight();

        // 2. Logic lật/xoay theo hướng (mặc định ảnh hướng sang phải)
        switch (dir) {
            case UP:
                x = px + (pw / 2) - (renderWidth / 2);
                y = py - renderHeight + 35; // Tăng offset để sát vào player
                rotation = -90; // Xoay lên trên
                this.isFlipped = false;
                break;
            case DOWN:
                x = px + (pw / 2) - (renderWidth / 2);
                y = py + ph - 35; // Sát vào player
                rotation = 90; // Xoay xuống dưới
                this.isFlipped = false;
                break;
            case LEFT:
                x = px - renderWidth + 35; // Sát vào player
                y = py + (ph / 2) - (renderHeight / 2);
                rotation = 0; // Giữ nguyên góc quay
                this.isFlipped = true; // Chỉ lật ảnh ngang (mirror)
                break;
            case RIGHT:
            default:
                x = px + pw - 35; // Sát vào player
                y = py + (ph / 2) - (renderHeight / 2);
                rotation = 0; // Giữ nguyên
                this.isFlipped = false;
                break;
        }
        
        // Frame của AttackEffect được đồng bộ trực tiếp từ frameIndex của Player
        // để đảm bảo vung kiếm khớp với tay nhân vật
        this.frameIndex = player.getFrameIndex();
    }

    @Override
    public void render(GraphicsContext gc) {
        if (!active || spriteSheet == null) return;
        double sx = frameIndex * frameWidth;
        gc.save();
        gc.translate(x + renderWidth / 2, y + renderHeight / 2);
        gc.rotate(rotation);
        if (isFlipped) {
            gc.drawImage(spriteSheet, sx, 0, frameWidth, frameHeight, renderWidth / 2, -renderHeight / 2, -renderWidth, renderHeight);
        } else {
            gc.drawImage(spriteSheet, sx, 0, frameWidth, frameHeight, -renderWidth / 2, -renderHeight / 2, renderWidth, renderHeight);
        }
        gc.restore();
    }
}