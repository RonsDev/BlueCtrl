/*
 * Copyright (C) 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ronsdev.bluectrl.widget;

import org.ronsdev.bluectrl.HidMouse;
import org.ronsdev.bluectrl.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * A touchpad that handles touch events and redirects them to a HID Mouse.
 */
public class TouchpadView extends View
        implements OnTouchpadGestureListener, OnScrollModeChangedListener {

    /** Indicates that a gesture from the top edge of the screen has been detected. */
    public static final int GESTURE_EDGE_TOP = 10;

    /** Indicates that a gesture from the right edge of the screen has been detected. */
    public static final int GESTURE_EDGE_RIGHT = 20;

    /** Indicates that a gesture from the bottom edge of the screen has been detected. */
    public static final int GESTURE_EDGE_BOTTOM = 30;

    /** Indicates that a gesture from the left edge of the screen has been detected. */
    public static final int GESTURE_EDGE_LEFT = 40;

    /** Indicates that a 2-finger gesture has been detected. */
    public static final int GESTURE_2FINGER = 200;

    /** Indicates that a 3-finger gesture has been detected. */
    public static final int GESTURE_3FINGER = 300;


    /** Indicates that the gesture movement went to the top. */
    public static final int GESTURE_DIRECTION_UP = 0;

    /** Indicates that the gesture movement went to the right. */
    public static final int GESTURE_DIRECTION_RIGHT = 90;

    /** Indicates that the gesture movement went to the bottom. */
    public static final int GESTURE_DIRECTION_DOWN = 180;

    /** Indicates that the gesture movement went to the left. */
    public static final int GESTURE_DIRECTION_LEFT = 270;


    /** Indicates that no scroll mode is active. */
    public static final int SCROLL_MODE_NONE = 0;

    /** Indicates that the vertical scroll mode is active. */
    public static final int SCROLL_MODE_VERTICAL = 10;


    private static final int BG_DOT_DIAMETER_DP = 2;
    private static final int BG_DOT_MARGIN_DP = 25;
    private static final int BG_DOT_COLOR = Color.GRAY;


    private Paint mPaint = new Paint();
    private Drawable mScrollDrawable = null;

    private MouseTouchListener mMouseTouchListener = null;

    private HidMouse mHidMouse = null;


    public TouchpadView(Context context) {
        super(context);

        initView();
    }

    public TouchpadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    public TouchpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initView();
    }


    private final void initView() {
        mPaint.setColor(BG_DOT_COLOR);
        mScrollDrawable = getResources().getDrawable(R.drawable.scroll);

        mMouseTouchListener = new MouseTouchListener(this);
        mMouseTouchListener.setOnTouchpadGestureListener(this);
        mMouseTouchListener.setOnScrollModeChangedListener(this);
    }

    public boolean isActive() {
        return ((mHidMouse != null) && mHidMouse.isConnected());
    }

    public HidMouse getHidMouse() {
        return mHidMouse;
    }
    public void setHidMouse(HidMouse hidMouse) {
        mHidMouse = hidMouse;
        mMouseTouchListener.setHidMouse(hidMouse);
    }

    public float getMouseSensitivity() {
        return mMouseTouchListener.getMouseSensitivity();
    }
    public void setMouseSensitivity(float value) {
        mMouseTouchListener.setMouseSensitivity(value);
    }

    public float getScrollSensitivity() {
        return mMouseTouchListener.getScrollSensitivity();
    }
    public void setScrollSensitivity(float value) {
        mMouseTouchListener.setScrollSensitivity(value);
    }

    public boolean getInvertScroll() {
        return mMouseTouchListener.getInvertScroll();
    }
    public void setInvertScroll(boolean value) {
        mMouseTouchListener.setInvertScroll(value);
    }

    public boolean getFlingScroll() {
        return mMouseTouchListener.getFlingScroll();
    }
    public void setFlingScroll(boolean value) {
        mMouseTouchListener.setFlingScroll(value);
    }

    public boolean onTouchpadGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                if (mMouseTouchListener != null) {
                    mMouseTouchListener.activateScrollMode();
                    return true;
                } else {
                    return false;
                }
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                if (mMouseTouchListener != null) {
                    mMouseTouchListener.activateScrollMode();
                    return true;
                } else {
                    return false;
                }
            case TouchpadView.GESTURE_DIRECTION_LEFT:
                if (mHidMouse != null) {
                    mHidMouse.clickButton(HidMouse.BUTTON_4);
                    return true;
                } else {
                    return false;
                }
            case TouchpadView.GESTURE_DIRECTION_RIGHT:
                if (mHidMouse != null) {
                    mHidMouse.clickButton(HidMouse.BUTTON_5);
                    return true;
                } else {
                    return false;
                }
            }
            break;
        }
        return false;
    }

    public void onScrollModeChanged(int newMode, int oldMode) {
        if ((oldMode == SCROLL_MODE_NONE) || (newMode == SCROLL_MODE_NONE)) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mMouseTouchListener != null) {
            return mMouseTouchListener.onTouch(this, event);
        }

        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackgroundDots(canvas);

        if (mMouseTouchListener != null) {
            if (mMouseTouchListener.getScrollMode() != SCROLL_MODE_NONE) {
                drawInfoDrawable(canvas, mScrollDrawable);
            }
        }
    }

    private void drawBackgroundDots(Canvas canvas) {
        final float scale = getResources().getDisplayMetrics().density;
        final int dotDiameter = (int)(BG_DOT_DIAMETER_DP * scale + 0.5f);
        final int dotMargin = (int)(BG_DOT_MARGIN_DP * scale + 0.5f);

        final int totalDotWidth = dotMargin + dotDiameter + dotMargin;

        int leftOffset = (getWidth() % totalDotWidth) / 2;
        int topOffset = (getHeight() % totalDotWidth) / 2;

        int topPos = topOffset + dotMargin;
        while (topPos < canvas.getHeight()) {
            int leftPos = leftOffset + dotMargin;
            while (leftPos < getWidth()) {
                canvas.drawRect(leftPos,
                        topPos,
                        leftPos + dotDiameter,
                        topPos + dotDiameter,
                        mPaint);

                leftPos += totalDotWidth;
            }

            topPos += totalDotWidth;
        }
    }

    private void drawInfoDrawable(Canvas canvas, Drawable drawable) {
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();
        final int left = (getWidth() / 2) - (drawableWidth / 2);
        final int top = (getHeight() / 2) - (drawableHeight / 2);

        drawable.setBounds(left, top, left + drawableWidth, top + drawableHeight);
        drawable.draw(canvas);
    }
}
