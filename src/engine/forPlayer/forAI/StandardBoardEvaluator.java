package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forPiece.Pawn;
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

  /*** Singleton instance of the StandardBoardEvaluator. */
  private static final StandardBoardEvaluator Instance = new StandardBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private StandardBoardEvaluator() {}

  /**
   * Returns the singleton instance of StandardBoardEvaluator.
   *
   * @return The instance of StandardBoardEvaluator.
   */
  public static StandardBoardEvaluator get() { return Instance; }

  /**
   * Evaluates the given board from the perspective of a player and returns a score.
   *
   * @param board The current state of the chess board.
   * @param depth The search depth in the AI's thinking process.
   * @return      The evaluation score of the board.
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
   * @return      A detailed evaluation breakdown of the board.
   */
  public String evaluationDetails(final Board board, final int depth) {
    return  "White pieceEval : " + pieceEvaluations(board.whitePlayer(), board) + "\n" +
            "---------------------\n" +
            "Black pieceEval : " + pieceEvaluations(board.blackPlayer(), board) + "\n" +
            "---------------------\n" +
            "Depth : " + depth + "\n\n" +
            "Final Score = " + evaluate(board, depth);
  }

  @VisibleForTesting
  private static double score(final Player player,
                              final int depth,
                              final Board board) {
    return pieceEvaluations(player, board) +
           centerControl(player, board) +
           (double)(player.getLegalMoves().size()) * 0.3 +
           doubledPawns(player, board) +
           isolatedPawns(player, board) +
           openLines(player, board) +
           pawnShield(player, board);
  }

  /**
   * Calculate the score based on piece evaluations for the specified player also give the game stage.
   *
   * @param player The player to evaluate.
   * @return       The piece evaluation score.
   */
  private static double pieceEvaluations(final Player player, final Board board) {
    double pieceEvaluationScore = 0;
    for (final Piece piece: player.getActivePieces()) {
      pieceEvaluationScore += piece.getPieceValue();
    } return pieceEvaluationScore;
  }

  /**
   * Calculates the bonus for control of the center of the board.
   *
   * @param player The player whose center control bonus is to be calculated.
   * @param board  The current state of the chess board.
   * @return       The center control bonus for the player.
   */
  private static double centerControl(final Player player, final Board board) {
    double centerControlScore = 0;
    for (final Piece piece: player.getActivePieces()) {
      int piecePosition = piece.getPiecePosition();
      if (piecePosition == 27 || piecePosition == 28 || piecePosition == 35 || piecePosition == 36) {
        centerControlScore += 15;
      }
    } return centerControlScore;
  }

  /**
   * Counts the number of doubled pawns for the specified player across the entire board.
   *
   * @param player The player to evaluate.
   * @param board  The current state of the chess board.
   * @return       The penalty score for doubled pawns.
   */
  private static double doubledPawns(final Player player, final Board board) {
    double doubledPawnsPenalty = 0;
    for (final Piece piece : player.getActivePieces()) {
      if (piece instanceof Pawn) {
        int numPawnsOnFile = countPawnsOnFile(player, board, piece.getPiecePosition() % 8);
        if (numPawnsOnFile > 1) doubledPawnsPenalty -= 15;
      }
    } return doubledPawnsPenalty;
  }

  /**
   * Counts the number of pawns belonging to the player on a given file.
   *
   * @param player   The player to evaluate.
   * @param board    The current state of the chess board.
   * @param file     The file to count pawns on.
   * @return         The number of pawns belonging to the player on the specified file.
   */
  private static int countPawnsOnFile(final Player player, final Board board, final int file) {
    int numPawns = 0;
    for (final Piece piece : player.getActivePieces()) {
      if ((piece instanceof Pawn) && (piece.getPiecePosition() % 8) == file) {
        numPawns++;
      }
    } return numPawns;
  }

  /**
   * Counts the number of isolated pawns for the specified player across the entire board.
   *
   * @param player The player to evaluate.
   * @param board  The current state of the chess board.
   * @return       The penalty score for isolated pawns.
   */
  private static double isolatedPawns(final Player player, final Board board) {
    double isolatedPawnsPenalty = 0;
    for (final Piece piece : player.getActivePieces()) {
      if (piece instanceof Pawn) {
        if (!hasAdjacentPawns(player, board, piece.getPiecePosition() % 8)) {
          isolatedPawnsPenalty -= 10;
        }
      }
    } return isolatedPawnsPenalty;
  }

  /**
   * Checks if there are adjacent pawns belonging to the player on neighboring files.
   *
   * @param player   The player to evaluate.
   * @param board    The current state of the chess board.
   * @param file     The file of the pawn to check.
   * @return         True if there are adjacent pawns, false otherwise.
   */
  private static boolean hasAdjacentPawns(final Player player, final Board board, final int file) {
    boolean leftAdjacent = false;
    boolean rightAdjacent = false;
    if (file > 0) {
      leftAdjacent = countPawnsOnFile(player, board, file - 1) > 0;
    } if (file < 7) {
      rightAdjacent = countPawnsOnFile(player, board, file + 1) > 0;
    } return leftAdjacent || rightAdjacent;
  }

  /**
   * Evaluates open lines around the king.
   *
   * @param player        The player to evaluate.
   * @param board         The current state of the chess board.
   * @param kingPosition  The position of the king.
   * @return              The open lines score.
   */
  private static double openLines(final Player player, final Board board, final int kingPosition) {
    final int kingRank = kingPosition / 8;
    final int kingFile = kingPosition % 8;
    double openLinesScore = 0;
    boolean openFile = true;
    for (final Piece piece: board.getAllPieces()) {
      if (piece instanceof Pawn && piece.getPiecePosition() % 8 == kingFile) {
        openFile = false;
        break;
      } if (openFile) openLinesScore -= 10;
    } for (int offset = -7; offset <= 7; offset++) {
        if (offset != 0) {
          int diagonalSquare = kingPosition + offset;
          if (diagonalSquare >= 0 && diagonalSquare < 64) {
            boolean openDiagonal = true;
            int rankDiff = Math.abs(kingRank - (diagonalSquare / 8));
            int fileDiff = Math.abs(kingFile - (diagonalSquare % 8));
            if (rankDiff == fileDiff) {
              for (final Piece piece: board.getAllPieces()) {
                if (piece instanceof Pawn) {
                  openDiagonal = false;
                  break;
                }
              }
            } if (openDiagonal) openLinesScore -= 10;
          }
        }
      }
    } return openLinesScore;
  }

  /**
   * Evaluates the pawn shield in front of the king.
   *
   * @param player       The player to evaluate.
   * @param board        The current state of the chess board.
   * @param kingPosition The position of the king.
   * @return             The pawn shield score.
   */
  private static double pawnShield(final Player player, final Board board, final int kingPosition) {
    double pawnShieldBonus = 0;
    final int[] pawnShieldSquares = {
      kingPosition - 8, kingPosition - 9, kingPosition - 7};
    for (int square: pawnShieldSquares) {
      if (board.getPiece(square) instanceof Pawn &&
          board.getPiece(square).getPieceAllegiance() == player.getAlliance()) {
        pawnShieldBonus += 10;
      }
    } if (pawnShieldBonus >= 2) return 15;
    else if (pawnShieldBonus >= 1) return 5;
    return 0;
  }

  /**
   * Evaluates the activity of a piece.
   *
   * @param player The player to evaluate.
   * @param board  The current state of the chess board.
   * @param piece  The piece to evaluate.
   * @return       The piece activity score.
   */
  private static double pieceActivity(final Player player, final Board board, final Piece piece) {
    double pieceActivity = 0;
    for (final Piece otherPiece : player.getActivePieces()) {
      if (!otherPiece.equals(piece) && piece.calculateLegalMoves(board).contains(otherPiece.getPiecePosition())) {
        pieceActivity += 5;
      }
    }
  }
}