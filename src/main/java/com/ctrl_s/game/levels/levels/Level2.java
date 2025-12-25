package com.ctrl_s.game.levels.levels;

import com.ctrl_s.game.levels.BackgroundedLevel;
import com.ctrl_s.game.levels.ILevelManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.ctrl_s.game.entities.Player;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import com.ctrl_s.game.levels.LevelShredder;
import com.ctrl_s.game.entities.JSObstacle;

/**
 * Level2 - edit positions to design
 */

public class Level2 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private final Array<Rectangle> lasers = new Array<>();
    private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 1000f;
    private float shredderY = 415f;
    private float shredderW = 80f; // was 50f
    private float shredderH = 80f; // was 50f
    private int totalDocs = 0;

    // centralized shredder visual
    private LevelShredder shredderVisual = null;

    // --- Intro/dialogue overlay fields (KMJS) ---
    private Texture introBg = null; // assets/sprites/ui/overlays/kmjs/1.png
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

    // JS obstacle configuration (shared with JSObstacle behavior)
    private static final float JS_LEFT_X = -200f;
    private static final float JS_RIGHT_X = 300f;
    private static final float JS_BASE_Y = (-10f + PLATFORM_H + 2f) - 270f;
    private static final float JS_W = 55f;
    private static final float JS_H = 110f;
    private static final float JS_SPEED = 60f;
    private static final float JS_SIGHT_DISTANCE = 35f;
    private static final float JS_SIGHT_VERTICAL_TOLERANCE = 0.1f;
    private static final float JS_VISUAL_SCALE = 10.2f;
    private static final float JS_CAUGHT_DELAY = 3f;

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();
        lasers.clear();
        float w = 1280;
        float h = 800;

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(50, 7, 1190, 20)); // LEFT SIDE

        // === FLOOR 2 ===
        platforms.add(new Rectangle(46, 225, 1074, 20)); // Left section

        // === FLOOR 3 ===
        platforms.add(new Rectangle(45, 400, 260, 20));
        platforms.add(new Rectangle(375, 400, 872, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(662, 583, 497, 20));

        // === Your existing items ===
        documents.add(new Rectangle(600, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 40, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(150, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(600, 580, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(800, 700, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(400, 500, DOC_SIZE, DOC_SIZE));

        // ========== WALLS START - Remove these if not needed ==========

        // Left boundary wall (full height)
        platforms.add(new Rectangle(15, 0, 15, h));

        // Right boundary wall (full height) - starts at w-50 to match image wall
        // position
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

        // obstacles.add(new Rectangle(250, 150, 60, 10));
        // obstacles.add(new Rectangle(w - 300, 250, 60, 10));

        // Lasers (cyan, semi-transparent) - adjustable positions for gameplay
        // Laser 1: Left side, positioned vertically to create a challenge
        lasers.add(new Rectangle(350, 220, 80, 200));

        // Laser 2: Right side, positioned differently for variety
        lasers.add(new Rectangle(1010, 220, 80, 200));

        // Keep the rectangle for gameplay/collision, but we'll draw the animated
        // shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;

        // initialize centralized shredder visual
        shredderVisual = new LevelShredder(shredder);
        shredderVisual.setFrameDuration(SHREDDER_FRAME_DURATION);
        shredderVisual.loadFromFolder("assets/sprites/objects/shredder", SHREDDER_FRAME_COUNT);
        // if no frames were loaded, try a single-image fallback
        try {
            if (!shredderVisual.hasVisual()) {
                shredderVisual.loadSingle("assets/sprites/objects/shredder/shredder.png");
            }
        } catch (Exception ignored) {
        }
        // --- load KMJS intro assets (defensive) ---
        try {
            introBg = new Texture(Gdx.files.internal("assets/sprites/ui/overlays/kmjs/1.png"));
            Gdx.app.log("Level2", "Loaded KMJS introBg from assets/sprites/ui/overlays/kmjs/1.png");
        } catch (Exception e) {
            introBg = null;
            Gdx.app.log("Level2", "KMJS intro background not found");
        }
        try {
            if (introFont == null) {
                introFont = new BitmapFont(Gdx.files.internal("assets/fonts/Pexelify_Sans.fnt"),
                        Gdx.files.internal("assets/fonts/Pexelify_Sans.png"), false);
                introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                Gdx.app.log("Level2", "Loaded intro font from assets/fonts/Pexelify_Sans.fnt");
            }
        } catch (Exception e) {
            introFont = new BitmapFont();
            introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            Gdx.app.log("Level2", "Using default BitmapFont for intro");
        }
        try {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 1f);
            pm.fill();
            underlineTex = new Texture(pm);
            pm.dispose();
        } catch (Exception ignored) {
            underlineTex = null;
        }
        // position text/continue
        try {
            if (introFont != null) {
                try {
                    introFont.getData().setScale(0.9f);
                } catch (Exception ignored) {
                }
                introTextX = (1280f - introTextWidth) / 2f + 40f;
                try {
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    continueX = (1280f - cont.width) / 2f + 500f;
                } catch (Exception ignored) {
                }
                continueY = 80f;
            }
        } catch (Exception ignored) {
        }
        // Add JS obstacle with shared behavior so Level2 matches the other levels
        try {
            JSObstacle.addTo(obstacles,
                    JS_LEFT_X,
                    JS_RIGHT_X,
                    JS_BASE_Y,
                    JS_W,
                    JS_H,
                    JS_SPEED,
                    JS_SIGHT_DISTANCE,
                    JS_SIGHT_VERTICAL_TOLERANCE,
                    JS_VISUAL_SCALE,
                    JS_CAUGHT_DELAY);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Array<Rectangle> getDocuments() {
        return documents;
    }

    @Override
    public Array<Rectangle> getPlatforms() {
        return platforms;
    }

    @Override
    public Array<Rectangle> getObstacles() {
        return obstacles;
    }

    @Override
    public Array<Rectangle> getLasers() {
        return lasers;
    }

    // Return null so external debug renderers won't draw the shredder collision
    // rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call
    // getShredderCollisionRect().
    @Override
    public Rectangle getShredder() {
        return null;
    }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() {
        return shredder;
    }

    @Override
    public int getTotalDocuments() {
        return totalDocs;
    }

    @Override
    public void dispose() {
        // dispose shredder visual if present
        if (shredderVisual != null) {
            try {
                shredderVisual.dispose();
            } catch (Exception ignored) {
            }
            shredderVisual = null;
        }
        // dispose intro assets
        if (introBg != null) {
            try {
                introBg.dispose();
            } catch (Exception ignored) {
            }
            introBg = null;
        }
        if (introFont != null) {
            try {
                introFont.dispose();
            } catch (Exception ignored) {
            }
            introFont = null;
        }
        if (underlineTex != null) {
            try {
                underlineTex.dispose();
            } catch (Exception ignored) {
            }
            underlineTex = null;
        }
    }

    @Override
    public String getBackgroundPath() {
        return "assets/maps/level_2_map.png";
    }

    @Override
    public String getMusicPath() {
        return "assets/audio/music/level_music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, Array<Rectangle> documents,
            Array<Rectangle> obstacles, Player player) {
        // Advance shredder animation state time
        if (shredderVisual != null) {
            shredderVisual.update(deltaTime);
        }
        // If the KMJS intro/dialogue is visible, handle its input and pause gameplay
        if (showIntro) {
            try {
                boolean advanced = false;
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER))
                    advanced = true;
                if (Gdx.input.justTouched()) {
                    int tx = Gdx.input.getX();
                    int ty = Gdx.graphics.getHeight() - Gdx.input.getY();
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    float cx = continueX;
                    float cy = continueY;
                    float pad = 8f;
                    if (tx >= cx - pad && tx <= cx + cont.width + pad && ty >= (cy - cont.height) - pad
                            && ty <= cy + pad)
                        advanced = true;
                }
                if (advanced) {
                    if (introTypingElapsed < INTRO_TYPING_DURATION) {
                        introTypingElapsed = INTRO_TYPING_DURATION;
                    } else {
                        introIndex++;
                        introTypingElapsed = 0f;
                        if (introIndex >= introLines.length)
                            showIntro = false;
                    }
                }
            } catch (Exception ignored) {
            }
            return; // pause the rest of Level2 while intro is shown
        }
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
            try {
                shredderVisual.update(Gdx.graphics.getDeltaTime());
            } catch (Exception ignored) {
            }
            shredderVisual.render(batch);
        } else {
            // No shredder visual to draw.
        }

    }

    /**
     * Draw overlays that must appear on top of gameplay (documents/platforms).
     * Called by LevelManager after documents are drawn.
     */
    @Override
    public void renderOverlay(SpriteBatch batch) {
        if (!showIntro)
            return;
        try {
            if (introBg != null)
                batch.draw(introBg, 0, 0, 1280, 800);
            if (introFont != null) {
                introTypingElapsed += Gdx.graphics.getDeltaTime();
                String full = introLines[introIndex];
                float frac = Math.min(1f, introTypingElapsed / INTRO_TYPING_DURATION);
                int chars = Math.max(0, Math.min(full.length(), (int) (full.length() * frac)));
                String visible = full.substring(0, chars);
                GlyphLayout layout = new GlyphLayout();
                layout.setText(introFont, visible, com.badlogic.gdx.graphics.Color.WHITE, introTextWidth,
                        com.badlogic.gdx.utils.Align.left, true);
                introFont.draw(batch, layout, introTextX, introTextY);
                GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                float cx = continueX;
                float cy = continueY;
                introFont.draw(batch, continueLabel, cx, cy);
                int mx = Gdx.input.getX();
                int my = Gdx.graphics.getHeight() - Gdx.input.getY();
                boolean hover = mx >= cx && mx <= cx + cont.width && my >= (cy - cont.height) && my <= cy;
                if (hover && underlineTex != null) {
                    float pad2 = 2f;
                    batch.draw(underlineTex, cx, cy - cont.height - pad2, cont.width, 2f);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isOverlayBlocking() {
        return showIntro;
    }

}
