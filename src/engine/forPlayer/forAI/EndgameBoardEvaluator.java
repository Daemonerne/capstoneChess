package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EndgameBoardEvaluator implements BoardEvaluator {

  /*** Singleton instance of the EndgameBoardEvaluator. */
  private static final EndgameBoardEvaluator Instance = new EndgameBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private EndgameBoardEvaluator() {}

  /**
   * Returns the singleton instance of EndgameBoardEvaluator.
   *
   * @return The instance of EndgameBoardEvaluator.
   */
  public static EndgameBoardEvaluator get() {
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

  /**
   * Calculates the overall score of the current board position for a given player.
   * This method evaluates various factors such as piece evaluations, piece development,
   * early monarch moves, piece chemistry, doubled pawns, king castling, king safety,
   * and pawn center control. Each factor contributes to the final score, which reflects
   * the advantage or disadvantage of the board position for the specified player.
   * This scoring method is specifically constructed for the end-game phase of play.
   *
   * @param player The player for whom the board position is being evaluated.
   * @param board  The current state of the chess board.
   * @return       The evaluation score of the board from the perspective of the specified player.
   */
  @VisibleForTesting
  private double score(final Player player,
                       final Board board) {
    final Collection<Move> playerMoves = player.getLegalMoves();
    final Collection<Piece> playerPieces = player.getActivePieces();
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opposingPawns = getPlayerPawns(player.getOpponent());
    return pieceEvaluations(playerPieces) +
            pieceChemistry(playerMoves) +
            doubledPawns(playerPawns) +
            isolatedPawns(playerPawns) +
            backwardPawns(playerPawns) +
            pawnChains(playerPawns) +
            passedPawns(playerPawns,  opposingPawns) +
            kingSafety(player, board);
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
   * Evaluates the chemistry of a piece with another.
   *
   * @param playerMoves The collection of moves for the player.
   * @return            The piece chemistry score.
   */
  private double pieceChemistry(final Collection<Move> playerMoves) {
    double pieceChemistryScore = 0;
    final ArrayList<Integer> coordinates = new ArrayList<>();
    for (final Move move: playerMoves) {
      coordinates.add(move.getDestinationCoordinate());
    } for (final Integer coordinate: coordinates) {
      double frequency = Collections.frequency(coordinates, coordinate);
      if (frequency == 2) {
        pieceChemistryScore += 10;
      } else pieceChemistryScore += 8 * frequency;
    } return pieceChemistryScore;
  }

  /**
   * Counts the number of doubled pawns for the specified player across the entire board.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The penalty score for doubled pawns.
   */
  private double doubledPawns(final List<Piece> playerPawns) {
    double doubledPawnScore = 0;
    for (final Piece pawn: playerPawns) {
      int numPawnsOnFile = countPawnsOnFile(pawn.getPiecePosition() % 8, playerPawns);
      if (numPawnsOnFile > 1) doubledPawnScore -= 20;
    } return doubledPawnScore;
  }

  /**
   * Evaluates the presence of isolated pawns for the specified player.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The isolated pawn score.
   */
  private double isolatedPawns(final List<Piece> playerPawns) {
    double isolatedPawnScore = 0;
    for (final Piece pawn : playerPawns) {
      int pawnFile = pawn.getPiecePosition() % 8;
      if (!hasAdjacentPawns(pawnFile, playerPawns)) {
        isolatedPawnScore -= 15;
      }
    }return isolatedPawnScore;
  }

  /**
   * Evaluates the presence of backward pawns for the specified player.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The backward pawn score.
   */
  private double backwardPawns(final List <Piece> playerPawns) {
    double backwardPawnScore = 0;
    for (final Piece pawn: playerPawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      final int pawnRank = pawn.getPiecePosition() / 8;
      boolean isBackward = true;
      for (int rank = pawnRank - 1; rank >= 0; rank--) {
        if (hasPawnOnFile(rank * 8 + pawnFile, playerPawns)) {
          isBackward = false;
          break;
        } if (pawnFile > 0 && hasPawnOnFile(rank * 8 + pawnFile - 1, playerPawns)) {
          isBackward = false;
          break;
        } if (pawnFile < 7 && hasPawnOnFile(rank * 8 + pawnFile + 1, playerPawns)) {
          isBackward = false;
          break;
        }
      } if (isBackward) {
        backwardPawnScore -= 15;
      }
    } return backwardPawnScore;
  }

  /**
   * Evaluates the presence of pawn chains for the specified player.
   *
   * @param playerPawns The collection of pawns for the player.
   * @return            The pawn chain score.
   */
  private double pawnChains(final List <Piece> playerPawns) {
    double pawnChainScore = 0;
    for (final Piece pawn: playerPawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      final int pawnRank = pawn.getPiecePosition() / 8;
      if ((pawnFile > 0 && hasPawnOnFile(pawnRank * 8 + pawnFile - 1, playerPawns)) ||
        (pawnFile < 7 && hasPawnOnFile(pawnRank * 8 + pawnFile + 1, playerPawns))) {
        pawnChainScore += 10;
      }
    } return pawnChainScore;
  }

  /**
   * Evaluates the presence of passed pawns for the specified player.
   * 
   * @param playerPawns The collection of pawns for the player.
   * @return            The passed pawn score.
   */
  private double passedPawns(final List <Piece> playerPawns, final List <Piece> opponentPawns) {
    double passedPawnScore = 0;
    for (final Piece pawn: playerPawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      final int pawnRank = pawn.getPiecePosition() / 8;
      boolean isPassedPawn = true;
      for (int rank = pawnRank + 1; rank < 8; rank++) {
        if (hasPawnOnFile(rank * 8 + pawnFile, opponentPawns)) {
          isPassedPawn = false;
          break;
        } if (pawnFile > 0 && hasPawnOnFile(rank * 8 + pawnFile - 1, opponentPawns)) {
          isPassedPawn = false;
          break;
        } if (pawnFile < 7 && hasPawnOnFile(rank * 8 + pawnFile + 1, opponentPawns)) {
          isPassedPawn = false;
          break;
        }
      } if (isPassedPawn) {
        passedPawnScore += 100;
      }
    } return passedPawnScore;
  }

  /**
   * Calculates the score based on the safety of the players king.
   *
   * @param player The player whose king safety is to be calculated.
   * @param board  The current state of the chess board.
   * @return       The king safety score of the player.
   */
  private double kingSafety(final Player player, final Board board) {
    return 0; //TODO to be implemented
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
   * Checks if there is a pawn at the given position in the player's pawns collection.
   *
   * @param position     The position to check.
   * @param playerPawns  The collection of pawns for the player.
   * @return             True if there is a pawn at the given position, false otherwise.
   */
  private boolean hasPawnOnFile(int position, List <Piece> playerPawns) {
    return playerPawns.stream().anyMatch(pawn -> pawn.getPiecePosition() == position);
  }

  /**
   * Checks if there are adjacent pawns to a given pawn on the same file.
   *
   * @param file        The file of the pawn.
   * @param playerPawns The collection of pawns for the player.
   * @return            True if there are adjacent pawns, false otherwise.
   */
  private boolean hasAdjacentPawns(final int file, final List < Piece > playerPawns) {
    for (final Piece pawn: playerPawns) {
      if (Math.abs(pawn.getPiecePosition() % 8 - file) == 1) {
        return true;
      }
    } return false;
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