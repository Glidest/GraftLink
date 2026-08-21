package com.graftlink.api;

import com.graftlink.api.event.EventBus;
import com.graftlink.api.util.GraftLogger;

public class GraftAPI {

    /**
     * Gets the global GraftLink EventBus.
     */
    public static EventBus getEventBus() {
        return EventBus.getInstance();
    }

    /**
     * Obtains a formatted logger for a given mod ID.
     */
    public static GraftLogger getLogger(String modId) {
        return new GraftLogger(modId);
    }

    /**
     * Obtains a formatted logger using a Graft class's @GraftInfo annotation.
     */
    public static GraftLogger getLogger(Class<?> clazz) {
        GraftInfo info = clazz.getAnnotation(GraftInfo.class);
        String id = (info != null) ? info.id() : clazz.getSimpleName();
        return new GraftLogger(id);
    }
}