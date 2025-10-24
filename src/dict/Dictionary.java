package dict;

import java.util.Optional;

/**
 * A dictionary interface mapping keys to values.=
 */
public interface Dictionary<K, V> {
    /**
     * Associates the specified value with the specified key.
     */
    void put(K key, V value);

    /**
     * Returns the value associated with the specified key.
     */
    Optional<V> get(K key);

    /**
     * Removes the mapping for the specified key if present.
     */
    void remove(K key);

    /**
     * Returns true if this dictionary contains the specified key.
     */
    boolean containsKey(K key);

    /**
     * Returns the number of key-value mappings.
     */
    int size();

    /**
     * Returns an iterable of all keys.
     */
    Iterable<K> keys();
}
