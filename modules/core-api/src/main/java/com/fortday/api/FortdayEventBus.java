package com.fortday.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FortdayEventBus<T> {
    private final List<Consumer<T>> subscribers = new ArrayList<>();

    public void subscribe(Consumer<T> consumer) {
        subscribers.add(consumer);
    }

    public void publish(T event) {
        for (Consumer<T> subscriber : subscribers) {
            subscriber.accept(event);
        }
    }
}
