package com.karacasoft.colorpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Triforce on 29.12.2014.
 */
public class ColorPickerView extends View {

    public interface OnColorPickListener{
        void onColorPick(int color);
    }

    public interface OnPinMoveListener{
        void onPinMove(int color, float x, float y);
    }

    public interface OnBrightnessHandleSlideListener {
        void onSlide(int brightness);
    }

    private OnColorPickListener onColorPickListener;
    private OnPinMoveListener onPinMoveListener;
    private OnBrightnessHandleSlideListener onBrightnessHandleSlideListener;

    private float pinScale = 1f;
    private long scaleAnimTimer = 0;
    private float[] scaleAnimInterpolator = {
        0.1f, 0.1f, 0.2f, 0.2f, 0.3f, 0.4f, 0.6f, 0.8f, 1f, 1f,
        1.1f, 1.1f, 1.1f, 1.1f, 1.1f, 1.2f, 1.2f, 1.2f, 1.1f, 1.0f
    };
    private Runnable pinScaleAnimator = new Runnable() {
        @Override
        public void run() {

            long dTime =  System.currentTimeMillis() - scaleAnimTimer;
            float timeRatio = (float)dTime / 400;

            pinScale = timeRatio * scaleAnimInterpolator[Math.max(0, Math.min(19, (int) (timeRatio * 20)))];
            if(pinScale < 1)
            {
                postDelayed(this, 10);
            }else{
                pinScale = 1f;
            }
            postInvalidate();
        }
    };

    private int initialRotate = 0;
    private int desiredRotate = 0;
    private int pinRotate = 0;
    private long rotateAnimTimer = 0;
    private boolean rotateAnimStarted = false;
    private Runnable pinRotateAnimator = new Runnable() {
        @Override
        public void run() {

            long dTime = System.currentTimeMillis() - rotateAnimTimer;
            float timeRatio = Math.min((float) dTime / 150, 1f);

            pinRotate = initialRotate + (int)((desiredRotate - initialRotate) * timeRatio);

            if(desiredRotate != pinRotate)
            {
                postDelayed(this, 10);
            }else{
                rotateAnimStarted = false;
            }
            postInvalidate();
        }
    };

    private float circleCenterX = 0;
    private float circleCenterY = 0;

    private float circleRadius;

    private Paint circlePaint;
    private Paint lightPaint;
    private Paint linePaint;

    private int brightness = 255;

    private Bitmap brightnessPicker;
    private Bitmap colorSelector;

    public ColorPickerView(Context context) {
        super(context);
        initializeView();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
    }

    private void initializeView()
    {
        circleRadius = (getWidth()) / 2;
        initializePaint();
        initializeBitmaps();
    }

    private void initializeBitmaps()
    {
        brightnessPicker = BitmapFactory.decodeResource(this.getResources(), R.drawable.brightness_handle);
        colorSelector = BitmapFactory.decodeResource(this.getResources(), R.drawable.color_selector);
    }

