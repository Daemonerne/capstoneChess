package engine.forPlayer;

/**
 * The MoveStatus enum represents the possible results of a chess move execution.
 * It defines three states: DONE, ILLEGAL_MOVE, and LEAVES_PLAYER_IN_CHECK.
 * Each state has a method `isDone()` that indicates whether the move is considered completed.
 * <br>
 * - DONE: The move is successfully executed and completed.
 * <br>
 * - ILLEGAL_MOVE: The move is not valid or illegal.
 * <br>
 * - LEAVES_PLAYER_IN_CHECK: The move is legal but leaves the player in check.
 * 
 * @author Aaron Ho
 * @author dareTo81
 */
public enum MoveStatus {

  /*** Represents a move that is successfully executed and completed. */
  DONE {
  
    @Override
    public boolean isDone() {
    
      return true;
    
    }
  
  },
  
  /*** Represents an illegal or invalid move. */
  ILLEGAL_MOVE {
    
    @Override
    public boolean isDone() {
    
      return false;
    
    }
    
  },
  
  /*** Represents a legal move that leaves the player in check. */
  LEAVES_PLAYER_IN_CHECK {
    
    @Override
    public boolean isDone() {
      
      return false;
      
    }
    
  };
  
  /**
   * Checks if the move is considered completed based on its status.
   *
   * @return true if the move is done, false otherwise.
   */
  public abstract boolean isDone();

}
