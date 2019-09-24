package generation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import gui.Controller;
import gui.MazePanel;
import gui.SimpleScreens;
import gui.StateGenerating;
import generation.Order;
import generation.Order.Builder;
import gui.DefaultState;
import gui.MazeFileReader;
import generation.OrderStub;


public class MazeFactoryTest {
	
	/*
	 * TODO:
	 *     allow for testing on mazes of variable difficulties
	 *     ensure deterministic maze generation
	 *     make sure methods are being tested on perfect and imperfect mazes, where applicable
	 *     create javadoc for methods
	 *     provide in-line comments in methods
	 *     any other tests?
	 */
	
	Maze perfectMaze;
	Maze imperfectMaze;
	int width, height;
	
	@Before
	final public void establishMazes() {
		perfectMaze=getMaze(true);
		imperfectMaze=getMaze(false);
		width=perfectMaze.getWidth();
		height=perfectMaze.getHeight();
		
	}
	
	@Test
	final public void testOneExitByDistance(){
		assertEquals(1,_testOneExitByDistance(perfectMaze));
		assertEquals(1,_testOneExitByDistance(imperfectMaze));
	}
	
	public int _testOneExitByDistance(Maze maze){
		/*
		 * get mazedists
		 * verify: only one occurrence of the value <1> in mazedists
		 */
		int count=0;
		int[][] dists = maze.getMazedists().getAllDistanceValues();
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(1==dists[x][y]) count+=1;
			}
		}
		return count;
	}
	
	@Test
	final public void testOneExitByBorders(){
		assertEquals(1,_testOneExitByBorders(perfectMaze));
		assertEquals(1,_testOneExitByBorders(imperfectMaze));
	}
	
	public int _testOneExitByBorders(Maze maze){
		/*
		 * get floorplan
		 * verify: only one border wallboard around the entire maze is missing
		 */
		int count=0;
		Floorplan floorplan = maze.getFloorplan();
		for(int x=0; x<width; x++) {
			if(floorplan.hasNoWall(x,0,CardinalDirection.North)) count+=1;
			if(floorplan.hasNoWall(x,height-1,CardinalDirection.South)) count+=1;
		}
		for(int y=0; y<height; y++) {
			if(floorplan.hasNoWall(0,y,CardinalDirection.West)) count+=1;
			if(floorplan.hasNoWall(width-1,y,CardinalDirection.East)) count+=1;
		}
		return count;
	}
	
	@Test
	final public void testEveryCellHasExit() {
		assertTrue(_testEveryCellHasExit(perfectMaze));
	}
	
	public boolean _testEveryCellHasExit(Maze maze) {
		/*
		 * verify: if we start a depth-first recursive walk at the exit cell,
		 *     where we walk to each cell with distance one greater than the current cell,
		 *     every cell is reached
		 */
		boolean[][] reached = new boolean[width][height];
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				reached[x][y]=false;
			}
		}
		
		Floorplan floorplan = maze.getFloorplan();
		Distance distMatrix=maze.getMazedists();
		int[][] dists = distMatrix.getAllDistanceValues();
		int[] exit = distMatrix.getExitPosition();
		
		
		
		_testEveryCellHasExit_recursiveFill(reached, floorplan, exit[0], exit[1], dists);
		
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(!reached[x][y]) return false;
			}
		}
		
		return true;
	}
	
	public void _testEveryCellHasExit_recursiveFill(
			boolean[][] reached, Floorplan floorplan, int x, int y, int[][] dists) {
		reached[x][y]=true;
		for(CardinalDirection dir: CardinalDirection.values()) {
			if(floorplan.hasNoWall(x, y, dir)) {
				int[] dx_dy = dir.getDirection();
				int new_x=x+dx_dy[0], new_y=y+dx_dy[1];
				if(new_x<0 || new_y<0 || new_x>=width || new_y>=height) continue;
				if(dists[new_x][new_y]>dists[x][y])
					_testEveryCellHasExit_recursiveFill(reached, floorplan, new_x, new_y, dists);
			}
		}
	}
	
	@Test
	final public void testMaxDistanceStartingPoint() {
		/*
		 * get mazedists
		 * test that the starting point in mazedists has the largest value in the distance array
		 */
		Distance dist = perfectMaze.getMazedists();
		int[] start = dist.getStartPosition();
		int[][] dists= dist.getAllDistanceValues();
		int maxDistance=0, max_x=-1, max_y=-1;
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(dists[x][y]>maxDistance) {
					maxDistance=dists[x][y];
					max_x=x; max_y=y;
				}
			}
		}
		
		assertTrue(max_x==start[0] && max_y==start[1]);
	}
	
	@Test
	final public void testNoRoomsInPerfectMaze() {
		assertTrue(_testNoRoomsInPerfectMaze());
	}
	
	public boolean _testNoRoomsInPerfectMaze() {
		Floorplan floorplan = perfectMaze.getFloorplan();
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++){
				if(floorplan.isInRoom(x, y)) return false;
			}
		}
		
		return true;
	}
	
	@Test
	final public void getPerfect() {
		Distance dists=perfectMaze.getMazedists();
		Floorplan floorplan = perfectMaze.getFloorplan();
		
		int[][] distVals=dists.getAllDistanceValues();
		
		System.out.println("mazedists:");
		for(int[] row:distVals) System.out.println(Arrays.toString(row));
		System.out.println("floorplan:\n"+floorplan);
		
		
		System.out.print("");
	}
	
	public Maze getMaze(boolean perfect){
		/*
		*/
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(4);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(perfect);
		//System.out.println("testline:");
		order.start(controller, null);
		
		/*MazeFactory factory = new MazeFactory(true);
		factory.order(order);
		factory.waitTillDelivered();*/
		
		
		MazeBuilder builder = new MazeBuilder(true);
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return order.getMaze();
	}
}

