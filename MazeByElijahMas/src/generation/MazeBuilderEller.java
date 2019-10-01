/**
 * 
 */
package generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * <p>This class has the responsibility to create a maze of given dimensions (width, height) 
 * together with a solution based on a distance matrix.
 * The MazeBuilder implements Runnable such that it can be run a separate thread.
 * The MazeFactory has a MazeBuilder and handles the thread management.</p>
 * 
 * <p>The maze is built with Eller's algorithm. 
 * Every cell begins as its own set, and sets expand by joining with other adjoining sets.
 * Ultimately every cell in the maze is merged into one all-containing set.</p>
 * 
 * <p>Note: some methods are defined herein that may be used in the future,
 * but which are not currently used,; this may affect the coverage rate.</p>
 * 
 * <p><b>Warning</b> for whatever reasons, there is a long delay for MazeBuilderEller
 * (few minutes? I have not timed it precisely) between the time when the MazeApplication
 * switches to the generating screen and the time when the maze update progress counter
 * moves off of 0% for a maze at level=F. The delay for level E is maybe 10-20 seconds
 * on my computer.
 * 
 * @author Elijah Mas
 */
public class MazeBuilderEller extends MazeBuilder implements Runnable {
	
	/*	----	----		FIELDS		----	----	*/
	//--------------------------------------------------//
	/**
	 * tracks the sets that maze cells belong to during progression of Eller's algorithm
	 */
	private int[][] cells;
	
	/**
	 * <p> As sets of cells are created and subsequently merged, cellSets keeps track of them.</p>
	 * <p>A HashSet allows for easy lookup, but
	 * Arrays.equals() works by identity, not value; so we cannot use int[]
	 * &#62;&#62; solution: use {@code List&#60;Integer&#62;} instead,
	 * as advised here: https://stackoverflow.com/questions/17606839
	 * </p>
	*/
	private HashMap<Integer,HashSet<List<Integer>>> cellSets;
	
	/**
	 * keeps track of boundaries around rooms
	 */
	HashMap<Integer, ArrayList<OrientedWallBoard>> roomWalls;
	
	/**
	 * Method that allows some of the more expensive in-line assertion tests
	 * to run. Note that some tests will run regardless of the value of
	 * this flag.
	 */
	protected static boolean ENABLE_TESTS=false;
	
	/** 
	 * ignoreRooms:
	 * 
	 * this is only used for testing purposes
	 * 
	 * say we want to test what happens if Eller's algorithm finishes,
	 * but by chance leaves a room with all walls standing
	 * 
	 * this is not very likely to happen by chance,
	 * so this flag is used to ensure it happens,
	 * so that the relevant functionality can be tested
	*/
	boolean ignoreRooms = false;
	
	/*	----	----	CONSTRUCTORS	----	----	*/
	//--------------------------------------------------//
	public MazeBuilderEller() {
		super();
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	public MazeBuilderEller(boolean det) {
		super(det);
		//System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze (deterministic enabled).");
		
	}
	
	/*	----	----	GETTERS/SETTERS	----	----	*/
	//--------------------------------------------------//
	/**
	 * Get the cells of set values for all coordinates in the maze. Used for testing.
	 * @return int[][] array holding set values.
	 */
	protected int[][] retrieve_cells() {
		return cells;
	}
	
	/**
	 * Get the HashMap that contains references to set ids (keys) and
	 * sets of cells that belong to these set ids (values)
	 * @return {@code HashMap<Integer,HashSet<List<Integer>>>} providing cell sets
	 */
	protected HashMap retrieve_cellSets() {
		return cellSets;
	}
	
	/**
	 * Toggle ignoreRooms=true externally; only used in testing.
	 */
	void setToIgnoreRooms() {
		ignoreRooms=true;
	}
	
	/*	--	--	--	--	COMPUTATION METHODS	--	--	--	--	*/
	//------------------------------------------------------//
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
		
		// we can afford to do these once in beginning
		assert testNoDuplicateCells();
		assert testNoIsolatedCells();
		assert test_cells_cellSets_Agree();
		
		for(int rowIndex=0; rowIndex<width; rowIndex++) {
			// if first row, no previous row to link with
			if(rowIndex>0) {
				link_CurrentRow_NewRow(rowIndex-1);
			}
			wallRemovalFromRow(rowIndex);
		}
		
		handleLastRow();
		
		// if we forget to reset ignoreRooms, the while loop is infinite,
		// as we are not allowed to open rooms (sets with value < 0)
		ignoreRooms=false;
		while(true) {
			Set<Integer> roomValues = new HashSet<Integer>(cellSets.keySet());
			roomValues.remove(1);
			if(0==roomValues.size()) break;
			int set = (int)getArbitraryValueFromSet(roomValues);
			ArrayList<OrientedWallBoard> roomBorders = getRemovableWallsOfGroup(set);
			
			// remove 2 walls if possible, otherwise just 1
			int removals = roomBorders.size()>1? 2 : 1;
			for(int i=0; i<removals; i++) {
				int index=SingleRandom.getRandom().nextIntWithinInterval(0, roomBorders.size()-1);
				attemptWallboardRemoval(roomBorders.get(index), false, true);
				roomBorders.remove(index);
			}
		}
		
		// we can also afford to do these once in end
		assert testNoDuplicateCells();
		assert testNoIsolatedCells();
		assert test_cells_cellSets_Agree();
		
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
			//if(ignoreRooms) return;
			int temp=receiver;
			receiver=absorbed;
			absorbed=temp;
		}
		// room cells cannot be receivers by design choice
		assert receiver>0;
		
