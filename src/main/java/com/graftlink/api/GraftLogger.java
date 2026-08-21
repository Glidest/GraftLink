package com.graftlink.api.util;

public class GraftLogger {
    private final String modId;

    public GraftLogger(String modId) {
        this.modId = modId;
    }

    public void info(String message) {
        System.out.println("[" + modId + "] INFO: " + message);
    }

    public void warn(String message) {
        System.out.println("[" + modId + "] WARN: " + message);
    }

    public void error(String message) {
        System.err.println("[" + modId + "] ERROR: " + message);
    }

    public void error(String message, Throwable t) {
        System.err.println("[" + modId + "] ERROR: " + message);
        t.printStackTrace();
    }
}