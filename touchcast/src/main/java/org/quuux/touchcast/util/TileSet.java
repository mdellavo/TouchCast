package org.quuux.touchcast.util;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;

import org.quuux.touchcast.Log;
import org.quuux.touchcast.game.Player;
import org.quuux.touchcast.game.Tile;
import org.quuux.touchcast.game.World;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TileSet {

    private static final String TAG = Log.buildTag(TileSet.class);
    private final Bitmap mBitmap;
    private final int mSize;
    private final Rect mSrc;
    private final Rect mDst;
    private final Paint mPaint;

    private Map<String, Rect> mTiles = new HashMap<String, Rect>();

    public interface LoadListener {
        void onTileSetLoaded(TileSet tileSet);
    }

    protected TileSet(final Bitmap bitmap, final int tileSize) {
        mBitmap = bitmap;
        mSize = tileSize;

        mSrc = new Rect();
        mDst = new Rect();

        mPaint = new Paint();
        mPaint.setFilterBitmap(true);
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);
    }

    public int getTileSize() {
        return mSize;
    }

    public void setTile(final String name, int x, int y) {
        Rect tile = mTiles.get(name);
        if (tile == null) {
            tile = new Rect();
            mTiles.put(name, tile);
        }

        tile.set(x * mSize, y * mSize, (x + 1) * mSize, (y + 1) * mSize);
    }

    public void drawTile(final Canvas canvas, final String tile, int x, int y) {
        final Rect rect = mTiles.get(tile);
        if (tile != null) {
            mSrc.set(rect);
            mDst.set(x * mSize, y*mSize, (x + 1) * mSize, (y + 1) * mSize);
            Log.d(TAG, "(x,y)=%s,%s | src=%s %sx%s | dst=%s %sx%s", x, y, mSrc, mSrc.width(), mSrc.height(), mDst, mDst.width(), mDst.height());
            canvas.drawBitmap(mBitmap, mSrc, mDst, mPaint);
        }
    }

    public void drawTile(final Canvas canvas, final Tile tile, int x, int y) {
        drawTile(canvas, tile.name, x, y);
    }

    public void drawTile(final Canvas canvas, final Player player, final int x, final int y) {
        drawTile(canvas, player.tileKey, x, y);
    }

    public void drawTile(final Canvas canvas, final World.Entity e) {
        drawTile(canvas, e.getTile(), e.getX(), e.getY());
    }

    public static void load(final Context context, final String path, final int tileSize, final LoadListener listener) {
        final AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(final Void... params) {
                try {
                    Log.d(TAG, "loading tileset...");
                    final long t1 = System.currentTimeMillis();
                    final Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(path));
                    final long t2 = System.currentTimeMillis();
                    Log.d(TAG, "tileset loaded in %dms", t2-t1);
                    return bitmap;
                } catch (IOException e) {
                    Log.e(TAG, "error loading tileset", e);
                }

                return null;
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (listener != null) {
                    listener.onTileSetLoaded(new TileSet(bitmap, tileSize));
                }
            }
        };

        task.execute();
    }


    public static void loadDefault(final Context context, final LoadListener listener) {
        load(context, "tilesets/default-64x64.png", 64, new LoadListener() {
            @Override
            public void onTileSetLoaded(final TileSet tileSet) {

                tileSet.setTile("grass", 9, 23);
                tileSet.setTile("player", 9, 3);
                tileSet.setTile("selector", 65, 1);

                if (listener != null)
                    listener.onTileSetLoaded(tileSet);
            }
        });
    }


    }
