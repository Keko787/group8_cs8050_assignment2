package apps;

import dict.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class WordCountMain {
    static final Pattern TOKEN = Pattern.compile("[^a-zA-Z0-9]+");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCountMain <path-to-text> <chaining|open-linear|open-quadratic> [topN]");
            System.exit(2);
        }
        Path path = Paths.get(args[0]);
        String impl = args[1];
        int topN = (args.length >= 3) ? Integer.parseInt(args[2]) : 20;

        Dictionary<String, Integer> map = buildMap(impl);
        long start = System.nanoTime();
        long tokens = 0;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = TOKEN.split(line.toLowerCase(Locale.ROOT));
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    tokens++;
                    Integer cur = map.get(p).orElse(0);
                    map.put(p, cur + 1);
                }
            }
        }
        long end = System.nanoTime();
        double secs = (end - start) / 1e9;

        List<Map.Entry<String,Integer>> entries = new ArrayList<>();
        for (String k : map.keys()) {
            entries.add(new AbstractMap.SimpleEntry<>(k, map.get(k).orElse(0)));
        }
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        if (entries.size() > topN) entries = entries.subList(0, topN);

        System.out.printf("Implementation: %s%n", impl);
        System.out.printf("Tokens processed: %,d in %.3f s (throughput: %.1f tokens/s)%n",
                tokens, secs, tokens / secs);
        System.out.println("Top " + topN + " words:");
        for (Map.Entry<String,Integer> e : entries) {
            System.out.printf("%-20s %10d%n", e.getKey(), e.getValue());
        }
    }

    static Dictionary<String,Integer> buildMap(String impl) {
        HashFunction<String> hf = new PolyHash();
        switch (impl) {
            case "chaining":
                return new HashTableChainingLinkedList<String, Integer>(1<<15, hf);
            case "open-linear":
                return new HashTableOpenAddressing<String, Integer>(1<<16, hf, ProbingStrategy.linear());
            case "open-quadratic":
                return new HashTableOpenAddressing<String, Integer>(1<<16, hf, ProbingStrategy.quadratic(1,3));
            default:
                throw new IllegalArgumentException("Unknown impl: " + impl);
        }
    }
}
