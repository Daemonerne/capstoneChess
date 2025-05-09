package engine.forBoard;

import engine.Alliance;
import engine.forPiece.Piece;
import engine.forPiece.Rook;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MovePool provides object pooling for Move instances to reduce garbage collection
 * pressure and improve performance during search. Each Move type has its own pool.
 *
 * @author Claude
 */
public final class MovePool {

  /** Singleton instance */
  public static final MovePool INSTANCE = new MovePool();

  /** Base pool size for each move type */
  private static final int POOL_SIZE = 1024;

  /** Statistics for monitoring pool usage */
  private final AtomicInteger majorMoveCreated = new AtomicInteger();
  private final AtomicInteger majorAttackMoveCreated = new AtomicInteger();
  private final AtomicInteger pawnMoveCreated = new AtomicInteger();
  private final AtomicInteger pawnAttackMoveCreated = new AtomicInteger();
  private final AtomicInteger pawnJumpCreated = new AtomicInteger();
  private final AtomicInteger pawnEnPassantAttackCreated = new AtomicInteger();
  private final AtomicInteger kingSideCastleMoveCreated = new AtomicInteger();
  private final AtomicInteger queenSideCastleMoveCreated = new AtomicInteger();
  private final AtomicInteger pawnPromotionCreated = new AtomicInteger();

  /** Move pools for each concrete move type */
  private final ConcurrentLinkedQueue<Move.MajorMove> majorMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.MajorAttackMove> majorAttackMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.PawnMove> pawnMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.PawnAttackMove> pawnAttackMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.PawnJump> pawnJumpPool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.PawnEnPassantAttack> pawnEnPassantAttackPool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.KingSideCastleMove> kingSideCastleMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.QueenSideCastleMove> queenSideCastleMovePool = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Move.PawnPromotion> pawnPromotionPool = new ConcurrentLinkedQueue<>();

  /** Private constructor to enforce singleton pattern */
  private MovePool() {
    initializePools();
  }

  /** Initialize all move pools with pre-allocated instances */
  private void initializePools() {
    // Initialize with dummy values that will be reset before use
    Board dummyBoard = null;
    Piece dummyPiece = null;

    for (int i = 0; i < POOL_SIZE; i++) {
      majorMovePool.add(new Move.MajorMove(dummyBoard, dummyPiece, -1) {
        @Override
        public Move.MajorMove reset(Board board, Piece pieceMoved, int destinationCoordinate) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          return this;
        }
      });

      majorAttackMovePool.add(new Move.MajorAttackMove(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.MajorAttackMove reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          this.attackedPiece = attackedPiece;
          return this;
        }
      });

