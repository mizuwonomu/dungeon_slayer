package com.hust.game.combat;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleManager {
    private static class Particle {
        double x, y;
        double vx, vy;
        int life, maxLife;
        Color color;
        double size;

        public Particle(double x, double y, double vx, double vy, int maxLife, Color color, double size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; 
            this.maxLife = maxLife; this.life = maxLife;
            this.color = color; this.size = size;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public void spawnBlood(double x, double y, com.hust.game.entities.Direction dir) {
        int count = 12 + random.nextInt(8); // 12-20 hạt máu
        for (int i = 0; i < count; i++) {
            double angle = 0;
            // Bắn theo hướng chém hình nón (khoảng 90 độ)
            switch (dir) {
                case RIGHT: angle = -Math.PI / 4 + random.nextDouble() * Math.PI / 2; break;
                case LEFT:  angle = Math.PI - Math.PI / 4 + random.nextDouble() * Math.PI / 2; break;
                case UP:    angle = -Math.PI / 2 - Math.PI / 4 + random.nextDouble() * Math.PI / 2; break;
                case DOWN:  angle = Math.PI / 2 - Math.PI / 4 + random.nextDouble() * Math.PI / 2; break;
            }
            
            double speed = 10 + random.nextDouble() * 15; // Tốc độ văng xa hơn rất nhiều
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            int life = 20 + random.nextInt(20); // Tồn tại lâu hơn một chút để bay được xa
            double size = 4 + random.nextDouble() * 6; // Kích thước pixel 4-10
            
            // Màu đỏ ngẫu nhiên (Đỏ thẫm đến Đỏ tươi)
            Color color = Color.rgb(150 + random.nextInt(105), 0, 0);
            
            particles.add(new Particle(x, y, vx, vy, life, color, size));
        }
    }

    public void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            
            // Giảm tốc độ dần (Ma sát)
            p.vx *= 0.90; // Giảm ma sát để hạt trượt đi xa hơn
            p.vy *= 0.90;
            
            p.life--;
            if (p.life <= 0) {
                it.remove();
            }
        }
    }

    public void render(GraphicsContext gc) {
        for (Particle p : particles) {
            gc.save();
            double alpha = (double) p.life / p.maxLife;
            gc.setGlobalAlpha(alpha);
            gc.setFill(p.color);
            gc.fillRect(p.x, p.y, p.size, p.size);
            gc.restore();
        }
    }
}