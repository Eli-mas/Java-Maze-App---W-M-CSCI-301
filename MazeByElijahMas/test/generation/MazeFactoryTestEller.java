package generation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gui.Controller;
import generation.Order;
//import generation.Order.Builder;
import generation.OrderStub;

public class MazeFactoryTestEller extends MazeFactoryTest {
	//Maze perfectMaze;
	//Maze imperfectMaze;
	//int width, height;
	OrderStub orderPerfect;
	OrderStub orderImperfect;
	MazeBuilderEller builderPerfect;
	MazeBuilderEller builderImperfect;
	
	@ParameterizedTest
	@ValueSource(ints = {0,1,2,3,4,5,6,7,8})
	@Override
	public void runAllTests(int level) {
		System.out.println("\n\n*   *   *   Eller: level="+level);
		
		establishMazes(level, true, false);
		
		allTests();
	
		establishMazes(level, true, true);
		
		allTests();
	}
	
	public void establishMazes(int level, boolean deterministic, boolean ignoreRooms) {
		perfectMaze=getMaze(true,ignoreRooms? false: deterministic,level,false);
		imperfectMaze=getMaze(false,deterministic,level,ignoreRooms);
		
		if(null==perfectMaze || null==imperfectMaze) {
			throw new RuntimeException("MazeFactoryTest.establishMazes: the maze order is not successful");
		}
		
		//mazes have same dimensions
		width=perfectMaze.getWidth();
		height=perfectMaze.getHeight();
		
	}
	/**
	 * Adds new methods to the suite of tests for Eller's algorithm
	 * beyond those specified in #{@link #baselineTests()}.
	 */
	@Override
	public void allTests() {
		baselineTests();
		testCells();
	}
	
	//@Override
	public Maze getMaze(boolean perfect, boolean deterministic, int level, boolean ignoreRooms){
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.Eller); 
		order.setPerfect(perfect);
		order.start(controller, null);
		
		MazeBuilderEller builder = new MazeBuilderEller(deterministic);
		if(ignoreRooms) builder.setToIgnoreRooms();
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(perfect) {
			this.orderPerfect=order;
			this.builderPerfect = builder;
		} else {
			this.orderImperfect=order;
			this.builderImperfect = builder;
		}
		
		System.out.println("100");
		
		return order.getMaze();
	}
	
	/**
	 * Tests that all maze cells belong to a single set
	 * by checking the array holding set values.
	 * Calls {@link #_testCells(MazeBuilderEller)}.
	 */
	public void testCells() {
		_testCells(builderPerfect);
		_testCells(builderImperfect);
	}

	/**
	 * Checks that all cells in the maze belong to the set with id=1,
	 * as prescribed by this implementation of Eller's algorithm.
	 * If any cell is found with any other value, return false.
	 * 
	 * <b>***Expected result:</b> all cells contain 1.
	 * 
	 * @param builder a MazeBuilderEller Instance
	 * @return boolean that is true if the test is successful.
	 */
	public boolean _testCells(MazeBuilderEller builder) {
		for(int[] row: builder.retrieve_cells()) {
			for(int v: row) {
				if(1!=v) return false;
			}
		}
		return true;
	}
	
	/**
	 * Tests that all maze cells belong to a single set
	 * by checking the array holding set values.
	 * Calls {@link #_testCellSets(MazeBuilderEller)}.
	 */
	public void testCellSets() {
		_testCells(builderPerfect);
		_testCells(builderImperfect);
	}
	
	/**
	 * Checks that there is only one set of cells identified in the maze,
	 * that the id of this set is 1, and that all cells in the maze are contained
	 * in this set.
	 * @param builder a MazeBuilderEller instance
	 * @return boolean that confirms the check
	 */
	public boolean _testCellSets(MazeBuilderEller builder) {
		HashMap<Integer,Set<List<Integer>>> sets = builder.retrieve_cellSets();
		
		// test only one set present
		assertTrue(1==sets.keySet().size());
		
		// test that the set's id is 1
		assertTrue(sets.keySet().contains(1));
		
		Set<List<Integer>> set1 = (Set<List<Integer>>)sets.get(1);
		
		// test that this set is of the expected size (total # of maze cells)
		assertEquals(width*height,set1.size());
		
		// test that every maze cell is indeed in this set
		boolean check = true;
		for(int x=0; x<width; x++) {
			for(int y=0; y<width; y++) {
				if(!set1.contains(Arrays.asList(x,y))) {
					check=false;
					break;
				}
			}
		}
		assertTrue(check);
		
		// we could also check that there are no cells in the set that are out of bounds
		// but the above two tests guarantee that this is so
		
		return true;
	}

}
