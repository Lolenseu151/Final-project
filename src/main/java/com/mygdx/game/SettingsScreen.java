package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Settings Screen - Game settings and options
 */
public class SettingsScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    
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
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
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
    }
}
