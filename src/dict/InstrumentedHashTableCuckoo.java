package dict;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Instrumented version of HashTableCuckoo that tracks detailed metrics.
 * Uses two hash functions and two tables to guarantee O(1) worst-case lookup time.
 */
public class InstrumentedHashTableCuckoo<K, V> implements Dictionary<K, V> {

    //
    // Class setup and Constructors
    //

    private static class Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final int DEFAULT_CAPACITY = 16;
    private static final int MAX_EVICTIONS = 500; // Maximum number of evictions before rehashing

    private Entry<K, V>[] table1; // First hash table
    private Entry<K, V>[] table2; // Second hash table
    private final HashFunction<K> hashFunction1; // First hash function
    private final HashFunction<K> hashFunction2; // Second hash function
    private int size; // Number of key-value pairs stored
    private int capacity; // Current capacity of each table
    private final HashTableMetrics metrics; // Performance metrics tracker
    private long totalEvictions = 0; // Track total number of evictions

    public InstrumentedHashTableCuckoo(int capacity, HashFunction<K> hashFunction1, HashFunction<K> hashFunction2) {
        this.capacity = capacity;
        this.hashFunction1 = hashFunction1;
        this.hashFunction2 = hashFunction2;
        this.size = 0;
        this.metrics = new HashTableMetrics();
        initializeTables(capacity);
    }

    //
    // Helper Functions
    //

    @SuppressWarnings("unchecked")
    private void initializeTables(int capacity) {
        // Initialize both tables with the given capacity
        this.table1 = (Entry<K, V>[]) new Entry[capacity];
        this.table2 = (Entry<K, V>[]) new Entry[capacity];
    }

    private int hash1(K key) {
        // First hash function - maps key to index in table1
        int hash = hashFunction1.hash(key);
        return (hash & 0x7FFFFFFF) % capacity;
    }

    private int hash2(K key) {
        // Second hash function - maps key to index in table2
        int hash = hashFunction2.hash(key);
        return (hash & 0x7FFFFFFF) % capacity;
    }

    //
    // Operator Methods
    //

    @Override
    public void put(K key, V value) {
        long startTime = System.nanoTime();
        int probes = 0; // Track probes for this operation

        // Check if key already exists in either table and update
        int idx1 = hash1(key);
        int idx2 = hash2(key);
        probes += 2; // Checked both positions

        if (table1[idx1] != null && table1[idx1].key.equals(key)) {
            table1[idx1].value = value;
            long endTime = System.nanoTime();
            metrics.recordInsert(probes, endTime - startTime);
            return;
        }

        if (table2[idx2] != null && table2[idx2].key.equals(key)) {
            table2[idx2].value = value;
            long endTime = System.nanoTime();
            metrics.recordInsert(probes, endTime - startTime);
            return;
        }

        // Try to insert new entry
        Entry<K, V> newEntry = new Entry<>(key, value);

        // Try inserting into table1 first
        if (table1[idx1] == null) {
            table1[idx1] = newEntry;
            size++;
            long endTime = System.nanoTime();
            metrics.recordInsert(probes, endTime - startTime);
            return;
        }

        // Try inserting into table2
        if (table2[idx2] == null) {
            table2[idx2] = newEntry;
            size++;
            long endTime = System.nanoTime();
            metrics.recordInsert(probes, endTime - startTime);
            return;
        }

        // Both positions occupied - perform cuckoo eviction
        metrics.recordCollision(); // Record collision since we need to evict
        int evictions = insertWithEviction(newEntry);

        if (evictions == -1) {
            // Eviction failed (cycle detected) - rehash and retry
            rehash();
            put(key, value); // Retry insertion after rehashing
        } else {
            totalEvictions += evictions;
            long endTime = System.nanoTime();
            metrics.recordInsert(probes + evictions, endTime - startTime);
        }
    }

