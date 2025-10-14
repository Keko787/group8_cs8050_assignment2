package benchmark;

import dict.HashFunction;
import dict.ProbingStrategy;

/**
 * Configuration for benchmark experiments.
 */
public class BenchmarkConfig {
    //
    // Class setup
    //

    public enum Implementation {
        CHAINING_LL,
        CHAINING_BST,
        OPEN_LINEAR,
        OPEN_QUADRATIC
    }

    public enum HashType {
        POLYNOMIAL,
        SHA256
    }

    public enum CapacityType {
        POWER_OF_TWO,
        PRIME
    }

    // Test parameters
    public final Implementation implementation;
    public final HashType hashType;
    public final CapacityType capacityType;
    public final int initialCapacity;
    public final double loadFactor;
    public final DataGenerator.Distribution distribution;
    public final int operationCount;
    public final double uniqueRatio;

    private BenchmarkConfig(Builder builder) {
        this.implementation = builder.implementation;
        this.hashType = builder.hashType;
        this.capacityType = builder.capacityType;
        this.initialCapacity = builder.initialCapacity;
        this.loadFactor = builder.loadFactor;
        this.distribution = builder.distribution;
        this.operationCount = builder.operationCount;
        this.uniqueRatio = builder.uniqueRatio;
    }

    //
    // Helper Methods
    //

    public String getDescription() {
        return String.format("%s + %s hash, capacity=%s(%d), LF=%.2f, dist=%s, ops=%d, unique=%.0f%%",
            implementation, hashType, capacityType, initialCapacity, loadFactor,
            distribution, operationCount, uniqueRatio * 100);
    }

    //
    // Builder Class Setup
    //

    public static class Builder {
        private Implementation implementation = Implementation.CHAINING_LL;
        private HashType hashType = HashType.POLYNOMIAL;
        private CapacityType capacityType = CapacityType.POWER_OF_TWO;
        private int initialCapacity = 16;
        private double loadFactor = 0.75;
        private DataGenerator.Distribution distribution = DataGenerator.Distribution.UNIFORM;
        private int operationCount = 10000;
        private double uniqueRatio = 1.0;

    //
    // Builder Class Operator Methods
    //

        public Builder implementation(Implementation impl) {
            this.implementation = impl;
            return this;
        }

        public Builder hashType(HashType type) {
            this.hashType = type;
            return this;
        }

        public Builder capacityType(CapacityType type) {
            this.capacityType = type;
            return this;
        }

        public Builder initialCapacity(int capacity) {
            this.initialCapacity = capacity;
            return this;
        }

        public Builder loadFactor(double lf) {
            this.loadFactor = lf;
            return this;
        }

        public Builder distribution(DataGenerator.Distribution dist) {
            this.distribution = dist;
            return this;
        }

        public Builder operationCount(int count) {
            this.operationCount = count;
            return this;
        }

        public Builder uniqueRatio(double ratio) {
            this.uniqueRatio = ratio;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }
    }
}
