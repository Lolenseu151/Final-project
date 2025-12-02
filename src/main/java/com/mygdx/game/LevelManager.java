package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
// REMOVED: wrong Scene2D/UI List import and wrong reflect Method import
// import com.badlogic.gdx.scenes.scene2d.ui.List;
// import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.Array;
// ADDED: BackgroundedLevel lives in com.mygdx.game.Levels
import com.mygdx.game.Levels.BackgroundedLevel;
import com.mygdx.game.Levels.Level;
import com.mygdx.game.Levels.Shredder;

/**
 * LevelManager - Manages all level elements including obstacles, documents, and shredders
 * Responsible for level layout, collision detection, and objective tracking
 */
public class LevelManager implements ILevelManager {
    /** Listener callback for level completion events. */
    public interface LevelCompleteListener {
        void onLevelComplete();
    }

    /** Listener notified when shredding becomes active (shred start) */
    public interface ShredStartListener {
        void onShredStart();
    }

    private LevelCompleteListener levelCompleteListener = null;
    private ShredStartListener shredStartListener = null;

    /**
     * Register a listener to be notified when the level completes (shredding finished).
     */
    public void setLevelCompleteListener(LevelCompleteListener l) {
        this.levelCompleteListener = l;
    }

    /**
     * Register a listener to be notified when shredding begins (visual ACTIVE state).
     */
    public void setShredStartListener(ShredStartListener l) {
        this.shredStartListener = l;
    }
    // Level elements
    private final Array<Rectangle> documents;      // Incriminating documents to collect
    private final Array<Rectangle> obstacles;      // Red Tape obstacles (slow player)
    private final Array<Rectangle> auditorBeams;   // Auditor Beams (time penalties)
    private final Array<Rectangle> platforms;      // Platforms/floors for player to stand on
    private Rectangle shredder;                    // The shredder (win condition)

    // Level state
    private int documentsCollected;
    private int totalDocuments;
    private boolean levelComplete;
    // Shred delay state: when player reaches shredder with all docs, wait before completing
    private boolean shredPending = false;
    private float shredTimer = 0f;
    private static final float SHRED_DELAY_SECONDS = 3.0f;

    // Visual properties
    private static final float DOCUMENT_SIZE = 36f;
    private static final float OBSTACLE_WIDTH = 60f;
    private static final float OBSTACLE_HEIGHT = 10f;
    private static final float BEAM_WIDTH = 5f;
    private static final float SHREDDER_SIZE = 36f; // was 50f ΓÇö smaller collision rect
    private static final float PLATFORM_HEIGHT = 15f;

    // NEW: texture for document visuals (spritesheet)
    private Texture documentSheetTex;
    private TextureRegion[] documentFrames;
    // NEW: animation for documents
    private Animation<TextureRegion> documentAnim;
    private float documentAnimTime = 0f;
    // NEW: per-level background texture (used by loadLevel)
    private Texture backgroundTex;
    // NEW: shared shredder visual instance across levels (for Level3/Level3_1)
    private Shredder sharedShredder;

