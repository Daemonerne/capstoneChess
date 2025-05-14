package engine.forPlayer.forAI;

import engine.forBoard.Board;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache for board evaluations to avoid redundant calculations.
 * Uses Zobrist hashing combined with depth for efficient and accurate lookups.
 */
public class EvaluationCache {
  // Singleton instance
  private static final EvaluationCache INSTANCE = new EvaluationCache();

  // Cache using board hash and depth as key
  private final ConcurrentHashMap<CacheKey, Double> cache;

  // Size limit to prevent excessive memory usage
  private static final int MAX_SIZE = 1_000_000;

  // Stats for monitoring
  private long hits = 0;
  private long misses = 0;

  // Private constructor for singleton
  private EvaluationCache() {
    this.cache = new ConcurrentHashMap<>(MAX_SIZE / 2);
  }

  /**
   * Returns the singleton instance.
   */
  public static EvaluationCache get() {
    return INSTANCE;
  }

  /**
   * Stores an evaluation score in the cache.
   */
  public void store(Board board, int depth, double score) {
    if (cache.size() >= MAX_SIZE) {
      // Simple eviction policy: clear half the cache when full
      clearSomeEntries();
    }

    CacheKey key = new CacheKey(board.getZobristHash(), depth);
    cache.put(key, score);
  }

  /**
   * Retrieves an evaluation score from the cache.
   */
  public Double probe(Board board, int depth) {
    CacheKey key = new CacheKey(board.getZobristHash(), depth);
    Double result = cache.get(key);

    if (result != null) {
      hits++;
    } else {
      misses++;
    }

    return result;
  }

  /**
   * Gets the cache hit rate statistics.
   */
  public String getStats() {
    long total = hits + misses;
    if (total == 0) return "No cache lookups yet";

    double hitRate = (double) hits / total * 100.0;
    return String.format("Cache: %d entries, %.2f%% hit rate (%d hits, %d misses)",
            cache.size(), hitRate, hits, misses);
  }

  /**
   * Clears the cache.
   */
  public void clear() {
    cache.clear();
    hits = 0;
    misses = 0;
  }

  /**
   * Clears some entries from the cache to prevent it from growing too large.
   */
  private void clearSomeEntries() {
    // Clear half the cache when it gets full
    int toRemove = MAX_SIZE / 2;

    int removed = 0;
    for (CacheKey key : cache.keySet()) {
      cache.remove(key);
      removed++;
      if (removed >= toRemove) break;
    }
  }

  /**
   * Key for the evaluation cache, combining a board's Zobrist hash and evaluation depth.
   */
  private static class CacheKey {
    private final long zobristHash;
    private final int depth;

    public CacheKey(long zobristHash, int depth) {
      this.zobristHash = zobristHash;
      this.depth = depth;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CacheKey cacheKey = (CacheKey) o;
      return zobristHash == cacheKey.zobristHash && depth == cacheKey.depth;
    }

    @Override
    public int hashCode() {
      return Objects.hash(zobristHash, depth);
    }
  }
}