package edu.bloomu.animationtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.ArrayList;

/**
 * Encapsulates all the rendering and touch event responses needed when the application
 * is in GameState.MENU_SCREEN. Displays title image and buttons to allow users to view
 * the GalleryScreen, the SphereScreen, or to begin gameplay. This class interfaces
 * directly with the GameManager class, and passes information from the GalleryScreen
 * and SphereScreen through it.
 *
 * @author Dakotah Kurtz
 */

public class TitleScreen extends Screen {

    private final Button startGameButton;
    private final Button chooseSphereButton;
    private final Button revealButton;

    private final Bitmap background;
    private final Bitmap startGameBitmap;
    private final Bitmap spheresBitmap;
    private final Bitmap tutorialBitmap;

    private GameState currentState;
    private final SphereScreen sphereScreen;
    private final GalleryScreen galleryScreen;

    private int sphereSelection;
    private int maxLevel;
    private boolean isTutorialEnabled;

    public TitleScreen(Context context, int left, int right, int top, int bottom,
                       int maxLevel, boolean isTutorialEnabled) {
        super(context, left, right, top, bottom);

        this.maxLevel = maxLevel;
        this.isTutorialEnabled = isTutorialEnabled;
        currentState = GameState.MENU_SCREEN;
        background = generateBitMap(context, R.drawable.title_bg, width, height);

        int buttonWidth = (int) (width * .3);
        int buttonHeight = (int) (height * .1);
        int buttonLeft = (int) (width * .65);
        int firstButtonHeight = (int) (height * .25);
        int verticalSpacing = 30;

        startGameBitmap = generateBitMap(context, R.drawable.title_button_startgame,
                buttonWidth, buttonHeight);
        spheresBitmap = generateBitMap(context, R.drawable.title_button_sphere,
                buttonWidth, buttonHeight);
        Bitmap revealBitmap = generateBitMap(context, R.drawable.title_button_reveal,
                buttonWidth, buttonHeight);

        int border = (int) (width * .1);
        tutorialBitmap = generateBitMap(context, R.drawable.title_tutorial,
                width - border, height - border);

        chooseSphereButton = new Button(buttonLeft, firstButtonHeight,
                spheresBitmap);
        revealButton = new Button(buttonLeft,
                firstButtonHeight + buttonHeight + verticalSpacing, revealBitmap);
        startGameButton = new Button(buttonLeft,
                firstButtonHeight + 2 * (buttonHeight + verticalSpacing),
                startGameBitmap);

        // load the screens
        sphereScreen = new SphereScreen(context, border, width - border, border,
                height - border, maxLevel);
        galleryScreen = new GalleryScreen(context, 0, width, border,
                height - border, maxLevel);

        sphereSelection = sphereScreen.getSelection();
    }

    /**
     * Interprets touch events when the GameState is in any of the following states:
     * MENU_SCREEN, SPHERE_SCREEN, GALLERY_SCREEN, and passes the information on to the
     * appropriate screen if necessary.
     * Updates the SphereScreen and GalleryScreen with the most recent gameplay
     * information if selected by the user.
     * This method is responsible for handling input if the state is MENU_SCREEN.
     */
    public GameState interpretTouch(ArrayList<float[]> touches,
                                    GameState currentGameState) {

        isTutorialEnabled = false;
        if (currentState == GameState.MENU_SCREEN) {
            if (startGameButton.clickedIn(touches)) { // start game
                return GameState.GAME_ACTIVE;
            } else if (chooseSphereButton.clickedIn(touches)) {
                // update
                sphereScreen.updateSphereButtons(maxLevel);
                currentState = GameState.SPHERE_SCREEN;
            } else if (revealButton.clickedIn(touches)) {
                // update
                galleryScreen.updateImageButtons(maxLevel);
                currentState = GameState.GALLERY_SCREEN;
            }
            // pass the information on
        } else if (currentState == GameState.SPHERE_SCREEN) {
            currentState = sphereScreen.interpretTouch(touches, currentState);
            if (currentState == GameState.MENU_SCREEN) {
                // coming back from sphere screen, update selection to give to GameManager
                sphereSelection = sphereScreen.getSelection();
            }
        } else if (currentState == GameState.GALLERY_SCREEN) {
            currentState = galleryScreen.interpretTouch(touches, currentGameState);
        }

        return currentGameState;
    }

    /**
     * Responsible for rendering when the application is in the following states:
     * MENU_SCREEN, SPHERE_SCREEN, GALLERY_SCREEN.
     * <p>
     * If in the latter two states, also calls on the appropriate Screen to overlay its
     * rendering on top of the TitleScreen
     */
    public void render(Canvas canvas) {
        switch (currentState) {
            case MENU_SCREEN:
                canvas.drawBitmap(background, 0, 0, GameView.FAILED_BITMAP_PAINT);
                canvas.drawBitmap(startGameBitmap, startGameButton.getLeft(),
                        startGameButton.getTop(), GameView.FAILED_BITMAP_PAINT);
                canvas.drawBitmap(chooseSphereButton.getBitmap(), chooseSphereButton.getLeft(),
                        chooseSphereButton.getTop(), GameView.FAILED_BITMAP_PAINT);
                canvas.drawBitmap(revealButton.getBitmap(), revealButton.getLeft(),
                        revealButton.getTop(), GameView.FAILED_BITMAP_PAINT);
                if (isTutorialEnabled) {
                    canvas.drawBitmap(tutorialBitmap, (int) (width * .05),
                            (int) (width * .1), GameView.FAILED_BITMAP_PAINT);
                }
                break;
            case SPHERE_SCREEN:
                sphereScreen.render(canvas);
                break;
            case GALLERY_SCREEN:
                galleryScreen.render(canvas);
                break;
        }
    }

    /**
     * Returns an int representing the Drawable of the currently selected sphere.
     */
    public int getSphereSelection() {
        return sphereSelection;
    }

    /**
     * Updates the maxLevel field to the new max.
     */
    public void updateMaxLevel(int max) {
        maxLevel = max;
    }


}
