package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forPiece.Piece;
import engine.forPlayer.forAI.BoardEvaluator;
import engine.forPlayer.forAI.EndgameBoardEvaluator;
import engine.forPlayer.forAI.MiddlegameBoardEvaluator;
import engine.forPlayer.forAI.OpeningGameEvaluator;

import static engine.forPiece.Piece.PieceType.*;

public class TaperedEvaluator implements BoardEvaluator {
  private static final TaperedEvaluator Instance = new TaperedEvaluator();

  // Phase weights (total = 256)
  private static final int PAWN_PHASE_WEIGHT = 0;
  private static final int KNIGHT_PHASE_WEIGHT = 1;
  private static final int BISHOP_PHASE_WEIGHT = 1;
  private static final int ROOK_PHASE_WEIGHT = 2;
  private static final int QUEEN_PHASE_WEIGHT = 4;

  // Game phase totals (material for both sides)
  private static final int TOTAL_PHASE =
          16 * PAWN_PHASE_WEIGHT +
                  4 * KNIGHT_PHASE_WEIGHT +
                  4 * BISHOP_PHASE_WEIGHT +
                  4 * ROOK_PHASE_WEIGHT +
                  2 * QUEEN_PHASE_WEIGHT;

  // Singleton getter
  public static TaperedEvaluator get() {
    return Instance;
  }

  private TaperedEvaluator() {}

  @Override
  public double evaluate(final Board board, final int depth) {
    // Calculate current game phase
    int phase = calculatePhase(board);

    // Get evaluations from each phase-specific evaluator
    double openingScore = OpeningGameEvaluator.get().evaluate(board, depth);
    double middlegameScore = MiddlegameBoardEvaluator.get().evaluate(board, depth);
    double endgameScore = EndgameBoardEvaluator.get().evaluate(board, depth);

    // Interpolate between phases
    if (phase > TOTAL_PHASE * 0.8) {
      // Early game: heavy opening weight
      return (openingScore * 0.8) + (middlegameScore * 0.2);
    } else if (phase > TOTAL_PHASE * 0.4) {
      // Middle game: blend of opening and middlegame
      double openingWeight = (phase - (TOTAL_PHASE * 0.4)) / (TOTAL_PHASE * 0.4);
      return (openingScore * openingWeight) + (middlegameScore * (1 - openingWeight));
    } else if (phase > TOTAL_PHASE * 0.1) {
      // Late middlegame: blend of middlegame and endgame
      double middlegameWeight = (phase - (TOTAL_PHASE * 0.1)) / (TOTAL_PHASE * 0.3);
      return (middlegameScore * middlegameWeight) + (endgameScore * (1 - middlegameWeight));
    } else {
      // Endgame: pure endgame evaluation
      return endgameScore;
    }
  }

  /**
   * Calculates the current game phase based on remaining material.
   * Returns a value between 0 (endgame) and TOTAL_PHASE (opening).
   */
  private int calculatePhase(final Board board) {
    int phase = TOTAL_PHASE;

    // Subtract phase value for each missing piece
    for (Piece.PieceType pieceType : Piece.PieceType.values()) {
      if (pieceType == Piece.PieceType.KING) continue;

      // Count pieces of each type
      int whitePieceCount = 0;
      int blackPieceCount = 0;

      for (Piece piece : board.getWhitePieces()) {
        if (piece.getPieceType() == pieceType) whitePieceCount++;
      }

      for (Piece piece : board.getBlackPieces()) {
        if (piece.getPieceType() == pieceType) blackPieceCount++;
      }

      // Calculate expected piece counts
      int expectedCount;
      int phaseWeight;

      switch (pieceType) {
        case PAWN:
          expectedCount = 16;
          phaseWeight = PAWN_PHASE_WEIGHT;
          break;
        case KNIGHT:
        case BISHOP:
          expectedCount = 4;
          phaseWeight = pieceType == Piece.PieceType.KNIGHT ?
                  KNIGHT_PHASE_WEIGHT : BISHOP_PHASE_WEIGHT;
          break;
        case ROOK:
          expectedCount = 4;
          phaseWeight = ROOK_PHASE_WEIGHT;
          break;
        case QUEEN:
          expectedCount = 2;
          phaseWeight = QUEEN_PHASE_WEIGHT;
          break;
        default:
          expectedCount = 0;
          phaseWeight = 0;
      }

      // Subtract phase weight for each missing piece
      phase -= (expectedCount - (whitePieceCount + blackPieceCount)) * phaseWeight;
    }

    return Math.max(0, phase);
  }
}