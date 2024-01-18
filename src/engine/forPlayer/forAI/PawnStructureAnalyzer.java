package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The `PawnStructureAnalyzer` class evaluates the pawn structure of a player in a chess game,
 * providing scores based on factors such as isolated pawns, doubled pawns, and proximity to pawn promotion.
 * It implements modern techniques used by powerful chess engines to assess the strength and weaknesses of the pawn structure.
 * The analysis includes penalties for isolated pawns and doubled pawns, and bonuses for pawn stacks and pawns close to promotion.
 * This class follows a singleton pattern to ensure only one instance exists, accessible via the static get() method.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
public final class PawnStructureAnalyzer {

  /*** Penalty applied for each isolated pawn. */
  public static final int IsolatedPawnPenalty = 5;

  /*** Penalty applied for each doubled pawn. */
  public static final int DoubledPawnPenalty = 8;

  /*** Bonus applied for each connected passed pawn. */
  public static final int ConnectedPassedPawnBonus = 15;

  /*** Bonus applied for each pawn chain. */
  public static final int PawnChainBonus = 4;

  /*** Bonus applied for pawn majority. */
  public static final int PawnMajorityBonus = 8;

  /*** Penalty applied for each pawn island. */
  public static final int PawnIslandPenalty = 6;

  /*** The single instance of the `PawnStructureAnalyzer` class. */
  private static final PawnStructureAnalyzer INSTANCE = new PawnStructureAnalyzer();

  /*** Private constructor to prevent instantiation outside of class. */
  private PawnStructureAnalyzer() {
  }

  /**
   * Gets the single instance of the `PawnStructureAnalyzer`.
   *
   * @return The `PawnStructureAnalyzer` instance.
   */
  public static PawnStructureAnalyzer get() {
    return INSTANCE;
  }

  /**
   * Evaluates the pawn structure of a player on the given chess board.
   * The evaluation includes factors such as isolated pawns, doubled pawns, backward pawns, connected passed pawns,
   * pawn chains, pawn majority, pawn islands, and pawn promotion.
   * Adjust the weights and scoring values based on testing and analysis.
   *
   * @param player The player whose pawn structure is being evaluated.
   * @param board The current state of the chess board.
   * @return The pawn structure score.
   */
  public int pawnStructureScore(final Player player, final Board board) {
    final int[] pawnsOnColumnTable = createPawnColumnTable(player);

    int pawnStructureScore = calculatePawnColumnStack(pawnsOnColumnTable);
    pawnStructureScore += calculateIsolatedPawnPenalty(pawnsOnColumnTable);
    //pawnStructureScore += calculateBackwardPawns(player, board); //Bug in logic
    pawnStructureScore += calculateConnectedPassedPawns(player, board);
    pawnStructureScore += calculatePawnChains(player, board);
    pawnStructureScore += calculatePawnMajority(pawnsOnColumnTable);
    pawnStructureScore += calculatePawnIslands(pawnsOnColumnTable);

    return pawnStructureScore;
  }

  /**
   * Calculates the bonus for connected passed pawns for the specified player on the given chess board.
   * A connected passed pawn is one that has no friendly pawns in front or on adjacent files.
   *
   * @param player The player whose connected passed pawns are being evaluated.
   * @param board The current state of the chess board.
   * @return The bonus for connected passed pawns.
   */
  private static int calculateConnectedPassedPawns(final Player player, final Board board) {
    int connectedPassedPawnBonus = 0;
    for (final Piece pawn : calculatePlayerPawns(player)) {
      if (isConnectedPassedPawn(pawn, board)) {
        connectedPassedPawnBonus += ConnectedPassedPawnBonus;
      }
    }
    return connectedPassedPawnBonus;
  }

  /**
   * Checks if the specified pawn is a connected passed pawn.
   * A connected passed pawn has no friendly pawns in front or on adjacent files.
   *
   * @param pawn The pawn being checked.
   * @param board The current state of the chess board.
   * @return True if the pawn is a connected passed pawn, false otherwise.
   */
  private static boolean isConnectedPassedPawn(final Piece pawn, final Board board) {
    final int[] pawnsOnColumnTable = createPawnColumnTable(board.currentPlayer());
    final int pawnSquare = pawn.getPiecePosition();
    final int file = pawnSquare % 8;
    final int rank = pawnSquare / 8;

    // Check if there are no friendly pawns in front or on adjacent files
    for (int i = Math.max(0, file - 1); i <= Math.min(7, file + 1); i++) {
      if (pawnsOnColumnTable[rank] > 0) {
        return false;
      }
    }

    return true;
  }

  /**
   * Calculates the bonus for pawn chains for the specified player on the given chess board.
   * A pawn chain is a connected sequence of pawns on adjacent files.
   *
   * @param player The player whose pawn chains are being evaluated.
   * @param board The current state of the chess board.
   * @return The bonus for pawn chains.
   */
  private static int calculatePawnChains(final Player player, final Board board) {
    int pawnChainBonus = 0;
    for (final Piece pawn : calculatePlayerPawns(player)) {
      if (isPawnChain(pawn, board)) {
        pawnChainBonus += PawnChainBonus;
      }
    }
    return pawnChainBonus;
  }

