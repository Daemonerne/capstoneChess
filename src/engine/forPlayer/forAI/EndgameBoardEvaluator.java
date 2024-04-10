package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forPiece.*;
import engine.forPlayer.Player;

public class EndgameBoardEvaluator implements BoardEvaluator {

  /*** Singleton instance of the StandardBoardEvaluator. */
  private static final EndgameBoardEvaluator Instance = new EndgameBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private EndgameBoardEvaluator() {
  }

  /**
   * Returns the singleton instance of StandardBoardEvaluator.
   *
   * @return The instance of StandardBoardEvaluator.
   */
  public static EndgameBoardEvaluator get() {
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
    return (score(board.whitePlayer(), board) - score(board.blackPlayer(), board));
  }

  @VisibleForTesting
  private double score(final Player player,
                       final Board board) {
    return pieceEvaluations(player) +
            pieceDevelopment(player) +
            pieceChemistry(player, board) +
            doubledPawns(player) +
            kingCastled(player) +
            kingSafety(player, board) +
            pawnCenter(player);
  }

  /**
   * Calculate the score based on piece evaluations for the specified player also give the game stage.
   *
   * @param player The player to evaluate.
   * @return The piece evaluation score.
   */
  private double pieceEvaluations(final Player player) {
    double pieceEvaluationScore = 0;
    for (final Piece piece : player.getActivePieces()) {
      pieceEvaluationScore += piece.getPieceValue();
    }
    return pieceEvaluationScore;
  }

  /**
   * Calculates the score based on the development of pieces for the specified player.
   *
   * @param player The player to evaluate.
   * @return The piece development score.
   */
  private double pieceDevelopment(final Player player) {
    double pieceDevelopmentScore = 0;
    for (final Piece piece : player.getActivePieces()) {
      if (!(piece instanceof King) && !(piece instanceof Pawn) && !(piece instanceof Rook) && !(piece instanceof Queen)) {
        pieceDevelopmentScore += centerDevelopment(piece);
      }
    }
    return pieceDevelopmentScore;
  }

  /**
   * Evaluates the development of a specific piece.
   *
   * @param piece The piece to evaluate.
   * @return The piece development score.
   */
  private double centerDevelopment(final Piece piece) {
    final double centerRank = 3.5;
    final double centerFile = 3.5;
    final double chebyshevDistance = Math.max(Math.abs(centerRank - ((double) piece.getPiecePosition() / 8)),
            Math.abs(centerFile - (piece.getPiecePosition() % 8)));
    return (double) (15) / (chebyshevDistance + 0.5);
  }


  /**
   * Evaluates the chemistry of a piece with another.
   *
   * @param player The player to evaluate.
   * @param board  The current state of the chess board.
   * @return The piece chemistry score.
   */
  private double pieceChemistry(final Player player, final Board board) {
    double pieceActivity = 0;
    for (final Piece piece : player.getActivePieces()) {
      for (final Piece otherPiece : player.getActivePieces()) {
        if (!otherPiece.equals(piece) && piece.calculateLegalMoves(board).contains(otherPiece.getPiecePosition())) {
          pieceActivity += 5;
        }
      }
    }
    return pieceActivity;
  }

  /**
   * Counts the number of doubled pawns for the specified player across the entire board.
   *
   * @param player The player to evaluate.
   * @return The penalty score for doubled pawns.
   */
  private double doubledPawns(final Player player) {
    double doubledPawnsPenalty = 0;
    for (final Piece piece : player.getActivePieces()) {
      if (piece instanceof Pawn) {
        int numPawnsOnFile = countPawnsOnFile(player, piece.getPiecePosition() % 8);
        if (numPawnsOnFile > 1) doubledPawnsPenalty -= 10;
      }
    }
    return doubledPawnsPenalty;
  }

  /**
   * Calculates the score based on whether the players king has castled.
   *
   * @param player The player to determine whether their king is castled.
   * @return The king castle score.
   */
  private double kingCastled(final Player player) {
    if (player.getPlayerKing().isCastled()) return 20;
    return 0;
  }

  /**
   * Calculates the score based on the safety of the players king.
   *
   * @param player The player whose king safety is to be calculated.
   * @param board  The current state of the chess board.
   * @return The king safety score of the player.
   */
  private double kingSafety(final Player player, final Board board) {
    return openLines(board, player.getPlayerKing().getPiecePosition()) +
            pawnShield(player, board, player.getPlayerKing().getPiecePosition()) +
            kingThreat(player);
  }

