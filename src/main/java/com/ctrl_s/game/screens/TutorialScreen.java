package com.ctrl_s.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.ctrl_s.game.core.GameApplication;

/**
 * TutorialScreen - Implements the tutorial sequence with slides, overlays, and
 * typing text.
 */
public class TutorialScreen implements Screen {

    private final GameApplication game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private BitmapFont captionFont; // Larger font for captions
    private GlyphLayout layout;

    // Slide management
    private int currentSlide = 1;
    private float timeElapsed = 0f;

    // Assets
    private Texture img1Texture, img2Texture;
    private TextureRegion img1, img2;
    private boolean ownsTextures = false;

    // Slide 4 Overlay Assets
    private Texture img4Texture;
    private TextureRegion image4;
    private boolean ownsImg4 = false;
    private boolean overlay4Started = false;
    private boolean overlay4Done = false;
    private float overlay4X = 0f;
    private float overlay4TargetX = 0f;

    // Slide 5 Overlay Assets (slide in from top)
    private Texture img5Texture;
    private TextureRegion image5;
    private boolean ownsImg5 = false;
    private boolean overlay5Started = false;
    private boolean overlay5Done = false;
    private float overlay5Y = 0f;
    private float overlay5TargetY = 0f;

    // Slide 6 Overlay Assets (slide in from right)
    private Texture img6Texture;
    private TextureRegion image6;
    private boolean ownsImg6 = false;
    private boolean overlay6Started = false;
    private boolean overlay6Done = false;
    private float overlay6X = 0f;
    private float overlay6TargetX = 0f; // will target center

    // Slide 7 Overlay Assets (slide in from right)
    private Texture img7Texture;
    private TextureRegion image7;
    private boolean ownsImg7 = false;
    private boolean image7Started = false; // logic for image sliding
    private boolean transitionedTo7 = false; // logic for when image settled
    private boolean overlay7Started = false; // logic for typing text
    private boolean overlay7Done = false;
    private float image7X = 0f;
    private float image7TargetX = 0f;
    private float image7Y = 0f;

    // Slide 8 & 9 Overlay Assets (slide 8 from top, slide 9 from bottom)
    private Texture img8Texture;
    private TextureRegion image8;
    private boolean ownsImg8 = false;
    private boolean overlay8Started = false;
    private boolean overlay8Done = false;
    private float overlay8Y = 0f;
    private float overlay8TargetY = 0f;

    private Texture img9Texture;
    private TextureRegion image9;
    private boolean ownsImg9 = false;
    private boolean overlay9Started = false;
    private boolean overlay9Done = false;
    private float overlay9Y = 0f;
    private float overlay9TargetY = 0f; // target bottom area

    // Text overlay variables for Slide 2
    private boolean overlay2Started = false;
    private boolean overlay2Done = false;
    private int overlay2Stage = 0; // 0=start, 1=typing, 2=pause, 3=typing, etc.
    private float overlay2TypingTimer = 0f;
    private float overlay2PostPauseTimer = 0f;
    private String overlay2Prefix = "Cat: ";
    private float overlay2TextPosX = 50f;
    private float overlay2TextPosY = 200f; // will be recalculated
    private boolean overlay2TypingDone = false;

    private boolean overlaySpeedUpdated = false;
    private float overlaySpeed = 800f; // pixels per second

    // Variables for Slide 6 typing
    private int overlay6Stage = 0;
    private float overlay6TypingTimer = 0f;
    private float overlay6PostPauseTimer = 0f;
    private boolean overlay6TypingDone = false;

    // Variables for Slide 7 typing
    private int overlay7Stage = 0;
    private float overlay7TypingTimer = 0f;
    private float overlay7PostPauseTimer = 0f;
    private boolean overlay7TypingDone = false;

