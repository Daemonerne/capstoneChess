package engine.forBoard;

import engine.Alliance;
import engine.forPiece.*;
import engine.forPlayer.BlackPlayer;
import engine.forPlayer.Player;
import engine.forPlayer.WhitePlayer;

import java.util.*;

import static engine.forBoard.Move.MoveFactory.getNullMove;

/** 
 * The Board class represents a chess board containing 64 tiles and thirty-two pieces at the start of the game. 
 * It provides constructors and methods for creating and maintaining the chess board, as well as managing the pieces 
 * and players associated with the board. The class also includes methods for storing and calculating legal moves. 
 * <br><br> 
 * The game board comprises a list of tiles and contains collections of white and black pieces. 
 * It includes two players, whitePlayer and blackPlayer, as well as the current player. 
 * The class provides methods for accessing the players, pieces, and tiles, and for calculating legal moves. 
 * 
 * @author Aaron Ho 
 * @author dareTo81
 */
public final class Board {
  
  /*** A map representing the configuration of pieces on the board. The keys are tile coordinates,
   * and the values are the corresponding pieces placed on those tiles. */
  private final Map<Integer, Piece> boardConfig;
  
  /*** A collection of white pieces currently present on the board. */
  private final Collection<Piece> whitePieces;
  
  /*** A collection of black pieces currently present on the board. */
  private final Collection<Piece> blackPieces;
  
  /*** The player controlling the white pieces on the board. */
  private final WhitePlayer whitePlayer;
  
  /*** The player controlling the black pieces on the board. */
  private final BlackPlayer blackPlayer;
  
  /*** The player whose turn it currently is to make a move. */
  private final Player currentPlayer;
  
  /*** The pawn that is susceptible to en passant capture in the current position, if any. */
  private final Pawn enPassantPawn;
  
  /*** The move that resulted in the current board position. If no move transition has occurred,
   * this value will be the null move. */
  private final Move transitionMove;

  /*** A pre-constructed standard chess board configuration representing the starting position.
   * This is a constant instance shared across all instances of the Board class. */
  private static final Board STANDARD_BOARD = createStandardBoardImpl();
  
  /**
   * Constructs a board from a builder object.
   *
   * @param builder The builder object containing board configuration details.
   */
  private Board(final Builder builder) {
    this.boardConfig = Collections.unmodifiableMap(builder.BoardConfigurations);
    this.whitePieces = calculateActivePieces(builder, Alliance.WHITE);
    this.blackPieces = calculateActivePieces(builder, Alliance.BLACK);
    this.enPassantPawn = builder.enPassantPawn;
    final Collection<Move> whiteStandardMoves = calculateLegalMoves(this.whitePieces);
    final Collection<Move> blackStandardMoves = calculateLegalMoves(this.blackPieces);
    this.whitePlayer = new WhitePlayer(this, whiteStandardMoves, blackStandardMoves);
    this.blackPlayer = new BlackPlayer(this, whiteStandardMoves, blackStandardMoves);
    this.currentPlayer = builder.nextMoveMaker.choosePlayerByAlliance(this.whitePlayer, this.blackPlayer);
    this.transitionMove = builder.transitionMove != null ? builder.transitionMove : getNullMove();
  }
  
  
  /**
   * Returns a string representation of the board, including piece configurations.
   *
   * @return A string representation of the board.
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
      builder.append(prettyPrint(this.boardConfig.get(i))).append(" ");
      if ((i + 1) % 8 == 0) {
        builder.append("\n");
      }
    } return builder.toString();
  }

  private static String prettyPrint(final Piece piece) {
    if(piece != null) {
      return piece.getPieceAllegiance().isBlack() ? 
        piece.toString().toLowerCase() : piece.toString();
    } return "-";
  }
  
  /**
   * Returns a collection of black pieces on the board.
   *
   * @return A collection of black pieces.
   */
  public Collection<Piece> getBlackPieces() {
    return this.blackPieces;
  }
  
  /**
   * Returns a collection of white pieces on the board.
   *
   * @return A collection of white pieces.
   */
  public Collection<Piece> getWhitePieces() {
    return this.whitePieces;
  }
  
  /**
   * Returns a collection of all pieces on the board, irrespective of their alliance.
   *
   * @return A collection of all pieces.
   */
  public Collection<Piece> getAllPieces() {
    final List<Piece> allPieces = new ArrayList<>();
    allPieces.addAll(this.whitePieces);
    allPieces.addAll(this.blackPieces);
    return allPieces;
  }
  
  /**
   * Returns a collection of all legal moves that can be made on the current board.
   *
   * @return A collection of legal moves.
   */
  public Collection<Move> getAllLegalMoves() {
    final List<Move> allLegalMoves = new ArrayList<>();
    allLegalMoves.addAll(this.whitePlayer.getLegalMoves());
    allLegalMoves.addAll(this.blackPlayer.getLegalMoves());
    return allLegalMoves;
  }
  
  /**
   * Retrieves the white player controlling the white pieces.
   *
   * @return The white player.
   */
  public WhitePlayer whitePlayer() {
    return this.whitePlayer;
  }
  
  /**
   * Retrieves the black player controlling the black pieces.
   *
   * @return The black player.
   */
  public BlackPlayer blackPlayer() {
    return this.blackPlayer;
  }
  
  /**
   * Retrieves the player whose turn it is to move.
   *
   * @return The current player.
   */
  public Player currentPlayer() {
    return this.currentPlayer;
  }
  
  /**
   * Retrieves the piece at the specified coordinate on the board.
   *
   * @param coordinate The coordinate to check.
   * @return The piece at the specified coordinate, or null if the tile is empty.
   */
  public Piece getPiece(final int coordinate) {
    return this.boardConfig.get(coordinate);
  }
  
