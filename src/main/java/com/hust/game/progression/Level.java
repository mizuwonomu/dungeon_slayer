package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;


public class Level {

    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;

    private int lvlID;

    private EnemyManager enemyManager;
    private MapManager map;
    Player player;

    Image treeImg;
    Image treeSkillImg;
    Image slimeImg;
    Image knightImg;
    Image knightSkillImg;
    Image witchImg;
    Image witchSkillImg;

    public Level(int lvlID,
                 EnemyManager enemyManager,
                 Player player,
                 Image treeImg, Image treeSkillImg,
                 Image slimeImg,
                 Image knightImg, Image knightSkillImg,
                 Image witchImg, Image witchSkillImg) {

        this.lvlID = lvlID;
        this.enemyManager = enemyManager;
        this.player = player;

        this.treeImg = treeImg;
        this.treeSkillImg = treeSkillImg;
        this.slimeImg = slimeImg;
        this.knightImg = knightImg;
        this.knightSkillImg = knightSkillImg;
        this.witchImg = witchImg;
        this.witchSkillImg = witchSkillImg;

        map = new MapManager(lvlID);
    }
    public void init(){
        spawnEnemy();
    }

    void draw(GraphicsContext gc) {
        map.draw(gc);
    }

    void spawnEnemy(){
        if(lvlID == 1){
            enemyManager.spawnEnemy("Tree", WIDTH / 2 + 100, HEIGHT / 2, treeImg, 8,
            TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Slime", WIDTH / 2 - 100, HEIGHT / 2, slimeImg, 8,
            TILE_SIZE, TILE_SIZE, player);
            enemyManager.spawnEnemy("Tree", WIDTH / 2 + 100, HEIGHT / 2 + 80, treeImg, 8,
            TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Slime", WIDTH / 2 - 100, HEIGHT / 2 + 80, slimeImg, 8,
            TILE_SIZE, TILE_SIZE, player);
        }
        else if(lvlID == 2){
            enemyManager.spawnEnemy("Knight", WIDTH / 2 - 200, HEIGHT / 2 - 200, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, 
                                    player, knightSkillImg);
            enemyManager.spawnEnemy("Knight", WIDTH / 2 + 50 , HEIGHT / 2 - 200, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, 
                                    player, knightSkillImg);
            enemyManager.spawnEnemy("Witch", WIDTH / 2 - 250 , HEIGHT / 2 , witchImg, 25, TILE_SIZE, TILE_SIZE,
                                    player, witchSkillImg);
        }
    }

    boolean isVictory(){
        return enemyManager.getEnemyList().isEmpty();
    }

    public MapManager getMap() {
        return map;
    }
}
