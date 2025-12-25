package com.ctrl_s.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class HoverSoundManager {
    private Sound hoverSound;
    private Sound shredSound;
    private Sound winSound;
    private Sound gameOverSound;
    private Sound documentSound;
    private Sound kmjsSound;

    public HoverSoundManager() {
        loadHoverSound();
    }

    private void loadHoverSound() {
        try {
            hoverSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/sfx/hover.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded hover sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load hover sound", e);
            hoverSound = null;
        }
    }

    private void loadShredSound() {
        try {
            shredSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/sfx/shredder.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded shred sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load shred sound", e);
            shredSound = null;
        }
    }

    private void loadWinSound() {
        try {
            winSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/sfx/win.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded win sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load win sound", e);
            winSound = null;
        }
    }

    private void loadGameOverSound() {
        try {
            gameOverSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/sfx/gameover.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded gameover sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load gameover sound", e);
            gameOverSound = null;
        }
    }

    private void loadDocumentSound() {
        try {
            documentSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/sfx/document.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded document sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load document sound", e);
            documentSound = null;
        }
    }

    private void loadKmjsSound() {
        try {
            kmjsSound = Gdx.audio.newSound(Gdx.files.internal("assets/audio/music/game_bgm.mp3"));
            Gdx.app.log("HoverSoundManager", "Loaded kmjs sound");
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to load kmjs sound", e);
            kmjsSound = null;
        }
    }

    public void playHover() {
        try {
            if (hoverSound != null)
                hoverSound.play(0.9f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play hover sound", e);
        }
    }

    public void playShred() {
        try {
            if (shredSound == null)
                loadShredSound();
            if (shredSound != null)
                shredSound.play(3.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play shred sound", e);
        }
    }

    public void playWin() {
        try {
            if (winSound == null)
                loadWinSound();
            if (winSound != null)
                winSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play win sound", e);
        }
    }

    public void playGameOver() {
        try {
            if (gameOverSound == null)
                loadGameOverSound();
            if (gameOverSound != null)
                gameOverSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play gameover sound", e);
        }
    }

    public void playDocument() {
        try {
            if (documentSound == null)
                loadDocumentSound();
            if (documentSound != null)
                documentSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play document sound", e);
        }
    }

    public void playKmjs() {
        try {
            if (kmjsSound == null)
                loadKmjsSound();
            if (kmjsSound != null)
                kmjsSound.play(1.0f);
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Failed to play kmjs sound", e);
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
            if (documentSound != null) {
                documentSound.stop();
                documentSound.dispose();
                documentSound = null;
            }
            if (kmjsSound != null) {
                kmjsSound.stop();
                kmjsSound.dispose();
                kmjsSound = null;
            }
        } catch (Exception e) {
            Gdx.app.log("HoverSoundManager", "Error disposing hover sound", e);
        }
    }
}
