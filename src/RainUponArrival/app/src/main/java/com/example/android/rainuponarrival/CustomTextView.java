package com.example.android.rainuponarrival;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class CustomTextView extends TextView {

    private static final float MIN_TEXT_SIZE = 10;

    private Paint mPaint = new Paint();
    private float mInitialTextSize = -1;

    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int viewWidth = this.getWidth();
        float textSize;
        if (mInitialTextSize > 0) {
            textSize = mInitialTextSize;
        } else {
            textSize = getTextSize();
            if (mInitialTextSize < 0)
                mInitialTextSize = textSize;
        }

        mPaint.setTextSize(textSize);
        float textWidth = mPaint.measureText(this.getText().toString());

        while (getMeasuredWidth() <  textWidth) {
            if (MIN_TEXT_SIZE >= textSize) {
                textSize = MIN_TEXT_SIZE;
                break;
            }

            textSize--;

            mPaint.setTextSize(textSize);
            textWidth = mPaint.measureText(this.getText().toString());
        }

        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }
}

