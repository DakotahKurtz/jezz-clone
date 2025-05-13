package edu.bloomu.animationtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import java.util.ArrayList;

/**
 * Encapsulates all the rendering and touch event responses needed when the application
 * is in GameState.GALLERY_SCREEN. Tracks and calculates the current number of images
 * unlocked throughout gameplay, and allows the user to swipe through those images.
 * Upon reaching "locked" images, displays a message overlaying a default image.
 *
 * @author Dakotah Kurtz
 */

public class GalleryScreen extends Screen {

    private int unlocked;
    private int current; // current image
    private ArrayList<Button> imageButtons;
    private Button doneButton;

    private final Bitmap bg; // frame for the images
    private final ArrayList<Integer> backgrounds; // all backgrounds

    public GalleryScreen(Context context, int left, int right, int top, int bottom,
                         int unlocked) {
        super(context, left, right, top, bottom);
        this.unlocked = unlocked;

        current = 0;
        backgrounds = GameManager.getBackgroundDrawables();

        bg = generateBitMap(context, R.drawable.options_bg, width, height);
        updateImageButtons(unlocked);
    }

    /**
     * Create Buttons containing unlocked images of number equal to the current max
     * level reached through gameplay. Create buttons equal to the remaining number
     * containing a default image overlaid with text informing the user to play more to
     * unlock more images.
     */
    public void updateImageButtons(int maxLevel) {
        unlocked = maxLevel - 1;
        imageButtons = new ArrayList<>();
        // messy size calculations
        int imageWidth = (int) (width * .8);
        int imageHeight = (int) (height * .7);
        int horPadding = (int) (width - imageWidth) / 2;
        int vertPadding = (int) ((height - imageHeight) * .2);

        Bitmap locked = generateBitMap(context, R.drawable.title_reveal_revealmore,
                imageWidth, imageHeight);

        Button button;

        // add unlocked image to button if possible, if not add default image to button
        for (int i = 0; i < backgrounds.size(); i++) {
            if (i < unlocked) {
                Bitmap b = generateBitMap(context, backgrounds.get(i), imageWidth, imageHeight);
                button = new Button(horPadding,
                        top + vertPadding, b);
            } else {
                button = new Button(horPadding, top + vertPadding, locked);
            }
            imageButtons.add(button);
        }

        // add button to return to the main menu
        doneButton = new Button((int) (width * .35),
                (int) (height - (height - vertPadding - (height * .7)) / 2),
                generateBitMap(context,
                        R.drawable.title_reveal_btn_done, (int) (width * .3),
                        (int) (height * .1)));
    }

    /**
     * Handle input events when application is in GameState.GALLERY_SCREEN, returning
     * an updated gamestate to the TitleScreen depending on user input.
     */
    @Override
    public GameState interpretTouch(ArrayList<float[]> touches,
                                    GameState currentGameState) {
        if (doneButton.clickedIn(touches)) { // back to main menu
            return GameState.MENU_SCREEN;
        } else if (imageButtons.get(current).clickedIn(touches)) {
            // did user try to swipe?
            float[] last = touches.get(touches.size() - 1);
            float[] first = touches.get(0);
            int swipeSize = 100;
            // swipe left, increment image Button
            if (first[0] - last[0] > swipeSize) {
                current = Math.min(++current, backgrounds.size() - 1);
                // swipe right, decrement image Button
            } else if (last[0] - first[0] > swipeSize) {
                current = Math.max(--current, 0);
            }
            // remain in GameState.GALLERY_SCREEN until user clicks in doneButton
            return GameState.GALLERY_SCREEN;
        }
        return GameState.GALLERY_SCREEN;
    }

    /**
     * Render the GalleryScreen when application is in GameState.GALLERY_SCREEN
     */
    @Override
    public void render(Canvas canvas) {
        canvas.drawBitmap(bg, left, top, GameView.FAILED_BITMAP_PAINT);
        Button currentView = imageButtons.get(current);
        canvas.drawBitmap(currentView.getBitmap(), currentView.getLeft(),
                currentView.getTop(), GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(doneButton.getBitmap(), doneButton.getLeft(),
                doneButton.getTop(), GameView.FAILED_BITMAP_PAINT);
    }
}
