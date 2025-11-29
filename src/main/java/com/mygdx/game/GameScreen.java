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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

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
        private com.badlogic.gdx.graphics.Texture overlayArrowTex;
        private com.badlogic.gdx.graphics.Texture overlayFullTex;
        private boolean overlayFullVisible = false;
        // Prevent the same mouse click that opened the overlay from immediately closing it
        private boolean overlaySuppressNextClick = false;

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
        // allow level 0 for the dedicated tutorial map
        this.currentLevel = Math.max(0, Math.min(level, MAX_LEVEL));
        
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
            case 0: level = new LevelTutorial(); break;
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
        
        if (fixer != null) {
            // For tutorial (level 0) make the player larger and spawn slightly higher
            if (currentLevel == 0) {
                try { fixer.setScale(1.5f); } catch (Exception ignored) {}
                fixer.reset(100, 50);
                try { fixer.setJumpVelocity(900f); } catch (Exception ignored) {}
            } else {
                try { fixer.setScale(1f); } catch (Exception ignored) {}
                fixer.reset(100, 0);
                try { fixer.setJumpVelocity(650f); } catch (Exception ignored) {}
            }
        }
        Gdx.app.log("GameScreen", "Loaded Level " + currentLevel);
        
            // Load overlay arrow texture (optional)
            try {
                overlayArrowTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/arrow.png"));
            } catch (Exception e) {
                try { overlayArrowTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("arrow.png")); }
                catch (Exception ex) { overlayArrowTex = null; }
            }
            // Load the full-screen overlay image (shown after OK)
            try {
                overlayFullTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/Overlay.png"));
            } catch (Exception e) {
                try { overlayFullTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("Overlay.png")); }
                catch (Exception ex) { overlayFullTex = null; }
            }
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
        // Player physics FIRST
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
        
        // Check win condition
        if (levelManager != null && levelManager.isLevelComplete()) {
            levelComplete();
        }
    }

    private void renderGame() {
        // ensure camera/projection are set so batch/shapeRenderer use same world coords
        if (camera != null) {
            camera.update();
            if (game != null && game.batch != null) game.batch.setProjectionMatrix(camera.combined);
            if (shapeRenderer != null) shapeRenderer.setProjectionMatrix(camera.combined);
        }

        // Render level (background, level FX, etc.)
        if (levelManager != null) {
            levelManager.render(shapeRenderer, game.batch, game.font);
        }


        // Draw dash smoke (existing logic) - unchanged
        if (game != null && game.batch != null) {
            if (fixer != null && fixer.isDashing() && shapeRenderer != null) {
                Rectangle playerBounds = fixer.getBounds();
                float playerCenterX = playerBounds.x + playerBounds.width / 2;
                float playerCenterY = playerBounds.y + playerBounds.height / 2;
                float velocityX = fixer.getVelocity().x;
                float smokeX = playerCenterX - (velocityX > 0 ? 40 : -40);
                float smokeY = playerCenterY;

                shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.6f, 0.6f, 0.65f, 0.2f);
                shapeRenderer.circle(smokeX, smokeY, 35);
                shapeRenderer.setColor(0.5f, 0.5f, 0.55f, 0.3f);
                shapeRenderer.circle(smokeX + 15, smokeY + 10, 25);
                shapeRenderer.setColor(0.4f, 0.4f, 0.45f, 0.4f);
                shapeRenderer.circle(smokeX - 10, smokeY - 10, 18);
                shapeRenderer.setColor(0.55f, 0.55f, 0.6f, 0.15f);
                shapeRenderer.circle(smokeX + 20, smokeY - 15, 20);
                shapeRenderer.circle(smokeX - 15, smokeY + 15, 20);
                shapeRenderer.end();
            }

            // Draw player sprite(s)
            game.batch.begin();
            if (fixer != null) fixer.draw(game.batch);
            game.batch.end();
        }

        // If tutorial overlay is active, draw it on top of everything
        if (currentLevel == 0) {
            drawTutorialOverlay();
        }

        // Draw the full-screen overlay if activated by the tutorial OK button
        drawFullOverlayIfActive();
    }

    /**
     * Draws an on-screen overlay for the tutorial map with movement instructions
     */
    private void drawTutorialOverlay() {
        if (shapeRenderer == null || game == null || game.batch == null || game.font == null) return;

        // Dim background slightly
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.35f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // The property-files "light" overlay was removed — the LevelTutorial
        // exposes `getPropertyFilesLight()` for optional highlighting elsewhere.
        shapeRenderer.end();
        // Instruction text: center on screen and allow customization via LevelTutorial
        String title = "MOVEMENT:";
        String detail = "Use [W], [A], [S], [D] to navigate the Archive floor.";
        String hint = "Try moving into the light near the property files.";

        // Try to obtain the active LevelTutorial (via reflection) to fetch custom text
        LevelTutorial lt = null;
        if (levelManager != null) {
            try {
                java.lang.reflect.Field f = LevelManager.class.getDeclaredField("currentLevel");
                f.setAccessible(true);
                Object cur = f.get(levelManager);
                if (cur instanceof LevelTutorial) {
                    lt = (LevelTutorial) cur;
                    title = lt.getTutorialTitle();
                    detail = lt.getTutorialDetail();
                    hint = lt.getTutorialHint();
                    // If the overlay has been dismissed, do not draw it
                    if (!lt.isShowOverlay()) {
                        Gdx.gl.glDisable(GL20.GL_BLEND);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Draw arrow asset centered instead of the box overlay
        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = Gdx.graphics.getHeight() * 0.5f;

        float arrowW = 800, arrowH = 528;
        float arrowX = centerX - arrowW * 0.5f;
        float arrowY = centerY - arrowH * 0.5f + 40f; // slightly above center to leave room for OK

        game.batch.begin();
        if (overlayArrowTex != null) {
            game.batch.draw(overlayArrowTex, arrowX, arrowY, arrowW, arrowH);
        }
        game.batch.end();

        // Draw OK button below the arrow
        float btnW = 120f, btnH = 40f;
        float btnX = centerX - btnW * 0.5f;
        float btnY = arrowY - btnH - 16f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapeRenderer.rect(btnX, btnY, btnW, btnH);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.7f);
        shapeRenderer.rect(btnX, btnY, btnW, btnH);
        shapeRenderer.end();

        com.badlogic.gdx.graphics.g2d.GlyphLayout glBtn = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, "OK");
        game.batch.begin();
        game.font.draw(game.batch, glBtn, centerX - glBtn.width * 0.5f, btnY + btnH * 0.66f + glBtn.height * 0.33f);
        game.batch.end();

        // Handle OK click
        if (lt != null && Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                try { 
                    // Hide the small tutorial overlay and show the full-screen overlay image
                    lt.setShowOverlay(false);
                    overlayFullVisible = true;
                    overlaySuppressNextClick = true; // ignore the initiating click
                } catch (Exception ignored) {}
            }
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // If the full-screen overlay is active, draw it on top of everything and allow dismissal
    private void drawFullOverlayIfActive() {
        if (!overlayFullVisible) return;
        if (game == null || game.batch == null) return;

        game.batch.begin();
        if (overlayFullTex != null) {
            game.batch.draw(overlayFullTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        game.batch.end();

        // Dismiss on any click or ESC, but ignore the click that opened the overlay
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            overlayFullVisible = false;
            overlaySuppressNextClick = false;
        } else if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
            if (overlaySuppressNextClick) {
                // consume this click (it was the OK click that opened the overlay)
                overlaySuppressNextClick = false;
            } else {
                overlayFullVisible = false;
            }
        }
    }

    private void drawText() {
        if (game == null || game.batch == null || game.font == null) return;

        game.batch.begin();
        game.font.draw(game.batch, "WASD/Arrows: Move | SPACE: Dash | P: Pause | Level: " + currentLevel,
                10, Gdx.graphics.getHeight() - 30);
        
        // Draw dash cooldown bar and text
        if (fixer != null && shapeRenderer != null) {
            float cooldown = fixer.getDashCooldown();
            float maxCooldown = 10.0f;  // Match DASH_COOLDOWN from Fixer (10 seconds)
            float barWidth = 150f;
            float barHeight = 20f;
            float barX = 10f;
            float barY = Gdx.graphics.getHeight() - 80f;
            
            // Draw dash effect glow if currently dashing
            if (fixer.isDashing()) {
                game.batch.end();
                shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0.2f, 1f, 1f, 0.3f);  // Cyan glow during dash
                shapeRenderer.rect(barX - 5, barY - 5, barWidth + 10, barHeight + 10);
                shapeRenderer.end();
                game.batch.begin();
            }
            
            // Draw cooldown bar background
            game.batch.end();
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);  // Dark gray background
            shapeRenderer.rect(barX, barY, barWidth, barHeight);
            
            // Draw cooldown bar fill
            if (cooldown > 0f) {
                float fillWidth = barWidth * (1f - (cooldown / maxCooldown));
                shapeRenderer.setColor(1f, 0f, 0f, 1f);  // Red for cooldown
                shapeRenderer.rect(barX, barY, fillWidth, barHeight);
            } else {
                shapeRenderer.setColor(0f, 1f, 0f, 1f);  // Green for ready
                shapeRenderer.rect(barX, barY, barWidth, barHeight);
            }
            
            // Draw bar border
            shapeRenderer.setColor(1f, 1f, 1f, 0.5f);  // White semi-transparent border
            shapeRenderer.rect(barX - 2, barY - 2, barWidth + 4, barHeight + 4);
            shapeRenderer.end();
            
            game.batch.begin();
            // Draw cooldown text
            String cooldownText;
            if (cooldown > 0f) {
                cooldownText = String.format("DASH: %.1fs", cooldown);
            } else {
                cooldownText = "DASH: READY";
            }
            game.font.draw(game.batch, cooldownText, barX + 10f, barY + barHeight - 5f);
        }
        
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
        if (overlayArrowTex != null) overlayArrowTex.dispose();
        if (overlayFullTex != null) overlayFullTex.dispose();
    }
}
