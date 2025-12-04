package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Papertrail GDX");
        config.setWindowedMode(1280, 800);
        config.setFullscreenMode(null); // Disable fullscreen
        config.setDecorated(true); // Show window border
        config.setResizable(true); // Allow window resizing
        config.useVsync(true); 
        try {
            // Provide multiple resolutions so the OS can pick the best icon size
            config.setWindowIcon(
                "assets/Start/1.png",
                "assets/Start/2.png",
                "assets/Start/3.png"
            );
        } catch (Exception ignored) {
            // If any icon is missing we just continue with the default LWJGL icon
        }
        
        // Enable debug logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        
        MyGdxGame game = new MyGdxGame();
        
        new Lwjgl3Application(game, config);
    }
}