    // Buttons
    private float btnW = 160f;
    private float btnH = 50f;
    private float btnMargin = 30f;
    private float btnContinueX, btnContinueY;
    private float btnSkipX, btnSkipY;
    private Color btnColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private Color btnHoverColor = new Color(0.4f, 0.4f, 0.45f, 0.9f);
    private boolean buttonsShown = false;
    private boolean hoverPrevContinue = false;
    private boolean hoverPrevSkip = false;

    // Music
    private Music ringMusic;
    private Music pickupMusic;
    private Music talkingMusic;
    private Music talingMusic; // Typo preserved from original file "TALING"
    private boolean musicStarted = false;
    private boolean pickupStarted = false;
    private boolean talkingStarted = false;
    private boolean talkingStopped = false;
    private boolean talingStarted = false;
    private boolean talkingFading = false;
    private float talkingFadeTimer = 0f;
    private float talkingInitialVolume = 1f;
    private boolean talingFading = false;
    private float talingFadeTimer = 0f;
    private static final float TALKING_FADE_DURATION = 1.5f; // seconds to fade out talking
    private static final float TALING_FADE_DURATION = 2.0f; // seconds to fade out "taling"

    // Transition logic
    private boolean transitionedTo6 = false;

    // Final sequence: pause then collapse
    private boolean finishing = false;
    private float finishTimer = 0f;
    private static final float FINISH_PAUSE = 5.0f; // 5s pause after last slide
    private boolean collapsing = false;
    private float collapseTimer = 0f;
    private static final float COLLAPSE_DURATION = 1.5f;
    // pixelation/grid collapse effect

    public TutorialScreen(GameApplication game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
        layout = new GlyphLayout();

        // Load caption font
        try {
            // Try loading Bold.fnt for larger captions if available
            captionFont = new BitmapFont(Gdx.files.internal("assets/fonts/bold/Bold.fnt"));
            captionFont.getData().setScale(0.35f); // Adjusted scale
            captionFont.setColor(Color.BLACK);
        } catch (Exception e) {
            Gdx.app.log("TutorialScreen", "Could not load custom caption font, using default.");
        }

        // Load images
        try {
            img1Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/1.png"));
            img1Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); // Filter for quality
            img1 = new TextureRegion(img1Texture);

            img2Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/2.png"));
            img2Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            img2 = new TextureRegion(img2Texture);

            ownsTextures = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load base slide images", e);
        }

        // Load Overlay 4
        try {
            img4Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/4.png"));
            img4Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image4 = new TextureRegion(img4Texture);
            ownsImg4 = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load overlay 4", e);
        }

        // Load Overlay 5
        try {
            img5Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/5.png"));
            img5Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image5 = new TextureRegion(img5Texture);
            ownsImg5 = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load overlay 5", e);
        }

        // Load Overlay 6
        try {
            img6Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/6.png"));
            img6Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image6 = new TextureRegion(img6Texture);
            ownsImg6 = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load overlay 6", e);
        }

        // Load Overlay 7
        try {
            img7Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/7.png"));
            img7Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image7 = new TextureRegion(img7Texture);
            ownsImg7 = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load overlay 7", e);
        }

        // Load Overlay 8 & 9
        try {
            img8Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/8.png"));
            img8Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image8 = new TextureRegion(img8Texture);
            ownsImg8 = true;

            img9Texture = new Texture(Gdx.files.internal("assets/sprites/ui/tutorial/9.png"));
            img9Texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            image9 = new TextureRegion(img9Texture);
            ownsImg9 = true;
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Failed to load overlay 8/9", e);
        }

        // Setup Buttons
        float sw = Gdx.graphics.getWidth();
        btnSkipX = sw - btnW - btnMargin;
        btnSkipY = btnMargin;
        btnContinueX = sw - btnW - btnMargin; // Same X position initially
        btnContinueY = btnMargin; // Same Y position
        // Actually, let's position them: Skip bottom right, Continue bottom right but
        // hidden or same spot?
        // Original logic seemed to use Continue/Skip in the same spot depending on
        // context or side-by-side.
        // Based on rendering, they are drawn separately. Let's stack them or place
        // side-by-side.
        // Placing side-by-side: Skip on far right, Continue to its left.
        btnContinueX = btnSkipX - btnW - 20f;

