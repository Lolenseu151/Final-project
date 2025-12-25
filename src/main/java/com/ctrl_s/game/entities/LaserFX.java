package com.ctrl_s.game.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Animated laser visual effects.
 * Loads laser.png sprite sheet and animates it.
 */
public class LaserFX {
    private Rectangle rect;
    private Texture spriteSheet;
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private float frameDuration = 0.1f;
    private int frameWidth = 32; // adjust based on your sprite sheet
    private int frameHeight = 32; // adjust based on your sprite sheet
    private int frameCount = 1;

    public LaserFX(Rectangle r) {
        this(r, 0.1f);
    }

    public LaserFX(Rectangle r, float frameDuration) {
        this.rect = r == null ? new Rectangle(0, 0, 20, 100) : new Rectangle(r);
        this.frameDuration = frameDuration;
        loadSpriteSheet();
        Gdx.app.log("LaserFX",
                "Created LaserFX at (" + rect.x + ", " + rect.y + ") size: " + rect.width + "x" + rect.height);
    }

    public void setFrameDuration(float d) {
        // Note: Cannot change animation frame duration after creation in LibGDX
        // Frame duration must be set during construction
        Gdx.app.log("LaserFX",
                "Warning: Frame duration cannot be changed after animation creation. Use constructor with duration parameter.");
    }

    private void loadSpriteSheet() {
        String[] paths = {
                "assets/sprites/fx/laser.png",
                "laser.png",
                "Lasers/laser.png"
        };

        for (String path : paths) {
            try {
                FileHandle fh = Gdx.files.internal(path);
                if (fh.exists()) {
                    spriteSheet = new Texture(fh);
                    spriteSheet.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

                    // Calculate frame count based on texture dimensions
                    int texWidth = spriteSheet.getWidth();
                    int texHeight = spriteSheet.getHeight();

                    Gdx.app.log("LaserFX", "Texture size: " + texWidth + "x" + texHeight);

                    // For 5 frames in a horizontal sprite sheet
                    frameCount = 5;
                    frameWidth = texWidth / frameCount; // Divide total width by 5
                    frameHeight = texHeight; // Full height for each frame

                    Gdx.app.log("LaserFX", "Calculated frame size: " + frameWidth + "x" + frameHeight + " for "
                            + frameCount + " frames");

                    // Manually create texture regions for each frame
                    Array<TextureRegion> frames = new Array<>();

                    for (int i = 0; i < frameCount; i++) {
                        int xPos = i * frameWidth;
                        TextureRegion frame = new TextureRegion(spriteSheet, xPos, 0, frameWidth, frameHeight);
                        frames.add(frame);
                        Gdx.app.log("LaserFX", "Created frame " + i + " at x=" + xPos + " size: "
                                + frame.getRegionWidth() + "x" + frame.getRegionHeight());
                    }

                    Gdx.app.log("LaserFX", "Total frames created: " + frames.size);

                    if (frames.size > 0) {
                        animation = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
                        Gdx.app.log("LaserFX",
                                "Animation created with " + frames.size + " frames, duration: " + frameDuration);
                    } else {
                        Gdx.app.error("LaserFX", "ERROR: No frames were added to animation!");
                    }

                    Gdx.app.log("LaserFX", "Loaded laser sprite sheet: " + path + " (" + frameCount + " frames)");
                    return;
                }
            } catch (Exception e) {
                Gdx.app.error("LaserFX", "Error loading: " + path, e);
            }
        }

        Gdx.app.error("LaserFX", "Could not load laser.png sprite sheet");
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void render(SpriteBatch batch) {
        if (animation != null && rect != null) {
            TextureRegion currentFrame = animation.getKeyFrame(stateTime, true);
            if (currentFrame != null) {
                // Render single animated frame at full size
                int frameIndex = animation.getKeyFrameIndex(stateTime);
                Gdx.app.log("LaserFX", "Rendering frame " + frameIndex + " at time " + stateTime + " - region: "
                        + currentFrame.getRegionWidth() + "x" + currentFrame.getRegionHeight());
                batch.draw(currentFrame, rect.x, rect.y, rect.width, rect.height);
            } else {
                Gdx.app.error("LaserFX", "Animation exists but currentFrame is null!");
            }
        } else {
            if (animation == null) {
                Gdx.app.error("LaserFX", "Animation is null - sprite sheet failed to load or split!");
            }
            if (rect == null) {
                Gdx.app.error("LaserFX", "Rectangle is null!");
            }
        }
    }

    public void dispose() {
        if (spriteSheet != null) {
            spriteSheet.dispose();
            spriteSheet = null;
        }
    }

    public boolean hasVisual() {
        return animation != null;
    }

    public Rectangle getRect() {
        return rect;
    }

    public void setRect(Rectangle r) {
        if (r != null)
            this.rect.set(r);
    }
}
