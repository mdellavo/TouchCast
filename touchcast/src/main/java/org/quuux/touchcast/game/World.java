package org.quuux.touchcast.game;

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

    public enum Position {

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
        void setX(int x);
        void setY(int y);
        String getTile();
        String getName();
    }

    public abstract class BaseEntity implements Entity, Serializable {

        int x,y;

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public void setX(final int x) {
            this.x = x;
        }

        @Override
        public void setY(final int y) {
            this.y = y;
        }
    }

    public class PlayerEntity extends BaseEntity {

        final Player mPlayer;

        public PlayerEntity(final Player player) {
            mPlayer = player;
        }

        @Override
        public String getTile() {
            return mPlayer.tileKey;
        }

        @Override
        public String getName() {
            return mPlayer.name;
        }

        public Player getPlayer() {
            return mPlayer;
        }
    }

    public class NonPlayerEntity extends BaseEntity {

        private final String mTileKey;
        private final String mName;

        public NonPlayerEntity(final String name, final String tileKey) {
            mName = name;
            mTileKey = tileKey;
        }

        @Override
        public String getTile() {
            return mTileKey;
        }

        @Override
        public String getName() {
            return mName;
        }
    }

    public interface Action {

    }

    class SpellAction implements Action {
        private final Spell mSpell;

        public SpellAction(final Spell spell) {
            mSpell = spell;
        }
    }

    Map<String, Player> mPlayers = new HashMap<String, Player>();
    List<String> mParticipantOrder = new ArrayList<String>();
    List<Entity> mEntities = new ArrayList<Entity>();
    List<Action> mActionJournal = new ArrayList<Action>();

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
            objectOut.close();
            zout.close();
            out.close();
            final byte[] data = out.toByteArray();
            Log.d(TAG, "serialized world to %d bytes", data.length);
            return data;
        } catch (IOException e) {
            Log.d(TAG, "error serializing world: %s", e);
        }

        return null;
    }

    public static World unserialize(final byte[] data) {
        try {
            Log.d(TAG, "unserializing %d bytes", data.length);
            final ByteArrayInputStream in = new ByteArrayInputStream(data);
            final GZIPInputStream zin = new GZIPInputStream(in);
            final ObjectInputStream objectIn = new ObjectInputStream(zin);
            final World rv =  (World) objectIn.readObject();
            objectIn.close();
            zin.close();
            in.close();
            return rv;
        } catch (IOException e) {
            Log.e(TAG, "error unserializing world", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "error unserializing world", e);
        }

        return null;
    }

    public static World generate() {
        final World world = new World();

        world.setMap(MapLoader.loadDefault());

        for (int i=0; i<5; i++)
            world.addNonPlayerEntity();

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

    private void placeEntity(final Entity entity) {
        boolean placed = false;
        while(!placed) {
            final int x = mRandom.nextInt(getWidth());
            final int y = mRandom.nextInt(getHeight());
            if (scanEntity(x, y) == null) {
                placed = true;
                entity.setX(x);
                entity.setY(y);
                Log.d(TAG, "placed at %s,%s", x, y);
            }
        }
    }

    public void join(final String particiapntId, final Player player) {
        if (!mPlayers.containsKey(particiapntId)) {
            mPlayers.put(particiapntId, player);

            final PlayerEntity playerEntity = new PlayerEntity(player);
            placeEntity(playerEntity);
            mEntities.add(playerEntity);
        }
    }

    public void addNonPlayerEntity() {
        final NonPlayerEntity npc = new NonPlayerEntity("Orange Beast", "beast-orange");
        placeEntity(npc);
        mEntities.add(npc);
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

    public void castSpell(final Spell spell) {
        final SpellAction action = new SpellAction(spell);
        mActionJournal.add(action);
    }

}
