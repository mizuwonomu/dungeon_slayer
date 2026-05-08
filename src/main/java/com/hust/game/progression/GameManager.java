package com.hust.game.progression;

import com.hust.game.enemy.EnemyManager;
import com.hust.game.entities.player.Player;
import com.hust.game.map.MapManager;

import javafx.scene.canvas.GraphicsContext;
import java.util.List;
import com.hust.game.entities.environment.Gate;
import com.hust.game.entities.item.HealthPotion;
import com.hust.game.entities.item.ManaPotion;

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

    Image healthPotionImg;
    Image manaPotionImg;

    public GameManager(EnemyManager enemyManager, Player player, Image treeImg,
            Image treeSkillImg, Image slimeImg, Image knightImg, Image knightSkillImg, Image witchImg,
            Image witchSkillImg, Image healthPotionImg, 
            Image manaPotionImg) {

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
            witchImg, witchSkillImg,
            healthPotionImg, manaPotionImg
        );

        // Đặt lại chỉ số và toạ độ người chơi theo level
        if (lvlID == 0 || lvlID == 1) {
            player.reset(4 * com.hust.game.constants.GameConstants.TILE_SIZE, 5 * com.hust.game.constants.GameConstants.TILE_SIZE);
        } else {
            player.reset(408, 200); // Tọa độ mặc định của Level 2
        }

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
    
    public void update() {
        if (currentLevel != null) {
            currentLevel.update();
        }
    }

    public void draw(GraphicsContext gc) {
        if (currentLevel != null) {
            currentLevel.draw(gc);
        }
    }

    public MapManager getMap(){
        return currentLevel.getMap();
    }

    public List<Gate> getGates() {
        return currentLevel != null ? currentLevel.getGates() : null;
    }

    public List<HealthPotion> getHealthPotions() {
        return currentLevel.getHealthPotions();
    }

    public List<ManaPotion> getManaPotions() {
        return currentLevel.getManaPotion();
    }
}
