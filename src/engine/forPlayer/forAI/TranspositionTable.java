/**
 * An optimized fixed-size transposition table that uses power-of-2 sizing
 * for fast modulo operations and implements a replacement strategy.
 */
public class TranspositionTable {
  private static final int DEFAULT_SIZE_MB = 256;
  private static class Entry {
    long key;
    double score;
    short depth;
    byte nodeType;
    byte age;
        
    Entry() {
      this.key = 0L;
      this.score = 0.0;
      this.depth = 0;
      this.nodeType = 0;
      this.age = 0;
    }
  }
    
  private final Entry[] table;
  private final int mask;
  private byte currentAge;
  public static final byte EXACT = 0;
  public static final byte LOWERBOUND = 1;
  public static final byte UPPERBOUND = 2;
    
  /**
   * Creates a transposition table with the specified size in megabytes.
   * The actual size will be the largest power of 2 that fits in the specified MB.
   */
  public TranspositionTable(int sizeMB) {
    long bytes = (long) sizeMB * 1024 * 1024;
    int entryCount = (int) (bytes / 24);
    int size = Integer.highestOneBit(entryCount);
    table = new Entry[size];
    mask = size - 1;
    currentAge = 0;
    for (int i = 0; i < size; i++) {
      table[i] = new Entry();
    } System.out.println("Transposition Table created with " + size + 
                         " entries (" + (size * 24 / (1024 * 1024)) + " MB)");
    }
    
  /*** Creates a transposition table with the default size. */
  public TranspositionTable() {
    this(DEFAULT_SIZE_MB);
  }
    
  /**
   * Increment the age counter at the start of a new search.
   * This helps with the replacement strategy.
   */
  public void incrementAge() {
    currentAge++;
    if (currentAge == 0) {
      currentAge = 1;
    }
  }
    
  /*** Clears the transposition table. */
  public void clear() {
    for (int i = 0; i < table.length; i++) {
      Entry entry = table[i];
      entry.key = 0L;
      entry.score = 0.0;
      entry.depth = 0;
      entry.nodeType = 0;
      entry.age = 0;
    } currentAge = 0;
  }
    
  /**
   * Probe the transposition table to find an entry.
   * 
   * @param zobristHash The Zobrist hash of the position
   * @return The entry if found and verified, null otherwise
   */
  public Entry probe(long zobristHash) {
    int index = (int) (zobristHash & mask);
    Entry entry = table[index];
    if (entry.key == zobristHash && entry.key != 0) {
      return entry;
    } return null;
  }
    
  /**
   * Store a position in the transposition table.
   * Uses a replacement strategy that prefers:
   * 1. Empty slots
   * 2. Entries with lower depth
   * 3. Older entries
   * 
   * @param zobristHash The Zobrist hash of the position
   * @param score The evaluation score
   * @param depth The search depth
   * @param nodeType The node type (EXACT, LOWERBOUND, UPPERBOUND)
   */
  public void store(long zobristHash, double score, int depth, byte nodeType) {
    int index = (int) (zobristHash & mask);
    Entry entry = table[index];
    if (entry.key != 0) {
      if (entry.depth > depth && entry.age == currentAge) {
        return;
      }
    } entry.key = zobristHash;
      entry.score = score;
      entry.depth = (short) depth;
      entry.nodeType = nodeType;
      entry.age = currentAge;
  }
    
  /*** Returns the approximate fill percentage of the table. */
  public double getUsage() {
    int count = 0;
    int sampleSize = Math.min(1000, table.length);        
    for (int i = 0; i < sampleSize; i++) {
      if (table[i].key != 0) {
        count++;
      }
    } return (double) count / sampleSize;
  }
} 
