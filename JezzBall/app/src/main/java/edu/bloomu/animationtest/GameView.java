package edu.bloomu.animationtest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * The entirety of the application goes through this GameView class. It is created in
 * MainActivity and is the object of the contentView.
 * Loads shared preferences and updates them as needed throughout runtime, initializes
 * GameManager class and is responsible for starting and stopping the gameplay and UI
 * threads safely.
 * <p>
 * Contains an inner Thread class which handles all UI updates and GameManager ticks,
 * more details within.
 * <p>
 * AESTHETIC bug -> When the application is in GameState.PAUSED, and the PauseScreen is
 * overlaid the gameplay screen, I need the PauseScreen to update the UI based on user
 * input, so I have to access the surfaceHolder. But when the surfaceHolder is locked,
 * there is a visible "jiggle" of all gameplay entities. I'm certain that the
 * attributes of these entities is NOT actually being updated, and it's just a visual
 * disturbance, but still. I have no idea how to fix this.
 *
 * @author Dakotah Kurtz
 */

@SuppressLint("ViewConstructor")
public class GameView extends SurfaceView {

    private final SurfaceHolder surfaceHolder;

    private final static int MAX_FPS = 35; // works best at 30
    private final static int FRAME_PERIOD = 1000 / MAX_FPS;
    private boolean isRunning = false;

    private int displayWidth;
    private int displayHeight;
    public final static Paint FAILED_BITMAP_PAINT = new Paint(Color.BLACK);
    protected final static int OPTIONS_HEIGHT = 300;
    protected static int ABSOLUTE_PADDING = 20; // The very tiny UI bar that shows
    // battery life and the very thin black slice at the bottom of the table screen are
    // roughly this size.

    private GameState gameState;
    private final GameManager manager;
    private int maxLevel;

    SharedPreferences sharedPreferences;
    private boolean soundOn;
    private boolean guideEnabled;
    private boolean isTutorialEnabled;

    GameLoopThread gameLoopThread = new GameLoopThread();

    @SuppressLint("ClickableViewAccessibility")
    public GameView(Context context, @Nullable AttributeSet attrs,
                    SharedPreferences sharedPref) {

        super(context, attrs);

        // set touch listener to GameView
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                // send touches to GameManager and update GameState accordingly
                manager.trackInputEvent(event, x, y);
                gameState = manager.getGameState();
                return true;
            }
        });

        sharedPreferences = sharedPref;
        displayWidth = getResources().getDisplayMetrics().widthPixels;
        displayHeight = getResources().getDisplayMetrics().heightPixels;

        maxLevel = sharedPref.getInt(MainActivity.levelSave, 1);
        soundOn = sharedPref.getBoolean(MainActivity.soundSave, true);
        guideEnabled = sharedPref.getBoolean(MainActivity.guideLine, true);
        isTutorialEnabled = sharedPref.getBoolean(MainActivity.firstTime, true);

        manager = new GameManager(context, displayWidth,
                displayHeight, this);
        surfaceHolder = getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                // it's important not to start Thread spinning until the surface is
                // actually created, because not all objects in GameManager are
                // guaranteed to be initialized until now.
                if (gameLoopThread.getState() == Thread.State.NEW) {
                    isRunning = true;
                    gameLoopThread.start();
                } else {
                    if (gameLoopThread.getState() == Thread.State.TERMINATED) {
                        gameLoopThread = new GameLoopThread();
                        isRunning = (true);
                        gameLoopThread.start();
                    }
                }
            }

            // couldn't get things to work horizontally
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            /*
                Update the sharedPreferences when closing out application
             */
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                SharedPreferences.Editor editor = sharedPref.edit();
//        editor.clear();
                editor.putInt(MainActivity.levelSave, getMaxLevel());
                editor.apply();
            }
        });
    }

    /**
     * Set value of soundOn attribute
     */
    public void setSoundOn(boolean b) {
        soundOn = b;
    }

    /**
     * Set value of guideEnabled attribute
     */
    public void setGuideEnabled(boolean b) {
        guideEnabled = b;
    }

    /**
     * Return true if the sound is currently on
     */
    public boolean isSoundOn() {
        return soundOn;
    }

    /**
     * Return true if the tutorial is enabled
     */
    public boolean isTutorialEnabled() {
        return isTutorialEnabled;
    }

    /**
     * Return true if the guide-line is currently enabled
     */
    public boolean isGuideEnabled() {
        return guideEnabled;
    }

    /**
     * Return the current max level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Sets the value of isTutorialEnabled.
     */
    public void setTutorialEnabled(boolean b) {
        isTutorialEnabled = b;
    }

    /**
     * Set the value of the maxLevel field
     */
    public void setMaxLevel(int level) {
        maxLevel = level;
    }

    /**
     * Display the current canvas to the screen
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /**
     * Pause the game by stopping the game Thread for update the canvas and gameEngine
     */
    public void pause() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        boolean retry = true;
        // keep trying to restart the thread
        while (retry) {
            try {
                gameLoopThread.join();
                retry = false;
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Create a new GamePlay thread and start it when safe to do so.
     */
    public void resume() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        gameLoopThread = new GameLoopThread();
        isRunning = true;
        gameLoopThread.start();
    }

    /**
     * All responsibility for rendering a "safe" canvas is passed to GameManager
     */
    public void render(Canvas canvas) {
        manager.render(canvas);
    }

    /**
     * Private class to update the UI and gameEngine every frame.
     */
    private class GameLoopThread extends Thread {

        @Override
        public void run() {
            super.run();

            int skipCounter = 0; // how many frames have been skipped

            /* Implementing the surfaceHolder within the Thread in this way comes from
               http://www.java2s.com/Tutorials/Android/Android_UI_How_to/View
               /Draw_to_SurfaceView_with_thread.htm
               This source was immensely helpful in seeing what the framework should
               actually look like, but very little of the code found at the above link
               actually remains in this project.
            */
            while (isRunning) {

                // Make sure previous surface has been rendered
                if (!(surfaceHolder == null) && !surfaceHolder.getSurface().isValid()) {
                    continue;
                }

                long started = System.currentTimeMillis();

                assert surfaceHolder != null; // we just checked above^^
                Canvas canvas = surfaceHolder.lockCanvas();

                gameState = manager.getGameState();
                if (gameState == GameState.GAME_OVER || gameState == GameState.LEVEL_WON || gameState == GameState.PAUSED) {
                    // gameEngine requires us to pause
                    maxLevel = Math.max(maxLevel, manager.getLevel());
                    if (canvas != null) {
                        render(canvas); // render one last frame and post it
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                    pause();
                }

                manager.update(); // tick
                // render under normal circumstances
                if (canvas != null) {
                    render(canvas);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }

                // try to keep an even frame rate by slowing down if the application is
                // running too fast
                float deltaTime = (System.currentTimeMillis() - started);
                int sleepTime = (int) (FRAME_PERIOD - deltaTime);
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime);
                    } catch (InterruptedException ignored) {
                    }
                }

                // if instead we're behind schedule, update the game without rendering
                // (skip a frame)
                while (sleepTime < 0) {
                    manager.update();
                    sleepTime += FRAME_PERIOD;
                    skipCounter++;
                    Log.d("Expected", "Frame skipped: " + skipCounter);
                }
                skipCounter = 0;
            }
        }
    }
}