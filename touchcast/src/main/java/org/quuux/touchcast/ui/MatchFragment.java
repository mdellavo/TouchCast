package org.quuux.touchcast.ui;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.R;
import org.quuux.touchcast.game.Player;
import org.quuux.touchcast.game.World;
import org.quuux.touchcast.util.TileSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MatchFragment extends Fragment implements View.OnTouchListener {

    private static final String TAG = Log.buildTag(MatchFragment.class);

    public interface Listener {
        Player getPlayer();
        TileSet getTileSet();
        GoogleApiClient getApiClient();

    }

    Listener mListener;

    TurnBasedMatch mMatch;
    World mWorld;

    WorldView mWorldView;
    GestureView mGestureView;
    IncantationView mIncantationView;
    TextView mCoverText;
    GestureDetectorCompat mGestureDetector;
    ViewConfiguration mViewConfiguration;
    Recognizer mRecognizer = new Recognizer();

    PopupWindow mPopupWindow;
    World.Entity mSelectedEntity;
    LinkedList<Gesture> mGestureBuffer = new LinkedList<Gesture>();

    Map<Incantation, Spell> mSpells = new HashMap<Incantation, Spell>();

    protected MatchFragment() {
        super();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            Log.e(TAG, "activity must implement listener", e);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();

        mGestureDetector = new GestureDetectorCompat(getActivity(), mGestureListener);

        mMatch = args.getParcelable("match");
        mWorld = World.getInstance(mMatch);
        mWorld.setOrder(mMatch.getParticipantIds());

        final String playerId = Games.Players.getCurrentPlayerId(mListener.getApiClient());
        final String participantId = mMatch.getParticipantId(playerId);
        mWorld.join(participantId, mListener.getPlayer());

        mViewConfiguration = ViewConfiguration.get(getActivity());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_match, container, false);

        mWorldView = (WorldView) view.findViewById(R.id.world);
        mWorldView.setWorld(mWorld);
        mWorldView.setTileSet(mListener.getTileSet());
        mWorldView.setOnTouchListener(this);

        mGestureView = (GestureView)view.findViewById(R.id.gesture_view);

        mCoverText = (TextView)view.findViewById(R.id.cover_text);
        mCoverText.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/8bit-wonder.ttf"));

        mIncantationView = (IncantationView) view.findViewById(R.id.incantation_view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        initMatch();

        final int matchStatus = mMatch.getStatus();

        Log.d(TAG, "match status = %s", matchStatus);

        switch (matchStatus) {

            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                break;

            case TurnBasedMatch.MATCH_STATUS_ACTIVE:
                final int turnStatus = mMatch.getTurnStatus();
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN)
                    onTheirTurn();
                else if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN)
                    onMyTurn();
                break;

            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                onMatchCanceled();
                break;

            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                onMatchComplete();
                break;

            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                onMatchExpired();
                break;
        }
    }

    @Override
    public boolean onTouch(final View v, final MotionEvent event) {
        boolean rv = mGestureDetector.onTouchEvent(event);
        if (!rv)
            rv = mUniStrokeListener.onTouch(v, event);

        return rv;
    }

    public static MatchFragment getInstance(final TurnBasedMatch match) {
        final MatchFragment fragment = new MatchFragment();
        final Bundle args = new Bundle();
        args.putParcelable("match", match);
        fragment.setArguments(args);
        return fragment;
    }

    final View.OnTouchListener mUniStrokeListener = new View.OnTouchListener() {

        ArrayList<PointF> mCurrentStroke = new ArrayList<PointF>(5000);

        PointF[] getPoints() {
            return mCurrentStroke.toArray(new PointF[mCurrentStroke.size()]);
        }

        void sample(final MotionEvent event) {
            final int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                final PointF p = new PointF(event.getHistoricalX(h), event.getHistoricalY(h));
                mCurrentStroke.add(p);
            }
        }

        void complete() {
            if(mCurrentStroke.size() > 8) {
                Pair<Recognizer.UniStroke, Float> result = mRecognizer.recognize(getPoints(), true);
                if (result != null) {
                    onStrokeRecognized(result.first.name, result.second, getPoints());
                }
            }
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent motionEvent) {

            dismissPopup();

            switch (motionEvent.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    mCurrentStroke.clear();
                    sample(motionEvent);
                    break;

                case MotionEvent.ACTION_MOVE:
                    sample(motionEvent);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sample(motionEvent);
                    complete();
                    break;

            }

            final PointF[] points = getPoints();
            if (Recognizer.pathLength(points) > mViewConfiguration.getScaledTouchSlop() * 2) {
                mGestureView.plot(Recognizer.resample(points, Recognizer.NUM_POINTS));
            }

            return true;
        }
    };

    private void addSpell(final String name, final String... gestures) {
        mSpells.put(new Incantation(gestures), new Spell(name));
    }

    private void initMatch() {
        if (mSpells.size() == 0) {
            addSpell("fireball", "triangle", "rectangle", "circle");
            addSpell("summon", "star");
            addSpell("protect", "pigtail");
            addSpell("lightning-bolt", "delete", "caret");
        }
    }

    private Spell checkCast(int length) {

        if (length > mGestureBuffer.size())
            return null;

        final List<String> names = new ArrayList<String>();

        for (int i=length-1; i>=0; i--)
            names.add(mGestureBuffer.get(i).name);

        final String[] namesArr = names.toArray(new String[names.size()]);
        Log.d(TAG, "check cast -> %s", Arrays.toString(namesArr));

        final Incantation incantation = new Incantation(namesArr);
        return mSpells.get(incantation);
    }

    private Spell checkCast() {
        for (int i=1; i<=mGestureBuffer.size(); i++) {
            final Spell spell = checkCast(i);
            if (spell != null)
                return spell;
        }

        return null;
    }

    private void onStrokeRecognized(final String name, final float score, final PointF[] points) {
        Log.d(TAG, "recognized: %s (score: %s)", name, score);
        if (name != null) {
            showCoverText(name);

            final Gesture gesture = new Gesture(name, score, points);

            mGestureBuffer.addFirst(gesture);
            while (mGestureBuffer.size() > 4) {
                mGestureBuffer.removeLast();
            }

            mIncantationView.addGesture(gesture);

            final Spell spell = checkCast();

            if (spell != null) {
                Log.d(TAG, "cast %s!!!", spell.name);
                showCoverText(spell.name);

                mGestureBuffer.clear();
                mIncantationView.clearGestures();
            }
        }

    }

    private void onMyTurn() {
        showCoverText(R.string.your_turn);
    }

    private void onTheirTurn() {
        showCoverText(R.string.their_turn);
        mWorldView.disable();
    }

    private void onMatchExpired() {
        showCoverText(R.string.match_expired);
        mWorldView.disable();
    }

    private void onMatchComplete() {
        showCoverText(R.string.match_complete);
        mWorldView.disable();
    }

    private void onMatchCanceled() {
        showCoverText(R.string.match_canceled);
        mWorldView.disable();
    }

    private void dismissPopup() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    private void onShowEntity(final World.Entity entity) {

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.player_popup, null, false);
        final TextView playerName = (TextView)view.findViewById(R.id.player_name);

        playerName.setText(entity.getName());
        mPopupWindow = new PopupWindow(getActivity());
        mPopupWindow.setHeight(400);
        mPopupWindow.setWidth(400);
        mPopupWindow.setContentView(view);

        final TileSet tileset = mListener.getTileSet();
        final Point translation = mWorldView.getMapTranslation();
        final float scale = mWorldView.getMapScale();
        mPopupWindow.showAtLocation(
                mWorldView,
                Gravity.NO_GRAVITY,
                Math.round(tileset.getTileSize() * entity.getX() * scale) + translation.x,
                Math.round(tileset.getTileSize() * entity.getY() * scale) + translation.y
        );

        Log.d(TAG, "popup = %s", mPopupWindow);
    }

    private void onEntitySelected(final World.Entity entity) {
        mSelectedEntity = entity;
        showCoverText(entity != null ? R.string.cast : R.string.select_target);
    }

    void showCoverText(final String string) {
        mCoverText.setText(string);
        mCoverText.setVisibility(View.VISIBLE);
    }

    void showCoverText(final int resId) {
        showCoverText(getString(resId));
    }

    private android.view.GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            final World.Entity entity = mWorldView.setSelection(e.getX(), e.getY());
            Log.d(TAG, "selected %s", entity);
            onEntitySelected(entity);

            mGestureView.clear();

            return true;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(final MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(final MotionEvent e) {
            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(final MotionEvent e) {
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    };

    class Gesture {
        final String name;
        final float score;
        final PointF[] points;

        public Gesture(final String name, final float score, final PointF[] points) {
            this.name = name;
            this.score = score;
            this.points = points;
        }
    }

    class Incantation {

        final String[] mGestures;

        public Incantation(final String[] gestures) {
            mGestures = gestures;
        }

        @Override
        public boolean equals(final Object o) {

            if (o == null || !(o instanceof Incantation))
                return false;

            final Incantation other = (Incantation)o;

            if (other.mGestures.length != mGestures.length)
                return false;

            for (int i=0; i<mGestures.length; i++)
                if (!mGestures[i].equals(other.mGestures[i]))
                    return false;

            return true;
        }

        @Override
        public int hashCode() {
            int rv = 1;
            for (String gesture : mGestures)
                rv = rv * 31 + gesture.hashCode();
            return rv;
        }
    }

    class Spell {
        final String name;

        public Spell(final String name) {
            this.name = name;
        }
    }

}
