package engine.forBoard;


import com.google.common.collect.ImmutableMap;
import engine.forPiece.Piece;

import java.util.HashMap;
import java.util.Map;


/**
 * The `Tile` class represents a tile on a chess board.
 * <br><br>
 * It can be either an empty tile or an occupied tile.
 * Subclasses of `Tile` include `EmptyTile` and `OccupiedTile`.
 *
 * @author Aaron Ho
 */
public abstract class Tile {

  /*** The coordinate of the tile on the chess board. */
  protected final int tileCoordinate;

  /*** Cache containing all possible empty tiles on the chess board. */
  private static final Map<Integer, EmptyTile> EMPTY_TILES_CACHE = createAllPossibleEmptyTiles();
  
  /**
   * Returns an ImmutableMap of Integer to EmptyTile, that details
   * the positions of all possible EmptyTiles on a chess board
   * @return an ImmutableMap of Integer to EmptyTile
   */
  private static Map<Integer, EmptyTile> createAllPossibleEmptyTiles() {
    final Map<Integer, EmptyTile> emptyTileMap = new HashMap<>();
    for(int i = 0; i < BoardUtils.NUM_TILES; i++) {
      emptyTileMap.put(i, new EmptyTile(i));
    }
    return ImmutableMap.copyOf(emptyTileMap);
  }
  
  /**
   * A factory method that allows the creation of
   * the package private Tile object
   */
  public static Tile createTile(final int tileCoordinate, final Piece piece) {
    return piece != null ? new OccupiedTile(tileCoordinate, piece) : EMPTY_TILES_CACHE.get(tileCoordinate);
  }
  
  /**
   * Will create a Tile object on tileCoordinate
   * @param tileCoordinate the desired coordinate of the Tile
   */
  private Tile(final int tileCoordinate) {
    this.tileCoordinate = tileCoordinate;
  }
  
  /**
   * Will return if a given Tile is occupied or not
   * @return true or false
   */
  public abstract boolean isTileOccupied();
  
  /**
   * If there is a piece on this Tile will return the Piece.
   * If not, will return null;
   * @return the Piece or null
   */
  public abstract Piece getPiece();
  
  /**
   * Will return the tile coordinate
   * @return the tile coordinate
   */
  public int getTileCoordinate() {
    return this.tileCoordinate;
  }
  
  /**
   * The EmptyTile class represents an empty tile on a chess board.
   * It is a subclass of the abstract Tile class.
   * <br><br>
   * The EmptyTile class represents an empty tile on a chess board.
   * Empty tiles are not occupied by any piece.
   * The class provides methods to retrieve the tile coordinate, check if the tile is occupied, and get the piece on the tile.
   * This class should not be instantiated directly but rather accessed through the parent Tile class.
   *
   * @author Aaron Ho
   */
  public static final class EmptyTile extends Tile {
    
    /**
     * The default constructor for an empty Tile.
     * A subclass of the abstract Tile class
     * @param coordinate the coordinate of the empty Tile
     */
    private EmptyTile(final int coordinate) {
      super(coordinate);
    }
  
    /**
     * Will return "-" because an empty tile has
     * no Piece on it, so it is represented by a "-"
     * @return "-"
     */
    @Override
    public String toString() {
      return "-";
    }
  
    /**
     * Returns false
     * @return false
     */
    @Override
    public boolean isTileOccupied() {
      return false;
    }
  
    /**
     * Returns null because empty Tile has no Piece
     * @return null
     */
    @Override
    public Piece getPiece() {
      return null;
    }
  }
  
  /**
   * The OccupiedTile class represents an occupied tile on a chess board.
   * It is a subclass of the abstract Tile class.
   * <br><br>
   * The OccupiedTile class represents an occupied tile on a chess board.
   * Occupied tiles are occupied by a chess piece.
   * The class provides methods to retrieve the tile coordinate, check if the tile is occupied, and get the piece on the tile.
   * This class should not be instantiated directly but rather accessed through the parent Tile class.
   *
   * @author Aaron Ho
   */
  public static final class OccupiedTile extends Tile {
    private final Piece pieceOnTile;
  
    /**
     * A constructor for an OccupiedTile. Will create a Tile object
     * that is occupied by a Piece
     * @param tileCoordinate the coordinate of the Tile
     */
    private OccupiedTile(final int tileCoordinate, final Piece pieceOnTile) {
      super(tileCoordinate);
      this.pieceOnTile = pieceOnTile;
    }
    
    /**
     * A toString method that will allow us
     * to print the Piece that calls this method
     * @return the Piece in String form
     */
    @Override
    public String toString() {
      return getPiece().getPieceAllegiance().isBlack() ? getPiece().toString().toLowerCase() : getPiece().toString();
    }
  
    /**
     * Returns true, because by default an OccupiedTile
     * is occupied by a Piece
     * @return true
     */
    @Override
    public boolean isTileOccupied() {
      return true;
    }
  
    /**
     * Returns the piece on the OccupiedTile
     * @return the piece on the OccupiedTile
     */
    @Override
    public Piece getPiece() {
      return this.pieceOnTile;
    }
  }
}
