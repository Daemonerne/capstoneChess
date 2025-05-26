package engine.forBoard;

import engine.forPlayer.MoveStatus;

/**
 * The MoveTransition class represents a transition between two chess board states resulting from a specific move.
 * It encapsulates the information related to the source and destination boards, the transition move performed,
 * and the status of the move, indicating whether it was successful, illegal, or resulted in leaving the player's king in check.
 * <p>
 * This class is implemented as a record, providing immutability and automatic implementations of
 * equals(), hashCode(), and toString() methods. It serves as a crucial component in the chess engine's
 * move validation and execution pipeline, providing a clean way to represent the outcome of a move attempt.
 *
 * @param toBoard The destination board state after the move.
 * @param moveStatus The status of the move, indicating whether it's done, illegal, or leaves the player in check.
 *
 * @author Aaron Ho
 */
public record MoveTransition(Board toBoard, MoveStatus moveStatus) {
  /**
   * Constructs a MoveTransition object with the provided details.
   *
   * @param toBoard    The destination board state after the move.
   * @param moveStatus The status of the move, indicating whether it's done, illegal, or leaves the player in check.
   */
  public MoveTransition {
  }

  /**
   * Retrieves the destination board state after the transition.
   *
   * @return The destination board state.
   */
  @Override
  public Board toBoard() {
    return this.toBoard;
  }

  /**
   * Retrieves the status of the move, indicating its outcome.
   *
   * @return The move status.
   */
  @Override
  public MoveStatus moveStatus() {
    return this.moveStatus;
  }

  /**
   * Retrieves the destination board state after the transition.
   * This method provides alternative access to the toBoard field.
   *
   * @return The destination board state.
   */
  public Board getTransitionBoard() {
    return this.toBoard;
  }

  /**
   * Returns a string representation of the MoveTransition.
   *
   * @return A string containing the board state and move status.
   */
  @Override
  public String toString() {
    return "MoveTransition[" +
            "toBoard=" + toBoard + ", " +
            "moveStatus=" + moveStatus + ']';
  }
}