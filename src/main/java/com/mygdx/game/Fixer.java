package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.io.File;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * The Fixer - The player character working for "Loloy the Crocodile"
 * Responsible for collecting incriminating documents and avoiding obstacles.
 */

public class Fixer {

    // Physics constants
    private static final float MOVE_SPEED = 200f;
    private static final float JUMP_VELOCITY = 500f;
    private static final float MAX_JUMP_HEIGHT = 200f;
    private static final float GRAVITY = 800f;
    private static final float FRICTION = 0.80f;
    private static final float DASH_SPEED = 800f;
    private static final float DASH_DURATION = 0.15f;
    private static final float WALL_SLIDE_SPEED = -50f;
    private static final float DASH_COOLDOWN_TIME = 1f;

    // Visual properties
    private static final float WIDTH = 40f;
    private static final float HEIGHT = 40f;
    private static final float FRAME_DURATION = 0.1f;
    private static final int FRAME_W = 48;
    private static final int FRAME_H = 64;

    // Physics and position
    private final Rectangle bounds;
    private final Vector2 velocity;
    private boolean isOnGround;
    private boolean canJump;
    private boolean isDashing;
    private boolean isWallSliding;
    private boolean isSlowed;
    private float initialJumpY;
    private float dashTimer;
    private float dashCooldown;

    // Sprite and animation
    private Texture playerSheet;
    private Animation<TextureRegion> runAnimation;
    private Animation<TextureRegion> idleAnimation;
    private TextureRegion jumpFrame;
    private TextureRegion currentFrame;
    private float stateTimer;
    private boolean facingRight = true;

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
        this.stateTimer = 0f;

        // --- robust texture loading ---
        Texture sheet = null;
        try {
            // candidate internal paths
            String[] candidates = new String[] {
                "MainChar.png",
                "assets/MainChar.png",
                "desktop/assets/MainChar.png",
                "android/assets/MainChar.png"
            };

            FileHandle fh = null;
            for (String c : candidates) {
                FileHandle trial = Gdx.files.internal(c);
                Gdx.app.log("Fixer", "trying internal candidate: " + c + " exists=" + trial.exists());
                if (trial.exists()) {
                    fh = trial;
                    break;
                }
            }

            // try absolute fallback (useful when running from IDE where working dir differs)
            if (fh == null) {
                File abs = new File("assets/MainChar.png");
                if (abs.exists()) {
                    Gdx.app.log("Fixer", "found absolute candidate: " + abs.getAbsolutePath());
                    fh = Gdx.files.absolute(abs.getAbsolutePath());
                } else {
                    // explicit project-root fallback
                    File rootAbs = new File("C:\\GameDev\\Final-project\\assets\\MainChar.png");
                    if (rootAbs.exists()) {
                        fh = Gdx.files.absolute(rootAbs.getAbsolutePath());
                        Gdx.app.log("Fixer", "found explicit fallback: " + rootAbs.getAbsolutePath());
                    }
                }
            }

            if (fh == null) {
                throw new RuntimeException("MainChar.png not found in candidate locations");
            }

            sheet = new Texture(fh);
            Gdx.app.log("Fixer", "Loaded sprite sheet: " + sheet.getWidth() + "x" + sheet.getHeight());
        } catch (Exception e) {
            Gdx.app.error("Fixer", "Failed to load animations", e);
            sheet = null;
        }

        // --- safe split & animation creation (guard indexes) ---
        if (sheet != null) {
            try {
                TextureRegion[][] tmp = TextureRegion.split(sheet, FRAME_W, FRAME_H);
                if (tmp != null && tmp.length > 0 && tmp[0].length > 0) {
                    // safe guards: check rows/cols exist before accessing
                    if (tmp[0].length > 0) idleAnimation = new Animation<>(FRAME_DURATION, tmp[0][0]);
                    if (tmp[0].length > 2) {
                        // example: run uses frames 1..2 if available
                        runAnimation = new Animation<>(FRAME_DURATION, tmp[0][1], tmp[0][2]);
                    }
                    if (tmp[0].length > 3) jumpFrame = tmp[0][3];
                    // set a sensible currentFrame fallback
                    if (idleAnimation != null) currentFrame = idleAnimation.getKeyFrame(0f);
                    else if (tmp[0].length > 0) currentFrame = tmp[0][0];
                    else currentFrame = new TextureRegion(sheet);
                } else {
                    Gdx.app.error("Fixer", "Sprite split produced no frames; using full texture");
                    currentFrame = new TextureRegion(sheet);
                }
                playerSheet = sheet;
            } catch (Exception e) {
                Gdx.app.error("Fixer", "Error while splitting/creating animations", e);
                playerSheet = sheet;
                currentFrame = new TextureRegion(sheet);
            }
        } else {
            // no sheet loaded -> keep currentFrame null and rely on debug rect fallback
            playerSheet = null;
            currentFrame = null;
        }
        // --- end robust loading ---

