package edu.bloomu.animationtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Encapsulates all the rendering and touch event responses needed when the application
 * is in GameState.PAUSED.
 * Is intended to overlay gameplay, and displays buttons to adjust settings, go to the
 * main menu, and resume gameplay.
 *
 * @author Dakotah Kurtz
 */

public class PauseScreen extends Screen {

    private final Bitmap bgBitmap;
    private final Button menuButton;
    private final Button resumeButton;
    private final Button muteButton;
    private final Button guideButton;
    private final Button tutorialButton;

    private boolean isSoundOn;
    private boolean isGuideEnabled;
    private final Paint textPaint;
    private final int textSize = 30;
    private final int textX;
    private final int guideY;
    private final int soundY;
    private final int tutorialY;
    private boolean isTutorialEnabled;


    public PauseScreen(Context context, int left, int right, int top, int bottom,
                       boolean isSoundOn, boolean isGuideEnabled, boolean isTutorialEnabled) {
        super(context, left, right, top, bottom);
        this.isSoundOn = isSoundOn;
        this.isGuideEnabled = isGuideEnabled;
        this.isTutorialEnabled = isTutorialEnabled;

        // initialize various buttons and bitmaps
        bgBitmap = generateBitMap(context, R.drawable.options_bg, width, height);
        int buttonWidth = (int) (width * .3);
        int buttonHeight = (int) (height * .17);
        int buttonY = top + (int) (height * .1);

        Bitmap menuBitmap = generateBitMap(context, R.drawable.pause_btn_menu,
                buttonWidth, buttonHeight);
        menuButton = new Button(left + (int) (width * .55), buttonY,
                menuBitmap);

        Bitmap playBitmap = generateBitMap(context, R.drawable.title_button_startgame,
                buttonWidth, buttonHeight);
        resumeButton = new Button(left + (int) (width * .15),
                buttonY, playBitmap);

        int toggleButtonWidth = (int) (width * .25);
        int toggleButtonHeight = (int) (height * .15);
        int toggleButtonX = left + (int) (width * .15);

        Bitmap guideBitmap = generateBitMap(context, R.drawable.pause_btn_guide,
                toggleButtonWidth, toggleButtonHeight);
        guideButton = new Button(toggleButtonX, top + (int) (height * .4),
                guideBitmap);

        Bitmap soundBitmap = generateBitMap(context, R.drawable.pause_btn_sound,
                toggleButtonWidth, toggleButtonHeight);
        muteButton = new Button(toggleButtonX,
                top + (int) (getHeight() * .55),
                soundBitmap);

        Bitmap tutorialBitmap = generateBitMap(context, R.drawable.pause_btn_tutorial,
                toggleButtonWidth, toggleButtonHeight);
        tutorialButton = new Button(toggleButtonX,
                top + (int) (getHeight() * .7), tutorialBitmap);

        textPaint = new Paint();
        textPaint.setColor(GameManager.TEXT_COLOR);
        textPaint.setTextSize(30);
        textPaint.setTypeface(GameManager.TYPEFACE);

        textX = guideButton.getRight() + (int) (width * .1);
        guideY = centerTextOn(guideButton);
        soundY = centerTextOn(muteButton);
        tutorialY = centerTextOn(tutorialButton);
    }

    private int centerTextOn(Button button) {
        return (int) ((button.getTop() + button.getBottom()) / 2);
    }

    /**
     * Return true if the sound is set to on.
     */
    public boolean isSoundOn() {
        return isSoundOn;
    }

    /**
     * Return true if the "guiding-line" is set to on.
     */
    public boolean isGuideEnabled() {
        return isGuideEnabled;
    }

    /**
     * Interprets touches for the application when in GameState.PAUSED, and
     * returns the updated GameState according to the input.
     */
    @Override
    public GameState interpretTouch(ArrayList<float[]> touches,
                                    GameState currentGameState) {

        if (menuButton.clickedIn(touches)) {
            return GameState.MENU_SCREEN;
        } else if (resumeButton.clickedIn(touches)) {
            return GameState.GAME_ACTIVE;
        }
        // remain in GameState.PAUSED
        else if (muteButton.clickedIn(touches)) {
            isSoundOn = !isSoundOn;
        } else if (guideButton.clickedIn(touches)) {
            isGuideEnabled = !isGuideEnabled;
        } else if (tutorialButton.clickedIn(touches)) {
            isTutorialEnabled = !isTutorialEnabled;
        }
        return GameState.PAUSED;
    }

    /**
     * Renders the PauseScreen when application is in GameState.PAUSED. Intended to be
     * overlaid on top of the game play screen.
     */
    @Override
    public void render(Canvas canvas) {
        // draw the background
        canvas.drawBitmap(bgBitmap, getLeft(), getTop(), GameView.FAILED_BITMAP_PAINT);
        // and the buttons
        canvas.drawBitmap(menuButton.getBitmap(), menuButton.getLeft(),
                menuButton.getTop(), GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(resumeButton.getBitmap(), resumeButton.getLeft(),
                resumeButton.getTop(), GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(muteButton.getBitmap(), muteButton.getLeft(),
                muteButton.getTop(), GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(guideButton.getBitmap(), guideButton.getLeft(),
                guideButton.getTop(), GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(tutorialButton.getBitmap(), tutorialButton.getLeft(),
                tutorialButton.getTop(), GameView.FAILED_BITMAP_PAINT);
        // update depending on the settings
        String guide = isGuideEnabled ? "ON" : "OFF";
        String sound = isSoundOn ? "ON" : "OFF";
        String tutorial = isTutorialEnabled ? "ON" : "OFF";

        canvas.drawText(guide, textX, guideY, textPaint);
        canvas.drawText(sound, textX, soundY, textPaint);
        canvas.drawText(tutorial, textX, tutorialY, textPaint);
    }

    public boolean isTutorialEnabled() {
        return isTutorialEnabled;
    }
}
