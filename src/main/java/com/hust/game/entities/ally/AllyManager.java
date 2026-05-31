package com.hust.game.entities.ally;

import com.hust.game.enemy.Enemy;
import com.hust.game.enemy.FinalBoss;
import com.hust.game.entities.player.Player;
import java.util.List;
import javafx.scene.image.Image;

public class AllyManager {
    private static final int SUMMON_COOLDOWN_FRAMES = 30 * 60;
    private static final double SPAWN_OFFSET_X = 34.0;
    private static final double SPAWN_OFFSET_Y = 10.0;

    private final List<Enemy> enemies;
    private final Image idleDown;
    private final Image idleUp;
    private final Image idleLeft;
    private final Image idleRight;
    private final Image combatDown;
    private final Image combatUp;
    private final Image combatLeft;
    private final Image combatRight;
    private final Image swordHit;

    private PlayerMinion activeMinion;
    private int summonCooldown = 0;

    public AllyManager(List<Enemy> enemies,
            Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
            Image combatDown, Image combatUp, Image combatLeft, Image combatRight,
            Image swordHit) {
        this.enemies = enemies;
        this.idleDown = idleDown;
        this.idleUp = idleUp;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.combatDown = combatDown;
        this.combatUp = combatUp;
        this.combatLeft = combatLeft;
        this.combatRight = combatRight;
        this.swordHit = swordHit;
    }

    public void update() {
        if (summonCooldown > 0) {
            summonCooldown--;
        }

        if (activeMinion != null) {
            activeMinion.update();
            if (!activeMinion.isAlive()) {
                activeMinion = null;
            }
        }
    }

    public boolean trySummon(Player player) {
        FinalBoss boss = findLiveBoss();
        if (player == null || boss == null || activeMinion != null || summonCooldown > 0) {
            return false;
        }

        double spawnX = player.getX() + SPAWN_OFFSET_X;
        double spawnY = player.getY() + SPAWN_OFFSET_Y;
        activeMinion = new PlayerMinion(
                spawnX, spawnY,
                idleDown, idleUp, idleLeft, idleRight,
                combatDown, combatUp, combatLeft, combatRight,
                swordHit,
                boss);
        summonCooldown = SUMMON_COOLDOWN_FRAMES;
        return true;
    }

    public void reset() {
        activeMinion = null;
        summonCooldown = 0;
    }

    public PlayerMinion getActiveMinion() {
        return activeMinion;
    }

    public int getSummonCooldownRemaining() {
        return summonCooldown;
    }

    public boolean hasActiveMinion() {
        return activeMinion != null;
    }

    public boolean canSummon() {
        return activeMinion == null && summonCooldown <= 0 && findLiveBoss() != null;
    }

    public boolean hasLiveBoss() {
        return findLiveBoss() != null;
    }

    private FinalBoss findLiveBoss() {
        if (enemies == null) {
            return null;
        }

        for (Enemy enemy : enemies) {
            if (enemy instanceof FinalBoss boss && boss.getHp() > 0) {
                return boss;
            }
        }
        return null;
    }
}
