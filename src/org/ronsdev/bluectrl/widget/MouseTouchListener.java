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

import org.ronsdev.bluectrl.DeviceSettings;
import org.ronsdev.bluectrl.HidKeyboard;
import org.ronsdev.bluectrl.HidMouse;
import org.ronsdev.bluectrl.IntArrayList;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;
import android.view.View.OnTouchListener;

/**
 * Touch event listener that controls a HID Mouse.
 */
public class MouseTouchListener implements OnTouchListener {

    private static final String TAG = "MouseTouchListener";
    private static final boolean V = false;


    /** Distance in percent from an edge of the touchpad where a gesture is detected. */
    private static final int GESTURE_EDGE_THRESHOLD_P = 10;

    /** Maximum distance from an edge of the touchpad where a gesture is detected. */
    private static final float MAX_GESTURE_EDGE_THRESHOLD_DP = 72.0f;

    /** Minimum distance to detect a gesture with a single touch point. */
    private static final float MIN_GESTURE_DISTANCE_DP = 10.0f;

    /**
     * Minimum distance to detect a gesture with multiple touch points.
     * Should be higher than the single touch equivalent because some touch screens lose accuracy
     * with multiple touch points.
     */
    private static final float MIN_MT_GESTURE_DISTANCE_DP = 35.0f;

    /** Minimum span distance to detect a pinch-to-zoom gesture. */
    private static final float MIN_PINCH_ZOOM_DISTANCE_DP = 35.0f;


    /** Maximum time (in ms) for a tap to be considered a click. */
    private static final int MAX_TAP_TOUCH_TIME = 150;

    /** Maximum time (in ms) between two taps to execute a double click or drag action. */
    private static final int MAX_TAP_GAP_TIME = 150;

    /** Maximum allowed distance for a tap with a single touch point to be considered a click. */
    private static final float MAX_TAP_DISTANCE_DP = 8.0f;

    /**
     * Maximum allowed distance for a tap with multiple touch points to be considered a click.
     * Should be higher than the single touch equivalent because some touch screens lose accuracy
     * with multiple touch points.
     */
    private static final float MAX_MT_TAP_DISTANCE_DP = 30.0f;


    /**
     * Distance from an edge of the touchpad where the pointer movement is continued automatically.
     */
    private static final float POINTER_EDGE_MOVE_THRESHOLD_DP = 18.0f;

    /** Pointer movement speed when the touch point is at the edge of the touchpad. */
    private static final float POINTER_EDGE_MOVE_STEP = 4.0f;

    /** Faster pointer movement speed when the touch point is at the edge of the touchpad. */
    private static final float FAST_POINTER_EDGE_MOVE_STEP = 20.0f;


    /**
     * Maximum time (in ms) the pointer movement can be interrupted when a touch end was predicted.
     */
    private static final int MAX_TOUCH_END_PREDICT_TIME = 50;

    /**
     * Maximum allowed distance the pointer movement can be interrupted when a touch end was
     * predicted.
     */
    private static final float MAX_TOUCH_END_PREDICT_DISTANCE_DP = 5.0f;


    /** Intermediate step count of the Smooth Scroll feature. */
    private static final int SMOOTH_SCROLL_STEPS = 16;

    /** Minimum touch move distance per millisecond that is required to change the scroll mode. */
    private static final float CHANGE_SCROLL_MODE_THRESHOLD_DP = 0.3f;


    /** Repeat time (in ms) for the fling scroll loop. */
    private static final int FLING_SCROLL_LOOP_TIME = 20;

    /** Minimum scroll distance that is required to start a fling scroll action. */
    private static final float FLING_SCROLL_THRESHOLD_DP = 6.0f;

    /** Minimum scroll distance that is required to retain the previous fling scroll speed. */
    private static final float MULTIPLE_FLING_SCROLL_THRESHOLD_DP = 24.0f;

    /** The amount of friction applied to the fling scroll movement. */
    private static final float FLING_SCROLL_MOVE_FRICTION_DP = 0.8f;


    private TouchpadView mTouchpadView;
    private HidMouse mHidMouse;
    private HidKeyboard mHidKeyboard;

    private Rect mTouchpadAreaRect;
    private float mMouseSensitivity;
    private float mSmoothScrollSensitivity;
    private float mStepScrollSensitivity;
    private float mPinchZoomSensitivity;
    private boolean mInvertScroll;
    private boolean mFlingScroll;


    private final float mDisplayDensity;

    private final float mMaxGestureEdgeThreshold;
    private final float mPointerEdgeMoveThreshold;
    private final float mMinGestureDistance;
    private final float mMinMultitouchGestureDistance;
    private final float mMinPinchZoomDistance;
    private final float mMaxTapDistanceSquare;
    private final float mMaxMultitouchTapDistanceSquare;
    private final float mMaxTouchEndPredictDistanceSquare;
    private final float mChangeScrollModeThreshold;
    private final float mFlingScrollThreshold;
    private final float mMultipleFlingScrollThreshold;
    private final float mFlingScrollMoveFriction;

    private IdleSubListener mIdleSubListener = new IdleSubListener();
    private GestureSubListener mGestureSubListener = new GestureSubListener();
    private TapSubListener mTapSubListener = new TapSubListener();
    private PointerSubListener mPointerSubListener = new PointerSubListener();
    private ScrollSubListener mScrollSubListener = new ScrollSubListener();
    private PinchZoomSubListener mPinchZoomSubListener = new PinchZoomSubListener();

    /** The current internal touch event listener that handles the touch events. */
    private SubListener mSubListener;

    /** A list with the currently tracked pointer IDs. */
    private IntArrayList mPointerIdList = new IntArrayList(10);

    /** The touched point from the first touch down event. */
    private PointerCoords mFirstPoint = new PointerCoords();

