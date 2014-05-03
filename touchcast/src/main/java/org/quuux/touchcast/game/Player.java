package org.quuux.touchcast.game;

import java.io.Serializable;
import java.util.UUID;

public class Player implements Serializable {
    public UUID uuid;
    public int version;

    public String name;
    public int stamina;
    public int timeUnits;

    public int experience;
    public int deaths;
    public int matches;
    public int wins;
    public int kills;

    public String tileKey;
}
