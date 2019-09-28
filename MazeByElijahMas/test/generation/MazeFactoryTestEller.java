package generation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import gui.Controller;
import generation.Order;
import generation.Order.Builder;
import generation.OrderStub;

public class MazeFactoryTestEller extends MazeFactoryTest {
	Maze perfectMaze;
	Maze imperfectMaze;
	int width, height;
	OrderStub orderPerfect;
	OrderStub orderImperfect;
	MazeBuilderEller builderPerfect;
	MazeBuilderEller builderImperfect;
	
	@Override
	public Maze getMaze(boolean perfect, boolean deterministic, int level){
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.Eller); 
		order.setPerfect(deterministic ? true : perfect);
		//System.out.println("testline:");
		order.start(controller, null);
		
		/*MazeFactory factory = new MazeFactory(true);
		factory.order(order);
		factory.waitTillDelivered();*/
		
		
		MazeBuilderEller builder = new MazeBuilderEller(deterministic);
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(perfect) {
			this.orderPerfect=order;
			this.builderPerfect = builder;
		} else {
			this.orderImperfect=order;
			this.builderImperfect = builder;
		}
		
		return order.getMaze();
	}
	
	@Test
	public void testCells() {
		_testCells(builderPerfect);
		_testCells(builderImperfect);
		for(int i=0; i<10; i++) {
			Maze mazePerfect = getMaze(true, false, i);
			Maze mazeImperfect = getMaze(false, false, i);
			System.out.printf("testCells %d (L%d): %s %s\n",i,i,builderPerfect,builderImperfect);
			_testCells(builderPerfect);
			_testCells(builderImperfect);
		}
	}

	public boolean _testCells(MazeBuilderEller builder) {
		for(int[] row: builder.retrieve_cells()) {
			for(int v: row) {
				if(1!=v) return false;
			}
		}
		return true;
	}

	/*
	 * think of test cases:
	 * 
	 */

}
