package com.hust.game.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.hust.game.constants.GameConstants;

public class DialogBox {
    private Image boxImage;
    private String currentText = "";
    private Font font;
    
    // Các trạng thái của Dialog
    private enum State { HIDDEN, APPEARING, VISIBLE, DISAPPEARING }
    private State state = State.HIDDEN;
    
    private int animTimer = 0;
    private static final int ANIM_DURATION = 30; // 0.5 giây = 30 frame
    
    private double renderWidth, renderHeight;
    private double targetY; // Tọa độ Y khi hiện hoàn toàn
    
    // Bộ đếm thời gian cho các Dialog tự biến mất
    private int visibleTimer = 0;
    private int maxVisibleTime = -1; // -1 nghĩa là hiển thị vô hạn tới khi gọi hide()

    // Hiệu ứng chữ chạy (Typewriter)
    private int visibleChars = 0;
    private int textTimer = 0;
    private static final int TEXT_DELAY = 2; // Số frame để hiện 1 chữ

    public DialogBox() {
        try {
            boxImage = new Image(getClass().getResourceAsStream("/assets/cmt_box.png"));
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không tìm thấy /assets/cmt_box.png");
        }
        
        try {
            // Đổi font chữ xuống cỡ 28 cho giống màn tutorial
            font = Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 28);
        } catch (Exception e) {}
        if (font == null) font = Font.font("Arial", FontWeight.BOLD, 28);
        
        // Kích thước gốc 1280x320 hơi to, ta scale xuống để vừa vặn cửa sổ 1440
        renderWidth = 1000;
        renderHeight = 250;
        
        // Mép dưới của hộp chạm đúng vào mép dưới màn hình
        targetY = GameConstants.WINDOW_HEIGHT - renderHeight; 
    }

    public void show(String text, int durationFrames) {
        this.currentText = text;
        this.maxVisibleTime = durationFrames;
        this.visibleTimer = 0;
        this.visibleChars = 0;
        this.textTimer = 0;
        this.state = State.APPEARING;
        this.animTimer = 0;
    }

    public void hide() {
        if (state == State.VISIBLE || state == State.APPEARING) {
            state = State.DISAPPEARING;
            animTimer = 0; // Đặt lại timer để bắt đầu chạy hiệu ứng trượt xuống
        }
    }
    
    public boolean isHidden() {
        return state == State.HIDDEN;
    }

    public void update() {
        if (state == State.APPEARING) {
            animTimer++;
            if (animTimer >= ANIM_DURATION) {
                animTimer = ANIM_DURATION;
                state = State.VISIBLE;
            }
        } else if (state == State.DISAPPEARING) {
            animTimer++;
            if (animTimer >= ANIM_DURATION) {
                animTimer = 0;
                state = State.HIDDEN;
            }
        } else if (state == State.VISIBLE) {
            // Typewriter effect
            if (visibleChars < currentText.length()) {
                textTimer++;
                if (textTimer >= TEXT_DELAY) {
                    textTimer = 0;
                    visibleChars++;
                }
            }

            if (maxVisibleTime > 0) {
                visibleTimer++;
                if (visibleTimer >= maxVisibleTime) {
                    hide();
                }
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (state == State.HIDDEN || boxImage == null) return;
        
        double drawX = (GameConstants.WINDOW_WIDTH - renderWidth) / 2.0; // Canh giữa trục X
        double offsetY = 0;
        
        double t = (double) animTimer / ANIM_DURATION;
        if (state == State.APPEARING) {
            // Hàm trượt: Nhanh lúc đầu, chậm dần ở cuối (Ease Out Quad)
            double progress = 1.0 - Math.pow(1.0 - t, 2);
            offsetY = renderHeight * (1.0 - progress);
        } else if (state == State.DISAPPEARING) {
            // Hàm trượt: Chậm lúc đầu, nhanh dần ở cuối (Ease In Quad)
            double progress = Math.pow(t, 2);
            offsetY = renderHeight * progress;
        }
        
        double drawY = targetY + offsetY;
        
        gc.drawImage(boxImage, drawX, drawY, renderWidth, renderHeight);
        
        // Vẽ chữ căn giữa (truyền toạ độ và chiều rộng hộp thoại)
        renderRichText(gc, currentText, drawX, drawY + 80, renderWidth);
    }
    
    private void renderRichText(GraphicsContext gc, String fullText, double boxX, double boxY, double boxWidth) {
        gc.setFont(font);
        gc.setLineWidth(2.5);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT); // CHỐT CĂN TRÁI ĐỂ KHÔNG BỊ LỆCH KHI TÍNH TOẠ ĐỘ X
        gc.setTextBaseline(javafx.geometry.VPos.TOP); // Đỉnh chữ trùng với tọa độ Y
        
        double currentY = boxY;
        
        // Cắt string theo số lượng ký tự đã hiển thị (Typewriter effect)
        int charsToDraw = Math.min(visibleChars, fullText.length());
        String visibleText = fullText.substring(0, charsToDraw);
        
        String[] lines = visibleText.split("\n", -1); // Tự động ngắt dòng nếu có \n
        String[] fullLines = fullText.split("\n", -1); // Dùng fullLines để lấy độ dài tĩnh, tránh text bị nở ra từ giữa
        
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String fullLine = fullLines[lineIndex];
            
            // Loại bỏ ngoặc vuông để tính toán chính xác bề rộng của dòng chữ CHUẨN (full length)
            String rawFullLine = fullLine.replace("[", "").replace("]", "");
            javafx.scene.text.Text tempNode = new javafx.scene.text.Text(rawFullLine);
            tempNode.setFont(font);
            double lineWidth = tempNode.getLayoutBounds().getWidth();
            
            // Căn giữa trục X dựa trên độ dài TỔNG của dòng, giúp chữ không bị chạy từ giữa ra 2 bên
            double currentX = boxX + (boxWidth - lineWidth) / 2.0;
            
            boolean isHighlight = false;
            StringBuilder currentPart = new StringBuilder();
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '[') { // Bắt đầu đoạn Highlight
                    if (currentPart.length() > 0) {
                        currentX = drawTextPart(gc, currentPart.toString(), currentX, currentY, Color.WHITE);
                        currentPart.setLength(0);
                    }
                    isHighlight = true;
                } else if (c == ']') { // Kết thúc đoạn Highlight
                    if (currentPart.length() > 0) {
                        currentX = drawTextPart(gc, currentPart.toString(), currentX, currentY, Color.ORANGE);
                        currentPart.setLength(0);
                    }
                    isHighlight = false;
                } else {
                    currentPart.append(c);
                }
            }
            // Vẽ phần cuối cùng còn sót lại
            if (currentPart.length() > 0) {
                drawTextPart(gc, currentPart.toString(), currentX, currentY, isHighlight ? Color.ORANGE : Color.WHITE);
            }
            currentY += 40; // Giãn dòng 40px cho font size 28
        }
        // Khôi phục cài đặt mặc định
        gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
    
    private double drawTextPart(GraphicsContext gc, String part, double x, double y, Color fill) {
        gc.setStroke(Color.BLACK);
        gc.setFill(fill);
        gc.strokeText(part, x, y);
        gc.fillText(part, x, y);
        
        // Dùng Node ảo để tính toán bề rộng pixel của chữ vừa vẽ, giúp chữ tiếp theo nối đuôi chính xác
        javafx.scene.text.Text tempNode = new javafx.scene.text.Text(part);
        tempNode.setFont(font);
        return x + tempNode.getLayoutBounds().getWidth();
    }
}