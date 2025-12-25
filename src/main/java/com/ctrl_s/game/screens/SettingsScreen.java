package com.ctrl_s.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import com.badlogic.gdx.math.Rectangle;
import com.ctrl_s.game.core.GameApplication;
import com.ctrl_s.game.utils.BackgroundMusicManager;

/**
 * Settings Screen - Game settings and options with PNG graphics and mouse
 * listeners
 */
public class SettingsScreen implements Screen {

    private final GameApplication game;

    // Animated background
    private Texture[] bgTextures = new Texture[9];
    private TextureRegion[] bgRegions = new TextureRegion[9];
    private int loadedFrameCount = 0; // Track how many frames actually loaded
    private float bgAnimTime = 0f;
    private static final float BG_FRAME_DURATION = 0.3f; // 0.3 seconds per frame

    // Settings PNG graphics
    private Texture soundOnTexture, soundOffTexture;
    private Texture musicOnTexture, musicOffTexture;
    private Texture documentFormTexture;
    private Texture lowCheckTexture, lowNotCheckTexture;
    private Texture mediumCheckTexture, mediumNotCheckTexture;
    private Texture highCheckTexture, highNotCheckTexture;
    private Texture menuTexture;
    private Texture graphicsTextTexture;

    // Button rectangles for mouse detection
    private Rectangle soundRect, musicRect, menuRect;
    private Rectangle lowQualityRect, mediumQualityRect, highQualityRect;
    private boolean soundHovered = false;
    private boolean musicHovered = false;
    private boolean menuHovered = false;
    private boolean lowQualityHovered = false;
    private boolean mediumQualityHovered = false;
    private boolean highQualityHovered = false;
    // previous hover states to detect hover-enter
    private boolean prevSoundHovered = false;
    private boolean prevMusicHovered = false;
    private boolean prevMenuHovered = false;
    private boolean prevLowQualityHovered = false;
    private boolean prevMediumQualityHovered = false;
    private boolean prevHighQualityHovered = false;

    // Settings state
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private int selectedQuality = 0; // 0=Low, 1=Medium, 2=High

    // Preferences/Save
    private com.badlogic.gdx.Preferences prefs;

    private enum SettingOption {
        SOUND_EFFECTS,
        MUSIC,
        FULLSCREEN,
        BACK
    }

    private SettingOption selectedOption = SettingOption.SOUND_EFFECTS;

    // Old keyboard tracking
    private boolean fullscreen = false;

    private boolean upKeyWasPressed = false;
    private boolean downKeyWasPressed = false;

    // Track if music has been started for this screen instance
    private boolean musicStarted = false;

