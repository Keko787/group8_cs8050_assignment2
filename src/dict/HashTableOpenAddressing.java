package dict;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hash table implementation using open addressing for collision resolution.
 * Supports dynamic resizing based on configurable load factor threshold.
 */
public class HashTableOpenAddressing<K, V> implements Dictionary<K, V> {

    //
    // Class setup and Constructors
    //

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

    private static final int DEFAULT_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.5;

    private Entry<K, V>[] table;
    private final HashFunction<K> hashFunction;
    private final ProbingStrategy probingStrategy;
    private final double loadFactorThreshold;
    private int size;
    private int deletedCount;

    public HashTableOpenAddressing(int capacity, HashFunction<K> hashFunction, ProbingStrategy probingStrategy) {
        this(capacity, hashFunction, probingStrategy, DEFAULT_LOAD_FACTOR);
    }

    @SuppressWarnings("unchecked")
    public HashTableOpenAddressing(int capacity, HashFunction<K> hashFunction, ProbingStrategy probingStrategy, double loadFactorThreshold) {
        this.table = (Entry<K, V>[]) new Entry[capacity];  // main data struct
        this.hashFunction = hashFunction;  // alogrithm to call either poly or sha256 hash
        this.probingStrategy = probingStrategy;  // select either linear or quadratic probing
        this.loadFactorThreshold = loadFactorThreshold;  // custom threshold factor to override default
        this.size = 0; // Number of key values currently being stored
        this.deletedCount = 0; // Count the size of deleted entries and resize when it gets too large
    }

    //
    // Helper Functions
    //

    private int getIndex(K key, int attempt) {  // find the index in array, using attempt to increment probe
        int hash = hashFunction.hash(key);  // gets an integer hash code from whether poly or sha256 hash
        int probe = probingStrategy.probe(attempt);  // set offset based on attempt amount with either linear or quadratic
        return ((hash & 0x7FFFFFFF) + probe) % table.length;  // return final index (0 to table.size()-1),
        // table.size will use the table that is currently being pointed to
    }

    //
    // Operator Methods
    //

    @Override
    public void put(K key, V value) {
        // Check if resize needed (including deleted entries in load calculation)
        if ((double) (size + deletedCount) / table.length >= loadFactorThreshold) {  // if the size and deleted size is bigger than threshold
            resize();  // resize if greater than load factor
        }

        // probing loop, i represents probing attempt
        for (int i = 0; i < table.length; i++) {  // for the length of the table
            int index = getIndex(key, i);  // find the given index of the key in the table, using probing technique in getIndex
            Entry<K, V> entry = table[index];  // assign it as an entry

            if (entry == null || entry.deleted) {  // assign it to a new/empty or deleted slot
                if (entry != null && entry.deleted) {  // if the slot was a deleted slot then decrease the delete count
                    deletedCount--;
                }
                table[index] = new Entry<>(key, value);  // assign the new key as an entry in the index
                size++;  // increment table size
                return;
            }

            if (entry.key.equals(key)) {  // if the key matches an existing key
                entry.value = value;  // update the existing entry by inserting the value
                return;
            }
        }
        throw new RuntimeException("Hash table is full");
    }

    @Override
    public Optional<V> get(K key) {
        for (int i = 0; i < table.length; i++) {  // for the length of the table
            int index = getIndex(key, i);  // find the index from the key, uses probe per attempt
            Entry<K, V> entry = table[index];  // get the entry from the index

            if (entry == null) {  // if the entry is null, return empty
                return Optional.empty();
            }

            if (!entry.deleted && entry.key.equals(key)) {  // if the entry is not deleted and matches with an existing key
                return Optional.of(entry.value);  // return the value from the entry
            }
        }
        return Optional.empty();  // if it couldnt find the entry in the table, return empty
    }

    @Override
    public void remove(K key) {
        for (int i = 0; i < table.length; i++) {  // for the length of the table
            int index = getIndex(key, i);  // find the index from the key, uses probe per attempt
            Entry<K, V> entry = table[index];  // get the entry from the index

            if (entry == null) {  // if the entry is null, return
                return;
            }

            if (!entry.deleted && entry.key.equals(key)) {  // if the entry is not deleted and matches an existing entry
                entry.deleted = true;  // set the entry as deleted
                size--;  // decrement table size
                deletedCount++;  // increment delete cound
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resize() {  // Resizes the table to double its current capacity and rehashes all entries. This also clears deleted markers.
        int newCapacity = table.length * 2;  // doubles size of table
        Entry<K, V>[] oldTable = table;  // saves old table as reference point

        // Create new table
        table = (Entry<K, V>[]) new Entry[newCapacity];  // make a new table based on the new capacity
        int oldSize = size;  // save the old size
        size = 0;  // make a new size
        deletedCount = 0;  // make a new deleted count

        // Rehash all active entries
        for (Entry<K, V> entry : oldTable) {  // for every entry in the old table
            if (entry != null && !entry.deleted) {  // if the entry is not null or deleted
                put(entry.key, entry.value);  // assign it to the new table
            }
        }
    }

    @Override
    public int size() {
        return size;  // returns the size of the pointed table
    }

    @Override
    public boolean containsKey(K key) {
        return get(key).isPresent();  // returns whether the key is present using get method
    }

    @Override
    public Iterable<K> keys() {  // collects all the keys in the table
        List<K> keys = new ArrayList<>();  // make an arraylist to collect keys
        for (Entry<K, V> entry : table) {  // for every entry in the table
            if (entry != null && !entry.deleted) {  // of the entry is not empty or deleted
                keys.add(entry.key);  // add the key to the key list
            }
        }
        return keys;  // return the keys
    }
}
