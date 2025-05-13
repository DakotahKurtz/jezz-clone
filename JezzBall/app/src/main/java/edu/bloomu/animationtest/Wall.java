package edu.bloomu.animationtest;

import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;

/**
 * Encapsulates all information and behaviour needed for the walls drawn by the
 * user to divide the game space.
 * If we consider the left, right, top, and bottom
 * coordinates that define the rectangle a Wall resides in, only one of those
 * dimensions will ever change, based on the Direction indicated in the constructor.
 * <p>
 * Walls are only created "locked" into an x,y grid, and therefore only grow at fixed
 * intervals. That is, if the thickness of a wall is 10, it can only grow through
 * 10->20->30->etc.
 *
 * @author Dakotah Kurtz
 */

public class Wall {

    private final float startX;
    private final float startY;

    private final int speed = 4;
    private int increment = 0; //track when the wall will next "grow"
    private final float thickness;
    private int length;
    private final Direction direction;

    public Paint paint;

    private boolean isMoving;
    private boolean isDrawn;
    private final RectF rect; // bounding box for Wall


    public Wall(float startX, float startY, float thickness, Direction direction,
                int color) {
        this.startX = startX;
        this.startY = startY;
        this.thickness = thickness;
        this.direction = direction;

        isMoving = true;
        isDrawn = false;
        length = 0;

        rect = new RectF((float) startX, (float) startY, (float) ((float) startX + thickness),
                (float) startY + thickness);

        this.paint = new Paint();
        this.paint.setColor(color);
    }

    /**
     * Called every "tick" by the GameEngine, this method updates the length of the
     * Wall and checks to see if it has reached either the edge of the game area or
     * another wall. Updates the isMoving field if so.
     */
    public void move(double width, double height, ArrayList<Wall> walls) {
        // only increase length once Wall can grow to the next multiple of its thickness
        increment += speed;
        if (increment < thickness) {
            return;
        }
        increment = 0;
        length += thickness;

        /*
         * Determine if the wall will hit another wall on it's next tick
         */
        RectF next = getNextRect(width, height);
        RectF collisionWall = null;

        for (int i = 0; i < walls.size(); i++) {
            if (!walls.get(i).isMoving() && RectF.intersects(next,
                    walls.get(i).getRect())) {
                collisionWall = walls.get(i).getRect();

                break;
            }
        }

        // if so, update coordinates of this Wall so it won't overlap with the Wall it hit
        float x, y;
        if (collisionWall != null) {
            isMoving = false;
            switch (direction) {
                case Left:
                    x = collisionWall.right;
                    rect.set(x, startY, startX + thickness, startY + thickness);
                    return;
                case Right:
                    x = collisionWall.left;

                    rect.set(startX, startY, x, startY + thickness);
                    return;
                case Up:
                    y = collisionWall.bottom;

                    rect.set(startX, y, startX + thickness, startY + thickness);
                    return;
                case Down:
                    y = collisionWall.top;
                    rect.set(startX, startY, startX + thickness, y);
                    return;
            }

        }

        // update the rectangle to where it will be on it's next tick
        rect.set(next.left, next.top, next.right, next.bottom);

        // hit the top or bottom
        if (direction == Direction.Down || direction == Direction.Up) {
            if (rect.top <= GameView.OPTIONS_HEIGHT || rect.bottom >= height) {
                isMoving = false;
            }
        }

        // hit the left or right
        if (direction == Direction.Left || direction == Direction.Right) {
            if (rect.left <= 0 || rect.right >= width) {
                isMoving = false;
            }
        }
    }


    /**
     * Returns the bounding of what this Wall will have AFTER the next tick
     */
    private RectF getNextRect(double width, double height) {

        float x;
        float y;

        // update the appropriate coordinate
        switch (direction) {
            case Left:
                x = rect.left - length;

                if (x <= 0) { // grew too far left, keep within bounds
                    x = 0;
                }
                return new RectF(x, startY, startX + thickness, startY + thickness);
            case Right:
                x = rect.right + length;

                if (x >= width) {// grew too far right, keep within bounds
                    x = (float) width;
                }
                return new RectF(startX, startY, x, startY + thickness);
            case Up:
                y = rect.top - length;

                if (y <= GameView.OPTIONS_HEIGHT) { // grew too far up, keep within bounds
                    y = GameView.OPTIONS_HEIGHT;
                }
                return new RectF(startX, y, startX + thickness, startY + thickness);
            case Down:
                y = rect.bottom + length;
                if (y >= height) { // grew too far down, keep within bounds
                    y = (float) height;
                }
                return new RectF(startX, startY, startX + thickness, y);

        }
        return this.rect;
    }


    /**
     * Return true if this Wall is currently moving
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * Return the Direction this wall is growing/grew
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns whether this Wall in it's final location has been drawn to the canvas
     */
    public boolean isDrawn() {
        return isDrawn;
    }

    /**
     * Sets the value of the isDrawn field
     */
    public void setDrawn(boolean b) {
        isDrawn = b;
    }

    /**
     * Return the current bounding of this Wall
     */
    public RectF getRect() {
        return rect;
    }
}
