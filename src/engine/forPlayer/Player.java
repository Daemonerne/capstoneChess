package engine.forPlayer;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.Move;
import engine.forBoard.MoveTransition;
import engine.forPiece.King;
import engine.forPiece.Piece;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static engine.forPiece.Piece.PieceType.KING;
import static java.util.stream.Collectors.collectingAndThen;

/**
 * The Player class represents an abstract chess player, providing the foundation for player-specific
 * operations and game state management. Each player is associated with a board position and maintains
 * their king piece, legal moves, and check status. The class handles move execution, validation,
 * and state queries including checkmate, stalemate, and castling status. Concrete subclasses must
 * implement alliance-specific behavior for piece management and castling calculations.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public abstract class Player {

  /** The chessboard associated with this player. */
  protected final Board board;

  /** The king piece belonging to this player. */
  protected final King playerKing;

  /** The collection of legal moves available to this player on the current board state. */
  protected final Collection<Move> legalMoves;

  /** Flag indicating whether this player is currently in check. */
  protected final boolean isInCheck;

  /**
   * Constructs a Player with the specified board and move collections.
   * Establishes the player's king, determines check status, and calculates all legal moves
   * including castling options.
   *
   * @param board The chessboard associated with this player.
   * @param playerLegals The collection of standard legal moves for this player.
   * @param opponentLegals The collection of legal moves for the opponent player.
   */
  Player(final Board board, final Collection<Move> playerLegals, final Collection<Move> opponentLegals) {
    this.board = board;
    this.playerKing = establishKing();
    this.isInCheck = !calculateAttacksOnTile(this.playerKing.getPiecePosition(), opponentLegals).isEmpty();
    playerLegals.addAll(calculateKingCastles(playerLegals, opponentLegals));
    this.legalMoves = Collections.unmodifiableCollection(playerLegals);
  }

  /**
   * Determines whether this player is currently in check.
   *
   * @return True if the player is in check, false otherwise.
   */
  public boolean isInCheck() {
    return this.isInCheck;
  }

  /**
   * Determines whether this player is in checkmate.
   * A player is in checkmate if they are in check and have no legal escape moves.
   *
   * @return True if the player is in checkmate, false otherwise.
   */
  public boolean isInCheckMate() {
    return this.isInCheck && !hasEscapeMoves();
  }

  /**
   * Determines whether this player is in stalemate.
   * A player is in stalemate if they are not in check but have no legal moves.
   *
   * @return True if the player is in stalemate, false otherwise.
   */
  public boolean isInStaleMate() {
    return !this.isInCheck && !hasEscapeMoves();
  }

  /**
   * Determines whether this player has castled.
   *
   * @return True if the player has castled, false otherwise.
   */
  public boolean isCastled() {
    return this.playerKing.isCastled();
  }

  /**
   * Retrieves this player's king piece.
   *
   * @return The king piece belonging to this player.
   */
  public King getPlayerKing() {
    return this.playerKing;
  }

  /**
   * Establishes the king piece for this player by locating it among active pieces.
   *
   * @return The king piece belonging to this player.
   * @throws RuntimeException If no king piece is found among active pieces.
   */
  private King establishKing() {
    return (King) getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == KING)
            .findAny()
            .orElseThrow(RuntimeException::new);
  }

  /**
   * Determines whether this player has any legal escape moves available.
   *
   * @return True if escape moves exist, false otherwise.
   */
  private boolean hasEscapeMoves() {
    return this.legalMoves.stream()
            .anyMatch(move -> makeMove(move)
                    .moveStatus().isDone());
  }

  /**
   * Retrieves the collection of legal moves for this player.
   *
   * @return An unmodifiable collection of legal moves.
   */
  public Collection<Move> getLegalMoves() {
    return this.legalMoves;
  }

  /**
   * Calculates all moves that attack a specific tile on the board.
   *
   * @param tile The tile coordinate to check for attacks.
   * @param moves The collection of moves to examine.
   * @return An unmodifiable collection of moves that attack the specified tile.
   */
  public static Collection<Move> calculateAttacksOnTile(final int tile, final Collection<Move> moves) {
    return moves.stream()
            .filter(move -> move.getDestinationCoordinate() == tile)
            .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  /**
   * Executes a move and returns the resulting board transition.
   * Validates the move legality and ensures the move does not leave the player in check.
   *
   * @param move The move to execute.
   * @return A MoveTransition object containing the resulting board state and move status.
   */
  public MoveTransition makeMove(final Move move) {
    if (!this.legalMoves.contains(move)) {
      return new MoveTransition(this.board, MoveStatus.ILLEGAL_MOVE);
    }
    final Board transitionedBoard = move.execute();
    return transitionedBoard.currentPlayer().getOpponent().isInCheck() ?
            new MoveTransition(this.board, MoveStatus.LEAVES_PLAYER_IN_CHECK) :
            new MoveTransition(transitionedBoard, MoveStatus.DONE);
  }

  /**
   * Undoes a move and returns the previous board state.
   *
   * @param move The move to undo.
   * @return A MoveTransition object containing the previous board state.
   */
  public MoveTransition unMakeMove(final Move move) {
    return new MoveTransition(move.undo(), MoveStatus.DONE);
  }

  /**
   * Retrieves the collection of active pieces belonging to this player.
   * Must be implemented by concrete subclasses to return alliance-specific pieces.
   *
   * @return The collection of active pieces for this player.
   */
  public abstract Collection<Piece> getActivePieces();

  /**
   * Retrieves the alliance (color) of this player.
   * Must be implemented by concrete subclasses to return the appropriate alliance.
   *
   * @return The alliance of this player.
   */
  public abstract Alliance getAlliance();

  /**
   * Retrieves the opponent player.
   * Must be implemented by concrete subclasses to return the opposing player.
   *
   * @return The opponent player.
   */
  public abstract Player getOpponent();

  /**
   * Calculates and returns possible castling moves for this player's king.
   * Must be implemented by concrete subclasses to handle alliance-specific castling rules.
   *
   * @param playerLegals The legal moves available to this player.
   * @param opponentLegals The legal moves available to the opponent.
   * @return A collection of possible castling moves.
   */
  protected abstract Collection<Move> calculateKingCastles(Collection<Move> playerLegals, Collection<Move> opponentLegals);

  /**
   * Determines whether this player has any castling opportunities available.
   * A player has castling opportunities if they are not in check, have not already castled,
   * and retain either kingside or queenside castling capabilities.
   *
   * @return True if castling opportunities exist, false otherwise.
   */
  protected boolean hasCastleOpportunities() {
    return !this.isInCheck || !this.playerKing.isCastled() ||
            (this.playerKing.isKingSideCastleCapable() && this.playerKing.isQueenSideCastleCapable());
  }
}