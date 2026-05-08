package com.hust.game.entities.interfaces;

import com.hust.game.entities.player.Player;

/**
 * Interactable — interface cho các entity có thể tương tác với Player.
 *
 * Có 2 kiểu tương tác:
 *   1. Auto pickup: Player chạm vào → kích hoạt ngay (bình máu, bình mana)
 *   2. Press E: Player đứng đủ gần + bấm E → kích hoạt (NPC, shop)
 */
public interface Interactable {

    /**
     * Tự động kích hoạt khi Player chạm vào entity này.
     * Dùng cho: bình máu, bình mana nhặt dưới đất.
     * @param player Player đang chạm vào
     */
    void onAutoPickUp(Player player);

    /**
     * Kích hoạt khi Player bấm E đứng đủ gần.
     * Dùng cho: NPC dialog, shop.
     * @param player Player đang tương tác
     */
    void onInteract(Player player);

    /**
     * Kiểm tra Player có đứng đủ gần để tương tác không.
     * @param player Player cần kiểm tra
     * @return true nếu đủ gần
     */
    boolean isInRange(Player player);
}