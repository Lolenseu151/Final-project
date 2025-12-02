package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Levels.Level;
import com.mygdx.game.Levels.Shredder;

/**
 * Common interface for LevelManager and LevelManager2 so BackgroundedLevel
 * can accept either implementation without type conflicts.
 */
public interface ILevelManager {
    int getDocumentsCollected();
    int getTotalDocuments();
    boolean isLevelComplete();
    int getDocumentsRemaining();
    void loadLevel(Level level);
    Shredder getSharedShredder(); // LevelManager2 has this, LevelManager can return null
    float update(float deltaTime, Fixer player); // Returns time penalty from auditor beams
}