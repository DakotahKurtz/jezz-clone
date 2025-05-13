package edu.bloomu.animationtest;

import android.graphics.Bitmap;

/**
 * This class extends the Button class for the purpose of forcing the object to track
 * which drawable ID it was initialized with.
 *
 * @author Dakotah Kurtz
 */

public class IDButton extends Button {
    int id;

    public IDButton(int left, int top, Bitmap bitmap, int id) {
        super(left, top, bitmap);
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
