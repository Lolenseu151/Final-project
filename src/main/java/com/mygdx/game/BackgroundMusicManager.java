package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * BackgroundMusicManager - Global music manager for continuous background music
 * across GameScreen, SettingsScreen, LevelSelectScreen, and menu screens.
 * Switches between screen music and level music as needed.
 */
public class BackgroundMusicManager {
    private static BackgroundMusicManager instance;
    
    private Music currentMusic = null;
    private float volume = 0.8f;
    private String currentMusicPath = null;
    private MusicType currentMusicType = MusicType.NONE;
    
    public enum MusicType {
        NONE,
        SCREEN_MUSIC,      // GameScreen, Settings, LevelSelect
        LEVEL_MUSIC         // During gameplay
    }
    
    // Screen music path
    private static final String SCREEN_MUSIC_PATH = "assets/Sounds/GameScreenMusic.MP3";
    
    private BackgroundMusicManager() {}
    
    /**
     * Get singleton instance
     */
    public static synchronized BackgroundMusicManager getInstance() {
        if (instance == null) {
            instance = new BackgroundMusicManager();
        }
        return instance;
    }
    
    /**
     * Play screen background music (for menus, settings, level select)
     */
    public void playScreenMusic() {
        playMusic(SCREEN_MUSIC_PATH, MusicType.SCREEN_MUSIC);
    }
    
    /**
     * Play level music
     */
    public void playLevelMusic(String levelMusicPath) {
        if (levelMusicPath == null || levelMusicPath.isEmpty()) {
            stopMusic();
            return;
        }
        playMusic(levelMusicPath, MusicType.LEVEL_MUSIC);
    }
    
    /**
     * Internal method to play music
     */
    private void playMusic(String musicPath, MusicType type) {
        // Only switch if different music is needed
        if (currentMusicPath != null && currentMusicPath.equals(musicPath)) {
            if (currentMusic != null && currentMusic.isPlaying()) {
                Gdx.app.log("BackgroundMusicManager", "Music already playing: " + musicPath);
                return;
            } else if (currentMusic != null && !currentMusic.isPlaying()) {
                // Music path is same but not playing, resume it
                currentMusic.play();
                Gdx.app.log("BackgroundMusicManager", "Resumed music: " + musicPath);
                return;
            }
        }
        
        stopMusic();
        
        if (musicPath == null || musicPath.isEmpty()) {
            Gdx.app.log("BackgroundMusicManager", "No music path provided");
            return;
        }
        
        try {
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
            if (currentMusic != null) {
                currentMusic.setLooping(true);
                currentMusic.setVolume(volume);
                currentMusic.play();
                currentMusicPath = musicPath;
                currentMusicType = type;
                Gdx.app.log("BackgroundMusicManager", "Started playing: " + musicPath + " (type: " + type + ") at volume: " + volume);
            } else {
                Gdx.app.error("BackgroundMusicManager", "Failed to load music: " + musicPath);
            }
        } catch (Exception e) {
            Gdx.app.error("BackgroundMusicManager", "Error loading music: " + musicPath, e);
        }
    }
    
    /**
     * Stop the current music playback
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            Gdx.app.log("BackgroundMusicManager", "Stopped music: " + currentMusicPath);
            currentMusicPath = null;
            currentMusicType = MusicType.NONE;
        }
    }
    
    /**
     * Pause the current music
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
            Gdx.app.log("BackgroundMusicManager", "Paused music");
        }
    }
    
    /**
     * Resume playing the paused music
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
            Gdx.app.log("BackgroundMusicManager", "Resumed music");
        }
    }
    
    /**
     * Set the volume for music playback
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        if (currentMusic != null) {
            currentMusic.setVolume(this.volume);
            Gdx.app.log("BackgroundMusicManager", "Music volume set to: " + this.volume);
        }
    }
    
    /**
     * Get the current volume level
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * Check if music is currently playing
     */
    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }
    
    /**
     * Get current music type
     */
    public MusicType getCurrentMusicType() {
        return currentMusicType;
    }
    
    /**
     * Dispose of music resources (call when game closes)
     */
    public void dispose() {
        stopMusic();
        if (currentMusic != null) {
            currentMusic.dispose();
            currentMusic = null;
            Gdx.app.log("BackgroundMusicManager", "Music resources disposed");
        }
    }
}
