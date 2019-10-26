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

public class WallFollowerTest extends AbstractRobotDriverTest {
	
	@Override
	void setDriver() {
		driver = new WallFollower();
	}
	
	@Test
	public void testGetClosestOperationalSensor() {
		//assume maze and fields have already been instantiated
		setMaze(false, true, 0);
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		
		CardinalDirection cardinalDirectionOfNextFailure = robot_cd;
		assertEquals(robot_cd, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.FORWARD, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.FORWARD);
		
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, 1);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.RIGHT, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.RIGHT);
		
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, -1);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.LEFT, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.LEFT);
		
		cardinalDirectionOfNextFailure = MazeMath.getFrom(robot_cd, 2);
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(robot_cd));
		assertEquals(Direction.BACKWARD, MazeMath.convertDirs(cardinalDirectionOfNextFailure, robot_cd));
		robot.triggerSensorFailure(Direction.BACKWARD);
		
		assertTrue(null==driver.getClosestFunctionalSensor(robot_cd));
	}
	
	/*private void failSensor(CardinalDirection cardinalDirectionOfNextFailure,
							Direction expectedRelativeDirectionOfNextFailure) {
		
		assertEquals(cardinalDirectionOfNextFailure, driver.getClosestFunctionalSensor(getRobotCD()));
		assertEquals(expectedRelativeDirectionOfNextFailure, MazeMath.convertDirs(cardinalDirectionOfNextFailure, getRobotCD()));
		robot.triggerSensorFailure(expectedRelativeDirectionOfNextFailure);
	}*/
	
	@Test
	public void failForwardSensor() {
		setMaze(false, true, 0);
		for(CardinalDirection cd: CardinalDirection.values()) {
			for(Direction d: Direction.values()) {
				failSensor(cd,d);
			}
		}
	}
	
	private void failSensor(CardinalDirection robot_cd, Direction failure_d) {
		for(Direction d: Direction.values()) {
			robot.repairFailedSensor(d);
		}
		
		driver.faceRobot(robot_cd);
		robot.triggerSensorFailure(failure_d);
		CardinalDirection failure_cd = MazeMath.convertDirs(failure_d, robot_cd);
		
		CardinalDirection[] cardinalDirectionsInRightwardsOrder = new CardinalDirection[4];
		for(int i=0; i<4; i++) {
			cardinalDirectionsInRightwardsOrder[i]=MazeMath.getFrom(failure_cd, i);
		}
		
		CardinalDirection[] expectedClosestOperationalSensors = new CardinalDirection[4];
		int[] closestSensorIndices = new int[]{1,1,2,3};
		int index=0;
		for(int i:closestSensorIndices) {
			CardinalDirection sensorCD = cardinalDirectionsInRightwardsOrder[i];
			expectedClosestOperationalSensors[index] = sensorCD;
			index++;
		}
		
		for(int i=0; i<4; i++) {
			CardinalDirection closestOperational = driver.getClosestFunctionalSensor(cardinalDirectionsInRightwardsOrder[i]);
			CardinalDirection expected = expectedClosestOperationalSensors[i];
			assertEquals(	"robot_cd="+robot_cd+", failed sensor at "+failure_cd+
							"; expected "+expected+", got "+closestOperational,
							expected, closestOperational);
		}
		
	}
	
	private int[] getRobotDistances() {
		int[] dists = new int[4];
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			dists[index++]=robot.distanceToObstacle(d);
		}
		return dists;
	}
	
	@Test
	public void testGetDistance_FailSingle() {
		setMaze(false, true, 0);
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			robot.triggerSensorFailure(d);
			
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			robot.repairFailedSensor(d);
			index++;
		}
	}
	
	@Test
	public void testGetDistance_FailTriple() {
		setMaze(false, true, 0);
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			robot.triggerSensorFailure(d);
			robot.triggerSensorFailure(MazeMath.getFrom(d, 1));
			robot.triggerSensorFailure(MazeMath.getFrom(d, -1));
			
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			robot.repairFailedSensor(d);
			robot.repairFailedSensor(MazeMath.getFrom(d, 1));
			robot.repairFailedSensor(MazeMath.getFrom(d, -1));
			index++;
		}
	}
	
	@Test
	public void testGetDistance_FailDouble() {
		setMaze(false, true, 0);
		testGetDistance_FailDouble_i(1);
		testGetDistance_FailDouble_i(-1);
		testGetDistance_FailDouble_i(2);
	}
	
	private void testGetDistance_FailDouble_i(int directionOfSecondFailure) {
		int[] dists = getRobotDistances();
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		int index=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			robot.triggerSensorFailure(d);
			robot.triggerSensorFailure(MazeMath.getFrom(d, directionOfSecondFailure));
			
			for(int i=0; i<4; i++) {
				assertTrue(driver.__getDistance__(MazeMath.convertDirs(MazeMath.getFrom(d, i), robot_cd))==dists[Math.floorMod(index+i,4)]);
			}
			
			robot.repairFailedSensor(d);
			robot.repairFailedSensor(MazeMath.getFrom(d, directionOfSecondFailure));
			index++;
		}
	}
	
	@Test
	public void testFaceRobot() {
		setMaze(false, true, 0);
		assertFaceRobot(CardinalDirection.East);
		assertFaceRobot(CardinalDirection.West);
		assertFaceRobot(CardinalDirection.North);
		assertFaceRobot(CardinalDirection.South);
	}
	
	private void assertFaceRobot(CardinalDirection cd) {
		driver.faceRobot(cd);
		CardinalDirection robot_cd = robot.getCurrentDirection();
		assertEquals("robot should be facing "+cd+" but is facing "+robot_cd,robot_cd,cd);
	}
}
