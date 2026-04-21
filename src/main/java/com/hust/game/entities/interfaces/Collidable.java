package com.hust.game.entities.interfaces;

import com.hust.game.entities.base.BaseEntity;

public interface Collidable {
    /**
     * Gọi khi entity này va chạm với một entity khác.
     * 
     * @param other entity bị va chạm vào
     */
    void onCollision(BaseEntity other);
}