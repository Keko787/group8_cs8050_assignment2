package apps;

import benchmark.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Main application for running comprehensive dictionary benchmarks.
 *
 * This benchmark suite evaluates:
 * - Per-operation runtimes
 * - Average probe counts
 * - Memory overhead
 * - Collision rates
 *
 * Under varying conditions:
 * - Load factors (0.25 - 0.95+)
 * - Key distributions (uniform, power-law, adversarial, clustered)
 * - Hash functions (polynomial, SHA-256)
 * - Table sizes (prime vs power-of-two capacities)
 */
public class ComprehensiveBenchmark {

    public static void main(String[] args) {
        //
        // Argument Parsing, setup, and Initial Logs
        //

        System.out.println("╔═══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║        COMPREHENSIVE DICTIONARY IMPLEMENTATION BENCHMARK SUITE            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        BenchmarkRunner runner = new BenchmarkRunner();

        if (args.length > 0 && args[0].equals("--quick")) {
            System.out.println("Running QUICK benchmark (subset of configurations)...");
            System.out.println();
            runQuickBenchmark(runner);
        } else if (args.length > 0 && args[0].equals("--custom")) {
            System.out.println("Running CUSTOM benchmark...");
            System.out.println();
            runCustomBenchmark(runner, args);
        } else {
            System.out.println("Running FULL comprehensive benchmark suite...");
            System.out.println("This will test all combinations of:");
            System.out.println("  - Implementations: Chaining (LL), Open Addressing (Linear & Quadratic)");
            System.out.println("  - Hash Functions: Polynomial, SHA-256");
            System.out.println("  - Distributions: Uniform, Power-Law, Adversarial, Clustered");
            System.out.println("  - Load Factors: 0.25, 0.5, 0.75, 0.9, 0.95");
            System.out.println("  - Capacity Types: Prime, Power-of-Two");
            System.out.println();
            System.out.println("This may take several minutes...");
            System.out.println();

            runFullBenchmark(runner);
        }
    }

    //
    // Helper Function
    //

    private static String getArgOrDefault(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java apps.ComprehensiveBenchmark [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  (none)       Run full comprehensive benchmark suite");
        System.out.println("  --quick      Run quick benchmark with subset of configurations");
        System.out.println("  --custom     Run custom benchmark with specified parameters:");
        System.out.println("               --impl CHAINING_LL|OPEN_LINEAR|OPEN_QUADRATIC");
        System.out.println("               --hash POLYNOMIAL|SHA256");
        System.out.println("               --dist UNIFORM|POWER_LAW|ADVERSARIAL|CLUSTERED");
        System.out.println("               --capacity POWER_OF_TWO|PRIME");
        System.out.println("               --lf <load_factor>");
        System.out.println("               --ops <operation_count>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java apps.ComprehensiveBenchmark");
        System.out.println("  java apps.ComprehensiveBenchmark --quick");
        System.out.println("  java apps.ComprehensiveBenchmark --custom --impl OPEN_LINEAR --hash SHA256 --dist ADVERSARIAL --lf 0.5 --ops 100000");
    }

    //
    // Benchmark Methods
    //

    private static void runFullBenchmark(BenchmarkRunner runner) {
        long startTime = System.currentTimeMillis();

        BenchmarkSuite suite = runner.runComprehensiveSuite();

        long endTime = System.currentTimeMillis();
        double totalTimeSecs = (endTime - startTime) / 1000.0;

        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println("Benchmark suite completed in " + String.format("%.2f", totalTimeSecs) + " seconds");
        System.out.println("=".repeat(100));
        System.out.println();

        // Print summary table
        suite.printSummaryTable();

        // Generate detailed report
        System.out.println();
        System.out.println(suite.generateReport());

        // Export to CSV
        try {
            String csvFile = "benchmark_results.csv";
            suite.exportToCSV(csvFile);
            System.out.println("\n✓ Results exported to: " + csvFile);
        } catch (IOException e) {
            System.err.println("✗ Failed to export CSV: " + e.getMessage());
        }

        // Export detailed report to file
        try {
            String reportFile = "benchmark_report.txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
                writer.println(suite.generateReport());
            }
            System.out.println("✓ Detailed report saved to: " + reportFile);
        } catch (IOException e) {
            System.err.println("✗ Failed to save report: " + e.getMessage());
        }
    }

    private static void runQuickBenchmark(BenchmarkRunner runner) {
        BenchmarkSuite suite = new BenchmarkSuite();

        // Quick benchmark with limited configurations
        BenchmarkConfig[] configs = {
            // Chaining with different distributions
            new BenchmarkConfig.Builder()
                .implementation(BenchmarkConfig.Implementation.CHAINING_LL)
                .hashType(BenchmarkConfig.HashType.POLYNOMIAL)
                .distribution(DataGenerator.Distribution.UNIFORM)
                .loadFactor(0.75)
                .operationCount(50000)
                .build(),

            new BenchmarkConfig.Builder()
                .implementation(BenchmarkConfig.Implementation.CHAINING_LL)
                .hashType(BenchmarkConfig.HashType.SHA256)
                .distribution(DataGenerator.Distribution.POWER_LAW)
                .loadFactor(0.75)
                .operationCount(50000)
                .build(),

            // Open addressing linear
            new BenchmarkConfig.Builder()
                .implementation(BenchmarkConfig.Implementation.OPEN_LINEAR)
                .hashType(BenchmarkConfig.HashType.POLYNOMIAL)
                .distribution(DataGenerator.Distribution.UNIFORM)
                .loadFactor(0.5)
                .operationCount(50000)
                .build(),

            // Open addressing quadratic
            new BenchmarkConfig.Builder()
                .implementation(BenchmarkConfig.Implementation.OPEN_QUADRATIC)
                .hashType(BenchmarkConfig.HashType.POLYNOMIAL)
                .distribution(DataGenerator.Distribution.ADVERSARIAL)
                .loadFactor(0.5)
                .operationCount(50000)
                .build(),

            // Prime vs power-of-two comparison
            new BenchmarkConfig.Builder()
                .implementation(BenchmarkConfig.Implementation.CHAINING_LL)
                .hashType(BenchmarkConfig.HashType.POLYNOMIAL)
                .distribution(DataGenerator.Distribution.UNIFORM)
                .capacityType(BenchmarkConfig.CapacityType.PRIME)
                .loadFactor(0.75)
                .operationCount(50000)
                .build(),
        };

        for (int i = 0; i < configs.length; i++) {
            System.out.printf("[%d/%d] Running: %s%n", i + 1, configs.length,
                configs[i].getDescription());

            BenchmarkResult result = runner.runBenchmark(configs[i]);
            suite.addResult(result);

            System.out.printf("  ✓ Completed: %.2f ms, %.0f ops/sec%n",
                result.getTotalTimeMs(), result.getThroughput());
            System.out.println();
        }

        suite.printSummaryTable();
        System.out.println();
        System.out.println(suite.generateReport());
    }

    private static void runCustomBenchmark(BenchmarkRunner runner, String[] args) {
        // Parse custom parameters
        String impl = getArgOrDefault(args, "--impl", "CHAINING_LL");
        String hash = getArgOrDefault(args, "--hash", "POLYNOMIAL");
        String dist = getArgOrDefault(args, "--dist", "UNIFORM");
        String capacity = getArgOrDefault(args, "--capacity", "POWER_OF_TWO");
        double lf = Double.parseDouble(getArgOrDefault(args, "--lf", "0.75"));
        int ops = Integer.parseInt(getArgOrDefault(args, "--ops", "100000"));

        BenchmarkConfig config = new BenchmarkConfig.Builder()
            .implementation(BenchmarkConfig.Implementation.valueOf(impl))
            .hashType(BenchmarkConfig.HashType.valueOf(hash))
            .distribution(DataGenerator.Distribution.valueOf(dist))
            .capacityType(BenchmarkConfig.CapacityType.valueOf(capacity))
            .loadFactor(lf)
            .operationCount(ops)
            .build();

        System.out.println("Configuration: " + config.getDescription());
        System.out.println();

        BenchmarkResult result = runner.runBenchmark(config);

        System.out.println();
        System.out.println(result);
    }


}
