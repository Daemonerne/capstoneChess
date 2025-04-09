package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.King;
import engine.forPiece.Knight;
import engine.forPiece.Piece;
import engine.forPiece.Queen;
import engine.forPlayer.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OpeningGameEvaluator implements BoardEvaluator {

  /*** Singleton instance of the OpeningGameEvaluator. */
  private static final OpeningGameEvaluator Instance = new OpeningGameEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private OpeningGameEvaluator() {}

  /**
   * Returns the singleton instance of OpeningGameEvaluator.
   *
   * @return The instance of OpeningGameEvaluator.
   */
  public static OpeningGameEvaluator get() {
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
   * center control, early monarch moves, mobility, and king safety.
   * Each factor contributes to the final score, which reflects the advantage or disadvantage
   * of the board position for the specified player.
   * This scoring method is specifically constructed for the opening phase of play.
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

    return pieceEvaluations(playerPieces) +
            pieceDevelopment(playerPieces) +
            centerControl(playerPieces, playerPawns) +
            mobility(playerMoves) +
            earlyQueenMovePenalty(playerPieces) +
            kingMovePenalty(player) +
            knightDevelopmentBonus(playerPieces) +
            bishopDevelopmentBonus(playerPieces) +
            castlingPreparationBonus(player, board) +
            pawnStructure(playerPawns);
  }

  /**
   * Calculate the score based on piece evaluations for the specified player.
   *
   * @param playerPieces The collection of pieces for the player.
   * @return             The piece evaluation score.
   */
  private double pieceEvaluations(final Collection<Piece> playerPieces) {
    double pieceEvaluationScore = 0;
    for (final Piece piece : playerPieces) {
      pieceEvaluationScore += piece.getPieceValue();
    }
    return pieceEvaluationScore;
  }

  /**
   * Calculates the score based on the development of pieces for the specified player.
   * In the opening, it's important to get pieces off the back rank.
   *
   * @param playerPieces The collection of pieces for the player.
   * @return             The piece development score.
   */
  private double pieceDevelopment(final Collection<Piece> playerPieces) {
    double pieceDevelopmentScore = 0;
    for (final Piece piece : playerPieces) {
      if (!(piece instanceof King)) {
        // Give bonus for pieces moved off their starting rank
        final int pieceRank = piece.getPiecePosition() / 8;
        final boolean isWhitePiece = piece.getPieceAllegiance().isWhite();

        // Check if piece has moved from starting rank
        if ((isWhitePiece && pieceRank != 7) || (!isWhitePiece && pieceRank != 0)) {
          pieceDevelopmentScore += 15;
        }
      }
    }
    return pieceDevelopmentScore;
  }

  /**
   * Evaluates control of the center squares (d4, e4, d5, e5) and nearby squares.
   *
   * @param playerPieces The collection of pieces for the player.
   * @param playerPawns  The list of pawns for the player.
   * @return             The center control score.
   */
  private double centerControl(final Collection<Piece> playerPieces, final List<Piece> playerPawns) {
    double centerScore = 0;
    // Center squares: d4 (27), e4 (28), d5 (35), e5 (36)
    final int[] centerSquares = {27, 28, 35, 36};

    // Check for pieces or pawns in the center
    for (final Piece piece : playerPieces) {
      final int position = piece.getPiecePosition();
      for (final int centerSquare : centerSquares) {
        if (position == centerSquare) {
          centerScore += 20;
        }
      }
    }

    // Bonus for pawns in extended center (c3-f3-c6-f6)
    for (final Piece pawn : playerPawns) {
      final int position = pawn.getPiecePosition();
      if ((position >= 18 && position <= 21) || (position >= 26 && position <= 29) ||
              (position >= 34 && position <= 37) || (position >= 42 && position <= 45)) {
        centerScore += 10;
      }
    }

    return centerScore;
  }

  /**
   * Evaluates piece mobility, which is important in the opening for controlling space.
   *
   * @param playerMoves The collection of moves for the player.
   * @return            The mobility score.
   */
  private double mobility(final Collection<Move> playerMoves) {
    // In opening, mobility is important but not as critical as development
    return playerMoves.size() * 2;
  }

  /**
   * Applies a penalty for moving the queen too early in the game.
   *
   * @param playerPieces The collection of pieces for the player.
   * @return             The queen move penalty.
   */
  private double earlyQueenMovePenalty(final Collection<Piece> playerPieces) {
    for (final Piece piece : playerPieces) {
      if (piece instanceof Queen && !piece.isFirstMove()) {
        // Queen has moved - apply a penalty
        return -25;
      }
    }
    return 0;
  }

  /**
   * Applies a penalty for moving the king too early in the game (except for castling).
   *
   * @param player The player to evaluate.
   * @return       The king move penalty.
   */
  private double kingMovePenalty(final Player player) {
    final King playerKing = player.getPlayerKing();
    if (!playerKing.isFirstMove() && !playerKing.isCastled()) {
      // King has moved but not castled - apply a penalty
      return -30;
    }
    return 0;
  }

  /**
   * Gives a bonus for developing knights, which is generally good in the opening.
   *
   * @param playerPieces The collection of pieces for the player.
   * @return             The knight development bonus.
   */
  private double knightDevelopmentBonus(final Collection<Piece> playerPieces) {
    double knightScore = 0;
    for (final Piece piece : playerPieces) {
      if (piece instanceof Knight && !piece.isFirstMove()) {
        // Knight has moved off starting square
        knightScore += 15;

        // Additional bonus for knights in the center area
        final int position = piece.getPiecePosition();
        if ((position >= 18 && position <= 21) || (position >= 26 && position <= 29) ||
                (position >= 34 && position <= 37) || (position >= 42 && position <= 45)) {
          knightScore += 10;
        }
      }
    }
    return knightScore;
  }

  /**
   * Gives a bonus for developing bishops, which is good in the opening.
   *
   * @param playerPieces The collection of pieces for the player.
   * @return             The bishop development bonus.
   */
  private double bishopDevelopmentBonus(final Collection<Piece> playerPieces) {
    double bishopScore = 0;
    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP && !piece.isFirstMove()) {
        // Bishop has moved off starting square
        bishopScore += 15;

        // Additional bonus for bishops on long diagonals
        final int position = piece.getPiecePosition();
        if (position == 9 || position == 14 || position == 49 || position == 54) {
          bishopScore += 10;
        }
      }
    }
    return bishopScore;
  }

  /**
   * Gives a bonus for castling or preparing to castle.
   *
   * @param player The player to evaluate.
   * @param board  The current state of the chess board.
   * @return       The castling preparation bonus.
   */
  private double castlingPreparationBonus(final Player player, final Board board) {
    double castleScore = 0;

    // Big bonus for actually castling
    if (player.getPlayerKing().isCastled()) {
      return 50;
    }

    // Check for cleared castling path
    if (player.getPlayerKing().isKingSideCastleCapable()) {
      // Path is clear for kingside castling
      castleScore += 20;
    }

    if (player.getPlayerKing().isQueenSideCastleCapable()) {
      // Path is clear for queenside castling
      castleScore += 15; // Slightly less as queenside castling is slower
    }

    return castleScore;
  }

  /**
   * Evaluates the pawn structure.
   *
   * @param playerPawns The list of pawns for the player.
   * @return            The pawn structure score.
   */
  private double pawnStructure(final List<Piece> playerPawns) {
    double pawnScore = 0;

    // Bonus for center pawns
    for (final Piece pawn : playerPawns) {
      final int position = pawn.getPiecePosition();
      // Center and extended center pawns
      if ((position >= 26 && position <= 29) || (position >= 34 && position <= 37)) {
        pawnScore += 15;
      }
    }

    // Penalty for doubled pawns
    for (int file = 0; file < 8; file++) {
      int pawnsOnFile = 0;
      for (final Piece pawn : playerPawns) {
        if (pawn.getPiecePosition() % 8 == file) {
          pawnsOnFile++;
        }
      }
      if (pawnsOnFile > 1) {
        pawnScore -= 10 * (pawnsOnFile - 1);
      }
    }

    return pawnScore;
  }

  /**
   * Calculates a list of pawns for the specified player.
   *
   * @param player The player.
   * @return       A list of the player's pawns.
   */
  private static List<Piece> getPlayerPawns(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN)
            .collect(java.util.stream.Collectors.toList());
  }
}