package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Loading Screen - Shows loading progress while assets are loaded
 */
public class LoadingScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    private float progress = 0f;
    private float timeElapsed = 0f;
    private float animTimer = 0f;
    private static final float MIN_LOAD_TIME = 8.0f; // Minimum time to show loading screen
    
    // Background animation frames (7 -> 1, reverse order)
    private Array<Texture> bgFrames = new Array<>();
    private Animation<TextureRegion> bgAnimation;
    private float bgAnimTime = 0f;
    
    public LoadingScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        loadBackgroundFrames();
    }
    
    @Override
    public void render(float delta) {
        timeElapsed += delta;
        animTimer += delta;
        bgAnimTime += delta;

        progress = Math.min(1f, timeElapsed / MIN_LOAD_TIME);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackgroundAnimation();
        drawLoadingBar();
        drawCharacters();
        drawText();

        // After loading completes, go to MainMenuScreen (not GameScreen)
        if (progress >= 1f && timeElapsed >= MIN_LOAD_TIME) {
            try {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            } catch (Throwable t) {
                Gdx.app.error("LoadingScreen", "Failed to switch to MainMenuScreen", t);
            }
        }
    }
    
    private void loadBackgroundFrames() {
        // Load frames in reverse order: 15, 14, 13, ..., 2, 1
        String[] framePaths = {
            "LoadingScreenBg/15.png",
            "LoadingScreenBg/14.png",
            "LoadingScreenBg/13.png",
            "LoadingScreenBg/12.png",
            "LoadingScreenBg/11.png",
            "LoadingScreenBg/10.png",
            "LoadingScreenBg/9.png",
            "LoadingScreenBg/8.png",
            "LoadingScreenBg/7.png",
            "LoadingScreenBg/6.png",
            "LoadingScreenBg/5.png",
            "LoadingScreenBg/4.png",
            "LoadingScreenBg/3.png",
            "LoadingScreenBg/2.png",
            "LoadingScreenBg/1.png"
        };
        
        Array<TextureRegion> regions = new Array<>();
        
        for (String path : framePaths) {
            try {
                Texture tex = null;
                if (Gdx.files.internal(path).exists()) {
                    tex = new Texture(Gdx.files.internal(path));
                } else if (Gdx.files.internal("assets/" + path).exists()) {
                    tex = new Texture(Gdx.files.internal("assets/" + path));
                }
                
                if (tex != null) {
                    tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    bgFrames.add(tex);
                    regions.add(new TextureRegion(tex));
                    Gdx.app.log("LoadingScreen", "Loaded background frame: " + path);
                } else {
                    Gdx.app.error("LoadingScreen", "Could not find: " + path);
                }
            } catch (Exception e) {
                Gdx.app.error("LoadingScreen", "Error loading frame: " + path, e);
            }
        }
        
        if (regions.size > 0) {
            bgAnimation = new Animation<>(0.3f, regions, Animation.PlayMode.NORMAL); // Changed from 0.15f to 0.3f for slower animation
            Gdx.app.log("LoadingScreen", "Created background animation with " + regions.size + " frames");
        } else {
            Gdx.app.error("LoadingScreen", "No background frames loaded!");
        }
    }
    
    private void drawBackgroundAnimation() {
        if (bgAnimation != null && game.batch != null) {
            try {
                // Use false to stop looping - will freeze on last frame (frame 1.png)
                TextureRegion currentFrame = bgAnimation.getKeyFrame(bgAnimTime, false);
                if (currentFrame != null) {
                    game.batch.begin();
                    game.batch.draw(currentFrame, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    game.batch.end();
                }
            } catch (Exception e) {
                Gdx.app.error("LoadingScreen", "Error drawing background animation", e);
            }
        }
    }
    
    private void drawLoadingBar() {
        float barWidth = 400;
        float barHeight = 30;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2;
        float barY = Gdx.graphics.getHeight() / 2 - barHeight / 2;
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Background (empty part)
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        
        // Foreground (filled part)
        shapeRenderer.setColor(0, 0.8f, 0.8f, 1); // Cyan
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight);
        
        // Border
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, 1);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        
        shapeRenderer.end();
    }
    
    private void drawText() {
        // Use game's batch/font if available, otherwise create minimal local fallback
        com.badlogic.gdx.graphics.g2d.BitmapFont localFont = null;
        boolean usingLocalFont = false;

        if (game.batch == null) {
            Gdx.app.log("LoadingScreen", "game.batch is null - skipping drawText()");
            return;
        }

        if (game.font == null) {
            localFont = new com.badlogic.gdx.graphics.g2d.BitmapFont();
            usingLocalFont = true;
        }

        try {
            game.batch.begin();

            String loadingText = "Loading... " + (int)(progress * 100) + "%";
            com.badlogic.gdx.graphics.g2d.BitmapFont fontToUse = (game.font != null) ? game.font : localFont;

            if (fontToUse != null) {
                fontToUse.draw(game.batch, loadingText,
                    Gdx.graphics.getWidth() / 2 - 60,
                    Gdx.graphics.getHeight() / 2 - 60);
            }
        } catch (Throwable t) {
            Gdx.app.error("LoadingScreen", "Error drawing loading text", t);
        } finally {
            try { game.batch.end(); } catch (Exception ignored) {}
            if (usingLocalFont && localFont != null) localFont.dispose();
        }
    }
    
    private void drawCharacters() {
        // Placeholder for character drawing logic
        // This method will be used to animate and draw characters during the loading screen
    }
    
    @Override
    public void show() {
        Gdx.app.log("LoadingScreen", "Loading game assets...");
        // Preload tutorial images so switching to tutorial doesn't block
        try {
            String[] paths = new String[] {"The Urgent Call/1.png", "The Urgent Call/2.png", "The Urgent Call/3.png"};
            com.badlogic.gdx.graphics.Texture[] imgs = new com.badlogic.gdx.graphics.Texture[paths.length];
            for (int i = 0; i < paths.length; i++) {
                String p = paths[i];
                try {
                    imgs[i] = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal(p));
                    imgs[i].setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
                    Gdx.app.log("LoadingScreen", "Preloaded tutorial image internal: " + p + " (w=" + imgs[i].getWidth() + ", h=" + imgs[i].getHeight() + ")");
                } catch (Exception ex) {
                    String absPath = System.getProperty("user.dir") + "/assets/" + p;
                    try {
                        imgs[i] = new com.badlogic.gdx.graphics.Texture(Gdx.files.absolute(absPath));
                        imgs[i].setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
                        Gdx.app.log("LoadingScreen", "Preloaded tutorial image absolute: " + absPath + " (w=" + imgs[i].getWidth() + ", h=" + imgs[i].getHeight() + ")");
                    } catch (Exception ex2) {
                        Gdx.app.error("LoadingScreen", "Failed to preload tutorial image: " + p, ex2);
                        imgs[i] = null;
                    }
                }
            }
            game.tutorialImages = imgs;
        } catch (Throwable t) {
            Gdx.app.error("LoadingScreen", "Error preloading tutorial images", t);
        }
    }
    
    @Override
    public void resize(int width, int height) {}
    
    @Override
    public void pause() {}
    
    @Override
    public void resume() {}
    
    @Override
    public void hide() {}
    
    @Override
    public void dispose() {
        shapeRenderer.dispose();
        for (Texture tex : bgFrames) {
            try {
                tex.dispose();
            } catch (Exception ignored) {}
        }
        bgFrames.clear();
    }
}
