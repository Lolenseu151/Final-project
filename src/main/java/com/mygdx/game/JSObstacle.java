package com.mygdx.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * JSObstacle - small utility wrapper for creating "JS" style obstacles.
 *
 * This class provides factory/helpers so levels can add the same obstacle
 * consistently without duplicating Rectangle creation code.
 *
 * Usage examples:
 * - Rectangle r = JSObstacle.createRect(x,y,w,h);
 * - JSObstacle.addTo(obstaclesArray, x,y,w,h); // adds and returns Rectangle
 */
public class JSObstacle {
    private Rectangle rect;
    private String tag = "JS";

    public JSObstacle(float x, float y, float w, float h) {
        this.rect = new Rectangle(x, y, w, h);
    }

    public Rectangle getRect() { return rect; }

    public String getTag() { return tag; }

    /**
     * Create a Rectangle instance for a JS obstacle.
     */
    public static Rectangle createRect(float x, float y, float w, float h) {
        return new Rectangle(x, y, w, h);
    }

    /**
     * Add a JS obstacle rectangle to an existing obstacles array and return it.
     * This keeps level code tidy: JSObstacle.addTo(obstacles, x,y,w,h);
     */
    public static Rectangle addTo(Array<Rectangle> obstacles, float x, float y, float w, float h) {
        if (obstacles == null) return createRect(x,y,w,h);
        Rectangle r = createRect(x, y, w, h);
        obstacles.add(r);
        return r;
    }
}
