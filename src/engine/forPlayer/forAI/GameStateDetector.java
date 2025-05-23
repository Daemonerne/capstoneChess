package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forPiece.King;
import engine.forPiece.Piece;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GameStateDetector provides precise identification of the current chess game phase
 * and selects the appropriate evaluation function based on comprehensive position analysis.
 * This class replaces the TaperedEvaluator with specialized evaluators for opening,
 * middlegame, and endgame positions, enhancing evaluation accuracy.
 *
 * @author Claude
 */
public class GameStateDetector {

  /** Singleton instance of the GameStateDetector. */
  private static final GameStateDetector INSTANCE = new GameStateDetector();

  /** Cache to avoid recalculating game state too frequently */
  private final Map<Long, GamePhase> gameStateCache = new HashMap<>();
  private static final int CACHE_MAX_SIZE = 10000;

  /** Phases of a chess game */
  public enum GamePhase {
    OPENING,
    MIDDLEGAME,
    ENDGAME
  }

  /** Private constructor to enforce singleton pattern */
  private GameStateDetector() {}

  /**
   * Returns the singleton instance of the GameStateDetector.
   *
   * @return The instance of GameStateDetector.
   */
  public static GameStateDetector get() {
    return INSTANCE;
  }

  /**
   * Determines the appropriate evaluator for the current board position
   * based on detailed analysis of the game state.
   *
   * @param board The current chess board.
   * @return The appropriate BoardEvaluator for the detected game phase.
   */
  public BoardEvaluator determineEvaluator(final Board board) {
    // Detect the game phase
    GamePhase gamePhase = detectGamePhase(board);

    // Return the specialized evaluator for the detected phase
    return switch (gamePhase) {
      case OPENING -> OpeningGameEvaluator.get();
      case MIDDLEGAME -> MiddlegameBoardEvaluator.get();
      case ENDGAME -> EndgameBoardEvaluator.get();
      default ->
        // Should never happen, but included for safety
              MiddlegameBoardEvaluator.get();
    };
  }

  /**
   * Determines the current phase of the game by analyzing the board position.
   * Uses caching to avoid redundant calculations.
   *
   * @param board The current chess board.
   * @return The detected game phase.
   */
  public GamePhase detectGamePhase(final Board board) {
    // Check cache first using Zobrist hash for speed
    long boardHash = board.getZobristHash();
    if (gameStateCache.containsKey(boardHash)) {
      return gameStateCache.get(boardHash);
    }

    // Calculate phase indicators
    int materialScore = calculateMaterialScore(board);
    int developmentScore = calculateDevelopmentScore(board);
    int moveCount = calculateMoveCount(board);
    int pieceCount = board.getAllPieces().size();
    int pawnStructureScore = evaluatePawnStructure(board);
    int kingActivityScore = evaluateKingActivity(board);

    // Detect game phase
    GamePhase phase = determinePhaseFromIndicators(
            materialScore, developmentScore, moveCount,
            pieceCount, pawnStructureScore, kingActivityScore, board);

    // Cache the result if cache isn't too large
    if (gameStateCache.size() < CACHE_MAX_SIZE) {
      gameStateCache.put(boardHash, phase);
    }

    return phase;
  }

