package engine.forPlayer.forAI;

import com.google.common.collect.ImmutableList;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The RookStructureAnalyzer class is responsible for analyzing the rook structure on the chess board,
 * calculating bonuses based on open files for rooks, and providing a score for rook-related factors.
 */
public final class RookStructureAnalyzer {

  /*** The singleton instance of RookStructureAnalyzer. */
  private static final RookStructureAnalyzer Instance = new RookStructureAnalyzer();

  /*** The list of columns on the chess board. */
  private static final List < List < Boolean >> BOARD_COLUMNS = initColumns();

  /*** Bonus applied for rooks on an open file. */
  private static final int OpenFileControlBonus = 10;

  /*** Bonus applied when a rook can lift itself to a higher rank position on the board. */
  private static final int RookLiftBonus = 5;

  /*** Bonus applied when the rooks can "see" each other on the board. */
  private static final int ConnectedRookBonus = 8;

  /*** Bonus applied when there is a half open file controlled by the player's rook. */
  private static final int HalfOpenFileBonus = 6;

  /*** Bonus applied when a rook threatens the opponents king. */
  private static final int UnsafeKingBonus = 8;

  /*** A private constructor to prevent instantiation outside of class. */
  private RookStructureAnalyzer() {}

  /**
   * Get the singleton instance of the RookStructureAnalyzer.
   *
   * @return The instance of RookStructureAnalyzer.
   */
  public static RookStructureAnalyzer get() {
    return Instance;
  }

  private static List<List<Boolean>> initColumns() {
    final List<List<Boolean>> columns = new ArrayList<>();
    columns.add(BoardUtils.Instance.FirstColumn);
    columns.add(BoardUtils.Instance.SecondColumn);
    columns.add(BoardUtils.Instance.ThirdColumn);
    columns.add(BoardUtils.Instance.FourthColumn);
    columns.add(BoardUtils.Instance.FifthColumn);
    columns.add(BoardUtils.Instance.SixthColumn);
    columns.add(BoardUtils.Instance.SeventhColumn);
    columns.add(BoardUtils.Instance.EighthColumn);
    return ImmutableList.copyOf(columns);
  }

  /**
   * Calculate the rook structure score based on open files and the positions of rooks.
   *
   * @param player The player to evaluate.
   * @param board  The chess board to evaluate.
   * @return The rook structure score.
   */
  public int rookStructureScore(final Player player, final Board board) {
    final int[] rookOnColumnTable = createRookColumnTable(calculatePlayerRooks(player));

    int score = 0;
    score += calculateOpenFileControlBonus(rookOnColumnTable, board);
    score += calculateConnectedRookBonus(rookOnColumnTable);
    //  score += calculateRookOn7thRankBonus(rookOnColumnTable);
    //  Alternative to bonus applied via `Alliance`
    score += calculateRookLiftBonus(player, board);
    score += calculateHalfOpenFileBonus(rookOnColumnTable, board);
    score += calculateOpenFileControlBonus(rookOnColumnTable, board);
    score += calculateUnsafeKingBonus(player, board);

    return score;
  }

  /**
   * Calculate the bonus for connected rooks.
   *
   * @param rookOnColumnTable The table of rook counts per column.
   * @return The connected rook bonus.
   */
  private static int calculateConnectedRookBonus(final int[] rookOnColumnTable) {
    int score = 0;
    for (int i = 0; i < rookOnColumnTable.length - 1; i++) {
      if (rookOnColumnTable[i] > 0 && rookOnColumnTable[i + 1] > 0) {
        score += ConnectedRookBonus;
      }
    }
    return score;
  }

  /**
   * Calculate the bonus for a rook on the 7th rank.
   *
   * @param rookOnColumnTable The table of rook counts per column.
   * @return The rook on 7th rank bonus.
   */
  private static int calculateRookOn7thRankBonus(final int[] rookOnColumnTable) {
    return 0; //See `rookStructureScore`
    //return rookOnColumnTable[6] > 0 ? ROOK_ON_7TH_RANK_BONUS * rookOnColumnTable[6] : NO_BONUS;
  }

  /**
   * Calculate the bonus for a rook lift.
   *
   * @param player The player for whom to calculate rook lift.
   * @param board  The chess board.
   * @return The rook lift bonus.
   */
  private static int calculateRookLiftBonus(final Player player, final Board board) {
    int bonus = 0;
    final Piece rook = player.getActivePieces()
            .stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.ROOK)
            .findFirst()
            .orElse(null);

