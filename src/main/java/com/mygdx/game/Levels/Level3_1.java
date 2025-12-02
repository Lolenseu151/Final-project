package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager;
import com.mygdx.game.LevelManager2;

/**
 * Level3_1 - continuation area for Level3.
 */
public class Level3_1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;

    // keep same shredder coords as Level3 for consistency
    private float shredderX = 250f;
    private float shredderY = 590f;
    private float shredderW = 64f;
    private float shredderH = 64f;

    // shredder visual provided by LevelManager shared instance

    private static final float DOC_SIZE = 36f;
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f;

    private int totalDocs = 0;

    // declare expected docs for this continuation area (must match actual added docs)
    public static final int DECLARED_DOCS = 5;

    public void init() {
        documents.clear(); platforms.clear(); obstacles.clear(); beams.clear();

        // continuation area layout: different platforms and doc positions
        // 1st level platform (extends from left edge)
        platforms.add(new Rectangle(215, 65, 1020, 20)); 
        
        //2nd level platforms
        platforms.add(new Rectangle(775, 245, 455, 20));
        platforms.add(new Rectangle(215, 245, 420, 20));

        //3rd level platform
        platforms.add(new Rectangle(220, 415, 350, 20));
        platforms.add(new Rectangle(822, 415, 60, 20));

        platforms.add(new Rectangle(220, 583, 455, 20));

        // === VERTICAL WALLS (added to platforms for solid collision) ===
        // You can delete any wall you don't want by removing/commenting the line
        
        // LEFT BOUNDARY WALL - prevents player from going too far left (allow some space for return transition)
        platforms.add(new Rectangle(195, 65, 20, 540)); // Full height left wall
        
        // RIGHT BOUNDARY WALL - prevents player from going off right edge
        platforms.add(new Rectangle(1235, 65, 20, 540)); // Full height right wall
        
        
        
        
        // FLOOR 3 - Left blocking wall
        platforms.add(new Rectangle(200, 435, 20, 150)); // Wall at left edge of floor 3
        
        // FLOOR 3 - Right side wall
        platforms.add(new Rectangle(880, 435, 20, 120)); // Right wall on floor 3
        
        // FLOOR 4 (Top floor) - Left wall near shredder
        platforms.add(new Rectangle(200, 603, 20, 120)); // Left wall for top section
        

        // documents (keep same count as Level3)
        documents.add(new Rectangle(950, 330, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(760, 150, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1100, 190, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(520, 400, DOC_SIZE, DOC_SIZE));

        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;
        // ensure the shared total is updated when this continuation is initialized
        try { Level3.SHARED_TOTAL_DOCS = documents.size + Level3.DECLARED_DOCS; } catch (Throwable ignored) {}
        // shared shredder handled by LevelManager
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }

    @Override public Rectangle getShredder() { return null; }
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { 
        // always report the combined total so UI shows docs across both maps
        try { return Level3.SHARED_TOTAL_DOCS; } catch (Throwable ignored) { return totalDocs; }
    }

    @Override public void dispose() {
        // per-level resources disposed by LevelManager (shared shredder)
    }

    @Override
    public String getBackgroundPath() {
        return "Level3.1Map.png";
    }

    @Override
    public String getMusicPath() {
        return "assets/Sounds/Level Music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, 
                                 Array<Rectangle> documents, Array<Rectangle> obstacles, 
                                 Fixer player) {
        LevelManager2 lm2 = (levelMgr instanceof LevelManager2) ? (LevelManager2) levelMgr : null;
        
        try { 
            if (lm2 != null && lm2.getSharedShredder() != null) 
                lm2.getSharedShredder().update(deltaTime); 
        } catch (Exception ignored) {}

        try {
            if (player != null && player.getBounds() != null && platforms.size > 0) {
                Rectangle leftEdgePlatform = platforms.get(0);
                Rectangle pb = player.getBounds();
                final float TOL = 12f;
                boolean nearFloorY = pb.y <= (leftEdgePlatform.y + leftEdgePlatform.height + 8f);
                boolean movingLeft = false;
                try { movingLeft = (player.getVelocity() != null && player.getVelocity().x < -40f); } 
                catch (Exception ignored) { movingLeft = false; }
                
                if (nearFloorY && pb.x <= (leftEdgePlatform.x + TOL) && movingLeft) {
                    try {
                        Level3 original = new Level3();
                        if (lm2 != null) {
                            // Load Level3 only (no levelB) to return to the first map
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
                        Gdx.app.log("Level3_1", "Returned to Level3");
                    } catch (Exception e) {
                        Gdx.app.error("Level3_1", "Failed to return to Level3", e);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
        if (backgroundTex != null) batch.draw(backgroundTex, 0, 0, 1280, 800);
        // shared shredder rendering handled by LevelManager
    }

    // Spawn helpers similar to Level3
    public float[] getEntranceSpawn() {
        if (platforms.size > 0) {
            Rectangle p = platforms.get(0);
            float x = p.x + 40f;
            float y = p.y + p.height + 12f; // spawn slightly above first-floor platform
            return new float[]{ x, y };
        }
        return new float[]{ 200f, 0f };
    }

    public float[] getReturnSpawn() {
        if (platforms.size > 0) {
            Rectangle p = platforms.get(0);
            float rightEdge = p.x + p.width;
            float x = rightEdge - 80f;
            float y = p.y + p.height + 12f; // spawn slightly above first-floor platform
            return new float[]{ x, y };
        }
        return new float[]{ 200f, 0f };
    }
}
