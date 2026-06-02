package com.hust.game.progression;

import com.hust.game.constants.GameConstants;
import com.hust.game.enemy.EnemyManager;
import com.hust.game.entities.player.Player;
import com.hust.game.entities.npc.Npc;
import com.hust.game.map.MapManager;

import javafx.scene.canvas.GraphicsContext;
import java.util.List;
import com.hust.game.entities.environment.Gate;

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
    Image bossImg;

    public GameManager(EnemyManager enemyManager, Player player, Image treeImg,
            Image treeSkillImg, Image slimeImg, Image knightImg, Image knightSkillImg, Image witchImg,
            Image witchSkillImg, Image bossImg) {

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
        this.bossImg = bossImg;
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
            bossImg
        );

        // Đặt lại chỉ số và toạ độ người chơi theo level
        if (lvlID == 0) {
            player.moveToLevelStart(4 * com.hust.game.constants.GameConstants.TILE_SIZE, 5 * com.hust.game.constants.GameConstants.TILE_SIZE);
        } else if (lvlID == 1) {
            player.moveToLevelStart(4 * com.hust.game.constants.GameConstants.TILE_SIZE, 16 * com.hust.game.constants.GameConstants.TILE_SIZE);
        } else if (lvlID == 2) {
            player.moveToLevelStart(7 * com.hust.game.constants.GameConstants.TILE_SIZE, 22 * com.hust.game.constants.GameConstants.TILE_SIZE); // Tọa độ mặc định của Level 2
        } else if (lvlID == 3) {
            player.moveToLevelStart(1 * com.hust.game.constants.GameConstants.TILE_SIZE, 19 * com.hust.game.constants.GameConstants.TILE_SIZE); // Player xuất hiện ở ô (2, 20)
        }

        currentLevel.init();
    }

    public String getBackgroundPath() {
        switch(currentLevelIndex) {
            case 2:
                return "/assets/lvl2_bg.png";
            default:
                return "/assets/back_screen.png";
        }
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

    public Npc getNpc() {
        return currentLevel != null ? currentLevel.getNpc() : null;
    }
}
