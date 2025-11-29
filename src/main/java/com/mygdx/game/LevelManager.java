package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import java.util.List;
import java.util.ArrayList;

/**
 * LevelManager - Manages all level elements including obstacles, documents, and shredders
 * Responsible for level layout, collision detection, and objective tracking
 */
public class LevelManager {

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

    // Visual properties
    private static final float DOCUMENT_SIZE = 36f;
    private static final float OBSTACLE_WIDTH = 60f;
    private static final float OBSTACLE_HEIGHT = 10f;
    private static final float BEAM_WIDTH = 5f;
    private static final float SHREDDER_SIZE = 50f;
    private static final float PLATFORM_HEIGHT = 15f;

    // NEW: texture for document visuals
    private Texture documentTex;
    // NEW: per-level background texture (used by loadLevel)
    private Texture backgroundTex;

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

        // load documents.png safely (try internal then absolute and common asset path), fallback placeholder
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
                    documentTex = new Texture(Gdx.files.internal(c));
                    Gdx.app.log("LevelManager", "Loaded document texture (internal): " + c);
                    loaded = true;
                    break;
                }
                if (Gdx.files.absolute(c).exists()) {
                    documentTex = new Texture(Gdx.files.absolute(c));
                    Gdx.app.log("LevelManager", "Loaded document texture (absolute): " + c);
                    loaded = true;
                    break;
                }
            }
            if (!loaded) {
                Pixmap pm = new Pixmap((int)DOCUMENT_SIZE, (int)DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
                pm.setColor(1f, 1f, 1f, 1f);
                pm.fill();
                documentTex = new Texture(pm);
                pm.dispose();
                Gdx.app.log("LevelManager", "documents.png not found, using placeholder.");
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager", "Error loading documents.png", e);
            Pixmap pm = new Pixmap((int)DOCUMENT_SIZE, (int)DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 1f);
            pm.fill();
            documentTex = new Texture(pm);
            pm.dispose();
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
            }
        }

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
        if (documentsCollected >= totalDocuments && player.getBounds().overlaps(shredder)) {
            if (!levelComplete) {
                levelComplete = true;
                Gdx.app.log("LevelManager", "LEVEL COMPLETE! All documents shredded!");
            }
        }

        // === Screen-edge invisible walls: prevent the player from leaving left/right edges ===
        try {
            if (player != null && player.getBounds() != null) {
                float minX = 0f;
                float maxX = Gdx.graphics.getWidth() - player.getBounds().width;
                if (player.getBounds().x < minX) {
                    player.getBounds().x = minX;
                    player.setVelocityX(0f);
                } else if (player.getBounds().x > maxX) {
                    player.getBounds().x = maxX;
                    player.setVelocityX(0f);
                }
            }
        } catch (Exception ignored) {}

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

            // compute previous horizontal relation
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
        }

        // === PHASE 2: Draw platforms/obstacles/beams (ShapeRenderer) ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Platforms are intentionally not rendered (invisible platforms)
        // They remain in `platforms` for collision detection but are not drawn.
        // If you want to debug them, set debugPlatformRender to true.
        boolean debugPlatformRender = false;
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

        // Shredder (green if ready, gray otherwise)
        if (documentsCollected >= totalDocuments) {
            shapeRenderer.setColor(0, 1, 0, 1);
        } else {
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1);
        }
        shapeRenderer.rect(shredder.x, shredder.y, shredder.width, shredder.height);

        shapeRenderer.end();  // *** END SHAPES ***

        // === PHASE 3: Draw documents (SpriteBatch) ===
        if (documents.size > 0) {
            batch.begin();
            int docNum = 1;
            for (Rectangle doc : documents) {
                if (documentTex != null) {
                    batch.draw(documentTex, doc.x, doc.y, DOCUMENT_SIZE, DOCUMENT_SIZE);
                }
                if (font != null) {
                    font.draw(batch, "D" + docNum, doc.x + 5, doc.y + DOCUMENT_SIZE + 15);
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
        shredder = level.getShredder();
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

    // Getters
    public int getDocumentsCollected() { return documentsCollected; }
    public int getTotalDocuments() { return totalDocuments; }
    public boolean isLevelComplete() { return levelComplete; }
    public int getDocumentsRemaining() { return totalDocuments - documentsCollected; }

    // Return copies or an unmodifiable list of collision/doc rectangles for debugging
    public List<Rectangle> getAllDebugRects() {
        List<Rectangle> out = new ArrayList<>();
        // example: add platform rects, doc rects, obstacle rects, beam rects
        // for (Platform p : platforms) out.add(new Rectangle(p.x, p.y, p.width, p.height));
        // for (Document d : documents) out.add(new Rectangle(d.x, d.y, d.width, d.height));
        // ...
        return out;
    }

    // NEW: dispose document texture when level manager disposed
    public void dispose() {
        if (documentTex != null) {
            documentTex.dispose();
            documentTex = null;
        }
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }
    }
}
