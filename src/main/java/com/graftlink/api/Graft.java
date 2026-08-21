package com.graftlink.api;

public interface Graft {
    /**
     * Called when the mod is loaded during GraftLink initialization.
     */
    void onInitialize();

    /**
     * Called after all Grafts have completed their initialization.
     */
    default void onPostInitialize() {}
}