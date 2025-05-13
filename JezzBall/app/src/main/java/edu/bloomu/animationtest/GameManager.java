package edu.bloomu.animationtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Primary class that handles the business logic for the application. Manages
 * GameState, where user touches go, and what gets rendered based on that GameState.
 * Handles all rendering and input events for GameState.GAME_ACTIVE, GameState.NEXT_LEVEL
 * and GameState.GAME_OVER.
 * Closely coupled with GameView class because GameManager needs access to the UI
 * thread and whether or not the GameEngine ticks.
 * Upon initialization, creates and loads images for all Screen objects, and updates
 * those Screens with information from the gameplay as needed.
 *
 * @author Dakotah Kurtz
 */

public class GameManager {
    private GameState gameState;

    private final Bitmap brickFillBitmap;
    private final Bitmap brickWallStoppedBitmap;
    private final Bitmap gameplay_options_buttons;
    private final Bitmap gameOverBitmap;
    private final Bitmap tutorialBitmap;

    private Bitmap ballBitmap;
    private Bitmap backgroundBitmap;

    private static ArrayList<Integer> backgrounds;

    private final Paint targetPaint;
    private final Button nextLevelButton;
    private final Button pauseButton;
    private final Button menuButton;

    private final TitleScreen titleScreen;
    private final PauseScreen pauseScreen;

    private final Context context;
    private final GameView gameView;

    private final GameEngine gameEngine;
    private int maxLevel;
    private int lives;
    private final int[][] gridRepresentation;
    private final float gridDimension;
    private final int displayWidth;
    private final int displayHeight;
    private final ArrayList<float[]> touchEventHistory = new ArrayList<>();

    protected static int TEXT_COLOR = Color.parseColor("#4f4c4c");
    protected static Typeface TYPEFACE;
    private final Paint textStyle;

    private boolean isSoundOn;
    private boolean isGuideEnabled;
    private boolean isTutorialEnabled;
    private boolean tutorialNeedsDisplayed;

    private final SoundPool soundPool;
    private final int gameOverSound;
    private final int lifeLostSound;
    private final int levelWonSound;


    public GameManager(Context context, int displayWidth, int displayHeight,
                       GameView gameView) {
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.context = context;
        this.gameView = gameView;
        maxLevel = gameView.getMaxLevel();

        isSoundOn = gameView.isSoundOn();
        isGuideEnabled = gameView.isGuideEnabled();
        isTutorialEnabled = gameView.isTutorialEnabled();
        tutorialNeedsDisplayed = isTutorialEnabled;

        /*
            Load audio
         */
        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
        soundPool = new SoundPool.Builder().setMaxStreams(3)
                .setAudioAttributes(audioAttributes).build();
        gameOverSound = soundPool.load(context, R.raw.game_over, 1);
        lifeLostSound = soundPool.load(context, R.raw.smash, 2);
        levelWonSound = soundPool.load(context, R.raw.level_complete, 1);

        targetPaint = new Paint();
        targetPaint.setColor(Color.WHITE);
        targetPaint.setStrokeWidth(20);

        TYPEFACE = ResourcesCompat.getFont(context, R.font.hotsweat);
        textStyle = new Paint();
        textStyle.setColor(TEXT_COLOR);
        textStyle.setTextSize(50);
        textStyle.setTypeface(TYPEFACE);

        gameEngine = new GameEngine(displayWidth, displayHeight);
        gameState = GameState.MENU_SCREEN;
        gridRepresentation = gameEngine.getGrid();
        gridDimension = gameEngine.getDimension();
        backgrounds = loadBackgrounds();

        titleScreen = new TitleScreen(context, 0, displayWidth, 0, displayHeight,
                maxLevel, isTutorialEnabled);
        pauseScreen = new PauseScreen(context, (int) (displayWidth * .2),
                (int) (displayWidth * .8), (int) (displayHeight * .35),
                (int) (displayHeight * .7), isSoundOn, isGuideEnabled, isTutorialEnabled);

        /*
            Load all final Bitmaps and Buttons needed. Although messy, this is done in
            constructor to allow all these fields to remain final.
         */
        tutorialBitmap = generateBitMap(context, R.drawable.game_tutorial,
                (int) (displayWidth * .9), (int) (displayHeight * .9));

        brickFillBitmap = generateBitMap(context, R.drawable.game_wall_fill,
                (int) gridDimension, (int) gridDimension);
        ballBitmap = generateBitMap(context, titleScreen.getSphereSelection(),
                gameEngine.getBallRadius() * 2, gameEngine.getBallRadius() * 2);
        backgroundBitmap = generateBitMap(context,
                backgrounds.get(gameEngine.getLevel() - 1), (int) displayWidth,
                (int) displayHeight);
        brickWallStoppedBitmap = generateBitMap(context, R.drawable.game_wall_broken
                , (int) gridDimension, (int) gridDimension);

        gameOverBitmap = generateBitMap(context, R.drawable.game_gameover,
                (int) (displayWidth * .9), (int) (displayHeight * .3));
        Bitmap menuBitmap = generateBitMap(context, R.drawable.pause_btn_menu,
                (int) (displayWidth * .3), (int) (displayHeight * .1));
        menuButton = new Button((int) (displayWidth * .35), (int) (displayHeight * .7),
                menuBitmap);

        Bitmap nextLevelBitmap = generateBitMap(context, R.drawable.game_btn_next,
                (int) (displayWidth * .8), (int) (displayWidth * .2));
        nextLevelButton = new Button((int) (displayWidth * .1),
                (int) ((displayHeight * .5) - (displayWidth * .1)),
                nextLevelBitmap);

        Bitmap pauseBitmap = generateBitMap(context, R.drawable.game_btn_pause,
                (int) (displayWidth * .2), (int) (GameView.OPTIONS_HEIGHT * .5));
        pauseButton = new Button((int) (displayWidth * .8),
                (int) (GameView.OPTIONS_HEIGHT * .25), pauseBitmap);


        gameplay_options_buttons = generateBitMap(context,
                R.drawable.game_header_bg, (int) displayWidth, GameView.OPTIONS_HEIGHT);
    }

