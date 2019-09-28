/**
 * 
 */
package generation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * This class has the responsibility to create a maze of given dimensions (width, height) 
 * together with a solution based on a distance matrix.
 * The MazeBuilder implements Runnable such that it can be run a separate thread.
 * The MazeFactory has a MazeBuilder and handles the thread management.   
 * 
 * The maze is built with Eller's algorithm. 
 * Every cell begins as its own set, and sets expand by joining with other adjoining sets.
 * Ultimately every cell in the maze is merged into one all-containing set. 
 * 
 * @author Elijah Mas
 */
public class MazeBuilderEller extends MazeBuilder implements Runnable {
	
	// tracks the sets that maze cells belong to during progression of Eller's algorithm
	private int[][] cells;
	
	/*
	cellSets:
	as sets of cells are created and subsequently merged, cellSets keeps track of them
	A HashSet allows for easy lookup, BUT
		Arrays.equals() works by identity, not value; so we cannot use int[]
		use List<Integer> instead
		as advised here: https://stackoverflow.com/questions/17606839/creating-a-set-of-arrays-in-java
	*/
	private HashMap<Integer,HashSet<List<Integer>>> cellSets;
	
	// keeps track of boundaries around rooms
	HashMap<Integer, ArrayList<OrientedWallBoard>> roomWalls;
	