    /** The touched point from the second touch down event. */
    private PointerCoords mSecondPoint = new PointerCoords();

    /** The event time of the first touch down event. */
    private long mFirstEventTime = 0;

    /** The previous touched point from the controlling pointer. */
    private PointerCoords mPreviousPoint = new PointerCoords();

    /** The event time of the previous touched point. */
    private long mPreviousEventTime = 0;


    private OnScrollModeChangedListener mOnScrollModeChangedListener;
    private OnTouchpadGestureListener mOnTouchpadGestureListener;


    public MouseTouchListener(TouchpadView touchpadView) {
        mTouchpadView = touchpadView;

        mSubListener = mIdleSubListener;

        setTouchpadAreaRect(new Rect(mTouchpadView.getLeft(),
                mTouchpadView.getTop(),
                mTouchpadView.getRight(),
                mTouchpadView.getBottom()));
        setMouseSensitivity(DeviceSettings.DEFAULT_MOUSE_SENSITIVITY);
        setScrollSensitivity(DeviceSettings.DEFAULT_SCROLL_SENSITIVITY);
        setPinchZoomSensitivity(DeviceSettings.DEFAULT_PINCH_ZOOM_SENSITIVITY);
        setInvertScroll(DeviceSettings.DEFAULT_INVERT_SCROLL);
        setFlingScroll(DeviceSettings.DEFAULT_FLING_SCROLL);

        mDisplayDensity = mTouchpadView.getResources().getDisplayMetrics().density;

        mMaxGestureEdgeThreshold = (MAX_GESTURE_EDGE_THRESHOLD_DP * mDisplayDensity);

        final float gestureDistance = (MIN_GESTURE_DISTANCE_DP * mDisplayDensity);
        mMinGestureDistance = gestureDistance * gestureDistance;

        final float multitouchGestureDistance = (MIN_MT_GESTURE_DISTANCE_DP * mDisplayDensity);
        mMinMultitouchGestureDistance = multitouchGestureDistance * multitouchGestureDistance;

        mMinPinchZoomDistance = (MIN_PINCH_ZOOM_DISTANCE_DP * mDisplayDensity);

        final float tapDistance = (MAX_TAP_DISTANCE_DP * mDisplayDensity);
        mMaxTapDistanceSquare = tapDistance * tapDistance;

        final float multitouchTapDistance = (MAX_MT_TAP_DISTANCE_DP * mDisplayDensity);
        mMaxMultitouchTapDistanceSquare = multitouchTapDistance * multitouchTapDistance;

        mPointerEdgeMoveThreshold = POINTER_EDGE_MOVE_THRESHOLD_DP * mDisplayDensity;

        final float touchEndDistance = (MAX_TOUCH_END_PREDICT_DISTANCE_DP * mDisplayDensity);
        mMaxTouchEndPredictDistanceSquare = touchEndDistance * touchEndDistance;

        mChangeScrollModeThreshold = CHANGE_SCROLL_MODE_THRESHOLD_DP * mDisplayDensity;
        mFlingScrollThreshold = FLING_SCROLL_THRESHOLD_DP * mDisplayDensity;
        mMultipleFlingScrollThreshold = MULTIPLE_FLING_SCROLL_THRESHOLD_DP * mDisplayDensity;
        mFlingScrollMoveFriction = FLING_SCROLL_MOVE_FRICTION_DP * mDisplayDensity;
    }


    private static boolean isDistanceReached(float x, float y, float maxDistanceSquare) {
        return (x * x) + (y * y) > maxDistanceSquare;
    }

    private static boolean isTopEdge(Rect rect, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((event.getY(pointerIndex) - rect.top) < threshold);
    }

    private static boolean isBottomEdge(Rect rect, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((rect.bottom - event.getY(pointerIndex)) < threshold);
    }

    private static boolean isLeftEdge(Rect rect, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((event.getX(pointerIndex) - rect.left) < threshold);
    }

    private static boolean isRightEdge(Rect rect, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((rect.right - event.getX(pointerIndex)) < threshold);
    }

    private static float getSpan(PointerCoords pointA, PointerCoords pointB) {
        return getSpan(pointA.x, pointA.y, pointB.x, pointB.y);
    }

    private static float getSpan(float aX, float aY, float bX, float bY) {
        final float diffX = aX - bX;
        final float diffY = aY - bY;
        return (float)Math.sqrt((diffX * diffX) + (diffY * diffY));
    }

    private static int convertPointerCountToButtonMask(int pointerCount) {
        switch (pointerCount) {
        case 1:
            return HidMouse.BUTTON_FIRST;
        case 2:
            return HidMouse.BUTTON_SECOND;
        case 3:
            return HidMouse.BUTTON_MIDDLE;
        default:
            return 0;
        }
    }


    private boolean isActive() {
        return ((mHidMouse != null) && mHidMouse.isConnected());
    }

    public HidMouse getHidMouse() {
        return mHidMouse;
    }
    public void setHidMouse(HidMouse hidMouse) {
        mHidMouse = hidMouse;
    }

    public HidKeyboard getHidKeyboard() {
        return mHidKeyboard;
    }
    public void setHidKeyboard(HidKeyboard hidKeyboard) {
        mHidKeyboard = hidKeyboard;
    }

    public Rect getTouchpadAreaRect() {
        return mTouchpadAreaRect;
    }
    public void setTouchpadAreaRect(Rect value) {
        mTouchpadAreaRect = value;
    }

    public float getMouseSensitivity() {
        return mMouseSensitivity;
    }
    public void setMouseSensitivity(float value) {
        mMouseSensitivity = value;
    }

    public float getScrollSensitivity() {
        return mSmoothScrollSensitivity;
    }
    public void setScrollSensitivity(float value) {
        mSmoothScrollSensitivity = value;
        mStepScrollSensitivity = value / SMOOTH_SCROLL_STEPS;
    }