    /**
     * Receives a MotionEvent from GameView and adds them to the TouchEventHistory
     * until the user stops drawing, at which point the ArrayList of "touches" is
     * passed to the appropriate Screen to handle based on the current GameState, (or
     * handled in house if displaying gameplay).
     */
    public void trackInputEvent(MotionEvent event, float x, float y) {

        touchEventHistory.add(new float[]{x, y});

        if (event.getAction() == MotionEvent.ACTION_UP) {
            processInputEvent(touchEventHistory);
            touchEventHistory.clear();
        }
    }

    /**
     * Process the set of input events after the user has finished with their touch,
     * pass to appropriate Screen based on current GameState. Update GameState
     * accordingly.
     */
    public void processInputEvent(ArrayList<float[]> touches) {

        switch (gameState) {
            case GAME_ACTIVE:
                tutorialNeedsDisplayed = false;
                if (pauseButton.clickedIn(touches)) {
                    gameState = GameState.PAUSED;
                    return;
                } else { // game is active, and they didn't click for pause, send to
                    // engine
                    gameEngine.interpretTouchEvent(touchEventHistory);
                }
                break;

            case LEVEL_WON:
                if (nextLevelButton.clickedIn(touches)) {
                    loadNextLevel();
                    gameState = GameState.GAME_ACTIVE;
                    gameView.resume(); // restart gameplay thread
                }
                break;

            case MENU_SCREEN:
                // pass to TitleScreen
                gameState = titleScreen.interpretTouch(touches, gameState);
                if (gameState == GameState.GAME_ACTIVE) {
                    // user clicked resume, so update the ball in case user chose a new
                    // one
                    ballBitmap = generateBitMap(context,
                            titleScreen.getSphereSelection(),
                            gameEngine.getBallRadius() * 2,
                            gameEngine.getBallRadius() * 2);
                    gameView.resume();
                }
                break;
            case PAUSED:
                if (pauseButton.clickedIn(touches)) {
                    return;
                }
                // pass responsibility to PauseScreen
                gameState = pauseScreen.interpretTouch(touches, gameState);

                if (gameState != GameState.PAUSED) {
                    // they unpaused, so update settings
                    isSoundOn = pauseScreen.isSoundOn();
                    isGuideEnabled = pauseScreen.isGuideEnabled();
                    isTutorialEnabled = pauseScreen.isTutorialEnabled();
                    gameView.setTutorialEnabled(isTutorialEnabled);
                    gameView.setGuideEnabled(isGuideEnabled);
                    gameView.setSoundOn(isSoundOn);
                }
                if (gameState == GameState.MENU_SCREEN) {
                    // update menu with latest information from gameplay
                    titleScreen.updateMaxLevel(maxLevel);
                }
                // necessary to update canvas with choices BUT everytime the user
                // touches, the GameView surfaceHolder is locked, and there is a
                // visible jiggle. I'm not sure of a way around this.
                gameView.resume();
                break;

            case GAME_OVER:
                if (menuButton.clickedIn(touches)) {
                    gameState = GameState.MENU_SCREEN;
                    titleScreen.updateMaxLevel(maxLevel);
                    gameEngine.newGame();
                    gameView.resume();
                }
        }
    }

