package dict;

/**
 * Tracks performance metrics for hash table operations.
 */
public class HashTableMetrics {
    //
    // Class setup
    //

    private long insertCount = 0;
    private long getCount = 0;
    private long deleteCount = 0;
    private long totalProbes = 0;
    private long collisionCount = 0;
    private long resizeCount = 0;
    private long maxProbeSequence = 0;

    // Timing metrics (nanoseconds)
    private long totalInsertTime = 0;
    private long totalGetTime = 0;
    private long totalDeleteTime = 0;

    //
    // Operator Methods
    //

    public void recordInsert(long probes, long timeNanos) {
        insertCount++;
        totalProbes += probes;
        totalInsertTime += timeNanos;
        if (probes > 1) collisionCount++;
        maxProbeSequence = Math.max(maxProbeSequence, probes);
    }

    public void recordGet(long probes, long timeNanos) {
        getCount++;
        totalProbes += probes;
        totalGetTime += timeNanos;
        maxProbeSequence = Math.max(maxProbeSequence, probes);
    }

    public void recordDelete(long probes, long timeNanos) {
        deleteCount++;
        totalProbes += probes;
        totalDeleteTime += timeNanos;
        maxProbeSequence = Math.max(maxProbeSequence, probes);
    }

    public void recordResize() {
        resizeCount++;
    }

    public void recordCollision() {
        collisionCount++;
    }

    //
    // Getter Methods
    //
    public long getInsertCount() { return insertCount; }
    public long getGetCount() { return getCount; }
    public long getDeleteCount() { return deleteCount; }
    public long getTotalProbes() { return totalProbes; }
    public long getCollisionCount() { return collisionCount; }
    public long getResizeCount() { return resizeCount; }
    public long getMaxProbeSequence() { return maxProbeSequence; }

    public double getAverageProbesPerOperation() {
        long totalOps = insertCount + getCount + deleteCount;
        return totalOps == 0 ? 0 : (double) totalProbes / totalOps;
    }

    public double getCollisionRate() {
        return insertCount == 0 ? 0 : (double) collisionCount / insertCount;
    }

    public double getAverageInsertTimeNanos() {
        return insertCount == 0 ? 0 : (double) totalInsertTime / insertCount;
    }

    public double getAverageGetTimeNanos() {
        return getCount == 0 ? 0 : (double) totalGetTime / getCount;
    }

    public double getAverageDeleteTimeNanos() {
        return deleteCount == 0 ? 0 : (double) totalDeleteTime / deleteCount;
    }

    //
    // Helper Functions
    //

    public void reset() {
        insertCount = 0;
        getCount = 0;
        deleteCount = 0;
        totalProbes = 0;
        collisionCount = 0;
        resizeCount = 0;
        maxProbeSequence = 0;
        totalInsertTime = 0;
        totalGetTime = 0;
        totalDeleteTime = 0;
    }

    @Override
    public String toString() {
        return String.format(
            "Metrics{inserts=%d, gets=%d, deletes=%d, avgProbes=%.2f, collisionRate=%.2f%%, " +
            "maxProbe=%d, resizes=%d, avgInsertTime=%.0fns, avgGetTime=%.0fns}",
            insertCount, getCount, deleteCount, getAverageProbesPerOperation(),
            getCollisionRate() * 100, maxProbeSequence, resizeCount,
            getAverageInsertTimeNanos(), getAverageGetTimeNanos()
        );
    }
}
