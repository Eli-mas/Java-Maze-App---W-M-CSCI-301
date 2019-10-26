package gui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import generation.Floorplan;
import generation.Maze;
import generation.MazeTestGenerator;
import gui.Robot.Direction;

public abstract class AbstractRobotDriverTest {
	
	Controller controller;
	Robot robot;
	AbstractRobotDriver driver;
	Maze maze;
	Floorplan floorplan;
	Distance distance;
	
	abstract void setDriver();
	
	void setMaze(boolean perfect, boolean deterministic, int level) {
		Controller.suppressUpdates=true;
		Controller.suppressWarnings=true;
		BasicRobot.VERBOSE=false;
		//System.out.println("testing maze at level "+level);
		controller=MazeTestGenerator.getController(perfect, deterministic, level);
		robot=controller.getRobot();
		maze = controller.getMazeConfiguration();
		
		setDriver();
		driver.setRobot(robot);
		driver.setController(controller);
		
		
		//System.out.printf("WallFollowerTest: robot set, robot cd=%s, driver cd =%s\n",robot.getCurrentDirection(),driver.getCurrentDirection());
		
		robot.setBatteryLevel(maze.getMazedists().getMaxDistance()*100);
		//System.out.println("WallFollowerTest.setMaze: robot initial energy is "+robot.getBatteryLevel());
	}
	
	void testExit(Direction... directionsOfSensorFailure) {
		setMaze(true, true, 0);
		
		/*
		for(CardinalDirection cd: CardinalDirection.values()) System.out.println(cd+": "+Arrays.toString(cd.getDirection()));
		System.out.println(Arrays.deepToString(controller.getMazeConfiguration().getMazedists().getAllDistanceValues()).replace("], [", "],\n["));
		System.out.println("\nstart position: "+Arrays.toString(driver.getRobotPosition())+", cd: "+robot.getCurrentDirection());
		System.out.println("exit position: "+Arrays.toString(controller.getMazeConfiguration().getMazedists().getExitPosition()));
		System.out.println("exit direction: "+Arrays.toString(MazeMath.getCardinalDirectionOfMazeExit(controller.getMazeConfiguration()).getDirection())+"\n");
		 */
		
		for(Direction d: directionsOfSensorFailure) {
			if(null==d) {
				//System.out.println("null found in testExit: breaking");
				break;
			}
			robot.triggerSensorFailure(d);
		}
		
		for(Direction d: directionsOfSensorFailure) {
			assertFalse(robot.hasOperationalSensor(d));
		}
		
		try {
			boolean exited = driver.drive2Exit();
			assertTrue(exited);
			assertTrue(null==driver.getRobotPosition());
			//System.out.println("WallFollowerTest: the robot is out of the maze");
			//System.out.println("robot position: " + Arrays.toString(driver.getRobotPosition()));
		} catch (Exception e) {
			System.out.println("");
			// TODO Auto-generated catch block
			System.out.println("drive to exit failed: "+e.getMessage());
			e.printStackTrace();
			
			prepareForMazePlot();
		}
	}
	
	void prepareForMazePlot() {
		System.out.println("\n\n\n");
		Maze maze = controller.getMazeConfiguration();
		int w=maze.getWidth(),h=maze.getHeight();
		Floorplan floorplan = maze.getFloorplan();
		System.out.print("walls=\"\"\"");
		for(int x=0; x<w; x++) {
			for(int y=0; y<h; y++) {
				System.out.print(x+" "+y+" : ");
				for(CardinalDirection cd: MazeMath.WestSouthEastNorth) {
					System.out.print(floorplan.hasWall(x, y, cd)+" ");
				}
				System.out.println();
			}
		}
		System.out.println("\"\"\"");
		
		int pcount=1;
		System.out.print("positions=np.array([");
		for(int[] p: driver.locations) {
			System.out.print(Arrays.toString(p)+", ");
			pcount++;
			if(10==pcount) {
				System.out.println();
				pcount=0;
			}
		}
		System.out.println("])\nmazeplot()\n");
		
		assertTrue(false);
	}
	
	@Test
	public void testExit() {
		testExit((Robot.Direction)null);
		
		testExit(Direction.LEFT);
		testExit(Direction.RIGHT);
		testExit(Direction.FORWARD);
		testExit(Direction.BACKWARD);
		
		testExit(Direction.LEFT,Direction.RIGHT);
		testExit(Direction.LEFT,Direction.FORWARD);
		testExit(Direction.LEFT,Direction.BACKWARD);
		testExit(Direction.RIGHT,Direction.FORWARD);
		testExit(Direction.RIGHT,Direction.BACKWARD);
		testExit(Direction.FORWARD,Direction.BACKWARD);
		
		testExit(Direction.LEFT,Direction.RIGHT,Direction.FORWARD);
		testExit(Direction.RIGHT,Direction.FORWARD,Direction.BACKWARD);
		testExit(Direction.FORWARD,Direction.BACKWARD,Direction.LEFT);
		testExit(Direction.BACKWARD,Direction.LEFT,Direction.RIGHT);
	}
	
}
