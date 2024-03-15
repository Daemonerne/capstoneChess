package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

/**
 * The StandardBoardEvaluator class is responsible for evaluating the current state of a chess board
 * using a standard scoring system. It considers various factors such as piece values, mobility, attacks,
 * pawn structure, king safety, and more, to provide an overall assessment of the board's state.
 * <br><br>
 * This class follows a singleton pattern to ensure that only one instance of the evaluator exists,
 * which can be accessed using the static get() method.
 *
 * @author Aaron Ho
 */
public class StandardBoardEvaluator implements BoardEvaluator {

  /*** Bonus applied when the opponent is in checkmate. */
  private final static int CheckMateBonus = 100000;

  /*** Bonus applied when the opponent is in check. */
  private final static int CheckBonus = 5;

  /*** Bonus applied when a player has successfully castled. */
  private final static int CastleBonus = 20;

  /*** Singleton instance of the StandardBoardEvaluator. */
  private static final StandardBoardEvaluator Instance = new StandardBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private StandardBoardEvaluator() {}

  /**
   * Returns the singleton instance of StandardBoardEvaluator.
   *
   * @return The instance of StandardBoardEvaluator.
   */
  public static StandardBoardEvaluator get() {
    return Instance;
  }

  /**
   * Evaluates the given board from the perspective of a player and returns a score.
   *
   * @param board The current state of the chess board.
   * @param depth The search depth in the AI's thinking process.
   * @return The evaluation score of the board.
   */
  @Override
  public double evaluate(final Board board, final int depth) {
    return (score(board.whitePlayer(), depth, board) - score(board.blackPlayer(), depth, board));
  }

  /**
   * Provides detailed evaluation details for debugging purposes.
   *
   * @param board The current state of the chess board.
   * @param depth The search depth in the AI's thinking process.
   * @return A detailed evaluation breakdown of the board.
   */
  public String evaluationDetails(final Board board, final int depth) {
    return ("White Mobility : " + mobility(board.whitePlayer()) + "\n") +
            "White castle : " + castle(board.whitePlayer()) + "\n" +
            "White pieceEval : " + pieceEvaluations(board.whitePlayer(), board) + "\n" +
            "---------------------\n" +
            "Black Mobility : " + mobility(board.blackPlayer()) + "\n" +
            "Black castle : " + castle(board.blackPlayer()) + "\n" +
            "Black pieceEval : " + pieceEvaluations(board.blackPlayer(), board) + "\n" +
            "Depth : " + depth + "\n\n" +
            "Final Score = " + evaluate(board, depth);
  }

  @VisibleForTesting
  private static double score(final Player player,
                              final int depth,
                              final Board board) {
    return  mobility(player) +
            castle(player) +
            pieceEvaluations(player, board);
  }

  /**
   * Calculate the score based on piece evaluations for the specified player also give the game stage.
   *
   * @param player The player to evaluate.
   * @return The piece evaluation score.
   */
  private static double pieceEvaluations(final Player player, final Board board) {
    double pieceEvaluationScore = 0;
    for (final Piece piece: player.getActivePieces()) {
      pieceEvaluationScore += (piece.getPieceValue() + piece.locationBonus(board));
    }
    return pieceEvaluationScore;
  }

  /**
   * Calculate the score based on mobility for the specified player.
   *
   * @param player The player to evaluate.
   * @return The mobility score.
   */
  private static double mobility(final Player player) {
    return player.getLegalMoves().size();
  }

  /**
   * Calculate the score based on checks performed by the specified player.
   *
   * @param player The player to evaluate.
   * @return The check score.
   */
  private static double check(final Player player) {
    return player.getOpponent().isInCheck() ? CheckBonus : 0;

  }

  /**
   * Calculate the depth bonus for the specified depth.
   *
   * @param depth The depth to evaluate.
   * @return The depth bonus.
   */
  private static double depthBonus(final int depth) {
    return depth == 0 ? 1 : 100 * depth;

  }

  /**
   * Calculate the score based on castling performed by the specified player.
   *
   * @param player The player to evaluate.
   * @return The castling score.
   */
  private static double castle(final Player player) {
    return player.isCastled() ? CastleBonus : 0;
  }
}