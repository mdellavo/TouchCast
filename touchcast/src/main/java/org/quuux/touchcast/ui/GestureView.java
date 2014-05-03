package org.quuux.touchcast.ui;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
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
    private Path mPath;

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
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setColor(Color.argb(96, 255, 255, 255));
        mPath = new Path();
        setStrokeWidth(32);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        mPath.reset();

        if (mPoints.size() > 4) {
            final PointF start = mPoints.get(0);
            mPath.moveTo(start.x, start.y);

            for (int i = 1; i < mPoints.size() / 2; i++) {
                final PointF a = mPoints.get(i * 2);
                final PointF b = mPoints.get(i * 2 + 1);
                mPath.quadTo(a.x, a.y, b.x, b.y);
            }
            canvas.drawPath(mPath, mPaint);
        }
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

    public void setStrokeWidth(final int width) {
        mPaint.setStrokeWidth(width);
    }
}
