package apps;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class Main {
    static final Pattern TOKEN = Pattern.compile("[^a-zA-Z0-9]+");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java apps.Main <path-to-text> [topN]");
            System.exit(2);
        }
        Path path = Paths.get(args[0]);
        int topN = (args.length >= 2) ? Integer.parseInt(args[1]) : 20;

        Map<String, Integer> map = new HashMap<>();
        long start = System.nanoTime();
        long tokens = 0;

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = TOKEN.split(line.toLowerCase(Locale.ROOT));
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    tokens++;
                    Integer cur = map.getOrDefault(p, 0);
                    map.put(p, cur + 1);
                }
            }
        }
        long end = System.nanoTime();
        double secs = (end - start) / 1e9;

        List<Map.Entry<String,Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        if (entries.size() > topN) entries = entries.subList(0, topN);
        System.out.printf("Tokens processed: %,d in %.3f s (throughput: %.1f tokens/s)%n",
                tokens, secs, tokens / secs);
        System.out.println("Top " + topN + " words:");
        for (Map.Entry<String,Integer> e : entries) {
            System.out.printf("%-20s %10d%n", e.getKey(), e.getValue());
        }
    }
}
