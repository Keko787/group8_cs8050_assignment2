package dict;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hash table implementation using open addressing for collision resolution.
 */
public class HashTableOpenAddressing<K, V> implements Dictionary<K, V> {
    private static class Entry<K, V> {
        K key;
        V value;
        boolean deleted;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.deleted = false;
        }
    }

    private final Entry<K, V>[] table;
    private final HashFunction<K> hashFunction;
    private final ProbingStrategy probingStrategy;
    private int size;

    @SuppressWarnings("unchecked")
    public HashTableOpenAddressing(int capacity, HashFunction<K> hashFunction, ProbingStrategy probingStrategy) {
        this.table = (Entry<K, V>[]) new Entry[capacity];
        this.hashFunction = hashFunction;
        this.probingStrategy = probingStrategy;
        this.size = 0;
    }

    private int getIndex(K key, int attempt) {
        int hash = hashFunction.hash(key);
        int probe = probingStrategy.probe(attempt);
        return ((hash & 0x7FFFFFFF) + probe) % table.length;
    }

    @Override
    public void put(K key, V value) {
        for (int i = 0; i < table.length; i++) {
            int index = getIndex(key, i);
            Entry<K, V> entry = table[index];

            if (entry == null || entry.deleted) {
                table[index] = new Entry<>(key, value);
                size++;
                return;
            }

            if (entry.key.equals(key)) {
                entry.value = value;
                return;
            }
        }
        throw new RuntimeException("Hash table is full");
    }

    @Override
    public Optional<V> get(K key) {
        for (int i = 0; i < table.length; i++) {
            int index = getIndex(key, i);
            Entry<K, V> entry = table[index];

            if (entry == null) {
                return Optional.empty();
            }

            if (!entry.deleted && entry.key.equals(key)) {
                return Optional.of(entry.value);
            }
        }
        return Optional.empty();
    }

    @Override
    public void remove(K key) {
        for (int i = 0; i < table.length; i++) {
            int index = getIndex(key, i);
            Entry<K, V> entry = table[index];

            if (entry == null) {
                return;
            }

            if (!entry.deleted && entry.key.equals(key)) {
                entry.deleted = true;
                size--;
                return;
            }
        }
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
        for (Entry<K, V> entry : table) {
            if (entry != null && !entry.deleted) {
                keys.add(entry.key);
            }
        }
        return keys;
    }
}
