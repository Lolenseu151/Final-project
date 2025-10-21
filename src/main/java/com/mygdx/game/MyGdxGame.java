package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game; // <-- Import Gdx for the log level
import com.badlogic.gdx.Gdx; // <-- Import Application for the log constants
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class MyGdxGame extends Game { // <--- **(1) CLASS START**

    // Public fields go here
    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() { // <--- **(2) METHOD START**
        // Log level line is SAFE here:
        Gdx.app.setLogLevel(Application.LOG_DEBUG); 
        
        batch = new SpriteBatch();
        font = new BitmapFont();
        setScreen(new GameScreen(this));
    } // <--- **(3) METHOD END**

    @Override
    public void dispose() {
        // ... dispose code ...
        super.dispose();
    }

} // <--- **(4) CLASS END (MUST BE THE LAST THING IN THE FILE)**