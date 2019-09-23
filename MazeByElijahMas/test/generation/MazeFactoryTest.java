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


public class MazeFactoryTest {
	/*
	 * Test cases for a maze:
	 * 
	 * --every cell has a path to the exit
	 * 
	 * --there is one opening/closing
	 * 
	 * --no closed rooms
	 * 
	 */
	
	Maze perfectMaze;
	Maze imperfectMaze;
	
	@Before
	final public void someCall() {
		perfectMaze=getPerfectMaze();
	}
	
	@Test
	final public void testOneExit(){
		/*
		 * get mazedists
		 * verify: only one occurrence of the value <1> in mazedists
		 */
		assertTrue(false);
	}
	
	@Test
	final public void testEveryCellHasExit() {
		/*
		 * get mazedists
		 * verify: every cell has at least one neighbor whose value
		 *     is one more or less than the neighbor's value
		 */
		assertTrue(false);
	}
	
	@Test
	final public void testNoClosedRooms() {
		/*
		 * one idea: test that every wallboard connects to other wallboards which ultimately
		 *     connect to the outer wall of the maze
		 * 
		 * but is there a simpler way?
		 * 
		 * still have to understand how the floorplan object works...
		 */
		assertTrue(false);
	}
	
	@Test
	final public void testMaxDistanceStartingPoint() {
		/*
		 * get mazedists
		 * test that the starting point in mazedists has the largest value in the distance array
		 */
		assertTrue(false);
	}
	
	@Test
	final public void testNoRoomsInPerfectMaze() {
		/*
		 * get a perfect maze via getPerfectMaze
		 * use Floorplan.isInRoom or Floorplan.areaOverlapsWithRoom to check that no cells are in rooms
		 */
		assertTrue(false);
	}
	
	@Test
	final public void getPerfect() {
		Distance dists=perfectMaze.getMazedists();
		Floorplan floorplan = perfectMaze.getFloorplan();
		
		int[][] distVals=dists.getAllDistanceValues();
		
		System.out.println("mazedists:\n"+Arrays.deepToString(distVals));
		System.out.println("floorplan:\n"+floorplan);
		
		
		System.out.print("");
	}
	
	public Maze getPerfectMaze(){
		/*
		*/
		
		Controller controller = new Controller();
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(0);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(true);
		System.out.println("testline:");
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

class OrderStub extends DefaultState implements Order{
	//SimpleScreens view;
	//MazePanel panel;
	Controller control;
	private String filename;
	
	private int skillLevel;
	private Builder builder;
	private boolean perfect; 
   
	protected Factory factory;

	private int percentdone;

	boolean started;
	
	private Maze mazeConfig;
	
	public OrderStub() {
		filename = null;
		factory = new MazeFactory(true) ;
		skillLevel = 0;
		builder = Order.Builder.DFS;
		perfect = true;
		percentdone = 0;
		started = false;
	}
	
	@Override
	public void setFileName(String filename) {
		this.filename = filename;  
	}
	@Override
	public void setSkillLevel(int skillLevel) {
		this.skillLevel = skillLevel;
	}
	@Override
	public void setBuilder(Builder builder) {
		this.builder = builder;
	}
	@Override
	public void setPerfect(boolean isPerfect) {
		perfect = isPerfect;
	}
	
	/*
	private Maze loadMazeConfigurationFromFile(String filename) {
		// load maze from file
		MazeFileReader mfr = new MazeFileReader(filename) ;
		// obtain MazeConfiguration
		return mfr.getMazeConfiguration();
	}
	*/
	
	public void start(Controller controller, MazePanel panel) {
		started = true;
		control = controller;
		
		percentdone = 0;
		
		
		if (filename != null) {
			//deliver(loadMazeConfigurationFromFile(filename));
			filename = null;  
		} else {
			assert null != factory : "Controller.init: factory must be present";
			//draw();
			factory.order(this) ;
		}
	}
	
	public void deliver(Maze mazeConfig) {
		this.mazeConfig=mazeConfig;
	}
	
	@Override
	public int getSkillLevel() {
		return skillLevel;
	}
	@Override
	public Builder getBuilder() {
		return builder;
	}
	@Override
	public boolean isPerfect() {
		return perfect;
	}
	public int getPercentDone() {
		return percentdone;
	}
	
    @Override
    public void updateProgress(int percentage) {
        /*
        if (this.percentdone < percentage && percentage <= 100) {
            this.percentdone = percentage;
            draw() ;
        }
        */
    }
    
    public Maze getMaze(){
    	return mazeConfig;
    }
	
}

