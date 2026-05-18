package com.hust.game.entities.npc;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.StaticEntity;
import com.hust.game.entities.player.Player;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.IdentityHashMap;
import java.util.Map;

public class Npc extends StaticEntity {
    private enum State {
        FAR_IDLE,
        NEAR_PROMPT,
        MENU,
        DIALOGUE,
        SHOP,
        APPROVAL
    }

    private static final int FRAMES = 8;
    private static final int ANIMATION_DELAY = 10;
    private static final int DIALOGUE_DELAY = 2;
    private static final int APPROVAL_FRAMES = FRAMES * ANIMATION_DELAY;
    private static final double INTERACTION_RANGE = GameConstants.TILE_SIZE;

    private final Image idle1;
    private final Image idle2;
    private final Image idle3;
    private final Image dialogue;
    private final Image approval;
    private final Image manaPotion;
    private final Image healthPotion;
    private final Font font;
    private final Map<Image, SheetBounds> boundsCache = new IdentityHashMap<>();

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

    private static final String PROMPT_TEXT = "Bấm H để tương tác";
    private static final String TIP_TEXT = "Cách đánh quái đơn giản lắm! Bạn chỉ cần canh thời gian đánh trúng!\n"
            + "Nếu thấy quái chuẩn bị đánh, hãy né hoặc chém đúng nhịp để phản đòn.";