		// add all absorbed cells to the receiving set
		HashSet<List<Integer>> transferCells = cellSets.get(absorbed);
		cellSets.get(receiver).addAll(transferCells);
		for(List<Integer> cellList: transferCells) addValueAtCell(cellList, receiver);
		
		//don't have to remove cells from absorbed set, just have to drop reference to the set
		cellSets.remove(absorbed);
		
		//expensive, but for testing we will be sure this is checked as often as possible
		if(ENABLE_TESTS) assert test_cells_cellSets_Agree();
	}
	
	/**
	 * <p>Wrapper for {@code mergeSets(int[], int[])} which allows for input of type {@code List<Integer>}</p>
	 * 
	 * <p>Here for potential future use; not currently used.</p>
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
		
		// assert no duplicate sets
		if(ENABLE_TESTS) {
			ArrayList<Integer> values = new ArrayList<Integer>(a);
			for(int i=0; i<values.size()-1; i++)
				assert(values.get(i)!=values.get(i+1));
		}
		
		return a;
	}
	
	/**
	 * Test that {@code cells} and {@code cellSets}
	 * are in agreement: that is,
	 * the information contained in each
	 * does not contradict the other.
	 * 
	 * @return true if information if the two sets contain identical information
	 */
	private boolean test_cells_cellSets_Agree() {
		// verify that every value is cells
		// agrees with the value indicated in cellSets
		for(int setValue: cellSets.keySet()) {
			for(List<Integer> cell: cellSets.get(setValue)) {
				if(getCellValue(cell)!=setValue) return false;
			}
		}
		
		// verify that each set in cellSets contains the expected values
		// as predicated by the values in cells
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(!cellSets.get(cells[x][y]).contains(Arrays.asList(x,y))) return false;
			}
		}
		
		return true;
	}
	
	/**
	 * <p> Test that no sets have duplicate cells.</p>
	 * 
	 * <p> How this works:
	 *   <ol>
	 *     <li> for every set of cells A,
	 *          get the set S which contains all the sets that are not A;</li>
	 *     <li> for every set B in A, let A = intersect(B, A);</li>
	 *     <li> if A's size has changed over, the test has failed (somewhwhere along
	 *          the way, a set in S had elements in common with A).</li>
	 *   </ol>
	 * </p>
	 * 
	 * <p><b>***Expected result:</b> no two sets share any cells,
	 * meaning case 2 above holds for all comparison set pairs.</p>
	 * 
	 * I have optimized this method to the extent possible;
	 * it gets expensive when called very frequently.
	 * 
	 * @return boolean:{no sets have duplicate cells}
	 */
	private boolean testNoDuplicateCells() {
		HashSet<List<Integer>> setA;
		
		// don't iterate over original
		// the .keySet() method is backed by the HashSet,
		// do not want to chance mutating it while iterating
		ArrayList<Integer> setValues = new ArrayList<Integer>(cellSets.keySet());
		
		int size=setValues.size(), v1, v2;
		
		/*
		//a more expensive way of doing this
		
		for(int i=0; i<size-1; i++) {
			v1=setValues.get(i);
			for(int j=i+1; j<size; j++) {
				v2=setValues.get(j);
				setA = new HashSet<List<Integer>>(cellSets.get(v1));
				setA.retainAll(cellSets.get(v2));
				if(0!=setA.size()) return false;
			}
		}
		*/
		
		for(int i=0; i<size; i++) {
			v1=setValues.get(i);
			setA = new HashSet<List<Integer>>(cellSets.get(v1));
			int initialSize=setA.size();
			for(int j=0; j<size; j++) {
				if(i==j) continue;
				setA.removeAll(cellSets.get(setValues.get(j)));
			}
			int newSize=setA.size();
			if(initialSize!=newSize) return false;
		}
		
		return true;
	}
	
	/**
	 * <p>Verify that there are no isolated cells: i.e.,
	 * every cell that is part of a set can be reached
	 * from every other cell in that set.</p>
	 * 
	 * <p>It is sufficient to choose one cell at random
	 * and then walk to its reachable neighbors
	 * in the set; if any cells are disconnected, this test fails.</p>
	 * 
	 * @return boolean: true if the specified test is successful
	 */
	private boolean testNoIsolatedCells() {
		for(int setValue:cellSets.keySet()) {
			
			// get a random cell in this set,
			// walk to every other cell in its set that can be reached,
			// put into a new set
			HashSet<List<Integer>> cells = getCellsInRoom(
				(List<Integer>) getArbitraryValueFromSet(cellSets.get(setValue))
			);
			
			// get all the cells in the maze that identify this set value
			HashSet<List<Integer>> cells2 = new HashSet<List<Integer>>(cells.size());
			for(int x=0; x<width; x++) {
				for(int y=0; y<height; y++) {
					if(setValue==getCellValue(new int[] {x,y})) cells2.add(Arrays.asList(x,y));
				}
			}
			
			// now check that both sets are equal
			// as per docs, .equals method tests for equality, not identity
			// https://docs.oracle.com/javase/8/docs/api/java/util/AbstractList.html#equals-java.lang.Object-
			if(!cells.equals(cells2)) return false;
		}
		return true;
	}
	
	/**
	 * Return all the wallboards (as VerticalWallBoard instances)
	 * positioned between the cells of a row, i.e. excluding the borders
	 * on the maze periphery.
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
	
	/**
	 * Attempt to remove a wallboard from the floorplan, whether probabilistically
	 * or enforced.
	 * 
	 * @param wall the OrientedWallBoard instance to be removed
	 * @param considerBorder if true, borders will not be removed
	 * @param forceRemoval if true, remove the wall if it is between cells of different sets;
	 *                     if false, the wall may or may not be removed
	 */
	private void attemptWallboardRemoval(OrientedWallBoard wall, boolean considerBorder, boolean forceRemoval) {
		/* the decision process for removing the wall:
		 *     if forceRemoval, the wall will be removed if its surrounding cell-sets are distinct
		 *     otherwise the wall may be removed:
		 *         if a border wall, do not remove
		 *         if not a border, there is a 50% chance of removal
		*/
		if(ignoreRooms && wall.hasRoomNeighbor()) return;
		
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
	@SuppressWarnings("unchecked")
	private void wallRemovalFromRow(int rowIndex){
		ArrayList<VerticalWallBoard> boards = getVerticalWallboardsInRow(rowIndex);
		ArrayList<int[]> initialSetValues = new ArrayList<int[]>(boards.size());
		for(VerticalWallBoard wall: boards) {
			initialSetValues.add(new int[] {
					getCellValue(wall.getBackCell()),
					getCellValue(wall.getForthCell())
			});
		}
		
		//make things more interesting by randomizing the order of iteration
		for(int index: IntStream.range(0, height-1).toArray()){
			VerticalWallBoard wall = boards.get(index);
			// wall removal is conducted probabilistically, border is considered
			attemptWallboardRemoval(wall, true, false);
		}
		
		for(VerticalWallBoard wall: boards) {
			int[] left = wall.getBackCell(), right = wall.getForthCell();
			
			// if the cells are in in different sets, there must be a separating wall
			if( getCellValue(left) != getCellValue(right) ) assert wall.isPresent();
			
			// I have not found an analogously simple assertion
			// (one that involves nothing more than checking the presence of walls)
			// for the case where the cells are in the same set;
			// for this we have the test below outside this loop
		}
		
		
		if(ENABLE_TESTS) {
			//System.out.println("performing duplicate test at row "+rowIndex);
			assert testNoIsolatedCells();
			assert testNoDuplicateCells();
		}
	}
	
	/*
	
	//not currently used
	private int countSetOccurrence(int setValue) {
		int count=0;
		for(int[] row: cells) {
			for(int v: row) {
				if(v==setValue) count++;
			}
		}
		return count;
	}
	*/
	
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
	 * Wrapper for {@code addValueAtCell(List<Integer>)}, allows for input of type {@code int[]}.
	 * 
	 * @param cell array containing {x,y}
	 * @param setValue the set to which the cell belongs
	 */
	private void addValueAtCell(int[] cell, int setValue) {
		List<Integer> cellList = Arrays.asList(cell[0],cell[1]);
		addValueAtCell(cellList, setValue);
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
		
		if(ENABLE_TESTS) {
			//System.out.println("performing duplicate test at row "+currentRowIndex);
			assert testNoDuplicateCells();
			assert testNoIsolatedCells();
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
	 * Given a group of cells that belong to a set,
	 * get the walls surrounding this set that are removable
	 * (i.e., not borders)
	 * @param set the integer value identifying the set
	 * @return the removable walls around this set
	 */
	private ArrayList<OrientedWallBoard> getRemovableWallsOfGroup(int set) {
		ArrayList<OrientedWallBoard> walls = new ArrayList<OrientedWallBoard>();
		
		int rX,rY;
		int[] d;
		OrientedWallBoard wall=null;
		
		// add walls around the room to a set that can be accessed later
		// we only add non-border walls
		for(List<Integer> cell: cellSets.get(set)) {
			for(CardinalDirection cd: CardinalDirection.values()) {
				rX=cell.get(0);
				rY=cell.get(1);
				if(floorplan.hasWall(rX, rY, cd)) {
					//check every direction for wall
					d=cd.getDirection();
					int[] cell1=new int[] {rX,rY}, cell2=new int[] {rX+d[0],rY+d[1]};
					
					// reject wall on maze periphery
					// it is true that we check for a border a few lines down;
					// but that check will throw an exception if cell2 is out of bounds
					if(cell2[0]<0 || cell2[0]>width || cell2[1]<0 || cell2[1]>height) continue;
					
					if(-1==d[0] && 0==d[1]) wall = new HorizontalWallBoard(cell2, cell1, this);
					else if(1==d[0] && 0==d[1]) wall = new HorizontalWallBoard(cell1, cell2, this);
					else if(0==d[0] && -1==d[1]) wall = new VerticalWallBoard(cell2, cell1, this);
					else if(0==d[0] && 1==d[1]) wall = new VerticalWallBoard(cell1, cell2, this);
					
					if(!wall.isBorder())
						walls.add(wall);
				}
			}
		}
		
		return walls;
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
	
	public boolean isPresent() {
		return floorplan.hasWall(cellBack[0], cellBack[1], cdForward);
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
		if(cellBack[1]!=cellForth[1])
			throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have different y-values: %d, %d",cellBack[1],cellForth[1]));
		// x-values should differ by 1
		if(1!=cellForth[0]-cellBack[0])
			throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have incompatible x-values: %d, %d",cellBack[0],cellForth[0]));
		
		initialize(cellBack, cellForth, builder);
	}
	
	public boolean isPresent() {
		return floorplan.hasWall(cellBack[0], cellBack[1], cdForward);
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
	protected int[] cellBack; // cell to the left of the wall
	protected int[] cellForth; // cell to the right of the wall
	protected Floorplan floorplan; // floorplan in which the wall is embedded
	private MazeBuilderEller builder;
	
	abstract Wallboard makeWall(int[] cellBack, int[] cellForth);
	abstract boolean isPresent();
	
	
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
	 * Test whether one of the wall's neighbors is in a room
	 * @return boolean:{wall has room neighbor}
	 */
	public boolean hasRoomNeighbor() {
		return
			(	floorplan.isInRoom(cellBack[0], cellBack[1])  ||
				floorplan.isInRoom(cellForth[0], cellForth[1])  );
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
