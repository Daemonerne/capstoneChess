package engine.forPiece;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import engine.Alliance;
import engine.forBoard.BoardUtils;

/**
 * The PieceUtils class is a utility class that provides methods for creating, accessing, and manipulating all possible
 * moved instances of different types of chess pieces. It uses Guava's ImmutableTable to store instances of different pieces
 * for each alliance, destination coordinate, and the number of moves made by the piece. The class follows the singleton
 * pattern to ensure only one instance of PieceUtils is created and used.
 * <br><br>
 * This class allows easy access to moved instances of various chess pieces, such as Pawns, Knights, Bishops, Rooks, and Queens.
 * These instances can be retrieved based on the alliance, destination coordinate, and the number of moves made by the piece.
 * Additionally, PieceUtils provides methods to increment the number of moves for each type of chess piece.
 * The utility is useful for generating all possible moves for a given piece type on the board and keeping track of the
 * number of moves made by each piece.
 *
 * @see com.google.common.collect.ImmutableTable
 * @author dareTo81
 * @author Aaron Ho
 */
enum PieceUtils {

  /*** The singleton instance of the PieceUtils class. */
  Instance;

  /*** Table storing all possible moved instances of Queen pieces for each alliance, destination coordinate, and number of moves. */
  private final Table<Alliance, Integer, Queen> ALL_POSSIBLE_QUEENS = PieceUtils.createAllPossibleMovedQueens();

  /*** Table storing all possible moved instances of Rook pieces for each alliance, destination coordinate, and number of moves. */
  private final Table<Alliance, Integer, Rook> ALL_POSSIBLE_ROOKS = PieceUtils.createAllPossibleMovedRooks();

  /*** Table storing all possible moved instances of Knight pieces for each alliance, destination coordinate, and number of moves. */
  private final Table<Alliance, Integer, Knight> ALL_POSSIBLE_KNIGHTS = PieceUtils.createAllPossibleMovedKnights();

  /*** Table storing all possible moved instances of Bishop pieces for each alliance, destination coordinate, and number of moves. */
  private final Table<Alliance, Integer, Bishop> ALL_POSSIBLE_BISHOPS = PieceUtils.createAllPossibleMovedBishops();

  /*** Table storing all possible moved instances of Pawn pieces for each alliance, destination coordinate, and number of moves. */
  private final Table<Alliance, Integer, Pawn> ALL_POSSIBLE_PAWNS = PieceUtils.createAllPossibleMovedPawns();

  /**
   * Gets a moved Pawn piece instance for the specified alliance, destination coordinate, and number of moves.
   *
   * @param alliance              The alliance of the Pawn piece.
   * @param destinationCoordinate The destination coordinate for the moved Pawn piece.
   * @return The moved Pawn piece instance.
   */
  Pawn getMovedPawn(final Alliance alliance,
                    final int destinationCoordinate) {
    return ALL_POSSIBLE_PAWNS.get(alliance, destinationCoordinate);
  }

  /**
   * Gets a moved Knight piece instance for the specified alliance, destination coordinate, and number of moves.
   *
   * @param alliance              The alliance of the Knight piece.
   * @param destinationCoordinate The destination coordinate for the moved Knight piece.
   * @return The moved Knight piece instance.
   */
  Knight getMovedKnight(final Alliance alliance,
                        final int destinationCoordinate) {
    return ALL_POSSIBLE_KNIGHTS.get(alliance, destinationCoordinate);
  }

  /**
   * Gets a moved Bishop piece instance for the specified alliance, destination coordinate, and number of moves.
   *
   * @param alliance              The alliance of the Bishop piece.
   * @param destinationCoordinate The destination coordinate for the moved Bishop piece.
   * @return  The moved Bishop piece instance.
   */
  Bishop getMovedBishop(final Alliance alliance,
                        final int destinationCoordinate) {
    return ALL_POSSIBLE_BISHOPS.get(alliance, destinationCoordinate);
  }

  /**
   * Gets a moved Rook piece instance for the specified alliance, destination coordinate, and number of moves.
   *
   * @param alliance              The alliance of the Rook piece.
   * @param destinationCoordinate The destination coordinate for the moved Rook piece.
   * @return The moved Rook piece instance.
   */
  Rook getMovedRook(final Alliance alliance,
                    final int destinationCoordinate) {
    return ALL_POSSIBLE_ROOKS.get(alliance, destinationCoordinate);
  }

  /**
   * Gets a moved Queen piece instance for the specified alliance, destination coordinate, and number of moves.
   *
   * @param alliance              The alliance of the Queen piece.
   * @param destinationCoordinate The destination coordinate for the moved Queen piece.
   * @return The moved Queen piece instance.
   */
  Queen getMovedQueen(final Alliance alliance,
                      final int destinationCoordinate) {
    return ALL_POSSIBLE_QUEENS.get(alliance, destinationCoordinate);
  }

  /**
   * Creates and returns an ImmutableTable containing all possible moved instances of Pawn pieces for each alliance, destination coordinate, and number of moves.
   *
   * @return The ImmutableTable containing all possible moved instances of Pawn pieces.
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
   * Creates and returns an ImmutableTable containing all possible moved instances of Knight pieces for each alliance, destination coordinate, and number of moves.
   *
   * @return The ImmutableTable containing all possible moved instances of Knight pieces.
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
   * Creates and returns an ImmutableTable containing all possible moved instances of Bishop pieces for each alliance, destination coordinate, and number of moves.
   *
   * @return The ImmutableTable containing all possible moved instances of Bishop pieces.
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
   * Creates and returns an ImmutableTable containing all possible moved instances of Rook pieces for each alliance, destination coordinate, and number of moves.
   *
   * @return The ImmutableTable containing all possible moved instances of Rook pieces.
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
   * Creates and returns an ImmutableTable containing all possible moved instances of Queen pieces for each alliance, destination coordinate, and number of moves.
   *
   * @return The ImmutableTable containing all possible moved instances of Queen pieces.
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
