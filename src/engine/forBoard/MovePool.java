package engine.forBoard;

import engine.forPiece.Piece;
import engine.forPiece.Rook;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The MovePool class provides object pooling for Move instances to reduce garbage collection
 * pressure and improve performance during chess engine search operations. Each Move type has
 * its own dedicated pool for efficient reuse of objects.
 * <p>
 * Object pooling is particularly important in chess engines where millions of move objects can be
 * created during search, causing significant garbage collection overhead. By reusing existing objects
 * instead of continuously creating new ones, the engine can achieve better performance and reduced
 * memory usage.
 * <p>
 * This class follows the singleton pattern with a static INSTANCE field to ensure only one pool
 * exists throughout the application lifecycle. It provides separate pools for each concrete move type
 * (MajorMove, MajorAttackMove, PawnMove, etc.) and maintains statistics about pool usage.
 *
 * @author Aaron Ho
 */
public final class MovePool {

  /** Singleton instance for global access to the move pools. */
  public static final MovePool INSTANCE = new MovePool();

  /** Default initial size for each move type pool. */
  private static final int POOL_SIZE = 1024;

  /** Counter tracking the number of MajorMove objects created when the pool was empty. */
  private final AtomicInteger majorMoveCreated = new AtomicInteger();

  /** Counter tracking the number of MajorAttackMove objects created when the pool was empty. */
  private final AtomicInteger majorAttackMoveCreated = new AtomicInteger();

  /** Counter tracking the number of PawnMove objects created when the pool was empty. */
  private final AtomicInteger pawnMoveCreated = new AtomicInteger();

  /** Counter tracking the number of PawnAttackMove objects created when the pool was empty. */
  private final AtomicInteger pawnAttackMoveCreated = new AtomicInteger();

  /** Counter tracking the number of PawnJump objects created when the pool was empty. */
  private final AtomicInteger pawnJumpCreated = new AtomicInteger();

  /** Counter tracking the number of PawnEnPassantAttack objects created when the pool was empty. */
  private final AtomicInteger pawnEnPassantAttackCreated = new AtomicInteger();

  /** Counter tracking the number of KingSideCastleMove objects created when the pool was empty. */
  private final AtomicInteger kingSideCastleMoveCreated = new AtomicInteger();

  /** Counter tracking the number of QueenSideCastleMove objects created when the pool was empty. */
  private final AtomicInteger queenSideCastleMoveCreated = new AtomicInteger();

  /** Counter tracking the number of PawnPromotion objects created when the pool was empty. */
  private final AtomicInteger pawnPromotionCreated = new AtomicInteger();

  /** Pool of MajorMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.MajorMove> majorMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of MajorAttackMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.MajorAttackMove> majorAttackMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of PawnMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.PawnMove> pawnMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of PawnAttackMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.PawnAttackMove> pawnAttackMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of PawnJump objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.PawnJump> pawnJumpPool = new ConcurrentLinkedQueue<>();

  /** Pool of PawnEnPassantAttack objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.PawnEnPassantAttack> pawnEnPassantAttackPool = new ConcurrentLinkedQueue<>();

  /** Pool of KingSideCastleMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.KingSideCastleMove> kingSideCastleMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of QueenSideCastleMove objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.QueenSideCastleMove> queenSideCastleMovePool = new ConcurrentLinkedQueue<>();

  /** Pool of PawnPromotion objects available for reuse. */
  private final ConcurrentLinkedQueue<Move.PawnPromotion> pawnPromotionPool = new ConcurrentLinkedQueue<>();

  /**
   * Private constructor to enforce the singleton pattern. Initializes all move type pools
   * with pre-allocated instances to reduce allocation during engine operation.
   */
  private MovePool() {
    initializePools();
  }

  /**
   * Initializes all move pools with pre-allocated move objects. This method populates each
   * type-specific pool with a default number of objects (POOL_SIZE) to minimize allocations
   * during the critical path of chess engine search operations.
   */
  private void initializePools() {
    Board dummyBoard = null;
    Piece dummyPiece = null;

    for (int i = 0; i < POOL_SIZE; i++) {
      majorMovePool.add(new Move.MajorMove(dummyBoard, dummyPiece, -1) {
        @Override
        public Move.MajorMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
          return super.reset(board, pieceMoved, destinationCoordinate);
        }
      });

      majorAttackMovePool.add(new Move.MajorAttackMove(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.MajorAttackMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
          return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
        }
      });

