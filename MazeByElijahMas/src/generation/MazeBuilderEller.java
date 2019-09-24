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
	}
	
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
		cellSets.get(minVal).addAll(cellSets.get(maxVal));
		cellSets.remove(maxVal);
	}
	
	private int getCellValue(int[] cell) {
		return cells[cell[0]][cell[1]];
	}
	
	private int getUniqueCount(int[] row) {
		HashSet<Integer> a = new HashSet<Integer>();
		for(int i: row) a.add(i);
		return (a.size());
	}
	
	private void wallRemovalFromRow(Object currentRow, int[] newRow){
		//boards={list of wallboards in newRow}
		ArrayList<VerticalWallBoard> boards = new ArrayList<VerticalWallBoard>();
		
		int y=newRow[1];
		
		for(int i=0; i<newRow.length-1; i++) {
			boards.add(new VerticalWallBoard(new int[] {i,y}, new int[] {i+1,y}, floorplan));
		}
		
		/*
			-->randomly choose how many sets we want after removals, set to finalCount
			low = (int)round(sqrt(length of row))
			finalCount = random integer in inclusive range (low, row.length-low)
			
			I made this range up, for the moment it seems to make sense
		*/
		int low = (int)Math.round(Math.sqrt(newRow.length));
		int finalCount = SingleRandom.getRandom().nextIntWithinInterval(low, newRow.length-low);
		
		while(true) {
			
		/*
			get the number of unique values in the row
			if this number is equal to or less than finalCount: break
		*/
		if(getUniqueCount(newRow)<=finalCount) break;
		
		
		/*
			select a wallboard at random
			get the neighboring cells (left/right) of this wallboard
		*/
		int index = SingleRandom.getRandom().nextIntWithinInterval(0, boards.size());
		VerticalWallBoard wall = boards.get(index);
		int[] left=wall.getLeft(), right=wall.getRight();
		
		/*
			if these cells are of different sets:
				remove the wallboard from Floorplan
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
	
	public MazeBuilderEller(boolean det) {
		super(det);
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze (deterministic enabled).");
	}
	
	@Override
	protected void generatePathways() {
		throw new RuntimeException("MazeBuilderEller: using unimplemented method generatePathways"); 
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
}
