package com.mygdx.game.Levels;
 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.LevelManager;
import com.mygdx.game.ILevelManager;
import com.badlogic.gdx.graphics.Texture;
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

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    // === JS (walking obstacle) fields and tunables ===
    // Public tunables for easy tweaking: edit these values to adjust patrol, speed, and size.
    public static float JS_LEFT_X = 70f;                      // left patrol X
    public static float JS_RIGHT_X = 400f;                     // right patrol X
    public static float JS_Y = 220f + PLATFORM_H + 2f;         // Y position (on second-level platform)
    public static float JS_W = 55;                            // draw / collision width
    public static float JS_H = 110f;                            // draw / collision height
    public static float JS_SPEED = 60f;                        // px/sec walking speed
    public static float JS_FRAME_DURATION = 0.12f;             // animation frame duration

    private static final int JS_FRAME_COUNT = 4;                // expected frame count
    private TextureRegion[] jsFrames = null;
    private Texture jsSheetTex = null; // if textures are single images we'll still load as a sheet
    private float jsAnimTime = 0f;
    // walking state
    private float jsX = JS_LEFT_X;
    private boolean jsFacingRight = true;
    private Rectangle jsRect = null;

    

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
        documents.add(new Rectangle(900, 500, DOC_SIZE, DOC_SIZE));


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
        // === Initialize JS (walking obstacle) textures/frames ===
        try {
            // Try to load a small sprite-sheet folder first (assets/kmjs/1.png ...)
            java.util.ArrayList<TextureRegion> tmp = new java.util.ArrayList<TextureRegion>();
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
                jsFrames = new TextureRegion[tmp.size()];
                tmp.toArray(jsFrames);
            }
        } catch (Exception ignored) {}

        // Ensure collision rect exists for JS
        if (jsRect == null) jsRect = new Rectangle(jsX, JS_Y, JS_W, JS_H);
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }

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
    }

    @Override
    public String getBackgroundPath() {
        return "Level2Map.png";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, Array<Rectangle> documents,
                                 Array<Rectangle> obstacles, Fixer player) {
        // Advance shredder animation state time
        if (shredderVisual != null) {
            shredderVisual.update(deltaTime);
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
                // update rect
                if (jsRect == null) jsRect = new Rectangle(jsX, JS_Y, JS_W, JS_H);
                else jsRect.setPosition(jsX, JS_Y);

                // collision: if overlaps player and JS is facing the player -> GAME OVER
                if (player != null && player.getBounds() != null && jsRect.overlaps(player.getBounds())) {
                    float playerCenterX = player.getBounds().x + player.getBounds().width * 0.5f;
                    float jsCenterX = jsX + JS_W * 0.5f;
                    boolean playerIsInFront = (jsFacingRight && playerCenterX > jsCenterX) || (!jsFacingRight && playerCenterX < jsCenterX);
                    if (playerIsInFront) {
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
                                            Object val = java.lang.Enum.valueOf((Class) enumType, "GAMEOVER");
                                            f.set(screen, val);
                                            Gdx.app.log("Level2", "JS caught the player — forcing GAMEOVER via reflection");
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    } else {
                        // player is behind JS; no effect (safe)
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
            if (jsFrames != null && jsFrames.length > 0) {
                int idx = (int)((jsAnimTime / Math.max(0.0001f, JS_FRAME_DURATION)) % jsFrames.length);
                TextureRegion fr = jsFrames[idx];
                // Ensure frame facing matches jsFacingRight (flip if necessary)
                boolean wantFlip = !jsFacingRight; // TextureRegion flip semantics: flipX==true means mirrored horizontally
                if (fr.isFlipX() != wantFlip) fr.flip(true, false);
                batch.draw(fr, jsX, JS_Y, JS_W, JS_H);
            }
        } catch (Exception ignored) {}
    }

}