    private void initializePaint()
    {
        circlePaint = new Paint();

        int[] colors = {
                Color.HSVToColor(new float[]{0f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{60f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{120f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{180f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{240f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{300f, 1f, brightness / 255f}),
                Color.HSVToColor(new float[]{0f, 1f, brightness / 255f}),
        };


        circleCenterX = circleRadius + getPaddingLeft();
        circleCenterY = circleRadius + getPaddingTop();

        SweepGradient circleGradient = new SweepGradient(circleCenterX,
                circleCenterY,
                colors,
                null);

        circlePaint.setShader(circleGradient);
        circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
//        circlePaint.setDither(true);

        lightPaint = new Paint();

        linePaint = new Paint();

        int[] colors2 = {
                Color.rgb(0, 0, 0),
                Color.rgb(255, 255, 255)
        };

        LinearGradient lineGradient = new LinearGradient(0, circleCenterY - circleRadius, 0, circleCenterY + circleRadius, colors2, null, Shader.TileMode.REPEAT);

        int[] radialColors = {
                Color.HSVToColor(new float[]{0, 0, brightness / 255f}),
                Color.argb(0, 0, 0, 0)
        };

        float[] radialStops = {
            0.0f, 1.0f
        };

        RadialGradient radialGradient = new RadialGradient(circleCenterX, circleCenterY, circleRadius + 1, radialColors, radialStops, Shader.TileMode.REPEAT);

        lightPaint.setShader(radialGradient);


        linePaint.setShader(lineGradient);
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    private boolean brightnessHandlePicked = false;
    private boolean colorPickerPlaced = false;

    private float colorPickerX = 0;
    private float colorPickerY = 0;

    private int pickedColor = Color.BLACK;

    private static double distance(double x1, double x2, double y1, double y2)
    {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private void setColorPickerPosition(double x, double y)
    {
        double distance;
        if((distance = distance(circleCenterX, x, circleCenterY, y)) > circleRadius)
        {
            colorPickerX = circleCenterX + (float)((x - circleCenterX) * circleRadius / distance);
            colorPickerY = circleCenterY + (float)((y - circleCenterY) * circleRadius / distance);
        } else {
            colorPickerX = (float) x;
            colorPickerY = (float) y;
        }


    }

    private int pickColor(double y, double x)
    {
        double distance = distance(circleCenterX, colorPickerX, circleCenterY, colorPickerY);
        double angleRad = Math.atan2(y, x);
        double angle = Math.toDegrees(angleRad);
        angle = (angle < 0) ? angle + 360 : angle;
        //Log.d("ColorPickerView", "Angle: " + angle);

        return pickedColor = Color.HSVToColor(new float[]{(float)angle, (float)(distance / circleRadius), (float)brightness / 255});

//        double ratio = (angle + Math.PI) / (2 * Math.PI);
//        if(ratio > 0.5 && ratio < 0.83)
//        {
//
//            double effectiveRatio = ratio - 0.5;
//            Color.HSVToColor(new float[]{})
//            pickedColor = Color.rgb((int)(brightness * (0.33 - effectiveRatio) / 0.33), (int)(brightness * effectiveRatio / 0.33), (int)((distance / circleRadius) * 255));
//        }else if(ratio > 0.83 || ratio < 0.16)
//        {
//            if(ratio < 0.2)
//            {
//                ratio += 1;
//            }
//            double effectiveRatio = ratio - 0.83;
//            pickedColor = Color.rgb((int)((distance / circleRadius) * 255),(int)(brightness * (0.33 - effectiveRatio) / 0.33), (int)(brightness * effectiveRatio / 0.33));
//        }else{
//            double effectiveRatio = ratio - 0.16;
//            pickedColor = Color.rgb((int)(brightness * (effectiveRatio) / 0.34), (int)((distance / circleRadius) * 255),(int)(brightness * (0.34 - effectiveRatio) / 0.34));
//        }
//        return pickedColor;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float brightnessPickPositionX1 = circleCenterX + circleRadius + 50;
        float brightnessPickPositionX2 = circleCenterX + circleRadius + 100;

        float brightnessPickPositionY1 = circleCenterY - circleRadius;
        float brightnessPickPositionY2 = circleCenterY + circleRadius;

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                if(event.getX() > brightnessPickPositionX1 &&  event.getX() < brightnessPickPositionX2
                    && event.getY() > brightnessPickPositionY1 && event.getY() < brightnessPickPositionY2)
                {
                    brightness = (int)((event.getY() - brightnessPickPositionY1) / (brightnessPickPositionY2 - brightnessPickPositionY1) * 255);
                    brightness = Math.min(255, brightness);
                    brightness = Math.max(0, brightness);
                    brightnessHandlePicked = true;
                    initializePaint();
                }
                if(event.getX() > circleCenterX - circleRadius && event.getX() < circleCenterX + circleRadius
                        && event.getY() > circleCenterY - circleRadius && event.getY() < circleCenterY + circleRadius)
                {
                    pinScale = 0.0f;
                    scaleAnimTimer = System.currentTimeMillis();
                    post(pinScaleAnimator);

                    setColorPickerPosition(event.getX(), event.getY());
                    colorPickerPlaced = true;

                    pickColor(event.getY() - circleCenterY, event.getX() - circleCenterX);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(brightnessHandlePicked)
                {
                    brightness = (int)((event.getY() - brightnessPickPositionY1) / (brightnessPickPositionY2 - brightnessPickPositionY1) * 255);
                    brightness = Math.min(255, brightness);
                    brightness = Math.max(0, brightness);
                    if(onBrightnessHandleSlideListener != null) onBrightnessHandleSlideListener.onSlide(brightness);
                    initializePaint();

                }
                if(colorPickerPlaced)
                {
                    setColorPickerPosition(event.getX(), event.getY());
                    int color = pickColor(event.getY() - circleCenterY, event.getX() - circleCenterX);
                    if(onPinMoveListener != null) onPinMoveListener.onPinMove(color, event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if(brightnessHandlePicked)
                {
                    brightness = (int)((event.getY() - brightnessPickPositionY1) / (brightnessPickPositionY2 - brightnessPickPositionY1) * 255);
                    brightness = Math.min(255, brightness);
                    brightness = Math.max(0, brightness);
                    brightnessHandlePicked = false;
                    initializePaint();
                }
                if(colorPickerPlaced)
                {
                    int color = pickColor(event.getY() - circleCenterY, event.getX() - circleCenterX);
                    if(onColorPickListener != null) onColorPickListener.onColorPick(color);
                    colorPickerPlaced = false;
                }
                break;
        }
        postInvalidate();
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //draw circle
        canvas.drawCircle(circleCenterX,
                circleCenterY,
                circleRadius, circlePaint);

        canvas.drawCircle(circleCenterX,
                circleCenterY,
                circleRadius, lightPaint);


        //draw line
        RectF rect = new RectF(circleCenterX + circleRadius + 50,
                circleCenterY - circleRadius,
                circleCenterX + circleRadius + 100,
                circleCenterY + circleRadius);

        canvas.drawRect(rect, linePaint);

        int brightnessHandlePosition = (int)(2 * circleRadius * (brightness / 255f));

        if(brightnessPicker != null) {
            canvas.drawBitmap(brightnessPicker, circleCenterX + circleRadius + 43,
                    circleCenterY - circleRadius + brightnessHandlePosition - 16, null);
        }
        if(colorPickerPlaced) {
            Matrix colorSelectorMatrix = new Matrix();

            Paint colorPaint = new Paint();
            colorPaint.setColor(pickedColor);
            colorPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            if(colorPickerY < 240)
            {
                if(colorPickerX < 240)
                {

                    if(!rotateAnimStarted)
                    {
                        desiredRotate = -180;
                        if(pinRotate != desiredRotate) {
                            rotateAnimTimer = System.currentTimeMillis();
                            initialRotate = pinRotate;
                            removeCallbacks(pinRotateAnimator);
                            postDelayed(pinRotateAnimator, 10);
                            rotateAnimStarted = true;
                        }
                    }else{
                        if(desiredRotate != -180) {
                            desiredRotate = -180;
                            rotateAnimTimer = System.currentTimeMillis();
                            initialRotate = pinRotate;
                        }
                    }

                    colorSelectorMatrix.postRotate(pinRotate, 50, 240);
                    colorSelectorMatrix.postScale(pinScale, pinScale, 50, 100);
                    colorSelectorMatrix.postTranslate(colorPickerX - 50, colorPickerY - 40 - (200 * pinScale));
                }else {


                    if(!rotateAnimStarted) {
                        desiredRotate = -90;
                        if(pinRotate != desiredRotate) {
                            rotateAnimTimer = System.currentTimeMillis();
                            initialRotate = pinRotate;
                            removeCallbacks(pinRotateAnimator);
                            postDelayed(pinRotateAnimator, 10);
                            rotateAnimStarted = true;

                        }
                    }else{
                        if(desiredRotate != -90) {
                            desiredRotate = -90;
                            rotateAnimTimer = System.currentTimeMillis();
                            initialRotate = pinRotate;
                        }
                    }

                    colorSelectorMatrix.postRotate(pinRotate, 50, 240);
                    colorSelectorMatrix.postScale(pinScale, pinScale, 50, 100);
                    colorSelectorMatrix.postTranslate(colorPickerX - 50, colorPickerY - 40 - (200 * pinScale));
//                    colorSelectorMatrix.postTranslate(colorPickerX, colorPickerY);


                }

            }else{
                if(!rotateAnimStarted) {

                    desiredRotate = 0;
                    if(pinRotate != desiredRotate && !rotateAnimStarted) {
                        rotateAnimTimer = System.currentTimeMillis();
                        initialRotate = pinRotate;
                        removeCallbacks(pinRotateAnimator);
                        postDelayed(pinRotateAnimator, 10);
                        rotateAnimStarted = true;
                    }
                }else{
                    if(desiredRotate != 0) {
                        rotateAnimTimer = System.currentTimeMillis();
                        desiredRotate = 0;
                        initialRotate = pinRotate;
                    }
                }

                colorSelectorMatrix.postRotate(pinRotate, 50, 240);
                colorSelectorMatrix.postScale(pinScale, pinScale, 50, 100);
                colorSelectorMatrix.postTranslate(colorPickerX - 50, colorPickerY - 40 - (200 * pinScale));


            }

            Matrix circleTransform = new Matrix();

            float circleX = colorPickerX;
            float circleY = colorPickerY + 30 - (210 * pinScale);

            circleTransform.setRotate(pinRotate, colorPickerX, colorPickerY);

            float[] pts = {circleX, circleY};

            circleTransform.mapPoints(pts);
            //draw colored circle
            canvas.drawCircle(pts[0], pts[1], 40 * pinScale, colorPaint);

            //draw pin
            if(colorSelector != null) {
                canvas.drawBitmap(colorSelector, colorSelectorMatrix, null);
            }

        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int minWidth = 500;
        int minHeight = 400;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;

        if(widthMode == MeasureSpec.EXACTLY)
        {
            width = widthSize;
        }else if(widthMode == MeasureSpec.AT_MOST)
        {
            width = Math.min(minWidth, widthSize);
        }else{
            width = minWidth;
        }

        if(heightMode == MeasureSpec.EXACTLY)
        {
            height = heightSize;
        }else if(heightMode == MeasureSpec.AT_MOST)
        {
            height = Math.min(widthSize - 100, heightSize);
        }else{
            height = minHeight;
        }

        circleRadius = ((width < height) ?
                width - getPaddingLeft() - getPaddingRight() - 100
                    :
                height - getPaddingTop() - getPaddingBottom()) / 2;

        initializePaint();

        super.setMeasuredDimension(width, height);
    }

    public void setOnColorPickListener(OnColorPickListener onColorPickListener) {
        this.onColorPickListener = onColorPickListener;
    }

    public void setOnPinMoveListener(OnPinMoveListener onPinMoveListener) {
        this.onPinMoveListener = onPinMoveListener;
    }

    public void setOnBrightnessHandleSlideListener(OnBrightnessHandleSlideListener onBrightnessHandleSlideListener) {
        this.onBrightnessHandleSlideListener = onBrightnessHandleSlideListener;
    }
}
