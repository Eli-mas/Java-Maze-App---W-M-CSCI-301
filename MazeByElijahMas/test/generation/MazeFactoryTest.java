package generation;

import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;

//import org.junit.Test;
//import org.junit.Before;

import gui.Controller;
import generation.Order;
//import generation.Order.Builder;
import generation.OrderStub;


/**
 * 
 * MazeFactoryTest tests the validity of a maze.
 * The black-box nature of the testing means that only the end state of the
 * maze is subject to testing; intermediary details are unknown.
 * 
 * JUnit {@code params} ({@code org.junit.jupiter.params}) is used
 * to automate application of test cases to mazes of variable difficulty levels.
 * 
 * @author Elijah Mas
 *
 */
public class MazeFactoryTest {
	/**
	 * a perfect (roomless) maze to subject to testing
	 */
	Maze perfectMaze;
	/**
	 * an imperfect perfect maze to subject to testing
	 */
	Maze imperfectMaze;
	
	int width, height;
	
	/**
	 * <p>Create a perfect maze and an imperfect maze to subject to the different test methods;
	 * the level parameter allows for a variable maze level.</p>
	 * 
	 * <p>
	 * This method calls
	 *     {@link #runAllTests(int)},
	 *     {@link #allTests()}, and
	 *     {@link #baselineTests()}.
	 * The hierarchy of calls allows for simple extension into subclasses.</p>
	 * 
	 * @param level the level of the maze
	 * @param deterministic whether or not the maze is generated deterministically
	 */
	public void establishMazes(int level, boolean deterministic) {
		perfectMaze=getMaze(true,deterministic,level);
		imperfectMaze=getMaze(false,deterministic,level);
		
		//mazes have same dimensions
		width=perfectMaze.getWidth();
		height=perfectMaze.getHeight();
		
	}
	
	/**
	 * Establishes perfect/imperfect mazes and runs all the tests specified in {@link #allTests()}.
	 * 
	 * @param level the level for the mazes to be established
	 */
	@ParameterizedTest
	@ValueSource(ints = {0,1,2,3,4,5,6,7,8})
	public void runAllTests(int level) {
		System.out.println("\n\n*   *   *   DFS: level="+level);
		establishMazes(level, true);
		
		allTests();
	}
	
	/**
	 * For MazeFactoryTest, {@code allTests()} runs only {@link #baselineTests()}.
	 * In subclasses, this can be overriden to introduce new tests.
	 */
	public void allTests() {
		baselineTests();
	}
	/**
	 * {@code baselineTests()} runs these tests:<br>
	 * {@link #testNoRoomsInPerfectMaze()}<br>
	 * {@link #testOneExitByBorders()}<br>
	 * {@link #testExitAtExpectedLocation()}<br>
	 * {@link #testOneExitByDistance()}<br>
	 * {@link #testMaxDistanceStartingPoint()}<br>
	 * {@link #testEveryCellHasExit()}
	 */
	public void baselineTests() {
		testNoRoomsInPerfectMaze();
		testOneExitByBorders();
		testExitAtExpectedLocation();
		testOneExitByDistance();
		testMaxDistanceStartingPoint();
		testEveryCellHasExit();
	}
	
	// allows the tests in this class to be run for mazes at different levels
	//public static @DataPoints int[] levels = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	
	/**
	 * Calls {@link #getMazeExitsByDistance(Maze)} on both perfect and imperfect mazes
	 * and verifies that the call returns a list containing only one cell.
	 */
	//@Test
	public void testOneExitByDistance(){
		assertEquals(1,getMazeExitsByDistance(perfectMaze).size());
		assertEquals(1,getMazeExitsByDistance(imperfectMaze).size());
	}
	
	/**
	 * Calls {@link #getMazeExitsByBorders(Maze)} on both perfect and imperfect mazes
	 * and verifies that the call returns a list containing only one cell.
	 */
	//@Test
	public void testOneExitByBorders(){
		assertEquals(1,getMazeExitsByBorders(perfectMaze).size());
		assertEquals(1,getMazeExitsByBorders(imperfectMaze).size());
	}
	
