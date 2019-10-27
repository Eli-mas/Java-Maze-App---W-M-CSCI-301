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

public class Wizard extends AbstractRobotDriver {
	
	ExtendedList<RobotOperation> operations;
	Maze maze;
	
	
	private void setOperations() {
		this.operations= new ExtendedList<RobotOperation>(
							RobotOperationTracker.getOperationsFrom(maze));
	}
	
	private void basicWalk() throws Exception {
		faceRobot(RobotOperationTracker.STARTING_CARDINAL_DIRECTION);
		
		for(RobotOperation op: operations) {
			op.operateRobot(robot);
			if(robot.hasStopped()) throw new Exception(
					"Exception in Wizard.basicWalk: "+controller.getRobotFailureMessage());
			Thread.sleep(walkDelay);
		}
		
	}
	
	public void setMaze(Maze maze) {
		this.maze=maze;
	}
	
	
	
	public Wizard() {
		
	}

	/**
	 * <b>Not implemented</b>:
	 * the Wizard class has access to full maze data,
	 * so there is no need to rely on sensors.
	 *
	 */
	@Override
	public void triggerUpdateSensorInformation() {
		
	}
	
	@Override
	public boolean drive2Exit() throws Exception {
		setOperations();
		basicWalk();
		return tellIfOutsideMaze();
	}
	
}
