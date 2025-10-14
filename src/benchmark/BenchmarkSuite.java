package benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collection of benchmark results with analysis and reporting capabilities.
 */
public class BenchmarkSuite {
    //
    // Class Setup
    //

    private final List<BenchmarkResult> results;

    public BenchmarkSuite() {
        this.results = new ArrayList<>();
    }

    public void addResult(BenchmarkResult result) {
        results.add(result);
    }

    public List<BenchmarkResult> getResults() {
        return new ArrayList<>(results);
    }

    //
    // Benchmark Operators
    //

    /**
     * Get results filtered by implementation type.
     */
    public List<BenchmarkResult> getResultsByImplementation(BenchmarkConfig.Implementation impl) {
        return results.stream()
            .filter(r -> r.getConfig().implementation == impl)
            .collect(Collectors.toList());
    }

    /**
     * Get results filtered by hash type.
     */
    public List<BenchmarkResult> getResultsByHashType(BenchmarkConfig.HashType hashType) {
        return results.stream()
            .filter(r -> r.getConfig().hashType == hashType)
            .collect(Collectors.toList());
    }

    /**
     * Get results filtered by distribution.
     */
    public List<BenchmarkResult> getResultsByDistribution(DataGenerator.Distribution dist) {
        return results.stream()
            .filter(r -> r.getConfig().distribution == dist)
            .collect(Collectors.toList());
    }

    /**
     * Find the best performing configuration overall.
     */
    public BenchmarkResult getBestOverall() {
        return results.stream()
            .max(Comparator.comparingDouble(BenchmarkResult::getThroughput))
            .orElse(null);
    }

    /**
     * Find the worst performing configuration overall.
     */
    public BenchmarkResult getWorstOverall() {
        return results.stream()
            .min(Comparator.comparingDouble(BenchmarkResult::getThroughput))
            .orElse(null);
    }


    //
    // Statstics Class Setup and Constructors
    //
    /**
     * Calculate statistics for a metric across all results.
     */
    public static class Statistics {
        public final double mean;
        public final double median;
        public final double stdDev;
        public final double min;
        public final double max;

        public Statistics(List<Double> values) {
            if (values.isEmpty()) {
                mean = median = stdDev = min = max = 0;
                return;
            }

            // Calculate mean
            mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            // Calculate median
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int size = sorted.size();
            median = size % 2 == 0
                ? (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0
                : sorted.get(size / 2);

            // Calculate standard deviation
            double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
            stdDev = Math.sqrt(variance);

            // Min and max
            min = sorted.get(0);
            max = sorted.get(size - 1);
        }

        //
        // Helper Methods
        //

        @Override
        public String toString() {
            return String.format("mean=%.2f, median=%.2f, stdDev=%.2f, min=%.2f, max=%.2f",
                mean, median, stdDev, min, max);
        }
    }

    //
    // Stastitics Operators
    //

