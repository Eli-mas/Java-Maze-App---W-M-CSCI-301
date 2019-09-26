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
 * @author HomeFolder
 *
 */
public class MazeBuilderEller extends MazeBuilder implements Runnable {
	
	private int[][] cells;
	
	// use List<Integer> as opposed to int[], as Arrays.equals() works by identity, not value
	// as advised here: https://stackoverflow.com/questions/17606839/creating-a-set-of-arrays-in-java
	private HashMap<Integer,HashSet<List<Integer>>> cellSets;
	
	public MazeBuilderEller() {
		super();
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	public MazeBuilderEller(boolean det) {
		super(det);
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze (deterministic enabled).");
		
	}
	
	/**
	 * Given two sets with reference to coordinates in <this.cells>,
	 * combine the two sets by transferring all coordinates to one of the two sets
	 * and deleting the other set.
	 * 
	 * Each set is identified by a number;
	 * the set that is kept is that which has the lower value of the two
	 * @param firstCoordinate 
	 * @param secondCoordinate
	 */
	private void mergeSets(int[] firstCoordinate, int[] secondCoordinate) {
		int val1=getCellValue(firstCoordinate);//cells[firstCoordinate[0]][firstCoordinate[1]]
		int val2=getCellValue(secondCoordinate);//cells[secondCoordinate[0]][secondCoordinate[1]]
		int minVal, maxVal;
		if(val1<val2) {
			minVal=val1;
			maxVal=val2;
		}
		else {
			minVal=val2;
			maxVal=val1;
		}
		if(minVal<0) {
			int temp=minVal;
			minVal=maxVal;
			maxVal=temp;
		}
		
		//as per documentation, addAll creates a union of two sets
		HashSet<List<Integer>> maxCells = cellSets.get(maxVal);
		cellSets.get(minVal).addAll(maxCells);
		for(List<Integer> cellList: maxCells) addValueAtCell(cellList, minVal);
		cellSets.remove(maxVal);
	}
	
	private void mergeSets(List<Integer> cell1, List<Integer> cell2) {
		mergeSets(new int[] {cell1.get(0),cell1.get(1)}, new int[] {cell2.get(0),cell2.get(1)});
	}
	
	private int getCellValue(int[] cell) {
		return cells[cell[0]][cell[1]];
	}
	
	private int getCellValue(List<Integer> cell) {
		return cells[cell.get(0)][cell.get(1)];
	}
	
	private HashSet<Integer> getUniqueRowValues(int[] row){
		HashSet<Integer> a = new HashSet<Integer>();
		for(int i: row) a.add(i);
		return a;
	}
	
	/*
	private HashSet<Integer> getUniqueValues(int[][] array){
		HashSet<Integer> a = new HashSet<Integer>();
		for(int[] row:array) {
			for(int i: row) a.add(i);
		}
		return a;
	}
	
	private int getUniqueCount(int[] row) {
		return getUniqueRowValues(row).size();
	}
	
	private int getUniqueCount(int[][] array) {
		return getUniqueValues(array).size();
	}
	*/
	
	ArrayList<VerticalWallBoard> getWallboardsInRow(int[] row, int rowIndex){
		ArrayList<VerticalWallBoard> boards = new ArrayList<VerticalWallBoard>();
		
		for(int i=0; i<row.length-1; i++) {
			boards.add(new VerticalWallBoard(new int[] {rowIndex,i}, new int[] {rowIndex,i+1}, floorplan));
		}
		return boards;
	}
	
	private void wallRemovalFromRow(int[] row, int rowIndex){
		
		//boards={list of wallboards in newRow}
		ArrayList<VerticalWallBoard> boards = getWallboardsInRow(row, rowIndex);
		
		/*
			-->randomly choose how many sets we want after removals, set to finalCount
		*/
		
		int numberOfPossibleWallRemovals=0;
		for(VerticalWallBoard wall: boards) {
			if (getCellValue(wall.getLeft())!=getCellValue(wall.getRight()) && !wall.isBorder()) numberOfPossibleWallRemovals++;
		}
		int low =  (int)Math.round(Math.sqrt(numberOfPossibleWallRemovals));
		int high = (int)Math.round(Math.pow( numberOfPossibleWallRemovals,0.75));
		int finalCount = (low<high) ? SingleRandom.getRandom().nextIntWithinInterval(low, high) : low;
		
		int removals=0;
		
		while(true) {
			
			//int unique=getUniqueCount(newRow);
			//if(unique<=finalCount) break;
			
			
			/*
				select a wallboard at random
				get the neighboring cells (left/right) of this wallboard
			*/
			int index = SingleRandom.getRandom().nextIntWithinInterval(0, boards.size()-1);
			VerticalWallBoard wall = boards.get(index);
			int[] left=wall.getLeft(), right=wall.getRight();
			
			/*
				if these cells are of different sets and the wall is not marked as non-removable:
					remove the wallboard from floorplan
					set all elements in both sets to the same value (the lower value of the two sets)
			*/
			boolean remove = 1==SingleRandom.getRandom().nextIntWithinInterval(0, 1)? true: false;
			if (remove && getCellValue(left)!=getCellValue(right) && !wall.isBorder()) {
				mergeSets(left, right);
				wall.removeFromFloorplan();
				removals++;
			};
			
			boards.remove(index);
			if(0==boards.size()) break;
			//if(removals>=finalCount || 0==boards.size()) break;
		}
	}
	
	private void addValueAtCell(List<Integer> cellList, int setValue) {
		HashSet<List<Integer>> s = cellSets.get(setValue);
		if(null==s) {
			s=new HashSet<List<Integer>>();
			s.add(cellList);
			cellSets.put(setValue, s);
		}
		else s.add(cellList);
		
		cells[cellList.get(0)][cellList.get(1)]=setValue;
	}
		
	private void addValueAtCell(int[] cell, int setValue) {
		List<Integer> cellList = Arrays.asList(cell[0],cell[1]);
		addValueAtCell(cellList, setValue);
	}
	
	int rowMax(int[] row) {
		int max=-1;
		for(int v: row) {
			if(v>max) max=v;
		}
		return max;
	}
	
	//void initializeRow(Object currentRow, int[] newRow){
	//	int start = (null==currentRow)? -1 : rowMax((int[])currentRow);
	//	for(int i=0; i<height; i++) addValueAtCell(new int[] {0,i},i+start+1);
	//}
	
	void link_CurrentRow_NewRow(int[] currentRow, int[] newRow, int currentRowIndex) {
		
		/*
		 * for each set overlapping the currentRow:
		 */
		for(int setValue: getUniqueRowValues(currentRow)) {
			HashSet<List<Integer>> extendable = new HashSet<List<Integer>>(); // = cells in currentRow belonging to this set
			for(int y=0; y<height; y++) {
				if(setValue==cells[currentRowIndex][y]) extendable.add(Arrays.asList(currentRowIndex,y));
			}
			int size=extendable.size();
			int count = (1==size)?
					1 : SingleRandom.getRandom().nextIntWithinInterval(1, (int)Math.ceil((double)size/2.0));
			HashSet<List<Integer>> nonExtendable = new HashSet<List<Integer>>(count);
			
			for(int i=0; i<count; i++) {
				 /*
				  * cell = random cell from extendable (array of two integers)
				  * set cell's lower neighbor to cell's value
				  */
				List<Integer> cell = (List<Integer>)getArbitraryValueFromSet(extendable, size);
				List<Integer> lowerCell = Arrays.asList(cell.get(0)+1, cell.get(1));
				
				//this conditional prevents attempt to join cells sharing a room
				if(getCellValue(cell)!=getCellValue(lowerCell)) {
					//the lower cell could belong to a larger set than itself if in a room
					//thus call mergeSets instead of addValueAtCell
					mergeSets(cell, lowerCell);
					floorplan.deleteWallboard(new Wallboard(cell.get(0), cell.get(1), CardinalDirection.getDirection(1, 0)));
				}
				extendable.remove(cell);
				nonExtendable.add(cell);
				size=extendable.size();
			}
		}
		
		
		
	}

	/**
	 * last row: remove all walls between cells of distinct sets
	 */
	void handleLastRow() {
		ArrayList<VerticalWallBoard> boards = getWallboardsInRow(cells[width-1], width-1);
		for(VerticalWallBoard wall:boards) {
			int[] left=wall.getLeft(), right=wall.getRight();
			
			//checks that cells are in different sets and wall is removable
			if (getCellValue(left)!=getCellValue(right) && !wall.isBorder()) {
				mergeSets(left, right);
				wall.removeFromFloorplan();
			};
		}
	}
	
	@SuppressWarnings("rawtypes")
	private Object retreiveArbitrarySetValue(Set s, int iters) {
		//initialize to null so compiler does not complain about uninitialized variable
		Object item=null;
		
		int count = 0;
		for(Object o: s) {
			item=o;
			if(iters==count++) return item; //count increments for each iteration
		}
		
		// we could just say "return o" inside the loop
		// but the compiler complains, so we define item and allow for return here
		return item;
	}
	
	@SuppressWarnings("rawtypes")
	private Object getArbitraryValueFromSet(Set s) {
		return retreiveArbitrarySetValue(s, 0);
	}
	
	@SuppressWarnings("rawtypes")
	private Object getArbitraryValueFromSet(Set s, int iters) {
		return retreiveArbitrarySetValue(s, iters);
	}
	
	private HashSet<List<Integer>> getCellRoomNeighbors(List<Integer> cell){
		HashSet<List<Integer>> list = new HashSet<List<Integer>>(4);
		
		int cX = cell.get(0), cY = cell.get(1);
		for(CardinalDirection cd : CardinalDirection.values()) {
			int[] d = cd.getDirection();
			if(floorplan.hasNoWall(cX, cY, cd))
				list.add((List<Integer>)Arrays.asList(cX+d[0],cY+d[1]));
		}
		return list;
	}
	
	HashSet<List<Integer>> getRoomNeighborsOfCells(Set<List<Integer>> cells){
		HashSet<List<Integer>> neighbors = new HashSet<List<Integer>>();
		for(List<Integer> cell: cells) neighbors.addAll(getCellRoomNeighbors(cell));
		return neighbors;
	}
	
	HashSet<List<Integer>> getCellsInRoom(List<Integer> cell){
		
		HashSet<List<Integer>> visited = new HashSet<List<Integer>>();
		visited.add(cell);
		HashSet<List<Integer>> newVisits = getCellRoomNeighbors(cell);
		
		while(newVisits.size()>0){
			visited.addAll(newVisits);
			newVisits=getRoomNeighborsOfCells(newVisits);
			newVisits.removeIf(visited::contains);
		}
		
		return visited;
	}
	
	/**
	 * Initialize the set values in the cells array so that
	 * (I) every cell not in a room belongs to its own set;
	 * (II) every room is delegated a unique set for its own cells.
	 * 
	 */
	@SuppressWarnings("unchecked")
	void initializeCells() {
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
		
		if(null==set0) return;
		
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
	
	@Override
	protected void generatePathways() {
		cellSets = new HashMap<Integer,HashSet<List<Integer>>>();
		
		cells=new int[width][height];
		initializeCells();
		
		int[] currentRow=null;
		for(int rowIndex=0; rowIndex<width; rowIndex++) {
			//if(rowIndex>1) break; //take this out when link_CurrentRow_NewRow is implemented
			int[] newRow = cells[rowIndex];
			
			if(null!=currentRow){
				link_CurrentRow_NewRow(currentRow,newRow,rowIndex-1);
			}
			
			wallRemovalFromRow(newRow, rowIndex);
			
			String shouldNotHaveWall = "x=%d, y=%d: this cell should NOT have a wall in the direction (%d,%d), but does";
			String shouldHaveWall = "x=%d, y=%d: this cell SHOULD have a wall in the direction (%d,%d), but does not";
			/*
			for(int i=0; i<height-1; i++) {
				boolean sameSet = (cells[rowIndex][i]==cells[rowIndex][i+1]);
				if(sameSet) {
					assert !floorplan.hasWall(rowIndex, i, VerticalWallBoard.cdForward) :
						String.format(shouldNotHaveWall,rowIndex,i,0,1);
					assert !floorplan.hasWall(rowIndex, i+1, VerticalWallBoard.cdBack) :
						String.format(shouldNotHaveWall,rowIndex,i+1,0,-1);
				}
				else {
					assert floorplan.hasWall(rowIndex, i, VerticalWallBoard.cdForward):
						String.format(shouldHaveWall,rowIndex,i,0,1);
					assert floorplan.hasWall(rowIndex, i+1, VerticalWallBoard.cdBack) :
						String.format(shouldHaveWall,rowIndex,i+1,0,-1);
				}
			}
			assert floorplan.hasWall(0, 0, VerticalWallBoard.cdBack): 
				String.format(shouldHaveWall,rowIndex,0,0,-1);
			assert floorplan.hasWall(0, height-1, VerticalWallBoard.cdForward): 
				String.format(shouldHaveWall,rowIndex,height-1,0,1);
			*/
			currentRow=newRow;
		}
		
		handleLastRow();
		
	}

}

class VerticalWallBoard{
	static final CardinalDirection cdBack = CardinalDirection.getDirection(0, -1);
	static final CardinalDirection cdForward= CardinalDirection.getDirection(0, 1);
	
	private int[] cellLeft;
	private int[] cellRight;
	private Floorplan floorplan;
	private Wallboard wall;
	
	public VerticalWallBoard(int[] cellLeft, int[] cellRight, Floorplan floorplan) {
		//int xLeft=, xRight=cellRight[0], y=;
		if(cellLeft[0]!=cellRight[0]) throw new RuntimeException(String.format("VerticalWallBoard constructor: cellLeft "
				+ "and cellRight have different y-values: %d, %d",cellLeft[1],cellRight[1]));
		
		this.wall = new Wallboard(cellLeft[0],cellLeft[1],cdForward);
		this.cellLeft=cellLeft;
		this.cellRight=cellRight;
		this.floorplan=floorplan;
	}
	
	public int[] getLeft() {
		return cellLeft;
	}
	
	public int[] getRight() {
		return cellRight;
	}
	
	public void removeFromFloorplan() {
		floorplan.deleteWallboard(wall);
	}
	
	public boolean isBorder() {
		return floorplan.isPartOfBorder(wall);
	}
	
	@Override
	public String toString() {
		return String.format("%s <--> %s", Arrays.toString(cellLeft), Arrays.toString(cellRight));
	}
}
