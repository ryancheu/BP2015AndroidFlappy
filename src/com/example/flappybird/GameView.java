package com.example.flappybird;

import java.util.ArrayList;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    private float playerY = 0;
    private int distanceTraveled = -200;
    private float playerYVel = 0;
    private Paint paint;
    private long lastFrame = -1;

    private int screenWidth;
    private int screenHeight;

    private RectF playerRect;

    private final int PIPE_OPENING_HEIGHT = 350;
    private final int PIPE_WIDTH = 100;
    private final int DIST_BETWEEN_PIPE = 500;

    private final int PLAYER_WIDTH = 100;
    private final int PLAYER_HEIGHT = 70;
    private final int PLAYER_OFFSET = 20;

    /** List of all pipe heights, generated as we go **/
    private ArrayList<Integer> pipeHeights = new ArrayList<Integer>();

    private Random random;

    /** This is the start x location, pipes start spawning at location 0 **/
    private final int PLAYER_START_POSITION = -1000;

    private int nextPassedPipe = 0;

    private Bitmap birdBitmap;
    private Bitmap backgroundBitmap;

    private MediaPlayer flapSound;

    private boolean paused = false;

    public GameView(Context context) {
        super(context);
        paint = new Paint();
        random = new Random(System.currentTimeMillis());
        birdBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.bird);
        backgroundBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.background);

        flapSound = MediaPlayer.create(getContext(), R.raw.sfx_wing);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenHeight = h;
        screenWidth = w;
        reset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*
         * This is our main game loop, it performs both drawing and updating.
         * 
         * Usually this is advised against, and you're supposed to run the logic
         * in a separate thread, but we'll do this for simplicity.
         */

        drawBackgroud(canvas);
        drawPlayer(canvas);
        drawPipes(canvas);
        drawScore(canvas);

        if (!paused) {
            update();
            checkCollisions();
        }

        // This call triggers the screen to redraw (basically call onDraw
        // again).
        invalidate();
    }

    /**
     * Draw the pipes on screen.
     */
    private void drawPipes(Canvas canvas) {

        /*
         * The world is broken up on the x axis into to PIPE_WIDTH sized
         * segments like |x| | | | |x| | | | | |x|. Every 5th segment has a pipe
         * in it these indexes are just estimates of the pipes we need to draw.
         * 
         * Math is a little complex, not sure if this needs to be fixed.
         */

        for (int index : getPipesOnScreen()) {
            int height = getPipeHeight(index);
            int left = getPipeLeft(index);

            drawPipe(left, height, true, canvas);
            drawPipe(left, height, false, canvas);
        }

    }

    /**
     * @return a list of pipe indexes that are currently on screen
     */
    private ArrayList<Integer> getPipesOnScreen() {
        final int lowIndex = (distanceTraveled - screenWidth)
                / (PIPE_WIDTH + DIST_BETWEEN_PIPE);
        final int highIndex = (int) ((distanceTraveled + 1.5 * screenWidth) / (PIPE_WIDTH + DIST_BETWEEN_PIPE));
        ArrayList<Integer> returnPipes = new ArrayList<Integer>();
        for (int i = lowIndex; i < highIndex; i++) {
            if (i >= 0) {
                returnPipes.add(i);
            }
        }
        return returnPipes;
    }

    private int getPipeLeft(int index) {
        return index * (PIPE_WIDTH + DIST_BETWEEN_PIPE) - distanceTraveled;
    }

    private int getPipeHeight(int index) {
        if (pipeHeights.size() - 1 < index) {
            for (int i = pipeHeights.size() - 1; i < index; i++) {
                pipeHeights.add(random.nextInt(screenHeight
                        - PIPE_OPENING_HEIGHT));
            }
        }
        return pipeHeights.get(index);
    }

    private void drawPipe(int x, int height, boolean isTopPipe, Canvas canvas) {

        // Draw the pipes as green
        paint.setColor(Color.GREEN);

        Rect rect;
        if (isTopPipe) {
            rect = new Rect(x, 0, x + PIPE_WIDTH, height);
        } else {
            rect = new Rect(x, height + PIPE_OPENING_HEIGHT, x + PIPE_WIDTH,
                    screenHeight);
        }
        canvas.drawRect(rect, paint);
    }

    /*
     * Draw the player
     */
    private void drawPlayer(Canvas canvas) {
        // draw the player as yellow
        paint.setColor(Color.YELLOW);

        float top = playerY;
        float bottom = top + PLAYER_HEIGHT;
        float left = PLAYER_OFFSET;
        float right = left + PLAYER_WIDTH;

        // Save where we drew the player for later in collisions
        playerRect = new RectF(left, top, right, bottom);
        canvas.drawBitmap(birdBitmap, null, playerRect, paint);
        // canvas.drawRect(playerRect, paint);
    }

    private void drawScore(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setTextSize(50);
        canvas.drawText("Score: " + nextPassedPipe, 50, 50, paint);
    }

    private void drawBackgroud(Canvas canvas) {
        RectF backgroundArea = new RectF(0, 0, screenWidth, screenHeight);
        canvas.drawBitmap(backgroundBitmap, null, backgroundArea, paint);
    }

    /**
     * Collision detection, collide with all possible pipes and the top/bottom
     * of the screen.
     **/
    private void checkCollisions() {
        if (playerY > screenHeight || playerY < 0) {
            gameOver();
            return;
        }

        for (int index : getPipesOnScreen()) {
            int left = getPipeLeft(index);
            int height = getPipeHeight(index);

            // perform bounding box collision detection
            RectF topPipe = new RectF(left, 0, left + PIPE_WIDTH, height);

            RectF bottomPipe = new RectF(left, height + PIPE_OPENING_HEIGHT,
                    left + PIPE_WIDTH, screenHeight);

            if (RectF.intersects(playerRect, topPipe)
                    || RectF.intersects(playerRect, bottomPipe)) {
                gameOver();
                return;
            }
        }
    }

    private void gameOver() {
        showScoreDialog();
    }

    /*
     * Reset to starting location
     */
    private void reset() {
        lastFrame = -1;
        playerY = screenHeight / 2;
        distanceTraveled = PLAYER_START_POSITION;
        playerYVel = 0;
        pipeHeights = new ArrayList<Integer>();
        nextPassedPipe = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            playerYVel = -800;
            flapSound.seekTo(0);
            flapSound.start();
            break;
        }
        return true;
    }

    /*
     * This is the main update code, it updates the player position based on his
     * velocity, and adds gravity as well.
     */
    private void update() {

        long currentTime = System.currentTimeMillis();
        if (lastFrame != -1) {
            long diff = currentTime - lastFrame;
            float mult = ((float) diff / 1000f);
            playerY += mult * playerYVel;
            playerYVel += mult * 2000;
            distanceTraveled += mult * 200;

            int passingPipeRight = getPipeLeft(nextPassedPipe) + PIPE_WIDTH;
            if (passingPipeRight < PLAYER_OFFSET) {
                nextPassedPipe++;
            }
        }
        lastFrame = currentTime;
    }

    private void showScoreDialog() {
        paused = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage("Your score was: " + nextPassedPipe).setTitle(
                "Game over!");

        builder.setPositiveButton("Play Again",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        paused = false;
                        reset();
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();

        dialog.show();
    }
}
