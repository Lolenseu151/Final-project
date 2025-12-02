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
// hover sound is provided centrally via HoverSoundManager on MyGdxGame
import java.util.HashMap;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.mygdx.game.Levels.Level;
import com.mygdx.game.Levels.Level1;
import com.mygdx.game.Levels.Level2;
import com.mygdx.game.Levels.Level3;
import com.mygdx.game.Levels.Level3_1;
import com.mygdx.game.Levels.Level4;
import com.mygdx.game.Levels.Level5;
import com.mygdx.game.Levels.Level5_1;

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
    private LevelMusicManager musicManager;
    private LevelManager2 levelManager2;
    private Fixer fixer;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private GameState currentState = GameState.RUNNING;
    private float remainingTime = 180f;
    private float accumulator = 0f;
    private boolean pKeyWasPressed = false;

    // --- pause/resume/freeze helpers ---
    private boolean initialized = false;                 // prevent re-init on show() after minimize
    private float savedX = Float.NaN, savedY = Float.NaN;
    private float savedVelX = 0f, savedVelY = 0f;
    private boolean wasPaused = false;                  // true when pause() was called (used to avoid unintended resets)
    
    // Floating UI icons (document counter)
    // Documents spritesheet (2 cols x 3 rows = 6 frames)
    private Texture documentSheetTexture;
    private com.badlogic.gdx.graphics.g2d.TextureRegion[] documentFrames;
    private float docAnimTime = 0f;
    private float docFrameDuration = 0.12f; // seconds per frame
    // Background image for the audit timer (centered above the timer)
    private Texture auditBgTexture;
    // Small clock icon to display beside the time (replaces the audit background)
    private Texture clockTexture;
    // Pause UI
    private Texture pauseButtonTexture;
    private Texture overlayPauseTex;
    private Texture btnRestartTex; // assets/buttons/10.png
    private Texture btnResumeTex;  // assets/buttons/11.png
    private Texture btnMenuTex;    // assets/buttons/12.png
    private boolean pauseOverlayVisible = false;
    private float docIconY;
    // Previous hover state map (play once on enter). Hover sound provided centrally.
    private HashMap<String, Boolean> hoverPrev = new HashMap<>();
    // Scale for the overlay stat font (adjust to increase/decrease stat text size)
    public static float STAT_FONT_SCALE = 2.0f;
    private float floatTimer = 0f;  // Track time for floating animation
    private BitmapFont uiFont;  // Font for timer and doc counter text
    private BitmapFont docFont; // Smaller font for document count only
    private BitmapFont timeFont; // Font specifically for the audit time
    // Font used for overlay stat text (documents/time)
    private BitmapFont  statFont;
        private com.badlogic.gdx.graphics.Texture overlayArrowTex;
        private com.badlogic.gdx.graphics.Texture overlayFullTex;
        private boolean overlayFullVisible = false;
        // Prevent the same mouse click that opened the overlay from immediately closing it
        private boolean overlaySuppressNextClick = false;
        // When an overlay appears we briefly suppress the click that opened it so the
        // same input doesn't immediately activate an overlay button. This timer
        // ensures suppression only lasts a short time instead of until the next click.
        private float overlaySuppressTimer = 0f; // seconds
        // Tutorial talking overlay (shown when first doc collected)
        private com.badlogic.gdx.graphics.Texture overlayTalkingTex;
        private boolean overlayTalkingVisible = false;
        // Win / GameOver overlays
        private com.badlogic.gdx.graphics.Texture overlayWinTex;
        private com.badlogic.gdx.graphics.Texture overlayGameOverTex;
        // Whether the Win overlay is currently visible and blocking progression
        private boolean winOverlayVisible = false;
        // Record the event stats to display on overlay (docs collected and time at event)
        private int lastEventDocs = 0;
        private float lastEventTime = 0f;
        private boolean lastEventRecorded = false;
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
        // Vertical offset for the audit time relative to the document baseline (pixels)
        public static float AUDIT_TIME_VERTICAL_OFFSET = 14f;
        // Vertical gap (pixels) between the two stat lines shown on overlays (documents / time)
        public static float STAT_LINE_GAP = 140f;
        // Horizontal spacing between Win overlay action buttons
        public static float WIN_BUTTON_SPACING = 48f;
        // Per-button scale multipliers for the Win overlay (allows shrinking specific buttons)
        public static float WIN_RETRY_SCALE = 0.75f; // retry (btn 10)
        public static float WIN_MENU_SCALE  = 0.75f; // menu  (btn 12)
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
        // Avoid re-initializing everything if show() is called again (e.g. on minimize/restore)
        if (initialized) {
            Gdx.app.log("GameScreen", "show() called but already initialized - skipping re-init");
            return;
        }
         musicManager = new LevelMusicManager();
        
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
        
        // Use LevelManager2 for multi-map levels (3 and 5) to enable map transitions with shared progress
        if (currentLevel == 3 || currentLevel == 5) {
            levelManager = null;
            levelManager2 = new LevelManager2();
            // Load only the first map initially - the level will handle transitioning to the second map
            if (level != null) {
                levelManager2.loadLevel(level);
                try {
                    // Register document-collected listener so UI/audio can react
                    levelManager2.setDocumentCollectedListener(new LevelManager2.DocumentCollectedListener() {
                        @Override
                        public void onDocumentCollected(int collected, int total) {
                            try {
                                if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playDocument();
                            } catch (Exception ignored) {}
                        }
                    });
                } catch (Exception ignored) {}
                // Start playing background music for the level using global manager
                String musicPath = level.getMusicPath();
                if (musicPath != null && !musicPath.isEmpty()) {
                    BackgroundMusicManager.getInstance().playLevelMusic(musicPath);
                }
            }
        } else {
            // Use regular LevelManager for single-map levels
            levelManager2 = null;
            levelManager = new LevelManager();
            if (level != null) {
                levelManager.loadLevel(level);
                // Start playing background music for the level using global manager
                String musicPath = level.getMusicPath();
                if (musicPath != null && !musicPath.isEmpty()) {
                    BackgroundMusicManager.getInstance().playLevelMusic(musicPath);
                }
            // Register for direct level-complete callbacks so the overlay can be shown
                try {
                    // Use reflection to avoid a compile-time dependency on the nested listener type
                    try {
                        Class<?> listenerClass = Class.forName("com.mygdx.game.LevelManager$LevelCompleteListener");
                        java.lang.reflect.Method setMethod = levelManager.getClass().getMethod("setLevelCompleteListener", listenerClass);
                        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                                listenerClass.getClassLoader(),
                                new Class<?>[] { listenerClass },
                                new java.lang.reflect.InvocationHandler() {
                                    @Override
                                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                                        if ("onLevelComplete".equals(method.getName())) {
                                            // Ensure overlay state is updated on the main thread
                                            try {
                                                Gdx.app.postRunnable(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            levelComplete();
                                                        } catch (Exception ignored) {}
                                                    }
                                                });
                                            } catch (Exception e) {
                                                // Fallback: call directly if postRunnable is unavailable
                                                try { levelComplete(); } catch (Exception ignored) {}
                                            }
                                        }
                                        return null;
                                    }
                                });
                        setMethod.invoke(levelManager, proxy);
                    } catch (ClassNotFoundException cnfe) {
                        // Listener type not present - skip registering the callback
                    }
                } catch (Exception ignored) {}
                // Also register a ShredStartListener (fires when shredding becomes ACTIVE)
                try {
                    try {
                        Class<?> shredClass = Class.forName("com.mygdx.game.LevelManager$ShredStartListener");
                        java.lang.reflect.Method setShredMethod = levelManager.getClass().getMethod("setShredStartListener", shredClass);
                        Object shredProxy = java.lang.reflect.Proxy.newProxyInstance(
                                shredClass.getClassLoader(),
                                new Class<?>[] { shredClass },
                                new java.lang.reflect.InvocationHandler() {
                                    @Override
                                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                                        if ("onShredStart".equals(method.getName())) {
                                            try {
                                                Gdx.app.postRunnable(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playShred();
                                                            try { if (musicManager != null) musicManager.stopMusic(); } catch (Exception ignored) {}
                                                        } catch (Exception ignored) {}
                                                    }
                                                });
                                            } catch (Exception e) {
                                                try {
                                                    if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playShred();
                                                    try { if (musicManager != null) musicManager.stopMusic(); } catch (Exception ignored) {}
                                                } catch (Exception ignored) {}
                                            }
                                        }
                                        return null;
                                    }
                                });
                        setShredMethod.invoke(levelManager, shredProxy);
                    } catch (ClassNotFoundException cnfe) {
                        // Not present - ignore
                    }
                } catch (Exception ignored) {}
                // Register document-collected listener on single-map LevelManager
                try {
                    levelManager.setDocumentCollectedListener(new LevelManager.DocumentCollectedListener() {
                        @Override
                        public void onDocumentCollected(int collected, int total) {
                            try {
                                if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playDocument();
                            } catch (Exception ignored) {}
                        }
                    });
                } catch (Exception ignored) {}
            }
        }

        // Ensure raw keyboard input is delivered to Fixer (prevents UI stage or other processors
        // from blocking keys). This doesn't change game logic ΓÇö it only sets the input target.
        Gdx.input.setInputProcessor(null);

        // Reset game state
        currentState = GameState.RUNNING;
        remainingTime = 180f;
        accumulator = 0f;
        showLevelComplete = false;
        levelCompleteTimer = 0f;
        // Reset overlay event recording
        lastEventRecorded = false;
        lastEventDocs = 0;
        lastEventTime = 0f;
        
        if (fixer != null) {
            // Level-specific adjustments: shrink the player size for Level 1 only
            try {
                if (currentLevel == 1) {
                    // Reduce visual/collision size to 64x64 for this level
                    fixer.getBounds().setSize(64f, 64f);
                    Gdx.app.log("GameScreen", "Applied Level1-specific fixer size: 64x64");
                }
            } catch (Exception ignored) {}
            // If we are here because the app was paused (minimized), avoid calling reset()
            // which moves the player to a spawn. Instead restore the saved position if available.
            if (wasPaused && !Float.isNaN(savedX)) {
                try {
                    fixer.getBounds().setPosition(savedX, savedY);
                    com.badlogic.gdx.math.Vector2 vel = fixer.getVelocity();
                    if (vel != null) vel.set(savedVelX, savedVelY);
                } catch (Exception ignored) {}
            } else {
                // Normal initial spawn behavior (only when not resuming from pause)
                if (currentLevel == 0) {
                    fixer.reset(100, 50);
                } else if (currentLevel == 3 && level instanceof Level3) {
                    try {
                        float[] sp = ((Level3)level).getEntranceSpawn();
                        if (sp != null && sp.length >= 2) fixer.reset(sp[0], sp[1]);
                        else fixer.reset(100, 0);
                    } catch (Exception e) {
                        fixer.reset(100, 0);
                    }
                } else if (currentLevel == 4 && level instanceof Level4) {
                    try {
                        float[] sp = ((Level4)level).getEntranceSpawn();
                        if (sp != null && sp.length >= 2) fixer.reset(sp[0], sp[1]);
                        else fixer.reset(100, 0);
                    } catch (Exception e) {
                        fixer.reset(100, 0);
                    }
                } else if (currentLevel == 5 && level instanceof Level5) {
                    try {
                        float[] sp = ((Level5)level).getEntranceSpawn();
                        if (sp != null && sp.length >= 2) fixer.reset(sp[0], sp[1]);
                        else fixer.reset(640, 580);
                    } catch (Exception e) {
                        fixer.reset(640, 580);
                    }
                } else {
                    fixer.reset(100, 0);
                }
            }
        }
        Gdx.app.log("GameScreen", "Loaded Level " + currentLevel);
        // mark as initialized so future show() calls (from minimize/restore) do not reload/reset
        initialized = true;
        
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
            // Load win / gameover overlays
            try {
                overlayWinTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/overlay/Win.png"));
            } catch (Exception e) {
                try { overlayWinTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("overlay/Win.png")); } catch (Exception ex) { overlayWinTex = null; }
            }
            try {
                overlayGameOverTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/overlay/Gameover.png"));
            } catch (Exception e) {
                try { overlayGameOverTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("overlay/Gameover.png")); } catch (Exception ex) { overlayGameOverTex = null; }
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
            
            // Load UI font for timer and document counter
            try {
                uiFont = new BitmapFont(
                        Gdx.files.internal("assets/smallwhite/Small_white.fnt"),
                        Gdx.files.internal("assets/smallwhite/Small_white.png"),
                        false);
                uiFont.getData().setScale(1.9f);  // Slightly larger for readability
            } catch (Exception e) {
                uiFont = new BitmapFont();  // Use default if loading fails
            }

            // Load a dedicated font for the audit time (prefer pixeloid Sans)
            try {
                timeFont = new BitmapFont(
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.fnt"),
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.png"),
                        false);
                Gdx.app.log("GameScreen", "Loaded time font: assets/fonts/pixeloid/Sans.fnt");
            } catch (Exception e) {
                try {
                    timeFont = new BitmapFont(
                            Gdx.files.internal("fonts/pixeloid/Sans.fnt"),
                            Gdx.files.internal("fonts/pixeloid/Sans.png"),
                            false);
                    Gdx.app.log("GameScreen", "Loaded time font from fallback: fonts/pixeloid/Sans.fnt");
                } catch (Exception ex) {
                    timeFont = null; // will fall back to uiFont when drawing
                    Gdx.app.log("GameScreen", "Time font not found; using uiFont as fallback");
                }
            }

            // Load the Pexelify font for overlay stats (documents/time)
            try {
                statFont = new BitmapFont(
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.fnt"),
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.png"),
                        false);
                // Slightly larger so the stats pop on the overlay
                statFont.getData().setScale(STAT_FONT_SCALE);
                Gdx.app.log("GameScreen", "Loaded stat font: assets/fonts/Pexelify_Sans.fnt");
            } catch (Exception e) {
                try {
                    statFont = new BitmapFont(
                            Gdx.files.internal("assets/fonts/pixeloid/Sans.fnt"),
                            Gdx.files.internal("assets/fonts/pixeloid/Sans.png"),
                            false);
                    statFont.getData().setScale(STAT_FONT_SCALE);
                    Gdx.app.log("GameScreen", "Loaded stat font fallback: fonts/Pexelify_Sans.fnt");
                } catch (Exception ex) {
                    statFont = null;
                    Gdx.app.log("GameScreen", "Stat font Pexelify not found; using default font");
                }
            }

            // Load a smaller font for the document counter so it doesn't share the large uiFont scale
            try {
                // Primary: use Pixeloid Sans for document count
                docFont = new BitmapFont(
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.fnt"),
                        Gdx.files.internal("assets/fonts/pixeloid/Sans.png"),
                        false);
                // Make the document count a bit smaller than before
                docFont.getData().setScale(.9f);
            } catch (Exception e) {
                try {
                    // Fallback to alternate relative path
                    docFont = new BitmapFont(
                            Gdx.files.internal("fonts/pixeloid/Sans.fnt"),
                            Gdx.files.internal("fonts/pixeloid/Sans.png"),
                            false);
                    docFont.getData().setScale(.9f);
                } catch (Exception ex) {
                    // Last resort: default font
                    docFont = new BitmapFont();
                    docFont.getData().setScale(0.95f);
                }
            }
            
            // (Removed) timer icon - we now render the audit timer centered using the audit background image
            
            try {
                documentSheetTexture = new Texture(Gdx.files.internal("assets/documents.png"));
                // Split into 2 columns x 3 rows
                int cols = 2, rows = 3;
                int frameW = documentSheetTexture.getWidth() / cols;
                int frameH = documentSheetTexture.getHeight() / rows;
                com.badlogic.gdx.graphics.g2d.TextureRegion[][] tmp = com.badlogic.gdx.graphics.g2d.TextureRegion.split(documentSheetTexture, frameW, frameH);
                documentFrames = new com.badlogic.gdx.graphics.g2d.TextureRegion[cols * rows];
                int idx = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        documentFrames[idx++] = tmp[r][c];
                    }
                }
                Gdx.app.log("GameScreen", "Loaded document spritesheet (2x3): assets/documents.png");
            } catch (Exception e) {
                // fallback to other path
                try {
                    documentSheetTexture = new Texture(Gdx.files.internal("documents.png"));
                    int cols = 2, rows = 3;
                    int frameW = documentSheetTexture.getWidth() / cols;
                    int frameH = documentSheetTexture.getHeight() / rows;
                    com.badlogic.gdx.graphics.g2d.TextureRegion[][] tmp = com.badlogic.gdx.graphics.g2d.TextureRegion.split(documentSheetTexture, frameW, frameH);
                    documentFrames = new com.badlogic.gdx.graphics.g2d.TextureRegion[cols * rows];
                    int idx = 0;
                    for (int r = 0; r < rows; r++) {
                        for (int c = 0; c < cols; c++) {
                            documentFrames[idx++] = tmp[r][c];
                        }
                    }
                    Gdx.app.log("GameScreen", "Loaded document spritesheet fallback: documents.png");
                } catch (Exception ex) {
                    documentSheetTexture = null;
                    documentFrames = null;
                    Gdx.app.log("GameScreen", "Document spritesheet not found");
                }
            }

            // Load audit timer background image (centered above the timer)
            try {
                auditBgTexture = new Texture(Gdx.files.internal("assets/buttons/Audit time.png"));
                Gdx.app.log("GameScreen", "Loaded audit timer background: assets/buttons/Audit time.png");
            } catch (Exception e) {
                try {
                    auditBgTexture = new Texture(Gdx.files.internal("buttons/Audit time.png"));
                    Gdx.app.log("GameScreen", "Loaded audit timer background from fallback: buttons/Audit time.png");
                } catch (Exception ex) {
                    auditBgTexture = null;
                    Gdx.app.log("GameScreen", "Audit timer background not found");
                }
            }

            // Load pause button and overlay
            try {
                pauseButtonTexture = new Texture(Gdx.files.internal("assets/overlay/pause.png"));
            } catch (Exception e) {
                try { pauseButtonTexture = new Texture(Gdx.files.internal("overlay/pause.png")); } catch (Exception ex) { pauseButtonTexture = null; }
            }
            // Hover sound provided centrally via HoverSoundManager on MyGdxGame
            try {
                overlayPauseTex = new Texture(Gdx.files.internal("assets/overlay/Pause overlay.png"));
            } catch (Exception e) {
                try { overlayPauseTex = new Texture(Gdx.files.internal("overlay/Pause overlay.png")); } catch (Exception ex) { overlayPauseTex = null; }
            }

            // Load pause overlay buttons: restart(10), resume(11), menu(12)
            try { btnRestartTex = new Texture(Gdx.files.internal("assets/buttons/10.png")); } catch (Exception e) { try { btnRestartTex = new Texture(Gdx.files.internal("buttons/10.png")); } catch (Exception ex) { btnRestartTex = null; } }
            try { btnResumeTex  = new Texture(Gdx.files.internal("assets/buttons/11.png")); } catch (Exception e) { try { btnResumeTex = new Texture(Gdx.files.internal("buttons/11.png")); } catch (Exception ex) { btnResumeTex = null; } }
            try { btnMenuTex    = new Texture(Gdx.files.internal("assets/buttons/12.png")); } catch (Exception e) { try { btnMenuTex = new Texture(Gdx.files.internal("buttons/12.png")); } catch (Exception ex) { btnMenuTex = null; } }
            
            // Load small clock icon to display beside the time (replaces audit background)
            try {
                clockTexture = new Texture(Gdx.files.internal("assets/overlay/Clock.png"));
            } catch (Exception e) {
                try { clockTexture = new Texture(Gdx.files.internal("overlay/Clock.png")); } catch (Exception ex) { clockTexture = null; }
            }

                // Initialize icon positions (document counter remains top-right)
                // Lower the document icon a bit to improve vertical placement
                docIconY = Gdx.graphics.getHeight() - 120f;
    }

    @Override
    public void render(float delta) {
        // If the win overlay is visible we prefer not to introduce an opaque
        // background color that could show through transparent parts of the
        // overlay. Clear to transparent when showing the level-complete overlay
        // so the already-rendered game appears as the backdrop for the overlay.
        if (showLevelComplete) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        } else {
            Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }

        // If level complete, still render the game underneath and then draw overlay on top
        if (showLevelComplete) {
            // Debug log to help diagnose blank/cleared-overlay issues
            try {
                String bgInfo = (levelManager != null) ? "hasLevelManager" : "noLevelManager";
                boolean hasBg = false;
                try {
                    java.lang.reflect.Field f = LevelManager.class.getDeclaredField("backgroundTex");
                    f.setAccessible(true);
                    Object o = (levelManager != null) ? f.get(levelManager) : null;
                    hasBg = (o != null);
                } catch (Exception ignored) {}
                Gdx.app.log("GameScreen", "showLevelComplete triggered - winOverlayVisible=" + winOverlayVisible + ", overlayWinTex=" + (overlayWinTex!=null) + ", levelBgPresent=" + hasBg + ", currentLevel=" + currentLevel);
            } catch (Exception ignored) {}

            // Do not call update (freeze game state); just render current frame
            renderGame();
            // Draw the win overlay on top of the currently rendered game
            if (winOverlayVisible) drawWinOverlay();
            return;
        }

        // Normal gameplay
        handleInput();
        
        if (currentState == GameState.RUNNING) {
            update(delta);
        }
        
        renderGame();
        // If LevelManager reports completion, ensure the win overlay is shown on top
        try {
            if (levelManager != null && levelManager.isLevelComplete()) {
                if (!showLevelComplete) {
                    Gdx.app.log("GameScreen", "Forcing showLevelComplete=true because LevelManager reports completion");
                    // Briefly suppress the initiating click so it doesn't immediately
                    // activate an overlay button. Use a short timer (120ms) instead
                    // of indefinitely consuming the next click.
                    overlaySuppressNextClick = true;
                    overlaySuppressTimer = 0.12f;
                }
                showLevelComplete = true;
                winOverlayVisible = true;
                // Draw the overlay immediately so it appears over the rendered game
                drawWinOverlay();
                return;
            }
        } catch (Exception ignored) {}
        
        // If game over, draw the GameOver overlay on top
        if (currentState == GameState.GAMEOVER) {
            drawGameOverOverlay();
            return; // don't draw anything else on top
        }
    }

    private void handleInput() {
        boolean pKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.P);

        if (pKeyIsPressed && !pKeyWasPressed && currentState != GameState.GAMEOVER) {
            currentState = (currentState == GameState.RUNNING) ? GameState.PAUSED : GameState.RUNNING;
        }

        pKeyWasPressed = pKeyIsPressed;

        if (currentState == GameState.GAMEOVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            // Force reinitialization so show() will reload the level
            initialized = false;
            show();  // reinit current level
        }

        if (currentState == GameState.GAMEOVER &&
                (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.Q))) {
            if (musicManager != null) {
                musicManager.stopMusic();
            }
            game.setScreen(new LevelSelectScreen(game));
            dispose();
        }
        // If we just transitioned to GAMEOVER record event stats for overlay
        if (currentState == GameState.GAMEOVER && !lastEventRecorded) {
            try {
                lastEventDocs = (levelManager != null) ? levelManager.getDocumentsCollected() : 0;
                lastEventTime = remainingTime;
                try {
                    if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playGameOver();
                } catch (Exception ignored) {}
                try {
                    if (musicManager != null) musicManager.stopMusic();
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
            lastEventRecorded = true;
        }

        // If the win overlay is visible, allow dismissal via click or key (Enter/Space)
            if (showLevelComplete && winOverlayVisible) {
            boolean dismiss = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT);
            if (dismiss) {
                    // If suppression timer is active, consume the input and let
                    // subsequent clicks work normally. Otherwise proceed.
                    if (overlaySuppressNextClick && overlaySuppressTimer > 0f) {
                        // consume the initiating click
                        overlaySuppressNextClick = false;
                        overlaySuppressTimer = 0f;
                    } else {
                        // Dismiss the win overlay and proceed
                        winOverlayVisible = false;
                        showLevelComplete = false;
                        proceedToNextLevel();
                    }
            }
        }
    }

    private void update(float deltaTime) {
        // Update floating animation timer
        floatTimer += deltaTime;
        // Diagnostic logging: report overlay state and tutorial flags when debugging
        try {
            if (levelManager != null) {
                Object cur = null;
                try { java.lang.reflect.Field f = LevelManager.class.getDeclaredField("currentLevel"); f.setAccessible(true); cur = f.get(levelManager); } catch (Exception ignored) {}
                if (cur != null) {
                    String lvlName = cur.getClass().getSimpleName();
                    boolean blocking = false;
                    try { if (cur instanceof com.mygdx.game.Levels.BackgroundedLevel) blocking = ((com.mygdx.game.Levels.BackgroundedLevel)cur).isOverlayBlocking(); } catch (Exception ignored) {}
                    Gdx.app.log("GameScreenDebug", "ActiveLevel=" + lvlName + " overlayFullVisible=" + overlayFullVisible + " overlaySuppressNextClick=" + overlaySuppressNextClick + " blocking=" + blocking + " currentLevelIndex=" + currentLevel);
                    // If it's the tutorial level, try to query its showOverlay flag
                    try {
                        if (cur instanceof com.mygdx.game.LevelTutorial) {
                            com.mygdx.game.LevelTutorial lt = (com.mygdx.game.LevelTutorial) cur;
                            Gdx.app.log("GameScreenDebug", "LevelTutorial.showOverlay=" + lt.isShowOverlay() + " finalOverlayVisible=" + lt.isFinalOverlayVisible() + " finalConsumed=" + lt.isFinalOverlayConsumed());
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // If the active level provides a blocking overlay, do not advance gameplay
        try {
            Object activeLevel = null;
            if (levelManager != null) {
                try { activeLevel = levelManager.getCurrentLevel(); } catch (Exception ignored) {}
            } else if (levelManager2 != null) {
                try {
                    // prefer currentLevelB if present, otherwise A
                    com.mygdx.game.Levels.Level b = levelManager2.getCurrentLevelB();
                    com.mygdx.game.Levels.Level a = levelManager2.getCurrentLevelA();
                    activeLevel = (b != null) ? b : a;
                } catch (Exception ignored) {}
            }
            if (activeLevel instanceof com.mygdx.game.Levels.BackgroundedLevel) {
                com.mygdx.game.Levels.BackgroundedLevel bl = (com.mygdx.game.Levels.BackgroundedLevel) activeLevel;
                if (bl.isOverlayBlocking()) {
                    // Allow the level to process its overlay input/update, but skip
                    // the rest of the gameplay updates (physics, timer, collisions).
                    try {
                        ILevelManager lm = (levelManager != null) ? (ILevelManager) levelManager : (ILevelManager) levelManager2;
                        // Call updateBackground directly so the level can handle clicks/keys
                        bl.updateBackground(deltaTime, lm, null, null, fixer);
                    } catch (Exception ignored) {}
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Player physics FIRST
        if (fixer != null) fixer.update(deltaTime);
        
        // Then collision resolution
        float timePenalty = 0f;
        if (levelManager != null) timePenalty = levelManager.update(deltaTime, fixer);
        else if (levelManager2 != null) timePenalty = levelManager2.update(deltaTime, fixer);
        
        remainingTime -= (deltaTime + timePenalty);
        if (remainingTime <= 0) {
            remainingTime = 0;
            currentState = GameState.GAMEOVER;
        }
        
        // Update UI labels
        if (docsLabel != null) {
            if (levelManager != null) {
                docsLabel.setText("Documents: " + levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments());
            } else if (levelManager2 != null) {
                docsLabel.setText("Documents: " + levelManager2.getDocumentsCollected() + "/" + levelManager2.getTotalDocuments());
            }
        }
        if (timeLabel != null) {
            int minutes = (int) (remainingTime / 60);
            int seconds = (int) (remainingTime % 60);
            timeLabel.setText(String.format("Time: %d:%02d", minutes, seconds));
        }
        
        // Check win condition
        if ((levelManager != null && levelManager.isLevelComplete()) || 
            (levelManager2 != null && levelManager2.isLevelComplete())) {
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
        } else if (levelManager2 != null) {
            levelManager2.render(shapeRenderer, game.batch, game.font);
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
            
            // Draw floating timer and document counter icons in top-right corner
            drawFloatingUI(game.batch);
            
            game.batch.end();
            
            // Draw instructions and dash cooldown indicator
            drawText();
        }

        // If tutorial overlay is active, draw it on top of everything
        if (currentLevel == 0) {
            drawTutorialOverlay();
        }

        // Draw the full-screen overlay if activated by the tutorial OK button
        drawFullOverlayIfActive();
        // Draw the talking overlay if the tutorial level requested it (first doc collected)
        drawTalkingOverlayIfActive();
        // Draw pause overlay if active
        drawPauseOverlayIfActive();
    }

    // Draw the pause overlay and its buttons
    private void drawPauseOverlayIfActive() {
        if (!pauseOverlayVisible) return;
        if (game == null || game.batch == null) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        game.batch.begin();
        if (overlayPauseTex != null) {
            // Draw the overlay full-screen (the asset likely contains the darkened background)
            game.batch.draw(overlayPauseTex, 0, 0, w, h);
        } else {
            // fallback: semi-transparent dark quad handled by ShapeRenderer
        }

        // Button sizes (use texture native size but fit to max while preserving aspect ratio)
        // Increased max to allow larger buttons; upscaling allowed so small assets can be enlarged.
        float btnMaxW = 300f;
        float btnMaxH = 112f;
        // compute per-texture draw widths/heights preserving aspect ratio
        float rW = btnMaxW, rH = btnMaxH, sW = btnMaxW, sH = btnMaxH, mW = btnMaxW, mH = btnMaxH;
        if (btnRestartTex != null) {
            float tw = btnRestartTex.getWidth();
            float th = btnRestartTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            rW = tw * scale;
            rH = th * scale;
        }
        if (btnResumeTex != null) {
            float tw = btnResumeTex.getWidth();
            float th = btnResumeTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            sW = tw * scale;
            sH = th * scale;
        }
        if (btnMenuTex != null) {
            float tw = btnMenuTex.getWidth();
            float th = btnMenuTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            mW = tw * scale;
            mH = th * scale;
        }

        float spacing = 24f;
        float totalW = rW + spacing + sW + spacing + mW;
        float baseX = w * 0.5f - totalW * 0.5f;
        float btnY = h * 0.5f - Math.max(Math.max(rH, sH), mH) * 0.5f - 40f; // slightly above center

        // Base positions (left-to-right)
        float rx = baseX;
        float sx = rx + rW + spacing;
        float mx = sx + sW + spacing;

        // Mouse position in world coords (screen coordinates)
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Determine hover state using base rects (hover detection uses base sizes)
        boolean hoverR = (mouseX >= rx && mouseX <= rx + rW && mouseY >= btnY && mouseY <= btnY + rH);
        boolean hoverS = (mouseX >= sx && mouseX <= sx + sW && mouseY >= btnY && mouseY <= btnY + sH);
        boolean hoverM = (mouseX >= mx && mouseX <= mx + mW && mouseY >= btnY && mouseY <= btnY + mH);
        // Play hover sound once when entering hover state
        playHoverSoundIfHovered("pause_restart", hoverR);
        playHoverSoundIfHovered("pause_resume", hoverS);
        playHoverSoundIfHovered("pause_menu", hoverM);

        // Hover scale factor (tweak to change effect strength)
        float hoverScale = 1.08f; // 8% scale up on hover

        // Compute drawn sizes and positions, centering scaled images on their original centers
        float drawRW = rW * (hoverR ? hoverScale : 1f);
        float drawRH = rH * (hoverR ? hoverScale : 1f);
        float drawRX = rx - (drawRW - rW) * 0.5f;

        float drawSW = sW * (hoverS ? hoverScale : 1f);
        float drawSH = sH * (hoverS ? hoverScale : 1f);
        float drawSX = sx - (drawSW - sW) * 0.5f;

        float drawMW = mW * (hoverM ? hoverScale : 1f);
        float drawMH = mH * (hoverM ? hoverScale : 1f);
        float drawMX = mx - (drawMW - mW) * 0.5f;

        // Draw restart (left)
        if (btnRestartTex != null) game.batch.draw(btnRestartTex, drawRX, btnY - (drawRH - rH) * 0.5f, drawRW, drawRH);
        // Draw resume (middle)
        if (btnResumeTex != null) game.batch.draw(btnResumeTex, drawSX, btnY - (drawSH - sH) * 0.5f, drawSW, drawSH);
        // Draw menu (right)
        if (btnMenuTex != null) game.batch.draw(btnMenuTex, drawMX, btnY - (drawMH - mH) * 0.5f, drawMW, drawMH);

        game.batch.end();

        // Handle clicks on overlay buttons. Use a short suppression timer so the
        // click that opened the overlay doesn't accidentally activate a button,
        // but clicks that target buttons still work immediately.
        if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
            float mxIn = Gdx.input.getX();
            float myIn = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (overlaySuppressNextClick && overlaySuppressTimer > 0f) {
                boolean insideRestart = (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH);
                boolean insideResume  = (mxIn >= drawSX && mxIn <= drawSX + drawSW && myIn >= btnY - (drawSH - sH) * 0.5f && myIn <= btnY - (drawSH - sH) * 0.5f + drawSH);
                boolean insideMenu    = (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH);
                // clear suppression state so next inputs are normal
                overlaySuppressNextClick = false;
                overlaySuppressTimer = 0f;
                if (!insideRestart && !insideResume && !insideMenu) {
                    // Click didn't target any button — consume it.
                    return;
                }
                // Otherwise fall through and handle the click below.
            }

            // Restart (use drawn rect)
            if (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH) {
                // restart current level
                pauseOverlayVisible = false;
                currentState = GameState.RUNNING;
                // Stop the current music completely before restarting
                BackgroundMusicManager.getInstance().stopMusic();
                // ensure show() actually reinitializes
                initialized = false;
                show();
                return;
            }
            // Resume
            if (mxIn >= drawSX && mxIn <= drawSX + drawSW && myIn >= btnY - (drawSH - sH) * 0.5f && myIn <= btnY - (drawSH - sH) * 0.5f + drawSH) {
                pauseOverlayVisible = false;
                currentState = GameState.RUNNING;
                // Resume the music
                BackgroundMusicManager.getInstance().resumeMusic();
                return;
            }
            // Menu
            if (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH) {
                try {
                    // Stop the level music when going back to menu
                    BackgroundMusicManager.getInstance().stopMusic();
                    game.setScreen(new LevelSelectScreen(game));
                    dispose();
                } catch (Exception ignored) {}
                return;
            }
        }
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

        // The property-files "light" overlay was removed ΓÇö the LevelTutorial
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
        // Allow keyboard dismissal (Enter/Space) as well as mouse click on OK
        if (lt != null && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            try {
                lt.setShowOverlay(false);
                overlayFullVisible = true;
                overlaySuppressNextClick = true;
            } catch (Exception ignored) {}
        }
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

    /**
     * Draws floating timer and document counter in the top-right corner
     */
    private void drawFloatingUI(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        if (batch == null || uiFont == null) return;
        
        // No floating motion: keep icons and audit timer stationary
        float floatOffset = 0f;
        
        int screenWidth = Gdx.graphics.getWidth();
        int iconSize = 90;
        int rightMargin = 20; // kept for potential future use
        
        // Timer (centered at top) and document counter (top-right)
        float centerX = screenWidth * 0.5f;
        float floatOffsetY = floatOffset;

        // Draw audit timer background and centered timer text
        int minutes = (int) (remainingTime / 60);
        int seconds = (int) (remainingTime % 60);
        String timeText = String.format("%d:%02d", minutes, seconds);
        // Time drawing will be positioned beside the document icon/text on the left side.
        // (Actual drawing occurs after document text is measured and drawn below.)

        // Draw pause button at top-right with hover-scale effect
        int pauseSize = 80;
        float pauseX = screenWidth - pauseSize - 40f;
        float pauseY = Gdx.graphics.getHeight() - pauseSize - 20f;

        // Mouse coordinates (screen space, with Y flipped)
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Hover detection on base rect
        boolean pauseHover = (mouseX >= pauseX && mouseX <= pauseX + pauseSize && mouseY >= pauseY && mouseY <= pauseY + pauseSize);
        // Play hover sound on top-right pause button
        playHoverSoundIfHovered("pause_top", pauseHover);
        float pauseHoverScale = 1.08f; // how much to scale on hover
        float pauseScale = pauseHover ? pauseHoverScale : 1f;
        float pauseDrawSize = pauseSize * pauseScale;
        // Center scaled image around original center
        float pauseDrawX = pauseX - (pauseDrawSize - pauseSize) * 0.5f;
        float pauseDrawY = pauseY - (pauseDrawSize - pauseSize) * 0.5f;

        if (pauseButtonTexture != null) {
            batch.draw(pauseButtonTexture, pauseDrawX, pauseDrawY, pauseDrawSize, pauseDrawSize);
        }

        // Handle pause button click using drawn rect (so hitbox matches visual)
        if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
            float mx = mouseX;
            float my = mouseY;
            if (mx >= pauseDrawX && mx <= pauseDrawX + pauseDrawSize && my >= pauseDrawY && my <= pauseDrawY + pauseDrawSize) {
                if (overlaySuppressNextClick) {
                    overlaySuppressNextClick = false;
                } else {
                    pauseOverlayVisible = true;
                    currentState = GameState.PAUSED;
                    // Pause the background music
                    BackgroundMusicManager.getInstance().pauseMusic();
                    overlaySuppressNextClick = true; // ignore the click that opened overlay
                }
            }
        }

        // Document counter icon and text (left side)
        float docX = 10f; // left margin
        float currentDocY = docIconY + floatOffset;
        // Keep the document count text at the original visual baseline (was at height-110f)
        // so lowering the icon does not move the text.
        float docTextBaselineY = Gdx.graphics.getHeight() - 100f + floatOffset;
        // Display only the specific frame: column 2, row 3 (1-based).
        // With a 2x3 sheet, this corresponds to zero-based r=2, c=1 -> index = 2*2 + 1 = 5.
        if (documentFrames != null && documentFrames.length > 5) {
            com.badlogic.gdx.graphics.g2d.TextureRegion frame = documentFrames[5];
            batch.draw(frame, docX, currentDocY, iconSize, iconSize);
        } else if (documentSheetTexture != null) {
            // fallback: draw whole texture scaled
            batch.draw(documentSheetTexture, docX, currentDocY, iconSize, iconSize);
        }

        // Document count text next to icon (to the right of the icon)
        String docText = "";
        com.badlogic.gdx.graphics.g2d.GlyphLayout glDoc = null;
        if (levelManager != null) {
            docText = levelManager.getDocumentsCollected() + "/" + levelManager.getTotalDocuments();
        } else if (levelManager2 != null) {
            docText = levelManager2.getDocumentsCollected() + "/" + levelManager2.getTotalDocuments();
        }
        if (!docText.isEmpty()) {
            // Draw the text at the fixed baseline so it does not move when the icon Y changes
            float textY = docTextBaselineY + iconSize / 2 + 8;
            if (docFont != null) {
                glDoc = new com.badlogic.gdx.graphics.g2d.GlyphLayout(docFont, docText);
                docFont.draw(batch, docText, docX + iconSize + -5, textY);
            } else {
                glDoc = new com.badlogic.gdx.graphics.g2d.GlyphLayout(uiFont, docText);
                uiFont.draw(batch, docText, docX + iconSize + -5, textY);
            }
        }

        // Now draw the time (clock + text) to the right of the document text
        // Position it with a small gap after the doc text
        float gapAfterDoc = 12f;
        float timeStartX = docX + iconSize + 4 + (glDoc != null ? glDoc.width : 0f) + gapAfterDoc;
        // Time baseline should match the doc text baseline
        float timeBaselineY = docTextBaselineY + iconSize / 2 + AUDIT_TIME_VERTICAL_OFFSET;

        // Prepare time glyph (use dedicated timeFont if available, otherwise fall back to uiFont)
        com.badlogic.gdx.graphics.g2d.BitmapFont timeFontToUse = (timeFont != null) ? timeFont : uiFont;
        com.badlogic.gdx.graphics.g2d.GlyphLayout glTime = new com.badlogic.gdx.graphics.g2d.GlyphLayout(timeFontToUse, timeText);
        float padding = 9f;
        // Determine desired clock height to match font height (so the icon aligns with text)
        float desiredClockH = glTime.height * 2.5f; // increase to ~60% larger than font height
        float clockW = 0f, clockH = 0f;
        if (clockTexture != null) {
            float texW = clockTexture.getWidth();
            float texH = clockTexture.getHeight();
            float scale = desiredClockH / texH;
            clockW = texW * scale;
            clockH = desiredClockH;
        }

        // Draw clock icon (left) then time text
        if (clockTexture != null && clockW > 0f) {
            float clockX = timeStartX;
            // Lower the clock icon slightly so it visually lines up better with the text
            float clockYOffset = -8f; // negative moves the icon down
            float clockY = timeBaselineY - clockH * 0.5f + clockYOffset;
            batch.draw(clockTexture, clockX, clockY, clockW, clockH);
        }

        float timeTextX = timeStartX + (clockW > 0f ? clockW + padding : 0f);
        timeFontToUse.draw(batch, glTime, timeTextX, timeBaselineY);
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

        // Dismiss on ESC, Enter, Space, or mouse click, but ignore the click that opened the overlay
        try {
            // If a tutorial Level is active, obtain it so we can clear its overlay flag when dismissing
            LevelTutorial lt = null;
            if (currentLevel == 0 && levelManager != null) {
                try {
                    java.lang.reflect.Field f = LevelManager.class.getDeclaredField("currentLevel");
                    f.setAccessible(true);
                    Object cur = f.get(levelManager);
                    if (cur instanceof LevelTutorial) lt = (LevelTutorial) cur;
                } catch (Exception ignored) {}
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                overlayFullVisible = false;
                overlaySuppressNextClick = false;
                if (lt != null) { try { lt.setShowOverlay(false); } catch (Exception ignored) {} }
            } else if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                if (overlaySuppressNextClick) {
                    // consume this click (it was the OK click that opened the overlay)
                    overlaySuppressNextClick = false;
                } else {
                    overlayFullVisible = false;
                    if (lt != null) { try { lt.setShowOverlay(false); } catch (Exception ignored) {} }
                }
            }
        } catch (Exception ignored) {}
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
                    playHoverSoundIfHovered("talking_continue", hovered);

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
                                            // Try to invoke addDocumentsAtPositions via reflection so this code
                                            // compiles even if LevelManager doesn't declare that method.
                                            try {
                                                java.lang.reflect.Method m = LevelManager.class.getMethod("addDocumentsAtPositions", float[][].class);
                                                m.invoke(levelManager, (Object) LevelTutorial.TUTORIAL_DOC_POSITIONS);
                                            } catch (NoSuchMethodException nsme) {
                                                // Method not present: fallback to adding documents without explicit positions
                                                levelManager.addDocuments(LevelTutorial.TUTORIAL_DOC_POSITIONS.length);
                                            } catch (Exception ex) {
                                                // Any other reflection error: fallback to adding documents
                                                try { levelManager.addDocuments(LevelTutorial.TUTORIAL_DOC_POSITIONS.length); } catch (Exception ignored2) {}
                                            }
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
        // Instructions removed - no longer displayed
        game.batch.end();
        
        // Draw dash cooldown bar beside the clock at top-right
        drawDashCooldownIndicator();
    }
    
    /**
     * Draw dash cooldown indicator bar beside the clock at top-right
     */
    private void drawDashCooldownIndicator() {
        if (fixer == null || shapeRenderer == null) {
            return;
        }
        
        float cooldown = fixer.getDashCooldown();
        float maxCooldown = 10.0f;  // Match DASH_COOLDOWN from Fixer (10 seconds)
        float barWidth = 120f;
        float barHeight = 16f;
        
        // Position to the right of the clock/timer area at top-right
        // The clock/timer is positioned at the right, so position the dash indicator below it
        float barX = Gdx.graphics.getWidth() - barWidth - 800f;
        float barY = Gdx.graphics.getHeight() - 60f;  // Below the clock/timer
        
        // Draw dash effect glow if currently dashing
        if (fixer.isDashing()) {
            shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.2f, 1f, 1f, 0.3f);  // Cyan glow during dash
            shapeRenderer.rect(barX - 4, barY - 4, barWidth + 8, barHeight + 8);
            shapeRenderer.end();
        }
        
        // Draw cooldown bar background
        shapeRenderer.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);  // Dark gray background
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        
        // Draw cooldown bar fill
        if (cooldown > 0f) {
            float fillWidth = barWidth * (1f - (cooldown / maxCooldown));
            shapeRenderer.setColor(1f, 0.4f, 0f, 1f);  // Orange/red for cooldown
            shapeRenderer.rect(barX, barY, fillWidth, barHeight);
        } else {
            shapeRenderer.setColor(0f, 1f, 0f, 1f);  // Green for ready
            shapeRenderer.rect(barX, barY, barWidth, barHeight);
        }
        
        // White border removed - only draw the filled bar
        shapeRenderer.end();
        
        // Draw status text
        game.batch.begin();
        if (cooldown > 0f) {
            // Show remaining cooldown time while cooling down
            String cooldownText = String.format("%.1fs", cooldown);
            float textX = barX + barWidth / 2 - 10f;
            float textY = barY + barHeight + 5f;
            BitmapFont fontToUse = (uiFont != null) ? uiFont : game.font;
            if (fontToUse != null) {
                fontToUse.draw(game.batch, cooldownText, textX, textY);
            }
        } else {
            // Show "READY" when dash is available
            float textX = barX + barWidth / 2 - 15f;
            float textY = barY + barHeight + 5f;
            BitmapFont fontToUse = (uiFont != null) ? uiFont : game.font;
            if (fontToUse != null) {
                fontToUse.draw(game.batch, "READY", textX, textY);
            }
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

        // Decrement suppression timer so the overlay only blocks the initiating
        // click for a short moment. Use delta from graphics frame time.
        if (overlaySuppressTimer > 0f) {
            overlaySuppressTimer -= Gdx.graphics.getDeltaTime();
            if (overlaySuppressTimer <= 0f) overlaySuppressNextClick = false;
        }

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        // Compute button and stat layout similar to win overlay but only two buttons: Restart and Menu
        // Draw background (texture if available, otherwise dark quad)
        game.batch.begin();
        if (overlayGameOverTex != null) {
            game.batch.draw(overlayGameOverTex, 0, 0, w, h);
        }
        game.batch.end();

        if (overlayGameOverTex == null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
            shapeRenderer.rect(0, 0, w, h);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Draw stats centered (documents / time) using same layout as win overlay
        String docsStr = "     " + lastEventDocs + "/" + (levelManager != null ? levelManager.getTotalDocuments() : 0);
        int minutes = (int) (lastEventTime / 60);
        int seconds = (int) (lastEventTime % 60);
        String timeStr = String.format("     %d:%02d", minutes, seconds);
        com.badlogic.gdx.graphics.g2d.BitmapFont fontToUse = (statFont != null) ? statFont : game.font;
        try {
            com.badlogic.gdx.graphics.g2d.GlyphLayout g1 = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontToUse, docsStr);
            com.badlogic.gdx.graphics.g2d.GlyphLayout g2 = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontToUse, timeStr);
            float cx = w * 0.5f;
            float baseY = h * 0.5f - 40f;
            float gap = STAT_LINE_GAP;
            float g1Y = baseY + gap * 0.5f + g1.height;
            float g2Y = baseY - gap * 0.5f + g2.height;
            game.batch.begin();
            fontToUse.draw(game.batch, g1, cx - g1.width * 0.5f, g1Y);
            fontToUse.draw(game.batch, g2, cx - g2.width * 0.5f, g2Y);
            game.batch.end();
        } catch (Exception ignored) {}

        // Buttons: Restart (left) and Menu (right)
        float btnMaxW = 300f;
        float btnMaxH = 112f;
        float rW = btnMaxW, rH = btnMaxH, mW = btnMaxW, mH = btnMaxH;
        if (btnRestartTex != null) {
            float tw = btnRestartTex.getWidth();
            float th = btnRestartTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            rW = tw * scale; rH = th * scale;
        }
        if (btnMenuTex != null) {
            float tw = btnMenuTex.getWidth();
            float th = btnMenuTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            mW = tw * scale; mH = th * scale;
        }

        rW *= WIN_RETRY_SCALE; rH *= WIN_RETRY_SCALE;
        mW *= WIN_MENU_SCALE;  mH *= WIN_MENU_SCALE;

        float spacing = WIN_BUTTON_SPACING;
        float totalW = rW + spacing + mW;
        float baseX = w * 0.5f - totalW * 0.5f;
        float btnY = Math.max(h * 0.22f, 120f) - 60f;
        float rx = baseX;
        float mx = rx + rW + spacing;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        boolean hoverR = (mouseX >= rx && mouseX <= rx + rW && mouseY >= btnY && mouseY <= btnY + rH);
        boolean hoverM = (mouseX >= mx && mouseX <= mx + mW && mouseY >= btnY && mouseY <= btnY + mH);

        // Play hover sounds when entering GameOver overlay buttons
        playHoverSoundIfHovered("gameover_restart", hoverR);
        playHoverSoundIfHovered("gameover_menu", hoverM);

        float hoverScale = 1.08f;

        float drawRW = rW * (hoverR ? hoverScale : 1f);
        float drawRH = rH * (hoverR ? hoverScale : 1f);
        float drawRX = rx - (drawRW - rW) * 0.5f;

        float drawMW = mW * (hoverM ? hoverScale : 1f);
        float drawMH = mH * (hoverM ? hoverScale : 1f);
        float drawMX = mx - (drawMW - mW) * 0.5f;

        // Draw buttons
        game.batch.begin();
        if (btnRestartTex != null) game.batch.draw(btnRestartTex, drawRX, btnY - (drawRH - rH) * 0.5f, drawRW, drawRH);
        if (btnMenuTex != null)    game.batch.draw(btnMenuTex, drawMX, btnY - (drawMH - mH) * 0.5f, drawMW, drawMH);
        game.batch.end();

        // Handle clicks on the buttons AFTER ending the batch
        try {
            if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                float mxIn = Gdx.input.getX();
                float myIn = Gdx.graphics.getHeight() - Gdx.input.getY();
                if (overlaySuppressNextClick) {
                    boolean insideRestart = (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH);
                    boolean insideMenu    = (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH);
                    overlaySuppressNextClick = false;
                    if (!insideRestart && !insideMenu) {
                        return;
                    }
                    // fall-through to process click
                }

                if (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH) {
                    // retry current level
                    currentState = GameState.RUNNING;
                    // ensure show() actually reinitializes
                    initialized = false;
                    show();
                    return;
                }
                if (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH) {
                    try {
                        if (musicManager != null) {
                            musicManager.stopMusic();
                        }
                        game.setScreen(new LevelSelectScreen(game));
                        dispose();
                    } catch (Exception ignored) {}
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Draw the Win overlay on top of the already-rendered game (DO NOT clear the screen).
     */
    private void drawWinOverlay() {
        if (game == null || game.batch == null) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        // Decrement suppression timer so the overlay only blocks the initiating
        // click for a short moment.
        if (overlaySuppressTimer > 0f) {
            overlaySuppressTimer -= Gdx.graphics.getDeltaTime();
            if (overlaySuppressTimer <= 0f) overlaySuppressNextClick = false;
        }
        // Precompute button geometry and hover state so drawing and clicks use
        // identical coordinates. We'll draw first, end the batch, then handle
        // input to avoid early returns leaving the SpriteBatch in a begun state.
        float btnMaxW = 300f;
        float btnMaxH = 112f;
        float rW = btnMaxW, rH = btnMaxH, sW = btnMaxW, sH = btnMaxH, mW = btnMaxW, mH = btnMaxH;
        if (btnRestartTex != null) {
            float tw = btnRestartTex.getWidth();
            float th = btnRestartTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            rW = tw * scale; rH = th * scale;
        }
        if (btnResumeTex != null) {
            float tw = btnResumeTex.getWidth();
            float th = btnResumeTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            sW = tw * scale; sH = th * scale;
        }
        if (btnMenuTex != null) {
            float tw = btnMenuTex.getWidth();
            float th = btnMenuTex.getHeight();
            float scale = Math.min(btnMaxW / tw, btnMaxH / th);
            mW = tw * scale; mH = th * scale;
        }

        rW *= WIN_RETRY_SCALE; rH *= WIN_RETRY_SCALE;
        mW *= WIN_MENU_SCALE;  mH *= WIN_MENU_SCALE;

        float spacing = WIN_BUTTON_SPACING;
        float totalW = rW + spacing + sW + spacing + mW;
        float baseX = w * 0.5f - totalW * 0.5f;
        float btnY = Math.max(h * 0.22f, 120f) - 60f;

        float rx = baseX;
        float sx = rx + rW + spacing;
        float mx = sx + sW + spacing;

        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

        boolean hoverR = (mouseX >= rx && mouseX <= rx + rW && mouseY >= btnY && mouseY <= btnY + rH);
        boolean hoverS = (mouseX >= sx && mouseX <= sx + sW && mouseY >= btnY && mouseY <= btnY + sH);
        boolean hoverM = (mouseX >= mx && mouseX <= mx + mW && mouseY >= btnY && mouseY <= btnY + mH);
        playHoverSoundIfHovered("win_restart", hoverR);
        playHoverSoundIfHovered("win_resume", hoverS);
        playHoverSoundIfHovered("win_menu", hoverM);

        float hoverScale = 1.08f;

        float drawRW = rW * (hoverR ? hoverScale : 1f);
        float drawRH = rH * (hoverR ? hoverScale : 1f);
        float drawRX = rx - (drawRW - rW) * 0.5f;

        float drawSW = sW * (hoverS ? hoverScale : 1f);
        float drawSH = sH * (hoverS ? hoverScale : 1f);
        float drawSX = sx - (drawSW - sW) * 0.5f;

        float drawMW = mW * (hoverM ? hoverScale : 1f);
        float drawMH = mH * (hoverM ? hoverScale : 1f);
        float drawMX = mx - (drawMW - mW) * 0.5f;

        // Draw everything
        game.batch.begin();
        if (overlayWinTex != null) {
            game.batch.draw(overlayWinTex, 0, 0, w, h);
        }

        // Draw stats: documents collected and time at win
        try {
            String docs = "     "+lastEventDocs + "/" + (levelManager != null ? levelManager.getTotalDocuments() : 0);
            int minutes = (int) (lastEventTime / 60);
            int seconds = (int) (lastEventTime % 60);
            String time = String.format("     %d:%02d", minutes, seconds);

            com.badlogic.gdx.graphics.g2d.BitmapFont fontToUse = (statFont != null) ? statFont : game.font;
            com.badlogic.gdx.graphics.g2d.GlyphLayout g1 = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontToUse, docs);
            com.badlogic.gdx.graphics.g2d.GlyphLayout g2 = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontToUse, time);
            float cx = w * 0.5f;
            float baseY = h * 0.5f - 40f;
            float gap = STAT_LINE_GAP;
            float g1Y = baseY + gap * 0.5f + g1.height;
            float g2Y = baseY - gap * 0.5f + g2.height;
            fontToUse.draw(game.batch, g1, cx - g1.width * 0.5f, g1Y);
            fontToUse.draw(game.batch, g2, cx - g2.width * 0.5f, g2Y);
        } catch (Exception ignored) {}

        // Draw hint
        try {
            com.badlogic.gdx.graphics.g2d.GlyphLayout hintGL = new com.badlogic.gdx.graphics.g2d.GlyphLayout(game.font, "Click or press Enter/Space to continue");
            float hx = w * 0.5f - hintGL.width * 0.5f;
            float hy = 48f + hintGL.height;
            game.font.draw(game.batch, hintGL, hx, hy);
        } catch (Exception ignored) {}

        if (btnRestartTex != null) game.batch.draw(btnRestartTex, drawRX, btnY - (drawRH - rH) * 0.5f, drawRW, drawRH);
        if (btnResumeTex != null)  game.batch.draw(btnResumeTex, drawSX, btnY - (drawSH - sH) * 0.5f, drawSW, drawSH);
        if (btnMenuTex != null)    game.batch.draw(btnMenuTex, drawMX, btnY - (drawMH - mH) * 0.5f, drawMW, drawMH);

        game.batch.end();

        // Handle clicks on the buttons AFTER ending the batch so we never return
        // while a begin() is active (which would break the SpriteBatch state).
        try {
            if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                float mxIn = Gdx.input.getX();
                float myIn = Gdx.graphics.getHeight() - Gdx.input.getY();
                // If suppression is active, allow the click to pass through if it
                // actually targets one of the overlay buttons. Otherwise consume
                // the click (it likely opened the overlay) and ignore it.
                if (overlaySuppressNextClick) {
                    boolean insideRestart = (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH);
                    boolean insideResume  = (mxIn >= drawSX && mxIn <= drawSX + drawSW && myIn >= btnY - (drawSH - sH) * 0.5f && myIn <= btnY - (drawSH - sH) * 0.5f + drawSH);
                    boolean insideMenu    = (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH);
                    overlaySuppressNextClick = false;
                    if (!insideRestart && !insideResume && !insideMenu) {
                        // Click didn't target any button ΓÇö consume it.
                        return;
                    }
                    // Otherwise fall-through and handle the button click normally.
                }

                if (mxIn >= drawRX && mxIn <= drawRX + drawRW && myIn >= btnY - (drawRH - rH) * 0.5f && myIn <= btnY - (drawRH - rH) * 0.5f + drawRH) {
                    winOverlayVisible = false; showLevelComplete = false; currentState = GameState.RUNNING; initialized = false; show(); return;
                }
                if (mxIn >= drawSX && mxIn <= drawSX + drawSW && myIn >= btnY - (drawSH - sH) * 0.5f && myIn <= btnY - (drawSH - sH) * 0.5f + drawSH) {
                    winOverlayVisible = false; showLevelComplete = false; proceedToNextLevel(); return;
                }
                if (mxIn >= drawMX && mxIn <= drawMX + drawMW && myIn >= btnY - (drawMH - mH) * 0.5f && myIn <= btnY - (drawMH - mH) * 0.5f + drawMH) {
                    try {
                        if (musicManager != null) {
                            musicManager.stopMusic();
                        }
                        game.setScreen(new LevelSelectScreen(game));
                        dispose();
                    } catch (Exception ignored) {}
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private void levelComplete() {
        showLevelComplete = true;
        levelCompleteTimer = 0f;
        // Play win sound and stop music after documents are shredded
        try {
            if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playWin();
        } catch (Exception ignored) {}
        BackgroundMusicManager.getInstance().stopMusic();
        // record event stats for the win overlay
        try {
            lastEventDocs = (levelManager != null) ? levelManager.getDocumentsCollected() : 0;
            lastEventTime = remainingTime;
        } catch (Exception ignored) {}
        lastEventRecorded = true;
        // Show the blocking win overlay and suppress the click that triggered completion
        winOverlayVisible = true;
        overlaySuppressNextClick = true;
        Gdx.app.log("GameScreen", "Level " + currentLevel + " Complete!");
    }

    private void renderLevelCompleteScreen() {
        // Render the current game frame underneath, then draw the win overlay on top.
        // This ensures the overlay appears over the level itself instead of on a cleared/new screen.
        try {
            renderGame();
            if (winOverlayVisible) {
                drawWinOverlay();
            } else if (currentState == GameState.GAMEOVER) {
                drawGameOverOverlay();
            }
        } catch (Exception ignored) {}
    }

    private void proceedToNextLevel() {
        if (currentLevel >= MAX_LEVEL) {
            // All levels completed
            Gdx.app.log("GameScreen", "All levels completed!");
            BackgroundMusicManager.getInstance().playScreenMusic();
            game.setScreen(new MainMenuScreen(game));
            dispose();
        } else {
            // Load next level
            currentLevel++;
            initialized = false;  // Allow show() to reinitialize for the new level
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

    @Override public void pause() {
        Gdx.app.log("GameScreen", "pause() called");
        // store state so we can restore exact position/velocity on resume
        if (fixer != null) {
            Rectangle b = fixer.getBounds();
            savedX = b.x;
            savedY = b.y;
            try {
                com.badlogic.gdx.math.Vector2 vel = fixer.getVelocity();
                if (vel != null) {
                    savedVelX = vel.x;
                    savedVelY = vel.y;
                }
            } catch (Exception ignored) {}
        }
        // stop updating while paused
        currentState = GameState.PAUSED;
        // clear accumulated time so resume won't apply a large physics step
        accumulator = 0f;
        // prevent input while paused
        try { Gdx.input.setInputProcessor(null); } catch (Exception ignored) {}
        // mark that we've been paused so show()/init logic knows to avoid resets
        wasPaused = true;
    }

    @Override
    public void resume() {
        Gdx.app.log("GameScreen", "resume() called");
        // avoid a huge dt on next frame
        accumulator = 0f;
        // restore exact saved position/velocity so character remains where the player left it
        if (fixer != null && !Float.isNaN(savedX)) {
            try {
                fixer.getBounds().setPosition(savedX, savedY);
                com.badlogic.gdx.math.Vector2 vel = fixer.getVelocity();
                if (vel != null) vel.set(savedVelX, savedVelY);
            } catch (Exception ignored) {}
        }
        currentState = GameState.RUNNING;
        // we resume, clear the paused marker so future show() inits behave normally
        wasPaused = false;
         // leave input processor null so UI won't accidentally receive input on immediate restore.
         // If you want input restored immediately, re-set the processor here.
    }
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (levelManager != null) levelManager.dispose();
        if (musicManager != null) musicManager.dispose();
        if (levelManager2 != null) levelManager2.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
        if (fixer != null) fixer.dispose();
        if (overlayArrowTex != null) overlayArrowTex.dispose();
        if (overlayFullTex != null) overlayFullTex.dispose();
        if (overlayTalkingTex != null) overlayTalkingTex.dispose();
        if (overlayTalkingFont != null) overlayTalkingFont.dispose();
        if (auditBgTexture != null) auditBgTexture.dispose();
        if (clockTexture != null) clockTexture.dispose();
        if (documentSheetTexture != null) documentSheetTexture.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (overlayPauseTex != null) overlayPauseTex.dispose();
        if (btnRestartTex != null) btnRestartTex.dispose();
        if (btnResumeTex != null) btnResumeTex.dispose();
        if (btnMenuTex != null) btnMenuTex.dispose();
        if (docFont != null) docFont.dispose();
        if (timeFont != null) timeFont.dispose();
        if (statFont != null) statFont.dispose();
        // hoverSound is managed centrally by HoverSoundManager on MyGdxGame
    }

    // Play hover sound once when entering hover state for a named UI element
    private void playHoverSoundIfHovered(String key, boolean hovering) {
        try {
            Boolean prev = hoverPrev.get(key);
            if (prev == null) prev = Boolean.FALSE;
            if (hovering && !prev) {
                try {
                    if (game != null && game.getHoverSoundManager() != null) game.getHoverSoundManager().playHover();
                } catch (Exception ignored) {}
            }
            hoverPrev.put(key, hovering);
        } catch (Exception ignored) {}
    }
}
