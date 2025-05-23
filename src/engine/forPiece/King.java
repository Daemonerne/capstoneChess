package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MovePool;

import java.util.*;

/**
 * The King class represents a king chess piece with specialized movement and castling capabilities.
 * Kings move one square in any direction and are the most important piece on the board, as their
 * capture results in checkmate. This class handles standard king movement, castling rights,
 * and maintains state information about whether the king has moved or castled.
 * <p>
 * Kings can participate in castling moves under specific conditions, including unmoved status,
 * clear paths to rooks, and absence of check. The class precomputes legal move patterns
 * for efficient move generation and validates edge cases for board boundaries.
 *
 * @author Aaron Ho
 */
public final class King extends Piece {

  /** An array of coordinate offsets representing all possible one-square king moves in eight directions. */
  private final static int[] CANDIDATE_MOVE_COORDINATES = { -9, -8, -7, -1, 1, 7, 8, 9 };

  /**
   * A precomputed map storing legal move offsets for each board position.
   * Keys represent tile coordinates and values contain arrays of valid destination offsets,
   * accounting for board edge restrictions.
   */
  private final static Map<Integer, int[]> PRECOMPUTED_CANDIDATES = computeCandidates();

  /** Indicates whether this king has completed a castling move. */
  private final boolean isCastled;

  /** Indicates whether this king retains the right to castle kingside. */
  private final boolean kingSideCastleCapable;

  /** Indicates whether this king retains the right to castle queenside. */
  private final boolean queenSideCastleCapable;

  /**
   * Constructs a King with specified alliance, position, and castling capabilities.
   * This constructor assumes the king has not moved and sets the move count to zero.
   *
   * @param alliance The alliance (color) of the king.
   * @param piecePosition The initial position of the king on the board.
   * @param kingSideCastleCapable Whether the king can castle kingside.
   * @param queenSideCastleCapable Whether the king can castle queenside.
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
   * Constructs a King with complete state information including move and castling status.
   *
   * @param alliance The alliance (color) of the king.
   * @param piecePosition The current position of the king on the board.
   * @param isFirstMove Whether this is the king's first move.
   * @param isCastled Whether the king has completed a castling move.
   * @param kingSideCastleCapable Whether the king can castle kingside.
   * @param queenSideCastleCapable Whether the king can castle queenside.
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
   * Precomputes and returns legal move offsets for each board position.
   * This method calculates valid king moves for all 64 squares, excluding
   * moves that would place the king off the board or violate edge constraints.
   *
   * @return A map containing precomputed legal move offsets for each board position.
   */
  private static Map<Integer, int[]> computeCandidates() {
    final Map<Integer, int[]> candidates = new HashMap<>();
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
   * Returns whether this king has completed a castling move.
   *
   * @return True if the king has castled, false otherwise.
   */
  public boolean isCastled() {
    return this.isCastled;
  }

  /**
   * Returns whether this king retains the right to castle kingside.
   *
   * @return True if kingside castling is possible, false otherwise.
   */
  public boolean isKingSideCastleCapable() {
    return this.kingSideCastleCapable;
  }

  /**
   * Returns whether this king retains the right to castle queenside.
   *
   * @return True if queenside castling is possible, false otherwise.
   */
  public boolean isQueenSideCastleCapable() {
    return this.queenSideCastleCapable;
  }

  /**
   * Calculates and returns all legal moves for this king on the given board.
   * This includes standard one-square moves and capturing moves, but excludes
   * castling moves which are handled separately by the player classes.
   *
   * @param board The current board state.
   * @return A collection of legal moves for this king.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    for (final int currentCandidateOffset: PRECOMPUTED_CANDIDATES.get(this.piecePosition)) {
      final int candidateDestinationCoordinate = this.piecePosition + currentCandidateOffset;
      final Piece pieceAtDestination = board.getPiece(candidateDestinationCoordinate);
      if (pieceAtDestination == null) {
        legalMoves.add(MovePool.INSTANCE.getMajorMove(board, this, candidateDestinationCoordinate));
      } else {
        final Alliance pieceAtDestinationAllegiance = pieceAtDestination.getPieceAllegiance();
        if (this.pieceAlliance != pieceAtDestinationAllegiance) {
          legalMoves.add(MovePool.INSTANCE.getMajorAttackMove(board, this, candidateDestinationCoordinate,
                  pieceAtDestination));
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Returns the string representation of this king piece.
   *
   * @return The piece type string representation.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Calculates the positional bonus for this king based on its current board position.
   * The bonus value is determined by alliance-specific evaluation tables.
   *
   * @param board The current board state.
   * @return The location bonus value for this king's position.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.kingBonus(this.piecePosition, board);
  }

  /**
   * Creates a new King instance representing this king after executing the given move.
   * The new king will have updated position and status information based on the move type.
   *
   * @param move The move being executed.
   * @return A new King instance reflecting the post-move state.
   */
  @Override
  public King movePiece(final Move move) {
    return new King(this.pieceAlliance, move.getDestinationCoordinate(), false, move.isCastlingMove(), false, false);
  }

  /**
   * Compares this King with another object for equality.
   * Kings are equal if they have the same basic piece properties and castling status.
   *
   * @param other The object to compare with this King.
   * @return True if the objects are equal, false otherwise.
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
   * Calculates the hash code for this King instance.
   * The hash code incorporates the base piece hash code and castling status.
   *
   * @return The hash code value for this King.
   */
  @Override
  public int hashCode() {
    return (31 * super.hashCode()) + (isCastled ? 1 : 0);
  }

  /**
   * Determines whether a move from the given position with the specified offset
   * would be excluded due to first column boundary constraints.
   *
   * @param currentCandidate The current position of the king.
   * @param candidateDestinationCoordinate The move offset being evaluated.
   * @return True if the move should be excluded due to first column constraints, false otherwise.
   */
  private static boolean isFirstColumnExclusion(final int currentCandidate,
                                                final int candidateDestinationCoordinate) {
    return BoardUtils.Instance.FirstColumn.get(currentCandidate) &&
            ((candidateDestinationCoordinate == -9) || (candidateDestinationCoordinate == -1) ||
                    (candidateDestinationCoordinate == 7));
  }

  /**
   * Determines whether a move from the given position with the specified offset
   * would be excluded due to eighth column boundary constraints.
   *
   * @param currentCandidate The current position of the king.
   * @param candidateDestinationCoordinate The move offset being evaluated.
   * @return True if the move should be excluded due to eighth column constraints, false otherwise.
   */
  private static boolean isEighthColumnExclusion(final int currentCandidate,
                                                 final int candidateDestinationCoordinate) {
    return BoardUtils.Instance.EighthColumn.get(currentCandidate) &&
            ((candidateDestinationCoordinate == -7) || (candidateDestinationCoordinate == 1) ||
                    (candidateDestinationCoordinate == 9));
  }
}