    public Npc(double x, double y, Image idle1, Image idle2, Image idle3,
            Image dialogue, Image approval, Image manaPotion, Image healthPotion) {
        super(x, y, idle1, FRAMES, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
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
        setSheet(idle1);
    }

    public void update(Player player) {
        boolean nearPlayer = isPlayerNear(player);
        if (!nearPlayer) {
            suppressPromptUntilLeave = false;
        }

        if (state == State.FAR_IDLE || state == State.NEAR_PROMPT) {
            if (nearPlayer && !suppressPromptUntilLeave) {
                state = State.NEAR_PROMPT;
                setSheet(idle2);
            } else {
                state = State.FAR_IDLE;
                updateFarIdleSheet();
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
    }

    public boolean isInteractionOpen() {
        return state == State.MENU || state == State.DIALOGUE || state == State.SHOP;
    }

    public boolean canStartInteraction() {
        return state == State.NEAR_PROMPT;
    }

    public void startInteraction() {
        if (!canStartInteraction()) {
            return;
        }
        state = State.MENU;
        setSheet(idle2);
    }

    public void handleOptionOne(Player player) {
        if (state == State.MENU) {
            state = State.DIALOGUE;
            dialogueVisibleChars = 0;
            dialogueTimer = 0;
            setSheet(dialogue);
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

    public void closeInteraction() {
        if (!isInteractionOpen()) {
            return;
        }
        boolean approved = state == State.SHOP;
        suppressPromptUntilLeave = true;
        if (approved) {
            state = State.APPROVAL;
            approvalTimer = APPROVAL_FRAMES;
            setSheet(approval);
        } else {
            state = State.FAR_IDLE;
            setSheet(idle1);
        }
    }

    public void renderOverlay(GraphicsContext gc) {
        if (state == State.NEAR_PROMPT) {
            renderPrompt(gc);
        } else if (state == State.MENU) {
            renderPanel(gc, "NPC", "[1] Tips and tricks\n[2] Buy items\n[O] Thoát");
        } else if (state == State.DIALOGUE) {
            int chars = Math.min(dialogueVisibleChars, TIP_TEXT.length());
            renderPanel(gc, "Tips and tricks", TIP_TEXT.substring(0, chars) + "\n\n[O] Thoát");
        } else if (state == State.SHOP) {
            renderShop(gc);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (spriteSheet == null) {
            return;
        }

        SheetBounds bounds = getSheetBounds(spriteSheet);
        FrameCrop crop = bounds.frames[Math.max(0, Math.min(frameIndex, bounds.frames.length - 1))];

        double scale = renderHeight / bounds.unionHeight;
        double drawX = x + renderWidth / 2.0 - (bounds.unionWidth * scale) / 2.0
                + (crop.x - bounds.unionX) * scale;
        double drawY = y + renderHeight - (bounds.unionHeight * scale)
                + (crop.y - bounds.unionY) * scale;

        gc.drawImage(spriteSheet,
                crop.sourceX, crop.y, crop.width, crop.height,
                drawX, drawY, crop.width * scale, crop.height * scale);
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

    private void renderPanel(GraphicsContext gc, String title, String body) {
        gc.save();
        double x = 220;
        double y = GameConstants.WINDOW_HEIGHT - 275;
        double width = GameConstants.WINDOW_WIDTH - 440;
        double height = 230;

        gc.setFill(Color.rgb(0, 0, 0, 0.82));
        gc.fillRoundRect(x, y, width, height, 14, 14);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRoundRect(x, y, width, height, 14, 14);

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

    private void renderShop(GraphicsContext gc) {
        gc.save();
        renderPanel(gc, "Buy items", "[1] Mana Potion\n[2] Health Potion\n[O] Hoàn tất");

        double iconY = GameConstants.WINDOW_HEIGHT - 165;
        drawPotion(gc, manaPotion, 560, iconY);
        drawPotion(gc, healthPotion, 560, iconY + 34);

        if (!feedbackText.isEmpty()) {
            gc.setFont(font);
            gc.setTextAlign(TextAlignment.CENTER);
            drawOutlinedText(gc, feedbackText, GameConstants.WINDOW_WIDTH / 2.0,
                    GameConstants.WINDOW_HEIGHT - 60, Color.LIGHTGREEN);
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
        idleSwapTimer++;
        if (idleSwapTimer >= 180) {
            idleSwapTimer = 0;
            useIdle3 = !useIdle3;
        }
        setSheet(useIdle3 ? idle3 : idle1);
    }

    private void animate() {
        animTimer++;
        if (animTimer >= ANIMATION_DELAY) {
            animTimer = 0;
            frameIndex = (frameIndex + 1) % FRAMES;
        }
    }

    private void setSheet(Image sheet) {
        if (spriteSheet == sheet) {
            return;
        }
        spriteSheet = sheet;
        frameWidth = spriteSheet.getWidth() / FRAMES;
        frameHeight = spriteSheet.getHeight();
        frameIndex = 0;
        animTimer = 0;
    }

    private SheetBounds getSheetBounds(Image sheet) {
        SheetBounds cached = boundsCache.get(sheet);
        if (cached != null) {
            return cached;
        }

        FrameCrop[] crops = new FrameCrop[FRAMES];
        double sheetFrameWidth = sheet.getWidth() / FRAMES;
        int frameW = Math.max(1, (int) Math.round(sheetFrameWidth));
        int frameH = Math.max(1, (int) Math.round(sheet.getHeight()));
        int unionMinX = frameW;
        int unionMinY = frameH;
        int unionMaxX = 0;
        int unionMaxY = 0;

        for (int frame = 0; frame < FRAMES; frame++) {
            FrameCrop crop = findVisibleCrop(sheet, frame, frameW, frameH);
            crops[frame] = crop;
            unionMinX = Math.min(unionMinX, crop.x);
            unionMinY = Math.min(unionMinY, crop.y);
            unionMaxX = Math.max(unionMaxX, crop.x + crop.width);
            unionMaxY = Math.max(unionMaxY, crop.y + crop.height);
        }

        if (unionMaxX <= unionMinX || unionMaxY <= unionMinY) {
            unionMinX = 0;
            unionMinY = 0;
            unionMaxX = frameW;
            unionMaxY = frameH;
        }

        SheetBounds computed = new SheetBounds(
                crops,
                unionMinX,
                unionMinY,
                Math.max(1, unionMaxX - unionMinX),
                Math.max(1, unionMaxY - unionMinY));
        boundsCache.put(sheet, computed);
        return computed;
    }

    private FrameCrop findVisibleCrop(Image sheet, int frame, int frameW, int frameH) {
        PixelReader reader = sheet.getPixelReader();
        int sourceX = (int) Math.round(frame * (sheet.getWidth() / FRAMES));
        if (reader == null) {
            return new FrameCrop(sourceX, 0, 0, frameW, frameH);
        }

        int minX = frameW;
        int minY = frameH;
        int maxX = -1;
        int maxY = -1;
        int maxReadableX = Math.min(frameW, (int) Math.round(sheet.getWidth()) - sourceX);

        for (int py = 0; py < frameH; py++) {
            for (int px = 0; px < maxReadableX; px++) {
                int alpha = (reader.getArgb(sourceX + px, py) >>> 24) & 0xff;
                if (alpha > 8) {
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new FrameCrop(sourceX, 0, 0, frameW, frameH);
        }

        return new FrameCrop(sourceX + minX, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static class FrameCrop {
        final int sourceX;
        final int x;
        final int y;
        final int width;
        final int height;

        FrameCrop(int sourceX, int x, int y, int width, int height) {
            this.sourceX = sourceX;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class SheetBounds {
        final FrameCrop[] frames;
        final int unionX;
        final int unionY;
        final int unionWidth;
        final int unionHeight;

        SheetBounds(FrameCrop[] frames, int unionX, int unionY, int unionWidth, int unionHeight) {
            this.frames = frames;
            this.unionX = unionX;
            this.unionY = unionY;
            this.unionWidth = unionWidth;
            this.unionHeight = unionHeight;
        }
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
