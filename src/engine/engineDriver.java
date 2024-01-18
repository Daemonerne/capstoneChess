package engine;

import engine.forBoard.Board;
import engine.forGUI.Table;

/*** The main client for the chessEngine package. */
public class engineDriver {

  public static void main(String[] args) {

    Board board = Board.createStandardBoard();
    
    Table.get().show();
  
  }
  
}

