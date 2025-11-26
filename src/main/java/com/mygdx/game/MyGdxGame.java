package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    // Preloaded tutorial images (optional)
    public com.badlogic.gdx.graphics.Texture[] tutorialImages;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        // ensure rendering resources exist for all screens
        batch = new SpriteBatch();
        font = new BitmapFont();

        // show the loading screen first (it will switch to the main menu when ready)
        setScreen(new LoadingScreen(this));
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