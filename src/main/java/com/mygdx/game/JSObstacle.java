package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * JSObstacle - utility wrapper that creates a collision rectangle and also
 * provides an optional animated visual (KMJS sprite frames) for any level
 * that wants a visible JS. Callers can keep using `addTo(obstacles, ...)`
 * as before; this class will also register an internal visual instance
 * that is drawn automatically via `renderAll(batch)`.
 */
public class JSObstacle {
    private Rectangle rect;
    private boolean facingRight = true; // visual facing hint

    // Per-instance behavior tunables
    private float leftX = 0f;
    private float rightX = 0f;
    private float speed = 60f;
    private float sightDistance = 70f;
    private float sightVerticalTolerance = 0.3f; // fraction of height
    private float caughtDelay = 3.0f;
    private boolean caughtPending = false;
    private float caughtTimer = 0f;
    // How long to wait (seconds) after detection before switching to GAMEOVER
    private float gameOverDelay = 2.0f;

    // Shared static frames for KMJS visuals (loaded once on demand)
    private static TextureRegion[] sharedFrames = null;
    private static Texture sharedCaughtTex = null;
    private static com.badlogic.gdx.graphics.g2d.TextureRegion sharedCaughtRegion = null;
    private static boolean framesLogged = false;

    // Instances to draw (each holds a reference to the same Rectangle returned to levels)
    private static final ArrayList<JSObstacle> instances = new ArrayList<>();
    // When one JS catches the player, prevent other JS instances from also entering caught state
    private static boolean anyCaughtActive = false;

    private float animTime = 0f;
    // visual scale stored so rendering and hitbox adjustments can be consistent
    private float visualScale = 1f;

    // Vertical tolerance (in pixels) for sight checks — player center must be within this
    // distance of the JS center to be considered on the same floor/platform.
    private static final float JS_VERTICAL_TOLERANCE_PX = 40f;

    public JSObstacle(Rectangle r) {
        this.rect = r;
        this.facingRight = true;
        this.leftX = r.x;
        this.rightX = r.x;
        ensureFramesLoaded();
    }

    public float getVisualScale() { return visualScale; }

    public Rectangle getRect() { return rect; }

