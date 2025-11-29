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
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    // use explicit position/size for visual shredder (no rectangle placeholder)
    private float shredderX = 80f;
    private float shredderY = 420f;
    private float shredderW = 50f;
    private float shredderH = 50f;
    private int totalDocs = 0;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 15f;

    // --- Shredder visuals ---
    private Texture[] shredderTextures = null;
    private Animation<TextureRegion> shredderAnim = null;
    private float shredderStateTime = 0f;
    private static final int SHREDDER_FRAME_COUNT = 9;
    private static final float SHREDDER_FRAME_DURATION = 0.08f; // tweak speed if needed

    /* 
    @Override
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Level 2 design (harder than Level 1)
        platforms.add(new Rectangle(0, 0, w, PLATFORM_H));
        platforms.add(new Rectangle(100, 150, 400, PLATFORM_H));
        platforms.add(new Rectangle(w - 350, 250, 300, PLATFORM_H));

        documents.add(new Rectangle(200, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(w - 250, 270, DOC_SIZE, DOC_SIZE));

        obstacles.add(new Rectangle(250, 150, 60, 10));
        obstacles.add(new Rectangle(w - 300, 250, 60, 10));
        beams.add(new Rectangle(400, 0, 5, h));

        shredder = new Rectangle(w - 80, 10, 50, 50);
        totalDocs = documents.size;
    }
 */

    @Override
    public void init() {
        documents.clear();
        platforms.clear();
        obstacles.clear();
        beams.clear();

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

        // === FLOOR 4 (Roof inside section) ===
        platforms.add(new Rectangle(0, 600, 1280, 20));


        // === Your existing items ===
        documents.add(new Rectangle(200, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(300, 160, DOC_SIZE, DOC_SIZE));
        documents.add(new Rectangle(w - 250, 270, DOC_SIZE, DOC_SIZE));


        obstacles.add(new Rectangle(250, 150, 60, 10));
        obstacles.add(new Rectangle(w - 300, 250, 60, 10));
    

        // Keep the rectangle for gameplay/collision, but we'll draw the animated shredder over it
        // shredder = new Rectangle(80, 420, 50, 50);
        // Remove the visible placeholder rectangle so the gray box is not rendered.
        // (We still keep explicit position/size in shredderX/Y/W/H for drawing the fx)
        shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
        totalDocs = documents.size;

        // --- Load shredder frames from assets/shredderFx/1..9 (png/jpg fallback) ---
        Gdx.app.log("Level1", "Loading shredder frames from 'shredderFx/1..9'");

        if (shredderTextures != null) {
            for (Texture t : shredderTextures) if (t != null) t.dispose();
            shredderTextures = null;
            shredderAnim = null;
        }

        Array<Texture> temp = new Array<>();
        for (int i = 1; i <= SHREDDER_FRAME_COUNT; i++) {
            String[] candidates = new String[] {
                    "shredderFx/" + i + ".png",
                    "shredderFx/" + i + ".PNG",
                    "shredderFx/" + i + ".jpg",
                    "shredderFx/" + i + ".jpeg",
                    // try with assets/ prefix in case your working dir expects it
                    "assets/shredderFx/" + i + ".png",
                    "assets/shredderFx/" + i + ".PNG"
            };
            Texture tex = null;
            for (String c : candidates) {
                try {
                    if (Gdx.files.internal(c).exists()) {
                        Gdx.app.log("Level1", "Found frame: " + c);
                        tex = new Texture(Gdx.files.internal(c));
                        break;
                    }
                } catch (Exception e) {
                    Gdx.app.error("Level1", "Error checking/loading " + c + " : " + e.getMessage());
                }
            }
            if (tex == null) {
                Gdx.app.log("Level1", "Frame " + i + " not found, stopping load sequence.");
                break;
            }
            temp.add(tex);
        }

        if (temp.size == 0) {
            // nothing loaded — list directory to help debug
            try {
                com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal("shredderFx");
                if (dir.exists() && dir.isDirectory()) {
                    com.badlogic.gdx.files.FileHandle[] listing = dir.list();
                    StringBuilder sb = new StringBuilder();
                    for (com.badlogic.gdx.files.FileHandle fh : listing) {
                        sb.append(fh.name()).append(", ");
                    }
                    Gdx.app.log("Level1", "Contents of shredderFx: " + sb.toString());
                } else {
                    Gdx.app.log("Level1", "shredderFx directory not found at internal path. Check assets folder.");
                }
            } catch (Exception e) {
                Gdx.app.error("Level1", "Error listing shredderFx directory: " + e.getMessage());
            }
        }

        if (temp.size > 0) {
            shredderTextures = new Texture[temp.size];
            TextureRegion[] regions = new TextureRegion[temp.size];
            for (int i = 0; i < temp.size; i++) {
                shredderTextures[i] = temp.get(i);
                regions[i] = new TextureRegion(shredderTextures[i]);
            }
            shredderAnim = new Animation<TextureRegion>(SHREDDER_FRAME_DURATION, regions);
            shredderStateTime = 0f;

            // resize visual shredder size to match texture (keeps bottom-left at shredderX/Y)
            Texture first = shredderTextures[0];
            if (first != null) {
                float texW = first.getWidth();
                float texH = first.getHeight();
                float scale = 1.0f;
                if (texW > 128 || texH > 128) scale = 0.5f;
                float newW = texW * scale;
                float newH = texH * scale;
                shredderW = newW;
                shredderH = newH;
                // update collision rectangle to match visual size
                if (shredder == null) {
                    shredder = new Rectangle(shredderX, shredderY, shredderW, shredderH);
                } else {
                    shredder.set(shredderX, shredderY, shredderW, shredderH);
                }
                Gdx.app.log("Level1", "Shredder visual resized to " + newW + "x" + newH + " at " + shredderX + "," + shredderY);
            }
        } else {
            Gdx.app.log("Level1", "No shredder frames loaded — shredderAnim is null.");
        }
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }

    // Return null so external debug renderers won't draw the shredder collision rectangle (removes the red box).
    // If your collision code relies on getShredder(), update it to call getShredderCollisionRect().
    @Override public Rectangle getShredder() { return null; }

    // Use this for actual collision checks if needed.
    public Rectangle getShredderCollisionRect() { return shredder; }

    @Override public int getTotalDocuments() { return totalDocs; }

    @Override public void dispose() {
        // dispose shredder textures if loaded
        if (shredderTextures != null) {
            for (Texture t : shredderTextures) if (t != null) t.dispose();
            shredderTextures = null;
        }
    }

    @Override
    public String getBackgroundPath() {
        return "Level1Map.png";
    }

    @Override
    public void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                  Array<Rectangle> obstacles, Fixer player) {
        // Advance shredder animation state time
        if (shredderAnim != null) {
            shredderStateTime += deltaTime;
        }
        // Example: track documents collected and adjust visual state
        // (You can add fields to Level1 to track state if needed)
    }

    @Override
    public void renderBackground(SpriteBatch batch, Texture backgroundTex) {
        Gdx.app.log("Level1", "renderBackground called; shredderAnim=" + (shredderAnim != null) + " shredderRect=" + shredder);

        // draw background first (if any)
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, 1280, 800);
        }

        // draw shredder animation on top (make it bigger so it's obvious)
        if (shredderAnim != null) {
            TextureRegion frame = shredderAnim.getKeyFrame(shredderStateTime, true);
            if (frame == null) {
                Gdx.app.log("Level1", "shredder frame is null");
                return;
            }

            // scale up for visibility while debugging
            float scaleDebug = 1.5f;
            float drawW = shredderW * scaleDebug;
            float drawH = shredderH * scaleDebug;
            // center the scaled draw on the visual bottom-left shredderX/Y
            float drawX = shredderX - (drawW - shredderW) * 0.5f;
            float drawY = shredderY - (drawH - shredderH) * 0.5f;

            // ensure full opacity
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(frame, drawX, drawY, drawW, drawH);

            Gdx.app.log("Level1", "Drew shredder frame at " + drawX + "," + drawY + " size " + drawW + "x" + drawH);
        } else {
            Gdx.app.log("Level1", "No shredderAnim or shredder rect to draw");
        }
    }

}
