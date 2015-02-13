package com.example.flappybird;

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
    private final int PLAYER_SIZE = 100;

    /* This is the start x location, pipes start spawning at location 0 */
    private final int PLAYER_START_POSITION = -200;

    public GameView(Context context) {
        super(context);
        paint = new Paint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenHeight = h;
        screenWidth = w;

        playerY = screenHeight / 2;
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

        drawPlayer(canvas);
        drawPipes(canvas);
        update();
        checkCollisions();

        // This call triggers the screen to redraw (basically call onDraw
        // again).
        invalidate();
    }

    /*
     * Draw the pipes on screen.
     * 
     * This uses the index of the pipes as seed into the random number generator
     * in order to save the positions of pipes on screen. It would be better to
     * save this information in a list, but this is simpler.
     */
    private void drawPipes(Canvas canvas) {

        // Draw the pipes as green
        paint.setColor(Color.GREEN);

        // The world is broken up on the x axis into to PIPE_WIDTH sized
        // segments
        // like |x| | | | |x| | | | | |x|
        // every 5th segment has a pipe in it
        // these indexes are just estimates of the pipes we need to draw
        final int lowIndex = (distanceTraveled - screenWidth) / PIPE_WIDTH;
        final int highIndex = (int) ((distanceTraveled + 1.5 * screenWidth) / PIPE_WIDTH);
        for (int i = lowIndex; i < highIndex; i++) {
            if (i > 0 && i % 5 == 0) {

                // Seeding the random number generator with the index gives us
                // the same result
                // every time for a single pipe.
                Random random = new Random(i);
                int height = random.nextInt(screenHeight) - PIPE_OPENING_HEIGHT;

                int left = i * PIPE_WIDTH - distanceTraveled;

                // Draw top pipe
                Rect topPipeRect = new Rect(left, 0, left + PIPE_WIDTH, height);
                canvas.drawRect(topPipeRect, paint);

                // Draw bottom pipe
                Rect bottomPipeRect = new Rect(left, height
                        + PIPE_OPENING_HEIGHT, left + PIPE_WIDTH, screenHeight);
                canvas.drawRect(bottomPipeRect, paint);
            }
        }

    }

    /*
     * Draw the player as yellow rectangle
     */
    private void drawPlayer(Canvas canvas) {
        // draw the player as yellow
        paint.setColor(Color.YELLOW);

        float top = playerY;
        float bottom = top + PLAYER_SIZE;
        float left = 20;
        float right = left + PLAYER_SIZE;

        // Save where we drew the player for later in collisions
        playerRect = new RectF(left, top, right, bottom);
        canvas.drawRect(playerRect, paint);
    }

    /*
     * Collision detection, collide with all possible pipes and the top/bottom
     * of the screen
     */
    private void checkCollisions() {
        if (playerY > screenHeight || playerY < 0) {
            reset();
            return;
        }

        final int lowIndex = (distanceTraveled - screenWidth) / PIPE_WIDTH;
        final int highIndex = (int) ((distanceTraveled + 1.5 * screenWidth) / PIPE_WIDTH);
        for (int i = lowIndex; i < highIndex; i++) {
            if (i > 0 && i % 5 == 0) {
                Random random = new Random(i);
                int height = random.nextInt(screenHeight) - PIPE_OPENING_HEIGHT;

                int left = i * PIPE_WIDTH - distanceTraveled;

                // perform bounding box collision detection
                RectF topPipe = new RectF(left, 0, left + PIPE_WIDTH, height);
                RectF bottomPipe = new RectF(left,
                        height + PIPE_OPENING_HEIGHT, left + PIPE_WIDTH,
                        screenHeight);
                if (RectF.intersects(playerRect, topPipe)
                        || RectF.intersects(playerRect, bottomPipe)) {
                    reset();
                    return;
                }

            }
        }
    }

    /*
     * Reset to starting location
     */
    private void reset() {
        playerY = screenHeight / 2;
        distanceTraveled = -200;
        playerYVel = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            playerYVel = -400;
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
            playerYVel += mult * 1000;
            distanceTraveled += mult * 200;
        }
        lastFrame = currentTime;
    }
}