      pawnMovePool.add(new Move.PawnMove(dummyBoard, dummyPiece, -1) {
        @Override
        public Move.PawnMove reset(Board board, Piece pieceMoved, int destinationCoordinate) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          return this;
        }
      });

      pawnAttackMovePool.add(new Move.PawnAttackMove(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.PawnAttackMove reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          this.attackedPiece = attackedPiece;
          return this;
        }
      });

      pawnJumpPool.add(new Move.PawnJump(dummyBoard, null, -1) {
        @Override
        public Move.PawnJump reset(Board board, Piece pieceMoved, int destinationCoordinate) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          return this;
        }
      });

      pawnEnPassantAttackPool.add(new Move.PawnEnPassantAttack(dummyBoard, dummyPiece, -1, dummyPiece) {
        @Override
        public Move.PawnEnPassantAttack reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          this.attackedPiece = attackedPiece;
          return this;
        }
      });

      kingSideCastleMovePool.add(new Move.KingSideCastleMove(dummyBoard, dummyPiece, -1, null, -1, -1) {
        @Override
        public Move.KingSideCastleMove reset(Board board, Piece pieceMoved, int destinationCoordinate,
                                             Rook castleRook, int castleRookStart, int castleRookDestination) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          this.castleRook = castleRook;
          this.castleRookStart = castleRookStart;
          this.castleRookDestination = castleRookDestination;
          return this;
        }
      });

      queenSideCastleMovePool.add(new Move.QueenSideCastleMove(dummyBoard, dummyPiece, -1, null, -1, -1) {
        @Override
        public Move.QueenSideCastleMove reset(Board board, Piece pieceMoved, int destinationCoordinate,
                                              Rook castleRook, int castleRookStart, int castleRookDestination) {
          this.board = board;
          this.movedPiece = pieceMoved;
          this.destinationCoordinate = destinationCoordinate;
          this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
          this.castleRook = castleRook;
          this.castleRookStart = castleRookStart;
          this.castleRookDestination = castleRookDestination;
          return this;
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
   * Gets a MajorMove instance from the pool or creates one if the pool is empty.
   */
  public Move.MajorMove getMajorMove(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.MajorMove move = majorMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    majorMoveCreated.incrementAndGet();
    return new Move.MajorMove(board, pieceMoved, destinationCoordinate) {
      @Override
      public Move.MajorMove reset(Board board, Piece pieceMoved, int destinationCoordinate) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        return this;
      }
    };
  }

  /**
   * Gets a MajorAttackMove instance from the pool or creates one if the pool is empty.
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
      public Move.MajorAttackMove reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        this.attackedPiece = attackedPiece;
        return this;
      }
    };
  }

  /**
   * Gets a PawnMove instance from the pool or creates one if the pool is empty.
   */
  public Move.PawnMove getPawnMove(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.PawnMove move = pawnMovePool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    pawnMoveCreated.incrementAndGet();
    return new Move.PawnMove(board, pieceMoved, destinationCoordinate) {
      @Override
      public Move.PawnMove reset(Board board, Piece pieceMoved, int destinationCoordinate) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        return this;
      }
    };
  }

  /**
   * Gets a PawnAttackMove instance from the pool or creates one if the pool is empty.
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
      public Move.PawnAttackMove reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        this.attackedPiece = attackedPiece;
        return this;
      }
    };
  }

  /**
   * Gets a PawnJump instance from the pool or creates one if the pool is empty.
   */
  public Move.PawnJump getPawnJump(Board board, Piece pieceMoved, int destinationCoordinate) {
    Move.PawnJump move = pawnJumpPool.poll();
    if (move != null) {
      return move.reset(board, pieceMoved, destinationCoordinate);
    }

    pawnJumpCreated.incrementAndGet();
    return new Move.PawnJump(board, (engine.forPiece.Pawn)pieceMoved, destinationCoordinate) {
      @Override
      public Move.PawnJump reset(Board board, Piece pieceMoved, int destinationCoordinate) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        return this;
      }
    };
  }

  /**
   * Gets a PawnEnPassantAttack instance from the pool or creates one if the pool is empty.
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
      public Move.PawnEnPassantAttack reset(Board board, Piece pieceMoved, int destinationCoordinate, Piece attackedPiece) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        this.attackedPiece = attackedPiece;
        return this;
      }
    };
  }

  /**
   * Gets a KingSideCastleMove instance from the pool or creates one if the pool is empty.
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
      public Move.KingSideCastleMove reset(Board board, Piece pieceMoved, int destinationCoordinate,
                                           Rook castleRook, int castleRookStart, int castleRookDestination) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        this.castleRook = castleRook;
        this.castleRookStart = castleRookStart;
        this.castleRookDestination = castleRookDestination;
        return this;
      }
    };
  }

  /**
   * Gets a QueenSideCastleMove instance from the pool or creates one if the pool is empty.
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
      public Move.QueenSideCastleMove reset(Board board, Piece pieceMoved, int destinationCoordinate,
                                            Rook castleRook, int castleRookStart, int castleRookDestination) {
        this.board = board;
        this.movedPiece = pieceMoved;
        this.destinationCoordinate = destinationCoordinate;
        this.isFirstMove = pieceMoved != null && pieceMoved.isFirstMove();
        this.castleRook = castleRook;
        this.castleRookStart = castleRookStart;
        this.castleRookDestination = castleRookDestination;
        return this;
      }
    };
  }

  /**
   * Gets a PawnPromotion instance from the pool or creates one if the pool is empty.
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

  // --- Return methods for different move types ---

  /**
   * Returns a MajorMove to the pool.
   */
  public void returnMajorMove(Move.MajorMove move) {
    if (move != null) {
      majorMovePool.offer(move);
    }
  }

  /**
   * Returns a MajorAttackMove to the pool.
   */
  public void returnMajorAttackMove(Move.MajorAttackMove move) {
    if (move != null) {
      majorAttackMovePool.offer(move);
    }
  }

  /**
   * Returns a PawnMove to the pool.
   */
  public void returnPawnMove(Move.PawnMove move) {
    if (move != null) {
      pawnMovePool.offer(move);
    }
  }

  /**
   * Returns a PawnAttackMove to the pool.
   */
  public void returnPawnAttackMove(Move.PawnAttackMove move) {
    if (move != null) {
      pawnAttackMovePool.offer(move);
    }
  }

  /**
   * Returns a PawnJump to the pool.
   */
  public void returnPawnJump(Move.PawnJump move) {
    if (move != null) {
      pawnJumpPool.offer(move);
    }
  }

  /**
   * Returns a PawnEnPassantAttack to the pool.
   */
  public void returnPawnEnPassantAttack(Move.PawnEnPassantAttack move) {
    if (move != null) {
      pawnEnPassantAttackPool.offer(move);
    }
  }

  /**
   * Returns a KingSideCastleMove to the pool.
   */
  public void returnKingSideCastleMove(Move.KingSideCastleMove move) {
    if (move != null) {
      kingSideCastleMovePool.offer(move);
    }
  }

  /**
   * Returns a QueenSideCastleMove to the pool.
   */
  public void returnQueenSideCastleMove(Move.QueenSideCastleMove move) {
    if (move != null) {
      queenSideCastleMovePool.offer(move);
    }
  }

  /**
   * Returns a PawnPromotion to the pool.
   */
  public void returnPawnPromotion(Move.PawnPromotion move) {
    if (move != null) {
      pawnPromotionPool.offer(move);
    }
  }

  /**
   * Returns any move to the appropriate pool based on its type.
   */
  public void returnMove(Move move) {
    if (move == null) return;

    if (move instanceof Move.MajorMove) {
      returnMajorMove((Move.MajorMove) move);
    } else if (move instanceof Move.MajorAttackMove) {
      returnMajorAttackMove((Move.MajorAttackMove) move);
    } else if (move instanceof Move.PawnEnPassantAttack) {
      returnPawnEnPassantAttack((Move.PawnEnPassantAttack) move);
    } else if (move instanceof Move.PawnAttackMove) {
      returnPawnAttackMove((Move.PawnAttackMove) move);
    } else if (move instanceof Move.PawnJump) {
      returnPawnJump((Move.PawnJump) move);
    } else if (move instanceof Move.PawnMove) {
      returnPawnMove((Move.PawnMove) move);
    } else if (move instanceof Move.KingSideCastleMove) {
      returnKingSideCastleMove((Move.KingSideCastleMove) move);
    } else if (move instanceof Move.QueenSideCastleMove) {
      returnQueenSideCastleMove((Move.QueenSideCastleMove) move);
    } else if (move instanceof Move.PawnPromotion) {
      returnPawnPromotion((Move.PawnPromotion) move);
    }
  }

  /**
   * Get statistics about pool usage.
   */
  public String getPoolStats() {
    return String.format(
            "MovePool Stats:\n" +
                    "MajorMove: %d created, %d available\n" +
                    "MajorAttackMove: %d created, %d available\n" +
                    "PawnMove: %d created, %d available\n" +
                    "PawnAttackMove: %d created, %d available\n" +
                    "PawnJump: %d created, %d available\n" +
                    "PawnEnPassantAttack: %d created, %d available\n" +
                    "KingSideCastleMove: %d created, %d available\n" +
                    "QueenSideCastleMove: %d created, %d available\n" +
                    "PawnPromotion: %d created, %d available",
            majorMoveCreated.get(), majorMovePool.size(),
            majorAttackMoveCreated.get(), majorAttackMovePool.size(),
            pawnMoveCreated.get(), pawnMovePool.size(),
            pawnAttackMoveCreated.get(), pawnAttackMovePool.size(),
            pawnJumpCreated.get(), pawnJumpPool.size(),
            pawnEnPassantAttackCreated.get(), pawnEnPassantAttackPool.size(),
            kingSideCastleMoveCreated.get(), kingSideCastleMovePool.size(),
            queenSideCastleMoveCreated.get(), queenSideCastleMovePool.size(),
            pawnPromotionCreated.get(), pawnPromotionPool.size()
    );
  }
}