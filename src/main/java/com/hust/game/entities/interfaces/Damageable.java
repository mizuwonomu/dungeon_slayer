package com.hust.game.entities.interfaces;

//contract với thg nào làm HUD, nên kế thừa những base sau
public interface Damageable {
    void takeDamage(int amount); // abstract method cần khai báo - khi bị nhận damage

    int getCurrentHp(); // máu của player hiện tại

    int getMaxHp(); // máu cao nhất 1 player có thể sở hữu

    boolean isDead(); // đã ngủm hay chưa
}