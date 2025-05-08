package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forPiece.Piece;

/**
 * The TaperedEvaluator class implements a tapered evaluation function that smoothly
 * transitions between opening, middlegame, and endgame evaluation based on the game phase.
 * This approach prevents abrupt changes in evaluation scores as the game progresses,
 * leading to more consistent play.
 *
 * @author Aaron Ho
 */
public class TaperedEvaluator implements BoardEvaluator {

  /** Singleton instance of the TaperedEvaluator. */
  private static final TaperedEvaluator INSTANCE = new TaperedEvaluator();

  /** Phase weights for each piece type. */
  private static final int PAWN_PHASE_VALUE = 0;
  private static final int KNIGHT_PHASE_VALUE = 1;
  private static final int BISHOP_PHASE_VALUE = 1;
  private static final int ROOK_PHASE_VALUE = 2;
  private static final int QUEEN_PHASE_VALUE = 4;

  /** Total phase weight at the start of the game. */
  private static final int TOTAL_PHASE =
          4 * KNIGHT_PHASE_VALUE + 4 * BISHOP_PHASE_VALUE + 4 * ROOK_PHASE_VALUE + 2 * QUEEN_PHASE_VALUE;

  /** References to phase-specific evaluators. */
  private final BoardEvaluator openingEvaluator;
  private final BoardEvaluator middlegameEvaluator;
  private final BoardEvaluator endgameEvaluator;

  /** Private constructor to enforce singleton pattern. */
  private TaperedEvaluator() {
    this.openingEvaluator = OpeningGameEvaluator.get();
    this.middlegameEvaluator = MiddlegameBoardEvaluator.get();
    this.endgameEvaluator = EndgameBoardEvaluator.get();
  }

  /**
   * Returns the singleton instance of the TaperedEvaluator.
   *
   * @return The TaperedEvaluator instance.
   */
  public static TaperedEvaluator get() {
    return INSTANCE;
  }

  /**
   * Evaluates the board position using a tapered evaluation that smoothly
   * transitions between opening, middlegame, and endgame evaluations.
   *
   * @param board The current chess board position.
   * @param depth The current search depth.
   * @return The evaluation score.
   */
  @Override
  public double evaluate(final Board board, final int depth) {
    // Special case: if the game is over, return immediate result
    if (BoardUtils.isEndOfGame(board)) {
      if (board.currentPlayer().isInCheckMate()) {
        return board.currentPlayer().getAlliance().isWhite() ? -9000 : 9000;
      } else {
        return 0; // Draw
      }
    }

    // Calculate the current game phase (0 = opening, 256 = endgame)
    int phase = calculatePhase(board);

    // Calculate each phase-specific evaluation
    double openingScore = openingEvaluator.evaluate(board, depth);

    // For efficiency, only calculate middlegame and endgame if needed
    double middlegameScore = 0;
    double endgameScore = 0;

    // Early opening phase - use mostly opening evaluation
    if (phase < 64) {
      // Bias heavily toward opening evaluation
      return (openingScore * (256 - phase) + middlegameEvaluator.evaluate(board, depth) * phase) / 256;
    }
    // Late opening to early middlegame
    else if (phase < 128) {
      middlegameScore = middlegameEvaluator.evaluate(board, depth);
      return (openingScore * (128 - (phase - 64)) + middlegameScore * (phase - 64)) / 128;
    }
    // Late middlegame to early endgame
    else if (phase < 192) {
      middlegameScore = middlegameEvaluator.evaluate(board, depth);
      endgameScore = endgameEvaluator.evaluate(board, depth);
      return (middlegameScore * (192 - (phase - 128)) + endgameScore * (phase - 128)) / 128;
    }
    // Late endgame
    else {
      return endgameEvaluator.evaluate(board, depth);
    }
  }

  /**
   * Calculates the current game phase based on the material on the board.
   * Returns a value from 0 (opening) to 256 (endgame).
   *
   * @param board The current chess board.
   * @return The game phase (0-256).
   */
  private int calculatePhase(final Board board) {
    int phase = TOTAL_PHASE;

    // Calculate remaining material with phase values
    for (final Piece piece : board.getAllPieces()) {
      switch (piece.getPieceType()) {
        case PAWN -> phase -= PAWN_PHASE_VALUE;
        case KNIGHT -> phase -= KNIGHT_PHASE_VALUE;
        case BISHOP -> phase -= BISHOP_PHASE_VALUE;
        case ROOK -> phase -= ROOK_PHASE_VALUE;
        case QUEEN -> phase -= QUEEN_PHASE_VALUE;
      }
    }

    // Convert to 0-256 range where 0 is opening and 256 is endgame
    phase = (phase * 256 + (TOTAL_PHASE / 2)) / TOTAL_PHASE;

    // Additional adjustments based on piece development and specific structures
    phase = adjustPhaseForDevelopment(board, phase);

    return Math.min(256, Math.max(0, phase));
  }

  /**
   * Adjusts the calculated phase value based on piece development and
   * specific board structures to better identify the actual game stage.
   *
   * @param board The current chess board.
   * @param phase The initially calculated phase.
   * @return The adjusted phase value.
   */
  private int adjustPhaseForDevelopment(final Board board, int phase) {
    // Count developed minor pieces
    int developedMinorPieces = 0;
    boolean whiteKingCastled = board.whitePlayer().getPlayerKing().isCastled();
    boolean blackKingCastled = board.blackPlayer().getPlayerKing().isCastled();

    // Recognize undeveloped position - push phase toward opening
    if (phase < 64 && (!whiteKingCastled || !blackKingCastled)) {
      return Math.max(0, phase - 16);
    }

    // Look for opening structures in the center
    int centralPawnCount = countCentralPawns(board);
    if (centralPawnCount >= 3 && phase < 128) {
      return Math.max(0, phase - 16);
    }

    return phase;
  }

  /**
   * Counts pawns in the central four squares of the board.
   *
   * @param board The current chess board.
   * @return The number of pawns in the center.
   */
  private int countCentralPawns(final Board board) {
    int count = 0;
    int[] centralSquares = {27, 28, 35, 36}; // d5, e5, d4, e4

    for (int square : centralSquares) {
      Piece piece = board.getPiece(square);
      if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN) {
        count++;
      }
    }

    return count;
  }
}