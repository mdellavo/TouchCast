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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || ((Object)this).getClass() != o.getClass()) return false;

        final Player player = (Player) o;

        if (!uuid.equals(player.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
