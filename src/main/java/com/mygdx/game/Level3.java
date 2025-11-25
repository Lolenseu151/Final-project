package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Level3 - edit positions to design
 */
public class Level3 implements Level {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    private int totalDocs = 0;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    @Override
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Level 2 design (harder than Level 1)
        platforms.add(new Rectangle(0, 0, w, PLATFORM_H));
        platforms.add(new Rectangle(100, 150, 400, PLATFORM_H));
        platforms.add(new Rectangle(w - 350, 250, 300, PLATFORM_H));

        documents.add(new Rectangle(200, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(w - 250, 270, DOC_SIZE, DOC_SIZE));

        obstacles.add(new Rectangle(250, 150, 60, 10));
        obstacles.add(new Rectangle(w - 300, 250, 60, 10));
        beams.add(new Rectangle(400, 0, 5, h));

        shredder = new Rectangle(w - 80, 10, 50, 50);
        totalDocs = documents.size;
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }
    @Override public Rectangle getShredder() { return shredder; }
    @Override public int getTotalDocuments() { return totalDocs; }
    @Override public void dispose() {}
}