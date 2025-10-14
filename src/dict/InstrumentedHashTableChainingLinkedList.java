package dict;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Instrumented version of HashTableChainingLinkedList that tracks detailed metrics.
 */
public class InstrumentedHashTableChainingLinkedList<K, V> implements Dictionary<K, V> {
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

    private static final int DEFAULT_CAPACITY = 16;  // Starting size of hash table
    private static final double DEFAULT_LOAD_FACTOR = 0.75;  // Ratio of entries to buckets

    private List<LinkedList<Entry<K, V>>> table;  // main data struct - array of buckets with each bucket being a linked list of entries
    private final HashFunction<K> hashFunction;  // Algorithm to call either poly hash or SHA256 Hash
    private final double loadFactorThreshold;  // custom threshold factor to override default
    private int size;  // Number of key values currently being stored
    private final HashTableMetrics metrics;  // Load Metrics object
    private final boolean usePrimeCapacity;  // flag to turn on prime numbers for capacity

    public InstrumentedHashTableChainingLinkedList(int capacity, HashFunction<K> hashFunction,
                                        double loadFactorThreshold, boolean usePrimeCapacity) {
        int actualCapacity = usePrimeCapacity ? nextPrime(capacity) : capacity;
        this.table = new ArrayList<>(actualCapacity);
        for (int i = 0; i < actualCapacity; i++) {
            table.add(new LinkedList<>());
        }
        this.hashFunction = hashFunction;
        this.loadFactorThreshold = loadFactorThreshold;
        this.size = 0;
        this.metrics = new HashTableMetrics();
        this.usePrimeCapacity = usePrimeCapacity;
    }

    public InstrumentedHashTableChainingLinkedList(int capacity, HashFunction<K> hashFunction) {
        this(capacity, hashFunction, DEFAULT_LOAD_FACTOR, false);
    }

    //
    // Helper Functions
    //

    private int getBucket(K key) {  // Determines which bucket a key belongs to
        int hash = hashFunction.hash(key);  // gets an integer hash code from wither poly or sha256 hash
        return (hash & 0x7FFFFFFF) % table.size();  // return bucket index (0 to table.size()-1),
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

        // Check if resize needed before inserting
        if ((double) size / table.size() >= loadFactorThreshold) {// check the size to see if its past the load factor
            resize();  // resize if so
        }

        // Calculate which bucket the current key goes in,
        // then get the LinkedList Chain at that bucket
        int bucket = getBucket(key);
        LinkedList<Entry<K, V>> chain = table.get(bucket);

        int probes = 1;
        boolean collision = chain.size() > 0;

        // Update if key exists
        for (Entry<K, V> entry : chain) { // Search the chain
            probes++;  // increment probe count to signify attempts
            if (entry.key.equals(key)) {  // check if the keys match
                entry.value = value;  // if found, replicate old value with new, return early to prevent duplication
                long endTime = System.nanoTime();
                metrics.recordInsert(probes, endTime - startTime);  // record the timing of the insert and how many probes it took
                return;
            }
        }

        // Add new entry, if matching key is not found
        chain.add(new Entry<>(key, value));// create new entry to the chain with newly created entry object, and append at the end of the LinkedList
        size++;  // increment size total count

        if (collision) {
            metrics.recordCollision();
        }

        long endTime = System.nanoTime();
        metrics.recordInsert(probes, endTime - startTime);
    }

    @Override
    public Optional<V> get(K key) {
        long startTime = System.nanoTime();
        int bucket = getBucket(key);  // finds bucket for the key based on referenced table
        LinkedList<Entry<K, V>> chain = table.get(bucket);  // retrieve the chain of the found bucket

        int probes = 1;
        for (Entry<K, V> entry : chain) {  // for every entry in the chain
            if (entry.key.equals(key)) {  // check if it matches with a key
                long endTime = System.nanoTime();
                metrics.recordGet(probes, endTime - startTime);
                return Optional.of(entry.value);  // if it does, then return with the value of the entry
            }
            probes++;
        }

        long endTime = System.nanoTime();
        metrics.recordGet(probes, endTime - startTime);
        return Optional.empty();  // if it cant be found in the chain then it returns with an empty object
    }

    @Override
    public void remove(K key) {
        long startTime = System.nanoTime();
        int bucket = getBucket(key);  // finds bucket for the key based on referenced table
        LinkedList<Entry<K, V>> chain = table.get(bucket);  // retrieve the chain of the found bucket

        int probes = 1;
        boolean removed = chain.removeIf(entry -> {  // iterates through the chain, remove if
            if (entry.key.equals(key)) {  // if entry matches with desired key, remove it with remove_if
                size--;  // if found, reduce size of chain
                return true;  // return true tell remove_if to remove it and declare that the removal occured
            }
            return false;  // return false if the value is not found
        });

        long endTime = System.nanoTime();
        if (removed) {
            metrics.recordDelete(probes, endTime - startTime);
        }
    }

    private void resize() {  // Resizes the table to double its current capacity and rehashes all entries.
        metrics.recordResize();
        int newCapacity = table.size() * 2;  // double the size of the capacity for amortalized O(1) inserts
        if (usePrimeCapacity) {
            newCapacity = nextPrime(newCapacity);
        }

        List<LinkedList<Entry<K, V>>> oldTable = table;  // save reference, pointer to current table to copy entries from old to new

        // Create new table that allow the key to make references to that new table
        table = new ArrayList<>(newCapacity);  // make new array with double capacity
        for (int i = 0; i < newCapacity; i++) {  // add empty LinkedList for each bucket
            table.add(new LinkedList<>());  // adding the empty LinkList to the bucket
        }

        // Rehash all entries, by visiting every entry in old table
        for (LinkedList<Entry<K, V>> chain : oldTable) {  // for each bucket in the old table
            for (Entry<K, V> entry : chain) {  // for each entry in the bucket
                int bucket = getBucket(entry.key);  // Recalculate key into new bucket based on new capacity ((hash & 0x7FFFFFFF) % ((double))table.size())
                // getBucket references the new/current table
                table.get(bucket).add(entry);  table.get(bucket).add(entry); // add to new table
            }
        }
    }

    @Override
    public int size() {
        return size;  // return size attribute based on how many buckets are in the table
    }

    @Override
    public boolean containsKey(K key) {
        return get(key).isPresent();  // returns true or false on whether the key is present based on the get function on the key
    }

    @Override
    public Iterable<K> keys() {
        List<K> keys = new ArrayList<>();  // create a array list to collect all the keys
        for (LinkedList<Entry<K, V>> chain : table) {  // for every chain in the table
            for (Entry<K, V> entry : chain) {  // for every entry in the chain
                keys.add(entry.key);  // add the key in the entry to the key list
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
        return table.size();
    }

    public double getCurrentLoadFactor() {
        return (double) size / table.size();
    }

    /**
     * Returns distribution of chain lengths for collision analysis.
     */
    public int[] getChainLengthDistribution() {
        int maxLen = 0;
        for (LinkedList<Entry<K, V>> chain : table) {
            maxLen = Math.max(maxLen, chain.size());
        }

        int[] distribution = new int[maxLen + 1];
        for (LinkedList<Entry<K, V>> chain : table) {
            distribution[chain.size()]++;
        }
        return distribution;
    }
}
