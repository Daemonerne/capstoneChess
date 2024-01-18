package engine.forBoard;

import engine.forPiece.King;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.*;

import static engine.forBoard.Move.MoveFactory.getNullMove;

/**
 * The BoardUtils class provides methods for checking valid tile coordinates and converting between positions and coordinates.
 * It also includes constants for file names, rank names, and other board-related values. This class cannot be instantiated.
 * It is designed as an enum singleton.
 * <br><br>
 * This utility class includes methods for various board-related calculations and checks, such as determining if a tile
 * coordinate is valid, generating column and row lists, managing algebraic notation, checking for threats to the king,
 * evaluating piece values, and identifying endgame conditions.
 *
 * @author Aaron Ho
 */
public enum BoardUtils {

  /*** The singleton instance of the BoardUtils class. */
  Instance;

  /*** A list indicating whether each tile belongs to the first column on the chessboard. */
  public final List < Boolean > FirstColumn = initColumn(0);
  
  /*** A list indicating whether each tile belongs to the second column on the chessboard. */
  public final List < Boolean > SecondColumn = initColumn(1);
  
  /*** A list indicating whether each tile belongs to the third column on the chessboard. */
  public final List < Boolean > ThirdColumn = initColumn(2);
  
  /*** A list indicating whether each tile belongs to the fourth column on the chessboard. */
  public final List < Boolean > FourthColumn = initColumn(3);
  
  /*** A list indicating whether each tile belongs to the fifth column on the chessboard. */
  public final List < Boolean > FifthColumn = initColumn(4);
  
  /*** A list indicating whether each tile belongs to the sixth column on the chessboard. */
  public final List < Boolean > SixthColumn = initColumn(5);
  
  /*** A list indicating whether each tile belongs to the seventh column on the chessboard. */
  public final List < Boolean > SeventhColumn = initColumn(6);
  
  /*** A list indicating whether each tile belongs to the eighth column on the chessboard. */
  public final List < Boolean > EighthColumn = initColumn(7);
  
  /*** A list indicating whether each tile belongs to the first row on the chessboard. */
  public final List < Boolean > FirstRow = initRow(0);
  
  /*** A list indicating whether each tile belongs to the second row on the chessboard. */
  public final List < Boolean > SecondRow = initRow(8);
  
  /*** A list indicating whether each tile belongs to the third row on the chessboard. */
  public final List < Boolean > ThirdRow = initRow(16);
  
  /*** A list indicating whether each tile belongs to the fourth row on the chessboard. */
  public final List < Boolean > FourthRow = initRow(24);
  
  /*** A list indicating whether each tile belongs to the fifth row on the chessboard. */
  public final List < Boolean > FifthRow = initRow(32);
  
  /*** A list indicating whether each tile belongs to the sixth row on the chessboard. */
  public final List < Boolean > SixthRow = initRow(40);
  
  /*** A list indicating whether each tile belongs to the seventh row on the chessboard. */
  public static final List < Boolean > SeventhRow = initRow(48);
  
  /*** A list indicating whether each tile belongs to the eighth row on the chessboard. */
  public final List < Boolean > EighthRow = initRow(56);
  
  /*** A list of algebraic notation representing each tile on the chessboard. */
  public static final List < String > ALGEBRAIC_NOTATION = initializeAlgebraicNotation();

  /*** The index of the starting tile. */
  public static final int START_TILE_INDEX = 0;
  
  /*** The number of tiles per row on the chessboard. */
  public static final int NUM_TILES_PER_ROW = 8;
  
  /*** The total number of tiles on the chessboard. */
  public static final int NUM_TILES = 64;

  /**
   * Initializes a list of boolean values representing the tiles in a specified column.
   *
   * @param columnNumber The column number for which to generate the list.
   * @return A list of boolean values indicating the tiles in the specified column.
   */
  private static List < Boolean > initColumn(int columnNumber) {

    final Boolean[] column = new Boolean[NUM_TILES];
    Arrays.fill(column, false);
    do {
      column[columnNumber] = true;
      columnNumber += NUM_TILES_PER_ROW;
    } while (columnNumber < NUM_TILES);
    return List.of((column));
  }

  /**
   * Initializes a list of boolean values representing the tiles in a specified row.
   *
   * @param rowNumber The row number for which to generate the list.
   * @return A list of boolean values indicating the tiles in the specified row.
   */
  private static List < Boolean > initRow(int rowNumber) {
    final Boolean[] row = new Boolean[NUM_TILES];
    Arrays.fill(row, false);
    do {
      row[rowNumber] = true;
      rowNumber++;
    } while (rowNumber % NUM_TILES_PER_ROW != 0);
    return List.of(row);
  }

  /**
   * Initializes a list of strings representing algebraic notation positions for each tile on the chessboard.
   *
   * @return A list of algebraic notation positions.
   */
  private static List < String > initializeAlgebraicNotation() {
    return List.of("a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
      "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
      "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
      "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
      "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
      "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
      "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
      "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1");
  }

  /**
   * Checks whether a given tile coordinate is within the valid range of tile coordinates on the chessboard.
   *
   * @param coordinate The tile coordinate to check.
   * @return True if the coordinate is valid, false otherwise.
   */
  public static boolean isValidTileCoordinate(final int coordinate) {

    return coordinate >= START_TILE_INDEX && coordinate < NUM_TILES;

  }

