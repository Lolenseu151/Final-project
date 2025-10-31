package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Main Menu Screen - Shows title and menu options
 */
public class MainMenuScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    
    private enum MenuOption {
        START_GAME,
        TUTORIAL,
        SETTINGS
    }
    
    private MenuOption selectedOption = MenuOption.START_GAME;
    private boolean upKeyWasPressed = false;
    private boolean downKeyWasPressed = false;
    
    public MainMenuScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        drawMenu();
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
                case START_GAME:
                    // Already at top
                    break;
                case TUTORIAL:
                    selectedOption = MenuOption.START_GAME;
                    break;
                case SETTINGS:
                    selectedOption = MenuOption.TUTORIAL;
                    break;
            }
        }
        
        // Move down in menu
        if (downKeyIsPressed && !downKeyWasPressed) {
            switch (selectedOption) {
                case START_GAME:
                    selectedOption = MenuOption.TUTORIAL;
                    break;
                case TUTORIAL:
                    selectedOption = MenuOption.SETTINGS;
                    break;
                case SETTINGS:
                    // Already at bottom
                    break;
            }
        }
        
        upKeyWasPressed = upKeyIsPressed;
        downKeyWasPressed = downKeyIsPressed;
        
        // Select option with ENTER or SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            selectCurrentOption();
        }
    }
    
    private void selectCurrentOption() {
        switch (selectedOption) {
            case START_GAME:
                Gdx.app.log("MainMenu", "Starting game...");
                game.setScreen(new GameScreen(game));
                break;
            case TUTORIAL:
                Gdx.app.log("MainMenu", "Opening tutorial...");
                game.setScreen(new TutorialScreen(game));
                break;
            case SETTINGS:
                Gdx.app.log("MainMenu", "Opening settings...");
                game.setScreen(new SettingsScreen(game));
                break;
        }
    }
    
    private void drawMenu() {
        float centerX = Gdx.graphics.getWidth() / 2;
        float centerY = Gdx.graphics.getHeight() / 2;
        
        // Draw menu background panels
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Title background
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 0.8f);
        shapeRenderer.rect(centerX - 200, centerY + 100, 400, 80);
        
        // Menu options backgrounds
        drawMenuOptionBox(centerX, centerY + 20, MenuOption.START_GAME);
        drawMenuOptionBox(centerX, centerY - 40, MenuOption.TUTORIAL);
        drawMenuOptionBox(centerX, centerY - 100, MenuOption.SETTINGS);
        
        shapeRenderer.end();
        
        // Draw borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0, 0.8f, 0.8f, 1); // Cyan border
        shapeRenderer.rect(centerX - 200, centerY + 100, 400, 80);
        shapeRenderer.end();
        
        // Draw text
        game.batch.begin();
        
        // Title
        game.font.draw(game.batch, "PAPER TRAIL PANIC", 
            centerX - 90, centerY + 150);
        
        game.font.draw(game.batch, "A Mission for Loloy the Crocodile", 
            centerX - 120, centerY + 120);
        
        // Menu options
        drawMenuOptionText("START GAME", centerX - 50, centerY + 30, MenuOption.START_GAME);
        drawMenuOptionText("TUTORIAL", centerX - 40, centerY - 30, MenuOption.TUTORIAL);
        drawMenuOptionText("SETTINGS", centerX - 40, centerY - 90, MenuOption.SETTINGS);
        
        // Instructions
        game.font.draw(game.batch, "UP/DOWN or W/S: Navigate | ENTER/SPACE: Select", 
            centerX - 180, 40);
        
        game.batch.end();
    }
    
    private void drawMenuOptionBox(float centerX, float y, MenuOption option) {
        boolean isSelected = (selectedOption == option);
        
        if (isSelected) {
            shapeRenderer.setColor(0, 0.6f, 0.6f, 0.9f); // Highlighted cyan
        } else {
            shapeRenderer.setColor(0.25f, 0.25f, 0.35f, 0.7f); // Dark grey
        }
        
        shapeRenderer.rect(centerX - 150, y - 20, 300, 40);
    }
    
    private void drawMenuOptionText(String text, float x, float y, MenuOption option) {
        boolean isSelected = (selectedOption == option);
        
        if (isSelected) {
            // Draw selection indicator
            game.font.draw(game.batch, "> " + text + " <", x - 20, y);
        } else {
            game.font.draw(game.batch, text, x, y);
        }
    }
    
    @Override
    public void show() {
        Gdx.app.log("MainMenuScreen", "Main menu displayed");
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
