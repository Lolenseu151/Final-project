package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    // Persisted game state to survive Screen recreation (minimize/restore)
    private LevelManager persistentLevelManager = null;
    private Fixer persistentFixer = null;
    // Preloaded tutorial images (optional)
    public com.badlogic.gdx.graphics.Texture[] tutorialImages;
    // Centralized hover sound manager (single load for app)
    private HoverSoundManager hoverSoundManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        // initialize hover sound manager early so screens can use it
        try { hoverSoundManager = new HoverSoundManager(); } catch (Exception e) { hoverSoundManager = null; }
        setScreen(new LoadingScreen(this));  // START WITH LOADING SCREEN
    }

    public LevelManager getPersistentLevelManager() { return persistentLevelManager; }
    public void setPersistentLevelManager(LevelManager lm) { this.persistentLevelManager = lm; }

    public Fixer getPersistentFixer() { return persistentFixer; }
    public void setPersistentFixer(Fixer f) { this.persistentFixer = f; }

    public HoverSoundManager getHoverSoundManager() { return hoverSoundManager; }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (tutorialImages != null) {
            for (com.badlogic.gdx.graphics.Texture t : tutorialImages) if (t != null) t.dispose();
        }
        if (hoverSoundManager != null) {
            try { hoverSoundManager.dispose(); } catch (Exception ignored) {}
            hoverSoundManager = null;
        }
        super.dispose();
    }
}