    /**
     * Called by GameView every tick that the thread is running. Passes responsibility
     * for rendering surface to the appropriate Screen based on GameState.
     */
    public void render(Canvas canvas) {

        switch (gameState) {
            case MENU_SCREEN:
                titleScreen.render(canvas);
                break;
            case LEVEL_WON:
                renderLevelTransition(canvas);
                break;
            case PAUSED:
                pauseScreen.render(canvas);
                break;
            case GAME_ACTIVE:
                renderGamePlay(canvas);
                renderHeading(canvas);
                if (isGuideEnabled) {
                    renderWallOutline(canvas);
                }
                if (tutorialNeedsDisplayed) {
                    canvas.drawBitmap(tutorialBitmap, (int) (displayWidth * .05),
                            (int) (displayHeight * .1), GameView.FAILED_BITMAP_PAINT);
                }
                break;
            case GAME_OVER:
                renderGamePlay(canvas);
                renderHeading(canvas);
                renderGameOver(canvas);
        }
    }

    /**
     * Render the GameOver bitmap and a button to allow the user to return to the main
     * menu to try again.
     */
    private void renderGameOver(Canvas canvas) {
        canvas.drawBitmap(gameOverBitmap, 0, (float) (displayHeight * .3),
                GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(menuButton.getBitmap(), menuButton.getLeft(),
                menuButton.getTop(), GameView.FAILED_BITMAP_PAINT);
    }

    /**
     * If the user has the "helper" line enabled, show the direction and location where
     * a wall WOULD be drawn if the user were to lift their finger.
     * <p>
     * Duplicates some logic from GameEngine class, but I couldn't see a way to
     * separate it.
     */
    private void renderWallOutline(Canvas canvas) {
        // make sure intention to draw a wall is clear
        if (touchEventHistory.size() < GameEngine.WALL_TOUCH_INTENTION) {
            return;
        }

        float firstX = touchEventHistory.get(0)[0];
        float firstY = touchEventHistory.get(0)[1];
        float endX = touchEventHistory.get(touchEventHistory.size() - 1)[0];
        float endY = touchEventHistory.get(touchEventHistory.size() - 1)[1];

        double theta =
                Math.toDegrees(Math.atan2(Math.abs(firstY - endY),
                        Math.abs(firstX - endX)));

        // get scaled x and y coords
        int[] gridLocation = gameEngine.floatToGrid(firstX, firstY);
        int[][] grid = gameEngine.getGrid();
        if (grid[gridLocation[1]][gridLocation[0]] == -1 || grid[gridLocation[1]][gridLocation[0]] == 0) {
            return;
        }

        // horizontal
        if (theta <= 25) {
            canvas.drawLine(firstX - (endX - firstX), firstY, endX, firstY, targetPaint);
            // vertical
        } else if (theta >= 65) {
            canvas.drawLine(firstX, firstY - (endY - firstY), firstX, endY, targetPaint);
        }

    }

    /**
     * Render the heading that appears at the top of the screen showing the score,
     * lives, and pause Button. Check if the number of lives decreased, and play a
     * sound if so.
     */
    private void renderHeading(Canvas canvas) {
        canvas.drawBitmap(gameplay_options_buttons, 0, 0, GameView.FAILED_BITMAP_PAINT);

        int score = gameEngine.getScoreAsPercentage();
        canvas.drawText("score: " + score, 30, 100, textStyle);

        // if the number of lives just went down, play a sad sound.
        int lives = gameEngine.getLives();
        if (this.lives > lives) {
            soundPool.play(lifeLostSound, 1, 1, 0, 0, 1);
        }
        this.lives = lives;

        canvas.drawText("lives: " + lives, 30, 200, textStyle);
        canvas.drawBitmap(pauseButton.getBitmap(), pauseButton.getLeft(),
                pauseButton.getTop(), GameView.FAILED_BITMAP_PAINT);
    }

    /**
     * Display the unlocked image without the header so the image can be seen in full,
     * and the next level button.
     */
    private void renderLevelTransition(Canvas canvas) {
        canvas.drawBitmap(backgroundBitmap, 0, 0, GameView.FAILED_BITMAP_PAINT);
        canvas.drawBitmap(nextLevelButton.getBitmap(), nextLevelButton.getLeft(),
                nextLevelButton.getTop(),
                GameView.FAILED_BITMAP_PAINT);
    }

    /**
     * Render gameplay using information taken from the GameEngine.
     */
    private void renderGamePlay(Canvas canvas) {

        canvas.drawBitmap(backgroundBitmap, 0, 0, GameView.FAILED_BITMAP_PAINT);
        // go through the current gameEngine grid
        for (int i = 0; i < gridRepresentation.length; i++) {
            for (int j = 0; j < gridRepresentation[0].length; j++) {
                if (gridRepresentation[i][j] == -1) { // draw broken walls where needed
                    canvas.drawBitmap(brickWallStoppedBitmap, j * gridDimension,
                            i * gridDimension + GameView.OPTIONS_HEIGHT + GameView.ABSOLUTE_PADDING,
                            GameView.FAILED_BITMAP_PAINT);
                }
                // draw walls covering the "locked" portion of the game
                else if (gridRepresentation[i][j] != 0 && gridRepresentation[i][j] != -1) {
                    canvas.drawBitmap(brickFillBitmap, j * gridDimension,
                            i * gridDimension + GameView.OPTIONS_HEIGHT + GameView.ABSOLUTE_PADDING,
                            GameView.FAILED_BITMAP_PAINT);
                }

            }
        }

        // add the walls
        for (Wall wall : gameEngine.getWalls()) {
            if (wall.isMoving()) {
                canvas.drawRect(wall.getRect(), wall.paint);
            }
        }

        // add the balls
        for (Ball ball : gameEngine.getBalls()) {
            canvas.drawBitmap(ballBitmap, ball.getMatrix(), GameView.FAILED_BITMAP_PAINT);
        }
    }

    /**
     * This method is called by GameView every tick, while the GamePlay thread is
     * running. Updates the gameEngine, if the game is active, and updates the
     * GameState, and plays noise, if needed based of the state of the gameEngine.
     */
    public void update() {
        if (gameState == GameState.GAME_ACTIVE) {
            gameEngine.tick();
        }

        if (gameEngine.isGameOver()) { // either way we'll need to pause in GameView
            if (gameEngine.isBeatLevel()) {
                soundPool.play(levelWonSound, 1, 1, 0, 0, (float) 1.3);
                gameState = GameState.LEVEL_WON;
            } else if (gameEngine.getLives() == 0) {
                soundPool.play(gameOverSound, 1, 1, 0, 0, 1);
                gameState = GameState.GAME_OVER;
            }
        }
    }

    /**
     * Return the current level in play
     */
    public int getLevel() {
        return gameEngine.getLevel();
    }

    /**
     * Return the current GameState
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Returns a Bitmap registered to this specific context of the dimensions passed.
     * Static method so it can be used in the Screen classes. Essentially a shortcut
     * for the BitMapFactory methods.
     */
    protected static Bitmap generateBitMap(Context context, int id,
                                           int x, int y) {
        Bitmap bitmap = ((BitmapDrawable) Objects.requireNonNull(AppCompatResources.
                getDrawable(context, id))).getBitmap();

        return Bitmap.createScaledBitmap(bitmap, x, y,
                true);
    }

    /**
     * Returns the list of all available background drawables
     */
    protected static ArrayList<Integer> getBackgroundDrawables() {
        return backgrounds;
    }

    /*
        Increment level, update saved maxLevel and background
     */
    private void loadNextLevel() {
        gameEngine.nextLevel();
        maxLevel = Math.max(gameEngine.getLevel(), maxLevel);
        gameView.setMaxLevel(maxLevel);
        backgroundBitmap = generateBitMap(context, backgrounds.get(gameEngine.getLevel() - 1),
                displayWidth, displayHeight);
    }

    /*
        Helper method to de-clutter constructor. Loads all backgrounds into arraylist.
     */
    private ArrayList<Integer> loadBackgrounds() {
        ArrayList<Integer> backgrounds = new ArrayList<>();
        backgrounds.add(R.drawable.game_bg_almosttoday);
        backgrounds.add(R.drawable.game_bg_nearfuturefarm);
        backgrounds.add(R.drawable.game_bg_idyllicfarm);
        backgrounds.add(R.drawable.game_bg_freshstart);
        backgrounds.add(R.drawable.game_bg_cyperpunkcity);
        backgrounds.add(R.drawable.game_bg_friendlystrangers);
        backgrounds.add(R.drawable.game_bg_sudoprison);
        backgrounds.add(R.drawable.game_bg_sparechange);
        backgrounds.add(R.drawable.game_bg_research);
        backgrounds.add(R.drawable.game_bg_invasion);
        backgrounds.add(R.drawable.game_bg_failedattack);
        backgrounds.add(R.drawable.game_bg_collapse);
        return backgrounds;
    }
}
