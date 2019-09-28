package generation;

import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import gui.Controller;
import generation.Order;
//import generation.Order.Builder;
import generation.OrderStub;

public class MazeFactoryTestEller extends MazeFactoryTest {
	Maze perfectMaze;
	Maze imperfectMaze;
	int width, height;
	OrderStub orderPerfect;
	OrderStub orderImperfect;
	MazeBuilderEller builderPerfect;
	MazeBuilderEller builderImperfect;
	
	@ParameterizedTest
	@ValueSource(ints = {0,1,2,3,4,5,6,7,8})
	@Override
	public void runAllTests(int level) {
		System.out.println("\n\n*   *   *   Eller: level="+level);
		
		establishMazes(level, true);
		
		allTests();
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
	
	@Override
	public Maze getMaze(boolean perfect, boolean deterministic, int level){
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.Eller); 
		order.setPerfect(deterministic ? true : perfect);
		order.start(controller, null);
		
		MazeBuilderEller builder = new MazeBuilderEller(deterministic);
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
	
	//@Test
	public void testCells() {
		_testCells(builderPerfect);
		_testCells(builderImperfect);
	}

	public boolean _testCells(MazeBuilderEller builder) {
		for(int[] row: builder.retrieve_cells()) {
			for(int v: row) {
				if(1!=v) return false;
			}
		}
		return true;
	}

}
