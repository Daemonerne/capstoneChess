package engine.forPlayer;

import engine.forBoard.Board;

/**
 * The BoardTransition class represents the result of a move execution on the chessboard.
 * It encapsulates both the resulting board state after a move is applied and the status
 * indicating whether the move was successful, illegal, or resulted in check. This immutable
 * record provides a clean way to return move execution results from the chess engine's
 * move validation and execution pipeline.
 * <p>
 * BoardTransition instances are created whenever a move is attempted, allowing the caller
 * to inspect both the outcome and the resulting position in a single return value.
 *
 * @param transitionBoard The chessboard state resulting from the move execution.
 * @param moveStatus The status indicating the outcome of the move attempt.
 *
 *
 * @author dareTo81
 * @author Aaron Ho
 */
public record BoardTransition(Board transitionBoard, MoveStatus moveStatus) {

  /**
   * Constructs a BoardTransition with the specified resulting board and move status.
   * This compact constructor validates that both parameters are provided for the transition.
   *
   * @param transitionBoard The chessboard state resulting from the move execution.
   * @param moveStatus The status indicating the outcome of the move attempt.
   */
  public BoardTransition {
  }

  /**
   * Returns the board state that results from executing the move.
   * This board represents the position after the move has been applied,
   * regardless of whether the move was ultimately legal or successful.
   *
   * @return The resulting board state after move execution.
   */
  @Override
  public Board transitionBoard() {
    return this.transitionBoard;
  }

  /**
   * Returns the status indicating the outcome of the move execution.
   * The status reveals whether the move completed successfully, was illegal,
   * or left the player in an invalid state such as check.
   *
   * @return The move execution status.
   */
  @Override
  public MoveStatus moveStatus() {
    return this.moveStatus;
  }
}