    public float getPinchZoomSensitivity() {
        return mPinchZoomSensitivity;
    }
    public void setPinchZoomSensitivity(float value) {
        mPinchZoomSensitivity = value;
    }

    public boolean getInvertScroll() {
        return mInvertScroll;
    }
    public void setInvertScroll(boolean value) {
        mInvertScroll = value;
    }

    public boolean getFlingScroll() {
        return mFlingScroll;
    }
    public void setFlingScroll(boolean value) {
        mFlingScroll = value;
    }

    public int getScrollMode() {
        if (mSubListener instanceof ScrollSubListener) {
            return ((ScrollSubListener)mSubListener).getScrollMode();
        } else {
            return TouchpadView.SCROLL_MODE_NONE;
        }
    }

    public void activatePointerMode() {
        changeSubListener(mPointerSubListener, null);
    }

    public void activateDragMode(int dragButton, boolean useFastEdgeMovement) {
        changeSubListener(mPointerSubListener, null);
        mPointerSubListener.setDragButton(dragButton, useFastEdgeMovement);
    }

    public void activateScrollMode(int scrollMode) {
        changeSubListener(mScrollSubListener, null);
        mScrollSubListener.setScrollMode(scrollMode);
    }

    /**
     * Changes the currently used touch listener mode and immediately redirects the touch event.
     */
    private void changeSubListener(SubListener listener, MotionEvent event) {
        if (listener != mSubListener) {
            mSubListener.stop();
            mSubListener = listener;
            mSubListener.start();

            if (V) {
                Log.v(TAG, String.format("mouse touch listener changed (%s)",
                        mSubListener.getClass().getSimpleName()));
            }

            if (event != null) {
                mSubListener.onTouch(mTouchpadView, event);
            }
        }
    }

    private void setFirstEventData(MotionEvent event, int pointerIndex) {
        event.getPointerCoords(pointerIndex, mFirstPoint);
        mFirstEventTime = event.getEventTime();
    }

    private void setPreviousEventData(MotionEvent event, int pointerIndex) {
        event.getPointerCoords(pointerIndex, mPreviousPoint);
        mPreviousEventTime = event.getEventTime();
    }

    private int getMainPointerIndex(MotionEvent event) {
        if (mPointerIdList.isEmpty()) {
            return -1;
        } else {
            return event.findPointerIndex(mPointerIdList.getValue(0));
        }
    }

    private void onScrollModeChanged(int newMode, int oldMode) {
        if (mOnScrollModeChangedListener != null) {
            mOnScrollModeChangedListener.onScrollModeChanged(newMode, oldMode);
        }
    }

    public void setOnScrollModeChangedListener(OnScrollModeChangedListener listener) {
        mOnScrollModeChangedListener = listener;
    }

    private boolean onTouchpadGesture(int gesture, int direction) {
        if (mOnTouchpadGestureListener != null) {
            return mOnTouchpadGestureListener.onTouchpadGesture(gesture, direction);
        } else {
            return false;
        }
    }

    public void setOnTouchpadGestureListener(OnTouchpadGestureListener listener) {
        mOnTouchpadGestureListener = listener;
    }


