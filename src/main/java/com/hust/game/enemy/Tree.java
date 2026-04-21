package com.hust.game.enemy;

import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;

public class Tree extends Enemy {
    private int skillCoolDown = 0;
    private final int SKILL_TIMING = 120;

    public Tree(double x, double y, Image sprSheet, int numFrames, double renderWidth, double renderHeight,
            Player targetPlayer) {
        super(x, y, sprSheet, numFrames, renderWidth, renderHeight, targetPlayer);
        this.speed = 1.5;
        this.maxHp = 100;
        this.hp = maxHp;
        this.damage = 10;
        this.knockback = 1;
    }

    @Override
    public void update() {
        super.update();

        if (skillCoolDown > 0) {
            skillCoolDown--;
        }
        if (this.intersects(targetPlayer)) {
            if (skillCoolDown <= 0) {
                castSkill();
                skillCoolDown = SKILL_TIMING;
            }
        }
    }

    private void castSkill() {
        // Image castSKill
        System.out.println("Tree is using skill!" + targetPlayer.getX());
        targetPlayer.takeDamage(this.damage);
    }
}