  /**
   * Returns the algebraic notation position corresponding to a given tile coordinate.
   *
   * @param coordinate The tile coordinate to convert.
   * @return The corresponding algebraic notation position.
   */
  public static String getPositionAtCoordinate(final int coordinate) {
    return ALGEBRAIC_NOTATION.get(coordinate);
  }

  /**
   * Checks if making a specific move puts the king in threat.
   *
   * @param move The move to evaluate.
   * @return True if the move puts the king in threat, false otherwise.
   */
  public static boolean kingThreat(final Move move) {
    final Board board = move.getBoard();
    final MoveTransition transition = board.currentPlayer().makeMove(move);
    return transition.toBoard().currentPlayer().isInCheck();
  }

  /**
   * Checks if a king is pawn-trapped, meaning it is blocked by pawns and unable to move.
   *
   * @param board The current state of the board.
   * @param king The king to check.
   * @param frontTile The tile in front of the king.
   * @return True if the king is pawn-trapped, false otherwise.
   */
  public static boolean isKingPawnTrap(final Board board,
    final King king,
    final int frontTile) {
    final Piece piece = board.getPiece(frontTile);
    return piece == null ||
      piece.getPieceType() != Piece.PieceType.PAWN ||
      piece.getPieceAllegiance() == king.getPieceAllegiance();
  }

  /**
   * Calculates the Modified Value, Victim, The Least Value, Victim Attacker (MVVLVA) score for a move.
   *
   * @param move The move for which to calculate the MVVLVA score.
   * @return The MVVLVA score.
   */
  public static int mvvlva(final Move move) {
    final Piece movingPiece = move.getMovedPiece();
    if (move.isAttack()) {
      final Piece attackedPiece = move.getAttackedPiece();
      return (attackedPiece.getPieceValue() - movingPiece.getPieceValue() + Piece.PieceType.KING.getPieceValue()) * 100;
    }
    return Piece.PieceType.KING.getPieceValue() - movingPiece.getPieceValue();
  }

  /**
   * Retrieves the last N moves from the move history of a board.
   *
   * @param board The board to retrieve moves from.
   * @param N The number of moves to retrieve.
   * @return A list of the last N moves.
   */
  public static List <Move> lastNMoves(final Board board, int N) {
    final List < Move > moveHistory = new ArrayList < > ();
    Move currentMove = board.getTransitionMove();
    int i = 0;
    while (currentMove != getNullMove() && i < N) {
      moveHistory.add(currentMove);
      currentMove = currentMove.getBoard().getTransitionMove();
      i++;
    }
    return Collections.unmodifiableList(moveHistory);
  }

  /**
   * Checks if the current board is in an endgame state, either checkmate or stalemate.
   *
   * @param board The current state of the board.
   * @return True if the game is in an endgame state, false otherwise.
   */
  public static boolean isEndOfGame(final Board board) {
    return board.currentPlayer().isInCheckMate() ||
      board.currentPlayer().isInStaleMate();
  }

  /**
   * Checks if the current game is in the opening stage.
   *
   * @param board The current state of the board.
   * @return True if the game is in the opening phase, false otherwise.
   */
  public static boolean isOpening(final Board board) {

      return lastNMoves(board, 100).size() < 12;
  }


  /**
   * Calculates the Static Exchange Evaluation (SEE) value for a given move on the board.
   *
   * @param board The current chess board.
   * @param move  The move for which SEE is calculated.
   * @return The SEE value for the move.
   */
  public static int see(Board board, Move move) {
      int toSquare = move.getDestinationCoordinate();

    int result;

    // Make the move on a temporary board
    Board tempBoard = board.currentPlayer().makeMove(move).toBoard();

    // Get the value of the captured piece
    int capturedValue = move.getAttackedPiece().getPieceValue();

    // Recursively evaluate the next capture
    int nextSeeValue = seeRecursively(tempBoard, toSquare, -capturedValue);

    // The result is the negation of the recursive value if the move is not a capture
    result = !(move.isAttack()) ? -nextSeeValue : nextSeeValue;

    return result;
  }

  /**
   * Recursively calculates the Static Exchange Evaluation (SEE) value for a given square on the board.
   *
   * @param board            The current chess board.
   * @param square           The square for which SEE is calculated.
   * @param accumulatedValue The accumulated value of the evaluation.
   * @return The SEE value for the square.
   */
  private static int seeRecursively(Board board, int square, int accumulatedValue) {
    // Base case: If the square is empty, return the accumulated value
    if (board.getPiece(square) == null) {
      return accumulatedValue;
    }

    // Get the piece on the square
    Piece piece = board.getPiece(square);

    // Update the accumulated value based on the piece value
    accumulatedValue += pieceValue(piece);

    // Generate all captures from the square
    for(final Move captureMove : Player.calculateAttacksOnTile(square, board.currentPlayer().getOpponent().getLegalMoves())) {
      // Recursive call for the next capture
      int nextSeeValue = seeRecursively(board, captureMove.getDestinationCoordinate(), -accumulatedValue);

      // Update the result based on the negation of the recursive value
      accumulatedValue = Math.max(accumulatedValue, -nextSeeValue);
    }

    return accumulatedValue;
  }

  /**
   * Calculates the value of a piece for SEE based on its type.
   *
   * @param piece The chess piece.
   * @return The value of the piece for SEE.
   */
  private static int pieceValue(Piece piece) {
    if (piece != null) {
      switch (piece.getPieceType()) {
        case PAWN:
          return 1;
        case KNIGHT:
        case BISHOP:
          return 3;
        case ROOK:
          return 5;
        case QUEEN:
          return 9;
      }
    }
    return 0; // Empty square
  }
}
