package com.hust.game.audio;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    public static AudioClip nsMiss, nsHitSlime, nsHitKnight, nsFinalHit;
    public static AudioClip slimeMove, knightAtk, knightReady, treeMoving, treeAtk;

    public static void loadSounds() {
        nsMiss = loadSound("normal_sword_miss.wav");
        nsHitSlime = loadSound("normal_sword_hit_slime.wav");
        nsHitKnight = loadSound("normal_sword_hit_knight.wav");
        nsFinalHit = loadSound("normal_sword_final_hit.wav");
        
        slimeMove = loadSound("slime_move.wav");
        knightAtk = loadSound("knight_atk.wav");
        knightReady = loadSound("knight_ready.wav");
        treeMoving = loadSound("tree_moving.wav");
        treeAtk = loadSound("tree_atk.wav");
    }

    private static AudioClip loadSound(String fileName) {
        try {
            // Tải từ thư mục resources/sounds
            URL url = SoundManager.class.getResource("/sounds/" + fileName);
            if (url != null) {
                return new AudioClip(url.toString());
            } else {
                System.err.println("Không tìm thấy file âm thanh: /sounds/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    public static void playNsMissSound() { if (nsMiss != null) nsMiss.play(); }
    public static void playNsHitSlimeSound() { if (nsHitSlime != null) nsHitSlime.play(); }
    public static void playNsHitKnightSound() { if (nsHitKnight != null) nsHitKnight.play(); }
    public static void playNsFinalHitSound() { if (nsFinalHit != null) nsFinalHit.play(); }
    
    public static void playSlimeMoveSound() { if (slimeMove != null) slimeMove.play(); }
    public static void playKnightAtkSound() { if (knightAtk != null) knightAtk.play(); }
    public static void playKnightReadySound() { if (knightReady != null) knightReady.play(); }
    public static void playTreeMovingSound() { if (treeMoving != null) treeMoving.play(); }
    public static void playTreeAtkSound() { if (treeAtk != null) treeAtk.play(); }
}