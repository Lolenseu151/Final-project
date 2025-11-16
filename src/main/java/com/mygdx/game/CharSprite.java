package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CharSprite {
    private Texture spriteSheet;
    private TextureRegion[][] frames; // frames[row][col]
    private int frameWidth = 0;
    private int frameHeight = 0;

    public CharSprite() {
        try {
            FileHandle fh = Gdx.files.internal("MainChar.png");
            Gdx.app.log("CharSprite", "load exists=" + fh.exists() + " path=" + fh.path());
            spriteSheet = new Texture(fh);
            Gdx.app.log("CharSprite", "Sprite loaded: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());
        } catch (Exception e) {
            Gdx.app.error("CharSprite", "Failed to load sprite", e);
            spriteSheet = null;
        }
    }

    // Call this once you know the frame cell size (pixels)
    public void splitFrames(int frameWidth, int frameHeight) {
        if (spriteSheet == null) return;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        frames = TextureRegion.split(spriteSheet, frameWidth, frameHeight);
        Gdx.app.log("CharSprite", "Split into rows=" + frames.length + " cols=" + (frames.length>0? frames[0].length:0));
    }

    // row/col using LibGDX's split result directly (row 0 = top row OR bottom row depending on texture layout)
    public TextureRegion getFrameBottom(int row, int col) {
        if (frames == null) return null;
        if (row < 0 || row >= frames.length || col < 0 || col >= frames[0].length) return null;
        return frames[row][col];
    }

    // If your sheet rows are ordered top->bottom, use this helper (row 0 = top)
    public TextureRegion getFrameTop(int row, int col) {
        if (frames == null) return null;
        int rows = frames.length;
        int r = rows - 1 - row;
        if (r < 0 || r >= rows || col < 0 || col >= frames[0].length) return null;
        return frames[r][col];
    }

    public TextureRegion getDefaultFrame() {
        if (frames != null && frames.length > 0 && frames[0].length > 0) return frames[0][0];
        if (spriteSheet != null) return new TextureRegion(spriteSheet);
        return null;
    }

    public int getFrameWidth() { return frameWidth > 0 ? frameWidth : (spriteSheet!=null? spriteSheet.getWidth():0); }
    public int getFrameHeight() { return frameHeight > 0 ? frameHeight : (spriteSheet!=null? spriteSheet.getHeight():0); }

    public void dispose() {
        if (spriteSheet != null) spriteSheet.dispose();
    }
}


