package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Tutorial map for the Archive floor. Simple layout with a highlighted light area
 * near the 'property files' to teach movement.
 */
public class LevelTutorial implements Level, BackgroundedLevel {

    private final Array<Rectangle> documents = new Array<>();
    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Rectangle> obstacles = new Array<>();
    private final Array<Rectangle> beams = new Array<>();
    private Rectangle shredder;
    private int totalDocs = 0;

    

    // special "light" area representing the property files spotlight
    private Rectangle propertyFilesLight;

    // Customizable tutorial text (defaults provided)
    private String tutorialTitle = "MOVEMENT:";
    private String tutorialDetail = "Use [W], [A], [S], [D] to navigate the Archive floor.";
    private String tutorialHint = "Try moving into the light near the property files.";
    // Should the tutorial overlay box be shown? Allows the player to dismiss it with OK.
    private boolean showOverlay = true;

    private static final float DOC_SIZE = 36f;
    private static final float PLATFORM_H = 27f;

    // Optional explicit positions for the documents that appear after the final caption.
    // Edit this array to specify exact X/Y pixel positions for each spawned document.
    // Example:
    // public static float[][] TUTORIAL_DOC_POSITIONS = new float[][] { {400f,300f}, {480f,300f}, {560f,300f}, {640f,300f} };
    public static float[][] TUTORIAL_DOC_POSITIONS = new float[][] {
        { 200f, 260f },
        { 700f, 260f },
        { 580f, 850f },
        { 100f, 20f }
    };

    // Allow this level to request a different document draw size (only affects visuals)
    private float documentScale = 1.5f; // 150% size for tutorial

    /**
     * Return the desired document pixel size for rendering in this level.
     * LevelManager will prefer this when this level is active.
     */
    public float getDocumentSize() { return DOC_SIZE * documentScale; }

    public void setDocumentScale(float scale) { if (scale > 0f) this.documentScale = scale; }

    @Override
    public void init() {
        documents.clear(); platforms.clear(); obstacles.clear(); beams.clear();

        float w = Gdx.graphics.getWidth();

        // Basic ground and a couple of low platforms to keep movement simple
        platforms.add(new Rectangle(0, 0, w, PLATFORM_H));
        platforms.add(new Rectangle(0, 217, 1132, PLATFORM_H));
        platforms.add(new Rectangle(w - 555, 432, 650, PLATFORM_H));
        platforms.add(new Rectangle(0, 432, 588, PLATFORM_H));
        
        // Place a few sample documents
        documents.add(new Rectangle(200, 500, DOC_SIZE, DOC_SIZE));


        // Shredder off to the right
        shredder = new Rectangle(w - 80, 10, 50, 50);

        // Property files light area (player should move into this light)
        float lightW = 160f, lightH = 120f;
        propertyFilesLight = new Rectangle(w/2f - lightW/2f, 220f, lightW, lightH);

        totalDocs = documents.size;
    }

    @Override public Array<Rectangle> getDocuments() { return documents; }
    @Override public Array<Rectangle> getPlatforms() { return platforms; }
    @Override public Array<Rectangle> getObstacles() { return obstacles; }
    @Override public Array<Rectangle> getAuditorBeams() { return beams; }
    @Override public Rectangle getShredder() { return shredder; }
    @Override public int getTotalDocuments() { return totalDocs; }

    @Override
    public String getBackgroundPath() {
        // use the tutorial map provided in assets: "tutorial map.png"
        return "tutorial map.png";
    }

