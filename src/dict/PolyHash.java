package dict;

/**
 * Polynomial rolling hash function for strings.
 */
public class PolyHash {
    private static final int BASE = 31;

    /**
     * Computes a polynomial hash for a string.
     */
    public int hash(String s) {
        if (s == null) return 0;

        int hash = 0;
        for (int i = 0; i < s.length(); i++) {
            hash = hash * BASE + s.charAt(i);
        }
        return hash;
    }
}
