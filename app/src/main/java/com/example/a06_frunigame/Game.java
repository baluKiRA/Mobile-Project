package com.example.a06_frunigame;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.content.res.ResourcesCompat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class Game extends View {
    float screenX, screenY;
    private final Paint paint;
    private final Typeface customFont;
    private Bitmap background;
    private final Bitmap[] shapeImages = new Bitmap[3];
    private final ArrayList<Shape> shapeList;
    private Bitmap matchingShape;
    private int matchingShapeX, matchingShapeY;
    private float baseSpeed = 5;
    private long lastSpeedIncreaseTime;
    private SoundPool soundPool;
    private int tapSound;
    private int score = 0;
    private int lives = 5;
    private int highScore = 0;
    private int currentMaxScore = 0;
    private boolean isGameOver = false;

    public Game(Context context) {
        super(context);
        paint = new Paint();
        loadHighScore();
        initializeSound(context);
        customFont = ResourcesCompat.getFont(context, R.font.bahnschrift);
        setBackground(context);
        setShapesImages();
        shapeList = new ArrayList<>();
    }

    private void loadHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("gamePrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
    }

    private void saveHighScore() {
        SharedPreferences prefs = getContext().getSharedPreferences("gamePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highScore", highScore);
        editor.apply();
    }

    private void initializeSound(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        tapSound = soundPool.load(context, R.raw.tap_sound, 1);
    }

    private void setBackground(Context context) {
        Bitmap bckg = Bitmap.createBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.background));

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);

        screenX = size.x;
        screenY = size.y;
        background = Bitmap.createScaledBitmap(bckg, size.x, size.y,false);
    }

    private void setShapesImages() {
        shapeImages[0] = scaleBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.circle), 0.1f);
        shapeImages[1] = scaleBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.rectangle), 0.1f);
        shapeImages[2] = scaleBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.donut), 0.1f);

        matchingShape = shapeImages[(int)(Math.random() * 3)];
        matchingShapeX = (int) (screenX / 2 - matchingShape.getWidth() / 2);
        matchingShapeY = (int) (screenY - matchingShape.getHeight() - (screenY/15));
    }

    private Bitmap scaleBitmap(Bitmap original, float scale) {
        int width = Math.round(original.getWidth() * scale);
        int height = Math.round(original.getHeight() * scale);
        return Bitmap.createScaledBitmap(original, width, height, false);
    }

    public void update() {
        if (lives <= 0) {
            isGameOver = true;
            return;
        }

        updateSpeed();
        addNewShapes();
        moveShapes();
    }

    private void updateSpeed() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpeedIncreaseTime >= 1000) {
            float speedIncreaseRate = 0.05f;
            baseSpeed += speedIncreaseRate;
            lastSpeedIncreaseTime = currentTime;
        }
    }

    private void addNewShapes() {
        if (shapeList.size() < 10) {
            if (Math.random() > 0.9) {
                Bitmap randomShapeImage = shapeImages[(int)(Math.random() * 3)];
                float randomSpeed = baseSpeed + (float)(Math.random() * baseSpeed);
                shapeList.add(new Shape(
                        40 + (int)(Math.random() * (screenX - randomShapeImage.getWidth() - 40)),
                        -randomShapeImage.getHeight(),
                        randomShapeImage,
                        randomSpeed));
            }
        }
    }

    private void moveShapes() {
        boolean matchingShapePresent = false;

        if (shapeList.size() > 0) {
            for (int i = shapeList.size() - 1; i >= 0; i--) {
                Shape shape = shapeList.get(i);
                shape.move();

                if (shape.getShapeBitmap().sameAs(matchingShape)) {
                    matchingShapePresent = true;
                }

                if (shape.getY() > screenY - (screenY / 10)) {
                    if (shape.getShapeBitmap() == matchingShape) {
                        lives -= 1;
                    }
                    shapeList.remove(i);
                }
            }

            if (!matchingShapePresent) {
                selectNewMatchingShape();
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver) {
            handleGameOver(event);
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            handleShapeClick(event);
        }
        return true;
    }

    private void handleGameOver(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (currentMaxScore > highScore) {
            highScore = currentMaxScore;
            saveHighScore();
        }

        if (isWithinBounds(x, y, screenX / 3, screenX / 3 * 2, screenY / 2 + 150, screenY / 2 + 300)) {
            soundPool.play(tapSound, 1, 1, 1, 0, 1);
            restartGame();
        }
    }

    private void handleShapeClick(MotionEvent event) {
        float clickX = event.getX();
        float clickY = event.getY();

        for (int i = shapeList.size() - 1; i >= 0; i--) {
            Shape shape = shapeList.get(i);
            Bitmap shapeBitmap = shape.getShapeBitmap();

            if (isClose(clickX, clickY, shape.getX(), shape.getY(), shapeBitmap.getWidth(), shapeBitmap.getHeight())) {
                processShapeClick(shape, i);
                break;
            }
        }

        invalidate();
    }

    private void processShapeClick(Shape shape, int index) {
        soundPool.play(tapSound, 1, 1, 1, 0, 1);

        if (shape.getShapeBitmap() == matchingShape) {
            score += 10;
        } else {
            score -= 5;
        }

        if (score > currentMaxScore) {
            currentMaxScore = score;
        }

        shapeList.remove(index);
    }

    private boolean isWithinBounds(float x, float y, float left, float right, float top, float bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private void selectNewMatchingShape() {
        matchingShape = shapeImages[(int)(Math.random() * 3)];
    }

    private boolean isClose(float ax, float ay, float bx, float by, float shapeWidth, float shapeHeight) {
        return ax >= bx && ax <= bx + shapeWidth && ay >= by && ay <= by + shapeHeight;
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(background, 0, 0, paint);
        drawScoreAndLives(canvas);
        drawShapes(canvas);
        drawMatchingShape(canvas);

        if (isGameOver) {
            drawDimmingEffect(canvas);
            drawGameOverScreen(canvas);
        }
    }

    private void drawScoreAndLives(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(80);
        paint.setTypeface(customFont);

        String scoreText = "Score: " + score;
        String livesText = "Lives: " + lives;
        float padding = 50;
        float scoreX = padding;
        float livesX = screenX - paint.measureText(livesText) - padding;
        float positionY = screenY / 15;

        canvas.drawText(scoreText, scoreX, positionY, paint);
        canvas.drawText(livesText, livesX, positionY, paint);
    }

    private void drawShapes(Canvas canvas) {
        for (Shape shape : shapeList) {
            canvas.drawBitmap(shape.getShapeBitmap(), shape.getX(), shape.getY(), paint);
        }
    }

    private void drawMatchingShape(Canvas canvas) {
        drawMatchingShapeHighlight(canvas);
        canvas.drawBitmap(matchingShape, matchingShapeX, matchingShapeY, paint);
    }

    private void drawMatchingShapeHighlight(Canvas canvas) {
        Paint highlightPaint = new Paint();
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setAlpha(100);
        highlightPaint.setStyle(Paint.Style.FILL);

        int padding = 20;
        float cornerRadius = 20f;

        float left = matchingShapeX - padding;
        float top = matchingShapeY - padding;
        float right = matchingShapeX + matchingShape.getWidth() + padding;
        float bottom = matchingShapeY + matchingShape.getHeight() + padding;

        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, highlightPaint);
    }

    private void drawDimmingEffect(Canvas canvas) {
        Paint dimPaint = new Paint();
        dimPaint.setColor(Color.BLACK);
        dimPaint.setAlpha(150);
        canvas.drawRect(0, 0, screenX, screenY, dimPaint);
    }

    private void drawGameOverScreen(Canvas canvas) {
        drawGameOverTitle(canvas);
        drawRestartButton(canvas);
        drawScore(canvas);
        drawHighScore(canvas);
    }

    private void drawGameOverTitle(Canvas canvas) {
        paint.setTextSize(150);
        paint.setTypeface(customFont);
        paint.setColor(Color.WHITE);

        String gameOverText = "Game over";
        float gameOverX = (screenX - paint.measureText(gameOverText)) / 2;
        canvas.drawText(gameOverText, gameOverX, screenY / 2, paint);
    }

    private void drawRestartButton(Canvas canvas) {
        float buttonLeft = (screenX / 2) - 160;
        float buttonRight = (screenX / 2) + 160;
        float buttonTop = screenY / 2 + 150;
        float buttonBottom = screenY / 2 + 300;

        Paint buttonPaint = createButtonPaint();
        drawRoundedRect(canvas, buttonLeft, buttonTop, buttonRight, buttonBottom, buttonPaint);

        drawRestartText(canvas, buttonLeft, buttonRight, buttonTop, buttonBottom);
    }

    private Paint createButtonPaint() {
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.WHITE);
        buttonPaint.setAlpha(128);
        buttonPaint.setStyle(Paint.Style.FILL);
        return buttonPaint;
    }

    private void drawRoundedRect(Canvas canvas, float left, float top, float right, float bottom, Paint paint) {
        Path path = new Path();
        float cornerRadius = 20;
        path.addRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.drawPath(path, paint);
    }

    private void drawRestartText(Canvas canvas, float buttonLeft, float buttonRight, float buttonTop, float buttonBottom) {
        paint.setColor(Color.WHITE);
        paint.setTextSize(80);

        String restartText = "Restart";
        float restartTextWidth = paint.measureText(restartText);
        float restartX = buttonLeft + (buttonRight - buttonLeft - restartTextWidth) / 2;
        float restartY = (buttonTop + (buttonBottom - buttonTop) / 2 + (paint.getTextSize() / 2)) - 10;

        canvas.drawText(restartText, restartX, restartY, paint);
    }

    private void drawScore(Canvas canvas) {
        paint.setTextSize(80);
        paint.setTypeface(customFont);
        paint.setColor(Color.WHITE);

        String highScoreText = "Score: " + score;
        float highScoreX = (screenX - paint.measureText(highScoreText)) / 2;
        float highScoreY = screenY / 2 + 100;
        canvas.drawText(highScoreText, highScoreX, highScoreY, paint);
    }

    private void drawHighScore(Canvas canvas) {
        paint.setTextSize(80);
        paint.setTypeface(customFont);
        paint.setColor(Color.WHITE);

        String highScoreText = "High Score: " + highScore;
        float highScoreX = (screenX - paint.measureText(highScoreText)) / 2;
        float highScoreY = (5 * screenY) / 6;
        canvas.drawText(highScoreText, highScoreX, highScoreY, paint);
    }

    private void restartGame() {
        isGameOver = false;
        lives = 5;
        score = 0;
        currentMaxScore = 0;
        shapeList.clear();
        baseSpeed = 5;
        lastSpeedIncreaseTime = System.currentTimeMillis();
        matchingShape = shapeImages[(int) (Math.random() * 3)];
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}