package com.hust.game.entities;

import javafx.scene.image.Image;

public class Player extends BaseEntity{
    //preloading 4 direction images (idle)
    private Image idleUp, idleDown, idleLeft, idleRight;

    //4 run images
    private Image runUp, runDown, runLeft, runRight;

    private double speed = 3.0;
    private double lastX, lastY;
    
    //dùng timer để chuyển khung hình
    private int animationTimer = 0;
    private final int animationDelay = 10;

    //current player state direction
    private Direction currentDirection = Direction.DOWN;
    private EntityState currentState = EntityState.IDLE;

    public Player (double x, double y, 
        Image idleDown, Image idleUp, Image idleLeft, Image idleRight,
        Image runDown, Image runUp, Image runLeft, Image runRight, 
        int numFrames, double renderWidth, double renderHeight){

        super(x, y, idleDown, numFrames, renderWidth, renderHeight);
        this.idleDown = idleDown;
        this.idleUp = idleUp;
        this.idleLeft = idleLeft;
        this.idleRight = idleRight;
        this.runUp = runUp;
        this.runLeft = runLeft;
        this.runRight = runRight;
        this.runDown = runDown;
    }

    public void loadOtherDirections(Image iup, Image ileft, Image iright, Image rDown, Image rUp, Image rLeft, Image rRight){
        this.idleUp = iup;
        this.idleLeft = ileft;
        this.idleRight = iright;
        this.runUp = rUp;
        this.runLeft = rLeft;
        this.runRight = rRight;
        this.runDown = rDown;
    }

    public void updateSpriteSheet(){
        if (currentState == EntityState.IDLE){
            switch (currentDirection){
                case UP: this.spriteSheet = idleUp; break;
                case DOWN: this.spriteSheet = idleDown; break;
                case LEFT: this.spriteSheet = idleLeft; break;
                case RIGHT: this.spriteSheet = idleRight; break;
                
            }
        }else if (currentState == EntityState.RUNNING){
            switch (currentDirection) {
                case UP: this.spriteSheet = runUp; break;
                case DOWN: this.spriteSheet = runDown; break;
                case LEFT: this.spriteSheet = runLeft; break;
                case RIGHT: this.spriteSheet = runRight; break;
            }
        }
        this.frameIndex = 0;
    }
    
    @Override
    public void update(){
        //logic update hoạt ảnh đứng yên (idle)
        animationTimer++;
        if (animationTimer >= animationDelay){
            animationTimer = 0;

            //chuyển sang khung tiếp theo, dùng toán tử chia lấy dư để quay về 0
            frameIndex = (frameIndex + 1) % numFrames;
        }
    }

    public void setState(EntityState newState){
        if (this.currentState != newState){
            this.currentState = newState;
            updateSpriteSheet();
        }
    }

    public void setDirection(Direction newDirection){
        if (this.currentDirection != newDirection){
            this.currentDirection = newDirection;
            updateSpriteSheet();
        }
        
    }

    public void onCollision(BaseEntity other){
        this.x = lastX;
        this.y = lastY;
    }

    public void savePosition(){
        this.lastX = x;
        this.lastY = y;
    }

    public void moveUp() { y-= speed; }
    public void moveDown() { y += speed; }
    public void moveLeft() { x -= speed; }
    public void moveRight() { x += speed; }

}