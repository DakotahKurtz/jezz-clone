package edu.bloomu.animationtest;

import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class to handle all game logic, including tracking and updating of:
 * Score, lives and current level
 * Relative and absolute locations of all entities
 * Interaction between entities (erase a wall, collisions)
 * Touch screen input when application is in GameState.Game_Active state.
 * <p>
 * GameEngine object is created once and only once through sequence MainActivity ->
 * GameView -> GameManager -> GameEngine.
 * As such, updating levels or restarting after a game over is done by updating this
 * game object.
 *
 * @author Dakotah Kurtz
 */

public class GameEngine {

    public static final int WALL_TOUCH_INTENTION = 20;
    private float width;
    private float height;

    // Minimum dimension of gridRepresentation regardless of screen size
    private final static int NARROW_SIZE = 30;
    private final int wideSize;

    private final ArrayList<Ball> balls;
    private final ArrayList<Wall> walls;
    private ArrayList<Wall> movingWalls;
    private final int[][] gridRepresentation;
    private final int gridWall = -1; // represent walls in the grid with a -1
    private int partitionFill = 1; // begin counting at 1

    private final int ballRadius;
    private final float dimension;
    private final static int BALL_SPEED = 6;

    private final static int STARTING_LIVES = 3;
    private final static int STARTING_LEVEL = 1;
    private final static double WINNING_PERCENTAGE = .65;
    private final int winningScore;
    private int score;
    private int level;
    private int lives;
    private boolean gameOver;

    public GameEngine(int width, int height) {
        this.width = width;
        this.height = height;

        lives = STARTING_LIVES;
        level = STARTING_LEVEL;
        gameOver = false;

        walls = new ArrayList<>();
        balls = new ArrayList<>();

        // calculate dimensions of each grid square to fit the maximum number into the 
        // available screen, such that the narrow dimension is 30 and the absolute size
        // is as large as possible.
        float narrow = Math.min(width, height);
        float wide = Math.max(width, height) - GameView.OPTIONS_HEIGHT;
        dimension = narrow / NARROW_SIZE;
        wideSize = (int) (wide / dimension);
        gridRepresentation = new int[wideSize][NARROW_SIZE];

        ballRadius = (int) Math.floor(dimension * .9);
        winningScore = (int) (wideSize * NARROW_SIZE * WINNING_PERCENTAGE);

        init();
    }

    /**
     * Increment the level and add a life, clear the game board and update game-over so
     * the GameManager knows to start ticking again.
     */
    public void nextLevel() {
        level++;
        lives++;
        gameOver = false;

        init();
    }

    /**
     * Initialize game, both in constructor, upon a level completion, and after a game
     * over. Clears all ArrayLists, sets score to 0, returns gridRepresentation to
     * original state.
     */
    public void init() {

        score = 0;
        walls.clear();
        balls.clear();

        for (int i = 0; i < wideSize; i++) {
            for (int j = 0; j < NARROW_SIZE; j++) {
                gridRepresentation[i][j] = 1;
            }
        }

        movingWalls = new ArrayList<>(2);

        double dx;
        double dy;

        // balls are added with some randomness
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x, y;
        double theta;
        boolean intersects;
        // one ball per level
        for (int i = 0; i < level; i++) {
            do {
                intersects = false;
                x = random.nextInt((int) (ballRadius * 3),
                        (int) (width - (ballRadius * 3)));
                y = random.nextInt((int) ((ballRadius * 3) + GameView.OPTIONS_HEIGHT),
                        (int) (height - (ballRadius * 3)));
                // go through current list already added and ensure the next ball
                // doesn't overlap with any
                for (int j = 0; j < balls.size() && !intersects; j++) {
                    if (balls.get(j).getOval().intersects(x - ballRadius, y - ballRadius,
                            x + ballRadius, y + ballRadius)) {
                        intersects = true;
                    }
                }

            } while (intersects);
            // random angle for the ball to travel in, scale dx and dy based off of
            // angle and intended ball speed
            theta = random.nextDouble(.1, 3);
//                theta = 0;
            if (random.nextBoolean()) {
                theta *= -1;
            }
            dx = BALL_SPEED * Math.cos(theta);
            dy = BALL_SPEED * Math.sin(theta);

            Ball ball = new Ball(x, y, dx, dy, ballRadius);
            balls.add(ball);
        }
    }

    /**
     * Return current ArrayList of balls in play
     */
    public ArrayList<Ball> getBalls() {
        return balls;
    }

