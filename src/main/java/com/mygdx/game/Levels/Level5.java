package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager;
import com.mygdx.game.LevelManager2;
import com.mygdx.game.LevelManager; // fallback legacy manager
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Level5 - edit positions to design
 */

public class Level5 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    // Shredder position/size matching Level3_1 for consistency
    private float shredderX = 250f;
    private float shredderY = 590f;
    private float shredderW = 64f;
    private float shredderH = 64f;
    

    // shared document total across Level5 + Level5_1
    public static int SHARED_TOTAL_DOCS = 0;
    public static final int DECLARED_DOCS = 6; // this level intends to contain 6 docs

    // Background switching state
    private boolean switchedToContinuation = false;
    private float transitionCooldown = 0f;
    private static final String BG_FIRST = "Level5Map.png";
    private static final String BG_CONTINUATION = "Level5.1Map.png";

    // legacy constants preserved for loading
  
  

    // size used for document collision rectangles
    private static final float DOC_SIZE = 36f;
    
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();
        
        // Reset transition flag
        switchedToContinuation = false;
        
        float w = 1280;
        float h = 800;
        
        // Initialize shredder for Level5 (shared between Level5 and Level5_1)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        
        // === Invisible Platforms Matching Level5Map.png ===
        platforms.clear();
        
        // === LEFT BOUNDARY WALL (Vertical wall at leftmost pillar) ===
        platforms.add(new Rectangle(90, 0, 20, 800)); // Vertical wall from bottom to top
        
        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(130, 75, 1160, 20)); // LEFT SIDE
       

        // === FLOOR 2 ===
        platforms.add(new Rectangle(115, 245, 80, 20)); // left section
         platforms.add(new Rectangle(320, 245,945, 20)); // left section
         
       
        
        // === FLOOR 3 ===
        platforms.add(new Rectangle(720, 395, 575, 20));
         platforms.add(new Rectangle(115, 395, 480, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(75, 560, 1280, 20));


        // === Your existing items ===
        documents.add(new Rectangle(900, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(600, 120, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(800, 420, DOC_SIZE, DOC_SIZE)); // 6th document



        //obstacles.add(new Rectangle(250, 150, 60, 10));
        //obstacles.add(new Rectangle(w - 300, 250, 60, 10));


        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
       
        // set shared total = this level's docs + continuation level declared docs
        try { SHARED_TOTAL_DOCS = documents.size + Level5_1.DECLARED_DOCS; } catch (Throwable ignored) {}

        // shredder visual will be managed by LevelManager (shared instance)
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }

    // Return null so external debug renderers won't draw the shredder collision rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call getShredderCollisionRect().
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

        // Update cooldown timer
        if (transitionCooldown > 0) {
            transitionCooldown -= deltaTime;
        }

        // Check if player reaches the right edge to transition to Level5_1
        try {
            if (player != null && player.getBounds() != null && platforms.size > 0 && transitionCooldown <= 0) {
                Rectangle pb = player.getBounds();
                final float TOL = 20f;
                final float SCREEN_WIDTH = 1280f;
                
                // Check if player is near the right edge of the screen (works from any floor)
                if (!switchedToContinuation && (pb.x + pb.width) >= (SCREEN_WIDTH - TOL)) {
                    try {
                        Level5_1 cont = new Level5_1();
                        // Use LevelManager2 merge API if available
                        if (lm2 != null) {
                            lm2.loadTwoMaps(this, cont);
                        } else if (levelMgr != null) {
                            // legacy fallback
                            levelMgr.loadLevel(cont);
                        }
                        float[] sp = cont.getEntranceSpawn();
                        if (sp != null && sp.length >= 2) {
                            try { player.reset(sp[0], sp[1]); } catch (Exception ignored) {}
                        }
                        switchedToContinuation = true;
                        transitionCooldown = 1.0f; // 1 second cooldown
                        Gdx.app.log("Level5", "Loaded continuation Level5_1");
                    } catch (Exception e) {
                        Gdx.app.error("Level5", "Failed to load continuation level", e);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // Provide entrance spawn positions so GameScreen can set initial player position
    public float[] getEntranceSpawn() {
        // Spawn player at the center of the top platform (near the door with clock)
        if (platforms.size >= 4) {
            Rectangle topPlatform = platforms.get(3);  // FLOOR 4 (top platform at y=560)
            float x = 640f;                            // center of screen (1280/2)
            float y = topPlatform.y + topPlatform.height; // player feet on top of platform
            return new float[]{ x, y };
        }
        return new float[]{ 640f, 580f };
    }

    public float[] getReturnSpawn() {
        if (platforms.size > 0) {
            Rectangle firstFloor = platforms.get(0);
            float x = firstFloor.x + firstFloor.width - 150f; // spawn 150 pixels from right edge
            float y = firstFloor.y + firstFloor.height; // top of platform
            return new float[]{ x, y };
        }
        return new float[]{ 1000f, 95f };
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
