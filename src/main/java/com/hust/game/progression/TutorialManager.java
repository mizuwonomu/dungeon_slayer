package com.hust.game.progression;

import com.hust.game.ui.DialogBox;
import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;
import javafx.scene.input.KeyCode;
import java.util.Set;
import javafx.scene.canvas.GraphicsContext;

public class TutorialManager {
    private DialogBox dialogBox;
    private int currentPhase = 0;
    private final int TILE = GameConstants.TILE_SIZE;
    
    private int moveTimer = 0;
    private int delayTimer = 0;
    
    private boolean isDamageDialogQueued = false;
    private int previousHp = -1;

    public TutorialManager() {
        dialogBox = new DialogBox();
    }

    public void update(Player player, Set<KeyCode> input) {
        dialogBox.update();
        
        if (previousHp == -1) {
            previousHp = player.getCurrentHp();
        }
        
        boolean isMoving = input.contains(KeyCode.W) || input.contains(KeyCode.A) || 
                           input.contains(KeyCode.S) || input.contains(KeyCode.D) ||
                           input.contains(KeyCode.UP) || input.contains(KeyCode.DOWN) || 
                           input.contains(KeyCode.LEFT) || input.contains(KeyCode.RIGHT);
                           
        boolean isJPressed = input.contains(KeyCode.J);
        boolean isLPressed = input.contains(KeyCode.L);
        
        boolean tookDamage = player.getCurrentHp() < previousHp;
        previousHp = player.getCurrentHp();

        // Phòng 1
        if (currentPhase == 0) {
            dialogBox.show("Sử dụng [WASD] để di chuyển", -1);
            currentPhase = 1;
        } 
        else if (currentPhase == 1) {
            if (isMoving) {
                moveTimer++;
                if (moveTimer >= 60) { // Đã di chuyển được 1 giây (60 frame)
                    dialogBox.hide();
                    currentPhase = 2;
                }
            }
        }
        // Phòng 2 (Qua cánh cổng tọa độ X=576)
        else if (currentPhase == 2) {
            if (player.getX() > 14 * TILE && dialogBox.isHidden()) {
                dialogBox.show("Sử dụng [J] để tấn công thường", -1);
                currentPhase = 3;
            }
        }
        else if (currentPhase == 3 && isJPressed) {
            delayTimer = 0;
            currentPhase = 35; // Chuyển sang trạng thái chờ 1s
        }
        else if (currentPhase == 35) {
            delayTimer++;
            if (delayTimer >= 60) { // Đợi 60 frame (1 giây)
                dialogBox.hide();
                currentPhase = 4;
            }
        }
        // Phòng 3 (Qua cánh cổng tọa độ X=1392)
        else if (currentPhase == 4) {
            if (player.getX() > 31 * TILE && dialogBox.isHidden()) {
                dialogBox.show("Sử dụng [L] để sử dụng [cuồng nộ]\n- tăng sát thương đòn đánh thường - tiêu hao 10 mana", -1);
                currentPhase = 5;
            }
        }
        else if (currentPhase == 5 && isLPressed) {
            delayTimer = 0;
            currentPhase = 55;
        }
        else if (currentPhase == 55) {
            delayTimer++;
            if (delayTimer >= 60) {
                dialogBox.hide();
                currentPhase = 6;
            }
        }
        // Phòng 4 (Qua cánh cổng tọa độ X=2160)
        else if (currentPhase == 6) {
            if (player.getX() > 47 * TILE && dialogBox.isHidden()) {
                dialogBox.show("Combo: chém liên tiếp vào quái sẽ tăng combo,\ncombo càng cao sát thương càng lớn", -1);
                currentPhase = 7;
            }
        }
        else if (currentPhase == 7 && isJPressed) {
            delayTimer = 0;
            currentPhase = 75;
        }
        else if (currentPhase == 75) {
            delayTimer++;
            if (delayTimer >= 60) {
                dialogBox.hide();
                currentPhase = 8;
            }
        }
        // Phòng 4 - Nhận Damage
        else if (currentPhase >= 8) {
            if (tookDamage && !isDamageDialogQueued && currentPhase == 8) {
                isDamageDialogQueued = true;
            }
            
            // Hộp thoại máu chỉ hiển thị khi hộp thoại combo đã thu xuống hoàn toàn
            if (isDamageDialogQueued && dialogBox.isHidden()) {
                dialogBox.show("Cẩn thận, player sẽ mất máu\nkhi bị quái đánh trúng đấy!", 180); // Tồn tại 180 frame (3s)
                isDamageDialogQueued = false;
                currentPhase = 9; 
            }
        }
    }

    public void render(GraphicsContext gc) {
        dialogBox.render(gc);
    }
}