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
		System.out.printf("initial width, height: %d, %d\n",width,height);
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
		/*
		System.out.printf("minVal=%d, maxVal=%d\n",minVal,maxVal);
		System.out.println("cellSets.get(minVal):\n"+cellSets.get(minVal));
		System.out.println("maxCells:\n"+maxCells);
		*/
		cellSets.get(minVal).addAll(maxCells);
		for(List<Integer> cellList: maxCells) addValueAtCell(cellList, minVal);
		cellSets.remove(maxVal);
		System.out.print("");
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
		System.out.println("finalCount: "+finalCount);
		
		while(true) {
			
			/*
				get the number of unique values in the row
				if this number is equal to or less than finalCount: break
			*/
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
			if (getCellValue(left)!=getCellValue(right) && !wall.isBorder()) {
				mergeSets(left, right);
				wall.removeFromFloorplan();
				removals++;
				//System.out.println(Arrays.toString(cells[0])+" <wallRemovalFromRow-->removeFromFloorplan>");
			};
			
			/*
				remove the wallboard from boards
			 */
			boards.remove(index);
			if(removals>=finalCount || 0==boards.size()) break;
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
				//cellSets.remove(getCellValue(lowerCell)); 
				//addValueAtCell(lowerCell, setValue);
				if(getCellValue(cell)!=getCellValue(lowerCell)) mergeSets(cell, lowerCell);
				//call mergeSets instead of addValueAtCell because the lower cell could belong to a larger set if in a room
				//the conditional is required because it is invalid to attempt joining cells which already share a room
				/*
				 * in floorplan, remove the wallboard between cell and lower neighbor
				 * //remove wallboard between cell and cell's lower neighbor //non-defined extra step
				 * remove cell from extendable
				 * add cell to nonExtendable
				 * remove cells in unextendable from extendable
				 */
				floorplan.deleteWallboard(new Wallboard(cell.get(0), cell.get(1), CardinalDirection.getDirection(1, 0)));
				extendable.remove(cell);
				nonExtendable.add(cell);
				
				size=extendable.size();
			}
		}
		
		
		
	}

	void handleLastRow() {
		ArrayList<VerticalWallBoard> boards = getWallboardsInRow(cells[width-1], width-1);
		for(VerticalWallBoard wall:boards) {
			int[] left=wall.getLeft(), right=wall.getRight();
			
			/*
				last row: remove all walls between distinct sets
			*/
			if (getCellValue(left)!=getCellValue(right) && !wall.isBorder()) {
				mergeSets(left, right);
				wall.removeFromFloorplan();
			};
		}
	}
	
	private Object retreiveArbitrarySetValue(Set s, int iters) {
		Object item=null;
		int count = 0;
		for(Object o: s) {
			item=o;
			if(iters==count++) return item;
		}
		return item;
	}
	
	Object getArbitraryValueFromSet(Set s) {
		return retreiveArbitrarySetValue(s, 0);
	}
	
	Object getArbitraryValueFromSet(Set s, int iters) {
		return retreiveArbitrarySetValue(s, iters);
	}
	
	HashSet<List<Integer>> getCellRoomNeighbors(List<Integer> cell){
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
		
		List<Integer> current;
		
		while(newVisits.size()>0){
			//System.out.println("toVisit.size(): "+newVisits.size());
			visited.addAll(newVisits);
			newVisits=getRoomNeighborsOfCells(newVisits);
			// on iter 0 ...
			// on iter > 0 ...
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
		
		
		/*
		for(int x=1; x<4; x++) {
			for(int y=12; y<17; y++) {
				cells[x][y]=roomIndex;
			}
		}
		*/
	}
	
	@Override
	protected void generatePathways() {
		cellSets = new HashMap<Integer,HashSet<List<Integer>>>();
		
		//int width=4,height=5;
		cells=new int[width][height];
		initializeCells();
		
		System.out.println("cells has been initialized to:\n"+Arrays.deepToString(cells).replace("], [", "],\n["));
		System.out.println("cellSets has keys: "+cellSets.keySet());
		
		//System.out.println("About to throw an error to stop execution");
		//System.out.println((new int[] {})[2]);
		
		System.out.printf("width, height: %d, %d\n",width,height);
		System.out.println("cells[0]:\n"+Arrays.toString(cells[0]));
		int[] currentRow=null;
		for(int rowIndex=0; rowIndex<width; rowIndex++) {
			//if(rowIndex>1) break; //take this out when link_CurrentRow_NewRow is implemented
			int[] newRow = cells[rowIndex];
			
			//initializeRow(currentRow, newRow);
			//replaced with initializeCells
			
			if(null!=currentRow){
				System.out.printf("cells[%d] before link_CurrentRow_NewRow:\n"+Arrays.toString(newRow)+"\n",rowIndex);
				link_CurrentRow_NewRow(currentRow,newRow,rowIndex-1);
				System.out.printf("cells[%d] after link_CurrentRow_NewRow:\n"+Arrays.toString(newRow)+"\n",rowIndex);
				//throw new RuntimeException("MazeBuilderEller: stopping here for link_CurrentRow_NewRow test");
			}
			
			wallRemovalFromRow(newRow, rowIndex);
			System.out.printf("cells[%d] after wall removal:\n"+Arrays.toString(cells[0])+"\n",rowIndex);
			
			/*
			for(int i=0; i<height-1; i++) {
				boolean sameSet = (cells[rowIndex][i]==cells[rowIndex][i+1]);
				if(sameSet) {
					assert !floorplan.hasWall(rowIndex, i, VerticalWallBoard.cdForward) :
						String.format("x=%d, y=%d: this cell should NOT have a wall in the direction (0,1), but does",rowIndex,i);
					assert !floorplan.hasWall(rowIndex, i+1, VerticalWallBoard.cdBack) :
						String.format("x=%d, y=%d: this cell should NOT have a wall in the direction (0,-1), but does",rowIndex,i+1);
				}
				else {
					assert floorplan.hasWall(rowIndex, i, VerticalWallBoard.cdForward):
						String.format("x=%d, y=%d: this cell SHOULD have a wall in the direction (0,1), but does not",rowIndex,i);
					assert floorplan.hasWall(rowIndex, i+1, VerticalWallBoard.cdBack) :
						String.format("x=%d, y=%d: this cell SHOULD have a wall in the direction (0,-1), but does not",rowIndex,i+1);
				}
			}
			assert floorplan.hasWall(0, 0, VerticalWallBoard.cdBack): 
				String.format("x=%d, y=0: this cell SHOULD have a wall in the direction (0,-1), but does not",rowIndex);
			assert floorplan.hasWall(0, height-1, VerticalWallBoard.cdForward): 
				String.format("x=%d, y=%d: this cell SHOULD have a wall in the direction (0,1), but does not",rowIndex,height-1);
			*/
			currentRow=newRow;
		}
		
		handleLastRow();
		
		System.out.println("floorplan generated: cells:\n"+Arrays.deepToString(cells).replace("], [", "],\n["));
		
		
		
		throw new RuntimeException("MazeBuilderEller: not ready to proceed beyond this point");
		
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
		//System.out.printf("deleting wallboard: x,y=(%d,%d) d=%s\n",wall.getX(),wall.getY(),Arrays.toString(wall.getDirection().getDirection()));
	}
	
	public boolean isBorder() {
		return floorplan.isPartOfBorder(wall);
	}
	
	@Override
	public String toString() {
		return String.format("%s <--> %s", Arrays.toString(cellLeft), Arrays.toString(cellRight));
	}
}
