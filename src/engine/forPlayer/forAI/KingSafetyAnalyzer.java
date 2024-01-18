package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPiece.Rook;
import engine.forPlayer.Player;

/**
 * The `KingSafetyAnalyzer` class is responsible for comprehensively analyzing the safety of the king during a chess game.
 * It incorporates various metrics, including calculating the king's tropism, evaluating the presence of pawns around the king for 
 * shelter, assessing the proximity of opponent pawns as a potential storm, penalizing the presence of an opposing rook on the same file,
 * determining control over key squares, and evaluating the safety of the king based on surrounding opponent pieces. 
 * These metrics provide a holistic evaluation of the player's king safety, contributing to a more nuanced understanding of the
 * positional aspects affecting the king's security. The class also includes helper methods for specific evaluations, ensuring a
 * comprehensive approach to king safety analysis. The `KingDistance` nested class represents the distance between the player's king
 * and the closest enemy piece, providing essential information for evaluating king tropism.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
public final class KingSafetyAnalyzer {

  /*** The single instance of the `KingSafetyAnalyzer` class. */
  private static final KingSafetyAnalyzer Instance = new KingSafetyAnalyzer();

  /*** Penalty applied when opposing rook is on the same file as the current player's king. */
  private static final int RookOnFilePenalty = 25;

  /*** Private constructor to prevent instantiation of `KingSafetyAnalyzer`. */
  private KingSafetyAnalyzer() {}

  /**
   * Gets the single instance of the `KingSafetyAnalyzer`.
   *
   * @return The `KingSafetyAnalyzer` instance.
   */
  public static KingSafetyAnalyzer get() {
    return Instance;
  }

  /**
   * Evaluates the potential threat to the player's king by assessing the presence of pawns in proximity.
   * The method assigns a score based on the number of pawns forming a shelter around the king.
   * A higher score suggests a more secure pawn shelter, contributing to improved king safety.
   *
   * @param player The player whose king's safety is being evaluated.
   * @return The pawn shelter score.
   */
  double evaluatePawnShelter(final Player player, final Board board) {
    int pawnShelterScore = 0;
    int kingSquare = player.getPlayerKing().getPiecePosition();

    for (int file = -1; file <= 1; file++) {
      int pawnSquare = kingSquare + file;

      if (BoardUtils.isValidTileCoordinate(pawnSquare)) {
        Piece pawn = board.getPiece(pawnSquare);

        if (pawn instanceof Pawn && pawn.getPieceAllegiance() == player.getAlliance()) {
          pawnShelterScore += 5;
        }
      }
    }

    return pawnShelterScore;
  }

  /**
   * Evaluates the potential risk of a pawn storm against the player's king. The method considers the distance
   * between the king and friendly pawns, assigning a penalty score if any pawn is within a certain proximity
   * to the king. This evaluation helps to account for vulnerabilities to pawn storms, a common attacking strategy.
   *
   * @param player The player whose king's safety is being evaluated.
   * @return The pawn storm score.
   */
  double evaluatePawnStorm(final Player player) {
    int pawnStormScore = 0;
    int kingPosition = player.getPlayerKing().getPiecePosition();

    for (Piece pawn : player.getOpponent().getActivePieces()) {
      if (pawn instanceof Pawn) {
        int distanceToKing = calculateChebyshevDistance(kingPosition, pawn.getPiecePosition());
        if (distanceToKing <= 3) {
          pawnStormScore -= 10;
        }
      }
    }
    return pawnStormScore;
  }

  /**
   * Evaluates the potential threat posed by an opposing rook on the same file as the player's king.
   * The method assigns a penalty score if an opponent's rook is positioned on the same file as the king,
   * indicating a potential threat to the king's safety.
   *
   * @param player The player whose king's safety is being evaluated.
   * @return The penalty score for an opposing rook on the same file as the king.
   */
  double OpponentRookOnFile(final Player player) {
    int score = 0;
    for (Piece piece : player.getOpponent().getActivePieces()) {
      if (piece instanceof Rook) {
        int pieceFile = piece.getPiecePosition() % BoardUtils.NUM_TILES_PER_ROW;
        int kingFile = player.getPlayerKing().getPiecePosition() % BoardUtils.NUM_TILES_PER_ROW;
        if (pieceFile == kingFile) {
          score -= RookOnFilePenalty;
        }
      }
    }
    return score;
  }

  /**
   * Evaluates the safety of the player's king from surrounding enemy pieces. The method calculates a threat
   * score based on the proximity and value of each opponent piece, providing an overall assessment of king safety.
   *
   * @param player The player whose king's safety is being evaluated.
   * @return The total threat score from surrounding enemy pieces.
   */
  double evaluateKingSafetyFromSurroundingPieces(final Player player) {
    int totalThreatScore = 0;
    for (Piece opponentPiece : player.getOpponent().getActivePieces()) {
      int threatScore = evaluatePieceThreatToKing(player, opponentPiece);
      totalThreatScore -= threatScore;
    }

    return totalThreatScore;
  }

  /**
   * Evaluates the threat posed by a specific opponent piece to the player's king. The method calculates
   * a threat score based on the proximity and value of the opponent piece, contributing to the overall
   * assessment of king safety.
   *
   * @param player         The player whose king's safety is being evaluated.
   * @param opponentPiece  The specific opponent piece under consideration.
   * @return The threat score from the specified opponent piece.
   */
  private int evaluatePieceThreatToKing(final Player player, final Piece opponentPiece) {
    int kingTile = player.getPlayerKing().getPiecePosition();
    int pieceTile = opponentPiece.getPiecePosition();
    int distance = calculateChebyshevDistance(kingTile, pieceTile);
    if (distance <= 3) {
      return opponentPiece.getPieceValue() / 90;
    } else {
      return 0;
    }
  }

  /**
   * Calculates the Chebyshev distance (maximum of rank and file distance) between two chess board tiles.
   *
   * @param kingTileId         The tile ID of the king.
   * @param enemyAttackTileId  The tile ID of the enemy piece.
   * @return The Chebyshev distance between the tiles.
   */
  private static int calculateChebyshevDistance(final int kingTileId, final int enemyAttackTileId) {
    final int rankDistance = Math.abs(getRank(enemyAttackTileId) - getRank(kingTileId));
    final int fileDistance = Math.abs(getFile(enemyAttackTileId) - getFile(kingTileId));
    return Math.max(rankDistance, fileDistance);
  }

  /**
   * Maps the file of a chess board coordinate.
   *
   * @param coordinate The chess board coordinate.
   * @return The file (column) of the coordinate.
   */
  private static int getFile(final int coordinate) {
    if (BoardUtils.Instance.FirstColumn.get(coordinate)) {
      return 1;
    } else if (BoardUtils.Instance.SecondColumn.get(coordinate)) {
      return 2;
    } else if (BoardUtils.Instance.ThirdColumn.get(coordinate)) {
      return 3;
    } else if (BoardUtils.Instance.FourthColumn.get(coordinate)) {
      return 4;
    } else if (BoardUtils.Instance.FifthColumn.get(coordinate)) {
      return 5;
    } else if (BoardUtils.Instance.SixthColumn.get(coordinate)) {
      return 6;
    } else if (BoardUtils.Instance.SeventhColumn.get(coordinate)) {
      return 7;
    } else if (BoardUtils.Instance.EighthColumn.get(coordinate)) {
      return 8;
    }
    throw new RuntimeException("Should not reach here!");
  }

  /**
   * Maps the rank of a chess board coordinate.
   *
   * @param coordinate The chess board coordinate.
   * @return The rank (row) of the coordinate.
   */
  private static int getRank(final int coordinate) {
    if (BoardUtils.Instance.FirstRow.get(coordinate)) {
      return 1;
    } else if (BoardUtils.Instance.SecondRow.get(coordinate)) {
      return 2;
    } else if (BoardUtils.Instance.ThirdRow.get(coordinate)) {
      return 3;
    } else if (BoardUtils.Instance.FourthRow.get(coordinate)) {
      return 4;
    } else if (BoardUtils.Instance.FifthRow.get(coordinate)) {
      return 5;
    } else if (BoardUtils.Instance.SixthRow.get(coordinate)) {
      return 6;
    } else if (BoardUtils.SeventhRow.get(coordinate)) {
      return 7;
    } else if (BoardUtils.Instance.EighthRow.get(coordinate)) {
      return 8;
    }
    throw new RuntimeException("Should not reach here!");
  }

}