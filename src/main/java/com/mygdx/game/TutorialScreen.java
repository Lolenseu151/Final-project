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

    // Simple text buttons
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float btnW = 150f, btnH = 45f;
    private float btnContinueX, btnContinueY, btnSkipX, btnSkipY;
    private boolean buttonsShown = false;

    // Music for slide 2
    private Music ringMusic;
    private boolean musicStarted = false;

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
        } catch (Exception e) {
            try {
                img4Texture = new Texture(Gdx.files.absolute(System.getProperty("user.dir") + "/assets/The Urgent Call/4.png"));
            } catch (Exception ignored) {
                img4Texture = null;
            }
        }
        if (img4Texture != null) image4 = new TextureRegion(img4Texture);

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
                    // stop music now that overlay (pic4) is settled
                    if (ringMusic != null) {
                        try { ringMusic.stop(); ringMusic.dispose(); } catch (Exception ignored) {}
                        ringMusic = null;
                        musicStarted = false;
                    }
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
        font.dispose();
        if (ringMusic != null) {
            try { ringMusic.stop(); ringMusic.dispose(); } catch (Exception ignored) {}
            ringMusic = null;
            musicStarted = false;
        }
    }
}
