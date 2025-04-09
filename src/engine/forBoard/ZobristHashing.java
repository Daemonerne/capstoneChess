package engine.forBoard;

import engine.Alliance;
import engine.forPiece.Pawn;
import engine.forPiece.Piece;
import engine.forPiece.Piece.PieceType;

import java.util.Random;

/**
 * The `ZobristHashing` class implements the Zobrist hashing algorithm for chess positions.
 * This algorithm creates unique hash codes for board positions that can be efficiently updated
 * when moves are made, without recalculating the entire hash. It's particularly useful for
 * transposition tables, detecting repeated positions, and other performance optimizations.
 * 
 * @author Aaron Ho
 */
public class ZobristHashing {
  
  /** Random number table for each piece type, on each square, for each alliance. */
  private static final long[][][] PIECE_POSITION_TABLE = new long[2][6][64];
  
  /** Random number for the side to move (when it's black's turn). */
  private static final long SIDE_TO_MOVE;
  
  /** Random numbers for castling rights. */
  private static final long[] CASTLING_RIGHTS = new long[4];
  
  /** Random numbers for en passant file. */
  private static final long[] EN_PASSANT_FILE = new long[8];
  
  static {
    final Random random = new Random(0); // Fixed seed for reproducibility
    for (int alliance = 0; alliance < 2; alliance++) {
      for (int pieceType = 0; pieceType < 6; pieceType++) {
        for (int position = 0; position < 64; position++) {
          PIECE_POSITION_TABLE[alliance][pieceType][position] = random.nextLong();
        }
      }
    } SIDE_TO_MOVE = random.nextLong();
    for (int i = 0; i < 4; i++) {
      CASTLING_RIGHTS[i] = random.nextLong();
    } for (int i = 0; i < 8; i++) {
      EN_PASSANT_FILE[i] = random.nextLong();
    }
  }
  
  /*** Private constructor to prevent instantiation. */
  private ZobristHashing() {
    throw new RuntimeException("Not instantiatable!");
  }
  
  /**
   * Calculates the Zobrist hash for a given board position.
   * 
   * @param board The current chess board.
   * @return The Zobrist hash value.
   */
  public static long calculateBoardHash(final Board board) {
    long hash = 0;
    for (final Piece piece : board.getAllPieces()) {
      hash ^= getPieceHash(piece);
    } if (board.currentPlayer().getAlliance() == Alliance.BLACK) {
      hash ^= SIDE_TO_MOVE;
    } if (board.whitePlayer().getPlayerKing().isKingSideCastleCapable()) {
      hash ^= CASTLING_RIGHTS[0];
    } if (board.whitePlayer().getPlayerKing().isQueenSideCastleCapable()) {
      hash ^= CASTLING_RIGHTS[1];
    } if (board.blackPlayer().getPlayerKing().isKingSideCastleCapable()) {
      hash ^= CASTLING_RIGHTS[2];
    } if (board.blackPlayer().getPlayerKing().isQueenSideCastleCapable()) {
      hash ^= CASTLING_RIGHTS[3];
    } final Pawn enPassantPawn = board.getEnPassantPawn();
    if (enPassantPawn != null) {
      final int enPassantFile = enPassantPawn.getPiecePosition() % 8;
      hash ^= EN_PASSANT_FILE[enPassantFile];
    } return hash;
  }
  
  /**
   * Updates a hash after a piece is moved.
   * 
   * @param hash The current hash.
   * @param piece The piece being moved.
   * @param fromPosition The starting position.
   * @param toPosition The destination position.
   * @return The updated hash.
   */
  public static long updateHashPieceMove(long hash, 
                                        final Piece piece, 
                                        final int fromPosition, 
                                        final int toPosition) {
    hash ^= getPieceHash(piece, fromPosition); // Remove piece from old position
    hash ^= getPieceHash(piece, toPosition);   // Add piece to new position
    return hash;
  }
  
