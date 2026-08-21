package com.graftlink.api.event;

public class InputEvents {

    public static class KeyPressEvent {
        private final int keycode;

        public KeyPressEvent(int keycode) {
            this.keycode = keycode;
        }

        public int getKeycode() {
            return keycode;
        }
    }
}