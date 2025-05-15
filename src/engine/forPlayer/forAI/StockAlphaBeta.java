package engine.forPlayer.forAI;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ComparisonChain;
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

  /** Countermove table for better move ordering */
  private final Move[][] counterMoves = new Move[64][64];

  /** The maximum number of quiescence searches allowed. */
  private static final int MAX_QUIESCENCE = 300000;

  /** Maximum search depth for data structures. */
  private static final int MAX_SEARCH_DEPTH = 100;

  /** The futility pruning depth used to prune futile moves. */
  private static final int FUTILITY_PRUNING_DEPTH = 2;

  /** A depth threshold that when crossed initiates a LMR (Late Move Reduction) in depth. */
  private static final int LMR_THRESHOLD = 9;

  /** The reduction scale used in a Late Move Reduction. */
  private static final double LMR_SCALE = 0.9;

  /** The delta pruning value used to prune likely unnecessary branches. */
  private static final double DELTA_PRUNING_VALUE = 5;

  /** Margin for razoring pruning */
  private static final double RAZOR_MARGIN = 300;

  /** Aspiration window size */
  private static final double ASPIRATION_WINDOW = 25;

  /** Delta material for pruning in quiescence search */
  private static final double DELTA_MATERIAL = 200;

  /** SEE pruning threshold for quiescence */
  private static final int SEE_PRUNING_THRESHOLD = -20;

  /** Alpha-beta window bounds for aspiration search. */
  private double highestSeenValue = Double.NEGATIVE_INFINITY;
  private double lowestSeenValue = Double.POSITIVE_INFINITY;

  /** Reference to static exchange evaluator */
  private final StaticExchangeEvaluator seeEvaluator = StaticExchangeEvaluator.get();

  /** Enumeration representing different move sorting strategies. */
  private enum MoveSorter {

    /** Standard sorting based on history heuristic. */
    STANDARD {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board,
                            final StockAlphaBeta engine, final int ply) {
        List<Move> sortedMoves = new ArrayList<>(moves);
        Move[][] killers = engine.killerMoves.get();
        Move lastMove = board.getTransitionMove();

        // Pre-calculate all values that will be used in comparisons
        // This ensures consistency in the comparator
        Map<Move, Integer> seeScores = new HashMap<>();
        Map<Move, Boolean> isUndefendedMap = new HashMap<>();
        Map<Move, Boolean> isKillerMap = new HashMap<>();
        Map<Move, Boolean> isCounterMap = new HashMap<>();

        for (Move move : sortedMoves) {
          // Pre-calculate SEE scores and undefended status
          if (move.isAttack()) {
            seeScores.put(move, engine.seeEvaluator.evaluate(board, move));
            // Pre-calculate before comparison to ensure consistency
            Piece attackedPiece = move.getAttackedPiece();
            isUndefendedMap.put(move, attackedPiece != null &&
                    !engine.seeEvaluator.isPieceDefended(attackedPiece, board));
          }

          // Pre-calculate killer move status
          isKillerMap.put(move, move.equals(killers[0][ply]) || move.equals(killers[1][ply]));

          // Pre-calculate counter move status
          boolean isCounter = false;
          if (lastMove != null && lastMove != MoveFactory.getNullMove() &&
                  lastMove.getCurrentCoordinate() >= 0 && lastMove.getDestinationCoordinate() >= 0 &&
                  lastMove.getCurrentCoordinate() < 64 && lastMove.getDestinationCoordinate() < 64) {
            isCounter = move.equals(engine.counterMoves[lastMove.getCurrentCoordinate()][lastMove.getDestinationCoordinate()]);
          }
          isCounterMap.put(move, isCounter);
        }

        sortedMoves.sort((move1, move2) -> {
          // First prioritize captures of undefended pieces (hanging pieces)
          boolean isUndefendedCapture1 = isUndefendedMap.getOrDefault(move1, false);
          boolean isUndefendedCapture2 = isUndefendedMap.getOrDefault(move2, false);

          if (isUndefendedCapture1 != isUndefendedCapture2) {
            return isUndefendedCapture1 ? -1 : 1;
          }

          // Next check if either move is a killer move
          boolean isKiller1 = isKillerMap.getOrDefault(move1, false);
          boolean isKiller2 = isKillerMap.getOrDefault(move2, false);

          if (isKiller1 != isKiller2) {
            return isKiller1 ? -1 : 1;
          }

          // Check if either move is a countermove to the last move
          boolean isCounter1 = isCounterMap.getOrDefault(move1, false);
          boolean isCounter2 = isCounterMap.getOrDefault(move2, false);

          if (isCounter1 != isCounter2) {
            return isCounter1 ? -1 : 1;
          }

          // For captures, use SEE scores
          boolean isCapture1 = move1.isAttack();
          boolean isCapture2 = move2.isAttack();

          if (isCapture1 && isCapture2) {
            // Compare the SEE scores directly
            int score1 = seeScores.getOrDefault(move1, 0);
            int score2 = seeScores.getOrDefault(move2, 0);
            return Integer.compare(score2, score1);
          } else if (isCapture1 != isCapture2) {
            // Fixed logic for comparing captures vs non-captures
            if (isCapture1) {
              int seeValue = seeScores.getOrDefault(move1, 0);
              return seeValue >= 0 ? -1 : 1;
            } else {
              int seeValue = seeScores.getOrDefault(move2, 0);
              return seeValue >= 0 ? 1 : -1;
            }
          }

          // Then use history heuristic with proper bounds checking
          int score1 = 0;
          int score2 = 0;

          // Add bounds checking for move1 and move2
          if (isValidPosition(move1)) {
            score1 = historyHeuristic[move1.getCurrentCoordinate()][move1.getDestinationCoordinate()];
          }

          if (isValidPosition(move2)) {
            score2 = historyHeuristic[move2.getCurrentCoordinate()][move2.getDestinationCoordinate()];
          }

          return Integer.compare(score2, score1);
        });
        return sortedMoves;
      }

      // Helper method to check if a move has valid coordinates for historyHeuristic
      private boolean isValidPosition(Move move) {
        if (move == null) return false;
        int current = move.getCurrentCoordinate();
        int dest = move.getDestinationCoordinate();
        return current >= 0 && current < 64 && dest >= 0 && dest < 64;
      }
    },

    /** Expensive sorting for root moves. */
    EXPENSIVE {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board,
                            final StockAlphaBeta engine, final int ply) {
        List<Move> sortedMoves = new ArrayList<>(moves);

        // Get SEE scores for all captures
        Map<Move, Integer> seeScores = new HashMap<>();
        for (Move move : sortedMoves) {
          if (move.isAttack()) {
            seeScores.put(move, engine.seeEvaluator.evaluate(board, move));
          }
        }

        sortedMoves.sort((move1, move2) -> ComparisonChain.start()
                .compareTrueFirst(BoardUtils.kingThreat(move1), BoardUtils.kingThreat(move2))
                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                // For captures, use SEE instead of MVV-LVA
                .compare(
                        move1.isAttack() ? seeScores.getOrDefault(move1, 0) : -1000,
                        move2.isAttack() ? seeScores.getOrDefault(move2, 0) : -1000
                )
                // Also consider history for non-captures with bounds checking
                .compare(
                        isValidPosition(move1) ?
                                historyHeuristic[move1.getCurrentCoordinate()][move1.getDestinationCoordinate()] : 0,
                        isValidPosition(move2) ?
                                historyHeuristic[move2.getCurrentCoordinate()][move2.getDestinationCoordinate()] : 0
                )
                .result());
        return sortedMoves;
      }

      // Helper method to check if a move has valid coordinates for historyHeuristic
      private boolean isValidPosition(Move move) {
        int current = move.getCurrentCoordinate();
        int dest = move.getDestinationCoordinate();
        return current >= 0 && current < 64 && dest >= 0 && dest < 64;
      }
    };

    abstract Collection<Move> sort(Collection<Move> moves, final Board board,
                                   final StockAlphaBeta engine, final int ply);
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

    // Reset evaluation cache stats for new search
    EvaluationCache.get().clear();

    try {
      // For each iterative deepening depth
      for (int currentDepth = 1; currentDepth <= maxDepth && !searchStopped; currentDepth++) {
        // Use aspiration windows for deeper searches
        if (currentDepth >= 3) {
          bestMove = searchRootAspirationWindow(board, currentDepth, bestMove);
        } else {
          bestMove = searchRootParallel(board, currentDepth, -Double.MAX_VALUE, Double.MAX_VALUE);
        }

        // Update history heuristic for the best move
        updateHistoryHeuristic(bestMove, currentDepth);

        // Output statistics
        final long evaluatedPositions = this.boardsEvaluated.get();
        final long executionTime = System.currentTimeMillis() - startTime;
        final String result = String.format(
                "%s | depth = %d | boards evaluated = %d | time = %.2f sec | nps = %.2f M | %s",
                bestMove, currentDepth, evaluatedPositions,
                executionTime / 1000.0,
                (evaluatedPositions / (executionTime / 1000.0)) / 1_000_000.0,
                EvaluationCache.get().getStats());

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
   * Gets a cached evaluation or computes a new one if not in cache.
   */
  private double getCachedEvaluation(Board board, int depth) {
    // Try to get from cache first
    Double cachedScore = EvaluationCache.get().probe(board, depth);
    if (cachedScore != null) {
      return cachedScore;
    }

    // Not in cache, calculate and store
    double score = this.evaluator.evaluate(board, depth);
    EvaluationCache.get().store(board, depth, score);
    return score;
  }

  /**
   * Implements aspiration window search for deeper iterations
   */
  private Move searchRootAspirationWindow(final Board board, final int depth, final Move previousBestMove) {
    double alpha = depth > 3 ? highestSeenValue - ASPIRATION_WINDOW : -Double.MAX_VALUE;
    double beta = depth > 3 ? lowestSeenValue + ASPIRATION_WINDOW : Double.MAX_VALUE;
    Move bestMove;

    // Try with narrow window first
    bestMove = searchRootParallel(board, depth, alpha, beta);

    // If window fails, try with full window
    if ((board.currentPlayer().getAlliance().isWhite() && highestSeenValue <= alpha) ||
            (!board.currentPlayer().getAlliance().isWhite() && lowestSeenValue >= beta)) {
      System.out.println("Aspiration window failed, re-searching with full window");
      bestMove = searchRootParallel(board, depth, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    return bestMove;
  }

  /**
   * Searches the root position in parallel using Lazy SMP.
   *
   * @param board The root board position
   * @param depth The current search depth
   * @param alpha The alpha bound
   * @param beta The beta bound
   * @return The best move found
   */
  private Move searchRootParallel(final Board board, final int depth, double alpha, double beta) {
    // Sort moves to try most promising first
    final List<Move> allMoves = new ArrayList<>(
            MoveSorter.EXPENSIVE.sort(board.currentPlayer().getLegalMoves(), board, this, 0));

    if (allMoves.isEmpty()) {
      return MoveFactory.getNullMove();
    }

    // For tracking the best move found
    final AtomicReference<Move> globalBestMove = new AtomicReference<>(allMoves.get(0));
    final AtomicReference<Double> globalBestScore = new AtomicReference<>(
            board.currentPlayer().getAlliance().isWhite() ? alpha : beta);

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
            searchFirstMove(board, allMoves.get(0), depth, globalBestMove, globalBestScore, alpha, beta);
            firstMoveLatch.countDown(); // Signal other threads to start
          } else {
            // Other threads wait for first move to be searched
            firstMoveLatch.await();
          }

          // Then all threads take moves from the shared queue
          int idx;
          while ((idx = moveIndex.getAndIncrement()) < allMoves.size() && !searchStopped) {
            if (idx == 0) continue; // Skip first move (already searched)

            // Slightly vary search depth for better parallel efficiency
            int threadDepth = depth - (id % 2);
            if (threadDepth < 1) threadDepth = 1;

            final Move move = allMoves.get(idx);
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

            if (moveTransition.moveStatus().isDone()) {
              searchMove(board, move, moveTransition.toBoard(), threadDepth, globalBestMove, globalBestScore, alpha, beta);
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
                               AtomicReference<Move> bestMove, AtomicReference<Double> bestScore,
                               double alpha, double beta) {
    final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
    if (moveTransition.moveStatus().isDone()) {
      searchMove(board, move, moveTransition.toBoard(), depth, bestMove, bestScore, alpha, beta);
    }
  }

  /**
   * Search a move at the root and update best move if better is found.
   */
  private void searchMove(Board board, Move move, Board toBoard, int depth,
                          AtomicReference<Move> bestMove, AtomicReference<Double> bestScore,
                          double alpha, double beta) {
    double score;
    if (board.currentPlayer().getAlliance().isWhite()) {
      // White is maximizing
      score = min(toBoard, depth - 1, alpha, beta, 1);
      if (score > bestScore.get()) {
        synchronized (bestScore) {
          if (score > bestScore.get()) {
            bestScore.set(score);
            bestMove.set(move);

            // Record as a countermove if possible
            recordCounterMove(board, move);
          }
        }
      }
    } else {
      // Black is minimizing
      score = max(toBoard, depth - 1, alpha, beta, 1);
      if (score < bestScore.get()) {
        synchronized (bestScore) {
          if (score < bestScore.get()) {
            bestScore.set(score);
            bestMove.set(move);

            // Record as a countermove if possible
            recordCounterMove(board, move);
          }
        }
      }
    }
  }

  /**
   * Records a move as a countermove to the last opponent move
   */
  private void recordCounterMove(Board board, Move move) {
    Move lastMove = board.getTransitionMove();
    if (lastMove != null && lastMove != MoveFactory.getNullMove()) {
      counterMoves[lastMove.getCurrentCoordinate()][lastMove.getDestinationCoordinate()] = move;
    }
  }

  /**
   * Calculates the depth for the quiescence search based on the board state and current depth.
   */
  private int calculateQuiescenceDepth(final Board toBoard, final int depth) {
    SearchStats stats = threadStats.get();

    // For final ply, consider quiescence search if it's an active position
    if (depth == 1 && stats.quiescenceCount < MAX_QUIESCENCE) {
      int activityMeasure = 0;

      // Always do quiescence in check
      if (toBoard.currentPlayer().isInCheck()) {
        return 1;
      }

      // Do quiescence after captures or checks
      for (final Move move : BoardUtils.lastNMoves(toBoard, 2)) {
        if (move.isAttack()) {
          activityMeasure += 1;
        }
      }

      if (activityMeasure >= 1) {
        stats.quiescenceCount++;
        return 1;
      }
    }
    return depth - 1;
  }

  /**
   * Implements the max portion of the alpha-beta search algorithm.
   */
  private double max(final Board board, int depth, double alpha, double beta, int ply) {
    SearchStats stats = threadStats.get();

    // Terminal node check
    if (depth <= 0 || BoardUtils.isEndOfGame(board) || searchStopped) {
      return depth <= 0 ? quiescenceSearch(board, alpha, beta, ply, true) :
              getCachedEvaluation(board, depth);
    }

    // Transposition table lookup
    long zobristHash = board.getZobristHash();
    TranspositionTable.Entry entry = transpositionTable.get(zobristHash);
    if (entry != null && entry.depth >= depth) {
      if (entry.nodeType == TranspositionTable.EXACT) {
        return entry.score;
      } else if (entry.nodeType == TranspositionTable.LOWERBOUND) {
        alpha = Math.max(alpha, entry.score);
      } else if (entry.nodeType == TranspositionTable.UPPERBOUND) {
        beta = Math.min(beta, entry.score);
      }
      if (alpha >= beta) {
        return entry.score;
      }
    }

    // Razoring
    if (depth == 1) {
      double eval = getCachedEvaluation(board, depth);
      if (eval + RAZOR_MARGIN < alpha) {
        return quiescenceSearch(board, alpha, beta, ply, true);
      }
    }

    // Futility pruning
    if (depth < FUTILITY_PRUNING_DEPTH && !board.currentPlayer().isInCheck()) {
      double eval = getCachedEvaluation(board, depth);
      if (eval >= beta + (depth * 100)) {
        return eval;
      }
    }

    // Internal Iterative Deepening (IID)
    Move ttMove = null;
    if (entry == null && depth >= 4) {
      max(board, depth - 2, alpha, beta, ply);
      entry = transpositionTable.get(zobristHash);
      if (entry != null) {
        // We don't have explicit ttMove but can get info from entry
        ttMove = findMoveFromHash(board, entry);
      }
    }

    // Null Move Pruning
    if (depth >= 3 && !board.currentPlayer().isInCheck() && hasNonPawnMaterial(board.currentPlayer())) {
      // Make a null move
      Board nullMoveBoard = makeNullMove(board);
      int R = 2 + depth / 6; // Dynamic reduction
      double nullMoveScore = min(nullMoveBoard, depth - 1 - R, alpha, beta, ply + 1);

      if (nullMoveScore >= beta) {
        // Verification search for deep positions
        if (depth >= 6) {
          double verificationScore = min(nullMoveBoard, depth - 4, alpha, beta, ply + 1);
          if (verificationScore >= beta) {
            return beta;
          }
        } else {
          return beta; // Regular null move pruning cutoff
        }
      }
    }

    double currentAlpha = alpha;
    boolean firstMove = true;
    Move bestFoundMove = null;
    Move[][] killers = killerMoves.get();
    int movesSearched = 0;

    // Sort moves for better cutoffs
    Collection<Move> sortedMoves = MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board, this, ply);

    // If we have a transposition table hit, try that move first
    if (ttMove != null) {
      // Ensure ttMove is at the front of the search
      List<Move> reorderedMoves = new ArrayList<>();
      for (Move move : sortedMoves) {
        if (move.equals(ttMove)) {
          reorderedMoves.add(0, move);
        } else {
          reorderedMoves.add(move);
        }
      }
      sortedMoves = reorderedMoves;
    }

    for (final Move move : sortedMoves) {
      // Skip negative SEE captures early in the search
      if (move.isAttack() && depth < 3 && movesSearched > 2) {
        int seeScore = seeEvaluator.evaluate(board, move);
        if (seeScore < SEE_PRUNING_THRESHOLD) {
          continue;
        }
      }

      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        double currentValue;

        // Check extensions
        int newDepth = depth - 1;
        if (toBoard.currentPlayer().isInCheck()) {
          newDepth++; // Extend search when in check
        }

        // Principal Variation Search
        if (firstMove) {
          currentValue = min(toBoard, newDepth, currentAlpha, beta, ply + 1);
        } else {
          // Try reduced depth for non-PV nodes
          // Late Move Reduction
          int reduction = 0;
          if (depth >= 3 && movesSearched >= 4 && !move.isAttack() && !board.currentPlayer().isInCheck()) {
            reduction = 1 + (movesSearched / 6);
            if (reduction > 3) reduction = 3;
          }

          // Reduced search with null window
          currentValue = min(toBoard, newDepth - reduction, currentAlpha, currentAlpha + 0.1, ply + 1);

          // Re-search if promising and in window
          if (currentValue > currentAlpha && currentValue < beta) {
            currentValue = min(toBoard, newDepth, currentAlpha, beta, ply + 1);
          }
        }

        if (currentValue > currentAlpha) {
          currentAlpha = currentValue;
          bestFoundMove = move;

          // Update killer moves and counter moves
          if (!move.isAttack()) {
            if (!move.equals(killers[0][ply])) {
              killers[1][ply] = killers[0][ply];
              killers[0][ply] = move;
            }
          }

          // Record counter move
          recordCounterMove(board, move);

          // Beta cutoff
          if (currentAlpha >= beta) {
            // Update history for good beta cutoffs
            if (!move.isAttack()) {
              historyHeuristic[move.getCurrentCoordinate()][move.getDestinationCoordinate()] += depth * depth;
            }

            // Store in transposition table
            transpositionTable.store(zobristHash, beta, depth, TranspositionTable.LOWERBOUND);
            return beta;
          }
        }

        firstMove = false;
        movesSearched++;
      }
    }

    // Store position in transposition table
    byte nodeType = TranspositionTable.EXACT;
    if (currentAlpha <= alpha) {
      nodeType = TranspositionTable.UPPERBOUND;
    } else if (currentAlpha >= beta) {
      nodeType = TranspositionTable.LOWERBOUND;
    }
    transpositionTable.store(zobristHash, currentAlpha, depth, nodeType);

    return currentAlpha;
  }

  /**
   * Implements the min portion of the alpha-beta search algorithm.
   */
  private double min(final Board board, int depth, double alpha, double beta, int ply) {
    SearchStats stats = threadStats.get();

    // Terminal node check
    if (depth <= 0 || BoardUtils.isEndOfGame(board) || searchStopped) {
      return depth <= 0 ? quiescenceSearch(board, alpha, beta, ply, false) :
              getCachedEvaluation(board, depth);
    }

    // Transposition table lookup
    long zobristHash = board.getZobristHash();
    TranspositionTable.Entry entry = transpositionTable.get(zobristHash);
    if (entry != null && entry.depth >= depth) {
      if (entry.nodeType == TranspositionTable.EXACT) {
        return entry.score;
      } else if (entry.nodeType == TranspositionTable.LOWERBOUND) {
        alpha = Math.max(alpha, entry.score);
      } else if (entry.nodeType == TranspositionTable.UPPERBOUND) {
        beta = Math.min(beta, entry.score);
      }
      if (alpha >= beta) {
        return entry.score;
      }
    }

    // Razoring
    if (depth == 1) {
      double eval = getCachedEvaluation(board, depth);
      if (eval - RAZOR_MARGIN > beta) {
        return quiescenceSearch(board, alpha, beta, ply, false);
      }
    }

    // Futility pruning
    if (depth < FUTILITY_PRUNING_DEPTH && !board.currentPlayer().isInCheck()) {
      double eval = getCachedEvaluation(board, depth);
      if (eval <= alpha - (depth * 100)) {
        return eval;
      }
    }

    // Internal Iterative Deepening (IID)
    Move ttMove = null;
    if (entry == null && depth >= 4) {
      min(board, depth - 2, alpha, beta, ply);
      entry = transpositionTable.get(zobristHash);
      if (entry != null) {
        ttMove = findMoveFromHash(board, entry);
      }
    }

    // Null Move Pruning
    if (depth >= 3 && !board.currentPlayer().isInCheck() && hasNonPawnMaterial(board.currentPlayer())) {
      Board nullMoveBoard = makeNullMove(board);
      int R = 2 + depth / 6; // Dynamic reduction
      double nullMoveScore = max(nullMoveBoard, depth - 1 - R, alpha, beta, ply + 1);

      if (nullMoveScore <= alpha) {
        // Verification search for deep positions
        if (depth >= 6) {
          double verificationScore = max(nullMoveBoard, depth - 4, alpha, beta, ply + 1);
          if (verificationScore <= alpha) {
            return alpha;
          }
        } else {
          return alpha; // Regular null move pruning cutoff
        }
      }
    }

    double currentBeta = beta;
    boolean firstMove = true;
    Move bestFoundMove = null;
    Move[][] killers = killerMoves.get();
    int movesSearched = 0;

    // Sort moves for better cutoffs
    Collection<Move> sortedMoves = MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board, this, ply);

    // If we have a transposition table hit, try that move first
    if (ttMove != null) {
      // Ensure ttMove is at the front of the search
      List<Move> reorderedMoves = new ArrayList<>();
      for (Move move : sortedMoves) {
        if (move.equals(ttMove)) {
          reorderedMoves.add(0, move);
        } else {
          reorderedMoves.add(move);
        }
      }
      sortedMoves = reorderedMoves;
    }

    for (final Move move : sortedMoves) {
      // Skip negative SEE captures early in the search
      if (move.isAttack() && depth < 3 && movesSearched > 2) {
        int seeScore = seeEvaluator.evaluate(board, move);
        if (seeScore < SEE_PRUNING_THRESHOLD) {
          continue;
        }
      }

      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        double currentValue;

        // Check extensions
        int newDepth = depth - 1;
        if (toBoard.currentPlayer().isInCheck()) {
          newDepth++; // Extend search when in check
        }

        // Principal Variation Search
        if (firstMove) {
          currentValue = max(toBoard, newDepth, alpha, currentBeta, ply + 1);
        } else {
          // Try reduced depth for non-PV nodes
          // Late Move Reduction
          int reduction = 0;
          if (depth >= 3 && movesSearched >= 4 && !move.isAttack() && !board.currentPlayer().isInCheck()) {
            reduction = 1 + (movesSearched / 6);
            if (reduction > 3) reduction = 3;
          }

          // Reduced search with null window
          currentValue = max(toBoard, newDepth - reduction, currentBeta - 0.1, currentBeta, ply + 1);

          // Re-search if promising and in window
          if (currentValue < currentBeta && currentValue > alpha) {
            currentValue = max(toBoard, newDepth, alpha, currentBeta, ply + 1);
          }
        }

        if (currentValue < currentBeta) {
          currentBeta = currentValue;
          bestFoundMove = move;

          // Update killer moves
          if (!move.isAttack()) {
            if (!move.equals(killers[0][ply])) {
              killers[1][ply] = killers[0][ply];
              killers[0][ply] = move;
            }
          }

          // Record counter move
          recordCounterMove(board, move);

          // Alpha cutoff
          if (currentBeta <= alpha) {
            // Update history for good alpha cutoffs
            if (!move.isAttack()) {
              historyHeuristic[move.getCurrentCoordinate()][move.getDestinationCoordinate()] += depth * depth;
            }

            // Store in transposition table
            transpositionTable.store(zobristHash, alpha, depth, TranspositionTable.UPPERBOUND);
            return alpha;
          }
        }

        firstMove = false;
        movesSearched++;
      }
    }

    // Store position in transposition table
    byte nodeType = TranspositionTable.EXACT;
    if (currentBeta <= alpha) {
      nodeType = TranspositionTable.UPPERBOUND;
    } else if (currentBeta >= beta) {
      nodeType = TranspositionTable.LOWERBOUND;
    }
    transpositionTable.store(zobristHash, currentBeta, depth, nodeType);

    return currentBeta;
  }

  /**
   * Dedicated quiescence search function to handle capture sequences
   */
  private double quiescenceSearch(Board board, double alpha, double beta, int ply, boolean maximizing) {
    SearchStats stats = threadStats.get();
    stats.boardsEvaluated++;

    // Handle terminal positions
    if (BoardUtils.isEndOfGame(board) || searchStopped) {
      return getCachedEvaluation(board, 0);
    }

    // Stand-pat score
    double standPat = getCachedEvaluation(board, 0);

    // Beta/alpha cutoff for maximizing/minimizing player
    if (maximizing) {
      if (standPat >= beta) return beta;
      if (standPat > alpha) alpha = standPat;
    } else {
      if (standPat <= alpha) return alpha;
      if (standPat < beta) beta = standPat;
    }

    // Delta pruning - if even the best capture won't improve alpha, return
    if (maximizing && standPat < alpha - DELTA_MATERIAL) return alpha;
    if (!maximizing && standPat > beta + DELTA_MATERIAL) return beta;

    // Check extensions in quiescence
    if (board.currentPlayer().isInCheck()) {
      // Consider all moves in check
      return maximizing ?
              max(board, 1, alpha, beta, ply + 1) :
              min(board, 1, alpha, beta, ply + 1);
    }

    // Get only captures for quiescence
    List<Move> captures = board.currentPlayer().getLegalMoves().stream()
            .filter(Move::isAttack)
            .sorted((m1, m2) -> {
              // Pre-compute values to ensure consistency
              boolean isM1Undefended = m1.isAttack() && m1.getAttackedPiece() != null &&
                      !seeEvaluator.isPieceDefended(m1.getAttackedPiece(), board);
              boolean isM2Undefended = m2.isAttack() && m2.getAttackedPiece() != null &&
                      !seeEvaluator.isPieceDefended(m2.getAttackedPiece(), board);

              // First compare undefended status
              if (isM1Undefended != isM2Undefended) {
                return isM1Undefended ? -1 : 1;
              }

              // Then use SEE for normal ordering
              int see1 = seeEvaluator.evaluate(board, m1);
              int see2 = seeEvaluator.evaluate(board, m2);
              return Integer.compare(see2, see1); // Best captures first
            })
            .toList();

    // Search captures
    for (Move move : captures) {
      // SEE pruning - but allow negative SEE captures that lead to check
      int seeScore = seeEvaluator.evaluate(board, move);

      // Modified SEE pruning - allow some negative captures
      // 1. Allow captures of undefended pieces regardless of SEE
      boolean isUndefendedCapture = !seeEvaluator.isPieceDefended(move.getAttackedPiece(), board);

      // 2. Skip only very bad captures that aren't checkers
      if (seeScore < SEE_PRUNING_THRESHOLD && !isUndefendedCapture) {
        // Make the move to check if it gives check
        final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
        if (moveTransition.moveStatus().isDone()) {
          boolean givesCheck = moveTransition.toBoard().currentPlayer().isInCheck();
          if (!givesCheck) {
            continue; // Skip only if not giving check
          }
        } else {
          continue; // Skip if move is illegal
        }
      }

      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        double score;
        if (maximizing) {
          score = quiescenceSearch(moveTransition.toBoard(), alpha, beta, ply + 1, false);
          if (score > alpha) alpha = score;
        } else {
          score = quiescenceSearch(moveTransition.toBoard(), alpha, beta, ply + 1, true);
          if (score < beta) beta = score;
        }

        if (alpha >= beta) {
          return maximizing ? beta : alpha;
        }
      }
    }

    return maximizing ? alpha : beta;
  }

  /**
   * Try to find a specific move from transposition table entry
   */
  private Move findMoveFromHash(Board board, TranspositionTable.Entry entry) {
    // This is a simplistic approach - a real TT would store the best move explicitly
    for (Move move : board.currentPlayer().getLegalMoves()) {
      MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        Board newBoard = moveTransition.toBoard();
        if (newBoard.getZobristHash() == entry.key) {
          return move;
        }
      }
    }
    return null;
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
   */
  private void updateHistoryHeuristic(Move move, int depth) {
    if (move != null && move != MoveFactory.getNullMove()) {
      historyHeuristic[move.getCurrentCoordinate()][move.getDestinationCoordinate()] += depth * depth;
    }
  }

  /**
   * Determines the game state for the current position to select appropriate evaluator.
   */
  @VisibleForTesting
  private BoardEvaluator determineGameState(final Board board) {
    return GameStateDetector.get().determineEvaluator(board);
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

        boolean useFirst = shouldReplace(entry, entry2, depth, nodeType);
        TranspositionTable.Entry target = useFirst ? entry : entry2;

        target.key = zobristHash;
        target.score = score;
        target.depth = (short) depth;
        target.nodeType = nodeType;
        target.age = currentAge;
      } finally {
        lock.writeLock().unlock();
      }
    }

    private boolean shouldReplace(TranspositionTable.Entry entry1,
                                  TranspositionTable.Entry entry2,
                                  int depth, byte nodeType) {
      // Always replace if entry is empty
      if (entry1.key == 0) return true;
      if (entry2.key == 0) return false;

      // Prefer deeper searches
      if (entry1.depth < entry2.depth) return true;
      if (entry1.depth > entry2.depth) return false;

      // Prefer exact values over bounds
      boolean isExact1 = entry1.nodeType == TranspositionTable.EXACT;
      boolean isExact2 = entry2.nodeType == TranspositionTable.EXACT;
      boolean newIsExact = nodeType == TranspositionTable.EXACT;

      if (!isExact1 && isExact2) return false;
      if (isExact1 && !isExact2) return true;
      if (!isExact1 && newIsExact) return true;

      // If same depth and type, prefer current age
      return entry1.age <= entry2.age;
    }
  }
}