	public MazeBuilderEller() {
		super();
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	public MazeBuilderEller(boolean det) {
		super(det);
		//System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze (deterministic enabled).");
		
	}
	
	protected int[][] retrieve_cells() {
		return cells;
	}
	
	/**
	 * <p> Overrides {@link generation.MazeBuilder#generatePathways()}; generates pathways into maze
	 * via wall removal by way of Eller's algorithm. Eller's algorithm follows these steps:
	 *   <ol>
	 *     <li> Initialize every cell in the maze:
	 *          <ul>
	 *            <li> to be in its own set, if not in a room </li>
	 *            <li> if in a room, in the same set with other cells in the room </li>
	 *          </ul>
	 *          The first row is the current row. </li>
	 *     <li> Randomly merge different sets in the current row by removing walls between them;
	 *          set cell values to be the same when two sets merge. </li>
	 *     <li> Randomly connect sets in the current row with the next row.
	 *          This involves joining cells with vertical neighbors and merging sets.
	 *          Each set in the current row must have at least one vertical connection to the new row. </li>
	 *     <li> Repeat steps 2-3 for succesive rows until the penultimate and last row are connected. </li>
	 *     <li> Finally, merge all sets in the last row by removing only those walls
	 *          which sit between cells of distinct sets. </li>
	 *   </ol>
	 * 
	 * One extra step is added: on the unlikely chance
	 * that there is still a closed room after the algorithm finishes,
	 * we check for rooms, and if one is found, we randomly remove one or two
	 * walls from its enclosing.
	 * </p>
	 * 
	 */
	@Override
	protected void generatePathways() {
		initializeCells();
		
		for(int rowIndex=0; rowIndex<width; rowIndex++) {
			// if first row, no previous row to link with
			if(rowIndex>0)link_CurrentRow_NewRow(rowIndex-1);
			wallRemovalFromRow(rowIndex);
		}
		
		handleLastRow();
		
		HashSet<Integer> roomValues = checkNoRooms();
		if(roomValues.size()!=0) {
			for(int v:roomValues) {
				ArrayList<OrientedWallBoard> roomBorders = roomWalls.get(v);
				
				// remove 2 walls if possible, otherwise just 1
				int removals = roomBorders.size()>1? 2 : 1;
				for(int i=0; i<removals; i++) {
					int index=SingleRandom.getRandom().nextIntWithinInterval(0, roomBorders.size()-1);
					attemptWallboardRemoval(roomBorders.get(index), false, true);
					roomBorders.remove(index);
				}
			}
		}
		
	}
	
	HashSet<Integer> checkNoRooms() {
		HashSet<Integer> roomValues = new HashSet<Integer>();
		for(int[] row: cells) {
			for(int y:row) {
				if(y<0) roomValues.add(y);
			}
		}
		
		return roomValues;
	}
	
	/**
	 * <p>
	 * Given two sets with reference to coordinates in {@code this.cells},
	 * combine the two sets by transferring all coordinates to one of the two sets
	 * and deleting the other set from {@code cellSets}.
	 * 
	 * Each set is identified by an integer; in most cases,
	 * the set that is kept is that which has the lower value of the two.
	 * The exception is if one set is a room; in that case, room cells
	 * are added to the set of the other cell, and the room set is removed.
	 * </p>
	 * 
	 * <p> This method does NOT check to see if the merge is valid,
	 * NOR does it perform wallboard removal--these must be handled externally.
	 * </p>
	 * @param firstCoordinate location of one cell
	 * @param secondCoordinate location of other cell
	 */
	private void mergeSets(int[] firstCoordinate, int[] secondCoordinate) {
		// get cell values and find the smaller of the two to serve as the receiving set
		// negative values indicate room sets, which are never the receiving sets
		
		int val1=getCellValue(firstCoordinate);
		int val2=getCellValue(secondCoordinate);
		
		int receiver, absorbed;
		if(val1<val2) {
			receiver=val1;
			absorbed=val2;
		}
		else {
			receiver=val2;
			absorbed=val1;
		}
		// case where one cell is in a room
		if(receiver<0) {
			int temp=receiver;
			receiver=absorbed;
			absorbed=temp;
		}
		
		// add all absorbed cells to the receiving set
		HashSet<List<Integer>> transferCells = cellSets.get(absorbed);
		cellSets.get(receiver).addAll(transferCells);
		for(List<Integer> cellList: transferCells) addValueAtCell(cellList, receiver);
		
		//don't have to remove cells from absorbed set, just have to drop reference to the set
		cellSets.remove(absorbed);
	}
	
	/**
	 * (wrapper for {@code mergeSets(int[], int[])} which allows for input of type {@code List<Integer>})
	 * 
	 * @param cell1 location of one cell
	 * @param cell2 location of other cell
	 */
	private void mergeSets(List<Integer> cell1, List<Integer> cell2) {
		mergeSets(new int[] {cell1.get(0),cell1.get(1)}, new int[] {cell2.get(0),cell2.get(1)});
	}
	
	/**
	 * Get the set (as integer) to which a maze cell belongs.
	 * 
	 * @param cell the coordinate of the cell
	 * @return the set of the cell
	 */
	private int getCellValue(int[] cell) {
		return cells[cell[0]][cell[1]];
	}
	
	/**
	 * Get the set (as integer) to which a maze cell belongs.
	 * 
	 * @param cell list containing {x,y}
	 * @return the set of the cell
	 */
	private int getCellValue(List<Integer> cell) {
		return cells[cell.get(0)][cell.get(1)];
	}
	
	/**
	 * Get the unique values (as a set) contained within a row.
	 * 
	 * Greatly simplified by the fact that making a Set of values
	 * automatically removes duplicates.
	 * 
	 * @param row an array of int values
	 * @return set of unique values in the row
	 */
	private static HashSet<Integer> getUniqueRowValues(int[] row){
		HashSet<Integer> a = new HashSet<Integer>();
		for(int i: row) a.add(i); //has no effect if i is already present
		return a;
	}
	
	/**
	 * Return all the wallboards (as VerticalWallBoard instances)
	 * positioned between the cells of a row, i.e. excluding the border cells.
	 * 
	 * @param rowIndex index of the row
	 * @return list of vertical wallboards in cell row
	 */
	ArrayList<VerticalWallBoard> getVerticalWallboardsInRow(int rowIndex){
		ArrayList<VerticalWallBoard> boards = new ArrayList<VerticalWallBoard>(height);
		
		// iterate over all row cells except last, with current y being left of wall, and y+1 right of wall
		// this has to effect of excluding the outermost borders of the maze
		for(int y=0; y<height-1; y++) {
			boards.add(new VerticalWallBoard(new int[] {rowIndex,y}, new int[] {rowIndex,y+1}, this));
		}
		return boards;
	}
	
	private void attemptWallboardRemoval(OrientedWallBoard wall, boolean considerBorder, boolean forceRemoval) {
		/* the decision process for removing the wall:
		 *     if forceRemoval, the wall will be removed if its surrounding cell-sets are distinct
		 *     otherwise the wall may be removed:
		 *         if a border wall, do not remove
		 *         if not a border, there is a 50% chance of removal
		*/
		if(!forceRemoval) {
			if(considerBorder && wall.isBorder()) {
				// we could use the lines below to probabilistically remove border walls
				// but as per use of roomWalls this is not required
				
				//int cancel = SingleRandom.getRandom().nextIntWithinInterval(0, 3);
				//if(0==cancel)
					return;
			}
			// 50% chance of removal
			else if(1==SingleRandom.getRandom().nextIntWithinInterval(0, 1)? true: false) return;
		}
		int[] back=wall.getBackCell(), forth=wall.getForthCell();
		if (getCellValue(back)!=getCellValue(forth)) {
			mergeSets(back, forth);
			wall.removeFromFloorplan();
		}
	}
	
	/**
	 * <p>
	 * Randomly remove vertical wallboards from {@code floorplan} in a row, predicated upon:
	 *     <ul>
	 *       <li> The walls must sit between cells of different sets </li>
	 *       <li> The walls must not be marked as borders </li>
	 *     </ul>
	 * </p>
	 * 
	 * <p>
	 * There are different ways to randomize wall removal. Two options are:
	 *   <ul>
	 *     <li> randomly decide how many walls to remove ahead of time,
	 *          and randomly remove walls until that number is reached. </li>
	 *     <li> visit each wall, and randomly decide (with some fixed probability)
	 *          to remove or keep that wall. </li>
	 *   </ul>
	 * </p>
	 *
	 * <p>
	 * This method makes use of the second approach.
	 * </p>
	 * @param rowIndex the index of the row
	 */
	private void wallRemovalFromRow(int rowIndex){
		ArrayList<VerticalWallBoard> boards = getVerticalWallboardsInRow(rowIndex);
		
		//make things more interesting by randomizing the order of iteration
		for(int index: IntStream.range(0, height-1).toArray()){
			VerticalWallBoard wall = boards.get(index);
			// wall removal is conducted probabilistically, border is considered
			attemptWallboardRemoval(wall, true, false);
		}
		
		for(VerticalWallBoard wall: boards) {
			int[] left = wall.getBackCell(), right = wall.getForthCell();
			if( cells[left[0]][left[1]] != cells[right[0]][right[1]] ) assert(true);
		}
	}
	
	/**
	 * Set the value of a given cell as the set to which it should belong;
	 * if a set does not already exist for that value, then first create it.
	 * 
	 * @param cell the cell at which to set the value
	 * @param setValue the set to which the cell will belong
	 */
	private void addValueAtCell(List<Integer> cell, int setValue) {
		HashSet<List<Integer>> s = cellSets.get(setValue);
		
		// if the set does not yet exist,
		// initialize it with <cell> as sole member and add it to cellSets
		if(null==s) {
			s=new HashSet<List<Integer>>();
			s.add(cell);
			cellSets.put(setValue, s);
		}
		else s.add(cell);
		
		cells[cell.get(0)][cell.get(1)]=setValue;
	}
		
	/**
	 * (wrapper for {@code addValueAtCell(List<Integer>)}, allows for input of type {@code int[]})
	 * 
	 * @param cell array containing {x,y}
	 * @param setValue the set to which the cell belongs
	 */
	private void addValueAtCell(int[] cell, int setValue) {
		List<Integer> cellList = Arrays.asList(cell[0],cell[1]);
		addValueAtCell(cellList, setValue);
	}
	
	/**
	 * get the maximum value of a row
	 * initialize
	 * @param row array of int values
	 * @return maximal value of the row
	 */
	static int rowMax(int[] row) {
		int max=row[0];
		for(int v: row) {
			if(v>max) max=v;
		}
		return max;
	}
	
	/**
	 * <p>
	 * Extend the sets of the current row into the following row,
	 * By randomly selecting locations where cells will merge vertically.
	 * 
	 * The following row is that in the positive x direction.
	 * </p>
	 * 
	 * <p>
	 * We again have ways to randomize this. Repeating the options from before:
	 *   <ul>
	 *     <li> randomly decide how many cells to join ahead of time,
	 *          and randomly do so until that number is reached. </li>
	 *     <li> visit each cell, and randomly decide (with some fixed probability)
	 *          to join the cell or leave it alone. </li>
	 *   </ul>
	 * </p>
	 * 
	 * This time we take a variant of the first approach: the number we arrive at
	 * is an upper bound on how many links we might establish.
	 * There is a reason for taking this approach: we must perform at least one extension
	 * for each set, and the semantics of performing this are simpler in the first approach.
	 * 
	 * @param currentRowIndex index of the current row
	 */
	void link_CurrentRow_NewRow(int currentRowIndex) {
		
		// iterate over sets overlapping current row
		for(int setValue: getUniqueRowValues(cells[currentRowIndex])) {
			HashSet<List<Integer>> extendable = new HashSet<List<Integer>>();
			
			//get all the cells in this set in this row
			for(int y=0; y<height; y++) {
				if(setValue==cells[currentRowIndex][y]) extendable.add(Arrays.asList(currentRowIndex,y));
			}
			int size=extendable.size();
			
			// randomly join up to half the cells in the set
			int count = (1==size)?
					1 : SingleRandom.getRandom().nextIntWithinInterval(1, (int)Math.ceil((double)size/2.0));
			
			//randomly select candidate walls for removal
			for(int i=0; i<count; i++) {
				@SuppressWarnings("unchecked")
				List<Integer> cell = (List<Integer>)getArbitraryValueFromSet(extendable, extendable.size());
				List<Integer> lowerCell = Arrays.asList(cell.get(0)+1, cell.get(1));
				HorizontalWallBoard wall = new HorizontalWallBoard(cell, lowerCell, this);
				
				// boolean flags indicate wall removal is forced (if valid), border is not considered
				attemptWallboardRemoval(wall, false, true);
				
				extendable.remove(cell);
			}
		}
	}

	/**
	 * Perform the final step of Eller's algorithm:
	 * remove all walls between cells of distinct sets last row.
	 */
	void handleLastRow() {
		ArrayList<VerticalWallBoard> boards = getVerticalWallboardsInRow(width-1);
		
		// iterate over all walls, no randomization involved
		for(VerticalWallBoard wall:boards) {
			int[] left=wall.getBackCell(), right=wall.getForthCell();
			
			attemptWallboardRemoval(wall,false, true);
		}
	}
	
	/**
	 * <p>Pull an arbitrary value from a given set.</p>
	 * 
	 * <p>Worded as "arbitrary" not "random" because random number generation is not invoked
	 * within this method: the method loops through the input set's iterator and
	 * pulls the object obtained at iteration {@code iters} (iterations start at 0).
	 * By default, the single-argument wrapper method sets {@code iters=0}.</p>
	 * 
	 * <p>"arbitrary" because the order of elements in general, the Set interface does not
	 * guarantee any particular ordering of elements.</p>
	 * 
	 * <p>Random number generation can be incorporated by passing a random integer
	 * generated elsewhere to the {@code iters} parameter.</p>
	 * 
	 * @param s the set of objects
	 * @param iters the position in the iteration at which to return
	 * @return an object from the set
	 */
	@SuppressWarnings("rawtypes")
	private Object retreiveArbitrarySetValue(Set s, int iters) {
		//initialize to null so compiler does not complain about uninitialized variable
		Object item=null;
		
		int count = 0;
		for(Object o: s) {
			item=o;
			if(iters==count++) return item; //increment count for each iteration
		}
		
		// compiler complains without this statement
		return item;
	}
	
	/**
	 * (wrapper around {@code retreiveArbitrarySetValue(Set s, int iters)}
	 * that sets default value of {@code iters} to 0)
	 * 
	 * @param s set
	 * @return object from the set
	 */
	@SuppressWarnings("rawtypes")
	private Object getArbitraryValueFromSet(Set s) {
		return retreiveArbitrarySetValue(s, 0);
	}
	
	/**
	 * (wrapper around {@code retreiveArbitrarySetValue(Set s, int iters)}
	 * that allows for variable value of {@code iters})
	 * 
	 * @param s set
	 * @return object from the set
	 */
	@SuppressWarnings("rawtypes")
	private Object getArbitraryValueFromSet(Set s, int iters) {
		return retreiveArbitrarySetValue(s, iters);
	}
	
	/**
	 * Return all the neighbors of a cell that are in the same room as the cell.
	 * This method will not be called before walls are removed,
	 * so it can use the property that rooms are initialized with walls bordering the room
	 * and no walls inside the room to select cell neighbors.
	 * 
	 * @param cell a cell, which must be in a room.
	 * @return the neighbors of this cell inside the cell's room
	 */
	private HashSet<List<Integer>> getCellRoomNeighbors(List<Integer> cell){
		HashSet<List<Integer>> list = new HashSet<List<Integer>>(4); //max # of neighbors
		
		int cX = cell.get(0), cY = cell.get(1);
		
		// check all directions
		for(CardinalDirection cd : CardinalDirection.values()) {
			int[] d = cd.getDirection();
			if(floorplan.hasNoWall(cX, cY, cd)) // true<-->neighbor shares room
				list.add((List<Integer>)Arrays.asList(cX+d[0],cY+d[1]));
		}
		return list;
	}
	
	/**
	 * Given a set of cells in a room,
	 * return a set all cells that are neighbors of these cells.
	 * The set will contain only unique cells, but this set may overlap
	 * with the original set of cells passed in as an argument,
	 * if any cells in that set were neighbors of each other.
	 * 
	 * @param cells a set of cells
	 * @return the set containing all neighbors of these cells
	 */
	HashSet<List<Integer>> getRoomNeighborsOfCells(Set<List<Integer>> cells){
		HashSet<List<Integer>> neighbors = new HashSet<List<Integer>>();
		
		// HashSet automatically prevents duplicates
		for(List<Integer> cell: cells) neighbors.addAll(getCellRoomNeighbors(cell));
		return neighbors;
	}
	
	/**
	 * Given an input cell,
	 * get all of the cells in the same room as this cell.
	 * 
	 * @param cell list containing {x,y}
	 * @return complete set of cells occupying the room this cell is in
	 */
	HashSet<List<Integer>> getCellsInRoom(List<Integer> cell){
		
		HashSet<List<Integer>> visited = new HashSet<List<Integer>>();
		visited.add(cell);
		HashSet<List<Integer>> newVisits = getCellRoomNeighbors(cell);
		
		/* how this works:
		 *     we are iterating over cells in the room by appending
		 *         neighbors of cells already discovered;
		 *     newVisits has the newest-discovered neighbor cells;
		 *     newVisits recurrently updates itself to be the set of
		 *         its neighbors that have not yet been visited
		 *     'visited' keeps track of visited cells
		 */
		while(newVisits.size()>0){
			visited.addAll(newVisits);
			newVisits=getRoomNeighborsOfCells(newVisits);
			newVisits.removeIf(visited::contains);
		}
		
		return visited;
	}
	
	/**
	 * Initialize the set values in the cells array so that
	 *     <ul>
	 *       <li> every cell not in a room belongs to its own set; </li>
	 *       <li> every room is delegated a unique set for its own cells. </li>
	 *     </ul>
	 */
	@SuppressWarnings("unchecked")
	void initializeCells() {
		cellSets = new HashMap<Integer,HashSet<List<Integer>>>();
		roomWalls = new HashMap<Integer,ArrayList<OrientedWallBoard>>();
		cells=new int[width][height];
		int count=1;
		int value;
		
		// every cell not in a room gets unique integer value
		// initially set all cells in rooms to 0
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				value = floorplan.isInRoom(x, y) ? 0 : count++;
				addValueAtCell(new int[] {x,y},value);
			}
		}
		
