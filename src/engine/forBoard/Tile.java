package engine.forBoard;


import com.google.common.collect.ImmutableMap;
import engine.forPiece.Piece;

import java.util.HashMap;
import java.util.Map;


/**
 * The Tile class represents a single square on a chess board.
 * This abstract class provides the base functionality for chess board tiles and offers
 * methods to determine if a tile is occupied and which piece occupies it. The implementation
 * uses the Flyweight pattern for empty tiles to minimize memory usage.
 * <p>
 * Subclasses of Tile include EmptyTile for unoccupied squares and OccupiedTile for squares
 * containing chess pieces. The class provides a factory method for creating the appropriate
 * type of tile based on whether a piece is present.
 *
 * @author Aaron Ho
 */
public abstract class Tile {

  /**
   * The coordinate of the tile on the chess board.
   * This value ranges from 0 to 63, representing the tiles from a8 to h1.
   */
  protected final int tileCoordinate;

  /**
   * Cache containing all possible empty tiles on the chess board.
   * This map uses the Flyweight pattern to reuse empty tile objects rather than
   * creating new instances for each empty square, improving memory efficiency.
   */
  private static final Map<Integer, EmptyTile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();

  /**
   * Initializes a map containing all possible empty tiles (0-63) to avoid creating
   * new empty tile instances during board creation or move execution.
   * This method is used to populate the EMPTY_TILES_CACHE when the class is loaded.
   *
   * @return An ImmutableMap of Integer to EmptyTile, containing all possible empty tiles.
   */
  private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
    final Map<Integer, EmptyTile> emptyTileMap = new HashMap<>();
    for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
      emptyTileMap.put(i, new EmptyTile(i));
    }
    return ImmutableMap.copyOf(emptyTileMap);
  }

  /**
   * Factory method that creates a tile at the given coordinate, either empty or occupied
   * depending on whether a piece is provided. This method returns a cached EmptyTile
   * instance if no piece is provided, or creates a new OccupiedTile if a piece is present.
   *
   * @param tileCoordinate The coordinate of the tile on the board (0-63).
   * @param piece The piece to place on the tile, or null for an empty tile.
   * @return A new Tile instance of the appropriate type.
   */
  public static Tile createTile(final int tileCoordinate, final Piece piece) {
    return piece != null ? new OccupiedTile(tileCoordinate, piece) : EMPTY_TILES_CACHE.get(tileCoordinate);
  }

  /**
   * Constructs a Tile with the specified coordinate. This constructor is protected
   * and used by subclasses to initialize the tile's position on the board.
   *
   * @param tileCoordinate The coordinate of the tile on the board (0-63).
   */
  private Tile(final int tileCoordinate) {
    this.tileCoordinate = tileCoordinate;
  }

  /**
   * Determines whether the tile is occupied by a chess piece.
   *
   * @return true if the tile is occupied, false if it is empty.
   */
  public abstract boolean isTileOccupied();

  /**
   * Returns the piece on this tile, if any.
   *
   * @return The piece on this tile, or null if the tile is empty.
   */
  public abstract Piece getPiece();

  /**
   * Returns the coordinate of this tile on the chess board.
   *
   * @return The tile coordinate, a value between 0 and 63.
   */
  public int getTileCoordinate() {
    return this.tileCoordinate;
  }

  /**
   * The EmptyTile class represents an empty tile on a chess board.
   * It is a subclass of the abstract Tile class that represents tiles without any pieces.
   * EmptyTile instances are cached and reused for memory efficiency.
   * <p>
   * This class provides implementations for the abstract methods defined in the Tile class,
   * always indicating that the tile is not occupied and has no piece associated with it.
   *
   * @author Aaron Ho
   */
  public static final class EmptyTile extends Tile {

    /**
     * Constructs an empty tile at the specified coordinate.
     * This constructor is private to enforce the use of the Tile factory method
     * and the Flyweight pattern for empty tiles.
     *
     * @param coordinate The coordinate of the empty tile on the board.
     */
    private EmptyTile(final int coordinate) {
      super(coordinate);
    }

    /**
     * Returns a string representation of the empty tile, which is "-".
     *
     * @return The string "-" to represent an empty tile.
     */
    @Override
    public String toString() {
      return "-";
    }

    /**
     * Indicates that this tile is not occupied by a piece.
     *
     * @return false, as an empty tile is never occupied.
     */
    @Override
    public boolean isTileOccupied() {
      return false;
    }

    /**
     * Returns the piece on this tile, which is always null for an empty tile.
     *
     * @return null, as an empty tile has no piece.
     */
    @Override
    public Piece getPiece() {
      return null;
    }
  }

  /**
   * The OccupiedTile class represents a tile on a chess board that contains a piece.
   * It is a subclass of the abstract Tile class that represents tiles occupied by chess pieces.
   * <p>
   * This class provides implementations for the abstract methods defined in the Tile class,
   * always indicating that the tile is occupied and returning the associated piece.
   *
   * @author Aaron Ho
   */
  public static final class OccupiedTile extends Tile {
    /**
     * The chess piece positioned on this tile.
     * This field stores the reference to the piece that occupies the tile.
     */
    private final Piece pieceOnTile;

    /**
     * Constructs an occupied tile at the specified coordinate with the given piece.
     * This constructor is private to enforce the use of the Tile factory method.
     *
     * @param tileCoordinate The coordinate of the tile on the board (0-63).
     * @param pieceOnTile The piece that occupies this tile.
     */
    private OccupiedTile(final int tileCoordinate, final Piece pieceOnTile) {
      super(tileCoordinate);
      this.pieceOnTile = pieceOnTile;
    }

    /**
     * Returns a string representation of the occupied tile, which is the string
     * representation of the piece on this tile. White pieces are displayed in
     * uppercase, while black pieces are displayed in lowercase.
     *
     * @return A string representing the piece on this tile.
     */
    @Override
    public String toString() {
      return getPiece().getPieceAllegiance().isBlack() ? getPiece().toString().toLowerCase() : getPiece().toString();
    }

    /**
     * Indicates that this tile is occupied by a piece.
     *
     * @return true, as an occupied tile always has a piece.
     */
    @Override
    public boolean isTileOccupied() {
      return true;
    }

    /**
     * Returns the piece on this tile.
     *
     * @return The piece that occupies this tile.
     */
    @Override
    public Piece getPiece() {
      return this.pieceOnTile;
    }
  }
}