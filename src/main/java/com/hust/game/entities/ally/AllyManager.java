package com.hust.game.entities.ally;

import com.hust.game.enemy.Enemy;
import com.hust.game.enemy.FinalBoss;
import com.hust.game.entities.player.Player;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;

public class AllyManager {
    private static final int SUMMON_COOLDOWN_FRAMES = 30 * 60;
    private static final double SPAWN_OFFSET_X = 34.0;
    private static final double SPAWN_OFFSET_Y = 10.0;

    private final List<Enemy> enemies;
    private final Image idleDown;
    private final Image idleUp;
    private final Image idleLeft;
    private final Image idleRight;
    private final Image runDown;
    private final Image runUp;
    private final Image runLeft;
    private final Image runRight;
    private final Image combatDown;
    private final Image combatUp;
    private final Image combatLeft;
    private final Image combatRight;
    private final Image swordHit;
    private final Image minionSpamImg;
    private final Image transformImg;

    private PlayerMinion activeMinion;
    private int summonCooldown = 0;
    private boolean isPlayingSummonAnim = false;
    private int summonAnimTimer = 0;
    private int summonAnimFrameIndex = 0;
    private Player summoningPlayer = null;
    private FinalBoss targetBoss = null;

    public AllyManager(List<Enemy> enemies,
            Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
            Image runDown, Image runUp, Image runLeft, Image runRight,
            Image combatDown, Image combatUp, Image combatLeft, Image combatRight,
            Image swordHit, Image minionSpamImg, Image transformImg) {
        this.enemies = enemies;
        this.idleDown = idleDown;
        this.idleUp = idleUp;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.runDown = runDown;
        this.runUp = runUp;
        this.runLeft = runLeft;
        this.runRight = runRight;
        this.combatDown = combatDown;
        this.combatUp = combatUp;
        this.combatLeft = combatLeft;
        this.combatRight = combatRight;
        this.swordHit = swordHit;
        this.minionSpamImg = minionSpamImg;
        this.transformImg = transformImg;
    }

    public void update() {
        if (summonCooldown > 0) {
            summonCooldown--;
        }
        
        if (isPlayingSummonAnim) {
            summonAnimTimer++;
            if (summonAnimTimer >= 4) { // Tốc độ animation (4 frame game / 1 frame ảnh)
                summonAnimTimer = 0;
                summonAnimFrameIndex++;
                
                if (summonAnimFrameIndex == 5) { // Frame thứ 6
                    double spawnX = summoningPlayer.getX() + SPAWN_OFFSET_X;
                    double spawnY = summoningPlayer.getY() + SPAWN_OFFSET_Y;
                    activeMinion = new PlayerMinion(
                            spawnX, spawnY,
                            idleDown, idleUp, idleLeft, idleRight,
                            runDown, runUp, runLeft, runRight,
                            combatDown, combatUp, combatLeft, combatRight,
                            swordHit, transformImg,
                            targetBoss);
                }
                
                if (summonAnimFrameIndex >= 12) {
                    isPlayingSummonAnim = false;
                }
            }
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
        if (player == null || boss == null || activeMinion != null || summonCooldown > 0 || isPlayingSummonAnim) {
            return false;
        }

        this.summoningPlayer = player;
        this.targetBoss = boss;
        this.isPlayingSummonAnim = true;
        this.summonAnimFrameIndex = 0;
        this.summonAnimTimer = 0;
        summonCooldown = SUMMON_COOLDOWN_FRAMES;
        return true;
    }
    
    public void renderSummonAnim(GraphicsContext gc) {
        if (isPlayingSummonAnim && minionSpamImg != null && summoningPlayer != null) {
            double fw = 128.0;
            double fh = 128.0;
            // Căn giữa với Player
            double drawX = summoningPlayer.getX() + summoningPlayer.getRenderWidth() / 2.0 - fw / 2.0;
            double drawY = summoningPlayer.getY() + summoningPlayer.getRenderHeight() / 2.0 - fh / 2.0;
            
            gc.drawImage(minionSpamImg, summonAnimFrameIndex * fw, 0, fw, fh, drawX, drawY, fw, fh);
        }
    }

    public void reset() {
        activeMinion = null;
        summonCooldown = 0;
        isPlayingSummonAnim = false;
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
        return activeMinion == null && summonCooldown <= 0 && !isPlayingSummonAnim && findLiveBoss() != null;
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
