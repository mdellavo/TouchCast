package org.quuux.touchcast.game;

import android.graphics.Point;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.util.MapLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class World implements Serializable {

    private static final String TAG = Log.buildTag(World.class);

    enum Position {
        N(0, 1),
        NE(1, 1),
        E(1, 0),
        SE(1, -1),
        S(0, -1),
        SW(-1, -1),
        W(-1, 0),
        NW(-1, 1);

        float x,y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

    }

    public interface Entity {
        int getX();
        int getY();
        String getTile();
    }

    public class PlayerEntity implements Entity, Serializable {

        final Player mPlayer;
        int x,y;

        public PlayerEntity(final Player player) {
            mPlayer = player;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public String getTile() {
            return mPlayer.tileKey;
        }

        public Player getPlayer() {
            return mPlayer;
        }
    }


    Map<String, Player> mPlayers = new HashMap<String, Player>();
    List<String> mParticipantOrder = new ArrayList<String>();
    List<Entity> mEntities = new ArrayList<Entity>();

    WorldMap mMap;
    Random mRandom = new Random();

    protected World() {
    }

    public byte[] serialize() {

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final GZIPOutputStream zout = new GZIPOutputStream(out);
            final ObjectOutputStream objectOut = new ObjectOutputStream(zout);
            objectOut.writeObject(this);
            return out.toByteArray();
        } catch (IOException e) {
            Log.d(TAG, "error serializing world: %s", e);
        }

        return null;
    }

    public static World unserialize(final byte[] data) {
        try {

            final ByteArrayInputStream in = new ByteArrayInputStream(data);
            final GZIPInputStream zin = new GZIPInputStream(in);
            final ObjectInputStream objectIn = new ObjectInputStream(zin);
            return (World) objectIn.readObject();
        } catch (IOException e) {
            Log.d(TAG, "error unserializing world: %s", e);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "error unserializing world: %s", e);
        }

        return null;
    }

    public static World generate() {
        final World world = new World();

        world.setMap(MapLoader.loadDefault());

        return world;
    }

    private void setMap(final WorldMap map) {
        mMap = map;
    }

    public static World getInstance(TurnBasedMatch match) {
        final byte[] data = match.getData();
        final World world = data != null ? unserialize(data) : generate();
        return world;
    }

    public void join(final String particiapntId, final Player player) {
        if (!mPlayers.containsKey(particiapntId))
            mPlayers.put(particiapntId, player);

        final PlayerEntity playerEntity = new PlayerEntity(player);
        playerEntity.x = mRandom.nextInt(getWidth());
        playerEntity.y = mRandom.nextInt(getHeight());
        mEntities.add(playerEntity);
    }


    public int getWidth() {
        return mMap.mMap.length;
    }

    public int getHeight() {
        return mMap.mMap[0].length;
    }

    public Tile getTile(final int x, final int y) {
        return mMap.getTile(x, y);
    }

    public void setOrder(final List<String> participantIds) {
        mParticipantOrder.clear();
        mParticipantOrder.addAll(participantIds);
    }

    public int getNumPlayers() {
        return mPlayers.size();
    }

    public Player getPlayer(final int n) {
        return mPlayers.get(mParticipantOrder.get(n));
    }

    public List<Entity> getEntities() {
        return mEntities;
    }

    public Entity scanEntity(final int x, final int y) {

        for (Entity e : mEntities)
            if (e.getX() == x && e.getY() == y)
                return e;

        return null;
    }


}
