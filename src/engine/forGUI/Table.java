package engine.forGUI;

import com.google.common.collect.Lists;
import engine.forBoard.*;
import engine.forPiece.Piece;
import engine.forPlayer.Player;
import engine.forPlayer.forAI.StandardBoardEvaluator;
import engine.forPlayer.forAI.StockAlphaBeta;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static engine.forBoard.Move.MoveFactory.createMove;
import static javax.swing.JDialog.setDefaultLookAndFeelDecorated;
import static javax.swing.SwingUtilities.*;

/**
 * The Table class represents the main user interface for the Capstone chess engine. It creates a graphical chess
 * board using Swing components and provides various features such as a menu bar, game history panel, debug panel,
 * taken pieces panel, and the ability to observe the game with an AI watcher. The Table class
 * initializes and displays the chess board, manages game setup, and controls the overall user experience. It also
 * handles the graphical representation of the chess pieces and their movements during gameplay. The class follows the
 * singleton pattern to ensure only one instance of the Table is created and used throughout the application.
 * <br><br>
 * Known bug:
 * At a very rapid pace of play (1 move in < .1 sec), promoted pawns will incorrectly remain pawns on the back ranks
 * instead of repainting to a queen. Likely due to a bug in the logic in the drawBoard() method, or even in the Pawn class. 
 * There is also a possibility of a runtime error because of the rapid rate of play.
 * <br><br>
 * Known bug:
 * Encountered mysterious bug when attempted to check for three-fold repetition among players. There is no indication as to
 * what is causing the bug, but it is likely a runtime error or a logic error because it is encountered after execution.
 * Most of the code has been deleted, or commented out, as it would likely require an overhaul to implement successfully.
 * <br><br>
 * Known bug:
 * Once a game has started and the computer is thinking, it is possible to make more AI players think in parallel. This
 * Drastically slows down the computation and glitches out the DebugPanel and the StockAlphaBeta class. A lock on selecting
 * the buttons in the "Setup Game" menu might be required to fix this.
 *
 * @author Aaron Ho
 * @author dareTo81
 */

public final class Table extends Observable {

  /*** The panel displaying the game's move history. */
  private final GameHistoryPanel gameHistoryPanel;

  /*** The panel for displaying debug information or messages. */
  private final DebugPanel debugPanel;

  /*** The panel representing the chess board and its tiles. */
  private final BoardPanel boardPanel;

  /*** The log of moves made in the game. */
  public static MoveLog moveLog;

  /*** The setup configuration for the current game. */
  private final GameSetup gameSetup;

  /*** The current state of the chess board. */
  private Board chessBoard;

  /*** The move made by the computer. */
  private Move computerMove;

  /*** The source tile of the piece being moved. */
  private Piece sourceTile;

  /*** The piece moved by the human player. */
  private Piece humanMovedPiece;

  /*** The direction of the chess board (normal or flipped). */
  private BoardDirection boardDirection;

  /*** The file path for the icons representing chess pieces. */
  private final String pieceIconPath;

  /*** Indicates whether legal moves should be highlighted on the board. */
  private boolean highlightLegalMoves;

  /*** The color of light tiles on the chess board. */
  private final Color lightTileColor = Color.decode("#FFFACD");

  /*** The color of dark tiles on the chess board. */
  private final Color darkTileColor = Color.decode("#593E1A");

  /*** A Dimension that represents the outer frame of the chessboard. */
  private static final Dimension OUTER_FRAME_DIMENSION = Toolkit.getDefaultToolkit().getScreenSize();

  /*** A Dimension that represents the board. */
  private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(620, 620);

  /*** A Dimension that represents a tile. */
  private static final Dimension TILE_PANEL_DIMENSION = new Dimension(40, 40);

  /*** The single instance of the Table class (singleton). */
  private static final Table Instance = new Table();


