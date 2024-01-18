package engine.forPlayer;

import engine.forBoard.Board;

/**
 * The `BoardTransition` class represents the result of a move on the chessboard.
 * It contains information about the new board state after the move and the status of the move.
 *
 * @author dareTo81
 * @author Aaron Ho
 */
public record BoardTransition(Board transitionBoard, MoveStatus moveStatus) {

  /**
   * Constructs a `BoardTransition` object with the given transition board and move status.
   *
   * @param transitionBoard The chessboard resulting from the move transition.
   * @param moveStatus      The status of the move (e.g., DONE, ILLEGAL_MOVE, LEAVES_PLAYER_IN_CHECK).
   */
  public BoardTransition {
  }

  /**
   * Gets the status of the move.
   *
   * @return The move status.
   */
  @Override
  public MoveStatus moveStatus() {
    return this.moveStatus;
  }

}
