package com.graftlink.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    /**
     * Registers a listener callback for a specific event type.
     */
    @SuppressWarnings("unchecked")
    public <T> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
                 .add((Consumer<Object>) listener);
    }

    /**
     * Posts an event to all registered listeners.
     */
    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    System.err.println("[GraftLink] Error dispatching event " + event.getClass().getName());
                    e.printStackTrace();
                }
            }
        }
        return event;
    }
}