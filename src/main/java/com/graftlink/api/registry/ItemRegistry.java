package com.graftlink.api.registry;

import minicraft.item.Item;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final Map<String, Item> CUSTOM_ITEMS = new HashMap<>();

    /**
     * Registers a custom item with GraftLink.
     */
    public static void registerItem(String id, Item item) {
        CUSTOM_ITEMS.put(id, item);
        System.out.println("[GraftLink API] Registered Custom Item: " + id);
    }

    public static Map<String, Item> getCustomItems() {
        return CUSTOM_ITEMS;
    }
}