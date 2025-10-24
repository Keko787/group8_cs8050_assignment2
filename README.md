# Dictionary ADT Implementation Project

This project implements a comprehensive Dictionary Abstract Data Type (ADT) with multiple hash table implementations and hash functions for performance comparison.

## Features

### Dictionary ADT Operations
- `put(K key, V value)` - Insert or update key-value pairs
- `get(K key)` - Retrieve value by key (returns Optional<V>)
- `remove(K key)` - Delete key-value mapping
- `containsKey(K key)` - Check if key exists
- `size()` - Get number of entries
- `keys()` - Get all keys as an Iterable

### Implementations

#### 1. Hash Table with Chaining (LinkedList)
- **File**: `HashTableChainingLinkedList.java`
- **Collision Resolution**: Separate chaining with linked lists
- **Dynamic Resizing**: Doubles capacity when load factor exceeds threshold (default 0.75)
- **Performance**: O(1) average case, O(n) worst case
- **Best For**: General purpose, handles high load factors well

#### 2. Hash Table with Chaining (BST)
- **File**: `HashTableChainingBST.java`
- **Collision Resolution**: Separate chaining with AVL trees (balanced BSTs)
- **Dynamic Resizing**: Doubles capacity when load factor exceeds threshold (default 0.75)
- **Performance**: O(1) average case, O(log n) worst case (better than linked lists)
- **Best For**: Datasets with poor hash distribution or high collision rates

#### 3. Hash Table with Open Addressing (Linear Probing)
- **File**: `HashTableOpenAddressing.java` with `ProbingStrategy.linear()`
- **Collision Resolution**: Linear probing (offset = i)
- **Dynamic Resizing**: Doubles capacity when load factor exceeds threshold (default 0.5)
- **Performance**: Cache-friendly, but suffers from primary clustering
- **Best For**: Small to medium datasets with good hash functions

#### 4. Hash Table with Open Addressing (Quadratic Probing)
- **File**: `HashTableOpenAddressing.java` with `ProbingStrategy.quadratic(c1, c2)`
- **Collision Resolution**: Quadratic probing (offset = c1*i + c2*i²)
- **Dynamic Resizing**: Doubles capacity when load factor exceeds threshold (default 0.5)
- **Performance**: Reduces clustering compared to linear probing
- **Best For**: Medium to large datasets, better distribution than linear probing

### Hash Functions

#### 1. Polynomial Rolling Hash (PolyHash)
- **File**: `PolyHash.java`
- **Type**: Classical polynomial accumulation hash
- **Formula**: hash = s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
- **Performance**: Very fast, good distribution for typical strings
- **Use Case**: General purpose, production-ready

#### 2. SHA-256 Hash
- **File**: `SHA256Hash.java`
- **Type**: Cryptographically strong hash function
- **Formula**: First 4 bytes of SHA-256(key)
- **Performance**: Slower but excellent distribution
- **Use Case**: When collision resistance is critical

## Usage

### Basic Word Count Example
```bash
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCounter <path-to-text> <chaining|open-linear|open-quadratic> [topN]
```

**Examples**:
```bash
# Using chaining with linked lists
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCounter data.txt chaining 20

# Using open addressing with linear probing
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCounter data.txt open-linear 20

# Using open addressing with quadratic probing
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.WordCounter data.txt open-quadratic 20
```

### Comprehensive Benchmark
```bash
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.DictionaryBenchmark <path-to-text> [topN]
```

This runs a comprehensive comparison of all implementations and hash functions, providing:
- Performance metrics (time, throughput)
- Top N most frequent words
- Detailed comparison summary
- Implementation characteristics analysis

**Example**:
```bash
java -cp target/assignment2-dictionaries-0.1.0-SNAPSHOT.jar apps.DictionaryBenchmark data.txt 20
```

## Design Details

### Load Factor Thresholds
- **Chaining**: Default 0.75 (can handle higher loads since chaining grows dynamically)
- **Open Addressing**: Default 0.5 (lower threshold to maintain performance)
- Both are configurable via constructor parameters

### Dynamic Resizing
All implementations support automatic resizing:
1. Monitor load factor: `size / capacity`
2. When threshold exceeded, double the table size
3. Rehash all existing entries to new table
4. For open addressing, this also clears deleted entry markers

### Complexity Analysis

