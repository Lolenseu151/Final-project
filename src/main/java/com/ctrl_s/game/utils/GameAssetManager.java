package com.ctrl_s.game.utils;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * Wrapper around LibGDX AssetManager to enforce singleton usage and provide
 * helper methods
 * for the new asset structure.
 */
public class GameAssetManager {
    private static GameAssetManager instance;
    private AssetManager manager;

    private GameAssetManager() {
        manager = new AssetManager();
    }

    public static synchronized GameAssetManager getInstance() {
        if (instance == null) {
            instance = new GameAssetManager();
        }
        return instance;
    }

    public AssetManager getManager() {
        return manager;
    }

    public void loadTexture(String path) {
        manager.load(path, Texture.class);
    }

    public void loadMusic(String path) {
        manager.load(path, Music.class);
    }

    public void loadSound(String path) {
        manager.load(path, Sound.class);
    }

    public void loadFont(String path) {
        manager.load(path, BitmapFont.class);
    }

    public Texture getTexture(String path) {
        if (manager.isLoaded(path)) {
            return manager.get(path, Texture.class);
        }
        return null;
    }

    public Music getMusic(String path) {
        if (manager.isLoaded(path)) {
            return manager.get(path, Music.class);
        }
        return null;
    }

    public Sound getSound(String path) {
        if (manager.isLoaded(path)) {
            return manager.get(path, Sound.class);
        }
        return null;
    }

    public BitmapFont getFont(String path) {
        if (manager.isLoaded(path)) {
            return manager.get(path, BitmapFont.class);
        }
        return null;
    }

    public void finishLoading() {
        manager.finishLoading();
    }

    public void dispose() {
        manager.dispose();
    }
}
