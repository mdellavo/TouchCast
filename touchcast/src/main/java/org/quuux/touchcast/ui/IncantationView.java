package org.quuux.touchcast.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class IncantationView extends LinearLayout {
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
        final GestureView gesturedView = new GestureView(getContext());
        gesturedView.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
        addView(gesturedView, 0);
        gesturedView.plot(gesture.points);
        invalidate();
    }

    public void clearGestures() {
        removeAllViews();
        invalidate();
    }
}
