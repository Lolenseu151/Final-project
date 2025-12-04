package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager;
import com.mygdx.game.JSObstacle;
import com.mygdx.game.LevelManager2;

/**
 * Level3_1 - continuation area for Level3.
 */
public class Level3_1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> lasers = new Array<>();
    private final com.badlogic.gdx.utils.Array<com.mygdx.game.SlopedPlatform> sloped = new com.badlogic.gdx.utils.Array<>();
    private Rectangle shredder;
    private static Texture platformPixel = null;
    private static boolean DRAW_PLATFORM_OVERLAY = false;

    // keep same shredder coords as Level3 for consistency
    private float shredderX = 70f;
    private float shredderY = 20f;
    private float shredderW = 110f;
    private float shredderH = 110f;

    // shredder visual provided by LevelManager shared instance

    private static final float DOC_SIZE = 36f;
    private int totalDocs = 0;

    // declare expected docs for this continuation area (must match actual added docs)
    public static final int DECLARED_DOCS = 5;

    public void init() {
        documents.clear(); platforms.clear(); obstacles.clear(); lasers.clear(); sloped.clear(); 

        // continuation area layout: different platforms and doc positions
        // 1st level platform (extends from left edge)
        platforms.add(new Rectangle(0, 0, 1070, 20)); 
        
        //2nd level platforms
        platforms.add(new Rectangle(560, 178, 465, 20));
        platforms.add(new Rectangle(0, 178, 430, 20));

        //3rd level platform
        platforms.add(new Rectangle(5, 348, 350, 20));
        platforms.add(new Rectangle(607, 348, 60, 20));

        platforms.add(new Rectangle(5, 518, 455, 20));

        // === VERTICAL WALLS (added to platforms for solid collision) ===
        // You can delete any wall you don't want by removing/commenting the line
        
        // LEFT BOUNDARY WALL - prevents player from going too far left (allow some space for return transition)
        platforms.add(new Rectangle(0, 178, 20, 540)); // Full height left wall
        
        // RIGHT BOUNDARY WALL - prevents player from going off right edge
        platforms.add(new Rectangle(1030, 0, 20, 540)); // Full height right wall
        
        
        
        
        // FLOOR 3 - Left blocking wall
        platforms.add(new Rectangle(660, 370, 20, 180)); // Wall at left edge of floor 3
        
        // FLOOR 3 - Right side wall
        platforms.add(new Rectangle(6650, 435, 20, 120)); // Right wall on floor 3
        
        // FLOOR 4 (Top floor) - Left wall near shredder
        //platforms.add(new Rectangle(630, 603, 20, 120)); // Left wall for top section
        

        // documents (keep same count as Level3) - randomly scattered but always on flat platforms
        documents.add(new Rectangle(140, 20, DOC_SIZE, DOC_SIZE));   // first floor
        documents.add(new Rectangle(280, 198, DOC_SIZE, DOC_SIZE));  // second floor (left segment)
        documents.add(new Rectangle(840, 198, DOC_SIZE, DOC_SIZE));  // second floor (right segment)
        documents.add(new Rectangle(60, 368, DOC_SIZE, DOC_SIZE));   // third floor (left)
        documents.add(new Rectangle(260, 538, DOC_SIZE, DOC_SIZE));  // upper platform center

        // Lasers (cyan, semi-transparent) - adjustable positions for gameplay
        // Laser 1: Left side
        lasers.add(new Rectangle(250, 220, 60, 120));
        
        // Laser 2: Right side
        lasers.add(new Rectangle(600, 185, 80, 140));

        // JS Obstacles unique to this continuation area
        try {
            JSObstacle.addTo(obstacles,
                -240f, 120f,
                38f,
                60f, 110f,
                40f,
                45f,
                0.12f,
                10f,
                3f
            );
        } catch (Exception ignored) {}

        try {
            JSObstacle.addTo(obstacles,
                200f, 660f,
                -480f,
                60f, 110f,
                55f,
                45f,
                0.12f,
                10f,
                3f
            );
        } catch (Exception ignored) {}

        // Add a shallow sloped platform to ease movement between tiers
        /*float slopeLeftX = 680f;
        float slopeLeftY = 590f;
        float slopeLength = 420f;
        double slopeAngleDeg = 149.0;
        double slopeAngleRad = Math.toRadians(slopeAngleDeg);
        float slopeRightX = slopeLeftX + (float) (Math.cos(slopeAngleRad) * slopeLength);
        float slopeRightY = slopeLeftY + (float) (Math.sin(slopeAngleRad) * slopeLength);
        sloped.add(new com.mygdx.game.SlopedPlatform(slopeLeftX, slopeLeftY, slopeRightX, slopeRightY));
*/
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;
        // ensure the shared total is updated when this continuation is initialized
        try { Level3.SHARED_TOTAL_DOCS = documents.size + Level3.DECLARED_DOCS; } catch (Throwable ignored) {}
        // shared shredder handled by LevelManager
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getLasers() { return lasers; }
    public com.badlogic.gdx.utils.Array<com.mygdx.game.SlopedPlatform> getSlopedPlatforms() { return sloped; }

    @Override public Rectangle getShredder() { return null; }
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { 
        // always report the combined total so UI shows docs across both maps
        try { return Level3.SHARED_TOTAL_DOCS; } catch (Throwable ignored) { return totalDocs; }
    }

    @Override public void dispose() {
        // per-level resources disposed by LevelManager (shared shredder)
        if (platformPixel != null) {
            try { platformPixel.dispose(); } catch (Exception ignored) {}
            platformPixel = null;
        }
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

    @Override
    public void renderOverlay(SpriteBatch batch) {
        if (!DRAW_PLATFORM_OVERLAY || batch == null) return;
        try {
            if (platformPixel == null) {
                Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pm.setColor(Color.WHITE);
                pm.fill();
                platformPixel = new Texture(pm);
                pm.dispose();
                platformPixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
            Color previous = batch.getColor().cpy();
            batch.setColor(1f, 0f, 0f, 1f);
            for (Rectangle p : platforms) {
                try { batch.draw(platformPixel, p.x, p.y, p.width, p.height); } catch (Exception ignored) {}
            }
            // Draw sloped platforms for visualization
            float thickness = 20f;
            for (com.mygdx.game.SlopedPlatform sp : sloped) {
                if (sp == null) continue;
                float x1 = sp.x1;
                float y1 = sp.y1;
                float x2 = sp.x2;
                float y2 = sp.y2;
                float dx = x2 - x1;
                float dy = y2 - y1;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                try {
                    com.badlogic.gdx.graphics.g2d.TextureRegion region = new com.badlogic.gdx.graphics.g2d.TextureRegion(platformPixel);
                    batch.draw(region, x1, y1 - thickness / 2f, 0f, thickness / 2f, len, thickness, 1f, 1f, angle);
                } catch (Exception ignored) {}
            }
            batch.setColor(previous);
        } catch (Exception ignored) {}
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
            float x = rightEdge - 0f;
            float y = p.y + p.height + 12f; // spawn slightly above first-floor platform
            return new float[]{ x, y };
        }
        return new float[]{ 200f, 0f };
    }
}
