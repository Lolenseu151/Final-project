package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager;
import com.mygdx.game.LevelManager2;
import com.mygdx.game.LevelManager; // fallback legacy manager
import com.mygdx.game.JSObstacle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Level3 - edit positions to design
 */

public class Level3 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
        private Rectangle shredder;
    // Shredder position/size matching Level3_1 for consistency
    private float shredderX = 250f;
    private float shredderY = 590f;
    private float shredderW = 64f;
    private float shredderH = 64f;
    

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
                
        float w = 1280;
        float h = 800;
        
        // Add shredder to Level3 so shredding action can be seen
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        
        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();
        
        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(50, 75, 1220, 20)); // LEFT SIDE
       

        // === FLOOR 2 ===
        platforms.add(new Rectangle(1070, 245,185, 20)); // left section
        platforms.add(new Rectangle(46, 245,935, 20)); // Left section
       

        // === FLOOR 3 ===
        platforms.add(new Rectangle(411, 420, 450, 20));
         platforms.add(new Rectangle(1080, 420, 170, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(597, 583, 656, 20));

        // === VERTICAL WALLS (added to platforms for solid collision) ===
        // You can delete any wall you don't want by removing/commenting the line
        
        // LEFT BOUNDARY WALL - prevents player from going off left edge
        platforms.add(new Rectangle(30, 75, 20, 530)); // Full height left wall
        
        // RIGHT BOUNDARY WALL - removed to allow transition to Level3_1
        // platforms.add(new Rectangle(1270, 250, 20, 530)); // Full height right wall
        
        // FLOOR 3 - Left blocking wall
        platforms.add(new Rectangle(390, 440, 20, 145)); // Wall at left edge of floor 3
        
        // FLOOR 4 - Right exit wall - removed to allow transition
        // platforms.add(new Rectangle(1250, 603, 20, 170)); // Right wall for roof section

        // === Your existing items ===
        documents.add(new Rectangle(850, 600, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));
        // added so Level3 has 5 docs total
        documents.add(new Rectangle(600, 120, DOC_SIZE, DOC_SIZE));


        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
       
        // set shared total = this level's docs + continuation level declared docs
        try { SHARED_TOTAL_DOCS = documents.size + Level3_1.DECLARED_DOCS; } catch (Throwable ignored) {}

        // Add a reusable JS obstacle (visual handled by LevelManager's obstacle rendering)
        try {
            // patrol from 600 -> 900 at y=445, width=60,height=110, speed=60,
            // sightDistance=35, sightVerticalTolerance=0.1, scale=1.0, caughtDelay=3s
            JSObstacle.addTo(obstacles,
                600f, 900f, // leftX, rightX
                85f,       // y
                60f, 110f,  // w, h
                60f,        // speed px/sec
                35f,        // sight distance
                0.1f,       // sight vertical tolerance
                10.5f,       // scale multiplier for visual size (increased)
                3f          // caught delay seconds
            );
        } catch (Exception ignored) {}

                try {
            // patrol from 600 -> 900 at y=445, width=60,height=110, speed=60,
            // sightDistance=35, sightVerticalTolerance=0.1, scale=1.0, caughtDelay=3s
            JSObstacle.addTo(obstacles,
                600f, 900f, // leftX, rightX
                -435f,       // y
                60f, 110f,  // w, h
                60f,        // speed px/sec
                35f,        // sight distance
                0.1f,       // sight vertical tolerance
                10f,       // scale multiplier for visual size (increased)
                3f          // caught delay seconds
            );
        } catch (Exception ignored) {}

        // shredder visual will be managed by LevelManager (shared instance)
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    

    @Override public Rectangle getShredder() { return shredder; }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { return SHARED_TOTAL_DOCS; }
    @Override public void dispose() { /* per-level resources disposed by LevelManager */ }

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
                Rectangle pb = player.getBounds();
                final float TOL = 12f;
                boolean nearFloorY = pb.y <= (firstFloor.y + firstFloor.height + 8f);

                if (!switchedToContinuation && nearFloorY && (pb.x + pb.width) >= (rightEdge - TOL)) {
                    try {
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
        // Center the Fixer horizontally, but place vertically on top of the
        // first platform so the player stands on the surface.
        // Use actual runtime screen size so centering matches the viewport
        float screenW = Gdx.graphics.getWidth();
        float approxPlayerSize = 74f; // matches Fixer sprite size used elsewhere
        float x = (screenW / 2f) - (approxPlayerSize / 2f);

        // If the first platform exists, place the player's feet just above it.
        if (platforms.size > 0) {
            Rectangle p = platforms.get(0);
            float y = p.y + p.height + 2f; // small safety offset above platform
            return new float[]{ x, y };
        }

        // Fallback: center vertically if no platform found
        float screenH = Gdx.graphics.getHeight();
        float y = (screenH / 2f) - (approxPlayerSize / 2f);
        return new float[]{ x, y };
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

}
