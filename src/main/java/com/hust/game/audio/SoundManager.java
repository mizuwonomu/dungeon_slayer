package com.hust.game.audio;

import javafx.scene.media.AudioClip;
import java.net.URL;

public class SoundManager {
    public static AudioClip nsMiss, nsHitSlime, nsHitKnight, nsFinalHit, playerHitS, playerPowerUp, sPowerUp,nsHitTree, thunder;
    public static AudioClip slimeMove, knightAtk, knightReady, treeMoving, treeAtk;
    public static AudioClip witchCircleFollow, witchCircleExplode, witchSummon, witchDmgTaken, witchDied;
    public static AudioClip bossStart, bossTele1, bossTele2, bossSkill2, bossUlti;
    public static AudioClip btnHover, btnClick;
    public static AudioClip gateBurn;
    public static AudioClip pauseSound, unpauseSound;
    public static AudioClip transformSound;
    private static double sfxVolume = 1.0; // Âm lượng hiệu ứng
    private static double bgmVolume = 1.0; // Âm lượng nhạc nền

    public static double getSfxVolume() {
        return sfxVolume;
    }

    public static double getBgmVolume() {
        return bgmVolume;
    }

    public static void setBgmVolume(double volume) {
        bgmVolume = volume;
    }

    public static void setSfxVolume(double volume) {
        sfxVolume = volume;

        // Apply to all loaded clips
        applyVolume(nsMiss, 1.0);
        applyVolume(nsHitSlime, 1.0);
        applyVolume(nsHitKnight, 1.0);
        applyVolume(nsFinalHit, 1.0);
        applyVolume(nsHitTree, 1.0);

        applyVolume(playerPowerUp, 1.0);
        applyVolume(sPowerUp, 1.0);
        applyVolume(thunder, 1.0);
        applyVolume(playerHitS, 1.0);

        applyVolume(slimeMove, 0.4);
        applyVolume(knightAtk, 0.8);
        applyVolume(knightReady, 0.8);
        applyVolume(treeMoving, 0.4);
        applyVolume(treeAtk, 0.8);
        applyVolume(witchCircleFollow, 0.8);
        applyVolume(witchCircleExplode, 1.0);
        applyVolume(witchSummon, 0.8);
        applyVolume(witchDmgTaken, 1.0);
        applyVolume(witchDied, 1.0);
        applyVolume(bossStart, 1.0);
        applyVolume(bossTele1, 0.9);
        applyVolume(bossTele2, 0.9);
        applyVolume(bossSkill2, 1.0);
        applyVolume(bossUlti, 1.0);
        applyVolume(gateBurn, 1.0);
        applyVolume(btnHover, 0.8);
        applyVolume(btnClick, 1.0);
        applyVolume(pauseSound, 1.0);
        applyVolume(unpauseSound, 1.0);
    }

    private static void applyVolume(AudioClip clip, double baseVolume) {
        if (clip != null) {
            clip.setVolume(baseVolume * sfxVolume);
        }
    }

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
        witchCircleFollow = loadSound("witch_circle_folow.wav", 0.8);
        witchCircleExplode = loadSound("witch_circle_explore.wav", 1.0);
        witchSummon = loadSound("summon.wav", 0.8);
        witchDmgTaken = loadSound("witch_dmg_taken.wav", 1.0);
        witchDied = loadSound("witch_died.wav", 1.0);
        bossStart = loadSound("start.wav", 1.0);
        bossTele1 = loadSound("tele1.wav", 0.9);
        bossTele2 = loadSound("tele2.wav", 0.9);
        bossSkill2 = loadSound("skill2.wav", 1.0);
        bossUlti = loadSound("ulti.wav", 1.0);
        gateBurn = loadSound("gate_burn.wav", 1.0);
        btnHover = loadSound("button_hold.wav", 0.8);
        btnClick = loadSound("button_click.wav", 1.0);
        
