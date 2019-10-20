package comp;

import gui.Robot;
import gui.Robot.Turn;

/**
 * Provides a mechanism to move a robot by a certain distance.
 * Requires no knowledge of the {@link RobotOperationTracker}
 * in which it is embedded.
 * 
 * @author Elijah Mas
 *
 */
public class RobotRotation extends RobotOperation{
	/**
	 * {@link Turn} to apply to rotate an operated robot
	 */
	private Turn turn;
	
	public RobotRotation(Turn turn) {
		this.turn=turn;
	}
	
	public Turn getTurn() {
		return turn;
	}
	
	/**
	 * Rotate a robot by the internally stored {@link #turn}
	 * @param robot a robot to operate on
	 */
	public void operateRobot(Robot robot) {
		if(null!=turn) {
			robot.rotate(turn);
		}
	}
	
	/*
	@Override
	public String toString() {
		return String.format("Rotation<%s>",turn);
	}
	*/
}