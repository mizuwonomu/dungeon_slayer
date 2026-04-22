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

    // cờ lật mặt ảnh (mirror) khi đổi hướng
    protected boolean isFlipped = false;

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
        if (spriteSheet == null)
            return;

        double sx = frameIndex * frameWidth;
        /*
         * Tất cả frame nằm trên 1 hàng ngang duy nhất
         * Chỉ cần dịch sx sang phải theo frameIndex là lấy được frame tiếp theo
         * sy luôn = 0 vì không có hàng nào khác phía dưới
         * 
         * /*Nếu sprite sheet có nhiều hàng (ví dụ hàng 1 idle, hàng 2 run) thì lúc đó
         * sy mới cần tính.
         */

        if (isFlipped) {
            // Vẽ ngược từ phải sang trái bằng cách cộng chiều rộng vào x và để renderWidth
            // mang dấu âm
            gc.drawImage(spriteSheet, sx, 0, frameWidth, frameHeight, x + renderWidth, y, -renderWidth, renderHeight);
        } else {
            gc.drawImage(spriteSheet, sx, 0, frameWidth, frameHeight, x, y, renderWidth, renderHeight);
        }
    }

    public Rectangle2D getBoundary() {
        return new Rectangle2D(x, y, renderWidth, renderHeight);
    }

    public boolean intersects(BaseEntity other) {
        return this.getBoundary().intersects(other.getBoundary());
    }
}