package benchmark;

import dict.*;

import java.util.List;

/**
 * Executes benchmark experiments and collects detailed metrics.
 */
public class BenchmarkRunner {

    //
    // Class Setup
    //

    private final DataGenerator dataGenerator;

    public BenchmarkRunner(long seed) {
        this.dataGenerator = new DataGenerator(seed);
    }

    public BenchmarkRunner() {
        this(42); // Fixed seed for reproducibility
    }

    //
    // Helper methods
    //

    private HashFunction<String> createHashFunction(BenchmarkConfig.HashType hashType) {
        switch (hashType) {
            case POLYNOMIAL:
                return new PolyHash();
            case SHA256:
                return new SHA256Hash();
            default:
                throw new IllegalArgumentException("Unknown hash type: " + hashType);
        }
    }

    //
    // Benchmark Method
    //

    /**
     * Run a single benchmark with the given configuration.
     */
    public BenchmarkResult runBenchmark(BenchmarkConfig config) {
        // Generate test data
        List<String> keys = dataGenerator.generateKeys(
            config.operationCount,
            config.distribution,
            config.uniqueRatio
        );

        // Create hash table based on configuration
        boolean usePrime = config.capacityType == BenchmarkConfig.CapacityType.PRIME;
        HashFunction<String> hashFunction = createHashFunction(config.hashType);

        // Memory measurement before
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTime = System.nanoTime();

        BenchmarkResult result = null;

        switch (config.implementation) {
            case CHAINING_LL:
                result = runChainingBenchmark(config, keys, hashFunction, usePrime);
                break;

            case OPEN_LINEAR:
                result = runOpenAddressingBenchmark(config, keys, hashFunction,
                    ProbingStrategy.linear(), usePrime);
                break;

            case OPEN_QUADRATIC:
                result = runOpenAddressingBenchmark(config, keys, hashFunction,
                    ProbingStrategy.quadratic(1, 3), usePrime);
                break;

            default:
                throw new IllegalArgumentException("Unsupported implementation: " + config.implementation);
        }

        return result;
    }

    private BenchmarkResult runChainingBenchmark(BenchmarkConfig config, List<String> keys,
                                                 HashFunction<String> hashFunction, boolean usePrime) {
        InstrumentedHashTableChainingLinkedList<String, Integer> table =
            new InstrumentedHashTableChainingLinkedList<>(config.initialCapacity, hashFunction,
                config.loadFactor, usePrime);

        long startTime = System.nanoTime();

        // Perform operations
        for (int i = 0; i < keys.size(); i++) {
            table.put(keys.get(i), i);
        }

        long endTime = System.nanoTime();

        // Memory measurement after
        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = Math.max(0, memoryAfter);

        return new BenchmarkResult(
            config,
            table.getMetrics(),
            endTime - startTime,
            table.size(),
            table.getCapacity(),
            table.getCurrentLoadFactor(),
            memoryUsed,
            table.getChainLengthDistribution(),
            null,
            0
        );
    }

    private BenchmarkResult runOpenAddressingBenchmark(BenchmarkConfig config, List<String> keys,
                                                       HashFunction<String> hashFunction,
                                                       ProbingStrategy strategy, boolean usePrime) {
        InstrumentedHashTableOpenAddressing<String, Integer> table =
            new InstrumentedHashTableOpenAddressing<>(config.initialCapacity, hashFunction,
                strategy, config.loadFactor, usePrime);

        long startTime = System.nanoTime();

        // Perform operations
        for (int i = 0; i < keys.size(); i++) {
            table.put(keys.get(i), i);
        }

        long endTime = System.nanoTime();

        // Memory measurement after
        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = Math.max(0, memoryAfter);

        return new BenchmarkResult(
            config,
            table.getMetrics(),
            endTime - startTime,
            table.size(),
            table.getCapacity(),
            table.getCurrentLoadFactor(),
            memoryUsed,
            null,
            table.getProbeSequenceLengths(),
            table.getClusterCount()
        );
    }

    /**
     * Run a comprehensive suite of benchmarks with varying parameters.
     */
    public BenchmarkSuite runComprehensiveSuite() {
        BenchmarkSuite suite = new BenchmarkSuite();

        // Test load factors
        double[] loadFactors = {0.25, 0.5, 0.75, 0.9, 0.95};

        // Test distributions
        DataGenerator.Distribution[] distributions = {
            DataGenerator.Distribution.UNIFORM,
            DataGenerator.Distribution.POWER_LAW,
            DataGenerator.Distribution.ADVERSARIAL,
            DataGenerator.Distribution.CLUSTERED
        };

        // Test implementations
        BenchmarkConfig.Implementation[] implementations = {
            BenchmarkConfig.Implementation.CHAINING_LL,
            BenchmarkConfig.Implementation.OPEN_LINEAR,
            BenchmarkConfig.Implementation.OPEN_QUADRATIC
        };

        // Test hash functions
        BenchmarkConfig.HashType[] hashTypes = {
            BenchmarkConfig.HashType.POLYNOMIAL,
            BenchmarkConfig.HashType.SHA256
        };

        // Test capacity types
        BenchmarkConfig.CapacityType[] capacityTypes = {
            BenchmarkConfig.CapacityType.POWER_OF_TWO,
            BenchmarkConfig.CapacityType.PRIME
        };

        int totalTests = loadFactors.length * distributions.length * implementations.length *
                        hashTypes.length * capacityTypes.length;
        int currentTest = 0;

        System.out.println("Running comprehensive benchmark suite...");
        System.out.println("Total configurations: " + totalTests);
        System.out.println();

        for (BenchmarkConfig.Implementation impl : implementations) {
            for (BenchmarkConfig.HashType hashType : hashTypes) {
                for (DataGenerator.Distribution dist : distributions) {
                    for (double lf : loadFactors) {
                        // Skip high load factors for open addressing
                        if ((impl == BenchmarkConfig.Implementation.OPEN_LINEAR ||
                             impl == BenchmarkConfig.Implementation.OPEN_QUADRATIC) && lf > 0.75) {
                            continue;
                        }

                        for (BenchmarkConfig.CapacityType capType : capacityTypes) {
                            currentTest++;

                            BenchmarkConfig config = new BenchmarkConfig.Builder()
                                .implementation(impl)
                                .hashType(hashType)
                                .distribution(dist)
                                .loadFactor(lf)
                                .capacityType(capType)
                                .initialCapacity(16)
                                .operationCount(10000)
                                .uniqueRatio(0.8)
                                .build();

                            System.out.printf("[%d/%d] Testing: %s%n",
                                currentTest, totalTests, config.getDescription());

                            try {
                                BenchmarkResult result = runBenchmark(config);
                                suite.addResult(result);
                                System.out.printf("  ✓ Completed in %.2f ms (%.0f ops/sec)%n",
                                    result.getTotalTimeMs(), result.getThroughput());
                            } catch (Exception e) {
                                System.out.printf("  ✗ Failed: %s%n", e.getMessage());
                            }

                            System.out.println();
                        }
                    }
                }
            }
        }

        return suite;
    }
}
