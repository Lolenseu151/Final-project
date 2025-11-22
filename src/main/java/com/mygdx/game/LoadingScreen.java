package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Loading Screen - Shows loading progress while assets are loaded
 */
public class LoadingScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    private float progress = 0f;
    private float timeElapsed = 0f;
    private static final float MIN_LOAD_TIME = 1.5f; // Minimum time to show loading screen
    
    public LoadingScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }
    
    @Override
    public void render(float delta) {
        timeElapsed += delta;

        // Simulate loading progress (in a real app, you'd check AssetManager progress)
        progress = Math.min(1f, timeElapsed / MIN_LOAD_TIME);

        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw loading UI
        drawLoadingBar();
        drawText();

        // When loading complete, switch to main menu (guarded)
        if (progress >= 1f && timeElapsed >= MIN_LOAD_TIME) {
            try {
                // Defensive: ensure MainMenuScreen exists and won't throw
                game.setScreen(new MainMenuScreen(game));
            } catch (Throwable t) {
                Gdx.app.error("LoadingScreen", "Failed to switch to MainMenuScreen", t);
                // keep showing loading screen so you can read the log
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

            String title = "PAPER TRAIL PANIC";
            String loadingText = "Loading... " + (int)(progress * 100) + "%";
            com.badlogic.gdx.graphics.g2d.BitmapFont fontToUse = (game.font != null) ? game.font : localFont;

            if (fontToUse != null) {
                fontToUse.draw(game.batch, title,
                    Gdx.graphics.getWidth() / 2 - 80,
                    Gdx.graphics.getHeight() / 2 + 80);

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
    
    @Override
    public void show() {
        Gdx.app.log("LoadingScreen", "Loading game assets...");
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
    }
}
