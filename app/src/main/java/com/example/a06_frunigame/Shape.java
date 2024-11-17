package com.example.a06_frunigame;

import android.graphics.Bitmap;

public class Shape {
    private final float positionX;
    private float positionY;
    private final float speed;
    private final Bitmap shapeBitmap;

    public Shape(float positionX, float positionY, Bitmap asteroidBitmap, float speed) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.shapeBitmap = asteroidBitmap;
        this.speed = speed;
    }

    public float getX() {
        return positionX;
    }

    public float getY() {
        return positionY;
    }

    public Bitmap getShapeBitmap() {
        return shapeBitmap;
    }

    public void move() {
        positionY += speed;
    }
}
