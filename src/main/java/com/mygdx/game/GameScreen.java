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
        // Tutorial talking overlay (shown when first doc collected)
        private com.badlogic.gdx.graphics.Texture overlayTalkingTex;
        private boolean overlayTalkingVisible = false;
        // --- Talking overlay tunables (edit these PUBLIC static values to reposition the overlay text) ---
        // Example: change these values at the top of this file to move the overlay text.
        // Width as fraction of screen (0.0 - 1.0). Max width caps the computed width.
        public static float TALKING_TEXT_WIDTH_PERCENT = 0.60f;
        public static float TALKING_TEXT_MAX_WIDTH = 680f;
        // Height as fraction of screen (0.0 - 1.0). Min height ensures readability.
        public static float TALKING_TEXT_HEIGHT_PERCENT = 0.18f;
        public static float TALKING_TEXT_MIN_HEIGHT = 10f;
        // Margin from screen edges (in pixels) and vertical gap above the Continue button
        public static float TALKING_TEXT_MARGIN = 30f;
        public static float TALKING_TEXT_BUTTON_GAP = 8f;
        // --- Continue button tunables ---
        // If BUTTON_X/Y are >= 0 they will be used as absolute screen coordinates (pixels).
        // Otherwise the button is positioned relative to the text box (default behavior).
        public static float TALKING_BUTTON_X = 1030f;
        public static float TALKING_BUTTON_Y = 60f;
        public static float TALKING_BUTTON_WIDTH = 160f;
        public static float TALKING_BUTTON_HEIGHT = 28f;
        // If false, do not draw the filled background rectangle (transparent button)
        public static boolean TALKING_BUTTON_DRAW_BG = false;
        // If false, do not draw the button border/stroke (transparent border)
        public static boolean TALKING_BUTTON_DRAW_BORDER = false;
        // Hover underline tunables
        public static boolean TALKING_BUTTON_HOVER_UNDERLINE = true;
        public static float TALKING_BUTTON_HOVER_UNDERLINE_THICKNESS = 2f;
        public static float TALKING_BUTTON_HOVER_UNDERLINE_R = 1f;
        public static float TALKING_BUTTON_HOVER_UNDERLINE_G = 1f;
        public static float TALKING_BUTTON_HOVER_UNDERLINE_B = 1f;
        public static float TALKING_BUTTON_HOVER_UNDERLINE_A = 1f;
        // Optional absolute Y for the talking text box. If >=0, used as pixel Y coordinate.
        // If left negative (default), the text box Y is computed independently of the button.
        public static float TALKING_TEXT_ABSOLUTE_Y = 50f;
        // Typing transition tunable (seconds)
        public static float TALKING_TEXT_TYPING_DURATION = 3f;
        // The default full talking overlay text (can be customized)
        public static String TALKING_OVERLAY_FULL_TEXT = "Now, listen closely. They are hunting for the 'Poblacion Water Fund Diversion' file. The one that shows... [whispers dramatically] ...me corrupting the people's money.";

        // Runtime typing state
        private com.badlogic.gdx.graphics.g2d.BitmapFont overlayTalkingFont = null;
        private float talkingTypingElapsed = 0f;
        private boolean talkingPreviouslyVisible = false;
        // Talking overlay stage (0 = first caption, 1 = second caption)
        private int talkingStage = 0;
        // Second caption to display after Continue is clicked once
        public static String TALKING_OVERLAY_SECOND_TEXT = "Yes, I said it! I need that gone. Or, better yet, changed.";
        // Third caption to display after second Continue click
        public static String TALKING_OVERLAY_THIRD_TEXT = "See that other document on the other floors? You must collect them all";

        // Tutorial document positions have been moved to `LevelTutorial.TUTORIAL_DOC_POSITIONS`.

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
            // Load the tutorial talking overlay (shown when first document collected)
            try {
                overlayTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/Loloy's talking.png"));
            } catch (Exception e) {
                try { overlayTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("Loloy's talking.png")); }
                catch (Exception ex) {
                    try { overlayTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/Loloy talk.png")); }
                    catch (Exception ex2) {
                        try { overlayTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("Loloy talk.png")); }
                        catch (Exception ex3) { overlayTalkingTex = null; }
                    }
                }
            }
            // Load the small white font for overlay text (optional)
            try {
                overlayTalkingFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(
                        Gdx.files.internal("assets/smallwhite/Small_white.fnt"),
                        Gdx.files.internal("assets/smallwhite/Small_white.png"),
                        false);
                Gdx.app.log("GameScreen", "Loaded Small_white font from assets/smallwhite/");
            } catch (Exception e) {
                try {
                    overlayTalkingFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(
                            Gdx.files.internal("smallwhite/Small_white.fnt"),
                            Gdx.files.internal("smallwhite/Small_white.png"),
                            false);
                    Gdx.app.log("GameScreen", "Loaded Small_white font from smallwhite/ fallback");
                } catch (Exception ex) {
                    overlayTalkingFont = null; // fallback to game.font when drawing
                    Gdx.app.log("GameScreen", "Small_white font not found; using default font");
                }
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
        // Draw the talking overlay if the tutorial level requested it (first doc collected)
        drawTalkingOverlayIfActive();
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

    // Draw the tutorial talking overlay (triggered when first document is collected)
    private void drawTalkingOverlayIfActive() {
        // Prioritize the talking overlay only when current level is tutorial and that level wants it
        if (levelManager == null) return;
        try {
            java.lang.reflect.Field f = LevelManager.class.getDeclaredField("currentLevel");
            f.setAccessible(true);
            Object cur = f.get(levelManager);
            if (cur instanceof LevelTutorial) {
                LevelTutorial lt = (LevelTutorial) cur;
                if (!lt.isTalkingOverlayVisible()) {
                    // Reset typing state when overlay is closed
                    talkingPreviouslyVisible = false;
                    return;
                }
                // Show the talking texture full-screen-ish centered (or scaled)
                if (game == null || game.batch == null) return;
                float w = Gdx.graphics.getWidth();
                float h = Gdx.graphics.getHeight();
                game.batch.begin();
                if (overlayTalkingTex != null) {
                    // Draw the talking image full-screen
                    game.batch.draw(overlayTalkingTex, 0, 0, w, h);
                }
                game.batch.end();

                // Draw an overlay text box and a Continue button (only the button will dismiss)

                // Text box area (margin and button location) using the tunable values
                float margin = TALKING_TEXT_MARGIN;
                float btnW = TALKING_BUTTON_WIDTH, btnH = TALKING_BUTTON_HEIGHT;
                // Compute button position: prefer explicit tunables if set, otherwise place relative to edges
                float btnX = (TALKING_BUTTON_X >= 0f) ? TALKING_BUTTON_X : (w - btnW - margin);
                float btnY = (TALKING_BUTTON_Y >= 0f) ? TALKING_BUTTON_Y : margin;

                // Position text using top-level tunables. Use an absolute Y if provided,
                // otherwise compute a default that does NOT depend on the button Y.
                float textBoxW = Math.min(w * TALKING_TEXT_WIDTH_PERCENT, TALKING_TEXT_MAX_WIDTH);
                float textBoxH = Math.max(h * TALKING_TEXT_HEIGHT_PERCENT, TALKING_TEXT_MIN_HEIGHT);
                // Position text on the LEFT side (use margin from left edge)
                float textBoxX = TALKING_TEXT_MARGIN + 220;
                float textBoxY;
                if (TALKING_TEXT_ABSOLUTE_Y >= 0f) {
                    textBoxY = TALKING_TEXT_ABSOLUTE_Y;
                } else {
                    // Default: use a low screen position independent of button Y
                    textBoxY = TALKING_TEXT_MARGIN + TALKING_TEXT_BUTTON_GAP + btnH;
                }

                // No background rectangle for the text box (transparent)

                // Draw the overlay text with wrapping and typing transition
                com.badlogic.gdx.graphics.g2d.BitmapFont fontToUse = (overlayTalkingFont != null) ? overlayTalkingFont : game.font;
                // Reset typing timer when the overlay first appears
                if (!talkingPreviouslyVisible) {
                    talkingTypingElapsed = 0f;
                    talkingPreviouslyVisible = true;
                    talkingStage = 0; // start at first caption when overlay appears
                }
                talkingTypingElapsed += Gdx.graphics.getDeltaTime();

                float duration = Math.max(0.001f, TALKING_TEXT_TYPING_DURATION);
                float frac = Math.min(1f, talkingTypingElapsed / duration);
                String fullText;
                if (talkingStage == 0) fullText = TALKING_OVERLAY_FULL_TEXT;
                else if (talkingStage == 1) fullText = TALKING_OVERLAY_SECOND_TEXT;
                else fullText = TALKING_OVERLAY_THIRD_TEXT;
                int chars = Math.max(0, Math.min(fullText.length(), (int) (fullText.length() * frac)));
                String visibleText = fullText.substring(0, chars);

                if (game.batch != null && fontToUse != null) {
                    float textPad = 12f;
                    com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
                    gl.setText(fontToUse, visibleText, com.badlogic.gdx.graphics.Color.WHITE, textBoxW - textPad * 2f, com.badlogic.gdx.utils.Align.left, true);
                    game.batch.begin();
                    fontToUse.draw(game.batch, gl, textBoxX + textPad, textBoxY + textBoxH - textPad);
                    game.batch.end();
                }

                // Draw Continue button (background optional)
                if (shapeRenderer != null) {
                    if (TALKING_BUTTON_DRAW_BG) {
                        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
                        shapeRenderer.setColor(0.12f, 0.12f, 0.12f, 1f);
                        shapeRenderer.rect(btnX, btnY, btnW, btnH);
                        shapeRenderer.end();
                    }

                    // Draw border only if enabled (allows fully transparent button)
                    if (TALKING_BUTTON_DRAW_BORDER) {
                        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
                        shapeRenderer.setColor(1f, 1f, 1f, 0.85f);
                        shapeRenderer.rect(btnX, btnY, btnW, btnH);
                        shapeRenderer.end();
                    }
                }

                // Draw button label and hover underline
                if (game.batch != null) {
                    com.badlogic.gdx.graphics.g2d.BitmapFont btnFont = (overlayTalkingFont != null) ? overlayTalkingFont : game.font;
                    com.badlogic.gdx.graphics.g2d.GlyphLayout glBtn = new com.badlogic.gdx.graphics.g2d.GlyphLayout(btnFont, "Continue");
                    // Vertically center the label inside the button using the glyph height as reference.
                    float textX = btnX + (btnW - glBtn.width) * 0.5f;
                    float textY = btnY + (btnH + glBtn.height) * 0.5f; // baseline for drawing
                    game.batch.begin();
                    btnFont.draw(game.batch, glBtn, textX, textY);
                    game.batch.end();

                    // Hover detection
                    float mx = Gdx.input.getX();
                    float my = Gdx.graphics.getHeight() - Gdx.input.getY();
                    boolean hovered = (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH);

                    if (hovered && TALKING_BUTTON_HOVER_UNDERLINE && shapeRenderer != null) {
                        // Draw underline BELOW the bottom of the laid-out glyphs so it doesn't overlap characters.
                        float thickness = TALKING_BUTTON_HOVER_UNDERLINE_THICKNESS;
                        // GlyphLayout.height represents the total vertical size of the text. When
                        // drawing at (textX, textY) the baseline is at textY and the bottom of the
                        // visible glyphs is roughly at (textY - glBtn.height). Place the underline
                        // a few pixels below that point.
                        float underlineY = textY - glBtn.height - 4f; // 4px padding below glyphs
                        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
                        shapeRenderer.setColor(TALKING_BUTTON_HOVER_UNDERLINE_R, TALKING_BUTTON_HOVER_UNDERLINE_G, TALKING_BUTTON_HOVER_UNDERLINE_B, TALKING_BUTTON_HOVER_UNDERLINE_A);
                        shapeRenderer.rect(textX, underlineY, glBtn.width, thickness);
                        shapeRenderer.end();
                    }

                    // Only dismiss / advance when clicking the Continue button
                    if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                            // Advance through captions: 0 -> 1 -> 2 -> dismiss
                            if (talkingStage == 0) {
                                talkingStage = 1;
                                talkingTypingElapsed = 0f; // restart typing for second caption
                            } else if (talkingStage == 1) {
                                talkingStage = 2;
                                talkingTypingElapsed = 0f; // restart typing for third caption
                            } else {
                                lt.setTalkingOverlayVisible(false);
                                // After final talking caption, spawn tutorial documents.
                                try {
                                    if (levelManager != null) {
                                        if (LevelTutorial.TUTORIAL_DOC_POSITIONS != null && LevelTutorial.TUTORIAL_DOC_POSITIONS.length > 0) {
                                            levelManager.addDocumentsAtPositions(LevelTutorial.TUTORIAL_DOC_POSITIONS);
                                        } else {
                                            levelManager.addDocuments(4);
                                        }
                                    }
                                } catch (Exception ignored) {}
                                talkingStage = 0;
                                talkingPreviouslyVisible = false;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
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
        if (overlayTalkingTex != null) overlayTalkingTex.dispose();
        if (overlayTalkingFont != null) overlayTalkingFont.dispose();
    }
}
