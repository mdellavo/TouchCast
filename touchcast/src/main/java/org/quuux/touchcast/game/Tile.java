package org.quuux.touchcast.game;

import java.io.Serializable;

public class Tile implements Serializable {
    public String name;

    public boolean walkable = true;
    public boolean damaging = false;

    public Tile(final String name) {
        this.name = name;
    }
}
