package engine.forPiece;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static engine.forBoard.Move.*;

/**
 * The `Pawn` class represents a pawn chess piece. It extends the Piece class and defines the specific properties and behaviors
 * of a pawn on the chess board. Pawns have a special move pattern that includes advancing by one or two squares on their first move,
 * capturing diagonally, and performing en passant captures. Additionally, pawns can be promoted to other pieces when they reach the
 * opposing player's back rank.
 * <br><br>
 * This class provides methods for calculating legal moves for a pawn, including standard moves, capturing moves, en passant moves,
 * and pawn promotion moves. It also implements the locationBonus method to evaluate a pawn's position on the board.
 * Additionally, the class keeps track of the number of moves made by the pawn, and this information is used for certain functionalities
 * such as pawn promotion and move tracking.
 * <br><br>
 * Known bug: Occasionally, at a rapid pace of input (1 move a second) a promoted pawn will not be promoted into a
 * queen and will remain a pawn on either the 1st or 8th rank. We think this has to do with the `paint()` or `repaint()` calls
 * in the `Table` class when the new queen is supposed to be painted.
 * <br><br>
 * Note: Despite the rules of chess allowing you to under-promote pieces, there are only a few rare circumstances where knights
 * are the best or only move to end the game or keep it alive. Therefore, to simplify this experience for both the coder and the
 * player, the promotion is hardcoded to be a `Queen`. This is also for your enjoyment of the game, because we know that you are
 * too stupid and dramatic and romantic to realize that promoting to any other piece is horribly pointless and idiotic.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public final class Pawn extends Piece {

  /*** An array of possible candidate move coordinates for a pawn, including advancing by one or two squares and capturing diagonally. */
  private final static int[] CANDIDATE_MOVE_COORDINATES = { 8, 16, 7, 9 };

  /**
   * Constructs a new Pawn instance with the given alliance and piece position.
   *
   * @param allegiance     The alliance (color) of the pawn.
   * @param piecePosition  The current position of the pawn on the board.
   */
  public Pawn(final Alliance allegiance,
              final int piecePosition, final int numMoves) {
    super(PieceType.PAWN, allegiance, piecePosition, true, numMoves);
  }

  /**
   * Constructs a new Pawn instance with the given alliance, piece position, and first move status.
   *
   * @param alliance       The alliance (color) of the pawn.
   * @param piecePosition  The current position of the pawn on the board.
   * @param isFirstMove    Whether it's the first move of the pawn.
   */
  public Pawn(final Alliance alliance,
              final int piecePosition,
              final boolean isFirstMove,
              final int numMoves) {
    super(PieceType.PAWN, alliance, piecePosition, isFirstMove, numMoves);
  }

  /**
   * Calculates the location bonus for the pawn based on its position on the board.
   * The bonus affects the pawn's tactical strength in different positions. This method
   * calls upon the `pawnBonus` method in `Alliance` and returns a value from a preset
   * array of values depending on the position of the piece.
   *
   * @return The location bonus value for the pawn.
   */
  @Override
  public int locationBonus(final Board board) {
    return this.pieceAlliance.pawnBonus(this.piecePosition, board);
  }

  /**
   * Calculates the legal moves for the pawn on the given board.
   *
   * @param board The current board.
   * @return A collection of legal moves for the pawn.
   */
  @Override
  public Collection<Move> calculateLegalMoves(final Board board) {
    final List<Move> legalMoves = new ArrayList<>();
    // Iterates over possible candidate move coordinates for the pawn
    for (final int currentCandidateOffset: CANDIDATE_MOVE_COORDINATES) {
      int candidateDestinationCoordinate =
              this.piecePosition + (this.pieceAlliance.getDirection() * currentCandidateOffset);
      if (!BoardUtils.isValidTileCoordinate(candidateDestinationCoordinate)) {
        continue;
      }
      // Handles standard pawn move (one square)
      if (currentCandidateOffset == 8 && board.getPiece(candidateDestinationCoordinate) == null) {
        // Handles pawn promotion move when reaching the last rank
        if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
          legalMoves.add(new PawnPromotion(
                  new PawnMove(board, this, candidateDestinationCoordinate), PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(new PawnPromotion(
                  new PawnMove(board, this, candidateDestinationCoordinate), PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(new PawnPromotion(
                  new PawnMove(board, this, candidateDestinationCoordinate), PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
          legalMoves.add(new PawnPromotion(
                  new PawnMove(board, this, candidateDestinationCoordinate), PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
        } else {
          legalMoves.add(new PawnMove(board, this, candidateDestinationCoordinate));
        }
      }
      // Handles pawn jump (pawn can move two squares on its first move)
      else if (currentCandidateOffset == 16 && this.isFirstMove() &&
              ((BoardUtils.Instance.SecondRow.get(this.piecePosition) && this.pieceAlliance.isBlack()) ||
                      (BoardUtils.SeventhRow.get(this.piecePosition) && this.pieceAlliance.isWhite()))) {
        final int behindCandidateDestinationCoordinate =
                this.piecePosition + (this.pieceAlliance.getDirection() * 8);
        if (board.getPiece(candidateDestinationCoordinate) == null &&
                board.getPiece(behindCandidateDestinationCoordinate) == null) {
          legalMoves.add(new PawnJump(board, this, candidateDestinationCoordinate));
        }
      }
      // Handles pawn capturing moves
      else if (currentCandidateOffset == 7 &&
              !((BoardUtils.Instance.EighthColumn.get(this.piecePosition) && this.pieceAlliance.isWhite()) ||
                      (BoardUtils.Instance.FirstColumn.get(this.piecePosition) && this.pieceAlliance.isBlack()))) {
        if (board.getPiece(candidateDestinationCoordinate) != null) {
          final Piece pieceOnCandidate = board.getPiece(candidateDestinationCoordinate);
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate), PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate), PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate), PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate), PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
            } else {
              legalMoves.add(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate, pieceOnCandidate));
            }
          }
        } else if (board.getEnPassantPawn() != null && board.getEnPassantPawn().getPiecePosition() ==
                (this.piecePosition + (this.pieceAlliance.getOppositeDirection()))) {
          final Piece pieceOnCandidate = board.getEnPassantPawn();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            legalMoves.add(
                    new PawnEnPassantAttack(board, this, candidateDestinationCoordinate, pieceOnCandidate));

          }
        }
      }
      // Handles en passant capture
      else if (currentCandidateOffset == 9 &&
              !((BoardUtils.Instance.FirstColumn.get(this.piecePosition) && this.pieceAlliance.isWhite()) ||
                      (BoardUtils.Instance.EighthColumn.get(this.piecePosition) && this.pieceAlliance.isBlack()))) {
        if (board.getPiece(candidateDestinationCoordinate) != null) {
          if (this.pieceAlliance !=
                  board.getPiece(candidateDestinationCoordinate).getPieceAllegiance()) {
            // Handles pawn promotion move after capturing
            if (this.pieceAlliance.isPawnPromotionSquare(candidateDestinationCoordinate)) {
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate,
                              board.getPiece(candidateDestinationCoordinate)), PieceUtils.Instance.getMovedQueen(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate,
                              board.getPiece(candidateDestinationCoordinate)), PieceUtils.Instance.getMovedRook(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate,
                              board.getPiece(candidateDestinationCoordinate)), PieceUtils.Instance.getMovedBishop(this.pieceAlliance, candidateDestinationCoordinate)));
              legalMoves.add(new PawnPromotion(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate,
                              board.getPiece(candidateDestinationCoordinate)), PieceUtils.Instance.getMovedKnight(this.pieceAlliance, candidateDestinationCoordinate)));
            } else {
              legalMoves.add(
                      new PawnAttackMove(board, this, candidateDestinationCoordinate,
                              board.getPiece(candidateDestinationCoordinate)));
            }
          }
        } else if (board.getEnPassantPawn() != null && board.getEnPassantPawn().getPiecePosition() ==
                (this.piecePosition - (this.pieceAlliance.getOppositeDirection()))) {
          final Piece pieceOnCandidate = board.getEnPassantPawn();
          if (this.pieceAlliance != pieceOnCandidate.getPieceAllegiance()) {
            legalMoves.add(new PawnEnPassantAttack(board, this, candidateDestinationCoordinate, pieceOnCandidate));

          }
        }
      }
    }
    return Collections.unmodifiableList(legalMoves);
  }

  /**
   * Returns a string representation of the pawn.
   *
   * @return The string representation of the pawn.
   */
  @Override
  public String toString() {
    return this.pieceType.toString();
  }

  /**
   * Creates a new Pawn instance after making the given move.
   *
   * @param move The move to be made.
   * @return A new Pawn instance after the move is made.
   */
  @Override
  public Pawn movePiece(final Move move) {
    return PieceUtils.Instance.getMovedPawn(move.getMovedPiece().getPieceAllegiance(), move.getDestinationCoordinate());
  }

}