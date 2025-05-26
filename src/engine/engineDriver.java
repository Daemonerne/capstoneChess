package engine;

import engine.forGUI.Table;

/**
 * The engineDriver class serves as the main entry point for the Capstone chess engine application.
 * This class contains the primary main method that initializes and launches the graphical user interface
 * for the chess game. It acts as the bootstrap class that starts the entire chess engine system,
 * providing users with access to the interactive chess board and all associated functionality.
 * <p>
 * The class follows a simple design pattern where it immediately delegates control to the Table
 * singleton instance, which manages the entire user interface and game state. This separation
 * allows for clean initialization and maintains the single responsibility principle.
 *
 * @author Aaron Ho
 */
public class engineDriver {

  /**
   * The main entry point for the chess engine application. This method initializes the graphical
   * user interface by retrieving the singleton Table instance and displaying the chess board.
   * The method serves as the application's startup procedure, launching the complete chess game
   * environment including the interactive board, menu system, and AI functionality.
   * <p>
   * Upon execution, this method will create and display the main game window with a standard
   * chess board configuration, allowing users to immediately begin playing or configuring
   * the game according to their preferences.
   *
   * @param args Command line arguments passed to the application (currently unused).
   */
  public static void main(String[] args) {
    Table.get().show();
  }
}