package engine.forPlayer.forAI;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.Piece;

import java.util.*;

/**
 * Implements Static Exchange Evaluation (SEE) for accurate evaluation of capture sequences.
 * SEE recursively evaluates the best possible outcome of a capture sequence for each side,
 * helping with move ordering and pruning bad exchanges.
 */
public class StaticExchangeEvaluator {

  /** Singleton instance of the StaticExchangeEvaluator. */
  private static final StaticExchangeEvaluator INSTANCE = new StaticExchangeEvaluator();

  /** Piece values for SEE calculations, simpler than full evaluation values */
  private static final int[] SEE_PIECE_VALUES = {
          100,    // PAWN
          320,    // KNIGHT
          330,    // BISHOP
          500,    // ROOK
          900,    // QUEEN
          20000   // KING - very high value to ensure kings are rarely considered for exchanges
  };

  /** Private constructor to enforce singleton pattern */
  private StaticExchangeEvaluator() {}

  /**
   * Returns the singleton instance.
   */
  public static StaticExchangeEvaluator get() {
    return INSTANCE;
  }

  /**
   * Evaluates the score of a capture move using static exchange evaluation.
   *
   * @param board The current board
   * @param move The capture move to evaluate
   * @return Estimated material gain/loss from the exchange sequence, positive = good for side to move
   */
  public int evaluate(final Board board, final Move move) {
    if (!move.isAttack()) {
      return 0; // Non-capture moves have SEE value of 0
    }

    // Get the square where the exchange happens
    final int targetSquare = move.getDestinationCoordinate();

    // Get the attacking piece value
    final Piece attackingPiece = move.getMovedPiece();
    final int attackerValue = getPieceValue(attackingPiece.getPieceType());

    // Get the captured piece value
    final Piece capturedPiece = move.getAttackedPiece();
    final int capturedValue = getPieceValue(capturedPiece.getPieceType());

    // Special case for en passant - simply return pawn value
    if (move instanceof Move.PawnEnPassantAttack) {
      return SEE_PIECE_VALUES[0]; // Pawn value
    }

    // Check if piece is undefended for fast path
    if (!isPieceDefended(capturedPiece, board)) {
      return capturedValue; // Just return the value of the captured piece
    }

    // For normal captures, we need to find all attackers to the square
    final Alliance sideToMove = board.currentPlayer().getAlliance();
    final Alliance opponentSide = board.currentPlayer().getOpponent().getAlliance();

    // Create attack maps for square
    final List<Piece> attackers = findAttackers(board, targetSquare);

    // Make the initial capture
    int gain = capturedValue;

    // If the sequence stops here, return the gain
    if (attackers.isEmpty()) {
      return gain;
    }

    // Simulate the capture sequence
    List<Piece> remainingAttackers = new ArrayList<>(attackers);

    // Remove the initial attacker
    remainingAttackers.removeIf(p -> p.equals(attackingPiece));

    Alliance side = opponentSide;
    int lastAttackerValue = attackerValue;

    // Keep capturing until no attackers remain or it's not beneficial
    while (!remainingAttackers.isEmpty()) {
      // Find the least valuable piece that can attack
      Piece nextAttacker = findLeastValuableAttacker(remainingAttackers, side);

      // If no attacker found for this side, break
      if (nextAttacker == null) {
        break;
      }

      // Make the capture
      gain = -gain + lastAttackerValue;

      // If this capture would result in a negative score, the side wouldn't make it
      if (gain < 0) {
        break;
      }

      // Update for next iteration
      lastAttackerValue = getPieceValue(nextAttacker.getPieceType());
      remainingAttackers.remove(nextAttacker);
      side = side.equals(Alliance.WHITE) ? Alliance.BLACK : Alliance.WHITE;
    }

    // FIX: Just return the gain from the exchange sequence
    return gain;
  }

  /**
   * Checks if a piece is defended by any other piece of the same alliance.
   * More efficient than full attackers calculation for a simple check.
   */
  public boolean isPieceDefended(final Piece piece, final Board board) {
    if (piece == null) return false;

    final int piecePosition = piece.getPiecePosition();
    final Alliance pieceAlliance = piece.getPieceAllegiance();

    // Check if any piece of the same alliance can attack the position
    for (Piece otherPiece : board.getAllPieces()) {
      if (otherPiece.getPieceAllegiance() == pieceAlliance &&
              !otherPiece.equals(piece)) {

        Collection<Move> moves = otherPiece.calculateLegalMoves(board);
        for (Move move : moves) {
          if (move.getDestinationCoordinate() == piecePosition) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Finds all pieces that can attack a specific square.
   */
  private List<Piece> findAttackers(final Board board, final int targetSquare) {
    List<Piece> attackers = new ArrayList<>();

    for (Piece piece : board.getAllPieces()) {
      Collection<Move> moves = piece.calculateLegalMoves(board);

      for (Move move : moves) {
        if (move.getDestinationCoordinate() == targetSquare) {
          attackers.add(piece);
          break;
        }
      }
    }

    return attackers;
  }

  /**
   * Finds the least valuable attacker of a given side from the list of attackers.
   */
  private Piece findLeastValuableAttacker(List<Piece> attackers, Alliance side) {
    Piece leastValuableAttacker = null;
    int leastValue = Integer.MAX_VALUE;

    for (Piece attacker : attackers) {
      if (attacker.getPieceAllegiance() == side) {
        int value = getPieceValue(attacker.getPieceType());
        if (value < leastValue) {
          leastValue = value;
          leastValuableAttacker = attacker;
        }
      }
    }

    return leastValuableAttacker;
  }

  /**
   * Gets the SEE value for a piece type.
   */
  private int getPieceValue(Piece.PieceType pieceType) {
    return switch (pieceType) {
      case PAWN -> SEE_PIECE_VALUES[0];
      case KNIGHT -> SEE_PIECE_VALUES[1];
      case BISHOP -> SEE_PIECE_VALUES[2];
      case ROOK -> SEE_PIECE_VALUES[3];
      case QUEEN -> SEE_PIECE_VALUES[4];
      case KING -> SEE_PIECE_VALUES[5];
    };
  }
}