package dict;

/**
 * Defines probing strategies for open addressing hash tables.
 */
@FunctionalInterface
public interface ProbingStrategy {
    /**
     * Returns the probe offset for the i-th probe attempt.
     */
    int probe(int i);

    /**
     * Linear probing: offset = i
     */
    static ProbingStrategy linear() {
        return i -> i;
    }

    /**
     * Quadratic probing: offset = c1*i + c2*i^2
     */
    static ProbingStrategy quadratic(int c1, int c2) {
        return i -> c1 * i + c2 * i * i;
    }
}
