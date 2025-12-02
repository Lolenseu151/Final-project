package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * LevelMusicManager - Manages background music for levels
 * Handles playing, stopping, looping, and volume control for level music
 */
public class LevelMusicManager {
    private Music currentMusic = null;
    private float volume = 0.8f;  // Default volume (0.0 to 1.0)
    private String currentMusicPath = null;

    /**
     * Play music for a given level
     * @param musicPath Path to the music file (e.g., "assets/Sounds/Level Music.mp3")
     */
    public void playMusic(String musicPath) {
        // Stop current music if playing
        stopMusic();

        if (musicPath == null || musicPath.isEmpty()) {
            Gdx.app.log("LevelMusicManager", "No music path provided");
            return;
        }

        try {
            // Load and play the music
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
            if (currentMusic != null) {
                currentMusic.setLooping(true);  // Loop continuously during gameplay
                currentMusic.setVolume(volume);
                currentMusic.play();
                currentMusicPath = musicPath;
                Gdx.app.log("LevelMusicManager", "Started playing: " + musicPath + " at volume: " + volume);
            } else {
                Gdx.app.error("LevelMusicManager", "Failed to load music: " + musicPath);
            }
        } catch (Exception e) {
            Gdx.app.error("LevelMusicManager", "Error loading music: " + musicPath, e);
        }
    }

    /**
     * Stop the current music playback
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            Gdx.app.log("LevelMusicManager", "Stopped music: " + currentMusicPath);
            currentMusicPath = null;
        }
    }

    /**
     * Pause the current music
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
            Gdx.app.log("LevelMusicManager", "Paused music");
        }
    }

    /**
     * Resume playing the paused music
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
            Gdx.app.log("LevelMusicManager", "Resumed music");
        }
    }

    /**
     * Set the volume for music playback
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));  // Clamp between 0 and 1
        if (currentMusic != null) {
            currentMusic.setVolume(this.volume);
            Gdx.app.log("LevelMusicManager", "Music volume set to: " + this.volume);
        }
    }

    /**
     * Get the current volume level
     * @return Current volume (0.0 to 1.0)
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Check if music is currently playing
     * @return true if music is playing, false otherwise
     */
    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    /**
     * Dispose of music resources
     * IMPORTANT: Call this when transitioning levels or screen changes to prevent memory leaks
     */
    public void dispose() {
        stopMusic();
        if (currentMusic != null) {
            currentMusic.dispose();
            currentMusic = null;
            Gdx.app.log("LevelMusicManager", "Music resources disposed");
        }
    }
}
