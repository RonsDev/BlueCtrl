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

package org.ronsdev.bluectrl;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MotionEvent.PointerCoords;
import android.view.View.OnTouchListener;

/**
 * Handles Touch events and redirects them to a HID Mouse.
 */
public class MouseInputHandler implements OnTouchListener {

    /** Indicates that the Mouse input handler is in the idle mode. */
    public static final int MODE_IDLE = 10;

    /** Indicates that the Mouse input handler is detecting or has detected a gesture. */
    public static final int MODE_GESTURE = 20;

    /** Indicates that the Mouse input handler is detecting a Tap action. */
    public static final int MODE_TAP = 30;

    /** Indicates that the Mouse input handler is in the pointer mode. */
    public static final int MODE_POINTER = 40;

    /** Indicates that the Mouse input handler is in the scroll wheel mode. */
    public static final int MODE_SCROLL = 50;


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


    private static final String TAG = "MouseInputHandler";
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


    /** Repeat time (in ms) for the fling scroll loop. */
    private static final int FLING_SCROLL_LOOP_TIME = 100;

    /** Minimum scroll distance that is required to start a fling scroll action. */
    private static final float FLING_SCROLL_THRESHOLD_DP = 30.0f;

    /** Minimum scroll distance that is required to continue the fling scroll loop. */
    private static final float FLING_SCROLL_STOP_THRESHOLD_DP = 2.0f;

    /** The amount of friction applied to scroll flings. */
    private static final float FLING_SCROLL_FRICTION = 0.15f;


    private View mView;
    private HidMouse mMouse;

    private final float mMouseSensitivity;
    private final float mScrollSensitivity;
    private final float mSmoothScrollSensitivity;
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

    private final boolean mIsFlingScrollOn;

    /** The current internal touch event listener that handles the touch events. */
    private OnTouchListener mInternalTouchListener;

    /** The pointer ID from the last touch down event. */
    private int mTouchDownPointerId;

    /** The touched point from the last touch down event. */
    private PointerCoords mTouchDownPoint;


    private OnMouseModeChangedListener mOnMouseModeChangedListener;
    private OnMouseGestureListener mOnMouseGestureListener;


    public MouseInputHandler(View view, HidMouse mouse, DeviceSettings settings) {
        mView = view;
        mMouse = mouse;

        mInternalTouchListener = new IdleTouchListener();
        mTouchDownPoint = new PointerCoords();

        mMouseSensitivity = settings.getMouseSensitivity();

        if (settings.getInvertScroll()) {
            mSmoothScrollSensitivity = settings.getScrollSensitivity();
        } else {
            mSmoothScrollSensitivity = settings.getScrollSensitivity() * -1;
        }
        mScrollSensitivity = mSmoothScrollSensitivity / 16;

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

        mIsFlingScrollOn = settings.getFlingScroll();

        view.setOnTouchListener(this);
    }

    private static boolean isDistanceReached(float x, float y, float maxDistanceSquare) {
        return (x * x) + (y * y) > maxDistanceSquare;
    }

    private static boolean isTopEdge(View view, MotionEvent event, float threshold) {
        return (event.getY() < threshold);
    }

    private static boolean isBottomEdge(View view, MotionEvent event, float threshold) {
        return ((view.getHeight() - event.getY()) < threshold);
    }

    private static boolean isLeftEdge(View view, MotionEvent event, float threshold) {
        return (event.getX() < threshold);
    }

