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

public class MazeFactoryTestEller {
	Maze perfectMaze;
	Maze imperfectMaze;
	int width, height;
	
	//@Before
	final public void establishMazes() {
		perfectMaze=getMaze(true);
		imperfectMaze=getMaze(false);
		width=perfectMaze.getWidth();
		height=perfectMaze.getHeight();
		
	}
	
	@Test
	final public void testMaze() {
		getMaze(false);
	}
	
	public static Maze getMaze(boolean perfect){
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
		
		
		MazeBuilderEller builder = new MazeBuilderEller(true);
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
