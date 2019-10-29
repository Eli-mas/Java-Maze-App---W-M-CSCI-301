package gui;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import comp.MazeMath;

import org.junit.Test;

import generation.CardinalDirection;
import generation.Floorplan;
import generation.Maze;
import generation.MazeTestGenerator;
import gui.Robot.Direction;

/**
 * Tests the operations of the {@link WallFollower} class.
 * @author Elijah Mas
 *
 */
public class WallFollowerTest extends AbstractRobotDriverTest {
	
	@Override
	void setDriver() {
		driver = new WallFollower();
	}
	
	/**
	 * Test some specific cases where the closest operational sensor
	 * to a given sensor is known.
	 */
	@Test
	public void testGetClosestOperationalSensor() {
		//assume maze and fields have already been instantiated
		setMaze(false, true, 0);
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		
		CardinalDirection cardinalDirectionOfNextFailure = robot_cd;
		// this sensor is operational, so the closest sensor should be itself
		assertEquals(robot_cd, driver.getClosestFunctionalSensor(robot_cd));
		// test the same thing, but test that the MazeMath method works correctly here
		assertEquals(Direction.FORWARD, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.FORWARD);
		
		//repeat similar block of tests to above
		
		// forward inoperable: now the closest sensor is on the right
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, 1);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.RIGHT, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.RIGHT);
		
		// forward, right inoperable: now the closest sensor is on the left
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, -1);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.LEFT, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.LEFT);
		
		// forward, right, left inoperable: now the closest sensor is at the back
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, 2);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.BACKWARD, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.BACKWARD);
		
		// forward, right, left backward: no sensors are functional
		assertTrue(null==driver.getClosestFunctionalSensor(robot_cd));
	}
	
	/**
	 * Automate testing of {@link #failSensor(CardinalDirection, Direction)}
	 * method over all possible (relative direction, absolute direction) pairs. 
	 */
	@Test
	public void failForwardSensor() {
		setMaze(false, true, 0);
		for(CardinalDirection cd: CardinalDirection.values()) {
			for(Direction d: Direction.values()) {
				failSensor(cd,d);
			}
		}
	}
	
	/**
	 * Test that when a sensor is failed, the driver correctly
	 * reports the direction of the closest functioning sensor
	 * relative to each Cardinal direction
	 * @param robot_cd cardinal direction of the robot to face
	 * @param failure_d relative direction on the robot to trigger sensor failure
	 */
	private void failSensor(CardinalDirection robot_cd, Direction failure_d) {
		//make sure no sensors are inoperable before starting
		for(Direction d: Direction.values()) {
			robot.repairFailedSensor(d);
		}
		
		//face the robot towards robot_cd
		driver.faceRobot(robot_cd);
		//face the sensor in direction failure_d
		robot.triggerSensorFailure(failure_d);
		CardinalDirection failure_cd = MazeMath.convertDirs(failure_d, robot_cd);
		
		// get a list of cardinal directions in rightwards order
		// starting at the direction of the sensor failure
		CardinalDirection[] cardinalDirectionsInRightwardsOrder = new CardinalDirection[4];
		for(int i=0; i<4; i++) {
			cardinalDirectionsInRightwardsOrder[i]=MazeMath.getFrom(failure_cd, i);
		}
		
		// map each cardinal direction to another cardinal direction
		// which we expect to be the direction of its closest functioning sensor
		CardinalDirection[] expectedClosestOperationalSensors = new CardinalDirection[4];
		
		// we expected that:
		//     forward (index 0): closest sensor is right (index 1)
		//     right (index 1): closest sensor is right (index 1)
		//     backward (index 2): closest sensor is backward (index 2)
		//     backward (index 3): closest sensor is backward (index 3)
		
		int[] closestSensorIndices = new int[]{1,1,2,3};
		int index=0;
		for(int i:closestSensorIndices) {
			CardinalDirection sensorCD = cardinalDirectionsInRightwardsOrder[i];
			expectedClosestOperationalSensors[index] = sensorCD;
			index++;
		}
		
		// compare results
		for(int i=0; i<4; i++) {
			CardinalDirection closestOperational = driver.getClosestFunctionalSensor(cardinalDirectionsInRightwardsOrder[i]);
			CardinalDirection expected = expectedClosestOperationalSensors[i];
			assertEquals(	"robot_cd="+robot_cd+", failed sensor at "+failure_cd+
							"; expected "+expected+", got "+closestOperational,
							expected, closestOperational);
		}
		
	}
	
	/**
	 * Get an array of robot distances in each direction,
	 * starting at forward and moving rightwards.
	 * @return array of distances
	 */
	private int[] getRobotDistances() {
		int[] dists = new int[4];
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			dists[index++]=robot.distanceToObstacle(d);
		}
		return dists;
	}
	
	/**
	 * Test that when a single sensor fails in any direction,
	 * we can still get the distance in any direction
	 * by way of the driver's internal logic.
	 */
	@Test
	public void testGetDistance_FailSingle() {
		// give the robot a maze to work with
		setMaze(false, true, 0);
		
		//get distances in all directions
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			//fail a single sensor
			robot.triggerSensorFailure(d);
			
			// make sure, for each direction, that the distance reported by the driver's logic to handle missing sensors
			// yields consistent results with distances reported when all sensors are operational
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			// test passed: repair the sensor, move to the next direction
			robot.repairFailedSensor(d);
			index++;
		}
	}
	
	/**
	 * Test that when three sensors fail in any directions,
	 * we can still get the distance in any direction
	 * by way of the driver's internal logic.
	 */
	@Test
	public void testGetDistance_FailTriple() {
		// give the robot a maze to work with
		setMaze(false, true, 0);
		//get distances in all directions
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			//fail three sensors
			robot.triggerSensorFailure(d);
			robot.triggerSensorFailure(MazeMath.getFrom(d, 1));
			robot.triggerSensorFailure(MazeMath.getFrom(d, -1));
			
			// make sure, for each direction, that the distance reported by the driver's logic to handle missing sensors
			// yields consistent results with distances reported when all sensors are operational
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			// test passed: repair the sensors, move to the next direction
			robot.repairFailedSensor(d);
			robot.repairFailedSensor(MazeMath.getFrom(d, 1));
			robot.repairFailedSensor(MazeMath.getFrom(d, -1));
			index++;
		}
	}
	
	/**
	 * Test that when two sensors fail in any directions,
	 * we can still get the distance in any direction
	 * by way of the driver's internal logic.
	 * 
	 * Tests on different possible combinations of sensor failures on
	 * {@link #testGetDistance_FailDouble_i(int)} method.
	 */
	@Test
	public void testGetDistance_FailDouble() {
		setMaze(false, true, 0);
		testGetDistance_FailDouble_i(1);
		testGetDistance_FailDouble_i(-1);
		testGetDistance_FailDouble_i(2);
	}
	
	/**
	 * Test that when two sensors fail in specified directions,
	 * we can still get the distance in any direction
	 * by way of the driver's internal logic.
	 * 
	 * @param directionOfSecondFailure integer corresponding to direction of second sensor failure
	 */
	private void testGetDistance_FailDouble_i(int directionOfSecondFailure) {
		//maze is set in testGetDistance_FailDouble
		//get distances in all directions
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			//fail two sensors
			robot.triggerSensorFailure(d);
			robot.triggerSensorFailure(MazeMath.getFrom(d, directionOfSecondFailure));
			
			// make sure, for each direction, that the distance reported by the driver's logic to handle missing sensors
			// yields consistent results with distances reported when all sensors are operational
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			// test passed: repair the sensors, move to the next direction
			robot.repairFailedSensor(d);
			robot.repairFailedSensor(MazeMath.getFrom(d, directionOfSecondFailure));
			index++;
		}
	}
	
	/**
	 * Iterate {@link #assertFaceRobot(CardinalDirection)}
	 * over the cardinal directions.
	 */
	@Test
	public void testFaceRobot() {
		setMaze(false, true, 0);
		assertFaceRobot(CardinalDirection.East);
		assertFaceRobot(CardinalDirection.West);
		assertFaceRobot(CardinalDirection.North);
		assertFaceRobot(CardinalDirection.South);
	}
	
	/**
	 * Test that when the robot is told to face a certain direction,
	 * It actually faces that direction.
	 * @param cd a cardinal direction
	 */
	private void assertFaceRobot(CardinalDirection cd) {
		driver.faceRobot(cd);
		CardinalDirection robot_cd = robot.getCurrentDirection();
		assertEquals("robot should be facing "+cd+" but is facing "+robot_cd,robot_cd,cd);
	}
}