    private static boolean isRightEdge(View view, MotionEvent event, float threshold) {
        return ((view.getWidth() - event.getX()) < threshold);
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

    private static int getMode(OnTouchListener touchListener) {
        if (touchListener instanceof IdleTouchListener) {
            return MODE_IDLE;
        } else if (touchListener instanceof GestureTouchListener) {
            return MODE_GESTURE;
        } else if (touchListener instanceof TapTouchListener) {
            return MODE_TAP;
        } else if (touchListener instanceof PointerTouchListener) {
            return MODE_POINTER;
        } else if (touchListener instanceof ScrollTouchListener) {
            return MODE_SCROLL;
        } else {
            return 0;
        }
    }

    public boolean isActive() {
        return (mView.isShown() && mMouse.isConnected());
    }

   /** Changes the currently used internal touch event listener. */
    private boolean changeInternalTouchListener(OnTouchListener touchListener) {
        return changeInternalTouchListener(touchListener, null);
    }

    /**
     * Changes the currently used internal touch event listener and immediately redirects the
     * touch event.
     */
    private boolean changeInternalTouchListener(OnTouchListener touchListener, MotionEvent event) {
        int oldMode = getMode(mInternalTouchListener);
        int newMode = getMode(touchListener);

        mInternalTouchListener = touchListener;

        if (newMode != oldMode) {
            if (V) Log.v(TAG, String.format("Mouse input mode changed (%d)", newMode));

            onMouseModeChanged(newMode, oldMode);
        }

        if (event != null) {
            return mInternalTouchListener.onTouch(mView, event);
        } else {
            return true;
        }
    }

    public void activateScrollMode() {
        changeInternalTouchListener(new ScrollTouchListener());
    }

    public boolean onTouch(View view, MotionEvent event) {
        if (!isActive() || (mInternalTouchListener == null)) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            final int pointerIndex = event.getActionIndex();

            mTouchDownPointerId = event.getPointerId(pointerIndex);
            event.getPointerCoords(pointerIndex, mTouchDownPoint);
        }

        return mInternalTouchListener.onTouch(view, event);
    }

    private void onMouseModeChanged(int newMode, int oldMode) {
        if (mOnMouseModeChangedListener != null) {
            mOnMouseModeChangedListener.onMouseModeChanged(newMode, oldMode);
        }
    }

    public void setOnMouseModeChangedListener(OnMouseModeChangedListener listener) {
        mOnMouseModeChangedListener = listener;
    }

    private boolean onMouseGesture(int gesture, int direction) {
        if (mOnMouseGestureListener != null) {
            return mOnMouseGestureListener.onMouseGesture(gesture, direction);
        } else {
            return false;
        }
    }

    public void setOnMouseGestureListener(OnMouseGestureListener listener) {
        mOnMouseGestureListener = listener;
    }


