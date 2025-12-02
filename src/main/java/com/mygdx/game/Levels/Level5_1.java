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
 * Level5_1 - edit positions to design
 */

public class Level5_1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    // Shredder position/size matching Level5_1 for consistency
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
    private static final String BG_CONTINUATION = "Level5.1Map.png";

    // legacy constants preserved for loading
  
  

    // size used for document collision rectangles
    private static final float DOC_SIZE = 36f;
    
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();
        
        float w = 1280;
        float h = 800;
        
        // No shredder in Level5_1 - shared shredder is in Level5 map
        shredder = null;
        
        // === Invisible Platforms Matching Level5.1Map.png ===
        platforms.clear();
        
        // === RIGHT BOUNDARY WALL (Vertical wall at rightmost pillar) ===
        platforms.add(new Rectangle(1230, 0, 20, 800)); // Vertical wall from bottom to top
        
        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(10, 75, 1240, 20)); // LEFT SIDE
       

        // === FLOOR 2 ===
        
        platforms.add(new Rectangle(10, 245,1075, 20)); // Left section
       

        // === FLOOR 3 ===
        platforms.add(new Rectangle(5, 401, 695, 20));
         platforms.add(new Rectangle(795, 401, 460, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(5, 570, 1270, 20));


        // === Your existing items ===
        documents.add(new Rectangle(900, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(600, 120, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(200, 420, DOC_SIZE, DOC_SIZE)); // 6th document



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
        return BG_CONTINUATION;
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

        try {
            if (player != null && player.getBounds() != null && platforms.size > 0 && transitionCooldown <= 0) {
                Rectangle pb = player.getBounds();
                final float TOL = 30f;
                boolean movingLeft = false;
                try { movingLeft = (player.getVelocity() != null && player.getVelocity().x < 0); } 
                catch (Exception ignored) { movingLeft = false; }
                
                // Check if player reaches the left edge from any floor
                if (pb.x <= TOL && movingLeft) {
                    try {
                        Level5 original = new Level5();
                        if (lm2 != null) {
                            // Load Level5 only (no levelB) to return to the first map
                            lm2.loadTwoMaps(original, null);
                            float[] sp = original.getReturnSpawn();
                            if (sp != null && sp.length >= 2) {
                                player.getBounds().setPosition(sp[0], sp[1]);
                                player.getVelocity().set(0, 0);
                            }
                        } else if (levelMgr != null) {
                            levelMgr.loadLevel(original);
                            float[] sp = original.getReturnSpawn();
                            if (sp != null && sp.length >= 2) {
                                player.getBounds().setPosition(sp[0], sp[1]);
                                player.getVelocity().set(0, 0);
                            }
                        }
                        transitionCooldown = 1.0f; // 1 second cooldown
                        Gdx.app.log("Level5_1", "Returned to Level5");
                    } catch (Exception e) {
                        Gdx.app.error("Level5_1", "Failed to return to Level5", e);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // Provide entrance spawn positions so GameScreen can set initial player position
    public float[] getEntranceSpawn() {
        // place player on top of the first platform so feet sit exactly on the surface
        if (platforms.size > 0) {
            Rectangle p = platforms.get(0);          // first-floor platform
            float x = p.x + 40f;                    // tweak horizontal offset as needed
            float y = p.y + p.height;               // player feet exactly on top of platform
            return new float[]{ x, y };
        }
        return new float[]{ 200f, 100f };
    }

    public float[] getReturnSpawn() {
        if (platforms.size > 0) {
            Rectangle firstFloor = platforms.get(0);
            float rightEdge = firstFloor.x + firstFloor.width;
            float x = rightEdge - 64f - 20f; // assume player width ~64
            // Match entrance height so returning places player at same vertical level
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
