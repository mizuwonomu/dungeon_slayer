package com.hust.game.progression;

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
            player.reset(4 * com.hust.game.constants.GameConstants.TILE_SIZE, 5 * com.hust.game.constants.GameConstants.TILE_SIZE);
        } else if (lvlID == 1) {
            player.reset(4 * com.hust.game.constants.GameConstants.TILE_SIZE, 16 * com.hust.game.constants.GameConstants.TILE_SIZE);
        } else if (lvlID == 2) {
            player.reset(408, 200); // Tọa độ mặc định của Level 2
        } else if (lvlID == 3) {
            player.reset(150, 312); // Dịch player sang trái và giữa trục Y cho màn đánh Boss
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

    public Npc getNpc() {
        return currentLevel != null ? currentLevel.getNpc() : null;
    }
}