| Operation | Chaining (Avg) | Chaining (Worst) | BST Chaining (Worst) | Open Addressing (Avg) | Open Addressing (Worst) |
|-----------|----------------|------------------|----------------------|----------------------|-------------------------|
| Insert    | O(1)          | O(n)            | O(log n)            | O(1)                | O(n)                   |
| Search    | O(1)          | O(n)            | O(log n)            | O(1)                | O(n)                   |
| Delete    | O(1)          | O(n)            | O(log n)            | O(1)                | O(n)                   |
| Space     | O(n)          | O(n)            | O(n)                | O(n)                | O(n)                   |

## Project Structure
```
src/
├── dict/
│   ├── Dictionary.java                  # ADT interface
│   ├── HashFunction.java                # Hash function interface
│   ├── HashTableChainingLinkedList.java # Chaining with LinkedLists
│   ├── HashTableChainingBST.java        # Chaining with AVL trees
│   ├── HashTableOpenAddressing.java     # Open addressing
│   ├── ProbingStrategy.java             # Linear/Quadratic probing
│   ├── PolyHash.java                    # Polynomial hash
│   └── SHA256Hash.java                  # Cryptographic hash
└── apps/
    ├── Main.java                        # Java HashMap reference
    ├── WordCountMain.java               # Single implementation test
    └── DictionaryBenchmark.java         # Comprehensive comparison
```

## Building the Project

```bash
# Compile all sources
javac -d bin src/dict/*.java src/apps/*.java

# Create JAR
jar cvf assignment2-dictionaries.jar -C bin .

# Run
java -cp assignment2-dictionaries.jar apps.DictionaryBenchmark <input-file>
```

## Performance Considerations

### When to Use Each Implementation

1. **Chaining with LinkedList**:
   - Default choice for most applications
   - Handles varying load factors well
   - Simple and reliable

2. **Chaining with BST**:
   - When hash distribution is questionable
   - Need guaranteed O(log n) worst case
   - Moderate memory overhead acceptable

3. **Open Addressing (Linear)**:
   - Cache-friendly access patterns
   - Small datasets
   - Good hash function available

4. **Open Addressing (Quadratic)**:
   - Larger datasets than linear probing
   - Better hash distribution needed
   - Can tolerate some clustering

### Hash Function Selection

- **Use PolyHash** for most applications (fast, proven)
- **Use SHA256Hash** when security or collision resistance is critical

## Comprehensive Benchmark Suite

### Overview
The project includes an advanced benchmark suite that measures detailed performance metrics under varying conditions.

### Metrics Tracked
- **Per-operation runtimes**: Insert, get, delete times in nanoseconds
- **Average probe counts**: Number of probes per operation
- **Memory overhead**: Actual memory usage in MB
- **Collision rates**: Percentage of operations causing collisions
- **Resize operations**: Number of table resizes
- **Chain lengths**: Distribution of chain lengths (for chaining)
- **Probe sequences**: Distribution of probe lengths (for open addressing)
- **Clustering factors**: Measure of key clustering (for open addressing)

### Test Conditions

#### Load Factors
Tests across various load factor thresholds:
- 0.25, 0.50, 0.75, 0.90, 0.95

#### Key Distributions
1. **Uniform Random**: Randomly generated uniformly distributed keys
2. **Power-Law (Zipf)**: Keys following a power-law distribution (realistic workloads)
3. **Adversarial**: Keys designed to maximize hash collisions
4. **Clustered**: Keys that hash to nearby buckets

#### Hash Functions
- Polynomial Rolling Hash (classical, fast)
- SHA-256 Hash (cryptographically strong, better distribution)

#### Table Capacities
- **Power-of-Two**: Traditional approach (16, 32, 64, ...)
- **Prime Numbers**: Alternative approach for better hash distribution

### Running Benchmarks

#### Full Comprehensive Benchmark
```bash
./run_benchmark.sh --full
# or
java -cp bin apps.ComprehensiveBenchmark
```

Tests all combinations of:
- 3 implementations × 2 hash functions × 4 distributions × 5 load factors × 2 capacity types
- Generates detailed reports and CSV output

#### Quick Benchmark
```bash
./run_benchmark.sh --quick
# or
java -cp bin apps.ComprehensiveBenchmark --quick
```

Runs a subset of configurations for faster results.

#### Custom Benchmark
```bash
./run_benchmark.sh --custom \
  --impl OPEN_LINEAR \
  --hash SHA256 \
  --dist ADVERSARIAL \
  --capacity PRIME \
  --lf 0.5 \
  --ops 100000
```

