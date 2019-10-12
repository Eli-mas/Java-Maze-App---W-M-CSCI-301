package gui;

import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BasicRobotTest {
	private BasicRobot robot1;
	private Robot robot2;
	
	private void instantiateApplication() {
		
	}
	
	
	/*
	 * test cases
	 * 
	 * does robot exit maze when it has just enough energy to do so?
	 * 
	 * does robot exit maze when it has more than enough energy?
	 * 
	 * in a rotation, do the distances shift as expected?
	 * 
	 * in a move forward, do forward and backward distances change as expected?
	 * 
	 * Immediately after a jump, is the backwards distance always 0?
	 * 
	 * If distance to wall is 0 in any direction, does rotating to face it and moving into it cause a crash?
	 * 
	 * If a robot is about to crash but does not have energy to move, does it run out of energy first before crashing?
	 * If a robot is about to jump out of maze but does not have sufficient energy, does it run out of energy first before bad jump?
	 * 
	 * Operations that could cause energy depletion:
	 *     move
	 *     jump
	 *     rotate
	 *     distance sensing
	 * 
	 * 
	 * 
	 * getCurrentPosition, getCurrentDirection:
	 *     assert equal to corresponding values in StatePlaying/Controller, should already be done
	 * 
	 * setMaze
	 *     ...
	 * 
	 * getBatteryLevel, setBatteryLevel
	 * 
	 * getOdometerReading, resetOdometer
	 *     verify that resetOdometer brings distance to 0
	 * 
	 * getEnergyForFullRotation, getEnergyForStepForward
	 *     ...
	 * 
	 * isAtExit
	 *     assert that this agrees with exit value in MazeConfiguration
	 * 
	 * isInsideRoom
	 *     assert that this agrees with exit value in floorplan
	 * 
	 * canSeeThroughTheExitIntoEternity
	 *     assert that the robot is either in the same x or same y position as exit
	 *     assert that there are no walls in floorplan between robot and exit
	 * 
	 * distanceToObstacle
	 *     assert that if the robot moves (distance+1), it crashes
	 * 
	 * hasRoomSensor, hasOperationalSensor, hasStopped
	 *     only thing I can think of is to let the robot run a while and make sure these sensors do not change operationality unless explicitly told to do so
	 * 
	 * triggerSensorFailure, repairFailedSensor
	 *     make sure that after calling these, hasOperationalSensor returns false/true respectively
	 * 
	 * rotate
	 *     assert that has changed as expected:
	 *         energy -= 3
	 *         getCurrentDirection()
	 *         distanceToObstacle() in each direction
	 *     
	 *     assert that has not changed:
	 *         position
	 *         canSeeThroughTheExitIntoEternity()
	 *         isInsideRoom()
	 *     
	 *     assert no rotation when
	 *         robot has stopped
	 *         robot lacks sufficient energy
	 *         (how to assert no rotation):
	 *             current absolute direction is same
	 *             distances are the same in all directions
	 *             energy has not changed
	 * 
	 * move
	 *     assert that has changed as expected
	 *         position
	 *         energy -= 7 (5 for move, two for sensing wall distances in two side directions)
	 *         distanceToObstacle() in forward/backward directions
	 *     
	 *     assert that crash happens when expected
	 *         if distance to wall is d, moving (d+1) towards that wall crashes (causes robot to stop)
	 *     
	 *     assert no move when
	 *         robot has stopped
	 *         robot lacks sufficient energy
	 *         (how to assert no move):
	 *             current position is same
	 *             distances are the same in all directions
	 *             energy has not changed
	 * 
	 * jump
	 *     assert that has changed as expected
	 *         position
	 *         energy -= 53 (50 for jump, three for sensing wall distances in all but backward direction)
	 *         distanceToObstacle()==0 in backward direction
	 *     
	 *     assert that stop happens when expected
	 *         if robot is at maze periphery and tries to jump out
	 *     
	 *     assert that jump does not occur when a move suffices
	 *         check that energy has decreased only by 7
	 *     
	 *     assert no jump when
	 *         robot has stopped
	 *         robot lacks sufficient energy
	 *         (how to assert no jump):
	 *             current position is same
	 *             distances are the same in all directions
	 *             energy has not changed
	 */
}