  /**
   * Updates a hash when a piece is captured.
   * 
   * @param hash The current hash.
   * @param capturedPiece The piece being captured.
   * @return The updated hash.
   */
  public static long updateHashPieceCapture(long hash, final Piece capturedPiece) {
    return hash ^ getPieceHash(capturedPiece);
  }
  
  /**
   * Updates a hash for castling.
   * 
   * @param hash The current hash.
   * @param king The king being moved.
   * @param rook The rook being moved.
   * @param kingFrom The king's starting position.
   * @param kingTo The king's destination position.
   * @param rookFrom The rook's starting position.
   * @param rookTo The rook's destination position.
   * @return The updated hash.
   */
  public static long updateHashCastle(long hash, 
                                     final Piece king, 
                                     final Piece rook, 
                                     final int kingFrom, 
                                     final int kingTo, 
                                     final int rookFrom, 
                                     final int rookTo) {
    hash ^= getPieceHash(king, kingFrom);
    hash ^= getPieceHash(king, kingTo);
    hash ^= getPieceHash(rook, rookFrom);
    hash ^= getPieceHash(rook, rookTo);
    return hash;
  }
  
  /**
   * Updates a hash for pawn promotion.
   * 
   * @param hash The current hash.
   * @param pawn The pawn being promoted.
   * @param promotedPiece The piece to which the pawn is promoted.
   * @param position The position of the promotion.
   * @return The updated hash.
   */
  public static long updateHashPromotion(long hash, 
                                        final Piece pawn, 
                                        final Piece promotedPiece, 
                                        final int position) {
    hash ^= getPieceHash(pawn, position);
    hash ^= getPieceHash(promotedPiece, position);
    return hash;
  }
  
  /**
   * Updates a hash for side to move.
   * 
   * @param hash The current hash.
   * @return The updated hash.
   */
  public static long updateHashSideToMove(long hash) {
    return hash ^ SIDE_TO_MOVE;
  }
  
  /**
   * Updates a hash for en passant possibility.
   * 
   * @param hash The current hash.
   * @param enPassantFile The file (0-7) where en passant is possible.
   * @return The updated hash.
   */
  public static long updateHashEnPassant(long hash, final int enPassantFile) {
    if (enPassantFile >= 0 && enPassantFile < 8) {
      hash ^= EN_PASSANT_FILE[enPassantFile];
    } return hash;
  }
  
  /**
   * Updates a hash for castling rights change.
   * 
   * @param hash The current hash.
   * @param castlingRightIndex The index of the castling right (0-3).
   * @return The updated hash.
   */
  public static long updateHashCastlingRight(long hash, final int castlingRightIndex) {
    if (castlingRightIndex >= 0 && castlingRightIndex < 4) {
      hash ^= CASTLING_RIGHTS[castlingRightIndex];
    } return hash;
  }
  
  /**
   * Gets the hash value for a piece at its current position.
   * 
   * @param piece The piece.
   * @return The hash value for the piece at its position.
   */
  private static long getPieceHash(final Piece piece) {
    return getPieceHash(piece, piece.getPiecePosition());
  }
  
  /**
   * Gets the hash value for a piece at a specified position.
   * 
   * @param piece The piece.
   * @param position The position.
   * @return The hash value for the piece at the position.
   */
  private static long getPieceHash(final Piece piece, final int position) {
    final int allianceIndex = piece.getPieceAllegiance().ordinal();
    final int pieceTypeIndex = convertPieceTypeToIndex(piece.getPieceType());
    return PIECE_POSITION_TABLE[allianceIndex][pieceTypeIndex][position];
  }
  
  /**
   * Converts a PieceType to an index for the hash table.
   * 
   * @param pieceType The type of piece.
   * @return The index for the hash table.
   */
  private static int convertPieceTypeToIndex(final PieceType pieceType) {
    return switch (pieceType) {
      case PAWN -> 0;
      case KNIGHT -> 1;
      case BISHOP -> 2;
      case ROOK -> 3;
      case QUEEN -> 4;
      case KING -> 5;
    };
  }
}
