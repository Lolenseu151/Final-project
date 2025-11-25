package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * GameScreen with Level progression (1-5), level select, and completion notifications
 */
public class GameScreen implements Screen {
    public enum GameState { RUNNING, PAUSED, GAMEOVER }

    private final MyGdxGame game;
    private Stage uiStage;
    private Skin uiSkin;
    private Table uiRoot;
    private Label docsLabel;
    private Label timeLabel;

    // Game logic
    private LevelManager levelManager;
    private Fixer fixer;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private GameState currentState = GameState.RUNNING;
    private float remainingTime = 180f;
    private float accumulator = 0f;
    private boolean pKeyWasPressed = false;

    // Level progression
    private int currentLevel = 1;
    private static final int MAX_LEVEL = 5;
    private boolean showLevelComplete = false;
    private float levelCompleteTimer = 0f;
    private static final float LEVEL_COMPLETE_DELAY = 3f;

    /**
     * Constructor - pass currentLevel to load
     */
    public GameScreen(MyGdxGame game, int level) {
        this.game = game;
        this.currentLevel = Math.max(1, Math.min(level, MAX_LEVEL));
        
        // Initialize rendering
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        // Initialize UI
        initUi();
        
        // Create fixer player (will be reinitialized in show())
        fixer = new Fixer(100, 100, null);
    }

    @Override
    public void show() {
        levelManager = new LevelManager();
        
        // Load level based on currentLevel
        Level level = null;
        switch (currentLevel) {
            case 1: level = new Level1(); break;
            case 2: level = new Level2(); break;
            case 3: level = new Level3(); break;
            case 4: level = new Level4(); break;
            case 5: level = new Level5(); break;
            default: level = new Level1(); break;
        }
        
        if (level != null) {
            levelManager.loadLevel(level);
        }

        // Ensure raw keyboard input is delivered to Fixer (prevents UI stage or other processors
        // from blocking keys). This doesn't change game logic — it only sets the input target.
        Gdx.input.setInputProcessor(null);

        // Reset game state
        currentState = GameState.RUNNING;
        remainingTime = 180f;
        accumulator = 0f;
        showLevelComplete = false;
        levelCompleteTimer = 0f;
        
        if (fixer != null) fixer.reset(100, 100);
        Gdx.app.log("GameScreen", "Loaded Level " + currentLevel);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Show level complete screen
        if (showLevelComplete) {
            levelCompleteTimer += delta;
            renderLevelCompleteScreen();
            if (levelCompleteTimer >= LEVEL_COMPLETE_DELAY) {
                proceedToNextLevel();
            }
            return;
        }

        // Normal gameplay
        handleInput();
        
        if (currentState == GameState.RUNNING) {
            update(delta);
        }
        
        renderGame();
    }

    private void handleInput() {
        boolean pKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.P);

        if (pKeyIsPressed && !pKeyWasPressed && currentState != GameState.GAMEOVER) {
            currentState = (currentState == GameState.RUNNING) ? GameState.PAUSED : GameState.RUNNING;
        }

        pKeyWasPressed = pKeyIsPressed;

        if (currentState == GameState.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            show();  // reinit current level
        }

        if (currentState == GameState.GAMEOVER &&
                (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            game.setScreen(new LevelSelectScreen(game));
            dispose();
        }
    }

