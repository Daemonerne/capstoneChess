package engine.forBoard;

import java.util.ArrayList;
import java.util.List;

/**
 * The `MoveUtils` class is a utility class that provides various helper methods related to chess moves and calculations.
 * It includes methods for obtaining exchange scores, managing coordinate lines, and handling null moves.
 *
 * @author Aaron Ho
 */
public class MoveUtils {

  /*** An instance of the `MoveUtils` class that can be used to access its methods. */
  public static final MoveUtils Instance = new MoveUtils();

  /*** A special move representing a lack of valid move, used to signify an invalid or non-existent move. */
  public static final Move NULL_MOVE = new Move.NullMove();

  /**
   * Computes the exchange score of a given move, indicating the relative value of pieces involved in the move.
   * Higher exchange scores suggest advantageous exchanges for the player.
   *
   * @param move The move for which the exchange score is calculated.
   * @return The computed exchange score for the move.
   */
  public static int exchangeScore(final Move move) {
    int score = 0;
    Move currentMove = move;
    while (currentMove != Move.MoveFactory.getNullMove()) {
      score += currentMove.isAttack() ? 5 : 1;
      currentMove = currentMove.getBoard().getTransitionMove();
    }
    return score;
  }

  /**
   * The Line class represents a sequence of integer coordinates forming a line.
   * It is used to manage and store coordinates for various chess-related calculations.
   */
  public static class Line {
    private final List <Integer> coordinates;

    /**
     * Creates a new Line instance with an empty list of coordinates.
     */
    public Line() {
      this.coordinates = new ArrayList < > ();
    }

    /**
     * Adds a coordinate to the line.
     *
     * @param coordinate The coordinate to be added.
     */
    public void addCoordinate(int coordinate) {
      this.coordinates.add(coordinate);
    }

    /**
     * Retrieves the list of coordinates in the line.
     *
     * @return The list of coordinates in the line.
     */
    public int[] getLineCoordinates() {
      int[] result = new int[coordinates.size()];
      for (int i = 0; i < coordinates.size(); i++) {
        result[i] = coordinates.get(i);
      }
      return result;
    }

    /**
     * Checks if the line is empty (contains no coordinates).
     *
     * @return true if the line is empty, false otherwise.
     */
    public boolean isEmpty() {
      return this.coordinates.isEmpty();
    }
  }
}