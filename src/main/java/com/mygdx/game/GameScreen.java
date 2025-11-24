package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Cleaned GameScreen: null-safe, owns fixerTexture only when loaded here,
 * draws world sprites and UI in the correct order.
 */
public class GameScreen implements Screen {

    public enum GameState {
        RUNNING, PAUSED, GAMEOVER
    }

    private final MyGdxGame game;
    private Stage uiStage;
    private Skin uiSkin;
    private Table uiRoot;
    private Label docsLabel;
    private Label timeLabel;

    private final LevelManager levelManager;
    private float remainingTime = 180;
    private static final float FIXED_TIME_STEP = 1 / 60f;
    private float accumulator = 0f;

    private GameState currentState = GameState.RUNNING;
    private boolean pKeyWasPressed = false;

    private OrthographicCamera camera;
    private Texture fixerTexture;
    private boolean fixerTextureOwned = false; // only dispose if we loaded it
    private ShapeRenderer shapeRenderer;
    private Fixer fixer;

    public GameScreen(MyGdxGame game) {
        this.game = game;
        this.levelManager = new LevelManager();
        initUi();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setAutoShapeType(true);

        // SAFE texture load (try common candidates)
        Texture standingTexture = safeLoadTexture("Standing.png", "standing.PNG", "standing.png", "Stand.png");
        if (standingTexture == null) {
            Gdx.app.error("GameScreen", "Standing.png not found in assets; player will use debug fallback.");
            fixerTextureOwned = false;
        } else {
            fixerTextureOwned = true;
        }
        fixerTexture = standingTexture;

        // pass shared texture to Fixer (Fixer should NOT dispose sharedTexture if provided)
        fixer = new Fixer(100f, 100f, fixerTexture);
    }

    @Override
    public void render(float delta) {
        handleInput();

        if (currentState == GameState.RUNNING) {
            if (delta > 0.25f) delta = 0.25f;
            accumulator += delta;
            while (accumulator >= FIXED_TIME_STEP) {
                update(FIXED_TIME_STEP);
                accumulator -= FIXED_TIME_STEP;
            }
        }

        // update camera and projection matrices
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
            currentState = (currentState == GameState.RUNNING) ? GameState.PAUSED : GameState.RUNNING;
        }

        pKeyWasPressed = pKeyIsPressed;

        if (currentState == GameState.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
        }

        if (currentState == GameState.GAMEOVER &&
                (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            // If you have a main menu screen: game.setScreen(new MainMenuScreen(game));
        }
    }

    private void update(float deltaTime) {
        if (fixer != null) fixer.update(deltaTime);
        float timePenalty = 0f;
        if (levelManager != null) timePenalty = levelManager.update(deltaTime, fixer);
        remainingTime -= (deltaTime + timePenalty);

        if (remainingTime <= 0) {
            remainingTime = 0;
            currentState = GameState.GAMEOVER;
        }

        if (docsLabel != null) docsLabel.setText("Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments());
        if (timeLabel != null) {
            int minutes = (int) (remainingTime / 60);
            int seconds = (int) (remainingTime % 60);
            timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
        }

        if (levelManager.isLevelComplete()) currentState = GameState.GAMEOVER;
    }

    private void renderGame() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Let LevelManager render shapes/background first
        if (levelManager != null) levelManager.render(shapeRenderer, game.batch, game.font);

        // Draw world sprites with a single SpriteBatch begin/end
        if (game != null && game.batch != null) {
            game.batch.begin();
            // If LevelManager requires sprite drawing via batch, call its draw method here:
            // levelManager.drawSprites(game.batch);
            if (fixer != null) fixer.draw(game.batch);
            game.batch.end();
        }

        // Draw HUD text (world-space) using the same projection as the camera
        drawText();

        // Draw UI stage (screen-space)
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
        // world-space HUD (top-left / top-right)
        game.font.draw(game.batch, "Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments(),
                10, Gdx.graphics.getHeight() - 10);
        int minutes = (int) (remainingTime / 60);
        int seconds = (int) (remainingTime % 60);
        String timeText = String.format("Time: %d:%02d", minutes, seconds);
        game.font.draw(game.batch, timeText, Gdx.graphics.getWidth() - 120, Gdx.graphics.getHeight() - 10);
        game.font.draw(game.batch, "WASD/Arrows: Move | SPACE: Dash | P: Pause",
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
            String title = "GAME OVER";
            String hint = "Press R to Restart";
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

    private void restartGame() {
        currentState = GameState.RUNNING;
        remainingTime = 180;
        if (fixer != null) fixer.reset(100, 100);
        if (levelManager != null) levelManager.reset();
        accumulator = 0f;
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

    // try several candidate locations; prefer internal assets
    private Texture safeLoadTexture(String... candidates) {
        for (String c : candidates) {
            try {
                com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal(c);
                if (fh.exists()) {
                    Gdx.app.log("GameScreen", "Loaded texture from internal: " + c);
                    return new Texture(fh);
                }
                fh = Gdx.files.absolute(c);
                if (fh.exists()) {
                    Gdx.app.log("GameScreen", "Loaded texture from absolute: " + fh.path());
                    return new Texture(fh);
                }
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Error trying candidate " + c, e);
            }
        }
        return null;
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
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        // Only dispose the fixerTexture if this class loaded it
        if (fixerTextureOwned && fixerTexture != null) fixerTexture.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
    }
}
