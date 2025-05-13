package edu.bloomu.animationtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

/**
 * DESCRIPTION
 * ****************
 * <p>
 * REVEALED
 * A single-player Android game based on JezzBall, which was first published by Windows
 * in 1992. Gameplay in both Jezzball and Revealed consists of drawing walls to
 * partition the map into subsections that have no "balls" in them. The challenge of
 * the game is to create these subsections separating the bouncing balls, without the
 * walls being struck while still "growing." Gameplay progresses by trapping all balls
 * within some constant percentage of the game space, at which point the game begins
 * again with another ball added.
 * <p>
 * Revealed differs from Jezzball in a few small ways, and one large one. First,
 * Revealed offers the player the ability to unlock different styles of balls, as well
 * as providing a "preview" of where your wall will grow. Of course, Revealed also is
 * played using a touch screen, while JezzBall used a mouse and keyboard.
 * That said, the biggest difference in the two games is that Revealed has an
 * overarching narrative. When a player creates a subsection of the game space that
 * doesn't contain a ball, part of an image is revealed. As gameplay progresses, more
 * and more of that image is revealed, until finally the level is completed and the
 * entire image is visible.
 * These images tell a story from level to level, and the game must be completed for
 * the whole story to be Revealed.
 * <p>
 * Technical Notes
 * *******************
 * <p>
 * UI Responsibility travels from :
 * MainActivity -> GameView -> GameManager -> Screen sub-classes
 * MainActivity is the entrance point into the entire application, but its only two
 * goals are to load SharedPreferences on startup/update them on close and load the
 * GameView.
 * GameView extends the SurfaceView class, and all UI updates go through GameView's
 * SurfaceHolder. A private Thread class locks, renders, and posts the SurfaceHolder
 * every frame, based on information coming from GameManager.
 * <p>
 * The UI thread locks the canvas provided by GameView and passes it to GameManager,
 * who either draws to the canvas or again passes it on to one of the abstract Screen
 * class's children, depending on the current GameState enum.
 * <p>
 * In much the same way, a user's touch input is passed along from :
 * GameView -> GameManager -> Screen subclass / GameEngine, depending on the current
 * GameState.
 * <p>
 * It is probably becoming apparent that the primary challenge in developing this
 * application was:
 * 1. Managing GameState
 * 2. Passing responsibility along in a way that followed OO programing principles.
 * <p>
 * Every Frame
 * *********************
 * GameView's Thread locks the canvas and tells GameManager to update(). GameManager
 * tells GameEngine to tick(), which is where the primary game play itself is updated.
 * GameManager then appropriately updates the GameState based on GameEngine's status.
 * GameView's Thread then tells GameManager to render the appropriate images to canvas,
 * again based on the GameState.
 * <p>
 * A challenge that appeared was somehow updating what appears on screen when the
 * gameplay itself is paused. If the UI thread is not running, user input can still be
 * read and processed appropriately, but the screen won't change to indicate input.
 * It is NOT a great solution, but my work-around was to, when this situation arises,
 * create a new thread and update it once, then pause() again. This has the affect of
 * rendering the updated canvas, but it often causes a visible "jiggle" of gameplay
 * entities. I don't know a way around this.
 * <p>
 * Another challenge is the need to create all screens and buttons from scratch using
 * Bitmaps. Every single example I could find online using SurfaceViews did not have
 * them interacting with xml, but the Android documentation makes it sound very much
 * possible. My attempts in doing so were frustrated mostly by the order in which
 * things are initialized by MainActivity. When I loaded the GameView in xml, I had no
 * control over when the surface was created, and therefore didn't know how to avoid
 * referencing uninitialized Objects when the UI thread spun up. I don't like the way I
 * had to structure the program, but time constraints forced my hand.
 *
 * @author Dakotah Kurtz
 * <p>
 * References
 * *************
 * All background images unlocked through gameplay where generated using Hotpot AI Art
 * Generator at https://hotpot.ai/art-generator.
 * <p>
 * "Smiley Face" ball was created by "The World Smiley Foundation" (no really)
 * "ball_locked" image was found on pngtree.com
 * <p>
 * HotSweat font was created by MaxinFeld at https://www.1001freefonts.com/hot-sweat.font
 * <p>
 * "game_over" sound was created by Mrrobodevin, found at freesound.org
 * "smash" sound was created by InspectorJ, found at freesound.org : filename: "Glass
 * Smash, Bottle, H.wav"
 * "level_complete" sound was found at FunWithSound
 * <p>
 * Information on drawing to a SurfaceView with a thread was found at:
 * http://www.java2s.com/Tutorials/Android/Android_UI_How_to/View
 * /Draw_to_SurfaceView_with_thread.htm
 */

public class MainActivity extends AppCompatActivity {

    protected GameView gameView;
    private SharedPreferences sharedPref;
    protected static final String levelSave = "max_level";
    protected static final String soundSave = "sound";
    protected static final String guideLine = "guideline";
    protected static final String firstTime = "first_time";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getPreferences(Context.MODE_PRIVATE);

        gameView = new GameView(this, null, sharedPref);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateSharedPrefs();
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateSharedPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateSharedPrefs() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(levelSave, gameView.getMaxLevel());
        editor.putBoolean(firstTime, gameView.isTutorialEnabled());
        editor.putBoolean(soundSave, gameView.isSoundOn());
        editor.putBoolean(guideLine, gameView.isGuideEnabled());
        editor.apply();
    }

}