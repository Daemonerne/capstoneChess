package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.King;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MiddlegameBoardEvaluator implements BoardEvaluator {

  /*** Singleton instance of the MiddlegameBoardEvaluator. */
  private static final MiddlegameBoardEvaluator Instance = new MiddlegameBoardEvaluator();

  /*** Private constructor to prevent instantiation outside of class. */
  private MiddlegameBoardEvaluator() {}

  /**
   * Returns the singleton instance of MiddlegameBoardEvaluator.
   *
   * @return The instance of MiddlegameBoardEvaluator.
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

  /**
   * Calculates the overall score of the current board position for a given player
   * using modern chess engine evaluation principles tuned for middlegame positions.
   *
   * @param player The player for whom the board position is being evaluated.
   * @param board  The current state of the chess board.
   * @return       The evaluation score of the board from the perspective of the specified player.
   */
  @VisibleForTesting
  private double score(final Player player, final Board board) {
    return materialEvaluation(player.getActivePieces()) +
            mobilityEvaluation(player) +
            kingSafetyEvaluation(player, board) +
            pawnStructureEvaluation(player) +
            pieceCoordinationEvaluation(player, board) +
            spaceControlEvaluation(player) +
            attackingPotentialEvaluation(player) +
            specialPatternsEvaluation(player, board);
  }

  /**
   * Evaluates material balance with refined piece values and contextual adjustments.
   * Modern engines use dynamic piece values based on the position.
   */
  private double materialEvaluation(final Collection<Piece> playerPieces) {
    double materialScore = 0;

    // Count pieces by type for additional bonuses/penalties
    int numBishops = 0;

    for (final Piece piece : playerPieces) {
      // Base material value
      materialScore += piece.getPieceValue();

      // Keep track of bishops for bishop pair bonus
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        numBishops++;
      }
    }

    // Bishop pair bonus (significant in middlegame)
    if (numBishops >= 2) {
      materialScore += 45;
    }

    return materialScore;
  }

  /**
   * Evaluates piece mobility, a critical factor in middlegame evaluation.
   * Mobility represents how many squares pieces can move to or attack.
   */
  private double mobilityEvaluation(final Player player) {
    final Collection<Move> playerMoves = player.getLegalMoves();
    final Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();
    double mobilityScore = 0;

    // Count moves for each piece type with weighted values
    Map<Piece.PieceType, Integer> movesPerPiece = new HashMap<>();
    for (final Move move : playerMoves) {
      Piece.PieceType pieceType = move.getMovedPiece().getPieceType();
      movesPerPiece.put(pieceType, movesPerPiece.getOrDefault(pieceType, 0) + 1);
    }

    // Apply mobility bonuses by piece type
    if (movesPerPiece.containsKey(Piece.PieceType.KNIGHT)) {
      mobilityScore += movesPerPiece.get(Piece.PieceType.KNIGHT) * 4.5;
    }

    if (movesPerPiece.containsKey(Piece.PieceType.BISHOP)) {
      mobilityScore += movesPerPiece.get(Piece.PieceType.BISHOP) * 5.0;
    }

    if (movesPerPiece.containsKey(Piece.PieceType.ROOK)) {
      mobilityScore += movesPerPiece.get(Piece.PieceType.ROOK) * 4.0;
    }

    if (movesPerPiece.containsKey(Piece.PieceType.QUEEN)) {
      mobilityScore += movesPerPiece.get(Piece.PieceType.QUEEN) * 2.0;
    }

    // Consider mobility advantage/disadvantage
    double relativeMobility = playerMoves.size() - opponentMoves.size();
    mobilityScore += relativeMobility * 2.5;

    return mobilityScore;
  }

  /**
   * Evaluates king safety, considering pawn shield, attacking pieces,
   * open lines, and king attackers. This is a critical middlegame factor.
   */
  private double kingSafetyEvaluation(final Player player, final Board board) {
    double kingSafetyScore = 0;
    final King playerKing = player.getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();

    // King castling bonus (slightly less important than in opening)
    if (playerKing.isCastled()) {
      kingSafetyScore += 40;
    }

    // Evaluate pawn shield - critical for king safety
    kingSafetyScore += evaluatePawnShield(player, board, kingPosition);

    // Evaluate king exposure to attacks
    kingSafetyScore -= evaluateKingExposure(player, kingPosition);

    // Evaluate attacking pieces near king (king tropism)
    kingSafetyScore -= evaluateKingTropism(player, kingPosition);

    // Open files near king (penalty)
    kingSafetyScore -= evaluateOpenFilesToKing(player, kingPosition);

    return kingSafetyScore;
  }

  /**
   * Evaluates the pawn shield protecting the king.
   */
  private double evaluatePawnShield(final Player player, final Board board, final int kingPosition) {
    double shieldScore = 0;
    final Alliance playerAlliance = player.getAlliance();

    // Identify pawn shield squares based on king position
    List<Integer> shieldSquares = getKingShieldSquares(kingPosition, playerAlliance);

    // Count pawns on shield squares
    int pawnsInShield = 0;
    int intactFileCount = 0;
    Set<Integer> shieldFiles = new HashSet<>();

    for (Integer shieldSquare : shieldSquares) {
      Piece piece = board.getPiece(shieldSquare);
      if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN &&
              piece.getPieceAllegiance() == playerAlliance) {
        pawnsInShield++;
        int file = shieldSquare % 8;
        if (!shieldFiles.contains(file)) {
          intactFileCount++;
          shieldFiles.add(file);
        }
      }
    }

    // Score based on shield quality
    if (pawnsInShield >= 3) {
      shieldScore += 35;  // Strong shield
    } else if (pawnsInShield == 2) {
      shieldScore += 20;  // Decent shield
    } else if (pawnsInShield == 1) {
      shieldScore += 5;   // Weak shield
    } else {
      shieldScore -= 20;  // No shield - penalty
    }

    // Bonus for intact files in front of king
    shieldScore += intactFileCount * 10;

    return shieldScore;
  }

  /**
   * Gets the squares that form a pawn shield for the king.
   */
  private List<Integer> getKingShieldSquares(final int kingPosition, final Alliance alliance) {
    List<Integer> shieldSquares = new ArrayList<>();
    final int kingFile = kingPosition % 8;
    final int kingRank = kingPosition / 8;

    // Determine if king is on kingside or queenside
    boolean kingSide = kingFile >= 4;

    // White's shield is above king, Black's shield is below
    int shieldRank = alliance.isWhite() ? kingRank - 1 : kingRank + 1;

    // Ensure shield rank is on the board
    if (shieldRank >= 0 && shieldRank < 8) {
      // Add shield squares based on king position
      for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
        shieldSquares.add(shieldRank * 8 + file);
      }

      // Add extended shield for castled position
      if ((kingSide && (kingFile == 6 || kingFile == 5)) ||
              (!kingSide && (kingFile == 1 || kingFile == 2))) {
        if (alliance.isWhite()) {
          if (kingRank == 7) { // Castled position for white
            for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
              shieldSquares.add((kingRank - 2) * 8 + file);
            }
          }
        } else {
          if (kingRank == 0) { // Castled position for black
            for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
              shieldSquares.add((kingRank + 2) * 8 + file);
            }
          }
        }
      }
    }

    return shieldSquares;
  }

  /**
   * Evaluates the king's exposure to attacks.
   */
  private double evaluateKingExposure(final Player player, final int kingPosition) {
    double exposureScore = 0;
    final Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();

    // Calculate attacks directly targeting king area
    int attacksNearKing = 0;

    for (final Move move : opponentMoves) {
      final int moveDestination = move.getDestinationCoordinate();
      // Check if move targets squares near king
      if (isSquareNearKing(kingPosition, moveDestination)) {
        attacksNearKing++;

        // Higher penalty for attacks by more valuable pieces
        if (move.getMovedPiece().getPieceType() == Piece.PieceType.QUEEN) {
          exposureScore += 10;
        } else if (move.getMovedPiece().getPieceType() == Piece.PieceType.ROOK) {
          exposureScore += 7;
        } else if (move.getMovedPiece().getPieceType() == Piece.PieceType.BISHOP ||
                move.getMovedPiece().getPieceType() == Piece.PieceType.KNIGHT) {
          exposureScore += 5;
        } else {
          exposureScore += 2;
        }
      }
    }

    // Penalty scales exponentially with number of attackers (attack coordination)
    if (attacksNearKing > 2) {
      exposureScore += (attacksNearKing - 2) * 15;
    }

    // Penalty for checks
    if (player.isInCheck()) {
      exposureScore += 30;
    }

    return exposureScore;
  }

  /**
   * Determines if a square is near the king.
   */
  private boolean isSquareNearKing(final int kingPosition, final int square) {
    final int kingRank = kingPosition / 8;
    final int kingFile = kingPosition % 8;
    final int squareRank = square / 8;
    final int squareFile = square % 8;

    // Check if square is within 2 squares of king (Chebyshev distance)
    return Math.max(Math.abs(kingRank - squareRank), Math.abs(kingFile - squareFile)) <= 2;
  }

  /**
   * Evaluates king tropism - the proximity of opponent pieces to the king.
   */
  private double evaluateKingTropism(final Player player, final int kingPosition) {
    double tropismScore = 0;
    final Collection<Piece> opponentPieces = player.getOpponent().getActivePieces();

    for (final Piece piece : opponentPieces) {
      if (piece.getPieceType() == Piece.PieceType.KING) {
        continue; // Skip opponent king
      }

      // Calculate distance to king
      int distance = calculateChebyshevDistance(kingPosition, piece.getPiecePosition());
      double pieceValue = piece.getPieceValue() / 100.0; // Normalize piece value

      // Different piece types have different tropism weights
      switch (piece.getPieceType()) {
        case QUEEN:
          // Queens are most dangerous near the king
          tropismScore += (7 - distance) * 6 * pieceValue;
          break;
        case ROOK:
          // Rooks are also very dangerous
          tropismScore += (7 - distance) * 4 * pieceValue;
          break;
        case BISHOP:
          // Bishops can be dangerous from a distance on diagonals
          tropismScore += (7 - distance) * 3 * pieceValue;
          break;
        case KNIGHT:
          // Knights need to be close to be effective
          if (distance <= 3) {
            tropismScore += (4 - distance) * 4 * pieceValue;
          }
          break;
        case PAWN:
          // Pawns are primarily dangerous when very close
          if (distance <= 2) {
            tropismScore += (3 - distance) * 2 * pieceValue;
          }
          break;
      }
    }

    return tropismScore;
  }

  /**
   * Evaluates open files leading to the king, which can be dangerous.
   */
  private double evaluateOpenFilesToKing(final Player player, final int kingPosition) {
    double openFileScore = 0;
    final int kingFile = kingPosition % 8;
    final List<Piece> playerPawns = getPlayerPawns(player);

    // Check the king's file
    if (isPawnOnFile(kingFile, playerPawns)) {
      // King on open file
      openFileScore += 25;

      // Even worse if opponent has rook or queen on this file
      if (hasHeavyPieceOnFile(kingFile, player.getOpponent().getActivePieces())) {
        openFileScore += 35;
      }
    }

    // Check files adjacent to king
    for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
      if (file == kingFile) continue; // Already checked

      if (isPawnOnFile(file, playerPawns)) {
        // Adjacent open file
        openFileScore += 15;

        // Worse if opponent has rook or queen on this file
        if (hasHeavyPieceOnFile(file, player.getOpponent().getActivePieces())) {
          openFileScore += 25;
        }
      }
    }

    return openFileScore;
  }

  /**
   * Checks if there's a pawn on the specified file.
   */
  private boolean isPawnOnFile(final int file, final List<Piece> pawns) {
    for (final Piece pawn : pawns) {
      if (pawn.getPiecePosition() % 8 == file) {
        return false;
      }
    } return true;
  }

  /**
   * Checks if there's a heavy piece (rook or queen) on the specified file.
   */
  private boolean hasHeavyPieceOnFile(final int file, final Collection<Piece> pieces) {
    for (final Piece piece : pieces) {
      if ((piece.getPieceType() == Piece.PieceType.ROOK ||
              piece.getPieceType() == Piece.PieceType.QUEEN) &&
              piece.getPiecePosition() % 8 == file) {
        return true;
      }
    }
    return false;
  }

  /**
   * Evaluates the pawn structure, considering passed pawns, isolated pawns,
   * doubled pawns, pawn chains, and pawn islands.
   */
  private double pawnStructureEvaluation(final Player player) {
    final List<Piece> playerPawns = getPlayerPawns(player);
    final List<Piece> opponentPawns = getPlayerPawns(player.getOpponent());
    double pawnStructureScore = 0;

    // Evaluate passed pawns - more valuable in middlegame than opening
    pawnStructureScore += evaluatePassedPawns(playerPawns, opponentPawns, player.getAlliance());

    // Evaluate pawn islands (groups of pawns separated by files with no pawns)
    pawnStructureScore += evaluatePawnIslands(playerPawns);

    // Evaluate doubled pawns (penalty)
    pawnStructureScore += evaluateDoubledPawns(playerPawns);

    // Evaluate isolated pawns (penalty)
    pawnStructureScore += evaluateIsolatedPawns(playerPawns);

    // Evaluate backward pawns (penalty)
    pawnStructureScore += evaluateBackwardPawns(playerPawns, player.getAlliance());

    // Evaluate pawn chains (bonus)
    pawnStructureScore += evaluatePawnChains(playerPawns);

    // Evaluate central pawn control
    pawnStructureScore += evaluateCentralPawnControl(playerPawns);

    return pawnStructureScore;
  }

  /**
   * Evaluates passed pawns, which are more valuable in the middlegame.
   */
  private double evaluatePassedPawns(final List<Piece> playerPawns,
                                     final List<Piece> opponentPawns,
                                     final Alliance alliance) {
    double passedPawnScore = 0;

    for (final Piece pawn : playerPawns) {
      if (isPassedPawn(pawn, opponentPawns, alliance)) {
        final int rank = pawn.getPiecePosition() / 8;
        final int advancementBonus = alliance.isWhite() ? 7 - rank : rank;

        // Passed pawns are more valuable the further they are advanced
        passedPawnScore += 20 + (advancementBonus * 10);

        // Extra bonus for protected passed pawns
        if (isPawnProtected(pawn, playerPawns, alliance)) {
          passedPawnScore += 15;
        }

        // Bonus for passed pawns on open files
        if (!opponentPieceControlsSquaresInFront(pawn, alliance, opponentPawns)) {
          passedPawnScore += 10;
        }
      }
    }

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
   * Checks if any opponent piece controls squares in front of the pawn.
   */
  private boolean opponentPieceControlsSquaresInFront(final Piece pawn,
                                                      final Alliance alliance,
                                                      final List<Piece> opponentPawns) {
    final int pawnPosition = pawn.getPiecePosition();
    final int pawnFile = pawnPosition % 8;
    final int pawnRank = pawnPosition / 8;

    // Direction to check
    final int rankDirection = alliance.isWhite() ? -1 : 1;

    // Check one square in front
    final int frontRank = pawnRank + rankDirection;
    if (frontRank < 0 || frontRank >= 8) {
      return false;
    }

    for (final Piece opponentPawn : opponentPawns) {
      // Check if opponent pawn attacks the square in front
      final int opponentFile = opponentPawn.getPiecePosition() % 8;
      final int opponentRank = opponentPawn.getPiecePosition() / 8;

      if (Math.abs(opponentFile - pawnFile) == 1 &&
              ((alliance.isWhite() && opponentRank == frontRank + 1) ||
                      (!alliance.isWhite() && opponentRank == frontRank - 1))) {
        return true;
      }
    }

    return false;
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

    // Penalty for multiple islands
    if (islands > 1) {
      islandScore -= (islands - 1) * 10;
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

    // Penalty for doubled pawns
    for (int count : pawnsPerFile) {
      if (count > 1) {
        doubledPawnScore -= (count - 1) * 20;
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
        isolatedPawnScore -= 15;

        // Isolated pawns on semi-open files are worse
        if (isOnSemiOpenFile(pawn, playerPawns)) {
          isolatedPawnScore -= 5;
        }

        // Isolated pawns are worse in the center
        if (pawnFile > 1 && pawnFile < 6) {
          isolatedPawnScore -= 5;
        }
      }
    }

    return isolatedPawnScore;
  }

  /**
   * Checks if a pawn is on a semi-open file (no opponent pawn on the same file).
   */
  private boolean isOnSemiOpenFile(final Piece pawn, final List<Piece> pawns) {
    final int pawnFile = pawn.getPiecePosition() % 8;

    for (final Piece otherPawn : pawns) {
      if (otherPawn != pawn && otherPawn.getPiecePosition() % 8 == pawnFile) {
        return false;
      }
    }

    return true;
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
      boolean isBackward = isIsBackward(alliance, pawn, lowestRankOnFile);

      if (isBackward) {
        backwardPawnScore -= 20;

        // Backward pawns on semi-open files are worse
        if (isOnSemiOpenFile(pawn, playerPawns)) {
          backwardPawnScore -= 5;
        }
      }
    }

    return backwardPawnScore;
  }

  private static boolean isIsBackward(Alliance alliance, Piece pawn, int[] lowestRankOnFile) {
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
    return isBackward;
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

    // Bonus for pawn chains
    pawnChainScore += chainLinks * 8;

    // Extra bonus for longer chains
    if (chainLinks >= 3) {
      pawnChainScore += 10;
    }

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
   * Evaluates central pawn control, which is important in the middlegame.
   */
  private double evaluateCentralPawnControl(final List<Piece> playerPawns) {
    double centralControlScore = 0;
    final int[] centralSquares = {27, 28, 35, 36}; // d5, e5, d4, e4

    // Direct control
    for (final Piece pawn : playerPawns) {
      final int position = pawn.getPiecePosition();

      // Pawns in the center
      for (final int centralSquare : centralSquares) {
        if (position == centralSquare) {
          centralControlScore += 25;
        }
      }

      // Pawns controlling central squares
      for (final int centralSquare : centralSquares) {
        if (pawnControlsSquare(pawn, centralSquare)) {
          centralControlScore += 15;
        }
      }
    }

    return centralControlScore;
  }

  /**
   * Checks if a pawn controls (attacks) a specific square.
   */
  private boolean pawnControlsSquare(final Piece pawn, final int square) {
    final int pawnPosition = pawn.getPiecePosition();
    final int pawnFile = pawnPosition % 8;
    final int pawnRank = pawnPosition / 8;
    final int squareFile = square % 8;
    final int squareRank = square / 8;

    // Check if pawn attacks the square
    if (pawn.getPieceAllegiance().isWhite()) {
      return (pawnRank - 1 == squareRank) &&
              (pawnFile - 1 == squareFile || pawnFile + 1 == squareFile);
    } else {
      return (pawnRank + 1 == squareRank) &&
              (pawnFile - 1 == squareFile || pawnFile + 1 == squareFile);
    }
  }

  /**
   * Evaluates piece coordination, synergy and cooperative control.
   */
  private double pieceCoordinationEvaluation(final Player player, final Board board) {
    double coordinationScore = 0;
    final Collection<Piece> playerPieces = player.getActivePieces();
    final Collection<Move> playerMoves = player.getLegalMoves();

    // Evaluate bishop pair (powerful in the middlegame)
    coordinationScore += evaluateBishopPair(playerPieces);

    // Evaluate rook coordination (connected rooks, rooks on 7th)
    coordinationScore += evaluateRookCoordination(playerPieces, board);

    // Evaluate knight outposts
    coordinationScore += evaluateKnightOutposts(playerPieces, getPlayerPawns(player), getPlayerPawns(player.getOpponent()));

    // Evaluate piece protection
    coordinationScore += evaluatePieceProtection(playerPieces, playerMoves);

    // Evaluate piece activity (centralized pieces)
    coordinationScore += evaluatePieceActivity(playerPieces);

    return coordinationScore;
  }

  /**
   * Evaluates bishop pair bonus, which is significant in middlegame.
   */
  private double evaluateBishopPair(final Collection<Piece> playerPieces) {
    double bishopPairScore = 0;
    boolean hasLightSquareBishop = false;
    boolean hasDarkSquareBishop = false;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        final int square = piece.getPiecePosition();
        final boolean isLightSquare = ((square / 8) + (square % 8)) % 2 == 0;

        if (isLightSquare) {
          hasLightSquareBishop = true;
        } else {
          hasDarkSquareBishop = true;
        }
      }
    }

    // Bishop pair bonus
    if (hasLightSquareBishop && hasDarkSquareBishop) {
      bishopPairScore += 50;
    }

    return bishopPairScore;
  }

  /**
   * Evaluates rook coordination, including connected rooks and rooks on the 7th rank.
   */
  private double evaluateRookCoordination(final Collection<Piece> playerPieces, final Board board) {
    double rookScore = 0;
    List<Piece> rooks = new ArrayList<>();

    // Collect rooks
    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.ROOK) {
        rooks.add(piece);
      }
    }

    if (rooks.size() >= 2) {
      // Check for rooks on the same file or rank
      boolean rooksConnected = false;
      for (int i = 0; i < rooks.size() - 1; i++) {
        for (int j = i + 1; j < rooks.size(); j++) {
          final int rookPos1 = rooks.get(i).getPiecePosition();
          final int rookPos2 = rooks.get(j).getPiecePosition();

          // Rooks on same rank
          if (rookPos1 / 8 == rookPos2 / 8) {
            rookScore += 20;
            rooksConnected = true;
          }

          // Rooks on same file
          if (rookPos1 % 8 == rookPos2 % 8) {
            rookScore += 15;
            rooksConnected = true;
          }
        }
      }

      // Connected rooks bonus
      if (rooksConnected) {
        rookScore += 10;
      }
    }

    // Rooks on 7th rank (or 2nd for black)
    for (final Piece rook : rooks) {
      final int rookRank = rook.getPiecePosition() / 8;

      if ((rook.getPieceAllegiance().isWhite() && rookRank == 1) ||
              (!rook.getPieceAllegiance().isWhite() && rookRank == 6)) {
        rookScore += 30;

        // Extra bonus if opponent king is on the back rank
        final King opponentKing = board.currentPlayer().getOpponent().getPlayerKing();
        final int kingRank = opponentKing.getPiecePosition() / 8;

        if ((opponentKing.getPieceAllegiance().isWhite() && kingRank == 7) ||
                (!opponentKing.getPieceAllegiance().isWhite() && kingRank == 0)) {
          rookScore += 15;
        }
      }
    }

    // Rooks on open files
    for (final Piece rook : rooks) {
      final int rookFile = rook.getPiecePosition() % 8;

      if (isOpenFile(rookFile, board)) {
        rookScore += 20;
      } else if (isSemiOpenFile(rookFile, board, rook.getPieceAllegiance())) {
        rookScore += 10;
      }
    }

    return rookScore;
  }

  /**
   * Checks if a file is completely open (no pawns on it).
   */
  private boolean isOpenFile(final int file, final Board board) {
    for (int rank = 0; rank < 8; rank++) {
      final Piece piece = board.getPiece(rank * 8 + file);
      if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if a file is semi-open (no friendly pawns on it).
   */
  private boolean isSemiOpenFile(final int file, final Board board, final Alliance alliance) {
    for (int rank = 0; rank < 8; rank++) {
      final Piece piece = board.getPiece(rank * 8 + file);
      if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN &&
              piece.getPieceAllegiance() == alliance) {
        return false;
      }
    }
    return true;
  }

  /**
   * Evaluates knight outposts - knights protected by pawns and in opponent's territory.
   */
  private double evaluateKnightOutposts(final Collection<Piece> playerPieces,
                                        final List<Piece> playerPawns,
                                        final List<Piece> opponentPawns) {
    double outpostScore = 0;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.KNIGHT) {
        final int position = piece.getPiecePosition();
        final int file = position % 8;
        final int rank = position / 8;
        final Alliance alliance = piece.getPieceAllegiance();

        // Check if knight is in opponent's half
        boolean inOpponentTerritory = (alliance.isWhite() && rank < 4) ||
                (!alliance.isWhite() && rank > 3);

        if (inOpponentTerritory) {
          // Check if knight can be attacked by opponent pawns
          boolean canBeAttackedByPawn = false;

          for (final Piece opponentPawn : opponentPawns) {
            if (pawnControlsSquare(opponentPawn, position)) {
              canBeAttackedByPawn = true;
              break;
            }
          }

          if (!canBeAttackedByPawn) {
            outpostScore += 20;

            // Extra bonus if protected by friendly pawn
            for (final Piece playerPawn : playerPawns) {
              if (pawnControlsSquare(playerPawn, position)) {
                outpostScore += 15;
                break;
              }
            }

            // Central outposts are better
            if (file >= 2 && file <= 5) {
              outpostScore += 10;
            }
          }
        }
      }
    }

    return outpostScore;
  }

  /**
   * Evaluates how well pieces protect each other.
   */
  private double evaluatePieceProtection(final Collection<Piece> playerPieces,
                                         final Collection<Move> playerMoves) {
    double protectionScore = 0;
    Map<Integer, Integer> squareProtectionCount = new HashMap<>();

    // Count protections for each square
    for (final Move move : playerMoves) {
      final int destination = move.getDestinationCoordinate();
      squareProtectionCount.put(destination,
              squareProtectionCount.getOrDefault(destination, 0) + 1);
    }

    // Score for protected pieces
    for (final Piece piece : playerPieces) {
      final int position = piece.getPiecePosition();
      final int protectionCount = squareProtectionCount.getOrDefault(position, 0);

      if (protectionCount > 0) {
        // Base protection bonus
        protectionScore += 5;

        // Extra bonus for protecting valuable pieces
        if (piece.getPieceType() == Piece.PieceType.QUEEN) {
          protectionScore += 5 * protectionCount;
        } else if (piece.getPieceType() == Piece.PieceType.ROOK) {
          protectionScore += 3 * protectionCount;
        }
      }
    }

    return protectionScore;
  }

  /**
   * Evaluates piece activity, particularly centralization of pieces.
   */
  private double evaluatePieceActivity(final Collection<Piece> playerPieces) {
    double activityScore = 0;

    for (final Piece piece : playerPieces) {
      final int position = piece.getPiecePosition();
      final int file = position % 8;
      final int rank = position / 8;

      // Evaluate piece centralization
      int fileDistance = Math.min(file, 7 - file);
      int rankDistance = Math.min(rank, 7 - rank);
      int distanceFromCenter = fileDistance + rankDistance;

      switch (piece.getPieceType()) {
        case KNIGHT:
          // Knights benefit greatly from central positions
          activityScore += (6 - distanceFromCenter) * 5;
          break;
        case BISHOP:
          // Bishops benefit from centralization and long diagonals
          activityScore += (6 - distanceFromCenter) * 4;
          break;
        case ROOK:
          // Rooks benefit more from central files than ranks
          activityScore += (3 - fileDistance) * 3;
          break;
        case QUEEN:
          // Queens benefit from centralization but should not be exposed
          activityScore += (6 - distanceFromCenter) * 2;
          break;
      }
    }

    return activityScore;
  }

  /**
   * Evaluates space control - territory and mobility advantages.
   */
  private double spaceControlEvaluation(final Player player) {
    double spaceScore = 0;
    final Collection<Move> playerMoves = player.getLegalMoves();
    final Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();
    final Alliance alliance = player.getAlliance();

    // Space advantage is measured as control of squares in the center and opponent's half
    int spacePiecesControl = 0;
    Map<Integer, Integer> controlledSquares = new HashMap<>();

    // Count control of each square
    for (final Move move : playerMoves) {
      final int destination = move.getDestinationCoordinate();
      controlledSquares.put(destination,
              controlledSquares.getOrDefault(destination, 0) + 1);

      // Squares in opponent's half or center are more valuable
      final int rank = destination / 8;
      final int file = destination % 8;

      if ((alliance.isWhite() && rank < 4) || (!alliance.isWhite() && rank > 3)) {
        spacePiecesControl++;
      }

      // Central squares control
      if (file >= 2 && file <= 5 && rank >= 2 && rank <= 5) {
        spacePiecesControl++;
      }
    }

    // Calculate opponent's space control
    int opponentSpaceControl = 0;
    for (final Move move : opponentMoves) {
      final int destination = move.getDestinationCoordinate();
      final int rank = destination / 8;
      final int file = destination % 8;

      if ((alliance.isWhite() && rank > 3) || (!alliance.isWhite() && rank < 4)) {
        opponentSpaceControl++;
      }

      // Central squares control
      if (file >= 2 && file <= 5 && rank >= 2 && rank <= 5) {
        opponentSpaceControl++;
      }
    }

    // Space advantage score
    spaceScore += (spacePiecesControl - opponentSpaceControl) * 0.5;

    // Mobility space control
    spaceScore += Math.min(playerMoves.size() - opponentMoves.size(), 10) * 3;

    // Control of key squares (outposts, center)
    final int[] keySquares = {27, 28, 35, 36}; // d5, e5, d4, e4
    for (final int square : keySquares) {
      spaceScore += controlledSquares.getOrDefault(square, 0) * 5;
    }

    return spaceScore;
  }

  /**
   * Evaluates the attacking potential against the opponent's king.
   */
  private double attackingPotentialEvaluation(final Player player) {
    double attackScore = 0;
    final Collection<Move> playerMoves = player.getLegalMoves();
    final King opponentKing = player.getOpponent().getPlayerKing();
    final int kingPosition = opponentKing.getPiecePosition();

    // Count attacking pieces
    int attackingPiecesCount = 0;
    int attackingSquaresCount = 0;

    // Track which piece types are participating in the attack
    boolean queenAttacking = false;
    boolean rookAttacking = false;
    boolean bishopAttacking = false;
    boolean knightAttacking = false;
    boolean pawnAttacking = false;

    for (final Move move : playerMoves) {
      final int destination = move.getDestinationCoordinate();
      final Piece piece = move.getMovedPiece();

      // Check if piece attacks near the king
      if (isSquareNearKing(kingPosition, destination)) {
        attackingSquaresCount++;

        // Count unique attacking pieces
        switch (piece.getPieceType()) {
          case QUEEN:
            if (!queenAttacking) {
              attackingPiecesCount++;
              queenAttacking = true;
            }
            break;
          case ROOK:
            if (!rookAttacking) {
              attackingPiecesCount++;
              rookAttacking = true;
            }
            break;
          case BISHOP:
            if (!bishopAttacking) {
              attackingPiecesCount++;
              bishopAttacking = true;
            }
            break;
          case KNIGHT:
            if (!knightAttacking) {
              attackingPiecesCount++;
              knightAttacking = true;
            }
            break;
          case PAWN:
            if (!pawnAttacking) {
              attackingPiecesCount++;
              pawnAttacking = true;
            }
            break;
        }
      }
    }

    // Attack potential score - based on Stockfish's king safety evaluation
    if (attackingPiecesCount >= 2) {
      // Base attack score
      attackScore += 20 * attackingPiecesCount;

      // Bonus for squares attacked near king
      attackScore += 10 * attackingSquaresCount;

      // Weighted attack value by piece type
      if (queenAttacking) attackScore += 40;
      if (rookAttacking) attackScore += 30;
      if (bishopAttacking) attackScore += 20;
      if (knightAttacking) attackScore += 15;
      if (pawnAttacking) attackScore += 10;

      // Attack safety check
      final boolean opponentKingCastled = opponentKing.isCastled();
      final boolean opponentChecked = player.getOpponent().isInCheck();

      if (opponentChecked) {
        attackScore += 50;
      }

      if (!opponentKingCastled) {
        attackScore += 30;
      }

      // Safety check - can the attack be easily defended?
      final int defendingPiecesCount = countDefendingPieces(player.getOpponent(), kingPosition);
      attackScore -= defendingPiecesCount * 15;
    }

    return attackScore;
  }

  /**
   * Counts pieces that can help defend the king.
   */
  private int countDefendingPieces(final Player player, final int kingPosition) {
    int defendingCount = 0;

    for (final Piece piece : player.getActivePieces()) {
      if (piece.getPieceType() == Piece.PieceType.KING) {
        continue;
      }

      final int piecePosition = piece.getPiecePosition();

      // Check if piece is near the king
      if (isSquareNearKing(kingPosition, piecePosition)) {
        defendingCount++;
      }
    }

    return defendingCount;
  }

  /**
   * Evaluates special positional patterns that are important in the middlegame.
   */
  private double specialPatternsEvaluation(final Player player, final Board board) {
    double patternScore = 0;
    final Collection<Piece> playerPieces = player.getActivePieces();
    final Alliance alliance = player.getAlliance();

    // Rooks on 7th rank
    patternScore += evaluateRooksOn7thRank(playerPieces, board, alliance);

    // Fianchettoed bishops
    patternScore += evaluateFianchetto(playerPieces, alliance);

    // Bad bishops (bishops blocked by own pawns)
    patternScore += evaluateBadBishops(playerPieces, getPlayerPawns(player));

    // Knight outposts
    patternScore += evaluateKnightOutposts(playerPieces, getPlayerPawns(player), getPlayerPawns(player.getOpponent()));

    // Queen positions - avoid early centralization
    patternScore += evaluateQueenPositioning(playerPieces, board, alliance);

    return patternScore;
  }

  /**
   * Evaluates rooks on the 7th rank (or 2nd for black), which is often very strong.
   */
  private double evaluateRooksOn7thRank(final Collection<Piece> playerPieces,
                                        final Board board,
                                        final Alliance alliance) {
    double rookScore = 0;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.ROOK) {
        final int position = piece.getPiecePosition();
        final int rank = position / 8;

        // Check if rook is on 7th (2nd for black)
        if ((alliance.isWhite() && rank == 1) || (!alliance.isWhite() && rank == 6)) {
          rookScore += 30;

          // Extra bonus if there are enemy pawns on the 7th/2nd rank
          final Collection<Piece> opponentPieces = alliance.isWhite() ?
                  board.getBlackPieces() :
                  board.getWhitePieces();

          for (final Piece opponentPiece : opponentPieces) {
            if (opponentPiece.getPieceType() == Piece.PieceType.PAWN) {
              final int opponentRank = opponentPiece.getPiecePosition() / 8;

              if ((alliance.isWhite() && opponentRank == 1) ||
                      (!alliance.isWhite() && opponentRank == 6)) {
                rookScore += 10;
              }
            }
          }

          // Check if opponent king is trapped on the back rank
          final King opponentKing = board.currentPlayer().getOpponent().getPlayerKing();
          final int kingRank = opponentKing.getPiecePosition() / 8;

          if ((alliance.isWhite() && kingRank == 0) || (!alliance.isWhite() && kingRank == 7)) {
            rookScore += 20;
          }
        }
      }
    }

    return rookScore;
  }

  /**
   * Evaluates fianchettoed bishops (bishops in the corner of a pawn structure).
   */
  private double evaluateFianchetto(final Collection<Piece> playerPieces, final Alliance alliance) {
    double fianchettoScore = 0;

    // Kingside fianchetto positions
    final int kingsideBishopPosition = alliance.isWhite() ? 62 : 6;

    // Queenside fianchetto positions
    final int queensideBishopPosition = alliance.isWhite() ? 57 : 1;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        final int position = piece.getPiecePosition();

        if (position == kingsideBishopPosition) {
          fianchettoScore += 20;
        } else if (position == queensideBishopPosition) {
          fianchettoScore += 15;
        }
      }
    }

    return fianchettoScore;
  }

  /**
   * Evaluates bad bishops - bishops blocked by their own pawns.
   */
  private double evaluateBadBishops(final Collection<Piece> playerPieces,
                                    final List<Piece> playerPawns) {
    double badBishopScore = 0;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        int pawnsOnSameColor = getPawnsOnSameColor(playerPawns, piece);

        // Penalty based on number of blocking pawns
        if (pawnsOnSameColor >= 3) {
          badBishopScore -= 20;
        } else if (pawnsOnSameColor == 2) {
          badBishopScore -= 10;
        }
      }
    }

    return badBishopScore;
  }

  private static int getPawnsOnSameColor(List<Piece> playerPawns, Piece piece) {
    final int position = piece.getPiecePosition();
    final boolean isLightSquare = ((position / 8) + (position % 8)) % 2 == 0;

    // Count pawns on same color squares
    int pawnsOnSameColor = 0;

    for (final Piece pawn : playerPawns) {
      final int pawnPosition = pawn.getPiecePosition();
      final boolean isPawnOnLightSquare = ((pawnPosition / 8) + (pawnPosition % 8)) % 2 == 0;

      if (isLightSquare == isPawnOnLightSquare) {
        pawnsOnSameColor++;
      }
    }
    return pawnsOnSameColor;
  }

  /**
   * Evaluates queen positioning in the middlegame.
   */
  private double evaluateQueenPositioning(final Collection<Piece> playerPieces,
                                          final Board board,
                                          final Alliance alliance) {
    double queenScore = 0;

    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.QUEEN) {
        final int position = piece.getPiecePosition();
        final int rank = position / 8;
        final int file = position % 8;

        // Score for queen centralization (modest bonus)
        if (file >= 2 && file <= 5 && rank >= 2 && rank <= 5) {
          queenScore += 10;
        }

        // Penalty for queen too far advanced without support
        boolean queenTooAdvanced = false;
        if (alliance.isWhite() && rank < 2) {
          queenTooAdvanced = true;
        } else if (!alliance.isWhite() && rank > 5) {
          queenTooAdvanced = true;
        }

        if (queenTooAdvanced) {
          // Check if queen has support
          int supportingPieces = getSupportingPieces(playerPieces, alliance, rank);

          if (supportingPieces < 2) {
            queenScore -= 25;
          }
        }

        // Bonus for queen near opponent king
        final King opponentKing = board.currentPlayer().getOpponent().getPlayerKing();
        final int kingPosition = opponentKing.getPiecePosition();

        final int distance = calculateChebyshevDistance(position, kingPosition);
        if (distance <= 2) {
          queenScore += 20;
        } else if (distance == 3) {
          queenScore += 10;
        }
      }
    }

    return queenScore;
  }

  private static int getSupportingPieces(Collection<Piece> playerPieces, Alliance alliance, int rank) {
    int supportingPieces = 0;
    for (final Piece supportPiece : playerPieces) {
      if (supportPiece.getPieceType() != Piece.PieceType.QUEEN &&
              supportPiece.getPieceType() != Piece.PieceType.KING) {
        final int supportPosition = supportPiece.getPiecePosition();
        final int supportRank = supportPosition / 8;

        if ((alliance.isWhite() && supportRank <= rank) ||
                (!alliance.isWhite() && supportRank >= rank)) {
          supportingPieces++;
        }
      }
    }
    return supportingPieces;
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