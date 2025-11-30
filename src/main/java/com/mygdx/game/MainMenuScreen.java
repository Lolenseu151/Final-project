package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Main Menu Screen - Shows title and menu options
 */
public class MainMenuScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
        private BitmapFont titleFont;
        private BitmapFont buttonFont;
        private Texture titleFontTexture;
        // Fixed title font size for pixel look
        private String foundTtfPath = null;
        private final int titleFontSize = 64;
    // Layout constants (change these to edit spacing / sizes)
    private final float MENU_TOP_OFFSET = -100f; // centerY offset for the top entry
    private final float MENU_SPACING = 85f;    // center-to-center vertical spacing
    private final float BOX_W = 200f;
    private final float BOX_H = 65f;
    private final float BOX_RADIUS = 20f;
    // Button text colors (change these to set button text colors)
    private final Color BUTTON_TEXT_COLOR = new Color(0f, 0f, 0f, 1f); // unselected (black)
    private final Color BUTTON_TEXT_COLOR_SELECTED = new Color(0f, 0f, 0f, 1f); // selected (white)
    // Running character animation (assets/Run.png or user-provided sheet)
    private Texture runTexture;
    private Animation<TextureRegion> runAnimation;
    private float runAnimTime = 0f;
    // number of columns in the loaded run sprite sheet (detected at runtime)
    private int runColumns = 8;

    // NEW: cat runner
    private Texture catTexture;
    private Animation<TextureRegion> catAnimation;
    private int catColumns = 4;                // Catrun.png uses 4 frames
    private float catScaleMultiplier = 1.75f;  // make cat smaller than main character (tweakable)
    private float catX = Float.NaN;            // current x for cat (init on first draw)
    private float catYOffset = -130f;            // align cat baseline with runner (adjust if needed)
    // NEW: main menu background texture
    private Texture backgroundTex;
    // NEW: horizontal runner state — moves left->right, then resets after a delay
    private float runX = Float.NaN;            // current x position (initialised on first draw)
    private float runSpeed = 260f;             // pixels per second
    private float runRestartDelay = 0.9f;      // seconds to wait after reaching end before restarting
    private float runPauseTimer = 0f;          // countdown when paused
    private boolean runPaused = false;         // true while waiting to restart
    // Menu animation tuning (slower motion for main menu)
    private final float MENU_RUN_FRAME_DURATION = 0.16f; // longer frame -> slower animation
    private final float MENU_RUN_SPEED_FACTOR = 0.6f;    // scale applied to delta when advancing time
    // Running sprite scale and vertical placement/bounce
    private final float RUN_SCALE = 0.9f;            // scale factor for the running sprite
    private final float RUN_Y_OFFSET = -40f;         // base offset from centerY for sprite center (moved up)
    private final float RUN_BOUNCE_AMPLITUDE = 3f;   // bounce amplitude in pixels (reduced)
    private final float RUN_BOUNCE_SPEED = 3f;       // bounce speed multiplier (reduced)
    // Run sizing modes
    private final int RUN_SIZE_MODE_UNIFORM = 0;     // use RUN_SCALE
    private final int RUN_SIZE_MODE_EXPLICIT = 1;    // use explicit pixel dimensions
    private final int RUN_SIZE_MODE_DESIRED_H = 2;   // use desired height, preserve aspect
    private final int RUN_SIZE_MODE_SCREEN_REL = 3;  // size relative to screen height
    // Choose default sizing mode here
    private final int RUN_SIZE_MODE = RUN_SIZE_MODE_SCREEN_REL;
    // explicit pixel size (used when RUN_SIZE_MODE_EXPLICIT)
    private final float RUN_DRAW_WIDTH = 220f;
    private final float RUN_DRAW_HEIGHT = 220f;
    // desired height in pixels (used when RUN_SIZE_MODE_DESIRED_H)
    private final float RUN_DESIRED_HEIGHT = 96f;
    // screen-relative height (fraction of screen height) used when RUN_SIZE_MODE_SCREEN_REL
    private final float RUN_SCREEN_HEIGHT_RATIO = 0.23f; // reduced so character is smaller on the menu
    // Button font scale (1.0 = normal). Set to 0.9 as requested.
    private final float BUTTON_FONT_SCALE = 0.7f;
    
    private enum MenuOption {
        START_GAME,
        TUTORIAL,
        SETTINGS
    }
    
    private MenuOption selectedOption = MenuOption.START_GAME;
    private boolean upKeyWasPressed = false;
    private boolean downKeyWasPressed = false;
    // Track which option the mouse is currently hovering over (visual only)
    private MenuOption hoveredOption = null;
    
    public MainMenuScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void show() {
        // Prefer a provided TTF (pixel font) and generate a BitmapFont at runtime using FreeType
        // Build a list of candidate TTF paths to try (variable font, explicit regular, and any in static/)
        List<String> candidates = new ArrayList<>();
        candidates.add("fonts/Pixelify_Sans/PixelifySans-VariableFont_wght.ttf");
        candidates.add("fonts/Pixelify_Sans/static/PixelifySans-Regular.ttf");

        // Add any .ttf files found under fonts/Pixelify_Sans/stati c
        FileHandle staticDir = Gdx.files.internal("fonts/Pixelify_Sans/static");
        if (staticDir.exists() && staticDir.isDirectory()) {
            for (FileHandle fh : staticDir.list()) {
                if (fh.extension() != null && fh.extension().equalsIgnoreCase("ttf")) {
                    String p = fh.path();
                    if (!candidates.contains(p)) candidates.add(p);
                }
            }
        }

        boolean generated = false;

        // PRIORITY: try the Gradient Pexilify bitmap font first so it overrides other fallbacks
        try {
            String gradFnt = "fonts/Gradient/Gradient pexilify.fnt";
            String gradPng = "fonts/Gradient/Gradient pexilify.png";
            FileHandle fntHandle = null;
            FileHandle pngHandle = null;
            if (Gdx.files.internal(gradFnt).exists() && Gdx.files.internal(gradPng).exists()) {
                fntHandle = Gdx.files.internal(gradFnt);
                pngHandle = Gdx.files.internal(gradPng);
            } else {
                String userDir = System.getProperty("user.dir");
                String absFnt = userDir + "/assets/" + gradFnt;
                String absPng = userDir + "/assets/" + gradPng;
                if (Gdx.files.absolute(absFnt).exists() && Gdx.files.absolute(absPng).exists()) {
                    fntHandle = Gdx.files.absolute(absFnt);
                    pngHandle = Gdx.files.absolute(absPng);
                }
            }
            if (fntHandle != null && pngHandle != null) {
                if (titleFont != null) { try { titleFont.dispose(); } catch (Exception ignored) {} titleFont = null; }
                if (titleFontTexture != null) { try { titleFontTexture.dispose(); } catch (Exception ignored) {} titleFontTexture = null; }
                titleFontTexture = new Texture(pngHandle);
                titleFontTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                titleFont = new BitmapFont(fntHandle, new TextureRegion(titleFontTexture), false);
                generated = true;
                Gdx.app.log("MainMenuScreen", "Loaded Gradient title font (priority) from: " + gradFnt);
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Error while attempting to load Gradient title font (priority)", e);
        }
        for (String ttfPath : candidates) {
            Gdx.app.log("MainMenuScreen", "Checking for TTF at: " + ttfPath);
            if (Gdx.files.internal(ttfPath).exists()) {
                Gdx.app.log("MainMenuScreen", "Found TTF, attempting FreeType generation: " + ttfPath);
                try {
                    // record the found path and generate using current size
                    foundTtfPath = ttfPath;
                    generateTitleFontWithSize(titleFontSize);
                    generated = true;
                    break;
                } catch (Exception e) {
                    Gdx.app.log("MainMenuScreen", "Failed to generate Pixelify font from TTF (" + ttfPath + "), falling back: " + e.getMessage());
                    titleFont = null;
                    foundTtfPath = null;
                }
            }
        }

        if (!generated && Gdx.files.internal("fonts/PixelifySans.fnt").exists()) {
            Gdx.app.log("MainMenuScreen", "Found .fnt font, loading BitmapFont");
            try {
                titleFont = new BitmapFont(Gdx.files.internal("fonts/PixelifySans.fnt"));
                // Ensure the font texture uses nearest filtering for a pixelated look
                if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                    titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                }
                Gdx.app.log("MainMenuScreen", "Loaded Pixelify .fnt successfully");
                generated = true;
            } catch (Exception e) {
                Gdx.app.log("MainMenuScreen", "Failed to load PixelifySans font, falling back: " + e.getMessage());
                titleFont = null;
            }
        }

        if (!generated) {
            Gdx.app.log("MainMenuScreen", "No Pixelify font assets found via internal paths; trying absolute asset paths...");

            // Try absolute paths (useful when running from Gradle where internal lookup may not find assets)
            String userDir = System.getProperty("user.dir");
            for (String ttfPath : candidates) {
                String absPath = userDir + "/assets/" + ttfPath;
                Gdx.app.log("MainMenuScreen", "Checking absolute path: " + absPath);
                if (Gdx.files.absolute(absPath).exists()) {
                    Gdx.app.log("MainMenuScreen", "Found TTF at absolute path, generating: " + absPath);
                    try {
                        foundTtfPath = absPath;
                        generateTitleFontWithSize(titleFontSize);
                        generated = true;
                        break;
                    } catch (Exception e) {
                        Gdx.app.log("MainMenuScreen", "Failed to generate font from absolute path " + absPath + ": " + e.getMessage());
                        titleFont = null;
                        foundTtfPath = null;
                    }
                }
            }

            if (!generated) {
                Gdx.app.log("MainMenuScreen", "No Pixelify font assets found; using default font with nearest filtering");
                titleFont = null;
            }
        }

        // If still not generated, try loading any .fnt present in assets/fonts (handles unexpected filenames)
        if (!generated) {
            FileHandle fontsRoot = Gdx.files.internal("fonts");
            if (fontsRoot.exists() && fontsRoot.isDirectory()) {
                for (FileHandle fh : fontsRoot.list()) {
                    if (fh.extension() != null && fh.extension().equalsIgnoreCase("fnt")) {
                        try {
                            titleFont = new BitmapFont(fh);
                            if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                                titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                            }
                            Gdx.app.log("MainMenuScreen", "Loaded .fnt font: " + fh.path());
                            generated = true;
                            break;
                        } catch (Exception e) {
                            Gdx.app.log("MainMenuScreen", "Failed to load .fnt at " + fh.path() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        // Final explicit attempt: try loading the specific Pexelify_Sans.fnt via internal and absolute paths
        if (!generated) {
            String explicitInternal = "fonts/Pexelify_Sans.fnt";
            String userDir = System.getProperty("user.dir");
            String explicitAbsolute = userDir + "/assets/fonts/Pexelify_Sans.fnt";
            Gdx.app.log("MainMenuScreen", "Attempting explicit loads: internal(" + explicitInternal + ") and absolute(" + explicitAbsolute + ")");
            try {
                if (Gdx.files.internal(explicitInternal).exists()) {
                    titleFont = new BitmapFont(Gdx.files.internal(explicitInternal));
                    if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                        titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                    }
                    Gdx.app.log("MainMenuScreen", "Loaded internal .fnt: " + explicitInternal);
                    generated = true;
                } else if (Gdx.files.absolute(explicitAbsolute).exists()) {
                    titleFont = new BitmapFont(Gdx.files.absolute(explicitAbsolute));
                    if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                        titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                    }
                    Gdx.app.log("MainMenuScreen", "Loaded absolute .fnt: " + explicitAbsolute);
                    generated = true;
                } else {
                    Gdx.app.log("MainMenuScreen", "Explicit .fnt not found in internal or absolute paths");
                }
            } catch (Exception e) {
                Gdx.app.error("MainMenuScreen", "Exception loading explicit .fnt", e);
                titleFont = null;
            }
        }
        // Try loading the Gradient Pexilify font (.fnt + .png) from assets/fonts/Gradient
        if (!generated) {
            String gradFnt = "fonts/Gradient/Gradient pexilify.fnt";
            String gradPng = "fonts/Gradient/Gradient pexilify.png";
            try {
                boolean fntExists = Gdx.files.internal(gradFnt).exists() || Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + gradFnt).exists();
                boolean pngExists = Gdx.files.internal(gradPng).exists() || Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + gradPng).exists();
                if (fntExists && pngExists) {
                    // prefer internal if available
                    FileHandle fntHandle = Gdx.files.internal(gradFnt).exists() ? Gdx.files.internal(gradFnt) : Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + gradFnt);
                    FileHandle pngHandle = Gdx.files.internal(gradPng).exists() ? Gdx.files.internal(gradPng) : Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + gradPng);
                    titleFontTexture = new Texture(pngHandle);
                    titleFontTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                    titleFont = new BitmapFont(fntHandle, new TextureRegion(titleFontTexture), false);
                    generated = true;
                    Gdx.app.log("MainMenuScreen", "Loaded Gradient title font from: " + gradFnt);
                } else {
                    Gdx.app.log("MainMenuScreen", "Gradient font files not found at: " + gradFnt + " / " + gradPng);
                }
            } catch (Exception e) {
                Gdx.app.error("MainMenuScreen", "Failed to load Gradient title font", e);
                // ensure titleFont null so fallback continues
                titleFont = null;
                if (titleFontTexture != null) { try { titleFontTexture.dispose(); } catch (Exception ignored) {} titleFontTexture = null; }
            }
        }
        // If no pixel font was created, force the default font to nearest filtering so scaled text appears pixelated
        if (titleFont == null && game.font != null && game.font.getRegion() != null && game.font.getRegion().getTexture() != null) {
            game.font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        }

        // Load button font: prefer a black Pexelify font if provided, then fall back to Pexelify_Sans
        try {
            String[] preferred = new String[] {
                "fonts/black pexelify/black.fnt",
                "fonts/Pexelify_Sans.fnt"
            };
            FileHandle btnHandle = null;
            String chosen = null;
            for (String p : preferred) {
                if (Gdx.files.internal(p).exists()) {
                    btnHandle = Gdx.files.internal(p);
                    chosen = p;
                    break;
                }
            }
            if (btnHandle == null) {
                String userDir = System.getProperty("user.dir");
                for (String p : preferred) {
                    String abs = userDir + "/assets/" + p;
                    if (Gdx.files.absolute(abs).exists()) {
                        btnHandle = Gdx.files.absolute(abs);
                        chosen = p;
                        break;
                    }
                }
            }
            if (btnHandle != null) {
                buttonFont = new BitmapFont(btnHandle);
                if (buttonFont.getRegion() != null && buttonFont.getRegion().getTexture() != null) {
                    buttonFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                }
                Gdx.app.log("MainMenuScreen", "Loaded button font: " + chosen);
            } else {
                buttonFont = null;
                Gdx.app.log("MainMenuScreen", "Button font not found in preferred locations");
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Failed to load button font", e);
            buttonFont = null;
        }

        Gdx.app.log("MainMenuScreen", "Main menu displayed");

        // Load running character sprite sheet: force `fixer run.png` only (internal or absolute).
        try {
            FileHandle runHandle = null;
            String internalName = "fixer run.png";
            // prefer internal asset path
            if (Gdx.files.internal(internalName).exists()) {
                runHandle = Gdx.files.internal(internalName);
            } else {
                // fallback to absolute path under project assets
                String userDir = System.getProperty("user.dir");
                String abs = userDir + "/assets/" + internalName;
                if (Gdx.files.absolute(abs).exists()) runHandle = Gdx.files.absolute(abs);
            }

            if (runHandle != null) {
                runTexture = new Texture(runHandle);
                runTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                // This specific sheet is expected to have 3 columns
                runColumns = 3;
                int frameW = Math.max(1, runTexture.getWidth() / runColumns);
                int frameH = runTexture.getHeight();
                TextureRegion[][] tmp = TextureRegion.split(runTexture, frameW, frameH);
                TextureRegion[] frames = new TextureRegion[runColumns];
                for (int i = 0; i < runColumns; i++) frames[i] = tmp[0][i];
                runAnimation = new Animation<TextureRegion>(MENU_RUN_FRAME_DURATION, frames);
            } else {
                Gdx.app.log("MainMenuScreen", "fixer run.png not found in assets (tried internal and absolute path)");
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Failed to load fixer run.png animation", e);
            runAnimation = null;
            runTexture = null;
        }

        // NEW: Load cat runner sprite sheet (Catrun.png)
        try {
            FileHandle catHandle = null;
            String catName = "Catrun.png";
            if (Gdx.files.internal(catName).exists()) {
                catHandle = Gdx.files.internal(catName);
            } else {
                String userDir = System.getProperty("user.dir");
                String abs = userDir + "/assets/" + catName;
                if (Gdx.files.absolute(abs).exists()) catHandle = Gdx.files.absolute(abs);
            }
            if (catHandle != null) {
                catTexture = new Texture(catHandle);
                catTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                // try 4 columns (fallback to 3 if width doesn't divide evenly)
                int ccols = catColumns;
                int cfw = Math.max(1, catTexture.getWidth() / ccols);
                int cfh = catTexture.getHeight();
                TextureRegion[][] ctmp = TextureRegion.split(catTexture, cfw, cfh);
                // if split produced fewer frames than expected, try fallback column count 3
                if (ctmp.length == 0 || ctmp[0].length < ccols) {
                    ccols = 3;
                    cfw = Math.max(1, catTexture.getWidth() / ccols);
                    ctmp = TextureRegion.split(catTexture, cfw, cfh);
                }
                int available = (ctmp.length > 0) ? Math.min(ctmp[0].length, ccols) : 0;
                TextureRegion[] cframes = new TextureRegion[Math.max(1, available)];
                for (int i = 0; i < cframes.length; i++) cframes[i] = ctmp[0][i];
                catAnimation = new Animation<TextureRegion>(MENU_RUN_FRAME_DURATION, cframes);
                Gdx.app.log("MainMenuScreen", "Loaded cat animation (Catrun.png) with " + cframes.length + " frames");
            } else {
                catAnimation = null;
                catTexture = null;
                Gdx.app.log("MainMenuScreen", "Catrun.png not found (internal or absolute).");
            }
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Failed to load Catrun.png animation", e);
            catAnimation = null;
            catTexture = null;
        }

        // Load main menu background (try internal then project assets/)
        try {
            String bgName = "MainMenuBG.png";
            if (Gdx.files.internal(bgName).exists()) {
                backgroundTex = new Texture(Gdx.files.internal(bgName));
                Gdx.app.log("MainMenuScreen", "Loaded background (internal): " + bgName);
            } else {
                String userDir = System.getProperty("user.dir");
                String abs = userDir + "/assets/" + bgName;
                if (Gdx.files.absolute(abs).exists()) {
                    backgroundTex = new Texture(Gdx.files.absolute(abs));
                    Gdx.app.log("MainMenuScreen", "Loaded background (absolute): " + abs);
                } else {
                    backgroundTex = null;
                    Gdx.app.log("MainMenuScreen", "MainMenuBG.png not found (internal or absolute).");
                }
            }
            if (backgroundTex != null) backgroundTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Error loading MainMenuBG.png", e);
            backgroundTex = null;
        }
    }

    private void generateTitleFontWithSize(int size) {
        // Dispose previous if present
        if (titleFont != null) {
            try { titleFont.dispose(); } catch (Exception ignored) {}
            titleFont = null;
        }
        if (foundTtfPath == null) return;
        FileHandle ttfHandle = null;
        try {
            // Prefer internal lookup, but if it's an absolute path use absolute file handle
            if (Gdx.files.internal(foundTtfPath).exists()) {
                ttfHandle = Gdx.files.internal(foundTtfPath);
            } else if (Gdx.files.absolute(foundTtfPath).exists()) {
                ttfHandle = Gdx.files.absolute(foundTtfPath);
            } else {
                // last-ditch: try user.dir + /assets/
                String userDir = System.getProperty("user.dir");
                String alt = userDir + "/assets/" + foundTtfPath;
                if (Gdx.files.absolute(alt).exists()) ttfHandle = Gdx.files.absolute(alt);
            }
            if (ttfHandle == null) {
                Gdx.app.error("MainMenuScreen", "TTF handle not found for path: " + foundTtfPath);
                return;
            }
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfHandle);
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = size;
            parameter.magFilter = TextureFilter.Nearest;
            parameter.minFilter = TextureFilter.Nearest;
            titleFont = generator.generateFont(parameter);
            generator.dispose();
            Gdx.app.log("MainMenuScreen", "Generated BitmapFont from TTF (size=" + parameter.size + ") using " + foundTtfPath);
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Exception while generating font from TTF: " + foundTtfPath, e);
            titleFont = null;
        }
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        // advance running animation timer
        runAnimTime += delta;
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background if available (stretched to fill window)
        if (backgroundTex != null) {
            game.batch.begin();
            game.batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            game.batch.end();
        }
        
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
        // Font size is fixed; size-cycling removed

        // Pointer / touch input: hover highlight + clicks/taps detection
        {
            float mx = Gdx.input.getX();
            float my = Gdx.graphics.getHeight() - Gdx.input.getY();
            float centerX = Gdx.graphics.getWidth() / 2f;
            float centerY = Gdx.graphics.getHeight() / 2f;
            float left = centerX - (BOX_W / 2f);

            // Precompute box vertical positions so both hover and click use the same values
            float yTop = centerY + MENU_TOP_OFFSET;
            float bottomTop = yTop - (BOX_H / 2f);
            float yMid = centerY + MENU_TOP_OFFSET - MENU_SPACING;
            float bottomMid = yMid - (BOX_H / 2f);
            float yBot = centerY + MENU_TOP_OFFSET - (2f * MENU_SPACING);
            float bottomBot = yBot - (BOX_H / 2f);

            // Hover: update hoveredOption (visual only) when pointer is over a box
            if (mx >= left && mx <= left + BOX_W && my >= bottomTop && my <= bottomTop + BOX_H) {
                hoveredOption = MenuOption.START_GAME;
            } else if (mx >= left && mx <= left + BOX_W && my >= bottomMid && my <= bottomMid + BOX_H) {
                hoveredOption = MenuOption.TUTORIAL;
            } else if (mx >= left && mx <= left + BOX_W && my >= bottomBot && my <= bottomBot + BOX_H) {
                hoveredOption = MenuOption.SETTINGS;
            } else {
                hoveredOption = null;
            }

            // Click / tap activation
            if (Gdx.input.justTouched()) {
                if (mx >= left && mx <= left + BOX_W && my >= bottomTop && my <= bottomTop + BOX_H) {
                    // activate the clicked option immediately
                    selectedOption = MenuOption.START_GAME;
                    selectCurrentOption();
                    return;
                }
                if (mx >= left && mx <= left + BOX_W && my >= bottomMid && my <= bottomMid + BOX_H) {
                    selectedOption = MenuOption.TUTORIAL;
                    selectCurrentOption();
                    return;
                }
                if (mx >= left && mx <= left + BOX_W && my >= bottomBot && my <= bottomBot + BOX_H) {
                    selectedOption = MenuOption.SETTINGS;
                    selectCurrentOption();
                    return;
                }
            }
        }
    }
    
    private void selectCurrentOption() {
        switch (selectedOption) {
            case START_GAME:
                Gdx.app.log("MainMenu", "Starting game...");
                game.setScreen(new LevelSelectScreen(game));
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
        
        // Title background removed — text will be drawn without a panel
        
        // Menu options backgrounds
        drawMenuOptionBox(centerX, centerY + MENU_TOP_OFFSET, MenuOption.START_GAME);
        drawMenuOptionBox(centerX, centerY + MENU_TOP_OFFSET - MENU_SPACING, MenuOption.TUTORIAL);
        drawMenuOptionBox(centerX, centerY + MENU_TOP_OFFSET - (2f * MENU_SPACING), MenuOption.SETTINGS);
        
        shapeRenderer.end();
        
        // NOTE: Title border removed as requested; only background panel remains.
        
        // Draw text
        game.batch.begin();
        
        // Title (use Pixelify Sans if available, scale up for title size)
        String titleText = "Paper Trail Panic";
        if (titleFont != null) {
            float prevScaleX = titleFont.getData().scaleX;
            float prevScaleY = titleFont.getData().scaleY;
            titleFont.getData().setScale(3.0f);
            GlyphLayout layout = new GlyphLayout(titleFont, titleText);
            float titleX = centerX - (layout.width / 2f);
            float titleY = centerY + 290;
            titleFont.draw(game.batch, titleText, titleX, titleY);
            titleFont.getData().setScale(prevScaleX, prevScaleY);
        } else {
            float prevX = game.font.getData().scaleX;
            float prevY = game.font.getData().scaleY;
            game.font.getData().setScale(2.0f);
            GlyphLayout layout = new GlyphLayout(game.font, titleText);
            float titleX = centerX - (layout.width / 2f);
            float titleY = centerY + 150;
            game.font.draw(game.batch, titleText, titleX, titleY);
            game.font.getData().setScale(prevX, prevY);
        }
        // Draw running character animation between the title and the buttons, scaled with a small bounce
        if (runAnimation != null) {
            // compute animation time (we pause advancing the runner's animation while in the restart delay)
            if (!runPaused) runAnimTime += Gdx.graphics.getDeltaTime();
            float menuTime = runAnimTime * MENU_RUN_SPEED_FACTOR;
            TextureRegion frame = runAnimation.getKeyFrame(menuTime, true);
            float fw = frame.getRegionWidth();
            float fh = frame.getRegionHeight();
            // determine draw size preserving existing sizing modes
            float drawW;
            float drawH;
            switch (RUN_SIZE_MODE) {
                case RUN_SIZE_MODE_EXPLICIT:
                    drawW = RUN_DRAW_WIDTH;
                    drawH = RUN_DRAW_HEIGHT;
                    break;
                case RUN_SIZE_MODE_DESIRED_H:
                    drawH = RUN_DESIRED_HEIGHT;
                    drawW = (fw * drawH) / fh;
                    break;
                case RUN_SIZE_MODE_SCREEN_REL:
                    drawH = Gdx.graphics.getHeight() * RUN_SCREEN_HEIGHT_RATIO;
                    drawW = (fw * drawH) / fh;
                    break;
                case RUN_SIZE_MODE_UNIFORM:
                default:
                    drawW = fw * RUN_SCALE;
                    drawH = fh * RUN_SCALE;
                    break;
            }

            // prepare cat draw sizes using the same sizing mode but scaled down
            float catDrawW = drawW * catScaleMultiplier;
            float catDrawH = drawH * catScaleMultiplier;
            float catGap = -15f; // smaller horizontal gap so cat runs closer to the runner

            // initialize runX and catX on first draw so we know draw sizes
            if (Float.isNaN(runX)) {
                runX = -drawW - 5f; // start a little off-screen left
                catX = runX - (catDrawW + catGap);
                runPaused = false;
                runPauseTimer = 0f;
            }

            // handle pause / restart timer
            if (runPaused) {
                runPauseTimer -= Gdx.graphics.getDeltaTime();
                if (runPauseTimer <= 0f) {
                    // restart from left
                    runX = -drawW - 10f;
                    catX = runX - (catDrawW + catGap);
                    runPaused = false;
                    // reset animation time so motion looks consistent
                    runAnimTime = 0f;
                }
            } else {
                // advance horizontal position
                float delta = Gdx.graphics.getDeltaTime();
                runX += runSpeed * delta;
                // keep cat locked to a fixed offset behind the runX (so it follows exactly)
                catX = runX - (catDrawW + catGap);

                // bounce and vertical placement as before (bounce uses menuTime so it's synced to animation)
                float bounce = (float)Math.sin(menuTime * RUN_BOUNCE_SPEED) * RUN_BOUNCE_AMPLITUDE;
                float drawY = centerY + RUN_Y_OFFSET + bounce;

                // draw cat behind runner if available
                if (catAnimation != null) {
                    TextureRegion cframe = catAnimation.getKeyFrame(menuTime, true);
                    // use drawY (runner bottom) as the cat baseline so feet line up;
                    // catYOffset remains available for fine tuning if required
                    float catDrawY = drawY + catYOffset;
                    game.batch.draw(cframe, catX, catDrawY, catDrawW, catDrawH);
                }

                // draw main runner
                game.batch.draw(frame, runX, drawY, drawW, drawH);

                // when the runner fully passes the right edge, start pause before restart
                float rightEdge = Gdx.graphics.getWidth();
                if (runX > rightEdge + 10f) {
                    runPaused = true;
                    runPauseTimer = runRestartDelay;
                }
            }
        }
        
        
        // Menu options (center text inside each rounded box)
        drawMenuOptionText("START GAME", centerX, centerY + MENU_TOP_OFFSET, MenuOption.START_GAME);
        drawMenuOptionText("TUTORIAL", centerX, centerY + MENU_TOP_OFFSET - MENU_SPACING, MenuOption.TUTORIAL);
        drawMenuOptionText("SETTINGS", centerX, centerY + MENU_TOP_OFFSET - (2f * MENU_SPACING), MenuOption.SETTINGS);
        
        // Instructions
        game.font.draw(game.batch, "UP/DOWN or W/S: Navigate | ENTER/SPACE: Select", 
            centerX - 180, 40);

        // On-screen debug: show which font source is active
        // Extra debug: whether titleFont was created and its texture filter
        // debug removed
        
        game.batch.end();
    }
    
    private void drawMenuOptionBox(float centerX, float y, MenuOption option) {
        // Visual selection is controlled by hover only
        boolean isSelected = (hoveredOption == option);
        float boxW = BOX_W;
        float boxH = BOX_H;
        float left = centerX - (boxW / 2f);
        float bottom = y - (boxH / 2f);
        float radius = BOX_RADIUS; // corner radius, adjustable via `BOX_RADIUS`

        // Fill rounded rectangle by composing center rects and corner circles
        shapeRenderer.setColor(isSelected ? new Color(0f, 0.6f, 0.6f, 0.95f) : new Color(217/255f, 217/255f, 217/255f, 1f));
        // center large rect
        shapeRenderer.rect(left + radius, bottom, boxW - 2f * radius, boxH);
        // left and right vertical strips
        shapeRenderer.rect(left, bottom + radius, radius, boxH - 2f * radius);
        shapeRenderer.rect(left + boxW - radius, bottom + radius, radius, boxH - 2f * radius);
        // top and bottom horizontal strips to smooth corners
        shapeRenderer.rect(left + radius, bottom + boxH - radius, boxW - 2f * radius, radius);
        shapeRenderer.rect(left + radius, bottom, boxW - 2f * radius, radius);

        // horizontal lines (inset by radius so corners appear rounded)
        shapeRenderer.line(left + radius, bottom + boxH, left + boxW - radius, bottom + boxH);
        shapeRenderer.line(left + radius, bottom, left + boxW - radius, bottom);
        // vertical lines
        shapeRenderer.line(left, bottom + radius, left, bottom + boxH - radius);
        shapeRenderer.line(left + boxW, bottom + radius, left + boxW, bottom + boxH - radius);
        // corner arcs (drawn as circle outlines centered at corner-circle centers)
        int segments = 16;
        shapeRenderer.circle(left + radius, bottom + radius, radius, segments);
        shapeRenderer.circle(left + boxW - radius, bottom + radius, radius, segments);
        shapeRenderer.circle(left + radius, bottom + boxH - radius, radius, segments);
        shapeRenderer.circle(left + boxW - radius, bottom + boxH - radius, radius, segments);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }
    
    private void drawMenuOptionText(String text, float x, float y, MenuOption option) {
        // Visual selection for text is based on hover only
        boolean isSelected = (hoveredOption == option);
        // Choose the button font if available, otherwise fall back to the game's default font
        BitmapFont font = (buttonFont != null) ? buttonFont : game.font;
        // apply requested scale while measuring and drawing, then restore previous scale
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(BUTTON_FONT_SCALE);
        GlyphLayout layout = new GlyphLayout(font, text);
        float textX = x - (layout.width / 2f);
        // BitmapFont.draw uses the y parameter as the baseline; to vertically center
        // inside a box whose center is at `y`, shift baseline up by half the layout height.
        float textY = y + (layout.height / 2f);

        // Indicate selection by configurable color (no arrow)
        // Use the SpriteBatch color to force a consistent tint regardless of font internals
        Color prevBatch = game.batch.getColor().cpy();
        try {
            if (isSelected) game.batch.setColor(BUTTON_TEXT_COLOR_SELECTED);
            else game.batch.setColor(BUTTON_TEXT_COLOR);
            font.draw(game.batch, layout, textX, textY);
        } finally {
            game.batch.setColor(prevBatch);
            // restore font scale
            font.getData().setScale(prevScaleX, prevScaleY);
        }
    }
    
    
    @Override
    public void resize(int width, int height) {
        // Update SpriteBatch and ShapeRenderer projection so UI scales with window
        Matrix4 proj = new Matrix4().setToOrtho2D(0, 0, width, height);
        game.batch.setProjectionMatrix(proj);
        shapeRenderer.setProjectionMatrix(proj);
    }
    
    @Override
    public void pause() {}
    
    @Override
    public void resume() {}
    
    @Override
    public void hide() {}
    
    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (titleFontTexture != null) { titleFontTexture.dispose(); titleFontTexture = null; }
        if (buttonFont != null) buttonFont.dispose();
        if (runTexture != null) runTexture.dispose();
        if (catTexture != null) {
            catTexture.dispose();
            catTexture = null;
        }
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }
    }
}
