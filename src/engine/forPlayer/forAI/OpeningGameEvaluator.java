package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import engine.Alliance;
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
    return (score(board.whitePlayer(), board, depth) - score(board.blackPlayer(), board, depth));
  }

  /**
   * Calculates the overall score of the current board position for a given player
   * focusing specifically on opening principles.
   *
   * @param player The player for whom the board position is being evaluated.
   * @param board  The current state of the chess board.
   * @param depth  The current search depth (higher depths may use different weights)
   * @return       The evaluation score of the board from the perspective of the specified player.
   */
  @VisibleForTesting
  private double score(final Player player, final Board board, final int depth) {
    // Material has lower weight in opening
    double materialScore = materialScore(player.getActivePieces()) * 0.7;

    // Development is critical in opening - heavily weighted
    double developmentScore = developmentScore(player, board) * 3.0;

    // Center control is fundamental in opening
    double centerControlScore = centerControlScore(player, board) * 2.5;

    // King safety is essential
    double kingSafetyScore = kingSafetyScore(player, board) * 2.2;

    // Pawn structure affects long-term position
    double pawnStructureScore = pawnStructureScore(player, board);

    // Space and mobility become more important as pieces develop
    double mobilityScore = mobilityScore(player, board) * 0.8;

    // Piece coordination and harmony
    double pieceCoordinationScore = pieceCoordinationScore(player, board) * 1.2;

    // Tempo is crucial in opening
    double tempoScore = tempoScore(player, board, depth) * 1.5;

    return materialScore + developmentScore + centerControlScore +
            kingSafetyScore + pawnStructureScore + mobilityScore +
            pieceCoordinationScore + tempoScore;
  }

  /**
   * Evaluates material with lower weight during opening.
   * Some material sacrifices are common in openings for initiative.
   */
  private double materialScore(final Collection<Piece> playerPieces) {
    double materialValue = 0;
    for (final Piece piece : playerPieces) {
      materialValue += piece.getPieceValue();
    }
    return materialValue;
  }

  /**
   * Evaluates development of pieces - a critical opening consideration.
   */
  private double developmentScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    Alliance alliance = player.getAlliance();
    boolean isWhite = alliance.isWhite();

    // Count developed minor pieces
    int developedMinorPieces = 0;
    boolean queenMoved = false;
    boolean castled = player.isCastled();

    // Points for developed knights and bishops
    for (Piece piece : playerPieces) {
      // Check if piece is not on its starting rank
      final int pieceRank = piece.getPiecePosition() / 8;

      if (piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) {

        boolean developedPosition = false;
        // Check if piece is moved from starting position
        if ((isWhite && pieceRank != 7) || (!isWhite && pieceRank != 0)) {
          // Basic development bonus
          score += 40;
          developedMinorPieces++;
          developedPosition = true;

          // Extra bonus for centralized pieces
          if (isCentralPosition(piece.getPiecePosition(), piece.getPieceType())) {
            score += 20;
          }
        } else {
          // Heavy penalty for undeveloped minor pieces
          score -= 60;
        }

        // Extra bonus for piece mobility
        if (developedPosition) {
          Collection<Move> pieceMoves = piece.calculateLegalMoves(board);
          score += pieceMoves.size() * 2;
        }
      }

      // Check if queen moved early (often a mistake)
      if (piece.getPieceType() == Piece.PieceType.QUEEN && !piece.isFirstMove()) {
        queenMoved = true;
        int queenRank = piece.getPiecePosition() / 8;
        int queenFile = piece.getPiecePosition() % 8;

        // Severe penalty for queen moves that enter opponent's territory too early
        if ((isWhite && queenRank < 4) || (!isWhite && queenRank > 3)) {
          score -= 100;
        }

        // Penalty for central queen development blocking minor pieces
        if (queenFile >= 2 && queenFile <= 5) {
          score -= 60;
        } else {
          // General penalty for early queen moves
          score -= 50;
        }
      }
    }

    // Strong bonus for castling
    if (castled) {
      score += 100;
    } else if (canCastle(player)) {
      // Small bonus for preserving castling rights
      score += 25;
    } else if (!canCastle(player)) {
      // Penalty for losing castling rights without castling
      score -= 60;
    }

    // Development harmony - all minor pieces should be developed before major pieces
    if (developedMinorPieces >= 3 && castled && !queenMoved) {
      score += 50; // Harmony bonus for following development principles
    }

    return score;
  }

  /**
   * Checks if the position is centralized (good development square for minor piece)
   */
  private boolean isCentralPosition(int position, Piece.PieceType pieceType) {
    int file = position % 8;
    int rank = position / 8;

    // Central area plus extended center
    boolean isExtendedCenter = (file >= 2 && file <= 5 && rank >= 2 && rank <= 5);

    // Ideal central outposts
    if (pieceType == Piece.PieceType.KNIGHT) {
      // Knights ideal on e4, d4, e5, d5 and surrounding squares
      return (file >= 2 && file <= 5 && rank >= 2 && rank <= 5);
    } else if (pieceType == Piece.PieceType.BISHOP) {
      // Bishops controlling long diagonals or fianchetto positions
      return isExtendedCenter ||
              // Fianchetto positions
              position == 16 || position == 23 || // g2, b2
              position == 40 || position == 47;  // g7, b7
    }

    return isExtendedCenter;
  }

  /**
   * Checks if player can still castle (has castling rights)
   */
  private boolean canCastle(Player player) {
    return player.getPlayerKing().isKingSideCastleCapable() ||
            player.getPlayerKing().isQueenSideCastleCapable();
  }

  /**
   * Evaluates center control - critical in opening theory
   * Rewards:
   * - Pawns in or controlling the center
   * - Pieces controlling central squares
   * - Moves that attack center squares
   */
  private double centerControlScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    Collection<Move> playerMoves = player.getLegalMoves();

    // Central squares: d4(28), e4(29), d5(36), e5(37)
    final int[] centralSquares = {28, 29, 36, 37};

    // Extended center: c3-f3-c6-f6 area
    final int[] extendedCenterSquares = {
            18, 19, 20, 21, 22, 23,  // Rank 3
            26, 27, 28, 29, 30, 31,  // Rank 4
            34, 35, 36, 37, 38, 39,  // Rank 5
            42, 43, 44, 45, 46, 47   // Rank 6
    };

    // Direct center occupation
    for (Piece piece : playerPieces) {
      int position = piece.getPiecePosition();

      // Center squares occupation
      for (int centralSquare : centralSquares) {
        if (position == centralSquare) {
          // Pawn in center is extremely valuable
          if (piece.getPieceType() == Piece.PieceType.PAWN) {
            score += 80;
          }
          // Minor pieces in center also good
          else if (piece.getPieceType() == Piece.PieceType.KNIGHT ||
                  piece.getPieceType() == Piece.PieceType.BISHOP) {
            score += 40;
          }
          // Other pieces
          else {
            score += 20;
          }
        }
      }

      // Extended center occupation
      for (int extendedSquare : extendedCenterSquares) {
        if (position == extendedSquare) {
          if (piece.getPieceType() == Piece.PieceType.PAWN) {
            score += 30;
          } else {
            score += 15;
          }
        }
      }
    }

    // Control of center - count moves to center
    Map<Integer, Integer> controlledSquares = new HashMap<>();

    for (Move move : playerMoves) {
      int destination = move.getDestinationCoordinate();
      controlledSquares.put(destination,
              controlledSquares.getOrDefault(destination, 0) + 1);
    }

    // Score central control
    for (int centralSquare : centralSquares) {
      score += controlledSquares.getOrDefault(centralSquare, 0) * 15;
    }

    // Score extended center control
    for (int extendedSquare : extendedCenterSquares) {
      score += controlledSquares.getOrDefault(extendedSquare, 0) * 5;
    }

    // Pawn structure supporting center
    int centerPawns = 0;
    boolean hasDPawn = false;
    boolean hasEPawn = false;

    for (Piece piece : playerPieces) {
      if (piece.getPieceType() == Piece.PieceType.PAWN) {
        int file = piece.getPiecePosition() % 8;
        if (file == 3 || file == 4) {
          centerPawns++;
          if (file == 3) hasDPawn = true;
          if (file == 4) hasEPawn = true;
        }
      }
    }

    // Bonus for having both central pawns
    if (hasDPawn && hasEPawn) {
      score += 50;
    }

    return score;
  }

  /**
   * Evaluates king safety - critical in opening
   */
  private double kingSafetyScore(final Player player, final Board board) {
    double score = 0;
    final King playerKing = player.getPlayerKing();
    final int kingPosition = playerKing.getPiecePosition();

    // Check if king is castled
    if (player.isCastled()) {
      score += 120; // Major bonus for castling

      // Check pawn shield integrity
      score += evaluatePawnShield(player, kingPosition);
    }
    else {
      // King in center is dangerous
      int file = kingPosition % 8;
      int rank = kingPosition / 8;

      boolean inCenter = (file >= 2 && file <= 5);
      if (inCenter) {
        score -= 80; // Very dangerous
      }

      // Penalty for moving king without castling
      if (!playerKing.isFirstMove()) {
        score -= 70;
      }
    }

    // Check opponent threats near king
    score -= evaluateKingAttackPotential(kingPosition, player.getOpponent());

    return score;
  }

  /**
   * Evaluates the pawn shield protecting the king
   */
  private double evaluatePawnShield(Player player, int kingPosition) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    List<Piece> pawns = playerPieces.stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.PAWN)
            .toList();

    // Determine king side (kingside vs queenside castle)
    int kingFile = kingPosition % 8;
    int kingRank = kingPosition / 8;
    boolean isKingSide = (kingFile >= 5);

    // Define pawn shield squares
    List<Integer> shieldSquares = new ArrayList<>();
    if (isKingSide) {
      // King side castle shield
      if (player.getAlliance().isWhite()) {
        // White kingside shield (f2, g2, h2)
        shieldSquares.add(kingPosition - 9);  // Upper left
        shieldSquares.add(kingPosition - 8);  // Upper center
        shieldSquares.add(kingPosition - 7);  // Upper right
      } else {
        // Black kingside shield (f7, g7, h7)
        shieldSquares.add(kingPosition + 7);  // Lower left
        shieldSquares.add(kingPosition + 8);  // Lower center
        shieldSquares.add(kingPosition + 9);  // Lower right
      }
    } else {
      // Queen side castle shield
      if (player.getAlliance().isWhite()) {
        // White queenside shield (a2, b2, c2)
        shieldSquares.add(kingPosition - 9);  // Upper left
        shieldSquares.add(kingPosition - 8);  // Upper center
        shieldSquares.add(kingPosition - 7);  // Upper right
      } else {
        // Black queenside shield (a7, b7, c7)
        shieldSquares.add(kingPosition + 7);  // Lower left
        shieldSquares.add(kingPosition + 8);  // Lower center
        shieldSquares.add(kingPosition + 9);  // Lower right
      }
    }

    // Count pawns in shield positions
    int pawnsInShield = 0;
    for (Integer shieldSquare : shieldSquares) {
      if (shieldSquare >= 0 && shieldSquare < 64) { // Valid square check
        for (Piece pawn : pawns) {
          if (pawn.getPiecePosition() == shieldSquare) {
            pawnsInShield++;
            break;
          }
        }
      }
    }

    // Score based on shield integrity
    if (pawnsInShield == 3) {
      score += 60; // Perfect shield
    } else if (pawnsInShield == 2) {
      score += 30; // Good shield
    } else if (pawnsInShield == 1) {
      score += 10;  // Weak shield
    } else {
      score -= 40;  // No shield - vulnerable
    }

    return score;
  }

  /**
   * Evaluates potential king attack threats
   */
  private double evaluateKingAttackPotential(int kingPosition, Player opponent) {
    double attackPotential = 0;
    Collection<Piece> opponentPieces = opponent.getActivePieces();

    // Calculate distance to king for each opponent piece
    for (Piece piece : opponentPieces) {
      int piecePos = piece.getPiecePosition();
      int rankDistance = Math.abs((kingPosition / 8) - (piecePos / 8));
      int fileDistance = Math.abs((kingPosition % 8) - (piecePos % 8));
      int distance = Math.max(rankDistance, fileDistance);

      // Evaluate attack potential based on piece type and distance
      if (distance <= 2) {
        switch (piece.getPieceType()) {
          case QUEEN:
            attackPotential += (3 - distance) * 40;
            break;
          case ROOK:
            attackPotential += (3 - distance) * 25;
            break;
          case BISHOP:
            attackPotential += (3 - distance) * 15;
            break;
          case KNIGHT:
            attackPotential += (3 - distance) * 20;
            break;
          case PAWN:
            attackPotential += (3 - distance) * 5;
            break;
        }
      }
    }

    return attackPotential;
  }

  /**
   * Evaluates pawn structure - important for long-term position
   */
  private double pawnStructureScore(final Player player, final Board board) {
    double score = 0;
    Alliance alliance = player.getAlliance();
    List<Piece> pawns = getPawns(player);
    List<Piece> opponentPawns = getPawns(player.getOpponent());

    // Evaluate center pawns
    score += evaluateCenterPawns(pawns, alliance);

    // Penalize doubled pawns (more severe in opening)
    score += evaluateDoubledPawns(pawns);

    // Penalize isolated pawns
    score += evaluateIsolatedPawns(pawns);

    // Evaluate pawn chains (positive in opening)
    score += evaluatePawnChains(pawns);

    // Penalize excessive pawn moves/advances in opening
    score += evaluatePawnAdvances(pawns, alliance);

    return score;
  }

  /**
   * Evaluates central pawn structure
   */
  private double evaluateCenterPawns(final List<Piece> pawns, final Alliance alliance) {
    double score = 0;

    // Check for center pawn presence
    boolean hasDPawn = false;
    boolean hasEPawn = false;
    boolean hasCPawn = false;
    boolean hasFPawn = false;

    for (Piece pawn : pawns) {
      int position = pawn.getPiecePosition();
      int file = position % 8;

      // Identify pawn files
      switch (file) {
        case 2: hasCPawn = true; break; // c-file
        case 3: hasDPawn = true; break; // d-file
        case 4: hasEPawn = true; break; // e-file
        case 5: hasFPawn = true; break; // f-file
      }
    }

    // Strong bonus for controlling the center with pawns
    if (hasDPawn && hasEPawn) {
      score += 60; // Very strong center control
    } else if (hasDPawn || hasEPawn) {
      score += 30; // Partial center control
    }

    // Bonus for supporting center pawns
    if ((hasDPawn && hasCPawn) || (hasEPawn && hasFPawn)) {
      score += 20; // Supported center pawns
    }

    return score;
  }

  /**
   * Evaluates doubled pawns (penalty)
   */
  private double evaluateDoubledPawns(final List<Piece> pawns) {
    double score = 0;

    // Count pawns on each file
    int[] pawnsPerFile = new int[8];
    for (Piece pawn : pawns) {
      pawnsPerFile[pawn.getPiecePosition() % 8]++;
    }

    // Severe penalty for doubled pawns in the opening
    for (int count : pawnsPerFile) {
      if (count > 1) {
        score -= (count - 1) * 35;
      }
    }

    return score;
  }

  /**
   * Evaluates isolated pawns (penalty)
   */
  private double evaluateIsolatedPawns(final List<Piece> pawns) {
    double score = 0;

    // Mark files with pawns
    boolean[] filesWithPawns = new boolean[8];
    for (Piece pawn : pawns) {
      filesWithPawns[pawn.getPiecePosition() % 8] = true;
    }

    // Check for isolated pawns
    for (Piece pawn : pawns) {
      int file = pawn.getPiecePosition() % 8;
      boolean isIsolated = file <= 0 || !filesWithPawns[file - 1];

      // Check adjacent files
      if (file < 7 && filesWithPawns[file + 1]) {
        isIsolated = false;
      }

      if (isIsolated) {
        // Higher penalty for isolated center pawns
        if (file == 3 || file == 4) {
          score -= 40; // Center isolated pawns
        } else {
          score -= 25; // Other isolated pawns
        }
      }
    }

    return score;
  }

  /**
   * Evaluates pawn chains (bonus)
   */
  private double evaluatePawnChains(final List<Piece> pawns) {
    double score = 0;

    // Group pawns by file
    Map<Integer, List<Piece>> pawnsByFile = new HashMap<>();
    for (Piece pawn : pawns) {
      int file = pawn.getPiecePosition() % 8;
      if (!pawnsByFile.containsKey(file)) {
        pawnsByFile.put(file, new ArrayList<>());
      }
      pawnsByFile.get(file).add(pawn);
    }

    // Check for adjacent files with pawns
    int chainLength = 0;
    for (int file = 0; file < 7; file++) {
      if (pawnsByFile.containsKey(file) && pawnsByFile.containsKey(file + 1)) {
        chainLength++;
      } else if (chainLength > 0) {
        // Score based on chain length
        score += 10 * chainLength;
        chainLength = 0;
      }
    }

    // Account for chain at the end of the loop
    if (chainLength > 0) {
      score += 10 * chainLength;
    }

    return score;
  }

  /**
   * Evaluates pawn advances (penalty for excessive advances)
   */
  private double evaluatePawnAdvances(final List<Piece> pawns, final Alliance alliance) {
    double score = 0;

    for (Piece pawn : pawns) {
      int position = pawn.getPiecePosition();
      int rank = position / 8;
      int file = position % 8;

      // Calculate distance from starting rank
      int advanceLevel = alliance.isWhite() ?
              (6 - rank) : (rank - 1);

      // Penalize excessive pawn advances in opening
      if (advanceLevel > 2) {
        // Center pawns can advance more
        if (file == 3 || file == 4) {
          if (advanceLevel > 3) {
            score -= (advanceLevel - 3) * 15;
          }
        } else {
          // Wing pawns should stay back
          score -= (advanceLevel - 2) * 20;
        }
      }

      // Special penalty for moving wing pawns too early
      if ((file == 0 || file == 7) && advanceLevel > 0) {
        score -= 15;
      }

      // Special penalty for weakening king's position with pawn advances
      if (advanceLevel > 0 &&
              ((alliance.isWhite() && rank <= 6 && (file >= 5 || file <= 2)) ||
                      (!alliance.isWhite() && rank >= 1 && (file >= 5 || file <= 2)))) {
        score -= 15; // Weakening potential castle position
      }
    }

    return score;
  }

  /**
   * Evaluates mobility - important but weighted less in opening
   * Movement potential of pieces is relevant but secondary to development
   */
  private double mobilityScore(final Player player, final Board board) {
    double score = 0;
    Collection<Move> playerMoves = player.getLegalMoves();
    Collection<Move> opponentMoves = player.getOpponent().getLegalMoves();

    // Basic mobility score - piece movement potential
    score += playerMoves.size() * 1.5;

    // Knight mobility is especially important early
    for (Piece piece : player.getActivePieces()) {
      if (piece.getPieceType() == Piece.PieceType.KNIGHT) {
        Collection<Move> knightMoves = piece.calculateLegalMoves(board);
        score += knightMoves.size() * 3;
      }
      else if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        Collection<Move> bishopMoves = piece.calculateLegalMoves(board);
        score += bishopMoves.size() * 2.5;
      }
    }

    // Relative mobility (compared to opponent) is strategically important
    int mobilityDifference = playerMoves.size() - opponentMoves.size();
    score += mobilityDifference * 2;

    return score;
  }

  /**
   * Evaluates piece coordination and harmony
   * Rewards:
   * - Pieces supporting each other
   * - Pieces protecting important squares
   * - Good piece placement patterns
   */
  private double pieceCoordinationScore(final Player player, final Board board) {
    double score = 0;
    Collection<Piece> playerPieces = player.getActivePieces();
    Collection<Move> playerMoves = player.getLegalMoves();

    // Find protected pieces
    Map<Integer, Integer> protectedSquares = new HashMap<>();

    // Count protective moves to pieces
    for (Move move : playerMoves) {
      int destination = move.getDestinationCoordinate();
      protectedSquares.put(destination,
              protectedSquares.getOrDefault(destination, 0) + 1);
    }

    // Score for protected pieces (especially minor pieces)
    for (Piece piece : playerPieces) {
      int position = piece.getPiecePosition();
      if (protectedSquares.getOrDefault(position, 0) > 0) {
        // Protected piece bonus
        score += 10;

        // Extra for protected minor pieces
        if (piece.getPieceType() == Piece.PieceType.KNIGHT ||
                piece.getPieceType() == Piece.PieceType.BISHOP) {
          score += 15;
        }
      }
    }

    // Look for specific good development patterns
    score += evaluateDevelopmentPatterns(player, board);

    return score;
  }

  /**
   * Evaluates specific good development patterns in opening
   */
  private double evaluateDevelopmentPatterns(Player player, Board board) {
    double score = 0;
    Collection<Piece> pieces = player.getActivePieces();
    Alliance alliance = player.getAlliance();

    // Check for bishop fianchetto (good pattern)
    boolean kingsideFianchetto = false;
    boolean queensideFianchetto = false;

    // Check for bishop control of diagonals
    for (Piece piece : pieces) {
      if (piece.getPieceType() == Piece.PieceType.BISHOP) {
        int position = piece.getPiecePosition();

        // Check for fianchetto positions
        if (alliance.isWhite()) {
          if (position == 16) queensideFianchetto = true; // b3
          if (position == 23) kingsideFianchetto = true;  // g3
        } else {
          if (position == 40) queensideFianchetto = true; // b6
          if (position == 47) kingsideFianchetto = true;  // g6
        }
      }
    }

    // Bonus for fianchetto development
    if (kingsideFianchetto) score += 25;
    if (queensideFianchetto) score += 20;

    // Connected rooks
    boolean rooksConnected = areRooksConnected(pieces);
    if (rooksConnected) score += 30;

    // Penalty for poorly placed knights
    for (Piece piece : pieces) {
      if (piece.getPieceType() == Piece.PieceType.KNIGHT) {
        int position = piece.getPiecePosition();
        int file = position % 8;
        int rank = position / 8;

        // Knights on rim are dim
        if (file == 0 || file == 7 || rank == 0 || rank == 7) {
          score -= 30;
        }
      }
    }

    return score;
  }

  /**
   * Checks if rooks are connected (on same rank)
   */
  private boolean areRooksConnected(Collection<Piece> pieces) {
    List<Piece> rooks = pieces.stream()
            .filter(p -> p.getPieceType() == Piece.PieceType.ROOK)
            .toList();

    if (rooks.size() >= 2) {
      int firstRookRank = rooks.get(0).getPiecePosition() / 8;

      for (int i = 1; i < rooks.size(); i++) {
        int rookRank = rooks.get(i).getPiecePosition() / 8;
        if (rookRank == firstRookRank) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Evaluates tempo and initiative
   * Rewards:
   * - Faster development
   * - Active threats
   * - Move efficiency
   */
  private double tempoScore(final Player player, final Board board, final int depth) {
    double score = 0;
    Collection<Move> playerMoves = player.getLegalMoves();
    Collection<Piece> playerPieces = player.getActivePieces();
    Collection<Piece> opponentPieces = player.getOpponent().getActivePieces();

    // Count attacking moves
    int attackingMoves = 0;
    for (Move move : playerMoves) {
      if (move.isAttack()) {
        attackingMoves++;

        // Extra for attacks on undefended pieces
        Piece attackedPiece = move.getAttackedPiece();
        if (attackedPiece != null &&
                !isPieceDefended(attackedPiece, player.getOpponent(), board)) {
          score += 15;
        }
      }
    }

    // Initiative bonus based on attacking potential
    score += Math.min(attackingMoves, 10) * 3;

    // Development advantage/tempo
    int developedMinorPieces = countDevelopedMinorPieces(playerPieces, player.getAlliance());
    int opponentDevelopedMinorPieces = countDevelopedMinorPieces(opponentPieces, player.getOpponent().getAlliance());

    // Bonus for development lead
    if (developedMinorPieces > opponentDevelopedMinorPieces) {
      score += (developedMinorPieces - opponentDevelopedMinorPieces) * 30;
    }

    // Depth-based tempo considerations
    // At higher depths, consider longer-term tempo factors
    if (depth > 5) {
      // Initiative for the position
      score += evaluateInitiative(player, board);
    }

    return score;
  }

  /**
   * Checks if a piece is defended by another piece
   */
  private boolean isPieceDefended(Piece piece, Player owner, Board board) {
    int position = piece.getPiecePosition();

    for (Move move : owner.getLegalMoves()) {
      if (move.getDestinationCoordinate() == position) {
        return true; // Piece is defended
      }
    }

    return false;
  }

  /**
   * Counts developed minor pieces
   */
  private int countDevelopedMinorPieces(Collection<Piece> pieces, Alliance alliance) {
    int count = 0;

    for (Piece piece : pieces) {
      if ((piece.getPieceType() == Piece.PieceType.KNIGHT ||
              piece.getPieceType() == Piece.PieceType.BISHOP) &&
              !piece.isFirstMove()) {

        // Check if it's moved from starting rank
        int rank = piece.getPiecePosition() / 8;
        if ((alliance.isWhite() && rank != 7) ||
                (!alliance.isWhite() && rank != 0)) {
          count++;
        }
      }
    }

    return count;
  }

  /**
   * Evaluates initiative factors
   */
  private double evaluateInitiative(Player player, Board board) {
    double score = 0;

    // Space advantage is good for initiative
    score += evaluateSpaceAdvantage(player, board);

    // Free piece movement is good for initiative
    score += evaluatePieceActivity(player, board);

    return score;
  }

  /**
   * Evaluates space advantage
   */
  private double evaluateSpaceAdvantage(Player player, Board board) {
    double score = 0;
    Alliance alliance = player.getAlliance();
    Collection<Move> moves = player.getLegalMoves();

    // Count moves to opponent's half of the board
    int advancedMoves = 0;
    for (Move move : moves) {
      int destRank = move.getDestinationCoordinate() / 8;

      if ((alliance.isWhite() && destRank < 4) ||
              (!alliance.isWhite() && destRank > 3)) {
        advancedMoves++;
      }
    }

    // Space advantage score
    score += Math.min(advancedMoves, 10) * 2;

    return score;
  }

  /**
   * Evaluates piece activity
   */
  private double evaluatePieceActivity(Player player, Board board) {
    double score = 0;
    Collection<Piece> pieces = player.getActivePieces();

    for (Piece piece : pieces) {
      // Ignore king and pawns for this evaluation
      if (piece.getPieceType() == Piece.PieceType.KING ||
              piece.getPieceType() == Piece.PieceType.PAWN) {
        continue;
      }

      Collection<Move> pieceMoves = piece.calculateLegalMoves(board);

      // Activity bonus based on piece type
      switch (piece.getPieceType()) {
        case KNIGHT:
          score += pieceMoves.size() * 2;
          break;
        case BISHOP:
          score += pieceMoves.size() * 1.5;
          break;
        case ROOK:
          score += pieceMoves.size();
          break;
        case QUEEN:
          score += pieceMoves.size() * 0.5; // Less important for queen early
          break;
      }
    }

    return score;
  }

  /**
   * Utility method to get pawns for a player
   */
  private List<Piece> getPawns(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN)
            .collect(Collectors.toList());
  }
}