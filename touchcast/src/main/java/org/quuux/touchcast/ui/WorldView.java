package org.quuux.touchcast.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.game.Tile;
import org.quuux.touchcast.game.World;
import org.quuux.touchcast.util.TileSet;

public class WorldView extends View {
    private static final String TAG = Log.buildTag(WorldView.class);

    World mWorld;
    TileSet mTileset;
    Point mSelectedTile = new Point(-1, -1);
    float mScale = 1;
    Point mTranslation = new Point(0, 0);
    boolean mDisabled;
    World.Entity mSelectedEntity;

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

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (mTileset == null)
            return;

        final int width = getWidth();
        final int height = getHeight();
        final int mapWidth = mWorld.getWidth() * mTileset.getTileSize();
        final int mapHeight = mWorld.getHeight() * mTileset.getTileSize();
        mScale = Math.min((float)width / (float)mapWidth, (float)height / (float)mapHeight);
        mTranslation.x = (width - Math.round(mScale * mapWidth)) / 2;
        mTranslation.y = (height - Math.round(mScale * mapHeight)) / 2;

        canvas.translate(mTranslation.x, mTranslation.y);
        canvas.scale(mScale, mScale);

        canvas.drawRGB(51, 51, 51);

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

        for (World.Entity e : mWorld.getEntities()) {
            mTileset.drawTile(canvas, e);
        }

        if (mDisabled)
            canvas.drawARGB(192, 0, 0, 0);

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
        mDisabled = true;
    }

    public void enable() {
        mDisabled = false;
    }

    public Point getMapTranslation() {
        return mTranslation;
    }

    public float getMapScale() {
        return mScale;
    }
}
