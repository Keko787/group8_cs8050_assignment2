package dict;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographically strong hash function using SHA-256.
 * Provides better distribution and collision resistance compared to simple polynomial hashing.
 */
public class SHA256Hash implements HashFunction<String> {
    //
    // Class setup and Constructors
    //

    private final MessageDigest digest;

    public SHA256Hash() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");  // calls on object that will be using the SHA-256 algorithm
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    //
    // Operator Methods
    //

    @Override
    public int hash(String key) {
        if (key == null) return 0;

        // Compute SHA-256 hash
        byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        // use the construct to use the SHA-256 algorithm while
        // converting the string into bytes using UTF8 encoding

        // Convert first 4 bytes to an integer
        int hash = 0;
        for (int i = 0; i < 4 && i < hashBytes.length; i++) {  // due to data type int being 4 bytes while SHA256 being 32 bytes
            hash = (hash << 8) | (hashBytes[i] & 0xFF);
        }

        return hash;
    }
}