    private int insertWithEviction(Entry<K, V> entry) {
        // Attempt to insert entry by evicting existing entries
        // Returns number of evictions performed, or -1 if failed
        Entry<K, V> current = entry;
        boolean useTable1 = true; // Start by trying to insert into table1

        for (int i = 0; i < MAX_EVICTIONS; i++) {
            if (useTable1) {
                int idx = hash1(current.key);
                Entry<K, V> evicted = table1[idx];
                table1[idx] = current;

                if (evicted == null) {
                    size++;
                    return i; // Return number of evictions
                }

                current = evicted;
                useTable1 = false; // Next iteration use table2
            } else {
                int idx = hash2(current.key);
                Entry<K, V> evicted = table2[idx];
                table2[idx] = current;

                if (evicted == null) {
                    size++;
                    return i; // Return number of evictions
                }

                current = evicted;
                useTable1 = true; // Next iteration use table1
            }
        }

        // Max evictions reached - cycle detected
        return -1;
    }

    @Override
    public Optional<V> get(K key) {
        long startTime = System.nanoTime();
        int probes = 0;

        // Check table1
        int idx1 = hash1(key);
        probes++;
        if (table1[idx1] != null && table1[idx1].key.equals(key)) {
            long endTime = System.nanoTime();
            metrics.recordGet(probes, endTime - startTime);
            return Optional.of(table1[idx1].value);
        }

        // Check table2
        int idx2 = hash2(key);
        probes++;
        if (table2[idx2] != null && table2[idx2].key.equals(key)) {
            long endTime = System.nanoTime();
            metrics.recordGet(probes, endTime - startTime);
            return Optional.of(table2[idx2].value);
        }

        long endTime = System.nanoTime();
        metrics.recordGet(probes, endTime - startTime);
        return Optional.empty();
    }

    @Override
    public void remove(K key) {
        long startTime = System.nanoTime();
        int probes = 0;

        // Check table1
        int idx1 = hash1(key);
        probes++;
        if (table1[idx1] != null && table1[idx1].key.equals(key)) {
            table1[idx1] = null;
            size--;
            long endTime = System.nanoTime();
            metrics.recordDelete(probes, endTime - startTime);
            return;
        }

        // Check table2
        int idx2 = hash2(key);
        probes++;
        if (table2[idx2] != null && table2[idx2].key.equals(key)) {
            table2[idx2] = null;
            size--;
            long endTime = System.nanoTime();
            metrics.recordDelete(probes, endTime - startTime);
        }
    }

    private void rehash() {
        // Double the capacity and rehash all entries
        metrics.recordResize();
        int newCapacity = capacity * 2;
        Entry<K, V>[] oldTable1 = table1;
        Entry<K, V>[] oldTable2 = table2;
        int oldSize = size;

        // Reset and create new tables
        this.capacity = newCapacity;
        this.size = 0;
        initializeTables(newCapacity);

        // Reinsert all entries from table1
        for (Entry<K, V> entry : oldTable1) {
            if (entry != null) {
                put(entry.key, entry.value);
            }
        }

        // Reinsert all entries from table2
        for (Entry<K, V> entry : oldTable2) {
            if (entry != null) {
                put(entry.key, entry.value);
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean containsKey(K key) {
        return get(key).isPresent();
    }

    @Override
    public Iterable<K> keys() {
        List<K> keys = new ArrayList<>();

        // Collect keys from table1
        for (Entry<K, V> entry : table1) {
            if (entry != null) {
                keys.add(entry.key);
            }
        }

        // Collect keys from table2
        for (Entry<K, V> entry : table2) {
            if (entry != null) {
                keys.add(entry.key);
            }
        }

        return keys;
    }

    //
    // Benchmark Methods
    //

    public HashTableMetrics getMetrics() {
        return metrics;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getCurrentLoadFactor() {
        // For cuckoo hashing, load factor considers both tables
        return (double) size / (2 * capacity);
    }

    public long getTotalEvictions() {
        return totalEvictions;
    }

    /**
     * Returns the utilization of each table separately.
     */
    public double[] getTableUtilizations() {
        int count1 = 0;
        int count2 = 0;

        for (Entry<K, V> entry : table1) {
            if (entry != null) count1++;
        }

        for (Entry<K, V> entry : table2) {
            if (entry != null) count2++;
        }

        return new double[]{
            (double) count1 / capacity,
            (double) count2 / capacity
        };
    }

    /**
     * For cuckoo hashing, worst-case lookup is always 2 probes (one per table).
     * This method returns statistics about eviction chains.
     */
    public double getAverageEvictionsPerInsert() {
        return metrics.getInsertCount() == 0 ? 0 : (double) totalEvictions / metrics.getInsertCount();
    }
}