  /**
   * Evaluates open lines around the king.
   *
   * @param board        The current state of the chess board.
   * @param kingPosition The position of the king.
   * @return The open lines score.
   */
  private double openLines(final Board board, final int kingPosition) {
    final int kingRank = kingPosition / 8;
    final int kingFile = kingPosition % 8;
    double openLinesScore = 0;
    for (final Piece piece : board.getAllPieces()) {
      if (piece instanceof Pawn && piece.getPiecePosition() % 8 == kingFile) {
        break;
      } openLinesScore += 10;
    }
    for (int offset = -7; offset <= 7; offset++) {
      if (offset != 0) {
        int diagonalSquare = kingPosition + offset;
        if (diagonalSquare >= 0 && diagonalSquare < 64) {
          boolean openDiagonal = true;
          int rankDiff = Math.abs(kingRank - (diagonalSquare / 8));
          int fileDiff = Math.abs(kingFile - (diagonalSquare % 8));
          if (rankDiff == fileDiff) {
            for (final Piece piece : board.getAllPieces()) {
              if (piece instanceof Pawn) {
                openDiagonal = false;
                break;
              }
            }
          }
          if (openDiagonal) openLinesScore -= 10;
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
  private double pawnShield(final Player player, final Board board, final int kingPosition) {
    double pawnShieldBonus = 0;
    final int[] pawnShieldSquares = { kingPosition - 8, kingPosition - 9, kingPosition - 7 };
    for (int square: pawnShieldSquares) {
      if (board.getPiece(square) instanceof Pawn &&
              board.getPiece(square).getPieceAllegiance() == player.getAlliance()) {
        pawnShieldBonus += 1;
      }
    } if (pawnShieldBonus >= 2) return 15;
    return 0;
  }


  /**
   * Evaluates the threat posed by a specific opponent piece to the player's king. The method calculates
   * a threat score based on the proximity and value of the opponent piece, contributing to the overall
   * assessment of king safety.
   *
   * @param player        The player whose king's safety is being evaluated.
   * @return              The threat score from the specified opponent piece.
   */
  private double kingThreat(final Player player) {
    double threatTotal = 0;
    for (final Piece opponentPiece: player.getOpponent().getActivePieces()) {
      int distance = calculateChebyshevDistance(player.getPlayerKing().getPiecePosition(),
              opponentPiece.getPiecePosition());
      if (distance <= 2) {
        return (double) opponentPiece.getPieceValue() / 30;
      } else if (distance == 3) {
        threatTotal += (double) opponentPiece.getPieceValue() / 45;
      } else if (distance == 4 && !(opponentPiece instanceof Queen)) {
        threatTotal += (double) opponentPiece.getPieceValue() / 90;
      }
    } return threatTotal;
  }

  /**
   * Calculates the Chebyshev distance (maximum of rank and file distance) between two chess board tiles.
   *
   * @param kingTileId        The tile ID of the king.
   * @param enemyAttackTileId The tile ID of the enemy piece.
   * @return                  The Chebyshev distance between the tiles.
   */
  private int calculateChebyshevDistance(final int kingTileId, final int enemyAttackTileId) {
    final int rankDistance = Math.abs(getRank(enemyAttackTileId) - getRank(kingTileId));
    final int fileDistance = Math.abs(getFile(enemyAttackTileId) - getFile(kingTileId));
    return Math.max(rankDistance, fileDistance);
  }

  /**
   * Gets the rank of the given coordinate.
   * @param coordinate The given coordinate.
   * @return           The rank of the coordinate.
   */
  private static int getRank(final int coordinate) {
    return (coordinate / 8) + 1;
  }

  /**
   * Gets the file of the given coordinate.
   * @param coordinate The given coordinate.
   * @return           The file of the coordinate.
   */
  private static int getFile(final int coordinate) {
    return (coordinate % 8) + 1;
  }



  /**
   * Calculates the score based on whether the player's pawns are in the center of the board.
   *
   * @param player The player to evaluate.
   * @return       The pawn center score.
   */
  private double pawnCenter(final Player player) {
    double centerPawnBonus = 0;
    for (final Piece piece: player.getActivePieces()) {
      if (piece instanceof Pawn) {
        final double centerRank = 3.5;
        final double centerFile = 3.5;
        final double chebyshevDistance = Math.max(Math.abs(centerRank - ((double) piece.getPiecePosition() / 8)),
                Math.abs(centerFile - (piece.getPiecePosition() % 8)));
        if (chebyshevDistance == 0.5) {
          centerPawnBonus += 1;
        } if (centerPawnBonus == 2) {
          return 25;
        }
      }
    } if (centerPawnBonus == 1) {
      return 15;
    } return 0;
  }

  /**
   * Counts the number of pawns belonging to the player on a given file.
   *
   * @param player The player to evaluate.
   * @param file   The file to count pawns on.
   * @return       The number of pawns belonging to the player on the specified file.
   */
  private int countPawnsOnFile(final Player player, final int file) {
    int numPawns = 0;
    for (final Piece piece : player.getActivePieces()) {
      if ((piece instanceof Pawn) && (piece.getPiecePosition() % 8) == file) {
        numPawns++;
      }
    } return numPawns;
  }
}