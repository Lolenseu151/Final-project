package com.ctrl_s.game.levels;

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
import com.badlogic.gdx.utils.Array;
import com.ctrl_s.game.entities.Player;
import com.ctrl_s.game.entities.JSObstacle;

import com.ctrl_s.game.objects.SlopedPlatform;
import com.ctrl_s.game.levels.levels.Level;

/**
 * LevelManager - Manages all level elements including obstacles, documents, and
 * shredders
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

    /** Listener notified when a document is collected */
    public interface DocumentCollectedListener {
        void onDocumentCollected(int collected, int total);
    }

    private LevelCompleteListener levelCompleteListener = null;
    private ShredStartListener shredStartListener = null;
    private DocumentCollectedListener documentCollectedListener = null;

    private Level currentLevel; // track current level

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public void addDocuments(int count) {
        this.totalDocuments += count;
    }

    /**
     * Register a listener to be notified when the level completes (shredding
     * finished).
     */
    public void setLevelCompleteListener(LevelCompleteListener l) {
        this.levelCompleteListener = l;
    }

    /**
     * Register a listener to be notified when shredding begins (visual ACTIVE
     * state).
     */
    public void setShredStartListener(ShredStartListener l) {
        this.shredStartListener = l;
    }

    public void setDocumentCollectedListener(DocumentCollectedListener l) {
        this.documentCollectedListener = l;
    }

    // Level elements
    private final Array<Rectangle> documents; // Incriminating documents to collect
    private final Array<Rectangle> obstacles; // Red Tape obstacles (slow player)
    private final Array<Rectangle> lasers; // Lasers (visual beams you can control)
    private final Array<Rectangle> platforms; // Platforms/floors for player to stand on
    private final Array<SlopedPlatform> slopedPlatforms = new Array<>();
    private Rectangle shredder; // The shredder (win condition)

    // Level state
    private int documentsCollected;
    private int totalDocuments;
    private boolean levelComplete;
    // Shred delay state: when player reaches shredder with all docs, wait before
    // completing
    private boolean shredPending = false;
    private float shredTimer = 0f;
    private static final float SHRED_DELAY_SECONDS = 3.0f;
    // Laser hit cooldown to avoid repeated penalties while overlapping a beam
    private float laserHitCooldown = 0f;

    // Visual properties
    private static final float DOCUMENT_SIZE = 36f;

    // NEW: texture for document visuals (spritesheet)
    private Texture documentSheetTex;
    private TextureRegion[] documentFrames;
    // NEW: animation for documents
    private Animation<TextureRegion> documentAnim;
    private float documentAnimTime = 0f;
    // NEW: per-level background texture (used by loadLevel)
    private Texture backgroundTex;
    // NEW: shared shredder visual instance across levels (for Level3/Level3_1)
    private LevelShredder sharedShredder; // Type update to LevelShredder

    /**
     * Creates a new Level Manager and initializes the level
     */
    public LevelManager() {
        this.documents = new Array<>();
        this.obstacles = new Array<>();
        this.lasers = new Array<>();
        this.platforms = new Array<>();
        this.documentsCollected = 0;
        this.levelComplete = false;

        // load documents spritesheet safely, fallback placeholder
        boolean loaded = false;
        try {
            documentSheetTex = new Texture(Gdx.files.internal("assets/sprites/items/documents.png"));
            Gdx.app.log("LevelManager", "Loaded document spritesheet: assets/sprites/items/documents.png");
            loaded = true;
        } catch (Exception e) {
            loaded = false;
        }
        if (!loaded) {
            Pixmap pm = new Pixmap((int) DOCUMENT_SIZE, (int) DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
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
        documents.clear();
        obstacles.clear();
        lasers.clear();
        platforms.clear();

        // These will be populated by loadLevel usually

        totalDocuments = documents.size;
        documentsCollected = 0;
        levelComplete = false;
    }

    @Override
    public void loadLevel(Level level) {
        if (level == null)
            return;
        this.currentLevel = level;
        level.init();

        // Clear previous
        documents.clear();
        obstacles.clear();
        lasers.clear();
        platforms.clear();
        slopedPlatforms.clear();

        // Populate
        if (level.getPlatforms() != null)
            platforms.addAll(level.getPlatforms());
        if (level.getDocuments() != null)
            documents.addAll(level.getDocuments());
        if (level.getObstacles() != null)
            obstacles.addAll(level.getObstacles());
        if (level.getLasers() != null)
            lasers.addAll(level.getLasers());

        this.shredder = level.getShredder();
        this.totalDocuments = documents.size;
        this.documentsCollected = 0;
        this.levelComplete = false;

        // Load background texture
        String bgPath = level.getBackgroundPath();
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }
        if (bgPath != null) {
            try {
                backgroundTex = new Texture(Gdx.files.internal(bgPath));
                if (backgroundTex != null)
                    backgroundTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                Gdx.app.error("LevelManager", "Failed to load background: " + bgPath);
            }
        }
    }

    /**
     * Updates the level (checks collisions, etc.)
     * 
     * @param deltaTime Time since last update
     * @param player    The Fixer (player) to check collisions against
     * @return Time penalty (in seconds) if any beams were touched
     */
    @Override
    public float update(float deltaTime, Player player) {
        float timePenalty = 0f;

        // Update background state if level provides it
        if (currentLevel instanceof BackgroundedLevel) {
            ((BackgroundedLevel) currentLevel).updateBackground(deltaTime, this, documents, obstacles, player);
        }

        // Update any JS obstacle AI (movement, sight checks)
        try {
            JSObstacle.updateAll(deltaTime, player);
        } catch (Exception ignored) {
        }

        // Hard clamp player position to prevent going through walls at screen edges
        if (player != null && player.getBounds() != null) {
            Rectangle pb = player.getBounds();
            // Find leftmost and rightmost walls
            float minAllowedX = 0f;
            float maxAllowedX = Gdx.graphics.getWidth() - pb.width;

            for (Rectangle platform : platforms) {
                if (platform.width < 100f) {
                    // Left wall (near left edge)
                    if (platform.x < 50) {
                        minAllowedX = Math.max(minAllowedX, platform.x + platform.width);
                    }
                    // Right wall (near right edge)
                    if (platform.x > Gdx.graphics.getWidth() - 200) {
                        maxAllowedX = Math.min(maxAllowedX, platform.x - pb.width);
                    }
                    // Interior narrow walls - check collision and push player out
                    if (platform.x >= 50 && platform.x <= Gdx.graphics.getWidth() - 200) {
                        if (pb.x + pb.width > platform.x && pb.x < platform.x + platform.width) {
                            if (pb.y < platform.y + platform.height && pb.y + pb.height > platform.y) {
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
                try {
                    if (documentCollectedListener != null) {
                        try {
                            documentCollectedListener.onDocumentCollected(documentsCollected, totalDocuments);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }

                // If we have collected all documents, attempt to set shredder visual to READY
                if (currentLevel != null && documentsCollected >= totalDocuments) {
                    try {
                        // Reflectively check for shredder visual (assumed specific to level
                        // implementation)
                        java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(currentLevel);
                        if (sv instanceof LevelShredder) {
                            ((LevelShredder) sv).setReady();
                        }
                    } catch (Exception ignored) {
                    }
                }
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

        // Laser collision: if overlapping any laser beam, apply a time penalty and slow
        // effect.
        if (laserHitCooldown > 0f) {
            laserHitCooldown -= deltaTime;
        }
        if (lasers != null && !lasers.isEmpty() && laserHitCooldown <= 0f) {
            for (Rectangle laser : lasers) {
                if (player != null && player.getBounds() != null && laser != null
                        && player.getBounds().overlaps(laser)) {
                    // apply 10 seconds penalty and slow player for 5 seconds
                    timePenalty += 10f;
                    try {
                        player.applySlow(5.0f);
                    } catch (Exception ignored) {
                    }
                    laserHitCooldown = 1.0f; // 1 second cooldown to avoid flood
                    Gdx.app.log("LevelManager", "Player hit laser: -10s penalty and slowed 5s");
                    break;
                }
            }
        }

        // Check if level is complete (all documents collected + reached shredder)
        if (documentsCollected >= totalDocuments && shredder != null && player.getBounds().overlaps(shredder)) {
            if (!levelComplete && !shredPending) {
                shredPending = true;
                shredTimer = 0f;
                Gdx.app.log("LevelManager",
                        "Shredding sequence started - delaying completion for " + SHRED_DELAY_SECONDS + "s");
                // Notify any registered listener that shredding has started
                try {
                    if (shredStartListener != null) {
                        try {
                            shredStartListener.onShredStart();
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }

                // Try to set shredder visual to ACTIVE
                try {
                    if (currentLevel != null) {
                        java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(currentLevel);
                        if (sv instanceof LevelShredder) {
                            ((LevelShredder) sv).setActive();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // If a shred sequence is pending, advance its timer and complete the level when
        // elapsed
        if (shredPending && !levelComplete) {
            shredTimer += deltaTime;
            if (shredTimer >= SHRED_DELAY_SECONDS) {
                levelComplete = true;
                shredPending = false;
                Gdx.app.log("LevelManager", "LEVEL COMPLETE! All documents shredded! (after delay)");
                // Optionally set shredder back to idle
                try {
                    if (currentLevel != null) {
                        java.lang.reflect.Field f = currentLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(currentLevel);
                        if (sv instanceof LevelShredder) {
                            ((LevelShredder) sv).setIdle();
                        }
                    }
                } catch (Exception ignored) {
                }
                // Notify listener (if any) that the level has completed so UI can react
                // immediately
                try {
                    if (levelCompleteListener != null)
                        levelCompleteListener.onLevelComplete();
                } catch (Exception ignored) {
                }
            }
        }

        return timePenalty;
    }

    /**
     * Checks if the player is colliding with any platforms and handles landing
     */
    private void checkPlatformCollisions(Player player, float deltaTime) {
        Rectangle p = player.getBounds();
        player.setOnGround(false);
        // Use previous-position checks to avoid jitter when hitting platform
        // tops/undersides.
        float vy = player.getVelocity().y;
        float vx = player.getVelocity().x;
        float prevY = p.y - vy * deltaTime; // approximate previous bottom
        float prevTop = prevY + p.height;
        float prevBottom = prevY;
        final float EPS = 0.6f; // small offset to prevent sticking
        final float MIN_HORIZONTAL_OVERLAP = Math.max(6f, p.width * 0.25f);

        // First, check sloped platforms (land on slope if crossing top)
        try {
            for (SlopedPlatform slope : slopedPlatforms) {
                if (slope == null)
                    continue;
                // use player's center X to sample the slope
                float cx = p.x + p.width * 0.5f;
                if (!slope.containsX(cx))
                    continue;
                float slopeY = slope.getYAt(cx);
                // if we were above slope and now intersect it while moving down, land
                if (vy <= 0f && prevBottom >= slopeY - EPS && p.y <= slopeY + EPS) {
                    p.y = slopeY;
                    player.setVelocityY(0f);
                    player.setOnGround(true);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform))
                continue;

            // compute horizontal overlap amount
            float left = Math.max(p.x, platform.x);
            float right = Math.min(p.x + p.width, platform.x + platform.width);
            float overlapX = right - left;

            // If not enough horizontal overlap, treat as side contact and ignore for
            // vertical landing
            if (overlapX < MIN_HORIZONTAL_OVERLAP)
                continue;

            // If moving down (vy <= 0): check if we crossed the platform top this frame ->
            // land
            if (vy <= 0f) {
                // previous bottom was at or above platform top (standing above) -> landed this
                // frame
                if (prevBottom >= platform.y + platform.height - EPS || prevTop > platform.y + platform.height) {
                    p.y = platform.y + platform.height;
                    player.setVelocityY(0f);
                    player.setOnGround(true);
                    return;
                }
            } else {
                // moving up: check if we came from below and pierced into platform this frame
                // -> block underside
                if (prevTop <= platform.y + EPS && (p.y + p.height) > platform.y + EPS) {
                    p.y = platform.y - p.height - EPS;
                    player.setVelocityY(0f);
                    player.setOnGround(false);
                    return;
                }
            }
        }

        // If no vertical collision resolved, check horizontal collisions to prevent
        // passing through sides
        float prevX = p.x - vx * deltaTime;
        float prevLeft = prevX;
        float prevRight = prevX + p.width;
        final float H_EPS = 0.6f;

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform))
                continue;

            // compute vertical overlap to ensure we're not resolving vertical case here
            float top = Math.min(p.y + p.height, platform.y + platform.height);
            float bottom = Math.max(p.y, platform.y);
            float overlapY = top - bottom;
            if (overlapY <= 0f)
                continue;

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

            // Push out mechanism (fallback)
            float overlapLeft = (p.x + p.width) - platform.x;
            float overlapRight = (platform.x + platform.width) - p.x;
            if (overlapLeft > 0 && overlapRight > 0) {
                if (overlapLeft < overlapRight) {
                    p.x = platform.x - p.width - H_EPS;
                    player.setVelocityX(0f);
                    return;
                } else {
                    p.x = platform.x + platform.width + H_EPS;
                    player.setVelocityX(0f);
                    return;
                }
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        // Render background first
        if (backgroundTex != null) {
            batch.begin();
            if (currentLevel instanceof BackgroundedLevel) {
                ((BackgroundedLevel) currentLevel).renderBackground(batch, backgroundTex);
            } else {
                batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
            batch.end();

            // Shared shredder on top of background
            if (sharedShredder != null) {
                batch.begin();
                try {
                    sharedShredder.update(Gdx.graphics.getDeltaTime());
                } catch (Exception ignored) {
                }
                try {
                    sharedShredder.render(batch);
                } catch (Exception ignored) {
                }
                batch.end();
            }
        }

        // Render shapes (obstacles, lasers debug, shredder collision debug)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Obstacles (red) - hidden to avoid drawing over visuals
        // shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1);
        // for (Rectangle obstacle : obstacles) {
        // shapeRenderer.rect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        // }

        // Shredder collision rect (debug/invisible)
        if (shredder != null) {
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0f);
            shapeRenderer.rect(shredder.x, shredder.y, shredder.width, shredder.height);
            shapeRenderer.setColor(1f, 1f, 1f, 1f);
        }

        shapeRenderer.end();

        // JS Obstacles
        try {
            JSObstacle.renderAll(batch);
        } catch (Exception ignored) {
        }

        // Documents
        if (documents.size > 0) {
            batch.begin();
            int docNum = 1;
            float drawDocSize = DOCUMENT_SIZE * 2.0f;
            documentAnimTime += Gdx.graphics.getDeltaTime();

            for (Rectangle doc : documents) {
                float phaseOffset = (docNum - 1) * 0.06f;
                // Draw doc
                if (documentAnim != null) {
                    batch.draw(documentAnim.getKeyFrame(documentAnimTime + phaseOffset, true), doc.x, doc.y,
                            drawDocSize, drawDocSize);
                } else if (documentSheetTex != null) {
                    batch.draw(documentSheetTex, doc.x, doc.y, drawDocSize, drawDocSize);
                }

                // number
                if (font != null) {
                    font.draw(batch, "D" + docNum, doc.x + 5, doc.y + drawDocSize + 15);
                }
                docNum++;
            }
            batch.end();
        }
    }

    @Override
    public boolean isLevelComplete() {
        return levelComplete;
    }

    @Override
    public int getDocumentsCollected() {
        return documentsCollected;
    }

    @Override
    public int getTotalDocuments() {
        return totalDocuments;
    }

    @Override
    public void dispose() {
        if (documentSheetTex != null)
            documentSheetTex.dispose();
        if (backgroundTex != null)
            backgroundTex.dispose();
    }
}
