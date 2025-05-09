package engine.forPlayer.forAI;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forPiece.Piece;
import engine.forPiece.Piece.PieceType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An improved tapered evaluator that uses a single unified evaluation function
 * with phase-dependent weights instead of multiple separate evaluators.
 * This design significantly improves performance while maintaining evaluation accuracy.
 *
 * @author Aaron Ho
 */
public class TaperedEvaluator implements BoardEvaluator {

  /** Singleton instance of the TaperedEvaluator. */
  private static final TaperedEvaluator INSTANCE = new TaperedEvaluator();

  /** Game phase definitions - from opening (0) to endgame (256) */
  private static final int PHASE_OPENING = 0;
  private static final int PHASE_MIDDLEGAME = 128;
  private static final int PHASE_ENDGAME = 256;

  /** Phase weights for each piece type */
  private static final int PAWN_PHASE_VALUE = 0;
  private static final int KNIGHT_PHASE_VALUE = 1;
  private static final int BISHOP_PHASE_VALUE = 1;
  private static final int ROOK_PHASE_VALUE = 2;
  private static final int QUEEN_PHASE_VALUE = 4;

  /** Total phase weight at the start of the game */
  private static final int TOTAL_PHASE =
          4 * KNIGHT_PHASE_VALUE + 4 * BISHOP_PHASE_VALUE + 4 * ROOK_PHASE_VALUE + 2 * QUEEN_PHASE_VALUE;

  /** Material values for piece types in different phases [opening, endgame] */
  private static final int[][] PIECE_VALUES = {
          {100, 120},     // PAWN
          {310, 290},     // KNIGHT
          {320, 330},     // BISHOP
          {500, 530},     // ROOK
          {900, 900},     // QUEEN
          {10000, 10000}  // KING
  };

  /** Component weights for different game phases [opening, endgame] */
  private static final double[] MATERIAL_WEIGHT = {1.0, 1.0};
  private static final double[] MOBILITY_WEIGHT = {0.7, 1.0};
  private static final double[] PAWN_STRUCTURE_WEIGHT = {0.8, 1.2};
  private static final double[] KING_SAFETY_WEIGHT = {1.5, 0.5};
  private static final double[] PIECE_ACTIVITY_WEIGHT = {1.0, 0.8};
  private static final double[] PASSED_PAWN_WEIGHT = {0.6, 1.5};
  private static final double[] CENTER_CONTROL_WEIGHT = {1.2, 0.6};
  private static final double[] PIECE_COORDINATION_WEIGHT = {0.8, 1.0};

  /** Private constructor to enforce singleton pattern */
  private TaperedEvaluator() {}

  /**
   * Returns the singleton instance of the TaperedEvaluator.
   *
   * @return The TaperedEvaluator instance
   */
  public static TaperedEvaluator get() {
    return INSTANCE;
  }

  /**
   * Evaluates the board position using a single evaluation function with
   * phase-dependent weights.
   *
   * @param board The current chess board position
   * @param depth The current search depth
   * @return The evaluation score
   */
  @Override
  public double evaluate(final Board board, final int depth) {
    // Special case: if the game is over, return immediate result
    if (BoardUtils.isEndOfGame(board)) {
      if (board.currentPlayer().isInCheckMate()) {
        return board.currentPlayer().getAlliance().isWhite() ? -9000 : 9000;
      } else {
        return 0; // Draw
      }
    }

    // Calculate the current game phase (0 = opening, 256 = endgame)
    int phase = calculatePhase(board);

    // Calculate the evaluation from white's perspective
    double whiteScore = evaluatePlayer(board, Alliance.WHITE, phase);
    double blackScore = evaluatePlayer(board, Alliance.BLACK, phase);

    // Return the difference (positive favors white, negative favors black)
    return whiteScore - blackScore;
  }