    public boolean onTouch(View view, MotionEvent event) {
        if (!isActive() || (event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            changeSubListener(mIdleSubListener, null);
            mPointerIdList.clear();
            return false;
        }

        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            final int downPointerIndex = event.getActionIndex();
            final int downPointerId = event.getPointerId(downPointerIndex);

            if (mPointerIdList.isEmpty()) {
                setFirstEventData(event, downPointerIndex);
                setPreviousEventData(event, downPointerIndex);
            } else if (mPointerIdList.size() == 1) {
                event.getPointerCoords(downPointerIndex, mSecondPoint);
            }

            if (!mPointerIdList.containsValue(downPointerId)) {
                mPointerIdList.addValue(downPointerId);
            }

            mSubListener.onTouch(view, event);
            break;
        case MotionEvent.ACTION_POINTER_UP:
            final int upPointerIndex = event.getActionIndex();
            final int upPointerId = event.getPointerId(upPointerIndex);
            final int listIndex = mPointerIdList.indexOfValue(upPointerId);

            if (listIndex >= 0) {
                mPointerIdList.remove(listIndex);

                // If the controlling pointer changed, reset the previous event data
                if (listIndex == 0) {
                    final int mainPointerIndex = getMainPointerIndex(event);
                    if (mainPointerIndex >= 0) {
                        setPreviousEventData(event, mainPointerIndex);
                    }
                }

                mSubListener.onTouch(view, event);
            }
            break;
        case MotionEvent.ACTION_UP:
            if (!mPointerIdList.isEmpty()) {
                mPointerIdList.clear();

                mSubListener.onTouch(view, event);
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (!mPointerIdList.isEmpty()) {
                mSubListener.onTouch(view, event);

                final int mainPointerIndex = getMainPointerIndex(event);
                if (mainPointerIndex >= 0) {
                    setPreviousEventData(event, mainPointerIndex);
                }
            }
            break;
        }

        return true;
    }

    /**
     * Abstract class for the event listener subclasses.
     */
    private abstract class SubListener implements OnTouchListener {

        public SubListener() {
            resetMembers();
        }

        protected void start() {
            resetMembers();
        }

        protected void stop() {
        }

        protected abstract void resetMembers();

        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                onTouchPointerDown(view, event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                onTouchPointerUp(view, event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(view, event);
                break;
            }
            return true;
        }

        protected abstract void onTouchPointerDown(View view, MotionEvent event);

        protected abstract void onTouchPointerUp(View view, MotionEvent event);

        protected abstract void onTouchMove(View view, MotionEvent event);
    }

    /**
     * Touchpad event listener that is used if no other listener is active.
     */
    private class IdleSubListener extends SubListener {

        @Override
        protected void resetMembers() {
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
            changeSubListener(mGestureSubListener, event);
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
        }
    }


    /**
     * Touchpad event listener that detects a gesture.
     */
    private class GestureSubListener extends SubListener {

        private float mFirstSpan;
        private int mEdgeGestureType;
        private boolean mWasHandled;


        @Override
        protected void resetMembers() {
            mFirstSpan = -1f;
            mEdgeGestureType = 0;
            mWasHandled = false;
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
            if (mPointerIdList.size() == 1) {
                final int pointerIndex = getMainPointerIndex(event);
                if (pointerIndex >= 0) {
                    final Rect rect = mTouchpadAreaRect;
                    final float thresholdY = getEdgeThreshold(rect.height());
                    final float thresholdX = getEdgeThreshold(rect.width());

                    if (isTopEdge(rect, event, pointerIndex, thresholdY)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_TOP;
                    } else if (isBottomEdge(rect, event, pointerIndex, thresholdY)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_BOTTOM;
                    } else if (isLeftEdge(rect, event, pointerIndex, thresholdX)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_LEFT;
                    } else if (isRightEdge(rect, event, pointerIndex, thresholdX)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_RIGHT;
                    }
                }

                if (mEdgeGestureType == 0) {
                    changeSubListener(mTapSubListener, event);
                }
            }
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                if (mWasHandled) {
                    changeSubListener(mIdleSubListener, event);
                } else {
                    changeSubListener(mTapSubListener, event);
                }
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            if (mWasHandled) {
                return;
            }

            if (mPointerIdList.size() > 1) {
                mEdgeGestureType = 0;
            }

            if (checkEdgeGesture(event) ||
                    checkPinchZoomGesture(event) ||
                    checkMultiTouchGesture(event)) {
                mWasHandled = true;
            }
        }

        private float getEdgeThreshold(int totalSize) {
            final float result = (totalSize * GESTURE_EDGE_THRESHOLD_P / 100);
            if (result > mMaxGestureEdgeThreshold) {
                return mMaxGestureEdgeThreshold;
            } else {
                return result;
            }
        }

        private boolean checkEdgeGesture(MotionEvent event) {
            if (mEdgeGestureType == 0) {
                return false;
            }

            final int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex < 0) {
                return false;
            }

            final float deltaX = mFirstPoint.x - event.getX(pointerIndex);
            final float deltaY = mFirstPoint.y - event.getY(pointerIndex);

            if (isDistanceReached(deltaX, deltaY, mMinGestureDistance)) {
                final int direction = getGestureDirection(deltaX, deltaY);

                if (V) Log.v(TAG, String.format("edge gesture detected (%d, %d)", mEdgeGestureType, direction));

                if (onTouchpadGesture(mEdgeGestureType, direction)) {
                    return true;
                } else {
                    changeSubListener(mPointerSubListener, event);
                }
            }

            return false;
        }

        private boolean checkPinchZoomGesture(MotionEvent event) {
            if (mPointerIdList.size() != 2) {
                return false;
            }

            final int firstPointerIndex = getMainPointerIndex(event);
            final int secondPointerIndex = event.findPointerIndex(mPointerIdList.getValue(1));
            if ((firstPointerIndex < 0) || (secondPointerIndex < 0)) {
                return false;
            }

            if (mFirstSpan < 0) {
                mFirstSpan = getSpan(mFirstPoint, mSecondPoint);
            }

            final float currentSpan = getSpan(event.getX(firstPointerIndex),
                    event.getY(firstPointerIndex),
                    event.getX(secondPointerIndex),
                    event.getY(secondPointerIndex));

            if (Math.abs(currentSpan - mFirstSpan) > mMinPinchZoomDistance) {
                if (V) Log.v(TAG, String.format("pinch-to-zoom gesture detected (%f)", (currentSpan - mFirstSpan)));

                changeSubListener(mPinchZoomSubListener, event);
                return true;
            }

            return false;
        }

        private boolean checkMultiTouchGesture(MotionEvent event) {
            if (mPointerIdList.size() < 2) {
                return false;
            }

            final int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex < 0) {
                return false;
            }

            final float deltaX = mFirstPoint.x - event.getX(pointerIndex);
            final float deltaY = mFirstPoint.y - event.getY(pointerIndex);

            if (isDistanceReached(deltaX, deltaY, mMinMultitouchGestureDistance)) {
                final int direction = getGestureDirection(deltaX, deltaY);

                if (mPointerIdList.size() == 2) {
                    if (V) Log.v(TAG, String.format("two finger gesture detected (%d)", direction));

                    if (onTouchpadGesture(TouchpadView.GESTURE_2FINGER, direction)) {
                        return true;
                    }
                } else if (mPointerIdList.size() == 3) {
                    if (V) Log.v(TAG, String.format("three finger gesture detected (%d)", direction));

                    if (onTouchpadGesture(TouchpadView.GESTURE_3FINGER, direction)) {
                        return true;
                    }
                } else if (mPointerIdList.size() == 4) {
                    if (V) Log.v(TAG, String.format("four finger gesture detected (%d)", direction));

                    if (onTouchpadGesture(TouchpadView.GESTURE_4FINGER, direction)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private int getGestureDirection(float deltaX, float deltaY) {
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                if (deltaX > 0) {
                    return TouchpadView.GESTURE_DIRECTION_LEFT;
                } else {
                    return TouchpadView.GESTURE_DIRECTION_RIGHT;
                }
            } else {
                if (deltaY > 0) {
                    return TouchpadView.GESTURE_DIRECTION_UP;
                } else {
                    return TouchpadView.GESTURE_DIRECTION_DOWN;
                }
            }
        }
    }


    /**
     * Touchpad event listener that detects a tap action.
     */
    private class TapSubListener extends SubListener {

        /** Maximum number of simultaneously touched points. */
        private int mMaxTouchPoints;

        /** Number of successive taps. */
        private int mTapCount;


        private final Runnable mDeferredClickRunnable = new Runnable()
        {
             @Override
             public void run() {
                 if (mTouchpadView.isShown() && isActive()) {
                     final int buttonMask = convertPointerCountToButtonMask(mMaxTouchPoints);
                     if (!mHidMouse.isButtonPressed(buttonMask)) {
                         mHidMouse.clickButton(buttonMask);
                         mTouchpadView.performButtonClickFeedback();
                     }
                 }
                 changeSubListener(mIdleSubListener, null);
             }
        };


        @Override
        protected void resetMembers() {
            mMaxTouchPoints = 1;
            mTapCount = 0;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            stopDeferredClick();

            if (mMaxTouchPoints < mPointerIdList.size()) {
                mMaxTouchPoints = mPointerIdList.size();
            }

            return super.onTouch(view, event);
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                if (isTap(event)) {
                    mTapCount++;

                    if (mTapCount == 2) {
                        executeDoubleClick();
                        changeSubListener(mIdleSubListener, event);
                    } else {
                        startDeferredClick();
                    }
                } else {
                    changeSubListener(mIdleSubListener, event);
                }
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            if (!isTap(event)) {
                if (mTapCount == 1) {
                    /*
                     * Don't pass the current MotionEvent to the pointer event listener. Otherwise
                     * the pointer can move before the drag button is pressed.
                     */
                    changeSubListener(mPointerSubListener, null);

                    mPointerSubListener.setDragButton(
                            convertPointerCountToButtonMask(mMaxTouchPoints));
                } else {
                    if (mMaxTouchPoints < 2) {
                        changeSubListener(mPointerSubListener, event);
                    } else {
                        changeSubListener(mGestureSubListener, event);
                    }
                }
            }
        }

        /**
         * Checks if the touch event is a valid tap action.
         */
        private boolean isTap(MotionEvent event) {
            if (!mTouchpadAreaRect.contains((int)mFirstPoint.x, (int)mFirstPoint.y)) {
                if (V) Log.v(TAG, "invalid tap  (area)");
                return false;
            }

            if ((event.getEventTime() - mFirstEventTime) > MAX_TAP_TOUCH_TIME) {
                if (V) Log.v(TAG, "invalid tap (time)");
                return false;
            }

            final int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex > -1) {
                final float deltaX = mFirstPoint.x - event.getX(pointerIndex);
                final float deltaY = mFirstPoint.y - event.getY(pointerIndex);
                float maxDistanceSquare;
                if (mPointerIdList.size() > 1) {
                    maxDistanceSquare = mMaxMultitouchTapDistanceSquare;
                } else {
                    maxDistanceSquare = mMaxTapDistanceSquare;
                }

                if (isDistanceReached(deltaX, deltaY, maxDistanceSquare)) {
                    if (V) Log.v(TAG, "invalid tap (movement)");
                    return false;
                }
            }

            return true;
        }

        private void startDeferredClick() {
            mTouchpadView.postDelayed(mDeferredClickRunnable, MAX_TAP_GAP_TIME);
        }

        private void stopDeferredClick() {
            mTouchpadView.removeCallbacks(mDeferredClickRunnable);
        }

        private void executeDoubleClick() {
            if (mHidMouse != null) {
                final int buttonMask = convertPointerCountToButtonMask(mMaxTouchPoints);
                if (!mHidMouse.isButtonPressed(buttonMask)) {
                    mHidMouse.clickButton(buttonMask);
                    mTouchpadView.performButtonClickFeedback();
                    mHidMouse.clickButton(buttonMask);
                    mTouchpadView.performButtonClickFeedback();
                }
            }
        }
    }


    /**
     * Touchpad event listener that moves the Mouse pointer.
     */
    private class PointerSubListener extends SubListener {

        /** A Mouse button that is pressed for the whole pointer movement. */
        private int mDragButton;

        /** {@code true} if the edge movement should be faster than normal. */
        private boolean mUseFastEdgeMovement;

        /** Stores the next movement on the X-axis. */
        private float mMoveX;

        /** Stores the next movement on the Y-axis. */
        private float mMoveY;

        /** See {@link PointerTouchListener#isTouchEndPredicted} */
        private boolean mIsTouchEndPredicted;

        /** The event time when a touch end was predicted. */
        private long mTouchEndPredictTime;

        /** The touched point when a touch end was predicted. */
        private PointerCoords mTouchEndPredictPoint = new PointerCoords();


        @Override
        protected void resetMembers() {
            mDragButton = 0;
            mUseFastEdgeMovement = false;
            mMoveX = 0f;
            mMoveY = 0f;
            mIsTouchEndPredicted = false;
            mTouchEndPredictTime = 0;
        }

        @Override
        protected void stop() {
            super.stop();

            if ((mHidMouse != null) && (mDragButton > 0)) {
                mHidMouse.releaseButton(mDragButton);
                mDragButton = 0;
                mTouchpadView.performButtonReleaseFeedback();
            }
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                if ((mHidMouse != null) && (mDragButton > 0)) {
                    mHidMouse.releaseButton(mDragButton);
                    mDragButton = 0;
                    mTouchpadView.performButtonReleaseFeedback();
                }
                changeSubListener(mIdleSubListener, event);
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            final int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex < 0) {
                return;
            }

            mMoveX += event.getX(pointerIndex) - mPreviousPoint.x;
            mMoveY += event.getY(pointerIndex) - mPreviousPoint.y;

            if ((mHidMouse != null) && !isTouchEndPredicted(event, pointerIndex)) {
                if (mDragButton > 0) {
                    addPointerEdgeMovement(view, event, pointerIndex);
                }

                final int reportMoveX = convertTouchDeltaValue(mMoveX);
                final int reportMoveY = convertTouchDeltaValue(mMoveY);
                if ((reportMoveX != 0) || (reportMoveY != 0)) {
                    mHidMouse.movePointer(reportMoveX, reportMoveY);

                    // Subtract only the actually moved value
                    mMoveX -= convertReportDeltaValue(reportMoveX);
                    mMoveY -= convertReportDeltaValue(reportMoveY);
                }
            }
        }

        public void setDragButton(int dragButton) {
            setDragButton(dragButton, false);
        }

        public void setDragButton(int dragButton, boolean useFastEdgeMovement) {
            if ((mHidMouse != null) && (dragButton != mDragButton)) {
                if (mDragButton > 0) {
                    mHidMouse.releaseButton(mDragButton);
                    mDragButton = 0;
                    mTouchpadView.performButtonReleaseFeedback();
                }
                if ((dragButton > 0) && !mHidMouse.isButtonPressed(dragButton)) {
                    mDragButton = dragButton;
                    mHidMouse.pressButton(mDragButton);
                    mTouchpadView.performButtonPressFeedback();
                }
            }

            mUseFastEdgeMovement = useFastEdgeMovement;
        }

        /** Converts the touch move value to the HID Report pointer move value. */
        private int convertTouchDeltaValue(float value) {
            return (int)(value / mDisplayDensity * mMouseSensitivity);
        }

        /** Converts the HID Report pointer move value to the touch move value. */
        private float convertReportDeltaValue(int value) {
            if (mMouseSensitivity != 0) {
                return (value * mDisplayDensity / mMouseSensitivity);
            } else {
                return 0.0f;
            }
        }

        /**
         * Adds a constant movement if the touch event is at an edge of the view.
         * This is especially useful if a drag action is active and the target hasn't been reached
         * yet.
         */
        private void addPointerEdgeMovement(View view, MotionEvent event, int pointerIndex) {
            final Rect rect = mTouchpadAreaRect;
            final float threshold = mPointerEdgeMoveThreshold;
            final float edgeMoveStep = (mUseFastEdgeMovement ? FAST_POINTER_EDGE_MOVE_STEP :
                POINTER_EDGE_MOVE_STEP);

            if (isTopEdge(rect, event, pointerIndex, threshold)) {
                mMoveY -= edgeMoveStep;
            } else if (isBottomEdge(rect, event, pointerIndex, threshold)) {
                mMoveY += edgeMoveStep;
            }

            if (isLeftEdge(rect, event, pointerIndex, threshold)) {
                mMoveX -= edgeMoveStep;
            } else if (isRightEdge(rect, event, pointerIndex, threshold)) {
                mMoveX += edgeMoveStep;
            }
        }

        /**
         * Checks if an imminent end of the touch action is expected.
         * This is useful to prevent a jumping pointer when the finger is removed from the screen
         * because some touch screens are getting inaccurate as soon as the touch pressure is
         * decreasing.
         */
        private boolean isTouchEndPredicted(MotionEvent event, int pointerIndex) {
            if (mIsTouchEndPredicted) {
                if ((event.getEventTime() - mTouchEndPredictTime) > MAX_TOUCH_END_PREDICT_TIME) {
                    if (V) Log.v(TAG, "invalid predicted touch end (Time)");
                    mIsTouchEndPredicted = false;
                }

                if (pointerIndex > -1) {
                    if (event.getPressure(pointerIndex) >= mPreviousPoint.pressure) {
                        if (V) Log.v(TAG, "invalid predicted touch end (Pressure)");
                        mIsTouchEndPredicted = false;
                    }

                    final float x = mTouchEndPredictPoint.x - event.getX(pointerIndex);
                    final float y = mTouchEndPredictPoint.y - event.getY(pointerIndex);
                    if (isDistanceReached(x, y, mMaxTouchEndPredictDistanceSquare)) {
                        if (V) Log.v(TAG, "invalid predicted touch end (Move)");
                        mIsTouchEndPredicted = false;
                    }
                }
            } else {
                if (pointerIndex > -1) {
                    final float x = event.getX(pointerIndex) - mPreviousPoint.x;
                    final float y = event.getY(pointerIndex) - mPreviousPoint.y;
                    if ((event.getPressure(pointerIndex) < mPreviousPoint.pressure) &&
                            !isDistanceReached(x, y, mMaxTouchEndPredictDistanceSquare)){
                        mIsTouchEndPredicted = true;
                        mTouchEndPredictTime = event.getEventTime();
                        event.getPointerCoords(pointerIndex, mTouchEndPredictPoint);

                        if (V) Log.v(TAG, "touch end predicted");
                    }
                }
            }

            return mIsTouchEndPredicted;
        }
    }


