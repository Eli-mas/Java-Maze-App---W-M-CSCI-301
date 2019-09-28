package generation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Before;

import gui.Controller;
import generation.Order;
import generation.Order.Builder;
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
	public void establishMazes() {
		int level=4;
		perfectMaze=getMaze(true,true,level);
		imperfectMaze=getMaze(false,true,level);
		width=perfectMaze.getWidth();
		height=perfectMaze.getHeight();
		
	}
	
	@Test
	public void testOneExitByDistance(){
		assertEquals(1,getMazeExitsByDistance(perfectMaze).size());
		assertEquals(1,getMazeExitsByDistance(imperfectMaze).size());
	}
	
	protected ArrayList<int[]> getMazeExitsByDistance(Maze maze){
		/*
		 * get mazedists
		 * verify: only one occurrence of the value <1> in mazedists
		 */
		int count=0;
		int[][] dists = maze.getMazedists().getAllDistanceValues();
		ArrayList<int[]> exitCells = new ArrayList<int[]>();
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(1==dists[x][y]) exitCells.add(new int[] {x,y});
			}
		}
		return exitCells;
	}
	
	@Test
	public void testExitAtExpectedLocation() {
		assertTrue(_testExitAtExpectedLocationByDistance(perfectMaze));
		assertTrue(_testExitAtExpectedLocationByDistance(imperfectMaze));
		assertTrue(_testExitAtExpectedLocationByBorders(perfectMaze));
		assertTrue(_testExitAtExpectedLocationByBorders(imperfectMaze));
	}
	
	protected boolean _testExitAtExpectedLocationByDistance(Maze maze) {
		int[] exit =  getMazeExitsByDistance(maze).get(0);
		int[] trueExit = maze.getMazedists().getExitPosition();
		return (exit[0]==trueExit[0] && exit[1]==trueExit[1]);
	}
	
	protected boolean _testExitAtExpectedLocationByBorders(Maze maze) {
		int[] exit =  getMazeExitsByBorders(maze).get(0);
		int[] trueExit = maze.getMazedists().getExitPosition();
		return (exit[0]==trueExit[0] && exit[1]==trueExit[1]);
	}
	
	@Test
	public void testOneExitByBorders(){
		assertEquals(1,getMazeExitsByBorders(perfectMaze).size());
		assertEquals(1,getMazeExitsByBorders(imperfectMaze).size());
	}
	
	protected ArrayList<int[]> getMazeExitsByBorders(Maze maze){
		/*
		 * get floorplan
		 * verify: only one border wallboard around the perimeter maze is missing
		 */
		int count=0;
		Floorplan floorplan = maze.getFloorplan();
		ArrayList<int[]> exitCells = new ArrayList<int[]>();
		for(int x=0; x<width; x++) {
			if(floorplan.hasNoWall(x,0,CardinalDirection.North)) exitCells.add(new int[] {x,0});
			if(floorplan.hasNoWall(x,height-1,CardinalDirection.South)) exitCells.add(new int[] {x,height-1});
		}
		for(int y=0; y<height; y++) {
			if(floorplan.hasNoWall(0,y,CardinalDirection.West)) exitCells.add(new int[] {0,y});
			if(floorplan.hasNoWall(width-1,y,CardinalDirection.East)) exitCells.add(new int[] {width-1,y});
		}
		return exitCells;
	}
	
	@Test
	public void testEveryCellHasExit() {
		//assertTrue(_testEveryCellHasExit(perfectMaze));
		//assertTrue(_testEveryCellHasExit(imperfectMaze));
		assertTrue(_testAllCellsConnected(perfectMaze));
		assertTrue(_testAllCellsConnected(imperfectMaze));
	}
	
	protected static String arrayString2d(Object array) {
		return Arrays.deepToString((Object[]) array).replace("], [", "],\n[");
	}
	
	protected boolean _testAllCellsConnected(Maze maze) {
		int[] dx_dy;
		int x2,y2;
		int[][] dists = maze.getMazedists().getAllDistanceValues();
		Floorplan floorplan = maze.getFloorplan();
		
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				for(CardinalDirection dir: CardinalDirection.values()) {
					dx_dy = dir.getDirection();
					x2=dx_dy[0]+x;
					if (x2<0 || x2>=width) continue;
					y2=dx_dy[1]+y;
					if (y2<0 || y2>=height) continue;
					
					// not enough to check that difference in distances is one:
					// there must also be no wallboard between the cells for valid check
					// distance difference can be +-1
					if(floorplan.hasNoWall(x, y, dir)) {
						if(Math.abs(dists[x][y]-dists[x2][y2])!=1) {
							System.out.printf(
								"_testAllCellsConnected failed on (%d,%d)[%d]-(%d,%d)[%d]\n"+
								arrayString2d(dists)+"\n",
								x, y, dists[x][y],
								x2,y2,dists[x2][y2]);
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	/*
	unused helper methods:
		
		what these do: if we start a depth-first recursive walk at the exit cell,
		where we walk to each cell with distance one greater than the current cell,
		make sure that every cell is reached
		
		this is a valid test, but according to Prof. Kemper
		it is not simple enough for application in test suite;
		Since I had already coded it, I leave it here for reference
	
	public boolean _testEveryCellHasExit(Maze maze) {
	
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
	*/
	
	@Test
	public void testMaxDistanceStartingPoint() {
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
					//max_x=x; max_y=y;
				}
			}
		}
		
		assertTrue(maxDistance==dists[start[0]][start[1]]);
	}
	
	@Test
	public void testNoRoomsInPerfectMaze() {
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
	
	public Maze getMaze(boolean perfect, boolean deterministic, int level){
		/*
		*/
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(perfect);
		//System.out.println("testline:");
		order.start(controller, null);
		
		/*MazeFactory factory = new MazeFactory(true);
		factory.order(order);
		factory.waitTillDelivered();*/
		
		
		MazeBuilder builder = new MazeBuilder(deterministic);
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

