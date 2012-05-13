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
import org.ronsdev.bluectrl.HidMouse;
import org.ronsdev.bluectrl.IntArrayList;

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


    /** Distance from an edge of the view where a gesture is detected. */
    private static final float GESTURE_EDGE_THRESHOLD_DP = 30.0f;

    /** Minimum distance to detect a gesture with a single touch point. */
    private static final float MIN_GESTURE_DISTANCE_DP = 10.0f;

    /**
     * Minimum distance to detect a gesture with multiple touch points.
     * Should be higher than the single touch equivalent because some touch screens lose accuracy
     * with multiple touch points.
     */
    private static final float MIN_MT_GESTURE_DISTANCE_DP = 35.0f;


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


    /** Distance from an edge of the view where the pointer movement is continued automatically. */
    private static final float POINTER_EDGE_MOVE_THRESHOLD_DP = 18.0f;

    /** Pointer movement speed when the touch point is at the edge of the view. */
    private static final float POINTER_EDGE_MOVE_STEP = 4.0f;


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


    /** Repeat time (in ms) for the fling scroll loop. */
    private static final int FLING_SCROLL_LOOP_TIME = 100;

    /** Minimum scroll distance that is required to start a fling scroll action. */
    private static final float FLING_SCROLL_THRESHOLD_DP = 30.0f;

    /** Minimum scroll distance that is required to continue the fling scroll loop. */
    private static final float FLING_SCROLL_STOP_THRESHOLD_DP = 2.0f;

    /** The amount of friction applied to scroll flings. */
    private static final float FLING_SCROLL_FRICTION = 0.15f;


    private View mView;
    private HidMouse mHidMouse;

    private float mMouseSensitivity;
    private boolean mInvertScroll;
    private float mSmoothScrollSensitivity;
    private float mStepScrollSensitivity;
    private boolean mFlingScroll;


    private final float mDisplayDensity;

    private final float mGestureEdgeThreshold;
    private final float mPointerEdgeMoveThreshold;
    private final float mMinGestureDistance;
    private final float mMinMultitouchGestureDistance;
    private final float mMaxTapDistanceSquare;
    private final float mMaxMultitouchTapDistanceSquare;
    private final float mMaxTouchEndPredictDistanceSquare;
    private final float mFlingScrollTreshold;
    private final float mFlingScrollStopTreshold;

    private IdleSubListener mIdleSubListener = new IdleSubListener();
    private GestureSubListener mGestureSubListener = new GestureSubListener();
    private TapSubListener mTapSubListener = new TapSubListener();
    private PointerSubListener mPointerSubListener = new PointerSubListener();
    private ScrollSubListener mScrollSubListener = new ScrollSubListener();

    /** The current internal touch event listener that handles the touch events. */
    private SubListener mSubListener;

    /** A list with the currently tracked pointer IDs. */
    private IntArrayList mPointerIdList = new IntArrayList(10);

    /** The touched point from the first touch down event. */
    private PointerCoords mFirstPoint = new PointerCoords();

    /** The event time of the first touch down event. */
    private long mFirstEventTime = 0;

    /** The previous touched point from the controlling pointer. */
    private PointerCoords mPreviousPoint = new PointerCoords();

    /** The event time of the previous touched point. */
    private long mPreviousEventTime = 0;


    private OnScrollModeChangedListener mOnScrollModeChangedListener;
    private OnTouchpadGestureListener mOnTouchpadGestureListener;


    public MouseTouchListener(View view) {
        mView = view;

        mSubListener = mIdleSubListener;

        setMouseSensitivity(DeviceSettings.DEFAULT_MOUSE_SENSITIVITY);
        setInvertScroll(DeviceSettings.DEFAULT_INVERT_SCROLL);
        setScrollSensitivity(DeviceSettings.DEFAULT_SCROLL_SENSITIVITY);
        setFlingScroll(DeviceSettings.DEFAULT_FLING_SCROLL);

        mDisplayDensity = mView.getResources().getDisplayMetrics().density;

        mGestureEdgeThreshold = GESTURE_EDGE_THRESHOLD_DP * mDisplayDensity;

        final float gestureDistance = (MIN_GESTURE_DISTANCE_DP * mDisplayDensity);
        mMinGestureDistance = gestureDistance * gestureDistance;

        final float multitouchGestureDistance = (MIN_MT_GESTURE_DISTANCE_DP * mDisplayDensity);
        mMinMultitouchGestureDistance = multitouchGestureDistance * multitouchGestureDistance;

        final float tapDistance = (MAX_TAP_DISTANCE_DP * mDisplayDensity);
        mMaxTapDistanceSquare = tapDistance * tapDistance;

        final float multitouchTapDistance = (MAX_MT_TAP_DISTANCE_DP * mDisplayDensity);
        mMaxMultitouchTapDistanceSquare = multitouchTapDistance * multitouchTapDistance;

        mPointerEdgeMoveThreshold = POINTER_EDGE_MOVE_THRESHOLD_DP * mDisplayDensity;

        final float touchEndDistance = (MAX_TOUCH_END_PREDICT_DISTANCE_DP * mDisplayDensity);
        mMaxTouchEndPredictDistanceSquare = touchEndDistance * touchEndDistance;

        mFlingScrollTreshold = FLING_SCROLL_THRESHOLD_DP * mDisplayDensity;
        mFlingScrollStopTreshold = FLING_SCROLL_STOP_THRESHOLD_DP * mDisplayDensity;
    }


    private static boolean isDistanceReached(float x, float y, float maxDistanceSquare) {
        return (x * x) + (y * y) > maxDistanceSquare;
    }

    private static boolean isTopEdge(View view, MotionEvent event, int pointerIndex,
            float threshold) {
        return (event.getY(pointerIndex) < threshold);
    }

    private static boolean isBottomEdge(View view, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((view.getHeight() - event.getY(pointerIndex)) < threshold);
    }

    private static boolean isLeftEdge(View view, MotionEvent event, int pointerIndex,
            float threshold) {
        return (event.getX(pointerIndex) < threshold);
    }

    private static boolean isRightEdge(View view, MotionEvent event, int pointerIndex,
            float threshold) {
        return ((view.getWidth() - event.getX(pointerIndex)) < threshold);
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
    public void activateScrollMode() {
        changeSubListener(mScrollSubListener, null);
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
                mSubListener.onTouch(mView, event);
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

        private int mEdgeGestureType;
        private boolean mWasHandled;


        @Override
        protected void resetMembers() {
            mEdgeGestureType = 0;
            mWasHandled = false;
        }

        @Override
        protected void onTouchPointerDown(View view, MotionEvent event) {
            if (mPointerIdList.size() == 1) {
                final int pointerIndex = getMainPointerIndex(event);
                if (pointerIndex >= 0) {
                    if (isTopEdge(view, event, pointerIndex, mGestureEdgeThreshold)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_TOP;
                    } else if (isBottomEdge(view, event, pointerIndex, mGestureEdgeThreshold)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_BOTTOM;
                    } else if (isLeftEdge(view, event, pointerIndex, mGestureEdgeThreshold)) {
                        mEdgeGestureType = TouchpadView.GESTURE_EDGE_LEFT;
                    } else if (isRightEdge(view, event, pointerIndex, mGestureEdgeThreshold)) {
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
                changeSubListener(mTapSubListener, event);
            }
        }

        @Override
        protected void onTouchMove(View view, MotionEvent event) {
            if (mWasHandled) {
                return;
            }

            final int pointerIndex = getMainPointerIndex(event);
            if (pointerIndex < 0) {
                return;
            }

            if (mPointerIdList.size() > 1) {
                mEdgeGestureType = 0;
            }

            final float deltaX = mFirstPoint.x - event.getX(pointerIndex);
            final float deltaY = mFirstPoint.y - event.getY(pointerIndex);
            float maxDistanceSquare;
            if (mEdgeGestureType != 0) {
                maxDistanceSquare = mMinGestureDistance;
            } else {
                maxDistanceSquare = mMinMultitouchGestureDistance;
            }

            if (isDistanceReached(deltaX, deltaY, maxDistanceSquare)) {
                final int direction = getGestureDirection(deltaX, deltaY);

                if (mEdgeGestureType != 0) {
                    if (V) Log.v(TAG, String.format("edge gesture detected (%d, %d)", mEdgeGestureType, direction));

                    if (onTouchpadGesture(mEdgeGestureType, direction)) {
                        mWasHandled = true;
                    } else {
                        changeSubListener(mPointerSubListener, event);
                    }
                } else if (mPointerIdList.size() == 2) {
                    if (V) Log.v(TAG, String.format("two finger gesture detected (%d)", direction));

                    if (onTouchpadGesture(TouchpadView.GESTURE_2FINGER, direction)) {
                        mWasHandled = true;
                    }
                } else if (mPointerIdList.size() == 3) {
                    if (V) Log.v(TAG, String.format("three finger gesture detected (%d)", direction));

                    if (onTouchpadGesture(TouchpadView.GESTURE_3FINGER, direction)) {
                        mWasHandled = true;
                    }
                }
            }
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
                 if (mView.isShown() && isActive()) {
                     final int buttonMask = convertPointerCountToButtonMask(mMaxTouchPoints);
                     if (!mHidMouse.isButtonPressed(buttonMask)) {
                         mHidMouse.clickButton(buttonMask);
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
                    changeSubListener(mPointerSubListener, event);
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
            if ((event.getEventTime() - mFirstEventTime) > MAX_TAP_TOUCH_TIME) {
                if (V) Log.v(TAG, "tap limit exceeded (Time)");
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
                    if (V) Log.v(TAG, "tap limit exceeded (Move)");
                    return false;
                }
            }

            return true;
        }

        private void startDeferredClick() {
            mView.postDelayed(mDeferredClickRunnable, MAX_TAP_GAP_TIME);
        }

        private void stopDeferredClick() {
            mView.removeCallbacks(mDeferredClickRunnable);
        }

        private void executeDoubleClick() {
            if (mHidMouse != null) {
                final int buttonMask = convertPointerCountToButtonMask(mMaxTouchPoints);
                if (!mHidMouse.isButtonPressed(buttonMask)) {
                    mHidMouse.clickButton(buttonMask);
                    mHidMouse.clickButton(buttonMask);
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
            if ((mHidMouse != null) && (dragButton != mDragButton)) {
                if (mDragButton > 0) {
                    mHidMouse.releaseButton(mDragButton);
                    mDragButton = 0;
                }
                if ((dragButton > 0) && !mHidMouse.isButtonPressed(dragButton)) {
                    mDragButton = dragButton;
                    mHidMouse.pressButton(mDragButton);
                }
            }
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
            if (isTopEdge(view, event, pointerIndex, mPointerEdgeMoveThreshold)) {
                mMoveY -= POINTER_EDGE_MOVE_STEP;
            } else if (isBottomEdge(view, event, pointerIndex, mPointerEdgeMoveThreshold)) {
                mMoveY += POINTER_EDGE_MOVE_STEP;
            }

            if (isLeftEdge(view, event, pointerIndex, mPointerEdgeMoveThreshold)) {
                mMoveX -= POINTER_EDGE_MOVE_STEP;
            } else if (isRightEdge(view, event, pointerIndex, mPointerEdgeMoveThreshold)) {
                mMoveX += POINTER_EDGE_MOVE_STEP;
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

        /** The current fling scroll movement on the Y-axis. */
        private float mFlingScrollMoveY;


        private final Runnable mFlingScrollRunnable = new Runnable()
        {
             @Override
             public void run() {
                 if (mView.isShown() && isActive() &&
                         (Math.abs(mFlingScrollMoveY) > mFlingScrollStopTreshold)) {
                     mFlingScrollMoveY -= mFlingScrollMoveY * FLING_SCROLL_FRICTION;

                     scrollWheel(mFlingScrollMoveY);

                     mView.postDelayed(mFlingScrollRunnable, FLING_SCROLL_LOOP_TIME);
                 } else {
                     changeSubListener(mIdleSubListener, null);
                 }
             }
        };


        @Override
        protected void resetMembers() {
            mScrollMode = TouchpadView.SCROLL_MODE_VERTICAL;
            mMoveY = 0.0f;
            mFlingScrollMoveY = 0.0f;
        }

        @Override
        protected void start() {
            super.start();

            onScrollModeChanged(mScrollMode, TouchpadView.SCROLL_MODE_NONE);
        }

        @Override
        protected void stop() {
            super.stop();

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
                resetMembers();
            }
        }

        @Override
        protected void onTouchPointerUp(View view, MotionEvent event) {
            if (mPointerIdList.isEmpty()) {
                if (mFlingScroll && (Math.abs(mFlingScrollMoveY) > mFlingScrollTreshold)) {
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

            scrollWheel(deltaY);
            calcFlingScrollMoveValues(event, deltaY);
        }

        public int getScrollMode() {
            return mScrollMode;
        }

        /** Converts the touch move value to the HID Report scroll value. */
        private int convertTouchDeltaValue(float value, boolean smooth) {
            float sensitivity = smooth ? mSmoothScrollSensitivity : mStepScrollSensitivity;
            if (!mInvertScroll) {
                sensitivity = sensitivity * -1;
            }
            return (int)(value / mDisplayDensity * sensitivity);
        }

        /** Converts the HID Report scroll value to the touch move value. */
        private float convertReportDeltaValue(int value, boolean smooth) {
            float sensitivity = smooth ? mSmoothScrollSensitivity : mStepScrollSensitivity;
            if (!mInvertScroll) {
                sensitivity = sensitivity * -1;
            }
            return (value * mDisplayDensity / sensitivity);
        }

        private void scrollWheel(float deltaY) {
            mMoveY += deltaY;

            if (mHidMouse != null) {
                final boolean smoothY = mHidMouse.isSmoothScrollYOn();
                final int scrollMoveY = convertTouchDeltaValue(mMoveY, smoothY);
                if (scrollMoveY != 0) {
                    mHidMouse.scrollWheel(scrollMoveY, 0);

                    // Subtract only the actually moved value
                    mMoveY -= convertReportDeltaValue(scrollMoveY, smoothY);
                }
            }
        }

        /** Calculates the move values for the fling scroll loop. */
        private void calcFlingScrollMoveValues(MotionEvent event, float deltaY) {
            if (mPreviousEventTime > 0) {
                final long timespan = event.getEventTime() - mPreviousEventTime;
                mFlingScrollMoveY = deltaY / timespan * FLING_SCROLL_LOOP_TIME;
            } else {
                mFlingScrollMoveY = 0;
            }
        }

        private void startFlingScroll() {
            mView.postDelayed(mFlingScrollRunnable, FLING_SCROLL_LOOP_TIME);
        }

        private void stopFlingScroll() {
            mView.removeCallbacks(mFlingScrollRunnable);
        }
    }
}
