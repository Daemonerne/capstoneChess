package engine.forPlayer;

/**
 * The MoveStatus enum represents the possible outcomes of a chess move execution.
 * It defines three states that indicate whether a move was successfully completed,
 * illegal, or leaves the player in an invalid position. Each status provides
 * information about the completion state through the isDone method.
 *
 * @author Aaron Ho
 * @author dareTo81
 */
public enum MoveStatus {

  /**
   * Represents a move that is successfully executed and completed.
   */
  DONE {

    /**
     * Indicates that the move is successfully completed.
     *
     * @return true as the move is done
     */
    @Override
    public boolean isDone() {
      return true;
    }
  },

  /**
   * Represents an illegal or invalid move that cannot be executed.
   */
  ILLEGAL_MOVE {

    /**
     * Indicates that the move is not completed due to being illegal.
     *
     * @return false as the move is not done
     */
    @Override
    public boolean isDone() {
      return false;
    }
  },

  /**
   * Represents a legal move that leaves the player's king in check.
   */
  LEAVES_PLAYER_IN_CHECK {

    /**
     * Indicates that the move is not completed as it leaves the king in check.
     *
     * @return false as the move is not done
     */
    @Override
    public boolean isDone() {
      return false;
    }
  };

  /**
   * Determines if the move is considered successfully completed based on its status.
   * Only moves with DONE status are considered completed.
   *
   * @return true if the move is successfully completed, false otherwise
   */
  public abstract boolean isDone();

}