    private void update(float deltaTime) {
        // Player physics FIRST (input, velocity, position)
        if (fixer != null) fixer.update(deltaTime);
        
        // Then collision resolution
        float timePenalty = 0f;
        if (levelManager != null) timePenalty = levelManager.update(deltaTime, fixer);
        
        remainingTime -= (deltaTime + timePenalty);
 
         if (remainingTime <= 0) {
             remainingTime = 0;
             currentState = GameState.GAMEOVER;
         }
 
         // Update UI labels
         if (docsLabel != null && levelManager != null) {
             docsLabel.setText("Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments());
         }
         if (timeLabel != null) {
             int minutes = (int) (remainingTime / 60);
             int seconds = (int) (remainingTime % 60);
             timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
         }
 
         // Check if level is complete
         if (levelManager != null && levelManager.isLevelComplete()) {
             levelComplete();
         }
     }

    private void renderGame() {
        // Render level shapes and documents
        if (levelManager != null) {
            levelManager.render(shapeRenderer, game.batch, game.font);
        }

        // Draw world sprites
        if (game != null && game.batch != null) {
            game.batch.begin();
            if (fixer != null) fixer.draw(game.batch);
            game.batch.end();
        }

        // Draw HUD text
        drawText();

        // Draw UI stage
        if (uiStage != null) {
            uiStage.act(Math.min(Gdx.graphics.getDeltaTime(), 1/30f));
            uiStage.draw();
        }

        if (currentState == GameState.PAUSED) drawPausedOverlay();
        else if (currentState == GameState.GAMEOVER) drawGameOverOverlay();
    }

    private void drawText() {
        if (game == null || game.batch == null || game.font == null) return;

        game.batch.begin();
        game.font.draw(game.batch, "WASD/Arrows: Move | SPACE: Dash | P: Pause | Level: " + currentLevel,
                10, Gdx.graphics.getHeight() - 30);
        game.batch.end();
    }

    private void drawPausedOverlay() {
        if (shapeRenderer == null || game == null || game.batch == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();

        if (game.font != null) {
            String text = "PAUSED";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, text);
            float x = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
            float y = (Gdx.graphics.getHeight() + layout.height) * 0.5f;

            game.batch.begin();
            game.font.draw(game.batch, layout, x, y);
            game.batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawGameOverOverlay() {
        if (shapeRenderer == null || game == null || game.batch == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();

        if (game.font != null) {
            String title = "GAME OVER - Time's Up!";
            String hint = "Press R to Restart or ESC for Level Select";
            com.badlogic.gdx.graphics.g2d.GlyphLayout titleLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, title);
            com.badlogic.gdx.graphics.g2d.GlyphLayout hintLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, hint);

            float centerX = Gdx.graphics.getWidth() * 0.5f;
            float centerY = Gdx.graphics.getHeight() * 0.5f;

            float titleX = centerX - (titleLayout.width * 0.5f);
            float titleY = centerY + (titleLayout.height * 0.5f) + 10f;

            float hintX = centerX - (hintLayout.width * 0.5f);
            float hintY = centerY - (hintLayout.height * 0.5f) - 10f;

            game.batch.begin();
            game.font.draw(game.batch, titleLayout, titleX, titleY);
            game.font.draw(game.batch, hintLayout, hintX, hintY);
            game.batch.end();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void levelComplete() {
        showLevelComplete = true;
        levelCompleteTimer = 0f;
        Gdx.app.log("GameScreen", "Level " + currentLevel + " Complete!");
    }

    private void renderLevelCompleteScreen() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        if (game.font != null) {
            String mainMsg = "LEVEL " + currentLevel + " COMPLETED!";
            String nextMsg = (currentLevel >= MAX_LEVEL) ? "All Levels Complete!" : "Proceeding to Level " + (currentLevel + 1) + "...";
            
            com.badlogic.gdx.graphics.g2d.GlyphLayout mainLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, mainMsg);
            com.badlogic.gdx.graphics.g2d.GlyphLayout nextLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, nextMsg);

            float centerX = Gdx.graphics.getWidth() * 0.5f;
            float centerY = Gdx.graphics.getHeight() * 0.5f;

            game.font.draw(game.batch, mainLayout, centerX - mainLayout.width * 0.5f, centerY + 50);
            game.font.draw(game.batch, nextLayout, centerX - nextLayout.width * 0.5f, centerY - 50);
        }
        game.batch.end();
    }

    private void proceedToNextLevel() {
        if (currentLevel >= MAX_LEVEL) {
            // All levels completed
            Gdx.app.log("GameScreen", "All levels completed!");
            game.setScreen(new MainMenuScreen(game));
            dispose();
        } else {
            // Load next level
            currentLevel++;
            show();  // reinit for next level
            showLevelComplete = false;
            levelCompleteTimer = 0f;
        }
    }

    private void initUi() {
        uiStage = new Stage(new ScreenViewport(), game.batch);

        uiSkin = new Skin();
        BitmapFont font = new BitmapFont();
        uiSkin.add("default-font", font);

        // Minimal background texture for labels
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        uiSkin.add("white", tex);
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        uiSkin.add("default", ls);

        uiRoot = new Table();
        uiRoot.setFillParent(true);
        uiStage.addActor(uiRoot);

        docsLabel = new Label("Documents: 0/0", uiSkin);
        timeLabel = new Label("Time: 0:00", uiSkin);

        Table top = new Table(uiSkin);
        top.add(docsLabel).left().pad(10);
        top.add().expandX();
        top.add(timeLabel).right().pad(10);

        uiRoot.top();
        uiRoot.add(top).expandX().fillX().row();
    }

    @Override
    public void resize(int width, int height) {
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
        if (camera != null) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (levelManager != null) levelManager.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
        if (fixer != null) fixer.dispose();
    }
}
