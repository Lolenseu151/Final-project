package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Fixer: simple character controller + animation state machine.
 * Uses 64x64 frames and running1/2/3 for run animation.
 */
public class Fixer {
    private enum State { IDLE, RUN, JUMP, FALL, DASH }

    // physics tunables
    private static final float MOVE_ACCEL = 1500f;   // px/s^2
    private static final float MAX_MOVE_SPEED = 220f; // px/s
    private static final float GROUND_FRICTION = 24f; // per second (increased to reduce sliding)
    private static final float AIR_DRAG = 1.0f; // lighter air drag
    private static final float JUMP_VY = 800f;  // Jump velocity - reasonable jump height
    private static final float GRAVITY = 1400f;
    private static final float DASH_SPEED = 1280f;  // Burst speed (3x character width = 192 pixels)
    private static final float DASH_TIME = 0.15f;  // Very short burst (150ms = quick dash)
    private static final float DASH_COOLDOWN = 10.0f;  // 10 second cooldown

    // Sprite / collision sizes (frames are 64x64 in assets)
    private static final float SPRITE_SIZE = 74f;  // Increased from 64f to 96f (1.5x larger)
    private static final float WIDTH = SPRITE_SIZE;
    private static final float HEIGHT = SPRITE_SIZE;

    private final Rectangle bounds;
    private Vector2 velocity = new Vector2();
    private boolean isOnGround = false;
    private boolean canJump = true;
    private boolean isSlowed = false;
    // when true, update() returns immediately so position/velocity do not change
    private boolean frozen = false;

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setSlowed(boolean slowed) {
        this.isSlowed = slowed;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public void setVelocityY(float vy) {
        velocity.y = vy;
    }

    public void setVelocityX(float vx) {
        velocity.x = vx;
    }

    public void setOnGround(boolean onGround) {
        this.isOnGround = onGround;
        if (onGround) {
            this.canJump = true;  // Can jump when landing on ground
        }
    }

    public float getDashCooldown() {
        return Math.max(0f, dashCooldownTimer);  // Return remaining cooldown time
    }

    public boolean isDashAvailable() {
        return dashCooldownTimer <= 0f;  // Check if dash is ready
    }

    public boolean isDashing() {
        return dashEffectTimer > 0f;  // Check if currently dashing (for visual effect)
    }

    // animation / visuals
    private Texture standingTex, jumpTex, runTex1, runTex2, runTex3;
    private TextureRegion standingFrame, jumpFrame, runFrame1, runFrame2, runFrame3;
    private Animation<TextureRegion> runAnim;
    private TextureRegion currentFrame;
    private float stateTime = 0f;
    private boolean facingRight = true;

    // dash visual effect texture and animation
    private Texture dashEffectTex;
    private Animation<TextureRegion> dashEffectAnim;
    private float dashAnimTime = 0f;

    // state machine
    private State state = State.IDLE;
    private float dashTimer = 0f;
    private float dashCooldownTimer = 0f;  // cooldown before next dash allowed
    private float dashEffectTimer = 0f;  // visual effect timer for dash animation

    // new: prevent external auto-resets while paused/minimized
    private boolean allowAutoReset = true;

    public void setAllowAutoReset(boolean allow) {
        this.allowAutoReset = allow;
    }

    public boolean isAllowAutoReset() {
        return allowAutoReset;
    }

    public Fixer(float x, float y, Texture fixerTexture) {
        bounds = new Rectangle(x, y, WIDTH, HEIGHT);

        // try load assets (several candidate names)
        standingTex = safeLoad("Standing.png", "standing.png", "assets/Standing.png");
        jumpTex = safeLoad("Jump.png", "jumping.png", "assets/jumping.png");

        // load three running frames: running1.png, running2.png, running3.png
        runTex1 = safeLoad("running1.png", "running1.PNG", "assets/running1.png");
        runTex2 = safeLoad("running2.png", "running2.PNG", "assets/running2.png");
        runTex3 = safeLoad("running3.png", "running3.PNG", "assets/running3.png");

        // load dash effect sprite sheet and create animation
        dashEffectTex = safeLoad("dashing.png", "assets/dashing.png");
        if (dashEffectTex != null) {
            // Assuming 1024x1024 sprite sheet with 16 frames (4x4 grid of 256x256 each)
            // Adjust FRAME_COLS and FRAME_ROWS if your sprite sheet is different
            int FRAME_COLS = 4;
            int FRAME_ROWS = 4;
            int frameWidth = dashEffectTex.getWidth() / FRAME_COLS;
            int frameHeight = dashEffectTex.getHeight() / FRAME_ROWS;
            
            TextureRegion[][] tmp = TextureRegion.split(dashEffectTex, frameWidth, frameHeight);
            Array<TextureRegion> dashFrames = new Array<>();
            
            // Add frames in reading order (left to right, top to bottom)
            for (int i = 0; i < FRAME_ROWS; i++) {
                for (int j = 0; j < FRAME_COLS; j++) {
                    dashFrames.add(tmp[i][j]);
                }
            }
            
            // Create animation - fast playback to match the short dash duration
            dashEffectAnim = new Animation<>(0.01f, dashFrames, Animation.PlayMode.NORMAL);
            Gdx.app.log("Fixer", "Loaded dash effect animation with " + dashFrames.size + " frames");
        }

        if (standingTex != null) standingFrame = new TextureRegion(standingTex);
        if (jumpTex != null) jumpFrame = new TextureRegion(jumpTex);
        if (runTex1 != null) runFrame1 = new TextureRegion(runTex1);
        if (runTex2 != null) runFrame2 = new TextureRegion(runTex2);
        if (runTex3 != null) runFrame3 = new TextureRegion(runTex3);

        // fallback to shared texture if provided
        if (standingFrame == null && fixerTexture != null) standingFrame = new TextureRegion(fixerTexture);

        // build run animation if frames present (in order)
        if (runFrame1 != null && runFrame2 != null && runFrame3 != null) {
            runAnim = new Animation<>(0.10f, runFrame1, runFrame2, runFrame3);
        } else if (runFrame1 != null && runFrame2 != null) {
            runAnim = new Animation<>(0.11f, runFrame1, runFrame2);
        }

        // set initial frame
        currentFrame = (standingFrame != null) ? standingFrame : (runFrame1 != null ? runFrame1 : jumpFrame);
        Gdx.app.log("Fixer", "Init frames: standing=" + (standingFrame!=null) + " run=" + (runAnim!=null) + " jump=" + (jumpFrame!=null) + " dashAnim=" + (dashEffectAnim!=null));
    }

    private Texture safeLoad(String... candidates) {
        for (String c : candidates) {
            try {
                if (c == null) continue;
                FileHandle fh = Gdx.files.internal(c);
                if (fh.exists()) {
                    Gdx.app.log("Fixer", "Loading internal: " + c);
                    return new Texture(fh);
                }
                fh = Gdx.files.absolute(c);
                if (fh.exists()) {
                    Gdx.app.log("Fixer", "Loading absolute: " + c);
                    return new Texture(fh);
                }
            } catch (Exception e) {
                Gdx.app.log("Fixer", "Failed to load " + c + ": " + e.getMessage());
            }
        }
        return null;
    }

    public void update(float dt) {
        if (frozen) return; // do not integrate physics while frozen (pause/minimize)
        // accumulate state time for animations
        stateTime += dt;

        // Fallback: if player falls too low, reset to ground
        if (bounds.y < -50f) {
            bounds.y = 50f;
            velocity.y = 0f;
            isOnGround = true;
            canJump = true;
            Gdx.app.log("Fixer", "Fell off world, respawning at y=50");
        }

        // input
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        // SPACE is dash, W/UP are jump (only trigger on key press, not hold)
        boolean dashPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        boolean jumpPressed = Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP);
        
        // Horizontal movement (kinematic) - disabled during dash
        if (dashTimer <= 0f) {  // Only allow normal movement when NOT dashing
            if (left) {
                velocity.x -= MOVE_ACCEL * dt;
            } else if (right) {
                velocity.x += MOVE_ACCEL * dt;
            } else {
                // ground friction: proportional damping so player comes to a stop
                if (isOnGround) {
                    float damping = Math.min(GROUND_FRICTION * dt, 1f);
                    velocity.x -= velocity.x * damping;
                    if (Math.abs(velocity.x) < 1f) velocity.x = 0f;
                } else {
                    // gentle air drag
                    velocity.x *= Math.max(1f - AIR_DRAG * dt, 0f);
                }
            }
        }
        // During dash, velocity.x is maintained and not affected by friction

        // clamp horizontal speed (only when not dashing to allow full dash speed)
        if (dashTimer <= 0f) {
            velocity.x = com.badlogic.gdx.math.MathUtils.clamp(velocity.x, -MAX_MOVE_SPEED, MAX_MOVE_SPEED);
        }

        // Dash (SPACE key)
        if (dashPressed && dashCooldownTimer <= 0f) {
            velocity.x = facingRight ? DASH_SPEED : -DASH_SPEED;
            dashTimer = DASH_TIME;
            dashCooldownTimer = DASH_COOLDOWN;
            dashEffectTimer = DASH_TIME;  // Show effect for dash duration
            Gdx.app.log("Fixer", "DASH triggered - burst movement");
        }

        // Jump (W/UP keys)
        if (jumpPressed && isOnGround && canJump) {
            velocity.y = JUMP_VY;
            isOnGround = false;
            canJump = false;
            Gdx.app.log("Fixer", "JUMP triggered! JUMP_VY=" + JUMP_VY + " current_vy=" + velocity.y + " GRAVITY=" + GRAVITY + " isOnGround=" + isOnGround);
        }

        // Gravity
        velocity.y -= GRAVITY * dt;

        // Integrate position
        float oldY = bounds.y;
        bounds.x += velocity.x * dt;
        bounds.y += velocity.y * dt;
        
        // Debug: Log jump height when jumping
        if (!isOnGround && Math.abs(oldY - bounds.y) > 5f && stateTime < 2f) {
            Gdx.app.log("Fixer", "Y-position: " + bounds.y + " (delta: " + (bounds.y - oldY) + ") velocity.y=" + velocity.y + " dt=" + dt);
        }

        // simple ground fallback if LevelManager didnt set ground
        if (bounds.y <= 0f) {
            bounds.y = 0f;
            velocity.y = 0f;
            isOnGround = true;
            canJump = true;
        }

        // Decrement dash cooldown
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= dt;
        }

