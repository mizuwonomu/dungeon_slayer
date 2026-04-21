package com.hust.game.entities.interfaces;

public interface Attackable {
    int getAttackDamage(); // player bị tấn công bao nhiêu damage

    boolean canAttack(); // cooldown check sau bao nhiêu thời gian thì được tấn công - cân nhắc hoặc spam
                         // skill liên tục :))))))

    void resetAttackCooldown();
}