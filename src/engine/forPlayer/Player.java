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


/***
 * The abstract class `Player` represents a player in a chess game, encapsulating essential functionality and state
 * for player-specific operations. A player is associated with a `Board`` instance, representing the chessboard, and possesses
 * a `King`, denoting the player's monarch. The collection of legal moves available to the player on the current board state is
 * maintained, and the player's check status is determined based on the presence of opponent attacks on the player's king.
 * Additionally, a threefold repetition mechanism is facilitated through a private hashmap tracking position repetitions. The class
 * offers methods to ascertain the player's check, checkmate, and stalemate statuses, as well as whether the player has executed
 * castling. The core functionality includes move handling, with methods to make and undo moves, along with checks for the existence
 * of legal escape moves. The player's alliance (color) is obtained through the `getAlliance()` method, and the opponent player is
 * retrieved via the `getOpponent()` method. Subclasses are required to implement methods for obtaining active pieces, calculating
 * king castling moves, and determining castle opportunities. This class serves as a foundational element for creating concrete player
 * implementations in a chess engine, providing a comprehensive framework for player-specific behaviors and interactions within the game.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public abstract class Player {

  /*** The chessboard associated with this player. */
  protected final Board board;

  /*** The king piece of this player. */
  protected final King playerKing;

  /*** The collection of legal moves that this player can make on the current board state. */
  protected final Collection<Move> legalMoves;

  /*** A flag indicating whether this player is currently in check. */
  protected final boolean isInCheck;

  /**
   * Constructs a new Player object with the given chessboard and legal moves.
   *
   * @param board           The chessboard associated with this player.
   * @param playerLegals    The collection of legal moves for this player.
   * @param opponentLegals  The collection of legal moves for the opponent player.
   */
  Player(final Board board, final Collection<Move> playerLegals, final Collection<Move> opponentLegals) {
    this.board = board;
    this.playerKing = establishKing();
    this.isInCheck = !calculateAttacksOnTile(this.playerKing.getPiecePosition(), opponentLegals).isEmpty();
    playerLegals.addAll(calculateKingCastles(playerLegals, opponentLegals));
    this.legalMoves = Collections.unmodifiableCollection(playerLegals);
  }

  /**
   * Checks if the player is currently in check.
   *
   * @return True if the player is in check, false otherwise.
   */
  public boolean isInCheck() {
    return this.isInCheck;
  }

  /**
   * Checks if the player is in checkmate.
   *
   * @return True if the player is in checkmate, false otherwise.
   */
  public boolean isInCheckMate() {
    return this.isInCheck && !hasEscapeMoves();
  }

  /**
   * Checks if the player is in stalemate.
   *
   * @return True if the player is in stalemate, false otherwise.
   */
  public boolean isInStaleMate() {
    return !this.isInCheck && !hasEscapeMoves());
  }

  /**
   * Checks if the player has castled.
   *
   * @return True if the player has castled, false otherwise.
   */
  public boolean isCastled() {
    return this.playerKing.isCastled();
  }

  /**
   * Gets the king piece of the player.
   *
   * @return The king piece.
   */
  public King getPlayerKing() {
    return this.playerKing;
  }

  /**
   * Establishes the king piece for the player by searching through active pieces.
   *
   * @return The king piece of the player.
   */
  private King establishKing() {
    return (King) getActivePieces().stream()
            .filter(piece -> piece.getPieceType() == KING)
            .findAny()
            .orElseThrow(RuntimeException::new);
  }

  /**
   * Checks if the player has any legal escape moves from the current position.
   *
   * @return True if there are escape moves, false otherwise.
   */
  private boolean hasEscapeMoves() {
    return this.legalMoves.stream()
            .anyMatch(move -> makeMove(move)
                    .moveStatus().isDone());
  }

  /**
   * Gets the collection of legal moves for the player.
   *
   * @return The collection of legal moves.
   */
  public Collection<Move> getLegalMoves() {
    return this.legalMoves;
  }

  /**
   * Calculates the attacks on a specific tile.
   *
   * @param tile   The tile to check for attacks.
   * @param moves  The collection of moves to consider.
   * @return A collection of moves that attack the specified tile.
   */
  public static Collection<Move> calculateAttacksOnTile(final int tile, final Collection<Move> moves) {
    return moves.stream()
            .filter(move -> move.getDestinationCoordinate() == tile)
            .collect(collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  /**
   * Makes a move on the chessboard and returns the resulting board state.
   *
   * @param move The move to make.
   * @return A MoveTransition object representing the result of the move.
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
   * Undoes a move on the chessboard and returns the resulting board state.
   *
   * @param move The move to undo.
   * @return A MoveTransition object representing the result of the move undo.
   */
  public MoveTransition unMakeMove(final Move move) {
    return new MoveTransition(move.undo(), MoveStatus.DONE);
  }

  /**
   * Gets the collection of active pieces for this player.
   *
   * @return The collection of active pieces.
   */
  public abstract Collection<Piece> getActivePieces();

  /**
   * Gets the alliance (color) of this player (e.g., WHITE or BLACK).
   *
   * @return The alliance of the player.
   */
  public abstract Alliance getAlliance();

  /**
   * Gets the opponent player.
   *
   * @return The opponent player.
   */
  public abstract Player getOpponent();

  /**
   * Calculates and returns the possible king castling moves for this player.
   *
   * @param playerLegals   The legal moves of this player.
   * @param opponentLegals The legal moves of the opponent.
   * @return A collection of possible king castling moves.
   */
  protected abstract Collection<Move> calculateKingCastles(Collection<Move> playerLegals, Collection<Move> opponentLegals);

  /**
   * Checks if the player has any castle opportunities (i.e., king-side or queen-side castling).
   *
   * @return True if there are castle opportunities, false otherwise.
   */
  protected boolean hasCastleOpportunities() {
    return !this.isInCheck || !this.playerKing.isCastled() ||
            (this.playerKing.isKingSideCastleCapable() && this.playerKing.isQueenSideCastleCapable());
  }
}
