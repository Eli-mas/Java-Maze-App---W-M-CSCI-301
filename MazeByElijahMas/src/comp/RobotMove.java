package comp;

import gui.Robot;

/**
 * Provides a mechanism to move a robot by a certain distance.
 * Also interacts with {@link RobotOperationTracker} to keep track
 * of the distances traveled through mazes.
 * 
 * @author Elijah Mas
 *
 */
public class RobotMove extends RobotOperation{
	/**
	 * distance to move an operated robot
	 */
	private int distance=0;
	
	/**
	 * Indicate that the current move operation proceeds
	 * over another cell without changing direction.
	 * @param tracker a {@link RobotOperationTracker} in which this is embedded
	 */
	public void incrementDistance(RobotOperationTracker tracker) {
		distance++;
		tracker.incrementTotalDistance();
	}
	
	public int getDistance() {
		return distance;
	}
	
	public RobotMove() {}
	
	public RobotMove(int distance) {
		this.distance=distance;
	}
	
	/**
	 * Move a robot by the internally stored {@link #distance}
	 * @param robot a robot to operate on
	 */
	public void operateRobot(Robot robot) {
		robot.move(distance, false);
	}
	
	/*
	@Override
	public String toString() {
		return String.format("Move(%d)",distance);
	}
	*/
}