        pauseSound = loadSound("pause.wav", 1.0);
        unpauseSound = loadSound("button_click.wav", 1.0); // Đổi sang dùng chung âm thanh với button click
        transformSound = loadSound("transform.wav", 1.0);
    }

    private static AudioClip loadSound(String fileName, double baseVolume) {
        try {
            URL url = SoundManager.class.getResource("/sounds/" + fileName);
            if (url != null) {
                AudioClip clip = new AudioClip(url.toString());
                clip.setVolume(baseVolume * sfxVolume); // 
                return clip;
            } else {
                System.err.println("Không tìm thấy file âm thanh: /sounds/" + fileName);
            }
        } catch (Exception e) {
            System.err.println("Lỗi load âm thanh " + fileName + ": " + e.getMessage());
        }
        return null;
    }

    public static void playTransformSound() {
        if (transformSound != null) { transformSound.stop(); transformSound.play(); }
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
    
    public static void playWitchCircleFollowSound() {
        if (witchCircleFollow != null) { witchCircleFollow.stop(); witchCircleFollow.play(); }
    }

    public static void playWitchCircleExplodeSound() {
        if (witchCircleExplode != null) { witchCircleExplode.stop(); witchCircleExplode.play(); }
    }

    public static void playWitchSummonSound() {
        if (witchSummon != null) { witchSummon.stop(); witchSummon.play(); }
    }

    public static void playWitchDmgTakenSound() {
        if (witchDmgTaken != null) { witchDmgTaken.stop(); witchDmgTaken.play(); }
    }

    public static void playWitchDiedSound() {
        if (witchDied != null) { witchDied.stop(); witchDied.play(); }
    }

    public static void playGateBurnSound() {
        if (gateBurn != null) { gateBurn.stop(); gateBurn.play(); }
    }

    public static void playButtonHoverSound() {
        if (btnHover != null) { btnHover.stop(); btnHover.play(); }
    }
    public static void playButtonClickSound() {
        if (btnClick != null) { btnClick.stop(); btnClick.play(); }
    }

    public static void playPauseSound() {
        if (pauseSound != null) { pauseSound.stop(); pauseSound.play(); }
    }

    public static void playUnpauseSound() {
        if (unpauseSound != null) { unpauseSound.stop(); unpauseSound.play(); }
    }

    public static void playBossStartSound() {
        if (bossStart != null) { bossStart.stop(); bossStart.play(); }
    }

    public static void playBossTeleport1Sound() {
        if (bossTele1 != null) { bossTele1.stop(); bossTele1.play(); }
    }

    public static void playBossTeleport2Sound() {
        if (bossTele2 != null) { bossTele2.stop(); bossTele2.play(); }
    }

    public static void playBossSkill2Sound() {
        if (bossSkill2 != null) { bossSkill2.stop(); bossSkill2.play(); }
    }

    public static void playBossUltimateSound() {
        if (bossUlti != null) { bossUlti.stop(); bossUlti.play(); }
    }

    public static void stopEnemySounds() {
        if (slimeMove != null) { slimeMove.stop(); }
        if (knightAtk != null) { knightAtk.stop(); }
        if (knightReady != null) { knightReady.stop(); }
        if (treeMoving != null) { treeMoving.stop(); }
        if (treeAtk != null) { treeAtk.stop(); }
        if (bossStart != null) { bossStart.stop(); }
        if (bossTele1 != null) { bossTele1.stop(); }
        if (bossTele2 != null) { bossTele2.stop(); }
        if (bossSkill2 != null) { bossSkill2.stop(); }
        if (bossUlti != null) { bossUlti.stop(); }
    }

    public static void stopGameplaySounds() {
        if (nsMiss != null) { nsMiss.stop(); }
        if (nsHitSlime != null) { nsHitSlime.stop(); }
        if (nsHitKnight != null) { nsHitKnight.stop(); }
        if (nsFinalHit != null) { nsFinalHit.stop(); }
        if (nsHitTree != null) { nsHitTree.stop(); }
        if (playerHitS != null) { playerHitS.stop(); }
        if (playerPowerUp != null) { playerPowerUp.stop(); }
        if (sPowerUp != null) { sPowerUp.stop(); }
        if (thunder != null) { thunder.stop(); }
        if (slimeMove != null) { slimeMove.stop(); }
        if (knightAtk != null) { knightAtk.stop(); }
        if (knightReady != null) { knightReady.stop(); }
        if (treeMoving != null) { treeMoving.stop(); }
        if (treeAtk != null) { treeAtk.stop(); }
        if (witchDmgTaken != null) { witchDmgTaken.stop(); }
        if (witchDied != null) { witchDied.stop(); }
        if (gateBurn != null) { gateBurn.stop(); }
        if (bossStart != null) { bossStart.stop(); }
        if (bossTele1 != null) { bossTele1.stop(); }
        if (bossTele2 != null) { bossTele2.stop(); }
        if (bossSkill2 != null) { bossSkill2.stop(); }
        if (bossUlti != null) { bossUlti.stop(); }
    }
}