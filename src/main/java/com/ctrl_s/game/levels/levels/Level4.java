package com.ctrl_s.game.levels.levels;

import com.ctrl_s.game.levels.BackgroundedLevel;
import com.ctrl_s.game.levels.ILevelManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.ctrl_s.game.entities.Player;
import com.badlogic.gdx.Gdx;
import com.ctrl_s.game.levels.LevelShredder;

/**
 * Level2 - edit positions to design
 */

public class Level4 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private final Array<Rectangle> lasers = new Array<>();
    private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 300f;
    private float shredderY = 90f;
    private float shredderW = 100f; // was 50f
    private float shredderH = 100f; // was 50f
    private int totalDocs = 0;

    // centralized shredder visual
    private LevelShredder shredderVisual = null;

    private static final float DOC_SIZE = 36f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();
        lasers.clear();

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(95, 55, 1130, 20)); // LEFT SIDE

        // === FLOOR 2 ===
        platforms.add(new Rectangle(225, 245, 865, 20)); // Left section

        // === FLOOR 3 ===
        platforms.add(new Rectangle(695, 400, 518, 20));
        platforms.add(new Rectangle(130, 400, 480, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(100, 552, 948, 20));

        // === VERTICAL WALLS (added to platforms for solid collision) ===
        // You can delete any wall you don't want by removing/commenting the line

        // LEFTMOST PILLAR - Full height wall
        platforms.add(new Rectangle(105, 60, 20, 500)); // Left boundary pillar wall

        // RIGHTMOST PILLAR - Full height wall
        platforms.add(new Rectangle(1185, 60, 20, 500)); // Right boundary pillar wall

        // === Your existing items ===
        documents.add(new Rectangle(230, 80, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(800, 100, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(100, 320, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(450, 610, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1000, 630, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1100, 400, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(700, 300, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(1100, 300, DOC_SIZE, DOC_SIZE));

        // obstacles.add(new Rectangle(250, 150, 60, 10));
        // obstacles.add(new Rectangle(w - 300, 250, 60, 10));

        // Lasers (cyan, semi-transparent) - adjustable positions for gameplay
        // Laser 1: Left side
        lasers.add(new Rectangle(200, 260, 60, 160));

        // Laser 2: Right side
        lasers.add(new Rectangle(1000, 260, 80, 160));

        // Laser 3: Middle area
        lasers.add(new Rectangle(800, 405, 70, 160));

        // Keep the rectangle for gameplay/collision, but we'll draw the animated
        // shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;

        // initialize centralized shredder visual
        shredderVisual = new LevelShredder(shredder);
        shredderVisual.setFrameDuration(SHREDDER_FRAME_DURATION);
        shredderVisual.loadFromFolder("assets/sprites/objects/shredder", SHREDDER_FRAME_COUNT);
        // if no frames were loaded, try a single-image fallback
        try {
            if (!shredderVisual.hasVisual()) {
                shredderVisual.loadSingle("assets/sprites/objects/shredder/shredder.png");
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public Array<Rectangle> getDocuments() {
        return documents;
    }

    @Override
    public Array<Rectangle> getPlatforms() {
        return platforms;
    }

    @Override
    public Array<Rectangle> getObstacles() {
        return obstacles;
    }

    @Override
    public Array<Rectangle> getLasers() {
        return lasers;
    }

    // Return null so external debug renderers won't draw the shredder collision
    // rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call
    // getShredderCollisionRect().
    @Override
    public Rectangle getShredder() {
        return null;
    }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() {
        return shredder;
    }

    @Override
    public int getTotalDocuments() {
        return totalDocs;
    }

    // Set initial spawn position at the center door on first floor
    public float[] getEntranceSpawn() {
        // Spawn player on the first floor platform inside house
        if (platforms.size > 0) {
            Rectangle firstFloor = platforms.get(0);
            float x = 200f; // left side position
            float y = firstFloor.y + firstFloor.height; // player bottom edge at platform top
            return new float[] { x, y };
        }
        return new float[] { 200f, 500f };
    }

    @Override
    public void dispose() {
        // dispose shredder visual if present
        if (shredderVisual != null) {
            try {
                shredderVisual.dispose();
            } catch (Exception ignored) {
            }
            shredderVisual = null;
        }
    }

    @Override
    public String getBackgroundPath() {
        return "assets/maps/level_4_map.png";
    }

    @Override
    public void updateBackground(float deltaTime, ILevelManager levelMgr, Array<Rectangle> documents,
            Array<Rectangle> obstacles, Player player) {
        // Advance shredder animation state time
        if (shredderVisual != null) {
            shredderVisual.update(deltaTime);
        }
        // Example: track documents collected and adjust visual state
    }

    @Override
    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
        // Render background; debug logging removed for shredder visuals.

        // draw background first (if any)
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, 1280, 800);
        }

        // draw shredder animation on top using centralized Shredder
        if (shredderVisual != null) {
            // ensure visual updates are reflected
            try {
                shredderVisual.update(Gdx.graphics.getDeltaTime());
            } catch (Exception ignored) {
            }
            shredderVisual.render(batch);
        } else {
            // No shredder visual to draw.
        }
    }

    @Override
    public String getMusicPath() {
        return "assets/audio/music/level_music.mp3";
    }
}
