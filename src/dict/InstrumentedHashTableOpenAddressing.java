package dict;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Instrumented version of HashTableOpenAddressing that tracks detailed metrics.
 */
public class InstrumentedHashTableOpenAddressing<K, V> implements Dictionary<K, V> {
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
    private final HashTableMetrics metrics;
    private final boolean usePrimeCapacity;

    public InstrumentedHashTableOpenAddressing(int capacity, HashFunction<K> hashFunction,
                                              ProbingStrategy probingStrategy,
                                              double loadFactorThreshold,
                                              boolean usePrimeCapacity) {
        int actualCapacity = usePrimeCapacity ? nextPrime(capacity) : capacity;
        this.table = createTable(actualCapacity);
        this.hashFunction = hashFunction;  // alogrithm to call either poly or sha256 hash
        this.probingStrategy = probingStrategy;  // select either linear or quadratic probing
        this.loadFactorThreshold = loadFactorThreshold;  // custom threshold factor to override default
        this.size = 0; // Number of key values currently being stored
        this.deletedCount = 0; // Count the size of deleted entries and resize when it gets too large
        this.metrics = new HashTableMetrics();
        this.usePrimeCapacity = usePrimeCapacity;
    }

    public InstrumentedHashTableOpenAddressing(int capacity, HashFunction<K> hashFunction,
                                              ProbingStrategy probingStrategy) {
        this(capacity, hashFunction, probingStrategy, DEFAULT_LOAD_FACTOR, false);
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] createTable(int capacity) {
        return (Entry<K, V>[]) new Entry[capacity];
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

    private static int nextPrime(int n) {
        if (n <= 2) return 2;
        if (n % 2 == 0) n++;

        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    private static boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;

        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    //
    // Operator Methods
    //

    @Override
    public void put(K key, V value) {
        long startTime = System.nanoTime();

        // Check if resize needed (including deleted entries in load calculation)
        if ((double) (size + deletedCount) / table.length >= loadFactorThreshold) {  // if the size and deleted size is bigger than threshold
            resize();  // resize if greater than load factor
        }

        int probes = 0;
        boolean hadCollision = false;

        // probing loop
        for (int i = 0; i < table.length; i++) {  // for the length of the table
            probes++;
            int index = getIndex(key, i);  // find the given index of the key in the table, using probing technique in getIndex
            Entry<K, V> entry = table[index];  // assign it as an entry

            if (entry == null || entry.deleted) {  // assign it to an empty or deleted slot
                if (entry != null && entry.deleted) {  // if the slot was a deleted slot then decrease the delete count
                    deletedCount--;
                }
                if (probes > 1) {
                    hadCollision = true;
                }
                table[index] = new Entry<>(key, value);  // assign the new key as an entry in the index
                size++;  // increment table size
                long endTime = System.nanoTime();
                if (hadCollision) {
                    metrics.recordCollision();
                }
                metrics.recordInsert(probes, endTime - startTime);
                return;
            }

            if (entry.key.equals(key)) {  // if the key matches an existing key
                entry.value = value;  // insert the value
                long endTime = System.nanoTime();
                metrics.recordInsert(probes, endTime - startTime);
                return;
            }

            // if this is true, a collision occur
            if (probes == 1) {
                hadCollision = true;
            }
        }

        throw new RuntimeException("Hash table is full");
    }

    @Override
    public Optional<V> get(K key) {
        long startTime = System.nanoTime();
        int probes = 0;

        for (int i = 0; i < table.length; i++) {  // for the length of the table
            probes++;
            int index = getIndex(key, i);  // find the index from the key, uses probe per attempt
            Entry<K, V> entry = table[index];  // get the entry from the index

            if (entry == null) {  // if the entry is null, return empty
                long endTime = System.nanoTime();
                metrics.recordGet(probes, endTime - startTime);
                return Optional.empty();
            }

            if (!entry.deleted && entry.key.equals(key)) {  // if the entry is not deleted and matches with an existing key
                long endTime = System.nanoTime();
                metrics.recordGet(probes, endTime - startTime);
                return Optional.of(entry.value);
            }
        }

        long endTime = System.nanoTime();
        metrics.recordGet(probes, endTime - startTime);
        return Optional.empty();
    }

    @Override
    public void remove(K key) {
        long startTime = System.nanoTime();
        int probes = 0;

        for (int i = 0; i < table.length; i++) {  // for the length of the table
            probes++;
            int index = getIndex(key, i);  // find the index from the key, uses probe per attempt
            Entry<K, V> entry = table[index];  // get the entry from the index

            if (entry == null) {  // if the entry is null, return
                return;
            }

            if (!entry.deleted && entry.key.equals(key)) {  // if the entry is not deleted and matches an existing entry
                entry.deleted = true;  // set the entry as deleted
                size--;  // decrement table size
                deletedCount++;  // increment delete cound
                long endTime = System.nanoTime();
                metrics.recordDelete(probes, endTime - startTime);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void resize() {  // Resizes the table to double its current capacity and rehashes all entries. This also clears deleted markers.
        metrics.recordResize();
        int newCapacity = table.length * 2;  // doubles size of table
        if (usePrimeCapacity) {
            newCapacity = nextPrime(newCapacity);
        }

        Entry<K, V>[] oldTable = table;  // saves old table as reference point

        // Create new table
        table = createTable(newCapacity);
        int oldSize = size;  // save the old size
        size = 0;  // make a new size
        deletedCount = 0;  // make a new deleted count

        // Rehash all active entries
        for (Entry<K, V> entry : oldTable) {  // for every entry in the old table
            if (entry != null && !entry.deleted) {  // if the entry is not null or deleted
                // Temporarily disable metrics during resize
                HashTableMetrics tempMetrics = new HashTableMetrics();
                put(entry.key, entry.value);  // assign it to the new table
            }
        }
    }

    @Override
    public int size() {
        return size;
    }  // returns the size of the pointed table

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
        return keys;
    }

    //
    // Benchmark Methods
    //

    public HashTableMetrics getMetrics() {
        return metrics;
    }

    public int getCapacity() {
        return table.length;
    }

    public double getCurrentLoadFactor() {
        return (double) size / table.length;
    }

    /**
     * Returns probe sequence lengths for all entries (for clustering analysis).
     */
    public int[] getProbeSequenceLengths() {
        List<Integer> lengths = new ArrayList<>();

        for (int i = 0; i < table.length; i++) {
            Entry<K, V> entry = table[i];
            if (entry != null && !entry.deleted) {
                // Find how many probes it takes to find this entry
                int probes = 0;
                for (int j = 0; j < table.length; j++) {
                    probes++;
                    int index = getIndex(entry.key, j);
                    if (index == i) {
                        lengths.add(probes);
                        break;
                    }
                }
            }
        }

        return lengths.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Returns the number of clustering groups and their sizes.
     */
    public int getClusterCount() {
        int clusters = 0;
        boolean inCluster = false;

        for (Entry<K, V> entry : table) {
            if (entry != null && !entry.deleted) {
                if (!inCluster) {
                    clusters++;
                    inCluster = true;
                }
            } else {
                inCluster = false;
            }
        }

        return clusters;
    }
}
