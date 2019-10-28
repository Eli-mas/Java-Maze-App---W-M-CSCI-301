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

public class Wizard extends AbstractRobotDriver {
	
	Queue<RobotOperation> opQueue;
	//ExtendedList<RobotOperation> operations;
	Maze maze;
	
	
	private void setOperations() {
		opQueue = new LinkedList<RobotOperation>(RobotOperationTracker.getOperationsFrom(maze));
		/*this.operations= new ExtendedList<RobotOperation>(
							RobotOperationTracker.getOperationsFrom(maze));
		System.out.println("setting operations in tracker in Wizard");
		for(int i=0; i<operations.size(); i++) {
			//System.out.println(operations.get(i)+" "+opQueue.poll());
			assert operations.get(i).equals(opQueue.poll());
		}*/
	}
	
	private void performNextOperation() {
		RobotOperation op = opQueue.poll();
		op.operateRobot(robot);
		
	}
	
	private boolean basicWalk() throws Exception {
		faceRobot(RobotOperationTracker.STARTING_CARDINAL_DIRECTION);
		while(opQueue.size()>0) {
			performNextOperation();
			
			if(robot.hasStopped()) throw new Exception(
					"Exception in Wizard.basicWalk: "+controller.getRobotFailureMessage());
			
			Thread.sleep(walkDelay);
			if(interrupted) {
				return false;
			}
			
		}
		return true;
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
