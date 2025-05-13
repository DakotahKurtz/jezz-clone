package edu.bloomu.animationtest;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

/**
 * Class that encapsulates the behaviour and states of a Ball. Tracks x and y
 * coordinates and ensures the ball remains within the dimensions of the game. Provides
 * method to calculate and update dx and dy of ball upon collision with another ball.
 *
 * @author Dakotah Kurtz
 */
public class Ball {
    private double x;
    private double y;
    private final int radius;

    private double dx;
    private double dy;
    private float rotation = 0;

    public RectF oval;
    private final Matrix matrix;

    public Ball(double x, double y, double dx, double dy, int radius) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.radius = radius;
        matrix = new Matrix();

        oval = new RectF((float) (x - radius), (float) (y - radius), (float) (x + radius),
                (float) (y + radius));
    }

    /**
     * Updates the x and y coordinates based on the current dx and dy attributes while
     * ensuring the ball remains within the given width and height parameters.
     */
    public void move(int width, int height) {
        oval.set((float) (x - radius), (float) (y - radius), (float) (x + radius),
                (float) (y + radius));

        Log.wtf("BALL", "dx: " + dx);
        // bounce the ball if it hits the edge of the game screen
        if (this.x - radius < 0 || this.x + radius > width) {
            reflectXaxis();
        }

        if (this.y - radius < GameView.OPTIONS_HEIGHT + GameView.ABSOLUTE_PADDING || this.y + radius > height - 2 * GameView.ABSOLUTE_PADDING) {
            reflectYaxis();
        }

        x += dx;
        y += dy;

        // super simple calculation that gives a ball rotation that looks "close
        // enough" to realistic
        if (dx > 0) {
            rotation += 2;
        } else {
            rotation -= 2;
        }
        Matrix temp = new Matrix();
        temp.postRotate(rotation, radius, radius);
        temp.postTranslate(oval.left, oval.top);
        matrix.set(temp);
    }

    /**
     * Returns the current matrix rotation applied to the ball
     */
    public Matrix getMatrix() {
        return matrix;
    }

    /**
     * Returns the current oval that "bounds" the edges of the ball
     */
    public RectF getOval() {
        return oval;
    }

    /**
     * Returns the oval that represents what the "bounds" of the ball will on the NEXT
     * call of the move method
     */
    public RectF getNext() {
        return new RectF((float) (x - radius + dx), (float) (y - radius + dy),
                (float) (x + radius + dx),
                (float) (y + radius + dy));
    }

    /**
     * Make dx = -dx, when the ball bounces off a vertical barrier
     */
    public void reflectXaxis() {
        dx = -dx;
    }

    /**
     * Make dy = -dy, when the ball bounces off a horizontal barrier
     */
    public void reflectYaxis() {
        dy = -dy;
    }

    /**
     * Static method that:
     * 1. Determines if the two given balls have collided and
     * 2. updates the dx and dy attributes of those balls if they have collided.
     * <p>
     * I did NOT come up with this algorithm myself. I had attempted to make a JezzBall
     * clone back in Java II but it was way beyond me. However, I did get ball
     * collisions working properly after modifying code I found on the internet. I
     * stole the code from my old project for the collision calculations used below,
     * but I do not know the original source and couldn't find it.
     */
    public static void ballCollisionAdjustment(Ball a, Ball b) {

        double xDist = a.x - b.x;
        double yDist = a.y - b.y;
        double distSquared = xDist * xDist + yDist * yDist;

        // if the distance between the balls is greater than double their radius
        if (distSquared <= (a.radius + b.radius) * (a.radius + b.radius)) {
            // calculate the resulting collision vector
            double xVelocity = b.dx - a.dx;
            double yVelocity = b.dy - a.dy;
            double dotProduct = xDist * xVelocity + yDist * yVelocity;

            if (dotProduct > 0) {
                // scale the collision vector and adjust dx and dy of balls
                double normalize = dotProduct / distSquared;
                double xCollision = xDist * normalize;
                double yCollision = yDist * normalize;

                a.dx += xCollision;
                a.dy += yCollision;
                b.dx -= xCollision;
                b.dy -= yCollision;
            }
        }
    }

    /**
     * Return the current dy
     */
    public double getDy() {
        return dy;
    }

    /**
     * Return the current dx
     */
    public double getDx() {
        return dx;
    }

    /**
     * Return the current radius of the ball
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Return the current value of the x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Return the current value of the y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Set the field x to the given parameter
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * Set the y field of the ball to the given parameter
     */
    public void setY(float y) {
        this.y = y;
    }
}

