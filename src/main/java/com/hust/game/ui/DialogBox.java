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

    public DialogBox() {
        try {
            boxImage = new Image(getClass().getResourceAsStream("/assets/cmt_box.png"));
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không tìm thấy /assets/cmt_box.png");
        }
        
        try {
            font = Font.loadFont(getClass().getResourceAsStream("/fonts/Pixel_VIE.ttf"), 36);
        } catch (Exception e) {}
        if (font == null) font = Font.font("Arial", FontWeight.BOLD, 36);
        
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
        renderRichText(gc, currentText, drawX, drawY + 70, renderWidth);
    }
    
    private void renderRichText(GraphicsContext gc, String text, double boxX, double boxY, double boxWidth) {
        gc.setFont(font);
        gc.setLineWidth(3.0); // Làm viền đen dày hơn
        gc.setTextBaseline(javafx.geometry.VPos.TOP); // Đỉnh chữ trùng với tọa độ Y
        
        double currentY = boxY;
        String[] lines = text.split("\n"); // Tự động ngắt dòng nếu có \n
        
        for (String line : lines) {
            // Loại bỏ ngoặc vuông để tính toán chính xác bề rộng của dòng chữ
            String rawLine = line.replace("[", "").replace("]", "");
            javafx.scene.text.Text tempNode = new javafx.scene.text.Text(rawLine);
            tempNode.setFont(font);
            double lineWidth = tempNode.getLayoutBounds().getWidth();
            
            // Căn giữa trục X
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
            currentY += 55; // Line height
        }
        gc.setTextBaseline(javafx.geometry.VPos.BASELINE); // Khôi phục cài đặt mặc định
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