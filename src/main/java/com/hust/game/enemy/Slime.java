package com.hust.game.enemy;

import com.hust.game.entities.Player;
import javafx.scene.image.Image;

public class Slime extends Enemy {
    private int damageTick = 0;

    public Slime(double x, double y, Image spriteSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 1.0;
        this.maxHp = 50;
        this.hp = maxHp;
        this.damage = 5;
        this.knockback = 3;
    }

    @Override
    public void update() {
        super.update();

        if (damageTick > 0)
            damageTick--;
        if (this.intersects(targetPlayer)) {
            if (damageTick <= 0) {
                targetPlayer.takeDamage(this.damage);
                damageTick = 30;
            }
        }
    }
}