  /**
   * Calculates a material-based score for phase detection.
   * Lower scores indicate progression toward endgame.
   *
   * @param board The current chess board.
   * @return A material score for phase detection.
   */
  private int calculateMaterialScore(final Board board) {
    int materialScore = 0;
    int queenCount = 0;
    int minorPieceCount = 0;
    int majorPieceCount = 0;

    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() != Piece.PieceType.KING &&
              piece.getPieceType() != Piece.PieceType.PAWN) {
        materialScore += piece.getPieceValue();

        switch (piece.getPieceType()) {
          case QUEEN:
            queenCount++;
            majorPieceCount++;
            break;
          case ROOK:
            majorPieceCount++;
            break;
          case BISHOP:
          case KNIGHT:
            minorPieceCount++;
            break;
        }
      }
    }

    return materialScore;
  }

  /**
   * Calculates a development score for phase detection.
   * Higher scores indicate more development (middlegame).
   *
   * @param board The current chess board.
   * @return A development score for phase detection.
   */
  private int calculateDevelopmentScore(final Board board) {
    int developmentScore = 0;
    int developedMinorPieces = 0;

    // Check minor piece development (knights and bishops)
    for (final Piece piece : board.getAllPieces()) {
      if ((piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) &&
              !piece.isFirstMove()) {

        // Check if piece is off its starting rank
        int rank = piece.getPiecePosition() / 8;
        boolean isOffStartingRank = (piece.getPieceAllegiance().isWhite() && rank != 7) ||
                (!piece.getPieceAllegiance().isWhite() && rank != 0);

        if (isOffStartingRank) {
          developedMinorPieces++;
          developmentScore += 10;
        }
      }
    }

    // Check castling status - important opening/middlegame indicator
    if (board.whitePlayer().isCastled()) {
      developmentScore += 20;
    }
    if (board.blackPlayer().isCastled()) {
      developmentScore += 20;
    }

    return developmentScore;
  }

  /**
   * Calculates the approximate number of moves that have been made in the game.
   *
   * @param board The current chess board.
   * @return An approximation of the number of moves made.
   */
  private int calculateMoveCount(final Board board) {
    // Get move history - limited to reasonable look-back
    List<Move> moveHistory = BoardUtils.lastNMoves(board, 100);

    // Count non-pawn pieces missing from starting position as additional move indicator
    int capturedPieces = 32 - board.getAllPieces().size();

    // Weight: each missing piece roughly represents 1-2 moves
    return moveHistory.size() + (capturedPieces / 2);
  }

  /**
   * Evaluates pawn structure to help determine game phase.
   *
   * @param board The current chess board.
   * @return A pawn structure score for phase detection.
   */
  private int evaluatePawnStructure(final Board board) {
    int score = 0;
    int totalPawns = 0;

    // Count pawns
    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() == Piece.PieceType.PAWN) {
        totalPawns++;

        // Check for advanced pawns (endgame indicator)
        int rank = piece.getPiecePosition() / 8;
        if ((piece.getPieceAllegiance().isWhite() && rank <= 2) ||
                (!piece.getPieceAllegiance().isWhite() && rank >= 5)) {
          score += 10; // Advanced pawns suggest later phases
        }
      }
    }

    // Fewer pawns suggest later game phases
    score += (16 - totalPawns) * 5;

    return score;
  }

  /**
   * Evaluates king activity to help determine game phase.
   * More active kings indicate endgame.
   *
   * @param board The current chess board.
   * @return A king activity score for phase detection.
   */
  private int evaluateKingActivity(final Board board) {
    int score = 0;

    King whiteKing = board.whitePlayer().getPlayerKing();
    King blackKing = board.blackPlayer().getPlayerKing();

    // Measure king centralization (higher in endgame)
    score += calculateKingCentralization(whiteKing.getPiecePosition());
    score += calculateKingCentralization(blackKing.getPiecePosition());

    // Kings off the back rank suggest later phases
    if (!isKingOnBackRank(whiteKing)) {
      score += 30;
    }

    if (!isKingOnBackRank(blackKing)) {
      score += 30;
    }

    return score;
  }

  /**
   * Calculates a king centralization score.
   * Higher values indicate more central kings (typical in endgame).
   *
   * @param kingPosition The position of the king.
   * @return A centralization score.
   */
  private int calculateKingCentralization(final int kingPosition) {
    final int file = kingPosition % 8;
    final int rank = kingPosition / 8;

    // Distance from edge (higher for more central positions)
    final int fileDistance = Math.min(file, 7 - file);
    final int rankDistance = Math.min(rank, 7 - rank);

    return (fileDistance + rankDistance) * 5;
  }

  /**
   * Checks if a king is on its original back rank.
   */
  private boolean isKingOnBackRank(final King king) {
    final int kingRank = king.getPiecePosition() / 8;
    return (king.getPieceAllegiance().isWhite() && kingRank == 7) ||
            (!king.getPieceAllegiance().isWhite() && kingRank == 0);
  }

  /**
   * Determines the game phase based on all calculated indicators.
   * Uses a sophisticated weighted approach to handle edge cases.
   *
   * @param materialScore Material-based score
   * @param developmentScore Development-based score
   * @param moveCount Approximate number of moves made
   * @param pieceCount Total pieces remaining
   * @param pawnStructureScore Pawn structure evaluation
   * @param kingActivityScore King activity evaluation
   * @param board The current chess board (for additional checks)
   * @return The determined game phase
   */
  private GamePhase determinePhaseFromIndicators(
          int materialScore, int developmentScore, int moveCount,
          int pieceCount, int pawnStructureScore, int kingActivityScore,
          Board board) {

    // Calculate weighted scores for each phase
    int openingScore = calculateOpeningScore(
            materialScore, developmentScore, moveCount, pieceCount, board);

    int middlegameScore = calculateMiddlegameScore(
            materialScore, developmentScore, moveCount, pieceCount, board);

    int endgameScore = calculateEndgameScore(
            materialScore, moveCount, pieceCount,
            pawnStructureScore, kingActivityScore, board);

    // Determine phase based on highest score
    if (endgameScore > middlegameScore && endgameScore > openingScore) {
      return GamePhase.ENDGAME;
    } else if (middlegameScore > openingScore) {
      return GamePhase.MIDDLEGAME;
    } else {
      return GamePhase.OPENING;
    }
  }

  /**
   * Calculates a score indicating how much the position resembles an opening.
   */
  private int calculateOpeningScore(
          int materialScore, int developmentScore, int moveCount,
          int pieceCount, Board board) {

    int score = 0;

    // High material count is opening indicator
    if (materialScore > 7000) {
      score += 50;
    } else if (materialScore > 6000) {
      score += 30;
    }

    // Low development is opening indicator
    if (developmentScore < 40) {
      score += 40;
    } else if (developmentScore < 80) {
      score += 20;
    }

    // Few moves made indicates opening
    if (moveCount < 10) {
      score += 50;
    } else if (moveCount < 20) {
      score += 30;
    }

    // Many pieces indicates opening
    if (pieceCount >= 30) {
      score += 40;
    } else if (pieceCount >= 26) {
      score += 20;
    }

    // Undeveloped minor pieces suggests opening
    int undevelopedMinorPieces = countUndevelopedMinorPieces(board);
    score += undevelopedMinorPieces * 5;

    // Kings on back rank suggests opening
    if (isKingOnBackRank(board.whitePlayer().getPlayerKing()) &&
            isKingOnBackRank(board.blackPlayer().getPlayerKing())) {
      score += 30;
    }

    return score;
  }

  /**
   * Counts undeveloped minor pieces (knights and bishops).
   */
  private int countUndevelopedMinorPieces(Board board) {
    int count = 0;

    for (final Piece piece : board.getAllPieces()) {
      if ((piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) &&
              piece.isFirstMove()) {
        count++;
      }
    }

    return count;
  }

  /**
   * Calculates a score indicating how much the position resembles a middlegame.
   */
  private int calculateMiddlegameScore(
          int materialScore, int developmentScore, int moveCount,
          int pieceCount, Board board) {

    int score = 0;

    // Moderate material count indicates middlegame
    if (materialScore >= 4000 && materialScore <= 7000) {
      score += 40;
    }

    // High development indicates middlegame
    if (developmentScore >= 80 && developmentScore <= 120) {
      score += 40;
    } else if (developmentScore > 40) {
      score += 20;
    }

    // Moderate move count indicates middlegame
    if (moveCount >= 15 && moveCount <= 40) {
      score += 40;
    } else if (moveCount > 10) {
      score += 20;
    }

    // Moderate piece count indicates middlegame
    if (pieceCount >= 20 && pieceCount < 30) {
      score += 40;
    } else if (pieceCount >= 16) {
      score += 20;
    }

    // Queens on board strongly suggests not endgame
    boolean whiteQueenPresent = false;
    boolean blackQueenPresent = false;

    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() == Piece.PieceType.QUEEN) {
        if (piece.getPieceAllegiance().isWhite()) {
          whiteQueenPresent = true;
        } else {
          blackQueenPresent = true;
        }
      }
    }

    if (whiteQueenPresent && blackQueenPresent) {
      score += 40; // Both queens present strongly indicates middlegame
    } else if (whiteQueenPresent || blackQueenPresent) {
      score += 20; // One queen present weakly indicates middlegame
    }

    // Castled kings with good pawn structure around them indicates middlegame
    if (board.whitePlayer().isCastled() || board.blackPlayer().isCastled()) {
      score += 20;
    }

    return score;
  }

  /**
   * Calculates a score indicating how much the position resembles an endgame.
   */
  private int calculateEndgameScore(
          int materialScore, int moveCount, int pieceCount,
          int pawnStructureScore, int kingActivityScore, Board board) {

    int score = 0;

    // Low material strongly indicates endgame
    if (materialScore < 3000) {
      score += 60;
    } else if (materialScore < 4000) {
      score += 40;
    }

    // Many moves made suggests endgame
    if (moveCount > 40) {
      score += 30;
    } else if (moveCount > 30) {
      score += 15;
    }

    // Few pieces strongly indicates endgame
    if (pieceCount < 16) {
      score += 60;
    } else if (pieceCount < 20) {
      score += 30;
    }

    // Add pawn structure and king activity indicators
    score += pawnStructureScore;
    score += kingActivityScore;

    // No queens strongly suggests endgame
    boolean queensPresent = false;
    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() == Piece.PieceType.QUEEN) {
        queensPresent = true;
        break;
      }
    }

    if (!queensPresent) {
      score += 50;
    }

    // Check for pawn promotion potential (endgame characteristic)
    if (hasPawnPromotionPotential(board)) {
      score += 30;
    }

    // Few minor and major pieces suggests endgame
    int nonPawnPieceCount = 0;
    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() != Piece.PieceType.PAWN &&
              piece.getPieceType() != Piece.PieceType.KING) {
        nonPawnPieceCount++;
      }
    }

    if (nonPawnPieceCount <= 6) {
      score += 40;
    } else if (nonPawnPieceCount <= 10) {
      score += 20;
    }

    return score;
  }

  /**
   * Checks if there are pawns with promotion potential.
   */
  private boolean hasPawnPromotionPotential(final Board board) {
    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() == Piece.PieceType.PAWN) {
        final int rank = piece.getPiecePosition() / 8;
        if ((piece.getPieceAllegiance().isWhite() && rank <= 2) ||
                (!piece.getPieceAllegiance().isWhite() && rank >= 5)) {
          return true;
        }
      }
    }
    return false;
  }
}