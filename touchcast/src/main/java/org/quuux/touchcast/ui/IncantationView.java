package org.quuux.touchcast.ui;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.quuux.touchcast.Log;

import java.util.Arrays;

public class IncantationView extends LinearLayout {
    private static final String TAG = Log.buildTag(IncantationView.class);

    public IncantationView(final Context context) {
        super(context);
    }

    public IncantationView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

//    public IncantationView(final Context context, final AttributeSet attrs, final int defStyle) {
//        super(context, attrs, defStyle);
//    }

    public void addGesture(final MatchFragment.Gesture gesture) {
        int size = 200;

        final GestureView gesturedView = new GestureView(getContext());
        gesturedView.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        addView(gesturedView, 0);

        gesturedView.setStrokeWidth(8);

        PointF[] points = gesture.points;
        //Log.d(TAG, "src = %s", Arrays.toString(gesture.points));
        final RectF boundingBox = Recognizer.boundingBox(points);
        //Log.d(TAG, "bounding box = %s", boundingBox);
        points = Recognizer.scaleTo(gesture.points, size/2);
        //Log.d(TAG, "scaled = %s", Arrays.toString(points));
        points = Recognizer.translateTo(points, new PointF(size/2, size/2));
        //Log.d(TAG, "translated = %s", Arrays.toString(points));

        gesturedView.plot(points);
        invalidate();
    }

    public void clearGestures() {
        removeAllViews();
        invalidate();
    }
}
