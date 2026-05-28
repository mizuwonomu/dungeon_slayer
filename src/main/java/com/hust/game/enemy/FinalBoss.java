package com.hust.game.enemy;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.Direction;
import com.hust.game.entities.base.BaseEntity;
import com.hust.game.entities.player.Player;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FinalBoss extends Enemy {
    // Bộ trạng thái của boss: mỗi thời điểm chỉ chạy một trạng thái để tránh spam/chồng chiêu.
    private enum BossState {
        CHASE,
        NORMAL_WINDUP,
        NORMAL_ACTIVE,
        SKILL1_TELEPORT,
        SKILL1_DELAY,
        SKILL1_SLASH1,
        SKILL1_GAP,
        SKILL1_SLASH2,
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
    private static final int TELE_FRAMES = 8;
    private static final int WALK_FRAMES = 10;
    private static final int ULTIMATE_FRAMES = 25;

    // Bộ đếm thời gian tính theo 60 FPS: 120 frame = 2 giây, 30 frame = 0.5 giây.
    private static final int ACTION_DELAY_FRAMES = 120;
    private static final int NORMAL_WINDUP_FRAMES = 36;
    private static final int NORMAL_ACTIVE_FRAMES = 18;
    private static final int NORMAL_RECOVERY_FRAMES = ACTION_DELAY_FRAMES;
    private static final int SKILL1_TELEPORT_FRAMES = 40;
    private static final int SKILL1_DELAY_FRAMES = 30;
    private static final int SKILL1_SLASH_FRAMES = 30;
    private static final int SKILL1_GAP_FRAMES = 20;
    private static final int SKILL1_RECOVERY_FRAMES = ACTION_DELAY_FRAMES;
    private static final int SKILL2_CAST_FRAMES = 42;
    private static final int SKILL2_RECOVERY_FRAMES = ACTION_DELAY_FRAMES;
    private static final int RAGE_TELEGRAPH_FRAMES = 60;

    private static final int TELEPORT_APPLY_FRAME = 5;

    private static final int NORMAL_COOLDOWN_FRAMES = 110;
    private static final int SKILL1_COOLDOWN_FRAMES = 330;
    private static final int SKILL2_COOLDOWN_FRAMES = 300;

    private static final double NORMAL_ATTACK_RADIUS_MULTIPLIER = 1.35;
    private static final double SKILL1_RADIUS_MULTIPLIER = 1.5;
    private static final double RAGE_RADIUS_MULTIPLIER = 3.0;
    private static final double FLYING_SWORD_RENDER_SIZE = 84.0;
    private static final double FLYING_SWORD_HIT_RADIUS = 30.0;
    private static final double SKILL2_BOSS_RENDER_SCALE = 2.0;
    private static final double WANDER_SPEED = 1.0;

    // Level 3 đang dùng lại level2.txt. Tâm arena đi được nằm quanh hàng 5,
    // không phải tâm cửa sổ vì tâm cửa sổ bị dính vùng hố có va chạm.
    private static final double ARENA_CENTER_X = GameConstants.TILE_SIZE * 8.5;
    private static final double ARENA_CENTER_Y = GameConstants.TILE_SIZE * 5.0;

    private final Image idleSprite;
    private final int idleFrames;
    private final Image attackSprite1;
    private final Image attackSprite2;
    private final Image teleSprite;
    private final Image walkSprite;
    private final Image ultiSprite;
    private final Image swordSprite;

    private BossState bossState = BossState.CHASE;
    private int stateTimer = 0;
    private int normalCooldownTimer = 45;
    private int skill1CooldownTimer = 120;
    private int skill2CooldownTimer = 80;
    private int attackCountSinceRage = 0;
    private boolean rageQueued = false;
    private boolean hasHitPlayerThisAttack = false;
    private boolean hasSpawnedSkill2Sword = false;
    private boolean hasTeleportedThisSkill = false;

    private double teleportTargetX = 0;
    private double teleportTargetY = 0;
    private double lastAimX = 1;
    private double lastAimY = 0;
    private double recoveryWanderX = 0;
    private double recoveryWanderY = 0;
    private int recoveryWanderTimer = 0;
    private double rageTargetX = ARENA_CENTER_X;
    private double rageTargetY = ARENA_CENTER_Y;

    private CircleHitbox activeHitbox;
    private DragonStrike dragonStrike;
    private final List<FlyingSwordProjectile> flyingSwords = new ArrayList<>();

    public FinalBoss(double x, double y, Image spriteSheet, int numFrames,
            double renderWidth, double renderHeight, Player targetPlayer) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight, targetPlayer);

        // Chỉ số chính của boss.
        this.maxHp = BOSS_MAX_HP;
        this.hp = maxHp;
        this.damage = Math.max(1, targetPlayer.getMaxHp() / 5);
        this.knockback = 1;
        this.speed = 1.15;
        this.isImmobile = false;
        this.animationDelay = 10;

        // Boss tự load các animation riêng. spriteSheet truyền vào chỉ làm ảnh dự phòng.
        Image bossIdleSprite = loadOptionalImage("/assets/enemy/boss_idle.png");
        this.idleSprite = bossIdleSprite != null ? bossIdleSprite : spriteSheet;
        this.idleFrames = bossIdleSprite != null ? IDLE_FRAMES : Math.max(1, numFrames);
        this.attackSprite1 = loadOptionalImage("/assets/enemy/boss_atk_1.png");
        this.attackSprite2 = loadOptionalImage("/assets/enemy/boss_atk_2.png");
        this.teleSprite = loadOptionalImage("/assets/enemy/boss_tele.png");
        this.walkSprite = loadOptionalImage("/assets/enemy/boss_walk.png");
        this.ultiSprite = loadOptionalImage("/assets/enemy/final_boss_ulti.png");
        this.swordSprite = loadOptionalImage("/assets/player/wswordhit.png");
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
            case CHASE -> updateChase();
            case NORMAL_WINDUP -> updateNormalWindup();
            case NORMAL_ACTIVE -> updateNormalActive();
            case SKILL1_TELEPORT -> updateSkill1Teleport();
            case SKILL1_DELAY -> updateTimedState(BossState.SKILL1_SLASH1, SKILL1_SLASH_FRAMES, () ->
                    activeHitbox = createSlashHitbox(SKILL1_DAMAGE, playerZoneRadius() * SKILL1_RADIUS_MULTIPLIER, SKILL1_SLASH_FRAMES));
            case SKILL1_SLASH1 -> updateActiveCircleHitbox(BossState.SKILL1_GAP, SKILL1_GAP_FRAMES);
            case SKILL1_GAP -> updateTimedState(BossState.SKILL1_SLASH2, SKILL1_SLASH_FRAMES, () ->
                    activeHitbox = createSlashHitbox(SKILL1_DAMAGE, playerZoneRadius() * SKILL1_RADIUS_MULTIPLIER, SKILL1_SLASH_FRAMES));
            case SKILL1_SLASH2 -> updateActiveCircleHitbox(BossState.RECOVERY, SKILL1_RECOVERY_FRAMES);
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
        if (normalCooldownTimer > 0) normalCooldownTimer--;
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

    private void updateChase() {
        // State mặc định: đuổi player và chọn chiêu nếu cooldown đã sẵn sàng.
        setSprite(idleSprite, idleFrames, 10);

        if (rageQueued) {
            startRage();
            return;
        }

        double distance = distanceToPlayerCenter();
        double preferredDistance = renderWidth * 0.55 + playerZoneRadius();

        // Giữ khoảng cách đủ gần để đánh, nhưng không ép boss đứng đè lên player.
        if (distance > preferredDistance && !isImmobile) {
            moveTowardPlayer(distance);
        }

        if (normalCooldownTimer <= 0 && distance <= normalAttackStartRange()) {
            startNormalAttack();
        } else if (skill1CooldownTimer <= 0 && distance <= GameConstants.TILE_SIZE * 6.0) {
            startSkill1();
        } else if (skill2CooldownTimer <= 0) {
            startSkill2();
        }
    }

    private void updateNormalWindup() {
        // Đánh thường có windup để player kịp nhìn animation trước khi dính damage.
        setSprite(attackSprite1 != null ? attackSprite1 : idleSprite,
                attackSprite1 != null ? ATTACK_1_FRAMES : idleFrames, 3);
        if (--stateTimer <= 0) {
            changeState(BossState.NORMAL_ACTIVE, NORMAL_ACTIVE_FRAMES);
            hasHitPlayerThisAttack = false;
        }
    }

    private void updateNormalActive() {
        // Một nhát đánh thường chỉ gây damage một lần nhờ hasHitPlayerThisAttack.
        CircleHitbox hitbox = createSlashHitbox(this.damage, normalAttackRadius(), NORMAL_ACTIVE_FRAMES);
        if (!hasHitPlayerThisAttack && hitbox.intersectsPlayer()) {
            targetPlayer.takeDamage(hitbox.damage, hitbox.centerX, hitbox.centerY);
            hasHitPlayerThisAttack = true;
        }

        if (--stateTimer <= 0) {
            normalCooldownTimer = NORMAL_COOLDOWN_FRAMES;
            changeState(BossState.RECOVERY, NORMAL_RECOVERY_FRAMES);
        }
    }

    private void updateSkill1Teleport() {
        // Skill 1: chạy animation tele, sau đó dịch chuyển thẳng sát player.
        setSprite(teleSprite != null ? teleSprite : idleSprite,
                teleSprite != null ? TELE_FRAMES : idleFrames, 5);

        // Tele không gây damage. Damage bắt đầu sau delay 0.5 giây ở state SKILL1_DELAY.
        if (!hasTeleportedThisSkill && frameIndex >= TELEPORT_APPLY_FRAME) {
            x = teleportTargetX;
            y = teleportTargetY;
            hasTeleportedThisSkill = true;
            lockAimToPlayer();
        }

        if (--stateTimer <= 0) {
            changeState(BossState.SKILL1_DELAY, SKILL1_DELAY_FRAMES);
        }
    }

    private void updateSkill2Cast() {
        // Skill 2: boss vận chiêu kiếm bay theo hướng player tại thời điểm bắt đầu.
        setSprite(attackSprite2 != null ? attackSprite2 : idleSprite,
                attackSprite2 != null ? ATTACK_2_FRAMES : idleFrames, 3);

        if (!hasSpawnedSkill2Sword && stateTimer <= SKILL2_CAST_FRAMES / 2) {
            hasSpawnedSkill2Sword = true;
            flyingSwords.add(new FlyingSwordProjectile());
        }

        if (--stateTimer <= 0) {
            skill2CooldownTimer = SKILL2_COOLDOWN_FRAMES;
            changeState(BossState.RECOVERY, SKILL2_RECOVERY_FRAMES);
        }
    }

    private void updateRageTelegraph() {
        // Chiêu nộ có 1 giây cảnh báo tại vị trí player bị khóa lúc cast.
        setSprite(ultiSprite != null ? ultiSprite : idleSprite,
                ultiSprite != null ? ULTIMATE_FRAMES : idleFrames, 4);
        if (--stateTimer <= 0) {
            dragonStrike = new DragonStrike(rageTargetX, rageTargetY, playerZoneRadius() * RAGE_RADIUS_MULTIPLIER);
            changeState(BossState.RAGE_DRAGON, 0);
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
                walkSprite != null ? WALK_FRAMES : idleFrames, 8);

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
        // Vùng đánh dạng tròn tồn tại nhiều frame nhưng tự chặn damage lặp lên player.
        setSprite(attackSprite1 != null ? attackSprite1 : idleSprite,
                attackSprite1 != null ? ATTACK_1_FRAMES : idleFrames, 3);

        if (activeHitbox != null) {
            activeHitbox.update();
            if (activeHitbox.isExpired()) {
                activeHitbox = null;
            }
        }

        if (--stateTimer <= 0) {
            activeHitbox = null;
            changeState(nextState, nextTimer);
        }
    }

    private void updateProjectiles() {
        // Cập nhật tất cả kiếm bay và xóa vật thể bay đã kết thúc.
        Iterator<FlyingSwordProjectile> iterator = flyingSwords.iterator();
        while (iterator.hasNext()) {
            FlyingSwordProjectile sword = iterator.next();
            sword.update();
            if (sword.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void updateDeath() {
        // Dọn toàn bộ hitbox/effect khi boss chết.
        bossState = BossState.DEAD;
        activeHitbox = null;
        dragonStrike = null;
        flyingSwords.clear();
        if (flashTimer > 0) {
            flashTimer--;
        }
        animateCurrentSprite();
    }

    private void startNormalAttack() {
        // Bắt đầu đánh thường và tính 1 action vào bộ đếm nộ.
        registerBossAttackAction();
        lockAimToPlayer();
        hasHitPlayerThisAttack = false;
        changeState(BossState.NORMAL_WINDUP, NORMAL_WINDUP_FRAMES);
    }

    private void startSkill1() {
        // Bắt đầu Skill 1: tele tới gần player rồi chém 2 lần.
        registerBossAttackAction();
        lockAimToPlayer();
        skill1CooldownTimer = SKILL1_COOLDOWN_FRAMES;
        hasTeleportedThisSkill = false;

        // Boss tele thẳng sát player, không kiểm tra vùng va chạm theo yêu cầu.
        prepareTeleportNearPlayer();

        changeState(BossState.SKILL1_TELEPORT, SKILL1_TELEPORT_FRAMES);
    }

    private void startSkill2() {
        // Bắt đầu Skill 2: khóa hướng player rồi phóng kiếm đi và quay về.
        registerBossAttackAction();
        lockAimToPlayer();
        hasSpawnedSkill2Sword = false;
        skill2CooldownTimer = SKILL2_COOLDOWN_FRAMES;
        changeState(BossState.SKILL2_CAST, SKILL2_CAST_FRAMES);
    }

    private void startRage() {
        // Chiêu nộ khóa vị trí hiện tại của player để vẽ cảnh báo rồi mới rơi xuống.
        lockAimToPlayer();
        rageTargetX = playerCenterX();
        rageTargetY = playerCenterY();
        rageQueued = false;
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
                || bossState == BossState.NORMAL_WINDUP;
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

    private void prepareTeleportNearPlayer() {
        // Không kiểm tra vùng va chạm/tường: lấy trực tiếp vị trí sát player theo hướng đã khóa.
        double stopDistance = playerZoneRadius() + renderWidth * 0.38;
        double preferredCenterX = playerCenterX() - lastAimX * stopDistance;
        double preferredCenterY = playerCenterY() - lastAimY * stopDistance;

        teleportTargetX = preferredCenterX - renderWidth / 2.0;
        teleportTargetY = preferredCenterY - renderHeight / 2.0;
    }

    private void chooseRecoveryWanderDirection() {
        // Khi đang quá gần player, boss ưu tiên lùi ra để player nhìn rõ delay.
        double dxFromPlayer = centerX() - playerCenterX();
        double dyFromPlayer = centerY() - playerCenterY();
        double distanceFromPlayer = Math.sqrt(dxFromPlayer * dxFromPlayer + dyFromPlayer * dyFromPlayer);

        if (distanceFromPlayer > 0.0001 && distanceFromPlayer < normalAttackStartRange()) {
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

    private double normalAttackRadius() {
        // Đánh thường cần rộng hơn người chơi một chút để không bị hụt khi boss đứng sát.
        return Math.max(playerZoneRadius() * NORMAL_ATTACK_RADIUS_MULTIPLIER, renderWidth * 0.25);
    }

    private double normalAttackStartRange() {
        // Khoảng cách đủ gần để bắt đầu đánh thường.
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

        if (this.spriteSheet != sprite || this.numFrames != frames) {
            this.spriteSheet = sprite;
            this.numFrames = frames;
            this.frameWidth = sprite.getWidth() / frames;
            this.frameHeight = sprite.getHeight();
            this.frameIndex = 0;
            this.animationTimer = 0;
        }
        this.animationDelay = delay;
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
        // Load asset phụ của boss. Thiếu asset không làm crash game, chỉ dùng animation dự phòng.
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Missing boss asset: " + path);
                return null;
            }
            return new Image(stream);
        } catch (Exception e) {
            System.err.println("Failed to load boss asset: " + path);
            return null;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Vẽ cảnh báo bên dưới entity trước, sau đó mới vẽ boss/vật thể bay bên trên.
        if (dragonStrike != null) {
            dragonStrike.renderUnder(gc);
        } else if (bossState == BossState.RAGE_TELEGRAPH) {
            renderRageTelegraph(gc);
        }

        if (this.hp <= 0) {
            gc.save();
            double alpha = Math.max(0.0, Math.min(1.0, flashTimer / 60.0));
            gc.setGlobalAlpha(alpha);
            super.render(gc);
            gc.restore();
        } else if (bossState == BossState.SKILL2_CAST && spriteSheet == attackSprite2) {
            renderSkill2BossSprite(gc);
        } else {
            super.render(gc);
        }

        for (FlyingSwordProjectile sword : flyingSwords) {
            sword.render(gc);
        }

        if (dragonStrike != null) {
            dragonStrike.renderOver(gc);
        }
    }

    private void renderSkill2BossSprite(GraphicsContext gc) {
        // boss_atk_2.png có frame 384x384, lớn gấp đôi các animation 192px.
        // Nếu ép về 144x144 như bình thường thì thân boss bị nhỏ, nên phóng to riêng trạng thái này.
        double drawW = renderWidth * SKILL2_BOSS_RENDER_SCALE;
        double drawH = renderHeight * SKILL2_BOSS_RENDER_SCALE;
        double drawCenterX = x + renderWidth / 2.0;
        double drawCenterY = y + renderHeight / 2.0;
        double drawX = drawCenterX - drawW / 2.0;
        double drawY = drawCenterY - drawH / 2.0;

        if (isFlipped) {
            gc.drawImage(spriteSheet,
                    frameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawCenterX + drawW / 2.0, drawY, -drawW, drawH);
        } else {
            gc.drawImage(spriteSheet,
                    frameIndex * frameWidth, 0, frameWidth, frameHeight,
                    drawX, drawY, drawW, drawH);
        }
    }

    private void renderRageTelegraph(GraphicsContext gc) {
        // Vùng cảnh báo chiêu nộ: player có 1 giây để né khỏi vòng tròn.
        double radius = playerZoneRadius() * RAGE_RADIUS_MULTIPLIER;
        double pulse = 0.35 + 0.25 * Math.sin(stateTimer * 0.35);

        gc.save();
        gc.setGlobalAlpha(pulse);
        gc.setFill(Color.rgb(255, 70, 30, 0.30));
        gc.fillOval(rageTargetX - radius, rageTargetY - radius, radius * 2.0, radius * 2.0);
        gc.setGlobalAlpha(0.85);
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(3.0);
        gc.strokeOval(rageTargetX - radius, rageTargetY - radius, radius * 2.0, radius * 2.0);
        gc.strokeLine(rageTargetX, rageTargetY - GameConstants.TILE_SIZE * 5.0, rageTargetX, rageTargetY + 12);
        gc.restore();
    }

    @Override
    public void setActive(boolean active) {
        // Boss không bị tắt update bởi culling camera.
        this.isActive = true;
    }

    @Override
    public void applyKnockback(Direction dir) {
        // Boss chỉ nhận knockback rất nhẹ để vẫn phản hồi khi bị đánh.
        if (this.hp <= 0) return;

        this.kbVectorX = 0;
        this.kbVectorY = 0;
        double kbSpeed = this.knockback;
        switch (dir) {
            case UP -> this.kbVectorY = -kbSpeed;
            case DOWN -> this.kbVectorY = kbSpeed;
            case LEFT -> this.kbVectorX = -kbSpeed;
            case RIGHT -> this.kbVectorX = kbSpeed;
        }
        this.kbTimer = 2;
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
                || bossState == BossState.SKILL1_TELEPORT
                || bossState == BossState.SKILL1_DELAY
                || bossState == BossState.SKILL1_SLASH1
                || bossState == BossState.SKILL1_GAP
                || bossState == BossState.SKILL1_SLASH2;
    }

    @Override
    public Rectangle2D getBoundary() {
        // Vùng nhận damage nhỏ hơn sprite để không bị trúng đòn ở vùng trong suốt.
        double paddingX = this.renderWidth * 0.12;
        double paddingY = this.renderHeight * 0.08;
        return new Rectangle2D(x + paddingX, y + paddingY, renderWidth - 2 * paddingX, renderHeight - 2 * paddingY);
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

    private class FlyingSwordProjectile {
        // Kiếm bay có 2 giai đoạn: bay ra theo hướng đã khóa, rồi quay về vị trí boss hiện tại.
        private enum Phase { OUT, RETURN }

        private Phase phase = Phase.OUT;
        private double x = centerX();
        private double y = centerY();
        private double dirX;
        private double dirY;
        private double traveled = 0;
        private boolean hitOut = false;
        private boolean hitReturn = false;
        private boolean finished = false;

        FlyingSwordProjectile() {
            // Hướng bay ra được khóa tại thời điểm boss cast Skill 2.
            dirX = lastAimX;
            dirY = lastAimY;
            if (Math.abs(dirX) < 0.0001 && Math.abs(dirY) < 0.0001) {
                dirX = 1.0;
                dirY = 0.0;
            }
        }

        void update() {
            if (finished) return;

            double speed = 6.5;
            if (phase == Phase.OUT) {
                // Giai đoạn bay ra: bay thẳng đến player theo hướng vận chiêu ban đầu.
                x += dirX * speed;
                y += dirY * speed;
                traveled += speed;

                if (!hitOut && intersectsPlayer()) {
                    targetPlayer.takeDamage(SKILL2_DAMAGE, x, y);
                    hitOut = true;
                }

                if (traveled >= GameConstants.TILE_SIZE * 5.0 || hitsWall()) {
                    phase = Phase.RETURN;
                }
            } else {
                // Giai đoạn quay về: luôn quay về tọa độ boss hiện tại, không quay về vị trí cũ.
                double dx = centerX() - x;
                double dy = centerY() - y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance <= speed) {
                    finished = true;
                    return;
                }

                x += (dx / distance) * speed;
                y += (dy / distance) * speed;

                if (!hitReturn && intersectsPlayer()) {
                    targetPlayer.takeDamage(SKILL2_DAMAGE, x, y);
                    hitReturn = true;
                }
            }
        }

        private boolean hitsWall() {
            return collisionChecker != null && collisionChecker.checkTile((int) x, (int) y);
        }

        private boolean intersectsPlayer() {
            return circleIntersectsRect(x, y, FLYING_SWORD_HIT_RADIUS, targetPlayer.getBoundary());
        }

        boolean isFinished() {
            return finished;
        }

        void render(GraphicsContext gc) {
            // Vẽ kiếm xoay theo hướng bay hiện tại.
            gc.save();
            gc.translate(x, y);
            double angle = Math.toDegrees(Math.atan2(
                    phase == Phase.OUT ? dirY : centerY() - y,
                    phase == Phase.OUT ? dirX : centerX() - x));
            gc.rotate(angle);

            if (swordSprite != null) {
                double frameW = swordSprite.getWidth() / 8.0;
                double halfSize = FLYING_SWORD_RENDER_SIZE / 2.0;
                gc.drawImage(swordSprite, 0, 0, frameW, swordSprite.getHeight(),
                        -halfSize, -halfSize, FLYING_SWORD_RENDER_SIZE, FLYING_SWORD_RENDER_SIZE);
            } else {
                gc.setFill(Color.LIGHTYELLOW);
                gc.fillRect(-FLYING_SWORD_RENDER_SIZE / 2.0, -6, FLYING_SWORD_RENDER_SIZE, 12);
                gc.setStroke(Color.ORANGE);
                gc.strokeRect(-FLYING_SWORD_RENDER_SIZE / 2.0, -6, FLYING_SWORD_RENDER_SIZE, 12);
            }
            gc.restore();
        }
    }

    private class DragonStrike {
        // Chiêu nộ: cảnh báo trước, sau đó vật thể rơi xuống và nổ một lần.
        private enum Phase { FLYING, EXPLODING, DONE }

        private Phase phase = Phase.FLYING;
        private final double targetX;
        private final double targetY;
        private final double radius;
        private double dragonX;
        private double dragonY;
        private int timer = 0;
        private boolean hasDealtDamage = false;

        DragonStrike(double targetX, double targetY, double radius) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.radius = radius;
            this.dragonX = targetX;
            this.dragonY = targetY - GameConstants.TILE_SIZE * 7.0;
        }

        void update() {
            timer++;
            if (phase == Phase.FLYING) {
                // Vật thể rơi thẳng từ trên xuống mục tiêu đã khóa.
                double dx = targetX - dragonX;
                double dy = targetY - dragonY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double speed = 12.0;

                if (distance <= speed || timer >= 80) {
                    dragonX = targetX;
                    dragonY = targetY;
                    phase = Phase.EXPLODING;
                    timer = 0;
                    com.hust.game.main.App.triggerScreenShake(18, 0.8);
                    return;
                }

                dragonX += (dx / distance) * speed;
                dragonY += (dy / distance) * speed;
            } else if (phase == Phase.EXPLODING) {
                // Vụ nổ chỉ gây damage một lần.
                if (!hasDealtDamage) {
                    if (circleIntersectsRect(targetX, targetY, radius, targetPlayer.getBoundary())) {
                        targetPlayer.takeDamage(RAGE_DAMAGE, targetX, targetY);
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
            // Vẽ vùng nổ bên dưới boss/player để player thấy nơi nguy hiểm.
            gc.save();
            gc.setGlobalAlpha(0.35);
            gc.setFill(Color.rgb(255, 40, 20, 0.30));
            gc.fillOval(targetX - radius, targetY - radius, radius * 2.0, radius * 2.0);
            gc.setGlobalAlpha(0.9);
            gc.setStroke(Color.RED);
            gc.setLineWidth(3.0);
            gc.strokeOval(targetX - radius, targetY - radius, radius * 2.0, radius * 2.0);
            gc.restore();
        }

        void renderOver(GraphicsContext gc) {
            // Vẽ vật thể rơi và hiệu ứng nổ bên trên màn chơi.
            gc.save();
            if (phase == Phase.FLYING) {
                gc.setStroke(Color.rgb(255, 185, 60));
                gc.setLineWidth(18.0);
                gc.strokeLine(dragonX, dragonY - 56, dragonX, dragonY + 56);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(5.0);
                gc.strokeLine(dragonX, dragonY - 34, dragonX, dragonY + 34);
            } else if (phase == Phase.EXPLODING) {
                double progress = timer / 36.0;
                double drawRadius = radius * (0.8 + progress * 0.45);
                gc.setGlobalAlpha(Math.max(0.0, 1.0 - progress));
                gc.setFill(Color.rgb(255, 110, 30, 0.75));
                gc.fillOval(targetX - drawRadius, targetY - drawRadius, drawRadius * 2.0, drawRadius * 2.0);
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(4.0);
                gc.strokeOval(targetX - drawRadius, targetY - drawRadius, drawRadius * 2.0, drawRadius * 2.0);
            }
            gc.restore();
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
