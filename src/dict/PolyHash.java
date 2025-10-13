package dict;

/**
 * Polynomial rolling hash function for strings.
 * A classical hash function using polynomial accumulation with base 31.
 */
public class PolyHash implements HashFunction<String> {
    //
    // Class setup and Constructors
    //

    private static final int BASE = 31;  // use a prime number

    //
    // Operator Methods
    //

    /**
     * Computes a polynomial hash for a string.
     */
    @Override
    public int hash(String s) {
        if (s == null) return 0;

        int hash = 0;
        for (int i = 0; i < s.length(); i++) {  // using Honer's Factorization ((s[0] x 31 + s[1]) x 31 +s[2])
            hash = hash * BASE + s.charAt(i);  // current hash times base value plus ascii value
        }
        return hash;
    }
}
