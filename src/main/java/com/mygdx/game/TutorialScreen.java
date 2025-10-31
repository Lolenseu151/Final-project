package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Tutorial Screen - Explains game mechanics and controls
 */
public class TutorialScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    private int currentPage = 0;
    private static final int TOTAL_PAGES = 3;
    
    public TutorialScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Clear screen
        Gdx.gl.glClearColor(0.15f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        drawTutorial();
    }
    
    private void handleInput() {
        // Next page
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || 
            Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            if (currentPage < TOTAL_PAGES - 1) {
                currentPage++;
            }
        }
        
        // Previous page
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || 
            Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            if (currentPage > 0) {
                currentPage--;
            }
        }
        
        // Back to menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || 
            Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }
    
    private void drawTutorial() {
        float centerX = Gdx.graphics.getWidth() / 2;
        float centerY = Gdx.graphics.getHeight() / 2;
        
        // Draw background panel
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.15f, 0.25f, 0.9f);
        shapeRenderer.rect(50, 50, Gdx.graphics.getWidth() - 100, Gdx.graphics.getHeight() - 100);
        shapeRenderer.end();
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.5f, 0.3f, 0.7f, 1); // Purple border
        shapeRenderer.rect(50, 50, Gdx.graphics.getWidth() - 100, Gdx.graphics.getHeight() - 100);
        shapeRenderer.end();
        
        // Draw text content
        game.batch.begin();
        
        game.font.draw(game.batch, "TUTORIAL - Page " + (currentPage + 1) + "/" + TOTAL_PAGES, 
            centerX - 80, Gdx.graphics.getHeight() - 80);
        
        switch (currentPage) {
            case 0:
                drawPage1();
                break;
            case 1:
                drawPage2();
                break;
            case 2:
                drawPage3();
                break;
        }
        
        // Navigation instructions
        game.font.draw(game.batch, "LEFT/RIGHT or A/D: Change page | ESC or Q: Back to menu", 
            centerX - 220, 80);
        
        game.batch.end();
    }
    
    private void drawPage1() {
        float x = 100;
        float y = Gdx.graphics.getHeight() - 150;
        
        game.font.draw(game.batch, "MISSION BRIEFING:", x, y);
        y -= 40;
        game.font.draw(game.batch, "You are 'The Fixer', working for Loloy the Crocodile.", x, y);
        y -= 30;
        game.font.draw(game.batch, "Your job: Collect all incriminating documents and", x, y);
        y -= 30;
        game.font.draw(game.batch, "shred them before the auditors arrive!", x, y);
        y -= 50;
        game.font.draw(game.batch, "OBJECTIVE:", x, y);
        y -= 30;
        game.font.draw(game.batch, "1. Collect all 7 documents scattered across platforms", x, y);
        y -= 30;
        game.font.draw(game.batch, "2. Reach the shredder (goal) with all documents", x, y);
        y -= 30;
        game.font.draw(game.batch, "3. Complete before time runs out (3 minutes)", x, y);
    }
    
    private void drawPage2() {
        float x = 100;
        float y = Gdx.graphics.getHeight() - 150;
        
        game.font.draw(game.batch, "CONTROLS:", x, y);
        y -= 40;
        game.font.draw(game.batch, "MOVEMENT:", x, y);
        y -= 30;
        game.font.draw(game.batch, "  Arrow Keys or WASD - Move left/right and jump", x, y);
        y -= 30;
        game.font.draw(game.batch, "  SPACE - Denial Dash (quick burst of speed)", x, y);
        y -= 50;
        game.font.draw(game.batch, "GAME CONTROLS:", x, y);
        y -= 30;
        game.font.draw(game.batch, "  P - Pause/Resume game", x, y);
        y -= 30;
        game.font.draw(game.batch, "  R - Restart (when game over)", x, y);
        y -= 50;
        game.font.draw(game.batch, "UI SHORTCUTS:", x, y);
        y -= 30;
        game.font.draw(game.batch, "  F5 - Reload UI (development)", x, y);
        y -= 30;
        game.font.draw(game.batch, "  F6 - Toggle UI debug mode", x, y);
    }
    
    private void drawPage3() {
        float x = 100;
        float y = Gdx.graphics.getHeight() - 150;
        
        game.font.draw(game.batch, "OBSTACLES & HAZARDS:", x, y);
        y -= 40;
        game.font.draw(game.batch, "RED TAPE (red obstacles):", x, y);
        y -= 30;
        game.font.draw(game.batch, "  Slows you down when touched", x, y);
        y -= 30;
        game.font.draw(game.batch, "  Try to avoid or dash through them", x, y);
        y -= 50;
        game.font.draw(game.batch, "AUDITOR BEAMS (yellow vertical beams):", x, y);
        y -= 30;
        game.font.draw(game.batch, "  Adds time penalty when you pass through", x, y);
        y -= 30;
        game.font.draw(game.batch, "  Find alternate routes or use dash", x, y);
        y -= 50;
        game.font.draw(game.batch, "TIPS:", x, y);
        y -= 30;
        game.font.draw(game.batch, "  - Plan your route before moving", x, y);
        y -= 30;
        game.font.draw(game.batch, "  - Use platforms to reach higher documents", x, y);
        y -= 30;
        game.font.draw(game.batch, "  - Save your dash for tough spots", x, y);
    }
    
    @Override
    public void show() {
        Gdx.app.log("TutorialScreen", "Tutorial displayed");
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
