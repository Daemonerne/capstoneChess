package engine.forPlayer.forAI;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import engine.forBoard.Board;
import engine.forBoard.BoardUtils;
import engine.forBoard.Move;
import engine.forBoard.MoveTransition;
import engine.forPlayer.Player;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import static engine.forBoard.BoardUtils.mvvlva;
import static engine.forBoard.Move.MoveFactory;

/**
 * The `StockAlphaBeta` class represents a chess engine that employs the alpha-beta search algorithm. It is
 * enhanced with various strategies to determine the optimal move in a given chess position. This engine
 * evaluates board positions, explores multiple moves ahead, and utilizes move-sorting strategies for
 * improved search efficiency. The class extends `Observable` to allow observers to be notified about search
 * results. The alpha-beta algorithm is parallelized using `ForkJoinPool`, a framework in Java that provides
 * support for parallel programming. ForkJoinPool recursively breaks down tasks into smaller subtasks,
 * allowing concurrent execution of different threads and enhancing the alpha-beta search performance.
 * Additionally, the engine incorporates quiescence search to handle tactical positions.
 * <br><br>
 * Alpha-beta search is an optimization of the minimax algorithm, a decision rule for minimizing potential
 * losses in a worst-case scenario. It prunes branches of the search tree that are unlikely to influence the
 * final decision, reducing the number of nodes evaluated. The class includes a constructor for initializing
 * the engine with a specified search depth, along with methods for executing the alpha-beta search, calculating
 * quiescence search depth, formatting evaluation scores, and more.
 * <br><br>
 * Iterative Deepening has been implemented to gradually increase the search depth until a maximum depth is reached.
 * <br><br>
 * The engine incorporates the use of a transposition table, a map for storing and retrieving previously evaluated positions to avoid
 * redundant computations. The transposition table is used to store entries with information about the score, depth, and node type,
 * allowing the engine to skip re-evaluating positions it has encountered before.
 * <br><br>
 * Delta pruning is applied in the search algorithm to prune likely unnecessary branches. When the difference between the current
 * highest and lowest values in the search exceeds a specified threshold (DeltaPruningValue), the search can be pruned further,
 * avoiding unnecessary computation.
 * <br><br>
 * Aspiration windows are employed to narrow the search space by considering only moves within a certain range of scores. The window
 * is dynamically adjusted based on the results of previous searches, allowing the algorithm to focus on the most promising moves.
 * <br><br>
 * Late Move Reduction (LMR) is a technique used to reduce the depth of the search for moves that occur later in the move ordering.
 * This reduction in depth helps improve the efficiency of the search, especially in positions where the move order can significantly
 * impact the evaluation.
 * <br><br>
 * The class utilizes the `StandardBoardEvaluator` for assessing board positions and generating evaluation scores.
 * Move sorting strategies, such as `STANDARD` and `EXPENSIVE`, are implemented for ordering moves based on
 * certain criteria. Transposition tables are used to store and retrieve previously evaluated positions to
 * avoid redundant computations.
 * <br><br>
 * A known bug occurs at very low depths (e.g., depth 1), where rapid parallelism may lead to an
 * `ExecutionException` caused by a `StackOverflowError`. This issue minimally impacts engine functionality
 * at such depths, as it is not practical to use an engine at depth 1. Setting the depth to 2 or 3 may provide
 * more manageable testing conditions, though moves are executed rapidly, posing challenges for performance
 * evaluation and tuning.
 *
 * @author Aaron Ho
 */
public class StockAlphaBeta extends Observable implements MoveStrategy {

  /*** The evaluator used to assess board positions and generate evaluation scores. */
  private final BoardEvaluator evaluator;

  /*** The maximum depth for iterative deepening. */
  private final int maxDepth;

  /*** The count of boards evaluated during the search. */
  private long boardsEvaluated;

  /*** The count of quiescence searches performed during the algorithm execution. */
  private int quiescenceCount;

  /*** A transposition table that tracks the scores of previously evaluated moves. */
  private final Map<Long, TranspositionTableEntry> transpositionTable = new HashMap<>();

  /*** The maximum number of quiescence searches allowed. The higher this value is, the longer the search will take,
   * but theoretically will increase the strength of the engine's result. */
  private static final int MaxQuiescence = 30000;