    /**
     * Touchpad event listener that scrolls the Mouse Wheel.
     */
    private class ScrollSubListener extends SubListener {

        /** The current scroll mode. */
        private int mScrollMode;

        /** Stores the next movement on the Y-axis. */
        private float mMoveY;

        /** Stores the next movement on the X-axis. */
        private float mMoveX;

        /** The current fling scroll movement on the Y-axis. */
        private float mFlingScrollMoveY;

        /** The current fling scroll movement on the X-axis. */
        private float mFlingScrollMoveX;

        /** The retained fling scroll movement on the Y-axis from the previous fling scroll. */
        private float mRetainedFlingScrollMoveY;

        /** The retained fling scroll movement on the X-axis from the previous fling scroll. */
        private float mRetainedFlingScrollMoveX;


        private final Runnable mFlingScrollRunnable = new Runnable()
        {
             @Override
             public void run() {
                 if (mTouchpadView.isShown() && isActive() &&
                         checkFlingScrollMoveThreshold(0)) {
                     mFlingScrollMoveY = subtractFlingScrollMoveFriction(mFlingScrollMoveY);
                     mFlingScrollMoveX = subtractFlingScrollMoveFriction(mFlingScrollMoveX);

                     scrollWheel(mFlingScrollMoveY, mFlingScrollMoveX);

                     mTouchpadView.postDelayed(mFlingScrollRunnable, FLING_SCROLL_LOOP_TIME);
                 } else {
                     changeSubListener(mIdleSubListener, null);
                 }
             }
        };


