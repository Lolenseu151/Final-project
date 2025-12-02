package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class HoverSoundManager {
    private Sound hoverSound;
    private Sound shredSound;
    private Sound winSound;
    private Sound gameOverSound;

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

    private void loadShredSound() {
        try {
            com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("Sounds/Shredder.mp3");
            if (fh.exists()) {
                shredSound = Gdx.audio.newSound(fh);
                Gdx.app.log("HoverSoundManager", "Loaded internal shred sound");
                return;
            }
            String abs = System.getProperty("user.dir") + "/assets/Sounds/Shredder.mp3";
            com.badlogic.gdx.files.FileHandle fha = Gdx.files.absolute(abs);
            if (fha.exists()) {
                shredSound = Gdx.audio.newSound(fha);
                Gdx.app.log("HoverSoundManager", "Loaded absolute shred sound: " + abs);
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load shred sound", e);
            shredSound = null;
        }
    }

    private void loadWinSound() {
        try {
            com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("Sounds/win.mp3");
            if (fh.exists()) {
                winSound = Gdx.audio.newSound(fh);
                Gdx.app.log("HoverSoundManager", "Loaded internal win sound");
                return;
            }
            String abs = System.getProperty("user.dir") + "/assets/Sounds/win.mp3";
            com.badlogic.gdx.files.FileHandle fha = Gdx.files.absolute(abs);
            if (fha.exists()) {
                winSound = Gdx.audio.newSound(fha);
                Gdx.app.log("HoverSoundManager", "Loaded absolute win sound: " + abs);
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load win sound", e);
            winSound = null;
        }
    }

    private void loadGameOverSound() {
        try {
            com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("Sounds/Gameover.mp3");
            if (fh.exists()) {
                gameOverSound = Gdx.audio.newSound(fh);
                Gdx.app.log("HoverSoundManager", "Loaded internal gameover sound");
                return;
            }
            String abs = System.getProperty("user.dir") + "/assets/Sounds/Gameover.mp3";
            com.badlogic.gdx.files.FileHandle fha = Gdx.files.absolute(abs);
            if (fha.exists()) {
                gameOverSound = Gdx.audio.newSound(fha);
                Gdx.app.log("HoverSoundManager", "Loaded absolute gameover sound: " + abs);
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load gameover sound", e);
            gameOverSound = null;
        }
    }

    public void playHover() {
        try {
            if (hoverSound != null) hoverSound.play(0.9f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play hover sound", e);
        }
    }

    public void playShred() {
        try {
            if (shredSound == null) loadShredSound();
            if (shredSound != null) shredSound.play(3.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play shred sound", e);
        }
    }

    public void playWin() {
        try {
            if (winSound == null) loadWinSound();
            if (winSound != null) winSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play win sound", e);
        }
    }

    public void playGameOver() {
        try {
            if (gameOverSound == null) loadGameOverSound();
            if (gameOverSound != null) gameOverSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play gameover sound", e);
        }
    }

    public void dispose() {
        try {
            if (hoverSound != null) {
                hoverSound.stop();
                hoverSound.dispose();
                hoverSound = null;
            }
            if (shredSound != null) {
                shredSound.stop();
                shredSound.dispose();
                shredSound = null;
            }
            if (winSound != null) {
                winSound.stop();
                winSound.dispose();
                winSound = null;
            }
            if (gameOverSound != null) {
                gameOverSound.stop();
                gameOverSound.dispose();
                gameOverSound = null;
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Error disposing hover sound", e);
        }
    }
}