  /*** The futility pruning depth used to prune futile moves. */
  private static final int FutilityPruningDepth = 2;

  /*** A depth threshold that when crossed initiates a LMR (Late Move Reduction) in depth. */
  private static final int LMRThreshold = 6;

  /*** The reduction scale used in a Late Move Reduction. */
  private static final double LMRScale = 0.9;

  /*** The delta pruning value used to prune likely unnecessary branches. */
  private static final double DeltaPruningValue = 5;

  /*** Enumeration representing different move sorting strategies. */
  private enum MoveSorter {

    /*** An enum representing the standard sorting method. */
    STANDARD {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board) {
        return Ordering.from((Comparator<Move>) (move1, move2) -> ComparisonChain.start()
                .compareTrueFirst(BoardUtils.kingThreat(move1), BoardUtils.kingThreat(move2))
                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                .compare(mvvlva(move2), mvvlva(move1))
                .result()).immutableSortedCopy(moves);
      }
    },
    /*** An enum representing the expensive sorting method. */
    EXPENSIVE {
      @Override
      Collection<Move> sort(final Collection<Move> moves, final Board board) {
        return Ordering.from((Comparator<Move>) (move1, move2) -> ComparisonChain.start()
                .compareTrueFirst(BoardUtils.kingThreat(move1), BoardUtils.kingThreat(move2))
                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                .compare(mvvlva(move2), mvvlva(move1))
                .result()).immutableSortedCopy(moves);
      }
    };

