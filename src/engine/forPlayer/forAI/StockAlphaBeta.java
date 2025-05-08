package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MoveTransition;
import engine.forPiece.Piece;
import engine.forPlayer.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static engine.forBoard.BoardUtils.mvvlva;
import static engine.forBoard.Move.MoveFactory;

/**
 * The StockAlphaBeta class represents a chess engine that employs the alpha-beta search algorithm
 * with Lazy SMP parallel search optimization. It uses the Young Brothers Wait concept to improve
 * search efficiency in a parallel environment.
 */
public class StockAlphaBeta extends Observable implements MoveStrategy {

  /** The evaluator used to assess board positions and generate evaluation scores. */
  private final BoardEvaluator evaluator;

  /** The maximum depth for iterative deepening. */
  private final int maxDepth;

  /** The count of boards evaluated during the search. */
  private final AtomicLong boardsEvaluated = new AtomicLong(0);

  /** Thread-local search statistics. */
  private final ThreadLocal<SearchStats> threadStats = new ThreadLocal<>();

  /** The number of threads to use for search. */
  private final int threadCount;

  /** Flag to indicate search should be stopped. */
  private volatile boolean searchStopped;

  /** Thread pool for search workers. */
  private final ExecutorService searchThreadPool;

  /** Thread-safe transposition table. */
  private final StripedTranspositionTable transpositionTable;

  /** An array of previous historically good moves that influences the sorting of moves. */
  private static final int[][] historyHeuristic = new int[64][64];

  /** Killer moves table [killer_slot][ply]. */
  private final ThreadLocal<Move[][]> killerMoves = ThreadLocal.withInitial(() ->
          new Move[2][MAX_SEARCH_DEPTH]);

  /** The maximum number of quiescence searches allowed. */
  private static final int MAX_QUIESCENCE = 100000;

  /** Maximum search depth for data structures. */
  private static final int MAX_SEARCH_DEPTH = 100;

  /** The futility pruning depth used to prune futile moves. */
  private static final int FUTILITY_PRUNING_DEPTH = 2;

  /** A depth threshold that when crossed initiates a LMR (Late Move Reduction) in depth. */
  private static final int LMR_THRESHOLD = 7;

  /** The reduction scale used in a Late Move Reduction. */
  private static final double LMR_SCALE = 0.9;

  /** The delta pruning value used to prune likely unnecessary branches. */
  private static final double DELTA_PRUNING_VALUE = 5;

  /** Alpha-beta window bounds for aspiration search. */
  private double highestSeenValue = Double.NEGATIVE_INFINITY;
  private double lowestSeenValue = Double.POSITIVE_INFINITY;

  /** Enumeration representing different move sorting strategies. */
  private enum MoveSorter {

    /** Standard sorting based on history heuristic. */
    STANDARD {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board, final ThreadLocal<Move[][]> killerMoves, final int ply) {
        List<Move> sortedMoves = new ArrayList<>(moves);
        Move[][] killers = killerMoves.get();

        sortedMoves.sort((move1, move2) -> {
          // First check if either move is a killer move
          boolean isKiller1 = move1.equals(killers[0][ply]) || move1.equals(killers[1][ply]);
          boolean isKiller2 = move2.equals(killers[0][ply]) || move2.equals(killers[1][ply]);

          if (isKiller1 && !isKiller2) return -1;
          if (!isKiller1 && isKiller2) return 1;

          // Then use history heuristic
          int score1 = historyHeuristic[move1.getCurrentCoordinate()][move1.getDestinationCoordinate()];
          int score2 = historyHeuristic[move2.getCurrentCoordinate()][move2.getDestinationCoordinate()];
          return Integer.compare(score2, score1);
        });
        return sortedMoves;
      }
    },