        @Override
        protected void resetMembers() {
            mScrollMode = TouchpadView.SCROLL_MODE_VERTICAL;
            mRetainedFlingScrollMoveY = 0.0f;
            mRetainedFlingScrollMoveX = 0.0f;
            resetMoveValues();
        }

        private void resetMoveValues() {
            mMoveY = 0.0f;
            mMoveX = 0.0f;
            mFlingScrollMoveY = 0.0f;
            mFlingScrollMoveX = 0.0f;
        }

        @Override
        protected void start() {
            super.start();

            mTouchpadView.performModeChangedFeedback();
            onScrollModeChanged(mScrollMode, TouchpadView.SCROLL_MODE_NONE);
        }

        @Override
        protected void stop() {
            super.stop();

            mTouchpadView.performModeChangedFeedback();
            onScrollModeChanged(TouchpadView.SCROLL_MODE_NONE, mScrollMode);
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            stopFlingScroll();

            return super.onTouch(view, event);
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
            if (mPointerIdList.size() == 1) {
                mRetainedFlingScrollMoveY = mFlingScrollMoveY;
                mRetainedFlingScrollMoveX = mFlingScrollMoveX;
                resetMoveValues();
            }
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                if (mFlingScroll && checkFlingScrollMoveThreshold(mFlingScrollThreshold)) {
                    startFlingScroll();
                } else {
                    changeSubListener(mIdleSubListener, event);
                }
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex < 0) {
                return;
            }

