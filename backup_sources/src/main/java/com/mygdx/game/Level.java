package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public interface Level {
    void init();                          // prepare internal lists / positions
    Array<Rectangle> getDocuments();
    Array<Rectangle> getPlatforms();
    Array<Rectangle> getObstacles();
    Array<Rectangle> getAuditorBeams();
    Rectangle getShredder();
    int getTotalDocuments();
    void dispose();
    String getBackgroundPath();
}