    /**
     * Return a copy of the current walls Arraylist. clone() is required to prevent
     * issues with two threads accessing the same ArrayList
     */
    public ArrayList<Wall> getWalls() {
        return (ArrayList<Wall>) walls.clone();
    }

    /**
     * Called every time the GameView thread updates when the GameManager's gameState
     * is GameState.GAME_ACTIVE. Updates all entities in the current game. If a moving
     * wall is hit by a ball, removes it. If no walls are moving, check to see if a new
     * portion of the map has been cleared.
     */
    public void tick() {
        Ball b;
        Wall w;
        ArrayList<Wall> toRemove = new ArrayList<>(); // track which walls need removed

        for (int i = 0; i < balls.size(); i++) {
            b = balls.get(i);
            // if a ball hits a ball, bounce
            for (int j = i; j < balls.size(); j++) {
                Ball.ballCollisionAdjustment(b, balls.get(j));
            }

            for (int j = 0; j < walls.size(); j++) {
                w = walls.get(j);
                // if a ball hits a wall
                if (RectF.intersects(b.getNext(), w.getRect())) {

                    /*
                     * Delete walls that are hit by a ball while moving
                     */
                    if (w.isMoving()) {
                        lives--;

                        if (!toRemove.contains(w)) {
                            toRemove.add(w);
                        }
                    } else {
                        // the ball needs to be reflected off the wall. Check whether 
                        // the ball has hit off the corner of a wall or a flat surface.
                        // if a corner, handle here, otherwise call 
                        // checkVertical/checkHorizontal methods
                        switch (w.getDirection()) {
                            case Left:
                                if (b.getX() - b.getRadius() > w.getRect().right) {
                                    b.setX(w.getRect().right + b.getRadius());
                                    b.reflectXaxis();
                                } else {
                                    checkVertical(b, w);
                                }
                                break;
                            case Right:
                                if (b.getX() + b.getRadius() < w.getRect().left) {
                                    b.setX(w.getRect().left - b.getRadius());
                                    b.reflectXaxis();

                                } else {
                                    checkVertical(b, w);
                                }
                                break;
                            case Up:
                                if (b.getY() - b.getRadius() > w.getRect().bottom) {
                                    b.setY(w.getRect().bottom + b.getRadius());
                                    b.reflectYaxis();
                                } else {
                                    checkHorizontal(b, w);
                                }
                                break;
                            case Down:
                                if (b.getY() + b.getRadius() < w.getRect().top) {
                                    b.setY(w.getRect().top - b.getRadius());
                                    b.reflectYaxis();
                                } else {
                                    checkHorizontal(b, w);
                                }
                                break;
                        }
                    }
                }
            }

            b.move((int) width, (int) height);
        }

        /*
         * If one wall was hit, remove it's pair if the pair is still moving
         */
        if (!toRemove.isEmpty()) {
            int i;

            for (i = 0; i < walls.size(); i++) {
                if (walls.get(i).isMoving()) {
                    toRemove.add(walls.get(i));
                }
            }

            walls.removeAll(toRemove);
            movingWalls.removeAll(toRemove);
        }

        /*
            Move walls, update list if they've stopped moving. Walls are tracked in 
            this convoluted way to make sure that:
            1. New partitions are only created when all walls have stopped moving, ONCE
            2. Walls are not added to grid more than once.
         */
        if (!movingWalls.isEmpty()) {
            for (int i = 0; i < movingWalls.size(); i++) {
                if (movingWalls.get(i).isMoving()) {
                    movingWalls.get(i).move(width, height, walls);
                }
            }

            int notMoving = 0;
            for (int i = 0; i < movingWalls.size(); i++) {
                if (!movingWalls.get(i).isMoving()) {
                    notMoving++;
                    // ensure walls are only drawn once
                    if (!movingWalls.get(i).isDrawn()) {
                        movingWalls.get(i).setDrawn(true);
                        addWallToGrid(movingWalls.get(i));
                    }
                }
            }
            // only add partitions when all walls have stopped moving
            if (notMoving == movingWalls.size()) {
                for (int i = 0; i < movingWalls.size(); i++) {
                    checkPartition(movingWalls.get(i));
                }
                movingWalls.clear();
            }

        }

        if (lives <= 0 || score > winningScore) {
            gameOver = true;
        }

    }


    /**
     * Return the score as a percentage of the winning score for displaying to user
     */
    public int getScoreAsPercentage() {
        return (int) Math.floor((score / (double) winningScore) * 100);
    }