    /**
     * Creates a new Level Manager and initializes the level
     */
    public LevelManager() {
        this.documents = new Array<>();
        this.obstacles = new Array<>();
        this.auditorBeams = new Array<>();
        this.platforms = new Array<>();
        this.documentsCollected = 0;
        this.levelComplete = false;

        // load documents spritesheet safely (try internal then absolute and common asset path), fallback placeholder
        try {
            String[] candidates = new String[] {
                "documents.png",
                "Documents.png",
                "assets/documents.png",
                "assets/Documents.png"
            };
            boolean loaded = false;
            for (String c : candidates) {
                if (c == null) continue;
                if (Gdx.files.internal(c).exists()) {
                    documentSheetTex = new Texture(Gdx.files.internal(c));
                    Gdx.app.log("LevelManager", "Loaded document spritesheet (internal): " + c);
                    loaded = true;
                    break;
                }
                if (Gdx.files.absolute(c).exists()) {
                    documentSheetTex = new Texture(Gdx.files.absolute(c));
                    Gdx.app.log("LevelManager", "Loaded document spritesheet (absolute): " + c);
                    loaded = true;
                    break;
                }
            }
            if (!loaded) {
                Pixmap pm = new Pixmap((int)DOCUMENT_SIZE, (int)DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
                pm.setColor(1f, 1f, 1f, 0f);
                pm.fill();
                documentSheetTex = new Texture(pm);
                pm.dispose();
                Gdx.app.log("LevelManager", "documents.png not found, using placeholder.");
            }

            // Split spritesheet into frames: 2 cols x 3 rows
            try {
                final int COLS = 2;
                final int ROWS = 3;
                int fw = Math.max(1, documentSheetTex.getWidth() / COLS);
                int fh = Math.max(1, documentSheetTex.getHeight() / ROWS);
                TextureRegion[][] tmp = TextureRegion.split(documentSheetTex, fw, fh);
                documentFrames = new TextureRegion[COLS * ROWS];
                int idx = 0;
                for (int r = 0; r < ROWS; r++) {
                    for (int c = 0; c < COLS; c++) {
                        if (r < tmp.length && c < tmp[r].length) {
                            documentFrames[idx++] = tmp[r][c];
                        }
                    }
                }
                if (idx < documentFrames.length) {
                    TextureRegion[] trimmed = new TextureRegion[idx];
                    System.arraycopy(documentFrames, 0, trimmed, 0, idx);
                    documentFrames = trimmed;
                }
            } catch (Exception e) {
                Gdx.app.error("LevelManager", "Error splitting document spritesheet", e);
                documentFrames = new TextureRegion[] { new TextureRegion(documentSheetTex) };
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager", "Error loading documents spritesheet", e);
            Pixmap pm = new Pixmap((int)DOCUMENT_SIZE, (int)DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 0f);
            pm.fill();
            documentSheetTex = new Texture(pm);
            pm.dispose();
            documentFrames = new TextureRegion[] { new TextureRegion(documentSheetTex) };
        }

        // after loading documentSheetTex (right after you create the Texture)
        if (documentSheetTex != null) {
            // smooth scaling when we draw the sprites larger than native size
            documentSheetTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        // Create an animation from the frames (safe even if there's only 1 frame).
        try {
            if (documentFrames != null && documentFrames.length > 0) {
                // SLOWER animation: 0.24s per frame (was 0.12s)
                final float FRAME_DURATION = 0.24f;
                documentAnim = new Animation<TextureRegion>(FRAME_DURATION, documentFrames);
                // keep ping-pong for a subtle pulse, but slower now
                documentAnim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
            } else if (documentSheetTex != null) {
                // single-frame fallback (slower)
                documentAnim = new Animation<TextureRegion>(0.5f, new TextureRegion(documentSheetTex));
                documentAnim.setPlayMode(Animation.PlayMode.LOOP);
            } else {
                documentAnim = null;
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager", "Error creating document animation", e);
            documentAnim = null;
        }

        initializeLevel();
    }

    /**
     * Initializes the level layout with documents, obstacles, and shredder
     */
    private void initializeLevel() {
        // Clear existing elements
        documents.clear();
        obstacles.clear();
        auditorBeams.clear();
        platforms.clear();

        // Place platforms first (so we can position documents on them)
        placePlatforms();

        // Place documents throughout the level
        placeDocuments();

        // Place obstacles (Red Tape)
        placeObstacles();

        // Place Auditor Beams
        placeAuditorBeams();

        // Place shredder at the end
        placeShredder();

        totalDocuments = documents.size;
        documentsCollected = 0;
        levelComplete = false;

        Gdx.app.log("LevelManager", String.format("Level initialized: %d documents, %d platforms, %d obstacles, %d beams",
            totalDocuments, platforms.size, obstacles.size, auditorBeams.size));
    }

    /**
     * Places platforms at different heights
     */
    private void placePlatforms() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Ground floor (full width)
        platforms.add(new Rectangle(0, 0, screenWidth, PLATFORM_HEIGHT));

        // First level platforms (around 150 units high)
        platforms.add(new Rectangle(0, 125, 700, PLATFORM_HEIGHT));
        //platforms.add(new Rectangle(screenWidth - 300, 150, 200, PLATFORM_HEIGHT));

        // Second level platforms (around 280 units high)
       platforms.add(new Rectangle(50, 250, 500, PLATFORM_HEIGHT));
       //platforms.add(new Rectangle(250, 280, 180, PLATFORM_HEIGHT));
       //platforms.add(new Rectangle(screenWidth - 250, 280, 180, PLATFORM_HEIGHT));

        // Third level platforms (around 410 units high) - harder to reach
        platforms.add(new Rectangle(screenWidth / 2 - 50, 350, 200, PLATFORM_HEIGHT));
        platforms.add(new Rectangle(screenWidth - 180, 410, 150, PLATFORM_HEIGHT));

        // Top platform (near ceiling) - requires precise jumps
        if (screenHeight > 500) {
            platforms.add(new Rectangle(screenWidth / 2 - 80, screenHeight - 120, 160, PLATFORM_HEIGHT));
        }
    }

    /**
     * Places documents throughout the level at different heights
     */
    private void placeDocuments() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        Gdx.app.log("LevelManager", "Screen size: " + screenWidth + "x" + screenHeight);

        // Document 1: On ground level - easy to get
        Rectangle doc1 = new Rectangle(150, PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc1);
        Gdx.app.log("LevelManager", "Doc 1 at: (" + doc1.x + ", " + doc1.y + ")");

        // Document 2: On first level platform (left) - requires jumping
        Rectangle doc2 = new Rectangle(120, 150 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc2);
        Gdx.app.log("LevelManager", "Doc 2 at: (" + doc2.x + ", " + doc2.y + ")");

        // Document 3: On second level platform (right) - requires multiple jumps
        Rectangle doc3 = new Rectangle(screenWidth - 200, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc3);
        Gdx.app.log("LevelManager", "Doc 3 at: (" + doc3.x + ", " + doc3.y + ")");

        // Document 4: On third level platform (center) - tricky to reach
        Rectangle doc4 = new Rectangle(screenWidth / 2 - 50, 410 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc4);
        Gdx.app.log("LevelManager", "Doc 4 at: (" + doc4.x + ", " + doc4.y + ")");

        // Document 5: On another second level platform (left) - strategic placement
        Rectangle doc5 = new Rectangle(80, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc5);
        Gdx.app.log("LevelManager", "Doc 5 at: (" + doc5.x + ", " + doc5.y + ")");

        // Document 6: On another second level platform (middle) - adds challenge
        Rectangle doc6 = new Rectangle(280, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc6);
        Gdx.app.log("LevelManager", "Doc 6 at: (" + doc6.x + ", " + doc6.y + ")");

        // Document 7: On right side first level - requires navigation
        Rectangle doc7 = new Rectangle(screenWidth - 250, 150 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE);
        documents.add(doc7);
        Gdx.app.log("LevelManager", "Doc 7 at: (" + doc7.x + ", " + doc7.y + ")");
    }

    /**
     * Places obstacles (Red Tape) that slow the player on various platforms
     */
    private void placeObstacles() {
        float screenWidth = Gdx.graphics.getWidth();

        // Obstacles on ground - positioned ON TOP of the platform (not inside it)
        obstacles.add(new Rectangle(200, PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(400, PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));

        // Obstacles on first level platforms (height 125) - positioned on top
        obstacles.add(new Rectangle(screenWidth - 280, 125 + PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));

        // Obstacles on second level platforms (height 250) - positioned on top
        obstacles.add(new Rectangle(70, 250 + PLATFORM_HEIGHT + 5, 50, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(350, 250 + PLATFORM_HEIGHT + 5, 50, OBSTACLE_HEIGHT));
    }

    /**
     * Places Auditor Beams (vertical laser grids)
     */
    private void placeAuditorBeams() {
        float screenHeight = Gdx.graphics.getHeight();

        // Place vertical beams
        auditorBeams.add(new Rectangle(350, 0, BEAM_WIDTH, screenHeight));
        auditorBeams.add(new Rectangle(550, 0, BEAM_WIDTH, screenHeight));
    }

    /**
     * Places the shredder (goal)
     */
    private void placeShredder() {
        float screenWidth = Gdx.graphics.getWidth();
        // keep same position but use the smaller constant size
        shredder = new Rectangle(screenWidth - 80, 10, SHREDDER_SIZE, SHREDDER_SIZE);
    }

    /**
     * Updates the level (checks collisions, etc.)
     * @param deltaTime Time since last update
     * @param player The Fixer (player) to check collisions against
     * @return Time penalty (in seconds) if any beams were touched
     */
    public float update(float deltaTime, Fixer player) {
        float timePenalty = 0f;

        // Update background state if level provides it
        if (currentLevel instanceof BackgroundedLevel) {
            ((BackgroundedLevel) currentLevel).updateBackground(deltaTime, this, documents, obstacles, player);
        }

        // Hard clamp player position to prevent going through walls at screen edges
        if (player != null && player.getBounds() != null) {
            Rectangle pb = player.getBounds();
            // Find leftmost and rightmost walls
            float minAllowedX = 0f;
            float maxAllowedX = Gdx.graphics.getWidth() - pb.width;
            
            for (Rectangle platform : platforms) {
                // Check if this is a vertical wall (height > 20 to catch short walls too)
                if (platform.height > 20) {
                    // Left wall
                    if (platform.x < 50) {
                        minAllowedX = Math.max(minAllowedX, platform.x + platform.width);
                    }
                    // Right wall
                    if (platform.x > Gdx.graphics.getWidth() - 200) {
                        maxAllowedX = Math.min(maxAllowedX, platform.x - pb.width);
                    }
                    // Interior walls (check all other walls)
                    if (platform.x >= 50 && platform.x <= Gdx.graphics.getWidth() - 200) {
                        // Check if player is overlapping this wall horizontally
                        if (pb.x + pb.width > platform.x && pb.x < platform.x + platform.width) {
                            // Player is in the wall's x-range, need to check vertical overlap
                            if (pb.y < platform.y + platform.height && pb.y + pb.height > platform.y) {
                                // Player is overlapping the wall, push them out
                                float overlapLeft = (pb.x + pb.width) - platform.x;
                                float overlapRight = (platform.x + platform.width) - pb.x;
                                if (overlapLeft < overlapRight) {
                                    pb.x = platform.x - pb.width;
                                    player.setVelocityX(0f);
                                } else {
                                    pb.x = platform.x + platform.width;
                                    player.setVelocityX(0f);
                                }
                            }
                        }
                    }
                }
            }
            
            if (pb.x < minAllowedX) {
                pb.x = minAllowedX;
                player.setVelocityX(0f);
            }
            if (pb.x > maxAllowedX) {
                pb.x = maxAllowedX;
                player.setVelocityX(0f);
            }
        }

        // Check platform collisions (player standing on platforms)
        checkPlatformCollisions(player, deltaTime);

        // Reset slowed state; will be set if overlapping any obstacle below
        player.setSlowed(false);

        // Check document collection
        for (int i = documents.size - 1; i >= 0; i--) {
            Rectangle doc = documents.get(i);
            if (player.getBounds().overlaps(doc)) {
                documents.removeIndex(i);
                documentsCollected++;
                Gdx.app.log("LevelManager", String.format("Document collected! (%d/%d)", 
                    documentsCollected, totalDocuments));
                // If this is the tutorial level and this is the first document, trigger talking overlay
                try {
                    if (currentLevel instanceof LevelTutorial && documentsCollected == 1) {
                        ((LevelTutorial) currentLevel).setTalkingOverlayVisible(true);
                    }
                    // If we have collected all documents, attempt to set shredder visual to READY
                    if (currentLevel != null && documentsCollected >= totalDocuments) {
                        try {
                            java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                            f.setAccessible(true);
                            Object sv = f.get(currentLevel);
                            if (sv != null) {
                                try {
                                    java.lang.reflect.Method m = sv.getClass().getMethod("setReady");
                                    m.invoke(sv);
                                } catch (NoSuchMethodException ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }

        // If this is the tutorial level and at least 4 documents have been collected,
        // request the LevelTutorial to show the final talking overlay image.
        try {
            if (currentLevel instanceof LevelTutorial && documentsCollected >= 4) {
                LevelTutorial lt = (LevelTutorial) currentLevel;
                // Only show the final overlay if it hasn't been consumed (dismissed) already
                if (!lt.isFinalOverlayVisible() && !lt.isFinalOverlayConsumed()) {
                    lt.setFinalOverlayVisible(true);
                }
            }
        } catch (Exception ignored) {}

        // Check obstacle collision (Red Tape - slows player)
        boolean slowed = false;
        for (Rectangle obstacle : obstacles) {
            if (player.getBounds().overlaps(obstacle)) {
                slowed = true;
                // Do not directly mutate velocity here; inform the player that they are slowed
                Gdx.app.log("LevelManager", "Hit Red Tape! Player slowed.");
                break; // one obstacle is enough to slow the player
            }
        }
        player.setSlowed(slowed);

        // Check Auditor Beam collision (time penalty)
        for (Rectangle beam : auditorBeams) {
            if (player.getBounds().overlaps(beam)) {
                timePenalty += 2f * deltaTime; // 2 seconds penalty per frame in beam
            }
        }

        // Check if level is complete (all documents collected + reached shredder)
        // Instead of completing immediately, start a delayed shred sequence so we can show the
        // paper being shredded visually (e.g., play shredder ACTIVE animation) for a short time.
        if (documentsCollected >= totalDocuments && rectsOverlap(player.getBounds(), shredder)) {
            if (!levelComplete && !shredPending) {
                shredPending = true;
                shredTimer = 0f;
                Gdx.app.log("LevelManager", "Shredding sequence started ΓÇö delaying completion for " + SHRED_DELAY_SECONDS + "s");
                // Notify any registered listener that shredding has started
                try {
                    if (shredStartListener != null) {
                        try { shredStartListener.onShredStart(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                // Try to set shredder visual to ACTIVE via reflection if present on the level
                try {
                    if (currentLevel != null) {
                        java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(currentLevel);
                        if (sv != null) {
                            try {
                                java.lang.reflect.Method m = sv.getClass().getMethod("setActive");
                                m.invoke(sv);
                            } catch (NoSuchMethodException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // If a shred sequence is pending, advance its timer and complete the level when elapsed
        if (shredPending && !levelComplete) {
            shredTimer += deltaTime;
                if (shredTimer >= SHRED_DELAY_SECONDS) {
                levelComplete = true;
                shredPending = false;
                Gdx.app.log("LevelManager", "LEVEL COMPLETE! All documents shredded! (after delay)");
                // Optionally set shredder back to idle via reflection
                try {
                    if (currentLevel != null) {
                        java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(currentLevel);
                        if (sv != null) {
                            try {
                                java.lang.reflect.Method m = sv.getClass().getMethod("setIdle");
                                m.invoke(sv);
                            } catch (NoSuchMethodException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
                // Notify listener (if any) that the level has completed so UI can react immediately
                try {
                    if (levelCompleteListener != null) levelCompleteListener.onLevelComplete();
                } catch (Exception ignored) {}
            }
        }

        // Screen-edge checks removed - using platform walls for boundaries instead

        return timePenalty;
    }

    /**
     * Checks if the player is colliding with any platforms and handles landing
     */
    private void checkPlatformCollisions(Fixer player, float deltaTime) {
        Rectangle p = player.getBounds();
        player.setOnGround(false);
        // Use previous-position checks to avoid jitter when hitting platform tops/undersides.
        // Also require a minimum horizontal overlap so corner/edge overlaps don't count as standing.
        float vy = player.getVelocity().y;
        float vx = player.getVelocity().x;
        float prevY = p.y - vy * deltaTime; // approximate previous bottom
        float prevTop = prevY + p.height;
        float prevBottom = prevY;
        final float EPS = 0.6f; // small offset to prevent sticking
        final float MIN_HORIZONTAL_OVERLAP = Math.max(6f, p.width * 0.25f);

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform)) continue;

            // compute horizontal overlap amount
            float left = Math.max(p.x, platform.x);
            float right = Math.min(p.x + p.width, platform.x + platform.width);
            float overlapX = right - left;

            // If not enough horizontal overlap, treat as side contact and ignore for vertical landing
            if (overlapX < MIN_HORIZONTAL_OVERLAP) continue;

            // If moving down (vy <= 0): check if we crossed the platform top this frame -> land
            if (vy <= 0f) {
                // previous bottom was at or above platform top (standing above) -> landed this frame
                if (prevBottom >= platform.y + platform.height - EPS || prevTop > platform.y + platform.height) {
                    p.y = platform.y + platform.height;
                    player.setVelocityY(0f);
                    player.setOnGround(true);
                    return;
                }
            } else {
                // moving up: check if we came from below and pierced into platform this frame -> block underside
                if (prevTop <= platform.y + EPS && (p.y + p.height) > platform.y + EPS) {
                    p.y = platform.y - p.height - EPS;
                    player.setVelocityY(0f);
                    player.setOnGround(false);
                    return;
                }
            }
        }

        // If no vertical collision resolved, check horizontal collisions to prevent passing through sides
        float prevX = p.x - vx * deltaTime;
        float prevLeft = prevX;
        float prevRight = prevX + p.width;
        final float H_EPS = 0.6f;

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform)) continue;

            // compute vertical overlap to ensure we're not resolving vertical case here
            float top = Math.min(p.y + p.height, platform.y + platform.height);
            float bottom = Math.max(p.y, platform.y);
            float overlapY = top - bottom;
            if (overlapY <= 0f) continue;
            
            // Check if moving horizontally and overlapping the wall
            if (Math.abs(vx) > 0.1f) {
                float currentLeft = p.x;
                float currentRight = p.x + p.width;
                float wallLeft = platform.x;
                float wallRight = platform.x + platform.width;
                
                // Moving right into left side of wall
                if (vx > 0 && currentRight > wallLeft && prevRight <= wallLeft + H_EPS) {
                    p.x = wallLeft - p.width - H_EPS;
                    player.setVelocityX(0f);
                    return;
                }
                // Moving left into right side of wall
                if (vx < 0 && currentLeft < wallRight && prevLeft >= wallRight - H_EPS) {
                    p.x = wallRight + H_EPS;
                    player.setVelocityX(0f);
                    return;
                }
            }

            // compute previous horizontal relation (original code kept as fallback)
            // collided from left?
            if (prevRight <= platform.x + H_EPS && (p.x + p.width) > platform.x + H_EPS) {
                // push player to left side of platform
                p.x = platform.x - p.width - H_EPS;
                // stop horizontal velocity
                player.setVelocityX(0f);
                return;
            }
            // collided from right?
            if (prevLeft >= platform.x + platform.width - H_EPS && p.x < platform.x + platform.width - H_EPS) {
                p.x = platform.x + platform.width + H_EPS;
                player.setVelocityX(0f);
                return;
            }
            
            // Additional check: if already overlapping, push out to nearest side
            float overlapLeft = (p.x + p.width) - platform.x;
            float overlapRight = (platform.x + platform.width) - p.x;
            if (overlapLeft > 0 && overlapRight > 0) {
                // Push to whichever side is closer
                if (overlapLeft < overlapRight) {
                    // Push left
                    p.x = platform.x - p.width - H_EPS;
                    player.setVelocityX(0f);
                    return;
                } else {
                    // Push right
                    p.x = platform.x + platform.width + H_EPS;
                    player.setVelocityX(0f);
                    return;
                }
            }
        }
    }

    /**
     * Renders all level elements
     * @param shapeRenderer The renderer to use
     * @param batch Sprite batch for text rendering
     * @param font Font for text rendering
     */
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        // === PHASE 1: Draw background (SpriteBatch) ===
        if (backgroundTex != null) {
            batch.begin();
            if (currentLevel instanceof BackgroundedLevel) {
                ((BackgroundedLevel) currentLevel).renderBackground(batch, backgroundTex);
            } else {
                batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
            batch.end();  // *** CRITICAL: END BATCH BEFORE SHAPES ***
            // Render shared shredder visual on top of background if present
            if (sharedShredder != null) {
                batch.begin();
                try { sharedShredder.update(Gdx.graphics.getDeltaTime()); } catch (Exception ignored) {}
                try { sharedShredder.render(batch); } catch (Exception ignored) {}
                batch.end();
            }
        }

        // === PHASE 2: Draw platforms/obstacles/beams (ShapeRenderer) ===
        // ensure alpha blending is enabled so any transparent draws are respected
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Platforms are intentionally not rendered (invisible platforms)
        // They remain in `platforms` for collision detection but are not drawn.
        // If you want to debug them, set debugPlatformRender to true.
        boolean debugPlatformRender = true;              // set to true to visualize platforms
        if (debugPlatformRender) {
            shapeRenderer.setColor(153f/255f, 170f/255f, 187f/255f, 1f);
            for (Rectangle platform : platforms) {
                shapeRenderer.rect(platform.x, platform.y, platform.width, platform.height);
            }
        }

        // Obstacles (red)
        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1);
        for (Rectangle obstacle : obstacles) {
            shapeRenderer.rect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        }

        // Auditor beams (yellow semi-transparent)
        shapeRenderer.setColor(1, 1, 0, 0.5f);
        for (Rectangle beam : auditorBeams) {
            shapeRenderer.rect(beam.x, beam.y, beam.width, beam.height);
        }

        // Shredder (collision rect) ΓÇö keep invisible so the sprite/animation shows through
        if (shredder != null) {
            // Use same RGB but zero alpha so it's not visible
            if (documentsCollected >= totalDocuments) {
                shapeRenderer.setColor(0f, 1f, 0f, 0f); // green but transparent
            } else {
                shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0f); // gray but transparent
            }
            shapeRenderer.rect(shredder.x, shredder.y, shredder.width, shredder.height);
            // restore shape color to opaque white for subsequent draws
            shapeRenderer.setColor(1f, 1f, 1f, 1f);
        }

        shapeRenderer.end();  // *** END SHAPES ***

        // === PHASE 3: Draw documents (SpriteBatch) ===
        if (documents.size > 0) {
            batch.begin();
            int docNum = 1;
            // increase drawn size slightly so sprite padding looks right on-screen
            float drawDocSize = DOCUMENT_SIZE * 2.0f;

            // advance animation time once per render frame
            documentAnimTime += Gdx.graphics.getDeltaTime();

            for (Rectangle doc : documents) {
                // small per-document phase offset so they aren't perfectly in-sync
                float phaseOffset = (docNum - 1) * 0.06f;

                if (documentAnim != null) {
                    TextureRegion frame = documentAnim.getKeyFrame(documentAnimTime + phaseOffset, true);
                    batch.draw(frame, doc.x, doc.y, drawDocSize, drawDocSize);
                } else if (documentFrames != null && documentFrames.length > 0) {
                    int idx = (docNum - 1) % documentFrames.length; // fallback cycling
                    batch.draw(documentFrames[idx], doc.x, doc.y, drawDocSize, drawDocSize);
                } else if (documentSheetTex != null) {
                    batch.draw(documentSheetTex, doc.x, doc.y, drawDocSize, drawDocSize);
                }

                if (font != null) {
                    font.draw(batch, "D" + docNum, doc.x + 5, doc.y + drawDocSize + 15);
                }
                docNum++;
            }
            batch.end();  // *** END BATCH ***
        }
    }

    /**
     * Resets the level to initial state
     */
    public void reset() {
        initializeLevel();
    }

    // NEW: field to hold current level instance (for callbacks)
    private Level currentLevel;

    // NEW: load a specific level's data into the manager
    @Override
    public void loadLevel(Level level) {
        if (level == null) return;

        // dispose previous background if any
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }

        level.init();
        currentLevel = level;  // STORE FOR CALLBACKS
        documents.clear(); documents.addAll(level.getDocuments());
        platforms.clear(); platforms.addAll(level.getPlatforms());
        obstacles.clear(); obstacles.addAll(level.getObstacles());
        auditorBeams.clear(); auditorBeams.addAll(level.getAuditorBeams());

        // Use helper that returns the level's shredder rect (or null if level has none)
        shredder = getLevelShredder(level);

        // If the level supplies no shredder rect, remove the shared shredder visual so
        // nothing is drawn. Otherwise create or update the shared shredder instance.
        try {
            if (shredder == null) {
                if (sharedShredder != null) {
                    try { sharedShredder.dispose(); } catch (Exception ignored) {}
                    sharedShredder = null;
                }
            } else {
                if (sharedShredder == null) sharedShredder = new Shredder(shredder);
                else sharedShredder.setRect(shredder);
                sharedShredder.setFrameDuration(0.08f);
                // attempt to load visuals if not loaded yet
                if (!sharedShredder.hasVisual()) {
                    try { sharedShredder.loadFromFolder("shredderFx", 9); } catch (Exception ignored) {}
                }
                    // If the level defines a `shredderVisual` field, point it at the sharedShredder
                    try {
                        java.lang.reflect.Field f = level.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object curr = f.get(level);
                        if (curr == null || curr != sharedShredder) {
                            try { f.set(level, sharedShredder); } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        totalDocuments = level.getTotalDocuments();
        documentsCollected = 0;
        levelComplete = false;

        // If the level supplies a background path (via BackgroundedLevel), try to load it.
        try {
            if (level instanceof BackgroundedLevel) {
                String bg = ((BackgroundedLevel) level).getBackgroundPath();
                if (bg != null && !bg.isEmpty()) {
                    String[] bgCandidates = new String[] { bg, bg.toLowerCase(), "assets/" + bg };
                    for (String c : bgCandidates) {
                        if (c == null) continue;
                        if (Gdx.files.internal(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.internal(c));
                            Gdx.app.log("LevelManager", "Loaded background: " + c);
                            break;
                        }
                        if (Gdx.files.absolute(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.absolute(c));
                            Gdx.app.log("LevelManager", "Loaded background (absolute): " + c);
                            break;
                        }
                    }
                    if (backgroundTex == null) {
                        Gdx.app.log("LevelManager", "Background not found for path: " + bg);
                    }
                }
            } else {
                backgroundTex = null;
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager", "Error loading background", e);
            if (backgroundTex != null) { backgroundTex.dispose(); backgroundTex = null; }
        }
    }

    /**
     * Load a level but preserve the existing documents and document progress.
     * Useful for swapping between sub-areas that share the same overall objective count.
     */
    public void loadLevelPreserveDocuments(Level level) {
        if (level == null) return;

        // do not dispose backgroundTex here; we'll replace it below if needed

        level.init();
        currentLevel = level;

        // Preserve existing documents but ADD any new documents from the incoming level
        // Replace platforms/obstacles/beams so collisions match the new area
        platforms.clear(); platforms.addAll(level.getPlatforms());
        obstacles.clear(); obstacles.addAll(level.getObstacles());
        auditorBeams.clear(); auditorBeams.addAll(level.getAuditorBeams());

        // Merge document lists so the total requirement is the union (sum across areas)
        try {
            Array<Rectangle> incoming = level.getDocuments();
            if (incoming != null) {
                for (Rectangle r : incoming) {
                    boolean dup = false;
                    for (Rectangle exist : documents) {
                        // consider duplicate if centers are within 6px
                        float dx = (exist.x + exist.width/2f) - (r.x + r.width/2f);
                        float dy = (exist.y + exist.height/2f) - (r.y + r.height/2f);
                        if (Math.abs(dx) < 6f && Math.abs(dy) < 6f) { dup = true; break; }
                    }
                    if (!dup) documents.add(new Rectangle(r));
                }
            }
        } catch (Exception ignored) {}

        // Update shredder rect; if the incoming level has no shredder, dispose/hide the shared visual.
        shredder = getLevelShredder(level);
        try {
            if (shredder == null) {
                if (sharedShredder != null) {
                    try { sharedShredder.dispose(); } catch (Exception ignored) {}
                    sharedShredder = null;
                }
            } else {
                if (sharedShredder == null) sharedShredder = new Shredder(shredder);
                else sharedShredder.setRect(shredder);
                sharedShredder.setFrameDuration(0.08f);
                if (!sharedShredder.hasVisual()) {
                    try { sharedShredder.loadFromFolder("shredderFx", 9); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        // Recompute totalDocuments as the unioned set
        totalDocuments = documents.size;
        Gdx.app.log("LevelManager", "Loaded level (preserve docs): " + level.getClass().getSimpleName() + " totalDocs=" + totalDocuments);

        // Try to load background for new level
        try {
            if (level instanceof BackgroundedLevel) {
                String bg = ((BackgroundedLevel) level).getBackgroundPath();
                if (bg != null && !bg.isEmpty()) {
                    // dispose previous texture
                    if (backgroundTex != null) { backgroundTex.dispose(); backgroundTex = null; }
                    String[] bgCandidates = new String[] { bg, bg.toLowerCase(), "assets/" + bg };
                    for (String c : bgCandidates) {
                        if (c == null) continue;
                        if (Gdx.files.internal(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.internal(c));
                            Gdx.app.log("LevelManager", "Loaded background: " + c);
                            break;
                        }
                        if (Gdx.files.absolute(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.absolute(c));
                            Gdx.app.log("LevelManager", "Loaded background (absolute): " + c);
                            break;
                        }
                    }
                }
            } else {
                backgroundTex = null;
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager", "Error loading background (preserve)", e);
            if (backgroundTex != null) { backgroundTex.dispose(); backgroundTex = null; }
        }
    }

    @Override
    public Shredder getSharedShredder() { 
        // Return the shared shredder instance if present
        return sharedShredder;
    }

    // Expose current level for debugging/inspection
    public Level getCurrentLevel() { return currentLevel; }

    // Getters
    @Override
    public int getDocumentsCollected() { return documentsCollected; }
    @Override
    public int getTotalDocuments() { return totalDocuments; }
    @Override
    public boolean isLevelComplete() { return levelComplete; }
    @Override
    public int getDocumentsRemaining() { return totalDocuments - documentsCollected; }

    // Return copies or an unmodifiable list of collision/doc rectangles for debugging
    public java.util.List<Rectangle> getAllDebugRects() {
        java.util.List<Rectangle> out = new java.util.ArrayList<Rectangle>();
        // example: add platform rects, doc rects, obstacle rects, beam rects
        // for (Platform p : platforms) out.add(new Rectangle(p.x, p.y, p.width, p.height));
        // for (Document d : documents) out.add(new Rectangle(d.x, d.y, d.width, d.height));
        // ...
        return out;
    }

    // NEW: dispose document texture when level manager disposed
    public void dispose() {
        if (documentSheetTex != null) {
            documentSheetTex.dispose();
            documentSheetTex = null;
        }
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }
        if (sharedShredder != null) {
            try { sharedShredder.dispose(); } catch (Exception ignored) {}
            sharedShredder = null;
        }
    }

    // Add a small utility to safely test overlap
    private boolean rectsOverlap(Rectangle a, Rectangle b) {
        return a != null && b != null && a.overlaps(b);
    }

    // Add a helper to retrieve the shredder rect from the level (with fallback)
    private Rectangle getLevelShredder(Level level) {
        // If no level provided, return manager's default shredder rect
        if (level == null) return this.shredder;

        // Ask the level for an explicit shredder rectangle; if it returns null,
        // treat that as "this level has no shredder" and return null.
        try {
            Rectangle r = level.getShredder();
            if (r != null) return r;
        } catch (Exception ignored) {}

        // try to call a non-standard public method used by some levels
        try {
            java.lang.reflect.Method m = level.getClass().getMethod("getShredderCollisionRect");
            Object o = m.invoke(level);
            if (o instanceof Rectangle) return (Rectangle) o;
        } catch (Exception ignored) {}

        // If the level provides no shredder, return null (do not fall back to manager's default)
        return null;
    }

    public void update(float dt, Fixer player, Level level) {
        // run the existing update logic (document collection, obstacles, beams, platform collisions)
        // The single-arg update(...) handles those and returns time penalty which we can ignore here.
        try {
            update(dt, player);
        } catch (StackOverflowError e) {
            // defensive: should never happen, but avoid crash if overload resolution changed
        }

        // Determine shredder rect to test against: prefer level-provided rect, fallback to manager's shredder
        Rectangle playerBounds = player != null ? player.getBounds() : null;
        Rectangle shredderRect = getLevelShredder(level);

        // Only mark level complete when all documents were collected and the player overlaps the shredder
        if (playerBounds != null && documentsCollected >= totalDocuments && rectsOverlap(playerBounds, shredderRect)) {
            if (!levelComplete) {
                levelComplete = true;
                Gdx.app.log("LevelManager", "LEVEL COMPLETE! All documents shredded!");
                try {
                    if (levelCompleteListener != null) levelCompleteListener.onLevelComplete();
                } catch (Exception ignored) {}
            }
        }
    }

    public void addDocuments(int length) {
        // simple fallback: add `length` documents spaced horizontally on the ground
        if (length <= 0) return;
        float startX = 100f;
        float y = PLATFORM_HEIGHT + 5f;
        float gap = DOCUMENT_SIZE + 20f;
        for (int i = 0; i < length; i++) {
            documents.add(new Rectangle(startX + i * gap, y, DOCUMENT_SIZE, DOCUMENT_SIZE));
        }
        totalDocuments = documents.size;
        Gdx.app.log("LevelManager", "addDocuments: added " + length + " docs, total=" + totalDocuments);
    }

    // NEW: add documents at explicit X/Y positions (called by GameScreen.TUTORIAL_DOC_POSITIONS)
    public void addDocumentsAtPositions(float[][] positions) {
        if (positions == null) return;
        int added = 0;
        for (float[] pos : positions) {
            if (pos == null || pos.length < 2) continue;
            documents.add(new Rectangle(pos[0], pos[1], DOCUMENT_SIZE, DOCUMENT_SIZE));
            added++;
        }
        totalDocuments = documents.size;
        Gdx.app.log("LevelManager", "addDocumentsAtPositions: added " + added + " docs, total=" + totalDocuments);
    }
}
