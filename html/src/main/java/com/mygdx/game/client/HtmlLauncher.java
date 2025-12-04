package com.mygdx.game.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

/**
 * Lightweight launcher used by GWT to boot the existing libGDX game inside a browser canvas.
 */
public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        // Match the desktop launcher resolution; canvas scales responsively in the browser.
        return new GwtApplicationConfiguration(1280, 800);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new MyGdxGame();
    }
}
