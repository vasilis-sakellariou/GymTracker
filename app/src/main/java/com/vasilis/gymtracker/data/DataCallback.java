package com.vasilis.gymtracker.data;

/**
 * Firebase answers asynchronously (a bit later, not immediately).
 * This interface says "when the data arrives, call this method".
 *
 * @param <T> the type of the data, e.g. List<Exercise>
 */
public interface DataCallback<T> {
    void onResult(T data);
}