    /**
     * Generate a comprehensive text report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("=" .repeat(100)).append("\n");
        sb.append("COMPREHENSIVE BENCHMARK REPORT\n");
        sb.append("=".repeat(100)).append("\n\n");

        sb.append("Total Configurations Tested: ").append(results.size()).append("\n\n");

        // Overall best and worst
        BenchmarkResult best = getBestOverall();
        BenchmarkResult worst = getWorstOverall();

        if (best != null) {
            sb.append("OVERALL BEST PERFORMANCE:\n");
            sb.append("-".repeat(100)).append("\n");
            sb.append(best.toString()).append("\n\n");
        }

        if (worst != null) {
            sb.append("OVERALL WORST PERFORMANCE:\n");
            sb.append("-".repeat(100)).append("\n");
            sb.append(worst.toString()).append("\n\n");
        }

        // Analysis by implementation
        sb.append("ANALYSIS BY IMPLEMENTATION:\n");
        sb.append("=".repeat(100)).append("\n");
        for (BenchmarkConfig.Implementation impl : BenchmarkConfig.Implementation.values()) {
            List<BenchmarkResult> implResults = getResultsByImplementation(impl);
            if (implResults.isEmpty()) continue;

            sb.append("\n").append(impl).append(":\n");
            sb.append("-".repeat(100)).append("\n");

            List<Double> throughputs = implResults.stream()
                .map(BenchmarkResult::getThroughput)
                .collect(Collectors.toList());
            Statistics stats = new Statistics(throughputs);

            sb.append(String.format("  Throughput: %s ops/sec\n", stats));

            List<Double> collisionRates = implResults.stream()
                .map(r -> r.getMetrics().getCollisionRate() * 100)
                .collect(Collectors.toList());
            Statistics collStats = new Statistics(collisionRates);

            sb.append(String.format("  Collision Rate: %s%%\n", collStats));

            List<Double> avgProbes = implResults.stream()
                .map(r -> r.getMetrics().getAverageProbesPerOperation())
                .collect(Collectors.toList());
            Statistics probeStats = new Statistics(avgProbes);

            sb.append(String.format("  Avg Probes: %s\n", probeStats));
        }

        // Analysis by hash function
        sb.append("\n\nANALYSIS BY HASH FUNCTION:\n");
        sb.append("=".repeat(100)).append("\n");
        for (BenchmarkConfig.HashType hashType : BenchmarkConfig.HashType.values()) {
            List<BenchmarkResult> hashResults = getResultsByHashType(hashType);
            if (hashResults.isEmpty()) continue;

            sb.append("\n").append(hashType).append(":\n");
            sb.append("-".repeat(100)).append("\n");

            List<Double> throughputs = hashResults.stream()
                .map(BenchmarkResult::getThroughput)
                .collect(Collectors.toList());
            Statistics stats = new Statistics(throughputs);

            sb.append(String.format("  Throughput: %s ops/sec\n", stats));

            List<Double> collisionRates = hashResults.stream()
                .map(r -> r.getMetrics().getCollisionRate() * 100)
                .collect(Collectors.toList());
            Statistics collStats = new Statistics(collisionRates);

            sb.append(String.format("  Collision Rate: %s%%\n", collStats));
        }

        // Analysis by distribution
        sb.append("\n\nANALYSIS BY KEY DISTRIBUTION:\n");
        sb.append("=".repeat(100)).append("\n");
        for (DataGenerator.Distribution dist : DataGenerator.Distribution.values()) {
            List<BenchmarkResult> distResults = getResultsByDistribution(dist);
            if (distResults.isEmpty()) continue;

            sb.append("\n").append(dist).append(":\n");
            sb.append("-".repeat(100)).append("\n");

            List<Double> throughputs = distResults.stream()
                .map(BenchmarkResult::getThroughput)
                .collect(Collectors.toList());
            Statistics stats = new Statistics(throughputs);

            sb.append(String.format("  Throughput: %s ops/sec\n", stats));
        }

        // Load factor analysis
        sb.append("\n\nLOAD FACTOR IMPACT:\n");
        sb.append("=".repeat(100)).append("\n");
        Map<Double, List<BenchmarkResult>> byLoadFactor = results.stream()
            .collect(Collectors.groupingBy(r -> r.getConfig().loadFactor));

        byLoadFactor.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                double lf = entry.getKey();
                List<BenchmarkResult> lfResults = entry.getValue();

                List<Double> throughputs = lfResults.stream()
                    .map(BenchmarkResult::getThroughput)
                    .collect(Collectors.toList());
                Statistics stats = new Statistics(throughputs);

                sb.append(String.format("\nLoad Factor %.2f: %s ops/sec\n", lf, stats));
            });

        // Capacity type comparison
        sb.append("\n\nCAPACITY TYPE COMPARISON (Prime vs Power-of-Two):\n");
        sb.append("=".repeat(100)).append("\n");
        for (BenchmarkConfig.CapacityType capType : BenchmarkConfig.CapacityType.values()) {
            List<BenchmarkResult> capResults = results.stream()
                .filter(r -> r.getConfig().capacityType == capType)
                .collect(Collectors.toList());

            if (capResults.isEmpty()) continue;

            List<Double> throughputs = capResults.stream()
                .map(BenchmarkResult::getThroughput)
                .collect(Collectors.toList());
            Statistics stats = new Statistics(throughputs);

            sb.append(String.format("\n%s: %s ops/sec\n", capType, stats));
        }

        sb.append("\n").append("=".repeat(100)).append("\n");

        return sb.toString();
    }

    /**
     * Export results to CSV format.
     */
    public void exportToCSV(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("Implementation,HashType,CapacityType,LoadFactor,Distribution," +
                "OperationCount,Throughput,AvgProbes,CollisionRate,ResizeCount,MaxProbe," +
                "FinalSize,FinalCapacity,FinalLoadFactor,TotalTimeMs,MemoryMB");

            // Data rows
            for (BenchmarkResult result : results) {
                BenchmarkConfig config = result.getConfig();
                writer.printf("%s,%s,%s,%.2f,%s,%d,%.2f,%.2f,%.4f,%d,%d,%d,%d,%.4f,%.3f,%.2f%n",
                    config.implementation,
                    config.hashType,
                    config.capacityType,
                    config.loadFactor,
                    config.distribution,
                    config.operationCount,
                    result.getThroughput(),
                    result.getMetrics().getAverageProbesPerOperation(),
                    result.getMetrics().getCollisionRate(),
                    result.getMetrics().getResizeCount(),
                    result.getMetrics().getMaxProbeSequence(),
                    result.getFinalSize(),
                    result.getFinalCapacity(),
                    result.getFinalLoadFactor(),
                    result.getTotalTimeMs(),
                    result.getMemoryUsedMB()
                );
            }
        }
    }

    /**
     * Print a summary table to console.
     */
    public void printSummaryTable() {
        System.out.println("\n" + "=".repeat(150));
        System.out.println("BENCHMARK SUMMARY TABLE");
        System.out.println("=".repeat(150));

        System.out.printf("%-25s %-12s %-15s %-8s %-12s %12s %10s %10s %8s%n",
            "Implementation", "Hash", "Distribution", "LF", "Capacity", "Throughput", "AvgProbe", "CollRate%", "Resizes");
        System.out.println("-".repeat(150));

        for (BenchmarkResult result : results) {
            BenchmarkConfig config = result.getConfig();
            System.out.printf("%-25s %-12s %-15s %-8.2f %-12s %,12.0f %10.2f %10.2f %8d%n",
                config.implementation,
                config.hashType,
                config.distribution,
                config.loadFactor,
                config.capacityType,
                result.getThroughput(),
                result.getMetrics().getAverageProbesPerOperation(),
                result.getMetrics().getCollisionRate() * 100,
                result.getMetrics().getResizeCount()
            );
        }

        System.out.println("=".repeat(150));
    }
}
