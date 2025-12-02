package com.mygdx.game;
 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;

/**
 * Level1 - edit positions to design
 */

public class Level1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
        private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 80f;
    private float shredderY = 415f;
    private float shredderW = 36f; // was 50f
    private float shredderH = 36f; // was 50f
    private int totalDocs = 0;

    // centralized shredder visual
    private Shredder shredderVisual = null;

    // --- Intro/dialogue overlay fields ---
    private Texture introBg = null; // keys.png full-screen overlay
    private BitmapFont introFont = null; // Pexelify_Sans
    private final String[] introLines = new String[] {
        "Fixer! You're in 'The Office'—Level One. Listen up, the clock is running on the audit, and if those documents are found, we're finished.",
        "Your mission is simple: find and shred every piece of evidence.",
        "Use the Arrow Keys to move through the cubicles, and if a guard gets too close, use [SPACE] to DASH.",
        "That dash is a power-up, Fixer, but it burns out fast—you've only got 10 seconds before it needs to recharge. Now move! Stop standing around!"
    };
    private boolean showIntro = true;
    private int introIndex = 0;
    // Default text positioning — edit these values later as needed
    public float introTextX = 100f;
    public float introTextY = 60f;
    public float introTextWidth = 90f; // wrap width
    // Continue button
    private final String continueLabel = "Continue";
    private float continueX = 1000f;
    private float continueY = 60f;
    private Texture underlineTex = null;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
                float w = 1280;
        float h = 800;

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(0, 7, 525, 20)); // LEFT SIDE
        platforms.add(new Rectangle(525, 22, 385, 20));   // MIDDLE SECTION
        platforms.add(new Rectangle(960, 0, 265, 4));  // RIGHT SIDE

        // === FLOOR 2 ===
        platforms.add(new Rectangle(0, 225, 620, 20));  // Left section
        platforms.add(new Rectangle(730, 225, 495, 20)); // Right section

        // === FLOOR 3 ===
        platforms.add(new Rectangle(0, 400, 925, 20));
         platforms.add(new Rectangle(1030, 400, 200, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(525, 540, 705, 20));


        // === Your existing items ===
        documents.add(new Rectangle(200, 30, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 40, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
         documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));

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
        // --- load intro assets (defensive) ---
        try {
            // Try multiple internal paths: 'keys.png' or 'assets/keys.png'
            String[] tryPaths = new String[]{"keys.png", "assets/keys.png", "overlay/keys.png", "assets/overlay/keys.png"};
            for (String p : tryPaths) {
                try {
                    if (Gdx.files.internal(p).exists()) { introBg = new Texture(Gdx.files.internal(p)); break; }
                    if (Gdx.files.absolute(p).exists()) { introBg = new Texture(Gdx.files.absolute(p)); break; }
                } catch (Exception ignored) {}
            }
            if (introBg != null) Gdx.app.log("Level1", "Loaded introBg from available path");
            else Gdx.app.log("Level1", "Intro background not found in known paths");
        } catch (Exception ignored) {}
        try {
            if (introFont == null) {
                // Try several likely font locations
                String[][] fontPaths = new String[][]{
                    {"fonts/Pexelify_Sans.fnt","fonts/Pexelify_Sans.png"},
                    {"assets/fonts/Pexelify_Sans.fnt","assets/fonts/Pexelify_Sans.png"},
                    {"assets/fonts/Pexelify_Sans.fnt","fonts/Pexelify_Sans.png"}
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
                    } catch (Exception ignored) {}
                }
                if (introFont == null) {
                    introFont = new BitmapFont();
                    Gdx.app.log("Level1", "Using default BitmapFont for intro (Pexelify not found)");
                } else {
                    Gdx.app.log("Level1", "Loaded Pexelify font for intro");
                }
                introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            }
        } catch (Exception ignored) {}
        // underline texture for hover effect (1x1 white pixel)
        try {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1,1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(1f,1f,1f,1f);
            pm.fill();
            underlineTex = new Texture(pm);
            pm.dispose();
        } catch (Exception ignored) { underlineTex = null; }
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
        // dispose intro assets
        if (introBg != null) {
            try { introBg.dispose(); } catch (Exception ignored) {}
            introBg = null;
        }
        if (introFont != null) {
            try { introFont.dispose(); } catch (Exception ignored) {}
            introFont = null;
        }
    }

    @Override
    public String getBackgroundPath() {
        return "Level1Map.png";
    }

    @Override
    public String getMusicPath() {
        return "assets/Sounds/Level Music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                 Array<Rectangle> obstacles, Fixer player) {
        // Advance shredder animation state time
        if (shredderVisual != null) {
            shredderVisual.update(deltaTime);
        }
        // If the intro/dialogue is visible, pause gameplay updates here and handle input to advance
        if (showIntro) {
            // advance on SPACE or touch/click
            try {
                boolean advanced = false;
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) advanced = true;
                if (Gdx.input.justTouched()) {
                    // detect if touch is on Continue label
                    int tx = Gdx.input.getX();
                    int ty = Gdx.graphics.getHeight() - Gdx.input.getY();
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    float cx = continueX;
                    float cy = continueY;
                    if (tx >= cx && tx <= cx + cont.width && ty >= (cy - cont.height) && ty <= cy) advanced = true;
                    else {
                        // also advance if anywhere touched
                        advanced = true;
                    }
                }
                if (advanced) {
                    introIndex++;
                    if (introIndex >= introLines.length) {
                        showIntro = false; // finished
                    }
                }
            } catch (Exception ignored) {}
            return; // skip other gameplay updates while intro is shown
        }
        // Example: track documents collected and adjust visual state
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

        // draw intro overlay on top if enabled
        if (showIntro) {
            try {
                if (introBg != null) batch.draw(introBg, 0, 0, 1280, 800);
                else if (underlineTex != null) {
                    // draw dim fallback background so text is visible
                    batch.setColor(0f,0f,0f,0.7f);
                    batch.draw(underlineTex, 0, 0, 1280, 800);
                    batch.setColor(1f,1f,1f,1f);
                }
                if (introFont != null) {
                    GlyphLayout layout = new GlyphLayout();
                    layout.setText(introFont, introLines[introIndex], com.badlogic.gdx.graphics.Color.WHITE, introTextWidth, com.badlogic.gdx.utils.Align.left, true);
                    introFont.draw(batch, layout, introTextX, introTextY + layout.height);
                    // draw Continue label and hover underline
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    float cx = continueX;
                    float cy = continueY;
                    introFont.draw(batch, continueLabel, cx, cy);
                    // detect hover
                    int mx = Gdx.input.getX();
                    int my = Gdx.graphics.getHeight() - Gdx.input.getY();
                    boolean hover = mx >= cx && mx <= cx + cont.width && my >= (cy - cont.height) && my <= cy;
                    if (hover && underlineTex != null) {
                        float pad = 2f;
                        batch.draw(underlineTex, cx, cy - cont.height - pad, cont.width, 2f);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

}
