package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * The Fixer - The player character working for "Loloy the Crocodile"
 * Responsible for collecting incriminating documents and avoiding obstacles.
 */
public class Fixer {
    
    // Physics and position
    private final Rectangle bounds;      // Collision box
    private final Vector2 velocity;      // Current velocity (pixels/second)
    
    // Physics constants
    private static final float MOVE_SPEED = 200f;      // Reduced from 300f
    private static final float JUMP_VELOCITY = 500f;    // Initial jump velocity
    private static final float MAX_JUMP_HEIGHT = 200f;  // Maximum jump height
    private static final float GRAVITY = 800f;          // Gravity (pixels/second²)
    private static final float FRICTION = 0.80f;        // Increased friction
    private static final float DASH_SPEED = 800f;       // Denial Dash speed
    private static final float DASH_DURATION = 0.15f;   // Dash duration in seconds
    private static final float WALL_SLIDE_SPEED = -50f; // Wall slide speed
    private static final float DASH_COOLDOWN_TIME = 1f; // Dash cooldown time
    
    // State flags
    private boolean isOnGround;
    private boolean canJump;
    private boolean isDashing;
    private boolean isWallSliding;
    private boolean isSlowed; // set by obstacles (red tape)
    private float initialJumpY;
    private float dashTimer;
    private float dashCooldown;
    
    // Visual properties
    private static final float WIDTH = 40f;
    private static final float HEIGHT = 40f;
    
    /**
     * Creates a new Fixer at the specified position
     * @param x Starting X position
     * @param y Starting Y position
     */
    public Fixer(float x, float y) {
        this.bounds = new Rectangle(x, y, WIDTH, HEIGHT);
        this.velocity = new Vector2();
        this.isOnGround = false;
        this.canJump = true;
        this.isDashing = false;
        this.isSlowed = false;
        this.dashTimer = 0f;
        this.isWallSliding = false;
        this.dashCooldown = 0f;
    }
    
    /**
     * Updates the Fixer's physics and state
     * @param deltaTime Time since last update (in seconds)
     */
    public void update(float deltaTime) {
        if (dashCooldown > 0) {
            dashCooldown -= deltaTime;
        }
        
        handleInput(deltaTime);
        applyPhysics(deltaTime);
        updatePosition(deltaTime);
        checkBounds();
    }
    
    /**
     * Handles player input for movement and actions
     */
    private void handleInput(float deltaTime) {
        // Update dash timer
        if (isDashing) {
            dashTimer -= deltaTime;
            if (dashTimer <= 0) {
                isDashing = false;
            }
        }
        
        // Denial Dash - Space bar (only if not already dashing)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isDashing) {
            performDash();
            return; // Skip normal movement during dash
        }
        
