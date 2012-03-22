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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * View that renders the background for the Touchpad.
 */
public class TouchpadBackgroundView extends View {

    private static final int DOT_DIAMETER_DP = 2;
    private static final int DOT_MARGIN_DP = 25;
    private static final int DOT_COLOR = Color.GRAY;


    private Paint mPaint = new Paint();


    public TouchpadBackgroundView(Context context) {
        super(context);

        initView();
    }

    public TouchpadBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    public TouchpadBackgroundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initView();
    }


    private final void initView() {
        mPaint.setColor(DOT_COLOR);
    }

    @Override
    public void onDraw(Canvas canvas) {
        final float scale = getResources().getDisplayMetrics().density;
        final int dotDiameter = (int)(DOT_DIAMETER_DP * scale + 0.5f);
        final int dotMargin = (int)(DOT_MARGIN_DP * scale + 0.5f);

        final int totalDotWidth = dotMargin + dotDiameter + dotMargin;

        int leftOffset = (canvas.getWidth() % totalDotWidth) / 2;
        int topOffset = (canvas.getHeight() % totalDotWidth) / 2;

        int topPos = topOffset + dotMargin;
        while (topPos < canvas.getHeight()) {
            int leftPos = leftOffset + dotMargin;
            while (leftPos < canvas.getWidth()) {
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
}
