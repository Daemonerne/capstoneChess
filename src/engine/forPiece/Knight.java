package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;

import java.util.*;

import static engine.forBoard.Move.MajorAttackMove;
import static engine.forBoard.Move.MajorMove;

/**
 * The Knight class represents a knight chess piece. It extends the Piece class and defines the specific properties and behaviors
 * of a knight on the chess board. Knights have a unique move pattern, making an L-shaped move that consists of two squares in
 * one direction and one square perpendicular to the first direction. Knights can jump over other pieces, allowing them to
 * control squares that other pieces cannot reach directly.
 * <br><br>
 * This class provides methods for calculating legal moves for a knight, including standard moves and capturing moves.
 * It also implements the locationBonus method to evaluate a knight's position on the board.
 *
 * @author Aaron Ho
 */
public final class Knight extends Piece {

  /*** An array of possible candidate move coordinates for a knight in its L-shaped move pattern. */
  private final static int[] CANDIDATE_MOVE_COORDINATES = { -17, -15, -10, -6, 6, 10, 15, 17 };

  /*** A map that stores the precomputed legal move offsets for each tile on the board, taking into account edge cases. */
  private final static Map < Integer, int[] > PRECOMPUTED_CANDIDATES = computeCandidates();

  /**
   * Constructs a new Knight instance with the given alliance, piece position, and number of moves.
   *
   * @param alliance       The alliance (color) of the knight.
   * @param piecePosition  The current position of the knight on the board.
   * @param numMoves       The number of moves made by the knight.
   */
  public Knight(final Alliance alliance,
                final int piecePosition,
                final int numMoves) {
    super(PieceType.KNIGHT, alliance, piecePosition, true, numMoves);
  }

  /**
   * Constructs a new Knight instance with the given alliance, piece position, first move status, and number of moves.
   *
   * @param alliance       The alliance (color) of the knight.
   * @param piecePosition  The current position of the knight on the board.
   * @param isFirstMove    Whether it's the first move of the knight.
   * @param numMoves       The number of moves made by the knight.
   */
  public Knight(final Alliance alliance,
                final int piecePosition,
                final boolean isFirstMove,
                final int numMoves) {
    super(PieceType.KNIGHT, alliance, piecePosition, isFirstMove, numMoves);
  }

  /**
   * Computes and precomputes the legal move offsets for each tile on the board for the knight.
   * Takes into account edge cases to exclude illegal move offsets.
   *
   * @return A map containing the precomputed legal move offsets for each tile on the board.
   */
  private static Map < Integer, int[] > computeCandidates() {
    final Map < Integer, int[] > candidates = new HashMap < > ();
    for (int position = 0; position < BoardUtils.NUM_TILES; position++) {
      final int[] legalOffsets = new int[CANDIDATE_MOVE_COORDINATES.length];
      int numLegalMoves = 0;
      for (final int offset: CANDIDATE_MOVE_COORDINATES) {
        if (isFirstColumnExclusion(position, offset) ||
                isSecondColumnExclusion(position, offset) ||
                isSeventhColumnExclusion(position, offset) ||
                isEighthColumnExclusion(position, offset)) {
          continue;
        }
        final int destination = position + offset;
        if (BoardUtils.isValidTileCoordinate(destination)) {
          legalOffsets[numLegalMoves++] = destination;
        }
      }
      candidates.put(position, Arrays.copyOf(legalOffsets, numLegalMoves));
    }
    return Collections.unmodifiableMap(candidates);
  }

  /**
   * Calculates the legal moves for the knight on the given board.
   *
   * @param board The current board.
   * @return A collection of legal moves for the knight.
   */
  @Override
  public Collection < Move > calculateLegalMoves(final Board board) {
    final List < Move > legalMoves = new ArrayList < > ();
    for (final int candidateDestinationCoordinate: PRECOMPUTED_CANDIDATES.get(this.piecePosition)) {
      final Piece pieceAtDestination = board.getPiece(candidateDestinationCoordinate);
      if (pieceAtDestination == null) {
        legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
      } else {
        final Alliance pieceAtDestinationAllegiance = pieceAtDestination.getPieceAllegiance();
        if (this.pieceAlliance != pieceAtDestinationAllegiance) {
          legalMoves.add(new MajorAttackMove(board, this, candidateDestinationCoordinate,
                  pieceAtDestination));
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Calculates the location bonus for the knight based on its position on the board.
   *
   * @return The location bonus for the knight.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.knightBonus(this.piecePosition, board);
  }

  /**
   * Creates a new Knight instance after making the given move.
   *
   * @param move The move to be made.
   * @return A new Knight instance after the move is made.
   */
  @Override
  public Knight movePiece(final Move move) {
    return PieceUtils.Instance.getMovedKnight(move.getMovedPiece().getPieceAllegiance(), move.getDestinationCoordinate());
  }

  /**
   * Returns a string representation of the knight.
   *
   * @return The string representation of the knight.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Checks if the current position is on the first column and the candidate offset is excluded.
   *
   * @param currentPosition  The current position on the board.
   * @param candidateOffset  The candidate offset to check.
   * @return true if the exclusion criteria are met, otherwise false.
   */
  private static boolean isFirstColumnExclusion(final int currentPosition,
                                                final int candidateOffset) {
    return BoardUtils.Instance.FirstColumn.get(currentPosition) && ((candidateOffset == -17) ||
            (candidateOffset == -10) || (candidateOffset == 6) || (candidateOffset == 15));
  }

  /**
   * Checks if the current position is on the second column and the candidate offset is excluded.
   *
   * @param currentPosition  The current position on the board.
   * @param candidateOffset  The candidate offset to check.
   * @return true if the exclusion criteria are met, otherwise false.
   */
  private static boolean isSecondColumnExclusion(final int currentPosition,
                                                 final int candidateOffset) {
    return BoardUtils.Instance.SecondColumn.get(currentPosition) && ((candidateOffset == -10) || (candidateOffset == 6));
  }

  /**
   * Checks if the current position is on the seventh column and the candidate offset is excluded.
   *
   * @param currentPosition  The current position on the board.
   * @param candidateOffset  The candidate offset to check.
   * @return true if the exclusion criteria are met, otherwise false.
   */
  private static boolean isSeventhColumnExclusion(final int currentPosition,
                                                  final int candidateOffset) {
    return BoardUtils.Instance.SeventhColumn.get(currentPosition) && ((candidateOffset == -6) || (candidateOffset == 10));
  }

  /**
   * Checks if the current position is on the eighth column and the candidate offset is excluded.
   *
   * @param currentPosition  The current position on the board.
   * @param candidateOffset  The candidate offset to check.
   * @return true if the exclusion criteria are met, otherwise false.
   */
  private static boolean isEighthColumnExclusion(final int currentPosition,
                                                 final int candidateOffset) {
    return BoardUtils.Instance.EighthColumn.get(currentPosition) && ((candidateOffset == -15) || (candidateOffset == -6) ||
            (candidateOffset == 10) || (candidateOffset == 17));
  }

}
