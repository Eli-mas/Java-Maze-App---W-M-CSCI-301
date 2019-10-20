package gui;

import comp.ExtendedList;
import comp.RobotOperation;
import comp.RobotOperationTracker;
import generation.Distance;
import generation.Maze;

public class Wizard extends AbstractRobotDriver {
	
	Robot robot;
	int width, height;
	Distance distance;
	
	ExtendedList<RobotOperation> operations;
	Maze maze;
	
	
	private void setOperations() {
		this.operations=(ExtendedList<RobotOperation>)
							RobotOperationTracker.getOperationsFrom(maze);
	}
	
	private void basicWalk() throws Exception {
		for(RobotOperation op: operations) {
			op.operateRobot(robot);
			if(robot.hasStopped()) throw new Exception("Exception in Wizard.basicWalk: "+control.getRobotFailureMessage());
		}
		
	}
	
	
	
	
	
	public Wizard() {
		
	}
	
	public Wizard(Robot robot) {
		setRobot(robot);
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