		HashSet<List<Integer>> set0 = cellSets.get(0);
		
		// case of no rooms
		if(null==set0) return;
		
		// now go back and distinguish cells in unique rooms
		// each room gets its own set, identified by a negative integer
		List<Integer> randomCell;
		int roomIndex=-1;
		while(set0.size()>0) {
			randomCell=(List<Integer>)getArbitraryValueFromSet(set0);
			for(List<Integer> cell: getCellsInRoom(randomCell)) {
				addValueAtCell(cell,roomIndex);
				set0.remove(cell);
			}
			
			//should wind up with 2*(width+height-1) cells
			ArrayList<OrientedWallBoard> walls = new ArrayList<OrientedWallBoard>(2*(width+height-1));
			roomWalls.put(roomIndex, walls);
			
			int rX,rY;
			int[] d;
			OrientedWallBoard wall=null;
			
			// add walls around the room to a set that can be accessed later
			// we only add non-border walls
			for(List<Integer> cell: cellSets.get(roomIndex)) {
				for(CardinalDirection cd: CardinalDirection.values()) {
					rX=cell.get(0);
					rY=cell.get(1);
					if(floorplan.hasWall(rX, rY, cd)) {
						//check every direction for wall
						d=cd.getDirection();
						int[] cell1=new int[] {rX,rY}, cell2=new int[] {rX+d[0],rY+d[1]};
						if(-1==d[0] && 0==d[1]) wall = new HorizontalWallBoard(cell2, cell1, this);
						else if(1==d[0] && 0==d[1]) wall = new HorizontalWallBoard(cell1, cell2, this);
						else if(0==d[0] && -1==d[1]) wall = new VerticalWallBoard(cell2, cell1, this);
						else if(0==d[0] && 1==d[1]) wall = new VerticalWallBoard(cell1, cell2, this);
						if(!wall.isBorder()) walls.add(wall);
					}
				}
			};
			
			roomIndex--;
		}
		cellSets.remove(0);
	}

}

