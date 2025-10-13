package dict;

/**
 * A functional interface for hash functions. (prototype)
 */
@FunctionalInterface
public interface HashFunction<K> {
    /**
     * Computes a hash code for the given key.
     */
    int hash(K key);
}
