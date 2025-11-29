package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Level1 - edit positions to design
 */
public class Level1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    private int totalDocs = 0;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    /* 
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
 */

 @Override
public void init() {
    documents.clear();
    platforms.clear();
    obstacles.clear();
    beams.clear();

    float w = 1280;
    float h = 800;

    // === Invisible Platforms Matching Level1Map.png ===
platforms.clear();
// === FLOOR 1 (Bottom floor) ===
platforms.add(new Rectangle(0, 7, 525, 20)); // LEFT SIDE
platforms.add(new Rectangle(525, 22, 385, 20));   // MIDDLE SECTION
platforms.add(new Rectangle(960, 0, 265, 4));  // RIGHT SIDE

// === FLOOR 2 ===
platforms.add(new Rectangle(0, 225, 620, 20));

// === FLOOR 3 ===
platforms.add(new Rectangle(0, 475, 1280, 20));

// === FLOOR 4 (Roof inside section) ===
platforms.add(new Rectangle(0, 500, 1280, 20));


    // === Your existing items ===
    documents.add(new Rectangle(200, 160, DOC_SIZE, DOC_SIZE));
     documents.add(new Rectangle(300, 160, DOC_SIZE, DOC_SIZE));
    documents.add(new Rectangle(w - 250, 270, DOC_SIZE, DOC_SIZE));

    
     obstacles.add(new Rectangle(250, 150, 60, 10));
     obstacles.add(new Rectangle(w - 300, 250, 60, 10));
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

    @Override
    public String getBackgroundPath() {
        return "Level1Map.png";
    }

    @Override
    public void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                  Array<Rectangle> obstacles, Fixer player) {
        // Example: track documents collected and adjust visual state
        // (You can add fields to Level1 to track state if needed)
        // For now, this is a placeholder for future effects
    }

    @Override

    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
    if (backgroundTex != null) {
        batch.draw(backgroundTex, 0, 0, 1280, 800);
    }
}

}
