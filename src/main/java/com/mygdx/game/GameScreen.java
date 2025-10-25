package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GameScreen implements Screen {
    
    /**
     * Game State Enum - Controls the flow of the game
     */
    public enum GameState {
        RUNNING,    // Game is actively running
        PAUSED,     // Game is paused (no updates)
        GAMEOVER    // Game has ended (time ran out or player died)
    }
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    
    // Entity Management - Role 5
    private final Fixer fixer;              // The player (Physics role)
    private final LevelManager levelManager; // Level management (Role 3)
    
    private float remainingTime = 60; // 60 seconds game time
    
    // Fixed time-step constants for consistent physics
    private static final float FIXED_TIME_STEP = 1/60f; // 60 FPS physics update
    private float accumulator = 0f;
    
    // State management
    private GameState currentState = GameState.RUNNING;
    private boolean pKeyWasPressed = false; // For toggle detection

    public GameScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        
        // Initialize entities - Entity Management (Role 5)
        this.fixer = new Fixer(100, 100);
        this.levelManager = new LevelManager();
        
        Gdx.app.log("GameScreen", "Entity Management initialized: Fixer + LevelManager");
    }

    @Override
    public void render(float delta) {
        // Handle input for state changes (always check, even when paused)
        handleInput();
        
        // Only update game logic if RUNNING
        if (currentState == GameState.RUNNING) {
            // Cap delta to prevent "spiral of death" on very slow frames
            if (delta > 0.25f) {
                delta = 0.25f;
            }
            
            // Fixed time-step update loop
            accumulator += delta;
            while (accumulator >= FIXED_TIME_STEP) {
                update(FIXED_TIME_STEP);
                accumulator -= FIXED_TIME_STEP;
            }
        }
        
        // Always render (so we can see paused/game over screens)
        renderGame();
    }
    
    /**
     * Handles input for state management (pause, resume, restart)
     */
    private void handleInput() {
        // Toggle pause with P key (only when not game over)
        boolean pKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.P);
        
        if (pKeyIsPressed && !pKeyWasPressed && currentState != GameState.GAMEOVER) {
            if (currentState == GameState.RUNNING) {
                currentState = GameState.PAUSED;
                Gdx.app.log("GameScreen", "Game PAUSED");
            } else if (currentState == GameState.PAUSED) {
                currentState = GameState.RUNNING;
                Gdx.app.log("GameScreen", "Game RESUMED");
            }
        }
        
        pKeyWasPressed = pKeyIsPressed;
        
        // Press R to restart when game over
        if (currentState == GameState.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
        }
    }
    
    /**
     * Main game update method - called at fixed intervals for consistent physics.
     * This ensures the game runs the same on all machines regardless of frame rate.
     * 
     * Entity Management (Role 5): Manages the update flow for all entities
     * 
     * @param deltaTime Fixed time step (1/60th of a second = ~0.0167 seconds)
     */
    private void update(float deltaTime) {
        // Update entities in correct order (Role 5 - Entity Management)
        fixer.update(deltaTime);                              // Update player (Physics role)
        float timePenalty = levelManager.update(deltaTime, fixer); // Update level (Role 3)
        updateTime(deltaTime, timePenalty);                   // Update game timer
        
        // Check win condition
        if (levelManager.isLevelComplete()) {
            currentState = GameState.GAMEOVER; // Reuse game over for win (can add WIN state later)
            Gdx.app.log("GameScreen", "YOU WIN! All documents shredded!");
        }
    }
    
    /**
     * Handles all rendering - called at variable frame rate.
     * Separating update and render ensures smooth visuals on high refresh rate monitors.
     * 
     * Entity Management (Role 5): Manages the render flow for all entities
     */
    private void renderGame() {
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Enable blending for alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Render entities in correct order (Role 5 - Entity Management)
        levelManager.render(shapeRenderer, game.batch, game.font);  // Render level elements first (Role 3)
        fixer.render(shapeRenderer);         // Render player on top (Physics role)
        drawText();                          // Render UI last
        
        // Draw state-specific overlays
        if (currentState == GameState.PAUSED) {
            drawPausedOverlay();
        } else if (currentState == GameState.GAMEOVER) {
            drawGameOverOverlay();
        }

        // Disable blending
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    /**
     * Draws semi-transparent overlay for paused state
     */
    private void drawPausedOverlay() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f); // Semi-transparent black
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        game.batch.begin();
        String pausedText = "PAUSED";
        String instructionText = "Press P to Resume";
        
        // Center the text
        game.font.draw(game.batch, pausedText, 
            Gdx.graphics.getWidth() / 2 - 40, 
            Gdx.graphics.getHeight() / 2 + 20);
        game.font.draw(game.batch, instructionText, 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2 - 20);
        game.batch.end();
    }
    
    /**
     * Draws game over overlay
     */
    private void drawGameOverOverlay() {
        boolean isWin = levelManager.isLevelComplete();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isWin) {
            shapeRenderer.setColor(0, 0.5f, 0, 0.6f); // Semi-transparent green for win
        } else {
            shapeRenderer.setColor(0.5f, 0, 0, 0.6f); // Semi-transparent red for loss
        }
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        game.batch.begin();
        String gameOverText = isWin ? "MISSION COMPLETE!" : "GAME OVER";
        String scoreText = "Documents Shredded: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments();
        String restartText = "Press R to Restart";
        
        // Center the text
        game.font.draw(game.batch, gameOverText, 
            Gdx.graphics.getWidth() / 2 - (isWin ? 80 : 50), 
            Gdx.graphics.getHeight() / 2 + 40);
        game.font.draw(game.batch, scoreText, 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2);
        game.font.draw(game.batch, restartText, 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2 - 40);
        game.batch.end();
    }
    
    /**
     * Restarts the game to initial state
     */
    private void restartGame() {
        currentState = GameState.RUNNING;
        remainingTime = 60;
        fixer.reset(100, 100);
        levelManager.reset();
        accumulator = 0f;
        Gdx.app.log("GameScreen", "Game RESTARTED");
    }

    private void updateTime(float delta, float timePenalty) {
        remainingTime -= delta;
        remainingTime -= timePenalty; // Apply penalties from Auditor Beams
        
        if (remainingTime <= 0) {
            remainingTime = 0;
            currentState = GameState.GAMEOVER;
            Gdx.app.log("GameScreen", "TIME'S UP - Game Over!");
        }
    }

    private void drawText() {
        game.batch.begin();
        game.font.draw(game.batch, "Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments(), 
            50, Gdx.graphics.getHeight() - 20);
        game.font.draw(game.batch, "Time: " + String.format("%.1f", remainingTime), 
            Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 20);
        
        // Show instructions
        game.font.draw(game.batch, "WASD/Arrows: Move | SPACE: Dash | P: Pause", 
            50, Gdx.graphics.getHeight() - 40);
        game.batch.end();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}