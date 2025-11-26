package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

/**
 * Level Select Screen - choose level 1-5 with same font styling as MainMenuScreen
 */
public class LevelSelectScreen implements Screen {
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    private int selectedLevel = 1;
    private static final int MAX_LEVEL = 5;

    public LevelSelectScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        // Handle input
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedLevel = Math.max(1, selectedLevel - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedLevel = Math.min(MAX_LEVEL, selectedLevel + 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, selectedLevel));
            dispose();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }

        // Render
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.batch.begin();
        if (game.font != null) {
            // Title
            String title = "SELECT LEVEL";
            GlyphLayout titleLayout = new GlyphLayout(game.font, title);
            float titleX = (Gdx.graphics.getWidth() - titleLayout.width) * 0.5f;
            float titleY = Gdx.graphics.getHeight() - 100;
            game.font.draw(game.batch, titleLayout, titleX, titleY);

            // Level buttons (same styling as MainMenuScreen menu items)
            float centerY = Gdx.graphics.getHeight() / 2;
            float spacing = 120f;
            float startX = (Gdx.graphics.getWidth() - (spacing * (MAX_LEVEL - 1))) * 0.5f;

            for (int i = 1; i <= MAX_LEVEL; i++) {
                float x = startX + (i - 1) * spacing;
                String levelText = "LEVEL " + i;
                GlyphLayout layout = new GlyphLayout(game.font, levelText);

                if (i == selectedLevel) {
                    game.font.setColor(1f, 1f, 0f, 1f);  // Yellow highlight (selected)
                } else {
                    game.font.setColor(1f, 1f, 1f, 1f);  // White (unselected)
                }
                game.font.draw(game.batch, layout, x - layout.width * 0.5f, centerY);
            }

            // Instructions
            game.font.setColor(1f, 1f, 1f, 1f);
            String instructions = "LEFT/RIGHT: Select | ENTER: Start | ESC: Back";
            GlyphLayout instLayout = new GlyphLayout(game.font, instructions);
            float instX = (Gdx.graphics.getWidth() - instLayout.width) * 0.5f;
            game.font.draw(game.batch, instLayout, instX, 100);
        }
        game.batch.end();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}