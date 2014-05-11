package org.quuux.touchcast.game;

import android.graphics.Point;

import java.io.Serializable;

/**
* Created by marc on 5/11/14.
*/
public class SpellAction implements World.Action, Serializable {
    private final Spell mSpell;
    private final World.Entity mTarget;
    private final World.Entity mActor;

    public SpellAction(final World.Entity actor, final Spell spell, final World.Entity target) {
        mActor = actor;
        mSpell = spell;
        mTarget = target;
    }

    public World.Entity getActor() {
        return mActor;
    }

    public Spell getSpell() {
        return mSpell;
    }

    public World.Entity getTarget() {
        return mTarget;
    }

    @Override
    public void execute() {

    }
}
