package engine.forPlayer;

import engine.Alliance;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MovePool;
import engine.forPiece.Piece;
import engine.forPiece.Rook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static engine.forPiece.Piece.PieceType.ROOK;

/**
 * The WhitePlayer class represents a player controlling the white pieces in a chess game.
 * It extends the abstract Player class and provides functionality specific to white pieces,
 * including move generation, castling validation, and piece management. This class is responsible
 * for generating legal moves for the white player and handling special moves such as castling.
 * <p>
 * The white player follows standard chess rules where pieces move from the bottom of the board
 * toward the top, with pawns advancing toward the eighth rank for promotion.
 *
 * @author Aaron
 * @author dareTo81
 */
public final class WhitePlayer extends Player {

  /**
   * Constructs a WhitePlayer object with the given chessboard and collections of legal moves
   * for both white and black players. The player is initialized with access to the current
   * board state and legal move sets for position evaluation.
   *
   * @param board The current chess board state.
   * @param whiteStandardLegals A collection of legal moves for the white player.
   * @param blackStandardLegals A collection of legal moves for the black player.
   */
  public WhitePlayer(final Board board,
                     final Collection<Move> whiteStandardLegals,
                     final Collection<Move> blackStandardLegals) {
    super(board, whiteStandardLegals, blackStandardLegals);
  }

  /**
   * Calculates and returns the possible king-side and queen-side castling moves for the white player.
   * A castling move is legal only when specific conditions are met including unmoved king and rook,
   * clear path between pieces, no check situations, and absence of king pawn traps. The method
   * validates both king-side and queen-side castling opportunities according to chess rules.
   * <p>
   * King-side castling requires the king to move from e1 to g1 and the rook from h1 to f1.
   * Queen-side castling requires the king to move from e1 to c1 and the rook from a1 to d1.
   *
   * @param playerLegals A collection of legal moves for the white player.
   * @param opponentLegals A collection of legal moves for the black player.
   * @return A collection of possible castling moves for the white player, which may be empty.
   */
  @Override
  protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
                                                  final Collection<Move> opponentLegals) {
    if(!hasCastleOpportunities()) {
      return Collections.emptyList();
    } final List<Move> kingCastles = new ArrayList<>();
    if(this.playerKing.isFirstMove() && this.playerKing.getPiecePosition() == 60 && !this.isInCheck()) {
      if(this.board.getPiece(61) == null && this.board.getPiece(62) == null) {
        final Piece kingSideRook = this.board.getPiece(63);
        if(kingSideRook != null && kingSideRook.isFirstMove()) {
          if(Player.calculateAttacksOnTile(61, opponentLegals).isEmpty() &&
                  Player.calculateAttacksOnTile(62, opponentLegals).isEmpty() &&
                  kingSideRook.getPieceType() == ROOK) {
            if(BoardUtils.isKingPawnTrap(this.board, this.playerKing, 52)) {
              kingCastles.add(MovePool.INSTANCE.getKingSideCastleMove(this.board, this.playerKing, 62, (Rook) kingSideRook, kingSideRook.getPiecePosition(), 61));
            }
          }
        }
      } if(this.board.getPiece(59) == null && this.board.getPiece(58) == null &&
              this.board.getPiece(57) == null) {
        final Piece queenSideRook = this.board.getPiece(56);
        if(queenSideRook != null && queenSideRook.isFirstMove()) {
          if(Player.calculateAttacksOnTile(58, opponentLegals).isEmpty() &&
                  Player.calculateAttacksOnTile(59, opponentLegals).isEmpty() && queenSideRook.getPieceType() == ROOK) {
            if(BoardUtils.isKingPawnTrap(this.board, this.playerKing, 52)) {
              kingCastles.add(MovePool.INSTANCE.getQueenSideCastleMove(this.board, this.playerKing, 58, (Rook) queenSideRook, queenSideRook.getPiecePosition(), 59));
            }
          }
        }
      }
    } return Collections.unmodifiableList(kingCastles);
  }

  /**
   * Gets the opponent of the white player, which is the black player in the chess game.
   * This method provides access to the opposing player for game state evaluation and
   * move calculation purposes.
   *
   * @return The BlackPlayer object representing the opponent.
   */
  @Override
  public BlackPlayer getOpponent() {
    return this.board.blackPlayer();
  }

  /**
   * Gets a collection of active white pieces on the current chess board.
   * Active pieces are those that remain on the board and have not been captured.
   * This collection is used for move generation and board evaluation.
   *
   * @return A collection of Piece objects representing the active white pieces.
   */
  @Override
  public Collection<Piece> getActivePieces() {
    return this.board.getWhitePieces();
  }

  /**
   * Gets the alliance of the white player, which is always Alliance.WHITE.
   * This method identifies the player's color for game logic and rule enforcement.
   *
   * @return The Alliance enum value representing the white player's alliance.
   */
  @Override
  public Alliance getAlliance() {
    return Alliance.WHITE;
  }

  /**
   * Returns a string representation of the white player's alliance.
   * The representation matches the alliance's string format for display purposes.
   *
   * @return The string "WHITE" to represent the white player.
   */
  @Override
  public String toString() {
    return Alliance.WHITE.toString();
  }
}