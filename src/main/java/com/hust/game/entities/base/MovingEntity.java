package com.hust.game.entities.base;

import com.hust.game.entities.Direction;
import com.hust.game.entities.EntityState;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//định nghĩa tầng trung gian với thực thể có thể di chuyển như player, enemy
//gom các thứ chung lại: speed, direction, state, và các hàm moveUp/Down/Left/Right 
// — để Player và Enemy khỏi phải viết lại
public abstract class MovingEntity extends BaseEntity{
    protected double speed;
    protected Direction direction = Direction.DOWN;
    protected EntityState state = EntityState.IDLE;

    protected MovingEntity(double x, double y, Image spriteSheet,
            int numFrames, double renderWidth, double renderHeight,
            double speed) {
        super(x, y, spriteSheet, numFrames, renderWidth, renderHeight);
        this.speed = speed;
    }

    // di chuyển cơ bản - subclass tự gọi
    public void moveUp() {
        y -= speed;
    }

    public void moveDown() {
        y += speed;
    }

    public void moveLeft() {
        x -= speed;
    }

    public void moveRight() {
        x += speed;
    }

}