    abstract Collection <Move> sort(Collection <Move> moves, final Board board);
  }

  /**
   * Constructs a StockAlphaBeta instance with the specified search depth.
   *
   * @param maxDepth The maximum depth for iterative deepening.
   */
  public StockAlphaBeta(final int maxDepth) {
    this.evaluator = StandardBoardEvaluator.get();
    this.maxDepth = maxDepth;
    this.boardsEvaluated = 0;
    this.quiescenceCount = 0;
  }

  /**
   * Returns a string representation of the StockAlphaBeta instance.
   *
   * @return A string representing the instance.
   */
  @Override
  public String toString() {
    return "StockAB";
  }

  /**
   * Calculates the depth for the quiescence search based on the board state and current depth.
   *
   * @param toBoard The resulting board after a move.
   * @param depth   The current search depth.
   * @return        The adjusted depth for quiescence search.
   */
  private int calculateQuiescenceDepth(final Board toBoard, final int depth) {
    if (depth == 1 && this.quiescenceCount < MaxQuiescence) {
      int activityMeasure = 0;
      if (toBoard.currentPlayer().isInCheck()) {
        activityMeasure += 1;
      } for (final Move move: BoardUtils.lastNMoves(toBoard, 2)) {
        if (move.isAttack()) {
          activityMeasure += 1;
        }
      } if (activityMeasure >= 2) {
        this.quiescenceCount++;
        return 2;
      }
    }
    return depth - 1;
  }

  /**
   * Executes the alpha-beta search algorithm with Iterative Deepening to find the best move for the current player.
   *
   * @param board The current chess board.
   * @return      The best move determined by the algorithm.
   */
  @Override
  public Move execute(final Board board) {
    final long startTime = System.currentTimeMillis();
    Move bestMove = MoveFactory.getNullMove();
    double highestSeenValue = Integer.MIN_VALUE;
    double lowestSeenValue = Integer.MAX_VALUE;
    ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    for (int currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
      final double aspirationWindow = calculateAspirationWindow(highestSeenValue, lowestSeenValue);
      RecursiveTask <Move> task = new ParallelSearchTask(board, this, highestSeenValue, lowestSeenValue, currentDepth);
      bestMove = forkJoinPool.invoke(task);
      highestSeenValue = Math.max(highestSeenValue - aspirationWindow, Integer.MIN_VALUE);
      lowestSeenValue = Math.min(lowestSeenValue + aspirationWindow, Integer.MAX_VALUE);
      final long executionTime = System.currentTimeMillis() - startTime;
      final String result = bestMove +
              " | depth = " + currentDepth +
              " | boards evaluated = " + this.boardsEvaluated +
              " | time taken = " + (executionTime / 1000) + " sec or " + (executionTime / 60000) + " min" +
              " | rate = " + ((double)(1000000 * this.boardsEvaluated) / (1000 * executionTime));
      System.out.println(result);
      setChanged();
      notifyObservers(result);
    }
    return bestMove;
  }

  /**
   * Calculates the dynamic aspiration window based on the previous search results.
   *
   * @param highestSeenValue The highest value seen in the search.
   * @param lowestSeenValue  The lowest value seen in the search.
   * @return                 The dynamic aspiration window.
   */
  private double calculateAspirationWindow(double highestSeenValue, double lowestSeenValue) {
    return Math.min(Math.abs(highestSeenValue - lowestSeenValue) * 4, 13);
  }

  /**
   * Implements the max portion of the alpha-beta search algorithm.
   *
   * @param board   The current chess board.
   * @param depth   The current search depth.
   * @param highest The highest value seen in the search.
   * @param lowest  The lowest value seen in the search.
   * @return        The score value.
   */
  private double max(final Board board, int depth,
                     double highest, double lowest) {

    if (depth <= 0 || BoardUtils.isEndOfGame(board)) {
      this.boardsEvaluated++;
      return this.evaluator.evaluate(board, depth);
    }
    TranspositionTableEntry entry = transpositionTable.get((long) board.hashCode());
    if (entry != null && entry.depth() >= depth) {
      if (entry.nodeType() == TranspositionTableEntry.NodeType.EXACT) {
        return entry.score();
      } else if (entry.nodeType() == TranspositionTableEntry.NodeType.LOWERBOUND) {
        lowest = Math.max(lowest, entry.score());
      } else if (entry.nodeType() == TranspositionTableEntry.NodeType.UPPERBOUND) {
        highest = Math.min(highest, entry.score());
      } if (lowest >= highest) {
        return entry.score();
      }
    } if (depth < FutilityPruningDepth) {
      double futilityValue = this.evaluator.evaluate(board, depth);
      if (futilityValue >= highest) {
        return futilityValue;
      }
    } if (depth <= LMRThreshold) depth = (int) (depth * LMRScale);
    double currentHighest = highest;
    for (final Move move: MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board)) {
      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        currentHighest = Math.max(currentHighest, min(toBoard, calculateQuiescenceDepth(toBoard, depth), currentHighest, lowest));
        if (currentHighest >= lowest) {
          if (currentHighest >= lowest + DeltaPruningValue) {
            storeTranspositionTableEntry(board, currentHighest, depth, TranspositionTableEntry.NodeType.LOWERBOUND);
            return lowest;
          }
        }
      }
    }
    storeTranspositionTableEntry(board, currentHighest, depth, TranspositionTableEntry.NodeType.EXACT);
    return currentHighest;
  }

  /**
   * Implements the min portion of the alpha-beta search algorithm.
   *
   * @param board   The current chess board.
   * @param depth   The current search depth.
   * @param highest The highest value seen in the search.
   * @param lowest  The lowest value seen in the search.
   * @return        The score value.
   */
  private double min(final Board board,
                     int depth,
                     double highest,
                     double lowest) {

    if (depth <= 0 || BoardUtils.isEndOfGame(board)) {
      this.boardsEvaluated++;
      return this.evaluator.evaluate(board, depth);
    }

    TranspositionTableEntry entry = transpositionTable.get((long) board.hashCode());
    if (entry != null && entry.depth() >= depth) {
      if (entry.nodeType() == TranspositionTableEntry.NodeType.EXACT) {
        return entry.score();
      } else if (entry.nodeType() == TranspositionTableEntry.NodeType.LOWERBOUND) {
        lowest = Math.max(lowest, entry.score());
      } else if (entry.nodeType() == TranspositionTableEntry.NodeType.UPPERBOUND) {
        highest = Math.min(highest, entry.score());
      } if (lowest >= highest) return entry.score();
    } if (depth < FutilityPruningDepth) {
      double futilityValue = this.evaluator.evaluate(board, depth);
      if (futilityValue <= lowest) {
        return futilityValue;
      }
    } if (depth <= LMRThreshold) depth = (int) (depth * LMRScale);
    double currentLowest = lowest;
    for (final Move move: MoveSorter.STANDARD.sort(board.currentPlayer().getLegalMoves(), board)) {
      final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
      if (moveTransition.moveStatus().isDone()) {
        final Board toBoard = moveTransition.toBoard();
        currentLowest = Math.min(currentLowest, max(toBoard, calculateQuiescenceDepth(toBoard, depth), highest, currentLowest));
        if (currentLowest <= highest) {
          if (currentLowest <= highest - DeltaPruningValue) {
            storeTranspositionTableEntry(board, currentLowest, depth, TranspositionTableEntry.NodeType.UPPERBOUND);
            return highest;
          }
        }
      }
    } storeTranspositionTableEntry(board, currentLowest, depth, TranspositionTableEntry.NodeType.EXACT);
    return currentLowest;
  }

  /**
   * Stores a move that was made into the transposition table.
   *
   * @param board    The board to be recorded
   * @param score    The score to be recorded
   * @param depth    The depth to be recorded
   * @param nodeType The NodeType to be recorded
   */
  private void storeTranspositionTableEntry(Board board, double score, int depth, TranspositionTableEntry.NodeType nodeType) {
    transpositionTable.put((long) board.hashCode(), new TranspositionTableEntry(score, depth, nodeType));
  }

  private record TranspositionTableEntry(double score, int depth, StockAlphaBeta.TranspositionTableEntry.NodeType nodeType) {
    public enum NodeType {
      EXACT, LOWERBOUND, UPPERBOUND
    }
  }

  /*** A RecursiveTask implementation for parallel alpha-beta search. */
  private static class ParallelSearchTask extends RecursiveTask <Move> {

    /*** The current chess board for the search. */
    private final Board board;

    /*** The current StockAlphaBeta for the search. */
    private final StockAlphaBeta stockAlphaBeta;

    /*** The current highest value seen in the search. */
    private double highest;

    /*** The current lowest value seen in the search. */
    private double lowest;

    /*** The depth for this search. */
    private final int depth;

    /**
     * The constructor for this recursive task.
     *
     * @param board          The current chess board for this search.
     * @param stockAlphaBeta The current StockAlphaBeta for this search.
     * @param highest        The highest value seen in the search.
     * @param lowest         The lowest value seen in the search.
     * @param depth          The current search depth for this search.
     */
    public ParallelSearchTask(Board board, StockAlphaBeta stockAlphaBeta, double highest, double lowest, int depth) {
      this.board = board;
      this.stockAlphaBeta = stockAlphaBeta;
      this.highest = highest;
      this.lowest = lowest;
      this.depth = depth;
    }

    /**
     * The main task to be executed by recursion.
     *
     * @return bestMove The best move calculated.
     */
    @Override
    protected Move compute() {
      Player currentPlayer = this.board.currentPlayer();
      Move bestMove = MoveFactory.getNullMove();
      for (Move move : MoveSorter.EXPENSIVE.sort(currentPlayer.getLegalMoves(), board)) {
        MoveTransition moveTransition = currentPlayer.makeMove(move);
        this.stockAlphaBeta.quiescenceCount = 0;
        if (moveTransition.moveStatus().isDone()) {
          double currentValue = 0.0;
          if (currentPlayer.getAlliance().isWhite()) {
            currentValue = this.stockAlphaBeta.min(moveTransition.toBoard(),
                           this.stockAlphaBeta.calculateQuiescenceDepth(moveTransition.toBoard(),
                           this.depth - 1), this.highest, this.lowest);
            if (currentValue > this.highest) {
              this.highest = currentValue;
              bestMove = move;
              if (moveTransition.toBoard().blackPlayer().isInCheckMate()) {
                break;
              }
            }
          } else if (currentPlayer.getAlliance().isBlack()) {
            currentValue = this.stockAlphaBeta.max(moveTransition.toBoard(),
                           this.stockAlphaBeta.calculateQuiescenceDepth(moveTransition.toBoard(),
                           this.depth - 1), this.highest, this.lowest);
            if (currentValue < this.lowest) {
              this.lowest = currentValue;
              bestMove = move;
              if (moveTransition.toBoard().whitePlayer().isInCheckMate()) {
                break;
              }
            }
          } if (currentPlayer.getAlliance().isWhite() && currentValue > this.highest) {
            this.highest = currentValue;
            bestMove = move;
            if (currentValue >= this.lowest) {
              break;
            }
          } else if (currentPlayer.getAlliance().isBlack() && currentValue < this.lowest) {
            this.lowest = currentValue;
            bestMove = move;
            if (currentValue <= this.highest) {
              break;
            }
          }
        }
      } return bestMove;
    }
  }
}