    /**
     * Interpret touch events when GameState in GameManager is GAME_ACTIVE. Add a new
     * set of walls to movingWalls if the touchEventHistory is valid (mostly horizontal
     * or vertical line drawn, with length great enough the user made their intention
     * clear). Only one set of walls can be "moving" at a given time.
     */
    public void interpretTouchEvent(ArrayList<float[]> touchEventHistory) {
        if (touchEventHistory.size() < WALL_TOUCH_INTENTION) {
            return;
        }

        // only one set of walls can moving at once, don't allow another to begin until
        // the previous set is finished.
        for (Wall wall : walls) {
            if (wall.isMoving()) {
                return;
            }
        }

        float firstX = touchEventHistory.get(0)[0];
        float firstY = touchEventHistory.get(0)[1];
        float endX = touchEventHistory.get(touchEventHistory.size() - 1)[0];
        float endY = touchEventHistory.get(touchEventHistory.size() - 1)[1];

        double theta =
                Math.toDegrees(Math.atan2(Math.abs(firstY - endY),
                        Math.abs(firstX - endX)));

        // if the wall isn't clearly meant to be vertical or horizontal, don't draw it.
        if (theta > 25 && theta < 65) {
            return;
        }

        // convert touch into grid coordinates, walls cannot be drawn "between" grids
        firstX = (float) (Math.floor(firstX / dimension) * dimension);
        firstY = (float) (Math.floor(firstY / dimension) * dimension);
        int floatToGridX = scaleToGridX(firstX);
        int floatToGridY = scaleToGridY(firstY);

        // can't draw wall out of bounds
        if (gridRepresentation[floatToGridY][floatToGridX] == gridWall || gridRepresentation[floatToGridY][floatToGridX] == 0) {
            return;
        }

        Wall w1;
        Wall w2;

        if (theta <= 25) { // draw horizontal set
            w1 = new Wall(firstX, firstY, dimension, Direction.Left, Color.RED);
            w2 = new Wall(firstX + dimension, firstY, dimension,
                    Direction.Right,
                    Color.BLUE);
        } else if (theta >= 65) { // draw vertical set
            w1 = new Wall(firstX, firstY, dimension, Direction.Down, Color.RED);
            w2 = (new Wall(firstX, firstY - dimension, dimension, Direction.Up,
                    Color.BLUE));
        } else {
            return;
        }

        movingWalls.add(w1);
        movingWalls.add(w2);
        walls.addAll(movingWalls);
    }

    /**
     * After a game over, reset starting values and initialize a new game state.
     */
    public void newGame() {
        level = STARTING_LEVEL;
        lives = STARTING_LIVES;
        score = 0;
        gameOver = false;
        init();
    }

    /**
     * Return the grid representation of the current board state
     */
    public int[][] getGrid() {
        return gridRepresentation;
    }

    /**
     * Returns the current actual dimension of each square in the gridRepresentation
     */
    public float getDimension() {
        return dimension;
    }

    /**
     * Return true if the game is over, or if the current level has been completed
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Return true if the current level has been completed. The game needs to be paused
     * regardless of GameState.GAME_OVER or GameState.LEVEL_WON, so we need a separate
     * method to distinguish what to display.
     */
    public boolean isBeatLevel() {
        return score > winningScore;
    }

    /**
     * Return the current level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Return the current ball radius
     */
    public int getBallRadius() {
        return ballRadius;
    }

    /**
     * Return the current number of lives
     */
    public int getLives() {
        return lives;
    }

    /**
     * Performs the full conversion from float to grid coordinates, has use in
     * GameManager class to determine when to display the helper line. CANNOT be used
     * in GameEngine interpretTouch method, because the conversion is a two step
     * process and Java only passes primitives by value.
     */
    protected int[] floatToGrid(float x, float y) {
        int[] grid = new int[2];
        float firstX = x;
        float firstY = y;

        firstX = (float) (Math.floor(firstX / dimension) * dimension);
        firstY = (float) (Math.floor(firstY / dimension) * dimension);

        grid[0] = scaleToGridX(firstX);
        grid[1] = scaleToGridY(firstY);

        return grid;
    }

    /*
        If a ball bounces off a vertical wall, determine the type of horizontal 
        reflection to perform
     */
    private void checkHorizontal(Ball b, Wall w) {
        if (b.getDx() < 0) { // moving left
            b.setX(w.getRect().right + b.getRadius());
        } else {
            b.setX(w.getRect().left - b.getRadius());
        }

        b.reflectXaxis();
    }

    /*
        When a ball bounces off a horizontal wall, determine the type of vertical 
        reflection to perform
     */
    private void checkVertical(Ball b, Wall w) {
        if (b.getDy() > 0) { // moving down
            b.setY(w.getRect().top - b.getRadius());
        } else {
            b.setY(w.getRect().bottom + b.getRadius());
        }
        b.reflectYaxis();
    }

