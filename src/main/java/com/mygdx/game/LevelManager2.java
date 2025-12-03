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
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Levels.BackgroundedLevel;
import com.mygdx.game.Levels.Level;
import com.mygdx.game.Levels.Shredder;

public class LevelManager2 implements ILevelManager {
    // make active instance discoverable by levels (so we don't have to pass LevelManager2 into the old API)
    private static LevelManager2 ACTIVE = null;
    public static LevelManager2 getActive() { return ACTIVE; }

    // Combined level elements (merged from map A and map B)
    private final Array<Rectangle> documents;      // Incriminating documents to collect
    private final Array<Rectangle> obstacles;     // Red Tape obstacles (slow player)
    private final Array<Rectangle> lasers;         // Lasers (visual beams)
    private final Array<Rectangle> platforms;      // Platforms/floors for player to stand on
    private Rectangle shredder;                    // The shredder (win condition)

    // per-map references (for callbacks / visuals)
    private Level currentLevelA;
    private Level currentLevelB;

    // Level state
    private int documentsCollected;
    private int totalDocuments;
    private boolean levelComplete;
    // Document-collected callback (optional)
    public interface DocumentCollectedListener { void onDocumentCollected(int collected, int total); }
    private DocumentCollectedListener documentCollectedListener = null;
    private boolean shredPending = false;
    private float shredTimer = 0f;
    private static final float SHRED_DELAY_SECONDS = 3.0f;

    // Visual properties
    private static final float DOCUMENT_SIZE = 36f;
    private static final float OBSTACLE_WIDTH = 60f;
    private static final float OBSTACLE_HEIGHT = 10f;
    private static final float BEAM_WIDTH = 5f;
    private static final float SHREDDER_SIZE = 36f;
    private static final float PLATFORM_HEIGHT = 15f;

    // Document textures/animation (shared)
    private Texture documentSheetTex;
    private TextureRegion[] documentFrames;
    private Animation<TextureRegion> documentAnim;
    private float documentAnimTime = 0f;

    // Background for combined view (prefer B then A)
    private Texture backgroundTex;

    // Shared shredder visual
    private Shredder sharedShredder;
    
    // Laser visual effects
    private Array<LaserFX> laserFXList = new Array<>();

