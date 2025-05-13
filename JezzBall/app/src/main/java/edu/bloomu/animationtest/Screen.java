package edu.bloomu.animationtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Abstract class to hold all shared functionality of the various screens that
 * represent different GameStates.
 * Holds the values for the left,right,top, and bottom coordinates and calculates the
 * width and height. Provides access to the generateBitmap method and enforces that all
 * screens provide rendering and handle user input.
 *
 * @author Dakotah Kurtz
 */

public abstract class Screen {

    final Context context;
    final int left;
    final int right;
    final int top;
    final int bottom;
    final int width;
    final int height;

    public Screen(Context context, int left, int right, int top, int bottom) {
        this.context = context;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.width = right - left;
        this.height = bottom - top;
    }

    /**
     * Interpret touch events and return an updated GameState
     */
    public abstract GameState interpretTouch(ArrayList<float[]> touches,
                                             GameState currentGameState);

    /**
     * Handle rendering the screen during a particular GameState
     */
    public abstract void render(Canvas canvas);

    /**
     * Generates a bitmap for the specific context of the size given.
     */
    protected Bitmap generateBitMap(Context context, int id,
                                    int x, int y) {
        return GameManager.generateBitMap(context, id, x, y);
    }

    /**
     * Returns the width of this screen
     */
    protected int getWidth() {
        return width;
    }

    /**
     * Returns the height of this screen
     */
    protected int getHeight() {
        return height;
    }

    protected Context getContext() {
        return context;
    }

    protected int getLeft() {
        return left;
    }

    protected int getRight() {
        return right;
    }

    protected int getTop() {
        return top;
    }

    protected int getBottom() {
        return bottom;
    }
}
