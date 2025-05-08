package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.*;
import engine.forPlayer.Player;

import java.util.*;
import java.util.stream.Collectors;

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
   * Calculates the overall score of the current board position for a given player
   * using modern chess engine evaluation principles tuned for endgame positions.
   *
   * @param player The player for whom the board position is being evaluated.
   * @param board  The current state of the chess board.
   * @return       The evaluation score of the board from the perspective of the specified player.
   */
  @VisibleForTesting
  private double score(final Player player, final Board board) {
    return materialEvaluation(player, board) +
            kingActivityEvaluation(player, board) +
            passedPawnEvaluation(player, board) +
            pawnStructureEvaluation(player, board) +
            pieceCoordinationEvaluation(player, board) +
            rookEndgameEvaluation(player, board) +
            bishopEndgameEvaluation(player, board) +
            drawPatternEvaluation(player, board) +
            mobilityEvaluation(player, board);
  }

  /**
   * Evaluates material balance with specific endgame piece values and
   * recognizes special endgame material combinations.
   */
  private double materialEvaluation(final Player player, final Board board) {
    double materialScore = 0;
    final Collection<Piece> playerPieces = player.getActivePieces();
    final Collection<Piece> opponentPieces = player.getOpponent().getActivePieces();
    final boolean isEndgame = isDeepEndgame(board);

    // Count pieces by type for material identification
    Map<Piece.PieceType, Integer> playerPieceCounts = countPieceTypes(playerPieces);
    Map<Piece.PieceType, Integer> opponentPieceCounts = countPieceTypes(opponentPieces);

    // Standard material counting with endgame-specific values
    for (final Piece piece : playerPieces) {
      // Apply endgame-specific piece values
      switch (piece.getPieceType()) {
        case PAWN:
          materialScore += 100; // Pawns gain value in endgames (promotion potential)
          break;
        case KNIGHT:
          // Knights lose value in open positions
          materialScore += isEndgame ? 290 : 310;
          break;
        case BISHOP:
          // Bishops maintain or gain value in open positions
          materialScore += isEndgame ? 330 : 320;
          break;
        case ROOK:
          // Rooks gain value in endgames
          materialScore += isEndgame ? 530 : 500;
          break;
        case QUEEN:
          materialScore += 900;
          break;
        case KING:
          materialScore += 10000; // Essentially infinite value
          break;
      }
    }

    // Bishop pair bonus (stronger in endgames with pawns on both sides)
    if (playerPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0) >= 2) {
      materialScore += 50;
    }

    // Special material combination recognitions

    // Recognize insufficient material combinations
    if (isInsufficientMaterial(playerPieceCounts, opponentPieceCounts)) {
      materialScore -= 800; // Significant drawish penalty
    }

    // Recognize favorable/unfavorable endgame material
    if (evaluateSpecialMaterialCombinations(playerPieceCounts, opponentPieceCounts, player.getAlliance())) {
      materialScore += 100; // Bonus for favorable combinations
    }

    // Recognize opposite-colored bishops (drawish)
    if (hasOppositeColoredBishops(playerPieces, opponentPieces)) {
      materialScore -= 50; // Drawish tendency
    }

    return materialScore;
  }

  /**
   * Counts the number of each piece type.
   */
  private Map<Piece.PieceType, Integer> countPieceTypes(final Collection<Piece> pieces) {
    Map<Piece.PieceType, Integer> pieceCounts = new HashMap<>();

    for (final Piece piece : pieces) {
      pieceCounts.put(piece.getPieceType(),
              pieceCounts.getOrDefault(piece.getPieceType(), 0) + 1);
    }

    return pieceCounts;
  }

  /**
   * Checks if the position is in a deep endgame (few pieces, typically with pawns).
   */
  private boolean isDeepEndgame(final Board board) {
    final Collection<Piece> allPieces = board.getAllPieces();
    int nonPawnPieceCount = 0;

    for (final Piece piece : allPieces) {
      if (piece.getPieceType() != Piece.PieceType.PAWN &&
              piece.getPieceType() != Piece.PieceType.KING) {
        nonPawnPieceCount++;
      }
    }

    return nonPawnPieceCount <= 4;
  }

  /**
   * Checks for insufficient material to force checkmate.
   */
  private boolean isInsufficientMaterial(final Map<Piece.PieceType, Integer> playerPieceCounts,
                                         final Map<Piece.PieceType, Integer> opponentPieceCounts) {
    // No pawns for either side
    boolean noPawns = playerPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) == 0 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) == 0;

    if (noPawns) {
      // King vs King
      if (playerPieceCounts.size() == 1 && opponentPieceCounts.size() == 1) {
        return true;
      }

      // King + minor piece vs King
      if ((playerPieceCounts.size() == 2 && opponentPieceCounts.size() == 1) ||
              (playerPieceCounts.size() == 1 && opponentPieceCounts.size() == 2)) {
        int minorsCount = playerPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) +
                playerPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0) +
                opponentPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) +
                opponentPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0);

        return minorsCount <= 1;
      }

      // King + 2 Knights vs King (insufficient material)
      return (playerPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) == 2 &&
              playerPieceCounts.size() == 2 && opponentPieceCounts.size() == 1) ||
              (opponentPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) == 2 &&
                      opponentPieceCounts.size() == 2 && playerPieceCounts.size() == 1);
    }

    return false;
  }

  /**
   * Evaluates special material combinations that could be advantageous.
   */
  private boolean evaluateSpecialMaterialCombinations(final Map<Piece.PieceType, Integer> playerPieceCounts,
                                                      final Map<Piece.PieceType, Integer> opponentPieceCounts,
                                                      final Alliance playerAlliance) {
    // Queen vs Rook (favorable)
    if (playerPieceCounts.getOrDefault(Piece.PieceType.QUEEN, 0) > 0 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.ROOK, 0) > 0 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.QUEEN, 0) == 0) {
      return true;
    }

    // Rook vs Minor Piece (favorable)
    if (playerPieceCounts.getOrDefault(Piece.PieceType.ROOK, 0) > 0 &&
            (opponentPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0) > 0 ||
                    opponentPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) > 0) &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.ROOK, 0) == 0 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.QUEEN, 0) == 0) {
      return true;
    }

    // Bishop pair vs Knight (favorable)
    return playerPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0) >= 2 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.KNIGHT, 0) > 0 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.BISHOP, 0) == 0;
  }

  /**
   * Checks if the position has opposite-colored bishops.
   */
  private boolean hasOppositeColoredBishops(final Collection<Piece> playerPieces,
                                            final Collection<Piece> opponentPieces) {
    Bishop playerBishop = null;
    Bishop opponentBishop = null;

    // Find bishops
    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        playerBishop = (Bishop) piece;
        break;
      }
    }

    for (final Piece piece : opponentPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        opponentBishop = (Bishop) piece;
        break;
      }
    }

    // Check if we have one bishop each and they're on opposite colored squares
    if (playerBishop != null && opponentBishop != null) {
      final int playerSquareColor = (playerBishop.getPiecePosition() / 8 + playerBishop.getPiecePosition() % 8) % 2;
      final int opponentSquareColor = (opponentBishop.getPiecePosition() / 8 + opponentBishop.getPiecePosition() % 8) % 2;

      return playerSquareColor != opponentSquareColor;
    }

    return false;
  }

  /**
   * Evaluates king activity, a critical factor in endgames.
   * In the endgame, king centralization and activity is extremely important.
   */
  private double kingActivityEvaluation(final Player player, final Board board) {
    double kingActivityScore = 0;
    final King playerKing = player.getPlayerKing();
    final King opponentKing = player.getOpponent().getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();

    // King centralization (critical in endgames)
    kingActivityScore += evaluateKingCentralization(kingPosition);

    // Distance to opponent king (in some endgames, we want to be close)
    kingActivityScore += evaluateKingProximity(kingPosition, opponentKing.getPiecePosition());

    // King attacking/defending pawns
    kingActivityScore += evaluateKingPawnDefense(player, board);

    // King safety (less important in many endgames)
    kingActivityScore -= evaluateKingExposure(player, board) * 0.5; // Apply reduced penalty

    return kingActivityScore;
  }

  /**
   * Evaluates king centralization - kings want to be as central as possible in endgames.
   */
  private double evaluateKingCentralization(final int kingPosition) {
    final int kingFile = kingPosition % 8;
    final int kingRank = kingPosition / 8;

    // Distance from center (3.5, 3.5)
    final double fileDistance = Math.abs(kingFile - 3.5);
    final double rankDistance = Math.abs(kingRank - 3.5);

    // Manhattan distance from center
    final double distanceFromCenter = fileDistance + rankDistance;

    // More central kings get higher scores
    return (7 - distanceFromCenter) * 10;
  }

  /**
   * Evaluates king proximity to opponent king - important in king and pawn endgames.
   */
  private double evaluateKingProximity(final int kingPosition, final int opponentKingPosition) {
    final int kingFile = kingPosition % 8;
    final int kingRank = kingPosition / 8;
    final int opponentKingFile = opponentKingPosition % 8;
    final int opponentKingRank = opponentKingPosition / 8;

    // Chebyshev distance between kings
    final int kingDistance = Math.max(
            Math.abs(kingFile - opponentKingFile),
            Math.abs(kingRank - opponentKingRank)
    );

    // In pure king and pawn endgames, opposition is important
    if (kingDistance == 2 && ((kingFile == opponentKingFile) || (kingRank == opponentKingRank))) {
      return 20; // Bonus for having opposition
    }

    // For other endgames, advantage depends on material
    return 0; // Neutral by default
  }

  /**
   * Evaluates how well the king defends friendly pawns and attacks enemy pawns.
   */
  private double evaluateKingPawnDefense(final Player player, final Board board) {
    double kingPawnDefenseScore = 0;
    final King playerKing = player.getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());

    // Check how many friendly pawns the king defends
    for (final Piece pawn : playerPawns) {
      final int distance = calculateChebyshevDistance(kingPosition, pawn.getPiecePosition());

      if (distance <= 1) {
        kingPawnDefenseScore += 10; // Direct defense
      } else if (distance == 2) {
        kingPawnDefenseScore += 5;  // Nearby defense
      }
    }

    // Check if king is attacking enemy pawns
    for (final Piece pawn : opponentPawns) {
      final int distance = calculateChebyshevDistance(kingPosition, pawn.getPiecePosition());

      if (distance <= 1) {
        kingPawnDefenseScore += 8; // Direct attack
      } else if (distance == 2) {
        kingPawnDefenseScore += 4; // Nearby attack
      }
    }

    return kingPawnDefenseScore;
  }

  /**
   * Evaluates king exposure to checks and attacks - less critical in endgames
   * but still relevant, especially with queens on the board.
   */
  private double evaluateKingExposure(final Player player, final Board board) {
    double exposureScore = 0;
    final Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();
    final King playerKing = player.getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();

    // Count checks and threats near king
    for (final Move move : opponentMoves) {
      final int destination = move.getDestinationCoordinate();

      // Direct check
      if (destination == kingPosition) {
        exposureScore += 30;
      }

      // Threat near king
      if (calculateChebyshevDistance(kingPosition, destination) <= 1) {
        exposureScore += 5;
      }
    }

    // If player is in check
    if (player.isInCheck()) {
      exposureScore += 20;
    }

    return exposureScore;
  }

  /**
   * Evaluates passed pawns, which are especially important in endgames.
   * This is a critical evaluation function for endgames.
   */
  private double passedPawnEvaluation(final Player player, final Board board) {
    double passedPawnScore = 0;
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());
    final Alliance alliance = player.getAlliance();
    final King playerKing = player.getPlayerKing();
    final King opponentKing = player.getOpponent().getPlayerKing();

    // Identify and evaluate passed pawns
    for (final Piece pawn : playerPawns) {
      if (isPassedPawn(pawn, opponentPawns, alliance)) {
        final int pawnPosition = pawn.getPiecePosition();
        final int pawnRank = pawnPosition / 8;

        // Calculate rank from pawn's perspective (distance to promotion)
        final int rankFromPromotion = alliance.isWhite() ? pawnRank : (7 - pawnRank);

        // Base score dependent on advancement
        final int advancementScore = (7 - rankFromPromotion) * 20;
        passedPawnScore += advancementScore;

        // Additional bonuses for advanced passed pawns
        if (rankFromPromotion <= 2) {
          // Very advanced pawn
          passedPawnScore += 50;
        } else if (rankFromPromotion <= 4) {
          // Moderately advanced pawn
          passedPawnScore += 30;
        }

        // King support for passed pawn
        final int kingDistance = calculateChebyshevDistance(playerKing.getPiecePosition(), pawnPosition);
        passedPawnScore += (8 - kingDistance) * 5;

        // Opponent king distance from passed pawn
        final int opponentKingDistance = calculateChebyshevDistance(opponentKing.getPiecePosition(), pawnPosition);
        passedPawnScore += opponentKingDistance * 3;

        // Path to promotion clear?
        if (isPathToPromotionClear(pawn, alliance, board)) {
          passedPawnScore += 40;
        }

        // Protected passed pawn bonus
        if (isPawnProtected(pawn, playerPawns, alliance)) {
          passedPawnScore += 25;
        }
      }
    }

    // Connected passed pawns (extremely strong)
    passedPawnScore += evaluateConnectedPassedPawns(playerPawns, opponentPawns, alliance);

    return passedPawnScore;
  }

  /**
   * Checks if a pawn is a passed pawn (no opposing pawns in front or on adjacent files).
   */
  private boolean isPassedPawn(final Piece pawn, final List<Piece> opponentPawns, final Alliance alliance) {
    final int pawnPosition = pawn.getPiecePosition();
    final int pawnFile = pawnPosition % 8;
    final int pawnRank = pawnPosition / 8;

    // Direction to check (forward for the pawn)
    final int rankDirection = alliance.isWhite() ? -1 : 1;

    // Check all squares in front of the pawn and on adjacent files
    for (int rank = pawnRank + rankDirection; alliance.isWhite() ? (rank >= 0) : (rank < 8); rank += rankDirection) {
      // Check same file
      if (isPawnAtPosition(opponentPawns, rank * 8 + pawnFile)) {
        return false;
      }

      // Check adjacent files
      if (pawnFile > 0 && isPawnAtPosition(opponentPawns, rank * 8 + (pawnFile - 1))) {
        return false;
      }

      if (pawnFile < 7 && isPawnAtPosition(opponentPawns, rank * 8 + (pawnFile + 1))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if there's a pawn at the specified position.
   */
  private boolean isPawnAtPosition(final List<Piece> pawns, final int position) {
    for (final Piece pawn : pawns) {
      if (pawn.getPiecePosition() == position) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the path to promotion is clear of pieces (not just pawns).
   */
  private boolean isPathToPromotionClear(final Piece pawn, final Alliance alliance, final Board board) {
    final int pawnPosition = pawn.getPiecePosition();
    final int pawnFile = pawnPosition % 8;
    final int pawnRank = pawnPosition / 8;

    // Direction to check
    final int rankDirection = alliance.isWhite() ? -1 : 1;

    // Check all squares in front of the pawn
    for (int rank = pawnRank + rankDirection; alliance.isWhite() ? (rank >= 0) : (rank < 8); rank += rankDirection) {
      final int squareToCheck = rank * 8 + pawnFile;
      if (board.getPiece(squareToCheck) != null) {
        return false; // Something is blocking the pawn
      }
    }

    return true;
  }

  /**
   * Checks if a pawn is protected by another pawn.
   */
  private boolean isPawnProtected(final Piece pawn, final List<Piece> playerPawns, final Alliance alliance) {
    final int pawnPosition = pawn.getPiecePosition();
    final int pawnFile = pawnPosition % 8;
    final int pawnRank = pawnPosition / 8;

    // Positions of potential protecting pawns (behind and to the sides)
    final int rankBehind = alliance.isWhite() ? pawnRank + 1 : pawnRank - 1;

    if (rankBehind < 0 || rankBehind >= 8) {
      return false;
    }

    // Check if there are friendly pawns that can protect this pawn
    if (pawnFile > 0 && isPawnAtPosition(playerPawns, rankBehind * 8 + (pawnFile - 1))) {
      return true;
    }

    return pawnFile < 7 && isPawnAtPosition(playerPawns, rankBehind * 8 + (pawnFile + 1));
  }

  /**
   * Evaluates connected passed pawns - a very powerful endgame feature.
   */
  private double evaluateConnectedPassedPawns(final List<Piece> playerPawns,
                                              final List<Piece> opponentPawns,
                                              final Alliance alliance) {
    double connectedScore = 0;
    List<Piece> passedPawns = new ArrayList<>();

    // Identify passed pawns
    for (final Piece pawn : playerPawns) {
      if (isPassedPawn(pawn, opponentPawns, alliance)) {
        passedPawns.add(pawn);
      }
    }

    // Check for adjacent passed pawns
    for (int i = 0; i < passedPawns.size(); i++) {
      for (int j = i + 1; j < passedPawns.size(); j++) {
        final int file1 = passedPawns.get(i).getPiecePosition() % 8;
        final int file2 = passedPawns.get(j).getPiecePosition() % 8;

        // Connected passed pawns are on adjacent files
        if (Math.abs(file1 - file2) == 1) {
          // Connected passed pawns are extremely powerful
          connectedScore += 80;

          // Additional bonus for advanced connected passed pawns
          final int rank1 = passedPawns.get(i).getPiecePosition() / 8;
          final int rank2 = passedPawns.get(j).getPiecePosition() / 8;

          // Rank from pawn's perspective
          final int advancedRank = alliance.isWhite() ?
                  Math.min(rank1, rank2) :
                  Math.max(rank1, rank2);

          if ((alliance.isWhite() && advancedRank <= 2) ||
                  (!alliance.isWhite() && advancedRank >= 5)) {
            connectedScore += 50; // Very advanced connected pawns
          }
        }
      }
    }

    return connectedScore;
  }

  /**
   * Evaluates pawn structure, including pawn islands, isolated pawns,
   * doubled pawns, and pawn majorities.
   */
  private double pawnStructureEvaluation(final Player player, final Board board) {
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());
    final Alliance alliance = player.getAlliance();
    double pawnStructureScore = 0;

    // Evaluate pawn islands (compact pawn structure is better)
    pawnStructureScore += evaluatePawnIslands(playerPawns);

    // Evaluate doubled pawns (more problematic in endgames)
    pawnStructureScore += evaluateDoubledPawns(playerPawns);

    // Evaluate isolated pawns (severe weakness in endgames)
    pawnStructureScore += evaluateIsolatedPawns(playerPawns);

    // Evaluate pawn majorities and minority attacks
    pawnStructureScore += evaluatePawnMajorities(playerPawns, opponentPawns);

    // Evaluate pawn chains (less important in endgames)
    pawnStructureScore += evaluatePawnChains(playerPawns);

    // Evaluate backward pawns
    pawnStructureScore += evaluateBackwardPawns(playerPawns, alliance);

    return pawnStructureScore;
  }

  /**
   * Evaluates pawn islands (groups of connected pawns).
   */
  private double evaluatePawnIslands(final List<Piece> playerPawns) {
    double islandScore = 0;

    // Mark files with pawns
    boolean[] filesWithPawns = new boolean[8];
    for (final Piece pawn : playerPawns) {
      filesWithPawns[pawn.getPiecePosition() % 8] = true;
    }

    // Count islands
    int islands = 0;
    boolean inIsland = false;

    for (int file = 0; file < 8; file++) {
      if (filesWithPawns[file]) {
        if (!inIsland) {
          islands++;
          inIsland = true;
        }
      } else {
        inIsland = false;
      }
    }

    // Penalty for multiple islands (worse in endgames)
    if (islands > 1) {
      islandScore -= (islands - 1) * 15;
    }

    return islandScore;
  }

  /**
   * Evaluates doubled pawns (multiple pawns on the same file).
   */
  private double evaluateDoubledPawns(final List<Piece> playerPawns) {
    double doubledPawnScore = 0;

    // Count pawns on each file
    int[] pawnsPerFile = new int[8];
    for (final Piece pawn : playerPawns) {
      pawnsPerFile[pawn.getPiecePosition() % 8]++;
    }

    // Penalty for doubled pawns (harsher in endgames)
    for (int count : pawnsPerFile) {
      if (count > 1) {
        doubledPawnScore -= (count - 1) * 25;
      }
    }

    return doubledPawnScore;
  }

  /**
   * Evaluates isolated pawns (pawns with no friendly pawns on adjacent files).
   */
  private double evaluateIsolatedPawns(final List<Piece> playerPawns) {
    double isolatedPawnScore = 0;

    // Mark files with pawns
    boolean[] filesWithPawns = new boolean[8];
    for (final Piece pawn : playerPawns) {
      filesWithPawns[pawn.getPiecePosition() % 8] = true;
    }

    // Check for isolated pawns
    for (final Piece pawn : playerPawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      boolean isIsolated = pawnFile <= 0 || !filesWithPawns[pawnFile - 1];

      // Check adjacent files

      if (pawnFile < 7 && filesWithPawns[pawnFile + 1]) {
        isIsolated = false;
      }

      if (isIsolated) {
        isolatedPawnScore -= 20; // Harsher penalty in endgames

        // Extra penalty for isolated pawns on open files
        if (isOnOpenFile(pawn, playerPawns)) {
          isolatedPawnScore -= 10;
        }
      }
    }

    return isolatedPawnScore;
  }

  /**
   * Checks if a pawn is on an open file (no other pawns on the file).
   */
  private boolean isOnOpenFile(final Piece pawn, final List<Piece> pawns) {
    final int pawnFile = pawn.getPiecePosition() % 8;

    for (final Piece otherPawn : pawns) {
      if (otherPawn != pawn && otherPawn.getPiecePosition() % 8 == pawnFile) {
        return false;
      }
    }

    return true;
  }

  /**
   * Evaluates pawn majorities on different sides of the board.
   */
  private double evaluatePawnMajorities(final List<Piece> playerPawns, final List<Piece> opponentPawns) {
    double majorityScore = 0;

    // Count pawns on kingside and queenside
    int playerKingsidePawns = 0;
    int playerQueensidePawns = 0;
    int opponentKingsidePawns = 0;
    int opponentQueensidePawns = 0;

    for (final Piece pawn : playerPawns) {
      final int file = pawn.getPiecePosition() % 8;
      if (file < 4) {
        playerQueensidePawns++;
      } else {
        playerKingsidePawns++;
      }
    }

    for (final Piece pawn : opponentPawns) {
      final int file = pawn.getPiecePosition() % 8;
      if (file < 4) {
        opponentQueensidePawns++;
      } else {
        opponentKingsidePawns++;
      }
    }

    // Evaluate kingside majority
    if (playerKingsidePawns > opponentKingsidePawns) {
      majorityScore += 15 + (playerKingsidePawns - opponentKingsidePawns) * 5;
    }

    // Evaluate queenside majority
    if (playerQueensidePawns > opponentQueensidePawns) {
      majorityScore += 15 + (playerQueensidePawns - opponentQueensidePawns) * 5;
    }

    return majorityScore;
  }

  /**
   * Evaluates pawn chains (connected pawns that protect each other).
   */
  private double evaluatePawnChains(final List<Piece> playerPawns) {
    double pawnChainScore = 0;

    // Map pawns by rank and file
    Map<Integer, Set<Integer>> pawnsByRank = new HashMap<>();

    for (final Piece pawn : playerPawns) {
      final int rank = pawn.getPiecePosition() / 8;
      final int file = pawn.getPiecePosition() % 8;

      if (!pawnsByRank.containsKey(rank)) {
        pawnsByRank.put(rank, new HashSet<>());
      }

      pawnsByRank.get(rank).add(file);
    }

    // Find chain links
    int chainLinks = 0;
    for (final Piece pawn : playerPawns) {
      final int rank = pawn.getPiecePosition() / 8;
      final int file = pawn.getPiecePosition() % 8;

      // Check if pawn is protected by another pawn
      if (isPawnProtectingFile(pawnsByRank, rank, file)) {
        chainLinks++;
      }
    }

    // Bonus for pawn chains (less valuable in endgames)
    pawnChainScore += chainLinks * 5;

    return pawnChainScore;
  }

  /**
   * Checks if a pawn is protected by another pawn on an adjacent file.
   */
  private boolean isPawnProtectingFile(final Map<Integer, Set<Integer>> pawnsByRank,
                                       final int rank,
                                       final int file) {
    // Check if there's a pawn on the previous rank that could protect this pawn
    if (pawnsByRank.containsKey(rank + 1)) {
      final Set<Integer> filesWithPawns = pawnsByRank.get(rank + 1);

      // Check if there are pawns on adjacent files that could protect this pawn
      return (file > 0 && filesWithPawns.contains(file - 1)) ||
              (file < 7 && filesWithPawns.contains(file + 1));
    }

    return false;
  }

  /**
   * Evaluates backward pawns (pawns that can't be protected by adjacent pawns).
   */
  private double evaluateBackwardPawns(final List<Piece> playerPawns, final Alliance alliance) {
    double backwardPawnScore = 0;

    // Mark ranks with pawns on each file
    int[] lowestRankOnFile = new int[8];
    for (int i = 0; i < 8; i++) {
      lowestRankOnFile[i] = alliance.isWhite() ? 7 : 0;
    }

    for (final Piece pawn : playerPawns) {
      final int file = pawn.getPiecePosition() % 8;
      final int rank = pawn.getPiecePosition() / 8;

      if (alliance.isWhite()) {
        lowestRankOnFile[file] = Math.min(lowestRankOnFile[file], rank);
      } else {
        lowestRankOnFile[file] = Math.max(lowestRankOnFile[file], rank);
      }
    }

    // Check for backward pawns
    for (final Piece pawn : playerPawns) {
      final int file = pawn.getPiecePosition() % 8;
      final int rank = pawn.getPiecePosition() / 8;

      boolean isBackward = false;

      // Compare with adjacent files
      if (file > 0) {
        if (alliance.isWhite() && rank > lowestRankOnFile[file - 1]) {
          isBackward = true;
        } else if (!alliance.isWhite() && rank < lowestRankOnFile[file - 1]) {
          isBackward = true;
        }
      }

      if (file < 7) {
        if (alliance.isWhite() && rank > lowestRankOnFile[file + 1]) {
          isBackward = true;
        } else if (!alliance.isWhite() && rank < lowestRankOnFile[file + 1]) {
          isBackward = true;
        }
      }

      if (isBackward) {
        backwardPawnScore -= 15;

        // Backward pawns on open files are worse
        if (isOnOpenFile(pawn, playerPawns)) {
          backwardPawnScore -= 10;
        }
      }
    }

    return backwardPawnScore;
  }

  /**
   * Evaluates piece coordination in endgames, focusing on cooperation
   * between pieces and with the king.
   */
  private double pieceCoordinationEvaluation(final Player player, final Board board) {
    double coordinationScore = 0;
    final Collection<Piece> playerPieces = player.getActivePieces();

    // Knight and bishop coordination
    coordinationScore += evaluateMinorPieceCoordination(playerPieces, board);

    // Piece placement relative to pawns
    coordinationScore += evaluatePiecePlacement(playerPieces, getPlayerPawns(player));

    // Support for passed pawns
    coordinationScore += evaluatePiecesSupportingPassedPawns(player, board);

    return coordinationScore;
  }

  /**
   * Evaluates minor piece coordination in the endgame.
   */
  private double evaluateMinorPieceCoordination(final Collection<Piece> playerPieces, final Board board) {
    double minorPieceScore = 0;

    // Check for bishop pair in endgames with pawns
    if (board.getAllPieces().stream().anyMatch(p -> p.getPieceType() == Piece.PieceType.PAWN)) {
      long bishopCount = playerPieces.stream()
              .filter(p -> p.getPieceType() == Piece.PieceType.BISHOP)
              .count();

      if (bishopCount >= 2) {
        minorPieceScore += 50; // Even more valuable in endgames
      }
    }

    // Knights lose value in endgames with few pawns
    if (getPlayerPawns(board.whitePlayer()).size() + getPlayerPawns(board.blackPlayer()).size() <= 4) {
      long knightCount = playerPieces.stream()
              .filter(p -> p.getPieceType() == Piece.PieceType.KNIGHT)
              .count();

      minorPieceScore -= knightCount * 10;
    }

    return minorPieceScore;
  }

  /**
   * Evaluates piece placement relative to pawns.
   */
  private double evaluatePiecePlacement(final Collection<Piece> playerPieces,
                                        final List<Piece> playerPawns) {
    double placementScore = 0;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        // Bishops behind pawns are well-placed
        placementScore += evaluateBishopPlacement((Bishop) piece, playerPawns);
      } else if (piece.getPieceType() == Piece.PieceType.KNIGHT) {
        // Knights near pawns are well-placed
        placementScore += evaluateKnightPlacement((Knight) piece, playerPawns);
      }
    }

    return placementScore;
  }

  /**
   * Evaluates bishops placement relative to pawns.
   */
  private double evaluateBishopPlacement(final Bishop bishop, final List<Piece> playerPawns) {
    double bishopScore = 0;
    final int bishopPosition = bishop.getPiecePosition();
    final int bishopFile = bishopPosition % 8;
    final int bishopRank = bishopPosition / 8;
    final Alliance alliance = bishop.getPieceAllegiance();

    // Check if bishop is behind pawns (good for long diagonals)
    int pawnsInFront = 0;
    for (final Piece pawn : playerPawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      final int pawnRank = pawn.getPiecePosition() / 8;

      // Pawn is on same or adjacent file
      if (Math.abs(pawnFile - bishopFile) <= 1) {
        // Pawn is in front of bishop
        if ((alliance.isWhite() && pawnRank < bishopRank) ||
                (!alliance.isWhite() && pawnRank > bishopRank)) {
          pawnsInFront++;
        }
      }
    }

    if (pawnsInFront > 0) {
      bishopScore += 10;
    }

    // Check if bishop is on a long diagonal
    if ((bishopPosition % 9 == 0) || (bishopPosition % 7 == 0 && bishopPosition % 8 != 0 && bishopPosition % 8 != 7)) {
      bishopScore += 15;
    }

    return bishopScore;
  }

  /**
   * Evaluates knight placement relative to pawns.
   */
  private double evaluateKnightPlacement(final Knight knight, final List<Piece> playerPawns) {
    double knightScore = 0;
    final int knightPosition = knight.getPiecePosition();

    // Knights are good near pawns in the endgame
    for (final Piece pawn : playerPawns) {
      final int distance = calculateChebyshevDistance(knightPosition, pawn.getPiecePosition());

      if (distance <= 2) {
        knightScore += (3 - distance) * 5;
      }
    }

    return knightScore;
  }

  /**
   * Evaluates how pieces support passed pawns.
   */
  private double evaluatePiecesSupportingPassedPawns(final Player player, final Board board) {
    double supportScore = 0;
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());
    final Alliance alliance = player.getAlliance();
    final Collection<Piece> playerPieces = player.getActivePieces();

    // Find passed pawns
    for (final Piece pawn : playerPawns) {
      if (isPassedPawn(pawn, opponentPawns, alliance)) {
        final int pawnPosition = pawn.getPiecePosition();
        final int promotionSquare = alliance.isWhite() ?
                (pawnPosition % 8) :
                ((7 * 8) + (pawnPosition % 8));

        // Check for pieces supporting the pawn
        for (final Piece piece : playerPieces) {
          if (piece.getPieceType() != Piece.PieceType.PAWN &&
                  piece.getPieceType() != Piece.PieceType.KING) {

            // Check if piece supports the passed pawn
            final int piecePosition = piece.getPiecePosition();
            final int distance = calculateChebyshevDistance(piecePosition, pawnPosition);

            if (distance <= 1) {
              supportScore += 20; // Direct support
            } else if (distance == 2) {
              supportScore += 10; // Near support
            }

            // Check if piece controls promotion square
            final Collection<Move> pieceMoves = piece.calculateLegalMoves(board);
            for (final Move move : pieceMoves) {
              if (move.getDestinationCoordinate() == promotionSquare) {
                supportScore += 15; // Controls promotion square
                break;
              }
            }
          }
        }
      }
    }

    return supportScore;
  }

  /**
   * Evaluates rook-specific endgame patterns, like rooks behind passed pawns,
   * rooks on the 7th rank, and open file control.
   */
  private double rookEndgameEvaluation(final Player player, final Board board) {
    double rookScore = 0;
    final List<Piece> playerRooks = player.getActivePieces().stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.ROOK)
            .collect(Collectors.toList());

    if (playerRooks.isEmpty()) {
      return 0;
    }

    // Get all pawns
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());
    final Alliance alliance = player.getAlliance();

    // Evaluate each rook
    for (final Piece rook : playerRooks) {
      // Rooks on open files
      rookScore += evaluateRookOnOpenFile(rook, board);

      // Rooks behind passed pawns
      rookScore += evaluateRookBehindPassedPawn(rook, playerPawns, opponentPawns, alliance);

      // Rooks on 7th rank
      rookScore += evaluateRookOn7thRank(rook, opponentPawns, alliance);

      // Connected rooks
      if (playerRooks.size() >= 2) {
        rookScore += evaluateConnectedRooks(playerRooks);
      }
    }

    return rookScore;
  }

  /**
   * Evaluates rooks on open or semi-open files.
   */
  private double evaluateRookOnOpenFile(final Piece rook, final Board board) {
    double fileScore = 0;
    final int rookFile = rook.getPiecePosition() % 8;

    boolean openFile = true;
    boolean semiOpenFile = true;

    // Check if file is open (no pawns at all)
    for (int rank = 0; rank < 8; rank++) {
      final Piece piece = board.getPiece(rank * 8 + rookFile);
      if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN) {
        openFile = false;

        // Check if semi-open (no friendly pawns)
        if (piece.getPieceAllegiance() == rook.getPieceAllegiance()) {
          semiOpenFile = false;
        }
      }
    }

    if (openFile) {
      fileScore += 30; // Open file is very valuable for rooks
    } else if (semiOpenFile) {
      fileScore += 15; // Semi-open file
    }

    return fileScore;
  }

  /**
   * Evaluates rooks behind passed pawns (Tarrasch rule).
   */
  private double evaluateRookBehindPassedPawn(final Piece rook,
                                              final List<Piece> playerPawns,
                                              final List<Piece> opponentPawns,
                                              final Alliance alliance) {
    double behindPawnScore = 0;
    final int rookPosition = rook.getPiecePosition();
    final int rookFile = rookPosition % 8;
    final int rookRank = rookPosition / 8;

    // Check for passed pawns on the same file
    for (final Piece pawn : playerPawns) {
      if (pawn.getPiecePosition() % 8 == rookFile &&
              isPassedPawn(pawn, opponentPawns, alliance)) {
        final int pawnRank = pawn.getPiecePosition() / 8;

        // Check if rook is behind the passed pawn
        if ((alliance.isWhite() && rookRank > pawnRank) ||
                (!alliance.isWhite() && rookRank < pawnRank)) {
          behindPawnScore += 30; // Tarrasch rule: Rooks belong behind passed pawns
        }
      }
    }

    // Check for opposing passed pawns on the same file
    for (final Piece pawn : opponentPawns) {
      if (pawn.getPiecePosition() % 8 == rookFile &&
              isPassedPawn(pawn, playerPawns, alliance.equals(Alliance.WHITE) ? Alliance.BLACK : Alliance.WHITE)) {
        final int pawnRank = pawn.getPiecePosition() / 8;

        // Check if rook is behind the opponent's passed pawn
        if ((alliance.isWhite() && rookRank > pawnRank) ||
                (!alliance.isWhite() && rookRank < pawnRank)) {
          behindPawnScore += 20; // Good to have rook behind opponent's passed pawn too
        }
      }
    }

    return behindPawnScore;
  }

  /**
   * Evaluates rooks on the 7th rank (or 2nd for black).
   */
  private double evaluateRookOn7thRank(final Piece rook,
                                       final List<Piece> opponentPawns,
                                       final Alliance alliance) {
    double seventhRankScore = 0;
    final int rookRank = rook.getPiecePosition() / 8;

    // Check if rook is on 7th rank (2nd rank for black)
    if ((alliance.isWhite() && rookRank == 1) || (!alliance.isWhite() && rookRank == 6)) {
      seventhRankScore += 30;

      // Additional bonus if there are enemy pawns on the 7th rank
      for (final Piece pawn : opponentPawns) {
        final int pawnRank = pawn.getPiecePosition() / 8;
        if ((alliance.isWhite() && pawnRank == 1) || (!alliance.isWhite() && pawnRank == 6)) {
          seventhRankScore += 10;
        }
      }
    }

    return seventhRankScore;
  }

  /**
   * Evaluates connected rooks (rooks that protect each other).
   */
  private double evaluateConnectedRooks(final List<Piece> playerRooks) {
    double connectedScore = 0;

    for (int i = 0; i < playerRooks.size() - 1; i++) {
      for (int j = i + 1; j < playerRooks.size(); j++) {
        final int rookPos1 = playerRooks.get(i).getPiecePosition();
        final int rookPos2 = playerRooks.get(j).getPiecePosition();

        // Rooks on same rank
        if (rookPos1 / 8 == rookPos2 / 8) {
          connectedScore += 20;
        }

        // Rooks on same file
        if (rookPos1 % 8 == rookPos2 % 8) {
          connectedScore += 15;
        }
      }
    }

    return connectedScore;
  }

  /**
   * Evaluates bishop endgame patterns, including color complexes,
   * mobility, and bishop vs knight dynamics.
   */
  private double bishopEndgameEvaluation(final Player player, final Board board) {
    double bishopScore = 0;
    final List<Piece> playerBishops = player.getActivePieces().stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.BISHOP)
            .collect(Collectors.toList());

    if (playerBishops.isEmpty()) {
      return 0;
    }

    // Evaluate color complex control
    bishopScore += evaluateColorComplexControl(playerBishops, board);

    // Bishop mobility (more important in endgames)
    for (final Piece bishop : playerBishops) {
      final Collection<Move> bishopMoves = bishop.calculateLegalMoves(board);
      bishopScore += bishopMoves.size() * 5;
    }

    // Bishop vs Knight evaluation in specific positions
    final List<Piece> playerKnights = player.getActivePieces().stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.KNIGHT)
            .toList();

    final List<Piece> opponentKnights = player.getOpponent().getActivePieces().stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.KNIGHT)
            .toList();

    // Bishop vs Knight dynamics
    if (!playerBishops.isEmpty() && !opponentKnights.isEmpty() && playerKnights.isEmpty()) {
      bishopScore += evaluateBishopVsKnight(board);
    }

    return bishopScore;
  }

  /**
   * Evaluates color complex control (important in endgames).
   */
  private double evaluateColorComplexControl(final List<Piece> playerBishops, final Board board) {
    double colorScore = 0;

    // Check bishop square colors
    boolean hasLightSquareBishop = false;
    boolean hasDarkSquareBishop = false;

    for (final Piece bishop : playerBishops) {
      final int position = bishop.getPiecePosition();
      final boolean isLightSquare = ((position / 8) + (position % 8)) % 2 == 0;

      if (isLightSquare) {
        hasLightSquareBishop = true;
      } else {
        hasDarkSquareBishop = true;
      }
    }

    // Bishop pair bonus
    if (hasLightSquareBishop && hasDarkSquareBishop) {
      colorScore += 50;
      return colorScore; // With bishop pair, no need for further evaluation
    }

    // Count pawns on same colored squares as the bishop
    final List<Piece> allPawns = new ArrayList<>();
    allPawns.addAll(getPlayerPawns(board.whitePlayer()));
    allPawns.addAll(getPlayerPawns(board.blackPlayer()));

    int lightSquarePawns = 0;
    int darkSquarePawns = 0;

    for (final Piece pawn : allPawns) {
      final int position = pawn.getPiecePosition();
      final boolean isLightSquare = ((position / 8) + (position % 8)) % 2 == 0;

      if (isLightSquare) {
        lightSquarePawns++;
      } else {
        darkSquarePawns++;
      }
    }

    // Favorable color complex
    if (hasLightSquareBishop && darkSquarePawns > lightSquarePawns) {
      colorScore += 20; // Good - bishop controls squares not blocked by pawns
    } else if (!hasLightSquareBishop && hasDarkSquareBishop && lightSquarePawns > darkSquarePawns) {
      colorScore += 20; // Good - bishop controls squares not blocked by pawns
    }

    // Unfavorable color complex
    if (hasLightSquareBishop && lightSquarePawns > darkSquarePawns) {
      colorScore -= 15; // Bad - bishop controls squares mostly blocked by pawns
    } else if (!hasLightSquareBishop && hasDarkSquareBishop && darkSquarePawns > lightSquarePawns) {
      colorScore -= 15; // Bad - bishop controls squares mostly blocked by pawns
    }

    return colorScore;
  }

  /**
   * Evaluates bishop vs knight dynamics in specific positions.
   */
  private double evaluateBishopVsKnight(final Board board) {
    double dynamicScore = 0;

    // Count pawns
    final int pawnCount = getPlayerPawns(board.whitePlayer()).size() +
            getPlayerPawns(board.blackPlayer()).size();

    // Bishops are better in open positions
    if (pawnCount <= 5) {
      dynamicScore += 20; // Open position favors bishop
    } else if (pawnCount >= 8) {
      dynamicScore -= 10; // Closed position favors knight
    }

    return dynamicScore;
  }

  /**
   * Evaluates common draw patterns in endgames.
   */
  private double drawPatternEvaluation(final Player player, final Board board) {
    double drawScore = 0;
    final Collection<Piece> playerPieces = player.getActivePieces();
    final Collection<Piece> opponentPieces = player.getOpponent().getActivePieces();

    // Count material
    Map<Piece.PieceType, Integer> playerPieceCounts = countPieceTypes(playerPieces);
    Map<Piece.PieceType, Integer> opponentPieceCounts = countPieceTypes(opponentPieces);

    // Insufficient material draws
    if (isInsufficientMaterial(playerPieceCounts, opponentPieceCounts)) {
      drawScore -= 800; // Large drawish penalty
    }

    // Opposite colored bishops endgame
    if (hasOppositeColoredBishops(playerPieces, opponentPieces) &&
            playerPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) <= 2 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) <= 2) {
      drawScore -= 200; // Likely draw with few pawns
    }

    // Rook and pawn vs rook (often drawish)
    if (playerPieceCounts.getOrDefault(Piece.PieceType.ROOK, 0) == 1 &&
            playerPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) == 1 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.ROOK, 0) == 1 &&
            opponentPieceCounts.getOrDefault(Piece.PieceType.PAWN, 0) == 0) {
      // Check if pawn is too advanced
      List<Piece> playerPawns = getPlayerPawns(player);
      if (!playerPawns.isEmpty()) {
        Piece pawn = playerPawns.get(0);
        int pawnRank = pawn.getPiecePosition() / 8;

        if ((player.getAlliance().isWhite() && pawnRank <= 1) ||
                (!player.getAlliance().isWhite() && pawnRank >= 6)) {
          drawScore += 100; // Advanced pawn might win
        } else {
          drawScore -= 100; // Likely draw
        }
      }
    }

    return drawScore;
  }

  /**
   * Evaluates mobility, which is less important in many endgames but
   * still relevant for certain positions.
   */
  private double mobilityEvaluation(final Player player, final Board board) {
    double mobilityScore = 0;
    final Collection<Move> playerMoves = player.getLegalMoves();
    final Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();

    // Adjust mobility importance based on position type
    double mobilityWeight = 1.0;

    // In pawn endgames, mobility is less important
    if (isPawnEndgame(board)) {
      mobilityWeight = 0.5;
    }

    // In positions with opposite bishops, mobility is more important
    if (hasOppositeColoredBishops(player.getActivePieces(), player.getOpponent().getActivePieces())) {
      mobilityWeight = 1.5;
    }

    // Calculate mobility difference
    int mobilityDifference = playerMoves.size() - opponentMoves.size();
    mobilityScore += mobilityDifference * 2 * mobilityWeight;

    return mobilityScore;
  }

  /**
   * Checks if the position is a pawn endgame (only kings and pawns).
   */
  private boolean isPawnEndgame(final Board board) {
    for (final Piece piece : board.getAllPieces()) {
      if (piece.getPieceType() != Piece.PieceType.KING &&
              piece.getPieceType() != Piece.PieceType.PAWN) {
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates the Chebyshev distance between two squares.
   */
  private int calculateChebyshevDistance(final int position1, final int position2) {
    final int file1 = position1 % 8;
    final int rank1 = position1 / 8;
    final int file2 = position2 % 8;
    final int rank2 = position2 / 8;

    return Math.max(Math.abs(file1 - file2), Math.abs(rank1 - rank2));
  }

  /**
   * Gets a list of pawn pieces for the specified player.
   */
  private static List<Piece> getPlayerPawns(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN)
            .collect(Collectors.toList());
  }
}