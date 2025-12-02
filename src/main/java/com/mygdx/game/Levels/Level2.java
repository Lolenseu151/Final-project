package com.mygdx.game.Levels;
 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.LevelManager;
import com.mygdx.game.ILevelManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/**
 * Level2 - edit positions to design
 */

public class Level2 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 1000f;
    private float shredderY = 415f;
    private float shredderW = 36f; // was 50f
    private float shredderH = 36f; // was 50f
    private int totalDocs = 0;

    // centralized shredder visual
    private Shredder shredderVisual = null;

    // --- Intro/dialogue overlay fields (KMJS) ---
    private Texture introBg = null; // assets/overlay/kmjs dialouge.png
    private BitmapFont introFont = null;
    private final String[] introLines = new String[] {
        "Hold it right there. I'm KMJS, and I'm not security. I've been tracking this 'Paper Panic Trail,' and that file is the final piece to expose Loloy.",
        "I know why you're here. But if you walk away now if you leave that evidence alone I guarantee I will destroy him legally. He won't be able to touch you or anyone else again.",
        "So, what's your choice, Fixer? Are you covering a criminal, or are you handing the truth to the city? Avoid being caught by me."
    };
    private boolean showIntro = true;
    private int introIndex = 0;
    // Positioning for the intro block (top baseline)
    public float introTextX = 1400f;
    public float introTextY = 200f;
    public float introTextWidth = 850f;
    private final String continueLabel = "Continue";
    private float continueX = 1200f;
    private float continueY = 200f;
    private Texture underlineTex = null;
    private float introTypingElapsed = 0f;
    private static final float INTRO_TYPING_DURATION = 4f;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    // === JS (walking obstacle) fields and tunables ===
    // Public tunables for easy tweaking: edit these values to adjust patrol, speed, and size.
    public static float JS_LEFT_X = -200f;                      // left patrol X
    public static float JS_RIGHT_X = 300f;                     // right patrol X
    public static float JS_Y = -10f + PLATFORM_H + 2f;         // Y position (on second-level platform)
    // Vertical offset (pixels). Negative moves JS lower on the screen.
    public static float JS_Y_OFFSET = -270f;
    public static float JS_W = 55;                            // draw / collision width
    public static float JS_H = 110f;                            // draw / collision height
    public static float JS_SPEED = 60f;                        // px/sec walking speed
    public static float JS_FRAME_DURATION = 0.12f;             // animation frame duration
    // Visual scale multiplier for JS (increase to make the sprite larger)
    public static float JS_SCALE = 10.2f;
    // Sight parameters: horizontal distance in pixels in front of JS that it can "see"
    public static float JS_SIGHT_DISTANCE = 35f;
    // Vertical tolerance as fraction of JS height for sight (0..1)
    public static float JS_SIGHT_VERTICAL_TOLERANCE = 0.1f;

    private static final int JS_FRAME_COUNT = 4;                // expected frame count
    private TextureRegion[] jsFrames = null;
    private Texture jsSheetTex = null; // if textures are single images we'll still load as a sheet
    private float jsAnimTime = 0f;
    // walking state
    private float jsX = JS_LEFT_X;
    private boolean jsFacingRight = true;
    private Rectangle jsRect = null;
    // --- JS caught/pause UI state ---
    private boolean caughtPending = false;
    private float caughtTimer = 0f;
    private static final float CAUGHT_DELAY = 3.0f;
    private Texture jsCaughtTex = null;
    private TextureRegion jsCaughtRegion = null;

    

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();

            // dispose JS textures if any
            try {
                if (jsFrames != null) {
                    for (TextureRegion tr : jsFrames) {
                        if (tr == null) continue;
                        try {
                            Texture t = tr.getTexture();
                            if (t != null) { t.dispose(); }
                        } catch (Exception ignored) {}
                    }
                    jsFrames = null;
                }
            } catch (Exception ignored) {}
        float w = 1280;
        float h = 800;

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(50, 7, 1190, 20)); // LEFT SIDE
        
       

        // === FLOOR 2 ===
        platforms.add(new Rectangle(46, 225, 1074, 20));  // Left section
       

        // === FLOOR 3 ===
        platforms.add(new Rectangle(45, 400, 260, 20));
         platforms.add(new Rectangle(375, 400, 872, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(662, 583, 497, 20));


        // === Your existing items ===
        documents.add(new Rectangle(900, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 40, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(900, 580, DOC_SIZE, DOC_SIZE));


        // ========== WALLS START - Remove these if not needed ==========
        
        // Left boundary wall (full height)
        platforms.add(new Rectangle(15, 0, 15, h));
        
        // Right boundary wall (full height) - starts at w-50 to match image wall position
        platforms.add(new Rectangle(w - 50, 0, 50, h));
        
        // Top ceiling wall
        platforms.add(new Rectangle(0, h - 15, w, 15));
        
        // Interior vertical walls (orange markers from image)
        // First orange column (left area)
        platforms.add(new Rectangle(655, 420, 30, 150));
        
        // Second orange column (right side of middle area)
        platforms.add(new Rectangle(980, 420, 30, 150));
        
        // Third orange column (far right area)
        platforms.add(new Rectangle(655, 1250, 30, 20));
        
        // ========== WALLS END ==========


        //obstacles.add(new Rectangle(250, 150, 60, 10));
        //obstacles.add(new Rectangle(w - 300, 250, 60, 10));


        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;

        // initialize centralized shredder visual
        shredderVisual = new Shredder(shredder);
        shredderVisual.setFrameDuration(SHREDDER_FRAME_DURATION);
        shredderVisual.loadFromFolder("shredderFx", SHREDDER_FRAME_COUNT);
        // if no frames were loaded, try a single-image fallback
        try {
            if (!shredderVisual.hasVisual()) {
                shredderVisual.loadSingle("shredder.png");
                if (!shredderVisual.hasVisual()) shredderVisual.loadSingle("assets/shredder.png");
            }
        } catch (Exception ignored) {}
        // --- load KMJS intro assets (defensive) ---
        try {
            String[] tryPaths = new String[]{"assets/overlay/kmjs dialouge.png", "overlay/kmjs dialouge.png", "assets/kmjs dialouge.png", "kmjs dialouge.png"};
            for (String p : tryPaths) {
                try {
                    if (Gdx.files.internal(p).exists()) { introBg = new Texture(Gdx.files.internal(p)); break; }
                    if (Gdx.files.absolute(p).exists()) { introBg = new Texture(Gdx.files.absolute(p)); break; }
                } catch (Exception ignored2) {}
            }
            if (introBg != null) Gdx.app.log("Level2", "Loaded KMJS introBg from available path");
            else Gdx.app.log("Level2", "KMJS intro background not found in known paths");
        } catch (Exception ignored) {}
        try {
            if (introFont == null) {
                String[][] fontPaths = new String[][]{
                    {"fonts/Pexelify_Sans.fnt","fonts/Pexelify_Sans.png"},
                    {"assets/fonts/Pexelify_Sans.fnt","assets/fonts/Pexelify_Sans.png"}
                };
                for (String[] p : fontPaths) {
                    try {
                        String fnt = p[0]; String png = p[1];
                        if (Gdx.files.internal(fnt).exists() && Gdx.files.internal(png).exists()) {
                            introFont = new BitmapFont(Gdx.files.internal(fnt), Gdx.files.internal(png), false);
                            break;
                        } else if (Gdx.files.internal(fnt).exists()) {
                            introFont = new BitmapFont(Gdx.files.internal(fnt));
                            break;
                        }
                    } catch (Exception ignored2) {}
                }
                if (introFont == null) introFont = new BitmapFont();
                introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                Gdx.app.log("Level2","Loaded intro font (or default)");
            }
        } catch (Exception ignored) {}
        try {
            Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
            pm.setColor(1f,1f,1f,1f);
            pm.fill();
            underlineTex = new Texture(pm);
            pm.dispose();
        } catch (Exception ignored) { underlineTex = null; }
        // position text/continue
        try {
            if (introFont != null) {
                try { introFont.getData().setScale(0.9f); } catch (Exception ignored) {}
                introTextX = (1280f - introTextWidth) / 2f + 40f;
                try { GlyphLayout cont = new GlyphLayout(introFont, continueLabel); continueX = (1280f - cont.width) / 2f + 500f; } catch (Exception ignored) {}
                continueY = 80f;
            }
        } catch (Exception ignored) {}
        // === Initialize JS (walking obstacle) textures/frames ===
        try {
            // Try to load a small sprite-sheet folder first (assets/kmjs/1.png ...)
            java.util.ArrayList<TextureRegion> tmp = new java.util.ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                String p = String.format("assets/kmjs/%d.png", i);
                try {
                    if (Gdx.files.internal(p).exists()) {
                        Texture t = new Texture(Gdx.files.internal(p));
                        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        tmp.add(new TextureRegion(t));
                    } else if (Gdx.files.absolute(p).exists()) {
                        Texture t = new Texture(Gdx.files.absolute(p));
                        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        tmp.add(new TextureRegion(t));
                    }
                } catch (Exception e) {
                    // ignore and try next
                }
            }
            if (tmp.size() > 0) {
                jsFrames = tmp.toArray(new TextureRegion[0]);
            }
            // load the 'caught' frame (assets/kmjs/5.png) used when JS captures the player
            try {
                if (jsCaughtTex != null) { try { jsCaughtTex.dispose(); } catch (Exception ignored) {} jsCaughtTex = null; jsCaughtRegion = null; }
                String p5 = "assets/kmjs/5.png";
                if (Gdx.files.internal(p5).exists()) {
                    jsCaughtTex = new Texture(Gdx.files.internal(p5));
                    jsCaughtTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    jsCaughtRegion = new TextureRegion(jsCaughtTex);
                } else if (Gdx.files.absolute(p5).exists()) {
                    jsCaughtTex = new Texture(Gdx.files.absolute(p5));
                    jsCaughtTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    jsCaughtRegion = new TextureRegion(jsCaughtTex);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        // Reset JS patrol state so edits to JS_LEFT_X/JS_RIGHT_X apply when init() runs
        jsX = JS_LEFT_X;
        jsFacingRight = true;
        jsAnimTime = 0f;
        // Ensure collision rect exists for JS (use scaled size)
        float effWInit = JS_W * JS_SCALE;
        float effHInit = JS_H * JS_SCALE;
        float baseJSY = JS_Y + JS_Y_OFFSET;
        if (jsRect == null) {
            jsRect = new Rectangle(jsX, baseJSY, effWInit, effHInit);
        } else {
            jsRect.setPosition(jsX, baseJSY);
            jsRect.setSize(effWInit, effHInit);
        }
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
   

    // Return null so external debug renderers won't draw the shredder collision rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call getShredderCollisionRect().
    @Override public Rectangle getShredder() { return null; }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { return totalDocs; }

    @Override public void dispose() {
        // dispose shredder visual if present
        if (shredderVisual != null) {
            try { shredderVisual.dispose(); } catch (Exception ignored) {}
            shredderVisual = null;
        }
        // dispose caught texture if loaded
        if (jsCaughtTex != null) {
            try { jsCaughtTex.dispose(); } catch (Exception ignored) {}
            jsCaughtTex = null;
            jsCaughtRegion = null;
        }
        // dispose intro assets
        if (introBg != null) { try { introBg.dispose(); } catch (Exception ignored) {} introBg = null; }
        if (introFont != null) { try { introFont.dispose(); } catch (Exception ignored) {} introFont = null; }
        if (underlineTex != null) { try { underlineTex.dispose(); } catch (Exception ignored) {} underlineTex = null; }
    }

    @Override
    public String getBackgroundPath() {
        return "Level2Map.png";
    }

    @Override
    public String getMusicPath() {
        return "assets/Sounds/Level Music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, Array<Rectangle> documents,
                                 Array<Rectangle> obstacles, Fixer player) {
        // Advance shredder animation state time
        if (shredderVisual != null) {
            shredderVisual.update(deltaTime);
        }
        // If the KMJS intro/dialogue is visible, handle its input and pause gameplay
        if (showIntro) {
            try {
                boolean advanced = false;
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) advanced = true;
                if (Gdx.input.justTouched()) {
                    int tx = Gdx.input.getX();
                    int ty = Gdx.graphics.getHeight() - Gdx.input.getY();
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    float cx = continueX;
                    float cy = continueY;
                    float pad = 8f;
                    if (tx >= cx - pad && tx <= cx + cont.width + pad && ty >= (cy - cont.height) - pad && ty <= cy + pad) advanced = true;
                }
                if (advanced) {
                    if (introTypingElapsed < INTRO_TYPING_DURATION) {
                        introTypingElapsed = INTRO_TYPING_DURATION;
                    } else {
                        introIndex++;
                        introTypingElapsed = 0f;
                        if (introIndex >= introLines.length) showIntro = false;
                    }
                }
            } catch (Exception ignored) {}
            return; // pause the rest of Level2 while intro is shown
        }
        // If we are in a caught pending state, advance timer and only set GAMEOVER after delay
        if (caughtPending) {
            try {
                caughtTimer += deltaTime;
                if (caughtTimer >= CAUGHT_DELAY) {
                    caughtPending = false;
                    // Now set GAMEOVER via reflection on current screen (same approach as before)
                    try {
                        Object app = Gdx.app.getApplicationListener();
                        if (app instanceof com.badlogic.gdx.Game) {
                            Screen screen = ((com.badlogic.gdx.Game) app).getScreen();
                            if (screen != null) {
                                java.lang.reflect.Field f = null;
                                try { f = screen.getClass().getDeclaredField("currentState"); } catch (NoSuchFieldException nsf) {
                                    Class<?> sc = screen.getClass().getSuperclass();
                                    if (sc != null) { try { f = sc.getDeclaredField("currentState"); } catch (Exception ignored) {} }
                                }
                                if (f != null) {
                                    f.setAccessible(true);
                                    Class<?> enumType = f.getType();
                                    if (enumType.isEnum()) {
                                        Object val = java.lang.Enum.valueOf((Class) enumType, "GAMEOVER");
                                        f.set(screen, val);
                                        Gdx.app.log("Level2", "JS caught delay expired — setting GAMEOVER");
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                } else {
                    // while pending, skip normal JS updates/collisions
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Update JS (walking obstacle) animation and movement
        try {
            if (jsFrames != null && jsFrames.length > 0) {
                jsAnimTime += deltaTime;
                // advance position
                float move = JS_SPEED * deltaTime * (jsFacingRight ? 1f : -1f);
                jsX += move;
                if (jsX > JS_RIGHT_X) {
                    jsX = JS_RIGHT_X;
                    jsFacingRight = false;
                } else if (jsX < JS_LEFT_X) {
                    jsX = JS_LEFT_X;
                    jsFacingRight = true;
                }
                // update rect: prefer to match the actual drawn sprite (so large source images center correctly)
                float effW = JS_W * JS_SCALE;
                float effH = JS_H * JS_SCALE;
                float baseY = JS_Y + JS_Y_OFFSET;
                float drawX = jsX;
                float drawY = baseY;
                float drawW = effW;
                float drawH = effH;
                // If we have frames, compute how the current frame will be drawn (preserve aspect ratio)
                try {
                    int idx = (int)((jsAnimTime / Math.max(0.0001f, JS_FRAME_DURATION)) % jsFrames.length);
                    TextureRegion fr = jsFrames[idx];
                    float texW = fr.getRegionWidth();
                    float texH = fr.getRegionHeight();
                    if (texW > 0f && texH > 0f) {
                        float scale = Math.min(effW / texW, effH / texH);
                        drawW = texW * scale;
                        drawH = texH * scale;
                        drawX = jsX + (effW - drawW) * 0.5f;
                        drawY = baseY + (effH - drawH) * 0.5f;
                    }
                } catch (Exception ignored) {}
                if (jsRect == null) jsRect = new Rectangle(drawX, drawY, drawW, drawH);
                else {
                    jsRect.setPosition(drawX, drawY);
                    jsRect.setSize(drawW, drawH);
                }

                // collision: JS only catches the player if player is BOTH in front AND inside JS's sight
                if (player != null && player.getBounds() != null) {
                    Rectangle playerBounds = player.getBounds();
                    float playerCenterX = playerBounds.x + playerBounds.width * 0.5f;
                    float jsCenterX = jsX + (JS_W * JS_SCALE) * 0.5f;
                    boolean playerIsInFront = (jsFacingRight && playerCenterX > jsCenterX) || (!jsFacingRight && playerCenterX < jsCenterX);
                    if (playerIsInFront) {
                        // build a frontal sight rectangle originating from the DRAWN sprite center
                        float centerX = jsRect.x + jsRect.width * 0.5f; // center of visible sprite
                        float sightW = JS_SIGHT_DISTANCE;
                        float sightX = jsFacingRight ? centerX : (centerX - sightW);
                        float sightH = jsRect.height * JS_SIGHT_VERTICAL_TOLERANCE; // base on drawn height
                        // ensure sight has at least a small vertical size
                        if (sightH < 2f) sightH = 2f;
                        float sightY = jsRect.y + (jsRect.height - sightH) * 0.5f;
                        Rectangle sightRect = new Rectangle(sightX, sightY, sightW, sightH);

                        boolean directOverlap = (jsRect != null && jsRect.overlaps(playerBounds));
                        boolean inSight = sightRect.overlaps(playerBounds);
                        if (directOverlap || inSight) {
                            // Try to set the active GameScreen to GAMEOVER via reflection on the current screen
                            try {
                                Object app = Gdx.app.getApplicationListener();
                                if (app instanceof com.badlogic.gdx.Game) {
                                    Screen screen = ((com.badlogic.gdx.Game) app).getScreen();
                                    if (screen != null) {
                                        java.lang.reflect.Field f = null;
                                        try {
                                            f = screen.getClass().getDeclaredField("currentState");
                                        } catch (NoSuchFieldException nsf) {
                                            // try superclass if obfuscated or wrapped
                                            Class<?> sc = screen.getClass().getSuperclass();
                                            if (sc != null) {
                                                try { f = sc.getDeclaredField("currentState"); } catch (Exception ignored) {}
                                            }
                                        }
                                        if (f != null) {
                                            f.setAccessible(true);
                                            Class<?> enumType = f.getType();
                                            if (enumType.isEnum()) {
                                                // Instead of forcing GAMEOVER immediately, start a caught delay
                                                if (!caughtPending) {
                                                    caughtPending = true;
                                                    caughtTimer = 0f;
                                                    Gdx.app.log("Level2", "JS caught the player — starting caught delay");
                                                    try {
                                                        // Attempt to play kmjs SFX via the game's HoverSoundManager (use reflection to avoid direct dependency)
                                                        Object appObj = Gdx.app.getApplicationListener();
                                                        if (appObj != null) {
                                                            try {
                                                                java.lang.reflect.Method getHsm = appObj.getClass().getMethod("getHoverSoundManager");
                                                                Object hsm = getHsm.invoke(appObj);
                                                                if (hsm != null) {
                                                                    try {
                                                                        java.lang.reflect.Method play = hsm.getClass().getMethod("playKmjs");
                                                                        play.invoke(hsm);
                                                                    } catch (NoSuchMethodException nsme) {
                                                                        // method not present: ignore
                                                                    }
                                                                }
                                                            } catch (NoSuchMethodException nsme) {
                                                                // game class doesn't expose getHoverSoundManager
                                                            }
                                                        }
                                                    } catch (Exception ignored) {}
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
        // Render background; debug logging removed for shredder visuals.

        // draw background first (if any)
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, 1280, 800);
        }

        // draw shredder animation on top using centralized Shredder
        if (shredderVisual != null) {
            // ensure visual updates are reflected
            try { shredderVisual.update(Gdx.graphics.getDeltaTime()); } catch (Exception ignored) {}
            shredderVisual.render(batch);
        } else {
            // No shredder visual to draw.
        }

        // Draw JS (walking obstacle) on top of background/shredder
        try {
            if (caughtPending && jsCaughtRegion != null) {
                // Draw the special caught frame while in the caught delay
                try {
                    float effW = JS_W * JS_SCALE;
                    float effH = JS_H * JS_SCALE;
                    float texW = jsCaughtRegion.getRegionWidth();
                    float texH = jsCaughtRegion.getRegionHeight();
                    float baseY = JS_Y + JS_Y_OFFSET;
                    if (texW <= 0f || texH <= 0f) {
                        batch.draw(jsCaughtRegion, jsX, baseY, effW, effH);
                    } else {
                        float scale = Math.min(effW / texW, effH / texH);
                        float drawW = texW * scale;
                        float drawH = texH * scale;
                        float drawX = jsX + (effW - drawW) * 0.5f;
                        float drawY = baseY + (effH - drawH) * 0.5f;
                        batch.draw(jsCaughtRegion, drawX, drawY, drawW, drawH);
                    }
                } catch (Exception ignored) {
                    float effW = JS_W * JS_SCALE;
                    float effH = JS_H * JS_SCALE;
                    float baseY = JS_Y + JS_Y_OFFSET;
                    batch.draw(jsCaughtRegion, jsX, baseY, effW, effH);
                }
            } else if (jsFrames != null && jsFrames.length > 0) {
                int idx = (int)((jsAnimTime / Math.max(0.0001f, JS_FRAME_DURATION)) % jsFrames.length);
                TextureRegion fr = jsFrames[idx];
                // Ensure frame facing matches jsFacingRight (flip if necessary)
                boolean wantFlip = !jsFacingRight; // TextureRegion flip semantics: flipX==true means mirrored horizontally
                if (fr.isFlipX() != wantFlip) fr.flip(true, false);
                // Preserve aspect ratio: compute scale to fit within scaled JS_W x JS_H without stretching
                try {
                    float effW = JS_W * JS_SCALE;
                    float effH = JS_H * JS_SCALE;
                    float texW = fr.getRegionWidth();
                    float texH = fr.getRegionHeight();
                    float baseY = JS_Y + JS_Y_OFFSET;
                    if (texW <= 0f || texH <= 0f) {
                        batch.draw(fr, jsX, baseY, effW, effH);
                    } else {
                        float scale = Math.min(effW / texW, effH / texH);
                        float drawW = texW * scale;
                        float drawH = texH * scale;
                        float drawX = jsX + (effW - drawW) * 0.5f;
                        float drawY = baseY + (effH - drawH) * 0.5f;
                        batch.draw(fr, drawX, drawY, drawW, drawH);
                    }
                } catch (Exception e) {
                    // fallback to stretched draw if something unexpected happens
                    float effW = JS_W * JS_SCALE;
                    float effH = JS_H * JS_SCALE;
                    float baseY = JS_Y + JS_Y_OFFSET;
                    batch.draw(fr, jsX, baseY, effW, effH);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Draw overlays that must appear on top of gameplay (documents/platforms).
     * Called by LevelManager after documents are drawn.
     */
    @Override
    public void renderOverlay(SpriteBatch batch) {
        if (!showIntro) return;
        try {
            if (introBg != null) batch.draw(introBg, 0, 0, 1280, 800);
            if (introFont != null) {
                introTypingElapsed += Gdx.graphics.getDeltaTime();
                String full = introLines[introIndex];
                float frac = (INTRO_TYPING_DURATION <= 0f) ? 1f : Math.min(1f, introTypingElapsed / INTRO_TYPING_DURATION);
                int chars = Math.max(0, Math.min(full.length(), (int) (full.length() * frac)));
                String visible = full.substring(0, chars);
                GlyphLayout layout = new GlyphLayout();
                layout.setText(introFont, visible, com.badlogic.gdx.graphics.Color.WHITE, introTextWidth, com.badlogic.gdx.utils.Align.left, true);
                introFont.draw(batch, layout, introTextX, introTextY);
                GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                float cx = continueX; float cy = continueY;
                introFont.draw(batch, continueLabel, cx, cy);
                int mx = Gdx.input.getX(); int my = Gdx.graphics.getHeight() - Gdx.input.getY();
                boolean hover = mx >= cx && mx <= cx + cont.width && my >= (cy - cont.height) && my <= cy;
                if (hover && underlineTex != null) {
                    float pad2 = 2f;
                    batch.draw(underlineTex, cx, cy - cont.height - pad2, cont.width, 2f);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isOverlayBlocking() { return showIntro; }


}