  /**
   * Constructs an instance of the Table class, creating the main graphical user interface for the Capstone chess engine.
   * The constructor initializes and configures various components, including the game frame, menu bar, chess board,
   * game history panel, debug panel, taken pieces panel, and game setup options. It sets up the user interface for
   * playing chess, including the display of chess pieces, legal move highlighting, and interaction with the user.
   * The Table class follows the singleton pattern, ensuring that only one instance is created for managing the user
   * interface of the chess game.
   */
  private Table() {
    JFrame gameFrame = new JFrame("Capstone Chess");
    final JMenuBar tableMenuBar = new JMenuBar();
    populateMenuBar(tableMenuBar);
    gameFrame.setJMenuBar(tableMenuBar);
    gameFrame.setLayout(new BorderLayout());
    this.chessBoard = Board.createStandardBoard();
    this.boardDirection = BoardDirection.NORMAL;
    this.highlightLegalMoves = false;
    this.pieceIconPath = "art/simple/";
    this.gameHistoryPanel = new GameHistoryPanel();
    this.debugPanel = new DebugPanel();
    this.boardPanel = new BoardPanel();
    moveLog = new MoveLog();
    this.addObserver(new TableGameAIWatcher());
    this.gameSetup = new GameSetup(gameFrame, true);
    gameFrame.add(this.boardPanel);
    gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
    gameFrame.add(debugPanel, BorderLayout.SOUTH);
    setDefaultLookAndFeelDecorated(true);
    gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    gameFrame.setSize(OUTER_FRAME_DIMENSION);
    center(gameFrame);
    gameFrame.setVisible(true);
  }

  /**
   * Retrieves the singleton instance of the Table class, providing access to the main graphical user interface.
   *
   * @return The singleton instance of the Table class.
   */
  public static Table get() {
    return Instance;
  }

  /**
   * Retrieves the Board object representing the current state of the chess board.
   *
   * @return The Board object representing the chess board.
   */
  private Board getGameBoard() {
    return this.chessBoard;
  }

  /**
   * Retrieves the MoveLog object responsible for recording and managing the sequence of moves made during the game.
   *
   * @return The MoveLog object for tracking the move history.
   */
  private MoveLog getMoveLog() {
    return moveLog;
  }

  /**
   * Retrieves the BoardPanel object responsible for rendering and displaying the graphical chess board.
   *
   * @return The BoardPanel object for rendering the chess board.
   */
  private BoardPanel getBoardPanel() {
    return this.boardPanel;
  }

  /**
   * Retrieves the GameHistoryPanel object responsible for displaying the sequence of moves and game history.
   *
   * @return The GameHistoryPanel object for displaying move history.
   */
  private GameHistoryPanel getGameHistoryPanel() {
    return this.gameHistoryPanel;
  }

  /**
   * Retrieves the DebugPanel object responsible for displaying debugging and game-related information.
   *
   * @return The DebugPanel object for displaying debugging information.
   */
  private DebugPanel getDebugPanel() {
    return this.debugPanel;
  }

  /**
   * Retrieves the GameSetup object responsible for configuring game parameters and setup options.
   *
   * @return The GameSetup object for configuring game settings.
   */
  private GameSetup getGameSetup() {
    return this.gameSetup;
  }

  /**
   * Retrieves the current status of legal move highlighting on the chess board.
   *
   * @return true if legal move highlighting is enabled, false otherwise.
   */
  private boolean getHighlightLegalMoves() {
    return this.highlightLegalMoves;
  }

  /**
   * Refreshes and updates the graphical user interface to reflect the current state of the chess game. This method is
   * responsible for clearing the move log, updating the game history panel, refreshing the taken pieces panel, redrawing
   * the chess board, and updating the debug panel. The method helps ensure that the user interface accurately reflects
   * the most recent changes and moves in the chess game. It is typically called after a move has been made to update
   * the display accordingly.
   */
  public void show() {
    if (moveLog.size() == 0) {
      Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
      Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
      Table.get().getDebugPanel().redo();
      setChanged();
      notifyObservers();
    }
  }

  /**
   * Populates the menu bar with Preferences and Options menus.
   *
   * @param tableMenuBar The JMenuBar to populate.
   */
  private void populateMenuBar(final JMenuBar tableMenuBar) {
    tableMenuBar.add(createPreferencesMenu());
    tableMenuBar.add(createOptionsMenu());
  }

