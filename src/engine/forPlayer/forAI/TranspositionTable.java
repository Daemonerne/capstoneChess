package engine.forPlayer.forAI;

/**
 * Definition for TranspositionTable.Entry (if not already defined)
 */
public class TranspositionTable {
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

  public static final byte EXACT = 0;
  public static final byte LOWERBOUND = 1;
  public static final byte UPPERBOUND = 2;
}