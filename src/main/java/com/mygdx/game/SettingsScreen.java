package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Settings Screen - Game settings and options
 */
public class SettingsScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    
    // Animated background
    private Texture[] bgTextures = new Texture[121];
    private TextureRegion[] bgRegions = new TextureRegion[121];
    private int loadedFrameCount = 0;  // Track how many frames actually loaded
    private float bgAnimTime = 0f;
    private static final float BG_FRAME_DURATION = 0.1f;  // 0.1 seconds per frame = 12.1 seconds total loop
    
    private enum SettingOption {
        SOUND_EFFECTS,
        MUSIC,
        FULLSCREEN,
        BACK
    }
    
    private SettingOption selectedOption = SettingOption.SOUND_EFFECTS;
    
    // Settings (would typically be saved to preferences)
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private boolean fullscreen = false;
    
    private boolean upKeyWasPressed = false;
    private boolean downKeyWasPressed = false;
    
    public SettingsScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        
        // Load animated Setting Background frames
        // Files exist as: 11-19 (9 frames) and 110-131 (22 frames) = 31 total frames
        int[] frameNumbers = new int[31];
        int idx = 0;
        // Add frames 11-19
        for (int i = 11; i <= 19; i++) {
            frameNumbers[idx++] = i;
        }
        // Add frames 110-131
        for (int i = 110; i <= 131; i++) {
            frameNumbers[idx++] = i;
        }
        
        bgTextures = new Texture[31];
        bgRegions = new TextureRegion[31];
        
        String userDir = System.getProperty("user.dir");
        for (int i = 0; i < frameNumbers.length; i++) {
            try {
                String framePath = "Setting Background/setting_background" + frameNumbers[i] + ".png";
                com.badlogic.gdx.files.FileHandle fh = null;
                if (Gdx.files.internal(framePath).exists()) {
                    fh = Gdx.files.internal(framePath);
                } else if (Gdx.files.absolute(userDir + "/assets/" + framePath).exists()) {
                    fh = Gdx.files.absolute(userDir + "/assets/" + framePath);
                } else {
                    Gdx.app.error("[SettingsScreen]", "✗ Setting Background frame " + (i + 1) + " NOT FOUND: " + framePath);
                }
                
                if (fh != null) {
                    bgTextures[i] = new Texture(fh);
                    bgTextures[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    bgRegions[i] = new TextureRegion(bgTextures[i]);
                    loadedFrameCount++;
                    Gdx.app.log("[SettingsScreen]", "✓ Loaded Setting Background frame " + (i + 1));
                }
            } catch (Exception e) {
                Gdx.app.error("[SettingsScreen]", "Error loading background frame " + (i + 1), e);
            }
        }
        Gdx.app.log("[SettingsScreen]", "Total loaded frames: " + loadedFrameCount + "/31");
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Update background animation (continuous, modulo handles looping)
        bgAnimTime += delta;
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Draw animated background first (behind UI)
        game.batch.begin();
        
        // Find a valid frame to display (skip null frames)
        int frameIndex = (int)(bgAnimTime / BG_FRAME_DURATION) % bgRegions.length;
        TextureRegion frameToRender = bgRegions[frameIndex];
        
        // If current frame is null, find the nearest valid frame
        if (frameToRender == null) {
            for (int i = 0; i < bgRegions.length; i++) {
                if (bgRegions[i] != null) {
                    frameToRender = bgRegions[i];
                    break;
                }
            }
        }
        
        // Draw the frame if we found a valid one
        if (frameToRender != null) {
            game.batch.draw(frameToRender, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        game.batch.end();
        
        drawSettings();
    }
    
    private void handleInput() {
        // Navigation with UP/DOWN arrows or W/S
        boolean upKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.UP) || 
                                 Gdx.input.isKeyPressed(Input.Keys.W);
        boolean downKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN) || 
                                   Gdx.input.isKeyPressed(Input.Keys.S);
        
        // Move up in menu
        if (upKeyIsPressed && !upKeyWasPressed) {
            switch (selectedOption) {
                case SOUND_EFFECTS:
                    // Already at top
                    break;
                case MUSIC:
                    selectedOption = SettingOption.SOUND_EFFECTS;
                    break;
                case FULLSCREEN:
                    selectedOption = SettingOption.MUSIC;
                    break;
                case BACK:
                    selectedOption = SettingOption.FULLSCREEN;
                    break;
            }
        }
        
        // Move down in menu
        if (downKeyIsPressed && !downKeyWasPressed) {
            switch (selectedOption) {
                case SOUND_EFFECTS:
                    selectedOption = SettingOption.MUSIC;
                    break;
                case MUSIC:
                    selectedOption = SettingOption.FULLSCREEN;
                    break;
                case FULLSCREEN:
                    selectedOption = SettingOption.BACK;
                    break;
                case BACK:
                    // Already at bottom
                    break;
            }
        }
        
        upKeyWasPressed = upKeyIsPressed;
        downKeyWasPressed = downKeyIsPressed;
        
        // Toggle setting or select option with ENTER or SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            toggleCurrentOption();
        }
        
        // Quick back with ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || 
            Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }
    
    private void toggleCurrentOption() {
        switch (selectedOption) {
            case SOUND_EFFECTS:
                soundEnabled = !soundEnabled;
                Gdx.app.log("Settings", "Sound Effects: " + (soundEnabled ? "ON" : "OFF"));
                break;
            case MUSIC:
                musicEnabled = !musicEnabled;
                Gdx.app.log("Settings", "Music: " + (musicEnabled ? "ON" : "OFF"));
                break;
            case FULLSCREEN:
                fullscreen = !fullscreen;
                if (fullscreen) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    Gdx.graphics.setWindowedMode(800, 600);
                }
                Gdx.app.log("Settings", "Fullscreen: " + (fullscreen ? "ON" : "OFF"));
                break;
            case BACK:
                game.setScreen(new MainMenuScreen(game));
                break;
        }
    }
    
    private void drawSettings() {
        float centerX = Gdx.graphics.getWidth() / 2;
        float centerY = Gdx.graphics.getHeight() / 2;
        
        // Draw background panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.25f, 0.2f, 0.9f);
        shapeRenderer.rect(centerX - 250, centerY - 150, 500, 400);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.3f, 0.8f, 0.3f, 1); // Green border
        shapeRenderer.rect(centerX - 250, centerY - 150, 500, 400);
        shapeRenderer.end();
        
        // Draw setting option boxes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawSettingBox(centerX, centerY + 120, SettingOption.SOUND_EFFECTS);
        drawSettingBox(centerX, centerY + 60, SettingOption.MUSIC);
        drawSettingBox(centerX, centerY, SettingOption.FULLSCREEN);
        drawSettingBox(centerX, centerY - 80, SettingOption.BACK);
        shapeRenderer.end();
        
        // Draw text
        game.batch.begin();
        
        game.font.draw(game.batch, "SETTINGS", centerX - 40, centerY + 220);
        
        // Settings options
        drawSettingText("Sound Effects: " + (soundEnabled ? "ON" : "OFF"), 
            centerX - 90, centerY + 130, SettingOption.SOUND_EFFECTS);
        drawSettingText("Music: " + (musicEnabled ? "ON" : "OFF"), 
            centerX - 60, centerY + 70, SettingOption.MUSIC);
        drawSettingText("Fullscreen: " + (fullscreen ? "ON" : "OFF"), 
            centerX - 80, centerY + 10, SettingOption.FULLSCREEN);
        drawSettingText("BACK TO MENU", 
            centerX - 70, centerY - 70, SettingOption.BACK);
        
        // Instructions
        game.font.draw(game.batch, "UP/DOWN or W/S: Navigate | ENTER/SPACE: Toggle/Select", 
            centerX - 220, centerY - 180);
        game.font.draw(game.batch, "ESC or Q: Back to menu", 
            centerX - 100, centerY - 210);
        
        game.batch.end();
    }
    
    private void drawSettingBox(float centerX, float y, SettingOption option) {
        boolean isSelected = (selectedOption == option);
        
        if (isSelected) {
            shapeRenderer.setColor(0.2f, 0.7f, 0.2f, 0.8f); // Highlighted green
        } else {
            shapeRenderer.setColor(0.25f, 0.3f, 0.25f, 0.6f); // Dark grey-green
        }
        
        shapeRenderer.rect(centerX - 180, y - 20, 360, 40);
    }
    
    private void drawSettingText(String text, float x, float y, SettingOption option) {
        boolean isSelected = (selectedOption == option);
        
        if (isSelected) {
            game.font.draw(game.batch, "> " + text + " <", x - 20, y);
        } else {
            game.font.draw(game.batch, text, x, y);
        }
    }
    
    @Override
    public void show() {
        Gdx.app.log("SettingsScreen", "Settings displayed");
    }
    
    @Override
    public void resize(int width, int height) {}
    
    @Override
    public void pause() {}
    
    @Override
    public void resume() {}
    
    @Override
    public void hide() {}
    
    @Override
    public void dispose() {
        shapeRenderer.dispose();
        // Dispose background textures
        for (int i = 0; i < bgTextures.length; i++) {
            if (bgTextures[i] != null) {
                bgTextures[i].dispose();
            }
        }
    }
}
