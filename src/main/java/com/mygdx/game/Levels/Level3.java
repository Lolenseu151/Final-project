package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager; // fallback legacy manager
import com.mygdx.game.JSObstacle;
import com.mygdx.game.LevelManager2;

/**
 * Level3 - edit positions to design
 */

public class Level3 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> lasers = new Array<>();
    private final com.badlogic.gdx.utils.Array<com.mygdx.game.SlopedPlatform> sloped = new com.badlogic.gdx.utils.Array<>();
    private Rectangle shredder;
    private boolean spawnJsObstacles = true;
    // Shredder position/size matching Level3_1 for consistency
    private float shredderX = 70f;
    private float shredderY = 20f;
    private float shredderW = 96f;
    private float shredderH = 96f;
    

    // shared document total across Level3 + Level3_1
    public static int SHARED_TOTAL_DOCS = 0;
    public static final int DECLARED_DOCS = 5; // this level intends to contain 5 docs

    // Background switching state
    private boolean switchedToContinuation = false;
    private static final String BG_FIRST = "Level3Map.png";
    private static final String BG_CONTINUATION = "Level3.1Map.png";

    // legacy constants preserved for loading
  
  

    // size used for document collision rectangles
    private static final float DOC_SIZE = 36f;
    
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        lasers.clear();
                
        float w = 1280;
        float h = 800;
        
        // Add shredder to Level3 so shredding action can be seen
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        
        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();
        
        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(20, 0, 1290, 20)); // LEFT SIDE
       

        // === FLOOR 2 ===
        platforms.add(new Rectangle(1070, 180,185, 20)); // left section
        platforms.add(new Rectangle(46, 180,935, 20)); // Left section
       

        // === FLOOR 3 ===
        platforms.add(new Rectangle(411, 355, 450, 20));
         platforms.add(new Rectangle(1080, 355, 170, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(597, 518, 656, 20));

        // === VERTICAL WALLS (added to platforms for solid collision) ===
        // You can delete any wall you don't want by removing/commenting the line
        
        // LEFT BOUNDARY WALL - prevents player from going off left edge
        platforms.add(new Rectangle(30, 0, 20, 530)); // Full height left wall
        
        // RIGHT BOUNDARY WALL - removed to allow transition to Level3_1
        // platforms.add(new Rectangle(1270, 250, 20, 530)); // Full height right wall
        
        // FLOOR 3 - Left blocking wall
        platforms.add(new Rectangle(390, 375, 20, 205)); // Wall at left edge of floor 3
        
        // FLOOR 4 - Right exit wall - removed to allow transition
        // platforms.add(new Rectangle(1250, 603, 20, 170)); // Right wall for roof section

        // === Documents (scattered across flat platforms) ===
        documents.add(new Rectangle(120, 20, DOC_SIZE, DOC_SIZE));   // bottom floor
        documents.add(new Rectangle(640, 200, DOC_SIZE, DOC_SIZE));  // middle floor span
        documents.add(new Rectangle(1100, 200, DOC_SIZE, DOC_SIZE)); // upper-right ledge
        documents.add(new Rectangle(430, 375, DOC_SIZE, DOC_SIZE));  // third floor ledge
        documents.add(new Rectangle(760, 538, DOC_SIZE, DOC_SIZE));  // roof platform

        // Level3 no longer has standalone lasers; hazards resume in Level3_1

        // Add a diagonal sloped platform with configurable length and angle
        // Left endpoint (higher): x=380,y=570
        float leftX = 700f, leftY = 720f;
        float length = 500f; // slope length in pixels — change this to adjust
        double angleDeg = 205.0; // slope angle in degrees (negative = descending to the right)
        double angleRad = Math.toRadians(angleDeg);
        float rightX = leftX + (float)(Math.cos(angleRad) * length);
        float rightY = leftY + (float)(Math.sin(angleRad) * length);
        sloped.add(new com.mygdx.game.SlopedPlatform(leftX, leftY, rightX, rightY));

        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
       
        // set shared total = this level's docs + continuation level declared docs
        try { SHARED_TOTAL_DOCS = documents.size + Level3_1.DECLARED_DOCS; } catch (Throwable ignored) {}

        if (spawnJsObstacles) {
            try {
                JSObstacle.addTo(obstacles,
                    600f, 900f,
                    20f,
                    60f, 110f,
                    60f,
                    35f,
                    0.1f,
                    10.5f,
                    3f
                );
            } catch (Exception ignored) {}

            try {
                JSObstacle.addTo(obstacles,
                    500f, 900f,
                    -480f,
                    60f, 110f,
                    60f,
                    35f,
                    0.1f,
                    10f,
                    3f
                );
            } catch (Exception ignored) {}
        }

        // shredder visual will be managed by LevelManager (shared instance)
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getLasers() { return lasers; }

    @Override public Rectangle getShredder() { return shredder; }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { return SHARED_TOTAL_DOCS; }
    // Expose sloped platforms for managers that support slopes.
    public com.badlogic.gdx.utils.Array<com.mygdx.game.SlopedPlatform> getSlopedPlatforms() {
        return sloped;
    }
    @Override
    public void dispose() {
        // Dispose per-level resources if any. Also dispose cached platformPixel to avoid small leak.
        try {
            if (platformPixel != null) {
                try { platformPixel.dispose(); } catch (Exception ignored) {}
                platformPixel = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public String getBackgroundPath() {
        // initially return the first part map
        return BG_FIRST;
    }

    @Override
    public String getMusicPath() {
        return "assets/Sounds/Level Music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, 
                                 Array<Rectangle> documents, Array<Rectangle> obstacles, 
                                 Fixer player) {
        // Try to get LevelManager2 features if available
        LevelManager2 lm2 = (levelMgr instanceof LevelManager2) ? (LevelManager2) levelMgr : null;
        
        try {
            if (lm2 != null && lm2.getSharedShredder() != null) {
                lm2.getSharedShredder().update(deltaTime);
            }
        } catch (Exception ignored) {}

        // Check if player reaches the right edge to transition to Level3_1
        try {
            if (player != null && player.getBounds() != null && platforms.size > 0) {
                Rectangle firstFloor = platforms.get(0);
                float rightEdge = firstFloor.x + firstFloor.width;
                try {
                    float screenWidth = Gdx.graphics.getWidth();
                    if (screenWidth > 0f) {
                        rightEdge = Math.min(rightEdge, screenWidth);
                    }
                } catch (Exception ignored) {}
                Rectangle pb = player.getBounds();
                final float TOL = 12f;
                boolean nearFloorY = pb.y <= (firstFloor.y + firstFloor.height + 60f);

                if (!switchedToContinuation && nearFloorY && (pb.x + pb.width) >= (rightEdge - TOL)) {
                    try {
                        spawnJsObstacles = false;
                        Level3_1 cont = new Level3_1();
                        // Use LevelManager2 merge API to transition while preserving document count
                        if (lm2 != null) {
                            lm2.loadTwoMaps(this, cont);
                        } else if (levelMgr != null) {
                            // legacy fallback
                            levelMgr.loadLevel(cont);
                        }
                        float[] sp = cont.getEntranceSpawn();
                        if (sp != null && sp.length >= 2) {
                            try { 
                                player.getBounds().setPosition(sp[0], sp[1]); 
                                player.getVelocity().set(0, 0);
                            } catch (Exception ignored) {}
                        }
                        switchedToContinuation = true;
                        Gdx.app.log("Level3", "Loaded continuation Level3_1");
                    } catch (Exception e) {
                        Gdx.app.error("Level3", "Failed to load continuation level", e);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // Provide entrance spawn positions so GameScreen can set initial player position
    public float[] getEntranceSpawn() {
        // Spawn player at a good starting position on the first floor
        // Position them clearly on the bottom-left platform
        // Adjusted spawn point for better positioning when entering from level select screen
        return new float[]{ 100f, 500f };  // x=100 (left side), y=100 (safely on first platform at y=75+20)
    }

    public float[] getReturnSpawn() {
        if (platforms.size > 0) {
            Rectangle firstFloor = platforms.get(0);
            float rightEdge = firstFloor.x + firstFloor.width;
            float x = rightEdge - 64f - 20f; // assume player width ~64
            float y = firstFloor.y + firstFloor.height; // top of platform
            return new float[]{ x, y };
        }
        return new float[]{ 100f, 90f };
    }

    @Override
    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
        // Render background; debug logging removed for shredder visuals.

        // draw background first (if any)
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, 1280, 800);
        }

        // shared shredder visuals are handled by LevelManager (no per-level shredder draw)
    }

    @Override
    public void renderOverlay(SpriteBatch batch) {
        // Draw red platform visuals for Level3 only
        if (batch == null || switchedToContinuation) return;
        try {
            if (platformPixel == null) {
                Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pm.setColor(Color.WHITE);
                pm.fill();
                platformPixel = new Texture(pm);
                pm.dispose();
                platformPixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
            Color prev = batch.getColor().cpy();
            batch.setColor(1f, 0f, 0f, 1f);
            for (Rectangle p : platforms) {
                try { batch.draw(platformPixel, p.x, p.y, p.width, p.height); } catch (Exception ignored) {}
            }
            // Draw sloped platforms as rotated thin rectangles
            try {
                float thickness = 20f;
                for (com.mygdx.game.SlopedPlatform sp : sloped) {
                    if (sp == null) continue;
                    float x1 = sp.x1;
                    float y1 = sp.y1;
                    float x2 = sp.x2;
                    float y2 = sp.y2;
                    float dx = x2 - x1;
                    float dy = y2 - y1;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
                    try {
                        com.badlogic.gdx.graphics.g2d.TextureRegion region = new com.badlogic.gdx.graphics.g2d.TextureRegion(platformPixel);
                        batch.draw(region, x1, y1 - thickness/2f, 0f, thickness/2f, len, thickness, 1f, 1f, angle);
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            batch.setColor(prev);
        } catch (Exception ignored) {}
    }

    // 1x1 texture used to draw platform rectangles in renderOverlay
    private static Texture platformPixel = null;

}