  /**
   * Evaluates the position from one player's perspective.
   *
   * @param board The current chess board
   * @param alliance The player's alliance (WHITE or BLACK)
   * @param phase The current game phase (0-256)
   * @return The evaluation score for the player
   */
  private double evaluatePlayer(final Board board, final Alliance alliance, final int phase) {
    // Calculate component scores
    double materialScore = evaluateMaterial(board, alliance, phase);
    double mobilityScore = evaluateMobility(board, alliance, phase);
    double pawnStructureScore = evaluatePawnStructure(board, alliance, phase);
    double kingSafetyScore = evaluateKingSafety(board, alliance, phase);
    double pieceActivityScore = evaluatePieceActivity(board, alliance, phase);
    double passedPawnScore = evaluatePassedPawns(board, alliance, phase);
    double centerControlScore = evaluateCenterControl(board, alliance, phase);
    double pieceCoordinationScore = evaluatePieceCoordination(board, alliance, phase);

    // Calculate phase interpolation factor (0.0 = opening, 1.0 = endgame)
    double phaseFactor = phase / (double) PHASE_ENDGAME;

    // Apply phase-dependent weights
    double weightedMaterialScore = materialScore * interpolate(MATERIAL_WEIGHT, phaseFactor);
    double weightedMobilityScore = mobilityScore * interpolate(MOBILITY_WEIGHT, phaseFactor);
    double weightedPawnStructureScore = pawnStructureScore * interpolate(PAWN_STRUCTURE_WEIGHT, phaseFactor);
    double weightedKingSafetyScore = kingSafetyScore * interpolate(KING_SAFETY_WEIGHT, phaseFactor);
    double weightedPieceActivityScore = pieceActivityScore * interpolate(PIECE_ACTIVITY_WEIGHT, phaseFactor);
    double weightedPassedPawnScore = passedPawnScore * interpolate(PASSED_PAWN_WEIGHT, phaseFactor);
    double weightedCenterControlScore = centerControlScore * interpolate(CENTER_CONTROL_WEIGHT, phaseFactor);
    double weightedPieceCoordinationScore = pieceCoordinationScore * interpolate(PIECE_COORDINATION_WEIGHT, phaseFactor);

    // Sum all component scores
    return weightedMaterialScore +
            weightedMobilityScore +
            weightedPawnStructureScore +
            weightedKingSafetyScore +
            weightedPieceActivityScore +
            weightedPassedPawnScore +
            weightedCenterControlScore +
            weightedPieceCoordinationScore;
  }

  /**
   * Calculates the current game phase based on the material on the board.
   * Returns a value from 0 (opening) to 256 (endgame).
   *
   * @param board The current chess board
   * @return The game phase (0-256)
   */
  private int calculatePhase(final Board board) {
    int phase = TOTAL_PHASE;

    // Calculate remaining material with phase values
    for (final Piece piece : board.getAllPieces()) {
      switch (piece.getPieceType()) {
        case PAWN -> phase -= PAWN_PHASE_VALUE;
        case KNIGHT -> phase -= KNIGHT_PHASE_VALUE;
        case BISHOP -> phase -= BISHOP_PHASE_VALUE;
        case ROOK -> phase -= ROOK_PHASE_VALUE;
        case QUEEN -> phase -= QUEEN_PHASE_VALUE;
      }
    }

    // Convert to 0-256 range where 0 is opening and 256 is endgame
    phase = (phase * 256 + (TOTAL_PHASE / 2)) / TOTAL_PHASE;

    // Basic adjustment for development status
    if (phase < 64) {
      boolean whiteKingCastled = board.whitePlayer().getPlayerKing().isCastled();
      boolean blackKingCastled = board.blackPlayer().getPlayerKing().isCastled();

      if (!whiteKingCastled || !blackKingCastled) {
        phase = Math.max(0, phase - 16);
      }
    }

    return Math.min(256, Math.max(0, phase));
  }

  /**
   * Interpolates between opening and endgame values based on game phase.
   *
   * @param values Array containing [opening, endgame] values
   * @param phaseFactor Phase factor from 0.0 (opening) to 1.0 (endgame)
   * @return Interpolated value
   */
  private double interpolate(double[] values, double phaseFactor) {
    return values[0] * (1 - phaseFactor) + values[1] * phaseFactor;
  }

