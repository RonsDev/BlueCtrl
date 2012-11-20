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

import org.ronsdev.bluectrl.HidKeyboard;
import org.ronsdev.bluectrl.HidMouse;
import org.ronsdev.bluectrl.IntArrayList;
import org.ronsdev.bluectrl.OnMouseButtonClickListener;
import org.ronsdev.bluectrl.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

/**
 * A touchpad that handles touch events and redirects them to a HID Mouse.
 */
public class TouchpadView extends View
        implements OnMouseButtonClickListener, OnScrollModeChangedListener {

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

    /** Indicates that a 4-finger gesture has been detected. */
    public static final int GESTURE_4FINGER = 400;


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

    /** Indicates that the horizontal scroll mode is active. */
    public static final int SCROLL_MODE_HORIZONTAL = 20;

    /** Indicates that the vertical and horizontal scroll mode is active. */
    public static final int SCROLL_MODE_ALL = 30;


    private static final int DEFAULT_TOUCHPAD_AREA_PADDING_DP = 16;

    private static final int BUTTON_BAR_HEIGHT_DP = 48;
    private static final float BUTTON_BAR_STROKE_WIDTH_DP = 1.5f;
    private static final float BUTTON_SEP_STROKE_WIDTH_DP = 0.8f;
    private static final int BUTTON_SEP_MARGIN_DP = 12;
    private static final int MIDDLE_BUTTON_WIDTH_DP = 48;
    private static final int BUTTON_CLICK_DURATION = 100;

    private static final int BUTTON_INDEX_FIRST = 0;
    private static final int BUTTON_INDEX_SECOND = 1;
    private static final int BUTTON_INDEX_MIDDLE = 2;
    private static final int BUTTON_COUNT = 3;


    private Paint mButtonBarPaint = new Paint();

    private Drawable mBackgroundDrawable = null;
    private Drawable mButtonDrawable = null;
    private Drawable mScrollVerticalDrawable = null;
    private Drawable mScrollHorizontalDrawable = null;
    private Drawable mScrollAllDrawable = null;

    private TouchpadViewGestureListener mGestureListener = null;
    private MouseTouchListener mMouseTouchListener = null;

    private HidMouse mHidMouse = null;
    private HidKeyboard mHidKeyboard = null;

    private boolean mShowButtons = true;
    private boolean mShowInfoGraphics = true;

    private Rect mInnerRect = null;
    private Rect mTouchpadAreaRect = null;

    private int mDefaultTouchpadAreaPadding;
    private int mTouchpadAreaPadding;

    private int mButtonBarColor;
    private int mButtonBarHeight;
    private int mButtonBarStrokeWidth;
    private int mButtonSepStrokeWidth;
    private int mButtonSepMargin;
    private int mMiddleButtonWidth;

    /**
     * Array with the rectangles for the buttons (see 'BUTTON_INDEX_*' constants for index
     * mapping).
     */
    private Rect[] mButtonRects = new Rect[BUTTON_COUNT];

    /**
     * Array with Pointer ID Lists of every touched button (see 'BUTTON_INDEX_*' constants for
     * index mapping).
     */
    private IntArrayList[] mButtonPointerIds = new IntArrayList[BUTTON_COUNT];

    /**
     * Array with the pressed states of short clicked buttons (see 'BUTTON_INDEX_*' constants for
     * index mapping).
     */
    private boolean[] mClickedButtons = new boolean[BUTTON_COUNT];


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
        final Resources res = getResources();

        mBackgroundDrawable = res.getDrawable(R.drawable.touchpad_background);
        mButtonDrawable = res.getDrawable(R.drawable.btn_touchpad);
        mScrollVerticalDrawable = res.getDrawable(R.drawable.scroll_vertical);
        mScrollHorizontalDrawable = res.getDrawable(R.drawable.scroll_horizontal);
        mScrollAllDrawable = res.getDrawable(R.drawable.scroll_all);

        final float displayDensity = res.getDisplayMetrics().density;

        mDefaultTouchpadAreaPadding =
                (int)(DEFAULT_TOUCHPAD_AREA_PADDING_DP * displayDensity + 0.5f);
        mTouchpadAreaPadding = mDefaultTouchpadAreaPadding;

        mButtonBarColor = res.getColor(R.color.btn_touchpad_border);
        mButtonBarHeight = (int)(BUTTON_BAR_HEIGHT_DP * displayDensity + 0.5f);
        mButtonBarStrokeWidth = (int)(BUTTON_BAR_STROKE_WIDTH_DP * displayDensity + 0.5f);
        mButtonSepStrokeWidth = (int)(BUTTON_SEP_STROKE_WIDTH_DP * displayDensity + 0.5f);
        mButtonSepMargin = (int)(BUTTON_SEP_MARGIN_DP * displayDensity + 0.5f);
        mMiddleButtonWidth = (int)(MIDDLE_BUTTON_WIDTH_DP * displayDensity + 0.5f);

        mGestureListener = new TouchpadViewGestureListener(this);

        mMouseTouchListener = new MouseTouchListener(this);
        mMouseTouchListener.setOnTouchpadGestureListener(mGestureListener);
        mMouseTouchListener.setOnScrollModeChangedListener(this);

        recalculateRects();

        for (int i = 0; i < mButtonPointerIds.length; i++) {
            mButtonPointerIds[i] = new IntArrayList(5);
        }
    }

    private void recalculateRects() {
        final int innerLeft = getPaddingLeft();
        final int innerRight = getWidth() - getPaddingRight();
        final int innerTop = getPaddingTop();
        final int innerBottom = getHeight() - getPaddingBottom();
        final int buttonBarTop = innerBottom - getVisibleButtonBarHeight();
        final int buttonCenterX = innerLeft + ((innerRight - innerLeft) / 2);
        final int middleButtonLeft = buttonCenterX - (mMiddleButtonWidth / 2);
        final int middleButtonRight = middleButtonLeft + mMiddleButtonWidth;

        mInnerRect = new Rect(innerLeft, innerTop, innerRight, innerBottom);

        mTouchpadAreaRect = new Rect(innerLeft, innerTop, innerRight, buttonBarTop);
        mMouseTouchListener.setTouchpadAreaRect(mTouchpadAreaRect);

        mButtonRects[BUTTON_INDEX_FIRST] =
                new Rect(innerLeft, buttonBarTop, buttonCenterX, innerBottom);

        mButtonRects[BUTTON_INDEX_SECOND] =
                new Rect(buttonCenterX, buttonBarTop, innerRight, innerBottom);

        mButtonRects[BUTTON_INDEX_MIDDLE] =
                new Rect(middleButtonLeft, buttonBarTop, middleButtonRight, innerBottom);
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

    public HidKeyboard getHidKeyboard() {
        return mHidKeyboard;
    }
    public void setHidKeyboard(HidKeyboard hidKeyboard) {
        mHidKeyboard = hidKeyboard;
        mMouseTouchListener.setHidKeyboard(hidKeyboard);
    }

    public boolean getShowButtons() {
        return mShowButtons;
    }
    public void setShowButtons(boolean value) {
        if (value != mShowButtons) {
            mShowButtons = value;
            recalculateRects();
            invalidate();
        }
    }

    public boolean getShowInfoGraphics() {
        return mShowInfoGraphics;
    }
    public void setShowInfoGraphics(boolean value) {
        if (value != mShowInfoGraphics) {
            mShowInfoGraphics = value;
            invalidate();
        }
    }

    public String getGestureMode() {
        return mGestureListener.getGestureMode();
    }
    public void setGestureMode(String value) {
        mGestureListener.setGestureMode(value);
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

    public float getPinchZoomSensitivity() {
        return mMouseTouchListener.getPinchZoomSensitivity();
    }
    public void setPinchZoomSensitivity(float value) {
        mMouseTouchListener.setPinchZoomSensitivity(value);
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

    public int getTouchpadAreaPadding() {
        return mTouchpadAreaPadding;
    }
    public void setTouchpadAreaPadding(int value) {
        mTouchpadAreaPadding = value;
    }
    public void resetTouchpadAreaPadding() {
        mTouchpadAreaPadding = mDefaultTouchpadAreaPadding;
    }

    public int getVisibleButtonBarHeight() {
        return (mShowButtons ? mButtonBarHeight : 0);
    }

    public void activatePointerMode() {
        mMouseTouchListener.activatePointerMode();
    }

    public void activateDragMode(int dragButton, boolean useFastEdgeMovement) {
        mMouseTouchListener.activateDragMode(dragButton, useFastEdgeMovement);
    }

    public void activateScrollMode(int scrollMode) {
        mMouseTouchListener.activateScrollMode(scrollMode);
    }

    public void performButtonPressFeedback() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        playSoundEffect(SoundEffectConstants.CLICK);
    }

    public void performButtonReleaseFeedback() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        playSoundEffect(SoundEffectConstants.CLICK);
    }

    public void performButtonClickFeedback() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        playSoundEffect(SoundEffectConstants.CLICK);
    }

    public void performGestureDetectedFeedback() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    public void performModeChangedFeedback() {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        recalculateRects();
    }

    public void onMouseButtonClick(int clickType, int button) {
        if (clickType == HidMouse.CLICK_TYPE_CLICK) {
            final int buttonIndex = convertHidMouseBtToBtIndex(button);
            if (buttonIndex > -1) {
                mClickedButtons[buttonIndex] = true;
                postDelayed(new Runnable()
                {
                     @Override
                     public void run() {
                         mClickedButtons[buttonIndex] = false;
                         invalidate();
                     }
                }, BUTTON_CLICK_DURATION);
            }
        }

        invalidate();
    }

    public void onScrollModeChanged(int newMode, int oldMode) {
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

            for (int i = (mButtonRects.length - 1); i >= 0; i--) {
                if (mButtonRects[i].contains(downX, downY)) {
                    if (mButtonPointerIds[i].isEmpty()) {
                        mHidMouse.pressButton(convertBtIndexToHidMouseBt(i));
                        performButtonPressFeedback();
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
                        performButtonReleaseFeedback();
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

    private int convertHidMouseBtToBtIndex(int hidMouseButton) {
        switch (hidMouseButton) {
        case HidMouse.BUTTON_FIRST:
            return BUTTON_INDEX_FIRST;
        case HidMouse.BUTTON_SECOND:
            return BUTTON_INDEX_SECOND;
        case HidMouse.BUTTON_MIDDLE:
            return BUTTON_INDEX_MIDDLE;
        default:
            return -1;
        }
    }

    private boolean isButtonPressed(int btIndex) {
        final int hidMouseBt = convertBtIndexToHidMouseBt(btIndex);
        return (mClickedButtons[btIndex] ||
                ((mHidMouse != null) && mHidMouse.isButtonPressed(hidMouseBt)));
    }

    private void releaseAllButtons() {
        for (int i = 0; i < mButtonPointerIds.length; i++) {
            if (!mButtonPointerIds[i].isEmpty()) {
                if (mHidMouse != null) {
                    mHidMouse.releaseButton(convertBtIndexToHidMouseBt(i));
                    performButtonReleaseFeedback();
                }
                mButtonPointerIds[i].clear();
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mBackgroundDrawable.setBounds(mInnerRect);
        mBackgroundDrawable.draw(canvas);

        if (mShowInfoGraphics) {
            switch (mMouseTouchListener.getScrollMode()) {
            case SCROLL_MODE_VERTICAL:
                drawInfoDrawable(canvas, mScrollVerticalDrawable);
                break;
            case SCROLL_MODE_HORIZONTAL:
                drawInfoDrawable(canvas, mScrollHorizontalDrawable);
                break;
            case SCROLL_MODE_ALL:
                drawInfoDrawable(canvas, mScrollAllDrawable);
                break;
            }
        }

        if (mShowButtons) {
            drawButtons(canvas);
        }
    }

    private void drawInfoDrawable(Canvas canvas, Drawable drawable) {
        final int maxWidth = mTouchpadAreaRect.width() - (mTouchpadAreaPadding * 2);
        final int maxHeight = mTouchpadAreaRect.height() - (mTouchpadAreaPadding * 2);

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();
        if ((drawableWidth > maxWidth) || (drawableHeight > maxHeight)) {
            if (maxWidth < maxHeight) {
                drawableWidth = maxWidth;
                drawableHeight = maxWidth;
            } else {
                drawableWidth = maxHeight;
                drawableHeight = maxHeight;
            }
        }

        final int touchpadCenterX = mTouchpadAreaRect.left + (mTouchpadAreaRect.width() / 2);
        final int touchpadCenterY = mTouchpadAreaRect.top + (mTouchpadAreaRect.height() / 2);
        final int left = touchpadCenterX - (drawableWidth / 2);
        final int top = touchpadCenterY - (drawableHeight / 2);

        drawable.setBounds(left, top, left + drawableWidth, top + drawableHeight);
        drawable.draw(canvas);
    }

    private void drawButtons(Canvas canvas) {
        final Rect firstButtonRect = mButtonRects[BUTTON_INDEX_FIRST];
        final Rect secondButtonRect = mButtonRects[BUTTON_INDEX_SECOND];

        mButtonDrawable.setBounds(firstButtonRect);
        if (isButtonPressed(BUTTON_INDEX_FIRST) || isButtonPressed(BUTTON_INDEX_MIDDLE)) {
            mButtonDrawable.setState(PRESSED_ENABLED_STATE_SET);
        } else {
            mButtonDrawable.setState(EMPTY_STATE_SET);
        }
        mButtonDrawable.draw(canvas);

        mButtonDrawable.setBounds(secondButtonRect);
        if (isButtonPressed(BUTTON_INDEX_SECOND) || isButtonPressed(BUTTON_INDEX_MIDDLE)) {
            mButtonDrawable.setState(PRESSED_ENABLED_STATE_SET);
        } else {
            mButtonDrawable.setState(EMPTY_STATE_SET);
        }
        mButtonDrawable.draw(canvas);


        final int buttonBarY = firstButtonRect.top - (mButtonBarStrokeWidth / 2);
        mButtonBarPaint.setColor(mButtonBarColor);
        mButtonBarPaint.setStrokeWidth(mButtonBarStrokeWidth);
        canvas.drawLine(firstButtonRect.left,
                buttonBarY,
                secondButtonRect.right,
                buttonBarY,
                mButtonBarPaint);

        final int buttonSepX = firstButtonRect.right - (mButtonSepStrokeWidth / 2);
        mButtonBarPaint.setColor(mButtonBarColor);
        mButtonBarPaint.setStrokeWidth(mButtonSepStrokeWidth);
        canvas.drawLine(buttonSepX,
                firstButtonRect.top + mButtonSepMargin,
                buttonSepX,
                firstButtonRect.bottom - mButtonSepMargin,
                mButtonBarPaint);
    }
}
