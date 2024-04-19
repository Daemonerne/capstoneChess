package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.Bishop;
import engine.forPiece.King;
import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MiddlegameBoardEvaluator implements BoardEvaluator {

  /*** Singleton instance of the StandardBoardEvaluator. */
  private static final MiddlegameBoardEvaluator Instance = new MiddlegameBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private MiddlegameBoardEvaluator() {}

  /**
   * Returns the singleton instance of StandardBoardEvaluator.
   *
   * @return The instance of StandardBoardEvaluator.
   */
  public static MiddlegameBoardEvaluator get() {
    return Instance;
  }

  /**
   * Evaluates the given board from the perspective of a player and returns a score.
   *
   * @param board The current state of the chess board.
   * @param depth The search depth in the AI's thinking process.
   * @return      The evaluation score of the board.
   */
  @Override
  public double evaluate(final Board board, final int depth) {
    return (score(board.whitePlayer(), board) - score(board.blackPlayer(), board));
  }

  @VisibleForTesting
  private double score(final Player player,
                       final Board board) {
    final Collection<Move> playerMoves = player.getLegalMoves();
    final Collection<Piece> playerPieces = player.getActivePieces();
    final List<Piece> playerPawns = getPlayerPawns(player);
    return pieceEvaluations(playerPieces) +
            pieceDevelopment(playerPieces) +
            pieceChemistry(playerMoves) +
            diagonalControl(playerPieces, board) +
            attackPotential(playerMoves) +
            doubledPawns(playerPawns) +
            kingCastled(player) +
            kingSafety(player, board) +
            pawnCenter(playerPawns);
  }

  /**
   * Calculate the score based on piece evaluations for the specified player also give the game stage.
   *
   * @return The piece evaluation score.
   */
  private double pieceEvaluations(final Collection<Piece> playerPieces) {
    double pieceEvaluationScore = 0;
    for (final Piece piece : playerPieces) {
      pieceEvaluationScore += piece.getPieceValue();
    } return pieceEvaluationScore;
  }

  /**
   * Calculates the score based on the development of pieces for the specified player.
   *
   * @param playerPieces the collection of player pieces.
   * @return             The piece development score.
   */
  private double pieceDevelopment(final Collection<Piece> playerPieces) {
    double pieceDevelopmentScore = 0;
    for (final Piece piece : playerPieces) {
      if (!(piece instanceof King) && !(piece instanceof Pawn)) {
        pieceDevelopmentScore += centerDevelopment(piece);
      }
    } return pieceDevelopmentScore;
  }

  /**
   * Evaluates the development of a specific piece.
   *
   * @param piece The piece to evaluate.
   * @return      The piece development score.
   */
  private double centerDevelopment(final Piece piece) {
    final double centerRank = 3.5;
    final double centerFile = 3.5;
    final double chebyshevDistance = Math.max(Math.abs(centerRank - ((double) piece.getPiecePosition() / 8)),
            Math.abs(centerFile - (piece.getPiecePosition() % 8)));
    return (double)(12) / (chebyshevDistance + 0.5);
  }


  /**
   * Evaluates the chemistry of a piece with another.
   *
   * @param playerMoves The collection of moves for the player.
   * @return            The piece chemistry score.
   */
  private double pieceChemistry(final Collection<Move> playerMoves) {
    double pieceActivity = 0;
    final ArrayList<Integer> coordinates = new ArrayList<>();
    for (final Move move: playerMoves) {
      coordinates.add(move.getDestinationCoordinate());
    } for (final Integer coordinate: coordinates) {
      double frequency = Collections.frequency(coordinates, coordinate);
      if (frequency == 2) {
        pieceActivity += 5;
      } else pieceActivity += 8 * frequency;
    } return pieceActivity;
  }

  /**
   * Evaluates bishop control of long diagonals.
   *
   * @param playerPieces The collection of pieces for the player.
   * @param board        The current state of the chess board.
   * @return             The diagonal control score.
   */
  private double diagonalControl(final Collection<Piece> playerPieces, final Board board) {
    double controlScore = 0;
    for (final Piece piece: playerPieces) {
      if (piece instanceof Bishop) controlScore += (1.2 * piece.calculateLegalMoves(board).size());
    } return controlScore;
  }

  /**
   * Evaluates the attack potential of a player.
   *
   * @param playerMoves The collection of moves for the player.
   * @return            The attack potential score.
   */
  private double attackPotential(final Collection<Move> playerMoves) {
    double attackScore = 0;
    for (final Move move: playerMoves) {
      if (move.isAttack()) {
        attackScore++;
      }
    } if (attackScore < 5) return attackScore;
    else if (attackScore < 12) return 20;
    else if (attackScore < 15) return 30;
    return 40;
  }

  /**
   * Counts the number of doubled pawns for the specified player across the entire board.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The penalty score for doubled pawns.
   */
  private double doubledPawns(final List<Piece> playerPawns) {
    double doubledPawnsPenalty = 0;
    for (final Piece pawn: playerPawns) {
      int numPawnsOnFile = countPawnsOnFile(pawn.getPiecePosition() % 8, playerPawns);
      if (numPawnsOnFile > 1) doubledPawnsPenalty -= 20;
    } return doubledPawnsPenalty;
  }

  /**
   * Calculates the score based on whether the players king has castled.
   *
   * @param player The player to determine whether their king is castled.
   * @return       The king castle score.
   */
  private double kingCastled(final Player player) {
    if (player.getPlayerKing().isCastled()) return 15;
    return 0;
  }

  /**
   * Calculates the score based on the safety of the players king.
   *
   * @param player The player whose king safety is to be calculated.
   * @param board  The current state of the chess board.
   * @return       The king safety score of the player.
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
   * @return             The open lines score.
   */
  private double openLines(final Board board, final int kingPosition) {
    final int kingRank = kingPosition / 8;
    final int kingFile = kingPosition % 8;
    double openLinesScore = 0;
    for (final Piece piece : board.getAllPieces()) {
      if (piece instanceof Pawn && piece.getPiecePosition() % 8 == kingFile) {
        break;
      } openLinesScore -= 15;
    } for (int offset = -7; offset <= 7; offset++) {
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
          } if (openDiagonal) openLinesScore -= 15;
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
    } if (pawnShieldBonus >= 2) return 5;
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
      if (distance == 1) {
        threatTotal -= (double) opponentPiece.getPieceValue() / 27;
      } else if (distance <= 2) {
        threatTotal -= (double) opponentPiece.getPieceValue() / 32;
      } else if (distance == 3) {
        threatTotal -= (double) opponentPiece.getPieceValue() / 37;
      } else if (distance == 4) {
        threatTotal -= (double) opponentPiece.getPieceValue() / 100;
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
   *
   * @param coordinate The given coordinate.
   * @return           The rank of the coordinate.
   */
  private static int getRank(final int coordinate) {
    return (coordinate / 8) + 1;
  }

  /**
   * Gets the file of the given coordinate.
   *
   * @param coordinate The given coordinate.
   * @return           The file of the coordinate.
   */
  private static int getFile(final int coordinate) {
    return (coordinate % 8) + 1;
  }



  /**
   * Calculates the score based on whether the player's pawns are in the center of the board.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The pawn center score.
   */
  private double pawnCenter(final List<Piece> playerPawns) {
    double centerPawnBonus = 0;
    for (final Piece pawn: playerPawns) {
      final double centerRank = 3.5;
      final double centerFile = 3.5;
      final double chebyshevDistance = Math.max(Math.abs(centerRank - ((double) pawn.getPiecePosition() / 8)),
                                                Math.abs(centerFile - (pawn.getPiecePosition() % 8)));
      if (chebyshevDistance == 0.5) {
        centerPawnBonus += 1;
      } if (centerPawnBonus == 2) {
        return 20;
      }
    } if (centerPawnBonus == 1) {
      return 10;
    } return 0;
  }

  /**
   * Counts the number of pawns belonging to the player on a given file.
   *
   * @param file        The file to count pawns on.
   * @param playerPawns The collection of pawns for the player.
   * @return            The number of pawns belonging to the player on the specified file.
   */
  private int countPawnsOnFile(final int file, final List<Piece> playerPawns) {
    int numPawns = 0;
    for (final Piece pawn: playerPawns) {
      if (pawn.getPiecePosition() % 8 == file) {
        numPawns++;
      }
    } return numPawns;
  }

  /**
   * Calculates a list of opponent pawns for the specified player.
   *
   * @param player The opponent player.
   * @return       A list of opponent pawns.
   */
  private static List<Piece> getPlayerPawns(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN).toList();
  }
}