  /**
   * Evaluates material balance for a player.
   */
  private double evaluateMaterial(Board board, Alliance alliance, int phase) {
    double materialScore = 0;
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    // Count pieces by type
    int[] pieceCounts = new int[6];
    for (Piece piece : pieces) {
      pieceCounts[piece.getPieceType().ordinal()]++;

      // Get phase-adjusted material value
      double phaseFactor = phase / (double) PHASE_ENDGAME;
      int pieceValue = (int) interpolate(
              new double[] {PIECE_VALUES[piece.getPieceType().ordinal()][0],
                      PIECE_VALUES[piece.getPieceType().ordinal()][1]},
              phaseFactor);

      materialScore += pieceValue;
    }

    // Bishop pair bonus
    if (pieceCounts[PieceType.BISHOP.ordinal()] >= 2) {
      materialScore += 45; // Bishop pair bonus
    }

    return materialScore;
  }

  /**
   * Evaluates piece mobility for a player.
   */
  private double evaluateMobility(Board board, Alliance alliance, int phase) {
    double mobilityScore = 0;
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    // Skip if in opening with undeveloped minor pieces to save calculation time
    if (phase < 64 && isOpeningWithUndevelopedMinors(board, alliance)) {
      return 0;
    }

    // Count legal moves by piece type with weighted values
    Map<PieceType, Double> mobilityWeights = new HashMap<>();
    mobilityWeights.put(PieceType.KNIGHT, 3.0);
    mobilityWeights.put(PieceType.BISHOP, 3.0);
    mobilityWeights.put(PieceType.ROOK, 2.0);
    mobilityWeights.put(PieceType.QUEEN, 1.0);

    for (Piece piece : pieces) {
      if (piece.getPieceType() == PieceType.KING || piece.getPieceType() == PieceType.PAWN) {
        continue; // Skip kings and pawns
      }

      int moveCount = piece.calculateLegalMoves(board).size();

      if (mobilityWeights.containsKey(piece.getPieceType())) {
        mobilityScore += moveCount * mobilityWeights.get(piece.getPieceType());
      }
    }

    return mobilityScore;
  }

  /**
   * Checks if the position is in opening with undeveloped minor pieces.
   */
  private boolean isOpeningWithUndevelopedMinors(Board board, Alliance alliance) {
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    int developedMinorPieces = 0;
    for (Piece piece : pieces) {
      if ((piece.getPieceType() == PieceType.KNIGHT || piece.getPieceType() == PieceType.BISHOP)
              && !piece.isFirstMove()) {
        developedMinorPieces++;
      }
    }

    return developedMinorPieces < 2;
  }

  /**
   * Evaluates pawn structure for a player.
   */
  private double evaluatePawnStructure(Board board, Alliance alliance, int phase) {
    double pawnStructureScore = 0;
    List<Piece> pawns = getPawns(board, alliance);
    List<Piece> opponentPawns = getPawns(board, alliance.equals(Alliance.WHITE) ?
            Alliance.BLACK : Alliance.WHITE);

    // Skip detailed pawn structure analysis in pure endgames
    if (phase > 200 && pawns.size() + opponentPawns.size() < 5) {
      return 0;
    }

    // Evaluate doubled pawns (penalty)
    pawnStructureScore += evaluateDoubledPawns(pawns, phase);

    // Evaluate isolated pawns (penalty)
    pawnStructureScore += evaluateIsolatedPawns(pawns, phase);

    // Evaluate pawn chains (bonus)
    pawnStructureScore += evaluatePawnChains(pawns, phase);

    // Evaluate pawn islands (penalty for multiple islands)
    pawnStructureScore += evaluatePawnIslands(pawns, phase);

    return pawnStructureScore;
  }

