package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.audio.Music;

public class TutorialScreen implements Screen {

    private final MyGdxGame game;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private Texture img1Texture;
    private Texture img2Texture;
    private Texture img3Texture;
    private boolean ownsTextures = false;

    private TextureRegion image1;
    private TextureRegion image2;

    private float screenW, screenH;

    // Image 1 (slide in)
    private float x1, y1;
    private boolean image1Done = false;

    // Image 2 (zoom in)
    private float scale2 = 0f;
    // slide-3 support
    private TextureRegion image3;
    private boolean transitionedTo3 = false;
    private float displayTimer = 0f;
    private static final float IMAGE_DISPLAY_DURATION = 1.5f;
    // overlay (image4) for slide3
    private Texture img4Texture;
    private TextureRegion image4;
    private boolean overlayStarted = false;
    private boolean overlayDone = false;
    private float overlayY = 0f;
    private float overlayTargetY = 0f;
    private float overlaySpeed = 800f; // pixels per second
    // second overlay (pic5)
    private Texture img5Texture;
    private TextureRegion image5;
    // additional slide after dialog
    private Texture img6Texture;
    private TextureRegion image6;
    private boolean ownsImg6 = false;
    private boolean transitionedTo6 = false;
    // overlay6 typing state (messages shown on slide 6)
    private boolean overlay6Started = false;
    private boolean overlay6Done = false;
    private float overlay6TypingTimer = 0f;
    private boolean overlay6TypingDone = false;
    private int overlay6Stage = 0;
    private float overlay6PostPauseTimer = 0f;
    private boolean overlay2Started = false;
    private boolean overlay2Done = false;
    private float overlay2Y = 0f;
    private float overlay2TargetY = 0f;
    // overlay2 X positions (for slide-in from right)
    private float overlay2X = 0f;
    private float overlay2TargetX = 0f;
    private boolean ownsImg4 = false;
    private boolean ownsImg5 = false;
    // pickup timer: start counting when pickup sound starts; show pic5 after delay
    private float pickupTimer = 0f;
    // make pic5 appear earlier by using a shorter delay
    private static final float PICKUP_DELAY = 1f;
    // text to show after pic5 appears: prefix shows instantly, main types
    private String overlay2Prefix = "Loloy:";
    private String overlay2Main = "Fixer! Are you in?";
    // typing state machine for overlay2 text (multiple messages)
    private float overlay2TypingTimer = 0f;
    private boolean overlay2TypingDone = false;
    // stage machine: even = typing stage, odd = pause stage; overlay2Stage starts at 0
    private int overlay2Stage = 0;
    // post-stage pause timer (used between typing stages)
    private float overlay2PostPauseTimer = 0f;
    // text position (easy to edit): coordinates from bottom-left
    private float overlay2TextPosX = 20f;
    private float overlay2TextPosY = 50f;

    // Simple text buttons
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float btnW = 150f, btnH = 45f;
    private float btnContinueX, btnContinueY, btnSkipX, btnSkipY;
    private boolean buttonsShown = false;

    // Music for slide 2
    private Music ringMusic;
    private boolean musicStarted = false;
    // pickup sound to play after overlay finishes
    private Music pickupMusic;
    private boolean pickupStarted = false;
    // talking/background dialogue music to play while dialog sequence runs
    private Music talkingMusic;
    private boolean talkingStarted = false;
    // flag to mark we've stopped/disposed the talking music when final text finished
    private boolean talkingStopped = false;
    // fade control for talking music
    private boolean talkingFading = false;
    private float talkingFadeTimer = 0f;
    private static final float TALKING_FADE_DURATION = 1f; // fade-out duration in seconds
    private float talkingInitialVolume = 0.8f;

    // Button customization
    // Requested color: rgb(9,35,53) -> normalized floats
    private Color btnColor = new Color(9f/255f, 35f/255f, 53f/255f, 1f);       // Normal color
    // Hover color: slightly lighter variant
    private Color btnHoverColor = new Color(20f/255f, 50f/255f, 70f/255f, 1f); // Hover color
    private float cornerRadius = 10f; // Rounded corners

