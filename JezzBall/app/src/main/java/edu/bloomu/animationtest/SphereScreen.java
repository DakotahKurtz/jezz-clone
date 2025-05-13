package edu.bloomu.animationtest;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Encapsulates all the rendering and touch event responses needed when the application
 * is in GameState.SPHERE_SCREEN.
 * Reached from the main menu, this Screen allows users to choose what style of spheres
 * they want to see bouncing around the game. Users unlock more spheres the more levels
 * they unlock. Until that point, locked spheres appear as a special "locked" bitmap,
 * which indicates to the user that they can somehow unlock more.
 *
 * @author Dakotah Kurtz
 */
public class SphereScreen extends Screen {

    private Bitmap background;
    private Bitmap selected;
    private Button selectButton;

    private ArrayList<IDButton> sphereChoices;
    private final ArrayList<Integer> drawables;

    private int selection;
    private int unlockedSpheres;
    private int buttonSize;

    public SphereScreen(Context context, int left, int right, int top, int bottom,
                        int maxLevel) {
        super(context, left, right, top, bottom);
        drawables = getDrawables();

        init(maxLevel);
    }


    /**
     * Interpret touch events when the application is in GameState.SPHERE_SCREEN.
     * Update the "selection" if the user chooses an unlocked sphere.
     * Return the appropriate GameState after touch.
     */
    @Override
    public GameState interpretTouch(ArrayList<float[]> touches,
                                    GameState currentGameState) {

        // allow the user to update their selection by touching an unlocked sphere
        for (int i = 0; i < sphereChoices.size(); i++) {
            if (sphereChoices.get(i).clickedIn(touches)) {
                if (i < unlockedSpheres) {
                    selection = sphereChoices.get(i).getId();
                }
                return GameState.SPHERE_SCREEN;
            }
        }
        // if they're done, go back to main menu
        if (selectButton.clickedIn(touches)) {
            return GameState.MENU_SCREEN;
        } // otherwise remain in state SPHERE_SCREEN
        else {
            return GameState.SPHERE_SCREEN;
        }
    }

    /**
     * Responsible for rendering when in GameState.SPHERE_SCREEN. This rendering is
     * intended to overlay the main menu screen. Display the available spheres for
     * selection and indicate which is currently selected by circling it.
     * Display a button that allows user to return to the main menu.
     */
    @Override
    public void render(Canvas canvas) {
        canvas.drawBitmap(background, left, top, new Paint(Color.BLACK));

        for (IDButton button : sphereChoices) {
            // draw circle around chosen sphere
            if (button.getId() == selection) {
                canvas.drawBitmap(selected, (int) (button.getLeft() - buttonSize * .15),
                        (int) (button.getTop() - buttonSize * .15),
                        new Paint(GameView.FAILED_BITMAP_PAINT));
            }
            canvas.drawBitmap(button.getBitmap(), button.getLeft(), button.getTop(),
                    new Paint(Color.BLACK));
        }

        canvas.drawBitmap(selectButton.getBitmap(), selectButton.getLeft(),
                selectButton.getTop(), new Paint(Color.BLACK));

    }

    /**
     * Return an integer representing the Drawable selected by the user
     */
    public int getSelection() {
        return selection;
    }

    /**
     * Update the list of spheres available based on game progress
     */
    public void updateSphereButtons(int maxLevel) {
        init(maxLevel);
    }

    /*
    Private method to declutter the constructor that loads the drawables into an
    arraylist.
 */
    private ArrayList<Integer> getDrawables() {
        ArrayList<Integer> drawables = new ArrayList<>();
        drawables.add(R.drawable.ball_smile_harvey);
        drawables.add(R.drawable.ball_soccer);
        drawables.add(R.drawable.ball_fire);
        drawables.add(R.drawable.ball_hamster);
        drawables.add(R.drawable.ball_pumpkin);
        return drawables;
    }


    /*
     * Initialize the Buttons and bitmaps based off of the max level currently reached.
     * Display the spheres in rows of 3, replacing the image for any currently "locked"
     * with a default "locked" image.
     */
    private void init(int maxLevel) {
        int startingUnlocked = 1;
        unlockedSpheres = startingUnlocked + maxLevel / 2;

        background = generateBitMap(getContext(), R.drawable.options_bg, width, height);
        // set the default selection to the first ball unlocked
        selection = R.drawable.ball_smile_harvey;

        int horizontalSpacing = 30;
        int numPerRow = 3;
        buttonSize = ((width - (numPerRow + 1) * horizontalSpacing) / (numPerRow + 1));

        selected = generateBitMap(context, R.drawable.ball_selected,
                (int) (buttonSize * 1.3), (int) (buttonSize * 1.3));
        Bitmap b = generateBitMap(context, R.drawable.title_option_selectbutton,
                (int) (width * .3), (int) (height * .1));
        selectButton = new Button((int) ((width * .5) - (width * .15)) + left,
                top + (int) (height * .8), b);
        // load ArrayList with appropriate spheres based on the number unlocked.
        sphereChoices = getButtons(drawables, buttonSize, numPerRow);
    }

    /*
        Loads ArrayList of IDButtons representing sphere choices.
     */
    private ArrayList<IDButton> getButtons(ArrayList<Integer> drawables, int size,
                                           int numPerRow) {

        ArrayList<IDButton> buttons = new ArrayList<>();

        // how many "spaces" will need to be divided up
        int division = (int) (width / (numPerRow + 1));
        // padding before the first row begins
        int vertPadding = 300;
        int i;

        for (i = 0; i < drawables.size(); i++) {
            // if the sphere is unlocked, add the bitmap for it. Otherwise, use the
            // default "locked" sphere
            int id = i < unlockedSpheres ? drawables.get(i) : R.drawable.ball_locked;
            Bitmap b = generateBitMap(context, id, size, size);
            int x = left + ((i % numPerRow) + 1) * division - (size / 2);
            int y = vertPadding + (int) ((i / numPerRow) * size * 1.3); // * 1.3
            // because it looks nice
            buttons.add(new IDButton(x, y, b, id));
        }
        // if all the spheres are unlocked, allow users to choose the "locked" sphere
        if (unlockedSpheres >= drawables.size()) {
            int x = left + ((i % numPerRow) + 1) * division - (size / 2);
            int y = vertPadding + (int) ((i / numPerRow) * size * 1.3);
            buttons.add(new IDButton(x, y, generateBitMap(context,
                    R.drawable.ball_locked, size, size), R.drawable.ball_locked));
        }
        return buttons;
    }
}
