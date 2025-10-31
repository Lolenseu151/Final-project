package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game {

    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() {
        // Set log level
        Gdx.app.setLogLevel(Application.LOG_DEBUG); 
        
        batch = new SpriteBatch();
        font = new BitmapFont(); // default Arial-like font
        
        // Start with loading screen instead of directly going to game
        setScreen(new LoadingScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        super.dispose();
    }
}
