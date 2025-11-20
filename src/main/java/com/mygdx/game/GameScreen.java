package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen implements Screen {
    
    public enum GameState {
        RUNNING,
        PAUSED,
        GAMEOVER
    }
    
    private final MyGdxGame game;
    private Stage uiStage;
    private Skin uiSkin;
    private Table uiRoot;
    private Label docsLabel;
    private Label timeLabel;
    private boolean uiDebug = false;
    private HotReloadService uiWatcher;
    
    private final LevelManager levelManager;
    
    private float remainingTime = 180;
    
    private static final float FIXED_TIME_STEP = 1/60f;
    private float accumulator = 0f;
    
    private GameState currentState = GameState.RUNNING;
    private boolean pKeyWasPressed = false;

    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private Fixer fixer;

    public GameScreen(MyGdxGame game) {
        this.game = game;
        this.levelManager = new LevelManager();
        initUi();
        Gdx.app.log("GameScreen", "Entity Management initialized: Fixer + LevelManager");
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);
        fixer = new Fixer(100f, 100f);
    }

    @Override
    public void render(float delta) {
        handleInput();
        
        if (currentState == GameState.RUNNING) {
            if (delta > 0.25f) {
                delta = 0.25f;
            }
            
            accumulator += delta;
            while (accumulator >= FIXED_TIME_STEP) {
                update(FIXED_TIME_STEP);
                accumulator -= FIXED_TIME_STEP;
            }
        }
        
        if (camera != null) {
            camera.update();
            if (shapeRenderer != null) shapeRenderer.setProjectionMatrix(camera.combined);
            if (game != null && game.batch != null) game.batch.setProjectionMatrix(camera.combined);
        }

        renderGame();
    }
    
    private void handleInput() {
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
        
        if (currentState == GameState.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
        }
        
        if (currentState == GameState.GAMEOVER && 
            (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            game.setScreen(new MainMenuScreen(game));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            uiDebug = !uiDebug;
            if (uiStage != null) uiStage.setDebugAll(uiDebug);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            reloadUi();
        }
    }
    
    private void update(float deltaTime) {
        fixer.update(deltaTime);
        float timePenalty = levelManager.update(deltaTime, fixer);
        updateTime(deltaTime, timePenalty);
        
        if (uiStage != null) {
            if (uiWatcher != null && uiWatcher.pollReload()) {
                Gdx.app.log("UI", "Detected UI asset change. Reloading...");
                reloadUi();
            }
            if (docsLabel != null) {
                docsLabel.setText("Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments());
            }
            if (timeLabel != null) {
                int minutes = (int) (remainingTime / 60);
                int seconds = (int) (remainingTime % 60);
                timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
            }
            uiStage.act(deltaTime);
        }
        
        if (levelManager.isLevelComplete()) {
            currentState = GameState.GAMEOVER;
            Gdx.app.log("GameScreen", "YOU WIN! All documents shredded!");
        }
    }
    
    public void renderGame() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Set up blending once for the whole render cycle
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- PHASE 1: SPRITE DRAWING (SpriteBatch) ---
        game.batch.begin();

        // 1. Draw Level Sprites 
        // NOTE: The original `levelManager.render` is likely an anti-pattern if it calls 
        // shapeRenderer.begin/end internally. If you see rendering issues with the level, 
        // you should split `levelManager.render` into `drawSprites` and `drawDebug`.
        levelManager.render(shapeRenderer, game.batch, game.font); 

        // 2. Draw Fixer Sprite (DRAW FIRST)
        if (fixer != null) {
            // Replaced problematic fixer.render() with the correct fixer.draw()
            fixer.draw(game.batch); 
        }

        // 3. Draw Game Info Text (No batch.begin/end inside this method now)
        drawGameInfoTextContent(); 
        
        game.batch.end();
        // --- END SPRITE DRAWING ---

        // --- PHASE 2: DEBUG SHAPE DRAWING (ShapeRenderer) ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 4. Draw Fixer Debug Box (DRAW LAST to appear over the sprite)
        if (fixer != null) {
            fixer.renderDebug(shapeRenderer); // <--- Use the new renderDebug method
        }

        // You may need to draw level debug shapes here if levelManager.render() was only drawing sprites.

        shapeRenderer.end();
        // --- END DEBUG SHAPE DRAWING ---

        // --- PHASE 3: UI Stage and Overlays ---
        if (uiStage != null) uiStage.draw();

        if (currentState == GameState.PAUSED) {
            drawPausedOverlay();
        } else if (currentState == GameState.GAMEOVER) {
            drawGameOverOverlay();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void drawPausedOverlay() {
        // This method correctly handles its own ShapeRenderer and SpriteBatch calls.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        game.batch.begin();
        game.font.draw(game.batch, "PAUSED", 
            Gdx.graphics.getWidth() / 2 - 40, 
            Gdx.graphics.getHeight() / 2 + 20);
        game.font.draw(game.batch, "Press P to Resume", 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2 - 20);
        game.batch.end();
    }
    
    private void drawGameOverOverlay() {
        // This method correctly handles its own ShapeRenderer and SpriteBatch calls.
        boolean isWin = levelManager.isLevelComplete();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        if (isWin) {
            shapeRenderer.setColor(0, 0.5f, 0, 0.6f);
        } else {
            shapeRenderer.setColor(0.5f, 0, 0, 0.6f);
        }
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        
        game.batch.begin();
        String gameOverText = isWin ? "MISSION COMPLETE!" : "GAME OVER";
        String scoreText = "Documents Shredded: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments();
        String restartText = "Press R to Restart | ESC/Q for Main Menu";
        
        game.font.draw(game.batch, gameOverText, 
            Gdx.graphics.getWidth() / 2 - (isWin ? 80 : 50), 
            Gdx.graphics.getHeight() / 2 + 40);
        game.font.draw(game.batch, scoreText, 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2);
        game.font.draw(game.batch, restartText, 
            Gdx.graphics.getWidth() / 2 - 140, 
            Gdx.graphics.getHeight() / 2 - 40);
        game.batch.end();
    }
    
    private void restartGame() {
        currentState = GameState.RUNNING;
        remainingTime = 180;
        fixer.reset(100, 100);
        levelManager.reset();
        accumulator = 0f;
        Gdx.app.log("GameScreen", "Game RESTARTED");
    }

    private void initUi() {
        uiStage = new Stage(new ScreenViewport(), game.batch);
        loadSkinOrFallback();
        rebuildUi();

        if (Gdx.app.getType().name().equals("Desktop")) {
            uiWatcher = new HotReloadService("assets/ui");
        }
    }

    private void loadSkinOrFallback() {
        try {
            if (Gdx.files.local("assets/ui/uiskin.json").exists()) {
                if (uiSkin != null) uiSkin.dispose();
                uiSkin = new Skin(Gdx.files.local("assets/ui/uiskin.json"));
                Gdx.app.log("UI", "Loaded skin from assets/ui/uiskin.json");
                return;
            }
            if (Gdx.files.internal("ui/uiskin.json").exists()) {
                if (uiSkin != null) uiSkin.dispose();
                uiSkin = new Skin(Gdx.files.internal("ui/uiskin.json"));
                Gdx.app.log("UI", "Loaded skin from classpath ui/uiskin.json");
                return;
            }
        } catch (Exception e) {
            Gdx.app.error("UI", "Error loading skin JSON, falling back to minimal skin", e);
        }
        buildFallbackSkin();
        Gdx.app.log("UI", "Using minimal programmatic skin (no external files found)");
    }

    private void buildFallbackSkin() {
        if (uiSkin != null) uiSkin.dispose();
        uiSkin = new Skin();
        uiSkin.add("default-font", new BitmapFont());
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        uiSkin.add("white", tex);
        Drawable bg = new TextureRegionDrawable(new TextureRegion(tex));
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = uiSkin.getFont("default-font");
        uiSkin.add("default", ls);
    }

    private void rebuildUi() {
        if (uiRoot == null) {
            uiRoot = new Table();
            uiRoot.setFillParent(true);
            uiStage.addActor(uiRoot);
        } else {
            uiRoot.clear();
        }
        docsLabel = new Label("Documents: 0/0", uiSkin);
        timeLabel = new Label("Time: 0:00", uiSkin);
        Table top = new Table(uiSkin);
        top.add(docsLabel).left().pad(10);
        top.add().expandX();
        top.add(timeLabel).right().pad(10);

        uiRoot.top();
        uiRoot.add(top).expandX().fillX().row();
        uiStage.setDebugAll(uiDebug);
    }

    private void reloadUi() {
        loadSkinOrFallback();
        rebuildUi();
    }

    private void updateTime(float delta, float timePenalty) {
        remainingTime -= delta;
        remainingTime -= timePenalty;
        
        if (remainingTime <= 0) {
            remainingTime = 0;
            currentState = GameState.GAMEOVER;
            Gdx.app.log("GameScreen", "TIME'S UP - Game Over!");
        }
    }

    // Draw only non-HUD on-screen text inside the main batch block.
    // HUD values (documents/time) are handled by Scene2D `docsLabel`/`timeLabel`.
    private void drawGameInfoTextContent() {
        // Draw instructions only; caller manages batch begin/end.
        game.font.draw(game.batch, "WASD/Arrows: Move | SPACE: Dash | P: Pause", 
            50, Gdx.graphics.getHeight() - 20);
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

    @Override
    public void hide() { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (fixer != null) {
            // Fixer.dispose() exists now
            fixer.dispose();
        }
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
    }
}
