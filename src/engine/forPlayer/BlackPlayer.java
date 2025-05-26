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
 * The BlackPlayer class represents a chess player controlling the black pieces.
 * It extends the abstract Player class and provides black-specific functionality for
 * move generation, castling calculations, and piece management. This implementation
 * handles all legal moves available to black pieces including special moves such as
 * castling and captures.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public final class BlackPlayer extends Player {

  /**
   * Constructs a new BlackPlayer with the specified board and legal move collections.
   * Initializes the player with access to the current board state and legal moves
   * for both black and white players to enable proper move validation and calculation.
   *
   * @param board The current chess board state.
   * @param whiteStandardLegals Collection of legal moves for the white player.
   * @param blackStandardLegals Collection of legal moves for the black player.
   */
  public BlackPlayer(final Board board,
                     final Collection<Move> whiteStandardLegals,
                     final Collection<Move> blackStandardLegals) {
    super(board, blackStandardLegals, whiteStandardLegals);
  }

  /**
   * Calculates all legal castling moves available to the black player.
   * Validates castling conditions including king and rook first-move status,
   * clear paths between pieces, absence of attacks on critical squares,
   * and verification that castling does not result in check. Returns both
   * kingside and queenside castling moves when legal.
   *
   * @param playerLegals Collection of legal moves for the black player.
   * @param opponentLegals Collection of legal moves for the white player.
   * @return Collection of legal castling moves, or empty collection if none available.
   */
  @Override
  protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals,
                                                  final Collection<Move> opponentLegals) {
    if (!hasCastleOpportunities()) {
      return Collections.emptyList();
    }
    final List<Move> kingCastles = new ArrayList<>();
    if (this.playerKing.isFirstMove() && this.playerKing.getPiecePosition() == 4 && !this.isInCheck) {
      if (this.board.getPiece(5) == null && this.board.getPiece(6) == null) {
        final Piece kingSideRook = this.board.getPiece(7);
        if (kingSideRook != null && kingSideRook.isFirstMove() &&
                Player.calculateAttacksOnTile(5, opponentLegals).isEmpty() &&
                Player.calculateAttacksOnTile(6, opponentLegals).isEmpty() &&
                kingSideRook.getPieceType() == ROOK) {
          if (BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
            kingCastles.add(MovePool.INSTANCE.getKingSideCastleMove(this.board, this.playerKing, 6, (Rook) kingSideRook, kingSideRook.getPiecePosition(), 5));
          }
        }
      }
      if (this.board.getPiece(1) == null && this.board.getPiece(2) == null &&
              this.board.getPiece(3) == null) {
        final Piece queenSideRook = this.board.getPiece(0);
        if (queenSideRook != null && queenSideRook.isFirstMove() &&
                Player.calculateAttacksOnTile(2, opponentLegals).isEmpty() &&
                Player.calculateAttacksOnTile(3, opponentLegals).isEmpty() &&
                queenSideRook.getPieceType() == ROOK) {
          if (BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
            kingCastles.add(MovePool.INSTANCE.getQueenSideCastleMove(this.board, this.playerKing, 2, (Rook) queenSideRook, queenSideRook.getPiecePosition(), 3));
          }
        }
      }
    }
    return Collections.unmodifiableList(kingCastles);
  }

  /**
   * Returns the opponent player for the black player.
   * The opponent of the black player is always the white player.
   *
   * @return The WhitePlayer instance representing the opponent.
   */
  @Override
  public WhitePlayer getOpponent() {
    return this.board.whitePlayer();
  }

  /**
   * Returns all active black pieces currently on the chess board.
   * Active pieces are those that remain in play and have not been captured.
   *
   * @return Collection of black pieces on the board.
   */
  @Override
  public Collection<Piece> getActivePieces() {
    return this.board.getBlackPieces();
  }

  /**
   * Returns the alliance designation for the black player.
   * The alliance identifies which color pieces this player controls.
   *
   * @return Alliance.BLACK indicating this player controls black pieces.
   */
  @Override
  public Alliance getAlliance() {
    return Alliance.BLACK;
  }

  /**
   * Returns the string representation of the black player's alliance.
   * Provides a human-readable identifier for this player type.
   *
   * @return String "BLACK" representing this player's alliance.
   */
  @Override
  public String toString() {
    return Alliance.BLACK.toString();
  }
}