/**
 * A wallboard that is oriented vertically, sitting between two cells in a row.
 * 
 * @author Elijah Mas
 *
 */
class VerticalWallBoard extends OrientedWallBoard{
	static final CardinalDirection cdBack = cdBackWithinRow;
	static final CardinalDirection cdForward = cdForwardWithinRow;
	
	/**
	 * Only constructor for the class. Sets left/right cell based on input values,
	 * and identifies the wallboard in the floorplan.
	 * Check that the input cells are valid (same y-value, x-values differ by 1).
	 * Defers to {@link #initialize(int[], int[], MazeBuilderEller)}.
	 * 
	 * @param cellBack cell to the wall's left
	 * @param cellForth cell to the wall's right
	 * @param builder {@code MazeBuilderEller} instance
	 */
	public VerticalWallBoard(int[] cellBack, int[] cellForth, MazeBuilderEller builder) {
		// assertion to make sure I know how Java's semantics work
		assert(cdBack==cdBackWithinRow && cdForward==cdForwardWithinRow);
		
		// a vertical wallboard must abut two cells in the same row, i.e. same x-values
		if(cellBack[0]!=cellForth[0]) throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have different x-values: %d, %d",cellBack[0],cellForth[0]));
		// y-values should differ by 1
		if(1!=cellForth[1]-cellBack[1]) throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have incompatible y-values: %d, %d",cellBack[1],cellForth[1]));
		
		initialize(cellBack, cellForth, builder);
	}
	