    /** Expensive sorting for root moves. */
    EXPENSIVE {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board, final ThreadLocal<Move[][]> killerMoves, final int ply) {
        return Ordering.from((Comparator<Move>) (move1, move2) -> ComparisonChain.start()
                .compareTrueFirst(BoardUtils.kingThreat(move1), BoardUtils.kingThreat(move2))
                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                .compare(mvvlva(move2), mvvlva(move1))
                .result()).immutableSortedCopy(moves);
      }
    };

    abstract Collection<Move> sort(Collection<Move> moves, final Board board, final ThreadLocal<Move[][]> killerMoves, final int ply);
  }

  /**
   * Constructs a StockAlphaBeta instance with the specified search depth.
   *
   * @param maxDepth The maximum depth for iterative deepening.
   * @param board The initial board for evaluator selection.
   */
  public StockAlphaBeta(final int maxDepth, final Board board) {
    this.maxDepth = maxDepth;
    this.threadCount = Runtime.getRuntime().availableProcessors();
    this.searchThreadPool = Executors.newFixedThreadPool(threadCount);
    this.transpositionTable = new StripedTranspositionTable(256); // 256MB default
    this.evaluator = determineGameState(board);
  }

  /**
   * Returns a string representation of the StockAlphaBeta instance.
   *
   * @return A string representing the instance.
   */
  @Override
  public String toString() {
    return "StockAB with Lazy SMP";
  }

  /**
   * Executes the alpha-beta search algorithm with Iterative Deepening and Lazy SMP
   * to find the best move for the current player.
   *
   * @param board The current chess board.
   * @return The best move determined by the algorithm.
   */
  @Override
  public Move execute(final Board board) {
    final long startTime = System.currentTimeMillis();
    Move bestMove = MoveFactory.getNullMove();

    this.searchStopped = false;
    this.boardsEvaluated.set(0);
    this.transpositionTable.incrementAge();

    try {
      // For each iterative deepening depth
      for (int currentDepth = 1; currentDepth <= maxDepth && !searchStopped; currentDepth++) {
        bestMove = searchRootParallel(board, currentDepth);

        // Update history heuristic for the best move
        updateHistoryHeuristic(bestMove, currentDepth);

        // Output statistics
        final long evaluatedPositions = this.boardsEvaluated.get();
        final long executionTime = System.currentTimeMillis() - startTime;
        final String result = String.format(
                "%s | depth = %d | boards evaluated = %d | time = %.2f sec | nps = %.2f M",
                bestMove, currentDepth, evaluatedPositions,
                executionTime / 1000.0,
                (evaluatedPositions / (executionTime / 1000.0)) / 1_000_000.0);

        System.out.println(result);
        setChanged();
        notifyObservers(result);
      }
    } finally {
      // Don't shut down the thread pool here as it's reused
    }

    return bestMove;
  }

  /**
   * Searches the root position in parallel using Lazy SMP.
   *
   * @param board The root board position
   * @param depth The current search depth
   * @return The best move found
   */
  private Move searchRootParallel(final Board board, final int depth) {
    // Sort moves to try most promising first
    final List<Move> allMoves = new ArrayList<>(
            MoveSorter.EXPENSIVE.sort(board.currentPlayer().getLegalMoves(), board, killerMoves, 0));

    if (allMoves.isEmpty()) {
      return MoveFactory.getNullMove();
    }

    // For tracking the best move found
    final AtomicReference<Move> globalBestMove = new AtomicReference<>(allMoves.get(0));
    final AtomicReference<Double> globalBestScore = new AtomicReference<>(
            board.currentPlayer().getAlliance().isWhite() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);

    // Counter for distributing moves to threads
    final AtomicInteger moveIndex = new AtomicInteger(0);

    // Ensure first move is fully searched before others (Young Brothers Wait)
    final CountDownLatch firstMoveLatch = new CountDownLatch(1);

    // Create tasks for each thread
    List<Future<?>> futures = new ArrayList<>();
    for (int threadId = 0; threadId < threadCount; threadId++) {
      final int id = threadId;
      futures.add(searchThreadPool.submit(() -> {
        // Initialize thread-local stats
        SearchStats stats = new SearchStats();
        threadStats.set(stats);

        // Initialize thread-local killers
        if (killerMoves.get() == null) {
          killerMoves.set(new Move[2][MAX_SEARCH_DEPTH]);
        }

        try {
          // First thread searches the first move
          if (id == 0) {
            searchFirstMove(board, allMoves.get(0), depth, globalBestMove, globalBestScore);
            firstMoveLatch.countDown(); // Signal other threads to start
          } else {
            // Other threads wait for first move to be searched
            firstMoveLatch.await();
          }

          // Then all threads take moves from the shared queue
          int idx;
          while ((idx = moveIndex.getAndIncrement()) < allMoves.size() && !searchStopped) {
            if (idx == 0) continue; // Skip first move (already searched)

            final Move move = allMoves.get(idx);
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

            if (moveTransition.moveStatus().isDone()) {
              searchMove(board, move, moveTransition.toBoard(), depth, globalBestMove, globalBestScore);
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          // Update shared statistics
          boardsEvaluated.addAndGet(stats.boardsEvaluated);
        }
      }));
    }

    // Wait for all threads to finish
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Update aspiration window bounds for next iteration
    if (board.currentPlayer().getAlliance().isWhite()) {
      highestSeenValue = globalBestScore.get();
    } else {
      lowestSeenValue = globalBestScore.get();
    }

    return globalBestMove.get();
  }

  /**
   * Search the first move at the root with Young Brothers Wait protocol.
   */
  private void searchFirstMove(Board board, Move move, int depth,
                               AtomicReference<Move> bestMove, AtomicReference<Double> bestScore) {
    final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
    if (moveTransition.moveStatus().isDone()) {
      searchMove(board, move, moveTransition.toBoard(), depth, bestMove, bestScore);
    }
  }

  /**
   * Search a move at the root and update best move if better is found.
   */
  private void searchMove(Board board, Move move, Board toBoard, int depth,
                          AtomicReference<Move> bestMove, AtomicReference<Double> bestScore) {
    double score;
    if (board.currentPlayer().getAlliance().isWhite()) {
      // White is maximizing
      score = min(toBoard, depth - 1, bestScore.get(), Double.POSITIVE_INFINITY, 1);
      if (score > bestScore.get()) {
        synchronized (bestScore) {
          if (score > bestScore.get()) {
            bestScore.set(score);
            bestMove.set(move);
          }
        }
      }
    } else {
      // Black is minimizing
      score = max(toBoard, depth - 1, Double.NEGATIVE_INFINITY, bestScore.get(), 1);
      if (score < bestScore.get()) {
        synchronized (bestScore) {
          if (score < bestScore.get()) {
            bestScore.set(score);
            bestMove.set(move);
          }
        }
      }
    }
  }

  /**
   * Calculates the depth for the quiescence search based on the board state and current depth.
   *
   * @param toBoard The resulting board after a move.
   * @param depth The current search depth.
   * @return The adjusted depth for quiescence search.
   */
  private int calculateQuiescenceDepth(final Board toBoard, final int depth) {
    SearchStats stats = threadStats.get();

    if (depth == 1 && stats.quiescenceCount < MAX_QUIESCENCE) {
      int activityMeasure = 0;
      if (toBoard.currentPlayer().isInCheck()) {
        activityMeasure += 1;
      }
      for (final Move move : BoardUtils.lastNMoves(toBoard, 2)) {
        if (move.isAttack()) {
          activityMeasure += 1;
        }
      }
      if (activityMeasure >= 2) {
        stats.quiescenceCount++;
        return 1;
      }
    }
    return depth - 1;
  }

  /**
   * Implements the max portion of the alpha-beta search algorithm.
   *
   * @param board The current chess board.
   * @param depth The current search depth.
   * @param highest The highest value seen in the search.
   * @param lowest The lowest value seen in the search.
   * @param ply The current ply in the search.
   * @return The score value.
   */
  private double max(final Board board, int depth, double highest, double lowest, int ply) {
    SearchStats stats = threadStats.get();

    if (depth <= 0 || BoardUtils.isEndOfGame(board) || searchStopped) {
      stats.boardsEvaluated++;
      return this.evaluator.evaluate(board, depth);
    }

    // Transposition table lookup
    long zobristHash = board.getZobristHash();
    TranspositionTable.Entry entry = transpositionTable.get(zobristHash);
    if (entry != null && entry.depth >= depth) {
      if (entry.nodeType == TranspositionTable.EXACT) {
        return entry.score;
      } else if (entry.nodeType == TranspositionTable.LOWERBOUND) {
        highest = Math.max(highest, entry.score);
      } else if (entry.nodeType == TranspositionTable.UPPERBOUND) {
        lowest = Math.min(lowest, entry.score);
      }
      if (highest >= lowest) {
        return entry.score;
      }
    }

    // Futility pruning
    if (depth < FUTILITY_PRUNING_DEPTH) {
      double futilityValue = this.evaluator.evaluate(board, depth);
      if (futilityValue >= lowest) {
        return futilityValue;
      }
    }

    // Late Move Reduction
    if (depth <= LMR_THRESHOLD) {
      depth = (int) (depth * LMR_SCALE);
    }

    // Null Move Pruning (added feature)
    if (depth >= 3 && !board.currentPlayer().isInCheck() && hasNonPawnMaterial(board.currentPlayer())) {
      Board nullMoveBoard = makeNullMove(board);
      double nullMoveScore = min(nullMoveBoard, depth - 1 - 2, highest, lowest, ply + 1);
      if (nullMoveScore >= lowest) {
        return lowest; // Beta cutoff
      }
    }

    double currentHighest = highest;
    boolean firstMove = true;
    Move[][] killers = killerMoves.get();

    for (final Move move : MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board, killerMoves, ply)) {
      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        double currentValue;

        // Principal Variation Search
        if (firstMove) {
          currentValue = min(toBoard, calculateQuiescenceDepth(toBoard, depth), currentHighest, lowest, ply + 1);
        } else {
          // Try reduced depth for non-PV nodes
          currentValue = min(toBoard, calculateQuiescenceDepth(toBoard, depth - 1), currentHighest, currentHighest + 0.1, ply + 1);

          // Re-search if promising and in window
          if (currentValue > currentHighest && currentValue < lowest) {
            currentValue = min(toBoard, calculateQuiescenceDepth(toBoard, depth), currentHighest, lowest, ply + 1);
          }
        }

        if (currentValue > currentHighest) {
          currentHighest = currentValue;

          // Update killer moves
          if (!move.isAttack()) {
            if (!move.equals(killers[0][ply])) {
              killers[1][ply] = killers[0][ply];
              killers[0][ply] = move;
            }
          }

          if (currentHighest >= lowest) {
            if (currentHighest >= lowest + DELTA_PRUNING_VALUE) {
              byte nodeType = TranspositionTable.LOWERBOUND;
              transpositionTable.store(zobristHash, currentHighest, depth, nodeType);
              return lowest;
            }
          }
        }

        firstMove = false;
      }
    }

    // Store position in transposition table
    byte nodeType = TranspositionTable.EXACT;
    if (currentHighest <= highest) {
      nodeType = TranspositionTable.UPPERBOUND;
    } else if (currentHighest >= lowest) {
      nodeType = TranspositionTable.LOWERBOUND;
    }
    transpositionTable.store(zobristHash, currentHighest, depth, nodeType);

    return currentHighest;
  }

  /**
   * Implements the min portion of the alpha-beta search algorithm.
   *
   * @param board The current chess board.
   * @param depth The current search depth.
   * @param highest The highest value seen in the search.
   * @param lowest The lowest value seen in the search.
   * @param ply The current ply in the search.
   * @return The score value.
   */
  private double min(final Board board, int depth, double highest, double lowest, int ply) {
    SearchStats stats = threadStats.get();

    if (depth <= 0 || BoardUtils.isEndOfGame(board) || searchStopped) {
      stats.boardsEvaluated++;
      return this.evaluator.evaluate(board, depth);
    }

    // Transposition table lookup
    long zobristHash = board.getZobristHash();
    TranspositionTable.Entry entry = transpositionTable.get(zobristHash);
    if (entry != null && entry.depth >= depth) {
      if (entry.nodeType == TranspositionTable.EXACT) {
        return entry.score;
      } else if (entry.nodeType == TranspositionTable.LOWERBOUND) {
        highest = Math.max(highest, entry.score);
      } else if (entry.nodeType == TranspositionTable.UPPERBOUND) {
        lowest = Math.min(lowest, entry.score);
      }
      if (lowest <= highest) {
        return entry.score;
      }
    }

    // Futility pruning
    if (depth < FUTILITY_PRUNING_DEPTH) {
      double futilityValue = this.evaluator.evaluate(board, depth);
      if (futilityValue <= highest) {
        return futilityValue;
      }
    }

    // Late Move Reduction
    if (depth <= LMR_THRESHOLD) {
      depth = (int) (depth * LMR_SCALE);
    }

    // Null Move Pruning
    if (depth >= 3 && !board.currentPlayer().isInCheck() && hasNonPawnMaterial(board.currentPlayer())) {
      Board nullMoveBoard = makeNullMove(board);
      double nullMoveScore = max(nullMoveBoard, depth - 1 - 2, highest, lowest, ply + 1);
      if (nullMoveScore <= highest) {
        return highest; // Alpha cutoff
      }
    }

    double currentLowest = lowest;
    boolean firstMove = true;
    Move[][] killers = killerMoves.get();

    for (final Move move : MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board, killerMoves, ply)) {
      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        double currentValue;

        // Principal Variation Search
        if (firstMove) {
          currentValue = max(toBoard, calculateQuiescenceDepth(toBoard, depth), highest, currentLowest, ply + 1);
        } else {
          // Try reduced depth for non-PV nodes
          currentValue = max(toBoard, calculateQuiescenceDepth(toBoard, depth - 1), currentLowest - 0.1, currentLowest, ply + 1);

          // Re-search if promising and in window
          if (currentValue < currentLowest && currentValue > highest) {
            currentValue = max(toBoard, calculateQuiescenceDepth(toBoard, depth), highest, currentLowest, ply + 1);
          }
        }

        if (currentValue < currentLowest) {
          currentLowest = currentValue;

          // Update killer moves
          if (!move.isAttack()) {
            if (!move.equals(killers[0][ply])) {
              killers[1][ply] = killers[0][ply];
              killers[0][ply] = move;
            }
          }

          if (currentLowest <= highest) {
            if (currentLowest <= highest - DELTA_PRUNING_VALUE) {
              byte nodeType = TranspositionTable.UPPERBOUND;
              transpositionTable.store(zobristHash, currentLowest, depth, nodeType);
              return highest;
            }
          }
        }

        firstMove = false;
      }
    }

    // Store position in transposition table
    byte nodeType = TranspositionTable.EXACT;
    if (currentLowest <= highest) {
      nodeType = TranspositionTable.UPPERBOUND;
    } else if (currentLowest >= lowest) {
      nodeType = TranspositionTable.LOWERBOUND;
    }
    transpositionTable.store(zobristHash, currentLowest, depth, nodeType);

    return currentLowest;
  }

  /**
   * Creates a board where the current player passes (makes a "null move").
   */
  private Board makeNullMove(Board board) {
    Board.Builder builder = new Board.Builder();
    for (Piece piece : board.getAllPieces()) {
      builder.setPiece(piece);
    }
    builder.setMoveMaker(board.currentPlayer().getOpponent().getAlliance());
    return builder.build();
  }

  /**
   * Checks if the player has non-pawn material (excluding king).
   */
  private boolean hasNonPawnMaterial(Player player) {
    for (Piece piece : player.getActivePieces()) {
      if (piece.getPieceType() != Piece.PieceType.PAWN &&
              piece.getPieceType() != Piece.PieceType.KING) {
        return true;
      }
    }
    return false;
  }

  /**
   * Stores a move that was made into the history heuristic table.
   *
   * @param move The move to be recorded.
   * @param depth The depth to be recorded.
   */
  private void updateHistoryHeuristic(Move move, int depth) {
    if (move != null && move != MoveFactory.getNullMove()) {
      historyHeuristic[move.getCurrentCoordinate()][move.getDestinationCoordinate()] += depth * depth;
    }
  }

  /**
   * Determines the game state for the current position to select appropriate evaluator.
   *
   * @param board The board whose game state is to be determined.
   * @return The appropriate board evaluator for the current phase.
   */
  @VisibleForTesting
  private BoardEvaluator determineGameState(final Board board) {
    return TaperedEvaluator.get();
  }

  /**
   * Thread-local search statistics.
   */
  private static class SearchStats {
    long boardsEvaluated;
    int quiescenceCount;
  }

  /**
   * A thread-safe transposition table using striped locking.
   */
  private static class StripedTranspositionTable {
    private final TranspositionTable.Entry[] table;
    private final int mask;
    private volatile byte currentAge;
    private final ReadWriteLock[] locks;
    private static final int LOCK_COUNT = 1024;  // Number of locks to reduce contention

    public StripedTranspositionTable(int sizeMB) {
      // Calculate size as power of 2
      long bytes = (long) sizeMB * 1024 * 1024;
      int entryCount = (int) (bytes / 24);  // Estimated size of an entry
      int size = Integer.highestOneBit(entryCount);

      table = new TranspositionTable.Entry[size];
      mask = size - 1;
      currentAge = 0;

      // Initialize locks
      locks = new ReentrantReadWriteLock[LOCK_COUNT];
      for (int i = 0; i < LOCK_COUNT; i++) {
        locks[i] = new ReentrantReadWriteLock();
      }

      // Initialize table entries
      for (int i = 0; i < size; i++) {
        table[i] = new TranspositionTable.Entry();
      }

      System.out.println("Transposition Table created with " + size +
              " entries (" + (size * 24 / (1024 * 1024)) + " MB)");
    }

    private ReadWriteLock getLock(long hash) {
      return locks[(int)(hash & (LOCK_COUNT - 1))];
    }

    public void incrementAge() {
      currentAge++;
      if (currentAge == 0) {
        currentAge = 1;
      }
    }

    public TranspositionTable.Entry get(long zobristHash) {
      int index = (int) (zobristHash & mask);

      ReadWriteLock lock = getLock(zobristHash);
      lock.readLock().lock();
      try {
        TranspositionTable.Entry entry = table[index];
        if (entry.key == zobristHash && entry.key != 0) {
          entry.age = currentAge;
          return entry;
        }

        // Try secondary hash location (XOR with high bits)
        int index2 = (index ^ (int)(zobristHash >>> 32)) & mask;
        TranspositionTable.Entry entry2 = table[index2];
        if (entry2.key == zobristHash && entry2.key != 0) {
          entry2.age = currentAge;
          return entry2;
        }

        return null;
      } finally {
        lock.readLock().unlock();
      }
    }

    public void store(long zobristHash, double score, int depth, byte nodeType) {
      int index = (int) (zobristHash & mask);

      ReadWriteLock lock = getLock(zobristHash);
      lock.writeLock().lock();
      try {
        TranspositionTable.Entry entry = table[index];
        int index2 = (index ^ (int)(zobristHash >>> 32)) & mask;
        TranspositionTable.Entry entry2 = table[index2];

        boolean useFirst = shouldReplace(entry, entry2, depth);
        TranspositionTable.Entry target = useFirst ? entry : entry2;

        if (target.key == 0 || target.depth <= depth || target.age < currentAge) {
          target.key = zobristHash;
          target.score = score;
          target.depth = (short) depth;
          target.nodeType = nodeType;
          target.age = currentAge;
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    private boolean shouldReplace(TranspositionTable.Entry entry1, TranspositionTable.Entry entry2, int depth) {
      if (entry1.key == 0) return true;
      if (entry2.key == 0) return false;
      if (entry1.depth < entry2.depth) return true;
      if (entry1.depth > entry2.depth) return false;
      return entry1.age <= entry2.age;
    }
  }
}