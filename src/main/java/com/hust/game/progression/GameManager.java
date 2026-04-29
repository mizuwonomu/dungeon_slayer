package com.hust.game.progression;

import com.hust.game.enemy.EnemyManager;
import com.hust.game.entities.player.Player;
import com.hust.game.map.MapManager;

import javafx.scene.canvas.GraphicsContext;

import javafx.scene.image.Image;

public class GameManager {
    private int currentLevelIndex;
    private Level currentLevel;

    private EnemyManager enemyManager;
    private Player player;

    Image treeImg;
    Image treeSkillImg;
    Image slimeImg;
    Image knightImg;
    Image knightSkillImg;
    Image witchImg;
    Image witchSkillImg;

    public GameManager(EnemyManager enemyManager, Player player, Image treeImg,
            Image treeSkillImg, Image slimeImg, Image knightImg, Image knightSkillImg, Image witchImg,
            Image witchSkillImg) {

        this.currentLevelIndex = 1;

        this.enemyManager = enemyManager;
        this.player = player;

        this.treeImg = treeImg;
        this.treeSkillImg = treeSkillImg;
        this.slimeImg = slimeImg;
        this.knightImg = knightImg;
        this.knightSkillImg = knightSkillImg;
        this.witchImg = witchImg;
        this.witchSkillImg = witchSkillImg;
    }

    public void loadLevel(int lvlID){
        currentLevelIndex = lvlID;

        currentLevel = new Level(
            lvlID,
            enemyManager,
            player,
            treeImg, treeSkillImg,
            slimeImg,
            knightImg, knightSkillImg,
            witchImg, witchSkillImg
        );

        player.reset();

        currentLevel.init();
    }

    public boolean isVictory(){
        return currentLevel.isVictory();
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public void loadNextLevel(){
        currentLevelIndex++;

        loadLevel(currentLevelIndex);
    }

    public void draw(GraphicsContext gc) {
        if (currentLevel != null) {
            currentLevel.draw(gc);
        }
    }

    public MapManager getMap(){
        return currentLevel.getMap();
    }

}