    /*
        After a wall has finished moving, check on both sizes of it to see if the wall 
        has divided the grid into a subdivision that doesn't contain any balls. If that
        is the case, fill the new partition.
     */
    private void checkPartition(Wall wall) {
        RectF rectF = wall.getRect();
        int top = scaleToGridY(rectF.top);
        int bottom = scaleToGridY(rectF.bottom);
        int left = scaleToGridX(rectF.left);
        int right = scaleToGridX(rectF.right);

        switch (wall.getDirection()) {
            case Left:
                // check below
                flood(bottom, left, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                // check above
                flood(top - 1, left, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                break;
            case Right:
                // check below
                flood(bottom, right - 1, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                // check above
                flood(top - 1, right - 1, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                break;
            case Up:
                // check the left
                flood(top, left - 1, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                // and the right
                flood(top, right, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                break;
            case Down:
                // check the left
                flood(bottom - 1, left - 1, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                    return;
                }
                // and the right
                flood(bottom - 1, right, ++partitionFill);
                if (isBallFree(partitionFill)) {
                    fillPartition(partitionFill);
                }
        }
    }

    /*
        If a partition has no balls in it, the fill the partition with 0's so the 
        GameManager can render it as "unlocked".
        Add points for every grid unlocked.
        Check to see if the level is complete.
     */
    private void fillPartition(int section) {
        for (int i = 0; i < gridRepresentation.length; i++) {
            for (int j = 0; j < gridRepresentation[0].length; j++) {
                if (gridRepresentation[i][j] == section) { // section is how the 
                    // partition is tracked (distinguished from the rest of the grid)
                    gridRepresentation[i][j] = 0;
                    score++;
                }
            }
        }

        if (score >= winningScore) {
            gameOver = true;
        }
    }

    /*
        Return true if a given partition has no balls inside it
     */
    private boolean isBallFree(int partitionFill) {
        for (Ball ball : balls) {
            if (gridRepresentation[scaleToGridY((float) ball.getY())][scaleToGridX((float) ball.getX())] == partitionFill) {
                return false;
            }
        }
        return true;
    }

    /*
        Recursively flood from the given y, x location until the given enclosed area 
        has the same "fill" int.
     */
    private void flood(int y, int x, int fill) {
        // don't go out of bounds
        if (y >= gridRepresentation.length || y < 0 || x >= gridRepresentation[0].length || x < 0) {
            return;
        }
        // don't fill a wall or waste time overwriting the current fill
        if (isFloodableSquare(y, x, fill)) {
            gridRepresentation[y][x] = fill; // add fill
            flood(y + 1, x, fill); // flood
            flood(y - 1, x, fill);
            flood(y, x + 1, fill);
            flood(y, x - 1, fill);

        }
    }

    /*
        Return true if the given y, x is:
        1. Not already filled with this fille
        2. Is not a wall
        3. Is not already flooded
     */
    private boolean isFloodableSquare(int y, int x, int fill) {
        return gridRepresentation[y][x] != gridWall && gridRepresentation[y][x] != fill
                && gridRepresentation[y][x] != 0;
    }

    /*
        Now that the wall has stopped moving, add to the grid and update 
        the game score.
     */
    private void addWallToGrid(Wall wall) {
        RectF rectF = wall.getRect();
        int top = scaleToGridY(rectF.top);
        int bottom = scaleToGridY(rectF.bottom);
        int left = scaleToGridX(rectF.left);
        int right = scaleToGridX(rectF.right);

        for (int i = top; i < bottom; i++) {
            for (int j = left; j < right; j++) {
                gridRepresentation[i][j] = gridWall;
                score++;
            }
        }
    }

    /*
     * Helper method to turn a float into grid coordinates
     */
    private int scaleToGridX(float x) {
        return (int) Math.floor(x / dimension);
    }

    /*
     * Helper method to turn a float into grid coordinates
     */
    private int scaleToGridY(float y) {
        return (int) Math.floor((y - GameView.OPTIONS_HEIGHT) / dimension);
    }

    /*
     * To be used exclusively for testing purposes within GameEngine class
     */
    private void printGrid() {
        String test = "\n  ";

        for (int i = 0; i < wideSize; i++) {
            test += "\n";

            for (int j = 0; j < NARROW_SIZE; j++) {
                test += gridRepresentation[i][j] + " ";
            }
        }
        Log.wtf("GRID", test);
    }
}