  /**
   * Centers the provided JFrame on the screen.
   *
   * @param frame The JFrame to be centered.
   */
  private static void center(final JFrame frame) {
    final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    final int w = Toolkit.getDefaultToolkit().getScreenSize().width;
    final int h = Toolkit.getDefaultToolkit().getScreenSize().height;
    final int x = (dim.width - w) / 2;
    final int y = (dim.height - h) / 2;
    frame.setLocation(x, y);
    frame.pack();
  }

  /**
   * This method constructs a JMenu with options to interact with the game, including starting a new game,
   * evaluating the current board position, displaying escape analysis scores, inspecting the current game state,
   * undoing the last move, and setting up the game parameters. Each option is associated with a specific action
   * when selected, such as undoing a move, displaying evaluation details, or configuring game settings.
   *
   * @return The JMenu containing gameplay options.
   */
  private JMenu createOptionsMenu() {

    final JMenu optionsMenu = new JMenu("Options");
    optionsMenu.setMnemonic(KeyEvent.VK_O);

    final JMenuItem resetMenuItem = new JMenuItem("New Game", KeyEvent.VK_P);
    resetMenuItem.addActionListener(e -> undoAllMoves());
    optionsMenu.add(resetMenuItem);

    final JMenuItem evaluateBoardMenuItem = new JMenuItem("Evaluate Board", KeyEvent.VK_E);
    evaluateBoardMenuItem.addActionListener(e -> System.out.println(StandardBoardEvaluator.get().evaluationDetails(chessBoard, gameSetup.getSearchDepth())));
    optionsMenu.add(evaluateBoardMenuItem);

    final JMenuItem escapeAnalysis = new JMenuItem("Escape Analysis Score", KeyEvent.VK_S);
    escapeAnalysis.addActionListener(e -> {
      final Move lastMove = moveLog.getMoves().get(moveLog.size() - 1);
      if (lastMove != null) {
        System.out.println(MoveUtils.exchangeScore(lastMove));
      }

    });
    optionsMenu.add(escapeAnalysis);

    final JMenuItem legalMovesMenuItem = new JMenuItem("Current State", KeyEvent.VK_L);
    legalMovesMenuItem.addActionListener(e -> {
      System.out.println(chessBoard.getWhitePieces());
      System.out.println(chessBoard.getBlackPieces());
      System.out.println(playerInfo(chessBoard.currentPlayer()));
      System.out.println(playerInfo(chessBoard.currentPlayer().getOpponent()));
    });
    optionsMenu.add(legalMovesMenuItem);

    final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move", KeyEvent.VK_M);
    undoMoveMenuItem.addActionListener(e -> {
      if (Table.get().getMoveLog().size() > 0) {
        undoLastMove();
      }
    });
    optionsMenu.add(undoMoveMenuItem);

    final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game", KeyEvent.VK_S);
    setupGameMenuItem.addActionListener(e -> {
      Table.get().getGameSetup().promptUser();
      Table.get().setupUpdate(Table.get().getGameSetup());
    });
    optionsMenu.add(setupGameMenuItem);

    return optionsMenu;
  }

  /**
   * This method constructs a JMenu with options for customizing user preferences and settings related to the
   * appearance and behavior of the chess game. Users can choose tile colors, chess piece images, highlight legal
   * moves, and toggle book moves usage.
   *
   * @return The JMenu containing user preferences and settings.
   */
  private JMenu createPreferencesMenu() {

    final JMenu preferencesMenu = new JMenu("Preferences");
    final JMenuItem flipBoardMenuItem = new JMenuItem("Flip board");

    flipBoardMenuItem.addActionListener(e -> {
      boardDirection = boardDirection.opposite();
      boardPanel.drawBoard(chessBoard);
    });

    preferencesMenu.add(flipBoardMenuItem);
    preferencesMenu.addSeparator();

    final JCheckBoxMenuItem cbLegalMoveHighlighter = new JCheckBoxMenuItem(
            "Highlight Legal Moves", false);

    cbLegalMoveHighlighter.addActionListener(e -> highlightLegalMoves = cbLegalMoveHighlighter.isSelected());

    preferencesMenu.add(cbLegalMoveHighlighter);
    return preferencesMenu;

  }

