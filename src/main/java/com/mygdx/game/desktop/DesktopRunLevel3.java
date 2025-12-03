package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mygdx.game.MyGdxGame;
import com.mygdx.game.GameScreen;

/**
 * Small desktop launcher that starts directly at Level 3 for testing.
 */
public class DesktopRunLevel3 {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Game - Direct Level 3 Run");
        config.setWindowedMode(1280, 800);

        MyGdxGame game = new MyGdxGame() {
            @Override
            public void create() {
                super.create();
                setScreen(new GameScreen(this, 3));
            }
        };

        new Lwjgl3Application(game, config);
    }
}