  /**
   * Evaluates doubled pawns (penalty).
   */
  private double evaluateDoubledPawns(List<Piece> pawns, int phase) {
    double doubledPawnScore = 0;

    // Count pawns on each file
    int[] pawnsPerFile = new int[8];
    for (Piece pawn : pawns) {
      pawnsPerFile[pawn.getPiecePosition() % 8]++;
    }

    // Penalty for doubled pawns (more severe in endgame)
    double phaseFactor = phase / (double) PHASE_ENDGAME;
    double doubledPenalty = interpolate(new double[] {-20, -25}, phaseFactor);

    for (int count : pawnsPerFile) {
      if (count > 1) {
        doubledPawnScore += doubledPenalty * (count - 1);
      }
    }

    return doubledPawnScore;
  }

  /**
   * Evaluates isolated pawns (penalty).
   */
  private double evaluateIsolatedPawns(List<Piece> pawns, int phase) {
    double isolatedPawnScore = 0;

    // Mark files with pawns
    boolean[] filesWithPawns = new boolean[8];
    for (Piece pawn : pawns) {
      filesWithPawns[pawn.getPiecePosition() % 8] = true;
    }

    // Interpolate penalty based on phase
    double phaseFactor = phase / (double) PHASE_ENDGAME;
    double isolatedPenalty = interpolate(new double[] {-15, -20}, phaseFactor);

    // Check for isolated pawns
    for (Piece pawn : pawns) {
      final int pawnFile = pawn.getPiecePosition() % 8;
      boolean isIsolated = (pawnFile == 0 || !filesWithPawns[pawnFile - 1]) &&
              (pawnFile == 7 || !filesWithPawns[pawnFile + 1]);

      if (isIsolated) {
        isolatedPawnScore += isolatedPenalty;
      }
    }

    return isolatedPawnScore;
  }

  /**
   * Evaluates pawn chains (connected pawns that protect each other).
   */
  private double evaluatePawnChains(List<Piece> pawns, int phase) {
    double pawnChainScore = 0;

    // Simplified pawn chain detection to save computation time
    Map<Integer, List<Piece>> pawnsByFile = new HashMap<>();

    // Group pawns by file
    for (Piece pawn : pawns) {
      int file = pawn.getPiecePosition() % 8;
      if (!pawnsByFile.containsKey(file)) {
        pawnsByFile.put(file, new ArrayList<>());
      }
      pawnsByFile.get(file).add(pawn);
    }

    // Find adjacent files with pawns (simple chain detection)
    for (int file = 0; file < 7; file++) {
      if (pawnsByFile.containsKey(file) && pawnsByFile.containsKey(file + 1)) {
        // Pawns on adjacent files
        double chainBonus = phase < 128 ? 8 : 5; // Less important in endgame
        pawnChainScore += chainBonus;
      }
    }

    return pawnChainScore;
  }

  /**
   * Evaluates pawn islands (groups of pawns separated by files with no pawns).
   */
  private double evaluatePawnIslands(List<Piece> pawns, int phase) {
    double islandScore = 0;

    // Mark files with pawns
    boolean[] filesWithPawns = new boolean[8];
    for (Piece pawn : pawns) {
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
      double phaseFactor = phase / (double) PHASE_ENDGAME;
      double islandPenalty = interpolate(new double[] {-10, -15}, phaseFactor);
      islandScore += islandPenalty * (islands - 1);
    }

    return islandScore;
  }

  /**
   * Evaluates king safety for a player.
   */
  private double evaluateKingSafety(Board board, Alliance alliance, int phase) {
    double kingSafetyScore = 0;

    // Skip detailed king safety analysis in endgame
    if (phase > 200) {
      return 0;
    }

    // Get king
    Piece king = alliance == Alliance.WHITE ?
            board.whitePlayer().getPlayerKing() : board.blackPlayer().getPlayerKing();
    int kingPosition = king.getPiecePosition();

    // Castling bonus - more important in opening/middlegame
    if (board.currentPlayer().isCastled()) {
      double phaseFactor = phase / (double) PHASE_ENDGAME;
      double castlingBonus = interpolate(new double[] {60, 10}, phaseFactor);
      kingSafetyScore += castlingBonus;

      // Pawn shield bonus
      kingSafetyScore += evaluatePawnShield(board, alliance, kingPosition, phase);
    } else {
      // Penalty for uncastled king in opening/middlegame
      if (phase < 128) {
        kingSafetyScore -= 30;
      }
    }

    // King exposure penalty
    kingSafetyScore -= evaluateKingExposure(board, alliance, kingPosition, phase);

    return kingSafetyScore;
  }