  /**
   * This method constructs a string containing details about the given player's alliance, legal moves,
   * check status, checkmate status, and castling status. The generated text provides insights into the current
   * state of the player in the chess game.
   *
   * @param player The player for which to generate the information.
   * @return       A string representation of the player's status and legal moves.
   */
  private static String playerInfo(final Player player) {
    return ("Player is: " + player.getAlliance() + 
            "\nlegal moves (" + player.getLegalMoves().size() + ") = " + player.getLegalMoves() +
            "\ninCheck = " + player.isInCheck() + 
            "\nisInCheckMate = " + player.isInCheckMate() +
            "\nisCastled = " + player.isCastled()) + 
            "\n";
  }

  /**
   * This method updates the internal state of the chess game's board to match the configuration
   * of the provided board. It is used to synchronize the displayed game board with the actual game state.
   *
   * @param board The new board configuration to be applied to the game.
   */
  private void updateGameBoard(final Board board) {
    this.chessBoard = board;
  }

  /**
   * This method updates the internal state of the computer's chosen move to the specified move.
   * It is used to keep track of the move selected by the computer player during the game.
   *
   * @param move The move chosen by the computer player.
   */
  private void updateComputerMove(final Move move) {
    this.computerMove = move;
  }

  /**
   * This method reverses all moves that have been made in the current game session, effectively
   * resetting the game to its initial state. It clears the move log, updates the game board and
   * history, and redraws the board panel to reflect the reset game state.
   */
  private void undoAllMoves() {
    for (int i = Table.get().getMoveLog().size() - 1; i >= 0; i--) {
      final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
      this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).toBoard();
    }
    this.computerMove = null;
    Table.get().getMoveLog().clear();
    Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
    Table.get().getBoardPanel().drawBoard(chessBoard);
    Table.get().getDebugPanel().redo();
  }

  /**
   * This method removes the last move from the move log, updates the game board state,
   * clears the computer move, and refreshes various components of the user interface to reflect
   * the changes after undoing the move.
   */
  private void undoLastMove() {
    final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
    this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).toBoard();
    this.computerMove = null;
    Table.get().getMoveLog().removeMove(lastMove);
    Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
    Table.get().getBoardPanel().drawBoard(chessBoard);
    Table.get().getDebugPanel().redo();
  }

  /**
   * This method sets the state as changed and notifies registered observers with the
   * specified player type as an argument.
   *
   * @param playerType The type of player (Human or Computer) who made the move.
   */
  private void moveMadeUpdate(final PlayerType playerType) {
    setChanged();
    notifyObservers(playerType);
  }

  /**
   * This method sets the state as changed and notifies registered observers with the
   * provided game setup configuration.
   *
   * @param gameSetup The updated game setup configuration.
   */
  private void setupUpdate(final GameSetup gameSetup) {
    setChanged();
    notifyObservers(gameSetup);
  }

  /**
   * A watcher class that observes changes in the game state and triggers AI actions
   * or displays game-over messages based on certain conditions. Observer is deprecated,
   * but its use in this class was heavily advised.
   */
  private static class TableGameAIWatcher implements Observer {

    /**
     * Responds to updates from the observed object, triggering AI actions or
     * displaying game-over messages based on the current game state.
     *
     * @param o   The observable object (not used in this context).
     * @param arg An argument passed by the observed object (not used in this context).
     */
    @Override
    public void update(final Observable o,
                       final Object arg) {

      if (Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer()) &&
              !Table.get().getGameBoard().currentPlayer().isInCheckMate() &&
              !Table.get().getGameBoard().currentPlayer().isInStaleMate()) {
        System.out.println(Table.get().getGameBoard().currentPlayer() + " is set to AI, thinking....");
        final AIThinkTank thinkTank = new AIThinkTank();
        thinkTank.execute();
      }

      if (Table.get().getGameBoard().currentPlayer().isInCheckMate()) {
        JOptionPane.showMessageDialog(Table.get().getBoardPanel(),
                "Game Over: Player " + Table.get().getGameBoard().currentPlayer() + " is in checkmate!", "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
      }

        if (Table.get().getGameBoard().currentPlayer().isInStaleMate()) {
        JOptionPane.showMessageDialog(Table.get().getBoardPanel(),
                "Game Over: Player " + Table.get().getGameBoard().currentPlayer() + " is in stalemate!", "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
      }

    }
  }

  /*** An enumeration representing the types of players in the chess game: Human and Computer. */
  enum PlayerType {

    /*** Represents a human player. */
    HUMAN,

    /*** Represents a computer player. */
    COMPUTER

  }

  /*** An asynchronous worker class responsible for AI move calculation and execution. */
  private static class AIThinkTank extends SwingWorker < Move, String > {

    private final ExecutorService executorService;  

    /*** Constructs an instance of AIThinkTank. */
    private AIThinkTank() {
      this.executorService = Executors.newFixedThreadPool(1);
    }

    /**
     * Performs AI move calculation in the background.
     *
     * @return The best move calculated by the AI.
     */
    @Override
    protected Move doInBackground() {
      final Move bestMove;
      final StockAlphaBeta strategy = new StockAlphaBeta(Table.get().getGameSetup().getSearchDepth());
      strategy.addObserver(Table.get().getDebugPanel());
      bestMove = strategy.execute(Table.get().getGameBoard());

      return bestMove;
    }

    /*** Handles the completion of the AI move calculation. */
    @Override
    public void done() {
      try {
        final Move bestMove = get();
        Table.get().updateComputerMove(bestMove);
        Table.get().updateGameBoard(Table.get().getGameBoard().currentPlayer().makeMove(bestMove).toBoard());
        Table.get().getMoveLog().addMove(bestMove);
        Table.get().getGameHistoryPanel().redo(Table.get().getGameBoard(), Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
        Table.get().getDebugPanel().redo();
        Table.get().moveMadeUpdate(PlayerType.COMPUTER);
      } catch (final Exception e) {
        System.out.println("Exception in AI move handling!");
        e.printStackTrace();
      } finally {
        executorService.shutdown();
      }
    }

  }

  /**
   * Represents a graphical panel displaying the chess board and its tiles.
   * This panel is used to visualize the current state of the chess game.
   */
  private class BoardPanel extends JPanel {

    /*** List of tile panels representing the individual tiles on the chess board. */
    final List < TilePanel > boardTiles;

    /*** Constructs a new BoardPanel and initializes its tile panels. */
    BoardPanel() {
      super(new GridLayout(8, 8));
      this.boardTiles = new ArrayList < > ();
      for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
        final TilePanel tilePanel = new TilePanel(this, i);
        this.boardTiles.add(tilePanel);
        add(tilePanel);
      }

      setPreferredSize(BOARD_PANEL_DIMENSION);
      setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      setBackground(Color.decode("#8B4726"));
      validate();
    }

    /**
     * Draws the chess board based on the given board state, updating the display.
     *
     * @param board The current state of the chess board.
     */
    void drawBoard(final Board board) {
      removeAll();
      for (final TilePanel boardTile: boardDirection.traverse(boardTiles)) {
        boardTile.drawTile(board);
        add(boardTile);
      }
      validate();
      repaint();

    }
  }

  /*** An enumeration representing the two possible directions of the chess board display. */
  enum BoardDirection {

    /*** Represents the normal direction of the chess board, where tiles are displayed in their natural order. */
    NORMAL {

      @Override
      List < TilePanel > traverse(final List < TilePanel > boardTiles) {
        return boardTiles;
      }

      @Override
      BoardDirection opposite() {
        return FLIPPED;
      }
    },

    /*** Represents the flipped direction of the chess board, where tiles are displayed in reverse order. */
    FLIPPED {
      @Override
      List < TilePanel > traverse(final List < TilePanel > boardTiles) {
        return Lists.reverse(boardTiles);
      }

      @Override
      BoardDirection opposite() {
        return NORMAL;
      }
    };

    /**
     * Traverses the list of tile panels according to the current board direction.
     *
     * @param boardTiles The list of tile panels to traverse.
     * @return           A list of tile panels in the desired traversal order.
     */
    abstract List < TilePanel > traverse(final List < TilePanel > boardTiles);

    /**
     * Returns the opposite board direction.
     *
     * @return The opposite board direction.
     */
    abstract BoardDirection opposite();

  }

  /*** A class representing a log of moves made during a chess game. */
  public static class MoveLog {

    private final List <Move> moves;

    /*** Constructs a new MoveLog. */
    MoveLog() {
      this.moves = new LinkedList <>();
    }

    /**
     * Gets the list of moves stored in the MoveLog.
     *
     * @return The list of moves.
     */
    public List < Move > getMoves() {
      return this.moves;
    }

    /**
     * Adds a move to the MoveLog.
     *
     * @param move The move to be added.
     */
    void addMove(final Move move) {
      this.moves.add(move);
    }

    /**
     * Gets the number of moves in the MoveLog.
     *
     * @return The number of moves.
     */
    public int size() {
      return this.moves.size();
    }

    /**
     * Clears all moves from the MoveLog.
     */
    void clear() {
      this.moves.clear();
    }

    /**
     * Removes a move at the specified index from the MoveLog.
     *
     * @param index The index of the move to be removed.
     * @return      The removed move.
     */
    Move removeMove(final int index) {
      return this.moves.remove(index);
    }

    /**
     * Removes a specific move from the MoveLog.
     *
     * @param move The move to be removed.
     */
    void removeMove(final Move move) {
      this.moves.remove(move);
    }

  }

  /**
   * Represents a graphical panel for an individual tile on the chessboard. Each
   * TilePanel displays a single tile of the chessboard grid along with its
   * associated piece, legal move highlights, and other visual elements.
   */
  private class TilePanel extends JPanel {

    private final int tileId;

    /**
     * Constructs a TilePanel for a specific tile on the chessboard.
     *
     * @param boardPanel The parent BoardPanel that contains this tile.
     * @param tileId The unique identifier of the tile (0 to 63).
     */
    TilePanel(final BoardPanel boardPanel,
              final int tileId) {
      super(new GridBagLayout());
      this.tileId = tileId;
      setPreferredSize(TILE_PANEL_DIMENSION);
      assignTileColor();
      assignTilePieceIcon(chessBoard);
      highlightTileBorder(chessBoard);
      addMouseListener(new MouseListener() {
        @Override
        public void mouseClicked(final MouseEvent event) {
          if (Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer()) ||
                  BoardUtils.isEndOfGame(Table.get().getGameBoard())) {
            return;
          }
          if (isRightMouseButton(event)) {
            sourceTile = null;
            humanMovedPiece = null;
          } else if (isLeftMouseButton(event)) {
            if (sourceTile == null) {
              sourceTile = chessBoard.getPiece(tileId);
              humanMovedPiece = sourceTile;
            } else {
              final Move move = createMove(chessBoard, sourceTile.getPiecePosition(),
                      tileId);
              final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
              if (transition.moveStatus().isDone()) {
                chessBoard = transition.toBoard();
                moveLog.addMove(move);
              }
              sourceTile = null;
              humanMovedPiece = null;
            }
          }
          invokeLater(() -> {
            gameHistoryPanel.redo(chessBoard, moveLog);
            Table.get().moveMadeUpdate(PlayerType.HUMAN);
            boardPanel.drawBoard(chessBoard);
            debugPanel.redo();
          });
        }

        @Override
        public void mouseExited(final MouseEvent e) {}

        @Override
        public void mouseEntered(final MouseEvent e) {}

        @Override
        public void mouseReleased(final MouseEvent e) {}

        @Override
        public void mousePressed(final MouseEvent e) {}
      });
      validate();
    }

    /**
     * Draws the appearance of the tile based on the current state of the chessboard.
     *
     * @param board The current chessboard.
     */
    void drawTile(final Board board) {
      SwingUtilities.invokeLater(() -> {
        assignTileColor();
        assignTilePieceIcon(board);
        highlightTileBorder(board);
        highlightLegals(board);
        highlightAIMove();
        validate();
        repaint();
      });
    }

    /**
     * Highlights the border of the tile based on the current state of the game.
     * The border is colored cyan if the specified humanMovedPiece is the current player's piece
     * and is positioned on this tile, indicating a possible move source. Otherwise, the border
     * is colored gray, indicating a regular tile.
     *
     * @param board The current state of the chessboard.
     */
    private void highlightTileBorder(final Board board) {
      if (humanMovedPiece != null &&
              humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() &&
              humanMovedPiece.getPiecePosition() == this.tileId) {
        setBorder(BorderFactory.createLineBorder(Color.cyan));
      } else {
        setBorder(BorderFactory.createLineBorder(Color.GRAY));  
      }
    }

    /**
     * Highlights the background of the tile if a computer move is available.
     * The tile is colored pink if it matches the current coordinate of the computer move,
     * and red if it matches the destination coordinate of the computer move.
     */
    private void highlightAIMove() {
      if (computerMove != null) {
        if (this.tileId == computerMove.getCurrentCoordinate()) {
          setBackground(Color.pink);
        } else if (this.tileId == computerMove.getDestinationCoordinate()) {
          setBackground(Color.red);
        }
      }
    }

    /**
     * Highlights legal move destinations on the tile panel, displaying a green dot icon.
     * Checks if highlighting legal moves is enabled and adds a green dot icon to the tile
     * if the move's destination coordinate matches the tile's ID.
     *
     * @param board The current state of the chess board.
     */
    private void highlightLegals(final Board board) {
      if (Table.get().getHighlightLegalMoves()) {
        for (final Move move: pieceLegalMoves(board)) {
          if (move.getDestinationCoordinate() == this.tileId) {
            try {
              add(new JLabel(new ImageIcon(ImageIO.read(new File("art/misc/green_dot.png")))));
            } catch (final IOException e) {
              System.out.println("Exception in highlightLegals in Table.java");
            }
          }
        }
      }
    }

    /**
     * Retrieves the collection of legal moves for the currently selected piece on the board.
     *
     * @param board The current state of the chess board.
     * @return A collection of legal moves for the selected piece.
     */
    private Collection < Move > pieceLegalMoves(final Board board) {
      if (humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance()) {
        return humanMovedPiece.calculateLegalMoves(board);
      }
      return Collections.emptyList();
    }

    /**
     * Assigns the appropriate piece icon to the tile on the board based on the current state of the chess board.
     *
     * @param board The current state of the chess board.
     */
    private void assignTilePieceIcon(final Board board) {
      this.removeAll();
      if (board.getPiece(this.tileId) != null) {
        try {
          final BufferedImage image = ImageIO.read(new File(pieceIconPath +
                  board.getPiece(this.tileId).getPieceAllegiance().toString().charAt(0) +
                  board.getPiece(this.tileId).toString() +
                  ".gif"));
          add(new JLabel(new ImageIcon(image)));
        } catch (final IOException e) {
          System.out.println("Exception in assignTilePieceIcon in Table.java");
        }
      }
    }

    /**
     * Assigns the appropriate background color to the tile based on its position on the chess board.
     * Differentiates between light and dark tiles in alternating rows.
     */
    private void assignTileColor() {
      if (BoardUtils.Instance.FirstRow.get(this.tileId) ||
              BoardUtils.Instance.ThirdRow.get(this.tileId) ||
              BoardUtils.Instance.FifthRow.get(this.tileId) ||
              BoardUtils.SeventhRow.get(this.tileId)) {
        setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
      } else if (BoardUtils.Instance.SecondRow.get(this.tileId) ||
              BoardUtils.Instance.FourthRow.get(this.tileId) ||
              BoardUtils.Instance.SixthRow.get(this.tileId) ||
              BoardUtils.Instance.EighthRow.get(this.tileId)) {
        setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
      }
    }
  }
}
