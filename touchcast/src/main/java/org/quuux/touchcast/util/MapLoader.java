package org.quuux.touchcast.util;

import org.quuux.touchcast.game.Tile;
import org.quuux.touchcast.game.WorldMap;

public class MapLoader {

   public static WorldMap loadDefault() {

       final WorldMap map = new WorldMap(11, 11);

       final Tile[] tiles = new Tile[] {
               new Tile("grass")
       };

       map.setTiles(tiles);
       return map;
   }

}