	/**
	 * Iterating over all cells in the maze, return a list that contains
	 * all cells that have distance=1 from the exit.
	 * <b>***Expected result:</b> the list should have only one cell.
	 * 
	 * @param maze a maze created by a {@link generation.MazeBuilder} instance
	 * @return the cells in the maze whose distance to the exit is 1
	 */
	protected ArrayList<int[]> getMazeExitsByDistance(Maze maze){
		int count=0;
		int[][] dists = maze.getMazedists().getAllDistanceValues();
		ArrayList<int[]> exitCells = new ArrayList<int[]>();
		
		//add any cell with distance=1 to exitCells
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				if(1==dists[x][y]) exitCells.add(new int[] {x,y});
			}
		}
		return exitCells;
	}
	
	/**
	 * call
	 * {@link #_testExitAtExpectedLocationByDistance(Maze)} and
	 * {@link #_testExitAtExpectedLocationByBorders(Maze)}
	 * on both perfect and imperfect mazes. Asserts that the exit cell
	 * identified in the maze's exit cell is where expected based on distances and borders.
	 */
	//@Test
	public void testExitAtExpectedLocation() {
		assertTrue(_testExitAtExpectedLocationByDistance(perfectMaze));
		assertTrue(_testExitAtExpectedLocationByDistance(imperfectMaze));
		assertTrue(_testExitAtExpectedLocationByBorders(perfectMaze));
		assertTrue(_testExitAtExpectedLocationByBorders(imperfectMaze));
	}
	
	/**
	 * <p>
	 * Get the exit cell identified by {@link #getMazeExitsByDistance(Maze)}
	 * and compare it with the cell returned by {@code Maze.getMazedists().getExitPosition()}
	 * </p>
	 * <b>***Expected result:</b> these cells are the same.
	 * 
	 * @param maze a {@code Maze} instance
	 * @return the result of the comparison; true indicates success
	 */
	protected boolean _testExitAtExpectedLocationByDistance(Maze maze) {
		int[] exit =  getMazeExitsByDistance(maze).get(0);
		int[] trueExit = maze.getMazedists().getExitPosition();
		return (exit[0]==trueExit[0] && exit[1]==trueExit[1]);
	}
	
	/**
	 * <p>
	 * Get the exit cell identified by {@link #getMazeExitsByBorders(Maze)}
	 * and compare it with the cell returned by {@code Maze.getMazedists().getExitPosition()}
	 * </p>
	 * <b>***Expected result:</b> these cells are the same.
	 * 
	 * @param maze a {@code Maze} instance
	 * @return the result of the comparison; true indicates success
	 */
	protected boolean _testExitAtExpectedLocationByBorders(Maze maze) {
		int[] exit =  getMazeExitsByBorders(maze).get(0);
		int[] trueExit = maze.getMazedists().getExitPosition();
		return (exit[0]==trueExit[0] && exit[1]==trueExit[1]);
	}
	
	/**
	 * Iterating over all cells in the maze, return a list that contains
	 * all cells with a missing border wall on the maze's external periphery.
	 * If a cell has two missing border walls (this only could happen for a corner cell),
	 * the cell is added twice.
	 * <b>***Expected result:</b> the list should have only one, non-duplicated cell.
	 * 
	 * @param maze a {@link generation.Maze} instance
	 * @return the cells in the maze whose distance to the exit is 1
	 */
	protected ArrayList<int[]> getMazeExitsByBorders(Maze maze){
		int count=0;
		Floorplan floorplan = maze.getFloorplan();
		ArrayList<int[]> exitCells = new ArrayList<int[]>();
		
		//check cells along the top/bottom borders
		for(int x=0; x<width; x++) {
			if(floorplan.hasNoWall(x,0,CardinalDirection.North)) exitCells.add(new int[] {x,0});
			if(floorplan.hasNoWall(x,height-1,CardinalDirection.South)) exitCells.add(new int[] {x,height-1});
		}
		
		//check cells along the left/right borders
		for(int y=0; y<height; y++) {
			if(floorplan.hasNoWall(0,y,CardinalDirection.West)) exitCells.add(new int[] {0,y});
			if(floorplan.hasNoWall(width-1,y,CardinalDirection.East)) exitCells.add(new int[] {width-1,y});
		}
		
		return exitCells;
	}
	
	/**
	 * Convenience method for representing array as line-separated rows.
	 * 
	 * @param array an array
	 * @return string representation of the array.
	 */
	protected static String arrayString2d(Object array) {
		return Arrays.deepToString((Object[]) array).replace("], [", "],\n[");
	}
	
	/**
	 * Verify that every cell has a path to the exit;
	 * calls {@link #_testAllCellsConnected(Maze)}.
	 */
	//@Test
	public void testEveryCellHasExit() {
		//assertTrue(_testEveryCellHasExit(perfectMaze));
		//assertTrue(_testEveryCellHasExit(imperfectMaze));
		assertTrue(_testAllCellsConnected(perfectMaze));
		assertTrue(_testAllCellsConnected(imperfectMaze));
	}
	
	/**
	 * Verify that every cell has a path to the exit by verifying that
	 * for every cell {@code c}, there is at least one neighbor {@code n}
	 * of {@code c} that satisfies:
	 * <ul>
	 *   <li> {@code |distance(c) - distance(n)|=1} </li>
	 *   <li> {@code c} and {@code n} are not separated by a wallboard.
	 * </ul>
	 * 
	 * <b>***Expected result:</b> all cells satisfy the above conditions.
	 * This method returns {@code false} immediately if an erroneous cell is found.
	 * (Though this option should be unreachable, as the maze generation process short-circuits
	 * by failing to calculate distances in the event that a maze has closed rooms).
	 */
	protected boolean _testAllCellsConnected(Maze maze) {
		int[] dx_dy;
		int x2,y2;
		int[][] dists = maze.getMazedists().getAllDistanceValues();
		Floorplan floorplan = maze.getFloorplan();
		
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				for(CardinalDirection dir: CardinalDirection.values()) {
					// get neighbor cell and check if it is in the maze;
					// if not, continue
					
					dx_dy = dir.getDirection();
					x2=dx_dy[0]+x;
					if (x2<0 || x2>=width) continue;
					y2=dx_dy[1]+y;
					if (y2<0 || y2>=height) continue;
					
					// not enough to check that difference in distances is 1:
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
	
	/**
	 * Tests that the maze's starting cell
	 * (as returned by {@code Maze.getMazedists().getStartPosition()})
	 * has a distance that coincides with the maximal value in that matrix.
	 * 
	 * <b>***Expected result:</b> maximal distance in the maze is observed at the starting cell.
	 */
	//@Test
	public void testMaxDistanceStartingPoint() {
		Distance dist = perfectMaze.getMazedists();
		int maxDistance = 0;
		int[] start = dist.getStartPosition();
		int[][] dists= dist.getAllDistanceValues();
		
		// get maximum over all cells
		for(int[] row: dists) {
			for(int d: row) {
				if(d>maxDistance) maxDistance=d;
			}
		}
		
		assertTrue(maxDistance==dists[start[0]][start[1]]);
	}
	
	/**
	 * Test that there are no rooms in a perfect maze.
	 * Calls {@link #_testNoRoomsInPerfectMaze()}.
	 */
	//@Test
	public void testNoRoomsInPerfectMaze() {
		assertTrue(_testNoRoomsInPerfectMaze());
	}
	
	/**
	 * Iterates over every cell in a perfect maze and confirms that
	 * the cell is not in a room (uses {@code Floorplan.isInRoom(int x, int y)}).
	 * If any cell in a room is found, immediately returns {@code false}.
	 * Otherwise, once all cells are exhausted, return true.
	 * 
	 * <b>***Expected result:</b> return true, no cells in rooms.
	 * 
	 * @return
	 */
	public boolean _testNoRoomsInPerfectMaze() {
		Floorplan floorplan = perfectMaze.getFloorplan();
		
		// simpler to check every cell manually
		// than to use areaOverlapsWithRoom and get the bounds straight
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++){
				if(floorplan.isInRoom(x, y)) return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Sets things in order for a maze to be generated.
	 * OrderStub, Controller, and MazeBuilder instances receive
	 * the parameters required to initialize the maze.
	 * Ensures that the maze is finished generating before tests are performed.
	 * 
	 * @param perfect boolean:{maze is perfect}
	 * @param deterministic boolean:{maze is generated deterministically}
	 * @param level level of the maze
	 * @return the generated Maze instance
	 */
	public Maze getMaze(boolean perfect, boolean deterministic, int level){
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(perfect);
		order.start(controller, null);
		
		MazeBuilder builder = new MazeBuilder(deterministic);
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("100");
		
		return order.getMaze();
	}
}