        // other initialization as needed...
    }

    /**
     * Updates the Fixer's physics and state, and selects the current animation frame.
     * @param deltaTime Time since last update (in seconds)
     */
    public void update(float deltaTime) {
        stateTimer += deltaTime;

        if (dashCooldown > 0) {
            dashCooldown -= deltaTime;
        }

        handleInput(deltaTime);
        applyPhysics(deltaTime);
        updatePosition(deltaTime);
        checkBounds();

        // Select animation frame based on state
        TextureRegion regionToUse;
        if (velocity.y != 0) { // Airborne (Jumping or Falling)
            regionToUse = jumpFrame;
        } else if (Math.abs(velocity.x) > 0) { // Moving Horizontally (Running)
            regionToUse = runAnimation.getKeyFrame(stateTimer, true);
        } else { // Static (Idle)
            regionToUse = idleAnimation.getKeyFrame(stateTimer, true);
        }

        // Handle sprite flipping (MUST clone the region to flip correctly without affecting the original in the array)
        TextureRegion currentRegion = new TextureRegion(regionToUse);

        if (velocity.x < 0 && facingRight) {
            facingRight = false;
            if (!currentRegion.isFlipX()) {
                 currentRegion.flip(true, false);
            }
        } else if (velocity.x > 0 && !facingRight) {
            facingRight = true;
            if (currentRegion.isFlipX()) {
                 currentRegion.flip(true, false);
            }
        } 
        // Ensure that if the player is idle but facing left, they still look left.
        else if (!facingRight && !currentRegion.isFlipX()) {
             currentRegion.flip(true, false);
        }

        currentFrame = currentRegion;
    }

    /**
     * Handles player input for movement and actions
     */
    private void handleInput(float deltaTime) {
        if (isDashing) {
            dashTimer -= deltaTime;
            if (dashTimer <= 0) {
                isDashing = false;
            }
        }

        // Denial Dash - Space bar (only if not already dashing)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isDashing && dashCooldown <= 0) {
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
                
                // Only reset velocity if the player isn't moving due to friction/physics
                if(Math.abs(velocity.x) < 50f) velocity.x = 0; 

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
            
            // Dash direction based on last movement or default right
            float dashDirection = (velocity.x != 0) ? Math.signum(velocity.x) : (facingRight ? 1f : -1f);
            velocity.x = dashDirection * DASH_SPEED;
            velocity.y = 0; // Prevent vertical movement while dashing
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

    // --- Rendering Methods ---

    /**
     * Draws the Fixer's current animation frame using the game's SpriteBatch.
     * @param batch The game's central SpriteBatch.
     */
    public void draw(SpriteBatch batch) {
        if (batch != null && currentFrame != null) {
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /**
     * Renders the Fixer's collision box for debugging.
     * @param shapeRenderer The ShapeRenderer object (should be used inside a begin/end block in GameScreen)
     */
    public void renderDebug(ShapeRenderer shapeRenderer) {
        if (shapeRenderer == null) return;
        shapeRenderer.setColor(1, 0, 1, 0.5f);
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    // --- Utility Methods ---

    /**
     * Disposes of the texture to free up memory. Called from GameScreen.dispose().
     */
    public void dispose() {
        if (playerSheet != null) {
            playerSheet.dispose();
        }
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

    // Getters
    public Rectangle getBounds() { return bounds; }
    public Vector2 getVelocity() { return velocity; }
    public float getX() { return bounds.x; }
    public float getY() { return bounds.y; }
    public boolean isOnGround() { return isOnGround; }
    public boolean isDashing() { return isDashing; }
    public boolean isWallSliding() { return isWallSliding; }
    public TextureRegion getCurrentFrame() {
        return currentFrame;
    }

    // public render method used by GameScreen (uses provided SpriteBatch)
    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch) {
        if (batch != null && currentFrame != null) {
            batch.begin();
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
            batch.end();
            return;
        }
        // fallback debug rect
        if (shapeRenderer != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(1, 0, 1, 0.5f);
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            shapeRenderer.end();
        }
    }

    // Setters (for external physics/collision)
    public void setOnGround(boolean onGround) {
        this.isOnGround = onGround;
        if (onGround) this.canJump = true;
    }
    public void setVelocityY(float vy) { velocity.y = vy; }
    public void setVelocityX(float vx) { velocity.x = vx; }
    public void setSlowed(boolean slowed) { this.isSlowed = slowed; }
}