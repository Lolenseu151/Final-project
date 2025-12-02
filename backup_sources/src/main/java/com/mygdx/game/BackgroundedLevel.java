package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;

/**
 * Optional interface for levels that provide a background image with interactive logic.
 * Backgrounds can respond to documents collected, obstacles hit, and player state.
 */
public interface BackgroundedLevel {
    /**
     * Return the internal/relative path to the background image (e.g. "Level1Map.png"),
     * or null if no background.
     */
    String getBackgroundPath();

    /**
     * Update background state based on level manager, documents, obstacles, and player.
     * Called once per frame by LevelManager.
     * @param deltaTime frame delta time
     * @param levelMgr the LevelManager instance
     * @param documents collected/remaining documents
     * @param obstacles red tape obstacles
     * @param player the Fixer player
     */
    default void updateBackground(float deltaTime, LevelManager levelMgr, Array<Rectangle> documents,
                                   Array<Rectangle> obstacles, Fixer player) {
        // default: no update logic (background is static)
    }

    /**
     * Render the background with custom effects (e.g., alpha fade, tint).
     * Called by LevelManager before drawing level shapes.
     * @param batch SpriteBatch for drawing
     * @param backgroundTexture the loaded background texture (may be null)
     */
    default void renderBackground(SpriteBatch batch, com.badlogic.gdx.graphics.Texture backgroundTexture) {
        // default: LevelManager handles rendering
    }
}