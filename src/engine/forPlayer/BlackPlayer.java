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
 * The `BlackPlayer` class extends `Player` to represent a player controlling the black pieces in a chess game.
 * It extends the abstract `Player` class and provides functionality specific to black pieces.
 * This class is responsible for generating legal moves for the black player, including special moves
 * like castling and en passant captures.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public final class BlackPlayer extends Player {

  /**
   * Constructs a new `BlackPlayer` object with the specified board and collections of legal moves
   * for both white and black players.
   *
   * @param board The current chess board.
   * @param whiteStandardLegals A collection of legal moves for the white player.
   * @param blackStandardLegals A collection of legal moves for the black player.
   */
  public BlackPlayer(final Board board,
                     final Collection<Move> whiteStandardLegals,
                     final Collection<Move> blackStandardLegals) {
    super(board, blackStandardLegals, whiteStandardLegals);
  }

  /**
   * Calculates the legal king-side and queen-side castling moves for the black player.
   * A castling move is legal if the following conditions are met:
   * 1. The black king has not moved before.
   * 2. The black king is not currently in check.
   * 3. The path between the king and the corresponding rook is clear of any pieces.
   * 4. Neither the king nor the corresponding rook have moved before.
   * 5. The squares between the king and the corresponding rook are not under attack by the opponent.
   * 6. The squares on which the king moves during castling are not under attack by the opponent.
   * 7. The castling move does not lead to a king pawn trap, where the king would be in check after castling.
   *
   * @param playerLegals A collection of legal moves for the black player.
   * @param opponentLegals A collection of legal moves for the white player (opponent).
   * @return A collection of legal castling moves for the black player, which may be empty.
   */
  @Override
  protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
                                                  final Collection<Move> opponentLegals) {
    if (!hasCastleOpportunities()) {
      return Collections.emptyList();
    } final List<Move> kingCastles = new ArrayList<>();
    if (this.playerKing.isFirstMove() && this.playerKing.getPiecePosition() == 4 && !this.isInCheck) {
      if (this.board.getPiece(5) == null && this.board.getPiece(6) == null) {
        final Piece kingSideRook = this.board.getPiece(7);
        if (kingSideRook != null && kingSideRook.isFirstMove() &&
                Player.calculateAttacksOnTile(5, opponentLegals).isEmpty() &&
                Player.calculateAttacksOnTile(6, opponentLegals).isEmpty() &&
                kingSideRook.getPieceType() == ROOK) {
          if (BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
            // Use MovePool for king-side castle move
            kingCastles.add(MovePool.INSTANCE.getKingSideCastleMove(this.board, this.playerKing, 6, (Rook) kingSideRook, kingSideRook.getPiecePosition(), 5));
          }
        }
      } if (this.board.getPiece(1) == null && this.board.getPiece(2) == null &&
              this.board.getPiece(3) == null) {
        final Piece queenSideRook = this.board.getPiece(0);
        if (queenSideRook != null && queenSideRook.isFirstMove() &&
                Player.calculateAttacksOnTile(2, opponentLegals).isEmpty() &&
                Player.calculateAttacksOnTile(3, opponentLegals).isEmpty() &&
                queenSideRook.getPieceType() == ROOK) {
          if (BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
            // Use MovePool for queen-side castle move
            kingCastles.add(MovePool.INSTANCE.getQueenSideCastleMove(this.board, this.playerKing, 2, (Rook) queenSideRook, queenSideRook.getPiecePosition(), 3));
          }
        }
      }
    } return Collections.unmodifiableList(kingCastles);
  }

  /**
   * Gets the opponent of the black player, which is the white player in the chess game.
   *
   * @return The `WhitePlayer` object representing the opponent.
   */
  @Override
  public WhitePlayer getOpponent() {
    return this.board.whitePlayer();
  }

  /**
   * Gets a collection of active black pieces on the current chess board.
   *
   * @return A collection of `Piece` objects representing the active black pieces.
   */
  @Override
  public Collection<Piece> getActivePieces() {
    return this.board.getBlackPieces();
  }

  /**
   * Gets the alliance of the black player, which is `Alliance.BLACK`.
   *
   * @return The `Alliance` enum value representing the black player's alliance.
   */
  @Override
  public Alliance getAlliance() {
    return Alliance.BLACK;
  }

  /**
   * Returns a string representation of the black player's alliance, which is "BLACK".
   *
   * @return The string "BLACK" to represent the black player.
   */
  @Override
  public String toString() {
    return Alliance.BLACK.toString();
  }
}