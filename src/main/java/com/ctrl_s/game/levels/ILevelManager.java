package com.ctrl_s.game.levels;

import com.ctrl_s.game.entities.Player;
import com.ctrl_s.game.levels.levels.Level;

/**
 * Interface for Level Managers to ensure consistency between single and
 * multi-level managers
 */
public interface ILevelManager {
    void loadLevel(Level level);

    float update(float deltaTime, Player player);

    boolean isLevelComplete();

    int getDocumentsCollected();

    int getTotalDocuments();

    void dispose();
}
