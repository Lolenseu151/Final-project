package com.ctrl_s.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;

/**
 * Manages level-specific separate background music (not global)
 */
public class LevelMusicManager implements Disposable {
    private Music currentMusic;
    private float volume = 0.4f;

    public void playMusic(String filePath) {
        stopMusic();
        try {
            if (filePath != null && !filePath.isEmpty()) {
                currentMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
                if (currentMusic != null) {
                    currentMusic.setLooping(true);
                    currentMusic.setVolume(volume);
                    currentMusic.play();
                }
            }
        } catch (Exception e) {
            Gdx.app.error("LevelMusicManager", "Failed to load/play music: " + filePath, e);
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
            currentMusic = null;
        }
    }

    @Override
    public void dispose() {
        stopMusic();
    }
}
