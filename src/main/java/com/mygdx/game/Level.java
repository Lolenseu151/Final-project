package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public interface Level {
    void init();                          // prepare internal lists / positions
    Array<Rectangle> getDocuments();
    Array<Rectangle> getPlatforms();
    Array<Rectangle> getObstacles();
    
    // Lasers (cyan visual beams you can control)
    default Array<Rectangle> getLasers() {
        return new Array<>(); // Default: no lasers
    }
    
    Rectangle getShredder();
    int getTotalDocuments();
    void dispose();
    String getBackgroundPath();
    String getMusicPath();                // return path to level background music
}