            final float deltaY = event.getY(pointerIndex) - mPreviousPoint.y;
            final float deltaYPerMs = getDeltaPerMs(event, deltaY);
            final float deltaX = event.getX(pointerIndex) - mPreviousPoint.x;
            final float deltaXPerMs = getDeltaPerMs(event, deltaX);

            reconsiderScrollMode(deltaYPerMs, deltaXPerMs);

            mFlingScrollMoveY = deltaYPerMs * FLING_SCROLL_LOOP_TIME;
            mFlingScrollMoveX = deltaXPerMs * FLING_SCROLL_LOOP_TIME;

            scrollWheel(deltaY, deltaX);
        }

        public int getScrollMode() {
            return mScrollMode;
        }
        public void setScrollMode(int value) {
            if (value != mScrollMode) {
                final int oldMode = mScrollMode;
                mScrollMode = value;
                onScrollModeChanged(mScrollMode, oldMode);
            }
        }

        private void reconsiderScrollMode(float deltaYPerMs, float deltaXPerMs) {
            switch (mScrollMode) {
            case TouchpadView.SCROLL_MODE_VERTICAL:
                if ((Math.abs(deltaXPerMs) > mChangeScrollModeThreshold) &&
                        (Math.abs(deltaXPerMs) > Math.abs(deltaYPerMs * 2))) {
                    setScrollMode(TouchpadView.SCROLL_MODE_ALL);
                }
                break;
            case TouchpadView.SCROLL_MODE_HORIZONTAL:
                if ((Math.abs(deltaYPerMs) > mChangeScrollModeThreshold) &&
                        (Math.abs(deltaYPerMs) > Math.abs(deltaXPerMs * 2))) {
                    setScrollMode(TouchpadView.SCROLL_MODE_ALL);
                }
                break;
            }
        }

        private boolean isVerticalScrollActive() {
            return ((mScrollMode == TouchpadView.SCROLL_MODE_VERTICAL) ||
                    (mScrollMode == TouchpadView.SCROLL_MODE_ALL));
        }

        private boolean isHorizontalScrollActive() {
            return ((mScrollMode == TouchpadView.SCROLL_MODE_HORIZONTAL) ||
                    (mScrollMode == TouchpadView.SCROLL_MODE_ALL));
        }

        /** Gets a movement delta per millisecond value. */
        private float getDeltaPerMs(MotionEvent event, float deltaValue) {
            if (mPreviousEventTime > 0) {
                final long timespan = event.getEventTime() - mPreviousEventTime;
                if (timespan > 0) {
                    return deltaValue / timespan;
                }
            }
            return 0.0f;
        }

        private float getSensitivity(boolean smooth) {
            if (smooth) {
                return mSmoothScrollSensitivity;
            } else {
                return mStepScrollSensitivity;
            }
        }

        /** Converts the Y-axis touch move value to the HID Report scroll value. */
        private int convertTouchDeltaValueY(float value, boolean smooth) {
            return -convertTouchDeltaValueX(value, smooth);
        }

        /** Converts the Y-axis HID Report scroll value to the touch move value. */
        private float convertReportDeltaValueY(int value, boolean smooth) {
            return -convertReportDeltaValueX(value, smooth);
        }

        /** Converts the X-axis touch move value to the HID Report scroll value. */
        private int convertTouchDeltaValueX(float value, boolean smooth) {
            final float sensitivity = getSensitivity(smooth);
            final int result = (int)(value / mDisplayDensity * sensitivity);
            return (mInvertScroll ? -result : result);
        }

        /** Converts the X-axis HID Report scroll value to the touch move value. */
        private float convertReportDeltaValueX(int value, boolean smooth) {
            final float sensitivity = getSensitivity(smooth);
            if (sensitivity != 0) {
                final float result = (value * mDisplayDensity / sensitivity);
                return (mInvertScroll ? -result : result);
            } else {
                return 0.0f;
            }
        }

