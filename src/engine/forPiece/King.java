package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MovePool;

import java.util.*;

/**
 * The King class represents a king chess piece. It extends the Piece class and defines the specific properties and behaviors
 * of a king on the chess board. Kings have limited movement, being able to move one square in any direction.
 * Kings can also participate in castling moves, which is a special double-move involving the king and a rook.
 * <br><br>
 * The King class provides methods for calculating legal moves for a king, including standard moves and capturing moves.
 * It also implements methods for checking if the king is castled and if it is capable of performing king-side or
 * queen-side castling moves.
 *
 * @author Aaron Ho
 */
public final class King extends Piece {

  /*** An array of possible candidate move coordinates for a king, representing all possible one-square moves. */
  private final static int[] CANDIDATE_MOVE_COORDINATES = { -9, -8, -7, -1, 1, 7, 8, 9 };

  /*** A map that stores the precomputed legal move offsets for each tile on the board,
   * taking edge cases into consideration. */
  private final static Map <Integer, int[]> PRECOMPUTED_CANDIDATES = computeCandidates();

  /*** A boolean indicating if the king has been castled. */
  private final boolean isCastled;

  /*** A boolean indicating if the king is capable of performing king-side castling. */
  private final boolean kingSideCastleCapable;

  /*** A boolean indicating if the king is capable of performing queen-side castling. */
  private final boolean queenSideCastleCapable;

  /**
   * Constructs a new King instance with the given alliance, piece position, and castling capabilities.
   *
   * @param alliance                 The alliance (color) of the king.
   * @param piecePosition            The current position of the king on the board.
   * @param kingSideCastleCapable    Whether the king is capable of performing king-side castling.
   * @param queenSideCastleCapable   Whether the king is capable of performing queen-side castling.
   */
  public King(final Alliance alliance,
              final int piecePosition,
              final boolean kingSideCastleCapable,
              final boolean queenSideCastleCapable) {
    super(PieceType.KING, alliance, piecePosition, true, 0);
    this.isCastled = false;
    this.kingSideCastleCapable = kingSideCastleCapable;
    this.queenSideCastleCapable = queenSideCastleCapable;
  }

  /**
   * Constructs a new King instance with the given alliance, piece position, first move status, castling status,
   * and castling capabilities.
   *
   * @param alliance                 The alliance (color) of the king.
   * @param piecePosition            The current position of the king on the board.
   * @param isFirstMove              Whether it's the first move of the king.
   * @param isCastled                Whether the king has been castled.
   * @param kingSideCastleCapable    Whether the king is capable of performing king-side castling.
   * @param queenSideCastleCapable   Whether the king is capable of performing queen-side castling.
   */
  public King(final Alliance alliance,
              final int piecePosition,
              final boolean isFirstMove,
              final boolean isCastled,
              final boolean kingSideCastleCapable,
              final boolean queenSideCastleCapable) {
    super(PieceType.KING, alliance, piecePosition, isFirstMove, 0);
    this.isCastled = isCastled;
    this.kingSideCastleCapable = kingSideCastleCapable;
    this.queenSideCastleCapable = queenSideCastleCapable;
  }

  /**
   * Computes and precomputes the legal move offsets for each tile on the board for the king.
   * Takes into account-edge cases to exclude illegal move offsets.
   *
   * @return A map containing the precomputed legal move offsets for each tile on the board.
   */
  private static Map < Integer, int[] > computeCandidates() {
    final Map < Integer, int[] > candidates = new HashMap < > ();
    for (int position = 0; position < BoardUtils.NUM_TILES; position++) {
      int[] legalOffsets = new int[CANDIDATE_MOVE_COORDINATES.length];
      int numLegalOffsets = 0;
      for (int offset: CANDIDATE_MOVE_COORDINATES) {
        if (isFirstColumnExclusion(position, offset) ||
                isEighthColumnExclusion(position, offset)) {
          continue;
        }
        int destination = position + offset;
        if (BoardUtils.isValidTileCoordinate(destination)) {
          legalOffsets[numLegalOffsets++] = offset;
        }
      }
      if (numLegalOffsets > 0) {
        candidates.put(position, Arrays.copyOf(legalOffsets, numLegalOffsets));
      }
    }
    return Collections.unmodifiableMap(candidates);
  }