	/**
	 * For this class, {@code cellBack} and {@code cellForth} refer to
	 * cells forward/backward within row.
	 */
	@Override
	Wallboard makeWall(int[] cellBack, int[] cellForth) {
		return new Wallboard(cellBack[0],cellBack[1],cdForwardWithinRow);
	}
	
}


/**
 * A wallboard that is oriented horizontally, sitting between two cells in different rows.
 * 
 * @author Elijah Mas
 *
 */
class HorizontalWallBoard extends OrientedWallBoard{
	static final CardinalDirection cdBack = cdBackAcrossRows;
	static final CardinalDirection cdForward = cdForwardAcrossRows;
	
	/**
	 * Sets top/bottom cell based on input values,
	 * and identifies the wallboard in the floorplan.
	 * 
	 * @param cellBack cell above the wall
	 * @param cellForth cell beneath the wall
	 * @param builder {@code MazeBuilderEller} instance
	 */
	public HorizontalWallBoard(List<Integer> cellBack, List<Integer> cellForth, MazeBuilderEller builder) {
		init(new int[] {cellBack.get(0), cellBack.get(1)}, new int[] {cellForth.get(0),cellForth.get(1)}, builder);
	}
	
	/**
	 * Sets top/bottom cell based on input values,
	 * and identifies the wallboard in the floorplan.
	 * 
	 * @param cellBack cell above the wall
	 * @param cellForth cell beneath the wall
	 * @param builder {@code MazeBuilderEller} instance
	 */
	public HorizontalWallBoard(int[] cellBack, int[] cellForth, MazeBuilderEller builder) {
		init(cellBack, cellForth, builder);
	}
	
