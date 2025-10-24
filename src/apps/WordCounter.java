package apps;

import dict.Dictionary;
import dict.HashFunction;
import dict.HashTableChainingLinkedList;
import dict.HashTableOpenAddressing;
import dict.PolyHash;
import dict.ProbingStrategy;

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

public class WordCounter{

    //
    // Argument Parsing
    //

    static final Pattern TOKEN = Pattern.compile("[^a-zA-Z0-9]+");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCountMain <path-to-text> <chaining|open-linear|open-quadratic> [topN]");
            System.exit(2);
        }
        Path path = Paths.get(args[0]);
        String impl = args[1];
        int topN = (args.length >= 3) ? Integer.parseInt(args[2]) : 20;

        //
        // Word Counting
        //

        // Create hash table implementation and start time
        Dictionary<String, Integer> map = buildMap(impl);
        long start = System.nanoTime();
        long tokens = 0;

        // Open file and read it line by line to populate hash table
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {  // uses object to read file
            String line;  // variable to hold the line

            while ((line = br.readLine()) != null) {  // read line by line until end
                // convert line to lower case and split line in to separate words using token pattern
                String[] parts = TOKEN.split(line.toLowerCase(Locale.ROOT));
                // for each word/token
                for (String p : parts) {
                    // skips empty strings
                    if (p.isEmpty()) continue;
                    // increment token total
                    tokens++;
                    // get current word count from hash based on value of word/token
                    Integer curWordCount = map.get(p).orElse(0);
                    // Increment word count while storing it back in the map/hash with the incremented count
                    map.put(p, curWordCount + 1);
                }
            }
        }

        // end time
        long end = System.nanoTime();
        double secs = (end - start) / 1e9;

        //
        // Sorting and Display
        //

        // extract all entries from the map
        List<Map.Entry<String,Integer>> entries = new ArrayList<>();
        for (String k : map.keys()) {
            // uses map.entry object to add them to the entries list
            entries.add(new AbstractMap.SimpleEntry<>(k, map.get(k).orElse(0)));
        }

        // Sort entries in descending order by count ('b' before 'a' for ascending)
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        if (entries.size() > topN) entries = entries.subList(0, topN);

        // Print Statistic and Performance stats
        System.out.printf("Implementation: %s%n", impl);
        System.out.printf("Tokens processed: %,d in %.3f s (throughput: %.1f tokens/s)%n",
                tokens, secs, tokens / secs);

        // print the word and its count
        System.out.println("Top " + topN + " words:");
        // for every entry, get the top 20 words in a 10 character field
        for (Map.Entry<String,Integer> e : entries) {
            System.out.printf("%-20s %10d%n", e.getKey(), e.getValue());
        }
    }

    //
    // Helper Method
    //

    // Implementation Selection
    static Dictionary<String,Integer> buildMap(String impl) {
        HashFunction<String> hf = new PolyHash();
        switch (impl) {
            // creates hash table with buckets
            case "chaining":
                return new HashTableChainingLinkedList<String, Integer>(1<<15, hf);
            // creates hash table with slots and linear probing
            case "open-linear":
                return new HashTableOpenAddressing<String, Integer>(1<<16, hf, ProbingStrategy.linear());
            // creates hash table with slots and quadratic probing with coeff. of 1 and 3
            case "open-quadratic":
                return new HashTableOpenAddressing<String, Integer>(1<<16, hf, ProbingStrategy.quadratic(1,3));
            default:
                throw new IllegalArgumentException("Unknown impl: " + impl);
        }
    }
}
