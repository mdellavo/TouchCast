package org.quuux.touchcast.util;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.AtomicFile;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.game.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class PlayerLoader {

    private static final String TAG = Log.buildTag(PlayerLoader.class);

    public interface LoadListener {
        void onPlayerLoaded(Player player);
    }

    private static Player loadPlayer(final Context context) {

        final File cacheDir = context.getCacheDir();
        final File playerFile = new File(cacheDir, "pl");

        Player player = null;

        if (playerFile.exists()) {

            final AtomicFile aPlayerFile = new AtomicFile(playerFile);
            FileInputStream fin = null;
            ObjectInputStream in = null;

            try {

                fin = aPlayerFile.openRead();
                in = new ObjectInputStream(fin);
                player = (Player) in.readObject();

            } catch (IOException e) {
                Log.e(TAG, "error loading player", e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "error loading player", e);
            } finally {
                try {
                    if (in != null)
                        in.close();

                    if (fin != null)
                        fin.close();
                } catch (IOException e) {
                    Log.e(TAG, "error closing file", e);
                }
            }
        }

        if (player == null)
            player = generatePlayer();

        return player;
    }

    public static void loadPlayer(final Context context, final LoadListener listener) {
        final AsyncTask<Void, Void, Player> task = new AsyncTask<Void, Void, Player>() {
            @Override
            protected Player doInBackground(final Void... params) {
                Log.d(TAG, "loading player...");
                return loadPlayer(context);
            }

            @Override
            protected void onPostExecute(final Player player) {
                super.onPostExecute(player);
                if (listener != null) {
                    listener.onPlayerLoaded(player);
                }
            }
        };
        task.execute();
    }

    private static Player generatePlayer() {
        final Player player = new Player();
        player.uuid = UUID.randomUUID();
        player.name = String.format("player(%s)", System.currentTimeMillis());
        player.tileKey = "player";
        return player;
    }

    private static void doSavePlayer(final Context context, final Player player) {
        final File cacheDir = context.getCacheDir();
        final AtomicFile playerFile = new AtomicFile(new File(cacheDir, "pl"));

        FileOutputStream fout = null;
        ObjectOutputStream out = null;
        try {
            fout = playerFile.startWrite();
            out = new ObjectOutputStream(fout);
            out.writeObject(player);
            playerFile.finishWrite(fout);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "error saving player", e);
            if (fout != null)
                playerFile.failWrite(fout);
        } catch (IOException e) {
            Log.e(TAG, "error saving player", e);
            if (fout != null)
                playerFile.failWrite(fout);
        } finally {
            try {
                if (out != null)
                    out.close();
                if (fout != null)
                    fout.close();
            } catch (IOException e) {
                Log.e(TAG, "error closing file", e);
            }
        }
    }

    public static void savePlayer(final Context context, final Player player) {
        final AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(final Object[] params) {
                Log.d(TAG, "saving player...");
                doSavePlayer(context, player);
                return null;
            }
        };

        task.execute();
    }
}