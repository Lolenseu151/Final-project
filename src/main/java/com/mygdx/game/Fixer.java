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

/**
 * Fixer: simple character controller + animation state machine.
 * Uses 64x64 frames and running1/2/3 for run animation.
 */
public class Fixer {
    private enum State { IDLE, RUN, JUMP, FALL, DASH }

    // physics tunables
    private static final float MOVE_ACCEL = 1500f;   // px/s^2
    private static final float MAX_MOVE_SPEED = 220f; // px/s
    private static final float GROUND_FRICTION = 12f; // per second
    private static final float AIR_DRAG = 1.5f;
    private static final float JUMP_VY = 800f;
    private static final float GRAVITY = 1400f;
    private static final float DASH_SPEED = 700f;
    private static final float DASH_TIME = 0.12f;

    // Sprite / collision sizes (frames are 64x64 in assets)
    private static final float SPRITE_SIZE = 64f;
    private static final float WIDTH = SPRITE_SIZE;
    private static final float HEIGHT = SPRITE_SIZE;

    private final Rectangle bounds;
    private Vector2 velocity = new Vector2();
    private boolean isOnGround = false;
    private boolean canJump = true;

    // animation / visuals
    private Texture standingTex, jumpTex, runTex1, runTex2, runTex3;
    private TextureRegion standingFrame, jumpFrame, runFrame1, runFrame2, runFrame3;
    private Animation<TextureRegion> runAnim;
    private TextureRegion currentFrame;
    private float stateTime = 0f;
    private boolean facingRight = true;

    // state machine
    private State state = State.IDLE;
    private float dashTimer = 0f;

    public Fixer(float x, float y, Texture fixerTexture) {
        bounds = new Rectangle(x, y, WIDTH, HEIGHT);

        // try load assets (several candidate names)
        standingTex = safeLoad("Standing.png", "standing.png", "assets/Standing.png");
        jumpTex = safeLoad("Jump.png", "jumping.png", "assets/jumping.png");

        // load three running frames: running1.png, running2.png, running3.png
        runTex1 = safeLoad("running1.png", "running1.PNG", "assets/running1.png");
        runTex2 = safeLoad("running2.png", "running2.PNG", "assets/running2.png");
        runTex3 = safeLoad("running3.png", "running3.PNG", "assets/running3.png");

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
        Gdx.app.log("Fixer", "Init frames: standing=" + (standingFrame!=null) + " run=" + (runAnim!=null) + " jump=" + (jumpFrame!=null));
    }

    private Texture safeLoad(String... candidates) {
        for (String c : candidates) {
            try {
                if (c == null) continue;
                FileHandle fh = Gdx.files.internal(c);
                if (fh.exists()) return new Texture(fh);
                fh = Gdx.files.absolute(c);
                if (fh.exists()) return new Texture(fh);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public void update(float delta) {
        stateTime += delta;

        // input + state transitions (dash has priority)
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        // use isKeyPressed so a jump input held during the same frame LevelManager
        // resolves grounding is still honored (prevents missed jump when order changes)
        boolean jumpPressed = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean dashPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        // dash handling
        if (dashPressed && dashTimer <= 0f) {
            dashTimer = DASH_TIME;
            state = State.DASH;
            // dash in facing direction; if standing, prefer right
            float dir = (velocity.x != 0) ? Math.signum(velocity.x) : (facingRight ? 1f : -1f);
            velocity.x = dir * DASH_SPEED;
            velocity.y = 0;
            canJump = false;
        }

        if (dashTimer > 0f) {
            dashTimer -= delta;
            if (dashTimer <= 0f) {
                // end dash, resume to fall/run or idle
                if (!isOnGround) state = State.FALL;
                else state = (Math.abs(velocity.x) > 1f) ? State.RUN : State.IDLE;
            }
        } else {
            // normal movement
            float accel = 0f;
            if (left) accel -= MOVE_ACCEL;
            if (right) accel += MOVE_ACCEL;

            // apply horizontal accel
            velocity.x += accel * delta;

            // clamp speed when not dashing
            if (Math.abs(velocity.x) > MAX_MOVE_SPEED && state != State.DASH) {
                velocity.x = Math.signum(velocity.x) * MAX_MOVE_SPEED;
            }

            // friction / drag
            if (accel == 0f) {
                float drag = isOnGround ? GROUND_FRICTION : AIR_DRAG;
                // approximate exponential friction
                velocity.x -= velocity.x * Math.min(1f, drag * delta);
                if (Math.abs(velocity.x) < 4f) velocity.x = 0f;
            }

            // jumping
            if (jumpPressed && isOnGround && canJump) {
                velocity.y = JUMP_VY;
                isOnGround = false;
                canJump = false;
                state = State.JUMP;
            }
        }

        // gravity
        if (state != State.DASH) velocity.y -= GRAVITY * delta;

        // update position
        bounds.x += velocity.x * delta;
        bounds.y += velocity.y * delta;

        // simple ground check fallback (LevelManager should call setOnGround(true) when colliding)
        if (bounds.y <= 0f) {
            bounds.y = 0f;
            velocity.y = 0f;
            isOnGround = true;
            canJump = true;
        }

        // compute state from velocities (if not dashing)
        if (dashTimer <= 0f) {
            if (!isOnGround) {
                state = (velocity.y > 0) ? State.JUMP : State.FALL;
            } else {
                state = (Math.abs(velocity.x) > 6f) ? State.RUN : State.IDLE;
            }
        }

        // facing
        if (velocity.x < -1f) facingRight = false;
        else if (velocity.x > 1f) facingRight = true;

        // animation selection
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
            // ensure flip matches facing
            boolean wantFlip = !facingRight;
            if (next.isFlipX() != wantFlip) next.flip(true, false);
            currentFrame = next;
        }
    }

    public void draw(SpriteBatch batch) {
        if (batch == null) return;
        if (currentFrame != null) {
            // draw at the 64x64 sprite size (bounds set to SPRITE_SIZE)
            batch.draw(currentFrame, bounds.x, bounds.y, bounds.width, bounds.height);
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
    }

    // LevelManager interaction helpers
    public Rectangle getBounds() { return bounds; }
    public Vector2 getVelocity() { return velocity; }
    public void setOnGround(boolean onGround) { this.isOnGround = onGround; if (onGround) { canJump = true; } }
    public void setSlowed(boolean slowed) { /* keep for compatibility */ }
    public void setVelocityY(float vy) { this.velocity.y = vy; }
    public void setVelocityX(float vx) { this.velocity.x = vx; }
    public void reset(float x, float y) { bounds.setPosition(x, y); velocity.set(0,0); stateTime = 0f; isOnGround = false; canJump = true; }
}
