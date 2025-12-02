package com.mygdx.game.Levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * State-aware Shredder visual component.
 *
 * Visual rules implemented:
 * - IDLE: display frame 1 (shredderFx/1.png)
 * - READY: display frame 10 (shredderFx/10.png)
 * - ACTIVE: loop frames 2..8 (shredderFx/2.png .. shredderFx/8.png)
 */
public class Shredder {

    public enum State { IDLE, READY, ACTIVE }

    private Rectangle rect;
    private Array<Texture> loadedTextures = new Array<>();
    private TextureRegion idleRegion;   // frame 1
    private TextureRegion readyRegion;  // frame 10
    private Animation<TextureRegion> activeAnim; // frames 2..8
    private Animation<TextureRegion> genericAnim; // fallback if active not present
    private float stateTime = 0f;
    private float frameDuration = 0.08f;
    private State state = State.IDLE;

    public Shredder() {
        this.rect = new Rectangle(0,0,48,48);
    }

    public Shredder(Rectangle r) {
        this.rect = r == null ? new Rectangle(0,0,48,48) : new Rectangle(r);
    }

    public Rectangle getRect() { return rect; }

    public void setRect(Rectangle r) { if (r != null) this.rect.set(r); }

    public void setFrameDuration(float d) { if (d > 0f) this.frameDuration = d; }

    private Texture tryLoadTextureForIndex(String folder, int i) {
        if (folder == null) return null;
        String[] candidates = new String[] {
            folder + "/" + i + ".png",
            folder + "/" + i + ".PNG",
            folder + "/" + i + ".jpg",
            folder + "/" + i + ".jpeg",
            "assets/" + folder + "/" + i + ".png",
        };
        for (String c : candidates) {
            try {
                FileHandle fh = Gdx.files.internal(c);
                if (fh.exists()) {
                    Texture tx = new Texture(fh);
                    try { tx.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); } catch (Exception ignored) {}
                    return tx;
                }
            } catch (Exception ignored) {}
            try {
                FileHandle fh2 = Gdx.files.absolute(c);
                if (fh2.exists()) {
                    Texture tx2 = new Texture(fh2);
                    try { tx2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); } catch (Exception ignored) {}
                    return tx2;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Load frames from a folder and prepare the Idle/Ready/Active visuals.
     * Attempts to load 1..maxFrames and explicitly tries to load frame 10.
     */
    public void loadFromFolder(String folder, int maxFrames) {
        disposeTextures();
        if (folder == null) return;

        Array<TextureRegion> allRegs = new Array<>();
        for (int i = 1; i <= maxFrames; i++) {
            Texture t = tryLoadTextureForIndex(folder, i);
            if (t != null) {
                loadedTextures.add(t);
                allRegs.add(new TextureRegion(t));
            } else {
                allRegs.add(null);
            }
        }

        // try frame 10 separately
        Texture readyTex = null;
        if (maxFrames < 10 || allRegs.size < 10 || allRegs.get(9) == null) {
            readyTex = tryLoadTextureForIndex(folder, 10);
            if (readyTex != null) loadedTextures.add(readyTex);
        }

        // set idle (frame 1)
        if (allRegs.size >= 1 && allRegs.get(0) != null) idleRegion = allRegs.get(0);
        // set ready (frame 10)
        if (allRegs.size >= 10 && allRegs.get(9) != null) readyRegion = allRegs.get(9);
        else if (readyTex != null) readyRegion = new TextureRegion(readyTex);

        // active frames 2..8
        Array<TextureRegion> activeRegs = new Array<>();
        for (int j = 2; j <= 8; j++) {
            int idx = j - 1;
            if (idx < allRegs.size) {
                TextureRegion r = allRegs.get(idx);
                if (r != null) activeRegs.add(r);
            }
        }
        if (activeRegs.size > 0) activeAnim = new Animation<TextureRegion>(frameDuration, activeRegs);

        // generic fallback
        Array<TextureRegion> genericRegs = new Array<>();
        for (TextureRegion tr : allRegs) if (tr != null) genericRegs.add(tr);
        if (genericRegs.size > 0) genericAnim = new Animation<TextureRegion>(frameDuration, genericRegs);

        stateTime = 0f;

        // optional: resize rect to first frame
        try {
            Texture first = (loadedTextures.size > 0) ? loadedTextures.get(0) : null;
            if (first != null) {
                float texW = first.getWidth();
                float texH = first.getHeight();
                float scale = 1f;
                if (texW > 128 || texH > 128) scale = 0.5f;
                rect.set(rect.x, rect.y, texW * scale, texH * scale);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Load a single texture as a fallback visual.
     */
    public void loadSingle(String path) {
        disposeTextures();
        if (path == null) return;
        Texture tex = null;
        try {
            if (Gdx.files.internal(path).exists()) tex = new Texture(Gdx.files.internal(path));
            else if (Gdx.files.absolute(path).exists()) tex = new Texture(Gdx.files.absolute(path));
        } catch (Exception ignored) {}
        if (tex != null) {
            loadedTextures.add(tex);
            TextureRegion reg = new TextureRegion(tex);
            idleRegion = reg;
            genericAnim = new Animation<TextureRegion>(frameDuration, new TextureRegion[] { reg });
            stateTime = 0f;
            try { rect.set(rect.x, rect.y, tex.getWidth(), tex.getHeight()); } catch (Exception ignored) {}
        }
    }

    public void update(float dt) {
        if (state == State.ACTIVE) {
            if (activeAnim != null) stateTime += dt;
            else if (genericAnim != null) stateTime += dt;
        }
    }

    public void setState(State s) {
        if (s == null) return;
        if (this.state != s) { this.state = s; this.stateTime = 0f; }
    }

    public void setIdle() { setState(State.IDLE); }
    public void setReady() { setState(State.READY); }
    public void setActive() { setState(State.ACTIVE); }

    public boolean hasVisual() {
        return idleRegion != null || readyRegion != null || activeAnim != null || genericAnim != null;
    }

    public void render(SpriteBatch batch) {
        try {
            if (rect == null) return;
            switch (state) {
                case IDLE:
                    if (idleRegion != null) { batch.draw(idleRegion, rect.x, rect.y, rect.width, rect.height); return; }
                    break;
                case READY:
                    if (readyRegion != null) { batch.draw(readyRegion, rect.x, rect.y, rect.width, rect.height); return; }
                    break;
                case ACTIVE:
                    if (activeAnim != null) {
                        TextureRegion f = activeAnim.getKeyFrame(stateTime, true);
                        if (f != null) { batch.draw(f, rect.x, rect.y, rect.width, rect.height); return; }
                    }
                    break;
            }
            // fallback
            if (genericAnim != null) {
                TextureRegion f = genericAnim.getKeyFrame(stateTime, true);
                if (f != null) { batch.draw(f, rect.x, rect.y, rect.width, rect.height); return; }
            }
        } catch (Exception ignored) {}
    }

    public Rectangle getCollisionRect() { return rect; }

    private void disposeTextures() {
        if (loadedTextures != null) {
            for (Texture t : loadedTextures) if (t != null) t.dispose();
            loadedTextures.clear();
        }
        idleRegion = null; readyRegion = null; activeAnim = null; genericAnim = null;
    }

    public void dispose() { disposeTextures(); }
}
