package benchmark;

import java.util.*;

/**
 * Generates synthetic datasets with various distribution patterns for benchmarking.
 */
public class DataGenerator {

    //
    // Class Setup
    //

    public enum Distribution {
        UNIFORM,        // Uniform random strings
        POWER_LAW,      // Zipfian/power-law distribution
        ADVERSARIAL,    // Designed to cause maximum collisions
        CLUSTERED       // Keys that hash to similar values
    }

    private final Random random;

    public DataGenerator(long seed) {
        this.random = new Random(seed);
    }

    public DataGenerator() {
        this(System.currentTimeMillis());
    }

    //
    // Generation Operators
    //

    /**
     * Generate a list of keys according to the specified distribution.
     *
     * @param count Number of keys to generate
     * @param distribution Distribution pattern
     * @param uniqueRatio Ratio of unique keys (0.0 to 1.0)
     * @return List of generated keys
     */
    public List<String> generateKeys(int count, Distribution distribution, double uniqueRatio) {
        int uniqueCount = (int) (count * uniqueRatio);
        List<String> uniqueKeys = generateUniqueKeys(uniqueCount, distribution);

        if (uniqueRatio >= 1.0) {
            return uniqueKeys;
        }

        // Add duplicates based on distribution
        List<String> result = new ArrayList<>(count);
        result.addAll(uniqueKeys);

        while (result.size() < count) {
            // Pick a random unique key to duplicate
            String key = uniqueKeys.get(random.nextInt(uniqueKeys.size()));
            result.add(key);
        }

        Collections.shuffle(result, random);
        return result;
    }

    private List<String> generateUniqueKeys(int count, Distribution distribution) {
        switch (distribution) {
            case UNIFORM:
                return generateUniform(count);
            case POWER_LAW:
                return generatePowerLaw(count);
            case ADVERSARIAL:
                return generateAdversarial(count);
            case CLUSTERED:
                return generateClustered(count);
            default:
                throw new IllegalArgumentException("Unknown distribution: " + distribution);
        }
    }

    /**
     * Generate uniformly distributed random strings.
     */
    private List<String> generateUniform(int count) {
        Set<String> unique = new HashSet<>();
        int length = 8;

        while (unique.size() < count) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                char c = (char) ('a' + random.nextInt(26));
                sb.append(c);
            }
            unique.add(sb.toString());
        }

        return new ArrayList<>(unique);
    }

    /**
     * Generate keys following a power-law (Zipf) distribution.
     * A small number of keys appear very frequently.
     */
    private List<String> generatePowerLaw(int count) {
        List<String> base = generateUniform(count);
        List<String> result = new ArrayList<>();

        // Zipf distribution: frequency ~ 1/rank^s, where s=1.0
        double[] probabilities = new double[count];
        double sum = 0;
        for (int i = 0; i < count; i++) {
            probabilities[i] = 1.0 / (i + 1);
            sum += probabilities[i];
        }

        // Normalize
        for (int i = 0; i < count; i++) {
            probabilities[i] /= sum;
        }

        // Generate keys according to distribution
        for (int i = 0; i < count; i++) {
            double r = random.nextDouble();
            double cumulative = 0;
            for (int j = 0; j < count; j++) {
                cumulative += probabilities[j];
                if (r <= cumulative) {
                    result.add(base.get(j));
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Generate adversarial keys designed to cause maximum hash collisions.
     * Creates keys that produce the same or similar hash values.
     */
    private List<String> generateAdversarial(int count) {
        Set<String> unique = new HashSet<>();

        // Strategy 1: Keys with same polynomial hash modulo small prime
        // For polynomial hash with base 31, create keys that collide
        int base = 31;
        String prefix = "collision_";

        for (int i = 0; unique.size() < count / 2 && i < count * 2; i++) {
            // Create keys that will hash to similar values
            String key = prefix + i;
            unique.add(key);
        }

        // Strategy 2: Keys with systematic patterns
        for (int i = 0; unique.size() < count; i++) {
            // Create keys with same character sums
            int targetSum = 500;
            StringBuilder sb = new StringBuilder();
            int currentSum = 0;

            while (currentSum < targetSum - 25) {
                char c = (char) ('a' + random.nextInt(26));
                sb.append(c);
                currentSum += c;
            }

            // Fill remaining to reach target sum
            if (targetSum - currentSum > 0 && targetSum - currentSum <= 122) {
                sb.append((char) (targetSum - currentSum));
            }

            String key = sb.toString() + "_" + i;
            unique.add(key);
        }

        return new ArrayList<>(unique);
    }

    /**
     * Generate clustered keys that hash to nearby buckets.
     */
    private List<String> generateClustered(int count) {
        Set<String> unique = new HashSet<>();
        int numClusters = Math.max(1, count / 100); // ~100 keys per cluster

        for (int cluster = 0; cluster < numClusters && unique.size() < count; cluster++) {
            String clusterPrefix = "cluster" + cluster + "_";

            int keysInCluster = count / numClusters;
            for (int i = 0; i < keysInCluster && unique.size() < count; i++) {
                unique.add(clusterPrefix + i);
            }
        }

        // Fill remaining with uniform random
        while (unique.size() < count) {
            unique.add("filler_" + UUID.randomUUID().toString().substring(0, 8));
        }

        return new ArrayList<>(unique);
    }

//
// Operation Class Setup
//

    /**
     * Generate a mixed workload with insert/get/delete operations.
     */
    public static class Operation {
        public enum Type { INSERT, GET, DELETE }

        public final Type type;
        public final String key;
        public final Integer value;

        public Operation(Type type, String key, Integer value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }

//
// Helper method
//

        @Override
        public String toString() {
            return type + "(" + key + (value != null ? "," + value : "") + ")";
        }
    }

//
// Operation Method
//

    /**
     * Generate a workload with mixed operations.
     *
     * @param count Total number of operations
     * @param insertRatio Ratio of insert operations (0.0 to 1.0)
     * @param getRatio Ratio of get operations (0.0 to 1.0)
     * @param deleteRatio Ratio of delete operations (0.0 to 1.0)
     * @param distribution Distribution of keys
     */
    public List<Operation> generateWorkload(int count, double insertRatio, double getRatio,
                                           double deleteRatio, Distribution distribution) {
        // Normalize ratios
        double total = insertRatio + getRatio + deleteRatio;
        insertRatio /= total;
        getRatio /= total;
        deleteRatio /= total;

        // Generate base keys
        List<String> keys = generateKeys(count, distribution, 0.7);

        List<Operation> operations = new ArrayList<>();
        Set<String> insertedKeys = new HashSet<>();

        for (int i = 0; i < count; i++) {
            double r = random.nextDouble();
            String key = keys.get(random.nextInt(keys.size()));

            if (r < insertRatio) {
                operations.add(new Operation(Operation.Type.INSERT, key, random.nextInt(10000)));
                insertedKeys.add(key);
            } else if (r < insertRatio + getRatio) {
                operations.add(new Operation(Operation.Type.GET, key, null));
            } else if (!insertedKeys.isEmpty()) {
                // Only delete if we have inserted keys
                String deleteKey = insertedKeys.iterator().next();
                operations.add(new Operation(Operation.Type.DELETE, deleteKey, null));
                insertedKeys.remove(deleteKey);
            }
        }

        return operations;
    }
}
