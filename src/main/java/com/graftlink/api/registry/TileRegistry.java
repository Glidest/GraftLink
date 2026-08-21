package com.graftlink.api.registry;

import minicraft.level.tile.Tile;
import java.util.HashMap;
import java.util.Map;

public class TileRegistry {
    private static final Map<String, Tile> CUSTOM_TILES = new HashMap<>();

    /**
     * Registers a custom block/tile with GraftLink.
     */
    public static void registerTile(String id, Tile tile) {
        CUSTOM_TILES.put(id, tile);
        System.out.println("[GraftLink API] Registered Custom Tile: " + id);
    }

    public static Map<String, Tile> getCustomTiles() {
        return CUSTOM_TILES;
    }
}