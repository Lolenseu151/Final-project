package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Level Select Screen using TextureAtlas
 * 
 * This screen uses a single TextureAtlas (ui1.atlas) containing all Level Select UI sprites:
 * - background_full (1280x800) - full-screen background image
 * - title_selectlevel - pixelated title text at top center
 * - arrow_back - back button sprite with hover support
 * - btn_level1 through btn_level5 - normal button sprites for each level
 * - btn_level1_hover through btn_level5_hover - hover state sprites for each level
 * 
 * The atlas approach provides:
 * 1. Efficient sprite packing (single texture, multiple regions)
 * 2. Per-button hover states using ImageButton's up/over drawables
 * 3. Pixel-perfect hitboxes aligned with visual sprites via Scene2D
 * 
 * Scene2D Layout Benefits:
 * - ImageButton automatically handles hover detection using its bounds
 * - No manual mouse coordinate tracking needed
 * - Hitboxes perfectly match visual button positions (no misalignment)
 * - Each button independently highlights on hover (only hovered button changes)
 */
public class LevelSelectScreen implements Screen {
    private final MyGdxGame game;
    private final Stage stage;
    private int selectedLevel = 1;
    
    // TextureAtlas containing all UI sprites
    private TextureAtlas uiAtlas;
    private BitmapFont buttonFont;  // Font for "Level 1", "Level 2", etc.

    public LevelSelectScreen(MyGdxGame game) {
        this.game = game;
        
        // Create Stage with ScreenViewport (matches screen size 1280x800)
        this.stage = new Stage(new ScreenViewport());
        
        // Load TextureAtlas from assets/ui select level/ui1.atlas
        // Atlas regions used: background_full, title_selectlevel, arrow_back,
        // btn_level1-5, btn_level1_hover-5_hover
        try {
            com.badlogic.gdx.files.FileHandle atlasHandle = null;
            String atlasPath = "ui select level/ui1.atlas";
            
            // Try internal path first (works when bundled)
            if (Gdx.files.internal(atlasPath).exists()) {
                atlasHandle = Gdx.files.internal(atlasPath);
                Gdx.app.log("[LevelSelectScreen]", "Loading atlas from internal: " + atlasPath);
            } else {
                // Fallback to absolute path (development mode)
                String userDir = System.getProperty("user.dir");
                String absPath = userDir + "/assets/" + atlasPath;
                if (Gdx.files.absolute(absPath).exists()) {
                    atlasHandle = Gdx.files.absolute(absPath);
                    Gdx.app.log("[LevelSelectScreen]", "Loading atlas from absolute: " + absPath);
                }
            }
            
            if (atlasHandle != null) {
                uiAtlas = new TextureAtlas(atlasHandle);
                Gdx.app.log("[LevelSelectScreen]", "✓ TextureAtlas loaded successfully");
            } else {
                throw new RuntimeException("ui1.atlas not found in assets/ui select level/");
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "✗ Failed to load ui1.atlas: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot load ui1.atlas", e);
        }
        
        // Create font for button labels (Level 1, Level 2, etc.)
        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.5f);  // Larger, readable text
        buttonFont.setColor(Color.WHITE);
        
        // Build UI using atlas regions
        initializeUI();
        
