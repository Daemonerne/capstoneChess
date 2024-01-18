package engine.forBoard;

import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPiece.Rook;

import java.util.Objects;

import static engine.forBoard.Board.Builder;

/**
 * The `Move` class represents a move in a game of chess.
 * It is a fundamental component of the chess engine, encapsulating the logic for executing and undoing various
 * types of moves on the chess board. This abstract class is extended by several specialized subclasses, each catering to
 * specific move scenarios. The MajorMove subclass represents a non-attacking move of a major piece (not a pawn), capturing
 * details of the current board state, the moved piece, and the destination coordinate. MajorAttackMove extends MajorMove
 * to encompass attacking moves, including information about the attacked piece. PawnMove and PawnAttackMove subclasses
 * handle pawn movements and attacks, distinguishing between regular and attacking pawn moves. The PawnEnPassantAttack
 * subclass manages en passant pawn attacks, ensuring accurate execution and undoing of such moves. PawnJump handles pawn's
 * initial two-square advances. CastleMove serves as the base class for castling moves, featuring details about the castling
 * rook, its starting and destination coordinates, and overrides the execute method to implement castling. KingSideCastleMove
 * and QueenSideCastleMove extend CastleMove for specific castling directions. AttackMove, a subclass of Move, specifically
 * deals with attacking moves, recording details about the attacked piece and providing accurate execution and undo mechanisms.
 * The NullMove subclass represents a placeholder for an invalid move, ensuring that attempts to execute it result in
 * exceptions, preventing unintended consequences. The PawnPromotion subclass represents a specialized pawn move involving
 * promotion, with decorators for executed moves and the promoted pawn, providing seamless execution and undoing of promotions.
 * The MoveFactory inner class acts as a factory for creating various types of moves, and its methods aid in move validation,
 * retrieval, and creation.
 * <br><br>
 * A known bug occurs occasionally when `.hashCode()` is called because the searching algorithm occasionally evaluates
 * a `NullMove` object, and a `NullPointerException` is thrown because of the reference to the null object.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
 
public abstract class Move {

  /*** The current state of the chess board. */
  protected final Board board;

  /*** The destination coordinate of the move. */
  protected final int destinationCoordinate;

  /*** The piece that is being moved. */
  protected Piece movedPiece = getMovedPiece();

  /*** Indicates whether it's the first move for the piece being moved. */
  protected boolean isFirstMove = true;

  /**
   * Creates a new Move object with the given board, moved piece, and destination coordinate.
   *
   * @param board The current state of the chess board.
   * @param pieceMoved The piece that is being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   */
  private Move(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
    this.board = board;
    this.destinationCoordinate = destinationCoordinate;
    this.movedPiece = pieceMoved;
    this.isFirstMove = pieceMoved.isFirstMove();
  }

  /**
   * Creates a new Move object with the given board and destination coordinate. This constructor is used for pawn jump moves.
   *
   * @param board The current state of the chess board.
   * @param destinationCoordinate The destination coordinate of the move.
   */
  private Move(final Board board, final int destinationCoordinate) {
    this.board = board;
    this.destinationCoordinate = destinationCoordinate;
  }

  /**
   * Calculates the hash code of the move.
   *
   * @return The calculated hash code.
   */
  @Override
  public int hashCode() {
    return Objects.hash(destinationCoordinate, movedPiece, movedPiece.getPiecePosition(), isFirstMove);
  }

  /**
   * Compares this move to another object to determine if they are equal.
   *
   * @param other The other object to compare to.
   * @return True if the moves are equal, false otherwise.
   */
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Move otherMove)) return false;
    return getCurrentCoordinate() == otherMove.getCurrentCoordinate() &&
           getDestinationCoordinate() == otherMove.getDestinationCoordinate() &&
           Objects.equals(movedPiece, otherMove.getMovedPiece());
  }

  /**
   * Gets the current state of the chess board.
   *
   * @return The current board.
   */
  public Board getBoard() {
    return this.board;
  }

  /**
   * Gets the current coordinate of the moved piece.
   *
   * @return The current coordinate.
   */
  public int getCurrentCoordinate() {
    return this.movedPiece.getPiecePosition();
  }

  /**
   * Gets the destination coordinate of the move.
   *
   * @return The destination coordinate.
   */
  public int getDestinationCoordinate() {
    return this.destinationCoordinate;
  }

  /**
   * Gets the piece that is being moved.
   *
   * @return The moved piece.
   */
  public Piece getMovedPiece() {
    return this.movedPiece;
  }

  /**
   * Checks if the move is an attack.
   *
   * @return True if the move is an attack, false otherwise.
   */
  public boolean isAttack() {
    return false;
  }

  /**
   * Checks if the move is a castling move.
   *
   * @return True if the move is a castling move, false otherwise.
   */
  public boolean isCastlingMove() {
    return false;
  }

  /**
   * Gets the attacked piece in case of an attack move.
   *
   * @return The attacked piece.
   */
  public Piece getAttackedPiece() {
    return null;
  }

  /**
   * Executes the move on the board and returns the resulting board.
   *
   * @return The board after executing the move.
   */
  public Board execute() {

    final Board.Builder builder = new Builder();
    this.board.currentPlayer().getActivePieces().stream().filter(piece -> !this.movedPiece.equals(piece)).forEach(builder::setPiece);
    this.board.currentPlayer().getOpponent().getActivePieces().forEach(builder::setPiece);
    builder.setPiece(this.movedPiece.movePiece(this));
    builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());
    builder.setMoveTransition(this);

    
    return builder.build();

  }

  /**
   * Undoes the move on the board and returns the resulting board.
   *
   * @return The board after undoing the move.
   */
  public Board undo() {
    final Board.Builder builder = new Builder();
    this.board.getAllPieces().forEach(builder::setPiece);
    builder.setMoveMaker(this.board.currentPlayer().getAlliance());
    return builder.build();
  }

  /**
   * Generates disambiguation information for moves involving pieces of the same type.
   *
   * @return The disambiguation file character.
   */
  String disambiguationFile() {
    for (final Move move: this.board.currentPlayer().getLegalMoves()) {
      if (move.getDestinationCoordinate() == this.destinationCoordinate && !this.equals(move) &&
        this.movedPiece.getPieceType().equals(move.getMovedPiece().getPieceType())) {
        return BoardUtils.getPositionAtCoordinate(this.movedPiece.getPiecePosition()).substring(0, 1);
      }
    }
    return "";
  }

  /**
   * Represents the possible outcomes of a chess move, including successful execution, an illegal move, or leaving the player's
   * king in check. Each status indicates whether a move is considered done or has specific implications on the game state.
   */
  public enum MoveStatus {
    DONE {
    
      @Override
      public boolean isDone() {
        return true;
      }
    },

    ILLEGAL_MOVE {
    
      @Override
      public boolean isDone() {
        return false;
      }
    },

    LEAVES_PLAYER_IN_CHECK {

      @Override
      public boolean isDone() {
        return false;
      }
    };

    /**
     * Checks if the move status indicates that the move is done.
     *
     * @return True if the move is done, false otherwise.
     */
    public abstract boolean isDone();
  }

  /**
   * The `PawnPromotion` class represents a special move in chess. When a pawn reaches the opponent's
   * back rank, it can be promoted to any other chess piece (queen, rook, bishop, or knight). This class extends a
   * standard `PawnMove` by adding promotion information and handles the execution of the promotion move.
   * <br>
   * Note that promotion is hard coded for a Queen. No under-promotions are allowed.
   *
   * @author Aaron Ho
   */
  public static class PawnPromotion extends PawnMove {
  
    /** The decorated move that represents the original pawn move. */
    final Move decoratedMove;
  
    /** The promoted pawn that is reaching the back rank. */
    final Pawn promotedPawn;
  
    /** The piece to which the pawn is promoted (queen, rook, bishop, or knight). */
    final Piece promotionPiece;
  
    /**
     * Constructs a `PawnPromotion` instance.
     *
     * @param decoratedMove    The decorated move representing the original pawn move.
     * @param promotionPiece   The piece to which the pawn is promoted (queen, rook, bishop, or knight).
     */
    public PawnPromotion(final Move decoratedMove, final Piece promotionPiece) {
      super(decoratedMove.getBoard(), decoratedMove.getMovedPiece(), decoratedMove.getDestinationCoordinate());
      this.decoratedMove = decoratedMove;
      this.promotedPawn = (Pawn) decoratedMove.getMovedPiece();
      this.promotionPiece = promotionPiece;
  
    }
  
    /**
     * Checks if two `PawnPromotion` instances are equal by comparing their attributes.
     *
     * @param o The object to compare with this `PawnPromotion`.
     * @return  `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      PawnPromotion that = (PawnPromotion) o;
      return Objects.equals(decoratedMove, that.decoratedMove) &&
             Objects.equals(promotedPawn, that.promotedPawn) &&
             Objects.equals(promotionPiece, that.promotionPiece);
    }
  
    /**
     * Generates a hash code for this `PawnPromotion` instance based on its attributes.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), decoratedMove, promotedPawn, promotionPiece);
    }
  
    /**
     * Executes the pawn promotion move by first executing the decorated move and then replacing the promoted pawn with
     * the promoted piece.
     *
     * @return The resulting board after executing the pawn promotion move.
     */
    @Override
    public Board execute() {
      final Board pawnMovedBoard = this.decoratedMove.execute();
      final Board.Builder builder = new Builder();
      pawnMovedBoard.currentPlayer().getActivePieces().stream().filter(piece -> !this.promotedPawn.equals(piece)).forEach(builder::setPiece);
      pawnMovedBoard.currentPlayer().getOpponent().getActivePieces().forEach(builder::setPiece);
      builder.setPiece(this.promotionPiece.movePiece(this));
      builder.setMoveMaker(pawnMovedBoard.currentPlayer().getAlliance());
      builder.setMoveTransition(this);
      return builder.build();
    }
  
    /**
     * Checks if the pawn promotion move is an attack on an opponent's piece.
     *
     * @return `true` if it's an attack, `false` otherwise.
     */
    @Override
    public boolean isAttack() {
      return this.decoratedMove.isAttack();
    }
  
    /**
     * Gets the piece attacked by this pawn promotion move.
     *
     * @return The attacked piece, or `null` if it's not an attack move.
     */
    @Override
    public Piece getAttackedPiece() {
      return this.decoratedMove.getAttackedPiece();
    }
  
    /**
     * Generates a string representation of the pawn promotion move.
     *
     * @return The string representing the move, e.g., "e8=Q" for a pawn promoting to a queen.
     */
    @Override
    public String toString() {
      return BoardUtils.getPositionAtCoordinate(this.movedPiece.getPiecePosition()) + "-" +
        BoardUtils.getPositionAtCoordinate(this.destinationCoordinate) + "=" + this.promotionPiece.getPieceType();
    }
  }


  /**
   * The `MajorMove` class represents a standard major move in chess. This move is typically associated with major
   * pieces (queen, rook, knight, or bishop) and involves the piece moving to a new destination coordinated on the board.
   *
   * @author Aaron Ho
   */
  public static class MajorMove extends Move {
  
    /**
     * Constructs a `MajorMove` instance with the provided board, piece, and destination coordinate.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The major piece (queen, rook, or bishop) that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     */
    public MajorMove(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
      super(board, pieceMoved, destinationCoordinate);  
    }
  
    /**
     * Checks if two `MajorMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `MajorMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof MajorMove && super.equals(other);
    }
  
    /**
     * Generates a string representation of the major move.
     *
     * @return The string representing the move, e.g., "Qd4" for a queen move to d4.
     */
    @Override
    public String toString() {
      return movedPiece.getPieceType().toString() + disambiguationFile() +
        BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
    }
  }
  
  /**
   * The `MajorAttackMove` class represents a major attack move in chess. This type of move involves a major piece
   * (queen, rook, knight, or bishop) moving to a new destination to coordinate and capturing an opponent's piece.
   *
   * @author Aaron Ho
   */
  public static class MajorAttackMove extends AttackMove {
  
    /**
     * Constructs a `MajorAttackMove` instance with the provided board, moving piece, destination coordinate, and the
     * attacked piece.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The major piece (queen, rook, or bishop) that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param pieceAttacked         The opponent's piece that is being attacked and captured.
     */
    public MajorAttackMove(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
      super(board, pieceMoved, destinationCoordinate, pieceAttacked);
    }
  
    /**
     * Checks if two `MajorAttackMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `MajorAttackMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof MajorAttackMove && super.equals(other);
    }
  
    /**
     * Generates a string representation of the major attack move.
     *
     * @return The string representing the move, e.g., "Qxd4" for a queen capturing a piece on d4.
     */
    @Override
    public String toString() {
      return movedPiece.getPieceType() + disambiguationFile() + "x" +
        BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
    }
  }
  
    
  /**
   * The `PawnMove` class represents a basic pawn move in chess. This move involves a pawn advancing to a new destination
   * coordinate without capturing any opponent's piece.
   *
   * @author Aaron Ho
   */
  public static class PawnMove extends Move {
  
    /**
     * Constructs a `PawnMove` instance with the provided board, moving piece, and destination coordinate.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The pawn piece that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     */
    public PawnMove(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
      super(board, pieceMoved, destinationCoordinate);
    }
  
    /**
     * Checks if two `PawnMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `PawnMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof PawnMove && super.equals(other);
    }
  
    /**
     * Generates a string representation of the pawn move.
     *
     * @return The string representing the move, e.g., "e4" for a pawn moving to e4.
     */
    @Override
    public String toString() {
      return BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
    }
  }
  
  /**
   * The `PawnAttackMove` class represents a pawn attack move in chess. This type of move involves a pawn moving to a new
   *  destination to coordinate and capturing an opponent's piece.
   *
   * @author Aaron Ho
   */
  public static class PawnAttackMove extends AttackMove {
  
    /**
     * Constructs a `PawnAttackMove` instance with the provided board, moving piece, destination coordinate, and the
     * attacked piece.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The pawn piece that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param pieceAttacked         The opponent's piece that is being attacked and captured.
     */
    public PawnAttackMove(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
      super(board, pieceMoved, destinationCoordinate, pieceAttacked);
    }
  
    /**
     * Checks if two `PawnAttackMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `PawnAttackMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof PawnAttackMove && super.equals(other);
    }
  
    /**
     * Generates a string representation of the pawn attack move.
     *
     * @return The string representing the move, e.g., "exd4" for a pawn capturing a piece on d4.
     */
    @Override
    public String toString() {
      return BoardUtils.getPositionAtCoordinate(this.movedPiece.getPiecePosition()).charAt(0) + "x" +
        BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
    }
  }
  
  /**
   * The `PawnEnPassantAttack` class represents a special pawn attack move called "en passant" in chess.
   * En passant is a situation where a pawn captures an opponent's pawn that has moved two squares forward from its starting position.
   *
   * @author Aaron Ho
   */
  public static class PawnEnPassantAttack extends PawnAttackMove {
  
    /**
     * Constructs a `PawnEnPassantAttack` instance with the provided board,
     * moving a piece, destination coordinate, and a piece attacked.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The pawn piece that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param pieceAttacked         The opponent's pawn piece that is attacked en passant.
     */
    public PawnEnPassantAttack(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
      super(board, pieceMoved, destinationCoordinate, pieceAttacked);
    }
  
    /**
     * Checks if two `PawnEnPassantAttack` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `PawnEnPassantAttack`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof PawnEnPassantAttack && super.equals(other);
    }
  
    /**
     * Executes the "en passant" pawn attack move, updating the board accordingly.
     * This method creates a new board with pieces moved and removed due to the en passant capture.
     *
     * @return The resulting board after the en passant capture.
     */
    @Override
    public Board execute() {
      final Board.Builder builder = new Builder();
      this.board.currentPlayer().getActivePieces().stream().filter(piece -> !this.movedPiece.equals(piece)).forEach(builder::setPiece);
      this.board.currentPlayer().getOpponent().getActivePieces().stream().filter(piece -> !piece.equals(this.getAttackedPiece())).forEach(builder::setPiece);
      builder.setPiece(this.movedPiece.movePiece(this));
      builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());
      builder.setMoveTransition(this);
      return builder.build();
    }
  
    /**
     * Undoes the "en passant" pawn attack move, reverting the board to the state before the capture.
     *
     * @return The board state before the en passant capture.
     */
    @Override
    public Board undo() {
      final Board.Builder builder = new Builder();
      this.board.getAllPieces().forEach(builder::setPiece);
      builder.setEnPassantPawn((Pawn) this.getAttackedPiece());
      builder.setMoveMaker(this.board.currentPlayer().getAlliance());
      return builder.build();
    }
  }
  
  /**
   * The `PawnJump` class represents a pawn move where a pawn advances two squares from its starting position.
   *
   * @author Aaron Ho
   */
  public static class PawnJump extends Move {
  
    /**
     * Constructs a `PawnJump` instance with the provided board, moving pawn, and destination coordinate.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The pawn piece that is moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     */
    public PawnJump(final Board board, final Pawn pieceMoved, final int destinationCoordinate) {
      super(board, pieceMoved, destinationCoordinate);
    }
  
    /**
     * Checks if two `PawnJump` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `PawnJump`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      return this == other || other instanceof PawnJump && super.equals(other);
    }
  
    /**
     * Executes the pawn jump move, updating the board accordingly. This method creates a new board with the pawn moved to the destination coordinate,
     * an en passant pawn set, and the move transition recorded.
     *
     * @return The resulting board after the pawn jump move.
     */
    @Override
    public Board execute() {
      final Board.Builder builder = new Builder();
      this.board.currentPlayer().getActivePieces().stream().filter(piece -> !this.movedPiece.equals(piece)).forEach(builder::setPiece);
      this.board.currentPlayer().getOpponent().getActivePieces().forEach(builder::setPiece);
      final Pawn movedPawn = (Pawn) this.movedPiece.movePiece(this);
      builder.setPiece(movedPawn);
      builder.setEnPassantPawn(movedPawn);
      builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());
      builder.setMoveTransition(this);
      return builder.build();
    }
  
    /**
     * Returns a string representation of the `PawnJump` move, showing only the destination coordinate.
     *
     * @return A string representing the `PawnJump` move.
     */
    @Override
    public String toString() {
      return BoardUtils.getPositionAtCoordinate(this.destinationCoordinate);
    }
  }
  
  
  /**
   * The `CastleMove` class represents a move that involves castling, which is a special king and rook move.
   *
   * @author dareTo81
   * @author Aaron Ho
   */
  static abstract class CastleMove extends Move {
  
    /*** The rook involved in the castling move. */
    final Rook castleRook;
  
    /*** The starting position of the castle rook. */
    final int castleRookStart;
  
    /*** The destination position of the castle rook. */
    final int castleRookDestination;
  
    /**
     * Constructs a `CastleMove` instance with the provided board, moving piece, destination coordinate, castle rook, and rook start and destination positions.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The piece being moved (usually the king).
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param castleRook            The rook involved in the castling move.
     * @param castleRookStart       The starting position of the castle rook.
     * @param castleRookDestination  The destination position of the castle rook.
     */
    CastleMove(final Board board,
      final Piece pieceMoved,
      final int destinationCoordinate,
      final Rook castleRook,
      final int castleRookStart,
      final int castleRookDestination) {
      super(board, pieceMoved, destinationCoordinate);
      this.castleRook = castleRook;
      this.castleRookStart = castleRookStart;
      this.castleRookDestination = castleRookDestination;
    }
  
    /**
     * Gets the rook involved in the castling move.
     *
     * @return The castle rook.
     */
    Rook getCastleRook() {
      return this.castleRook;
    }
  
    /**
     * Checks if this move is a castling move.
     *
     * @return `true` since it is a castling move.
     */
    @Override
    public boolean isCastlingMove() {
      return true;
    }
  
    /**
     * Executes the castling move on the chess board. This method creates a new board with the pieces moved as part of the castling move,
     * records the move transition, and updates the board state accordingly.
     *
     * @return The resulting board after the castling move.
     */
    @Override
    public Board execute() {
      final Board.Builder builder = new Builder();
      for (final Piece piece: this.board.getAllPieces()) {
        if (!this.movedPiece.equals(piece) && !this.castleRook.equals(piece)) {
          builder.setPiece(piece);
        }
      }
      builder.setPiece(this.movedPiece.movePiece(this));
      builder.setPiece(new Rook(this.castleRook.getPieceAllegiance(), this.castleRookDestination, false, 1));
      builder.setMoveMaker(this.board.currentPlayer().getOpponent().getAlliance());
      builder.setMoveTransition(this);
      return builder.build();
    }
  
    /**
     * Generates a hash code for this castling move, considering its attributes.
     *
     * @return The hash code for the castling move.
     */
    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), castleRook);
    }
  
    /**
     * Checks if two `CastleMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `CastleMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      if (this == other) return true;
      if (!(other instanceof CastleMove otherCastleMove)) return false;
      return super.equals(otherCastleMove) && this.castleRook.equals(otherCastleMove.getCastleRook());
    }
  }
  
  
  /**
   * The `KingSideCastleMove` class represents a move for king-side castling in chess.
   *
   * @author Aaron Ho
   */
  public static class KingSideCastleMove extends CastleMove {
  
    /**
     * Constructs a `KingSideCastleMove` instance with the provided board, moving piece, destination coordinate,
     * castle rook, and rook start and destination positions.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The piece being moved (usually the king).
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param castleRook            The rook involved in the castling move.
     * @param castleRookStart       The starting position of the castle rook.
     * @param castleRookDestination  The destination position of the castle rook.
     */
    public KingSideCastleMove(final Board board,
      final Piece pieceMoved,
      final int destinationCoordinate,
      final Rook castleRook,
      final int castleRookStart,
      final int castleRookDestination) {
      super(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
    }
  
    /**
     * Checks if two `KingSideCastleMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `KingSideCastleMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      if (this == other) return true;
      if (!(other instanceof KingSideCastleMove otherKingSideCastleMove)) return false;
      return super.equals(otherKingSideCastleMove) && this.castleRook.equals(otherKingSideCastleMove.getCastleRook());
    }
  
    /**
     * Returns a string representation of the `KingSideCastleMove`, indicating king-side castling.
     *
     * @return The string "O-O" representing king-side castling.
     */
    @Override
    public String toString() {
      return "O-O";
    }
  }
  
    
  /**
   * The `QueenSideCastleMove` class represents a move for queen-side castling in chess.
   *
   * @author Aaron Ho
   */
  public static class QueenSideCastleMove extends CastleMove {
  
    /**
     * Constructs a `QueenSideCastleMove` instance with the provided board, moving piece, destination coordinate,
     * castle rook, and rook start and destination positions.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The piece being moved (usually the king).
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param castleRook            The rook involved in the castling move.
     * @param castleRookStart       The starting position of the castle rook.
     * @param rookCastleDestination  The destination position of the castle rook.
     */
    public QueenSideCastleMove(final Board board,
      final Piece pieceMoved,
      final int destinationCoordinate,
      final Rook castleRook,
      final int castleRookStart,
      final int rookCastleDestination) {
      super(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, rookCastleDestination);
    }
  
    /**
     * Checks if two `QueenSideCastleMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `QueenSideCastleMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      if (this == other) return true;
      if (!(other instanceof QueenSideCastleMove otherQueenSideCastleMove)) return false;
      return super.equals(otherQueenSideCastleMove) && this.castleRook.equals(otherQueenSideCastleMove.getCastleRook());
    }
  
    /**
     * Returns a string representation of the `QueenSideCastleMove`, indicating queen-side castling.
     *
     * @return The string "O-O-O" representing queen-side castling.
     */
    @Override
    public String toString() {
      return "O-O-O";
    }
  }
  
  /**
   * The `AttackMove` class represents a move that results in attacking an opponent's piece in chess.
   *
   * @author dareTo81
   * @author Aaron Ho
   */
  static abstract class AttackMove extends Move {
  
    /*** The piece that is attacked as a result of this move. */
    private final Piece attackedPiece;
  
    /**
     * Constructs an `AttackMove` instance with the provided board, moving piece, destination coordinate,
     * and the piece being attacked.
     *
     * @param board                 The chess board on which the move is made.
     * @param pieceMoved            The piece being moved.
     * @param destinationCoordinate The coordinate to which the piece is moved.
     * @param pieceAttacked         The piece that is attacked in this move.
     */
    AttackMove(final Board board, final Piece pieceMoved,
      final int destinationCoordinate, final Piece pieceAttacked) {
      super(board, pieceMoved, destinationCoordinate);
      this.attackedPiece = pieceAttacked;
    }
  
    /**
     * Calculates a hash code for the `AttackMove` by combining the hash codes of the moving piece and the attacked piece.
     *
     * @return The hash code for this `AttackMove`.
     */
    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), attackedPiece);
    }
  
    /**
     * Checks if two `AttackMove` instances are equal by comparing their attributes.
     *
     * @param other The object to compare with this `AttackMove`.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    @Override
    public boolean equals(final Object other) {
      if (this == other) return true;
      if (!(other instanceof AttackMove otherAttackMove)) return false;
      return super.equals(otherAttackMove) && getAttackedPiece().equals(otherAttackMove.getAttackedPiece());
    }
  
    /**
     * Gets the piece attacked as a result of this move.
     *
     * @return The attacked piece.
     */
    @Override
    public Piece getAttackedPiece() {
      return this.attackedPiece;
    }
  
    /**
     * Indicates whether this move is an attack on an opponent's piece.
     *
     * @return `true` for an attack move, `false` otherwise.
     */
    @Override
    public boolean isAttack() {
      return true;
    }
  }
  
   /**
   * The `NullMove` class represents a placeholder for no move in a chess game.
   *
   * @author Aaron Ho
   * @author dareTo81
   */
  public static class NullMove extends Move {
  
    /**
     * Constructs a `NullMove` instance. A null move has no board or destination coordinate.
     */
    public NullMove() {
      super(null, -1);
    }
  
    /**
     * Gets the current coordinate, which is set to -1 for a null move.
     *
     * @return The current coordinate, which is -1.
     */
    @Override
    public int getCurrentCoordinate() {
      return -1;
    }
  
    /**
     * Gets the destination coordinate, which is also set to -1 for a null move.
     *
     * @return The destination coordinate, which is -1.
     */
    @Override
    public int getDestinationCoordinate() {
      return -1;
    }
  
    /**
     * Throws a runtime exception because a null move cannot be executed.
     *
     * @return A runtime exception with the message "cannot execute null move!"
     */
    @Override
    public Board execute() {
      throw new RuntimeException("Cannot execute null move!");
    }
  
    /**
     * Gets a string representation of the null move.
     *
     * @return The string "Null Move."
     */
    @Override
    public String toString() {
      return "Null Move";
    }
  }

  /*** Represents a factory for creating moves. */
  public static class MoveFactory {

    /*** Private constructor to prevent instantiation. */
    private MoveFactory() {
      throw new RuntimeException("Not instantiatable!");
    }

    /**
     * Creates and returns a null move.
     *
     * @return The created null move.
     */
    public static Move getNullMove() {
      return MoveUtils.NULL_MOVE;
    }

    /**
     * Creates a move with the specified current and destination coordinates on the given board.
     *
     * @param board The current state of the chess board.
     * @param currentCoordinate The current coordinate of the moved piece.
     * @param destinationCoordinate The destination coordinate of the move.
     * @return The created move.
     */
    public static Move createMove(final Board board,
      final int currentCoordinate,
      final int destinationCoordinate) {
      for (final Move move: board.getAllLegalMoves()) {
        if (move.getCurrentCoordinate() == currentCoordinate &&
          move.getDestinationCoordinate() == destinationCoordinate) {
          return move;
        }
      }
      return MoveUtils.NULL_MOVE;
    }
  }
}