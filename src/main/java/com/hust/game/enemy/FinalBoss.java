package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.Direction;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.player.Player;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FinalBoss extends Enemy {
    // Bộ trạng thái của boss: mỗi thời điểm chỉ chạy một trạng thái để tránh spam/chồng chiêu.
    private enum BossState {
        WAITING,
        INTRO,
        CHASE,
        SKILL1_TELEPORT,
        SKILL1_SLASH1,
        SKILL2_CAST,
        RAGE_TELEGRAPH,
        RAGE_DRAGON,
        RECOVERY,
        DEAD
    }

    private static final int BOSS_MAX_HP = 500;
    private static final int SKILL1_DAMAGE = 30;
    private static final int SKILL2_DAMAGE = 20;
    private static final int RAGE_DAMAGE = 50;

    // Số frame phải khớp số cột trong từng spritesheet.
    private static final int IDLE_FRAMES = 5;
    private static final int ATTACK_1_FRAMES = 21;
    private static final int ATTACK_2_FRAMES = 40;
    private static final int TELE_FRAMES = 11;
    private static final int WALK_FRAMES = 10;
    private static final int ULTIMATE_FRAMES = 50;
    private static final int DRAGON_FRAMES = 3;
    private static final int SKILL2_ITEM_FRAMES = 2;
    private static final double ULTIMATE_SAFE_FRAME_SIZE = 160.0;
    private static final double ULTIMATE_FRAME_CACHE_SIZE = 384.0;
    private static final int BOSS_FRAME_DELAY = 6;

    // Bộ đếm thời gian tính theo 60 FPS: 120 frame = 2 giây, 30 frame = 0.5 giây.
    private static final int ACTION_DELAY_FRAMES = 120;
    private static final int SKILL1_TELEPORT_FRAMES = TELE_FRAMES * BOSS_FRAME_DELAY;
    private static final int SKILL1_SLASH_FRAMES = ATTACK_1_FRAMES * BOSS_FRAME_DELAY;
    private static final int SKILL1_RECOVERY_FRAMES = ACTION_DELAY_FRAMES;
    private static final int SKILL1_HIT_FRAME_1 = 5;
    private static final int SKILL1_HIT_FRAME_2 = 9;
    private static final int SKILL1_HITBOX_ACTIVE_FRAMES = 12;
    private static final int SKILL2_CAST_FRAMES = ATTACK_2_FRAMES * BOSS_FRAME_DELAY;
    private static final int SKILL2_RECOVERY_FRAMES = ACTION_DELAY_FRAMES;
    private static final int RAGE_TELEGRAPH_FRAMES = ULTIMATE_FRAMES * BOSS_FRAME_DELAY;

    private static final int SKILL2_VOLLEY_1_FRAME = 12;
    private static final int SKILL2_VOLLEY_2_FRAME = 16;
    private static final int SKILL2_VOLLEY_3_FRAME = 24;
    private static final int RAGE_DRAGON_RELEASE_FRAME = 32;

    private static final int SKILL1_COOLDOWN_FRAMES = 330;
    private static final int SKILL2_COOLDOWN_FRAMES = 300;

    private static final double SKILL1_RADIUS_MULTIPLIER = 1.5;
    private static final double SKILL2_ITEM_RENDER_SIZE = 96.0;
    private static final double SKILL2_ITEM_HIT_RADIUS = 30.0;
    private static final double SKILL2_PROJECTILE_SPEED = 9.0;
    private static final double SKILL2_PROJECTILE_MAX_DISTANCE = GameConstants.TILE_SIZE * 40.0;
    private static final double SKILL2_PROJECTILE_ROTATION_OFFSET_DEGREES = 60.0;
    private static final double SKILL2_SPREAD_ANGLE_DEGREES = 60.0;
    private static final double SKILL2_BOSS_RENDER_SCALE = 2.0;
    private static final double ULTIMATE_BOSS_RENDER_SCALE = 4.0;
    private static final double DRAGON_SPEED = 20.0;
    private static final int DRAGON_MAX_FLIGHT_FRAMES = 120;
    private static final int DRAGON_IMPACT_DELAY_FRAMES = 15;
    private static final double DRAGON_DIVE_TILT_DEGREES = 24.0;
    private static final double DRAGON_RENDER_WIDTH = 288.0;
    private static final double DRAGON_RENDER_HEIGHT = 198.0;
    private static final double WANDER_SPEED = 1.0;

    private static final int DANGER_ZONE_FRAMES = 10;
    private static final int DANGER_ZONE_FRAME_DELAY = 6; // 100ms mỗi frame (6 frame game tại 60 FPS)
    private static final double DANGER_ZONE_WIDTH = 236.0;
    private static final double DANGER_ZONE_HEIGHT = 145.0;

    // Tâm sàn đấu của map Level 3 (Nằm giữa điểm spawn của Player và Boss tại Y=19)
    private static final double ARENA_CENTER_X = GameConstants.TILE_SIZE * 15.5;
    private static final double ARENA_CENTER_Y = GameConstants.TILE_SIZE * 19.0;

    private final Image idleSprite;
    private final int idleFrames;
    private final Image attackSprite1;
    private final Image attackSprite2;
    private final Image teleSprite;
    private final Image walkSprite;
    private final Image ultiSprite;
    private final Image[] ultiFrameSprites;
    private final Image dragonSprite;
    private final Image skill2Item2Sprite;
    private final Image explosionSprite;
    private final Image dangerZoneSprite;

    private BossState bossState = BossState.WAITING;
    private int stateTimer = 0;
    private int skill1CooldownTimer = 120;
    private int skill2CooldownTimer = 80;
    private int attackCountSinceRage = 0;
    private int skillPatternStep = 0;
    private boolean rageQueued = false;
    private boolean hasFiredSkill2Volley1 = false;
    private boolean hasFiredSkill2Volley2 = false;
    private boolean hasFiredSkill2Volley3 = false;
    private boolean hasSpawnedSkill1Hitbox1 = false;
    private boolean hasSpawnedSkill1Hitbox2 = false;
    private boolean hasSpawnedSkill1Hitbox = false;
    private boolean hasTeleportedThisSkill = false;
    private boolean hasSoundedSkill1 = false;
    private boolean hasSoundedSkill2 = false;
    private boolean hasSoundedTele = false;
    private boolean hasSoundedWhoosh = false;
    private boolean hasSoundedUltiSwing = false;
    private boolean hasSoundedDragonRoar = false;

    private double teleportTargetX = 0;
    private double teleportTargetY = 0;
    private double lastAimX = 1;
    private double lastAimY = 0;
    private double recoveryWanderX = 0;
    private double recoveryWanderY = 0;
    private int recoveryWanderTimer = 0;
    private double rageTargetX = ARENA_CENTER_X;
    private double rageTargetY = ARENA_CENTER_Y;

    private int dangerZoneFrameIndex = 0;
    private int dangerZoneAnimTimer = 0;

    private CircleHitbox activeHitbox;
    private DragonStrike dragonStrike;
    private final List<Skill2SlashProjectile> skill2Projectiles = new ArrayList<>();

    public FinalBoss(double x, double y, Image spriteSheet, int numFrames,
            double renderWidth, double renderHeight, Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);

        // Chỉ số chính của boss.
        this.maxHp = BOSS_MAX_HP;
        this.hp = maxHp;
        this.damage = 0;
        this.knockback = 1;
        this.speed = 1.15;
        this.isImmobile = false;
        this.animationDelay = BOSS_FRAME_DELAY;

        // Boss tự load các animation riêng. spriteSheet truyền vào chỉ làm ảnh dự phòng.
        Image bossIdleSprite = loadOptionalImage("/assets/enemy/boss_idle.png");
        this.idleSprite = bossIdleSprite != null ? bossIdleSprite : spriteSheet;
        this.idleFrames = bossIdleSprite != null ? IDLE_FRAMES : Math.max(1, numFrames);
        this.attackSprite1 = loadOptionalImage("/assets/enemy/boss_atk_1.png");
        this.attackSprite2 = loadOptionalImage("/assets/enemy/boss_atk_2.png");
        this.teleSprite = loadOptionalImage("/assets/enemy/boss_tele.png");
        this.walkSprite = loadOptionalImage("/assets/enemy/boss_walk.png");
        // Ảnh ulti gốc rộng 38400px. Nếu vẽ trực tiếp, JavaFX Canvas có thể ném NullPointerException do vượt giới hạn texture GPU.
        this.ultiSprite = loadOptionalImage("/assets/enemy/boss_ultimate.png",
                ULTIMATE_SAFE_FRAME_SIZE * ULTIMATE_FRAMES, ULTIMATE_SAFE_FRAME_SIZE);
        this.ultiFrameSprites = loadOptionalFrames("/assets/enemy/boss_ultimate.png",
                ULTIMATE_FRAMES, (int) ULTIMATE_FRAME_CACHE_SIZE, (int) ULTIMATE_FRAME_CACHE_SIZE);
        this.dragonSprite = loadOptionalImage("/assets/enemy/dragon.png");
        this.skill2Item2Sprite = loadOptionalImage("/assets/enemy/atk_2_item_2.png");
        this.explosionSprite = loadOptionalImage("/assets/enemy/explosion.png");
        this.dangerZoneSprite = loadOptionalImage("/assets/enemy/danger_zone.png");
        
        // Pre-bake: Tạo sẵn ảnh chớp trắng khi nhận đòn
        preBakeWhiteSprite(this.idleSprite);
        preBakeWhiteSprite(this.attackSprite1);
        preBakeWhiteSprite(this.attackSprite2);
        preBakeWhiteSprite(this.teleSprite);
        preBakeWhiteSprite(this.walkSprite);
    }

    @Override
    public void update() {
        // Boss luôn active để không bị cơ chế tắt quái ngoài camera làm ngắt AI ở phòng boss.
        this.isActive = true;

        if (this.hp <= 0) {
            updateDeath();
            return;
        }

        this.lastX = this.x;
        this.lastY = this.y;

        // Bộ đếm/hồi chiêu/vật thể bay được update trước trạng thái hiện tại.
        updateCommonTimers();
        if (shouldTrackPlayer()) {
            updateFacingToPlayer();
        } else {
            isFlipped = lastAimX < 0;
        }
        updateProjectiles();

        switch (bossState) {
            case WAITING -> updateWaiting();
            case INTRO -> updateIntro();
            case CHASE -> updateChase();
            case SKILL1_TELEPORT -> updateSkill1Teleport();
            case SKILL1_SLASH1 -> updateActiveCircleHitbox(BossState.RECOVERY, SKILL1_RECOVERY_FRAMES);
            case SKILL2_CAST -> updateSkill2Cast();
            case RAGE_TELEGRAPH -> updateRageTelegraph();
            case RAGE_DRAGON -> updateRageDragon();
            case RECOVERY -> updateRecovery();
            case DEAD -> updateDeath();
        }

        animateCurrentSprite();
    }

    private void updateCommonTimers() {
        // Giảm cooldown mỗi frame.
        if (flashTimer > 0) flashTimer--;
        if (skill1CooldownTimer > 0) skill1CooldownTimer--;
        if (skill2CooldownTimer > 0) skill2CooldownTimer--;

        // Knockback nhẹ để boss không bị stun-lock.
        if (kbTimer > 0) {
            double multiplier = kbTimer / 5.0;
            kbTimer--;
            x += kbVectorX * multiplier;
            y += kbVectorY * multiplier;
        }
    }

    private void updateWaiting() {
        setSprite(idleSprite, idleFrames, BOSS_FRAME_DELAY);
        
        // Đợi player bước vào vùng kích hoạt (Bán kính 7 ô)
        if (distanceToPlayerCenter() <= GameConstants.TILE_SIZE * 7.0) {
            changeState(BossState.INTRO, 370);
            com.hust.game.main.App.setCutsceneActive(true);
            com.hust.game.main.App.showDialog("Hừ, có cố gắng,\nnhưng đây là thứ thuộc về ta!", 300);
            com.hust.game.audio.SoundManager.playBossStartSound();
        }
    }

    private void updateIntro() {
        setSprite(idleSprite, idleFrames, BOSS_FRAME_DELAY);
        
        if (--stateTimer <= 0) {
            com.hust.game.main.App.setCutsceneActive(false);
            changeState(BossState.CHASE, 0);
        }
    }

    private void updateChase() {
        // State mac dinh: chon skill theo pattern co dinh, khong luot sat player truoc khi danh.
        setSprite(idleSprite, idleFrames, BOSS_FRAME_DELAY);

        if (rageQueued) {
            startRage();
            return;
        }

        // Boss dung yen o CHASE va chon skill ngay, tranh tao cam giac dang luot.

        // Pattern co dinh: Skill 1 -> Skill 1 -> Skill 2.
        if (skillPatternStep == 2) {
            startSkill2();
        } else {
            startSkill1();
        }
    }

    private void updateSkill2Cast() {
        // Skill 2: đứng im chạy đủ boss_atk_2, lần lượt bắn 1/2/3 volley ở frame 12/16/24.
        setSprite(attackSprite2 != null ? attackSprite2 : idleSprite,
                attackSprite2 != null ? ATTACK_2_FRAMES : idleFrames, BOSS_FRAME_DELAY);

        // Phát âm thanh đúng vào lúc bắt đầu hoạt ảnh chém Skill 2
        if (!hasSoundedSkill2) {
            com.hust.game.audio.SoundManager.playBossSkill2Sound();
            hasSoundedSkill2 = true;
        }

        if (!hasFiredSkill2Volley1 && frameIndex >= SKILL2_VOLLEY_1_FRAME) {
            spawnSkill2Volley(1);
            hasFiredSkill2Volley1 = true;
            com.hust.game.audio.SoundManager.playSwordSwing2Sound();
        }
        if (!hasFiredSkill2Volley2 && frameIndex >= SKILL2_VOLLEY_2_FRAME) {
            spawnSkill2Volley(2);
            hasFiredSkill2Volley2 = true;
            com.hust.game.audio.SoundManager.playSwordSwing2Sound();
        }
        if (!hasFiredSkill2Volley3 && frameIndex >= SKILL2_VOLLEY_3_FRAME) {
            spawnSkill2Volley(3);
            hasFiredSkill2Volley3 = true;
            com.hust.game.audio.SoundManager.playSwordSwing2Sound();
        }

        if (--stateTimer <= 0) {
            skill2CooldownTimer = SKILL2_COOLDOWN_FRAMES;
            finishSkillOrStartRage(SKILL2_RECOVERY_FRAMES);
        }
    }

    private void updateRageTelegraph() {
        // Chiêu nộ dùng boss_ultimate.png. Cổng mở ở frame 24-45, rồng chui ra từ cổng ở frame 24.
        setSprite(ultiSprite != null ? ultiSprite : idleSprite,
                ultiSprite != null ? ULTIMATE_FRAMES : idleFrames, BOSS_FRAME_DELAY);

        if (frameIndex < 25) {
            rageTargetX = playerCenterX();
            rageTargetY = playerCenterY();
            if (dragonStrike != null) {
                dragonStrike.updateTarget(rageTargetX, rageTargetY);
            }
        }

        if (dangerZoneFrameIndex < DANGER_ZONE_FRAMES - 1) {
            dangerZoneAnimTimer++;
            if (dangerZoneAnimTimer >= DANGER_ZONE_FRAME_DELAY) {
                dangerZoneAnimTimer = 0;
                dangerZoneFrameIndex++;
            }
        }

        if (!hasSoundedWhoosh && frameIndex >= 1) {
            com.hust.game.audio.SoundManager.playWhooshSound();
            hasSoundedWhoosh = true;
        }
        
        if (!hasSoundedUltiSwing && frameIndex >= 22) {
            com.hust.game.audio.SoundManager.playSwordSwing2Sound();
            hasSoundedUltiSwing = true;
        }

        if (!hasSoundedDragonRoar && frameIndex >= 31) {
            com.hust.game.audio.SoundManager.playDragonRoarSound();
            hasSoundedDragonRoar = true;
        }

        if (dragonStrike == null && frameIndex >= RAGE_DRAGON_RELEASE_FRAME) {
            // Xác định góc xuất hiện của rồng (Cánh cổng) dựa trên việc boss có lật mặt hay không
            double offsetScale = ULTIMATE_BOSS_RENDER_SCALE / 2.0;
            double spawnX = isFlipped ? centerX() + renderWidth * offsetScale : centerX() - renderWidth * offsetScale;
            double spawnY = centerY() - renderHeight * offsetScale;
            dragonStrike = new DragonStrike(spawnX, spawnY, rageTargetX, rageTargetY);
        }
        if (dragonStrike != null && !dragonStrike.isDone()) {
            dragonStrike.update();
        }

        // Giảm âm lượng ulti_ready khi rồng bắt đầu xuất hiện
        if (frameIndex >= RAGE_DRAGON_RELEASE_FRAME) {
            double fade = 1.0 - (double)(frameIndex - RAGE_DRAGON_RELEASE_FRAME) / (ULTIMATE_FRAMES - RAGE_DRAGON_RELEASE_FRAME);
            fade = Math.max(0.0, fade);
            if (com.hust.game.audio.SoundManager.ultiReady != null) {
                com.hust.game.audio.SoundManager.ultiReady.setVolume(fade * com.hust.game.audio.SoundManager.getSfxVolume());
            }
        }

        if (--stateTimer <= 0) {
            if (dragonStrike != null && !dragonStrike.isDone()) {
                changeState(BossState.RAGE_DRAGON, 0);
            } else {
                dragonStrike = null;
                changeState(BossState.RECOVERY, ACTION_DELAY_FRAMES);
            }
        }
    }

    private void updateRageDragon() {
        // Sau cảnh báo, vật thể nộ rơi xuống và nổ tại rageTarget.
        if (dragonStrike != null) {
            dragonStrike.update();
            if (dragonStrike.isDone()) {
                dragonStrike = null;
                changeState(BossState.RECOVERY, ACTION_DELAY_FRAMES);
            }
        } else {
            changeState(BossState.CHASE, 0);
        }
    }

    private void updateRecovery() {
        // Delay 2 giây giữa các chiêu: boss đi lang thang bằng animation walk.
        setSprite(walkSprite != null ? walkSprite : idleSprite,
                walkSprite != null ? WALK_FRAMES : idleFrames, BOSS_FRAME_DELAY);

        if (!isImmobile) {
            if (recoveryWanderTimer <= 0) {
                chooseRecoveryWanderDirection();
            }

            if (Math.abs(recoveryWanderX) > 0.1) {
                isFlipped = recoveryWanderX < 0;
            }
            moveByWithCollision(recoveryWanderX * WANDER_SPEED, recoveryWanderY * WANDER_SPEED);
            recoveryWanderTimer--;
        }

        if (--stateTimer <= 0) {
            changeState(BossState.CHASE, 0);
        }
    }

    private void updateTimedState(BossState nextState, int nextTimer, Runnable onEnterNextState) {
        // Hàm phụ cho các trạng thái chỉ cần đếm thời gian rồi chuyển sang trạng thái kế tiếp.
        if (--stateTimer <= 0) {
            changeState(nextState, nextTimer);
            if (onEnterNextState != null) {
                onEnterNextState.run();
            }
        }
    }

    private void updateActiveCircleHitbox(BossState nextState, int nextTimer) {
        // Skill 1 chỉ bật hitbox đúng frame chém chính, tránh player bị dính damage trễ sau animation.
        setSprite(attackSprite1 != null ? attackSprite1 : idleSprite,
                attackSprite1 != null ? ATTACK_1_FRAMES : idleFrames, BOSS_FRAME_DELAY);

        // Phát âm thanh đúng vào lúc bắt đầu hoạt ảnh chém (được gán nhảy cóc frameIndex = 5 từ trước)
        if (!hasSoundedSkill1 && frameIndex >= 5) {
            if (Math.random() > 0.5) {
                com.hust.game.audio.SoundManager.playBossTeleport1Sound();
            } else {
                com.hust.game.audio.SoundManager.playBossTeleport2Sound();
            }
            hasSoundedSkill1 = true;
        }

        if (!hasSpawnedSkill1Hitbox1 && frameIndex >= SKILL1_HIT_FRAME_1) {
            activeHitbox = createSlashHitbox(
                    SKILL1_DAMAGE,
                    playerZoneRadius() * SKILL1_RADIUS_MULTIPLIER,
                    SKILL1_HITBOX_ACTIVE_FRAMES);
            hasSpawnedSkill1Hitbox1 = true;
            com.hust.game.audio.SoundManager.playSwordSwingSound();
        }
        if (!hasSpawnedSkill1Hitbox2 && frameIndex >= SKILL1_HIT_FRAME_2) {
            activeHitbox = createSlashHitbox(
                    SKILL1_DAMAGE,
                    playerZoneRadius() * SKILL1_RADIUS_MULTIPLIER,
                    SKILL1_HITBOX_ACTIVE_FRAMES);
            hasSpawnedSkill1Hitbox2 = true;
            com.hust.game.audio.SoundManager.playSwordSwing1Sound();
        }

        if (activeHitbox != null) {
            activeHitbox.update();
            if (activeHitbox.isExpired()) {
                activeHitbox = null;
            }
        }

        if (--stateTimer <= 0) {
            activeHitbox = null;
            if (nextState == BossState.RECOVERY) {
                finishSkillOrStartRage(nextTimer);
            } else {
                changeState(nextState, nextTimer);
            }
        }
    }

    private void updateProjectiles() {
        // Cập nhật tất cả vệt chém bay và xóa vật thể bay đã kết thúc.
        Iterator<Skill2SlashProjectile> iterator = skill2Projectiles.iterator();
        while (iterator.hasNext()) {
            Skill2SlashProjectile slash = iterator.next();
            slash.update();
            if (slash.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void updateDeath() {
        // Dọn toàn bộ hitbox/effect khi boss chết.
        bossState = BossState.DEAD;
        activeHitbox = null;
        dragonStrike = null;
        skill2Projectiles.clear();
        if (flashTimer > 0) {
            flashTimer--;
        }
        animateCurrentSprite();
    }

    private void updateSkill1Teleport() {
        setSprite(teleSprite != null ? teleSprite : idleSprite,
                teleSprite != null ? TELE_FRAMES : idleFrames, BOSS_FRAME_DELAY);

        if (!hasSoundedTele) {
            com.hust.game.audio.SoundManager.playTeleSound();
            hasSoundedTele = true;
        }

        // Dịch chuyển ở giữa animation (ví dụ frame 5), lúc boss đang mờ/biến mất
        if (!hasTeleportedThisSkill && frameIndex >= 5) {
            prepareTeleportNearPlayer();
            x = teleportTargetX;
            y = teleportTargetY;
            lockAimToPlayer();
            hasTeleportedThisSkill = true;
        }

        if (--stateTimer <= 0) {
            hasSpawnedSkill1Hitbox1 = false;
            hasSpawnedSkill1Hitbox2 = false;

            // Cắt đi 5 frame thời gian của state vì đã nhảy cóc frame animation
            changeState(BossState.SKILL1_SLASH1, SKILL1_SLASH_FRAMES - 5 * BOSS_FRAME_DELAY);
            setSprite(attackSprite1 != null ? attackSprite1 : idleSprite,
                    attackSprite1 != null ? ATTACK_1_FRAMES : idleFrames, BOSS_FRAME_DELAY);
            this.frameIndex = 5; // Bắt đầu hoạt ảnh trực tiếp ở frame 5
        }
    }

    private void startSkill1() {
        // Skill 1: Chạy animation teleport, biến đến gần player rồi chém.
        registerBossAttackAction();
        advanceSkillPattern();
        skill1CooldownTimer = SKILL1_COOLDOWN_FRAMES;
        hasTeleportedThisSkill = false;
        hasSoundedSkill1 = false;
        hasSoundedTele = false;

        changeState(BossState.SKILL1_TELEPORT, SKILL1_TELEPORT_FRAMES);
    }

    private void startSkill2() {
        // Bắt đầu Skill 2: đứng im và bắn đủ 1/2/3 volley trong cùng một lần cast.
        registerBossAttackAction();
        advanceSkillPattern();
        lockAimToPlayer();
        hasFiredSkill2Volley1 = false;
        hasFiredSkill2Volley2 = false;
        hasFiredSkill2Volley3 = false;
        skill2CooldownTimer = SKILL2_COOLDOWN_FRAMES;
        hasSoundedSkill2 = false;
        changeState(BossState.SKILL2_CAST, SKILL2_CAST_FRAMES);
    }

    private void spawnSkill2Volley(int volleyCount) {
        // Tất cả volley của Skill 2 dùng chung atk_2_item_2 để hình ảnh đồng bộ.
        double[] aim = directionToPlayerNow();
        Image projectileSprite = skill2Item2Sprite;

        // Rung màn hình nhẹ khi phóng ra mỗi đợt kiếm khí
        com.hust.game.main.App.triggerScreenShake(6, 0.3);

        if (volleyCount == 1) {
            spawnSkill2Projectile(aim[0], aim[1], projectileSprite);
        } else if (volleyCount == 2) {
            spawnSkill2Projectile(aim[0], aim[1], projectileSprite);
            spawnSkill2Projectile(-aim[0], -aim[1], projectileSprite);
        } else {
            double[] left = rotateVector(aim[0], aim[1], Math.toRadians(SKILL2_SPREAD_ANGLE_DEGREES));
            double[] right = rotateVector(aim[0], aim[1], Math.toRadians(-SKILL2_SPREAD_ANGLE_DEGREES));
            spawnSkill2Projectile(aim[0], aim[1], projectileSprite);
            spawnSkill2Projectile(left[0], left[1], projectileSprite);
            spawnSkill2Projectile(right[0], right[1], projectileSprite);
        }
    }

    private void spawnSkill2Projectile(double dirX, double dirY, Image projectileSprite) {
        double spawnX = centerX() + dirX * renderWidth * 0.35;
        double spawnY = centerY() + dirY * renderHeight * 0.35;
        skill2Projectiles.add(new Skill2SlashProjectile(spawnX, spawnY, dirX, dirY, projectileSprite));
    }

    private void startRage() {
        // Chiêu nộ khóa vị trí hiện tại của player để vẽ cảnh báo rồi mới rơi xuống.
        lockAimToPlayer();
        rageTargetX = playerCenterX();
        rageTargetY = playerCenterY();
        dangerZoneFrameIndex = 0;
        dangerZoneAnimTimer = 0;
        dragonStrike = null;
        rageQueued = false;
        hasSoundedWhoosh = false;
        hasSoundedUltiSwing = false;
        hasSoundedDragonRoar = false;
        com.hust.game.audio.SoundManager.playBossUltimateSound();
        com.hust.game.audio.SoundManager.playUltiReadySound();
        if (com.hust.game.audio.SoundManager.ultiReady != null) {
            com.hust.game.audio.SoundManager.ultiReady.setVolume(1.0 * com.hust.game.audio.SoundManager.getSfxVolume());
        }
        changeState(BossState.RAGE_TELEGRAPH, RAGE_TELEGRAPH_FRAMES);
    }

    private void registerBossAttackAction() {
        // Sau mỗi 5 hành động bất kỳ, boss xếp hàng chiêu nộ. Chiêu nộ chạy khi boss về CHASE.
        attackCountSinceRage++;
        if (attackCountSinceRage >= 5) {
            attackCountSinceRage = 0;
            rageQueued = true;
        }
    }

    private void advanceSkillPattern() {
        skillPatternStep = (skillPatternStep + 1) % 3;
    }

    private void finishSkillOrStartRage(int recoveryFrames) {
        // Nếu skill vừa rồi là skill thứ 5, bỏ recovery để chiêu nộ ra ngay sau khi skill kết thúc.
        if (rageQueued) {
            startRage();
        } else {
            changeState(BossState.RECOVERY, recoveryFrames);
        }
    }

    private void changeState(BossState newState, int timer) {
        // Mỗi lần đổi state đều reset animation để frame đầu khớp với chiêu mới.
        bossState = newState;
        stateTimer = timer;
        animationTimer = 0;
        frameIndex = 0;

        if (newState == BossState.RECOVERY) {
            chooseRecoveryWanderDirection();
        }
    }

    private void moveTowardPlayer(double distance) {
        // Di chuyển thường có kiểm tra va chạm để boss không đi xuyên tường.
        double dx = playerCenterX() - centerX();
        double dy = playerCenterY() - centerY();
        if (distance <= 0.0001) return;

        moveX = (dx / distance) * speed;
        moveY = (dy / distance) * speed;
        moveByWithCollision(moveX, moveY);
    }

    private void updateFacingToPlayer() {
        // Cập nhật hướng nhìn và vector aim liên tục khi boss đang theo dõi player.
        double dx = playerCenterX() - centerX();
        double dy = playerCenterY() - centerY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > 0.0001) {
            lastAimX = dx / distance;
            lastAimY = dy / distance;
        }
        isFlipped = dx < 0;
    }

    private boolean shouldTrackPlayer() {
        // Một số trạng thái phải khóa hướng từ lúc vận chiêu, nên không cập nhật aim liên tục.
        return bossState == BossState.CHASE
                || bossState == BossState.RECOVERY
                || bossState == BossState.WAITING
                || bossState == BossState.INTRO;
    }

    // Cho phép App.java kiểm tra xem Boss đã qua đoạn hội thoại mở đầu chưa
    public boolean hasStartedCombat() {
        return bossState != BossState.WAITING && bossState != BossState.INTRO;
    }

    private void lockAimToPlayer() {
        // Khóa hướng từ boss tới player tại đúng thời điểm bắt đầu chiêu.
        double dx = playerCenterX() - centerX();
        double dy = playerCenterY() - centerY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.0001) {
            lastAimX = dx / distance;
            lastAimY = dy / distance;
        }
        isFlipped = lastAimX < 0;
    }

    private double[] directionToPlayerNow() {
        // Lấy hướng mới nhất từ boss tới player để mỗi nhịp Skill 2 có thể nhắm lại.
        double dx = playerCenterX() - centerX();
        double dy = playerCenterY() - centerY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 0.0001) {
            return new double[]{lastAimX, lastAimY};
        }
        return new double[]{dx / distance, dy / distance};
    }

    private double[] rotateVector(double x, double y, double angle) {
        // Xoay vector để tạo pattern tam giác đều cho nhịp 3 vệt chém.
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }

    private void prepareTeleportNearPlayer() {
        // Teleport quét quanh player và chỉ nhận điểm có vùng thân boss không đè tile solid.
        double playerRadius = playerZoneRadius();
        double stopDistance = playerRadius + renderWidth * 0.52;
        double baseAngle = Math.atan2(-lastAimY, -lastAimX);

        for (int ring = 0; ring < 10; ring++) {
            double candidateDistance = stopDistance + ring * playerRadius * 0.5;
            for (int step = 0; step <= 18; step++) {
                double offset = Math.toRadians(step * 10.0);
                if (trySetTeleportTarget(baseAngle + offset, candidateDistance)) {
                    return;
                }
                if (step > 0 && trySetTeleportTarget(baseAngle - offset, candidateDistance)) {
                    return;
                }
            }
        }

        // Nếu quanh player đều kín, boss giữ nguyên vị trí để không bị nhét vào tile solid.
        teleportTargetX = x;
        teleportTargetY = y;
    }

    private boolean trySetTeleportTarget(double angle, double distanceFromPlayer) {
        double candidateCenterX = playerCenterX() + Math.cos(angle) * distanceFromPlayer;
        double candidateCenterY = playerCenterY() + Math.sin(angle) * distanceFromPlayer;
        double candidateX = candidateCenterX - renderWidth / 2.0;
        double candidateY = candidateCenterY - renderHeight / 2.0;

        if (!canTeleportOccupy(candidateX, candidateY)) {
            return false;
        }

        teleportTargetX = candidateX;
        teleportTargetY = candidateY;
        return true;
    }

    private boolean canTeleportOccupy(double nextX, double nextY) {
        if (collisionChecker == null) {
            return true;
        }

        Rectangle2D footBounds = collisionBoundaryAt(nextX, nextY);
        Rectangle2D bodyBounds = teleportBoundaryAt(nextX, nextY);
        double footCenterX = footBounds.getMinX() + footBounds.getWidth() / 2.0;
        double footCenterY = footBounds.getMinY() + footBounds.getHeight() / 2.0;

        if (!isTileAreaClear(footBounds, GameConstants.TILE_SIZE / 8.0)) {
            return false;
        }
        if (!isTileAreaClear(bodyBounds, GameConstants.TILE_SIZE / 6.0)) {
            return false;
        }
        if (!isTeleportCircleClear(footCenterX, footCenterY, playerZoneRadius())) {
            return false;
        }
        return !footBounds.intersects(targetPlayer.getCollisionBoundary());
    }

    private boolean isTileAreaClear(Rectangle2D bounds, double step) {
        int xSamples = Math.max(2, (int) Math.ceil(bounds.getWidth() / step) + 1);
        int ySamples = Math.max(2, (int) Math.ceil(bounds.getHeight() / step) + 1);

        for (int iy = 0; iy < ySamples; iy++) {
            double sampleY = bounds.getMinY() + (bounds.getHeight() - 1.0) * iy / (ySamples - 1.0);
            for (int ix = 0; ix < xSamples; ix++) {
                double sampleX = bounds.getMinX() + (bounds.getWidth() - 1.0) * ix / (xSamples - 1.0);
                if (collisionChecker.checkTile((int) sampleX, (int) sampleY)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTeleportCircleClear(double centerX, double centerY, double radius) {
        if (collisionChecker == null) {
            return true;
        }

        double safeRadius = Math.max(1.0, radius);
        if (collisionChecker.checkTile((int) centerX, (int) centerY)) {
            return false;
        }

        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0 * i / 16.0;
            double sampleX = centerX + Math.cos(angle) * safeRadius;
            double sampleY = centerY + Math.sin(angle) * safeRadius;
            if (collisionChecker.checkTile((int) sampleX, (int) sampleY)) {
                return false;
            }
        }
        return true;
    }

    private Rectangle2D teleportBoundaryAt(double entityX, double entityY) {
        double paddingX = renderWidth * 0.06;
        double paddingTop = renderHeight * 0.06;
        double paddingBottom = renderHeight * 0.04;
        return new Rectangle2D(
                entityX + paddingX,
                entityY + paddingTop,
                renderWidth - 2.0 * paddingX,
                renderHeight - paddingTop - paddingBottom);
    }

    private void chooseRecoveryWanderDirection() {
        // Khi đang quá gần player, boss ưu tiên lùi ra để player nhìn rõ delay.
        double dxFromPlayer = centerX() - playerCenterX();
        double dyFromPlayer = centerY() - playerCenterY();
        double distanceFromPlayer = Math.sqrt(dxFromPlayer * dxFromPlayer + dyFromPlayer * dyFromPlayer);

        if (distanceFromPlayer > 0.0001 && distanceFromPlayer < closePlayerWanderRange()) {
            recoveryWanderX = dxFromPlayer / distanceFromPlayer;
            recoveryWanderY = dyFromPlayer / distanceFromPlayer;
            recoveryWanderTimer = 30 + (int) (Math.random() * 45);
            return;
        }

        double dxToCenter = ARENA_CENTER_X - centerX();
        double dyToCenter = ARENA_CENTER_Y - centerY();
        double distanceToCenter = Math.sqrt(dxToCenter * dxToCenter + dyToCenter * dyToCenter);

        if (distanceToCenter > GameConstants.TILE_SIZE * 3.5) {
            recoveryWanderX = dxToCenter / distanceToCenter;
            recoveryWanderY = dyToCenter / distanceToCenter;
        } else {
            double angle = Math.random() * Math.PI * 2.0;
            recoveryWanderX = Math.cos(angle);
            recoveryWanderY = Math.sin(angle);
        }

        recoveryWanderTimer = 30 + (int) (Math.random() * 45);
    }

    private void moveByWithCollision(double dx, double dy) {
        // Chia chuyển động thành nhiều bước nhỏ để không xuyên qua tile solid khi đi nhanh.
        if (Math.abs(dx) < 0.0001 && Math.abs(dy) < 0.0001) {
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 8.0));
        double stepX = dx / steps;
        double stepY = dy / steps;

        for (int i = 0; i < steps; i++) {
            double nextX = x + stepX;
            if (canOccupy(nextX, y)) {
                x = nextX;
            } else {
                recoveryWanderTimer = 0;
            }

            double nextY = y + stepY;
            if (canOccupy(x, nextY)) {
                y = nextY;
            } else {
                recoveryWanderTimer = 0;
            }
        }
    }

    private boolean canOccupy(double nextX, double nextY) {
        // Kiểm tra 4 góc của hitbox chân với tile có va chạm.
        if (collisionChecker == null) {
            return true;
        }

        // Trừ 1 ở cạnh max để đứng đúng mép tile không bị tính sang tile kế bên.
        Rectangle2D bounds = collisionBoundaryAt(nextX, nextY);
        double bx = bounds.getMinX();
        double by = bounds.getMinY();
        double right = bounds.getMaxX() - 1.0;
        double bottom = bounds.getMaxY() - 1.0;

        return !collisionChecker.checkTile((int) bx, (int) by)
                && !collisionChecker.checkTile((int) right, (int) by)
                && !collisionChecker.checkTile((int) bx, (int) bottom)
                && !collisionChecker.checkTile((int) right, (int) bottom);
    }

    private Rectangle2D collisionBoundaryAt(double entityX, double entityY) {
        // Va chạm vật lý chỉ lấy phần chân, không lấy toàn bộ sprite lớn của boss.
        double w = renderWidth * 0.5;
        double h = renderHeight * 0.3;
        double bx = entityX + (renderWidth - w) / 2.0;
        double by = entityY + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }

    private CircleHitbox createSlashHitbox(int hitDamage, double radius, int life) {
        // Hitbox chém nằm phía trước boss theo hướng lastAim đã khóa.
        double offset = renderWidth * 0.55;
        return new CircleHitbox(
                centerX() + lastAimX * offset,
                centerY() + lastAimY * offset,
                radius,
                hitDamage,
                life
        );
    }

    private double playerZoneRadius() {
        // Bán kính vùng quanh player theo yêu cầu: player.renderWidth / 2.
        return targetPlayer.getRenderWidth() / 2.0;
    }

    private double closePlayerWanderRange() {
        // Khi recovery mà đứng quá sát player thì boss lùi nhẹ để người chơi đọc animation skill kế tiếp.
        return renderWidth * 0.55 + playerZoneRadius() * 2.0;
    }

    private double centerX() {
        return x + renderWidth / 2.0;
    }

    private double centerY() {
        return y + renderHeight / 2.0;
    }

    private double playerCenterX() {
        return targetPlayer.getX() + targetPlayer.getRenderWidth() / 2.0;
    }

    private double playerCenterY() {
        return targetPlayer.getY() + targetPlayer.getRenderHeight() / 2.0;
    }

    private double distanceToPlayerCenter() {
        double dx = playerCenterX() - centerX();
        double dy = playerCenterY() - centerY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void setSprite(Image sprite, int frames, int delay) {
        // Đổi spritesheet hiện tại và reset frame khi animation thay đổi.
        if (sprite == null) {
            sprite = idleSprite;
            frames = idleFrames;
        }
        if (sprite == null) {
            return;
        }

        int safeFrames = Math.max(1, frames);
        int safeDelay = Math.max(1, delay);

        if (this.spriteSheet != sprite || this.numFrames != safeFrames) {
            this.spriteSheet = sprite;
            this.numFrames = safeFrames;
            this.frameWidth = sprite.getWidth() / safeFrames;
            this.frameHeight = sprite.getHeight();
            this.frameIndex = 0;
            this.animationTimer = 0;
        }
        this.animationDelay = safeDelay;
    }

    private void animateCurrentSprite() {
        // Animation ngang một hàng: tăng frameIndex theo animationDelay.
        if (spriteSheet == null || numFrames <= 1) return;

        animationTimer++;
        if (animationTimer >= animationDelay) {
            animationTimer = 0;
            frameIndex++;
            if (frameIndex >= numFrames) {
                frameIndex = 0;
            }
        }
    }

    private Image loadOptionalImage(String path) {
        return loadOptionalImage(path, 0, 0);
    }

    private Image loadOptionalImage(String path, double requestedWidth, double requestedHeight) {
        // Load asset phụ của boss. Thiếu asset không làm crash game, chỉ dùng animation dự phòng.
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Missing boss asset: " + path);
                return null;
            }
            if (requestedWidth > 0 && requestedHeight > 0) {
                return new Image(stream, requestedWidth, requestedHeight, true, false);
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println("Failed to load boss asset: " + path);
            return null;
        }
    }

    private Image[] loadOptionalFrames(String path, int frames, int targetWidth, int targetHeight) {
        // Tách spritesheet quá rộng thành từng frame nhỏ để Canvas không phải vẽ texture ngang 38400px.
        try (java.io.InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                System.err.println("Missing boss frame asset: " + path);
                return null;
            }

            BufferedImage sheet = ImageIO.read(stream);
            if (sheet == null || frames <= 0) {
                System.err.println("Invalid boss frame asset: " + path);
                return null;
            }

            int sourceFrameWidth = sheet.getWidth() / frames;
            int sourceFrameHeight = sheet.getHeight();
            if (sourceFrameWidth <= 0 || sourceFrameHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
                return null;
            }

            Image[] result = new Image[frames];
            for (int frame = 0; frame < frames; frame++) {
                WritableImage frameImage = new WritableImage(targetWidth, targetHeight);
                PixelWriter writer = frameImage.getPixelWriter();
                int sourceOffsetX = frame * sourceFrameWidth;

                for (int y = 0; y < targetHeight; y++) {
                    int sourceY = Math.min(sourceFrameHeight - 1, y * sourceFrameHeight / targetHeight);
                    for (int x = 0; x < targetWidth; x++) {
                        int sourceX = sourceOffsetX + Math.min(sourceFrameWidth - 1, x * sourceFrameWidth / targetWidth);
                        writer.setArgb(x, y, sheet.getRGB(sourceX, sourceY));
                    }
                }

                result[frame] = frameImage;
            }
            return result;
        } catch (Exception e) {
            System.err.println("Failed to split boss frames: " + path);
            return null;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Vẽ cảnh báo bên dưới entity trước, sau đó mới vẽ boss/vật thể bay bên trên.
        DragonStrike currentDragonStrike = dragonStrike;
        if (currentDragonStrike != null && !currentDragonStrike.isDone()) {
            currentDragonStrike.renderUnder(gc);
        } else if (bossState == BossState.RAGE_TELEGRAPH && frameIndex < RAGE_DRAGON_RELEASE_FRAME) {
            renderRageTelegraph(gc);
        }

        if (this.hp <= 0) {
            gc.save();
            double alpha = Math.max(0.0, Math.min(1.0, flashTimer / 60.0));
            gc.setGlobalAlpha(alpha);
            super.render(gc);
            gc.restore();
        } else if (bossState == BossState.SKILL2_CAST && spriteSheet == attackSprite2) {
            renderScaledBossSprite(gc, SKILL2_BOSS_RENDER_SCALE);
        } else if (bossState == BossState.RAGE_TELEGRAPH && spriteSheet == ultiSprite) {
            renderScaledBossSprite(gc, ULTIMATE_BOSS_RENDER_SCALE);
        } else if (spriteSheet == attackSprite1 || spriteSheet == teleSprite) {
            renderSourceAspectBossSprite(gc);
        } else {
            super.render(gc);
        }

        for (Skill2SlashProjectile slash : skill2Projectiles) {
            slash.render(gc);
        }

        if (currentDragonStrike != null && !currentDragonStrike.isDone()) {
            currentDragonStrike.renderOver(gc);
        }
    }

    private void renderScaledBossSprite(GraphicsContext gc, double scale) {
        // Một số animation có frame lớn hơn 192px nên cần phóng to riêng khi render.
        if (gc == null || spriteSheet == null || numFrames <= 0 || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }

        int safeFrameIndex = Math.max(0, Math.min(frameIndex, numFrames - 1));
        if (bossState == BossState.RAGE_TELEGRAPH && hasUltimateFrame(safeFrameIndex)) {
            renderSingleBossFrame(gc, ultiFrameSprites[safeFrameIndex], scale);
            return;
        }

        double drawW = renderWidth * scale;
        double drawH = renderHeight * scale;
        double drawCenterX = x + renderWidth / 2.0;
        double drawCenterY = y + renderHeight / 2.0;
        double drawX = drawCenterX - drawW / 2.0;
        double drawY = drawCenterY - drawH / 2.0;

        if (isFlipped) {
            gc.drawImage(spriteSheet,
                    safeFrameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawCenterX + drawW / 2.0, drawY, -drawW, drawH);
        } else {
            gc.drawImage(spriteSheet,
                    safeFrameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH);
        }
    }

    private void renderSourceAspectBossSprite(GraphicsContext gc) {
        // boss_atk_1/boss_tele có thể là frame 384x192, nên render theo tỉ lệ frame gốc thay vì ép vuông.
        if (gc == null || spriteSheet == null || numFrames <= 0 || frameWidth <= 0 || frameHeight <= 0) {
            return;
        }

        int safeFrameIndex = Math.max(0, Math.min(frameIndex, numFrames - 1));
        double sourceAspect = frameWidth / frameHeight;
        double drawH = renderHeight;
        double drawW = drawH * sourceAspect;
        double drawCenterX = x + renderWidth / 2.0;
        double drawCenterY = y + renderHeight / 2.0;
        double drawX = drawCenterX - drawW / 2.0;
        double drawY = drawCenterY - drawH / 2.0;

        if (isFlipped) {
            gc.drawImage(spriteSheet,
                    safeFrameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawCenterX + drawW / 2.0, drawY, -drawW, drawH);
        } else {
            gc.drawImage(spriteSheet,
                    safeFrameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH);
        }
    }

    private boolean hasUltimateFrame(int index) {
        return ultiFrameSprites != null
                && index >= 0
                && index < ultiFrameSprites.length
                && ultiFrameSprites[index] != null;
    }

    private void renderSingleBossFrame(GraphicsContext gc, Image frame, double scale) {
        // Render 1 frame đã tách sẵn, tránh drawImage trên spritesheet quá rộng.
        if (gc == null || frame == null) {
            return;
        }

        double drawW = renderWidth * scale;
        double drawH = renderHeight * scale;
        double drawCenterX = x + renderWidth / 2.0;
        double drawCenterY = y + renderHeight / 2.0;
        double drawX = drawCenterX - drawW / 2.0;
        double drawY = drawCenterY - drawH / 2.0;

        if (isFlipped) {
            gc.drawImage(frame, drawCenterX + drawW / 2.0, drawY, -drawW, drawH);
        } else {
            gc.drawImage(frame, drawX, drawY, drawW, drawH);
        }
    }

    private void renderRageTelegraph(GraphicsContext gc) {
        if (gc == null) return;
        renderDangerZone(gc, rageTargetX, rageTargetY);
    }

    private void renderDangerZone(GraphicsContext gc, double targetX, double targetY) {
        if (dangerZoneSprite != null && !dangerZoneSprite.isError() && dangerZoneSprite.getWidth() > 0) {
            double frameW = dangerZoneSprite.getWidth() / DANGER_ZONE_FRAMES;
            double frameH = dangerZoneSprite.getHeight();
            double drawX = targetX - DANGER_ZONE_WIDTH / 2.0;
            double drawY = targetY - DANGER_ZONE_HEIGHT / 2.0;

            gc.drawImage(dangerZoneSprite,
                    dangerZoneFrameIndex * frameW, 0, frameW, frameH,
                    drawX, drawY, DANGER_ZONE_WIDTH, DANGER_ZONE_HEIGHT);
        }
    }

    @Override
    public void setActive(boolean active) {
        // Boss không bị tắt update bởi culling camera.
        this.isActive = true;
    }

    @Override
    public void applyKnockback(Direction dir) {
        if (this.hp <= 0) return;
        // Boss chỉ nhận knockback vật lý nhẹ, không bị stun hay reset frame
        this.kbTimer = 3;
        double kbSpeed = this.knockback * 1.0;
        this.kbVectorX = 0;
        this.kbVectorY = 0;
        switch (dir) {
            case UP: this.kbVectorY = -kbSpeed; break;
            case DOWN: this.kbVectorY = kbSpeed; break;
            case LEFT: this.kbVectorX = -kbSpeed; break;
            case RIGHT: this.kbVectorX = kbSpeed; break;
        }
        // KHÔNG set hitStunTimer, KHÔNG reset frameIndex
    }

    @Override
    public void onCollision(BaseEntity other) {
        // Trong các state boss cần giữ vị trí sát player, không kéo boss về lastX/lastY.
        if (other == targetPlayer && shouldIgnorePlayerCollisionReset()) {
            return;
        }
        super.onCollision(other);
    }

    private boolean shouldIgnorePlayerCollisionReset() {
        // Các trạng thái này chủ động đứng sát player; va chạm chung không được hủy vị trí đó.
        return bossState == BossState.RECOVERY
                || bossState == BossState.SKILL1_SLASH1
                || bossState == BossState.SKILL1_TELEPORT;
    }

    @Override
    public Rectangle2D getBoundary() {
        // Vùng nhận damage nhỏ hơn sprite để không bị trúng đòn ở vùng trong suốt.
        double paddingX = this.renderWidth * 0.12;
        double paddingY = this.renderHeight * 0.08;
        
        double originalHeight = renderHeight - 2 * paddingY;
        double newHeight = originalHeight / 2.0; // Chiều cao = 1/2 hiện tại
        
        return new Rectangle2D(x + paddingX, y + paddingY + newHeight, renderWidth - 2 * paddingX, newHeight);
    }

    @Override
    public Rectangle2D getCollisionBoundary() {
        // Va chạm vật lý chỉ lấy chân để boss có thể đứng sát tường/đồ vật tự nhiên hơn.
        double w = renderWidth * 0.5;
        double h = renderHeight * 0.3;
        double bx = x + (renderWidth - w) / 2.0;
        double by = y + renderHeight - h;
        return new Rectangle2D(bx, by, w, h);
    }

    private class CircleHitbox {
        // Hitbox tròn dùng cho các nhát chém. Mỗi hitbox tự nhớ đã đánh player chưa.
        private final double centerX;
        private final double centerY;
        private final double radius;
        private final int damage;
        private int life;
        private boolean hasHitPlayer = false;

        CircleHitbox(double centerX, double centerY, double radius, int damage, int life) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.damage = damage;
            this.life = life;
        }

        void update() {
            // Chỉ gây damage một lần trong suốt vòng đời hitbox.
            if (!hasHitPlayer && intersectsPlayer()) {
                targetPlayer.takeDamage(damage, centerX, centerY);
                hasHitPlayer = true;
            }
            life--;
        }

        boolean intersectsPlayer() {
            return circleIntersectsRect(centerX, centerY, radius, targetPlayer.getBoundary());
        }

        boolean isExpired() {
            return life <= 0;
        }

    }

    private class Skill2SlashProjectile {
        // Vệt chém bay thẳng ra khỏi màn, không quay về tay boss nữa.
        private double x;
        private double y;
        private final double dirX;
        private final double dirY;
        private final Image sprite;
        private double traveled = 0;
        private int animationTimer = 0;
        private int frameIndex = 0;
        private boolean hasHitPlayer = false;
        private boolean finished = false;

        Skill2SlashProjectile(double x, double y, double dirX, double dirY, Image sprite) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.sprite = sprite;
        }

        void update() {
            if (finished) return;

            x += dirX * SKILL2_PROJECTILE_SPEED;
            y += dirY * SKILL2_PROJECTILE_SPEED;
            traveled += SKILL2_PROJECTILE_SPEED;
            updateAnimation();

            if (!hasHitPlayer && circleIntersectsRect(x, y, SKILL2_ITEM_HIT_RADIUS, targetPlayer.getBoundary())) {
                targetPlayer.takeDamage(SKILL2_DAMAGE, x, y);
                hasHitPlayer = true;
            }

            if (traveled >= SKILL2_PROJECTILE_MAX_DISTANCE || isOutsideArena()) {
                finished = true;
            }
        }

        private void updateAnimation() {
            animationTimer++;
            if (animationTimer >= 6) {
                animationTimer = 0;
                frameIndex = (frameIndex + 1) % SKILL2_ITEM_FRAMES;
            }
        }

        private boolean isOutsideArena() {
            double margin = SKILL2_ITEM_RENDER_SIZE;
            return x < -margin
                    || x > GameConstants.TILE_SIZE * GameConstants.MAX_WORLD_COL + margin
                    || y < -margin
                    || y > GameConstants.TILE_SIZE * GameConstants.MAX_WORLD_ROW + margin;
        }

        boolean isFinished() {
            return finished;
        }

        void render(GraphicsContext gc) {
            // Vẽ vệt chém xoay theo hướng bay.
            gc.save();
            gc.translate(x, y);
            gc.rotate(Math.toDegrees(Math.atan2(dirY, dirX)) + SKILL2_PROJECTILE_ROTATION_OFFSET_DEGREES);

            if (sprite != null) {
                double frameW = sprite.getWidth() / SKILL2_ITEM_FRAMES;
                double halfSize = SKILL2_ITEM_RENDER_SIZE / 2.0;
                gc.drawImage(sprite,
                        frameIndex * frameW, 0, frameW, sprite.getHeight(),
                        -halfSize, -halfSize, SKILL2_ITEM_RENDER_SIZE, SKILL2_ITEM_RENDER_SIZE);
            } else {
                gc.setFill(Color.LIGHTYELLOW);
                gc.fillRect(-SKILL2_ITEM_RENDER_SIZE / 2.0, -6, SKILL2_ITEM_RENDER_SIZE, 12);
                gc.setStroke(Color.ORANGE);
                gc.strokeRect(-SKILL2_ITEM_RENDER_SIZE / 2.0, -6, SKILL2_ITEM_RENDER_SIZE, 12);
            }
            gc.restore();
        }
    }

    private class DragonStrike {
        // Chiêu nộ: cảnh báo trước, sau đó vật thể rơi xuống và nổ một lần.
        private enum Phase { FLYING, EXPLODING, DONE }

        private Phase phase = Phase.FLYING;
        private double targetX;
        private double targetY;
        private double dragonTargetX;
        private double dragonTargetY;
        private double dragonX;
        private double dragonY;
        private final boolean dragonFacesLeft;
        private final double dragonAngleDegrees;
        private int timer = 0;
        private boolean hasDealtDamage = false;
        private double initialDistance;
        private double currentScale = 1.0;

        DragonStrike(double startX, double startY, double targetX, double targetY) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.dragonTargetX = targetX;
            this.dragonTargetY = targetY - (DANGER_ZONE_HEIGHT / 2.0); // Điểm giữa phía trên của danger_zone
            this.dragonX = startX;
            this.dragonY = startY;
            this.dragonFacesLeft = dragonTargetX < startX;
            this.dragonAngleDegrees = dragonFacesLeft ? -DRAGON_DIVE_TILT_DEGREES : DRAGON_DIVE_TILT_DEGREES;

            double dx = dragonTargetX - startX;
            double dy = dragonTargetY - startY;
            this.initialDistance = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
        }

        void updateTarget(double newX, double newY) {
            this.targetX = newX;
            this.targetY = newY;
            this.dragonTargetX = newX;
            this.dragonTargetY = newY - (DANGER_ZONE_HEIGHT / 2.0);

            double dx = this.dragonTargetX - this.dragonX;
            double dy = this.dragonTargetY - this.dragonY;
            this.initialDistance = Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
        }

        void update() {
            timer++;
            if (phase == Phase.FLYING) {
                // Rồng bay từ cổng trong boss_ultimate xuống điểm hình ảnh, còn damage vẫn lấy tâm vòng nổ.
                double dx = dragonTargetX - dragonX;
                double dy = dragonTargetY - dragonY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double speed = DRAGON_SPEED;

                if (distance <= speed || timer >= DRAGON_MAX_FLIGHT_FRAMES) {
                    dragonX = dragonTargetX;
                    dragonY = dragonTargetY;
                    phase = Phase.EXPLODING;
                    timer = 0;
                    // Rung màn hình mạnh hơn khi rồng chạm đất phát nổ
                    com.hust.game.main.App.triggerScreenShake(24, 1.2);
                    com.hust.game.audio.SoundManager.playExplosionSound();
                    return;
                }

                dragonX += (dx / distance) * speed;
                dragonY += (dy / distance) * speed;
            } else if (phase == Phase.EXPLODING) {
                // Vụ nổ chỉ gây damage một lần.
                if (!hasDealtDamage) {
                    Rectangle2D playerBounds = targetPlayer != null ? targetPlayer.getBoundary() : null;
                    if (playerBounds != null) {
                        Rectangle2D dangerZoneRect = new Rectangle2D(
                                targetX - DANGER_ZONE_WIDTH / 2.0,
                                targetY - DANGER_ZONE_HEIGHT / 2.0,
                                DANGER_ZONE_WIDTH,
                                DANGER_ZONE_HEIGHT
                        );
                        if (playerBounds.intersects(dangerZoneRect)) {
                            targetPlayer.takeDamage(RAGE_DAMAGE, targetX, targetY);
                        }
                    }
                    hasDealtDamage = true;
                }

                if (timer >= 36) {
                    phase = Phase.DONE;
                }
            }
        }

        boolean isDone() {
            return phase == Phase.DONE;
        }

        void renderUnder(GraphicsContext gc) {
            if (gc == null || phase == Phase.DONE || phase == Phase.EXPLODING) {
                return;
            }
            renderDangerZone(gc, targetX, targetY);
        }

        void renderOver(GraphicsContext gc) {
            // Vẽ vật thể rơi và hiệu ứng nổ bên trên màn chơi.
            if (gc == null || phase == Phase.DONE) {
                return;
            }

            gc.save();
            if (phase == Phase.FLYING) {
                renderDragon(gc);
            } else if (phase == Phase.EXPLODING) {
                if (explosionSprite != null && !explosionSprite.isError() && explosionSprite.getWidth() > 0) {
                    int expFrame = (timer / 3) % 12; // 36 frames game = 12 frames ảnh (mỗi frame chạy 3 nhịp)
                    double frameW = explosionSprite.getWidth() / 12.0;
                    double frameH = explosionSprite.getHeight();
                    
                    double renderW = frameW * 3.0; // Phóng to x3
                    double renderH = frameH * 3.0; // Phóng to x3
                    
                    gc.drawImage(explosionSprite,
                            expFrame * frameW, 0, frameW, frameH,
                            dragonX - renderW / 2.0, dragonY - renderH / 2.0,
                            renderW, renderH);
                } else {
                    double progress = timer / 36.0;
                    double fallbackRadius = DANGER_ZONE_WIDTH / 2.0;
                    double drawRadius = fallbackRadius * (0.8 + progress * 0.45);
                    gc.setGlobalAlpha(Math.max(0.0, 1.0 - progress));
                    gc.setFill(Color.rgb(255, 110, 30, 0.75));
                    gc.fillOval(targetX - drawRadius, targetY - drawRadius, drawRadius * 2.0, drawRadius * 2.0);
                    gc.setStroke(Color.YELLOW);
                    gc.setLineWidth(4.0);
                    gc.strokeOval(targetX - drawRadius, targetY - drawRadius, drawRadius * 2.0, drawRadius * 2.0);
                }
            }
            gc.restore();
        }

        private void renderDragon(GraphicsContext gc) {
            if (gc == null) {
                return;
            }

            gc.translate(dragonX, dragonY);
            gc.rotate(dragonAngleDegrees);
            gc.scale(currentScale, currentScale);

            // Hiệu ứng "chui ra": Tăng dần phần trăm hiển thị trong 12 frame (0.2s)
            double revealProgress = 1.0;
            if (phase == Phase.FLYING) {
                revealProgress = Math.min(1.0, timer / 12.0);
            }

            if (dragonSprite != null && !dragonSprite.isError() && dragonSprite.getWidth() > 0 && dragonSprite.getHeight() > 0) {
                int dragonFrame = (timer / 6) % DRAGON_FRAMES;
                double frameW = dragonSprite.getWidth() / DRAGON_FRAMES;
                
                double srcX = dragonFrame * frameW;
                double srcY = 0;
                double srcW = frameW * revealProgress;
                double srcH = dragonSprite.getHeight();

                if (dragonFacesLeft) {
                    double dstX = -DRAGON_RENDER_WIDTH / 2.0;
                    double dstY = -DRAGON_RENDER_HEIGHT / 2.0;
                    double dstW = DRAGON_RENDER_WIDTH * revealProgress;
                    double dstH = DRAGON_RENDER_HEIGHT;
                    
                    gc.drawImage(dragonSprite,
                            srcX, srcY, srcW, srcH,
                            dstX, dstY, dstW, dstH);
                } else {
                    double dstX = DRAGON_RENDER_WIDTH / 2.0;
                    double dstY = -DRAGON_RENDER_HEIGHT / 2.0;
                    double dstW = -DRAGON_RENDER_WIDTH * revealProgress;
                    double dstH = DRAGON_RENDER_HEIGHT;
                    
                    gc.drawImage(dragonSprite,
                            srcX, srcY, srcW, srcH,
                            dstX, dstY, dstW, dstH);
                }
            } else {
                gc.setStroke(Color.rgb(255, 185, 60));
                gc.setLineWidth(18.0);
                gc.strokeLine(-56, 0, -56 + 112 * revealProgress, 0);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(5.0);
                gc.strokeLine(-34, 0, -34 + 68 * revealProgress, 0);
            }
        }
    }

    private boolean circleIntersectsRect(double circleX, double circleY, double radius, Rectangle2D rect) {
        // Collision giữa hitbox tròn và rectangle: lấy điểm gần nhất của rect với tâm tròn.
        double nearestX = clamp(circleX, rect.getMinX(), rect.getMaxX());
        double nearestY = clamp(circleY, rect.getMinY(), rect.getMaxY());
        double dx = circleX - nearestX;
        double dy = circleY - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private double clamp(double value, double min, double max) {
        // Giới hạn value trong khoảng [min, max].
        return Math.max(min, Math.min(max, value));
    }
}
