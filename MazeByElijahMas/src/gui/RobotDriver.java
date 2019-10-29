package gui;

import generation.Distance;

/**
 * This interface specifies a robot driver that operates a robot to escape from a given maze. 
 * 
 * Collaborators: {@link Robot}
 * 
 * Implementing classes: {@link WallFollower}, {@link Wizard}
 * 
 * New for Project 4: interrupt method
 * 
 * @author Peter Kemper
 *
 */
public interface RobotDriver {
	
	/**
	 * Assigns a robot platform to the driver. 
	 * The driver uses a robot to perform, this method provides it with this necessary information.
	 * @param r robot to operate
	 */
	void setRobot(Robot r);
	
	/**
	 * Provides the robot driver with information on the dimensions of the 2D maze
	 * measured in the number of cells in each direction.
	 * @param width of the maze
	 * @param height of the maze
	 * @precondition 0 <= width, 0 <= height of the maze.
	 */
	void setDimensions(int width, int height);
	
	/**
	 * Provides the robot driver with information on the distance to the exit.
	 * Only some drivers such as the wizard rely on this information to find the exit.
	 * @param distance gives the length of path from current position to the exit.
	 * @precondition null != distance, a full functional distance object for the current maze.
	 */
	void setDistance(Distance distance);
	
	/**
	 * Tells the driver to check its robot for operational sensor. 
	 * If one or more of the robot's distance sensor become 
	 * operational again after a repair operation, this method
	 * allows to make the robot driver aware of this change 
	 * and to bring its understanding of which sensors are operational
	 * up to date.  
	 */
	void triggerUpdateSensorInformation();
	
	/**
	 * Drives the robot towards the exit given it exists and 
	 * given the robot's energy supply lasts long enough. 
	 * @return true if driver successfully reaches the exit, false otherwise
	 * @throws exception if robot stopped due to some problem, e.g. lack of energy
	 */
	boolean drive2Exit() throws Exception;
	
	/**
	 * Returns the total energy consumption of the journey, i.e.,
	 * the difference between the robot's initial energy level at
	 * the starting position and its energy level at the exit position. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	float getEnergyConsumption();
	
	/**
	 * Returns the total length of the journey in number of cells traversed. 
	 * Being at the initial position counts as 0. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	int getPathLength();
	
	/**
	 * Changes internal state of the driver to realize that the algorithm has been
	 * interrupted and should prematurely terminate.
	 * 
	 * When interrupted, drive2Exit should return false.
	 * 
	 * In order for this to have the intended effect, drive2Exit should be called
	 * within a thread separate from the main thread;
	 * otherwise this method has no effect.
	 */
	void interrupt();
	
}
