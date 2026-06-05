package com.hust.game.progression;

import com.hust.game.entities.player.Player;
import com.hust.game.constants.GameConstants;
import javafx.scene.input.KeyCode;
import java.util.Set;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class TutorialManager {
    private enum DialogState { HIDDEN, APPEARING, VISIBLE, DISAPPEARING }

    private int currentPhase = 0;
    private final int TILE = GameConstants.TILE_SIZE;
    
    private int moveTimer = 0;
    private int delayTimer = 0;
    
    // --- Animation ---
    private DialogState dialogState = DialogState.HIDDEN;
    private int animTimer = 0;
    private static final int ANIM_DURATION = 30; // 0.5 giây

    // --- Typewriter & Skip ---
    private String currentDialogText = "";
    private boolean isDialogVisible = false;
    private int visibleChars = 0;
    private int textTimer = 0;
    private static final int TEXT_DELAY = 2; // Số frame để hiện 1 chữ
    private boolean skipRequested = false;

    // --- Rendering ---
    private Font font;
    private Image cmtBox;

    private boolean isDamageDialogQueued = false;
    private int previousHp = -1;

    public TutorialManager() {
        try {
            java.io.InputStream fontStream = getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf");
            Font loadedFont = fontStream != null ? Font.loadFont(fontStream, 24) : null;
            this.font = loadedFont != null ? loadedFont : Font.font("Arial", FontWeight.BOLD, 24);
            this.cmtBox = new Image(getClass().getResourceAsStream("/assets/cmt_box.png"));
        } catch (Exception e) {
            System.err.println("Lỗi tải font hoặc ảnh cho TutorialManager: " + e.getMessage());
        }
    }

    private void showDialog(String text) {
        this.currentDialogText = text;
        this.visibleChars = 0;
        this.textTimer = 0;
        this.dialogState = DialogState.APPEARING;
        this.animTimer = 0;
    }

    private void hideDialog() {
        if (dialogState == DialogState.VISIBLE || dialogState == DialogState.APPEARING) {
            this.dialogState = DialogState.DISAPPEARING;
            this.animTimer = 0;
        }
    }

    public void update(Player player, Set<KeyCode> input, boolean isMousePressed) {
        
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

        // --- Logic Animation ---
        if (dialogState == DialogState.APPEARING) {
            animTimer++;
            if (animTimer >= ANIM_DURATION) {
                animTimer = ANIM_DURATION;
                dialogState = DialogState.VISIBLE;
            }
        } else if (dialogState == DialogState.DISAPPEARING) {
            animTimer++;
            if (animTimer >= ANIM_DURATION) {
                animTimer = 0;
                dialogState = DialogState.HIDDEN;
                currentDialogText = ""; // Clear text after it's hidden
            }
        }

        // --- Logic Typewriter & Skip ---
        if (dialogState != DialogState.HIDDEN && visibleChars < currentDialogText.length()) {
            if (isMousePressed || input.contains(KeyCode.SPACE)) {
                if (!skipRequested) {
                    visibleChars = currentDialogText.length();
                    skipRequested = true;
                }
            } else {
                skipRequested = false;
                textTimer++;
                if (textTimer >= TEXT_DELAY) {
                    textTimer = 0;
                    visibleChars++;
                }
            }
        } else {
            skipRequested = false;
        }

        // Phòng 1
        if (currentPhase == 0) {
            showDialog("Sử dụng [WASD] để di chuyển");
            currentPhase = 1;
        } 
        else if (currentPhase == 1) {
            if (isMoving) {
                moveTimer++;
                if (moveTimer >= 60) { // Đã di chuyển được 1 giây (60 frame)
                    hideDialog();
                    currentPhase = 2;
                }
            }
        }
        // Phòng 2 (Qua cánh cổng tọa độ X=576)
        else if (currentPhase == 2) {
            if (player.getX() > 14 * TILE && dialogState == DialogState.HIDDEN) {
                showDialog("Sử dụng [J] để tấn công thường");
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
                hideDialog();
                currentPhase = 4;
            }
        }
        // Phòng 3 (Qua cánh cổng tọa độ X=1392)
        else if (currentPhase == 4) {
            if (player.getX() > 31 * TILE && dialogState == DialogState.HIDDEN) {
                showDialog("Sử dụng [L] để sử dụng [cuồng nộ]\n- tăng sát thương đòn đánh thường - tiêu hao 10 mana");
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
                hideDialog();
                currentPhase = 6;
            }
        }
        // Phòng 4 (Qua cánh cổng tọa độ X=2160)
        else if (currentPhase == 6) {
            if (player.getX() > 47 * TILE && dialogState == DialogState.HIDDEN) {
                showDialog("Combo: chém liên tiếp vào quái sẽ tăng combo,\ncombo càng cao sát thương càng lớn");
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
                hideDialog();
                currentPhase = 8;
            }
        }
        // Phòng 4 - Nhận Damage
        else if (currentPhase >= 8) {
            if (tookDamage && !isDamageDialogQueued && currentPhase == 8) {
                isDamageDialogQueued = true;
            }
            
            // Hộp thoại máu chỉ hiển thị khi hộp thoại combo đã thu xuống hoàn toàn
            if (isDamageDialogQueued && dialogState == DialogState.HIDDEN) {
                showDialog("Cẩn thận, player sẽ mất máu\nkhi bị quái đánh trúng đấy!");
                isDamageDialogQueued = false;
                currentPhase = 9; 
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (dialogState != DialogState.HIDDEN) {
            renderPanel(gc, "Tutorial", currentDialogText);
        }
    }

    private void renderPanel(GraphicsContext gc, String title, String body) {
        gc.save();
        
        double targetY = GameConstants.WINDOW_HEIGHT - 275;
        double height = 230;
        double offsetY = 0;
        double t = (double) animTimer / ANIM_DURATION;

        if (dialogState == DialogState.APPEARING) {
            double progress = 1.0 - Math.pow(1.0 - t, 2); // Ease Out Quad
            offsetY = height * (1.0 - progress);
        } else if (dialogState == DialogState.DISAPPEARING) {
            double progress = Math.pow(t, 2); // Ease In Quad
            offsetY = height * progress;
        }

        double x = 220;
        double y = targetY + offsetY;
        double width = GameConstants.WINDOW_WIDTH - 440;

        if (cmtBox != null) {
            gc.drawImage(cmtBox, x, y, width, height);
        } else {
            gc.setFill(Color.rgb(0, 0, 0, 0.82));
            gc.fillRoundRect(x, y, width, height, 14, 14);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(3);
            gc.strokeRoundRect(x, y, width, height, 14, 14);
        }

        gc.setFont(Font.font(font.getFamily(), FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        drawOutlinedText(gc, title, x + width / 2.0, y + 44, Color.ORANGE);

        gc.setFont(font);
        gc.setTextAlign(TextAlignment.LEFT);
        double textY = y + 88;
        int charsToDraw = Math.min(visibleChars, body.length());
        for (String line : body.substring(0, charsToDraw).split("\n")) {
            // Căn giữa dòng chữ
            javafx.scene.text.Text tempNode = new javafx.scene.text.Text(line);
            tempNode.setFont(font);
            double lineWidth = tempNode.getLayoutBounds().getWidth();
            double lineX = x + (width - lineWidth) / 2.0;
            drawOutlinedText(gc, line, lineX, textY, Color.WHITE);
            textY += 34;
        }
        gc.restore();
    }

    private void drawOutlinedText(GraphicsContext gc, String text, double x, double y, Color fill) {
        gc.setStroke(Color.BLACK);
        gc.setFill(fill);
        gc.setLineWidth(2.5);
        gc.strokeText(text, x, y);
        gc.fillText(text, x, y);
    }
}