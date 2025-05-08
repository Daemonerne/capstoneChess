package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forPiece.*;
import engine.forPlayer.Player;

import java.util.*;
import java.util.stream.Collectors;

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
   * This method incorporates modern engine evaluation principles for the opening phase.
   *
   * @param player The player for whom the board position is being evaluated.
   * @param board  The current state of the chess board.
   * @return       The evaluation score of the board from the perspective of the specified player.
   */
  @VisibleForTesting
  private double score(final Player player, final Board board) {
    return materialScore(player.getActivePieces()) +
            developmentScore(player, board) +
            centerControlScore(player, board) +
            kingSecurityScore(player, board) +
            pawnStructureScore(player, board) +
            spaceAndMobilityScore(player, board) +
            pieceCoordinationScore(player, board) +
            tempoScore(player, board);
  }

  /**
   * Calculates the material score based on piece values.
   */
  private double materialScore(final Collection<Piece> playerPieces) {
    double materialValue = 0;
    for (final Piece piece : playerPieces) {
      materialValue += piece.getPieceValue();
    }
    return materialValue;
  }

  /**
   * Evaluates piece development with higher penalties for undeveloped pieces
   * and bonuses for harmonious development.
   */
  private double developmentScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    boolean isWhite = player.getAlliance().isWhite();

    // Base development score - pieces off starting rank
    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) {

        // Check if minor piece has moved from starting rank
        final int pieceRank = piece.getPiecePosition() / 8;
        if ((isWhite && pieceRank != 7) || (!isWhite && pieceRank != 0)) {
          score += 20;

          // Extra bonus for development to active squares
          if (isActiveDevelopmentSquare(piece.getPiecePosition(), piece.getPieceType(), isWhite)) {
            score += 10;
          }
        } else {
          // Penalty for undeveloped minor pieces (higher than before)
          score -= 30;
        }
      }
    }

    // Development harmony - penalize disharmonious development
    if (isDevelopmentImbalanced(playerPieces, isWhite)) {
      score -= 15;
    }

    // Penalize early queen development even more
    for (final Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.QUEEN && !piece.isFirstMove()) {
        final int queenRank = piece.getPiecePosition() / 8;

        // Extra penalty for deep queen excursions
        if ((isWhite && queenRank < 5) || (!isWhite && queenRank > 2)) {
          score -= 35;
        } else {
          score -= 25;
        }
      }
    }

    // Bonus for completed development and castling
    if (isMinorPiecesDeveloped(playerPieces, isWhite) &&
            player.isCastled() &&
            !hasEarlyQueenMove(playerPieces)) {
      score += 40; // Significant bonus for completing development correctly
    }

    return score;
  }

  /**
   * Checks if a square is an active development square for the given piece type.
   */
  private boolean isActiveDevelopmentSquare(final int position, final Piece.PieceType pieceType, final boolean isWhite) {
    // Central knights
    if (pieceType == Piece.PieceType.KNIGHT) {
      if (isWhite) {
        return position == 18 || position == 21 || position == 33 || position == 36;
      } else {
        return position == 27 || position == 30 || position == 42 || position == 45;
      }
    }
    // Fianchetto or active diagonals for bishops
    else if (pieceType == Piece.PieceType.BISHOP) {
      if (isWhite) {
        return position == 16 || position == 23 || position == 17 || position == 22 ||
                position == 40 || position == 45;
      } else {
        return position == 40 || position == 47 || position == 41 || position == 46 ||
                position == 16 || position == 21;
      }
    }
    return false;
  }

  /**
   * Detects imbalanced development (e.g., developing only one flank).
   */
  private boolean isDevelopmentImbalanced(Collection<Piece> pieces, boolean isWhite) {
    int kingsideDeveloped = 0;
    int queensideDeveloped = 0;

    for (Piece piece : pieces) {
      if ((piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) &&
              !piece.isFirstMove()) {

        int file = piece.getPiecePosition() % 8;
        if (file < 4) {
          queensideDeveloped++;
        } else {
          kingsideDeveloped++;
        }
      }
    }

    // Development is imbalanced if one side has 2+ pieces developed and the other has none
    return (kingsideDeveloped >= 2 && queensideDeveloped == 0) ||
            (queensideDeveloped >= 2 && kingsideDeveloped == 0);
  }

  /**
   * Checks if all minor pieces are developed.
   */
  private boolean isMinorPiecesDeveloped(Collection<Piece> pieces, boolean isWhite) {
    int developedMinorPieces = 0;

    for (Piece piece : pieces) {
      if ((piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) &&
              !piece.isFirstMove()) {

        int rank = piece.getPiecePosition() / 8;
        if ((isWhite && rank != 7) || (!isWhite && rank != 0)) {
          developedMinorPieces++;
        }
      }
    }

    return developedMinorPieces >= 4; // All 4 minor pieces developed
  }

  /**
   * Checks if the queen has moved early.
   */
  private boolean hasEarlyQueenMove(Collection<Piece> pieces) {
    for (Piece piece : pieces) {
      if (piece.getPieceType() == Piece.PieceType.QUEEN && !piece.isFirstMove()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Evaluates center control using a more sophisticated approach that considers:
   * - Direct center occupation (d4, e4, d5, e5)
   * - Extended center control (c3-f3-c6-f6)
   * - Attacks on center squares
   * - Potential to occupy center in the future
   */
  private double centerControlScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    Collection<Move> playerMoves = player.getLegalMoves();

    // Center squares: d4(28), e4(29), d5(36), e5(37)
    final int[] centerSquares = {28, 29, 36, 37};

    // Extended center
    final int[] extendedCenterSquares = {
            20, 21, 22, 23, // Rank 3
            28, 29, 30, 31, // Rank 4
            36, 37, 38, 39, // Rank 5
            44, 45, 46, 47  // Rank 6
    };

    // Direct center occupation
    for (Piece piece : playerPieces) {
      int position = piece.getPiecePosition();

      // Center squares
      for (int centerSquare : centerSquares) {
        if (position == centerSquare) {
          // Pawn in center is especially valuable
          if (piece.getPieceType() == Piece.PieceType.PAWN) {
            score += 30;
          } else {
            score += 20;
          }
        }
      }

      // Extended center squares
      for (int extendedSquare : extendedCenterSquares) {
        if (position == extendedSquare) {
          if (piece.getPieceType() == Piece.PieceType.PAWN) {
            score += 15;
          } else {
            score += 10;
          }
        }
      }
    }

    // Center control (pieces attacking center)
    for (Move move : playerMoves) {
      int destination = move.getDestinationCoordinate();

      for (int centerSquare : centerSquares) {
        if (destination == centerSquare) {
          score += 7;
        }
      }
    }

    // Potential future center control - pawn advances toward center
    List<Piece> pawns = playerPieces.stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.PAWN)
            .toList();

    for (Piece pawn : pawns) {
      int pawnFile = pawn.getPiecePosition() % 8;

      // Pawns on d and e files that can advance to center
      if (pawnFile == 3 || pawnFile == 4) {
        score += 5;
      }
      // Pawns on c and f files that can influence center
      else if (pawnFile == 2 || pawnFile == 5) {
        score += 3;
      }
    }

    return score;
  }

  /**
   * Evaluates king security in the opening, focusing on:
   * - Castling status
   * - Pawn shield integrity
   * - Potential for opponent attacks
   * - Piece proximity to king
   */
  private double kingSecurityScore(final Player player, final Board board) {
    double score = 0;
    final King playerKing = player.getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();

    // Major bonus for castling
    if (playerKing.isCastled()) {
      score += 60;

      // Additional bonus for intact pawn shield after castling
      score += evaluatePawnShield(playerKing, player.getActivePieces());
    }
    else {
      // Bonus for castling availability
      if (playerKing.isKingSideCastleCapable()) {
        score += 20;
      }
      if (playerKing.isQueenSideCastleCapable()) {
        score += 15; // Slightly less as queenside castling is slower
      }

      // Penalty for moving king in opening without castling
      if (!playerKing.isFirstMove()) {
        score -= 40;
      }
    }

    // Penalty for opponent pieces near king
    score -= evaluateKingAttackPotential(kingPosition, player.getOpponent().getActivePieces());

    // Penalty for open lines toward king
    score -= evaluateOpenLinesNearKing(kingPosition, board);

    return score;
  }

  /**
   * Evaluates the integrity of the pawn shield around a castled king.
   */
  private double evaluatePawnShield(final King king, final Collection<Piece> playerPieces) {
    double score = 0;
    final int kingPosition = king.getPiecePosition();
    final boolean isKingSideCastled = (kingPosition == 62 || kingPosition == 6);

    // Identify pawn shield squares based on castling side
    List<Integer> pawnShieldSquares = new ArrayList<>();

    if (isKingSideCastled) {
      // King side pawn shield squares
      if (king.getPieceAllegiance().isWhite()) {
        pawnShieldSquares.addAll(Arrays.asList(46, 47, 48));
      } else {
        pawnShieldSquares.addAll(Arrays.asList(14, 15, 16));
      }
    } else {
      // Queen side pawn shield squares
      if (king.getPieceAllegiance().isWhite()) {
        pawnShieldSquares.addAll(Arrays.asList(48, 49, 50));
      } else {
        pawnShieldSquares.addAll(Arrays.asList(8, 9, 10));
      }
    }

    // Count pawns on shield squares
    int pawnsInShield = 0;
    for (Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.PAWN &&
              pawnShieldSquares.contains(piece.getPiecePosition())) {
        pawnsInShield++;
      }
    }

    // Score based on pawn shield integrity
    if (pawnsInShield == 3) {
      score += 30; // Perfect shield
    } else if (pawnsInShield == 2) {
      score += 15; // Good shield
    } else if (pawnsInShield == 1) {
      score += 5;  // Partial shield
    }

    return score;
  }

  /**
   * Evaluates potential attacks against the king.
   */
  private double evaluateKingAttackPotential(final int kingPosition, final Collection<Piece> opponentPieces) {
    double attackPotential = 0;

    for (Piece piece : opponentPieces) {
      // Calculate distance to king
      int rankDistance = Math.abs((kingPosition / 8) - (piece.getPiecePosition() / 8));
      int fileDistance = Math.abs((kingPosition % 8) - (piece.getPiecePosition() % 8));
      int distance = Math.max(rankDistance, fileDistance);

      // The closer the piece, the higher the threat
      if (distance <= 2) {
        // Weight by piece type - more dangerous pieces are weighted higher
        switch (piece.getPieceType()) {
          case QUEEN:
            attackPotential += (3 - distance) * 8;
            break;
          case ROOK:
            attackPotential += (3 - distance) * 5;
            break;
          case BISHOP:
          case KNIGHT:
            attackPotential += (3 - distance) * 3;
            break;
          default:
            attackPotential += (3 - distance);
        }
      }
    }

    return attackPotential;
  }

  /**
   * Evaluates open lines (files and diagonals) near the king.
   */
  private double evaluateOpenLinesNearKing(final int kingPosition, final Board board) {
    double openLinesPenalty = 0;
    final int kingFile = kingPosition % 8;

    // Check for open files near king
    for (int file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
      boolean isFileOpen = true;

      // Check if there are pawns on this file
      for (int rank = 0; rank < 8; rank++) {
        final int square = rank * 8 + file;
        final Piece piece = board.getPiece(square);

        if (piece != null && piece.getPieceType() == Piece.PieceType.PAWN) {
          isFileOpen = false;
          break;
        }
      }

      if (isFileOpen) {
        openLinesPenalty += 15;
      }
    }

    return openLinesPenalty;
  }

  /**
   * Evaluates pawn structure specifically for the opening phase, considering:
   * - Center pawns
   * - Doubled pawns
   * - Isolated pawns
   * - Pawn chains
   * - Pawn advances
   */
  private double pawnStructureScore(final Player player, final Board board) {
    double score = 0;
    List<Piece> pawns = player.getActivePieces().stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.PAWN)
            .collect(Collectors.toList());

    boolean isWhite = player.getAlliance().isWhite();

    // Center pawn structure
    score += evaluateCenterPawns(pawns, isWhite);

    // Pawn chains
    score += evaluatePawnChains(pawns);

    // Doubled pawns (penalty)
    score += evaluateDoubledPawns(pawns);

    // Isolated pawns (penalty)
    score += evaluateIsolatedPawns(pawns);

    // Early pawn advances
    score += evaluatePawnAdvances(pawns, isWhite);

    return score;
  }

  /**
   * Evaluates center pawns structure.
   */
  private double evaluateCenterPawns(final List<Piece> pawns, final boolean isWhite) {
    double score = 0;

    // Center files (d and e files)
    boolean hasDPawn = false;
    boolean hasEPawn = false;
    boolean hasCPawn = false;
    boolean hasFPawn = false;

    for (Piece pawn : pawns) {
      int pawnFile = pawn.getPiecePosition() % 8;
      int pawnRank = pawn.getPiecePosition() / 8;

      // Identify pawn files
      if (pawnFile == 3) hasDPawn = true;
      if (pawnFile == 4) hasEPawn = true;
      if (pawnFile == 2) hasCPawn = true;
      if (pawnFile == 5) hasFPawn = true;

      // Bonus for center pawns on 4th/5th rank
      if ((pawnFile == 3 || pawnFile == 4) &&
              ((isWhite && (pawnRank == 3 || pawnRank == 4)) ||
                      (!isWhite && (pawnRank == 3 || pawnRank == 4)))) {
        score += 25;
      }
    }

    // Bonus for having both center pawns
    if (hasDPawn && hasEPawn) {
      score += 25;
    }

    // Bonus for protecting center with c and f pawns
    if ((hasDPawn && hasCPawn) || (hasEPawn && hasFPawn)) {
      score += 15;
    }

    return score;
  }

  /**
   * Evaluates pawn chains for mobility and piece support.
   */
  private double evaluatePawnChains(final List<Piece> pawns) {
    double score = 0;
    Map<Integer, List<Piece>> pawnsByFile = new HashMap<>();

    // Group pawns by file
    for (Piece pawn : pawns) {
      int file = pawn.getPiecePosition() % 8;
      pawnsByFile.computeIfAbsent(file, k -> new ArrayList<>()).add(pawn);
    }

    // Check for pawn chains (adjacent files)
    for (int file = 0; file < 7; file++) {
      if (pawnsByFile.containsKey(file) && pawnsByFile.containsKey(file + 1)) {
        // Found pawns on adjacent files
        score += 8;

        // Extra bonus for longer chains
        if (file > 0 && pawnsByFile.containsKey(file - 1)) {
          score += 5;
        }
      }
    }

    return score;
  }

  /**
   * Evaluates doubled pawns (penalty).
   */
  private double evaluateDoubledPawns(final List<Piece> pawns) {
    double score = 0;
    Map<Integer, Integer> pawnCountByFile = new HashMap<>();

    // Count pawns on each file
    for (Piece pawn : pawns) {
      int file = pawn.getPiecePosition() % 8;
      pawnCountByFile.put(file, pawnCountByFile.getOrDefault(file, 0) + 1);
    }

    // Penalize doubled pawns
    for (int count : pawnCountByFile.values()) {
      if (count > 1) {
        score -= 25 * (count - 1);
      }
    }

    return score;
  }

  /**
   * Evaluates isolated pawns (penalty).
   */
  private double evaluateIsolatedPawns(final List<Piece> pawns) {
    double score = 0;
    Set<Integer> pawnFiles = new HashSet<>();

    // Collect all files with pawns
    for (Piece pawn : pawns) {
      pawnFiles.add(pawn.getPiecePosition() % 8);
    }

    // Check for isolated pawns
    for (int file : pawnFiles) {
      boolean hasAdjacentFile = pawnFiles.contains(file - 1) || pawnFiles.contains(file + 1);

      if (!hasAdjacentFile) {
        // Count pawns on this isolated file (each isolated pawn counts)
        int isolatedCount = 0;
        for (Piece pawn : pawns) {
          if (pawn.getPiecePosition() % 8 == file) {
            isolatedCount++;
          }
        }
        score -= 20 * isolatedCount;
      }
    }

    return score;
  }

  /**
   * Evaluates pawn advances in the opening.
   */
  private double evaluatePawnAdvances(final List<Piece> pawns, final boolean isWhite) {
    double score = 0;

    for (Piece pawn : pawns) {
      int pawnRank = pawn.getPiecePosition() / 8;
      int pawnFile = pawn.getPiecePosition() % 8;

      // Penalize excessive pawn advances in the opening
      // For white: rank 0 is the 8th rank, 7 is the 1st rank
      // For black: rank 7 is the 8th rank, 0 is the 1st rank

      // Center files can advance more
      if (pawnFile == 3 || pawnFile == 4) {  // d and e files
        if ((isWhite && pawnRank < 4) || (!isWhite && pawnRank > 3)) {
          // Center pawns should generally not advance beyond the 4th/5th rank in opening
          score -= 5;
        }
      } else {
        // Wing pawns should be more conservative
        if ((isWhite && pawnRank < 5) || (!isWhite && pawnRank > 2)) {
          // Non-center pawns should generally not advance beyond the 3rd rank in opening
          score -= 10;
        }

        // Extra penalty for rook pawns advances (weakens castle position)
        if ((pawnFile == 0 || pawnFile == 7) &&
                ((isWhite && pawnRank < 6) || (!isWhite && pawnRank > 1))) {
          score -= 15;
        }
      }
    }

    return score;
  }

  /**
   * Evaluates piece mobility and space control:
   * - Knight mobility
   * - Bishop mobility
   * - Space advantage
   * - Control of key squares
   */
  private double spaceAndMobilityScore(final Player player, final Board board) {
    double score = 0;
    Collection<Move> playerMoves = player.getLegalMoves();
    Collection<Piece> playerPieces = player.getActivePieces();
    boolean isWhite = player.getAlliance().isWhite();

    // Count moves for each piece type
    Map<Piece.PieceType, Integer> movesByPieceType = new HashMap<>();
    for (Move move : playerMoves) {
      Piece piece = move.getMovedPiece();
      movesByPieceType.put(piece.getPieceType(),
              movesByPieceType.getOrDefault(piece.getPieceType(), 0) + 1);
    }

    // Mobility bonuses by piece type
    if (movesByPieceType.containsKey(Piece.PieceType.KNIGHT)) {
      score += movesByPieceType.get(Piece.PieceType.KNIGHT) * 2.5;
    }

    if (movesByPieceType.containsKey(Piece.PieceType.BISHOP)) {
      score += movesByPieceType.get(Piece.PieceType.BISHOP) * 2.0;
    }

    if (movesByPieceType.containsKey(Piece.PieceType.ROOK)) {
      score += movesByPieceType.get(Piece.PieceType.ROOK) * 1.0;
    }

    // Queen mobility - less important in the opening, can even be negative if too early
    if (movesByPieceType.containsKey(Piece.PieceType.QUEEN)) {
      boolean earlyQueenMove = false;
      for (Piece piece : playerPieces) {
        if (piece.getPieceType() == Piece.PieceType.QUEEN && !piece.isFirstMove()) {
          earlyQueenMove = true;
          break;
        }
      }

      if (earlyQueenMove) {
        // Discount queen mobility if moved too early
        score += movesByPieceType.get(Piece.PieceType.QUEEN) * 0.5;
      } else {
        // Normal queen mobility if still on starting square
        score += movesByPieceType.get(Piece.PieceType.QUEEN) * 0.8;
      }
    }

    // Space advantage - control of opponent's half
    int spaceCount = 0;
    for (Move move : playerMoves) {
      int destRank = move.getDestinationCoordinate() / 8;

      // Count moves to opponent's half of the board
      if ((isWhite && destRank < 4) || (!isWhite && destRank > 3)) {
        spaceCount++;
      }
    }

    // Space bonus
    score += Math.min(spaceCount, 15) * 1.5;

    return score;
  }

  /**
   * Evaluates piece coordination:
   * - Bishop pair bonus
   * - Knights supporting each other
   * - Rooks connected or on same file
   * - Pieces protecting each other
   */
  private double pieceCoordinationScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    Collection<Move> playerMoves = player.getLegalMoves();

    // Check for bishop pair
    long bishopCount = playerPieces.stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.BISHOP)
            .count();

    if (bishopCount >= 2) {
      score += 30; // Bishop pair bonus
    }

    // Check for connected/doubled rooks
    List<Piece> rooks = playerPieces.stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.ROOK)
            .toList();

    if (rooks.size() >= 2) {
      // Check if rooks are on the same file
      boolean rooksOnSameFile = false;
      for (int i = 0; i < rooks.size(); i++) {
        for (int j = i + 1; j < rooks.size(); j++) {
          if (rooks.get(i).getPiecePosition() % 8 == rooks.get(j).getPiecePosition() % 8) {
            rooksOnSameFile = true;
            break;
          }
        }
      }

      if (rooksOnSameFile) {
        score += 15; // Rooks doubled on a file
      }

      // Check for rook connection (one rook protects another)
      boolean rooksConnected = false;
      for (Move move : playerMoves) {
        if (move.getMovedPiece().getPieceType() == Piece.PieceType.ROOK) {
          for (Piece rook : rooks) {
            if (move.getDestinationCoordinate() == rook.getPiecePosition() &&
                    !move.getMovedPiece().equals(rook)) {
              rooksConnected = true;
              break;
            }
          }
        }
      }

      if (rooksConnected) {
        score += 10; // Rooks can protect each other
      }
    }

    // Piece protection analysis
    Map<Integer, Integer> protectionCount = new HashMap<>();
    for (Move move : playerMoves) {
      if (move.isAttack() &&
              move.getAttackedPiece().getPieceAllegiance() != player.getAlliance()) {
        continue; // Skip attacks on opponent pieces
      }

      // Count how many pieces protect each square
      protectionCount.put(move.getDestinationCoordinate(),
              protectionCount.getOrDefault(move.getDestinationCoordinate(), 0) + 1);
    }

    // Bonus for pieces that are protected
    for (Piece piece : playerPieces) {
      if (protectionCount.getOrDefault(piece.getPiecePosition(), 0) > 0) {
        score += 5; // Piece is protected

        // Extra bonus for protected knights in opponent's half
        if (piece.getPieceType() == Piece.PieceType.KNIGHT) {
          int rank = piece.getPiecePosition() / 8;
          boolean inOpponentHalf = (player.getAlliance().isWhite() && rank < 4) ||
                  (!player.getAlliance().isWhite() && rank > 3);
          if (inOpponentHalf) {
            score += 10;
          }
        }
      }
    }

    return score;
  }

  /**
   * Evaluates tempo and initiative in the opening.
   */
  private double tempoScore(final Player player, final Board board) {
    double score = 0;
    Collection<Move> playerMoves = player.getLegalMoves();
    boolean isWhite = player.getAlliance().isWhite();

    // Count attacking moves
    int attackingMoves = 0;
    for (Move move : playerMoves) {
      if (move.isAttack()) {
        attackingMoves++;
      }
    }

    // Initiative bonus based on attacking moves
    score += Math.min(attackingMoves, 10) * 2;

    // Bonus for pinning opponent pieces
    score += evaluatePins(player, board);

    // Tempo evaluation based on move number
    // Since this is for opening evaluation, we assume we're in the opening

    // The side to move has tempo
    score += 10;

    // By tracking the transition move, we could reconstruct move number
    // but for simplicity, we'll just use the number of pieces developed

    int developedPieces = (int)player.getActivePieces().stream()
            .filter(p -> (p.getPieceType() == Piece.PieceType.KNIGHT ||
                    p.getPieceType() == Piece.PieceType.BISHOP) &&
                    !p.isFirstMove())
            .count();

    int opponentDevelopedPieces = (int)player.getOpponent().getActivePieces().stream()
            .filter(p -> (p.getPieceType() == Piece.PieceType.KNIGHT ||
                    p.getPieceType() == Piece.PieceType.BISHOP) &&
                    !p.isFirstMove())
            .count();

    // Development lead bonus (each piece ahead is worth tempo)
    if (developedPieces > opponentDevelopedPieces) {
      score += (developedPieces - opponentDevelopedPieces) * 15;
    }

    return score;
  }

  /**
   * Evaluates pins against opponent pieces.
   */
  private double evaluatePins(final Player player, final Board board) {
    double score = 0;
    Collection<Move> playerMoves = player.getLegalMoves();
    Collection<Piece> opponentPieces = player.getOpponent().getActivePieces();
    King opponentKing = player.getOpponent().getPlayerKing();

    // Check for each opponent piece if it's potentially pinned
    for (Piece opponentPiece : opponentPieces) {
      if (opponentPiece.getPieceType() == Piece.PieceType.KING) {
        continue; // Skip the king
      }

      // Check if this piece is between our slider and their king
      boolean isPinned = isPiecePinned(opponentPiece, opponentKing, playerMoves, board);

      if (isPinned) {
        // Bonus varies by piece value - pinning more valuable pieces is better
        score += opponentPiece.getPieceValue() / 20.0;
      }
    }

    return score;
  }

  /**
   * Checks if a piece is pinned to its king.
   * This is a simplified check - a full engine would analyze rays between pieces.
   */
  private boolean isPiecePinned(Piece piece, King king, Collection<Move> attackerMoves, Board board) {
    // Direction from king to piece
    int kingPos = king.getPiecePosition();
    int piecePos = piece.getPiecePosition();

    int rankDiff = (piecePos / 8) - (kingPos / 8);
    int fileDiff = (piecePos % 8) - (kingPos % 8);

    // If not on same rank, file, or diagonal, can't be pinned
    if (rankDiff != 0 && fileDiff != 0 && Math.abs(rankDiff) != Math.abs(fileDiff)) {
      return false;
    }

    // Normalize direction
    int rankDir = (rankDiff == 0) ? 0 : rankDiff / Math.abs(rankDiff);
    int fileDir = (fileDiff == 0) ? 0 : fileDiff / Math.abs(fileDiff);

    // Check if there's a piece that could pin in the opposite direction
    int checkPos = piecePos + rankDir * 8 + fileDir;
    while (checkPos >= 0 && checkPos < 64) {
      // Ensure we're still on the same line
      if ((checkPos % 8) - (piecePos % 8) != fileDir * ((checkPos - piecePos) / (rankDir * 8 + fileDir))) {
        break; // Wrapped to next/prev row
      }

      Piece potentialPinner = board.getPiece(checkPos);
      if (potentialPinner != null) {
        // Found a piece - is it an appropriate attacker?
        if (potentialPinner.getPieceAllegiance() != piece.getPieceAllegiance()) {
          if ((rankDir == 0 || fileDir == 0) &&
                  (potentialPinner.getPieceType() == Piece.PieceType.ROOK ||
                          potentialPinner.getPieceType() == Piece.PieceType.QUEEN)) {
            return true; // Pinned by rook or queen on rank or file
          } else if (rankDir != 0 && fileDir != 0 &&
                  (potentialPinner.getPieceType() == Piece.PieceType.BISHOP ||
                          potentialPinner.getPieceType() == Piece.PieceType.QUEEN)) {
            return true; // Pinned by bishop or queen on diagonal
          }
        }
        break; // Any other piece blocks the pin
      }

      checkPos += rankDir * 8 + fileDir;
    }

    return false;
  }
}