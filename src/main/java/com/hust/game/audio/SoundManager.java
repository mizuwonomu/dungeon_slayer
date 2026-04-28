package com.hust.game.audio;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    public static AudioClip nsMiss, nsHitSlime, nsHitKnight, nsFinalHit, playerHitS, playerPowerUp, sPowerUp,nsHitTree, thunder;
    public static AudioClip slimeMove, knightAtk, knightReady, treeMoving, treeAtk;

    public static void loadSounds() {
        // Thêm tham số âm lượng (từ 0.0 đến 1.0) để điều chỉnh mức to/nhỏ của từng file
        nsMiss = loadSound("normal_sword_miss.wav", 1.0);
        nsHitSlime = loadSound("normal_sword_hit_slime.wav", 1.0);
        nsHitKnight = loadSound("normal_sword_hit_knight.wav", 1.0);
        nsFinalHit = loadSound("normal_sword_final_hit.wav", 1.0);
        nsHitTree = loadSound("normal_sword_hit_tree.wav", 1.0);
        playerPowerUp = loadSound("player_power_up.wav", 1.0);
        sPowerUp = loadSound("sword_power_up.wav", 1.0);
        thunder = loadSound("thunder.wav", 1.0);

        playerHitS = loadSound("player_hit.wav", 1.0);
        
        slimeMove = loadSound("slime_move.wav", 0.4); // Ví dụ: giảm âm lượng di chuyển của Slime xuống 40%
        knightAtk = loadSound("knight_atk.wav", 0.8);
        knightReady = loadSound("knight_ready.wav", 0.8);
        treeMoving = loadSound("tree_moving.wav", 0.4);
        treeAtk = loadSound("tree_atk.wav", 0.8);
    }

    private static AudioClip loadSound(String fileName, double volume) {
        try {
            // Tải từ thư mục resources/sounds
            URL url = SoundManager.class.getResource("/sounds/" + fileName);
            if (url != null) {
                AudioClip clip = new AudioClip(url.toString());
                clip.setVolume(volume); // Gán âm lượng mặc định cho file âm thanh
                return clip;
            } else {
                System.err.println("Không tìm thấy file âm thanh: /sounds/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    public static void playNsMissSound() { 
        if (nsMiss != null) { nsMiss.stop(); nsMiss.play(); } 
    }
    public static void playNsHitSlimeSound() { 
        if (nsHitSlime != null) { nsHitSlime.stop(); nsHitSlime.play(); } 
    }

    public static void playNsHitTreeSound() { 
        if (nsHitTree != null) { nsHitTree.stop(); nsHitTree.play(); } 
    }

    public static void playNsHitKnightSound() { 
        if (nsHitKnight != null) { nsHitKnight.stop(); nsHitKnight.play(); } 
    }
    public static void playNsFinalHitSound() { 
        if (nsFinalHit != null) { nsFinalHit.stop(); nsFinalHit.play(); } 
    }

    public static void playPlayerHitSound() { 
        if (playerHitS != null) { playerHitS.stop(); playerHitS.play(); } 
    }
    
    public static void playPlayerPowerUpSound() {
        if (playerPowerUp != null) { playerPowerUp.stop(); playerPowerUp.play(); }
    }

    public static void playThunderSound() {
        if (thunder != null) { thunder.stop(); thunder.play(); }
    }

    public static void playSwordPowerUpSound(double volume) {
        if (sPowerUp != null) {
            sPowerUp.stop();
            sPowerUp.setVolume(volume);
            sPowerUp.play();
        }
    }

    public static void playSlimeMoveSound() { 
        if (slimeMove != null) { slimeMove.stop(); slimeMove.play(); } 
    }
    public static void playKnightAtkSound() { 
        if (knightAtk != null) { knightAtk.stop(); knightAtk.play(); } 
    }
    public static void playKnightReadySound() { 
        if (knightReady != null) { knightReady.stop(); knightReady.play(); } 
    }
    public static void playTreeMovingSound() { 
        if (treeMoving != null) { treeMoving.stop(); treeMoving.play(); } 
    }
    public static void playTreeAtkSound() { 
        if (treeAtk != null) { treeAtk.stop(); treeAtk.play(); } 
    }
}