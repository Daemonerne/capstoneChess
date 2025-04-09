package engine.forPlayer.forAI;

/**
 * An optimized fixed-size transposition table that uses power-of-2 sizing
 * for fast modulo operations and implements an enhanced replacement strategy.
 */
public class TranspositionTable {
  private static final int DEFAULT_SIZE_MB = 256;
  static class Entry {
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
    
  /** Creates a transposition table with the default size. */
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
    
  /** Clears the transposition table. */
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
   * Get an entry from the transposition table.
   * 
   * @param zobristHash The Zobrist hash of the position
   * @return The entry if found, null otherwise
   */
  public Entry get(long zobristHash) {
    int index = (int) (zobristHash & mask);
    Entry entry = table[index];
    if (entry.key == zobristHash && entry.key != 0) {
      entry.age = currentAge;
      return entry;
    } int index2 = (index ^ (int)(zobristHash >>> 32)) & mask;
    Entry entry2 = table[index2];
    if (entry2.key == zobristHash && entry2.key != 0) {
      entry2.age = currentAge;
      return entry2;
    } return null;
  }
    
  /**
   * Store a position in the transposition table.
   * Uses an enhanced replacement strategy:
   * 1. Empty slots
   * 2. Entries with lower depth
   * 3. Older entries
   * 4. Secondary hash location if primary is occupied
   * 
   * @param zobristHash The Zobrist hash of the position
   * @param score The evaluation score
   * @param depth The search depth
   * @param nodeType The node type (EXACT, LOWERBOUND, UPPERBOUND)
   */
  public void store(long zobristHash, double score, int depth, byte nodeType) {
    int index = (int) (zobristHash & mask);
    Entry entry = table[index];
    int index2 = (index ^ (int)(zobristHash >>> 32)) & mask;
    Entry entry2 = table[index2];
    boolean useFirst = shouldReplace(entry, entry2, depth);
    Entry target = useFirst ? entry : entry2;
    if (target.key == 0 || target.depth <= depth || target.age < currentAge) {
      target.key = zobristHash;
      target.score = score;
      target.depth = (short) depth;
      target.nodeType = nodeType;
      target.age = currentAge;
    }
  }
  
  /*** Helper method to determine which entry to replace. */
  private boolean shouldReplace(Entry entry1, Entry entry2, int depth) {
    if (entry1.key == 0) return true;
    if (entry2.key == 0) return false;
    if (entry1.depth < entry2.depth) return true;
    if (entry1.depth > entry2.depth) return false; 
    return entry1.age <= entry2.age;
  }
    
  /** Returns the approximate fill percentage of the table. */
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
