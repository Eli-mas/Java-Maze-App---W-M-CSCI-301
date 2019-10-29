package gui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * The WallFollower algorithm does not rely on any knowledge
 * of the maze beyond what the robot can see immediately around it.
 * It traces the robot's leftward wall until an exit it found.
 * In imperfect mazes, there is a possibility that
 * this algorithm gets stuck on an inner wall
 * that never touches the maze's outer wall,
 * in which case it does not converge to the exit.
 * 
 * @author Elijah Mas
 *
 */
public class WallFollower extends AbstractRobotDriver {
	
	/**
	 * used to slow down operations when sensors become inoperable
	 */
	private int delayExpand=1;
	
	/**
	 * default constructor does nothing
	 */
	public WallFollower() {
		
	}

	/**
	 * WallFollower does not use distance, so this is not set
	 */
	@Override
	public void setDistance(Distance distance) {
		System.out.println("warning: the WallFollower class does not implement the setDistance method");
	}
	
	/**
	 * The only thing this does is to change the value of {@link #delayExpand}.
	 * Longer delay between operations when fewer sensors are enabled.
	 */
	@Override
	public void triggerUpdateSensorInformation(){
		int delayExpand=1;
		if(!robot.hasOperationalSensor(Direction.LEFT)) delayExpand++;
		if(!robot.hasOperationalSensor(Direction.FORWARD)) delayExpand++;
		
		if(delayExpand>0) {
			if(!robot.hasOperationalSensor(Direction.RIGHT)) delayExpand++;
			if(!robot.hasOperationalSensor(Direction.BACKWARD)) delayExpand++;
		}
		
		this.delayExpand=delayExpand;
		System.out.println("WallFollower: sensor udpate: delayExpand = "+delayExpand);
	}
	
	/**
	 * Decide on the next operation from the current position.
	 * If we can move leftwards, do so;
	 * otherwise if we can move forward, do so;
	 * if neither are possible, turn rightwards.
	 * 
	 */
	private void performNextOperation(){
		assert null!=robot;
		log("perform next operation");
		//System.out.printf("** next operation: currentDirection=%s, currentPosition=%s\n",currentDirection,Arrays.toString(getRobotPosition()));
		CardinalDirection cdForward = getCurrentDirection();
		CardinalDirection cdLeft = MazeMath.getFrom(cdForward,Turn.LEFT);
		log("   cdForward=%s, cdLeft=%s\n",cdForward,cdLeft);
		log("   Getting cdLeft="+cdLeft+ " "+Arrays.toString(cdLeft.getDirection()));
		Integer distanceLeft = getDistance(cdLeft);
		log("   Getting cdForward="+cdForward+ " "+Arrays.toString(cdForward.getDirection()));
		Integer distanceForward = getDistance(cdForward);
		log("   performNextOperation: %s (e=%.0f) dF=%s:%d dL=%s:%d\n",Arrays.toString(getRobotPosition()),robot.getBatteryLevel(),
				cdForward,distanceForward,cdLeft,distanceLeft);
		
		//if(!verbose) System.out.printf("performNextOperation: %s\n",Arrays.toString(getRobotPosition()));
		locations.add(getRobotPosition());
		
		
		// get distances to left and forward walls
		boolean leftWall = (0==distanceLeft);
		boolean forwardWall = (0==distanceForward);
		
		// used for testing
		int option=-1;
		
		// left wall present, forward wall present --> turn right
		if(leftWall && forwardWall) {
			currentDirection = MazeMath.getFrom(cdForward,Turn.RIGHT);
			option=1;
		}
		// left wall present, no forward wall --> move forward
		else if(leftWall && !forwardWall) {
			moveRobot(cdForward);
			option=2;
		}
		// no left wall, forward wall present --> move left
		else if(!leftWall && forwardWall) {
			moveRobot(cdLeft);
			currentDirection = cdLeft;
			option=3;
		}
		// no left wall, no forward wall --> move left
		else if(!leftWall && !forwardWall) {
			moveRobot(cdLeft);
			currentDirection=cdLeft;
			option=4;
		}
		
		//make sure robot is facing the new direction specified by the driver
		faceRobot(currentDirection);
		
		log("      option="+option);
		
		// bring positions into alignment
		currentPosition = getRobotPosition();
	}
	
	@Override
	public boolean drive2Exit() throws Exception {
		//used for testing
		locations=new LinkedList<int[]>();
		
		while(true) {
			// decide what the robot should do, and do it
			performNextOperation();
			
			// wait an interval before next operation
			try{
				Thread.sleep(walkDelay*delayExpand);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// now check for the various conditions
			// that should cause the method to return
			if(robot.hasStopped())
				throw new Exception(getRobotFailureMessage());
			if(robot.isAtExit()) break;
			if(interrupted) return false;
		}
		
		// we should not be outside the maze by now, but check
		if(null==getRobotPosition()) {
			return true;
		}
		
		// if not outside, get the direction of the exit
		CardinalDirection exitDirection = directionOfExit();
		faceRobot(exitDirection);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		// move through exit if sufficient energy
		robot.move(1,false);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		// exited successfully
		return true;
	}

}
