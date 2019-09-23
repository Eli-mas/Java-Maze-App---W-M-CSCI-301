package generation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Test;

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
	
	@Test
	final public void testMazeDists(){
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
		
		
		Maze maze = order.getMaze();
		Distance dists=maze.getMazedists();
		int[][] distVals=dists.getAllDistanceValues();
		
		System.out.println("mazedists:\n"+Arrays.deepToString(distVals));
		System.out.println("floorplan:\n"+maze.getFloorplan());
		
		assertEquals(0,0);
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