        private void scrollWheel(float deltaY, float deltaX) {
            mMoveY = (isVerticalScrollActive() ? mMoveY + deltaY : 0.0f);
            mMoveX = (isHorizontalScrollActive() ? mMoveX + deltaX : 0.0f);

            if (mHidMouse != null) {
                final boolean smoothY = mHidMouse.isSmoothScrollYOn();
                final boolean smoothX = mHidMouse.isSmoothScrollXOn();
                final int reportMoveY = convertTouchDeltaValueY(mMoveY, smoothY);
                final int reportMoveX = convertTouchDeltaValueX(mMoveX, smoothX);
                if ((reportMoveY != 0) || (reportMoveX != 0)) {
                    mHidMouse.scrollWheel(reportMoveY, reportMoveX);

                    // Subtract only the actually moved value
                    mMoveY -= convertReportDeltaValueY(reportMoveY, smoothY);
                    mMoveX -= convertReportDeltaValueX(reportMoveX, smoothX);
                }
            }
        }

        /** Returns true if one of the fling scroll move values is bigger than the threshold. */
        private boolean checkFlingScrollMoveThreshold(float threshold) {
            return ((isVerticalScrollActive() && (Math.abs(mFlingScrollMoveY) > threshold)) ||
                    (isHorizontalScrollActive() && (Math.abs(mFlingScrollMoveX) > threshold)));
        }

        private void startFlingScroll() {
            if (checkFlingScrollMoveThreshold(mMultipleFlingScrollThreshold)) {
                if (((mFlingScrollMoveY > 0) && (mRetainedFlingScrollMoveY > 0)) ||
                        ((mFlingScrollMoveY < 0) && (mRetainedFlingScrollMoveY < 0))) {
                    mFlingScrollMoveY += mRetainedFlingScrollMoveY;
                }
                if (((mFlingScrollMoveX > 0) && (mRetainedFlingScrollMoveX > 0)) ||
                        ((mFlingScrollMoveX < 0) && (mRetainedFlingScrollMoveX < 0))) {
                    mFlingScrollMoveX += mRetainedFlingScrollMoveX;
                }
            }

            mTouchpadView.postDelayed(mFlingScrollRunnable, FLING_SCROLL_LOOP_TIME);
        }

        private void stopFlingScroll() {
            mTouchpadView.removeCallbacks(mFlingScrollRunnable);
        }

        private float subtractFlingScrollMoveFriction(float moveValue) {
            if (Math.abs(moveValue) <= mFlingScrollMoveFriction) {
                return 0.0f;
            } else if (moveValue < 0) {
                return (moveValue + mFlingScrollMoveFriction);
            } else {
                return (moveValue - mFlingScrollMoveFriction);
            }
        }
    }


    /**
     * Touchpad event listener for a pinch-to-zoom gesture.
     */
    private class PinchZoomSubListener extends SubListener {

        /** The previous span of the touch points. */
        private float mPreviousSpan;

        /** Stores the unprocessed span delta. */
        private float mSpanDelta;


        @Override
        protected void resetMembers() {
            mPreviousSpan = -1f;
            mSpanDelta = 0f;
        }

        @Override
        protected void start() {
            super.start();

            mTouchpadView.performModeChangedFeedback();
            if (mHidKeyboard != null) {
                mHidKeyboard.pressModifierKey(HidKeyboard.MODIFIER_LEFT_CTRL);
            }
        }

        @Override
        protected void stop() {
            super.stop();

            mTouchpadView.performModeChangedFeedback();
            if (mHidKeyboard != null) {
                mHidKeyboard.releaseModifierKey(HidKeyboard.MODIFIER_LEFT_CTRL);
            }
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                changeSubListener(mIdleSubListener, event);
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            if (mPointerIdList.size() != 2) {
                resetMembers();
                return;
            }

            final int firstPointerIndex = getMainPointerIndex(event);
            final int secondPointerIndex = event.findPointerIndex(mPointerIdList.getValue(1));
            if ((firstPointerIndex < 0) || (secondPointerIndex < 0)) {
                return;
            }

            final float currentSpan = getSpan(event.getX(firstPointerIndex),
                    event.getY(firstPointerIndex),
                    event.getX(secondPointerIndex),
                    event.getY(secondPointerIndex));

            if (mPreviousSpan < 0) {
                mPreviousSpan = currentSpan;
                return;
            }

            mSpanDelta += currentSpan - mPreviousSpan;

            if (mHidMouse != null) {
                int reportScroll = convertTouchDeltaValue(mSpanDelta);
                if ((reportScroll != 0)) {
                    if (mHidMouse.isSmoothScrollYOn()) {
                        /*
                         * Report only complete scroll steps and ignore the intermediate steps of
                         * the Smooth Scroll feature. Otherwise most programs would zoom too fast.
                         */
                        mHidMouse.scrollWheel(reportScroll * SMOOTH_SCROLL_STEPS, 0);
                    } else {
                        mHidMouse.scrollWheel(reportScroll, 0);
                    }

                    // Subtract only the actually reported value
                    mSpanDelta -= convertReportDeltaValue(reportScroll);
                }
            }

            mPreviousSpan = currentSpan;
        }

        /** Converts the touch span value to a HID Report scroll value. */
        private int convertTouchDeltaValue(float value) {
            return (int)(value / mDisplayDensity * mPinchZoomSensitivity / SMOOTH_SCROLL_STEPS);
        }

        /** Converts the HID Report scroll value to a touch span value. */
        private float convertReportDeltaValue(int value) {
            if (mPinchZoomSensitivity != 0) {
                return (value * mDisplayDensity / mPinchZoomSensitivity * SMOOTH_SCROLL_STEPS);
            } else {
                return 0.0f;
            }
        }
    }
}
