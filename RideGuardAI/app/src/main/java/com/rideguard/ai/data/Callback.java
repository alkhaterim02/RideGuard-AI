package com.rideguard.ai.data;

/** Result callback. Exactly one of result / error is meaningful. */
public interface Callback<T> {
    void onResult(T result, String error);
}
