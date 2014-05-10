package org.quuux.touchcast.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class MeterView extends View {
    private String mLabel;
    private float mValue;
    private Paint mPaint;
    private Paint mLabelPaint;
    private Rect mRect;
    private int mColor;

    public MeterView(final Context context) {
        super(context);
        init();
    }

    public MeterView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MeterView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mLabelPaint = new Paint();
        mLabelPaint.setTextAlign(Paint.Align.CENTER);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setDither(true);
        mLabelPaint.setSubpixelText(true);
        mLabelPaint.setLinearText(true);
        mLabelPaint.setShadowLayer(5, 1, 1, Color.BLACK);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mRect = new Rect();
    }

    public void setLabel(final String label) {
        mLabel = label;
        invalidate();
    }

    public void setColor(final int color) {
        mColor = color;
        invalidate();
    }

    public void setLabelColor(final int color) {
        mLabelPaint.setColor(color);
        invalidate();
    }

    public void update(final float value) {
        mValue = value;
        invalidate();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        mRect.top = 0;
        mRect.bottom = getHeight();
        mRect.left = 0;
        mRect.right = Math.round(mValue * getWidth());

        mPaint.setShader(new LinearGradient(0, mRect.centerY(), mRect.width(), mRect.centerY(), Color.TRANSPARENT, mColor, Shader.TileMode.MIRROR));
        canvas.drawRect(mRect, mPaint);

        mLabelPaint.setTextSize(mRect.height() * .75f);

        final String label = String.format(mLabel, (int) Math.round(mValue * 100));
        final Rect bounds = new Rect();
        mLabelPaint.getTextBounds(label, 0, label.length(), bounds);

        int xPos = (mRect.width() / 2);
        int yPos = (int) ((mRect.height() / 2)+ ((bounds.bottom - bounds.top) / 2));


        canvas.drawText(label, xPos, yPos, mLabelPaint);
    }
}
