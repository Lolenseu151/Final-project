package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    // Preloaded tutorial images (optional)
    public com.badlogic.gdx.graphics.Texture[] tutorialImages;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        setScreen(new LoadingScreen(this));  // START WITH LOADING SCREEN
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (tutorialImages != null) {
            for (com.badlogic.gdx.graphics.Texture t : tutorialImages) if (t != null) t.dispose();
        }
        super.dispose();
    }
}