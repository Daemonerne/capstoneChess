package engine.forPlayer.forAI;

import engine.forBoard.Board;
import engine.forBoard.Move;

public interface MoveStrategy {
  Move execute(Board board);
}
