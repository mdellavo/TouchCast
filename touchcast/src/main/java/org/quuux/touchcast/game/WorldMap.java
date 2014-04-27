package org.quuux.touchcast.game;

import java.io.Serializable;

public class WorldMap implements Serializable {

    Tile[] mTiles;
    int[][] mMap;

    public WorldMap(int width, int height) {
        init(width, height);
    }

    void init(final int width, final int height) {
        mMap = new int[width][height];
    }

    public void setTiles(final Tile[] tiles) {
        mTiles = tiles;
    }

    public void setMap(final int[][] map) {
        mMap = map;
    }

    public Tile getTile(final int x, final int y) {
        return mTiles[mMap[x][y]];
    }

}
