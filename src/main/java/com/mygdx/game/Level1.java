package com.mygdx.game;
 
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;

/**
 * Level1 - edit positions to design
 */

public class Level1 implements Level, BackgroundedLevel {
    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
        private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 80f;
    private float shredderY = 415f;
    private float shredderW = 36f; // was 50f
    private float shredderH = 36f; // was 50f
    private int totalDocs = 0;

    // centralized shredder visual
    private Shredder shredderVisual = null;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    // legacy constants preserved for loading
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    

    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
                float w = 1280;
        float h = 800;

        // === Invisible Platforms Matching Level1Map.png ===
        platforms.clear();

        // === FLOOR 1 (Bottom floor) ===
        platforms.add(new Rectangle(0, 7, 525, 20)); // LEFT SIDE
        platforms.add(new Rectangle(525, 22, 385, 20));   // MIDDLE SECTION
        platforms.add(new Rectangle(960, 0, 265, 4));  // RIGHT SIDE

        // === FLOOR 2 ===
        platforms.add(new Rectangle(0, 225, 620, 20));  // Left section
        platforms.add(new Rectangle(730, 225, 495, 20)); // Right section

        // === FLOOR 3 ===
        platforms.add(new Rectangle(0, 400, 925, 20));
         platforms.add(new Rectangle(1030, 400, 200, 20));

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(525, 540, 705, 20));


        // === Your existing items ===
        documents.add(new Rectangle(200, 30, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(500, 40, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 270, DOC_SIZE, DOC_SIZE));
         documents.add(new Rectangle(350, 270, DOC_SIZE, DOC_SIZE));

        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;

        // initialize centralized shredder visual
        shredderVisual = new Shredder(shredder);
        shredderVisual.setFrameDuration(SHREDDER_FRAME_DURATION);
        shredderVisual.loadFromFolder("shredderFx", SHREDDER_FRAME_COUNT);
        // if no frames were loaded, try a single-image fallback
        try {
            if (!shredderVisual.hasVisual()) {
                shredderVisual.loadSingle("shredder.png");
                if (!shredderVisual.hasVisual()) shredderVisual.loadSingle("assets/shredder.png");
            }
        } catch (Exception ignored) {}
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
        // Return null so external debug renderers won't draw the shredder collision rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call getShredderCollisionRect().
    @Override public Rectangle getShredder() { return null; }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { return totalDocs; }

    @Override public void dispose() {
        // dispose shredder visual if present
        if (shredderVisual != null) {
            try { shredderVisual.dispose(); } catch (Exception ignored) {}
            shredderVisual = null;
        }
    }

    @Override
    public String getBackgroundPath() {
        return "Level1Map.png";
    }

    @Override
    public String getMusicPath() {
        return "assets/Sounds/Level Music.mp3";
    }

    @Override
    public void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                 Array<Rectangle> obstacles, Fixer player) {
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
            try { shredderVisual.update(Gdx.graphics.getDeltaTime()); } catch (Exception ignored) {}
            shredderVisual.render(batch);
        } else {
            // No shredder visual to draw.
        }
    }

}
