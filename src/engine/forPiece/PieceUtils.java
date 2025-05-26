package engine.forPiece;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import engine.Alliance;
import engine.forBoard.BoardUtils;

/**
 * The PieceUtils class provides utility methods for creating and accessing pre-computed instances
 * of moved chess pieces. This singleton enum uses Guava's ImmutableTable to store all possible
 * piece instances for each alliance and destination coordinate combination, optimizing performance
 * by avoiding repeated object creation during move generation.
 * <p>
 * The class maintains separate tables for pawns, knights, bishops, rooks, and queens, allowing
 * efficient retrieval of moved piece instances based on alliance and destination coordinate.
 * All stored pieces are configured as having been moved (isFirstMove = false) with a move count of 1.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
enum PieceUtils {

  /**
   * The singleton instance of the PieceUtils class providing access to all utility methods
   * and pre-computed piece tables.
   */
  Instance;

  /**
   * Table storing all possible moved Queen instances for each alliance and destination coordinate.
   */
  private final Table<Alliance, Integer, Queen> ALL_POSSIBLE_QUEENS = PieceUtils.createAllPossibleMovedQueens();

  /**
   * Table storing all possible moved Rook instances for each alliance and destination coordinate.
   */
  private final Table<Alliance, Integer, Rook> ALL_POSSIBLE_ROOKS = PieceUtils.createAllPossibleMovedRooks();

  /**
   * Table storing all possible moved Knight instances for each alliance and destination coordinate.
   */
  private final Table<Alliance, Integer, Knight> ALL_POSSIBLE_KNIGHTS = PieceUtils.createAllPossibleMovedKnights();

  /**
   * Table storing all possible moved Bishop instances for each alliance and destination coordinate.
   */
  private final Table<Alliance, Integer, Bishop> ALL_POSSIBLE_BISHOPS = PieceUtils.createAllPossibleMovedBishops();

  /**
   * Table storing all possible moved Pawn instances for each alliance and destination coordinate.
   */
  private final Table<Alliance, Integer, Pawn> ALL_POSSIBLE_PAWNS = PieceUtils.createAllPossibleMovedPawns();

  /**
   * Retrieves a moved Pawn instance for the specified alliance and destination coordinate.
   *
   * @param alliance The alliance of the Pawn piece.
   * @param destinationCoordinate The destination coordinate for the moved Pawn piece.
   * @return The moved Pawn piece instance.
   */
  Pawn getMovedPawn(final Alliance alliance,
                    final int destinationCoordinate) {
    return ALL_POSSIBLE_PAWNS.get(alliance, destinationCoordinate);
  }

  /**
   * Retrieves a moved Knight instance for the specified alliance and destination coordinate.
   *
   * @param alliance The alliance of the Knight piece.
   * @param destinationCoordinate The destination coordinate for the moved Knight piece.
   * @return The moved Knight piece instance.
   */
  Knight getMovedKnight(final Alliance alliance,
                        final int destinationCoordinate) {
    return ALL_POSSIBLE_KNIGHTS.get(alliance, destinationCoordinate);
  }

  /**
   * Retrieves a moved Bishop instance for the specified alliance and destination coordinate.
   *
   * @param alliance The alliance of the Bishop piece.
   * @param destinationCoordinate The destination coordinate for the moved Bishop piece.
   * @return The moved Bishop piece instance.
   */
  Bishop getMovedBishop(final Alliance alliance,
                        final int destinationCoordinate) {
    return ALL_POSSIBLE_BISHOPS.get(alliance, destinationCoordinate);
  }

  /**
   * Retrieves a moved Rook instance for the specified alliance and destination coordinate.
   *
   * @param alliance The alliance of the Rook piece.
   * @param destinationCoordinate The destination coordinate for the moved Rook piece.
   * @return The moved Rook piece instance.
   */
  Rook getMovedRook(final Alliance alliance,
                    final int destinationCoordinate) {
    return ALL_POSSIBLE_ROOKS.get(alliance, destinationCoordinate);
  }

  /**
   * Retrieves a moved Queen instance for the specified alliance and destination coordinate.
   *
   * @param alliance The alliance of the Queen piece.
   * @param destinationCoordinate The destination coordinate for the moved Queen piece.
   * @return The moved Queen piece instance.
   */
  Queen getMovedQueen(final Alliance alliance,
                      final int destinationCoordinate) {
    return ALL_POSSIBLE_QUEENS.get(alliance, destinationCoordinate);
  }

  /**
   * Creates and populates an ImmutableTable containing all possible moved Pawn instances
   * for each alliance and board coordinate combination.
   *
   * @return An ImmutableTable containing all possible moved Pawn instances.
   */
  private static Table<Alliance, Integer, Pawn> createAllPossibleMovedPawns() {
    final ImmutableTable.Builder<Alliance, Integer, Pawn> pieces = ImmutableTable.builder();
    for(final Alliance alliance : Alliance.values()) {
      for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
        pieces.put(alliance, i, new Pawn(alliance, i, false, 1));
      }
    }
    return pieces.build();
  }

  /**
   * Creates and populates an ImmutableTable containing all possible moved Knight instances
   * for each alliance and board coordinate combination.
   *
   * @return An ImmutableTable containing all possible moved Knight instances.
   */
  private static Table<Alliance, Integer, Knight> createAllPossibleMovedKnights() {
    final ImmutableTable.Builder<Alliance, Integer, Knight> pieces = ImmutableTable.builder();
    for(final Alliance alliance : Alliance.values()) {
      for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
        pieces.put(alliance, i, new Knight(alliance, i, false, 1));
      }
    }
    return pieces.build();
  }

  /**
   * Creates and populates an ImmutableTable containing all possible moved Bishop instances
   * for each alliance and board coordinate combination.
   *
   * @return An ImmutableTable containing all possible moved Bishop instances.
   */
  private static Table<Alliance, Integer, Bishop> createAllPossibleMovedBishops() {
    final ImmutableTable.Builder<Alliance, Integer, Bishop> pieces = ImmutableTable.builder();
    for(final Alliance alliance : Alliance.values()) {
      for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
        pieces.put(alliance, i, new Bishop(alliance, i, false, 1));
      }
    }
    return pieces.build();
  }

  /**
   * Creates and populates an ImmutableTable containing all possible moved Rook instances
   * for each alliance and board coordinate combination.
   *
   * @return An ImmutableTable containing all possible moved Rook instances.
   */
  private static Table<Alliance, Integer, Rook> createAllPossibleMovedRooks() {
    final ImmutableTable.Builder<Alliance, Integer, Rook> pieces = ImmutableTable.builder();
    for(final Alliance alliance : Alliance.values()) {
      for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
        pieces.put(alliance, i, new Rook(alliance, i, false, 1));
      }
    }
    return pieces.build();
  }

  /**
   * Creates and populates an ImmutableTable containing all possible moved Queen instances
   * for each alliance and board coordinate combination.
   *
   * @return An ImmutableTable containing all possible moved Queen instances.
   */
  private static Table<Alliance, Integer, Queen> createAllPossibleMovedQueens() {
    final ImmutableTable.Builder<Alliance, Integer, Queen> pieces = ImmutableTable.builder();
    for(final Alliance alliance : Alliance.values()) {
      for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
        pieces.put(alliance, i, new Queen(alliance, i, false, 1));
      }
    }
    return pieces.build();
  }
}