        // Decrement dash effect timer and update dash animation time
        if (dashEffectTimer > 0f) {
            dashEffectTimer -= dt;
            dashAnimTime += dt;  // Advance dash animation
        } else {
            dashAnimTime = 0f;  // Reset animation when not dashing
        }

        // update state from velocities (if not dashing)
        if (dashTimer <= 0f) {
            if (!isOnGround) {
                state = (velocity.y > 0f) ? State.JUMP : State.FALL;
            } else {
                // lower run threshold so running activates earlier and make idle threshold small
                state = (Math.abs(velocity.x) > 1f) ? State.RUN : State.IDLE;
            }
        } else {
            dashTimer -= dt;
            state = State.DASH;
        }

        // facing
        if (velocity.x < -1f) facingRight = false;
        else if (velocity.x > 1f) facingRight = true;

        // select animation frame
        TextureRegion next = currentFrame;
        switch (state) {
            case JUMP:
            case FALL:
                if (jumpFrame != null) next = jumpFrame;
                break;
            case RUN:
                if (runAnim != null) next = runAnim.getKeyFrame(stateTime, true);
                else if (runFrame1 != null) next = runFrame1;
                break;
            case DASH:
            case IDLE:
            default:
                if (standingFrame != null) next = standingFrame;
                break;
        }