    private static void ensureFramesLoaded() {
        if (sharedFrames != null) return;
        try {
            java.util.ArrayList<TextureRegion> tmp = new java.util.ArrayList<>();
            // Try common candidate base paths first (both with and without leading 'assets/')
            String[] bases = new String[]{"assets/kmjs", "kmjs"};
            for (String base : bases) {
                try {
                    for (int i = 1; i <= 4; i++) {
                        String p = String.format("%s/%d.png", base, i);
                        try {
                            if (Gdx.files.internal(p).exists()) {
                                Texture t = new Texture(Gdx.files.internal(p));
                                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                                tmp.add(new TextureRegion(t));
                                continue;
                            }
                            if (Gdx.files.absolute(p).exists()) {
                                Texture t = new Texture(Gdx.files.absolute(p));
                                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                                tmp.add(new TextureRegion(t));
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }
                    // If we found frames in this base, stop searching other bases
                    if (tmp.size() > 0) break;
                } catch (Exception ignored) {}
            }

            // If still empty, try listing png files in known directories and load them sorted by name.
            if (tmp.isEmpty()) {
                for (String base : bases) {
                    try {
                        com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal(base);
                        if (dir != null && dir.exists() && dir.isDirectory()) {
                            com.badlogic.gdx.files.FileHandle[] files = dir.list("png");
                            java.util.Arrays.sort(files, (a,b) -> a.name().compareToIgnoreCase(b.name()));
                            for (com.badlogic.gdx.files.FileHandle fh : files) {
                                try {
                                    Texture t = new Texture(fh);
                                    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                                    tmp.add(new TextureRegion(t));
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!tmp.isEmpty()) break;
                }
            }

            if (tmp.size() > 0) sharedFrames = tmp.toArray(new TextureRegion[0]);
            if (sharedFrames != null && !framesLogged) {
                Gdx.app.log("JSObstacle", "Loaded JS frames: " + sharedFrames.length);
                framesLogged = true;
            }

            // load optional caught texture: prefer a file explicitly named '5.png' else pick a frame that contains '5' in name
            try {
                // try exact names first
                String[] caughtCandidates = new String[]{"assets/kmjs/5.png", "kmjs/5.png"};
                boolean loadedCaught = false;
                for (String p5 : caughtCandidates) {
                    try {
                        if (Gdx.files.internal(p5).exists()) {
                            sharedCaughtTex = new Texture(Gdx.files.internal(p5));
                            sharedCaughtTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                            loadedCaught = true;
                            break;
                        }
                        if (Gdx.files.absolute(p5).exists()) {
                            sharedCaughtTex = new Texture(Gdx.files.absolute(p5));
                            sharedCaughtTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                            loadedCaught = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                // if not found, scan directories for file names containing '5' (handles names like '5 (3).png')
                if (!loadedCaught) {
                    for (String base : new String[]{"assets/kmjs", "kmjs"}) {
                        try {
                            com.badlogic.gdx.files.FileHandle d = Gdx.files.internal(base);
                            if (d != null && d.exists() && d.isDirectory()) {
                                com.badlogic.gdx.files.FileHandle[] files = d.list("png");
                                for (com.badlogic.gdx.files.FileHandle fh : files) {
                                    if (fh.name().contains("5")) {
                                        try { sharedCaughtTex = new Texture(fh); sharedCaughtTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); loadedCaught = true; Gdx.app.log("JSObstacle", "Loaded caught texture: " + fh.path()); break; } catch (Exception ignored) {}
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                        if (loadedCaught) break;
                    }
                }
                if (sharedCaughtTex != null) {
                    try { sharedCaughtRegion = new com.badlogic.gdx.graphics.g2d.TextureRegion(sharedCaughtTex); } catch (Exception ignored) { sharedCaughtRegion = null; }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {
            sharedFrames = null;
        }
    }

    /**
     * Create a Rectangle instance for a JS obstacle.
     */
    public static Rectangle createRect(float x, float y, float w, float h) {
        return new Rectangle(x, y, w, h);
    }

    /**
     * Add a JS obstacle rectangle to an existing obstacles array and return it.
     * Also register an internal visual instance to be rendered by `renderAll`.
     */
    public static Rectangle addTo(Array<Rectangle> obstacles, float x, float y, float w, float h) {
        if (obstacles == null) return createRect(x, y, w, h);
        Rectangle r = createRect(x, y, w, h);
        obstacles.add(r);
        JSObstacle inst = new JSObstacle(r);
        instances.add(inst);
        Gdx.app.log("JSObstacle", String.format("Added JS instance at %.1f,%.1f size %.1fx%.1f (total=%d)", r.x, r.y, r.width, r.height, instances.size()));
        return r;
    }

    /**
     * Add a JS obstacle with behavior configuration.
     */
    public static Rectangle addTo(Array<Rectangle> obstacles,
                                  float leftX, float rightX, float y,
                                  float w, float h,
                                  float speed, float sightDistance,
                                  float sightVerticalTolerance, float scale,
                                  float caughtDelay) {
        if (obstacles == null) return createRect(leftX, y, w, h);
        // create rect positioned at leftX
        Rectangle r = createRect(leftX, y, w * scale, h * scale);
        obstacles.add(r);
        JSObstacle inst = new JSObstacle(r);
        inst.leftX = leftX;
        inst.rightX = rightX;
        inst.speed = speed;
        inst.sightDistance = sightDistance;
        inst.sightVerticalTolerance = sightVerticalTolerance;
        inst.caughtDelay = caughtDelay;
        inst.visualScale = scale;
        instances.add(inst);
        Gdx.app.log("JSObstacle", String.format("Added JS instance (configured) left=%.1f right=%.1f y=%.1f size=%.1fx%.1f speed=%.1f sight=%.1f (total=%d)", leftX, rightX, y, r.width, r.height, speed, sightDistance, instances.size()));
        return r;
    }

    /**
     * Returns true if the provided rectangle belongs to a registered JS visual instance.
     */
    public static boolean isRegisteredRect(Rectangle r) {
        if (r == null) return false;
        for (JSObstacle jo : instances) {
            if (jo != null && jo.rect == r) return true;
        }
        return false;
    }

    /**
     * Update all registered JS instances (movement, sight checks).
     */
    public static void updateAll(float dt, Fixer player) {
        if (instances.isEmpty()) return;
        ensureFramesLoaded();
        Iterator<JSObstacle> it = instances.iterator();
        while (it.hasNext()) {
            JSObstacle jo = it.next();
            if (jo == null || jo.rect == null) { it.remove(); continue; }
            try {
                jo.update(dt, player);
            } catch (Exception ignored) {}
        }
    }

    private void update(float dt, Fixer player) {
        // If we're in a caught pending state, advance its timer and trigger GAMEOVER after `gameOverDelay`.
        if (caughtPending) {
            caughtTimer += dt;
            if (caughtTimer >= gameOverDelay) {
                // trigger GAMEOVER via helper
                try { triggerGameOver(); } catch (Exception ignored) {}
                // clear global caught lock so subsequent runs won't be blocked
                anyCaughtActive = false;
                caughtPending = false;
                caughtTimer = 0f;
            }
            // while pending, skip patrol/movement
            return;
        }

        // Patrol between leftX and rightX
        if (rightX > leftX + 1f) {
            float dir = facingRight ? 1f : -1f;
            float move = speed * dt * dir;
            rect.x += move;
            if (rect.x > rightX) { rect.x = rightX; facingRight = false; }
            if (rect.x < leftX) { rect.x = leftX; facingRight = true; }
        }

        // sight and catch logic
        if (player != null && player.getBounds() != null) {
            Rectangle pb = player.getBounds();
            float playerCenterX = pb.x + pb.width * 0.5f;
            float jsCenterX = rect.x + rect.width * 0.5f;
            boolean playerIsInFront = (facingRight && playerCenterX > jsCenterX) || (!facingRight && playerCenterX < jsCenterX);
            
                {
                boolean directOverlap = rect.overlaps(pb);
                boolean inSight = false;
                    if (playerIsInFront) {
                    // Horizontal sight: start from sprite center so the ray originates from the
                    // midpoint (roughly pixel 2000 of the 4000px art). Still treat sightDistance as
                    // "distance beyond the visible front", so we add half the sprite width to cover
                    // the guard's body before extending outward.
                    float centerX = rect.x + rect.width * 0.5f;
                    float halfWidth = rect.width * 0.5f;
                    float sightW = sightDistance + halfWidth;
                    float sightStart = facingRight ? centerX : (centerX - sightW);
                    boolean inHorizontalSight = (playerCenterX >= sightStart && playerCenterX <= (sightStart + sightW));

                    // Require player's bottom to be close to JS base (`rect.y`) so JS only detects
                    // players on the same floor/platform. This prevents cross-floor detection
                    // when large visual scales inflate the JS rect height.
                    float playerCenterY = pb.y + pb.height * 0.5f;
                    float jsCenterY = rect.y + rect.height * 0.5f;
                    float verticalDelta = Math.abs(playerCenterY - jsCenterY);
                    boolean verticalOk = verticalDelta <= JS_VERTICAL_TOLERANCE_PX;

                    inSight = inHorizontalSight && verticalOk;
                    // Debug: log sight checks every frame when player is horizontally aligned
                    if (inHorizontalSight) {
                        Gdx.app.log("JSObstacleSight", String.format(
                            "jsCenterY=%.1f playerCenterY=%.1f | delta=%.1f | tol=%.1f | horiz=%b | vertOk=%b | inSight=%b",
                            jsCenterY, playerCenterY, verticalDelta, JS_VERTICAL_TOLERANCE_PX,
                            inHorizontalSight, verticalOk, inSight));
                    }
                }
                boolean overlapAllowed = directOverlap && playerIsInFront && inSight;
                // Only catch if player is in front (sight) OR if directly overlapping while also in sight.
                // This prevents the player from being caught when sneaking behind JS or on other floors.
                if (inSight || overlapAllowed) {
                    // Only allow one JS to enter caught state at a time
                    // Log diagnostic info to help debug false-positive catches
                    try {
                        int idx = instances.indexOf(this);
                        float playerCenterY = pb.y + pb.height * 0.5f;
                        float jsCenterY = rect.y + rect.height * 0.5f;
                        Gdx.app.log("JSObstacle", String.format("CatchAttempt idx=%d jsX=%.1f playerX=%.1f jsY=%.1f playerY=%.1f inSight=%b overlap=%b playerIsInFront=%b", idx, jsCenterX, playerCenterX, jsCenterY, playerCenterY, inSight, directOverlap, playerIsInFront));
                    } catch (Exception ignored) {}

                    if (!anyCaughtActive) {
                        anyCaughtActive = true;
                        // try to play kmjs sfx (only for the active catcher)
                        try {
                            Object appObj = Gdx.app.getApplicationListener();
                            if (appObj != null) {
                                try {
                                    java.lang.reflect.Method getHsm = appObj.getClass().getMethod("getHoverSoundManager");
                                    Object hsm = getHsm.invoke(appObj);
                                    if (hsm != null) {
                                        try {
                                            java.lang.reflect.Method play = hsm.getClass().getMethod("playKmjs");
                                            play.invoke(hsm);
                                        } catch (NoSuchMethodException nsme) {}
                                    }
                                } catch (NoSuchMethodException nsme) {}
                            }
                        } catch (Exception ignored) {}

                        // GAMEOVER will be triggered after `gameOverDelay` elapses (caughtPending timer)
                        // mark visual as pending so the caught frame can be drawn briefly if needed
                        caughtPending = true;
                        caughtTimer = 0f;
                    }
                }
            }
        }
    }

    // Helper to set the screen's currentState to GAMEOVER via reflection (mirrors Level2 behavior)
    private void triggerGameOver() {
        try {
            Object app = Gdx.app.getApplicationListener();
            if (app instanceof com.badlogic.gdx.Game) {
                com.badlogic.gdx.Screen screen = ((com.badlogic.gdx.Game) app).getScreen();
                if (screen != null) {
                    java.lang.reflect.Field f = null;
                    try { f = screen.getClass().getDeclaredField("currentState"); } catch (NoSuchFieldException nsf) {
                        Class<?> sc = screen.getClass().getSuperclass();
                        if (sc != null) { try { f = sc.getDeclaredField("currentState"); } catch (Exception ignored) {} }
                    }
                    if (f != null) {
                        f.setAccessible(true);
                        Class<?> enumType = f.getType();
                        if (enumType.isEnum()) {
                            Object val = java.lang.Enum.valueOf((Class) enumType, "GAMEOVER");
                            try { f.set(screen, val); } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Render all registered JS visuals. Call from LevelManager's batch phase.
     */
    public static void renderAll(SpriteBatch batch) {
        if (instances.isEmpty()) return;
        ensureFramesLoaded();
        // If no frames loaded, nothing to draw
        if (sharedFrames == null || sharedFrames.length == 0) return;

        float dt = Gdx.graphics.getDeltaTime();
        batch.begin();
        try {
            Iterator<JSObstacle> it = instances.iterator();
            while (it.hasNext()) {
                JSObstacle jo = it.next();
                if (jo == null || jo.rect == null) { it.remove(); continue; }
                jo.animTime += dt;
                int idx = (int) ((jo.animTime / 0.12f) % sharedFrames.length);
                TextureRegion fr = sharedFrames[idx];
                if (fr == null) continue;

                // draw caught texture if pending
                if (jo.caughtPending && sharedCaughtRegion != null) {
                    try {
                        float texW = sharedCaughtRegion.getRegionWidth();
                        float texH = sharedCaughtRegion.getRegionHeight();
                        float effW = jo.rect.width;
                        float effH = jo.rect.height;
                        float drawW = effW;
                        float drawH = effH;
                        float drawX = jo.rect.x;
                        float drawY = jo.rect.y;
                        if (texW > 0 && texH > 0) {
                            float scale = Math.min(effW / texW, effH / texH);
                            drawW = texW * scale;
                            drawH = texH * scale;
                            drawX = jo.rect.x + (effW - drawW) * 0.5f;
                            drawY = jo.rect.y + (effH - drawH) * 0.5f;
                        }
                        // If the JS is facing left, mirror the caught texture so the reaction faces the player
                        boolean flipCaught = !jo.facingRight;
                        if (!flipCaught) {
                            batch.draw(sharedCaughtRegion, drawX, drawY, drawW, drawH);
                        } else {
                            // draw mirrored by using a negative width
                            batch.draw(sharedCaughtRegion, drawX + drawW, drawY, -drawW, drawH);
                        }
                        continue;
                    } catch (Exception ignored) {}
                }

                float texW = fr.getRegionWidth();
                float texH = fr.getRegionHeight();
                float effW = jo.rect.width;
                float effH = jo.rect.height;
                float drawW = effW;
                float drawH = effH;
                float drawX = jo.rect.x;
                float drawY = jo.rect.y;
                if (texW > 0 && texH > 0) {
                    float scale = Math.min(effW / texW, effH / texH);
                    drawW = texW * scale;
                    drawH = texH * scale;
                    drawX = jo.rect.x + (effW - drawW) * 0.5f;
                    drawY = jo.rect.y + (effH - drawH) * 0.5f;
                }
                // draw flipped if facing left
                boolean flip = !jo.facingRight;
                if (fr.isFlipX() != flip) fr.flip(true, false);
                batch.draw(fr, drawX, drawY, drawW, drawH);
            }
        } catch (Exception ignored) {}
        batch.end();
    }

    /**
     * Clear all registered JS visuals (called when loading a new level)
     */
    public static void clearAll() {
        // Dispose textures we created in sharedFrames
        if (sharedFrames != null) {
            for (TextureRegion tr : sharedFrames) {
                try {
                    Texture t = tr.getTexture();
                    if (t != null) { t.dispose(); }
                } catch (Exception ignored) {}
            }
            sharedFrames = null;
        }
        if (sharedCaughtTex != null) {
            try { sharedCaughtTex.dispose(); } catch (Exception ignored) {}
            sharedCaughtTex = null;
        }
        instances.clear();
        // Reset global caught lock on level clear so future levels can catch again
        anyCaughtActive = false;
    }
}
