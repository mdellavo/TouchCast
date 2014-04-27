package org.quuux.touchcast.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GestureView extends View {
    private static final String TAG = "GestureView";
    private List<PointF> mPoints = new ArrayList<PointF>();
    private Paint mPaint;

    public GestureView(final Context context) {
        super(context);
        init();
    }

    public GestureView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GestureView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(Color.WHITE);
        for (final PointF point : mPoints) {
            canvas.drawCircle(point.x, point.y, 10, mPaint);
        }

        if (mPoints.size() > 0) {
            final PointF start = mPoints.get(0);
            mPaint.setColor(Color.GREEN);
            canvas.drawCircle(start.x-5, start.y-5, 20, mPaint);
        }

        invalidate();
    }

    public void clear() {
        mPoints.clear();
        invalidate();
    }

    public void plot(final PointF[] points) {
        clear();
        mPoints.addAll(Arrays.asList(points));
        invalidate();
    }

}
