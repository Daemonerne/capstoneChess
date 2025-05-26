package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MovePool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Pawn class represents a pawn chess piece with its unique movement and capture rules.
 * Pawns can move forward one square, move forward two squares on their first move, capture
 * diagonally, perform en passant captures, and promote to other pieces upon reaching the
 * opponent's back rank. This class extends the Piece class and implements all pawn-specific
 * behaviors including promotion to queen, rook, bishop, or knight pieces.
 * <p>
 * Pawn promotion is handled by generating all possible promotion moves when a pawn reaches
 * the promotion square, allowing the chess engine to evaluate all promotion options.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public final class Pawn extends Piece {

  /** Array of possible move coordinate offsets for pawn movement and capture patterns. */
  private static final int[] CANDIDATE_MOVE_COORDINATES = { 8, 16, 7, 9 };

  /**
   * Constructs a new Pawn instance with the specified alliance and position.
   *
   * @param allegiance The alliance (color) of the pawn.
   * @param piecePosition The current position of the pawn on the board.
   * @param numMoves The number of moves made by the pawn.
   */
  public Pawn(final Alliance allegiance,
              final int piecePosition, final int numMoves) {
    super(PieceType.PAWN, allegiance, piecePosition, true, numMoves);
  }

  /**
   * Constructs a new Pawn instance with the specified alliance, position, and move status.
   *
   * @param alliance The alliance (color) of the pawn.
   * @param piecePosition The current position of the pawn on the board.
   * @param isFirstMove Whether this is the pawn's first move.
   * @param numMoves The number of moves made by the pawn.
   */
  public Pawn(final Alliance alliance,
              final int piecePosition,
              final boolean isFirstMove,
              final int numMoves) {
    super(PieceType.PAWN, alliance, piecePosition, isFirstMove, numMoves);
  }

  /**
   * Calculates the positional bonus for this pawn based on its current board position.
   * The bonus value is determined by alliance-specific pawn position tables that favor
   * advanced and central pawn positions.
   *
   * @param board The current chess board.
   * @return The positional bonus value for this pawn's position.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.pawnBonus(this.piecePosition, board);
  }

  /**
   * Calculates all legal moves for this pawn on the given board, including standard moves,
   * two-square initial moves, diagonal captures, en passant captures, and promotion moves.
   * Returns all possible moves that can be legally executed from the current position.
   *
   * @param board The current chess board.
   * @return A collection of legal moves for this pawn.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();

    for (final int currentCandidateOffset: CANDIDATE_MOVE_COORDINATES) {
      int candidateDestinationCoordinate =
              this.piecePosition + (this.pieceAlliance.getDirection() * currentCandidateOffset);
      if (!BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)) {
        continue;
      }

      if (currentCandidateOffset == 8 && board.getPiece(candidateDestinationCoordinate) == null) {
        if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
          Move pawnMove = MovePool.INSTANCE.getPawnMove(board, this, candidateDestinationCoordinate);

          legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                  pawnMove, PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                  pawnMove, PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                  pawnMove, PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                  pawnMove, PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
        } else {
          legalMoves.add(MovePool.INSTANCE.getPawnMove(board, this, candidateDestinationCoordinate));
        }
      }
      else if (currentCandidateOffset == 16 && this.isFirstMove() &&
              ((BoardUtils.Instance.SecondRow.get(this.piecePosition) && this.pieceAlliance.isBlack()) ||
                      (BoardUtils.SeventhRow.get(this.piecePosition) && this.pieceAlliance.isWhite()))) {
        final int behindCandidateDestinationCoordinate =
                this.piecePosition + (this.pieceAlliance.getDirection() * 8);
        if (board.getPiece(candidateDestinationCoordinate) == null &&
                board.getPiece(behindCandidateDestinationCoordinate) == null) {
          legalMoves.add(MovePool.INSTANCE.getPawnJump(board, this, candidateDestinationCoordinate));
        }
      }
      else if (currentCandidateOffset == 7 &&
              !((BoardUtils.Instance.EighthColumn.get(this.piecePosition) && this.pieceAlliance.isWhite()) ||
                      (BoardUtils.Instance.FirstColumn.get(this.piecePosition) && this.pieceAlliance.isBlack()))) {
        if (board.getPiece(candidateDestinationCoordinate) != null) {
          final Piece pieceOnCandidate = board.getPiece(candidateDestinationCoordinate);
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
              Move pawnAttackMove = MovePool.INSTANCE.getPawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate);

              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
            } else {
              legalMoves.add(MovePool.INSTANCE.getPawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
            }
          }
        } else if (board.getEnPassantPawn() != null && board.getEnPassantPawn().getPiecePosition() ==
                (this.piecePosition + (this.pieceAlliance.getOppositeDirection()))) {
          final Piece pieceOnCandidate = board.getEnPassantPawn();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            legalMoves.add(MovePool.INSTANCE.getPawnEnPassantAttack(board, this, candidateDestinationCoordinate, pieceOnCandidate));
          }
        }
      }
      else if (currentCandidateOffset == 9 &&
              !((BoardUtils.Instance.FirstColumn.get(this.piecePosition) && this.pieceAlliance.isWhite()) ||
                      (BoardUtils.Instance.EighthColumn.get(this.piecePosition) && this.pieceAlliance.isBlack()))) {
        if (board.getPiece(candidateDestinationCoordinate) != null) {
          if (this.pieceAlliance !=
                  board.getPiece(candidateDestinationCoordinate).getPieceAllegiance()) {
            if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
              Move pawnAttackMove = MovePool.INSTANCE.getPawnAttackMove(board, this, candidateDestinationCoordinate,
                      board.getPiece(candidateDestinationCoordinate));

              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(MovePool.INSTANCE.getPawnPromotion(
                      pawnAttackMove, PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
            } else {
              legalMoves.add(MovePool.INSTANCE.getPawnAttackMove(board, this, candidateDestinationCoordinate,
                      board.getPiece(candidateDestinationCoordinate)));
            }
          }
        } else if (board.getEnPassantPawn() != null && board.getEnPassantPawn().getPiecePosition() ==
                (this.piecePosition - (this.pieceAlliance.getOppositeDirection()))) {
          final Piece pieceOnCandidate = board.getEnPassantPawn();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            legalMoves.add(MovePool.INSTANCE.getPawnEnPassantAttack(board, this, candidateDestinationCoordinate, pieceOnCandidate));
          }
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Returns the string representation of this pawn piece.
   *
   * @return The string representation of the pawn piece type.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Creates a new Pawn instance representing this pawn after the specified move is executed.
   * The new pawn will be positioned at the move's destination coordinate.
   *
   * @param move The move to be executed.
   * @return A new Pawn instance at the destination position.
   */
  @Override
  public Pawn movePiece(final Move move) {
    return PieceUtils.Instance.getMovedPawn(move.getMovedPiece().getPieceAllegiance(), move.getDestinationCoordinate());
  }
}