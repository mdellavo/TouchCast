package org.quuux.touchcast.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchBuffer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.R;

import java.util.ArrayList;
import java.util.Date;

public class LobbyFragment extends ListFragment implements View.OnClickListener {

    private static final int RC_SELECT_PLAYERS = 4352;
    private static final int RC_OPEN_INBOX = 9843;

    private static final String TAG = Log.buildTag(LobbyFragment.class);
    private Adapter mAdapter;

    public interface Listener {
        GoogleApiClient getApiClient();
        void openMatch(TurnBasedMatch match);
    }

    Listener mListener;

    protected LobbyFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Listener");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_lobby, container, false);

        final Button startMatch = (Button)view.findViewById(R.id.start_match);
        startMatch.setOnClickListener(this);

        final Button inboxButton = (Button)view.findViewById(R.id.inbox);
        inboxButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new Adapter(getActivity());
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        final MatchInfo match = mAdapter.getItem(position);

        if (match instanceof InvitationMatch) {

        } else {
            mListener.openMatch(((Match)match).getMatch());
        }
    }

    private void refresh() {
        Games.TurnBasedMultiplayer
                .loadMatchesByStatus(mListener.getApiClient(), TurnBasedMatch.MATCH_TURN_STATUS_ALL)
                .setResultCallback(mRefreshCallback);
    }

    public static LobbyFragment newInstance() {
        final LobbyFragment rv = new LobbyFragment();
        return rv;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.start_match:
                startMatch();
                break;

            case R.id.inbox:
                openInbox();
                break;
        }
    }

    private void openInbox() {
        final Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mListener.getApiClient());
        startActivityForResult(intent, RC_OPEN_INBOX);
    }

    private void startMatch() {
        final Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mListener.getApiClient(),
                1, 7, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        Log.d(TAG, "data: %s", data);

        switch (requestCode) {
            case RC_SELECT_PLAYERS:
                onPlayersSelected(resultCode, data);
                break;
        }

    }

    private void onPlayersSelected(final int resultCode, final Intent data) {

        // get the invitee list
        final ArrayList<String> invitees = data
                .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

        for (final String invitee : invitees)
            Log.d(TAG, "inviting %s", invitee);

        // get automatch criteria
        Bundle autoMatchCriteria = null;

        int minAutoMatchPlayers = data.getIntExtra(
                Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(
                Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

        if (minAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
        } else {
            autoMatchCriteria = null;
        }

        final TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .addInvitedPlayers(invitees)
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();

        // Start the match
        Games.TurnBasedMultiplayer.createMatch(mListener.getApiClient(), tbmc).setResultCallback(mCreateMatchCallback);
    }

    interface MatchInfo {
        String getCreator();
        String getSummary();
        String getAge();
        String getStatus();
    }

    class Match implements MatchInfo {
        private final TurnBasedMatch mMatch;

        public Match(final TurnBasedMatch match) {
            mMatch = match;
        }

        @Override
        public String getCreator() {
            return mMatch.getParticipant(mMatch.getCreatorId()).getDisplayName();
        }

        @Override
        public String getSummary() {
            return "";
        }

        @Override
        public String getAge() {
            return new Date(mMatch.getLastUpdatedTimestamp()).toString();
        }

        @Override
        public String getStatus() {
            return mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN ? "Your Turn!" : "waiting...";
        }

        public TurnBasedMatch getMatch() {
            return mMatch;
        }
    }

    class InvitationMatch implements MatchInfo {
        private final Invitation mInvition;

        public InvitationMatch(final Invitation invitation) {
            mInvition = invitation;
        }

        @Override
        public String getCreator() {
            return mInvition.getInviter().getDisplayName();
        }

        @Override
        public String getSummary() {
            return "";
        }

        @Override
        public String getAge() {
            return new Date(mInvition.getCreationTimestamp()).toString();
        }

        @Override
        public String getStatus() {
            return "join now!";
        }
    }

    class Holder {
        TextView status, summary, creator, age;
    }

    class Adapter extends ArrayAdapter<MatchInfo> {
        final LayoutInflater mInflater;

        public Adapter(final Context context) {
            super(context, -1);
            mInflater = getLayoutInflater(null);
        }

        public void update(final LoadMatchesResponse matches) {
            clear();
            addAll(matches.getInvitations());
            addAll(matches.getCompletedMatches());
            addAll(matches.getMyTurnMatches());
            addAll(matches.getTheirTurnMatches());
        }

        private void addAll(final InvitationBuffer invitations) {
            for (int i=0; i<invitations.getCount(); i++) {
                final Invitation invitation = invitations.get(i);
                Log.d(TAG, "invitation = %s", invitation);
                add(new InvitationMatch(invitation));
            }
        }

        private void addAll(final TurnBasedMatchBuffer matches) {
            for(int i=0; i<matches.getCount(); i++) {
                final TurnBasedMatch match = matches.get(i);
                Log.d(TAG, "match = %s", match);
                add(new Match(match));
            }
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view = convertView == null ? newView(parent) : convertView;
            bindView(view, position);
            return view;
        }

        private void bindView(final View view, final int position) {
            final MatchInfo match = getItem(position);
            final Holder holder = (Holder) view.getTag();
            holder.summary.setText(match.getSummary());
            holder.status.setText(match.getStatus());
            holder.creator.setText(match.getCreator());
            holder.age.setText(match.getAge());
        }

        private View newView(final ViewGroup root) {
            final View view = mInflater.inflate(R.layout.item_match, root, false);
            final Holder holder = new Holder();
            holder.summary = (TextView)view.findViewById(R.id.summary);
            holder.status = (TextView)view.findViewById(R.id.status);
            holder.creator = (TextView)view.findViewById(R.id.creator);
            holder.age = (TextView)view.findViewById(R.id.age);
            view.setTag(holder);
            return view;
        }
    }


    final ResultCallback<TurnBasedMultiplayer.LoadMatchesResult> mRefreshCallback = new ResultCallback<TurnBasedMultiplayer.LoadMatchesResult>() {
        @Override
        public void onResult(final TurnBasedMultiplayer.LoadMatchesResult loadMatchesResult) {

            mAdapter.update(loadMatchesResult.getMatches());
        }
    };

    private ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> mCreateMatchCallback = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
        @Override
        public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
            final TurnBasedMatch match = result.getMatch();
            Log.d(TAG, "match created - %s", match);
            mListener.openMatch(match);
        }
    };

}