      pawnMovePool.add(new Move.PawnMove(dummyBoard, dummyPiece, -1) {
        @Override
        public Move.PawnMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
          return super.reset(board, pieceMoved, destinationCoordinate);
        }
      });

      pawnAttackMovePool.add(new Move.PawnAttackMove(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.PawnAttackMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
          return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
        }
      });

      pawnJumpPool.add(new Move.PawnJump(dummyBoard, null, -1) {
        @Override
        public Move.PawnJump reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
          return super.reset(board, pieceMoved, destinationCoordinate);
        }
      });

      pawnEnPassantAttackPool.add(new Move.PawnEnPassantAttack(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.PawnEnPassantAttack reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
          return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
        }
      });

      kingSideCastleMovePool.add(new Move.KingSideCastleMove(dummyBoard, dummyPiece, -1, null, -1, -1) {
        @Override
        public Move.KingSideCastleMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate,
                                             final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
          return super.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
        }
      });

      queenSideCastleMovePool.add(new Move.QueenSideCastleMove(dummyBoard, dummyPiece, -1, null, -1, -1) {
        @Override
        public Move.QueenSideCastleMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate,
                                              final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
          return super.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
        }
      });

      pawnPromotionPool.add(new Move.PawnPromotion(null, null) {
        @Override
        public Move.PawnPromotion reset(Move decoratedMove, Piece promotionPiece) {
          this.decoratedMove = decoratedMove;
          this.promotedPawn = (decoratedMove.getMovedPiece() instanceof engine.forPiece.Pawn) ?
                  (engine.forPiece.Pawn) decoratedMove.getMovedPiece() : null;
          this.promotionPiece = promotionPiece;
          this.board = decoratedMove.getBoard();
          this.movedPiece = decoratedMove.getMovedPiece();
          this.destinationCoordinate = decoratedMove.getDestinationCoordinate();
          this.isFirstMove = decoratedMove.getMovedPiece() != null &&
                  decoratedMove.getMovedPiece().isFirstMove();
          return this;
        }
      });
    }
  }

  /**
   * Retrieves a MajorMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The piece being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @return A MajorMove instance with the specified parameters.
   */
  public Move.MajorMove getMajorMove(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.MajorMove move = majorMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    majorMoveCreated.incrementAndGet();
    return new Move.MajorMove(board, pieceMoved, destinationCoordinate) {
      @Override
      public Move.MajorMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
        return super.reset(board, pieceMoved, destinationCoordinate);
      }
    };
  }

  /**
   * Retrieves a MajorAttackMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The piece being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @param attackedPiece The piece being attacked.
   * @return A MajorAttackMove instance with the specified parameters.
   */
  public Move.MajorAttackMove getMajorAttackMove(Board board, Piece pieceMoved, int destinationCoordinate,
                                                 Piece attackedPiece) {
    Move.MajorAttackMove move = majorAttackMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate, attackedPiece);
    }

    majorAttackMoveCreated.incrementAndGet();
    return new Move.MajorAttackMove(board, pieceMoved, destinationCoordinate, attackedPiece) {
      @Override
      public Move.MajorAttackMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
        return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
      }
    };
  }

  /**
   * Retrieves a PawnMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The pawn being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @return A PawnMove instance with the specified parameters.
   */
  public Move.PawnMove getPawnMove(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.PawnMove move = pawnMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    pawnMoveCreated.incrementAndGet();
    return new Move.PawnMove(board, pieceMoved, destinationCoordinate) {
      @Override
      public Move.PawnMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
        return super.reset(board, pieceMoved, destinationCoordinate);
      }
    };
  }

  /**
   * Retrieves a PawnAttackMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The pawn being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @param attackedPiece The piece being attacked.
   * @return A PawnAttackMove instance with the specified parameters.
   */
  public Move.PawnAttackMove getPawnAttackMove(Board board, Piece pieceMoved, int destinationCoordinate,
                                               Piece attackedPiece) {
    Move.PawnAttackMove move = pawnAttackMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate, attackedPiece);
    }

    pawnAttackMoveCreated.incrementAndGet();
    return new Move.PawnAttackMove(board, pieceMoved, destinationCoordinate, attackedPiece) {
      @Override
      public Move.PawnAttackMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
        return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
      }
    };
  }

  /**
   * Retrieves a PawnJump instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The pawn being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @return A PawnJump instance with the specified parameters.
   */
  public Move.PawnJump getPawnJump(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.PawnJump move = pawnJumpPool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    pawnJumpCreated.incrementAndGet();
    return new Move.PawnJump(board, (engine.forPiece.Pawn)pieceMoved, destinationCoordinate) {
      @Override
      public Move.PawnJump reset(final Board board, final Piece pieceMoved, final int destinationCoordinate) {
        return super.reset(board, pieceMoved, destinationCoordinate);
      }
    };
  }

  /**
   * Retrieves a PawnEnPassantAttack instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The pawn being moved.
   * @param destinationCoordinate The destination coordinate of the move.
   * @param attackedPiece The pawn being captured en passant.
   * @return A PawnEnPassantAttack instance with the specified parameters.
   */
  public Move.PawnEnPassantAttack getPawnEnPassantAttack(Board board, Piece pieceMoved, int destinationCoordinate,
                                                         Piece attackedPiece) {
    Move.PawnEnPassantAttack move = pawnEnPassantAttackPool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate, attackedPiece);
    }

    pawnEnPassantAttackCreated.incrementAndGet();
    return new Move.PawnEnPassantAttack(board, pieceMoved, destinationCoordinate, attackedPiece) {
      @Override
      public Move.PawnEnPassantAttack reset(final Board board, final Piece pieceMoved, final int destinationCoordinate, final Piece pieceAttacked) {
        return super.reset(board, pieceMoved, destinationCoordinate, pieceAttacked);
      }
    };
  }

  /**
   * Retrieves a KingSideCastleMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The king being moved.
   * @param destinationCoordinate The destination coordinate of the king.
   * @param castleRook The rook involved in the castling move.
   * @param castleRookStart The starting position of the rook.
   * @param castleRookDestination The destination position of the rook.
   * @return A KingSideCastleMove instance with the specified parameters.
   */
  public Move.KingSideCastleMove getKingSideCastleMove(Board board, Piece pieceMoved, int destinationCoordinate,
                                                       Rook castleRook, int castleRookStart, int castleRookDestination) {
    Move.KingSideCastleMove move = kingSideCastleMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
    }

    kingSideCastleMoveCreated.incrementAndGet();
    return new Move.KingSideCastleMove(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination) {
      @Override
      public Move.KingSideCastleMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate,
                                           final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
        return super.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
      }
    };
  }

  /**
   * Retrieves a QueenSideCastleMove instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param board The current chess board.
   * @param pieceMoved The king being moved.
   * @param destinationCoordinate The destination coordinate of the king.
   * @param castleRook The rook involved in the castling move.
   * @param castleRookStart The starting position of the rook.
   * @param castleRookDestination The destination position of the rook.
   * @return A QueenSideCastleMove instance with the specified parameters.
   */
  public Move.QueenSideCastleMove getQueenSideCastleMove(Board board, Piece pieceMoved, int destinationCoordinate,
                                                         Rook castleRook, int castleRookStart, int castleRookDestination) {
    Move.QueenSideCastleMove move = queenSideCastleMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
    }

    queenSideCastleMoveCreated.incrementAndGet();
    return new Move.QueenSideCastleMove(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination) {
      @Override
      public Move.QueenSideCastleMove reset(final Board board, final Piece pieceMoved, final int destinationCoordinate,
                                            final Rook castleRook, final int castleRookStart, final int castleRookDestination) {
        return super.reset(board, pieceMoved, destinationCoordinate, castleRook, castleRookStart, castleRookDestination);
      }
    };
  }

  /**
   * Retrieves a PawnPromotion instance from the pool or creates a new one if the pool is empty.
   * Retrieved instances have their fields reset to the provided values.
   *
   * @param decoratedMove The underlying move being decorated with promotion.
   * @param promotionPiece The piece to which the pawn is being promoted.
   * @return A PawnPromotion instance with the specified parameters.
   */
  public Move.PawnPromotion getPawnPromotion(Move decoratedMove, Piece promotionPiece) {
    Move.PawnPromotion move = pawnPromotionPool.poll();
    if (move != null) {
      return move.reset(decoratedMove, promotionPiece);
    }

    pawnPromotionCreated.incrementAndGet();
    return new Move.PawnPromotion(decoratedMove, promotionPiece) {
      @Override
      public Move.PawnPromotion reset(Move decoratedMove, Piece promotionPiece) {
        this.decoratedMove = decoratedMove;
        this.promotedPawn = (decoratedMove.getMovedPiece() instanceof engine.forPiece.Pawn) ?
                (engine.forPiece.Pawn) decoratedMove.getMovedPiece() : null;
        this.promotionPiece = promotionPiece;
        this.board = decoratedMove.getBoard();
        this.movedPiece = decoratedMove.getMovedPiece();
        this.destinationCoordinate = decoratedMove.getDestinationCoordinate();
        this.isFirstMove = decoratedMove.getMovedPiece() != null &&
                decoratedMove.getMovedPiece().isFirstMove();
        return this;
      }
    };
  }
}