    if (rook != null) {
      final int rank = rook.getPiecePosition() / 8;
      if (rank > 1) {
        bonus += RookLiftBonus;
      }
    }
    return bonus;
  }

  /**
   * Calculate the bonus for a rook on a half-open file.
   *
   * @param rookOnColumnTable The table of rook counts per column.
   * @param board              The chess board.
   * @return The half-open file bonus.
   */
  private static int calculateHalfOpenFileBonus(final int[] rookOnColumnTable, final Board board) {
    int bonus = 0;
    for (int i = 0; i < rookOnColumnTable.length; i++) {
      if (rookOnColumnTable[i] > 0) {
        final int file = i;
        final boolean isHalfOpen = board.getAllPieces().stream()
                .filter(piece -> piece.getPieceType() == Piece.PieceType.PAWN)
                .noneMatch(piece -> piece.getPiecePosition() % 8 == file);

        if (isHalfOpen) {
          bonus += HalfOpenFileBonus * rookOnColumnTable[i];
        }
      }
    }
    return bonus;
  }

  /**
   * Calculate the bonus for control of an open file.
   *
   * @param rookOnColumnTable The table of rook counts per column.
   * @param board              The chess board.
   * @return The open file control bonus.
   */
  private static int calculateOpenFileControlBonus(final int[] rookOnColumnTable, final Board board) {
    int bonus = 0;
    for (int i = 0; i < rookOnColumnTable.length; i++) {
      if (rookOnColumnTable[i] > 0) {
        final int file = i;
        final boolean isOpenFile = board.getAllPieces().stream()
                .filter(piece -> piece.getPieceType() == Piece.PieceType.ROOK)
                .anyMatch(piece -> piece.getPiecePosition() % 8 == file);

        if (isOpenFile) {
          bonus += OpenFileControlBonus * rookOnColumnTable[i];
        }
      }
    }
    return bonus;
  }

  /**
   * Calculate the penalty for an unsafe opponent king due to rooks.
   *
   * @param player The player to evaluate.
   * @param board  The chess board to evaluate.
   * @return The penalty for an unsafe opponent king.
   */
  private static int calculateUnsafeKingBonus(final Player player, final Board board) {
    final Player opponent = player.getOpponent();
    final int opponentKingPosition = opponent.getPlayerKing().getPiecePosition();
    for (final Piece piece: player.getActivePieces()) {
      if (piece.getPieceType() == Piece.PieceType.ROOK) {
        final Collection <Move> moves = piece.calculateLegalMoves(board);
        for (final Move move: moves) {
          final Collection<Move> oneMove = ImmutableList.of(move);
          if(!Player.calculateAttacksOnTile(opponentKingPosition, oneMove).isEmpty()) {
            return UnsafeKingBonus;
          }
        }
      }
    }
    return 0;
  }


  /**
   * Create a table that represents the number of rooks on each column.
   *
   * @param playerRooks The rooks owned by the player.
   * @return The table containing rook counts per column.
   */
  private static int[] createRookColumnTable(final Collection <Piece> playerRooks) {
    final int[] table = new int[8];
    for (final Piece playerRook: playerRooks) {
      table[playerRook.getPiecePosition() % 8]++;
    }
    return table;
  }

  /**
   * Calculate the rooks owned by the player.
   *
   * @param player The player for whom to calculate rooks.
   * @return A collection of rooks owned by the player.
   */
  private static Collection<Piece> calculatePlayerRooks(final Player player) {
    return player.getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == Piece.PieceType.ROOK)
            .collect(ImmutableList.toImmutableList());
  }

  /**
   * Create a table that represents the number of pieces on each column.
   *
   * @param board The chess board.
   * @return The table containing piece counts per column.
   */
  private static int[] createPiecesOnColumnTable(final Board board) {
    final int[] piecesOnColumnTable = new int[BOARD_COLUMNS.size()];
    for (final Piece piece: board.getAllPieces()) {
      for (int i = 0; i < BOARD_COLUMNS.size(); i++) {
        if (BOARD_COLUMNS.get(i).get(piece.getPiecePosition())) {
          piecesOnColumnTable[i]++;
        }
      }
    }
    return piecesOnColumnTable;
  }

}