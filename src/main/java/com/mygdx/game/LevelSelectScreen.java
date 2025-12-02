package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LevelSelectScreen implements Screen {
    private final MyGdxGame game;
    private final Stage stage;

    private TextureAtlas uiAtlas;
    private BitmapFont buttonFont;
    private BitmapFont levelsFont = null;

    // Animated background
    private Texture[] bgTextures = null;
    private TextureRegion[] bgRegions = null;
    private Image animatedBg = null;
    private float bgAnimTime = 0f;
    private static final float BG_FRAME_DURATION = 0.5f;

    // Optional custom textures for Level 1 button (normal and hover)
    private Texture level1ButtonTexture = null;
    private Texture level1HoverTexture = null;
    // Optional custom textures for Level 2 button (normal and hover)
    private Texture level2ButtonTexture = null;
    private Texture level2HoverTexture = null;
    // Optional custom textures for Level 3/4/5 buttons (normal and hover)
    private Texture level3ButtonTexture = null;
    private Texture level3HoverTexture = null;
    private Texture level4ButtonTexture = null;
    private Texture level4HoverTexture = null;
    private Texture level5ButtonTexture = null;
    private Texture level5HoverTexture = null;
    // Optional custom textures for Back button
    private Texture backButtonTexture = null;
    private Texture backButtonHoverTexture = null;
    // Runtime refs for hover polling
    private ImageButton level1ButtonRef = null;
    private TextureRegionDrawable level1UpDrawable = null;
    private TextureRegionDrawable level1OverDrawable = null;
    private boolean level1Hovered = false;
    private ImageButton level2ButtonRef = null;
    private TextureRegionDrawable level2UpDrawable = null;
    private TextureRegionDrawable level2OverDrawable = null;
    private boolean level2Hovered = false;
    private ImageButton level3ButtonRef = null;
    private TextureRegionDrawable level3UpDrawable = null;
    private TextureRegionDrawable level3OverDrawable = null;
    private boolean level3Hovered = false;
    private ImageButton level4ButtonRef = null;
    private TextureRegionDrawable level4UpDrawable = null;
    private TextureRegionDrawable level4OverDrawable = null;
    private boolean level4Hovered = false;
    private ImageButton level5ButtonRef = null;
    private TextureRegionDrawable level5UpDrawable = null;
    private TextureRegionDrawable level5OverDrawable = null;
    private boolean level5Hovered = false;
    // Reusable vector to avoid allocations during hover polling
    private final com.badlogic.gdx.math.Vector2 tmpStageCoords = new com.badlogic.gdx.math.Vector2();
    // Debugging: enable to draw actor bounds and show pointer coordinates
    private final boolean DEBUG_HOVER = false;
    private Label debugPointerLabel = null; // kept for optional debugging
    // Temporary: enable hover logging and subtle tint fallback when hover drawable appears identical
    private final boolean ENABLE_HOVER_DIAGNOSTICS = true;
    // Unified hover info list so polling works for all buttons (fixes touchpad hover issues)
    private static class HoverInfo {
        int id; // level id (optional)
        ImageButton button;
        TextureRegionDrawable up;
        TextureRegionDrawable over;
        boolean hovered;
        HoverInfo(int id, ImageButton b, TextureRegionDrawable u, TextureRegionDrawable o) { this.id = id; button = b; up = u; over = o; hovered = false; }
    }
    private final java.util.List<HoverInfo> hoverInfos = new java.util.ArrayList<>();

    // Layout
    private static final float BUTTON_WIDTH = 220f;
    private static final float BUTTON_HEIGHT = 128f;
    private static final float HOVER_SCALE = 1.06f;
    private static final float BUTTON_SPACING = 12f;
    private static final float TITLE_TOP_MARGIN = 30f;
    private static final float LABEL_FONT_SCALE = 1.5f;
    private static final float BUTTON_VERTICAL_OFFSET = 70f;
    private static final float LEVELS_FONT_SCALE = 4.0f;
    private static final float LEVELS_TOP_MARGIN = 180f;
    private static final float BACK_BUTTON_SCALE = 0.09f;
    
    // Track if music has been started for this screen instance
    private boolean musicStarted = false;

    public LevelSelectScreen(MyGdxGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // Atlas loading removed - now using individual PNG files for level buttons
        // The uiAtlas field is kept for backward compatibility but not used

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.5f);
        buttonFont.setColor(Color.WHITE);

        initializeUI();

        Gdx.input.setInputProcessor(stage);
    }

    private void initializeUI() {
        float screenWidth = stage.getViewport().getWorldWidth();
        float screenHeight = stage.getViewport().getWorldHeight();

        // Try load animated frames
        String[] framePaths = new String[] {"Level BG/2.png","Level BG/3.png","Level BG/4.png","Level BG/5.png","Level BG/6.png","Level BG/7.png","Level BG/8.png","Level BG/9.png","Level BG/10.png","Level BG/11.png","Level BG/12.png","Level BG/13.png"};
        java.util.List<Texture> loaded = new java.util.ArrayList<>();
        String userDir = System.getProperty("user.dir");
        for (String p : framePaths) {
            try {
                com.badlogic.gdx.files.FileHandle fh = null;
                if (Gdx.files.internal(p).exists()) fh = Gdx.files.internal(p);
                else if (Gdx.files.absolute(userDir + "/assets/" + p).exists()) fh = Gdx.files.absolute(userDir + "/assets/" + p);
                if (fh != null) {
                    Texture t = new Texture(fh);
                    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    loaded.add(t);
                }
            } catch (Exception e) {
                Gdx.app.error("[LevelSelectScreen]", "Error loading bg frame: " + p, e);
            }
        }

        // Load custom Level 1 button images (if present) - internal path first, then project assets
        try {
            String btnBase = "Level buttons/1.png";
            String btnHover = "Level buttons/2.png";
            com.badlogic.gdx.files.FileHandle fh1 = null;
            com.badlogic.gdx.files.FileHandle fh2 = null;
            if (Gdx.files.internal(btnBase).exists()) fh1 = Gdx.files.internal(btnBase);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase).exists()) fh1 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase);
            if (Gdx.files.internal(btnHover).exists()) fh2 = Gdx.files.internal(btnHover);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover).exists()) fh2 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover);
            if (fh1 != null) {
                level1ButtonTexture = new Texture(fh1);
                level1ButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level1 button image: " + btnBase);
            }
            if (fh2 != null) {
                level1HoverTexture = new Texture(fh2);
                level1HoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level1 hover image: " + btnHover);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Level1 button images", e);
        }

        // Load custom Level 2 button images (if present)
        try {
            String btnBase2 = "Level 2/1.png";
            String btnHover2 = "Level 2/2.png";
            com.badlogic.gdx.files.FileHandle fh21 = null;
            com.badlogic.gdx.files.FileHandle fh22 = null;
            if (Gdx.files.internal(btnBase2).exists()) fh21 = Gdx.files.internal(btnBase2);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase2).exists()) fh21 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase2);
            if (Gdx.files.internal(btnHover2).exists()) fh22 = Gdx.files.internal(btnHover2);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover2).exists()) fh22 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover2);
            if (fh21 != null) {
                level2ButtonTexture = new Texture(fh21);
                level2ButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level2 button image: " + btnBase2);
            }
            if (fh22 != null) {
                level2HoverTexture = new Texture(fh22);
                level2HoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level2 hover image: " + btnHover2);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Level2 button images", e);
        }

        // Load custom Level 3 button images (if present)
        try {
            String btnBase3 = "Level 3/1.png";
            String btnHover3 = "Level 3/2.png";
            com.badlogic.gdx.files.FileHandle fh31 = null;
            com.badlogic.gdx.files.FileHandle fh32 = null;
            if (Gdx.files.internal(btnBase3).exists()) fh31 = Gdx.files.internal(btnBase3);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase3).exists()) fh31 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase3);
            if (Gdx.files.internal(btnHover3).exists()) fh32 = Gdx.files.internal(btnHover3);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover3).exists()) fh32 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover3);
            if (fh31 != null) {
                level3ButtonTexture = new Texture(fh31);
                level3ButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level3 button image: " + btnBase3);
            }
            if (fh32 != null) {
                level3HoverTexture = new Texture(fh32);
                level3HoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level3 hover image: " + btnHover3);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Level3 button images", e);
        }

        // Load custom Level 4 button images (if present)
        try {
            String btnBase4 = "level 4/1.png";
            String btnHover4 = "level 4/2.png";
            com.badlogic.gdx.files.FileHandle fh41 = null;
            com.badlogic.gdx.files.FileHandle fh42 = null;
            if (Gdx.files.internal(btnBase4).exists()) fh41 = Gdx.files.internal(btnBase4);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase4).exists()) fh41 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase4);
            if (Gdx.files.internal(btnHover4).exists()) fh42 = Gdx.files.internal(btnHover4);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover4).exists()) fh42 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover4);
            if (fh41 != null) {
                level4ButtonTexture = new Texture(fh41);
                level4ButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level4 button image: " + btnBase4);
            }
            if (fh42 != null) {
                level4HoverTexture = new Texture(fh42);
                level4HoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level4 hover image: " + btnHover4);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Level4 button images", e);
        }

        // Load custom Level 5 button images (if present)
        try {
            String btnBase5 = "level 5/1.png";
            String btnHover5 = "level 5/2.png";
            com.badlogic.gdx.files.FileHandle fh51 = null;
            com.badlogic.gdx.files.FileHandle fh52 = null;
            if (Gdx.files.internal(btnBase5).exists()) fh51 = Gdx.files.internal(btnBase5);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase5).exists()) fh51 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnBase5);
            if (Gdx.files.internal(btnHover5).exists()) fh52 = Gdx.files.internal(btnHover5);
            else if (Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover5).exists()) fh52 = Gdx.files.absolute(System.getProperty("user.dir") + "/assets/" + btnHover5);
            if (fh51 != null) {
                level5ButtonTexture = new Texture(fh51);
                level5ButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level5 button image: " + btnBase5);
            }
            if (fh52 != null) {
                level5HoverTexture = new Texture(fh52);
                level5HoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Level5 hover image: " + btnHover5);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Level5 button images", e);
        }

        // Load custom Back button images (if present)
        try {
            String backBase = "back button/7.png";
            String backHover = "back button/8.png";
            com.badlogic.gdx.files.FileHandle fhb = null;
            com.badlogic.gdx.files.FileHandle fhbh = null;
            if (Gdx.files.internal("back button/7.png").exists()) fhb = Gdx.files.internal(backBase);
            else if (Gdx.files.absolute(userDir + "/assets/" + backBase).exists()) fhb = Gdx.files.absolute(userDir + "/assets/" + backBase);
            if (Gdx.files.internal(backHover).exists()) fhbh = Gdx.files.internal(backHover);
            else if (Gdx.files.absolute(userDir + "/assets/" + backHover).exists()) fhbh = Gdx.files.absolute(userDir + "/assets/" + backHover);
            if (fhb != null) {
                backButtonTexture = new Texture(fhb);
                backButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Back button image: " + backBase);
            }
            if (fhbh != null) {
                backButtonHoverTexture = new Texture(fhbh);
                backButtonHoverTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Back button hover image: " + backHover);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Error loading Back button images", e);
        }

        if (!loaded.isEmpty()) {
            bgTextures = loaded.toArray(new Texture[0]);
            bgRegions = new TextureRegion[bgTextures.length];
            for (int i = 0; i < bgTextures.length; i++) bgRegions[i] = new TextureRegion(bgTextures[i]);
            animatedBg = new Image(bgRegions[0]);
            animatedBg.setFillParent(true);
            stage.addActor(animatedBg);
        }

        // Title - now using individual PNG approach (removed atlas fallback)
        try {
            String fontPath = "fonts/bold/Bold.fnt";
            com.badlogic.gdx.files.FileHandle fh = null;
            if (Gdx.files.internal(fontPath).exists()) fh = Gdx.files.internal(fontPath);
            else {
                String abs = userDir + "/assets/" + fontPath;
                if (Gdx.files.absolute(abs).exists()) fh = Gdx.files.absolute(abs);
            }
            if (fh != null) {
                levelsFont = new BitmapFont(fh);
                levelsFont.getData().setScale(LEVELS_FONT_SCALE);
                levelsFont.setColor(Color.WHITE);
                Gdx.app.log("[LevelSelectScreen]", "Loaded Levels bold font: " + fontPath);
            }
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Failed to load levels font", e);
        }

        // Add top-center "Levels" text if font loaded
        if (levelsFont != null) {
            Label.LabelStyle ls = new Label.LabelStyle(levelsFont, Color.WHITE);
            Label levelsLabel = new Label("Levels", ls);
            float lw = levelsLabel.getPrefWidth();
            float lh = levelsLabel.getPrefHeight();
            levelsLabel.setPosition((screenWidth - lw) / 2f, screenHeight - lh - LEVELS_TOP_MARGIN);
            levelsLabel.setAlignment(Align.center);
            levelsLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            stage.addActor(levelsLabel);
        }

        // debugPointerLabel not added in normal runs

        // Back button: using custom textures (assets/back button/7.png, 8.png)
        TextureRegion arrow = null;
        TextureRegion arrowHover = null;
        if (backButtonTexture != null) {
            arrow = new TextureRegion(backButtonTexture);
            arrowHover = (backButtonHoverTexture != null) ? new TextureRegion(backButtonHoverTexture) : arrow;
        }
        if (arrow != null) {
            ImageButton back = createBackButton(arrow, arrowHover, screenHeight);
            stage.addActor(back);
        }

        // Horizontal level buttons
        int count = 5;
        float spacing = 30f;
        float totalWidth = (BUTTON_WIDTH * count) + (spacing * (count - 1));
        float startX = (screenWidth - totalWidth) / 2f;
        float y = (screenHeight - BUTTON_HEIGHT) / 2f - BUTTON_VERTICAL_OFFSET;
        for (int i = 1; i <= count; i++) {
            float x = startX + (i - 1) * (BUTTON_WIDTH + spacing);
            createLevelButton(i, x, y);
        }
        // Enable stage debug outlines when troubleshooting hover issues
        stage.setDebugAll(DEBUG_HOVER);
    }

    private ImageButton createBackButton(TextureRegion arrowRegion, TextureRegion arrowHoverRegion, float screenHeight) {
        TextureRegionDrawable up = new TextureRegionDrawable(arrowRegion);
        TextureRegionDrawable over = (arrowHoverRegion != null) ? new TextureRegionDrawable(arrowHoverRegion) : up;
        ImageButton b = new ImageButton(up, over, over);
        float w = arrowRegion.getRegionWidth();
        float h = arrowRegion.getRegionHeight();
        float sw = w * BACK_BUTTON_SCALE;
        float sh = h * BACK_BUTTON_SCALE;
        b.setSize(sw, sh);
        b.setPosition(20, screenHeight - sh - 20);
        b.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });
        return b;
    }

    private void createLevelButton(int levelNum, float x, float y) {
        final ImageButton ib;
        // If this is Level 1 and custom textures were loaded, use them
        if (levelNum == 1 && level1ButtonTexture != null) {
            TextureRegion norm = new TextureRegion(level1ButtonTexture);
            TextureRegion hover = (level1HoverTexture != null) ? new TextureRegion(level1HoverTexture) : norm;
            TextureRegionDrawable up = new TextureRegionDrawable(norm);
            TextureRegionDrawable over = new TextureRegionDrawable(hover);
            ib = new ImageButton(up, over, over);
            // Use consistent button size and scale the drawable to fit
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        } else if (levelNum == 2 && level2ButtonTexture != null) {
            TextureRegion norm2 = new TextureRegion(level2ButtonTexture);
            TextureRegion hover2 = (level2HoverTexture != null) ? new TextureRegion(level2HoverTexture) : norm2;
            TextureRegionDrawable up2 = new TextureRegionDrawable(norm2);
            TextureRegionDrawable over2 = new TextureRegionDrawable(hover2);
            ib = new ImageButton(up2, over2, over2);
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        } else if (levelNum == 3 && level3ButtonTexture != null) {
            TextureRegion norm3 = new TextureRegion(level3ButtonTexture);
            TextureRegion hover3 = (level3HoverTexture != null) ? new TextureRegion(level3HoverTexture) : norm3;
            TextureRegionDrawable up3 = new TextureRegionDrawable(norm3);
            TextureRegionDrawable over3 = new TextureRegionDrawable(hover3);
            ib = new ImageButton(up3, over3, over3);
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        } else if (levelNum == 4 && level4ButtonTexture != null) {
            TextureRegion norm4 = new TextureRegion(level4ButtonTexture);
            TextureRegion hover4 = (level4HoverTexture != null) ? new TextureRegion(level4HoverTexture) : norm4;
            TextureRegionDrawable up4 = new TextureRegionDrawable(norm4);
            TextureRegionDrawable over4 = new TextureRegionDrawable(hover4);
            ib = new ImageButton(up4, over4, over4);
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        } else if (levelNum == 5 && level5ButtonTexture != null) {
            TextureRegion norm5 = new TextureRegion(level5ButtonTexture);
            TextureRegion hover5 = (level5HoverTexture != null) ? new TextureRegion(level5HoverTexture) : norm5;
            TextureRegionDrawable up5 = new TextureRegionDrawable(norm5);
            TextureRegionDrawable over5 = new TextureRegionDrawable(hover5);
            ib = new ImageButton(up5, over5, over5);
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        } else {
            // No texture found for this level button - skip creating button
            Gdx.app.error("[LevelSelectScreen]", "No button texture found for Level " + levelNum);
            return;
        }
        // enable transform origin so scaling centers on the button
        ib.setTransform(true);
        ib.setOrigin(BUTTON_WIDTH / 2f, BUTTON_HEIGHT / 2f);
        stage.addActor(ib);
        // Add input listener: hover -> scale, touchDown/touchUp -> swap drawables
        ib.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                ib.setScale(HOVER_SCALE);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
                ib.setScale(1f);
            }
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (ib.getStyle().imageOver != null) ib.getImage().setDrawable((TextureRegionDrawable) ib.getStyle().imageOver);
                // remove hover tint while clicking
                if (ENABLE_HOVER_DIAGNOSTICS) {
                    try { ib.getImage().setColor(Color.WHITE); } catch (Exception e) {}
                }
                return false; // allow ClickListener to also handle
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                // on release, restore to over if still hovered, otherwise up
                boolean inside = x >= 0 && x <= ib.getWidth() && y >= 0 && y <= ib.getHeight();
                if (inside && ib.getStyle().imageOver != null) ib.getImage().setDrawable((TextureRegionDrawable) ib.getStyle().imageOver);
                else if (ib.getStyle().imageUp != null) ib.getImage().setDrawable((TextureRegionDrawable) ib.getStyle().imageUp);
                // restore tint after click if still hovered
                if (ENABLE_HOVER_DIAGNOSTICS) {
                    try {
                        if (inside) ib.getImage().setColor(Color.LIGHT_GRAY);
                        else ib.getImage().setColor(Color.WHITE);
                    } catch (Exception e) {}
                }
            }
        });

        if (levelNum != 1 && levelNum != 2 && levelNum != 3 && levelNum != 4 && levelNum != 5) {
            Label.LabelStyle ls = new Label.LabelStyle(buttonFont, Color.WHITE);
            Label label = new Label("Level " + levelNum, ls);
            label.setFontScale(LABEL_FONT_SCALE);
            label.setSize(BUTTON_WIDTH, label.getPrefHeight() * LABEL_FONT_SCALE);
            label.setAlignment(Align.center);
            label.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            ib.addActor(label);
            label.setPosition(0, (BUTTON_HEIGHT - label.getHeight()) / 2f);
        }

        final int lvl = levelNum;
        ib.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game, lvl));
                dispose();
            }
        });

        // For Level 1/2, if hover textures exist, keep references and use polling in render()
        if (levelNum == 1 && level1HoverTexture != null) {
                level1UpDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
                level1OverDrawable = (TextureRegionDrawable) ib.getStyle().imageOver;
             level1ButtonRef = ib;
             level1Hovered = false;
        }
        if (levelNum == 2 && level2HoverTexture != null) {
                level2UpDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
                level2OverDrawable = (TextureRegionDrawable) ib.getStyle().imageOver;
             level2ButtonRef = ib;
             level2Hovered = false;
        }
        if (levelNum == 3 && level3HoverTexture != null) {
                level3UpDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
                level3OverDrawable = (TextureRegionDrawable) ib.getStyle().imageOver;
             level3ButtonRef = ib;
             level3Hovered = false;
        }
        if (levelNum == 4 && level4HoverTexture != null) {
                level4UpDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
                level4OverDrawable = (TextureRegionDrawable) ib.getStyle().imageOver;
             level4ButtonRef = ib;
             level4Hovered = false;
        }
        if (levelNum == 5 && level5HoverTexture != null) {
                level5UpDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
                level5OverDrawable = (TextureRegionDrawable) ib.getStyle().imageOver;
             level5ButtonRef = ib;
             level5Hovered = false;
        }
        // Register this button for unified hover polling (works with touchpads)
        try {
                TextureRegionDrawable upDrawable = (TextureRegionDrawable) ib.getStyle().imageUp;
            TextureRegionDrawable overDrawable = (ib.getStyle().imageOver != null) ? (TextureRegionDrawable) ib.getStyle().imageOver : upDrawable;
                hoverInfos.add(new HoverInfo(levelNum, ib, upDrawable, overDrawable));
        } catch (Exception e) {
            // ignore if style drawables are not TextureRegionDrawable
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // Start music on first render if not already started
        if (!musicStarted) {
            BackgroundMusicManager.getInstance().playScreenMusic();
            musicStarted = true;
        }
        
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Poll mouse position once and update all level hover states immediately (no per-frame allocations)
        tmpStageCoords.set(Gdx.input.getX(), Gdx.input.getY());
        stage.screenToStageCoordinates(tmpStageCoords);
        // Update hoverInfos for unified hover polling
        for (HoverInfo hi : hoverInfos) {
                hi.hovered = updateButtonHover(hi.button, hi.up, hi.over, hi.hovered, tmpStageCoords);
        }
        // (hover polling done above as part of debug label update)
        if (animatedBg != null && bgRegions != null && bgRegions.length > 0) {
            bgAnimTime += delta;
            int frame = (int)(bgAnimTime / BG_FRAME_DURATION) % bgRegions.length;
            animatedBg.setDrawable(new TextureRegionDrawable(bgRegions[frame]));
        }
        stage.act(delta);
        stage.draw();
    }

    // Helper: update hover state for an ImageButton using an existing stage-coordinate point
    private boolean updateButtonHover(ImageButton ref, TextureRegionDrawable up, TextureRegionDrawable over, boolean wasHovered, com.badlogic.gdx.math.Vector2 stagePoint) {
        float bx = ref.getX();
        float by = ref.getY();
        float bw = ref.getWidth();
        float bh = ref.getHeight();
        boolean nowOver = (stagePoint.x >= bx && stagePoint.x <= bx + bw && stagePoint.y >= by && stagePoint.y <= by + bh);
        if (nowOver && !wasHovered) {
            try {
                ImageButton.ImageButtonStyle newStyle = new ImageButton.ImageButtonStyle(ref.getStyle());
                newStyle.imageUp = up;
                newStyle.imageOver = over;
                ref.setStyle(newStyle);
            } catch (Exception e) {
                // fallback
            }
            try {
                ref.getImage().setDrawable(over);
                if (ENABLE_HOVER_DIAGNOSTICS) ref.getImage().setColor(Color.LIGHT_GRAY);
                // play centralized hover sound on enter
                try { game.getHoverSoundManager().playHover(); } catch (Exception ignored) {}
                if (ENABLE_HOVER_DIAGNOSTICS) Gdx.app.log("[HoverDiag]", "ENTER at " + (int)stagePoint.x + "," + (int)stagePoint.y + " for button " + ref);
            } catch (Exception e) {}
            return true;
        } else if (!nowOver && wasHovered) {
            try {
                ImageButton.ImageButtonStyle newStyle = new ImageButton.ImageButtonStyle(ref.getStyle());
                newStyle.imageUp = up;
                newStyle.imageOver = over;
                ref.setStyle(newStyle);
            } catch (Exception e) {
                // fallback
            }
            try {
                ref.getImage().setDrawable(up);
                if (ENABLE_HOVER_DIAGNOSTICS) ref.getImage().setColor(Color.WHITE);
                if (ENABLE_HOVER_DIAGNOSTICS) Gdx.app.log("[HoverDiag]", "EXIT at " + (int)stagePoint.x + "," + (int)stagePoint.y + " for button " + ref);
            } catch (Exception e) {}
            return false;
        }
        return wasHovered;
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
        if (uiAtlas != null) uiAtlas.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (bgTextures != null) {
            for (Texture t : bgTextures) if (t != null) t.dispose();
            bgTextures = null;
        }
        if (level1ButtonTexture != null) {
            level1ButtonTexture.dispose();
            level1ButtonTexture = null;
        }
        if (level2ButtonTexture != null) {
            level2ButtonTexture.dispose();
            level2ButtonTexture = null;
        }
        if (level3ButtonTexture != null) {
            level3ButtonTexture.dispose();
            level3ButtonTexture = null;
        }
        if (level4ButtonTexture != null) {
            level4ButtonTexture.dispose();
            level4ButtonTexture = null;
        }
        if (level5ButtonTexture != null) {
            level5ButtonTexture.dispose();
            level5ButtonTexture = null;
        }
        if (level1HoverTexture != null) {
            level1HoverTexture.dispose();
            level1HoverTexture = null;
        }
        if (level2HoverTexture != null) {
            level2HoverTexture.dispose();
            level2HoverTexture = null;
        }
        if (level3HoverTexture != null) {
            level3HoverTexture.dispose();
            level3HoverTexture = null;
        }
        if (level4HoverTexture != null) {
            level4HoverTexture.dispose();
            level4HoverTexture = null;
        }
        if (level5HoverTexture != null) {
            level5HoverTexture.dispose();
            level5HoverTexture = null;
        }
        if (backButtonTexture != null) {
            backButtonTexture.dispose();
            backButtonTexture = null;
        }
        if (backButtonHoverTexture != null) {
            backButtonHoverTexture.dispose();
            backButtonHoverTexture = null;
        }
        if (levelsFont != null) {
            levelsFont.dispose();
            levelsFont = null;
        }
    }
}
