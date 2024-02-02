package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.HashSet;
import java.util.Set;

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
public final class StandardBoardEvaluator implements BoardEvaluator {

  /*** Bonus applied when the opponent is in checkmate. */
  private final static int CheckMateBonus = 100000;

  /*** Bonus applied when the opponent is in check. */
  private final static int CheckBonus = 5;

  /*** Bonus applied when a player has successfully castled. */
  private final static int CastleBonus = 20;

    /*** Bonus applied when a player has two pawns in the center. */
  private final static double PawnCenterBonus = 25;

  /*** Bonus applied when a developed piece is a minor piece. */
  private final static int MinorDevelopmentBonus = 15;

  /*** Penalty applied for moving the same piece multiple times in the opening period. */
  private final static int RepeatedMoveInOpeningPenalty = -10; 

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
            "White kingThreats : " + kingThreats(board.whitePlayer(), depth) + "\n" +
            "White castle : " + castle(board.whitePlayer()) + "\n" +
            "White pieceEval : " + pieceEvaluations(board.whitePlayer(), board) + "\n" +
            "White pawnStructure : " + pawnStructure(board.whitePlayer(), board) + "\n" +
            "White kingSafety : " + kingSafety(board.whitePlayer(), board) + "\n\n" +
            "---------------------\n" +
            "Black Mobility : " + mobility(board.blackPlayer()) + "\n" +
            "Black kingThreats : " + kingThreats(board.blackPlayer(), depth) + "\n" +
            "Black castle : " + castle(board.blackPlayer()) + "\n" +
            "Black pieceEval : " + pieceEvaluations(board.blackPlayer(), board) + "\n" +
            "Black pawnStructure : " + pawnStructure(board.blackPlayer(), board) + "\n\n" +
            "Black kingSafety : " + kingSafety(board.blackPlayer(), board) + "\n\n" +
            "Depth : " + depth + "\n\n" +
            "Final Score = " + evaluate(board, depth);
  }

  @VisibleForTesting
  private static double score(final Player player,
                              final int depth,
                              final Board board) {

    return kingThreats(player, depth) +
            kingSafety(player, board) +
            mobility(player) +
            pawnCenter(board) +
            castle(player) +
            pieceEvaluations(player, board) +
            minorPieceDevelopment(player) +
            pawnStructure(player, board) +
            rookStructure(player, board) +
            calculateRepeatedMoveInOpeningPenalty(player);

  }


  public static double pawnCenter(final Board board) {
    if ((board.getPiece(35) instanceof Pawn
            && board.getPiece(35).getPieceAllegiance().isWhite())
            && (board.getPiece(36) instanceof Pawn
            && board.getPiece(36).getPieceAllegiance().isWhite())) {
      return PawnCenterBonus;
    }  else if((board.getPiece(27) instanceof Pawn
            && board.getPiece(27).getPieceAllegiance().isBlack())
            && (board.getPiece(28) instanceof Pawn
            && board.getPiece(28).getPieceAllegiance().isBlack())) {
      return PawnCenterBonus;
    }

    return 0;
  }

  /**
   * Calculate the score based on piece evaluations for the specified player also give the game stage.
   *
   * @param player The player to evaluate.
   * @return The piece evaluation score.
   */
  private static double pieceEvaluations(final Player player, final Board board) {
    double pieceValuationScore = 0;
    for (final Piece piece: player.getActivePieces()) {
      pieceValuationScore += (piece.getPieceValue() + piece.locationBonus(board));
    }
    return pieceValuationScore;
  }


  /**
   * Calculate the repeated move penalty for a player in the opening.
   *
   * @param player The player to evaluate.
   * @return The repeated move penalty score.
   */
  private static double calculateRepeatedMoveInOpeningPenalty(Player player) {
    double penalty = 0;
    Set<Piece> movedPieces = new HashSet<>();

    for(final Piece piece: player.getActivePieces()) {
      if(movedPieces.contains(piece)) {
        penalty += RepeatedMoveInOpeningPenalty;
      } else {
        movedPieces.add(piece);
      }
    }

    return penalty;
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
   * Calculate the score based on king threats for the specified player.
   *
   * @param player The player to evaluate.
   * @param depth The depth of the evaluation.
   * @return The king threat score.
   */
  private static double kingThreats(final Player player,
                                    final int depth) {
    return player.getOpponent().isInCheckMate() ? CheckMateBonus * depthBonus(depth) : check(player);
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
   * Calculate whether a bonus should be applied for an active piece.
   *
   * @param player The player to evaluate.
   * @return The minor piece development score.
   */
  private static double minorPieceDevelopment(final Player player) {
    double score = 0;
    for (final Piece piece : player.getActivePieces()) {
      if (piece.getPieceType().isMinorPiece()) {
        score += MinorDevelopmentBonus;
      }
    }
    return score;
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

  /**
   * Calculate the score based on pawn structure for the specified player.
   *
   * @param player The player to evaluate.
   * @return The pawn structure score.
   */
  private static double pawnStructure(final Player player, final Board board) {
    return PawnStructureAnalyzer.get().pawnStructureScore(player, board);
  }

  /**
   * Calculate the score based on king safety for the specified player.
   *
   * @param player The player to evaluate.
   * @return The king safety score. (usually negative)
   */
  private static double kingSafety(final Player player, final Board board) {
    final KingSafetyAnalyzer kingSafetyAnalyzer = KingSafetyAnalyzer.get();

    double score = 0;
    score += kingSafetyAnalyzer.evaluatePawnStorm(player);
    score += kingSafetyAnalyzer.evaluatePawnShelter(player, board);
    score += kingSafetyAnalyzer.OpponentRookOnFile(player);
    score += kingSafetyAnalyzer.evaluateKingSafetyFromSurroundingPieces(player);

    return score;
  }

  /**
   * Calculate the score based on rook structure for the specified player and board.
   *
   * @param player The player to evaluate.
   * @param board The chess board to evaluate.
   * @return The rook structure score.
   */
  private static double rookStructure(final Player player, final Board board) {

    RookStructureAnalyzer rookStructureAnalyzer = RookStructureAnalyzer.get();
    return rookStructureAnalyzer.rookStructureScore(player, board);

  }

}