  /**
   * Evaluates pawn shield around the king.
   */
  private double evaluatePawnShield(Board board, Alliance alliance, int kingPosition, int phase) {
    double shieldScore = 0;
    List<Piece> pawns = getPawns(board, alliance);

    // Determine king side
    int kingFile = kingPosition % 8;
    boolean isKingSide = kingFile >= 4;

    // Define shield area based on castling side
    List<Integer> shieldOffsets = new ArrayList<>();
    if (isKingSide) {
      // King side shield
      shieldOffsets.addAll(Arrays.asList(-9, -8, -7));
    } else {
      // Queen side shield
      shieldOffsets.addAll(Arrays.asList(-9, -8, -7));
    }

    // Count pawns in shield area
    int pawnsInShield = 0;
    for (int offset : shieldOffsets) {
      int shieldPos = kingPosition + offset;
      if (shieldPos >= 0 && shieldPos < 64) {
        for (Piece pawn : pawns) {
          if (pawn.getPiecePosition() == shieldPos) {
            pawnsInShield++;
            break;
          }
        }
      }
    }

    // Score based on shield integrity
    if (pawnsInShield >= 3) {
      shieldScore += 30; // Perfect shield
    } else if (pawnsInShield == 2) {
      shieldScore += 15; // Good shield
    } else if (pawnsInShield == 1) {
      shieldScore += 5;  // Partial shield
    }

    return shieldScore;
  }

  /**
   * Evaluates king exposure to attacks.
   */
  private double evaluateKingExposure(Board board, Alliance alliance, int kingPosition, int phase) {
    double exposureScore = 0;

    // No need for detailed exposure evaluation in endgame
    if (phase > 200) {
      return 0;
    }

    Alliance opponentAlliance = alliance.equals(Alliance.WHITE) ? Alliance.BLACK : Alliance.WHITE;
    Collection<Piece> opponentPieces = opponentAlliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    // Count opponent pieces that could threaten the king
    for (Piece piece : opponentPieces) {
      if (piece.getPieceType() == PieceType.KING) {
        continue; // Skip opponent's king
      }

      // Calculate distance to king
      int distance = calculateChebyshevDistance(kingPosition, piece.getPiecePosition());

      // Threat level based on piece type and distance
      if (distance <= 2) {
        switch (piece.getPieceType()) {
          case QUEEN:
            exposureScore += (3 - distance) * 8;
            break;
          case ROOK:
            exposureScore += (3 - distance) * 5;
            break;
          case BISHOP:
          case KNIGHT:
            exposureScore += (3 - distance) * 3;
            break;
          default:
            exposureScore += (3 - distance);
        }
      }
    }

    // Check if player is in check
    if ((alliance == Alliance.WHITE && board.whitePlayer().isInCheck()) ||
            (alliance == Alliance.BLACK && board.blackPlayer().isInCheck())) {
      exposureScore += 20;
    }

    return exposureScore;
  }

