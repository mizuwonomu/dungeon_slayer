package com.hust.game.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

public class DamageTextManager {
    private static class DamageText {
        Object target; // Đối tượng gắn với sát thương này (Player hoặc Enemy)
        double x, y;
        String text;
        int timer, maxTimer;
        Color color;

        DamageText(Object target, double x, double y, String text, Color color) {
            this.target = target;
            this.x = x;
            this.y = y;
            this.text = text;
            this.timer = 45; // Tồn tại trong 45 frames (0.75 giây)
            this.maxTimer = 45;
            this.color = color;
        }
    }

    private static final List<DamageText> texts = new ArrayList<>();
    private static Font customFont;

    // Khối static này sẽ chạy 1 lần duy nhất khi class được gọi lần đầu
    static {
        try {
            // Tải font từ thư mục src/main/resources/fonts/ (Bạn nhớ copy file font vào đây)
            customFont = Font.loadFont(DamageTextManager.class.getResourceAsStream("/fonts/PixelFont.ttf"), 22);
        } catch (Exception e) {
            System.err.println("Lỗi tải font, sử dụng font mặc định!");
        }
        if (customFont == null) {
            customFont = Font.font("Arial", FontWeight.BOLD, 22);
        }
    }

    public static Font getCustomFont() {
        return customFont;
    }

    public static void addText(Object target, double x, double y, String text, Color color) {
        texts.removeIf(dt -> dt.target == target); // Xóa ngay số cũ của cùng một mục tiêu
        texts.add(new DamageText(target, x, y, text, color));
    }

    public static void update() {
        texts.removeIf(dt -> --dt.timer <= 0); // Giảm timer và xoá nếu hết thời gian
        for (DamageText dt : texts) {
            dt.y -= 1.5; // Bay dần lên trên mỗi frame
        }
    }

    public static void render(GraphicsContext gc) {
        gc.save();
        gc.setFont(customFont); // Sử dụng font đã tải ở khối static
        for (DamageText dt : texts) {
            double alpha = 1.0;
            if (dt.timer <= 15) { // Nét trong 30 frames (0.5s) đầu, chỉ mờ dần trong 15 frames cuối
                alpha = (double) dt.timer / 15.0;
            }
            gc.setGlobalAlpha(alpha);
            
            // Vẽ viền đen bọc xung quanh chữ (Outline stroke)
            gc.setLineWidth(2.5); // Bạn có thể chỉnh số này to nhỏ tùy ý (vd: 1.0, 2.0) để viền mỏng hay dày
            gc.setStroke(Color.BLACK);
            gc.strokeText(dt.text, dt.x, dt.y);
            
            gc.setFill(dt.color); gc.fillText(dt.text, dt.x, dt.y); // Vẽ màu chính
        }
        gc.restore();
    }
}