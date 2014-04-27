package org.quuux.touchcast.ui;

import android.app.Activity;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
    TextView mCoverText;
    Recognizer mRecognizer = new Recognizer();
    GestureDetectorCompat mGestureDetector;


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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

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
                Recognizer.UniStroke u = mRecognizer.recognize(getPoints(), true);
                if (u != null) {
                    Log.d(TAG, "recognized: %s", u.name);
                }
            }
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent motionEvent) {

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

            if (mCurrentStroke.size() > 4)
                mGestureView.plot(Recognizer.resample(getPoints(), Recognizer.NUM_POINTS));

            return true;
        }
    };

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

    private void onShowPlayer(final World.PlayerEntity player) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.player_popup, null, false);
        final TextView playerName = (TextView)view.findViewById(R.id.player_name);
        playerName.setText(player.getPlayer().name);
        final PopupWindow popupWindow = new PopupWindow(getActivity());
        popupWindow.setContentView(view);
        popupWindow.showAtLocation(mWorldView, Gravity.NO_GRAVITY, 0, 0);
    }

    void showCoverText(final int resId) {
        mCoverText.setText(resId);
        mCoverText.setVisibility(View.VISIBLE);
    }

    private android.view.GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            World.Entity entity = mWorldView.setSelection(e.getX(), e.getY());
            if (entity != null) {
                Log.d(TAG, "selected %s", entity);

                if (entity instanceof World.PlayerEntity)
                    onShowPlayer((World.PlayerEntity) entity);
            }
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

}