  /**
   * Evaluates piece activity and placement.
   */
  private double evaluatePieceActivity(Board board, Alliance alliance, int phase) {
    double activityScore = 0;
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    for (Piece piece : pieces) {
      if (piece.getPieceType() == PieceType.KING || piece.getPieceType() == PieceType.PAWN) {
        continue; // Evaluated separately
      }

      int position = piece.getPiecePosition();
      int file = position % 8;
      int rank = position / 8;

      // Adjust rank for black perspective
      if (alliance == Alliance.BLACK) {
        rank = 7 - rank;
      }

      // Distance from center
      int fileDistance = Math.min(file, 7 - file);
      int rankDistance = Math.min(rank, 7 - rank);
      int distanceFromCenter = fileDistance + rankDistance;

      // Piece-specific activity evaluation
      switch (piece.getPieceType()) {
        case KNIGHT:
          // Knights benefit from central positions
          activityScore += (6 - distanceFromCenter) * 3;
          break;

        case BISHOP:
          // Bishops benefit from diagonals
          activityScore += (6 - distanceFromCenter) * 2.5;

          // Bishop development bonus in opening
          if (phase < 64 && !piece.isFirstMove()) {
            activityScore += 15;
          }
          break;

        case ROOK:
          // Rooks on open files in middlegame/endgame
          if (phase > 64 && isOnOpenFile(board, position)) {
            activityScore += 20;
          }

          // Rooks on 7th rank in middlegame/endgame
          if (phase > 64 && ((alliance == Alliance.WHITE && rank == 1) ||
                  (alliance == Alliance.BLACK && rank == 6))) {
            activityScore += 25;
          }
          break;

        case QUEEN:
          // Queen activity - early development is penalized
          if (phase < 64 && !piece.isFirstMove()) {
            activityScore -= 20; // Penalty for early queen development
          } else {
            activityScore += (6 - distanceFromCenter) * 1.5;
          }
          break;
      }
    }

    return activityScore;
  }

  /**
   * Checks if a piece is on an open file (no pawns on the file).
   */
  private boolean isOnOpenFile(Board board, int position) {
    int file = position % 8;

    for (int rank = 0; rank < 8; rank++) {
      Piece piece = board.getPiece(rank * 8 + file);
      if (piece != null && piece.getPieceType() == PieceType.PAWN) {
        return false;
      }
    }

    return true;
  }

  /**
   * Evaluates passed pawns.
   */
  private double evaluatePassedPawns(Board board, Alliance alliance, int phase) {
    double passedPawnScore = 0;
    List<Piece> pawns = getPawns(board, alliance);
    List<Piece> opponentPawns = getPawns(board, alliance.equals(Alliance.WHITE) ?
            Alliance.BLACK : Alliance.WHITE);

    // Identify and evaluate passed pawns
    for (Piece pawn : pawns) {
      if (isPassedPawn(pawn, opponentPawns, alliance)) {
        int position = pawn.getPiecePosition();
        int rank = position / 8;

        // Adjust rank for perspective
        int rankFromPromotionLine = alliance == Alliance.WHITE ?
                7 - rank : rank;

        // Scale bonus based on advancement - higher in endgame
        double phaseFactor = phase / (double) PHASE_ENDGAME;
        double advancementBonus = interpolate(
                new double[] {20, 40}, phaseFactor) * (6 - rankFromPromotionLine) / 6.0;
        passedPawnScore += advancementBonus;

        // Additional bonus for very advanced passed pawns
        if (rankFromPromotionLine <= 2) {
          passedPawnScore += 30;
        }
      }
    }

    return passedPawnScore;
  }

