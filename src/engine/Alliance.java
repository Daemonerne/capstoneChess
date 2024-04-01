package engine;

import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forPlayer.BlackPlayer;
import engine.forPlayer.Player;
import engine.forPlayer.WhitePlayer;

/**
 * The `Alliance` enum represents the two player alliances in a game of chess: White and Black.
 * Each alliance has its own set of characteristics, such as movement direction, pawn promotion criteria,
 * and positional bonuses for different pieces on the chessboard. This enum encapsulates the behavior and
 * attributes associated with each alliance, providing a structured way to handle alliance-specific logic
 * within a chess engine. The class includes methods for retrieving movement directions, pawn promotion checks,
 * choosing player objects based on the alliance, and calculates positional bonuses for different chess pieces.
 *
 * @author Aaron Ho
 */
public enum Alliance {

  /*** An enumeration representing white in a game of chess. */
  WHITE() {

    /**
     * Gets the direction in which the pieces of this alliance move on the board.
     *
     * @return The direction: UP_DIRECTION (-1).
     */
    @Override
    public int getDirection() {
      return UP_DIRECTION;
    }

    /**
     * Gets the opposite direction to the one in which the pieces of this alliance move on the board.
     *
     * @return The opposite direction: DOWN_DIRECTION (1).
     */
    @Override
    public int getOppositeDirection() {
      return DOWN_DIRECTION;
    }

    /**
     * Checks if a given position on the board is a square where a pawn of this alliance can be promoted.
     *
     * @param position The position to check.
     * @return         True if it is a pawn promotion square, false otherwise.
     */
    @Override
    public boolean isPawnPromotionSquare(final int position) {
      return BoardUtils.Instance.FirstRow.get(position);
    }

    /**
     * Chooses the player object that represents this alliance.
     *
     * @param whitePlayer The WhitePlayer object.
     * @param blackPlayer The BlackPlayer object.
     * @return            The WhitePlayer object.
     */
    @Override
    public Player choosePlayerByAlliance(final WhitePlayer whitePlayer, final BlackPlayer blackPlayer) {
      return whitePlayer;
    }

    /**
     * Gets a string representation of this alliance.
     *
     * @return "White".
     */
    @Override
    public String toString() {
      return "White";
    }

    /**
     * Calculates the bonus for a pawn at a given position on the board.
     *
     * @param position The position of the pawn.
     * @param board    The current chess board.
     * @return         The pawn bonus.
     */
    @Override
    public int pawnBonus(final int position, final Board board) {
      return WHITE_PAWN_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a knight at a given position on the board.
     *
     * @param position The position of the knight.
     * @param board    The current chess board.
     * @return         The knight bonus.
     */
    @Override
    public int knightBonus(final int position, final Board board) {
      return WHITE_KNIGHT_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a bishop at a given position on the board.
     *
     * @param position The position of the bishop.
     * @param board    The current chess board.
     * @return         The bishop bonus.
     */
    @Override
    public int bishopBonus(final int position, final Board board) {
      return WHITE_BISHOP_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a rook at a given position on the board.
     *
     * @param position The position of the rook.
     * @param board    The current chess board.
     * @return         The rook bonus.
     */
    @Override
    public int rookBonus(final int position, final Board board) {
      return WHITE_ROOK_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a queen at a given position on the board.
     *
     * @param position The position of the queen.
     * @param board    The current chess board.
     * @return         The queen bonus.
     */
    @Override
    public int queenBonus(final int position, final Board board) {
      if (BoardUtils.isOpening(board) && !(board.getPiece(position).isFirstMove())) {
        return -250;
      }
      return 0;
    }

    /**
     * Calculates the bonus for a king at a given position on the board.
     *
     * @param position The position of the king.
     * @param board    The current chess board.
     * @return         The king bonus.
     */
    @Override
    public int kingBonus(final int position, final Board board) {
      return 0;
    }

    /**
     * Checks if the alliance is white.
     *
     * @return True if white, false otherwise.
     */
    @Override
    public boolean isWhite() {
      return true;
    }

    /**
     * Checks if the alliance is black.
     *
     * @return True if black, false otherwise.
     */
    @Override
    public boolean isBlack() {
      return false;
    }
  },

  /*** An enumeration representing black in a game of chess. */
  BLACK() {

    /**
     * Gets the direction in which the pieces of this alliance move on the board.
     *
     * @return The direction: DOWN_DIRECTION (1).
     */
    @Override
    public int getDirection() {
      return DOWN_DIRECTION;
    }

    /**
     * Gets the opposite direction to the one in which the pieces of this alliance move on the board.
     *
     * @return The opposite direction: UP_DIRECTION (-1).
     */
    @Override
    public int getOppositeDirection() {
      return UP_DIRECTION;
    }

    /**
     * Checks if a given position on the board is a square where a pawn of this alliance can be promoted.
     *
     * @param position The position to check.
     * @return         True if it is a pawn promotion square, false otherwise.
     */
    @Override
    public boolean isPawnPromotionSquare(final int position) {
      return BoardUtils.Instance.EighthRow.get(position);
    }

    /**
     * Chooses the player object that represents this alliance.
     *
     * @param whitePlayer The WhitePlayer object.
     * @param blackPlayer The BlackPlayer object.
     * @return            The BlackPlayer object.
     */
    @Override
    public Player choosePlayerByAlliance(final WhitePlayer whitePlayer, final BlackPlayer blackPlayer) {
      return blackPlayer;
    }

    /**
     * Gets a string representation of this alliance.
     *
     * @return "Black".
     */
    @Override
    public String toString() {
      return "Black";
    }

    /**
     * Calculates the bonus for a pawn at a given position on the board.
     *
     * @param position The position of the pawn.
     * @param board    The current chess board.
     * @return         The pawn bonus.
     */
    @Override
    public int pawnBonus(final int position, final Board board) {
      return BLACK_PAWN_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a knight at a given position on the board.
     *
     * @param position The position of the knight.
     * @param board    The current chess board.
     * @return         The knight bonus.
     */
    @Override
    public int knightBonus(final int position, final Board board) {
      return BLACK_KNIGHT_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a bishop at a given position on the board.
     *
     * @param position The position of the bishop.
     * @param board    The current chess board.
     * @return         The bishop bonus.
     */
    @Override
    public int bishopBonus(final int position, final Board board) {
      return BLACK_BISHOP_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a rook at a given position on the board.
     *
     * @param position The position of the rook.
     * @param board    The current chess board.
     * @return         The rook bonus.
     */
    @Override
    public int rookBonus(final int position, final Board board) {
      return BLACK_ROOK_PREFERRED_COORDINATES[position];
    }

    /**
     * Calculates the bonus for a queen at a given position on the board.
     *
     * @param position The position of the queen.
     * @param board    The current chess board.
     * @return         The queen bonus.
     */
    @Override
    public int queenBonus(final int position, final Board board) {
      if (BoardUtils.isOpening(board) && !(board.getPiece(position).isFirstMove())) {
        return -250;
      } else return 0;
    }

    /**
     * Calculates the bonus for a king at a given position on the board.
     *
     * @param position The position of the king.
     * @param board    The current chess board.
     * @return         The king bonus.
     */
    @Override
    public int kingBonus(final int position, final Board board) {
      return 0;
    }

    /**
     * Checks if the alliance is white.
     *
     * @return True if white, false otherwise.
     */
    @Override
    public boolean isWhite() {
      return false;
    }

    /**
     * Checks if the alliance is black.
     *
     * @return True if black, false otherwise.
     */
    @Override
    public boolean isBlack() {
      return true;
    }
  };

  /**
   * Gets the direction in which the pieces of this alliance move on the board.
   *
   * @return The direction.
   */
  public abstract int getDirection();

  /**
   * Gets the opposite direction to the one in which the pieces of this alliance move on the board.
   *
   * @return The opposite direction.
   */
  public abstract int getOppositeDirection();

  /**
   * Calculates the bonus for a pawn at a given position on the board.
   *
   * @param position The position of the pawn.
   * @param board    The current chess board.
   * @return         The pawn bonus.
   */
  public abstract int pawnBonus(final int position, final Board board);

  /**
   * Calculates the bonus for a knight at a given position on the board.
   *
   * @param position The position of the knight.
   * @param board    The current chess board.
   * @return         The knight bonus.
   */
  public abstract int knightBonus(final int position, final Board board);

  /**
   * Calculates the bonus for a bishop at a given position on the board.
   *
   * @param position The position of the bishop.
   * @param board    The current chess board.
   * @return         The bishop bonus.
   */
  public abstract int bishopBonus(final int position, final Board board);

  /**
   * Calculates the bonus for a rook at a given position on the board.
   *
   * @param position The position of the rook.
   * @param board    The current chess board.
   * @return         The rook bonus.
   */
  public abstract int rookBonus(final int position, final Board board);

  /**
   * Calculates the bonus for a queen at a given position on the board.
   *
   * @param position The position of the queen.
   * @param board    The current chess board.
   * @return         The queen bonus.
   */
  public abstract int queenBonus(final int position, final Board board);

  /**
   * Calculates the bonus for a king at a given position on the board.
   *
   * @param position The position of the king.
   * @param board    The current chess board.
   * @return         The king bonus.
   */
  public abstract int kingBonus(final int position, final Board board);

  /**
   * Checks if the alliance is white.
   *
   * @return True if white, false otherwise.
   */
  public abstract boolean isWhite();

  /**
   * Checks if the alliance is black.
   *
   * @return True if black, false otherwise.
   */
  public abstract boolean isBlack();

  /**
   * Checks if a given position on the board is a square where a pawn of this alliance can be promoted.
   *
   * @param position The position to check.
   * @return         True if it is a pawn promotion square, false otherwise.
   */
  public abstract boolean isPawnPromotionSquare(final int position);

  /**
   * Chooses the player object that represents this alliance.
   *
   * @param whitePlayer The WhitePlayer object.
   * @param blackPlayer The BlackPlayer object.
   * @return            The chosen player object.
   */
  public abstract Player choosePlayerByAlliance(final WhitePlayer whitePlayer, final BlackPlayer blackPlayer);

  /*** A constant that represents the upward direction. For example, pawns facing this direction move toward the higher-ranked rows. */
  private final static int UP_DIRECTION = -1;

  /*** A constant that represents the downward direction. For example, pawns facing this direction move toward the lower-ranked rows. */
  private final static int DOWN_DIRECTION = 1;

  /**
   * Represents the preferred coordinates for white pawns on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for white pawns.
   * The values are tuned to reflect strategic advantages and disadvantages for white pawns in different positions.
   */
  private final static int[] WHITE_PAWN_PREFERRED_COORDINATES = {
          0,  0,  0,  0,  0,  0,  0,  0,
          50, 50, 50, 50, 50, 50, 50, 50,
          10, 10, 30, 40, 40, 30, 10, 10,
          5,  5, 15, 35, 35, 15,  5,  5,
          0,  0,  5, 30, 30,  5,  0,  0,
          4, 0,-10,  -5,  -5,-10, 0,  4,
          10, 10, 10,-20,-20, 10, 10, 10,
          0,  0,  0,  0,  0,  0,  0,  0
  };

  /**
   * Represents the preferred coordinates for black pawns on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for black pawns.
   * The values are tuned to reflect strategic advantages and disadvantages for black pawns in different positions.
   */
  private final static int[] BLACK_PAWN_PREFERRED_COORDINATES = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10,-20,-20, 10, 10,  5,
          5, 0,-10,  -5, -5, -10, 0,  5,
          0,  0,  5, 30, 30,  5,  0,  0,
          5,  5, 15, 35, 35, 15,  5,  5,
          10, 10, 30, 40, 40, 30, 10, 10,
          50, 50, 50, 50, 50, 50, 50, 50,
          0,  0,  0,  0,  0,  0,  0,  0
  };

  /**
   * Represents the preferred coordinates for white knights on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for white knights.
   * The values are tuned to reflect strategic advantages and disadvantages for white knights in different positions.
   */
  private final static int[] WHITE_KNIGHT_PREFERRED_COORDINATES = {
          -20,-20,-20,-20,-20,-20,-20,-20,
          -20,-20,  0,  5,  5,  0,-20,-20,
          -20,  5, 10, 15, 15, 10,  5,-20,
          -15,  0, 15, 20, 20, 15,  0,-15,
          -15,  5, 15, 20, 20, 15,  5,-15,
          -20,  0, 5, 15, 15, 10,  0, -20,
          -20,-20,  0, 10, 7,  0,-20, -20,
          -20,-15,-20,-20,-20,-20,-15,-20
  };

  /**
   * Represents the preferred coordinates for black knights on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for black knights.
   * The values are tuned to reflect strategic advantages and disadvantages for black knights in different positions.
   */
  private final static int[] BLACK_KNIGHT_PREFERRED_COORDINATES = {
          -20,-20,-20,-20,-20,-20,-20,-20,
          -20,-20,  0, 10, 7,  0, -20,-20,
          -20,  5, 10, 15, 15, 5,  5, -20,
          -15,  0, 15, 20, 20, 15, 0, -15,
          -15,  5, 15, 20, 20, 15, 5, -15,
          -20,  0, 10, 15, 15, 10, 0, -20,
          -20,-20,  0,  0,  0,  0,-20,-20,
          -20,-20,-20,-20,-20,-20,-20,-20
  };

  /**
   * Represents the preferred coordinates for white bishops on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for white bishops.
   * The values are tuned to reflect strategic advantages and disadvantages for white bishops in different positions.
   */
  private final static int[] WHITE_BISHOP_PREFERRED_COORDINATES = {
          -20,-10,-10,-10,-10,-10,-10,-20,
          -10,  0,  0,  0,  0,  0,  0,-10,
          -10,  0, 10, 10, 10, 10,  0,-10,
          -10,  5, 10, 15, 15, 10,  0,-10,
          -10,  5, 10, 15, 15, 10,  5,-10,
          -10,  9,  8, 10, 10, 8,   9,-10,
          -10,  10, 0, 0,  10, 0,  10,-10,
          -20,-10,-10,-10,-10,-10,-10,-20
  };

  /**
   * Represents the preferred coordinates for black bishops on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for black bishops.
   * The values are tuned to reflect strategic advantages and disadvantages for black bishops in different positions.
   */
  private final static int[] BLACK_BISHOP_PREFERRED_COORDINATES = {
          -20,-10,-10,-10,-10,-10,-10,-20,
          -10, 10, 0, 0,  15,  0,  10,-10,
          -10, 9,  8, 10, 10,  8,  9, -10,
          -10, 0, 10, 15, 15, 10,  0, -10,
          -10, 2, 10, 15, 15, 10,  2, -10,
          -10, 0, 10, 10, 10, 10,  0, -10,
          -10, 0,  0,  0,  0,  0,  0, -10,
          -20,-10,-10,-10,-10,-10,-10,-20
  };

  /**
   * Represents the preferred coordinates for white rooks on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for white rooks.
   * The values are tuned to reflect strategic advantages and disadvantages for white rooks in different positions.
   */
  private final static int[] WHITE_ROOK_PREFERRED_COORDINATES = {
          0,  0,  0,  0,  0,  0,  0, 0,
          5, 20, 20, 20, 20, 20, 20, 5,
          -5, 0,  0,  0,  0,  0,  0,-5,
          -5, 0,  0,  0,  0,  0,  0,-5,
          -5, 0,  0,  0,  0,  0,  0,-5,
          -5, 0,  0,  0,  0,  0,  0,-5,
          -5, 0,  0,  0,  0,  0,  0,-5,
          0,  0,  5,  5,  5,  0,  0, 0
  };

  /**
   * Represents the preferred coordinates for black rooks on the chessboard.
   * Each element in the array corresponds to a square on the board, providing positional bonuses for black rooks.
   * The values are tuned to reflect strategic advantages and disadvantages for black rooks in different positions.
   */
  private final static int[] BLACK_ROOK_PREFERRED_COORDINATES = {
          0,  0,  5,  5,  5,  0,  0,  0,
          -5,  0,  0,  0,  0,  0,  0,-5,
          -5,  0,  0,  0,  0,  0,  0,-5,
          -5,  0,  0,  0,  0,  0,  0,-5,
          -5,  0,  0,  0,  0,  0,  0,-5,
          -5,  0,  0,  0,  0,  0,  0,-5,
          5, 20, 20, 20, 20, 20, 20,  5,
          0,  0,  0,  0,  0,  0,  0,  0,
  };

}