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
	
	@Override
	public String toString() {
		return String.format("Rotation<%s>",turn);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((turn == null) ? 0 : turn.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RobotRotation other = (RobotRotation) obj;
		if (turn != other.turn)
			return false;
		return true;
	}
}