	/**
	 * For this class, {@code cellBack} and {@code cellForth} refer to
	 * cells forward/backward across rows.
	 */
	@Override
	Wallboard makeWall(int[] cellBack, int[] cellForth) {
		return new Wallboard(cellBack[0],cellBack[1],cdForwardAcrossRows);
	}
	
	/**
	 * Check that the input cells are valid (same y-value, x-values differ by 1).
	 * Defers to {@link #initialize(int[], int[], MazeBuilderEller)}.
	 * 
	 * @param cellBack cell above the wall
	 * @param cellForth cell beneath the wall
	 * @param builder {@code MazeBuilderEller} instance
	 */
	private void init(int[] cellBack, int[] cellForth, MazeBuilderEller builder) {
		// assertion to make sure I know how Java's semantics work
		assert(cdBack==cdBackAcrossRows && cdForward==cdForwardAcrossRows);
		
		//a horizontal wallboard must abut two cells in the same column, i.e. same y-values
		if(cellBack[1]!=cellForth[1]) throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have different y-values: %d, %d",cellBack[1],cellForth[1]));
		// x-values should differ by 1
		if(1!=cellForth[0]-cellBack[0]) throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have incompatible x-values: %d, %d",cellBack[0],cellForth[0]));
		
		initialize(cellBack, cellForth, builder);
	}
	
}


