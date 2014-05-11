package org.quuux.touchcast.game;

import java.io.Serializable;

public class Spell implements Serializable {
    final String mName;

    public Spell(final String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }
}