    public TutorialScreen(MyGdxGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // -------------------------------
        // Load images
        // -------------------------------
        if (game != null && game.tutorialImages != null && game.tutorialImages.length >= 2) {
            img1Texture = game.tutorialImages[0];
            img2Texture = game.tutorialImages[1];
            if (game.tutorialImages.length >= 3) {
                img3Texture = game.tutorialImages[2];
            }
            ownsTextures = false;
        } else {
            ownsTextures = true;
            try {
                img1Texture = new Texture(Gdx.files.internal("The Urgent Call/1.png"));
            } catch (Exception e) {
                img1Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/1.png"));
            }
            try {
                img2Texture = new Texture(Gdx.files.internal("The Urgent Call/2.png"));
            } catch (Exception e) {
                img2Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/2.png"));
            }
            try {
                img3Texture = new Texture(Gdx.files.internal("The Urgent Call/3.png"));
            } catch (Exception e) {
                // absolute fallback; if not present this may throw later when used
                try {
                    img3Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/3.png"));
                } catch (Exception ignored) {
                    img3Texture = null;
                }
            }
        }

        image1 = new TextureRegion(img1Texture);
        image2 = new TextureRegion(img2Texture);
        if (img3Texture != null) image3 = new TextureRegion(img3Texture);
        // load image4 if available (overlay for slide 3)
        try {
            img4Texture = new Texture(Gdx.files.internal("The Urgent Call/4.png"));
            ownsImg4 = true;
        } catch (Exception e) {
            try {
                img4Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/4.png"));
                ownsImg4 = true;
            } catch (Exception ignored) {
                img4Texture = null;
                ownsImg4 = false;
            }
        }
        if (img4Texture != null) image4 = new TextureRegion(img4Texture);
        // load image5 if available (overlay after slide 4)
        try {
            img5Texture = new Texture(Gdx.files.internal("The Urgent Call/5.png"));
            ownsImg5 = true;
        } catch (Exception e) {
            try {
                img5Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/5.png"));
                ownsImg5 = true;
            } catch (Exception ignored) {
                img5Texture = null;
                ownsImg5 = false;
            }
        }
        if (img5Texture != null) image5 = new TextureRegion(img5Texture);
        // load image6 if available (new slide after dialog)
        try {
            img6Texture = new Texture(Gdx.files.internal("The Urgent Call/6.png"));
            ownsImg6 = true;
        } catch (Exception e) {
            try {
                img6Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/6.png"));
                ownsImg6 = true;
            } catch (Exception ignored) {
                img6Texture = null;
                ownsImg6 = false;
            }
        }
        if (img6Texture != null) image6 = new TextureRegion(img6Texture);

        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        // Image 1 start off-screen
        x1 = -image1.getRegionWidth();
        y1 = (screenH - image1.getRegionHeight()) / 2f;

        scale2 = 0f;

        // -------------------------------
        // Font: prefer loading Small_white (internal), then Pexelify, then fallback
        // -------------------------------
        try {
            // try Small_white first
            com.badlogic.gdx.files.FileHandle fntS = Gdx.files.internal("smallwhite/Small_white.fnt");
            com.badlogic.gdx.files.FileHandle imgS = Gdx.files.internal("smallwhite/Small_white.png");
            if (fntS.exists() && imgS.exists()) {
                font = new BitmapFont(fntS, imgS, false);
                Gdx.app.log("TutorialScreen", "Loaded internal Small_white font and image");
            } else {
                // try absolute asset path
                String baseSW = System.getProperty("user.dir") + "/assets/smallwhite/";
                com.badlogic.gdx.files.FileHandle fntSA = Gdx.files.absolute(baseSW + "Small_white.fnt");
                com.badlogic.gdx.files.FileHandle imgSA = Gdx.files.absolute(baseSW + "Small_white.png");
                if (fntSA.exists() && imgSA.exists()) {
                    font = new BitmapFont(fntSA, imgSA, false);
                    Gdx.app.log("TutorialScreen", "Loaded absolute Small_white font and image: " + baseSW);
                } else {
                    // fallback to Pexelify (internal)
                    com.badlogic.gdx.files.FileHandle fntP = Gdx.files.internal("fonts/Pexelify_Sans.fnt");
                    com.badlogic.gdx.files.FileHandle imgP = Gdx.files.internal("fonts/Pexelify_Sans.png");
                    if (fntP.exists() && imgP.exists()) {
                        font = new BitmapFont(fntP, imgP, false);
                        Gdx.app.log("TutorialScreen", "Loaded internal Pexelify font and image as fallback");
                    } else {
                        // try absolute Pexelify path
                        String baseP = System.getProperty("user.dir") + "/assets/fonts/";
                        com.badlogic.gdx.files.FileHandle fntPA = Gdx.files.absolute(baseP + "Pexelify_Sans.fnt");
                        com.badlogic.gdx.files.FileHandle imgPA = Gdx.files.absolute(baseP + "Pexelify_Sans.png");
                        if (fntPA.exists() && imgPA.exists()) {
                            font = new BitmapFont(fntPA, imgPA, false);
                            Gdx.app.log("TutorialScreen", "Loaded absolute Pexelify font and image as fallback: " + baseP);
                        } else if (fntP.exists()) {
                            font = new BitmapFont(fntP);
                            Gdx.app.log("TutorialScreen", "Loaded Pexelify .fnt (internal) as fallback");
                        } else {
                            font = new BitmapFont();
                            Gdx.app.log("TutorialScreen", "No preferred font found; using default BitmapFont");
                        }
                    }
                }
            }
        } catch (Exception e) {
            font = new BitmapFont();
            Gdx.app.log("TutorialScreen", "Error loading font; using default", e);
        }

        // Apply requested style: scale and white text color
        try { font.getData().setScale(0.9f); } catch (Exception ignored) {}
        font.setColor(Color.WHITE);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Compute button positions (bottom right)
        float paddingRight = 20f;
        float paddingBottom = 10f;
        float spacing = 20f;
        btnSkipX = screenW - paddingRight - btnW;
        btnSkipY = paddingBottom;
        btnContinueX = btnSkipX - btnW - spacing;
        btnContinueY = paddingBottom;

        // -------------------------------
        // Load slide-2 music (looped)
        // -------------------------------
        try {
            com.badlogic.gdx.files.FileHandle musicInternal = Gdx.files.internal("Sounds/old-telephone-ringing-362034.mp3");
            if (musicInternal.exists()) {
                ringMusic = Gdx.audio.newMusic(musicInternal);
                Gdx.app.log("TutorialScreen", "Loaded internal ring music");
            } else {
                String abs = System.getProperty("user.dir") + "/assets/Sounds/old-telephone-ringing-362034.mp3";
                com.badlogic.gdx.files.FileHandle musicAbs = Gdx.files.absolute(abs);
                if (musicAbs.exists()) {
                    ringMusic = Gdx.audio.newMusic(musicAbs);
                    Gdx.app.log("TutorialScreen", "Loaded absolute ring music: " + abs);
                }
            }
            if (ringMusic != null) {
                ringMusic.setLooping(true);
                ringMusic.setVolume(0.5f);
            }
        } catch (Exception e) {
            Gdx.app.log("TutorialScreen", "Failed to load ring music", e);
        }
        // -------------------------------
        // Load talking/dialogue music (played while dialog runs)
        // -------------------------------
        try {
            com.badlogic.gdx.files.FileHandle talkInternal = Gdx.files.internal("Sounds/Talking.mp3");
            if (talkInternal.exists()) {
                talkingMusic = Gdx.audio.newMusic(talkInternal);
                Gdx.app.log("TutorialScreen", "Loaded internal talking music");
            } else {
                String absTalk = System.getProperty("user.dir") + "/assets/Sounds/Talking.mp3";
                com.badlogic.gdx.files.FileHandle talkAbs = Gdx.files.absolute(absTalk);
                if (talkAbs.exists()) {
                    talkingMusic = Gdx.audio.newMusic(talkAbs);
                    Gdx.app.log("TutorialScreen", "Loaded absolute talking music: " + absTalk);
                }
            }
            if (talkingMusic != null) {
                // we'll start it later when pic5 has appeared; prepare defaults
                talkingMusic.setLooping(true);
                talkingMusic.setVolume(0.8f);
            }
        } catch (Exception e) {
            Gdx.app.log("TutorialScreen", "Failed to load talking music", e);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float dt = delta;

        // --------------------------
        // Image 1 Slide-in
        // --------------------------
        if (!image1Done) {
            x1 += 400 * dt;
            if (x1 >= (screenW - image1.getRegionWidth()) / 2f) {
                x1 = (screenW - image1.getRegionWidth()) / 2f;
                image1Done = true;
            }
        } else {
            // --------------------------
            // Image 2 Zoom-in
            // --------------------------
            if (scale2 < 1f) {
                scale2 += 0.8f * dt;
                if (scale2 > 1f) scale2 = 1f;
            }

            if (scale2 >= 1f && !buttonsShown) {
                buttonsShown = true;
                // start the ring music when slide 2 appears
                if (ringMusic != null && !musicStarted) {
                    try { ringMusic.play(); } catch (Exception e) { Gdx.app.log("TutorialScreen","Could not play ring music", e); }
                    musicStarted = true;
                }
            }
            // after slide2 is fully visible, count display time then transition to slide3
            if (scale2 >= 1f && !transitionedTo3) {
                displayTimer += dt;
                if (displayTimer >= IMAGE_DISPLAY_DURATION) {
                    // instant transition: show image3 immediately (no zoom)
                    if (image3 != null) {
                        image1 = image3;          // replace the displayed image
                        image1Done = true;
                        // clear image2 so we don't try to zoom it
                        image2 = null;
                        // continue music into overlay (will stop after overlay completes)
                        // ensure zoom is disabled
                        scale2 = 1f;
                        transitionedTo3 = true;
                        // start overlay animation if we have an overlay image4
                        if (image4 != null) {
                            overlayStarted = true;
                            overlayDone = false;
                            // start overlay above the screen
                            overlayY = screenH + 10f;
                            overlayTargetY = (screenH - image4.getRegionHeight()) / 2f;
                            // hide buttons until overlay finishes
                            buttonsShown = false;
                        } else {
                            // no overlay -> show buttons immediately
                            buttonsShown = true;
                        }
                        displayTimer = 0f;
                    }
                }
            }

            // handle overlay slide-in animation when on slide3
            if (overlayStarted && !overlayDone) {
                overlayY -= overlaySpeed * dt;
                if (overlayY <= overlayTargetY) {
                    overlayY = overlayTargetY;
                    overlayDone = true;
                    buttonsShown = true; // reveal buttons when overlay settled
                    // stop ring music now that overlay (pic4) is settled
                    if (ringMusic != null) {
                        try { ringMusic.stop(); ringMusic.dispose(); } catch (Exception ignored) {}
                        ringMusic = null;
                        musicStarted = false;
                    }
                    // play pickup sound once (continue as Music if needed)
                    if (!pickupStarted) {
                        try {
                            com.badlogic.gdx.files.FileHandle pickInternal = Gdx.files.internal("Sounds/Telephone Ring Pick Up Sound Effect.mp3");
                            if (pickInternal.exists()) {
                                pickupMusic = Gdx.audio.newMusic(pickInternal);
                            } else {
                                String absPick = System.getProperty("user.dir") + "/assets/Sounds/Telephone Ring Pick Up Sound Effect.mp3";
                                com.badlogic.gdx.files.FileHandle pickAbs = Gdx.files.absolute(absPick);
                                if (pickAbs.exists()) pickupMusic = Gdx.audio.newMusic(pickAbs);
                            }
                            if (pickupMusic != null) {
                                pickupMusic.setLooping(false);
                                pickupMusic.setVolume(0.9f);
                                pickupMusic.play();
                                pickupStarted = true;
                                pickupTimer = 0f;
                            } else {
                                // no pickup audio file available: still wait PICKUP_DELAY then show pic5
                                pickupStarted = true;
                                pickupTimer = 0f;
                            }
                        } catch (Exception e) {
                            Gdx.app.log("TutorialScreen", "Failed to play pickup sound", e);
                            // on error, still start the pickup timer so pic5 appears after PICKUP_DELAY
                            pickupStarted = true;
                            pickupTimer = 0f;
                        }
                    }
                }
            }
            // if pickup music was started, wait PICKUP_DELAY seconds then start overlay2 (pic5)
            if (pickupStarted && !overlay2Started) {
                pickupTimer += dt;
                if (pickupTimer >= PICKUP_DELAY && image5 != null) {
                    overlay2Started = true;
                    overlay2Done = false;
                    // slide in from right: start off to the right
                    overlay2X = screenW + 10f;
                    overlay2TargetX = (screenW - image5.getRegionWidth()) / 2f;
                    // vertical center
                    overlay2Y = (screenH - image5.getRegionHeight()) / 2f;
                    buttonsShown = false;
                }
            }
            // handle overlay2 slide-in animation (from right)
            if (overlay2Started && !overlay2Done) {
                overlay2X -= overlaySpeed * dt;
                if (overlay2X <= overlay2TargetX) {
                    overlay2X = overlay2TargetX;
                    overlay2Done = true;
                    // start/reset typing transition when overlay2 finishes sliding
                    overlay2TypingTimer = 0f;
                    overlay2TypingDone = false;
                    // initialize multi-stage typing: start at stage 0 (first message)
                    overlay2Stage = 0;
                    overlay2PostPauseTimer = 0f; // reset post-stage pause timer
                    buttonsShown = true;
                        // start talking/dialogue music when pic5 has settled
                        if (talkingMusic != null && !talkingStarted) {
                            try {
                                talkingMusic.play();
                                talkingStarted = true;
                                talkingStopped = false;
                            } catch (Exception e) {
                                Gdx.app.log("TutorialScreen", "Could not play talking music", e);
                            }
                        }
                    // nothing further to play after this; pickup already played
                }
            }
            
        }

        // Draw images
        batch.begin();
        batch.draw(image1, x1, y1);
        if (image1Done && image2 != null) {
            float img2W = image2.getRegionWidth() * scale2;
            float img2H = image2.getRegionHeight() * scale2;
            float x2 = (screenW - img2W) / 2f;
            float y2 = (screenH - img2H) / 2f;
            batch.draw(image2, x2, y2, img2W, img2H);
        }

        // Draw overlay (image4) sliding in from above when active
        if (overlayStarted && image4 != null) {
            float img4W = image4.getRegionWidth();
            float img4H = image4.getRegionHeight();
            float x4 = (screenW - img4W) / 2f;
            batch.draw(image4, x4, overlayY, img4W, img4H);
            // draw prefix instantly once image4 has settled
            if (overlayDone && overlay2Prefix != null && overlay2Prefix.length() > 0) {
                float prevScaleX = 1f; float prevScaleY = 1f;
                try { prevScaleX = font.getData().scaleX; prevScaleY = font.getData().scaleY; } catch (Exception ignored) {}
                float targetScale = 1.6f;
                font.getData().setScale(targetScale);

                layout.setText(font, overlay2Prefix);
                float tx = overlay2TextPosX;
                float ty = overlay2TextPosY + layout.height;

                Color prevColor = font.getColor().cpy();
                font.setColor(Color.BLACK);
                font.draw(batch, overlay2Prefix, tx - 1f, ty);
                font.draw(batch, overlay2Prefix, tx + 1f, ty);
                font.draw(batch, overlay2Prefix, tx, ty - 1f);
                font.draw(batch, overlay2Prefix, tx, ty + 1f);
                font.setColor(Color.WHITE);
                font.draw(batch, overlay2Prefix, tx, ty);
                font.setColor(prevColor);

                font.getData().setScale(prevScaleX, prevScaleY);
            }
        }

        // Draw second overlay (image5) sliding in from the right when active
        if ((image5 != null && (overlay2Started || overlay2Done)) || transitionedTo6) {
            // draw the current image5 if present and active; if we've transitioned to 6, image1 is set to image6
            if (image5 != null && (overlay2Started || overlay2Done)) {
                float img5W = image5.getRegionWidth();
                float img5H = image5.getRegionHeight();
                float x5 = overlay2X; // horizontal position controlled by slide-in
                batch.draw(image5, x5, overlay2Y, img5W, img5H);
            }
            // draw post-image text (prefix instant, main types) when overlay2 is settled
            if (overlay2Done && ((overlay2Prefix != null && overlay2Prefix.length() > 0) || (overlay2Main != null && overlay2Main.length() > 0))) {
                // Multi-stage sequence:
                // texts and typing durations
                final String[] STAGE_TEXTS = new String[] {
                    "Fixer! Are you in?",    // index 0
                    "Good",                  // index 1
                    "No, wait don't answer.",// index 2
                    "I don't care. Just listen" // index 3
                };
                final float[] STAGE_DURATIONS = new float[] {
                    2f, // Fixer! Are you in? -> 2s
                    1f, // Good -> 1s
                    2f, // No, wait—don't answer. -> 2s
                    2f  // I don't care... -> 2s
                };
                final float STAGE_PAUSE = 1f; // 1s pause after each typing

                String typed = "";

                // stage encoding: even stage = typing stage for STAGE_TEXTS[stage/2]
                // odd stage = pause after that typing
                // initial overlay2Stage is 0 (type first message)
                if (overlay2Stage >= 8) {
                    // finished all stages, show final message fully
                    overlay2Main = STAGE_TEXTS[3];
                    typed = overlay2Main;
                    // after dialog fully finished, transition to slide 6 (no overlay) if available
                    if (!transitionedTo6 && image6 != null) {
                        image1 = image6; // make slide 6 the background
                        // stop drawing image5 overlay but keep text
                        image5 = null;
                        overlay2Started = false;
                        overlay2Done = false;
                        // also disable image4 overlay so it doesn't draw over slide 6
                        overlayStarted = false;
                        overlayDone = false;
                        image4 = null;
                        transitionedTo6 = true;
                        // ensure overlay6 typing state is initialized when we transition
                        overlay6Started = true;
                        overlay6Done = false;
                        overlay6Stage = 0;
                        overlay6TypingTimer = 0f;
                        overlay6TypingDone = false;
                        overlay6PostPauseTimer = 0f;
                        buttonsShown = true;
                    }
                } else if ((overlay2Stage % 2) == 1) {
                    // pause stage
                    overlay2PostPauseTimer += dt;
                    int lastTextIndex = (overlay2Stage - 1) / 2;
                    overlay2Main = STAGE_TEXTS[Math.min(lastTextIndex, STAGE_TEXTS.length - 1)];
                    typed = overlay2Main;
                    if (overlay2PostPauseTimer >= STAGE_PAUSE) {
                        overlay2PostPauseTimer = 0f;
                        overlay2Stage += 1; // move to next typing stage
                        overlay2TypingTimer = 0f;
                        overlay2TypingDone = false;
                    }
                } else {
                    // typing stage
                    int textIndex = overlay2Stage / 2;
                    if (textIndex < 0) textIndex = 0;
                    if (textIndex >= STAGE_TEXTS.length) textIndex = STAGE_TEXTS.length - 1;
                    overlay2Main = STAGE_TEXTS[textIndex];
                    float currentDuration = STAGE_DURATIONS[textIndex];

                    if (!overlay2TypingDone) overlay2TypingTimer += dt;
                    int totalChars = overlay2Main != null ? overlay2Main.length() : 0;
                    float progress = Math.min(1f, overlay2TypingTimer / Math.max(0.0001f, currentDuration));
                    int chars = (int)Math.floor(progress * totalChars);
                    if (chars >= totalChars) {
                        chars = totalChars;
                        overlay2TypingDone = true;
                    }
                    typed = (overlay2Main != null && chars > 0) ? overlay2Main.substring(0, chars) : "";

                    if (overlay2TypingDone) {
                        // move to pause stage after typing
                        overlay2Stage += 1;
                        // reset timers (post pause will accumulate)
                        overlay2TypingTimer = 0f;
                        overlay2TypingDone = false;
                    }
                }

                // Make the text bigger and bold-looking at left-bottom (position editable)
                float prevScaleX = 1f; float prevScaleY = 1f;
                try { prevScaleX = font.getData().scaleX; prevScaleY = font.getData().scaleY; } catch (Exception ignored) {}
                float targetScale = 1.6f;
                font.getData().setScale(targetScale);

                // Fade out talking music over TALKING_FADE_DURATION when the final message finished
                if (talkingStarted && !talkingStopped && !talkingFading) {
                    if (transitionedTo6) {
                        // overlay6 now has four messages -> finish when overlay6Stage >= 8
                        if (overlay6Stage >= 8) {
                            talkingFading = true;
                            talkingFadeTimer = 0f;
                            if (talkingMusic != null) {
                                try { talkingInitialVolume = talkingMusic.getVolume(); } catch (Exception ignored) { talkingInitialVolume = 0.8f; }
                            }
                        }
                    } else {
                        if (overlay2Stage >= 7) {
                            talkingFading = true;
                            talkingFadeTimer = 0f;
                            if (talkingMusic != null) {
                                try { talkingInitialVolume = talkingMusic.getVolume(); } catch (Exception ignored) { talkingInitialVolume = 0.8f; }
                            }
                        }
                    }
                }
                // handle ongoing fade
                if (talkingFading) {
                    talkingFadeTimer += dt;
                    float p = Math.min(1f, talkingFadeTimer / Math.max(0.0001f, TALKING_FADE_DURATION));
                    if (talkingMusic != null) {
                        try { talkingMusic.setVolume((1f - p) * talkingInitialVolume); } catch (Exception ignored) {}
                    }
                    if (p >= 1f) {
                        // finished fade: stop and dispose
                        if (talkingMusic != null) {
                            try { talkingMusic.stop(); talkingMusic.dispose(); } catch (Exception ignored) {}
                            talkingMusic = null;
                        }
                        talkingStopped = true;
                        talkingStarted = false;
                        talkingFading = false;
                    }
                }

                // draw prefix instantly
                float tx = overlay2TextPosX;
                layout.setText(font, overlay2Prefix != null ? overlay2Prefix : "");
                float prefixWidth = layout.width;
                float ty = overlay2TextPosY + layout.height;

                // draw typed main text after prefix with a small gap
                if (typed.length() > 0) {
                    float gap = 8f;
                    float txMain = tx + prefixWidth + gap;
                    Color prevColor2 = font.getColor().cpy();
                    font.setColor(Color.BLACK);
                    font.draw(batch, typed, txMain - 1f, ty);
                    font.draw(batch, typed, txMain + 1f, ty);
                    font.draw(batch, typed, txMain, ty - 1f);
                    font.draw(batch, typed, txMain, ty + 1f);
                    font.setColor(Color.WHITE);
                    font.draw(batch, typed, txMain, ty);
                    font.setColor(prevColor2);
                }

                // restore font scale
                font.getData().setScale(prevScaleX, prevScaleY);
            }
        }

        // Draw overlay6 typing on slide 6 (prefix + two messages) when transitioned
        if (transitionedTo6 && overlay6Started) {
            // scale font
            float prevScaleX6 = 1f; float prevScaleY6 = 1f;
            try { prevScaleX6 = font.getData().scaleX; prevScaleY6 = font.getData().scaleY; } catch (Exception ignored) {}
            float targetScale6 = 1.6f;
            font.getData().setScale(targetScale6);

            float tx6 = overlay2TextPosX;
            layout.setText(font, overlay2Prefix != null ? overlay2Prefix : "");
            float prefixWidth6 = layout.width;
            float ty6 = overlay2TextPosY + layout.height;

            // draw prefix
            Color prevC = font.getColor().cpy();
            font.setColor(Color.BLACK);
            font.draw(batch, overlay2Prefix, tx6 - 1f, ty6);
            font.draw(batch, overlay2Prefix, tx6 + 1f, ty6);
            font.draw(batch, overlay2Prefix, tx6, ty6 - 1f);
            font.draw(batch, overlay2Prefix, tx6, ty6 + 1f);
            font.setColor(Color.WHITE);
            font.draw(batch, overlay2Prefix, tx6, ty6);
            font.setColor(prevC);

            final String[] STAGE6_TEXTS = new String[] {
                "The auditors are here early!",
                "They're hitting the main accounts in two hours.",
                "Two hours! This is a Paper Panic Trail, my friend",
                "and you're running it!"
            };
            final float[] STAGE6_DURATIONS = new float[] { 2f, 3f, 2f, 2f };
            final float STAGE6_PAUSE = 1f;

            String typed6 = "";
            if (overlay6Stage >= STAGE6_TEXTS.length * 2) {
                overlay6Done = true;
                typed6 = STAGE6_TEXTS[STAGE6_TEXTS.length - 1];
            } else if ((overlay6Stage % 2) == 1) {
                overlay6PostPauseTimer += dt;
                int lastTextIndex = (overlay6Stage - 1) / 2;
                lastTextIndex = Math.min(lastTextIndex, STAGE6_TEXTS.length - 1);
                typed6 = STAGE6_TEXTS[lastTextIndex];
                if (overlay6PostPauseTimer >= STAGE6_PAUSE) {
                    overlay6PostPauseTimer = 0f;
                    overlay6Stage += 1;
                    overlay6TypingTimer = 0f;
                    overlay6TypingDone = false;
                }
            } else {
                int textIndex = overlay6Stage / 2;
                if (textIndex < 0) textIndex = 0;
                if (textIndex >= STAGE6_TEXTS.length) textIndex = STAGE6_TEXTS.length - 1;
                String current = STAGE6_TEXTS[textIndex];
                float currentDuration = STAGE6_DURATIONS[textIndex];
                if (!overlay6TypingDone) overlay6TypingTimer += dt;
                int totalChars = current != null ? current.length() : 0;
                float progress = Math.min(1f, overlay6TypingTimer / Math.max(0.0001f, currentDuration));
                int chars = (int)Math.floor(progress * totalChars);
                if (chars >= totalChars) {
                    chars = totalChars;
                    overlay6TypingDone = true;
                }
                typed6 = (current != null && chars > 0) ? current.substring(0, chars) : "";
                if (overlay6TypingDone) {
                    overlay6Stage += 1;
                    overlay6TypingTimer = 0f;
                    overlay6TypingDone = false;
                }
            }

            if (typed6.length() > 0) {
                float gap = 8f;
                float txMain6 = tx6 + prefixWidth6 + gap;
                Color prevColor6 = font.getColor().cpy();
                font.setColor(Color.BLACK);
                font.draw(batch, typed6, txMain6 - 1f, ty6);
                font.draw(batch, typed6, txMain6 + 1f, ty6);
                font.draw(batch, typed6, txMain6, ty6 - 1f);
                font.draw(batch, typed6, txMain6, ty6 + 1f);
                font.setColor(Color.WHITE);
                font.draw(batch, typed6, txMain6, ty6);
                font.setColor(prevColor6);
            }

            font.getData().setScale(prevScaleX6, prevScaleY6);
        }

        batch.end();

        // Draw procedural rounded buttons
        if (buttonsShown) {
            float mx = Gdx.input.getX();
            float my = screenH - Gdx.input.getY();

            Color cContinue = btnColor;
            Color cSkip = btnColor;

            // Hover detection
            if (mx >= btnContinueX && mx <= btnContinueX + btnW && my >= btnContinueY && my <= btnContinueY + btnH)
                cContinue = btnHoverColor;
            if (mx >= btnSkipX && mx <= btnSkipX + btnW && my >= btnSkipY && my <= btnSkipY + btnH)
                cSkip = btnHoverColor;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // Continue button
            shapeRenderer.setColor(cContinue);
            shapeRenderer.rect(btnContinueX + cornerRadius, btnContinueY, btnW - 2*cornerRadius, btnH);
            shapeRenderer.rect(btnContinueX, btnContinueY + cornerRadius, btnW, btnH - 2*cornerRadius);
            shapeRenderer.circle(btnContinueX + cornerRadius, btnContinueY + cornerRadius, cornerRadius);
            shapeRenderer.circle(btnContinueX + btnW - cornerRadius, btnContinueY + cornerRadius, cornerRadius);
            shapeRenderer.circle(btnContinueX + cornerRadius, btnContinueY + btnH - cornerRadius, cornerRadius);
            shapeRenderer.circle(btnContinueX + btnW - cornerRadius, btnContinueY + btnH - cornerRadius, cornerRadius);

            // Skip button
            shapeRenderer.setColor(cSkip);
            shapeRenderer.rect(btnSkipX + cornerRadius, btnSkipY, btnW - 2*cornerRadius, btnH);
            shapeRenderer.rect(btnSkipX, btnSkipY + cornerRadius, btnW, btnH - 2*cornerRadius);
            shapeRenderer.circle(btnSkipX + cornerRadius, btnSkipY + cornerRadius, cornerRadius);
            shapeRenderer.circle(btnSkipX + btnW - cornerRadius, btnSkipY + cornerRadius, cornerRadius);
            shapeRenderer.circle(btnSkipX + cornerRadius, btnSkipY + btnH - cornerRadius, cornerRadius);
            shapeRenderer.circle(btnSkipX + btnW - cornerRadius, btnSkipY + btnH - cornerRadius, cornerRadius);

            shapeRenderer.end();

            // Draw button texts
            batch.begin();
            layout.setText(font, "Continue");
            font.draw(batch, "Continue", btnContinueX + (btnW - layout.width) / 2f, btnContinueY + (btnH + layout.height) / 2f);
            layout.setText(font, "Skip");
            font.draw(batch, "Skip", btnSkipX + (btnW - layout.width) / 2f, btnSkipY + (btnH + layout.height) / 2f);
            batch.end();

            // Button input detection
            if (Gdx.input.justTouched()) {
                if (mx >= btnContinueX && mx <= btnContinueX + btnW && my >= btnContinueY && my <= btnContinueY + btnH) {
                    Gdx.app.log("UI", "Continue clicked");
                    // TODO: go to next screen
                }
                if (mx >= btnSkipX && mx <= btnSkipX + btnW && my >= btnSkipY && my <= btnSkipY + btnH) {
                    Gdx.app.log("UI", "Skip clicked");
                    // stop music before leaving
                    if (ringMusic != null) {
                        try { ringMusic.stop(); ringMusic.dispose(); } catch (Exception ignored) {}
                        ringMusic = null;
                        musicStarted = false;
                    }
                    if (pickupMusic != null) {
                        try { pickupMusic.stop(); pickupMusic.dispose(); } catch (Exception ignored) {}
                        pickupMusic = null;
                        pickupStarted = false;
                    }
                    if (talkingMusic != null) {
                        try { talkingMusic.stop(); talkingMusic.dispose(); } catch (Exception ignored) {}
                        talkingMusic = null;
                        talkingStarted = false;
                        talkingStopped = false;
                    }
                    game.setScreen(new MainMenuScreen(game));
                }
            }
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (ownsTextures) {
            img1Texture.dispose();
            img2Texture.dispose();
        }
        if (ownsImg4 && img4Texture != null) img4Texture.dispose();
        if (ownsImg5 && img5Texture != null) img5Texture.dispose();
        if (ownsImg6 && img6Texture != null) img6Texture.dispose();
        font.dispose();
        if (ringMusic != null) {
            try { ringMusic.stop(); ringMusic.dispose(); } catch (Exception ignored) {}
            ringMusic = null;
            musicStarted = false;
        }
        if (pickupMusic != null) {
            try { pickupMusic.stop(); pickupMusic.dispose(); } catch (Exception ignored) {}
            pickupMusic = null;
            pickupStarted = false;
        }
        if (talkingMusic != null) {
            try { talkingMusic.stop(); talkingMusic.dispose(); } catch (Exception ignored) {}
            talkingMusic = null;
            talkingStarted = false;
            talkingStopped = false;
        }
    }
}
