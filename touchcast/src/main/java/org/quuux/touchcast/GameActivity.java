package org.quuux.touchcast;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;

import org.quuux.touchcast.game.Player;
import org.quuux.touchcast.ui.GestureView;
import org.quuux.touchcast.ui.MatchFragment;
import org.quuux.touchcast.ui.LobbyFragment;
import org.quuux.touchcast.ui.Recognizer;
import org.quuux.touchcast.util.PlayerLoader;
import org.quuux.touchcast.util.TileSet;


public class GameActivity
        extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener,
                   LobbyFragment.Listener,
                   MatchFragment.Listener,
                   TileSet.LoadListener, PlayerLoader.LoadListener {

    private static final String TAG = Log.buildTag(GameActivity.class);

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private static final String FRAG_LOBBY = "lobby";
    private static final String FRAG_GAME = "game";

    Recognizer mRecognizer = new Recognizer();
    GestureView mContentView;
    GoogleApiClient mGoogleApiClient;
    boolean mResolvingError;
    TileSet mTileSet;
    Player mPlayer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        setContentView(R.layout.activity_game);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addScope(Games.SCOPE_GAMES)
                .addApi(Games.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .setViewForPopups(getWindow().getDecorView())
                .build();

        TileSet.loadDefault(this, this);
        PlayerLoader.loadPlayer(this, this);
    }

    private void onShowLobby() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(FRAG_LOBBY);
        if (frag == null)
            frag = LobbyFragment.newInstance();

        fragReplace(frag, FRAG_LOBBY, false);
    }

    private void fragReplace(Fragment frag, final String tag, final boolean addToBackStack) {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.container, frag, tag);
        if (addToBackStack)
            transaction.addToBackStack(tag);
        transaction.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    public void onConnected(final Bundle bundle) {
        Log.d(TAG, "connected -> %s", bundle);
        onShowLobby();
    }

    @Override
    public void onConnectionSuspended(final int i) {

    }

    @Override
    public void onConnectionFailed(final ConnectionResult result) {
        Log.d(TAG, "connection failed - %s", result);

        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    @Override
    public GoogleApiClient getApiClient() {
        return mGoogleApiClient;
    }

    @Override
    public void openMatch(final TurnBasedMatch match) {
        final String tag = FRAG_GAME + match.getMatchId();
        Fragment frag = getSupportFragmentManager().findFragmentByTag(tag);
        if (frag == null)
            frag = MatchFragment.getInstance(match);
        fragReplace(frag, tag, true);
    }

    @Override
    public void onTileSetLoaded(final TileSet tileSet) {
        mTileSet = tileSet;
    }

    @Override
    public Player getPlayer() {
        return mPlayer;
    }

    @Override
    public TileSet getTileSet() {
        return mTileSet;
    }

    @Override
    public void onPlayerLoaded(final Player player) {
        mPlayer = player;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(final DialogInterface dialog) {
            ((GameActivity)getActivity()).onDialogDismissed();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    // Port of https://depts.washington.edu/aimgroup/proj/dollar/dollar.js

}
