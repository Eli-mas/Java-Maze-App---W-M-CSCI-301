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

/**
 * Class that other test classes can inherit from.
 * Tests that a driver algorithm can walk a robot
 * to the exit.
 * 
 * Some methods are not tested here because they are tested
 * by assertions in production environment--e.g.
 * {@link RobotDriver#getEnergyConsumption()},
 * {@link RobotDriver#getPathLength()}.
 * @author Elijah Mas
 *
 */
public abstract class AbstractRobotDriverTest {
	
	/**
	 * reference to a {@link Controller}
	 */
	Controller controller;
	
	/**
	 * reference to a {@link Robot}
	 */
	Robot robot;
	
	/**
	 * reference to a {@link AbstractRobotDriver}
	 */
	AbstractRobotDriver driver;
	
	/**
	 * reference to a {@link Maze}
	 */
	Maze maze;
	
	/**
	 * reference to a {@link Floorplan}
	 */
	Floorplan floorplan;
	
	/**
	 * reference to a {@link Distance}
	 */
	Distance distance;
	
	/**
	 * Set a driver and do other initializations;
	 * details handled in subclasses.
	 */
	abstract void setDriver();
	
	/**
	 * Generate a maze and link all relevant data structures.
	 * @param perfect maze perfect/imperfect
	 * @param deterministic deterministic/non-deterministic generation
	 * @param level difficulty level
	 */
	void setMaze(boolean perfect, boolean deterministic, int level) {
		// no GUI, so no delay between operations
		AbstractRobotDriver.walkDelay=0;
		
		//keep terminal clear
		Controller.suppressUpdates=true;
		Controller.suppressWarnings=true;
		BasicRobot.VERBOSE=false;
		
		//System.out.println("testing maze at level "+level);
		controller=MazeTestGenerator.getController(perfect, deterministic, level);
		robot=controller.getRobot();
		maze = controller.getMazeConfiguration();
		
		// set robot and driver
		setDriver();
		driver.setRobot(robot);
		driver.setController(controller);
		
		robot.setBatteryLevel(maze.getMazedists().getMaxDistance()*100);
	}
	
	/**
	 * Test that the driver algorithm exits the maze.
	 * @param directionsOfSensorFailure directions of sensors to disable
	 */
	void testExit(Direction... directionsOfSensorFailure) {
		if(driver instanceof WallFollower) setMaze(true, true, 0);
		else setMaze(false, true, 1);
		
		// fail specified sensors
		for(Direction d: directionsOfSensorFailure) {
			// null means all sensors operational
			if(null==d) {
				break;
			}
			robot.triggerSensorFailure(d);
		}
		
		// make sure sensor failure worked as intended
		for(Direction d: directionsOfSensorFailure) {
			assertFalse(robot.hasOperationalSensor(d));
		}
		
		try {
			boolean exited = driver.drive2Exit();
			if(!exited) {
				throw new Exception();
			}
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
	
	/**
	 * Exports data in a format for use in a Python script.
	 * See 'sample_debug.py' in the 'extra' folder for an example of
	 * how to use this output.
	 */
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
	
	/**
	 * Iterate on {@link #testExit(Direction...)} over
	 * all possible combinations of sensor failure directions,
	 * including the case where no sensors fail.
	 */
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
