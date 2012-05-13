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
import org.ronsdev.bluectrl.IntArrayList;
import org.ronsdev.bluectrl.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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


    private static final int BG_DOT_COLOR = Color.rgb(80, 84, 89);
    private static final int BG_DOT_DIAMETER_DP = 2;
    private static final int BG_DOT_MARGIN_DP = 25;

    private static final int BUTTON_BAR_HEIGHT_DP = 48;
    private static final int MIDDLE_BUTTON_WIDTH_DP = 48;
    private static final int MAX_BUTTON_WIDTH_DP = 300;

    private static final int BUTTON_INDEX_FIRST = 0;
    private static final int BUTTON_INDEX_SECOND = 1;
    private static final int BUTTON_INDEX_MIDDLE = 2;


    private Paint mBackgroundPaint = new Paint();

    private Drawable mButtonDrawable = null;
    private Drawable mScrollDrawable = null;

    private MouseTouchListener mMouseTouchListener = null;

    private HidMouse mHidMouse = null;

    private boolean mShowButtons = true;

    private float mDisplayDensity;

    private int mBgDotDiameter;
    private int mBgDotMargin;

    private int mButtonBarHeight;
    private int mMiddleButtonWidth;
    private int mMaxButtonWidth;

    /**
     * Array with the rectangles for the buttons (see 'BUTTON_INDEX_*' constants for index
     * mapping).
     */
    private Rect[] mButtonRects = new Rect[3];

    /**
     * Array with Pointer ID Lists of every touched button (see 'BUTTON_INDEX_*' constants for
     * index mapping).
     */
    private IntArrayList[] mButtonPointerIds = new IntArrayList[3];


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
        mBackgroundPaint.setColor(BG_DOT_COLOR);

        final Resources res = getResources();

        mButtonDrawable = res.getDrawable(R.drawable.btn_touchpad);
        mScrollDrawable = res.getDrawable(R.drawable.scroll);

        mDisplayDensity = res.getDisplayMetrics().density;

        mBgDotDiameter = (int)(BG_DOT_DIAMETER_DP * mDisplayDensity + 0.5f);
        mBgDotMargin = (int)(BG_DOT_MARGIN_DP * mDisplayDensity + 0.5f);

        mButtonBarHeight = (int)(BUTTON_BAR_HEIGHT_DP * mDisplayDensity + 0.5f);
        mMiddleButtonWidth = (int)(MIDDLE_BUTTON_WIDTH_DP * mDisplayDensity + 0.5f);
        mMaxButtonWidth = (int)(MAX_BUTTON_WIDTH_DP * mDisplayDensity + 0.5f);

        mMouseTouchListener = new MouseTouchListener(this);
        mMouseTouchListener.setOnTouchpadGestureListener(this);
        mMouseTouchListener.setOnScrollModeChangedListener(this);

        initButtonRects();

        for (int i = 0; i < mButtonPointerIds.length; i++) {
            mButtonPointerIds[i] = new IntArrayList(5);
        }
    }

    private void initButtonRects() {
        final int width = getWidth();
        final int height = getHeight();
        final int buttonBarTop = getTouchpadAreaHeight();
        final int middleButtonLeft = (width / 2) - (mMiddleButtonWidth / 2);
        final int middleButtonRight = middleButtonLeft + mMiddleButtonWidth;

        Rect firstButtonRect = new Rect(0, buttonBarTop, middleButtonLeft, height);
        if (firstButtonRect.width() > mMaxButtonWidth){
            firstButtonRect.left = firstButtonRect.right - mMaxButtonWidth;
        }
        mButtonRects[BUTTON_INDEX_FIRST] = firstButtonRect;

        Rect secondButtonRect = new Rect(middleButtonRight, buttonBarTop, width, height);
        if (secondButtonRect.width() > mMaxButtonWidth){
            secondButtonRect.right = secondButtonRect.left + mMaxButtonWidth;
        }
        mButtonRects[BUTTON_INDEX_SECOND] = secondButtonRect;

        mButtonRects[BUTTON_INDEX_MIDDLE] =
                new Rect(middleButtonLeft, buttonBarTop, middleButtonRight, height);
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

    public boolean getShowButtons() {
        return mShowButtons;
    }
    public void setShowButtons(boolean value) {
        if (value != mShowButtons) {
            mShowButtons = value;
            initButtonRects();
            invalidate();
        }
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

    public int getTouchpadAreaWidth() {
        return getWidth();
    }

    public int getTouchpadAreaHeight() {
        if (mShowButtons) {
            return getHeight() - mButtonBarHeight;
        } else {
            return getHeight();
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        initButtonRects();
    }

    public void onMouseButtonClick(int clickType) {
        invalidate();
    }

    public boolean onTouchpadGesture(int gesture, int direction) {
        switch (gesture) {
        case TouchpadView.GESTURE_EDGE_RIGHT:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                mMouseTouchListener.activateScrollMode();
                return true;
            }
            break;
        case TouchpadView.GESTURE_2FINGER:
            switch (direction) {
            case TouchpadView.GESTURE_DIRECTION_UP:
            case TouchpadView.GESTURE_DIRECTION_DOWN:
                mMouseTouchListener.activateScrollMode();
                return true;
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
        if (mShowButtons && handleButtonsTouchEvent(event)) {
            return true;
        }

        return mMouseTouchListener.onTouch(this, event);
    }

    private boolean handleButtonsTouchEvent(MotionEvent event) {
        if (!isActive() || (event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            releaseAllButtons();
            return false;
        }

        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            final int downPointerIndex = event.getActionIndex();
            final int downPointerId = event.getPointerId(downPointerIndex);
            final int downX = (int)event.getX(downPointerIndex);
            final int downY = (int)event.getY(downPointerIndex);

            for (int i = 0; i < mButtonRects.length; i++) {
                if (mButtonRects[i].contains(downX, downY)) {
                    if (mButtonPointerIds[i].isEmpty()) {
                        mHidMouse.pressButton(convertBtIndexToHidMouseBt(i));
                    }
                    if (!mButtonPointerIds[i].containsValue(downPointerId)) {
                        mButtonPointerIds[i].addValue(downPointerId);
                    }
                    return true;
                }
            }
            break;
        case MotionEvent.ACTION_POINTER_UP:
            final int upPointerIndex = event.getActionIndex();
            final int upPointerId = event.getPointerId(upPointerIndex);

            for (int i = 0; i < mButtonPointerIds.length; i++) {
                if (mButtonPointerIds[i].containsValue(upPointerId)) {
                    mButtonPointerIds[i].removeValue(upPointerId);
                    if (mButtonPointerIds[i].isEmpty()) {
                        mHidMouse.releaseButton(convertBtIndexToHidMouseBt(i));
                    }
                    return true;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            releaseAllButtons();
            break;
        }

        return false;
    }

    private int convertBtIndexToHidMouseBt(int buttonIndex) {
        switch (buttonIndex) {
        case BUTTON_INDEX_FIRST:
            return HidMouse.BUTTON_FIRST;
        case BUTTON_INDEX_SECOND:
            return HidMouse.BUTTON_SECOND;
        case BUTTON_INDEX_MIDDLE:
            return HidMouse.BUTTON_MIDDLE;
        default:
            return 0;
        }
    }

    private void releaseAllButtons() {
        for (int i = 0; i < mButtonPointerIds.length; i++) {
            if (!mButtonPointerIds[i].isEmpty()) {
                if (mHidMouse != null) {
                    mHidMouse.releaseButton(convertBtIndexToHidMouseBt(i));
                }
                mButtonPointerIds[i].clear();
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackgroundDots(canvas);

        if (mShowButtons) {
            drawButtons(canvas);
        }

        if (mMouseTouchListener.getScrollMode() != SCROLL_MODE_NONE) {
            drawInfoDrawable(canvas, mScrollDrawable);
        }
    }

    private void drawBackgroundDots(Canvas canvas) {
        final int width = getTouchpadAreaWidth();
        final int height = getTouchpadAreaHeight();
        final int totalDotWidth = mBgDotMargin + mBgDotDiameter + mBgDotMargin;
        final int leftOffset = (width % totalDotWidth) / 2;
        final int topOffset = (height % totalDotWidth) / 2;

        int topPos = topOffset + mBgDotMargin;
        while (topPos < height) {
            int leftPos = leftOffset + mBgDotMargin;
            while (leftPos < width) {
                canvas.drawRect(leftPos,
                        topPos,
                        leftPos + mBgDotDiameter,
                        topPos + mBgDotDiameter,
                        mBackgroundPaint);

                leftPos += totalDotWidth;
            }

            topPos += totalDotWidth;
        }
    }

    private void drawInfoDrawable(Canvas canvas, Drawable drawable) {
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();
        final int left = (getTouchpadAreaWidth() / 2) - (drawableWidth / 2);
        final int top = (getTouchpadAreaHeight() / 2) - (drawableHeight / 2);

        drawable.setBounds(left, top, left + drawableWidth, top + drawableHeight);
        drawable.draw(canvas);
    }

    private void drawButtons(Canvas canvas) {
        for (int i = 0; i < mButtonRects.length; i++) {
            if ((mHidMouse != null) &&
                    mHidMouse.isButtonPressed(convertBtIndexToHidMouseBt(i))) {
                mButtonDrawable.setState(PRESSED_ENABLED_STATE_SET);
            } else {
                mButtonDrawable.setState(EMPTY_STATE_SET);
            }
            mButtonDrawable.setBounds(mButtonRects[i]);
            mButtonDrawable.draw(canvas);
        }
    }
}
