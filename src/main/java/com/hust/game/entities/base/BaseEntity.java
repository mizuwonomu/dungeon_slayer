package com.hust.game.entities.base;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity {
    protected double x, y;

    // biến lưu cả dải ảnh (sprite sheet)
    protected Image spriteSheet;

    // các biến để quản lí sprite sheet
    protected int numFrames;
    protected double frameWidth; // chiều rộng gốc của 1 khung hình
    protected double frameHeight;

    protected int frameIndex = 0; // chỉ số khung hình hiện tại đang vẽ (0->numFrames - 1)

    // kích thước ta muốn vẽ ra màn hình game (để upscale lên)
    protected double renderWidth;
    protected double renderHeight;

    public BaseEntity(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight) {
        this.x = x;
        this.y = y;
        this.spriteSheet = spriteSheet;
        this.numFrames = numFrames;
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;

        // kích thước gốc của 1 khung hình
        this.frameWidth = spriteSheet.getWidth() / numFrames;
        this.frameHeight = spriteSheet.getHeight();
    }

    public abstract void update();

    // hàm render để cắt sprite
    public void render(GraphicsContext gc) {
        if (spriteSheet != null) {
            // bước 1: tính toán toạ độ cắt nguồn (sx) trên dải ảnh gốc dựa trên frameIndex
            double sx = frameIndex * frameWidth;
            double sy = 0;

            // bước 2: dùng constructor drawImage
            // gc.drawImage(ảnh, sx, sy, sw, sh, dx, dy, dw, dh)
            // - sx, sy: toạ độ nguồn (để cắt)
            // - sw, sh: kích thước nguồn (kích thước gốc của 1 ô)
            // - dx, dy: toạ độ đích trên màn game
            // - dw, dh: kích thước đích(để upscale)
            gc.drawImage(spriteSheet, sx, sy, frameWidth, frameHeight, x, y, renderWidth, renderHeight);
        }
    }

    public Rectangle2D getBoundary() {
        return new Rectangle2D(x, y, renderWidth, renderHeight);
    }

    public boolean intersects(BaseEntity other) {
        return this.getBoundary().intersects(other.getBoundary());
    }
}