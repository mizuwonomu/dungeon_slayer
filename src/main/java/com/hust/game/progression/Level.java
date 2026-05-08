package com.hust.game.progression;

import com.hust.game.map.MapManager;
import com.hust.game.enemy.*;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.ArrayList;
import java.util.List;
import com.hust.game.entities.environment.Gate;
import com.hust.game.entities.item.HealthPotion;
import com.hust.game.entities.item.ManaPotion;


public class Level {

    private static final int WIDTH = 816; // 17 * 48
    private static final int HEIGHT = 624; // 13 * 48
    private static final int TILE_SIZE = GameConstants.TILE_SIZE;

    private List<HealthPotion> healthPotions = new ArrayList<>();
    private List<ManaPotion> manaPotions = new ArrayList<>();
    
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
    
    Image healthPotionImg;
    Image manaPotionImg;

    private List<Gate> gates = new ArrayList<>();
    private Image gateImg;

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
        
        try {
            gateImg = new Image(getClass().getResourceAsStream("/assets/tiles/gate.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy gate.png");
        }
    }

    private void spawnItems() {
        if (lvlID == 1) {
            healthPotions.add(new HealthPotion(5 * TILE_SIZE, 5 * TILE_SIZE, healthPotionImg));
            manaPotions.add(new ManaPotion(8 * TILE_SIZE, 5 * TILE_SIZE, manaPotionImg));
        } else if (lvlID == 2) {
            healthPotions.add(new HealthPotion(10 * TILE_SIZE, 8 * TILE_SIZE, healthPotionImg));
            manaPotions.add(new ManaPotion(12 * TILE_SIZE, 8 * TILE_SIZE, manaPotionImg));
        }
    }
    public void init(){
        spawnEnemy();
        spawnGates();
        spawnItems();
    }

    void draw(GraphicsContext gc) {
        map.draw(gc);
        for (Gate gate : gates) {
            gate.render(gc);
        }
    }
    
    public void update() {
        for (Gate gate : gates) {
            gate.update();
        }
        
        // Logic mở cổng Tutorial
        if (lvlID == 0) {
            List<Enemy> enemies = enemyManager.getEnemyList();
            boolean slimeDead = true;
            boolean knightDead = true;
            
            for (Enemy e : enemies) {
                if (e instanceof Slime && e.getHp() > 0) slimeDead = false;
                if (e instanceof Knight && e.getHp() > 0) knightDead = false;
            }
            
            for (Gate g : gates) {
                if (g.getX() < 20 * TILE_SIZE) {
                    g.open(); // Cổng đầu tiên (nếu có) luôn mở sẵn
                } else if (g.getX() < 35 * TILE_SIZE && slimeDead) {
                    g.open(); // Cổng qua phòng 3 mở khi Slime chết
                } else if (g.getX() > 35 * TILE_SIZE && knightDead) {
                    g.open(); // Cổng qua phòng cuối mở khi Knight chết
                }
            }
            
            // Đánh thức quái vật ở phòng cuối (Tree) khi cổng phòng 4 được mở (Knight chết)
            if (knightDead) {
                for (Enemy e : enemies) {
                    if (e instanceof Tree) {
                        e.setImmobile(false);
                        e.setHarmless(false);
                    }
                }
            }
        }
    }
    
    private void spawnGates() {
        if (gateImg != null) {
            for (int[] pos : map.gatePositions) {
                Gate gate = new Gate(pos[0] * TILE_SIZE, pos[1] * TILE_SIZE, gateImg);
                gates.add(gate);
            }
        }
    }

    public List<Gate> getGates() {
        return gates;
    }

    public List<HealthPotion> getHealthPotions() { return healthPotions; }
    public List<ManaPotion> getManaPotion() { return manaPotions; }

    void spawnEnemy(){
        if (lvlID == 0) {
            // Phòng 2: Slime bất động
            enemyManager.spawnEnemy("Slime", 20 * TILE_SIZE, 6 * TILE_SIZE, slimeImg, 8, TILE_SIZE, TILE_SIZE, player);
            // Phòng 3: Knight bất động
            enemyManager.spawnEnemy("Knight", 37 * TILE_SIZE, 5 * TILE_SIZE, knightImg, 8, TILE_SIZE * 2, TILE_SIZE * 2, player, knightSkillImg);
            // Phòng 4: 3 Tree bình thường
            enemyManager.spawnEnemy("Tree", 52 * TILE_SIZE, 3 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 54 * TILE_SIZE, 5 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            enemyManager.spawnEnemy("Tree", 52 * TILE_SIZE, 7 * TILE_SIZE, treeImg, 8, TILE_SIZE, TILE_SIZE, player, treeSkillImg);
            
            for (Enemy e : enemyManager.getEnemyList()) {
                // Khóa toàn bộ quái vật ban đầu (Bao gồm cả Tree ở phòng cuối)
                e.setImmobile(true);
                e.setHarmless(true);
            }
        } else if(lvlID == 1){
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