        // Normal movement (only if not dashing)
            if (!isDashing) {
            // Reset horizontal velocity when no keys are pressed
            if (!Gdx.input.isKeyPressed(Input.Keys.LEFT) && 
                !Gdx.input.isKeyPressed(Input.Keys.RIGHT) && 
                !Gdx.input.isKeyPressed(Input.Keys.A) && 
                !Gdx.input.isKeyPressed(Input.Keys.D)) {
                velocity.x = 0;
            } else {
                // Left/Right movement with direct speed setting
                float speedFactor = isSlowed ? 0.5f : 1f;
                if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                    velocity.x = -MOVE_SPEED * speedFactor;
                }
                if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                    velocity.x = MOVE_SPEED * speedFactor;
                }
            }
            
            // Jump - Up arrow or W (only if on ground)
            if ((Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) 
                && isOnGround && canJump) {
                velocity.y = JUMP_VELOCITY;
                isOnGround = false;
                canJump = false;
                initialJumpY = bounds.y;
                Gdx.app.log("Fixer", "Jump initiated");
            }
        }
    }
    
    /**
     * Performs the Denial Dash - a quick burst of speed
     */
    private void performDash() {
        if (dashCooldown <= 0) {
            isDashing = true;
            dashTimer = DASH_DURATION;
            dashCooldown = DASH_COOLDOWN_TIME;
            
            // Dash in the direction of movement, or right if stationary
            float dashDirection = (velocity.x != 0) ? Math.signum(velocity.x) : 1f;
            velocity.x = dashDirection * DASH_SPEED;
            
            Gdx.app.log("Fixer", "Denial Dash!");
        }
    }
    
    /**
     * Applies physics (gravity, friction)
     */
    private void applyPhysics(float deltaTime) {
        // Limit jump height
        if (!isOnGround && !canJump && bounds.y - initialJumpY >= MAX_JUMP_HEIGHT) {
            velocity.y = Math.min(velocity.y, 0);
        }
        
        // Apply gravity
        velocity.y -= GRAVITY * deltaTime;
        
        // Apply friction only in the air or during dash
        if (!isOnGround || isDashing) {
            velocity.x *= FRICTION;
        }
        
        // Apply wall slide
        if (isWallSliding) {
            velocity.y = Math.max(velocity.y, WALL_SLIDE_SPEED);
        }
    }
    
    /**
     * Updates position based on velocity
     */
    private void updatePosition(float deltaTime) {
        bounds.x += velocity.x * deltaTime;
        bounds.y += velocity.y * deltaTime;
    }
    
    /**
     * Checks and enforces screen boundaries
     */
    private void checkBounds() {
        // Left/Right boundaries
        if (bounds.x < 0) {
            bounds.x = 0;
            velocity.x = 0;
        }
        if (bounds.x > Gdx.graphics.getWidth() - bounds.width) {
            bounds.x = Gdx.graphics.getWidth() - bounds.width;
            velocity.x = 0;
        }
        
        // Bottom boundary (fall off the level - reset position)
        if (bounds.y < -100) {
            bounds.y = 100; // Respawn at safe height
            velocity.y = 0;
            Gdx.app.log("Fixer", "Fell off level! Respawning...");
        }
        
        // Ceiling collision
        if (bounds.y > Gdx.graphics.getHeight() - bounds.height) {
            bounds.y = Gdx.graphics.getHeight() - bounds.height;
            velocity.y = 0;
        }
        
        // Check for wall sliding
        isWallSliding = (bounds.x <= 0 || bounds.x >= Gdx.graphics.getWidth() - bounds.width) 
            && !isOnGround && velocity.y < 0;
    }
    
    /**
     * Renders the Fixer
     * @param shapeRenderer The renderer to use
     */
    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Change color based on state
        if (isDashing) {
            shapeRenderer.setColor(1, 1, 0, 1); // Yellow when dashing
        } else if (isWallSliding) {
            shapeRenderer.setColor(0, 0.8f, 1, 1); // Light blue when wall sliding
        } else {
            shapeRenderer.setColor(0, 1, 1, 1); // Cyan normally
        }
        
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Draw dash cooldown indicator
        if (dashCooldown > 0) {
            shapeRenderer.setColor(1, 0, 0, 0.5f);
            float cooldownHeight = (bounds.height * dashCooldown) / DASH_COOLDOWN_TIME;
            shapeRenderer.rect(bounds.x - 5, bounds.y, 3, cooldownHeight);
        }
        
        shapeRenderer.end();
    }
    
    /**
     * Resets the Fixer to initial state
     * @param x Starting X position
     * @param y Starting Y position
     */
    public void reset(float x, float y) {
        bounds.setPosition(x, y);
        velocity.set(0, 0);
        isOnGround = false;
        canJump = true;
        isDashing = false;
        dashTimer = 0f;
        isWallSliding = false;
        dashCooldown = 0f;
    }

    // Allow external systems (like LevelManager) to slow the player
    public void setSlowed(boolean slowed) {
        this.isSlowed = slowed;
    }
    
    // Getters
    public Rectangle getBounds() { return bounds; }
    public Vector2 getVelocity() { return velocity; }
    public float getX() { return bounds.x; }
    public float getY() { return bounds.y; }
    public boolean isOnGround() { return isOnGround; }
    public boolean isDashing() { return isDashing; }
    public boolean isWallSliding() { return isWallSliding; }
    
    // Setters (for external physics/collision)
    public void setOnGround(boolean onGround) { 
        this.isOnGround = onGround;
        if (onGround) {
            this.canJump = true;
        }
    }
    
    public void setVelocityY(float vy) { velocity.y = vy; }
    public void setVelocityX(float vx) { velocity.x = vx; }
}