        if (next != null) {
            boolean wantFlip = !facingRight;
            if (next.isFlipX() != wantFlip) next.flip(true, false);
            currentFrame = next;
        }
    }

    public void draw(SpriteBatch batch) {
        if (batch == null) return;
        
        // Draw character sprite normally
        if (currentFrame != null) {
            batch.setColor(1f, 1f, 1f, 1f);  // Normal white
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
        }
        
        // Draw animated dash effect behind/around character if currently dashing
        if (isDashing() && dashEffectAnim != null) {
            TextureRegion dashFrame = dashEffectAnim.getKeyFrame(dashAnimTime, false);
            batch.setColor(1f, 1f, 1f, 0.5f);  // More transparent
            
            // Match the dash frame flip to character facing direction
            boolean wantFlip = !facingRight;
            if (dashFrame.isFlipX() != wantFlip) dashFrame.flip(true, false);
            
            // Center the dash effect on the character but make it smaller
            // Scale down from 256x256 to ~128x128 and center it
            float effectSize = 96f;  // Smaller than character's 64x64 to be more subtle
            float offsetX = bounds.x + (bounds.width - effectSize) / 2f;
            float offsetY = bounds.y + (bounds.height - effectSize) / 2f;
            
            batch.draw(dashFrame, offsetX, offsetY, effectSize, effectSize);
            batch.setColor(1f, 1f, 1f, 1f);  // Reset color
        }
    }

    public void renderDebug(ShapeRenderer sr) {
        if (sr == null) return;
        if (currentFrame == null) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(1, 0, 1, 1);
            sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            sr.end();
        }
    }

    public void dispose() {
        if (standingTex != null) { standingTex.dispose(); standingTex = null; }
        if (jumpTex != null) { jumpTex.dispose(); jumpTex = null; }
        if (runTex1 != null) { runTex1.dispose(); runTex1 = null; }
        if (runTex2 != null) { runTex2.dispose(); runTex2 = null; }
        if (runTex3 != null) { runTex3.dispose(); runTex3 = null; }
        if (dashEffectTex != null) { dashEffectTex.dispose(); dashEffectTex = null; }
    }

    public void reset(float x, float y) {
        // suppress automatic resets when disabled (paused/minimized)
        if (!allowAutoReset) {
            com.badlogic.gdx.Gdx.app.log("Fixer", "reset() suppressed while paused/minimized");
            return;
        }

        bounds.setPosition(x, y); 
        velocity.set(0, 0); 
        stateTime = 0f; 
        isOnGround = true; 
        canJump = true; 
        Gdx.app.log("Fixer", "Reset to position (" + x + "," + y + ") onGround=true");
    }

    public void checkSlowingEffect(Array<Rectangle> obstacles) {
        for (Rectangle obstacle : obstacles) {
            if (obstacle.overlaps(bounds)) {
                setSlowed(true);
                return;
            }
        }
        setSlowed(false);
    }

    public boolean canJumpToUpperPlatform(Array<Rectangle> obstacles, Array<Rectangle> platforms) {
        Rectangle jumpArea = new Rectangle(bounds.x, bounds.y + HEIGHT, WIDTH, HEIGHT);
        for (Rectangle obstacle : obstacles) {
            if (obstacle.overlaps(jumpArea)) {
                return false; // Obstacle is blocking the jump
            }
        }
        for (Rectangle platform : platforms) {
            if (platform.overlaps(jumpArea)) {
                return false; // Platform is blocking the jump
            }
        }
        return true;
    }
}

