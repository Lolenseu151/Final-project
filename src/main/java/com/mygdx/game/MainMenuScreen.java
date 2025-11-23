package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

/**
 * Main Menu Screen - Shows title and menu options
 */
public class MainMenuScreen implements Screen {
    
    private final MyGdxGame game;
    private final ShapeRenderer shapeRenderer;
        private BitmapFont titleFont;
        // Fixed title font size for pixel look
        private String foundTtfPath = null;
        private final int titleFontSize = 64;
    
    private enum MenuOption {
        START_GAME,
        TUTORIAL,
        SETTINGS
    }
    
    private MenuOption selectedOption = MenuOption.START_GAME;
    private boolean upKeyWasPressed = false;
    private boolean downKeyWasPressed = false;
    
    public MainMenuScreen(MyGdxGame game) {
        this.game = game;
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void show() {
        // Prefer a provided TTF (pixel font) and generate a BitmapFont at runtime using FreeType
        // Build a list of candidate TTF paths to try (variable font, explicit regular, and any in static/)
        List<String> candidates = new ArrayList<>();
        candidates.add("fonts/Pixelify_Sans/PixelifySans-VariableFont_wght.ttf");
        candidates.add("fonts/Pixelify_Sans/static/PixelifySans-Regular.ttf");

        // Add any .ttf files found under fonts/Pixelify_Sans/stati c
        FileHandle staticDir = Gdx.files.internal("fonts/Pixelify_Sans/static");
        if (staticDir.exists() && staticDir.isDirectory()) {
            for (FileHandle fh : staticDir.list()) {
                if (fh.extension() != null && fh.extension().equalsIgnoreCase("ttf")) {
                    String p = fh.path();
                    if (!candidates.contains(p)) candidates.add(p);
                }
            }
        }

        boolean generated = false;
        for (String ttfPath : candidates) {
            Gdx.app.log("MainMenuScreen", "Checking for TTF at: " + ttfPath);
            if (Gdx.files.internal(ttfPath).exists()) {
                Gdx.app.log("MainMenuScreen", "Found TTF, attempting FreeType generation: " + ttfPath);
                try {
                    // record the found path and generate using current size
                    foundTtfPath = ttfPath;
                    generateTitleFontWithSize(titleFontSize);
                    generated = true;
                    break;
                } catch (Exception e) {
                    Gdx.app.log("MainMenuScreen", "Failed to generate Pixelify font from TTF (" + ttfPath + "), falling back: " + e.getMessage());
                    titleFont = null;
                    foundTtfPath = null;
                }
            }
        }

        if (!generated && Gdx.files.internal("fonts/PixelifySans.fnt").exists()) {
            Gdx.app.log("MainMenuScreen", "Found .fnt font, loading BitmapFont");
            try {
                titleFont = new BitmapFont(Gdx.files.internal("fonts/PixelifySans.fnt"));
                // Ensure the font texture uses nearest filtering for a pixelated look
                if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                    titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                }
                Gdx.app.log("MainMenuScreen", "Loaded Pixelify .fnt successfully");
                generated = true;
            } catch (Exception e) {
                Gdx.app.log("MainMenuScreen", "Failed to load PixelifySans font, falling back: " + e.getMessage());
                titleFont = null;
            }
        }

        if (!generated) {
            Gdx.app.log("MainMenuScreen", "No Pixelify font assets found via internal paths; trying absolute asset paths...");

            // Try absolute paths (useful when running from Gradle where internal lookup may not find assets)
            String userDir = System.getProperty("user.dir");
            for (String ttfPath : candidates) {
                String absPath = userDir + "/assets/" + ttfPath;
                Gdx.app.log("MainMenuScreen", "Checking absolute path: " + absPath);
                if (Gdx.files.absolute(absPath).exists()) {
                    Gdx.app.log("MainMenuScreen", "Found TTF at absolute path, generating: " + absPath);
                    try {
                        foundTtfPath = absPath;
                        generateTitleFontWithSize(titleFontSize);
                        generated = true;
                        break;
                    } catch (Exception e) {
                        Gdx.app.log("MainMenuScreen", "Failed to generate font from absolute path " + absPath + ": " + e.getMessage());
                        titleFont = null;
                        foundTtfPath = null;
                    }
                }
            }

            if (!generated) {
                Gdx.app.log("MainMenuScreen", "No Pixelify font assets found; using default font with nearest filtering");
                titleFont = null;
            }
        }

        // If still not generated, try loading any .fnt present in assets/fonts (handles unexpected filenames)
        if (!generated) {
            FileHandle fontsRoot = Gdx.files.internal("fonts");
            if (fontsRoot.exists() && fontsRoot.isDirectory()) {
                for (FileHandle fh : fontsRoot.list()) {
                    if (fh.extension() != null && fh.extension().equalsIgnoreCase("fnt")) {
                        try {
                            titleFont = new BitmapFont(fh);
                            if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                                titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                            }
                            Gdx.app.log("MainMenuScreen", "Loaded .fnt font: " + fh.path());
                            generated = true;
                            break;
                        } catch (Exception e) {
                            Gdx.app.log("MainMenuScreen", "Failed to load .fnt at " + fh.path() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        // Final explicit attempt: try loading the specific Pexelify_Sans.fnt via internal and absolute paths
        if (!generated) {
            String explicitInternal = "fonts/Pexelify_Sans.fnt";
            String userDir = System.getProperty("user.dir");
            String explicitAbsolute = userDir + "/assets/fonts/Pexelify_Sans.fnt";
            Gdx.app.log("MainMenuScreen", "Attempting explicit loads: internal(" + explicitInternal + ") and absolute(" + explicitAbsolute + ")");
            try {
                if (Gdx.files.internal(explicitInternal).exists()) {
                    titleFont = new BitmapFont(Gdx.files.internal(explicitInternal));
                    if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                        titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                    }
                    Gdx.app.log("MainMenuScreen", "Loaded internal .fnt: " + explicitInternal);
                    generated = true;
                } else if (Gdx.files.absolute(explicitAbsolute).exists()) {
                    titleFont = new BitmapFont(Gdx.files.absolute(explicitAbsolute));
                    if (titleFont.getRegion() != null && titleFont.getRegion().getTexture() != null) {
                        titleFont.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
                    }
                    Gdx.app.log("MainMenuScreen", "Loaded absolute .fnt: " + explicitAbsolute);
                    generated = true;
                } else {
                    Gdx.app.log("MainMenuScreen", "Explicit .fnt not found in internal or absolute paths");
                }
            } catch (Exception e) {
                Gdx.app.error("MainMenuScreen", "Exception loading explicit .fnt", e);
                titleFont = null;
            }
        }
        // If no pixel font was created, force the default font to nearest filtering so scaled text appears pixelated
        if (titleFont == null && game.font != null && game.font.getRegion() != null && game.font.getRegion().getTexture() != null) {
            game.font.getRegion().getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        }
        Gdx.app.log("MainMenuScreen", "Main menu displayed");
    }

    private void generateTitleFontWithSize(int size) {
        // Dispose previous if present
        if (titleFont != null) {
            try { titleFont.dispose(); } catch (Exception ignored) {}
            titleFont = null;
        }
        if (foundTtfPath == null) return;
        FileHandle ttfHandle = null;
        try {
            // Prefer internal lookup, but if it's an absolute path use absolute file handle
            if (Gdx.files.internal(foundTtfPath).exists()) {
                ttfHandle = Gdx.files.internal(foundTtfPath);
            } else if (Gdx.files.absolute(foundTtfPath).exists()) {
                ttfHandle = Gdx.files.absolute(foundTtfPath);
            } else {
                // last-ditch: try user.dir + /assets/
                String userDir = System.getProperty("user.dir");
                String alt = userDir + "/assets/" + foundTtfPath;
                if (Gdx.files.absolute(alt).exists()) ttfHandle = Gdx.files.absolute(alt);
            }
            if (ttfHandle == null) {
                Gdx.app.error("MainMenuScreen", "TTF handle not found for path: " + foundTtfPath);
                return;
            }
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(ttfHandle);
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = size;
            parameter.magFilter = TextureFilter.Nearest;
            parameter.minFilter = TextureFilter.Nearest;
            titleFont = generator.generateFont(parameter);
            generator.dispose();
            Gdx.app.log("MainMenuScreen", "Generated BitmapFont from TTF (size=" + parameter.size + ") using " + foundTtfPath);
        } catch (Exception e) {
            Gdx.app.error("MainMenuScreen", "Exception while generating font from TTF: " + foundTtfPath, e);
            titleFont = null;
        }
    }
    
    @Override
    public void render(float delta) {
        handleInput();
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        drawMenu();
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
                case START_GAME:
                    // Already at top
                    break;
                case TUTORIAL:
                    selectedOption = MenuOption.START_GAME;
                    break;
                case SETTINGS:
                    selectedOption = MenuOption.TUTORIAL;
                    break;
            }
        }
        
        // Move down in menu
        if (downKeyIsPressed && !downKeyWasPressed) {
            switch (selectedOption) {
                case START_GAME:
                    selectedOption = MenuOption.TUTORIAL;
                    break;
                case TUTORIAL:
                    selectedOption = MenuOption.SETTINGS;
                    break;
                case SETTINGS:
                    // Already at bottom
                    break;
            }
        }
        
        upKeyWasPressed = upKeyIsPressed;
        downKeyWasPressed = downKeyIsPressed;
        
        // Select option with ENTER or SPACE
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || 
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            selectCurrentOption();
        }
        // Font size is fixed; size-cycling removed
    }
    
    private void selectCurrentOption() {
        switch (selectedOption) {
            case START_GAME:
                Gdx.app.log("MainMenu", "Starting game...");
                game.setScreen(new GameScreen(game));
                break;
            case TUTORIAL:
                Gdx.app.log("MainMenu", "Opening tutorial...");
                game.setScreen(new TutorialScreen(game));
                break;
            case SETTINGS:
                Gdx.app.log("MainMenu", "Opening settings...");
                game.setScreen(new SettingsScreen(game));
                break;
        }
    }
    
    private void drawMenu() {
        float centerX = Gdx.graphics.getWidth() / 2;
        float centerY = Gdx.graphics.getHeight() / 2;
        
        // Draw menu background panels
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Title background removed — text will be drawn without a panel
        
        // Menu options backgrounds
        drawMenuOptionBox(centerX, centerY + 20, MenuOption.START_GAME);
        drawMenuOptionBox(centerX, centerY - 40, MenuOption.TUTORIAL);
        drawMenuOptionBox(centerX, centerY - 100, MenuOption.SETTINGS);
        
        shapeRenderer.end();
        
        // NOTE: Title border removed as requested; only background panel remains.
        
        // Draw text
        game.batch.begin();
        
        // Title (use Pixelify Sans if available, scale up for title size)
        String titleText = "Paper Trail Panic";
        if (titleFont != null) {
            float prevScaleX = titleFont.getData().scaleX;
            float prevScaleY = titleFont.getData().scaleY;
            titleFont.getData().setScale(2.0f);
            GlyphLayout layout = new GlyphLayout(titleFont, titleText);
            float titleX = centerX - (layout.width / 2f);
            float titleY = centerY + 150;
            titleFont.draw(game.batch, titleText, titleX, titleY);
            titleFont.getData().setScale(prevScaleX, prevScaleY);
        } else {
            float prevX = game.font.getData().scaleX;
            float prevY = game.font.getData().scaleY;
            game.font.getData().setScale(2.0f);
            GlyphLayout layout = new GlyphLayout(game.font, titleText);
            float titleX = centerX - (layout.width / 2f);
            float titleY = centerY + 150;
            game.font.draw(game.batch, titleText, titleX, titleY);
            game.font.getData().setScale(prevX, prevY);
        }
        
        
        // Menu options
        drawMenuOptionText("START GAME", centerX - 50, centerY + 30, MenuOption.START_GAME);
        drawMenuOptionText("TUTORIAL", centerX - 40, centerY - 30, MenuOption.TUTORIAL);
        drawMenuOptionText("SETTINGS", centerX - 40, centerY - 90, MenuOption.SETTINGS);
        
        // Instructions
        game.font.draw(game.batch, "UP/DOWN or W/S: Navigate | ENTER/SPACE: Select", 
            centerX - 180, 40);

        // On-screen debug: show which font source is active
        // Extra debug: whether titleFont was created and its texture filter
        // debug removed
        
        game.batch.end();
    }
    
    private void drawMenuOptionBox(float centerX, float y, MenuOption option) {
        boolean isSelected = (selectedOption == option);
        
        if (isSelected) {
            shapeRenderer.setColor(0, 0.6f, 0.6f, 0.9f); // Highlighted cyan
        } else {
            shapeRenderer.setColor(0.25f, 0.25f, 0.35f, 0.7f); // Dark grey
        }
        
        shapeRenderer.rect(centerX - 150, y - 20, 300, 40);
    }
    
    private void drawMenuOptionText(String text, float x, float y, MenuOption option) {
        boolean isSelected = (selectedOption == option);
        
        // Indicate selection by color only (no arrow)
        Color prev = game.font.getColor().cpy();
        if (isSelected) {
            game.font.setColor(Color.BLACK);
        } else {
            game.font.setColor(Color.WHITE);
        }
        game.font.draw(game.batch, text, x, y);
        game.font.setColor(prev);
    }
    
    
    
    @Override
    public void resize(int width, int height) {
        // Update SpriteBatch and ShapeRenderer projection so UI scales with window
        Matrix4 proj = new Matrix4().setToOrtho2D(0, 0, width, height);
        game.batch.setProjectionMatrix(proj);
        shapeRenderer.setProjectionMatrix(proj);
    }
    
    @Override
    public void pause() {}
    
    @Override
    public void resume() {}
    
    @Override
    public void hide() {}
    
    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
    }
}
