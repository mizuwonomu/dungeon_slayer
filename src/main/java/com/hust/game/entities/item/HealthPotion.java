package com.hust.game.entities.item;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.StaticEntity;
import com.hust.game.entities.interfaces.Interactable;
import com.hust.game.entities.player.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * HealthPotion — bình máu nằm dưới đất, Player chạm vào tự động hồi HP.
 *
 * Kế thừa StaticEntity (đứng yên trên map).
 * Implement Interactable để xử lý auto pickup.
 *
 * Cơ chế:
 *   - Chạy animation 8 frame liên tục để bắt mắt
 *   - Player chạm vào → onAutoPickUp() → cộng HEAL_AMOUNT HP
 *   - Nếu HP đã đầy → không nhặt, bình vẫn còn đó
 *   - Sau khi nhặt → isConsumed = true → App.java xóa khỏi danh sách
 */
public class HealthPotion extends StaticEntity implements Interactable {

    // -------------------------------------------------------
    // HẰNG SỐ
    // -------------------------------------------------------
    private static final int    NUM_FRAMES  = 8;    // Số frame animation
    private static final double FRAME_W     = 32.0; // Chiều rộng 1 frame (256 / 8)
    private static final double FRAME_H     = 32.0; // Chiều cao 1 frame
    private static final int    ANIM_DELAY  = 8;    // Frame game giữa mỗi lần đổi ảnh
    private static final int    HEAL_AMOUNT = GameConstants.POTION_HEAL_AMOUNT; // Lượng máu hồi
    private static final double RANGE       = GameConstants.TILE_SIZE; // Tầm nhặt = 1 ô

    // -------------------------------------------------------
    // TRẠNG THÁI
    // -------------------------------------------------------
    private boolean isConsumed = false; // Đã được nhặt chưa
    private int animationTimer = 0;     // Đếm frame để đổi ảnh

    /**
     * Constructor — nhận Image từ App.java truyền vào
     * thay vì load cứng trong class.
     *
     * @param x           Tọa độ x trên map
     * @param y           Tọa độ y trên map
     * @param spriteSheet Sprite sheet 8 frame do App.java load và truyền vào
     */
    public HealthPotion(double x, double y, Image spriteSheet) {
        super(x, y, spriteSheet, NUM_FRAMES, FRAME_W, FRAME_H);
    }

    // -------------------------------------------------------
    // UPDATE — chạy animation liên tục
    // -------------------------------------------------------
    @Override
    public void update() {
        if (isConsumed) return; // Đã nhặt rồi thì không cần update nữa

        animationTimer++;
        if (animationTimer >= ANIM_DELAY) {
            animationTimer = 0;
            frameIndex = (frameIndex + 1) % NUM_FRAMES; // Vòng lặp 0→7→0
        }
    }

    // -------------------------------------------------------
    // RENDER
    // -------------------------------------------------------
    @Override
    public void render(GraphicsContext gc) {
        if (isConsumed) return; // Đã nhặt rồi thì không vẽ nữa
        super.render(gc);
    }

    // -------------------------------------------------------
    // INTERACTABLE
    // -------------------------------------------------------

    /**
     * Tự động hồi máu khi Player chạm vào.
     * Chỉ nhặt nếu HP chưa đầy.
     */
    @Override
    public void onAutoPickUp(Player player) {
        if (isConsumed) return; // Tránh nhặt 2 lần

        // Chỉ nhặt khi HP chưa đầy
        if (player.getCurrentHp() < player.getMaxHp()) {
            player.heal(HEAL_AMOUNT);
            isConsumed = true;
            System.out.println("Nhặt bình máu! HP +" + HEAL_AMOUNT);
        }
    }

    /**
     * Bình máu không có tương tác bấm E → để trống.
     */
    @Override
    public void onInteract(Player player) {
        // Không có press E interaction
    }

    /**
     * Kiểm tra Player có đứng đủ gần để nhặt không (trong vòng 1 tile).
     */
    @Override
    public boolean isInRange(Player player) {
        double px = player.getX() + player.getRenderWidth() / 2.0;
        double py = player.getY() + player.getRenderHeight() / 2.0;
        double bx = this.x + this.renderWidth / 2.0;
        double by = this.y + this.renderHeight / 2.0;

        double dist = Math.sqrt((px - bx) * (px - bx) + (py - by) * (py - by));
        return dist <= RANGE;
    }

    /**
     * App.java gọi hàm này để biết có cần xóa bình khỏi danh sách không.
     */
    public boolean isConsumed() {
        return isConsumed;
    }
}