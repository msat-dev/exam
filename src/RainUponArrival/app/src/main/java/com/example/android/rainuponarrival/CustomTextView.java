package com.example.android.rainuponarrival;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

public class CustomTextView extends TextView {
    private static final String LOG_TAG = CustomTextView.class.getSimpleName();

    private static final float MIN_TEXT_SIZE = 10;

    private Paint mPaint;
    private float mDensity = -1.0f;
    private float mInitialTextSize = -1.0f;
    private float mInitialMeasuredWidth = -1.0f;

    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        Log.d(LOG_TAG, "onMeasure");

        float density = getContext().getResources().getDisplayMetrics().scaledDensity;
        if (density > 0)
            mDensity = density;

        if (getMeasuredWidth() == 0) {
            Log.d(LOG_TAG, "measure width is 0.");
            return;
        }

        if (mInitialMeasuredWidth <= 0)
            mInitialMeasuredWidth = getMeasuredWidth();
        if (mInitialTextSize <= 0)
            mInitialTextSize = getTextSize();

        float textSize = mInitialTextSize;
//        Log.d(LOG_TAG, "start text size:" + textSize);

        if (mPaint == null)
            mPaint = new Paint();
        float textWidth;
//        Log.d(LOG_TAG, "view width:" + mInitialMeasuredWidth); // + ", density:" + mDensity);
        do {
            mPaint.setTextSize(textSize);
            textWidth = mPaint.measureText(this.getText().toString());
//            Log.d(LOG_TAG, "text:" + getText() + ", text size:" + textSize + ", text width:" + textWidth);
            if (MIN_TEXT_SIZE >= textSize) {
                textSize = MIN_TEXT_SIZE;
                break;
            }
            textSize--;
//        } while ((mInitialMeasuredWidth * mDensity) < textWidth);
        } while (mInitialMeasuredWidth < textWidth);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }
}

