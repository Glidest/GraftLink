package com.graftlink.api.event;

public class GameEvents {

    /**
     * Fired every tick in Minicraft+.
     */
    public static class TickEvent {
        public static class Pre {}
        public static class Post {}
    }

    /**
     * Fired when the game initializes.
     */
    public static class GameInitEvent {}

    /**
     * Fired when a world is loaded.
     */
    public static class WorldLoadEvent {
        private final String worldName;

        public WorldLoadEvent(String worldName) {
            this.worldName = worldName;
        }

        public String getWorldName() {
            return worldName;
        }
    }
}