package gui;

import comp.ExtendedList;
import comp.MazeMath;
import comp.RobotOperation;
import comp.RobotOperationTracker;
import generation.CardinalDirection;
import generation.Distance;
import generation.Maze;
import gui.Robot.Direction;
import gui.Robot.Turn;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 
 * The Wizard class has full knowledge of the maze in which
 * it operates and uses this to its advantage.
 * 
 * @author Elijah Mas
 *
 */
public class Wizard extends AbstractRobotDriver {
	
	/**
	 * Track which robot operations are to be performed
	 */
	Queue<RobotOperation> opQueue;
	
	/**
	 * The maze to navigate
	 */
	Maze maze;
	
	
	/**
	 * Establish all the operations that the robot will perform in advance.
	 */
	private void setOperations() {
		opQueue = new LinkedList<RobotOperation>(RobotOperationTracker.getOperationsFrom(maze));
	}
	
	/**
	 * Get the next operation in the queue
	 * and act on the robot.
	 */
	private void performNextOperation() {
		RobotOperation op = opQueue.poll();
		op.operateRobot(robot);
	}
	
	/**
	 * Walk through every operation specified by
	 * the {@link #opQueue} queue. Includes
	 * rotations and moves.
	 * @return true if the walk is successful, false if otherwise.
	 * @throws Exception if the robot encounters an error along the way
	 */
	private boolean basicWalk() throws Exception {
		// the operations are constructed under the assumption
		// that the robot is facing a fixed direction
		// ensure this to be true
		faceRobot(RobotOperationTracker.STARTING_CARDINAL_DIRECTION);
		
		// queue has zero size when all operations are performed
		while(opQueue.size()>0) {
			// internally removes an operation from the queue and performs it
			performNextOperation();
			
			// make sure robot is operational
			if(robot.hasStopped()) throw new Exception(
					"Exception in Wizard.basicWalk: "+controller.getRobotFailureMessage());
			
			//wait an interval for next operation
			Thread.sleep(walkDelay);
			
			// exit if interrupted
			if(interrupted) {
				return false;
			}
			
		}
		return true;
	}
	
	/**
	 * Set maze and dimensions.
	 * @param maze a {@link Maze} instance
	 */
	public void setMaze(Maze maze) {
		this.maze=maze;
		setDimensions(maze.getWidth(),maze.getHeight());
	}
	
	
	/**
	 * Default constructor does nothing.
	 */
	public Wizard() {
		
	}

	/**
	 * <b>Not implemented</b>:
	 * the Wizard class has access to full maze data,
	 * so there is no need to rely on sensors.
	 * This method purposely does nothing.
	 */
	@Override
	public void triggerUpdateSensorInformation() {
		
	}
	
	/**
	 * Construct the series of operations to be performed
	 * on the robot, and then perform them all.
	 * 
	 * @return whether the robot has left the maze
	 */
	@Override
	public boolean drive2Exit() throws Exception {
		setOperations();
		basicWalk();
		return tellIfOutsideMaze();
	}
	
}
