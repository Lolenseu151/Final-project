package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class GameScreen implements Screen {
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
    private final Rectangle player;
    private final Vector2 velocity;
    private float remainingTime = 60; // 60 seconds game time
    private int score = 0;
    private boolean isOnGround = false;
    private boolean canJump = true;
    private static final float JUMP_VELOCITY = 500f;
    private static final float MAX_JUMP_HEIGHT = 200f;
    private float initialJumpY = 0f;

    public GameScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
        this.player = new Rectangle(100, 100, 40, 40);
        this.velocity = new Vector2();
    }

    @Override
    public void render(float delta) {
        // Update
        updatePlayer(delta);
        updateTime(delta);
        
        // Clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Enable blending for alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw in correct order
        drawPlayer();
        drawText();

        // Disable blending
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updatePlayer(float delta) {
        // Apply momentum-based movement
        float moveSpeed = 1000f * delta;
        float friction = 0.98f;
        float gravity = 800f; // pixels per second squared

        // Debug state
        if (Gdx.app.getLogLevel() >= Gdx.app.LOG_DEBUG) {
            Gdx.app.debug("GameScreen", String.format(
                "Position: (%.2f, %.2f), Velocity: (%.2f, %.2f), OnGround: %b, CanJump: %b",
                player.x, player.y, velocity.x, velocity.y, isOnGround, canJump));
        }

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) 
            velocity.x -= moveSpeed;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) 
            velocity.x += moveSpeed;
        
        // Jump logic
        if ((Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP) || 
            Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SPACE)) && 
            isOnGround && canJump) {
            velocity.y = JUMP_VELOCITY;
            isOnGround = false;
            canJump = false;
            initialJumpY = player.y;
        }

        // Limit jump height by checking distance from jump start
        if (!isOnGround && !canJump && player.y - initialJumpY >= MAX_JUMP_HEIGHT) {
            velocity.y = Math.min(velocity.y, 0);
        }

        // Apply gravity
        velocity.y -= gravity * delta;

        // Apply friction (only to horizontal movement)
        velocity.x *= friction;

        // Update position
        player.x += velocity.x * delta;
        player.y += velocity.y * delta;

        // Keep player in bounds and check ground collision
        if (player.x < 0) {
            player.x = 0;
            velocity.x = 0;
        }
        if (player.x > Gdx.graphics.getWidth() - player.width) {
            player.x = Gdx.graphics.getWidth() - player.width;
            velocity.x = 0;
        }
        if (player.y < 0) {
            player.y = 0;
            velocity.y = 0;
            isOnGround = true;
            canJump = true;
        }
        if (player.y > Gdx.graphics.getHeight() - player.height) {
            player.y = Gdx.graphics.getHeight() - player.height;
            velocity.y = 0;
        }
    }

    private void updateTime(float delta) {
        remainingTime -= delta;
        if (remainingTime <= 0) {
            remainingTime = 0;
            // TODO: Implement game over state
        }
    }

    private void drawPlayer() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 1, 1); // Cyan color
        shapeRenderer.rect(player.x, player.y, player.width, player.height);
        shapeRenderer.end();
    }

    private void drawText() {
        game.batch.begin();
        game.font.draw(game.batch, "Score: " + score, 50, Gdx.graphics.getHeight() - 20);
        game.font.draw(game.batch, "Time: " + String.format("%.1f", remainingTime), 
            Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 20);
        game.batch.end();
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}