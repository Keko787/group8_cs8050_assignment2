package apps;

import dict.Dictionary;
import dict.HashFunction;
import dict.HashTableChainingLinkedList;
import dict.HashTableOpenAddressing;
import dict.HashTableCuckoo;
import dict.PolyHash;
import dict.ProbingStrategy;
import dict.SHA256Hash;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive benchmark comparing all dictionary implementations and hash functions.
 * Tests performance, collision distribution, and load factor behavior.
 */
public class DictionaryBenchmark {
    //
    // Main Process
    //

    static final Pattern TOKEN = Pattern.compile("[^a-zA-Z0-9]+");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java apps.DictionaryBenchmark <path-to-text> [topN]");
            System.exit(2);
        }

        Path path = Paths.get(args[0]);
        int topN = (args.length >= 2) ? Integer.parseInt(args[1]) : 20;

        System.out.println("=".repeat(80));
        System.out.println("DICTIONARY IMPLEMENTATION BENCHMARK");
        System.out.println("=".repeat(80));
        System.out.println("Input file: " + path.getFileName());
        System.out.println();

        // Define Test Configs

        // Test configurations
        String[][] configs = {
            {"Hash Table with Chaining (LinkedList)", "chaining-ll", "poly"},
            {"Hash Table with Chaining (LinkedList)", "chaining-ll", "sha256"},
            {"Hash Table with Open Addressing (Linear)", "open-linear", "poly"},
            {"Hash Table with Open Addressing (Linear)", "open-linear", "sha256"},
            {"Hash Table with Open Addressing (Quadratic)", "open-quadratic", "poly"},
            {"Hash Table with Open Addressing (Quadratic)", "open-quadratic", "sha256"},
            {"Hash Table with Cuckoo Hashing", "cuckoo", "dual"}
        };

        BenchmarkResult[] results = new BenchmarkResult[configs.length];

        // Main Benchmark Loop
        for (int i = 0; i < configs.length; i++) {
            // extract the config data
            String name = configs[i][0];
            String impl = configs[i][1];
            String hashType = configs[i][2];

            // print the implementation config
            System.out.println("-".repeat(80));
            System.out.printf("Test %d/%d: %s + %s hash%n", i + 1, configs.length, name, hashType.toUpperCase());
            System.out.println("-".repeat(80));

            // run the benchmark and extract results
            results[i] = runBenchmark(path, impl, hashType, topN);
            System.out.println();
        }

        // Print comparison summary
        printComparisonSummary(results, configs);
    }

    //
    // Result Class, class setup
    //

    static class BenchmarkResult {
        String implName;
        String hashType;
        long tokenCount;
        long uniqueWords;
        double timeSecs;
        double throughput;
        List<Map.Entry<String, Integer>> topWords;

        BenchmarkResult(String implName, String hashType, long tokenCount, long uniqueWords,
                       double timeSecs, double throughput, List<Map.Entry<String, Integer>> topWords) {
            this.implName = implName;
            this.hashType = hashType;
            this.tokenCount = tokenCount;
            this.uniqueWords = uniqueWords;
            this.timeSecs = timeSecs;
            this.throughput = throughput;
            this.topWords = topWords;
        }
    }

    //
    // Helper Methods
    //

    static Dictionary<String, Integer> buildMap(String impl, String hashType) {

        // select hashing type
        HashFunction<String> hf = hashType.equals("sha256") ? new SHA256Hash() : new PolyHash();

        // selecting hashing implementation, based on hashing type, probing methods, load factor, capacity
        switch (impl) {
            case "chaining-ll":
                return new HashTableChainingLinkedList<>(16, hf, 0.75);
            case "open-linear":
                return new HashTableOpenAddressing<>(16, hf, ProbingStrategy.linear(), 0.5);
            case "open-quadratic":
                return new HashTableOpenAddressing<>(16, hf, ProbingStrategy.quadratic(1, 3), 0.5);
            case "cuckoo":
                // Cuckoo hashing uses two different hash functions
                HashFunction<String> hf1 = new PolyHash();
                HashFunction<String> hf2 = new SHA256Hash();
                return new HashTableCuckoo<>(16, hf1, hf2);
            default:
                throw new IllegalArgumentException("Unknown impl: " + impl);
        }
    }

    // Method to print result summary
    static void printComparisonSummary(BenchmarkResult[] results, String[][] configs) {
        System.out.println("=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println();

        System.out.printf("%-45s %-10s %12s %15s%n",
                "Implementation", "Hash", "Time (s)", "Throughput");
        System.out.println("-".repeat(80));

        for (int i = 0; i < results.length; i++) {
            BenchmarkResult r = results[i];
            System.out.printf("%-45s %-10s %12.3f %,15.0f%n",
                    configs[i][0], configs[i][2].toUpperCase(), r.timeSecs, r.throughput);
        }

        System.out.println();
        System.out.println("ANALYSIS:");
        System.out.println("-".repeat(80));

        // Find fastest
        BenchmarkResult fastest = results[0];
        String fastestConfig = configs[0][0] + " + " + configs[0][2];
        for (int i = 1; i < results.length; i++) {
            if (results[i].throughput > fastest.throughput) {
                fastest = results[i];
                fastestConfig = configs[i][0] + " + " + configs[i][2];
            }
        }

        System.out.printf("Fastest: %s (%.0f tokens/sec)%n", fastestConfig, fastest.throughput);

        // Compare hash functions
        System.out.println("\nHash Function Comparison:");
        System.out.println("  Polynomial Hash: Classical, fast, good for typical datasets");
        System.out.println("  SHA-256 Hash: Cryptographically strong, better distribution, slower");

        System.out.println("\nImplementation Characteristics:");
        System.out.println("  Chaining (LinkedList): Simple, good average case, no table full issues");
        System.out.println("  Open Addressing (Linear): Cache-friendly, clustering issues");
        System.out.println("  Open Addressing (Quadratic): Reduces clustering, needs good hash");
        System.out.println("  Cuckoo Hashing: O(1) worst-case lookup, uses two tables and hash functions");
    }

    //
    // Benchmark Method
    //

    static BenchmarkResult runBenchmark(Path path, String impl, String hashType, int topN) throws Exception {

        // Create hash table
        Dictionary<String, Integer> map = buildMap(impl, hashType);

        // start time
        long start = System.nanoTime();
        long tokens = 0;

        // Read file and populate hash table
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;  // variable to hold the line
            while ((line = br.readLine()) != null) {  // read the line
                String[] parts = TOKEN.split(line.toLowerCase(Locale.ROOT));  // split the line into tokenized parts to extract words
                for (String p : parts) {  // for every tokenized part
                    if (p.isEmpty()) continue;  // skip empty tokens
                    tokens++;  // increase token total
                    Integer curWordCount = map.get(p).orElse(0);  // get word count of word by searching its value
                    map.put(p, curWordCount + 1);  // insert it into the hash after searching
                }
            }
        }

        // end time
        long end = System.nanoTime();
        double secs = (end - start) / 1e9;

        // Collect top words
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        for (String k : map.keys()) {
            entries.add(new AbstractMap.SimpleEntry<>(k, map.get(k).orElse(0)));
        }
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<Map.Entry<String, Integer>> topWords = entries.size() > topN
            ? entries.subList(0, topN)
            : entries;

        // Print results
        System.out.printf("Tokens processed: %,d%n", tokens);
        System.out.printf("Unique words: %,d%n", map.size());
        System.out.printf("Time: %.3f seconds%n", secs);
        System.out.printf("Throughput: %,.1f tokens/sec%n", tokens / secs);
        System.out.println("\nTop " + topN + " words:");
        for (int i = 0; i < Math.min(10, topWords.size()); i++) {
            Map.Entry<String, Integer> e = topWords.get(i);
            System.out.printf("  %-20s %,10d%n", e.getKey(), e.getValue());
        }

        return new BenchmarkResult(impl, hashType, tokens, map.size(), secs, tokens / secs, topWords);
    }


}
