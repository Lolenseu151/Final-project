package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Papertrail GDX");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        
<<<<<<< HEAD
        // This is safe to keep or remove, as it's not the Gdx.app call
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG"); 
        
        MyGdxGame game = new MyGdxGame();
        
        // This is the CRUCIAL line. It must be the last thing before the method ends.
        new Lwjgl3Application(game, config); 
=======
        // Enable debug logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        
        MyGdxGame game = new MyGdxGame();
        // Set libGDX debug level
        Gdx.app.setLogLevel(Gdx.app.LOG_DEBUG);
        
        new Lwjgl3Application(game, config);
>>>>>>> 25ebd588c7230c56d19f7cf0ce608c079f754c16
    }
}
