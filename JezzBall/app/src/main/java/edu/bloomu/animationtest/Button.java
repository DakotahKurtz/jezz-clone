package edu.bloomu.animationtest;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.util.ArrayList;

/**
 * Encapsulates the behaviour needed for a Button in the application. Tracks left,
 * right, top, and bottom coordinates to determine whether a given touchEvent "clicks"
 * the button.
 */

public class Button {
    private final Bitmap bitmap;
    private final int left;
    private final int right;
    private final int top;
    private final int bottom;

    public Button(int left, int top, Bitmap bitmap) {
        this.left = left;
        this.right = left + bitmap.getWidth();
        this.top = top;
        this.bottom = top + bitmap.getHeight();
        this.bitmap = bitmap;
    }

    /**
     * Return true if both the first and last elements in the list of touches are
     * within the button.
     */
    public boolean clickedIn(ArrayList<float[]> touches) {
        float[] first = touches.get(0);
        float[] last = touches.get(touches.size() - 1);
        return first[0] > left && first[0] < right && first[1] < bottom && first[1] > top
                && last[0] > left && last[0] < right && last[1] < bottom && last[1] > top;
    }

    /**
     * Return the bitmap assigned to this button
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Return the value of the left-most coordinate of this button
     */
    public int getLeft() {
        return left;
    }

    /**
     * Return the value of the top-most coordinate of this button
     */
    public float getTop() {
        return top;
    }

    /**
     * Return the value of the right-most coordinate of this button
     */
    public int getRight() {
        return right;
    }

    /**
     * Return the value of the bottom-most coordinate of this button
     */
    public int getBottom() {
        return bottom;
    }
}
