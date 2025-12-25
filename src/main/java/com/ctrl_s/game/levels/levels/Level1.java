package com.ctrl_s.game.levels.levels;

import com.ctrl_s.game.levels.BackgroundedLevel;
import com.ctrl_s.game.levels.ILevelManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.ctrl_s.game.entities.Player;

import com.badlogic.gdx.Gdx;
import com.ctrl_s.game.levels.LevelShredder;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * Level1 - edit positions to design
 */

public class Level1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> lasers = new Array<>();
    private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 80f;
    private float shredderY = 415f;
    private float shredderW = 36f; // was 50f
    private float shredderH = 36f; // was 50f
    // scale applied to the shredder visual/collision for this level only
    private float shredderScale = 1.6f; // increased scale for a much larger visual
    private int totalDocs = 0;

    // centralized shredder visual
    private LevelShredder shredderVisual = null;

    // --- Intro/dialogue overlay fields ---
    private Texture introBg = null; // keys.png full-screen overlay
    private BitmapFont introFont = null; // Pexelify_Sans
    private final String[] introLines = new String[] {
            "Fixer! You're in 'The Office' Level One. Listen up, the clock is running on the audit, and if those documents are found, we're finished.",
            "Your mission is simple: find and shred every piece of evidence.",
            "Use the Arrow Keys to move through the cubicles, and if time is running out or a guard gets too close, use [SPACE] to DASH.",
            "That dash is a power-up, Fixer, but it burns out fast you've only got 10 seconds before it needs to recharge. Now move! Stop standing around!"
    };
    private boolean showIntro = true;
    private int introIndex = 0;
    // Default text positioning — edit these values later as needed
    public float introTextX = 1400f; // will be centered in init()
    // introTextY is the TOP baseline for the block so it grows downward
    // Lower this value to move the block downward on the screen
    public float introTextY = 200f; // lowered further to move the intro block down

    public float introTextWidth = 850f; // reduced wrap width
    // Continue button
    private final String continueLabel = "Continue";
    private float continueX = 1200f;
    private float continueY = 20f;
    private Texture underlineTex = null;
    // typing transition state for intro text
    private float introTypingElapsed = 0f;
    private static final float INTRO_TYPING_DURATION = 2f; // seconds to type full caption

    private static final float DOC_SIZE = 36f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        lasers.clear();

        float w = 1280;
        float h = 800;

        // place shredder at the right-bottom based on level width
        try {
            shredderX = w - 130f; // 80px from right edge
            shredderY = 25f; // small offset above ground
        } catch (Exception ignored) {
        }

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(0, 7, 525, 20)); // LEFT SIDE
        platforms.add(new Rectangle(525, 22, 385, 20)); // MIDDLE SECTION
        platforms.add(new Rectangle(960, 0, 265, 4)); // RIGHT SIDE

        // === FLOOR 2 ===
        platforms.add(new Rectangle(0, 225, 620, 20)); // Left section
        platforms.add(new Rectangle(730, 225, 495, 20)); // Right section

        // === FLOOR 3 ===
        platforms.add(new Rectangle(0, 400, 925, 20));
        platforms.add(new Rectangle(1030, 400, 200, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(525, 540, 705, 20));

        // === Your existing items ===
        documents.add(new Rectangle(80, 410, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 300, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 90, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1000, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(600, 600, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1100, 600, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1100, 450, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1000, 100, DOC_SIZE, DOC_SIZE));

        // === WALLS (solid barriers that block player movement) ===
        // Using platforms array because it provides solid collision blocking

        // Left boundary wall (full height)
        platforms.add(new Rectangle(0, 0, 15, h));

        // Right boundary wall (full height) - starts at w-50 to match image wall
        // position
        platforms.add(new Rectangle(w - 50, 0, 50, h));

        // Top ceiling wall
        platforms.add(new Rectangle(0, h - 15, w, 15));

        // Between floor 1 middle and right
        platforms.add(new Rectangle(910, 4, 15, 18)); // short wall at floor 1

        // Wall for floor 4 right edge
        platforms.add(new Rectangle(1215, 560, 15, 240)); // from floor 4 to top

        // Lasers (cyan, semi-transparent) - you can control these easily

        lasers.add(new Rectangle(820, 250, 80, 140));

        // Keep the rectangle for gameplay/collision, but we'll draw the animated
        // shredder over it
        // Apply level-specific scale so this level's shredder appears larger
        float sw = shredderW * shredderScale;
        float sh = shredderH * shredderScale;
        shredder = new Rectangle(shredderX, shredderY, sw, sh);
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
        // --- load intro assets (defensive) ---
        try {
            introBg = new Texture(Gdx.files.internal("assets/sprites/ui/overlays/keys.png"));
            Gdx.app.log("Level1", "Loaded introBg from assets/sprites/ui/overlays/keys.png");
        } catch (Exception e) {
            introBg = null;
            Gdx.app.log("Level1", "Intro background not found");
        }
        try {
            if (introFont == null) {
                introFont = new BitmapFont(Gdx.files.internal("assets/fonts/Pexelify_Sans.fnt"),
                        Gdx.files.internal("assets/fonts/Pexelify_Sans.png"), false);
                introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                Gdx.app.log("Level1", "Loaded intro font from assets/fonts/Pexelify_Sans.fnt");
            }
        } catch (Exception e) {
            introFont = new BitmapFont();
            introFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            Gdx.app.log("Level1", "Using default BitmapFont for intro (Pexelify not found)");
        }
        // underline texture for hover effect (1x1 white pixel)
        try {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 1f);
            pm.fill();
            underlineTex = new Texture(pm);
            pm.dispose();
        } catch (Exception ignored) {
            underlineTex = null;
        }

        // Adjust font scale and center text/continue after font is loaded
        try {
            if (introFont != null) {
                // reduce font size slightly for a more compact dialog
                try {
                    introFont.getData().setScale(0.9f);
                } catch (Exception ignored) {
                }
                // center the text block horizontally and nudge it slightly to the right
                introTextX = (1280f - introTextWidth) / 2f + 40f;
                // center the Continue label horizontally
                try {
                    GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                    // center then nudge the Continue button to the right for accessibility
                    continueX = (1280f - cont.width) / 2f + 500f;
                } catch (Exception ignored) {
                }
                // place the Continue label below the intro block so it sits visually under the
                // text
                // lower it further for better spacing
                continueY = 90f;
            }
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

    // Return the level's shredder rect so LevelManager will position the shared
    // visual here.
    @Override
    public Rectangle getShredder() {
        return shredder;
    }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() {
        return shredder;
    }

    /**
     * Set whether the intro/dialogue has already been shown (for retry handling)
     */
    public void setIntroShown(boolean shown) {
        this.showIntro = !shown; // if shown=true, showIntro=false
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
        return "assets/maps/level_1_map.png";
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
        // If the intro/dialogue is visible, pause gameplay updates here and handle
        // input to advance
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
                    Gdx.app.log("Level1",
                            "Touch at: " + tx + "," + ty + " Continue area: x=" + cx + ".." + (cx + cont.width) + " y="
                                    + (cy - cont.height) + ".." + cy + " cont.width=" + cont.width + " cont.height="
                                    + cont.height);
                    // allow a small padding around the Continue label for easier tapping
                    float pad = 8f;
                    if (tx >= cx - pad && tx <= cx + cont.width + pad && ty >= (cy - cont.height) - pad
                            && ty <= cy + pad)
                        advanced = true;
                    // do not advance on a global touch; require click on Continue label
                }
                if (advanced) {
                    // if the typing animation is still running, finish it first
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
            try {
                shredderVisual.update(Gdx.graphics.getDeltaTime());
            } catch (Exception ignored) {
            }
            shredderVisual.render(batch);
        } else {
            // No shredder visual to draw.
        }

        // Note: overlay rendering moved to renderOverlay() so it can be drawn above
        // documents/platforms
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
            // log that render reached the intro overlay (useful to confirm draw path)
            Gdx.app.log("Level1", "renderOverlay: showIntro=" + showIntro + " introIndex=" + introIndex);
            if (introBg != null)
                batch.draw(introBg, 0, 0, 1280, 800);
            if (introFont != null) {
                // advance typing animation time
                introTypingElapsed += Gdx.graphics.getDeltaTime();
                String full = introLines[introIndex];
                float frac = Math.min(1f, introTypingElapsed / INTRO_TYPING_DURATION);
                int chars = Math.max(0, Math.min(full.length(), (int) (full.length() * frac)));
                String visible = full.substring(0, chars);
                GlyphLayout layout = new GlyphLayout();
                layout.setText(introFont, visible, com.badlogic.gdx.graphics.Color.WHITE, introTextWidth,
                        com.badlogic.gdx.utils.Align.left, true);
                // Draw at the top baseline so wrapped lines extend downward
                introFont.draw(batch, layout, introTextX, introTextY);
                // draw Continue label with a visible translucent rectangle behind it for
                // debugging
                GlyphLayout cont = new GlyphLayout(introFont, continueLabel);
                float cx = continueX;
                float cy = continueY;
                // draw Continue label (no red tint background)
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
