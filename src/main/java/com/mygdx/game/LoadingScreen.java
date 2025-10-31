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
        
        // When loading complete, switch to main menu
        if (progress >= 1f && timeElapsed >= MIN_LOAD_TIME) {
            game.setScreen(new MainMenuScreen(game));
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
        game.batch.begin();
        
        String title = "PAPER TRAIL PANIC";
        String loadingText = "Loading... " + (int)(progress * 100) + "%";
        
        // Title
        game.font.draw(game.batch, title, 
            Gdx.graphics.getWidth() / 2 - 80, 
            Gdx.graphics.getHeight() / 2 + 80);
        
        // Loading percentage
        game.font.draw(game.batch, loadingText, 
            Gdx.graphics.getWidth() / 2 - 60, 
            Gdx.graphics.getHeight() / 2 - 60);
        
        game.batch.end();
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
