package engine.forPlayer.forAI;

import engine.forBoard.Board;

public interface BoardEvaluator {

  double evaluate(Board board, int depth);

}