  /**
   * Checks if a pawn is a passed pawn.
   */
  private boolean isPassedPawn(Piece pawn, List<Piece> opponentPawns, Alliance alliance) {
    if (pawn.getPieceType() != PieceType.PAWN) {
      return false;
    }

    int position = pawn.getPiecePosition();
    int file = position % 8;
    int rank = position / 8;

    // Direction to check
    int direction = alliance == Alliance.WHITE ? -1 : 1;

    // Check all squares in front and on adjacent files
    for (int r = rank + direction; r >= 0 && r < 8; r += direction) {
      // Check same file
      for (Piece opponentPawn : opponentPawns) {
        int oppPosition = opponentPawn.getPiecePosition();
        int oppFile = oppPosition % 8;
        int oppRank = oppPosition / 8;

        if (oppRank == r && (oppFile == file ||
                (Math.abs(oppFile - file) == 1))) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Evaluates center control.
   */
  private double evaluateCenterControl(Board board, Alliance alliance, int phase) {
    double centerScore = 0;

    // Center control less important in endgame
    if (phase > 200) {
      return 0;
    }

    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    // Central squares d4, e4, d5, e5
    int[] centralSquares = {27, 28, 35, 36};

    // Evaluate piece control of center
    for (Piece piece : pieces) {
      int position = piece.getPiecePosition();

      // Direct center occupation
      for (int centralSquare : centralSquares) {
        if (position == centralSquare) {
          // Pawn in center is especially valuable
          if (piece.getPieceType() == PieceType.PAWN) {
            centerScore += 25;
          } else {
            centerScore += 15;
          }
          break;
        }
      }

      // Count attacks on center
      if (phase < 128 && piece.getPieceType() != PieceType.KING &&
              piece.getPieceType() != PieceType.PAWN) {

        for (Move move : piece.calculateLegalMoves(board)) {
          int destination = move.getDestinationCoordinate();
          for (int centralSquare : centralSquares) {
            if (destination == centralSquare) {
              centerScore += 5;
              break;
            }
          }
        }
      }
    }

    return centerScore;
  }

  /**
   * Evaluates piece coordination.
   */
  private double evaluatePieceCoordination(Board board, Alliance alliance, int phase) {
    double coordinationScore = 0;
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    // Skip detailed coordination analysis in some positions to save time
    if (pieces.size() <= 4) {
      return 0;
    }

    // Protected pieces bonus
    Map<Integer, Integer> protectedSquares = new HashMap<>();

    // Count how many pieces protect each square
    for (Piece piece : pieces) {
      if (piece.getPieceType() == PieceType.KING) {
        continue;
      }

      Collection<Move> moves = piece.calculateLegalMoves(board);
      for (Move move : moves) {
        if (move.isAttack() && move.getAttackedPiece().getPieceAllegiance() != alliance) {
          continue; // Skip attacks on opponent pieces
        }

        int destination = move.getDestinationCoordinate();
        protectedSquares.put(destination,
                protectedSquares.getOrDefault(destination, 0) + 1);
      }
    }

    // Score for protected pieces
    for (Piece piece : pieces) {
      int position = piece.getPiecePosition();
      int protectionCount = protectedSquares.getOrDefault(position, 0);

      if (protectionCount > 0) {
        // Base protection bonus
        coordinationScore += 4;

        // Extra bonus for valuable protected pieces
        if (piece.getPieceType() == PieceType.QUEEN) {
          coordinationScore += 4 * protectionCount;
        } else if (piece.getPieceType() == PieceType.ROOK) {
          coordinationScore += 2 * protectionCount;
        }
      }
    }

    // Connected rooks bonus in middlegame/endgame
    if (phase > 64) {
      List<Piece> rooks = pieces.stream()
              .filter(piece -> piece.getPieceType() == PieceType.ROOK)
              .collect(Collectors.toList());

      if (rooks.size() >= 2) {
        // Check for rooks on same rank
        for (int i = 0; i < rooks.size() - 1; i++) {
          for (int j = i + 1; j < rooks.size(); j++) {
            int pos1 = rooks.get(i).getPiecePosition();
            int pos2 = rooks.get(j).getPiecePosition();

            if (pos1 / 8 == pos2 / 8) {
              coordinationScore += 15; // Same rank
            } else if (pos1 % 8 == pos2 % 8) {
              coordinationScore += 15; // Same file
            }
          }
        }
      }
    }

    return coordinationScore;
  }

  /**
   * Gets a list of pawns for the specified alliance.
   */
  private List<Piece> getPawns(Board board, Alliance alliance) {
    Collection<Piece> pieces = alliance == Alliance.WHITE ?
            board.getWhitePieces() : board.getBlackPieces();

    return pieces.stream()
            .filter(piece -> piece.getPieceType() == PieceType.PAWN)
            .collect(Collectors.toList());
  }

  /**
   * Calculates the Chebyshev distance between two positions.
   */
  private int calculateChebyshevDistance(int pos1, int pos2) {
    int file1 = pos1 % 8;
    int rank1 = pos1 / 8;
    int file2 = pos2 % 8;
    int rank2 = pos2 / 8;

    return Math.max(Math.abs(file1 - file2), Math.abs(rank1 - rank2));
  }
}