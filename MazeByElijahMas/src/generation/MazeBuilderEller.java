/**
 * 
 */
package generation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
	
	/*
	 * { cells = new int[width][height]; for(int i=0; i<height; i++) cells[0][i]=i;
	 * }
	 */
	
	private void mergeSets(int[] a, int[] b) {
		int val1=cells[a[0]][a[1]], val2=cells[b[0]][b[1]], minVal, maxVal;
		if(val1<val2) {
			minVal=val1;
			maxVal=val2;
		}
		else {
			minVal=val2;
			maxVal=val1;
		}
		//as per documentation, addAll creates a union of two sets
		HashSet<List<Integer>> maxCells = cellSets.get(maxVal);
		cellSets.get(minVal).addAll(maxCells);
		for(List<Integer> cellList: maxCells) addValueAtCell(cellList, minVal);
		cellSets.remove(maxVal);
		System.out.print("");
	}
	
	private int getCellValue(int[] cell) {
		return cells[cell[0]][cell[1]];
	}
	
	private int getUniqueCount(int[] row) {
		HashSet<Integer> a = new HashSet<Integer>();
		for(int i: row) a.add(i);
		return (a.size());
	}
	
	private void wallRemovalFromRow(Object currentRow, int[] newRow, int rowIndex){
		//boards={list of wallboards in newRow}
		ArrayList<VerticalWallBoard> boards = new ArrayList<VerticalWallBoard>();
		
		
		for(int i=0; i<newRow.length-1; i++) {
			boards.add(new VerticalWallBoard(new int[] {rowIndex,i}, new int[] {rowIndex,i+1}, floorplan));
		}
		
		/*
			-->randomly choose how many sets we want after removals, set to finalCount
			low = (int)round(sqrt(length of row))
			finalCount = random integer in inclusive range (low, row.length-low)
			
			I made this range up, for the moment it seems to make sense
		*/
		int low = (int)Math.round(Math.sqrt(newRow.length));
		int finalCount = SingleRandom.getRandom().nextIntWithinInterval(low, newRow.length-low);
		
		System.out.println("finalCount: "+finalCount);
		
		while(true) {
			
			/*
				get the number of unique values in the row
				if this number is equal to or less than finalCount: break
			*/
			int unique=getUniqueCount(newRow);
			if(unique<=finalCount) break;
			
			
			/*
				select a wallboard at random
				get the neighboring cells (left/right) of this wallboard
			*/
			int index = SingleRandom.getRandom().nextIntWithinInterval(0, boards.size()-1);
			VerticalWallBoard wall = boards.get(index);
			int[] left=wall.getLeft(), right=wall.getRight();
			
			/*
				if these cells are of different sets:
					remove the wallboard from floorplan
					set all elements in both sets to the same value (the lower value of the two sets)
			*/
			if (getCellValue(left)!=getCellValue(right)) {
				mergeSets(left, right);
				wall.removeFromFloorplan();
			};
			
			/*
				remove the wallboard from boards
			 */
			boards.remove(index);
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
	
	@Override
	protected void generatePathways() {
		cellSets = new HashMap<Integer,HashSet<List<Integer>>>();
		
		int width=4,height=5;
		cells=new int[width][height];
		for(int i=0; i<height; i++) addValueAtCell(new int[] {0,i},i);
		
		System.out.printf("width, height: %d, %d\n",width,height);
		System.out.println("cells[0]:\n"+Arrays.toString(cells[0]));
		wallRemovalFromRow(null, cells[0], 0);
		System.out.println("cells[0] after wall removal:\n"+Arrays.toString(cells[0]));
		
		throw new RuntimeException("MazeBuilderEller: not ready to proceed beyond this point");
		
	}
	
	public static void main(String[] args) {
		
		(new MazeBuilderEller()).generatePathways();
	}

}

class VerticalWallBoard{
	static final CardinalDirection cdBack = CardinalDirection.getDirection(-1, 0);
	static final CardinalDirection cdForward= CardinalDirection.getDirection(1, 0);
	
	private int[] cellLeft;
	private int[] cellRight;
	private Floorplan floorplan;
	private Wallboard wall;
	
	public VerticalWallBoard(int[] cellLeft, int[] cellRight, Floorplan floorplan) {
		int xLeft=cellLeft[0], xRight=cellRight[0], y=cellLeft[1];
		if(y!=cellRight[1]);
		
		this.wall = new Wallboard(xLeft,y,cdForward);
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
	
	@Override
	public String toString() {
		return String.format("%s <--> %s", Arrays.toString(cellLeft), Arrays.toString(cellRight));
	}
}