        // Set input processor to stage for button clicks
        Gdx.input.setInputProcessor(stage);
    }

    /**
     * Initialize UI using TextureAtlas regions
     * 
     * Layout order (back-to-front Z-order):
     * 1. level_bg (1280x800 stretched to fill screen)
     * 2. title_selectlevel (centered at top)
     * 3. arrow_back (top-left corner with hover)
     * 4. Five level buttons (level_btn_orig with level_btn_orig_hover on mouseover)
     * 5. Text labels ("Level 1" through "Level 5") centered on each button
     * 
     * Each ImageButton uses:
     * - up drawable = normal button sprite (level_btn_orig)
     * - over drawable = hover sprite (level_btn_orig_hover)
     * - down drawable = hover sprite (level_btn_orig_hover)
     * 
     * This ensures ONLY the hovered button highlights, while others remain normal.
     */
    private void initializeUI() {
        // 1. Add level_bg (1280x800) as background
        TextureRegion bgRegion = uiAtlas.findRegion("level_bg");
        if (bgRegion != null) {
            Image background = new Image(bgRegion);
            background.setSize(1280, 800);  // Stretch to full screen
            background.setPosition(0, 0);
            stage.addActor(background);
            Gdx.app.log("[LevelSelectScreen]", "✓ Background (level_bg) added at 1280x800");
        } else {
            Gdx.app.error("[LevelSelectScreen]", "✗ level_bg not found in atlas");
        }
        
        // 2. Add title_selectlevel centered at top
        TextureRegion titleRegion = uiAtlas.findRegion("title_selectlevel");
        if (titleRegion != null) {
            Image title = new Image(titleRegion);
            float titleWidth = titleRegion.getRegionWidth();
            float titleHeight = titleRegion.getRegionHeight();
            title.setSize(titleWidth, titleHeight);
            // Center horizontally, position near top (y=720)
            title.setPosition((1280 - titleWidth) / 2, 720);
            stage.addActor(title);
            Gdx.app.log("[LevelSelectScreen]", "✓ Title (title_selectlevel) added");
        } else {
            Gdx.app.error("[LevelSelectScreen]", "✗ title_selectlevel not found in atlas");
        }
        
        // 3. Add arrow_back button at top-left with hover support
        TextureRegion arrowRegion = uiAtlas.findRegion("arrow_back");
        if (arrowRegion != null) {
            ImageButton backButton = createBackButton(arrowRegion);
            stage.addActor(backButton);
            Gdx.app.log("[LevelSelectScreen]", "✓ Back arrow (arrow_back) added");
        } else {
            Gdx.app.error("[LevelSelectScreen]", "✗ arrow_back not found in atlas");
        }
        
        // 4. Create 5 level buttons using level_btn_orig sprites
        // Each button uses the same sprite but with individual text labels
        for (int i = 1; i <= 5; i++) {
            createLevelButton(i);
        }
    }
    
    /**
     * Create back button using arrow_back sprite
     * 
     * Positioned at top-left (20, 720) with 60x60 size.
     * Clicking returns to MainMenuScreen.
     */
    private ImageButton createBackButton(TextureRegion arrowRegion) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(arrowRegion);
        ImageButton backButton = new ImageButton(drawable);
        backButton.setSize(60, 60);
        backButton.setPosition(20, 720);  // Top-left corner
        
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("[LevelSelectScreen]", "✓ Back button clicked");
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        
        return backButton;
    }
    
    /**
     * Create a level button using sprites from atlas
     * 
     * All levels use the same button sprites:
     * - Normal state: level_btn_orig
     * - Hover state: level_btn_orig_hover
     * 
     * Text labels ("Level 1" through "Level 5") are added on top of buttons
     * to differentiate them visually.
     * 
     * This approach ensures:
     * 1. Consistent button appearance across all levels
     * 2. Only the hovered button highlights (ImageButton handles state internally)
     * 3. Hitbox matches visual sprite bounds exactly (Scene2D positioning)
     * 4. Clear, centered text labels for easy identification
     * 
     * Button positioning:
     * - Vertically centered with equal spacing
     * - Horizontally centered on screen
     * 
     * On click, calls goToLevel(levelNum) to navigate to GameScreen.
     * 
     * @param levelNum Level number (1-5)
     */
    private void createLevelButton(int levelNum) {
        // Load the shared button sprites (same for all levels)
        TextureRegion btnRegion = uiAtlas.findRegion("level_btn_orig");
        TextureRegion btnHoverRegion = uiAtlas.findRegion("level_btn_orig_hover");
        
        if (btnRegion == null) {
            Gdx.app.error("[LevelSelectScreen]", "✗ level_btn_orig not found in atlas");
            return;
        }
        
        // Use normal sprite as fallback if hover sprite missing
        if (btnHoverRegion == null) {
            Gdx.app.log("[LevelSelectScreen]", "⚠ level_btn_orig_hover not found, using normal sprite");
            btnHoverRegion = btnRegion;
        }
        
        // Create ImageButton with up/over/down drawables
        // up = normal state, over = mouse hover, down = clicked (also hover sprite)
        TextureRegionDrawable upDrawable = new TextureRegionDrawable(btnRegion);
        TextureRegionDrawable overDrawable = new TextureRegionDrawable(btnHoverRegion);
        
        final ImageButton levelButton = new ImageButton(upDrawable, overDrawable, overDrawable);
        
        // Get button size from sprite dimensions (preserves original artwork size)
        final float buttonWidth = btnRegion.getRegionWidth();
        final float buttonHeight = btnRegion.getRegionHeight();
        levelButton.setSize(buttonWidth, buttonHeight);
        
        // Position buttons vertically centered with equal spacing
        float spacing = 20;
        float totalHeight = (buttonHeight * 5) + (spacing * 4);
        float startY = (800 - totalHeight) / 2;
        float buttonX = (1280 - buttonWidth) / 2;  // Horizontally centered
        float buttonY = startY + (5 - levelNum) * (buttonHeight + spacing);  // Bottom-to-top ordering
        
        levelButton.setPosition(buttonX, buttonY);
        
        stage.addActor(levelButton);
        
        // Add "Level X" text label centered on button
        Label.LabelStyle labelStyle = new Label.LabelStyle(buttonFont, Color.WHITE);
        final Label levelLabel = new Label("Level " + levelNum, labelStyle);
        
        // Set font scale for readable size
        levelLabel.setFontScale(1.2f);
        
        // Calculate centered position on button
        // We need to position the label after setting scale
        float labelWidth = levelLabel.getPrefWidth() * 1.2f;  // Account for scale
        float labelHeight = levelLabel.getPrefHeight() * 1.2f;
        
        float labelX = buttonX + (buttonWidth - labelWidth) / 2;
        float labelY = buttonY + (buttonHeight - labelHeight) / 2;
        
        levelLabel.setPosition(labelX, labelY);
        levelLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);  // Let clicks pass through to button
        stage.addActor(levelLabel);
        
        // Add unified listener for both click and hover
        final int level = levelNum;
        levelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedLevel = level;
                Gdx.app.log("[LevelSelectScreen]", "✓ Clicked Level " + level);
                goToLevel(selectedLevel);  // Uses existing goToLevel method (not renamed)
            }
            
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                // Brighten text on hover (yellow highlight) - stays while mouse is over button
                levelLabel.setColor(Color.YELLOW);
            }
            
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                // Return to white when mouse leaves button
                levelLabel.setColor(Color.WHITE);
            }
        });
        
        Gdx.app.log("[LevelSelectScreen]", "✓ Level " + levelNum + " button created at (" + buttonX + ", " + buttonY + ") with centered label");
    }

    /**
     * Navigate to the selected level
     */
    private void goToLevel(int level) {
        Gdx.app.log("[LevelSelectScreen]", "Loading Level " + level);
        game.setScreen(new GameScreen(game, level));
        dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Handle keyboard navigation
        handleKeyboardInput();
        
        // Update and draw stage
        stage.act(delta);
        stage.draw();
    }

    /**
     * Handle keyboard input for level selection
     */
    private void handleKeyboardInput() {
        // Arrow keys to select level
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedLevel = Math.max(1, selectedLevel - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedLevel = Math.min(5, selectedLevel + 1);
        }
        
        // Enter to confirm selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            goToLevel(selectedLevel);
        }
        
        // Escape to go back
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        if (uiAtlas != null) {
            uiAtlas.dispose();
        }
        if (buttonFont != null) {
            buttonFont.dispose();
        }
    }
}