    public LevelManager2() {
        this.documents = new Array<>();
        this.obstacles = new Array<>();
        this.lasers = new Array<>();
        this.platforms = new Array<>();
        this.documentsCollected = 0;
        this.levelComplete = false;

        // load document spritesheet (same fallback strategy)
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
                    Gdx.app.log("LevelManager2", "Loaded document spritesheet (internal): " + c);
                    loaded = true;
                    break;
                }
                if (Gdx.files.absolute(c).exists()) {
                    documentSheetTex = new Texture(Gdx.files.absolute(c));
                    Gdx.app.log("LevelManager2", "Loaded document spritesheet (absolute): " + c);
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
                Gdx.app.log("LevelManager2", "documents.png not found, using placeholder.");
            }

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
            Gdx.app.error("LevelManager2", "Error loading documents spritesheet", e);
            Pixmap pm = new Pixmap((int)DOCUMENT_SIZE, (int)DOCUMENT_SIZE, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 1f, 1f, 0f);
            pm.fill();
            documentSheetTex = new Texture(pm);
            pm.dispose();
            documentFrames = new TextureRegion[] { new TextureRegion(documentSheetTex) };
        }

        if (documentSheetTex != null) {
            documentSheetTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        try {
            if (documentFrames != null && documentFrames.length > 0) {
                final float FRAME_DURATION = 0.24f;
                documentAnim = new Animation<TextureRegion>(FRAME_DURATION, documentFrames);
                documentAnim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
            } else if (documentSheetTex != null) {
                documentAnim = new Animation<TextureRegion>(0.5f, new TextureRegion(documentSheetTex));
                documentAnim.setPlayMode(Animation.PlayMode.LOOP);
            } else {
                documentAnim = null;
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager2", "Error creating document animation", e);
            documentAnim = null;
        }

        ACTIVE = this; // register active instance

        initializeLevel();
    }

    public void setDocumentCollectedListener(DocumentCollectedListener l) {
        this.documentCollectedListener = l;
    }

    private void initializeLevel() {
        documents.clear();
        obstacles.clear();
        platforms.clear();

        placePlatforms();
        placeDocuments();
        placeObstacles();
        placeShredder();

        totalDocuments = documents.size;
        documentsCollected = 0;
        levelComplete = false;

        Gdx.app.log("LevelManager2", String.format("Initialized combined level: docs=%d platforms=%d obs=%d",
            totalDocuments, platforms.size, obstacles.size));
    }

    private void placePlatforms() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        platforms.add(new Rectangle(0, 0, screenWidth, PLATFORM_HEIGHT));
        platforms.add(new Rectangle(0, 125, 700, PLATFORM_HEIGHT));
        platforms.add(new Rectangle(50, 250, 500, PLATFORM_HEIGHT));
        platforms.add(new Rectangle(screenWidth / 2 - 50, 350, 200, PLATFORM_HEIGHT));
        platforms.add(new Rectangle(screenWidth - 180, 410, 150, PLATFORM_HEIGHT));
        if (screenHeight > 500) {
            platforms.add(new Rectangle(screenWidth / 2 - 80, screenHeight - 120, 160, PLATFORM_HEIGHT));
        }
    }

    private void placeDocuments() {
        float screenWidth = Gdx.graphics.getWidth();

        documents.add(new Rectangle(150, PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(120, 150 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(screenWidth - 200, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(screenWidth / 2 - 50, 410 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(80, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(280, 280 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
        documents.add(new Rectangle(screenWidth - 250, 150 + PLATFORM_HEIGHT + 5, DOCUMENT_SIZE, DOCUMENT_SIZE));
    }

    private void placeObstacles() {
        float screenWidth = Gdx.graphics.getWidth();
        obstacles.add(new Rectangle(200, PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(400, PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(screenWidth - 280, 125 + PLATFORM_HEIGHT + 5, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(70, 250 + PLATFORM_HEIGHT + 5, 50, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(350, 250 + PLATFORM_HEIGHT + 5, 50, OBSTACLE_HEIGHT));
    }

    private void placeShredder() {
        float screenWidth = Gdx.graphics.getWidth();
        shredder = new Rectangle(screenWidth - 80, 10, SHREDDER_SIZE, SHREDDER_SIZE);
    }

    /**
     * Main update, same semantics as LevelManager.update
     */
    public float update(float deltaTime, Fixer player) {
        float timePenalty = 0f;

        // Update any JS obstacle AI (movement, sight checks)
        try { com.mygdx.game.JSObstacle.updateAll(deltaTime, player); } catch (Exception ignored) {}

        // Pass 'this' (ILevelManager) to updateBackground so levels can use either manager type
        try {
            if (currentLevelA instanceof BackgroundedLevel) {
                ((BackgroundedLevel) currentLevelA).updateBackground(deltaTime, this, this.documents, this.obstacles, player);
            }
        } catch (Exception ignored) {}

        try {
            if (currentLevelB instanceof BackgroundedLevel) {
                ((BackgroundedLevel) currentLevelB).updateBackground(deltaTime, this, this.documents, this.obstacles, player);
            }
        } catch (Exception ignored) {}

        // Hard clamp player position to prevent going through walls at screen edges
        if (player != null && player.getBounds() != null) {
            Rectangle pb = player.getBounds();
            float minAllowedX = 0f;
            float maxAllowedX = Gdx.graphics.getWidth() - pb.width;
            
            for (Rectangle platform : platforms) {
                if (platform.height > 20) {
                    if (platform.x < 50) {
                        minAllowedX = Math.max(minAllowedX, platform.x + platform.width);
                    }
                    if (platform.x > Gdx.graphics.getWidth() - 200) {
                        maxAllowedX = Math.min(maxAllowedX, platform.x - pb.width);
                    }
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

        checkPlatformCollisions(player, deltaTime);

        player.setSlowed(false);

        for (int i = documents.size - 1; i >= 0; i--) {
            Rectangle doc = documents.get(i);
            if (player.getBounds().overlaps(doc)) {
                documents.removeIndex(i);
                documentsCollected++;
                Gdx.app.log("LevelManager2", String.format("Document collected! (%d/%d)", 
                    documentsCollected, totalDocuments));
                try {
                    if (documentCollectedListener != null) {
                        try { documentCollectedListener.onDocumentCollected(documentsCollected, totalDocuments); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                try {
                    if (currentLevelA != null && currentLevelA.getClass().getSimpleName().toLowerCase().contains("tutorial") && documentsCollected == 1) {
                        try {
                            java.lang.reflect.Method m = currentLevelA.getClass().getMethod("setTalkingOverlayVisible", boolean.class);
                            m.invoke(currentLevelA, true);
                        } catch (Exception ignored) {}
                    }
                    if (currentLevelB != null && currentLevelB.getClass().getSimpleName().toLowerCase().contains("tutorial") && documentsCollected == 1) {
                        try {
                            java.lang.reflect.Method m = currentLevelB.getClass().getMethod("setTalkingOverlayVisible", boolean.class);
                            m.invoke(currentLevelB, true);
                        } catch (Exception ignored) {}
                    }

                    if (documentsCollected >= totalDocuments) {
                        // set shredder visual READY if present on either level (try B then A)
                        Level tryLevel = (currentLevelB != null) ? currentLevelB : currentLevelA;
                        if (tryLevel != null) {
                            try {
                                java.lang.reflect.Field f = tryLevel.getClass().getDeclaredField("shredderVisual");
                                f.setAccessible(true);
                                Object sv = f.get(tryLevel);
                                if (sv != null) {
                                    try {
                                        java.lang.reflect.Method m = sv.getClass().getMethod("setReady");
                                        m.invoke(sv);
                                    } catch (NoSuchMethodException ignored) {}
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // tutorial final overlay handling (try both)
        try {
            if (currentLevelA != null && currentLevelA.getClass().getSimpleName().toLowerCase().contains("tutorial") && documentsCollected >= 4) {
                try {
                    java.lang.reflect.Method isFinal = currentLevelA.getClass().getMethod("isFinalOverlayVisible");
                    java.lang.reflect.Method isConsumed = currentLevelA.getClass().getMethod("isFinalOverlayConsumed");
                    boolean vis = (boolean)isFinal.invoke(currentLevelA);
                    boolean cons = (boolean)isConsumed.invoke(currentLevelA);
                    if (!vis && !cons) {
                        java.lang.reflect.Method setFinal = currentLevelA.getClass().getMethod("setFinalOverlayVisible", boolean.class);
                        setFinal.invoke(currentLevelA, true);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try {
            if (currentLevelB != null && currentLevelB.getClass().getSimpleName().toLowerCase().contains("tutorial") && documentsCollected >= 4) {
                try {
                    java.lang.reflect.Method isFinal = currentLevelB.getClass().getMethod("isFinalOverlayVisible");
                    java.lang.reflect.Method isConsumed = currentLevelB.getClass().getMethod("isFinalOverlayConsumed");
                    boolean vis = (boolean)isFinal.invoke(currentLevelB);
                    boolean cons = (boolean)isConsumed.invoke(currentLevelB);
                    if (!vis && !cons) {
                        java.lang.reflect.Method setFinal = currentLevelB.getClass().getMethod("setFinalOverlayVisible", boolean.class);
                        setFinal.invoke(currentLevelB, true);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        boolean slowed = false;
        for (Rectangle obstacle : obstacles) {
            if (player.getBounds().overlaps(obstacle)) {
                slowed = true;
                Gdx.app.log("LevelManager2", "Hit Red Tape! Player slowed.");
                break;
            }
        }
        player.setSlowed(slowed);

        if (documentsCollected >= totalDocuments && rectsOverlap(player.getBounds(), shredder)) {
            if (!levelComplete && !shredPending) {
                shredPending = true;
                shredTimer = 0f;
                Gdx.app.log("LevelManager2", "Shredding sequence started — delaying completion for " + SHRED_DELAY_SECONDS + "s");
                // Try to set shredder visual to ACTIVE on B then A
                Level tryLevel = (currentLevelB != null) ? currentLevelB : currentLevelA;
                try {
                    if (tryLevel != null) {
                        java.lang.reflect.Field f = tryLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(tryLevel);
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

        if (shredPending && !levelComplete) {
            shredTimer += deltaTime;
            if (shredTimer >= SHRED_DELAY_SECONDS) {
                levelComplete = true;
                shredPending = false;
                Gdx.app.log("LevelManager2", "LEVEL COMPLETE! All documents shredded! (after delay)");
                Level tryLevel = (currentLevelB != null) ? currentLevelB : currentLevelA;
                try {
                    if (tryLevel != null) {
                        java.lang.reflect.Field f = tryLevel.getClass().getDeclaredField("shredderVisual");
                        f.setAccessible(true);
                        Object sv = f.get(tryLevel);
                        if (sv != null) {
                            try {
                                java.lang.reflect.Method m = sv.getClass().getMethod("setIdle");
                                m.invoke(sv);
                            } catch (NoSuchMethodException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

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

    private void checkPlatformCollisions(Fixer player, float deltaTime) {
        Rectangle p = player.getBounds();
        player.setOnGround(false);
        float vy = player.getVelocity().y;
        float vx = player.getVelocity().x;
        float prevY = p.y - vy * deltaTime;
        float prevTop = prevY + p.height;
        float prevBottom = prevY;
        final float EPS = 0.6f;
        final float MIN_HORIZONTAL_OVERLAP = Math.max(6f, p.width * 0.25f);

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform)) continue;
            float left = Math.max(p.x, platform.x);
            float right = Math.min(p.x + p.width, platform.x + platform.width);
            float overlapX = right - left;
            if (overlapX < MIN_HORIZONTAL_OVERLAP) continue;
            if (vy <= 0f) {
                if (prevBottom >= platform.y + platform.height - EPS || prevTop > platform.y + platform.height) {
                    p.y = platform.y + platform.height;
                    player.setVelocityY(0f);
                    player.setOnGround(true);
                    return;
                }
            } else {
                if (prevTop <= platform.y + EPS && (p.y + p.height) > platform.y + EPS) {
                    p.y = platform.y - p.height - EPS;
                    player.setVelocityY(0f);
                    player.setOnGround(false);
                    return;
                }
            }
        }

        float prevX = p.x - vx * deltaTime;
        float prevLeft = prevX;
        float prevRight = prevX + p.width;
        final float H_EPS = 0.6f;

        for (Rectangle platform : platforms) {
            if (!p.overlaps(platform)) continue;
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
            
            if (prevRight <= platform.x + H_EPS && (p.x + p.width) > platform.x + H_EPS) {
                p.x = platform.x - p.width - H_EPS;
                player.setVelocityX(0f);
                return;
            }
            if (prevLeft >= platform.x + platform.width - H_EPS && p.x < platform.x + platform.width - H_EPS) {
                p.x = platform.x + platform.width + H_EPS;
                player.setVelocityX(0f);
                return;
            }
            
            // Additional check: if already overlapping, push out to nearest side
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
        if (backgroundTex != null) {
            batch.begin();
            // Try B then A for background rendering hook
            try {
                if (currentLevelB instanceof BackgroundedLevel) {
                    ((BackgroundedLevel) currentLevelB).renderBackground(batch, backgroundTex);
                } else if (currentLevelA instanceof BackgroundedLevel) {
                    ((BackgroundedLevel) currentLevelA).renderBackground(batch, backgroundTex);
                } else {
                    batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                }
            } catch (Exception e) {
                batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
            batch.end();

            if (sharedShredder != null) {
                // Don't render shredder for Level3 until it transitions to Level3_1
                boolean isLevel3Only = (currentLevelA != null && 
                                       currentLevelA.getClass().getSimpleName().equals("Level3") && 
                                       currentLevelB == null);
                // Don't render shredder in Level5_1 (only show in Level5 map)
                boolean isLevel5_1 = (currentLevelB != null && 
                                     currentLevelB.getClass().getSimpleName().equals("Level5_1"));
                
                if (!isLevel3Only && !isLevel5_1) {
                    batch.begin();
                    try { sharedShredder.update(Gdx.graphics.getDeltaTime()); } catch (Exception ignored) {}
                    try { sharedShredder.render(batch); } catch (Exception ignored) {}
                    batch.end();
                }
            }
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        boolean debugPlatformRender = false;
        if (debugPlatformRender) {
            shapeRenderer.setColor(153f/255f, 170f/255f, 187f/255f, 1f);
            for (Rectangle platform : platforms) {
                shapeRenderer.rect(platform.x, platform.y, platform.width, platform.height);
            }
        }

        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1);
        for (Rectangle obstacle : obstacles) {
            // skip drawing rectangles that correspond to a visual JS obstacle
            try {
                if (com.mygdx.game.JSObstacle.isRegisteredRect(obstacle)) continue;
            } catch (Exception ignored) {}
            shapeRenderer.rect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        }

        if (shredder != null) {
            // Don't render shredder for Level3 until it transitions to Level3_1
            boolean isLevel3Only = (currentLevelA != null && 
                                   currentLevelA.getClass().getSimpleName().equals("Level3") && 
                                   currentLevelB == null);
            // Don't render shredder collision box in Level5_1 (only show in Level5 map)
            boolean isLevel5_1 = (currentLevelB != null && 
                                 currentLevelB.getClass().getSimpleName().equals("Level5_1"));
            
            if (!isLevel3Only && !isLevel5_1) {
                if (documentsCollected >= totalDocuments) {
                    shapeRenderer.setColor(0f, 1f, 0f, 0f);
                } else {
                    shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 0f);
                }
                shapeRenderer.rect(shredder.x, shredder.y, shredder.width, shredder.height);
                shapeRenderer.setColor(1f, 1f, 1f, 1f);
            }
        }

        shapeRenderer.end();

        // Render any JS obstacle visuals registered by levels (e.g., KMJS sprites)
        try { com.mygdx.game.JSObstacle.renderAll(batch); } catch (Exception ignored) {}

        if (documents.size > 0) {
            batch.begin();
            int docNum = 1;
            float drawDocSize = DOCUMENT_SIZE * 2.0f;
            documentAnimTime += Gdx.graphics.getDeltaTime();

            for (Rectangle doc : documents) {
                float phaseOffset = (docNum - 1) * 0.06f;
                if (documentAnim != null) {
                    TextureRegion frame = documentAnim.getKeyFrame(documentAnimTime + phaseOffset, true);
                    batch.draw(frame, doc.x, doc.y, drawDocSize, drawDocSize);
                } else if (documentFrames != null && documentFrames.length > 0) {
                    int idx = (docNum - 1) % documentFrames.length;
                    batch.draw(documentFrames[idx], doc.x, doc.y, drawDocSize, drawDocSize);
                } else if (documentSheetTex != null) {
                    batch.draw(documentSheetTex, doc.x, doc.y, drawDocSize, drawDocSize);
                }

                if (font != null) {
                    font.draw(batch, "D" + docNum, doc.x + 5, doc.y + drawDocSize + 15);
                }
                docNum++;
            }
            batch.end();
        }
        
        // Render animated lasers
        if (laserFXList.size > 0) {
            batch.begin();
            for (LaserFX laserFX : laserFXList) {
                laserFX.update(Gdx.graphics.getDeltaTime());
                laserFX.render(batch);
            }
            batch.end();
        }
    }

    public void reset() {
        initializeLevel();
    }

    // Merge two maps into the manager (mapB can be null)
    public void loadTwoMaps(Level levelA, Level levelB) {
        // Check if we're reloading the same level pair (e.g., Level3 + Level3_1 transition)
        // If so, preserve the document count to maintain continuity
        boolean isSameLevelPair = false;
        int preservedDocCount = 0;
        
        // Check for Level3/Level3_1 transitions (special case for single->dual map transitions)
        if (currentLevelA != null && levelA != null) {
            String oldClassA = currentLevelA.getClass().getSimpleName();
            String oldClassB = currentLevelB != null ? currentLevelB.getClass().getSimpleName() : "";
            String newClassA = levelA.getClass().getSimpleName();
            String newClassB = levelB != null ? levelB.getClass().getSimpleName() : "";
            
            // Check if both involve Level3 and Level3_1 (in any combination)
            boolean hasLevel3 = oldClassA.equals("Level3") || oldClassB.equals("Level3") || 
                               newClassA.equals("Level3") || newClassB.equals("Level3");
            boolean hasLevel3_1 = oldClassA.equals("Level3_1") || oldClassB.equals("Level3_1") || 
                                 newClassA.equals("Level3_1") || newClassB.equals("Level3_1");
            
            if (hasLevel3 && hasLevel3_1) {
                isSameLevelPair = true;
                preservedDocCount = documentsCollected;
                Gdx.app.log("LevelManager2", "Preserving document count (" + preservedDocCount + ") for Level3/Level3_1 transition");
            }
        }
        
        // dispose old background if present
        if (backgroundTex != null) {
            backgroundTex.dispose();
            backgroundTex = null;
        }

        // Clear any JS obstacle visuals from a previous level so visuals don't leak
        try { com.mygdx.game.JSObstacle.clearAll(); } catch (Exception ignored) {}

        // store references for callbacks
        this.currentLevelA = levelA;
        this.currentLevelB = levelB;

        // init levels
        try { if (levelA != null) levelA.init(); } catch (Exception ignored) {}
        try { if (levelB != null) levelB.init(); } catch (Exception ignored) {}

        // For same level pair transitions, preserve the documents array (already collected docs removed)
        // Only merge new documents from levelB
        if (isSameLevelPair && levelB != null) {
            // Keep existing documents array (with collected docs already removed)
            // Only add new documents from levelB that aren't already in the list
            try {
                Array<Rectangle> docsB = levelB.getDocuments();
                if (docsB != null) {
                    for (Rectangle r : docsB) {
                        boolean exists = false;
                        for (Rectangle ex : documents) {
                            float dx = Math.abs((ex.x + ex.width/2f) - (r.x + r.width/2f));
                            float dy = Math.abs((ex.y + ex.height/2f) - (r.y + r.height/2f));
                            if (dx < 6f && dy < 6f) { exists = true; break; }
                        }
                        if (!exists) documents.add(new Rectangle(r));
                    }
                }
            } catch (Exception ignored) {}
        } else {
            // New level load: clear and merge lists from both levels
            documents.clear();
            try {
                if (levelA != null) {
                    Array<Rectangle> docs = levelA.getDocuments();
                    if (docs != null) documents.addAll(docs);
                }
            } catch (Exception ignored) {}
            try {
                if (levelB != null) {
                    Array<Rectangle> docs = levelB.getDocuments();
                    if (docs != null) {
                        // avoid exact duplicates (by center proximity)
                        for (Rectangle r : docs) {
                            boolean dup = false;
                            for (Rectangle ex : documents) {
                                float dx = Math.abs((ex.x + ex.width/2f) - (r.x + r.width/2f));
                                float dy = Math.abs((ex.y + ex.height/2f) - (r.y + r.height/2f));
                                if (dx < 6f && dy < 6f) { dup = true; break; }
                            }
                            if (!dup) documents.add(new Rectangle(r));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Always clear and reload platforms, obstacles, lasers
        platforms.clear();
        obstacles.clear();
        lasers.clear();
        try {
            if (levelA != null) {
                // Only add levelA platforms if levelB is null (not transitioning)
                if (levelB == null) {
                    Array<Rectangle> plats = levelA.getPlatforms();
                    if (plats != null) platforms.addAll(plats);
                }
                Array<Rectangle> obs = levelA.getObstacles();
                if (obs != null) obstacles.addAll(obs);
                Array<Rectangle> lasersA = levelA.getLasers();
                if (lasersA != null) lasers.addAll(lasersA);
            }
        } catch (Exception ignored) {}

        try {
            if (levelB != null) {
                // Use levelB platforms exclusively (replace levelA platforms)
                Array<Rectangle> plats = levelB.getPlatforms();
                if (plats != null) platforms.addAll(plats);
                Array<Rectangle> obs = levelB.getObstacles();
                if (obs != null) obstacles.addAll(obs);
                Array<Rectangle> lasersB = levelB.getLasers();
                if (lasersB != null) lasers.addAll(lasersB);
            }
        } catch (Exception ignored) {}
        
        // Create LaserFX animations for each laser
        for (LaserFX fx : laserFXList) {
            try { fx.dispose(); } catch (Exception ignored) {}
        }
        laserFXList.clear();
        for (Rectangle laser : lasers) {
            LaserFX fx = new LaserFX(laser, 0.15f); // Slower animation
            laserFXList.add(fx);
        }
        
        Gdx.app.log("LevelManager2", "Loaded " + lasers.size + " lasers from level classes");
        
        // choose shredder: prefer B then A; if both null use default manager's placement
        shredder = getLevelShredder(levelB);
        if (shredder == null) shredder = getLevelShredder(levelA);
        // Only place a default shredder if both levels are loaded (levelB != null)
        // For single-map loads that will transition later, don't create a shredder yet
        if (shredder == null && levelB != null) placeShredder();

        // Setup shared shredder visual
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

        // load background preferring B then A
        try {
            Level choose = (levelB != null) ? levelB : levelA;
            if (choose instanceof BackgroundedLevel) {
                String bg = ((BackgroundedLevel) choose).getBackgroundPath();
                if (bg != null && !bg.isEmpty()) {
                    String[] bgCandidates = new String[] { bg, bg.toLowerCase(), "assets/" + bg };
                    for (String c : bgCandidates) {
                        if (c == null) continue;
                        if (Gdx.files.internal(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.internal(c));
                            Gdx.app.log("LevelManager2", "Loaded background: " + c);
                            break;
                        }
                        if (Gdx.files.absolute(c).exists()) {
                            backgroundTex = new Texture(Gdx.files.absolute(c));
                            Gdx.app.log("LevelManager2", "Loaded background (absolute): " + c);
                            break;
                        }
                    }
                }
            } else {
                backgroundTex = null;
            }
        } catch (Exception e) {
            Gdx.app.error("LevelManager2", "Error loading background", e);
            if (backgroundTex != null) { backgroundTex.dispose(); backgroundTex = null; }
        }

        totalDocuments = documents.size;
        
        // If the level reports a different total (e.g., for multi-map levels), use that instead
        try {
            if (levelA != null) {
                int levelTotal = levelA.getTotalDocuments();
                if (levelTotal > 0 && levelTotal != documents.size) {
                    totalDocuments = levelTotal;
                    Gdx.app.log("LevelManager2", "Using level's reported total: " + totalDocuments + " (instead of " + documents.size + ")");
                }
            }
        } catch (Exception ignored) {}
        
        // Restore document count if we're reloading the same level pair
        if (isSameLevelPair) {
            documentsCollected = Math.min(preservedDocCount, totalDocuments);
            Gdx.app.log("LevelManager2", "Restored document count: " + documentsCollected + "/" + totalDocuments);
        } else {
            documentsCollected = 0;
        }
        
        levelComplete = false;
        Gdx.app.log("LevelManager2", "Loaded two-map level: totalDocs=" + totalDocuments);
    }

    // compatibility single-map loader
    public void loadLevel(Level level) {
        loadTwoMaps(level, null);
    }

    public Shredder getSharedShredder() { return sharedShredder; }
    public Level getCurrentLevelA() { return currentLevelA; }
    public Level getCurrentLevelB() { return currentLevelB; }
    @Override
    public int getDocumentsCollected() { return documentsCollected; }
    @Override
    public int getTotalDocuments() { return totalDocuments; }
    @Override
    public boolean isLevelComplete() { return levelComplete; }
    @Override
    public int getDocumentsRemaining() { return totalDocuments - documentsCollected; }

    public java.util.List<Rectangle> getAllDebugRects() {
        java.util.List<Rectangle> out = new java.util.ArrayList<Rectangle>();
        return out;
    }

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
        for (LaserFX fx : laserFXList) {
            try { fx.dispose(); } catch (Exception ignored) {}
        }
        laserFXList.clear();
        try { com.mygdx.game.JSObstacle.clearAll(); } catch (Exception ignored) {}
    }

    private boolean rectsOverlap(Rectangle a, Rectangle b) {
        return a != null && b != null && a.overlaps(b);
    }

    private Rectangle getLevelShredder(Level level) {
        if (level == null) return this.shredder;
        try {
            Rectangle r = level.getShredder();
            if (r != null) return r;
        } catch (Exception ignored) {}
        try {
            java.lang.reflect.Method m = level.getClass().getMethod("getShredderCollisionRect");
            Object o = m.invoke(level);
            if (o instanceof Rectangle) return (Rectangle) o;
        } catch (Exception ignored) {}
        return null;
    }

    // Additional helpers similar to LevelManager
    public void update(float dt, Fixer player, Level level) {
        try {
            update(dt, player);
        } catch (StackOverflowError e) {
        }
        Rectangle playerBounds = player != null ? player.getBounds() : null;
        Rectangle shredderRect = getLevelShredder(level);
        if (playerBounds != null && documentsCollected >= totalDocuments && rectsOverlap(playerBounds, shredderRect)) {
            if (!levelComplete) {
                levelComplete = true;
                Gdx.app.log("LevelManager2", "LEVEL COMPLETE! All documents shredded!");
            }
        }
    }

    public void addDocuments(int length) {
        if (length <= 0) return;
        float startX = 100f;
        float y = PLATFORM_HEIGHT + 5f;
        float gap = DOCUMENT_SIZE + 20f;
        for (int i = 0; i < length; i++) {
            documents.add(new Rectangle(startX + i * gap, y, DOCUMENT_SIZE, DOCUMENT_SIZE));
        }
        totalDocuments = documents.size;
        Gdx.app.log("LevelManager2", "addDocuments: added " + length + " docs, total=" + totalDocuments);
    }

    public void addDocumentsAtPositions(float[][] positions) {
        if (positions == null) return;
        int added = 0;
        for (float[] pos : positions) {
            if (pos == null || pos.length < 2) continue;
            documents.add(new Rectangle(pos[0], pos[1], DOCUMENT_SIZE, DOCUMENT_SIZE));
            added++;
        }
        totalDocuments = documents.size;
        Gdx.app.log("LevelManager2", "addDocumentsAtPositions: added " + added + " docs, total=" + totalDocuments);
    }
}