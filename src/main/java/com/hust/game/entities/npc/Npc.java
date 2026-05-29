package com.hust.game.entities.npc;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.StaticEntity;
import com.hust.game.entities.player.Player;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class Npc extends StaticEntity {
    private enum State {
        FAR_IDLE,
        NEAR_PROMPT,
        MENU,
        DIALOGUE,
        SHOP,
        APPROVAL
    }

    private enum PanelState { HIDDEN, APPEARING, VISIBLE, DISAPPEARING }

    private static final int ANIMATION_DELAY = 10;
    private static final int DIALOGUE_DELAY = 2;
    private static final int APPROVAL_FRAMES = 8 * ANIMATION_DELAY; // Approval có 8 frames
    private static final double INTERACTION_RANGE = GameConstants.TILE_SIZE;

    private final Image idle1;
    private final Image idle2;
    private final Image idle3;
    private final Image dialogue;
    private final Image approval;
    private final Image manaPotion;
    private final Image healthPotion;
    private final Font font;
    private Image cmtBox;

    // --- Main State Machine ---
    private State state = State.FAR_IDLE;
    private int animTimer = 0;
    private int idleSwapTimer = 0;
    private boolean useIdle3 = false;
    private boolean suppressPromptUntilLeave = false;
    private int dialogueVisibleChars = 0;
    private int dialogueTimer = 0;
    private int approvalTimer = 0;
    private String feedbackText = "";
    private int feedbackTimer = 0;
    private State postCloseState = State.FAR_IDLE;
    private boolean playedGreeting = false;
    // --- Panel Animation State ---
    private PanelState panelState = PanelState.HIDDEN;
    private int panelAnimTimer = 0;
    private static final int PANEL_ANIM_DURATION = 30;

    private static final String PROMPT_TEXT = "Bấm H để tương tác";
    private static final String TIP_TEXT = "Cách đánh quái đơn giản lắm! Bạn chỉ cần canh thời gian đánh trúng!\n"
            + "Nếu thấy quái chuẩn bị đánh, hãy né hoặc chém đúng nhịp để phản đòn.";

    public Npc(double x, double y, Image idle1, Image idle2, Image idle3,
            Image dialogue, Image approval, Image manaPotion, Image healthPotion) {
        super(x, y, idle1, 6, GameConstants.TILE_SIZE * 2, GameConstants.TILE_SIZE * 2); // idle1 có 6 frames, x2 kích thước
        this.idle1 = idle1;
        this.idle2 = idle2;
        this.idle3 = idle3;
        this.dialogue = dialogue;
        this.approval = approval;
        this.manaPotion = manaPotion;
        this.healthPotion = healthPotion;

        java.io.InputStream fontStream = getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf");
        Font loadedFont = fontStream != null ? Font.loadFont(fontStream, 24) : null;
        this.font = loadedFont != null ? loadedFont : Font.font("Arial", FontWeight.BOLD, 24);
        
        try {
            this.cmtBox = new Image(getClass().getResourceAsStream("/assets/cmt_box.png"));
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh cmt_box.png trong /assets/");
        }
        setSheet(idle1);
    }

    public void update(Player player) {
        boolean nearPlayer = isPlayerNear(player);
        if (!nearPlayer) {
            suppressPromptUntilLeave = false;
        }

        if (panelState == PanelState.HIDDEN && (state == State.FAR_IDLE || state == State.NEAR_PROMPT)) {
            if (nearPlayer && !suppressPromptUntilLeave) {
                if (state == State.FAR_IDLE) {
                    state = State.NEAR_PROMPT;
                    setSheet(idle2);
                    playedGreeting = false;
                }
            } else {
                if (!(spriteSheet == idle2 && !playedGreeting)) {
                    state = State.FAR_IDLE;
                    updateFarIdleSheet();
                }
            }
        } else if (state == State.APPROVAL) {
            approvalTimer--;
            if (approvalTimer <= 0) {
                state = State.FAR_IDLE;
                suppressPromptUntilLeave = true;
                setSheet(idle1);
            }
        } else if (state == State.DIALOGUE) {
            dialogueTimer++;
            if (dialogueTimer >= DIALOGUE_DELAY) {
                dialogueTimer = 0;
                if (dialogueVisibleChars < TIP_TEXT.length()) {
                    dialogueVisibleChars++;
                }
            }
        }

        animate();

        if (feedbackTimer > 0) {
            feedbackTimer--;
            if (feedbackTimer <= 0) {
                feedbackText = "";
            }
        }

        // --- Panel Animation Logic ---
        if (panelState == PanelState.APPEARING) {
            panelAnimTimer++;
            if (panelAnimTimer >= PANEL_ANIM_DURATION) {
                panelAnimTimer = PANEL_ANIM_DURATION;
                panelState = PanelState.VISIBLE;
            }
        } else if (panelState == PanelState.DISAPPEARING) {
            panelAnimTimer++;
            if (panelAnimTimer >= PANEL_ANIM_DURATION) {
                panelAnimTimer = 0;
                panelState = PanelState.HIDDEN;
                // Now that panel is hidden, transition to the post-close state
                state = postCloseState;
                setSheet(idle1);
            }
        }
    }

    public boolean isInteractionOpen() {
        return panelState != PanelState.HIDDEN;
    }

    public boolean canStartInteraction() {
        return state == State.NEAR_PROMPT && panelState == PanelState.HIDDEN;
    }

    public void startInteraction() {
        if (!canStartInteraction()) {
            return;
        }
        state = State.MENU;
        panelState = PanelState.APPEARING;
        panelAnimTimer = 0;
        setSheet(idle1);
    }

    public void handleOptionOne(Player player) {
        if (state == State.MENU) {
            state = State.DIALOGUE;
            dialogueVisibleChars = 0;
            dialogueTimer = 0;
            setSheet(idle1);
        } else if (state == State.SHOP) {
            showFeedback(player.addManaPotion() ? "Đã nhận Mana Potion" : "Túi đồ đã đầy");
        }
    }

    public void handleOptionTwo(Player player) {
        if (state == State.MENU) {
            state = State.SHOP;
            setSheet(dialogue);
        } else if (state == State.SHOP) {
            showFeedback(player.addHealthPotion() ? "Đã nhận Health Potion" : "Túi đồ đã đầy");
        }
    }

    public void skipDialogue() {
        if (state == State.DIALOGUE) {
            dialogueVisibleChars = TIP_TEXT.length();
        }
    }

    public void closeInteraction() {
        if (!isInteractionOpen()) {
            return;
        }
        suppressPromptUntilLeave = true;
        
        // Decide where to go after closing, but don't go there yet.
        postCloseState = State.FAR_IDLE;

        // Trigger the closing animation
        panelState = PanelState.DISAPPEARING;
        panelAnimTimer = 0;
    }

    public void renderOverlay(GraphicsContext gc) {
        if (state == State.NEAR_PROMPT && panelState == PanelState.HIDDEN) {
            renderPrompt(gc);
        } else if (panelState != PanelState.HIDDEN) {
            // Calculate animated Y position for the panel
            double targetY = GameConstants.WINDOW_HEIGHT - 275;
            double height = 230;
            double offsetY = 0;
            double t = (double) panelAnimTimer / PANEL_ANIM_DURATION;

            if (panelState == PanelState.APPEARING) {
                double progress = 1.0 - Math.pow(1.0 - t, 2); // Ease Out Quad
                offsetY = height * (1.0 - progress);
            } else if (panelState == PanelState.DISAPPEARING) {
                double progress = Math.pow(t, 2); // Ease In Quad
                offsetY = height * progress;
            }
            double animatedY = targetY + offsetY;

            // Render the correct panel based on the internal NPC state
            if (state == State.MENU) {
                renderPanel(gc, "NPC", "[1] Tips and tricks\n[2] Buy items\n[O] Thoát", animatedY);
            } else if (state == State.DIALOGUE) {
                int chars = Math.min(dialogueVisibleChars, TIP_TEXT.length());
                renderPanel(gc, "Tips and tricks", TIP_TEXT.substring(0, chars) + "\n\n[O] Thoát", animatedY);
            } else if (state == State.SHOP) {
                renderShop(gc, animatedY);
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (spriteSheet == null) {
            return;
        }

        // Vẽ NPC theo Grid-based, tránh hiện tượng trượt hình khi crop pixel động
        double sx = frameIndex * frameWidth;
        double scale = renderHeight / frameHeight;
        double drawW = frameWidth * scale;
        double drawH = renderHeight;

        double drawX = x + (renderWidth - drawW) / 2.0;
        double drawY = y + renderHeight - drawH;

        gc.drawImage(spriteSheet, sx, 0, frameWidth, frameHeight, drawX, drawY, drawW, drawH);
    }

    private void renderPrompt(GraphicsContext gc) {
        gc.save();
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        double centerX = GameConstants.WINDOW_WIDTH / 2.0;
        double y = GameConstants.WINDOW_HEIGHT - 130;
        double width = 360;
        double height = 54;
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRoundRect(centerX - width / 2.0, y - height / 2.0, width, height, 12, 12);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(centerX - width / 2.0, y - height / 2.0, width, height, 12, 12);
        drawOutlinedText(gc, PROMPT_TEXT, centerX, y, Color.WHITE);
        gc.restore();
    }

    private void renderPanel(GraphicsContext gc, String title, String body, double yPos) {
        gc.save();
        double x = 220;
        double y = yPos;
        double width = GameConstants.WINDOW_WIDTH - 440;
        double height = 230;

        // Vẽ hình ảnh comment box nếu có, ngược lại fallback về vẽ bằng code như cũ
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
        for (String line : body.split("\n")) {
            drawOutlinedText(gc, line, x + 46, textY, Color.WHITE);
            textY += 34;
        }
        gc.restore();
    }

    private void renderShop(GraphicsContext gc, double yPos) {
        gc.save();
        renderPanel(gc, "Buy items", "[1] Mana Potion\n[2] Health Potion\n[O] Hoàn tất", yPos);

        double iconY = yPos + 110; // yPos is the top of the panel, text starts at y+88, so this is ~line 1
        drawPotion(gc, manaPotion, 560, iconY);
        drawPotion(gc, healthPotion, 560, iconY + 34);

        if (!feedbackText.isEmpty()) {
            gc.setFont(font);
            gc.setTextAlign(TextAlignment.CENTER);
            double height = 230;
            drawOutlinedText(gc, feedbackText, GameConstants.WINDOW_WIDTH / 2.0, yPos + height + 30, Color.LIGHTGREEN);
        }
        gc.restore();
    }

    private void drawPotion(GraphicsContext gc, Image sheet, double x, double y) {
        if (sheet == null) {
            return;
        }
        double frameW = sheet.getWidth() / 8.0;
        double frameH = sheet.getHeight();
        gc.drawImage(sheet, 0, 0, frameW, frameH, x, y - 24, 32, 32);
    }

    private void drawOutlinedText(GraphicsContext gc, String text, double x, double y, Color fill) {
        gc.setStroke(Color.BLACK);
        gc.setFill(fill);
        gc.setLineWidth(2.5);
        gc.strokeText(text, x, y);
        gc.fillText(text, x, y);
    }

    private void updateFarIdleSheet() {
        // Tạm bỏ qua idle3
        setSheet(idle1);
    }

    private void animate() {
        animTimer++;
        if (animTimer >= ANIMATION_DELAY) {
            animTimer = 0;
            
            if (spriteSheet == idle2 && !playedGreeting) {
                if (frameIndex == numFrames - 1) {
                    playedGreeting = true;
                    setSheet(idle1);
                } else {
                    frameIndex++;
                }
            } else if (spriteSheet == dialogue) {
                if (frameIndex >= 6) {
                    frameIndex = 3;
                } else {
                    frameIndex++;
                }
            } else {
                frameIndex = (frameIndex + 1) % numFrames;
            }
        }
    }

    @Override
    public void update() {
        // Trống, vì NPC sử dụng logic qua hàm update(Player player)
    }

    private void setSheet(Image sheet) {
        if (spriteSheet == sheet) {
            return;
        }
        spriteSheet = sheet;
        
        if (sheet == idle1) {
            numFrames = 6;
        } else if (sheet == idle2) {
            numFrames = 8;
        } else if (sheet == idle3) {
            numFrames = 7;
        } else if (sheet == dialogue) {
            numFrames = 8;
        } else if (sheet == approval) {
            numFrames = 8;
        }
        
        frameWidth = spriteSheet.getWidth() / numFrames;
        frameHeight = spriteSheet.getHeight();
        frameIndex = 0;
        animTimer = 0;
    }

    private boolean isPlayerNear(Player player) {
        if (player == null) {
            return false;
        }
        Rectangle2D range = new Rectangle2D(
                x - INTERACTION_RANGE,
                y - INTERACTION_RANGE,
                renderWidth + INTERACTION_RANGE * 2,
                renderHeight + INTERACTION_RANGE * 2);
        return range.intersects(player.getCollisionBoundary());
    }

    private void showFeedback(String text) {
        feedbackText = text;
        feedbackTimer = 120;
    }
}