    @Override
    public void renderBackground(SpriteBatch batch, com.badlogic.gdx.graphics.Texture backgroundTex) {
        // Draw the default background via LevelManager, then overlay a subtle light at propertyFilesLight
        if (backgroundTex != null) {
            batch.draw(backgroundTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        // the actual light highlight will be drawn by the LevelManager or GameScreen overlay logic
        // Draw the final tutorial overlay image full-screen when requested
        if (finalOverlayVisible) {
            if (finalTalkingTex == null) {
                try {
                    finalTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/Loloy talk.png"));
                } catch (Exception e) {
                    try { finalTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("Loloy talk.png")); }
                    catch (Exception ex) {
                        try { finalTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("assets/Loloy's talking.png")); }
                        catch (Exception ex2) {
                            try { finalTalkingTex = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("Loloy's talking.png")); }
                            catch (Exception ex3) { finalTalkingTex = null; }
                        }
                    }
                }
            }
            if (finalTalkingTex != null) {
                batch.draw(finalTalkingTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }

            // Draw a subtle shredder highlight so the shredder location remains visible
            try {
                if (shredder != null) {
                    if (pixelTex == null) {
                        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                        pm.setColor(1f, 1f, 1f, 1f);
                        pm.fill();
                        pixelTex = new com.badlogic.gdx.graphics.Texture(pm);
                        pm.dispose();
                    }
                    // outline thickness and color
                    float t = 3f;
                    float sx = shredder.x;
                    float sy = shredder.y;
                    float sw = shredder.width;
                    float sh = shredder.height;
                    // Draw a large filled marker at the shredder center (bright yellow) for higher visibility
                    float markerSize = Math.max(48f, Math.min(160f, Math.max(sw, sh) * 1.5f));
                    float mx = sx + (sw - markerSize) * 0.5f;
                    float my = sy + (sh - markerSize) * 0.5f;
                    batch.setColor(1f, 1f, 0f, 0.98f);
                    batch.draw(pixelTex, mx, my, markerSize, markerSize);
                    // crosshair lines through the center
                    float cx = sx + sw * 0.5f;
                    float cy = sy + sh * 0.5f;
                    float lineW = 4f;
                    batch.setColor(1f, 0f, 0f, 0.95f);
                    // horizontal
                    batch.draw(pixelTex, sx - markerSize * 0.2f, cy - lineW * 0.5f, sw + markerSize * 0.4f, lineW);
                    // vertical
                    batch.draw(pixelTex, cx - lineW * 0.5f, sy - markerSize * 0.2f, lineW, sh + markerSize * 0.4f);
                    
                    Gdx.app.log("LevelTutorial", "Drawing shredder marker at " + sx + "," + sy + " size " + sw + "x" + sh + " markerSize=" + markerSize);
                    // Draw a contrasting red outline around the shredder rect
                    batch.setColor(1f, 0.2f, 0.2f, 0.95f);
                    // top
                    batch.draw(pixelTex, sx - t, sy + sh, sw + 2f * t, t);
                    // bottom
                    batch.draw(pixelTex, sx - t, sy - t, sw + 2f * t, t);
                    // left
                    batch.draw(pixelTex, sx - t, sy - t, t, sh + 2f * t);
                    // right
                    batch.draw(pixelTex, sx + sw, sy - t, t, sh + 2f * t);
                    // Draw a small label above the shredder
                    try {
                        if (finalOverlayFont != null) {
                            String label = "SHREDDER";
                            com.badlogic.gdx.graphics.g2d.GlyphLayout lbl = new com.badlogic.gdx.graphics.g2d.GlyphLayout(finalOverlayFont, label);
                            float lx = sx + (sw - lbl.width) * 0.5f;
                            float ly = sy + sh + lbl.height + 6f;
                            finalOverlayFont.setColor(1f, 1f, 1f, 0.95f);
                            finalOverlayFont.draw(batch, lbl, lx, ly);
                        }
                    } catch (Exception ignored) {}
                    // restore batch tint
                    batch.setColor(1f, 1f, 1f, 1f);
                }
            } catch (Exception ignored) {}

            // Draw the overlay text above the image (centered near the top) with typing transition
            String fullFinalText = (finalOverlayStage == 0) ? FINAL_OVERLAY_FIRST_TEXT : FINAL_OVERLAY_SECOND_TEXT;
            try {
                if (finalOverlayFont == null) {
                    // Try to load smallwhite font (match GameScreen behavior), fallback to default BitmapFont
                    try {
                        finalOverlayFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(
                                Gdx.files.internal("assets/smallwhite/Small_white.fnt"),
                                Gdx.files.internal("assets/smallwhite/Small_white.png"),
                                false);
                    } catch (Exception e) {
                        try {
                            finalOverlayFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(
                                    Gdx.files.internal("smallwhite/Small_white.fnt"),
                                    Gdx.files.internal("smallwhite/Small_white.png"),
                                    false);
                        } catch (Exception ex) {
                            finalOverlayFont = new com.badlogic.gdx.graphics.g2d.BitmapFont();
                        }
                    }
                }

                if (finalOverlayFont != null) {
                    float screenW = Gdx.graphics.getWidth();
                    float screenH = Gdx.graphics.getHeight();
                    float margin = 90f;

                    // Reset typing when overlay first appears
                    if (!finalTypingPreviouslyVisible) {
                        finalTypingElapsed = 0f;
                        finalTypingPreviouslyVisible = true;
                    }
                    finalTypingElapsed += Gdx.graphics.getDeltaTime();
                    float duration = Math.max(0.001f, FINAL_TEXT_TYPING_DURATION);
                    float frac = Math.min(1f, finalTypingElapsed / duration);
                    int chars = Math.max(0, Math.min(fullFinalText.length(), (int) (fullFinalText.length() * frac)));
                    String visibleText = fullFinalText.substring(0, chars);

                    com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
                    // Wrap to a reasonable width so text stays readable. Use a fixed wrap width
                    // so the text box position remains constant while characters are revealed.
                    float wrapWidth = Math.min(screenW - margin * 2f, 820f);
                    // Left-align the final overlay text and anchor the text box at the left margin
                    gl.setText(finalOverlayFont, visibleText, com.badlogic.gdx.graphics.Color.WHITE, wrapWidth, com.badlogic.gdx.utils.Align.left, true);
                    float textX = margin; // place the left edge at the margin
                    // Place the text near the top (leave margin)
                    float textTopY = screenH - margin;
                    finalOverlayFont.draw(batch, gl, textX, textTopY);

                    // Compute a fixed text box height (so the Continue button also stays put)
                    float textBoxH = Math.max(screenH * 0.18f, 80f);

                    // Draw Continue button below the (fixed) text box
                    String btnText = "Continue";
                    com.badlogic.gdx.graphics.g2d.BitmapFont btnFont = finalOverlayFont;
                    com.badlogic.gdx.graphics.g2d.GlyphLayout glBtn = new com.badlogic.gdx.graphics.g2d.GlyphLayout(btnFont, btnText);
                    float btnX = (screenW - glBtn.width) * 0.5f + FINAL_BUTTON_X_OFFSET;
                    float btnY = textTopY - textBoxH - 24f + FINAL_BUTTON_Y_OFFSET; // 24px gap below fixed text box
                    btnFont.draw(batch, glBtn, btnX, btnY);

                    // Hover detection and click handling for the Continue button
                    float mx = Gdx.input.getX();
                    float my = Gdx.graphics.getHeight() - Gdx.input.getY();
                    boolean hovered = (mx >= btnX && mx <= btnX + glBtn.width && my >= btnY - glBtn.height && my <= btnY);

                    if (hovered) {
                        try {
                            if (pixelTex == null) {
                                com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                                pm.setColor(1f, 1f, 1f, 1f);
                                pm.fill();
                                pixelTex = new com.badlogic.gdx.graphics.Texture(pm);
                                pm.dispose();
                            }
                            float thickness = 2f;
                            float underlineY = btnY - glBtn.height - 4f; // a few pixels below glyphs
                            batch.draw(pixelTex, btnX, underlineY, glBtn.width, thickness);
                        } catch (Exception ignored) {}
                    }

                    if (Gdx.input.isButtonJustPressed(com.badlogic.gdx.Input.Buttons.LEFT)) {
                        // Button bounding box: x..x+width, (btnY - glBtn.height) .. btnY
                        if (mx >= btnX && mx <= btnX + glBtn.width && my >= btnY - glBtn.height && my <= btnY) {
                            // Advance through final overlay stages: show second sentence, then dismiss
                            if (finalOverlayStage == 0) {
                                finalOverlayStage = 1;
                                finalTypingElapsed = 0f;
                                finalTypingPreviouslyVisible = false;
                            } else {
                                setFinalOverlayVisible(false);
                                finalTypingPreviouslyVisible = false;
                                finalTypingElapsed = 0f;
                                finalOverlayStage = 0;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // expose light area for overlay logic
    public Rectangle getPropertyFilesLight() { return propertyFilesLight; }

    // Overlay visibility
    public boolean isShowOverlay() { return showOverlay; }
    public void setShowOverlay(boolean show) { this.showOverlay = show; }

    // Talking overlay (shown when the first document is collected)
    private boolean talkingOverlayVisible = false;
    public boolean isTalkingOverlayVisible() { return talkingOverlayVisible; }
    public void setTalkingOverlayVisible(boolean v) { this.talkingOverlayVisible = v; }

    // Final overlay shown after all tutorial documents are collected
    private boolean finalOverlayVisible = false;
    private com.badlogic.gdx.graphics.Texture finalTalkingTex = null;
    private com.badlogic.gdx.graphics.g2d.BitmapFont finalOverlayFont = null;
    // 1x1 white pixel used for underlines (lazy-created)
    private com.badlogic.gdx.graphics.Texture pixelTex = null;
    // Typing transition state for the final overlay text
    private float finalTypingElapsed = 0f;
    private boolean finalTypingPreviouslyVisible = false;
    public static float FINAL_TEXT_TYPING_DURATION = 2.0f; // seconds
    // Allow small runtime offsets for the final overlay Continue button
    public static float FINAL_BUTTON_X_OFFSET = 290;
    public static float FINAL_BUTTON_Y_OFFSET = 100;
    // Final overlay stages: 0 = first sentence, 1 = follow-up instruction
    private int finalOverlayStage = 0;
    public static String FINAL_OVERLAY_FIRST_TEXT = "Good! Now look right. See the shredder? That's our holy grail.";
    public static String FINAL_OVERLAY_SECOND_TEXT = "I need you to destroy it, Fixer. Make it disappear.";
    // If true, the final overlay has been dismissed and should not re-open automatically
    private boolean finalOverlayConsumed = false;
    public boolean isFinalOverlayVisible() { return finalOverlayVisible; }
    public void setFinalOverlayVisible(boolean v) {
        this.finalOverlayVisible = v;
        if (!v) {
            // reset typing state when overlay hidden
            finalTypingPreviouslyVisible = false;
            finalTypingElapsed = 0f;
            finalOverlayStage = 0;
            // mark consumed so LevelManager won't re-open it
            finalOverlayConsumed = true;
        } else {
            // when showing, start at stage 0
            finalOverlayStage = 0;
            finalTypingPreviouslyVisible = false;
            finalTypingElapsed = 0f;
            finalOverlayConsumed = false;
        }
    }

    public boolean isFinalOverlayConsumed() { return finalOverlayConsumed; }

    // Tutorial text getters/setters so the game or designer can customize the overlay
    public String getTutorialTitle() { return tutorialTitle; }
    public void setTutorialTitle(String title) { this.tutorialTitle = title; }

    public String getTutorialDetail() { return tutorialDetail; }
    public void setTutorialDetail(String detail) { this.tutorialDetail = detail; }

    public String getTutorialHint() { return tutorialHint; }
    public void setTutorialHint(String hint) { this.tutorialHint = hint; }

    @Override
    public void dispose() {
        if (finalTalkingTex != null) {
            finalTalkingTex.dispose();
            finalTalkingTex = null;
        }
        if (finalOverlayFont != null) {
            finalOverlayFont.dispose();
            finalOverlayFont = null;
        }
        if (pixelTex != null) {
            pixelTex.dispose();
            pixelTex = null;
        }
    }
}
