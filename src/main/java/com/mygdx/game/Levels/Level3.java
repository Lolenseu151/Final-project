package com.mygdx.game.Levels;
 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.LevelManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Level3 - edit positions to design
 */

public class Level3 implements Level, BackgroundedLevel {
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
        beams.clear();
        
        float w = 1280;
        float h = 800;
        
        // Initialize shredder collision rectangle
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


        // === Your existing items ===
        documents.add(new Rectangle(900, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));
        // added so Level3 has 5 docs total
        documents.add(new Rectangle(600, 120, DOC_SIZE, DOC_SIZE));



        //obstacles.add(new Rectangle(250, 150, 60, 10));
        //obstacles.add(new Rectangle(w - 300, 250, 60, 10));


        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
       
        // set shared total = this level's docs + continuation level declared docs
        try { SHARED_TOTAL_DOCS = documents.size + Level3_1.DECLARED_DOCS; } catch (Throwable ignored) {}

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
    public void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                 Array<Rectangle> obstacles, Fixer player) {
        // Advance shared shredder animation state time (if present)
        try {
            if (levelMgr != null && levelMgr.getSharedShredder() != null) levelMgr.getSharedShredder().update(deltaTime);
        } catch (Exception ignored) {}
        // If player reaches the right edge of the first-floor platform, switch to continuation map.
        try {
            if (player != null && player.getBounds() != null && platforms.size > 0) {
                Rectangle firstFloor = platforms.get(0);
                float rightEdge = firstFloor.x + firstFloor.width;
                Rectangle pb = player.getBounds();
                // tolerance so the player doesn't need pixel-perfect overlap
                final float TOL = 12f;
                // require player to be near the floor vertically to avoid mid-air triggers
                boolean nearFloorY = pb.y <= (firstFloor.y + firstFloor.height + 8f);

                // --- Move to continuation map when exiting to the right ---
                if (!switchedToContinuation && nearFloorY && (pb.x + pb.width) >= (rightEdge - TOL)) {
                        try {
                            Level3_1 cont = new Level3_1();
                            levelMgr.loadLevelPreserveDocuments(cont);
                            float[] sp = cont.getEntranceSpawn();
                            if (sp != null && sp.length >= 2) {
                                try { player.reset(sp[0], sp[1]); } catch (Exception ignored) {}
                            }
                            switchedToContinuation = true;
                            com.badlogic.gdx.Gdx.app.log("Level3", "Loaded continuation Level3_1 instance (preserve docs)");
                        } catch (Exception e) {
                            com.badlogic.gdx.Gdx.app.error("Level3", "Failed to load continuation level", e);
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
