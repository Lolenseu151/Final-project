package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Tutorial map for the Archive floor. Simple layout with a highlighted light area
 * near the 'property files' to teach movement.
 */
public class LevelTutorial implements Level, BackgroundedLevel {

    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    private int totalDocs = 0;

    // special "light" area representing the property files spotlight
    private Rectangle propertyFilesLight;

    // Customizable tutorial text (defaults provided)
    private String tutorialTitle = "MOVEMENT:";
    private String tutorialDetail = "Use [W], [A], [S], [D] to navigate the Archive floor.";
    private String tutorialHint = "Try moving into the light near the property files.";
    // Should the tutorial overlay box be shown? Allows the player to dismiss it with OK.
    private boolean showOverlay = true;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 27f;

    @Override
    public void init() {
        documents.clear(); platforms.clear(); obstacles.clear(); beams.clear();

        float w = Gdx.graphics.getWidth();

        // Basic ground and a couple of low platforms to keep movement simple
        platforms.add(new Rectangle(0, 0, w, PLATFORM_H));
        platforms.add(new Rectangle(0, 217, 1132, PLATFORM_H));
        platforms.add(new Rectangle(w - 555, 432, 650, PLATFORM_H));
        platforms.add(new Rectangle(0, 432, 588, PLATFORM_H));
        
        // Place a few sample documents
        documents.add(new Rectangle(200, 400, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(340, 140, DOC_SIZE, DOC_SIZE));

        // Shredder off to the right
        shredder = new Rectangle(w - 80, 10, 50, 50);

        // Property files light area (player should move into this light)
        float lightW = 160f, lightH = 120f;
        propertyFilesLight = new Rectangle(w/2f - lightW/2f, 220f, lightW, lightH);

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
        // use the tutorial map provided in assets: "tutorial map.png"
        return "tutorial map.png";
    }

    @Override
    public void renderBackground(SpriteBatch batch, com.badlogic.gdx.graphics.Texture backgroundTex) {
        // Draw the default background via LevelManager, then overlay a subtle light at propertyFilesLight
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        // the actual light highlight will be drawn by the LevelManager or GameScreen overlay logic
    }

    // expose light area for overlay logic
    public Rectangle getPropertyFilesLight() { return propertyFilesLight; }

    // Overlay visibility
    public boolean isShowOverlay() { return showOverlay; }
    public void setShowOverlay(boolean show) { this.showOverlay = show; }

    // Tutorial text getters/setters so the game or designer can customize the overlay
    public String getTutorialTitle() { return tutorialTitle; }
    public void setTutorialTitle(String title) { this.tutorialTitle = title; }

    public String getTutorialDetail() { return tutorialDetail; }
    public void setTutorialDetail(String detail) { this.tutorialDetail = detail; }

    public String getTutorialHint() { return tutorialHint; }
    public void setTutorialHint(String hint) { this.tutorialHint = hint; }
}