    /**
     * Internal touch event listener that is used if no other listener is active.
     */
    private class IdleTouchListener implements OnTouchListener {

        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return changeInternalTouchListener(new GestureTouchListener(), event);
            default:
                return false;
            }
        }
    }


    /**
     * Internal touch event listener that detects a gesture.
     */
    private class GestureTouchListener implements OnTouchListener {

        private int mEdgeGestureType = 0;
        private boolean mWasHandled = false;


        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onTouchDownEvent(view, event)) {
                    return true;
                } else {
                    return changeInternalTouchListener(new TapTouchListener(), event);
                }
            case MotionEvent.ACTION_UP:
                return changeInternalTouchListener(new TapTouchListener(), event);
            case MotionEvent.ACTION_CANCEL:
                return changeInternalTouchListener(new IdleTouchListener(), event);
            case MotionEvent.ACTION_MOVE:
                if (!mWasHandled) {
                    if (onTouchMoveEvent(event)) {
                        mWasHandled = true;
                        return true;
                    }
                }
                return false;
            default:
                return false;
            }
        }

        private int getGestureDirection(float deltaX, float deltaY) {
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                if (deltaX > 0) {
                    return GESTURE_DIRECTION_LEFT;
                } else {
                    return GESTURE_DIRECTION_RIGHT;
                }
            } else {
                if (deltaY > 0) {
                    return GESTURE_DIRECTION_UP;
                } else {
                    return GESTURE_DIRECTION_DOWN;
                }
            }
        }

        private boolean onTouchDownEvent(View view, MotionEvent event) {
            if (isTopEdge(view, event, mGestureEdgeThreshold)) {
                mEdgeGestureType = GESTURE_EDGE_TOP;
            } else if (isBottomEdge(view, event, mGestureEdgeThreshold)) {
                mEdgeGestureType = GESTURE_EDGE_BOTTOM;
            } else if (isLeftEdge(view, event, mGestureEdgeThreshold)) {
                mEdgeGestureType = GESTURE_EDGE_LEFT;
            } else if (isRightEdge(view, event, mGestureEdgeThreshold)) {
                mEdgeGestureType = GESTURE_EDGE_RIGHT;
            }

            return (mEdgeGestureType != 0);
        }

        private boolean onTouchMoveEvent(MotionEvent event) {
            final int pointerIndex = event.findPointerIndex(mTouchDownPointerId);
            if (pointerIndex < 0) {
                return false;
            }

            final float deltaX = mTouchDownPoint.x - event.getX(pointerIndex);
            final float deltaY = mTouchDownPoint.y - event.getY(pointerIndex);
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

                    if (onMouseGesture(mEdgeGestureType, direction)) {
                        return true;
                    } else {
                        return changeInternalTouchListener(new PointerTouchListener(), event);
                    }
                } else if (event.getPointerCount() == 2) {
                    if (V) Log.v(TAG, String.format("two finger gesture detected (%d)", direction));

                    if (onMouseGesture(GESTURE_2FINGER, direction)) {
                        return true;
                    }
                } else if (event.getPointerCount() == 3) {
                    if (V) Log.v(TAG, String.format("three finger gesture detected (%d)", direction));

                    if (onMouseGesture(GESTURE_3FINGER, direction)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }


    /**
     * Internal touch event listener that detects a tap action.
     */
    private class TapTouchListener implements OnTouchListener {

        /** Maximum number of simultaneously touched points. */
        private int mMaxTouchPoints = 0;

        /** Number of successive taps. */
        private int mTapCount = 0;


        private final Runnable mDeferredClickRunnable = new Runnable()
        {
             @Override
             public void run() {
                 if (isActive()) {
                     mMouse.clickButton(convertPointerCountToButtonMask(mMaxTouchPoints));
                     changeInternalTouchListener(new IdleTouchListener());
                 }
             }
        };


        public boolean onTouch(View view, MotionEvent event) {
            stopDeferredClick();

            if (mMaxTouchPoints < event.getPointerCount()) {
                mMaxTouchPoints = event.getPointerCount();
            }

            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (isTap(event)) {
                    mTapCount++;

                    if (mTapCount == 2) {
                        executeDoubleClick();
                        changeInternalTouchListener(new IdleTouchListener(), event);
                        return true;
                    } else {
                        startDeferredClick();
                        return true;
                    }
                } else {
                    return changeInternalTouchListener(new IdleTouchListener(), event);
                }
            case MotionEvent.ACTION_CANCEL:
                return changeInternalTouchListener(new IdleTouchListener(), event);
            case MotionEvent.ACTION_MOVE:
                if (!isTap(event)) {
                    if (mTapCount == 1) {
                        final int buttons = convertPointerCountToButtonMask(mMaxTouchPoints);
                        return changeInternalTouchListener(
                                new PointerTouchListener(buttons), event);
                    } else {
                        if (mMaxTouchPoints < 2) {
                            return changeInternalTouchListener(new PointerTouchListener(), event);
                        } else {
                            return changeInternalTouchListener(new GestureTouchListener(), event);
                        }
                    }
                }
                return true;
            default:
                return false;
            }
        }

        /**
         * Checks if the touch event is a valid tap action.
         */
        private boolean isTap(MotionEvent event) {
            if ((event.getEventTime() - event.getDownTime()) > MAX_TAP_TOUCH_TIME) {
                if (V) Log.v(TAG, "tap limit exceeded (Time)");
                return false;
            }

            final int pointerIndex = event.findPointerIndex(mTouchDownPointerId);
            if (pointerIndex > -1) {
                final float deltaX = mTouchDownPoint.x - event.getX(pointerIndex);
                final float deltaY = mTouchDownPoint.y - event.getY(pointerIndex);
                float maxDistanceSquare;
                if (event.getPointerCount() > 1) {
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
            int buttonMask = convertPointerCountToButtonMask(mMaxTouchPoints);

            mMouse.clickButton(buttonMask);
            mMouse.clickButton(buttonMask);
        }
    }


    /**
     * Internal touch event listener that moves the Mouse pointer.
     */
    private class PointerTouchListener implements OnTouchListener {

        /** The pointer ID of the touch point that controls the pointer. */
        private int mPointerId;

        /** The previous touched point. */
        private PointerCoords mPreviousTouch;

        /** A Mouse button that is pressed for the whole pointer movement. */
        private int mDragButton = 0;

        /** Stores the next movement on the X-axis. */
        private float mMoveX = 0f;

        /** Stores the next movement on the Y-axis. */
        private float mMoveY = 0f;

        /** See {@link PointerTouchListener#isTouchEndPredicted} */
        private boolean mIsTouchEndPredicted = false;

        /** The event time when a touch end was predicted. */
        private long mTouchEndPredictTime = 0;

        /** The touched point when a touch end was predicted. */
        private PointerCoords mTouchEndPredictPoint;


        public PointerTouchListener() {
            this(0);
        }

        public PointerTouchListener(int dragButton) {
            mPointerId = mTouchDownPointerId;
            mDragButton = dragButton;
            mTouchEndPredictPoint = new PointerCoords();

            if (mDragButton > 0) {
                mMouse.pressButton(mDragButton);
            }
        }

        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragButton > 0) {
                    mMouse.releaseButton(mDragButton);
                }
                return changeInternalTouchListener(new IdleTouchListener(), event);
            case MotionEvent.ACTION_MOVE:
                return onTouchMoveEvent(view, event);
            default:
                return false;
            }
        }

        private boolean onTouchMoveEvent(View view, MotionEvent event) {
            int pointerIndex = event.findPointerIndex(mPointerId);
            if (pointerIndex < 0) {
                // Switch to another pointer if the last one isn't available anymore
                pointerIndex = 0;
                mPointerId = event.getPointerId(pointerIndex);
                mPreviousTouch = null;
                mIsTouchEndPredicted = false;
            }

            if (mPreviousTouch == null) {
                mPreviousTouch = new PointerCoords();
            } else {
                mMoveX += event.getX(pointerIndex) - mPreviousTouch.x;
                mMoveY += event.getY(pointerIndex) - mPreviousTouch.y;

                if (!isTouchEndPredicted(event, pointerIndex)) {
                    if (mMouse.getPressedButtons() > 0) {
                        addPointerEdgeMovement(view, event);
                    }

                    final int reportMoveX = convertTouchDeltaValue(mMoveX);
                    final int reportMoveY = convertTouchDeltaValue(mMoveY);
                    if ((reportMoveX != 0) || (reportMoveY != 0)) {
                        mMouse.movePointer(reportMoveX, reportMoveY);

                        // Subtract only the actually moved value
                        mMoveX -= convertReportDeltaValue(reportMoveX);
                        mMoveY -= convertReportDeltaValue(reportMoveY);
                    }
                }
            }

            event.getPointerCoords(pointerIndex, mPreviousTouch);

            return true;
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
        private void addPointerEdgeMovement(View view, MotionEvent event) {
            if (isTopEdge(view, event, mPointerEdgeMoveThreshold)) {
                mMoveY -= POINTER_EDGE_MOVE_STEP;
            } else if (isBottomEdge(view, event, mPointerEdgeMoveThreshold)) {
                mMoveY += POINTER_EDGE_MOVE_STEP;
            }

            if (isLeftEdge(view, event, mPointerEdgeMoveThreshold)) {
                mMoveX -= POINTER_EDGE_MOVE_STEP;
            } else if (isRightEdge(view, event, mPointerEdgeMoveThreshold)) {
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
                    if (event.getPressure(pointerIndex) >= mPreviousTouch.pressure) {
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
                    final float x = event.getX(pointerIndex) - mPreviousTouch.x;
                    final float y = event.getY(pointerIndex) - mPreviousTouch.y;
                    if ((event.getPressure(pointerIndex) < mPreviousTouch.pressure) &&
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
     * Internal touch event listener that scrolls the Mouse Wheel.
     */
    private class ScrollTouchListener implements OnTouchListener {

        /** The pointer ID of the touch point that controls the wheel. */
        private int mPointerId;

        /** The event time of the previous touched point. */
        private long mPreviousTouchTime = 0;

        /** The previous touched point. */
        private PointerCoords mPreviousTouch;

        /** Stores the next movement on the Y-axis. */
        private float mMoveY = 0f;

        /** The current fling scroll movement on the Y-axis. */
        private float mFlingScrollMoveY = 0;


        private final Runnable mFlingScrollRunnable = new Runnable()
        {
             @Override
             public void run() {
                 if ((Math.abs(mFlingScrollMoveY) > mFlingScrollStopTreshold) && isActive()) {
                     mFlingScrollMoveY -= mFlingScrollMoveY * FLING_SCROLL_FRICTION;

                     scrollWheel(mFlingScrollMoveY);

                     mView.postDelayed(mFlingScrollRunnable, FLING_SCROLL_LOOP_TIME);
                 } else {
                     changeInternalTouchListener(new IdleTouchListener());
                 }
             }
        };


        public ScrollTouchListener() {
            resetMembers();
        }

        private void resetMembers() {
            mPointerId = mTouchDownPointerId;
            mPreviousTouchTime = 0;
            mPreviousTouch = null;
            mMoveY = 0.0f;
            mFlingScrollMoveY = 0.0f;
        }

        public boolean onTouch(View view, MotionEvent event) {
            stopFlingScroll();

            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetMembers();
                return true;
            case MotionEvent.ACTION_UP:
                if (mIsFlingScrollOn && (Math.abs(mFlingScrollMoveY) > mFlingScrollTreshold)) {
                    startFlingScroll();
                    return true;
                } else {
                    return changeInternalTouchListener(new IdleTouchListener(), event);
                }
            case MotionEvent.ACTION_CANCEL:
                return changeInternalTouchListener(new IdleTouchListener(), event);
            case MotionEvent.ACTION_MOVE:
                return onTouchMoveEvent(event);
            default:
                return false;
            }
        }

        private boolean onTouchMoveEvent(MotionEvent event) {
            int pointerIndex = event.findPointerIndex(mPointerId);
            if (pointerIndex < 0) {
                // Switch to another pointer if the last one isn't available anymore
                pointerIndex = 0;
                mPointerId = event.getPointerId(pointerIndex);
                mPreviousTouch = null;
            }

            if (mPreviousTouch == null) {
                mPreviousTouch = new PointerCoords();
            } else {
                final float deltaY = event.getY(pointerIndex) - mPreviousTouch.y;

                scrollWheel(deltaY);
                calcFlingScrollMoveValues(event, deltaY);
            }

            mPreviousTouchTime = event.getEventTime();
            event.getPointerCoords(pointerIndex, mPreviousTouch);

            return true;
        }

        /** Converts the touch move value to the HID Report scroll value. */
        private int convertTouchDeltaValue(float value, boolean smooth) {
            final float sensitivity = smooth ? mSmoothScrollSensitivity : mScrollSensitivity;
            return (int)(value / mDisplayDensity * sensitivity);
        }

        /** Converts the HID Report scroll value to the touch move value. */
        private float convertReportDeltaValue(int value, boolean smooth) {
            final float sensitivity = smooth ? mSmoothScrollSensitivity : mScrollSensitivity;
            return (value * mDisplayDensity / sensitivity);
        }

        private void scrollWheel(float deltaY) {
            mMoveY += deltaY;

            final boolean smoothY = mMouse.isSmoothScrollYOn();
            final int scrollMoveY = convertTouchDeltaValue(mMoveY, smoothY);
            if (scrollMoveY != 0) {
                mMouse.scrollWheel(scrollMoveY, 0);

                // Subtract only the actually moved value
                mMoveY -= convertReportDeltaValue(scrollMoveY, smoothY);
            }
        }

        /** Calculates the move values for the fling scroll loop. */
        private void calcFlingScrollMoveValues(MotionEvent event, float deltaY) {
            if (mPreviousTouchTime > 0) {
                final long timespan = event.getEventTime() - mPreviousTouchTime;
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
