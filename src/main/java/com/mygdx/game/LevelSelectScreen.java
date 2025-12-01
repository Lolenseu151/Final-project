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

    // Layout
    private static final float BUTTON_WIDTH = 220f;
    private static final float BUTTON_HEIGHT = 128f;
    private static final float BUTTON_SPACING = 12f;
    private static final float TITLE_TOP_MARGIN = 30f;
    private static final float LABEL_FONT_SCALE = 1.5f;
    private static final float BUTTON_VERTICAL_OFFSET = 70f;
    private static final float LEVELS_FONT_SCALE = 6.0f;
    private static final float LEVELS_TOP_MARGIN = 25f;
    private static final float BACK_BUTTON_SCALE = 0.09f;

    public LevelSelectScreen(MyGdxGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());

        // Load atlas (try internal then absolute)
        try {
            String atlasPath = "ui select original/ui.atlas";
            com.badlogic.gdx.files.FileHandle handle = null;
            if (Gdx.files.internal(atlasPath).exists()) handle = Gdx.files.internal(atlasPath);
            else {
                String userDir = System.getProperty("user.dir");
                String abs = userDir + "/assets/" + atlasPath;
                if (Gdx.files.absolute(abs).exists()) handle = Gdx.files.absolute(abs);
            }
            if (handle == null) throw new RuntimeException("ui.atlas not found");
            uiAtlas = new TextureAtlas(handle);
        } catch (Exception e) {
            Gdx.app.error("[LevelSelectScreen]", "Failed to load atlas", e);
            throw new RuntimeException(e);
        }

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
        } else {
            TextureRegion bgRegion = uiAtlas.findRegion("level_bg");
            if (bgRegion != null) {
                Image bg = new Image(bgRegion);
                bg.setFillParent(true);
                stage.addActor(bg);
            }
        }

        // Title
        TextureRegion titleRegion = uiAtlas.findRegion("title_selectlevel");
        if (titleRegion != null) {
            Image title = new Image(titleRegion);
            float titleW = titleRegion.getRegionWidth();
            float titleH = titleRegion.getRegionHeight();
            title.setSize(titleW, titleH);
            title.setPosition((screenWidth - titleW) / 2f, screenHeight - titleH - TITLE_TOP_MARGIN);
            stage.addActor(title);
        }

        // Load bitmap font for the top-center "Levels" label (internal first, then project assets)
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

        // Back button: prefer custom textures (assets/back button/7.png, 8.png), fallback to atlas
        TextureRegion arrow = null;
        TextureRegion arrowHover = null;
        if (backButtonTexture != null) {
            arrow = new TextureRegion(backButtonTexture);
            arrowHover = (backButtonHoverTexture != null) ? new TextureRegion(backButtonHoverTexture) : arrow;
        } else {
            arrow = uiAtlas.findRegion("arrow_back");
            arrowHover = uiAtlas.findRegion("arrow_back_hover");
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
        ImageButton ib;
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
            TextureRegion btn = uiAtlas.findRegion("level_button");
            TextureRegion btnHover = uiAtlas.findRegion("level_button_hover");
            if (btn == null) return;
            TextureRegionDrawable up = new TextureRegionDrawable(btn);
            TextureRegionDrawable over = (btnHover != null) ? new TextureRegionDrawable(btnHover) : up;
            ib = new ImageButton(up, over, over);
            ib.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);
            ib.setPosition(x, y);
        }
        stage.addActor(ib);
        // Add enter/exit listener to swap inner drawable immediately on mouse hover
        ib.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor from) {
                if (ib.getStyle().imageOver != null) {
                    ib.getImage().setDrawable((TextureRegionDrawable) ib.getStyle().imageOver);
                }
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor to) {
                if (ib.getStyle().imageUp != null) {
                    ib.getImage().setDrawable((TextureRegionDrawable) ib.getStyle().imageUp);
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
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Poll mouse position once and update all level hover states immediately (no per-frame allocations)
        tmpStageCoords.set(Gdx.input.getX(), Gdx.input.getY());
        stage.screenToStageCoordinates(tmpStageCoords);
        if (level1ButtonRef != null && level1UpDrawable != null && level1OverDrawable != null)
            level1Hovered = updateButtonHover(level1ButtonRef, level1UpDrawable, level1OverDrawable, level1Hovered, tmpStageCoords);
        if (level2ButtonRef != null && level2UpDrawable != null && level2OverDrawable != null)
            level2Hovered = updateButtonHover(level2ButtonRef, level2UpDrawable, level2OverDrawable, level2Hovered, tmpStageCoords);
        if (level3ButtonRef != null && level3UpDrawable != null && level3OverDrawable != null)
            level3Hovered = updateButtonHover(level3ButtonRef, level3UpDrawable, level3OverDrawable, level3Hovered, tmpStageCoords);
        if (level4ButtonRef != null && level4UpDrawable != null && level4OverDrawable != null)
            level4Hovered = updateButtonHover(level4ButtonRef, level4UpDrawable, level4OverDrawable, level4Hovered, tmpStageCoords);
        if (level5ButtonRef != null && level5UpDrawable != null && level5OverDrawable != null)
            level5Hovered = updateButtonHover(level5ButtonRef, level5UpDrawable, level5OverDrawable, level5Hovered, tmpStageCoords);
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
            ref.getImage().setDrawable(over);
            return true;
        } else if (!nowOver && wasHovered) {
            ref.getImage().setDrawable(up);
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
