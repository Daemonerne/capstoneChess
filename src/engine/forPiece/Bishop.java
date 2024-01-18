package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;

import java.util.*;

import static engine.forBoard.BoardUtils.isValidTileCoordinate;
import static engine.forBoard.Move.MajorAttackMove;
import static engine.forBoard.Move.MajorMove;
import static engine.forBoard.MoveUtils.Line;

/**
 * The Bishop class represents a bishop chess piece. It extends the Piece class and defines the specific properties and
 * behaviors of a bishop on the chess board. Bishops move diagonally across the board, and each bishop stays on the
 * color of square it started on.
 * <br><br>
 * This class provides methods for calculating legal moves for a bishop, including diagonal moves and capturing moves.
 * It precomputes legal move lines for each tile on the board, taking edge cases and exclusions into consideration.
 *
 * @author Aaron Ho
 */
public final class Bishop extends Piece {

  /*** An array of possible candidate move coordinates for a bishop, representing diagonal moves. */
  private final static int[] CANDIDATE_MOVE_COORDINATES = { -9, -7, 7, 9 };

  /*** A map that stores the precomputed legal move lines for each tile on the board, considering diagonal moves. */
  private final static Map<Integer, Line[]> PRECOMPUTED_CANDIDATES = computeCandidates();

  /**
   * Constructs a new Bishop instance with the given alliance, piece position, and number of moves.
   *
   * @param alliance         The alliance (color) of the bishop.
   * @param piecePosition    The current position of the bishop on the board.
   * @param numMoves         The number of moves made by the bishop.
   */
  public Bishop(final Alliance alliance,
                final int piecePosition,
                final int numMoves) {
    super(PieceType.BISHOP, alliance, piecePosition, true, numMoves);
  }

  /**
   * Constructs a new Bishop instance with the given alliance, piece position, first move status, and number of moves.
   *
   * @param alliance         The alliance (color) of the bishop.
   * @param piecePosition    The current position of the bishop on the board.
   * @param isFirstMove      Whether it's the first move of the bishop.
   * @param numMoves         The number of moves made by the bishop.
   */
  public Bishop(final Alliance alliance,
                final int piecePosition,
                final boolean isFirstMove,
                final int numMoves) {
    super(PieceType.BISHOP, alliance, piecePosition, isFirstMove, numMoves);
  }

  /**
   * Computes and precomputes the legal move lines for each tile on the board for the bishop, considering diagonal moves.
   * Takes edge cases and exclusions into consideration
   *
   * @return A map containing the precomputed legal move lines for each tile on the board.
   */
  private static Map<Integer, Line[]> computeCandidates() {
    Map<Integer, Line[]> candidates = new HashMap<>();
    for (int position = 0; position < BoardUtils.NUM_TILES; position++) {
      List<Line> lines = new ArrayList<>();
      for (int offset : CANDIDATE_MOVE_COORDINATES) {
        int destination = position;
        Line line = new Line();
        while (isValidTileCoordinate(destination)) {
          if (isFirstColumnExclusion(destination, offset) ||
                  isEighthColumnExclusion(destination, offset)) {
            break;
          }
          destination += offset;
          if (isValidTileCoordinate(destination)) {
            line.addCoordinate(destination);
          }
        }
        if (!line.isEmpty()) {
          lines.add(line);
        }
      }
      if (!lines.isEmpty()) {
        candidates.put(position, lines.toArray(new Line[0]));
      }
    }
    return Collections.unmodifiableMap(candidates);
  }

  /**
   * Calculates the legal moves for the bishop on the board. Generates diagonal moves and capturing moves along each line.
   *
   * @param board The current state of the chess board.
   * @return A collection of legal moves for the bishop.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    for (final Line line : PRECOMPUTED_CANDIDATES.get(this.piecePosition)) {
      for (final int candidateDestinationCoordinate : line.getLineCoordinates()) {
        final Piece pieceAtDestination = board.getPiece(candidateDestinationCoordinate);
        if (pieceAtDestination == null) {
          legalMoves.add(new MajorMove(board, this, candidateDestinationCoordinate));
        } else {
          final Alliance pieceAlliance = pieceAtDestination.getPieceAllegiance();
          if (this.pieceAlliance != pieceAlliance) {
            legalMoves.add(new MajorAttackMove(board, this, candidateDestinationCoordinate,
                    pieceAtDestination));
          }
          break;
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Computes the location bonus for the bishop based on its current position on the board.
   *
   * @return The location bonus for the bishop.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.bishopBonus(this.piecePosition, board);
  }

  /**
   * Creates a new Bishop instance after making the given move.
   *
   * @param move The move to be made.
   * @return A new Bishop instance after the move is made.
   */
  @Override
  public Bishop movePiece(final Move move) {
    return PieceUtils.Instance.getMovedBishop(move.getMovedPiece().getPieceAllegiance(), move.getDestinationCoordinate());
  }

  /**
   * Creates a new Bishop instance after making the given move.
   *
   * @return A new Bishop instance after the move is made.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Checks if the given position and offset lead to an exclusion for the bishop's diagonal move due to being on the first column.
   *
   * @param position            The current position of the bishop.
   * @param offset              The offset being considered for the diagonal move.
   * @return true, if the exclusion applies, otherwise false.
   */
  private static boolean isFirstColumnExclusion(final int position,
                                                final int offset) {
    return (BoardUtils.Instance.FirstColumn.get(position) &&
            ((offset == -9) || (offset == 7)));
  }

  /**
   * Checks if the given position and offset lead to an exclusion for the bishop's diagonal move due to being on the eighth column.
   *
   * @param position            The current position of the bishop.
   * @param offset              The offset being considered for the diagonal move.
   * @return true, if the exclusion applies, otherwise false.
   */
  private static boolean isEighthColumnExclusion(final int position,
                                                 final int offset) {
    return BoardUtils.Instance.EighthColumn.get(position) &&
            ((offset == -7) || (offset == 9));
  }

}