    public SettingsScreen(GameApplication game) {
        this.game = game;

        // Load preferences
        prefs = Gdx.app.getPreferences("GameSettings");
        soundEnabled = prefs.getBoolean("soundEnabled", true);
        musicEnabled = prefs.getBoolean("musicEnabled", true);
        selectedQuality = prefs.getInteger("selectedQuality", 0);

        // Load animated Setting Background frames (1.png to 9.png)
        bgTextures = new Texture[9];
        bgRegions = new TextureRegion[9];

        for (int i = 0; i < 9; i++) {
            try {
                String framePath = "assets/backgrounds/settings/" + (i + 1) + ".png";
                bgTextures[i] = new Texture(Gdx.files.internal(framePath));
                bgTextures[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                bgRegions[i] = new TextureRegion(bgTextures[i]);
                loadedFrameCount++;
                Gdx.app.log("[SettingsScreen]", "✓ Loaded Setting Background frame " + (i + 1));
            } catch (Exception e) {
                Gdx.app.error("[SettingsScreen]", "Error loading background frame " + (i + 1), e);
            }
        }
        Gdx.app.log("[SettingsScreen]", "Total loaded frames: " + loadedFrameCount + "/9");

        // Load Settings PNG graphics
        loadSettingTextures();

        // Initialize button rectangles
        soundRect = new Rectangle();
        musicRect = new Rectangle();
        menuRect = new Rectangle();
        lowQualityRect = new Rectangle();
        mediumQualityRect = new Rectangle();
        highQualityRect = new Rectangle();
    }

    private void loadSettingTextures() {
        try {
            // Load sound textures
            soundOnTexture = loadTextureFromFile("assets/sprites/ui/settings/soundeffect_ON.png");
            soundOffTexture = loadTextureFromFile("assets/sprites/ui/settings/soundeffect_OFF.png");

            // Music ON/OFF
            musicOnTexture = loadTextureFromFile("assets/sprites/ui/settings/Music_On.png");
            musicOffTexture = loadTextureFromFile("assets/sprites/ui/settings/Music_OFF.png");

            // DocumentForm
            documentFormTexture = loadTextureFromFile("assets/sprites/ui/settings/DocumentForm.png");

            // Checkboxes
            lowCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/Low_Check.png");
            lowNotCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/Low_not_Check.png");
            mediumCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/Medium_Check.png");
            mediumNotCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/Medium_not_Check.png");
            highCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/High_Check.png");
            highNotCheckTexture = loadTextureFromFile("assets/sprites/ui/settings/High_not_Check.png");

            // Load menu background
            menuTexture = loadTextureFromFile("assets/sprites/ui/settings/Menu.png");

            // Load text textures
            // settingsTextTexture =
            // loadTextureFromFile("assets/sprites/ui/settings/Settings_Text.png");
            graphicsTextTexture = loadTextureFromFile("assets/sprites/ui/settings/Graphics_Text.png");
        } catch (Exception e) {
            Gdx.app.error("[SettingsScreen]", "Error loading setting textures", e);
        }
    }

    private Texture loadTextureFromFile(String filename) {
        try {
            Texture tex = new Texture(Gdx.files.internal(filename));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.log("[SettingsScreen]", "✓ Loaded " + filename);
            return tex;
        } catch (Exception e) {
            Gdx.app.error("[SettingsScreen]", "Error loading " + filename, e);
            return null;
        }
    }

    @Override
    public void render(float delta) {
        // Start music on first render if not already started
        if (!musicStarted) {
            BackgroundMusicManager.getInstance().playScreenMusic();
            musicStarted = true;
        }

        handleInput();
        handleMouseInput();

        // Update background animation (continuous, modulo handles looping)
        bgAnimTime += delta;

        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw animated background first (behind UI)
        game.batch.begin();

        // Find a valid frame to display (skip null frames)
        int frameIndex = (int) (bgAnimTime / BG_FRAME_DURATION) % bgRegions.length;
        TextureRegion frameToRender = bgRegions[frameIndex];

        // If current frame is null, find the nearest valid frame
        if (frameToRender == null) {
            for (int i = 0; i < bgRegions.length; i++) {
                if (bgRegions[i] != null) {
                    frameToRender = bgRegions[i];
                    break;
                }
            }
        }

        // Draw the frame if we found a valid one
        if (frameToRender != null) {
            game.batch.draw(frameToRender, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        game.batch.end();

        drawSettings();
    }

    private void handleMouseInput() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY(); // Flip Y for LibGDX

        // Check hover on buttons
        soundHovered = soundRect.contains(mouseX, mouseY);
        musicHovered = musicRect.contains(mouseX, mouseY);
        menuHovered = menuRect.contains(mouseX, mouseY);
        lowQualityHovered = lowQualityRect.contains(mouseX, mouseY);
        mediumQualityHovered = mediumQualityRect.contains(mouseX, mouseY);
        highQualityHovered = highQualityRect.contains(mouseX, mouseY);

        // Play hover sound on enter transitions
        try {
            if (soundHovered && !prevSoundHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }
        try {
            if (musicHovered && !prevMusicHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }
        try {
            if (menuHovered && !prevMenuHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }
        try {
            if (lowQualityHovered && !prevLowQualityHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }
        try {
            if (mediumQualityHovered && !prevMediumQualityHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }
        try {
            if (highQualityHovered && !prevHighQualityHovered)
                game.getHoverSoundManager().playHover();
        } catch (Exception ignored) {
        }

        // update previous hover trackers
        prevSoundHovered = soundHovered;
        prevMusicHovered = musicHovered;
        prevMenuHovered = menuHovered;
        prevLowQualityHovered = lowQualityHovered;
        prevMediumQualityHovered = mediumQualityHovered;
        prevHighQualityHovered = highQualityHovered;

        // Handle clicks
        if (Gdx.input.isButtonJustPressed(0)) { // Left mouse button
            if (soundHovered) {
                soundEnabled = !soundEnabled;
                prefs.putBoolean("soundEnabled", soundEnabled);
                prefs.flush();
                Gdx.app.log("[Settings]", "Sound: " + (soundEnabled ? "ON" : "OFF"));
            } else if (musicHovered) {
                musicEnabled = !musicEnabled;
                prefs.putBoolean("musicEnabled", musicEnabled);
                prefs.flush();

                // Control background music based on toggle
                if (musicEnabled) {
                    BackgroundMusicManager.getInstance().playScreenMusic();
                } else {
                    BackgroundMusicManager.getInstance().stopMusic();
                }

                Gdx.app.log("[Settings]", "Music: " + (musicEnabled ? "ON" : "OFF"));
            } else if (menuHovered) {
                game.setScreen(new MainMenuScreen(game));
            } else if (lowQualityHovered) {
                selectedQuality = 0;
                prefs.putInteger("selectedQuality", selectedQuality);
                prefs.flush();
                Gdx.app.log("[Settings]", "Quality: Low");
            } else if (mediumQualityHovered) {
                selectedQuality = 1;
                prefs.putInteger("selectedQuality", selectedQuality);
                prefs.flush();
                Gdx.app.log("[Settings]", "Quality: Medium");
            } else if (highQualityHovered) {
                selectedQuality = 2;
                prefs.putInteger("selectedQuality", selectedQuality);
                prefs.flush();
                Gdx.app.log("[Settings]", "Quality: High");
            }
        }
    }

    private void handleInput() {
        // Navigation with UP/DOWN arrows or W/S
        boolean upKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.UP) ||
                Gdx.input.isKeyPressed(Input.Keys.W);
        boolean downKeyIsPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                Gdx.input.isKeyPressed(Input.Keys.S);

        // Move up in menu
        if (upKeyIsPressed && !upKeyWasPressed) {
            switch (selectedOption) {
                case SOUND_EFFECTS:
                    // Already at top
                    break;
                case MUSIC:
                    selectedOption = SettingOption.SOUND_EFFECTS;
                    break;
                case FULLSCREEN:
                    selectedOption = SettingOption.MUSIC;
                    break;
                case BACK:
                    selectedOption = SettingOption.FULLSCREEN;
                    break;
            }
        }

        // Move down in menu
        if (downKeyIsPressed && !downKeyWasPressed) {
            switch (selectedOption) {
                case SOUND_EFFECTS:
                    selectedOption = SettingOption.MUSIC;
                    break;
                case MUSIC:
                    selectedOption = SettingOption.FULLSCREEN;
                    break;
                case FULLSCREEN:
                    selectedOption = SettingOption.BACK;
                    break;
                case BACK:
                    // Already at bottom
                    break;
            }
        }

        upKeyWasPressed = upKeyIsPressed;
        downKeyWasPressed = downKeyIsPressed;

        // Toggle setting or select option with ENTER or SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            toggleCurrentOption();
        }

        // Quick back with ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.setScreen(new MainMenuScreen(game));
        }
    }

    private void toggleCurrentOption() {
        switch (selectedOption) {
            case SOUND_EFFECTS:
                soundEnabled = !soundEnabled;
                Gdx.app.log("Settings", "Sound Effects: " + (soundEnabled ? "ON" : "OFF"));
                break;
            case MUSIC:
                musicEnabled = !musicEnabled;
                Gdx.app.log("Settings", "Music: " + (musicEnabled ? "ON" : "OFF"));
                break;
            case FULLSCREEN:
                fullscreen = !fullscreen;
                if (fullscreen) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    Gdx.graphics.setWindowedMode(800, 600);
                }
                Gdx.app.log("Settings", "Fullscreen: " + (fullscreen ? "ON" : "OFF"));
                break;
            case BACK:
                game.setScreen(new MainMenuScreen(game));
                break;
        }
    }

    private void drawSettings() {
        float screenCenterX = Gdx.graphics.getWidth() / 2f;
        float screenCenterY = Gdx.graphics.getHeight() / 2f;

        game.batch.begin();

        // Draw DocumentForm background (the board) - LARGER SIZE
        float boardWidth = 850f;
        float boardHeight = 750f;
        float boardX = screenCenterX - boardWidth / 2f;
        float boardY = screenCenterY - boardHeight / 2f;

        if (documentFormTexture != null) {
            game.batch.setColor(1f, 1f, 1f, 1f); // White (neutral, no tinting for transparent PNG)
            game.batch.draw(documentFormTexture, boardX, boardY, boardWidth, boardHeight);
        }

        // Calculate board center (center of the DocumentForm)
        float boardCenterX = boardX + boardWidth / 2f;
        float boardCenterY = boardY + boardHeight / 2f;

        // ===== TOP SECTION: Sound, Music, Menu buttons =====
        float buttonSize = 240f;
        float topRowY = boardCenterY + 05f; // Position near top of board
        float topRowSpacing = 110f; // Distance between buttons

        // Draw Sound button (slightly larger to match visual size)
        float soundSize = 260f;
        float soundX = boardCenterX - topRowSpacing - soundSize / 2f;
        float soundY = topRowY - 10f; // Slightly lower
        soundRect.set(soundX, soundY, soundSize, soundSize);

        Texture soundTex = soundEnabled ? soundOnTexture : soundOffTexture;
        if (soundTex != null) {
            // For transparent PNGs, just use white (1,1,1,1) to show the image as-is
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(soundTex, soundX, soundY, soundSize, soundSize);
        }

        // Draw Music button
        float musicX = boardCenterX - buttonSize / 2f;
        musicRect.set(musicX, topRowY, buttonSize, buttonSize);

        Texture musicTex = musicEnabled ? musicOnTexture : musicOffTexture;
        if (musicTex != null) {
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(musicTex, musicX, topRowY, buttonSize, buttonSize);
        }

        // Draw Menu button
        float menuX = boardCenterX + topRowSpacing - buttonSize / 2f;
        menuRect.set(menuX, topRowY, buttonSize, buttonSize);

        if (menuTexture != null) {
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(menuTexture, menuX, topRowY, buttonSize, buttonSize);
        }

        // ===== BOTTOM SECTION: Quality Level buttons (inside board) =====
        float qualityRowY = boardCenterY - 200f; // Moved down lower
        float qualitySpacing = 90f; // Distance between buttons
        float qualitySize = 100f;

        // Low Quality
        float lowX = boardCenterX - qualitySpacing - qualitySize / 2f;
        lowQualityRect.set(lowX, qualityRowY, qualitySize, qualitySize);

        Texture lowTex = (selectedQuality == 0) ? lowCheckTexture : lowNotCheckTexture;
        if (lowTex != null) {
            // Transparent PNGs - just draw with white color to show natural colors
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(lowTex, lowX, qualityRowY, qualitySize, qualitySize);
        }

        // Medium Quality
        float mediumX = boardCenterX - qualitySize / 2f;
        mediumQualityRect.set(mediumX, qualityRowY, qualitySize, qualitySize);

        Texture mediumTex = (selectedQuality == 1) ? mediumCheckTexture : mediumNotCheckTexture;
        if (mediumTex != null) {
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(mediumTex, mediumX, qualityRowY, qualitySize, qualitySize);
        }

        // High Quality
        float highX = boardCenterX + qualitySpacing - qualitySize / 2f;
        highQualityRect.set(highX, qualityRowY, qualitySize, qualitySize);

        Texture highTex = (selectedQuality == 2) ? highCheckTexture : highNotCheckTexture;
        if (highTex != null) {
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(highTex, highX, qualityRowY, qualitySize, qualitySize);
        }

        // ===== TEXT LABELS =====
        // Draw Settings_Text - above top buttons
        // if (settingsTextTexture != null) {
        // float textWidth = 280f;
        // float textHeight = 120f;
        // float settingsTextX = boardCenterX - textWidth / 2f;
        // float settingsTextY = boardCenterY + 180f; // Above top row buttons
        // game.batch.setColor(1f, 1f, 1f, 1f);
        // game.batch.draw(settingsTextTexture, settingsTextX, settingsTextY, textWidth,
        // textHeight);
        // }

        // Draw Graphics_Text - between top and bottom sections
        if (graphicsTextTexture != null) {
            float textWidth = 500f;
            float textHeight = 410f;
            float graphicsTextX = boardCenterX - textWidth / 2f;
            float graphicsTextY = boardCenterY - 230f; // Moved down
            game.batch.setColor(1f, 1f, 1f, 1f);
            game.batch.draw(graphicsTextTexture, graphicsTextX, graphicsTextY, textWidth, textHeight);
        }

        game.batch.end();
    }

    @Override
    public void show() {
        Gdx.app.log("SettingsScreen", "Settings displayed");
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
        // Dispose background textures
        for (int i = 0; i < bgTextures.length; i++) {
            if (bgTextures[i] != null) {
                bgTextures[i].dispose();
            }
        }
        // Dispose settings textures
        if (soundOnTexture != null)
            soundOnTexture.dispose();
        if (soundOffTexture != null)
            soundOffTexture.dispose();
        if (musicOnTexture != null)
            musicOnTexture.dispose();
        if (musicOffTexture != null)
            musicOffTexture.dispose();
        if (documentFormTexture != null)
            documentFormTexture.dispose();
        if (lowCheckTexture != null)
            lowCheckTexture.dispose();
        if (lowNotCheckTexture != null)
            lowNotCheckTexture.dispose();
        if (mediumCheckTexture != null)
            mediumCheckTexture.dispose();
        if (mediumNotCheckTexture != null)
            mediumNotCheckTexture.dispose();
        if (highCheckTexture != null)
            highCheckTexture.dispose();
        if (highNotCheckTexture != null)
            highNotCheckTexture.dispose();
        if (menuTexture != null)
            menuTexture.dispose();
        // if (settingsTextTexture != null)
        // settingsTextTexture.dispose();
        if (graphicsTextTexture != null)
            graphicsTextTexture.dispose();
    }
}
