package com.mygdx.game;

<<<<<<< HEAD
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game; // <-- Import Gdx for the log level
import com.badlogic.gdx.Gdx; // <-- Import Application for the log constants
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class MyGdxGame extends Game { // <--- **(1) CLASS START**

    // Public fields go here
=======
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MyGdxGame extends Game {
>>>>>>> 25ebd588c7230c56d19f7cf0ce608c079f754c16
    public SpriteBatch batch;
    public BitmapFont font;

    @Override
<<<<<<< HEAD
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
=======
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont(); // default Arial-like font
        setScreen(new GameScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        super.dispose();
    }
}
>>>>>>> 25ebd588c7230c56d19f7cf0ce608c079f754c16
