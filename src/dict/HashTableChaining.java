package dict;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Hash table implementation using chaining for collision resolution.
 */
public class HashTableChaining<K, V> implements Dictionary<K, V> {
    private static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final List<LinkedList<Entry<K, V>>> table;
    private final HashFunction<K> hashFunction;
    private int size;

    public HashTableChaining(int capacity, HashFunction<K> hashFunction) {
        this.table = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            table.add(new LinkedList<>());
        }
        this.hashFunction = hashFunction;
        this.size = 0;
    }

    private int getBucket(K key) {
        int hash = hashFunction.hash(key);
        return (hash & 0x7FFFFFFF) % table.size();
    }

    @Override
    public void put(K key, V value) {
        int bucket = getBucket(key);
        LinkedList<Entry<K, V>> chain = table.get(bucket);

        // Update if key exists
        for (Entry<K, V> entry : chain) {
            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }

        // Add new entry
        chain.add(new Entry<>(key, value));
        size++;
    }

    @Override
    public Optional<V> get(K key) {
        int bucket = getBucket(key);
        LinkedList<Entry<K, V>> chain = table.get(bucket);

        for (Entry<K, V> entry : chain) {
            if (entry.key.equals(key)) {
                return Optional.of(entry.value);
            }
        }
        return Optional.empty();
    }

    @Override
    public void remove(K key) {
        int bucket = getBucket(key);
        LinkedList<Entry<K, V>> chain = table.get(bucket);

        chain.removeIf(entry -> {
            if (entry.key.equals(key)) {
                size--;
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean containsKey(K key) {
        return get(key).isPresent();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Iterable<K> keys() {
        List<K> keys = new ArrayList<>();
        for (LinkedList<Entry<K, V>> chain : table) {
            for (Entry<K, V> entry : chain) {
                keys.add(entry.key);
            }
        }
        return keys;
    }
}
