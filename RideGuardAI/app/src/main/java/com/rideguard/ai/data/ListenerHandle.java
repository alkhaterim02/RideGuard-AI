package com.rideguard.ai.data;

/** Returned by observe methods. Call remove() to stop listening. */
public interface ListenerHandle {
    void remove();
}
