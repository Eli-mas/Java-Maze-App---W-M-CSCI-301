package comp;

import gui.Robot;

/**
 * This class is extended by classes that operate on a robot.
 * Extensions are utilized in {@link RobotOperationTracker} to
 * construct paths through mazes.
 * 
 * @author Elijah Mas
 *
 */
public abstract class RobotOperation{
	
	public abstract void operateRobot(Robot robot);
	
}