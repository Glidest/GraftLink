package com.graftlink.api.event;

public class WorldEvents {

    public static class WorldGenEvent {
        private final String levelName;
        private final int depth;

        public WorldGenEvent(String levelName, int depth) {
            this.levelName = levelName;
            this.depth = depth;
        }

        public String getLevelName() {
            return levelName;
        }

        public int getDepth() {
            return depth;
        }
    }
}