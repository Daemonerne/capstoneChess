package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MovePool;
import engine.forBoard.MoveUtils;

import java.util.*;

/**
 * The Queen class represents a queen chess piece. It is capable of moving diagonally, vertically, and horizontally on the chessboard.
 * It provides methods for calculating legal moves, evaluating positional bonuses, and creating new instances after valid moves.
 * This class is designed to encapsulate the behavior and attributes specific to a queen piece in the context of a chess game.
 *
 * @author Aaron Ho
 */
public final class Queen extends Piece {

  private static final int[] CANDIDATE_MOVE_COORDINATES = { -9, -8, -7, -1, 1, 7, 8, 9 };

  private static final Map<Integer, MoveUtils.Line[]> PRECOMPUTED_CANDIDATES = computeCandidates();

  private static final MoveUtils.Line[] EMPTY_LINES_ARRAY = new MoveUtils.Line[0];

  /**
   * Constructs a queen chess piece with the specified alliance, initial position, and number of moves.
   *
   * @param alliance       The alliance (color) of the queen piece.
   * @param piecePosition  The initial position of the queen on the board.
   * @param numMoves       The number of moves made by the queen.
   */
  public Queen(final Alliance alliance, final int piecePosition, final int numMoves) {
    super(PieceType.QUEEN, alliance, piecePosition, true, numMoves);
  }

  /**
   * Constructs a queen chess piece with the specified alliance, initial position, first move status, and number of moves.
   *
   * @param alliance       The alliance (color) of the queen piece.
   * @param piecePosition  The initial position of the queen on the board.
   * @param isFirstMove    True if this is the queen's first move, false otherwise.
   * @param numMoves       The number of moves made by the queen.
   */
  public Queen(final Alliance alliance,
               final int piecePosition,
               final boolean isFirstMove,
               final int numMoves) {
    super(PieceType.QUEEN, alliance, piecePosition, isFirstMove, numMoves);
  }

  private static Map<Integer, MoveUtils.Line[]> computeCandidates() {
    Map<Integer, MoveUtils.Line[]> candidates = new HashMap<>();
    for (int position = 0; position < BoardUtils.NUM_TILES; position++) {
      List<MoveUtils.Line> lines = new ArrayList<>();
      for (int offset : CANDIDATE_MOVE_COORDINATES) {
        int destination = position;
        MoveUtils.Line line = new MoveUtils.Line();
        while (BoardUtils.isValidTileCoordinate(destination)) {
          if (isFirstColumnExclusion(destination, offset) || isEightColumnExclusion(destination, offset)) {
            break;
          }
          destination += offset;
          if (BoardUtils.isValidTileCoordinate(destination)) {
            line.addCoordinate(destination);
          }
        }
        if (!line.isEmpty()) {
          lines.add(line);
        }
      }
      if (!lines.isEmpty()) {
        candidates.put(position, lines.toArray(EMPTY_LINES_ARRAY != null ? EMPTY_LINES_ARRAY : new MoveUtils.Line[0]));
      }
    }
    return Collections.unmodifiableMap(candidates);
  }

  /**
   * Calculates the legal moves that the queen can make on the given chess board.
   *
   * @param board The current chess board.
   * @return A collection of legal moves that the queen can make.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    for (final MoveUtils.Line line : PRECOMPUTED_CANDIDATES.get(this.piecePosition)) {
      for (final int candidateDestinationCoordinate : line.getLineCoordinates()) {
        final Piece pieceAtDestination = board.getPiece(candidateDestinationCoordinate);
        if (pieceAtDestination == null) {
          // Use MovePool instead of creating new Move instances
          legalMoves.add(MovePool.INSTANCE.getMajorMove(board, this, candidateDestinationCoordinate));
        } else {
          final Alliance pieceAlliance = pieceAtDestination.getPieceAllegiance();
          if (this.pieceAlliance != pieceAlliance) {
            // Use MovePool instead of creating new Move instances
            legalMoves.add(MovePool.INSTANCE.getMajorAttackMove(board, this, candidateDestinationCoordinate, pieceAtDestination));
          }
          break;
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Evaluates the positional bonus of the queen at its current location on the board.
   *
   * @return The positional bonus value for the queen's current location.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.queenBonus(this.piecePosition, board);
  }

  /**
   * Creates a new queen piece after a valid move.
   *
   * @param move The move to be executed.
   * @return A new queen piece resulting from the move.
   */
  @Override
  public Queen movePiece(final Move move) {
    return PieceUtils.Instance.getMovedQueen(move.getMovedPiece().getPieceAllegiance(), move.getDestinationCoordinate());
  }

  /**
   * Returns a string representation of the queen.
   *
   * @return A string indicating the type of the piece.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  private static boolean isFirstColumnExclusion(final int position, final int offset) {
    return BoardUtils.Instance.FirstColumn.get(position) && ((offset == -9) || (offset == -1) || (offset == 7));
  }

  private static boolean isEightColumnExclusion(final int position, final int offset) {
    return BoardUtils.Instance.EighthColumn.get(position) && ((offset == -7) || (offset == 1) || (offset == 9));
  }
}