package benchmark;

import dict.HashTableMetrics;

/**
 * Results from a single benchmark run.
 */
public class BenchmarkResult {
    //
    // Class setup
    //

    private final BenchmarkConfig config;
    private final HashTableMetrics metrics;
    private final long totalTimeNanos;
    private final int finalSize;
    private final int finalCapacity;
    private final double finalLoadFactor;
    private final long memoryUsedBytes;

    // Distribution-specific metrics
    private final int[] chainLengthDistribution;  // For chaining
    private final int[] probeSequenceLengths;      // For open addressing
    private final int clusterCount;                // For open addressing

    public BenchmarkResult(BenchmarkConfig config, HashTableMetrics metrics,
                          long totalTimeNanos, int finalSize, int finalCapacity,
                          double finalLoadFactor, long memoryUsedBytes,
                          int[] chainLengthDistribution, int[] probeSequenceLengths,
                          int clusterCount) {
        this.config = config;
        this.metrics = metrics;
        this.totalTimeNanos = totalTimeNanos;
        this.finalSize = finalSize;
        this.finalCapacity = finalCapacity;
        this.finalLoadFactor = finalLoadFactor;
        this.memoryUsedBytes = memoryUsedBytes;
        this.chainLengthDistribution = chainLengthDistribution;
        this.probeSequenceLengths = probeSequenceLengths;
        this.clusterCount = clusterCount;
    }

    public BenchmarkConfig getConfig() { return config; }
    public HashTableMetrics getMetrics() { return metrics; }
    public long getTotalTimeNanos() { return totalTimeNanos; }
    public double getTotalTimeMs() { return totalTimeNanos / 1_000_000.0; }
    public double getTotalTimeSecs() { return totalTimeNanos / 1_000_000_000.0; }
    public int getFinalSize() { return finalSize; }
    public int getFinalCapacity() { return finalCapacity; }
    public double getFinalLoadFactor() { return finalLoadFactor; }
    public long getMemoryUsedBytes() { return memoryUsedBytes; }
    public double getMemoryUsedMB() { return memoryUsedBytes / (1024.0 * 1024.0); }
    public int[] getChainLengthDistribution() { return chainLengthDistribution; }
    public int[] getProbeSequenceLengths() { return probeSequenceLengths; }
    public int getClusterCount() { return clusterCount; }

    public double getThroughput() {
        return config.operationCount / getTotalTimeSecs();
    }

    //
    // Helper Method
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BenchmarkResult{\n");
        sb.append("  Config: ").append(config.getDescription()).append("\n");
        sb.append("  Total Time: ").append(String.format("%.3f ms", getTotalTimeMs())).append("\n");
        sb.append("  Throughput: ").append(String.format("%,.0f ops/sec", getThroughput())).append("\n");
        sb.append("  Final Size: ").append(finalSize).append("\n");
        sb.append("  Final Capacity: ").append(finalCapacity).append("\n");
        sb.append("  Final Load Factor: ").append(String.format("%.3f", finalLoadFactor)).append("\n");
        sb.append("  Memory Used: ").append(String.format("%.2f MB", getMemoryUsedMB())).append("\n");
        sb.append("  Metrics: ").append(metrics).append("\n");

        if (chainLengthDistribution != null) {
            sb.append("  Avg Chain Length: ").append(String.format("%.2f", getAverageChainLength())).append("\n");
            sb.append("  Max Chain Length: ").append(getMaxChainLength()).append("\n");
        }

        if (probeSequenceLengths != null && probeSequenceLengths.length > 0) {
            sb.append("  Avg Probe Length: ").append(String.format("%.2f", getAverageProbeLength())).append("\n");
            sb.append("  Max Probe Length: ").append(getMaxProbeLength()).append("\n");
            sb.append("  Cluster Count: ").append(clusterCount).append("\n");
            sb.append("  Clustering Factor: ").append(String.format("%.3f", getClusteringFactor())).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

    //
    // Result Operators
    //

    /**
     * Get average chain length (for chaining implementations).
     */
    public double getAverageChainLength() {
        if (chainLengthDistribution == null) return 0;

        long totalChains = 0;
        long totalLength = 0;
        for (int i = 0; i < chainLengthDistribution.length; i++) {
            totalChains += chainLengthDistribution[i];
            totalLength += i * chainLengthDistribution[i];
        }

        return totalChains == 0 ? 0 : (double) totalLength / totalChains;
    }

    /**
     * Get maximum chain length (for chaining implementations).
     */
    public int getMaxChainLength() {
        if (chainLengthDistribution == null) return 0;

        for (int i = chainLengthDistribution.length - 1; i >= 0; i--) {
            if (chainLengthDistribution[i] > 0) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get average probe sequence length (for open addressing).
     */
    public double getAverageProbeLength() {
        if (probeSequenceLengths == null || probeSequenceLengths.length == 0) {
            return 0;
        }

        long sum = 0;
        for (int length : probeSequenceLengths) {
            sum += length;
        }
        return (double) sum / probeSequenceLengths.length;
    }

    /**
     * Get maximum probe sequence length (for open addressing).
     */
    public int getMaxProbeLength() {
        if (probeSequenceLengths == null || probeSequenceLengths.length == 0) {
            return 0;
        }

        int max = 0;
        for (int length : probeSequenceLengths) {
            max = Math.max(max, length);
        }
        return max;
    }

    /**
     * Calculate clustering factor (for open addressing).
     * Lower is better - indicates less clustering.
     */
    public double getClusteringFactor() {
        if (finalCapacity == 0 || finalSize == 0) return 0;
        // Ideal number of clusters equals the number of entries
        // Actual clusters will be less if there's clustering
        return (double) clusterCount / finalSize;
    }


}
