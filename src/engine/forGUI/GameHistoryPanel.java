package engine.forGUI;

import engine.forBoard.Board;
import engine.forBoard.Move;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static engine.forGUI.Table.*;

/**
 * Represents a panel that displays the move history of the game in a table format. It keeps track of moves made by both
 * white and black players, indicating if a move leads to check or checkmate for the opposing player.
 *
 * @author Aaron Ho
 */
class GameHistoryPanel extends JPanel {

  /*** The DataModel containing the moves. */
  private final DataModel model;

  /*** The scroll pane for the panel. */
  private final JScrollPane scrollPane;

  /*** The Dimension of the panel. */
  private static final Dimension HistoryPanelDimension = new Dimension(250, 40);

  /**
   * Constructs a new GameHistoryPanel by setting up the layout and initializing the data model for the move history table.
   * The panel displays move history in a table with two columns: "White" and "Black".
   */
  GameHistoryPanel() {
    this.setLayout(new BorderLayout());
    this.model = new DataModel();
    final JTable table = new JTable(model);
    table.setRowHeight(15);
    this.scrollPane = new JScrollPane(table);
    scrollPane.setColumnHeaderView(table.getTableHeader());
    scrollPane.setPreferredSize(HistoryPanelDimension);
    this.add(scrollPane, BorderLayout.CENTER);
    this.setVisible(true);
  }

  /**
   * Updates the move history panel to display the latest move made in the game. It adds the move text to the appropriate
   * column based on the player's alliance and indicates check or checkmate if applicable.
   *
   * @param board       The current game board after the move.
   * @param moveHistory The move history log that contains all previous moves made in the game.
   */
  void redo(final Board board, final MoveLog moveHistory) {
    int currentRow = 0;
    this.model.clear();
    for (final Move move: moveHistory.getMoves()) {
      final String moveText = move.toString();
      if (move.getMovedPiece().getPieceAllegiance().isWhite()) {
        this.model.setValueAt(moveText, currentRow, 0);
      } else if (move.getMovedPiece().getPieceAllegiance().isBlack()) {
        this.model.setValueAt(moveText, currentRow, 1);
        currentRow++;
      }
    }
    if (!moveHistory.getMoves().isEmpty()) {
      final Move lastMove = moveHistory.getMoves().get(moveHistory.size() - 1);
      final String moveText = lastMove.toString();
      if (lastMove.getMovedPiece().getPieceAllegiance().isWhite()) {
        this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow, 0);
      } else if (lastMove.getMovedPiece().getPieceAllegiance().isBlack()) {
        this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow - 1, 1);
      }
    }
    final JScrollBar vertical = scrollPane.getVerticalScrollBar();
    vertical.setValue(vertical.getMaximum());
  }

  /**
   * Calculates and returns a string hash indicating check or checkmate based on the current state of the game board.
   *
   * @param board The current game board.
   * @return      A hash string representing the status of the game, indicating check (+) or checkmate (#).
   */
  private static String calculateCheckAndCheckMateHash(final Board board) {
    if (board.currentPlayer().isInCheckMate()) {
      return "#";
    } else if (board.currentPlayer().isInCheck()) {
      return "+";
    }
    return "";
  }

  /*** Represents a row of data in the move history table, containing move texts for both white and black players. */
  private class Row {

    /*** The move text for the white player. */
    private String whiteMove;

    /*** The move text for the black player. */
    private String blackMove;

    /*** Constructs a new Row instance. */
    Row() {
    }

    /**
     * Gets the move text for the white player.
     *
     * @return The move text for the white player.
     */
    public String getWhiteMove() {
      return this.whiteMove;
    }

    /**
     * Gets the move text for the black player.
     *
     * @return The move text for the black player.
     */
    public String getBlackMove() {
      return this.blackMove;
    }

    /**
     * Sets the move text for the white player.
     *
     * @param move The move text to be set.
     */
    public void setWhiteMove(final String move) {
      this.whiteMove = model.getRowCount() + ". " + move;
    }

    /**
     * Sets the move text for the black player.
     *
     * @param move The move text to be set.
     */
    public void setBlackMove(final String move) {
      this.blackMove = model.getRowCount() + ". " + move;
    }
  }

  /*** Custom data model for the move history table. Manages the storage and retrieval of move texts for display. */
  private class DataModel extends DefaultTableModel {

    /*** The list of row objects representing data in the move history table. */
    private final List<Row> values;

    /*** The column names for the move history table. */
    private static final String[] NAMES = {
            "White",
            "Black"
    };

    /*** Constructs a new DataModel with an empty list of values. */
    DataModel() {
      this.values = new ArrayList<>();
    }

    /*** Clears the data model, removing all stored values. */
    public void clear() {
      this.values.clear();
      setRowCount(0);
    }

    /*** Returns the total row count for the DataModel. */
    @Override
    public int getRowCount() {
      if (this.values == null) {
        return 0;
      }
      return this.values.size();
    }

    /*** Returns the column count for the DataModel. */
    @Override
    public int getColumnCount() {
      return NAMES.length;
    }

    /*** Returns the value in a specific row and column (one cell). */
    @Override
    public Object getValueAt(final int row, final int col) {
      final Row currentRow = this.values.get(row);
      if (col == 0) {
        return currentRow.getWhiteMove();
      } else if (col == 1) {
        return currentRow.getBlackMove();
      }
      return null;
    }

    /*** Sets the value in a specific row and column (one cell). */
    @Override
    public void setValueAt(final Object aValue, final int row, final int col) {
      final Row currentRow;
      if (this.values.size() <= row) {
        currentRow = new Row();
        this.values.add(currentRow);
      } else {
        currentRow = this.values.get(row);
      }
      if (col == 0) {
        currentRow.setWhiteMove((String) aValue);
        fireTableRowsInserted(row, row);
      } else if (col == 1) {
        currentRow.setBlackMove((String) aValue);
        fireTableCellUpdated(row, col);
      }
    }

    /*** Returns the class type for the specified column. */
    @Override
    public Class<?> getColumnClass(final int col) {
      return Move.class;
    }

    /*** Returns the name of the specified column. */
    @Override
    public String getColumnName(final int col) {
      return NAMES[col];
    }
  }
}