Available options:
- `--impl`: CHAINING_LL | OPEN_LINEAR | OPEN_QUADRATIC
- `--hash`: POLYNOMIAL | SHA256
- `--dist`: UNIFORM | POWER_LAW | ADVERSARIAL | CLUSTERED
- `--capacity`: POWER_OF_TWO | PRIME
- `--lf`: Load factor threshold (e.g., 0.75)
- `--ops`: Number of operations (e.g., 100000)

### Output Files

The benchmark suite generates:

1. **benchmark_results.csv**: Machine-readable results with all metrics
   - Columns: Implementation, HashType, CapacityType, LoadFactor, Distribution, etc.
   - Suitable for analysis in Excel, R, Python, etc.

2. **benchmark_report.txt**: Human-readable detailed report
   - Statistical analysis (mean, median, std dev)
   - Best/worst configurations
   - Comparison across dimensions

3. **Console output**: Real-time progress and summary tables

### Example Output

```
Implementation            Hash         Distribution     LF       Capacity        Throughput    AvgProbe   CollRate%  Resizes
-----------------------------------------------------------------------------------------
CHAINING_LL              POLYNOMIAL   UNIFORM          0.75     POWER_OF_TWO    1,234,567      1.23       45.67      8
OPEN_LINEAR              SHA256       ADVERSARIAL      0.50     PRIME           987,654        2.45       78.90      5
```

### Automated Testing

The benchmark suite uses:
- **Fixed seed** for reproducible experiments
- **Synthetic data generators** for controlled distributions
- **Instrumented hash tables** that track all metrics without affecting correctness
- **Statistical analysis** with mean, median, standard deviation, min, max

### Project Structure (Updated)
```
src/
├── dict/
│   ├── Dictionary.java                          # ADT interface
│   ├── HashFunction.java                        # Hash function interface
│   ├── HashTableChainingLinkedList.java         # Chaining with LinkedLists
│   ├── HashTableChainingBST.java                # Chaining with AVL trees
│   ├── HashTableOpenAddressing.java             # Open addressing
│   ├── InstrumentedHashTableChainingLinkedList.java # Instrumented chaining for metrics
│   ├── InstrumentedHashTableOpenAddressing.java # Instrumented open addressing
│   ├── HashTableMetrics.java                    # Metrics tracking class
│   ├── ProbingStrategy.java                     # Linear/Quadratic probing
│   ├── PolyHash.java                            # Polynomial hash
│   └── SHA256Hash.java                          # Cryptographic hash
├── benchmark/
│   ├── BenchmarkConfig.java                     # Configuration builder
│   ├── BenchmarkResult.java                     # Single test result
│   ├── BenchmarkRunner.java                     # Test executor
│   ├── BenchmarkSuite.java                      # Results collection & analysis
│   └── DataGenerator.java                       # Synthetic data generation
└── apps/
    ├── Main.java                                # Java HashMap reference
    ├── WordCountMain.java                       # Single implementation test
    ├── DictionaryBenchmark.java                 # Basic comparison
    └── ComprehensiveBenchmark.java              # Full benchmark suite
```

## Academic Requirements Met

✅ Dictionary ADT with standard operations (insert, find, delete, update, keys, size)
✅ Hash Table with Chaining using LinkedLists
✅ Hash Table with Chaining using Balanced BSTs (AVL trees)
✅ Hash Table with Open Addressing - Linear Probing
✅ Hash Table with Open Addressing - Quadratic Probing
✅ Dynamic resizing with adjustable load factor thresholds
✅ Two string hash functions (Polynomial vs SHA-256)
✅ Comprehensive comparison and demonstration program

### Advanced Benchmark Suite Requirements
✅ Reproducible experiments with fixed seeds
✅ Per-operation runtime measurements (nanoseconds)
✅ Average probe count tracking
✅ Memory overhead measurement
✅ Collision rate analysis
✅ Load factor testing (0.25 - 0.95+)
✅ Multiple key distributions (uniform, power-law, adversarial, clustered)
✅ Hash function comparison (polynomial vs SHA-256)
✅ Table size comparison (prime vs power-of-two)
✅ Automated testing with synthetic data
✅ Real-world dataset support (word count examples)
✅ Statistical analysis and reporting
✅ CSV export for external analysis
