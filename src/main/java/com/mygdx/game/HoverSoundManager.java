package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class HoverSoundManager {
    private Sound hoverSound;

    public HoverSoundManager() {
        loadHoverSound();
    }

    private void loadHoverSound() {
        try {
            com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("Sounds/Hover.mp3");
            if (fh.exists()) {
                hoverSound = Gdx.audio.newSound(fh);
                Gdx.app.log("HoverSoundManager", "Loaded internal hover sound");
                return;
            }
            String abs = System.getProperty("user.dir") + "/assets/Sounds/Hover.mp3";
            com.badlogic.gdx.files.FileHandle fha = Gdx.files.absolute(abs);
            if (fha.exists()) {
                hoverSound = Gdx.audio.newSound(fha);
                Gdx.app.log("HoverSoundManager", "Loaded absolute hover sound: " + abs);
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load hover sound", e);
            hoverSound = null;
        }
    }

    public void playHover() {
        try {
            if (hoverSound != null) hoverSound.play(0.9f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play hover sound", e);
        }
    }

    public void dispose() {
        try {
            if (hoverSound != null) {
                hoverSound.stop();
                hoverSound.dispose();
                hoverSound = null;
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Error disposing hover sound", e);
        }
    }
}
