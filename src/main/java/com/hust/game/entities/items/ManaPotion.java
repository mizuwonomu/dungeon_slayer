package com.hust.game.entities.items;

import com.hust.game.constants.GameConstants;
import com.hust.game.entities.base.StaticEntity;
import com.hust.game.entities.interfaces.Interactable;
import com.hust.game.entities.player.Player;
import javafx.scene.image.Image;

public class ManaPotion extends StaticEntity implements Interactable {
    public ManaPotion(double x, double y, Image spriteSheet) {
        super(x, y, spriteSheet,
                GameConstants.POTION_NUM_FRAMES,
                GameConstants.POTION_RENDER_SIZE,
                GameConstants.POTION_RENDER_SIZE);
    }

    @Override
    public boolean onInteract(Player player) {
        return player.addManaPotion();
    }
}