  /**
   * Checks if the king has been castled.
   *
   * @return true, if the king has been castled, otherwise false.
   */
  public boolean isCastled() {
    return this.isCastled;
  }

  /**
   * Checks if the king is capable of performing king-side castling.
   *
   * @return true if king is capable of king-side castling, otherwise false.
   */
  public boolean isKingSideCastleCapable() {
    return this.kingSideCastleCapable;
  }

  /**
   * Checks if the king is capable of performing queen-side castling.
   *
   * @return true if king is capable of queen-side castling, otherwise false.
   */
  public boolean isQueenSideCastleCapable() {
    return this.queenSideCastleCapable;
  }

  /**
   * Calculates the legal moves for the king on the board. Generates both standard moves and capturing moves.
   *
   * @param board The current state of the chess board.
   * @return A collection of legal moves for the king.
   */
  @Override
  public Collection < Move > calculateLegalMoves(final Board board) {
    final List < Move > legalMoves = new ArrayList < > ();
    for (final int currentCandidateOffset: PRECOMPUTED_CANDIDATES.get(this.piecePosition)) {
      final int candidateDestinationCoordinate = this.piecePosition + currentCandidateOffset;
      final Piece pieceAtDestination = board.getPiece(candidateDestinationCoordinate);
      if (pieceAtDestination == null) {
        // Use MovePool instead of creating new Move instances
        legalMoves.add(MovePool.INSTANCE.getMajorMove(board, this, candidateDestinationCoordinate));
      } else {
        final Alliance pieceAtDestinationAllegiance = pieceAtDestination.getPieceAllegiance();
        if (this.pieceAlliance != pieceAtDestinationAllegiance) {
          // Use MovePool instead of creating new Move instances
          legalMoves.add(MovePool.INSTANCE.getMajorAttackMove(board, this, candidateDestinationCoordinate,
                  pieceAtDestination));
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Returns a string representation of the king.
   *
   * @return The string representation of the king.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Computes the location bonus for the king based on its current position on the board.
   *
   * @return The location bonus for the king.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.kingBonus(this.piecePosition, board);
  }

  /**
   * Creates a new King instance after making the given move.
   *
   * @param move The move to be made.
   * @return A new King instance after the move is made.
   */
  @Override
  public King movePiece(final Move move) {
    return new King(this.pieceAlliance, move.getDestinationCoordinate(), false, move.isCastlingMove(), false, false);
  }

  /**
   * Compares the King object with another object for equality.
   *
   * @param other The object to compare with.
   * @return true if the objects are equal, otherwise false.
   */
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof King king)) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }
    return isCastled == king.isCastled;
  }

  /**
   * Computes the hash code for the King object.
   *
   * @return The hash code value of the King object.
   */
  @Override
  public int hashCode() {
    return (31 * super.hashCode()) + (isCastled ? 1 : 0);
  }

  /**
   * Checks if the given candidate destination is excluded as a valid move due to the king's position on the first column.
   *
   * @param currentCandidate              The current position of the king.
   * @param candidateDestinationCoordinate   The candidate destination coordinate to check.
   * @return true if the candidate destination is excluded, otherwise false.
   */
  private static boolean isFirstColumnExclusion(final int currentCandidate,
                                                final int candidateDestinationCoordinate) {
    return BoardUtils.Instance.FirstColumn.get(currentCandidate) &&
            ((candidateDestinationCoordinate == -9) || (candidateDestinationCoordinate == -1) ||
                    (candidateDestinationCoordinate == 7));
  }

  /**
   * Checks if the given candidate destination is excluded as a valid move due to the king's position on the eighth column.
   *
   * @param currentCandidate              The current position of the king.
   * @param candidateDestinationCoordinate   The candidate destination coordinate to check.
   * @return true if the candidate destination is excluded, otherwise false.
   */
  private static boolean isEighthColumnExclusion(final int currentCandidate,
                                                 final int candidateDestinationCoordinate) {
    return BoardUtils.Instance.EighthColumn.get(currentCandidate) &&
            ((candidateDestinationCoordinate == -7) || (candidateDestinationCoordinate == 1) ||
                    (candidateDestinationCoordinate == 9));
  }
}