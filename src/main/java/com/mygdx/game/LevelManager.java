package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
    private static final float DOCUMENT_SIZE = 20f;
    private static final float OBSTACLE_WIDTH = 60f;
    private static final float OBSTACLE_HEIGHT = 10f;
    private static final float BEAM_WIDTH = 5f;
    private static final float SHREDDER_SIZE = 50f;
    private static final float PLATFORM_HEIGHT = 15f;
    
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
        
        // Obstacles on ground
        obstacles.add(new Rectangle(200, PLATFORM_HEIGHT, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(400, PLATFORM_HEIGHT, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        
        // Obstacles on first level platforms
        obstacles.add(new Rectangle(screenWidth - 280, 150 + PLATFORM_HEIGHT, OBSTACLE_WIDTH, OBSTACLE_HEIGHT));
        
        // Obstacles on second level platforms
        obstacles.add(new Rectangle(70, 280 + PLATFORM_HEIGHT, 50, OBSTACLE_HEIGHT));
        obstacles.add(new Rectangle(350, 280 + PLATFORM_HEIGHT, 50, OBSTACLE_HEIGHT));
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
        
        return timePenalty;
    }
    
    /**
     * Checks if the player is colliding with any platforms and handles landing
     */
    private void checkPlatformCollisions(Fixer player, float deltaTime) {
        Rectangle p = player.getBounds();
        player.setOnGround(false); // Reset, will be set to true if on a platform

        // Cache current and previous edges
        float vx = player.getVelocity().x;
        float vy = player.getVelocity().y;
        float prevLeft = p.x - vx * deltaTime;
        float prevRight = prevLeft + p.width;
        float prevBottom = p.y - vy * deltaTime;
        float prevTop = prevBottom + p.height;
        float curLeft = p.x;
        float curRight = p.x + p.width;
        float curBottom = p.y;
        float curTop = p.y + p.height;

        final float slop = 0.5f; // small tolerance to reduce jitter

        for (Rectangle platform : platforms) {
            float platLeft = platform.x;
            float platRight = platform.x + platform.width;
            float platBottom = platform.y;
            float platTop = platform.y + platform.height;

            // --- Vertical collisions ---
            // Landing from above (one-way top surface): allow only when moving down and crossing the top
            boolean horizOverlapForVertical = (curRight > platLeft + slop) && (curLeft < platRight - slop);
            if (vy <= 0) {
                // Continuity: if we're already essentially on the platform top and still overlapping horizontally,
                // keep the player grounded (prevents falling through on flat motion when vy == 0)
                if (horizOverlapForVertical && Math.abs(curBottom - platTop) <= 1.0f) {
                    p.y = platTop;
                    player.setVelocityY(0);
                    player.setOnGround(true);
                    // Recompute edges
                    curBottom = p.y;
                    curTop = p.y + p.height;
                    continue;
                }
                if (horizOverlapForVertical && prevBottom > platTop && curBottom <= platTop) {
                    // Snap to top
                    p.y = platTop;
                    player.setVelocityY(0);
                    player.setOnGround(true);
                    // Recompute current edges after resolution
                    curBottom = p.y;
                    curTop = p.y + p.height;
                    continue; // Check other platforms for side collisions
                }
            }

            // Hitting from below (barrier under platform): block when moving up and crossing the bottom
            if (vy > 0) {
                if (horizOverlapForVertical && prevTop < platBottom && curTop >= platBottom) {
                    // Snap just below the platform
                    p.y = platBottom - p.height;
                    player.setVelocityY(0);
                    // Recompute current edges after resolution
                    curBottom = p.y;
                    curTop = p.y + p.height;
                    // Do not set onGround; remain airborne
                    continue;
                }
            }

            // --- Horizontal collisions (simple) ---
            // Only when vertical ranges overlap and crossing a side edge
            boolean vertOverlapForHorizontal = (curTop > platBottom + slop) && (curBottom < platTop - slop);
            if (vertOverlapForHorizontal && vx != 0) {
                // Crossing from left to right into the platform's left edge
                if (prevRight <= platLeft && curRight > platLeft) {
                    p.x = platLeft - p.width;
                    player.setVelocityX(0);
                    // update current edges
                    curLeft = p.x;
                    curRight = p.x + p.width;
                    continue;
                }
                // Crossing from right to left into the platform's right edge
                if (prevLeft >= platRight && curLeft < platRight) {
                    p.x = platRight;
                    player.setVelocityX(0);
                    // update current edges
                    curLeft = p.x;
                    curRight = p.x + p.width;
                    continue;
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
        // Draw platforms and documents with the ShapeRenderer first
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw platforms (brown/wood color)
        shapeRenderer.setColor(0.6f, 0.4f, 0.2f, 1);
        for (Rectangle platform : platforms) {
            shapeRenderer.rect(platform.x, platform.y, platform.width, platform.height);
        }

        // Draw documents (white/paper color)
        shapeRenderer.setColor(1, 1, 1, 1);
        for (Rectangle doc : documents) {
            shapeRenderer.rect(doc.x, doc.y, doc.width, doc.height);
        }

        // Finish shape rendering before using the SpriteBatch for text
        shapeRenderer.end();

        // Draw document numbers (and any other text) with the SpriteBatch
        // (avoid calling batch.begin() while ShapeRenderer is active)
        if (documents.size > 0) {
            boolean beganBatch = false;
            if (!batch.isDrawing()) {
                batch.begin();
                beganBatch = true;
            }
            int docNum = 1;
            for (Rectangle doc : documents) {
                font.draw(batch, "D" + docNum, doc.x + 5, doc.y + doc.height + 15);
                docNum++;
            }
            if (beganBatch) batch.end();
        }

        // Draw obstacles, beams and shredder with ShapeRenderer again
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw obstacles (red tape - red color)
        shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1);
        for (Rectangle obstacle : obstacles) {
            shapeRenderer.rect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
        }

        // Draw Auditor Beams (yellow/warning color with transparency)
        shapeRenderer.setColor(1, 1, 0, 0.5f);
        for (Rectangle beam : auditorBeams) {
            shapeRenderer.rect(beam.x, beam.y, beam.width, beam.height);
        }

        // Draw shredder (green when all docs collected, gray otherwise)
        if (documentsCollected >= totalDocuments) {
            shapeRenderer.setColor(0, 1, 0, 1); // Green - ready to win
        } else {
            shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1); // Gray - not ready yet
        }
        shapeRenderer.rect(shredder.x, shredder.y, shredder.width, shredder.height);

        shapeRenderer.end();
    }
    
    /**
     * Resets the level to initial state
     */
    public void reset() {
        initializeLevel();
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
}
