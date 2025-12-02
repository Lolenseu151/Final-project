package com.mygdx.game.Levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.Fixer;
import com.mygdx.game.ILevelManager; // changed from LevelManager

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
    default void updateBackground(float deltaTime, ILevelManager levelMgr, Array<Rectangle> documents,
                                   Array<Rectangle> obstacles, Fixer player) {
        // default: no update logic (background is static)
    }

    /**
     * Render the background with custom effects (e.g., alpha fade, tint).
     * Called by LevelManager before drawing level shapes.
     * @param batch SpriteBatch for drawing
     * @param backgroundTexture the loaded background texture (may be null)
     */
    default void renderBackground(SpriteBatch batch, Texture backgroundTexture) {
        // default: LevelManager handles rendering
    }

    /**
     * Optional: render overlays that must appear on top of level objects (documents, platforms).
     * Called by LevelManager after documents and sprites are drawn so overlays appear above gameplay.
     * @param batch SpriteBatch for drawing
     */
    default void renderOverlay(SpriteBatch batch) {
        // default: no overlay
    }

    /**
     * If true the level's overlay should block core gameplay updates (player movement,
     * timers, collisions). Default is false. Levels that show blocking dialogs should
     * return true while the dialog is visible.
     */
    default boolean isOverlayBlocking() { return false; }
}