        // Load Music
        try {
            if (Gdx.files.internal("assets/audio/sfx/telephone_ring.mp3").exists())
                ringMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/audio/sfx/telephone_ring.mp3"));
            if (Gdx.files.internal("assets/audio/sfx/telephone_pickup.mp3").exists())
                pickupMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/audio/sfx/telephone_pickup.mp3"));
            if (Gdx.files.internal("assets/audio/sfx/talking.mp3").exists())
                talkingMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/audio/sfx/talking.mp3"));
            if (Gdx.files.internal("assets/audio/sfx/talking_alt.mp3").exists())
                talingMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/audio/sfx/talking_alt.mp3"));
        } catch (Exception e) {
            Gdx.app.error("TutorialScreen", "Error loading music", e);
        }

        Gdx.input.setInputProcessor(null); // Reset input processor
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw(delta);
    }

    private void update(float dt) {
        timeElapsed += dt;

        // Music logic
        if (!musicStarted && ringMusic != null) {
            ringMusic.setLooping(true);
            ringMusic.play();
            musicStarted = true;
        }

        // Slide 2 logic: Phone pickup sound
        if (overlay2Started && !pickupStarted && pickupMusic != null) {
            if (ringMusic != null && ringMusic.isPlaying())
                ringMusic.stop();
            pickupMusic.play();
            pickupStarted = true;
        }

        // Slide 2 logic: Talking sound starts with text
        if (overlay2Started && !talkingStarted && talkingMusic != null
                && (overlay2Stage > 0 || overlay2TypingTimer > 0.1f)) {
            talkingMusic.setLooping(true);
            talkingMusic.setVolume(1.5f);
            talkingMusic.play();
            talkingStarted = true;
        }

        // Slide transition logic (1 -> 2 managed by overlay2Started flag mostly)
        // Auto-start Slide 2 (overlay2) after a delay if desired, or purely wait for
        // user continue?
        // Original code seemed to wait for button presses or sequence.
        // Let's assume slide 1 is static until "Continue" is pressed, OR it transitions
        // automatically.
        // Actually, looking at the code, "Continue" button triggers events.
        // But for tutorial flow, let's follow the previous logic: auto-start overlays
        // if configured.

        if (currentSlide == 1 && timeElapsed > 0.5f && !overlay2Started) {
            // Wait for user input or just show buttons
            buttonsShown = true;
        }
    }

    private void draw(float dt) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!overlaySpeedUpdated) {
            overlaySpeed = screenW * 1.5f; // Traverse screen width in ~0.66s
            overlaySpeedUpdated = true;
            // Recalculate text positions based on current screen size
            overlay2TextPosX = screenW * 0.12f;
            overlay2TextPosY = screenH * 0.25f; // Lower third
        }

        batch.begin();

        // Draw Slide 1 (Background 1)
        if (img1 != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(img1, 0, 0, screenW, screenH);
        }

        // Draw Slide 2 (Background 2) overlay sliding in or fading?
        // Actually, logic is: img1 is base. img2 slides in or overlays?
        // Let's implement the overlay sequences.

        // Logic:
        // Slide 1 is visible.
        // Press Continue -> Start Overlay 2 (Phone pick up interaction).
        // Let's automate some of this if "Continue" logic is complex.
        // Actually, the previous file had logic where "Continue" advanced the state.

        // State 1: Showing img1. Buttons shown.
        // User clicks Continue -> overlay2Started = true.
        // overlay2 (Slide 2 actions): img2 is drawn?
        // In original code, img2 was drawn if overlay2Started.

        if (overlay2Started && img2 != null) {
            // Draw img2 over img1. Transition?
            // Assume instant switch or fade. Let's do instant for now as per simple logic.
            batch.draw(img2, 0, 0, screenW, screenH);
            buttonsShown = false; // Hide buttons during typing
        }

        // Overlay 2 Typing Logic (The conversation)
        if (overlay2Started && !overlay2Done) {
            BitmapFont cap = captionFont != null ? captionFont : font;

            // Text rendering setup
            final String[] STAGE2_TEXTS = new String[] {
                    "Hey, are you there?",
                    "Listen, I need a favor.",
                    "Big one.",
                    "I'm kinda... stuck in the system.",
                    "Don't ask."
            };
            final float[] STAGE2_DURATIONS = new float[] { 2f, 2.5f, 1.5f, 3f, 2f };
            final float STAGE2_PAUSE = 1.0f;

            // Typing logic similar to original file
            String typed = "";
            if (overlay2Stage >= STAGE2_TEXTS.length * 2) {
                // Done
                overlay2Done = true;
                typed = STAGE2_TEXTS[STAGE2_TEXTS.length - 1]; // Keep last text
                buttonsShown = true;

                // Trigger next sequence (Slide 4 sliding in)
                if (image4 != null && !overlay4Started) {
                    overlay4Started = true;
                    overlay4Done = false;
                    overlay4X = screenW + 10f; // Start off-screen right
                    overlay4TargetX = (screenW - image4.getRegionWidth()) / 2f;
                    buttonsShown = false;
                }
            } else if ((overlay2Stage % 2) == 1) {
                // Pausing state
                overlay2PostPauseTimer += dt;
                int lastTextIndex = (overlay2Stage - 1) / 2;
                lastTextIndex = Math.min(lastTextIndex, STAGE2_TEXTS.length - 1);
                typed = STAGE2_TEXTS[lastTextIndex];
                if (overlay2PostPauseTimer >= STAGE2_PAUSE) {
                    overlay2PostPauseTimer = 0f;
                    overlay2Stage++;
                    overlay2TypingTimer = 0f;
                    overlay2TypingDone = false;
                }
            } else {
                // Typing state
                int textIndex = overlay2Stage / 2;
                if (textIndex < 0)
                    textIndex = 0;
                if (textIndex >= STAGE2_TEXTS.length)
                    textIndex = STAGE2_TEXTS.length - 1;

                String current = STAGE2_TEXTS[textIndex];
                float currentDuration = STAGE2_DURATIONS[textIndex];

                if (!overlay2TypingDone)
                    overlay2TypingTimer += dt;

                int totalChars = current.length();
                float progress = Math.min(1f, overlay2TypingTimer / Math.max(0.0001f, currentDuration));
                int chars = (int) Math.floor(progress * totalChars);
                if (chars >= totalChars) {
                    chars = totalChars;
                    overlay2TypingDone = true;
                }

                typed = current.substring(0, chars);

                if (overlay2TypingDone) {
                    overlay2Stage++;
                    overlay2TypingTimer = 0f;
                    overlay2TypingDone = false;
                }
            }

            // Draw Prefix + Typed Text
            float tx = overlay2TextPosX;
            float ty = overlay2TextPosY;

            Color prevC = cap.getColor().cpy();
            cap.setColor(Color.BLACK);

            layout.setText(cap, overlay2Prefix);
            float prefixWidth = layout.width;
            cap.draw(batch, overlay2Prefix, tx, ty);

            if (!typed.isEmpty()) {
                cap.draw(batch, typed, tx + prefixWidth + 10f, ty);
            }
            cap.setColor(prevC);

            // Allow skipping typing with click?
            if (Gdx.input.justTouched()) {
                // Logic to fast forward typing could go here
            }
        }

        // Stop talking music when overlay2 is done
        if (overlay2Done && talkingStarted && !talkingStopped && !talkingFading) {
            talkingFading = true;
            talkingFadeTimer = 0f;
        }

        // Handle Talking Music Fade
        if (talkingFading) {
            talkingFadeTimer += dt;
            float vol = talkingInitialVolume * (1f - (talkingFadeTimer / TALKING_FADE_DURATION));
            if (talkingMusic != null)
                talkingMusic.setVolume(Math.max(0, vol));
            if (talkingFadeTimer >= TALKING_FADE_DURATION) {
                if (talkingMusic != null)
                    talkingMusic.stop();
                talkingFading = false;
                talkingStopped = true;
                talkingStarted = false;
            }
        }

        // Slide 4: Slide in from right
        if (overlay4Started && image4 != null) {
            if (!overlay4Done) {
                overlay4X -= overlaySpeed * dt;
                if (overlay4X <= overlay4TargetX) {
                    overlay4X = overlay4TargetX;
                    overlay4Done = true;

                    // Trigger Slide 5 (from top)
                    if (image5 != null && !overlay5Started) {
                        overlay5Started = true;
                        overlay5Done = false;
                        overlay5Y = screenH + 10f; // Start above screen
                        overlay5TargetY = (screenH - image5.getRegionHeight()) / 2f;
                    }
                }
            }
            // Draw
            float w = image4.getRegionWidth();
            float h = image4.getRegionHeight();
            batch.draw(image4, overlay4X, (screenH - h) / 2f, w, h);
        }

        // Slide 5: Slide in from top
        if (overlay5Started && image5 != null) {
            if (!overlay5Done) {
                overlay5Y -= overlaySpeed * dt;
                if (overlay5Y <= overlay5TargetY) {
                    overlay5Y = overlay5TargetY;
                    overlay5Done = true;

                    // Trigger Slide 6 (from right)
                    if (image6 != null && !overlay6Started) {
                        overlay6Started = true;
                        overlay6Done = false;
                        overlay6X = screenW + 10f;
                        overlay6TargetX = (screenW - image6.getRegionWidth()) / 2f;
                    }
                }
            }
            float w = image5.getRegionWidth();
            float h = image5.getRegionHeight();
            float x = (screenW - w) / 2f; // Centered X
            batch.draw(image5, x, overlay5Y, w, h);
        }

        // Slide 6: Slide in from right + Typing
        if (overlay6Started && image6 != null) {
            if (!transitionedTo6) { // Sliding phase
                overlay6X -= overlaySpeed * dt;
                if (overlay6X <= overlay6TargetX) {
                    overlay6X = overlay6TargetX;
                    transitionedTo6 = true;
                    // Start overlay6 typing logic here if needed, or just let it render below
                }
            }
            float w = image6.getRegionWidth();
            float h = image6.getRegionHeight();
            float y = (screenH - h) / 2f;
            batch.draw(image6, overlay6X, y, w, h);

            // Text for Slide 6
            if (transitionedTo6 && !overlay6Done) {
                BitmapFont cap = captionFont != null ? captionFont : font;
                // Similar typing logic for Slide 6
                final String[] STAGE6_TEXTS = {
                        "The auditors are here early!",
                        "They're hitting the main accounts in two hours.",
                        "Two hours! This is a Paper Panic Trail, my friend",
                        "and you're running it!"
                };
                final float[] STAGE6_DURATIONS = { 2f, 3f, 2f, 2f };
                final float STAGE6_PAUSE = 1f;

                // (Simplified typing logic - reuse or copy-paste pattern)
                // ... [Implementation abridged for brevity, assume similar to Overlay 2] ...
                // For now, let's just show full text to save space or implement fully if
                // critical.
                // Given the task, I should implement it fully to preserve functionality.

                String typed = "";
                if (overlay6Stage >= STAGE6_TEXTS.length * 2) {
                    overlay6Done = true;
                    typed = STAGE6_TEXTS[STAGE6_TEXTS.length - 1];
                    // Trigger Slide 7
                    if (image7 != null && !image7Started) {
                        image7Started = true;
                        image7X = screenW + 10f;
                        image7TargetX = (screenW - image7.getRegionWidth()) / 2f;
                        image7Y = (screenH - image7.getRegionHeight()) / 2f;
                    }
                } else if ((overlay6Stage % 2) == 1) {
                    // Pause logic
                    overlay6PostPauseTimer += dt;
                    int idx = (overlay6Stage - 1) / 2;
                    idx = Math.min(idx, STAGE6_TEXTS.length - 1);
                    typed = STAGE6_TEXTS[idx];
                    if (overlay6PostPauseTimer >= STAGE6_PAUSE) {
                        overlay6PostPauseTimer = 0;
                        overlay6Stage++;
                        overlay6TypingTimer = 0;
                        overlay6TypingDone = false;
                    }
                } else {
                    // Typing logic
                    int idx = overlay6Stage / 2;
                    if (idx >= STAGE6_TEXTS.length)
                        idx = STAGE6_TEXTS.length - 1;

                    // Start TALING music on first message
                    if (idx == 0 && !talingStarted && talingMusic != null) {
                        talingMusic.setLooping(true);
                        talingMusic.play();
                        talingStarted = true;
                    }

                    String curr = STAGE6_TEXTS[idx];
                    if (!overlay6TypingDone)
                        overlay6TypingTimer += dt;
                    int total = curr.length();
                    float p = Math.min(1f, overlay6TypingTimer / Math.max(0.001f, STAGE6_DURATIONS[idx]));
                    int chars = (int) (p * total);
                    if (chars >= total) {
                        chars = total;
                        overlay6TypingDone = true;
                    }
                    typed = curr.substring(0, chars);
                    if (overlay6TypingDone) {
                        overlay6Stage++;
                        overlay6TypingTimer = 0;
                        overlay6TypingDone = false;
                    }
                }

                // Draw text
                Color prevC6 = cap.getColor().cpy();
                cap.setColor(Color.BLACK);
                cap.draw(batch, overlay2Prefix, overlay2TextPosX, overlay2TextPosY);
                cap.draw(batch, typed, overlay2TextPosX + 70f, overlay2TextPosY); // approximate offset
                cap.setColor(prevC6);
            }
        }

        // Slide 7: Slide in Image 7 -> Type Text
        if (image7Started && image7 != null) {
            if (!transitionedTo7) {
                image7X -= overlaySpeed * dt;
                if (image7X <= image7TargetX) {
                    image7X = image7TargetX;
                    transitionedTo7 = true;
                    overlay7Started = true; // Start typing
                }
            }
            float w = image7.getRegionWidth();
            float h = image7.getRegionHeight();
            batch.draw(image7, image7X, image7Y, w, h);

            // Text for Slide 7
            if (overlay7Started && !overlay7Done) {
                BitmapFont cap = captionFont != null ? captionFont : font;
                final String[] STAGE7_TEXTS = {
                        "We start with the Archives. I need you to move now.",
                        "Straight ahead, near the obsolete property files."
                };
                final float[] STAGE7_DURATIONS = { 2f, 2f };
                final float STAGE7_PAUSE = 1f;

                // (Simplified Logic)
                String typed = "";
                if (overlay7Stage >= STAGE7_TEXTS.length * 2) {
                    overlay7Done = true;
                    typed = STAGE7_TEXTS[STAGE7_TEXTS.length - 1];

                    // Trigger Slides 8 & 9
                    if (image8 != null && !overlay8Started) {
                        overlay8Started = true;
                        overlay8Y = screenH + 10f;
                        overlay8TargetY = (screenH - image8.getRegionHeight()) / 2f;
                    }
                    if (image9 != null && !overlay9Started) {
                        overlay9Started = true;
                        overlay9Y = -image9.getRegionHeight() - 10f;
                        overlay9TargetY = (screenH - image9.getRegionHeight()) / 2f;
                    }
                } else if ((overlay7Stage % 2) == 1) {
                    overlay7PostPauseTimer += dt;
                    int idx = (overlay7Stage - 1) / 2;
                    typed = STAGE7_TEXTS[Math.min(idx, STAGE7_TEXTS.length - 1)];
                    if (overlay7PostPauseTimer >= STAGE7_PAUSE) {
                        overlay7PostPauseTimer = 0;
                        overlay7Stage++;
                        overlay7TypingTimer = 0;
                        overlay7TypingDone = false;
                    }
                } else {
                    int idx = overlay7Stage / 2;
                    if (idx >= STAGE7_TEXTS.length)
                        idx = STAGE7_TEXTS.length - 1;
                    String curr = STAGE7_TEXTS[idx];
                    if (!overlay7TypingDone)
                        overlay7TypingTimer += dt;
                    int total = curr.length();
                    float p = Math.min(1f, overlay7TypingTimer / Math.max(0.001f, STAGE7_DURATIONS[idx]));
                    int chars = (int) (p * total);
                    if (chars >= total) {
                        chars = total;
                        overlay7TypingDone = true;
                    }
                    typed = curr.substring(0, chars);
                    if (overlay7TypingDone) {
                        overlay7Stage++;
                        overlay7TypingTimer = 0;
                        overlay7TypingDone = false;
                    }
                }

                Color prevC7 = cap.getColor().cpy();
                cap.setColor(Color.BLACK);
                cap.draw(batch, overlay2Prefix, overlay2TextPosX, overlay2TextPosY);
                cap.draw(batch, typed, overlay2TextPosX + 70f, overlay2TextPosY);
                cap.setColor(prevC7);
            }
        }

        // Slide 8 (Top) & 9 (Bottom)
        if (overlay8Started && image8 != null) {
            if (!overlay8Done) {
                overlay8Y -= overlaySpeed * dt;
                if (overlay8Y <= overlay8TargetY) {
                    overlay8Y = overlay8TargetY;
                    overlay8Done = true;
                }
            }
            float w = image8.getRegionWidth();
            float h = image8.getRegionHeight();
            batch.draw(image8, (screenW - w) / 2f, overlay8Y, w, h);
        }

        if (overlay9Started && image9 != null) {
            if (!overlay9Done) {
                overlay9Y += overlaySpeed * dt;
                if (overlay9Y >= overlay9TargetY) {
                    overlay9Y = overlay9TargetY;
                    overlay9Done = true;
                }
            }
            float w = image9.getRegionWidth();
            float h = image9.getRegionHeight();
            batch.draw(image9, (screenW - w) / 2f, overlay9Y, w, h);
        }

        // Final sequence
        if (overlay8Done && overlay9Done && !finishing) {
            buttonsShown = true;
            finishing = true;
            finishTimer = 0f;
            collapsing = false;
        }

        // Collapse Effect (Pixelation/Fade out)
        if (finishing) {
            if (!collapsing) {
                finishTimer += dt;
                if (finishTimer >= FINISH_PAUSE) {
                    collapsing = true;
                    collapseTimer = 0f;
                    // Start fading taling music
                    if (talingMusic != null && !talingFading) {
                        talingFading = true;
                        talingFadeTimer = 0f;
                    }
                }
            } else {
                collapseTimer += dt;
                float p = Math.min(1f, collapseTimer / COLLAPSE_DURATION);

                // Draw black rectangles shrinking inward or just fading to black
                batch.end();
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(0, 0, 0, p);
                shapeRenderer.rect(0, 0, screenW, screenH);
                shapeRenderer.end();
                Gdx.gl.glDisable(GL20.GL_BLEND);

                // Fade taling music
                if (talingFading && talingMusic != null) {
                    talingFadeTimer += dt;
                    float vol = 1f - (talingFadeTimer / TALING_FADE_DURATION);
                    talingMusic.setVolume(Math.max(0, vol));
                }

                if (p >= 1f) {
                    launchGame();
                    return; // Prevent further drawing
                }

                batch.begin(); // Restore batch for button check
            }
        }

        batch.end();

        // Draw Buttons (Separate batch)
        if (buttonsShown && !collapsing) {
            drawButtonsAndHandleInput();
        }
    }

    private void drawButtonsAndHandleInput() {
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY();

        boolean hoverContinue = (mx >= btnContinueX && mx <= btnContinueX + btnW && my >= btnContinueY
                && my <= btnContinueY + btnH);
        boolean hoverSkip = (mx >= btnSkipX && mx <= btnSkipX + btnW && my >= btnSkipY && my <= btnSkipY + btnH);

        // Play hovers
        try {
            if (hoverContinue && !hoverPrevContinue)
                game.getHoverSoundManager().playHover();
            if (hoverSkip && !hoverPrevSkip)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }

        hoverPrevContinue = hoverContinue;
        hoverPrevSkip = hoverSkip;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Continue Button
        shapeRenderer.setColor(hoverContinue ? btnHoverColor : btnColor);
        shapeRenderer.rect(btnContinueX, btnContinueY, btnW, btnH); // Simplified rect

        // Skip Button
        shapeRenderer.setColor(hoverSkip ? btnHoverColor : btnColor);
        shapeRenderer.rect(btnSkipX, btnSkipY, btnW, btnH);

        shapeRenderer.end();

        batch.begin();
        String cText = "Continue";
        String sText = "Skip";
        layout.setText(font, cText);
        font.draw(batch, cText, btnContinueX + (btnW - layout.width) / 2f, btnContinueY + (btnH + layout.height) / 2f);
        layout.setText(font, sText);
        font.draw(batch, sText, btnSkipX + (btnW - layout.width) / 2f, btnSkipY + (btnH + layout.height) / 2f);
        batch.end();

        // Clicks
        if (Gdx.input.justTouched()) {
            if (hoverSkip) {
                // Stop all music and jump to game
                stopAllMusic();
                launchGame();
            } else if (hoverContinue) {
                if (!overlay2Started) {
                    overlay2Started = true;
                    // Trigger sound/logic
                } else if (finishing) {
                    // If finishing pause, jump to end
                    collapsing = true;
                    collapseTimer = COLLAPSE_DURATION - 0.1f; // Almost done
                }
                // If in middle of slides, maybe fast forward?
            }
        }
    }

    private void stopAllMusic() {
        if (ringMusic != null)
            ringMusic.stop();
        if (pickupMusic != null)
            pickupMusic.stop();
        if (talkingMusic != null)
            talkingMusic.stop();
        if (talingMusic != null)
            talingMusic.stop();
    }

    private void launchGame() {
        stopAllMusic();
        game.setScreen(new GameScreen(game, 0)); // Tutorial Level = 0
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        overlaySpeedUpdated = false; // Recalculate speeds/positions
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
        if (batch != null)
            batch.dispose();
        if (shapeRenderer != null)
            shapeRenderer.dispose();
        if (font != null)
            font.dispose();
        if (captionFont != null)
            captionFont.dispose();

        // Dispose owned textures
        if (ownsTextures) {
            if (img1Texture != null)
                img1Texture.dispose();
            if (img2Texture != null)
                img2Texture.dispose();
        }
        if (ownsImg4 && img4Texture != null)
            img4Texture.dispose();
        if (ownsImg5 && img5Texture != null)
            img5Texture.dispose();
        if (ownsImg6 && img6Texture != null)
            img6Texture.dispose();
        if (ownsImg7 && img7Texture != null)
            img7Texture.dispose();
        if (ownsImg8 && img8Texture != null)
            img8Texture.dispose();
        if (ownsImg9 && img9Texture != null)
            img9Texture.dispose();

        // Dispose music
        if (ringMusic != null)
            ringMusic.dispose();
        if (pickupMusic != null)
            pickupMusic.dispose();
        if (talkingMusic != null)
            talkingMusic.dispose();
        if (talingMusic != null)
            talingMusic.dispose();
    }
}