  /**
   * Retrieves the pawn susceptible to en passant capture, if any.
   *
   * @return The en passant pawn, or null if no pawn is susceptible.
   */
  public Pawn getEnPassantPawn() {
    return this.enPassantPawn;
  }
  
  /**
   * Retrieves the move that led to the current board state.
   *
   * @return The transition move, or a null move if no transition occurred.
   */
  public Move getTransitionMove() {
    return this.transitionMove;
  }
  
  /**
   * Returns a standard chess board configuration representing the starting position.
   *
   * @return A standard chess board.
   */
  public static Board createStandardBoard() {
    return STANDARD_BOARD;
  }
  
  /**
   * Creates and returns a standard chess board configuration.
   *
   * @return A new Board instance with the standard starting positions of pieces.
   */
  private static Board createStandardBoardImpl() {
	
    final Builder builder = new Builder();

    builder.setPiece(new Rook(Alliance.BLACK, 0, 0));
    builder.setPiece(new Knight(Alliance.BLACK, 1, 0));
    builder.setPiece(new Bishop(Alliance.BLACK, 2, 0));
    builder.setPiece(new Queen(Alliance.BLACK, 3, 0));
    builder.setPiece(new King(Alliance.BLACK, 4, true, true));
    builder.setPiece(new Bishop(Alliance.BLACK, 5, 0));
    builder.setPiece(new Knight(Alliance.BLACK, 6, 0));
    builder.setPiece(new Rook(Alliance.BLACK, 7, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 8, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 9,0));
    builder.setPiece(new Pawn(Alliance.BLACK, 10, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 11, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 12, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 13, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 14, 0));
    builder.setPiece(new Pawn(Alliance.BLACK, 15, 0));
		
    builder.setPiece(new Pawn(Alliance.WHITE, 48, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 49, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 50, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 51, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 52, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 53, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 54, 0));
    builder.setPiece(new Pawn(Alliance.WHITE, 55, 0));
    builder.setPiece(new Rook(Alliance.WHITE, 56, 0));
    builder.setPiece(new Knight(Alliance.WHITE, 57, 0));
    builder.setPiece(new Bishop(Alliance.WHITE, 58, 0));
    builder.setPiece(new Queen(Alliance.WHITE, 59, 0));
    builder.setPiece(new King(Alliance.WHITE, 60, true, true));
    builder.setPiece(new Bishop(Alliance.WHITE, 61, 0));
    builder.setPiece(new Knight(Alliance.WHITE, 62, 0));
    builder.setPiece(new Rook(Alliance.WHITE, 63, 0));
    builder.setMoveMaker(Alliance.WHITE);

    return builder.build();
  }
  
  /**
   * Calculates the legal moves for a collection of pieces on the board.
   *
   * @param pieces The collection of pieces for which to calculate legal moves.
   * @return A collection of legal moves for the given pieces.
   */
  private Collection<Move> calculateLegalMoves(Collection<Piece> pieces) {
    List<Move> legalMoves = new ArrayList<>();
    for (Piece piece : pieces) {
      legalMoves.addAll(piece.calculateLegalMoves(this));
    } return legalMoves;
  }
 
  /**
   * Calculates and returns the active pieces of a specified alliance on the board.
   *
   * @param builder  The builder containing the board configurations.
   * @param alliance The alliance (color) of the pieces to be considered.
   * @return A collection of active pieces belonging to the specified alliance.
   */
  private Collection<Piece> calculateActivePieces(Builder builder, Alliance alliance) {
    List<Piece> activePieces = new ArrayList<>();
    for (Piece piece : builder.BoardConfigurations.values()) {
      if (piece.getPieceAllegiance() == alliance) {
        activePieces.add(piece);
      }
    } return activePieces;
  }
  
  
  /*** A builder class for constructing instances of the Board class with specific configurations. */
  public static class Builder {
    private final Map<Integer, Piece> BoardConfigurations;
    private Alliance nextMoveMaker;
    private Pawn enPassantPawn;
    private Move transitionMove;
    
    public Builder() {
      this.BoardConfigurations = new HashMap<>(32, 1.0f);
    }
    
    /**
     * Sets the piece configuration for a specific position on the board.
     *
     * @param piece The piece to be placed on the board at the specified position.
     * @return The current builder instance to continue configuring the board.
     */
    public Builder setPiece(final Piece piece) {
      this.BoardConfigurations.put(piece.getPiecePosition(), piece);
      return this;
    }
    
    /**
     * Sets the alliance (color) of the player who will make the next move.
     *
     * @param nextMoveMaker The alliance of the player who has the next move.
     * @return The current builder instance to continue configuring the board.
     */
    public Builder setMoveMaker(final Alliance nextMoveMaker) {
      this.nextMoveMaker = nextMoveMaker;
      return this;
    }
    
    /**
     * Sets the en passant pawn for the current move.
     *
     * @param enPassantPawn The pawn that can be captured en passant in the current move.
     * @return The current builder instance to continue configuring the board.
     */
    public Builder setEnPassantPawn(final Pawn enPassantPawn) {
      this.enPassantPawn = enPassantPawn;
      return this;
    }
    
    /**
     * Sets the move transition for the board configuration.
     *
     * @param transitionMove The move that represents the transition between the previous and current board states.
     */
    public void setMoveTransition(final Move transitionMove) {
      this.transitionMove = transitionMove;
    }

    /**
     * Constructs and returns a new instance of the Board class based on the configured parameters.
     *
     * @return A new Board instance with the specified configurations.
     */
    public Board build() {
      return new Board(this);
    }
  }
}