/**
 * Wrapper around (not subclass of) the {@code Wallboard} class that allows
 * for convenient access of cells adjoining the wallboard
 * and location/removal of the wallboard in the floorplan.
 * 
 * Abstract class that is meant to be instantiated in the
 * {@link generation.VerticalWallBoard} or {@link generation.HorizontalWallBoard} classes.
 * 
 * @author Elijah Mas
 *
 */
abstract class OrientedWallBoard{
	// here for clarity
	static final CardinalDirection cdBackWithinRow = CardinalDirection.getDirection(0, -1);
	static final CardinalDirection cdForwardWithinRow= CardinalDirection.getDirection(0, 1);
	static final CardinalDirection cdBackAcrossRows = CardinalDirection.getDirection(-1, 0);
	static final CardinalDirection cdForwardAcrossRows= CardinalDirection.getDirection(1, 0);
	
	static CardinalDirection cdBack, cdForward;
	
	private Wallboard wall; // the referent wall (wallboard)
	private int[] cellBack; // cell to the left of the wall
	private int[] cellForth; // cell to the right of the wall
	private Floorplan floorplan; // floorplan in which the wall is embedded
	private MazeBuilderEller builder;
	
	abstract Wallboard makeWall(int[] cellBack, int[] cellForth);
	
	
	void initialize(int[] cellBack, int[] cellForth, MazeBuilderEller builder) {
		this.wall = makeWall(cellBack,cellForth);//new Wallboard(cellBack[0],cellBack[1],cdForwardVertical);
		this.cellBack=cellBack;
		this.cellForth=cellForth;
		this.floorplan=builder.floorplan;
		this.builder=builder;
	}
	
	/**
	 * return the cell to the wall's left
	 * @return {x, y} of leftward cell
	 */
	public int[] getBackCell() {
		return cellBack;
	}
	
	/**
	 * return the cell to the wall's right
	 * @return {x, y} of rightward cell
	 */
	public int[] getForthCell() {
		return cellForth;
	}
	
	/**
	 * remove the wall from the floorplan, simple wrapper around {@code floorplan.deleteWallboard}
	 */
	public void removeFromFloorplan() {
		floorplan.deleteWallboard(wall);
	}
	
	/**
	 * Test whether the wall is a border, simpler wrapper around {@code floorplan.isPartOfBorder}
	 * 
	 * @return boolean:{the wall is a border}
	 */
	public boolean isBorder() {
		return floorplan.isPartOfBorder(wall);
	}
	
	/**
	 * convenient string representation of wallboard: display its neighbors.
	 * 
	 * @return string displaying neighbor cells
	 */
	@Override
	public String toString() {
		return String.format("%s <--> %s", Arrays.toString(cellBack), Arrays.toString(cellForth));
	}
}
