package com.hust.game.entities.player;

import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.Direction;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class AttackEffect extends BaseEntity {
    private boolean active = false;
    private Player player;
    private double rotation = 0;
    private boolean isThrusting = false;

    // Thêm 2 field
    private final Image normalSprite; // sprite kiếm thường
    private final Image rageSprite;   // sprite kiếm khi bật skill

    public AttackEffect(Image normalSprite, Image rageSprite, Player player) {
        // Phóng to kích thước render lên x1.5 (90x90)
        super(0, 0, normalSprite, 8, 90, 90);
        this.player = player;
        this.normalSprite = normalSprite;
        this.rageSprite = rageSprite;
    }

    public void trigger(boolean isThrust) {
        this.active = true;
        this.isThrusting = isThrust;
        this.frameIndex = 0;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Thêm method đổi sprite
    public void setRageMode(boolean active) {
        this.spriteSheet = active ? rageSprite : normalSprite;
        this.frameWidth  = spriteSheet.getWidth() / numFrames; // cập nhật frameWidth
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

        // 2. Logic lật/xoay theo hướng
        double baseX = 0, baseY = 0;
        switch (dir) {
            case UP:
                baseX = px + (pw / 2) - (renderWidth / 2);
                baseY = py - renderHeight + 35;
                rotation = -90; // Xoay lên trên
                this.isFlipped = false;
                break;
            case DOWN:
                baseX = px + (pw / 2) - (renderWidth / 2);
                baseY = py + ph - 35; // Sát vào player
                rotation = 90; // Xoay xuống dưới
                this.isFlipped = false;
                break;
            case LEFT:
                baseX = px - renderWidth + 35; // Sát vào player
                baseY = py + (ph / 2) - (renderHeight / 2);
                rotation = 0; // Giữ nguyên góc quay
                this.isFlipped = true; // Chỉ lật ảnh ngang (mirror)
                break;
            case RIGHT:
            default:
                baseX = px + pw - 35; // Sát vào player
                baseY = py + (ph / 2) - (renderHeight / 2);
                rotation = 0; // Giữ nguyên
                this.isFlipped = false;
                break;
        }
        
        if (isThrusting) {
            int tTimer = player.getThrustTimer();
            int phase = 18 - tTimer; // Khung hình chạy từ 0 đến 17
            int cycle = phase % 6; // Lặp lại 3 nhịp (mỗi nhịp 6 khung hình)
            // Tính toán khoảng cách chọc (Thò ra và thụt vào)
            double offset = 0;
            if (cycle == 0 || cycle == 5) offset = 10;
            else if (cycle == 1 || cycle == 4) offset = 25;
            else offset = 40; // Điểm chọc xa nhất

            switch (dir) {
                case UP: baseY -= offset; break;
                case DOWN: baseY += offset; break;
                case LEFT: baseX -= offset; break;
                case RIGHT: baseX += offset; break;
            }
            this.frameIndex = 0; // Luôn dùng frame đầu tiên của kiếm để chọc
        } else {
            this.frameIndex = player.getFrameIndex();
        }

        this.x = baseX;
        this.y = baseY;
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