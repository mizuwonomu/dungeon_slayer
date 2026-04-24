package com.hust.game.entities.player;

import javafx.geometry.Rectangle2D;

/**
 * PlayerCombat - quản lý hitbox của đòn chém.
 *
 * Khi player chém, sinh ra 1 vùng chữ nhật phía trước mặt player.
 * CombatManager dùng hitbox này để kiểm tra enemy nào bị dính đòn.
 *
 * Tách ra khỏi Player để Player không phình to quá.
 */
public class PlayerCombat {

    // Kích thước vùng hitbox chém (pixel)
    // Rộng hơn player 1 chút để dễ chém
    private static final double ATTACK_WIDTH  = 40;
    private static final double ATTACK_HEIGHT = 40;

    /**
     * Tính toán và trả về hitbox chém dựa trên vị trí + hướng của player.
     *
     * Hitbox nằm sát phía trước mặt player theo hướng đang nhìn.
     * Ví dụ: nhìn RIGHT → hitbox nằm bên phải player.
     *
     * @param playerX      tọa độ x hiện tại của player
     * @param playerY      tọa độ y hiện tại của player
     * @param playerW      chiều rộng render của player
     * @param playerH      chiều cao render của player
     * @param direction    hướng player đang nhìn
     * @return Rectangle2D là vùng hitbox chém
     */
    public static Rectangle2D getAttackHitbox(
            double playerX, double playerY,
            double playerW, double playerH,
            com.hust.game.entities.Direction direction) {

        // Tọa độ hitbox sẽ thay đổi tùy hướng
        double hx, hy;

        switch (direction) {
            case UP:
                // Hitbox nằm phía trên player
                // Căn giữa theo chiều ngang
                hx = playerX + (playerW / 2) - (ATTACK_WIDTH / 2);
                hy = playerY - ATTACK_HEIGHT; // nằm trên player
                break;

            case DOWN:
                // Hitbox nằm phía dưới player
                hx = playerX + (playerW / 2) - (ATTACK_WIDTH / 2);
                hy = playerY + playerH; // nằm dưới player
                break;

            case LEFT:
                // Hitbox nằm bên trái player
                hx = playerX - ATTACK_WIDTH; // nằm trái player
                hy = playerY + (playerH / 2) - (ATTACK_HEIGHT / 2);
                break;

            case RIGHT:
            default:
                // Hitbox nằm bên phải player
                hx = playerX + playerW; // nằm phải player
                hy = playerY + (playerH / 2) - (ATTACK_HEIGHT / 2);
                break;
        }

        return new Rectangle2D(hx, hy, ATTACK_WIDTH, ATTACK_HEIGHT);
    }
}