  /**
   * Checks if the specified pawn is part of a pawn chain.
   * A pawn chain is a connected sequence of pawns on adjacent files.
   *
   * @param pawn The pawn being checked.
   * @param board The current state of the chess board.
   * @return True if the pawn is part of a pawn chain, false otherwise.
   */
  private static boolean isPawnChain(final Piece pawn, final Board board) {
    final int[] pawnsOnColumnTable = createPawnColumnTable(board.currentPlayer());
    final int pawnSquare = pawn.getPiecePosition();
    final int file = pawnSquare % 8;

    for (int i = Math.max(0, file - 1); i <= Math.min(7, file + 1); i++) {
      if (pawnsOnColumnTable[i] > 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Calculates the bonus for pawn majority for the specified player.
   * A pawn majority occurs when the player has more pawns on one side of the board than the opponent.
   *
   * @param pawnsOnColumnTable The pawn distribution on each column.
   * @return The bonus for pawn majority.
   */
  private static int calculatePawnMajority(final int[] pawnsOnColumnTable) {
    int pawnMajorityBonus = 0;
    int halfBoard = 4;

    for (int i = 0; i < halfBoard; i++) {
      pawnMajorityBonus += PawnMajorityBonus * pawnsOnColumnTable[i];
    }

    for (int i = halfBoard; i < 8; i++) {
      pawnMajorityBonus -= PawnMajorityBonus * pawnsOnColumnTable[i];
    }

    return pawnMajorityBonus;
  }

  /**
   * Calculates the penalty for pawn islands for the specified player.
   * A pawn island is a group of one or more connected pawns separated from other pawns by empty files.
   *
   * @param pawnsOnColumnTable The pawn distribution on each column.
   * @return The penalty for pawn islands.
   */
  private static int calculatePawnIslands(final int[] pawnsOnColumnTable) {
    int count = 0;

    for (int i = 0; i < 8; i++) {
      if (pawnsOnColumnTable[i] > 0) {
        if (i > 0 && pawnsOnColumnTable[i - 1] == 0) {
          count++;
        }
      }
    }

    return count * PawnIslandPenalty;
  }

  /**
   * Calculates the penalty for each isolated pawn for the specified player on the given chess board.
   * An isolated pawn is one that has no friendly pawns on adjacent files.
   *
   * @param pawnsOnColumnTable The pawn distribution on each column.
   * @return The penalty for isolated pawns.
   */
  private static int calculateIsolatedPawnPenalty(final int[] pawnsOnColumnTable) {
    int count = 0;

    if (pawnsOnColumnTable[0] > 0 && pawnsOnColumnTable[1] == 0) {
      count += pawnsOnColumnTable[0];
    }

    if (pawnsOnColumnTable[7] > 0 && pawnsOnColumnTable[6] == 0) {
      count += pawnsOnColumnTable[7];
    }

    for (int i = 1; i < pawnsOnColumnTable.length - 1; i++) {
      if (pawnsOnColumnTable[i - 1] == 0 && pawnsOnColumnTable[i + 1] == 0) {
        count += pawnsOnColumnTable[i];
      }
    }

    return count * IsolatedPawnPenalty;
  }

  /**
   * Calculates the penalty for each doubled pawn for the specified player on the given chess board.
   * A doubled pawn is one that has another pawn on the same file.
   *
   * @param pawnsOnColumnTable The pawn distribution on each column.
   * @return The penalty for doubled pawns.
   */
  private static int calculatePawnColumnStack(final int[] pawnsOnColumnTable) {
    int pawnStackPenalty = 0;
    for (final int pawnStack : pawnsOnColumnTable) {
      if (pawnStack > 1) {
        pawnStackPenalty += pawnStack;
      }
    }
    return pawnStackPenalty * DoubledPawnPenalty;
  }

  /**
   * Creates a table representing the distribution of player pawns on each column of the chess board.
   *
   * @param player The current player
   * @return An array representing the pawn distribution on each column.
   */
  private static int[] createPawnColumnTable(final Player player) {
    final Collection<Piece> pieces = player.getActivePieces();
    Collection<Piece> playerPawns = new ArrayList<>();
    for(Piece piece : pieces) {
      if(piece instanceof Pawn) {
        playerPawns.add(piece);
      }
    }
    final int[] table = new int[8];
    for (final Piece playerPawn : playerPawns) {
      table[playerPawn.getPiecePosition() % 8]++;
    }
    return table;
  }

  /**
   * Calculates a list of opponent pawns for the specified player.
   *
   * @param player The opponent player.
   * @return A list of opponent pawns.
   */
  private static List<Piece> calculatePlayerPawns(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN)
            .collect(Collectors.toList());
  }
}
