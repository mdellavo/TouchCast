package org.quuux.touchcast.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.game.SpellAction;
import org.quuux.touchcast.game.Tile;
import org.quuux.touchcast.game.World;
import org.quuux.touchcast.util.TileSet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WorldView extends View {
    private static final String TAG = Log.buildTag(WorldView.class);
    private static final int FPS = 30;
    private static final long TICK = 1000/FPS;

    interface Sprite {
        boolean update(float lerp);
        void draw(Canvas canvas);
        PointF getPosition();
        String getTile();
    }

    World mWorld;
    TileSet mTileset;
    final Point mSelectedTile = new Point(-1, -1);
    float mScale = 1;
    final Point mTranslation = new Point(0, 0);
    boolean mDisabled;
    World.Entity mSelectedEntity;
    World mLastWorld;
    long mLast;
    int mLastJournal;
    final List<Sprite> mSprites = new LinkedList<Sprite>();

    public WorldView(final Context context) {
        super(context);
    }

    public WorldView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public WorldView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWorld(final World world) {
        mWorld = world;
        invalidate();
    }

    public void setTileSet(final TileSet tileSet) {
        mTileset = tileSet;
        invalidate();
    }

    private void drawWorld(final Canvas canvas, final World world, final float lerp) {

        for (int x=0; x<mWorld.getWidth(); x++) {
            for (int y=0; y<mWorld.getHeight(); y++) {
                final Tile tile = mWorld.getTile(x, y);
                mTileset.drawTile(canvas, tile, x, y);
            }
        }

        if (hasSelection()) {
            final String key = mSelectedEntity != null ? "target" : "selector";
            mTileset.drawTile(canvas, key, mSelectedTile.x, mSelectedTile.y);
        }

    }

    private void drawEntities(final Canvas canvas, final World world, final float lerp) {
        for (World.Entity e : mWorld.getEntities()) {
            mTileset.drawTile(canvas, e);
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (mLast == 0)
            mLast = System.currentTimeMillis();

        final long start = System.currentTimeMillis();

        canvas.drawRGB(51, 51, 51);

        if (mTileset == null)
            return;

        if (mWorld != null) {

            final int width = getWidth();
            final int height = getHeight();
            final int mapWidth = mWorld.getWidth() * mTileset.getTileSize();
            final int mapHeight = mWorld.getHeight() * mTileset.getTileSize();
            mScale = Math.min((float)width / (float)mapWidth, (float)height / (float)mapHeight);
            mTranslation.x = (width - Math.round(mScale * mapWidth)) / 2;
            mTranslation.y = (height - Math.round(mScale * mapHeight)) / 2;

            canvas.translate(mTranslation.x, mTranslation.y);
            canvas.scale(mScale, mScale);

            final World world;
            if (mLastWorld != null) {
                world = mLastWorld;
            } else {
                world = mWorld;
            }

            if (mLastJournal < mWorld.getJournal().size()) {
                Log.d(TAG,
                        "world journal at %s, last  journal at %s",
                        mWorld.getJournal().size(),
                        mLastJournal);

                for (int i = mLastJournal; i < mWorld.getJournal().size(); i++) {
                    final World.Action action = mWorld.getJournal().get(i);
                    final Sprite sprite = createSprite(action);
                    if (sprite != null)
                        mSprites.add(sprite);
                }

                mLastJournal = mWorld.getJournal().size();
            }

            final float lerp = (float)(start - mLast) / TICK;

            drawWorld(canvas, world, lerp);
            drawEntities(canvas, world, lerp);
            drawSprites(canvas, lerp);

            if (mDisabled)
                canvas.drawARGB(192, 0, 0, 0);
        }

        final long end = System.currentTimeMillis();
        final long sleep = TICK - (end - start);

        Log.d(TAG, "draw took %sms, sleeping for %sms", end - start, sleep);

        if (mWorld != null)
            postInvalidateDelayed(sleep);

        mLast = start;
    }

    private Sprite createSprite(final World.Action action) {

        Sprite rv = null;

        if (action instanceof SpellAction) {
            rv = new SpellSprite((SpellAction)action);
        } else {
            Log.d(TAG, "unhandled action - %s", action);
        }

        return rv;
    }

    private void drawSprites(final Canvas canvas, final float lerp) {
        final Set<Sprite> dead = new HashSet<Sprite>();
        for (final Sprite sprite : mSprites) {
            if (!sprite.update(lerp))
                dead.add(sprite);

            sprite.draw(canvas);
        }

        mSprites.removeAll(dead);
    }

    public void clearSelection() {

        if (mDisabled)
            return;

        if (hasSelection()) {
            mSelectedTile.x = mSelectedTile.y = -1;
            mSelectedEntity = null;
            invalidate();
        }
    }

    public boolean hasSelection() {
        return mSelectedTile.x > -1 && mSelectedTile.y > -1;
    }

    public World.Entity setSelection(float x, float y) {
        if (mDisabled)
            return null;

        clearSelection();

        if (x < mTranslation.x || y < mTranslation.y)
            return null;

        final int scaledMapWidth = (int) Math.round(mWorld.getWidth() * mTileset.getTileSize() * mScale);
        final int scaledMapHeight = (int) Math.round(mWorld.getHeight() * mTileset.getTileSize() * mScale);

        if (x > mTranslation.x + scaledMapWidth || y > mTranslation.y + scaledMapHeight)
            return null;

        mSelectedTile.x = (int) ((x - mTranslation.x) / mScale / mTileset.getTileSize());
        mSelectedTile.y = (int) ((y - mTranslation.y) / mScale / mTileset.getTileSize());

        Log.d(TAG, "selecting tile %s", mSelectedTile);
        invalidate();

        mSelectedEntity = mWorld.scanEntity(mSelectedTile.x, mSelectedTile.y);

        return mSelectedEntity;
    }

    public void disable() {
        clearSelection();
        mDisabled = true;
        invalidate();
    }

    public void enable() {
        mDisabled = false;
        invalidate();
    }

    public Point getMapTranslation() {
        return mTranslation;
    }

    public float getMapScale() {
        return mScale;
    }

    public void setInitialWorld(final World lastWorld) {
        mLastWorld = lastWorld;
    }

    abstract class BaseSprite implements Sprite {
        private static final int FRAMES = FPS;

        private float mProgress;
        private PointF mPosition = new PointF();

        abstract void tick(float progress);

        @Override
        public boolean update(final float lerp) {
            float progress = (lerp / (float)FRAMES);
            mProgress += progress;
            Log.d(TAG, "progress: %s (total %s)", progress, mProgress);
            tick(mProgress);
            return mProgress < 1;
        }

        @Override
        public void draw(final Canvas canvas) {
            mTileset.drawTile(canvas, getTile(), getPosition());
        }

        public void setPosition(final float x, final float y) {
            mPosition.set(x, y);
        }

        @Override
        public PointF getPosition() {
            return mPosition;
        }

    }

    class SpellSprite extends BaseSprite {

        private final SpellAction mAction;
        private AccelerateInterpolator mInterpolator = new AccelerateInterpolator();

        public SpellSprite(final SpellAction action) {
            mAction = action;
        }

        @Override
        public String getTile() {
            return "spell";
        }

        @Override
        void tick(final float progress) {
            final float lerp = mInterpolator.getInterpolation(progress);
            final float startX = mAction.getActor().getX();
            final float endX = mAction.getTarget().getX();
            final float startY = mAction.getActor().getY();
            final float endY = mAction.getTarget().getY();
            final float dx = endX - startX;
            final float dy = endY - startY;
            final float stepX = dx * lerp;
            final float stepY = dy * lerp;
            final float currentX = startX + stepX;
            final float currentY = startY + stepY;
//            Log.d(TAG, "casting spell - progress %s - lerp %s - %s,%s -> %s, %s (d = %s,%s) @ step %s,%s (current = %s,%s) ",
//                    progress, lerp, startX, startY, endX, endY, dx, dy, stepX, stepY, currentX, currentY);
            setPosition(currentX, currentY);
        }
    }
}
