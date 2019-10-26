package gui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

public class WallFollower extends AbstractRobotDriver {
	
	public WallFollower() {
		
	}

	@Override
	public void setDistance(Distance distance) {
		System.out.println("warning: the WallFollower class does not implement the setDistance method");
	}
	
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
		
		
		boolean leftWall = (0==distanceLeft);
		boolean forwardWall = (0==distanceForward);
		
		int option=-1;
		
		if(leftWall && forwardWall) {//left,forward --> turn right
			currentDirection = MazeMath.getFrom(cdForward,Turn.RIGHT);
			option=1;
		}
		else if(leftWall && !forwardWall) {//left, no forward --> move forward
			moveRobot(cdForward);
			option=2;
		}
		else if(!leftWall && forwardWall) {//no left, forward --> move left
			moveRobot(cdLeft);
			currentDirection = cdLeft;
			option=3;
		}
		else if(!leftWall && !forwardWall) {//no left, no forward --> move left
			moveRobot(cdLeft);
			currentDirection=cdLeft;
			option=4;
		}
		
		faceRobot(currentDirection);
		//System.out.printf("   L=%s, F=%s\n",leftWall,forwardWall);
		
		log("      option="+option);
		
		currentPosition = getRobotPosition();
		/*System.out.printf("-->currentPosition: %s, currentDirection: %s (%s)\n",
				Arrays.toString(currentPosition),
				getCurrentDirection(),
				Arrays.toString(getCurrentDirection().getDirection())
				);*/
	}
	
	@Override
	public boolean drive2Exit() throws Exception {
		locations=new LinkedList<int[]>();
		
		while(true) {
			performNextOperation();
			try{
				Thread.sleep(walkDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(robot.hasStopped())
				throw new Exception(getRobotFailureMessage());
			if(robot.isAtExit()) break;
		}
		
		CardinalDirection exitDirection = directionOfExit();
		faceRobot(exitDirection);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		robot.move(1,false);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		return true;
	}
	
	/*
	public boolean drive2ExitFormer() throws Exception {
		CardinalDirection exitDirection = null;
		
		while (null==exitDirection){
			performNextOperation();
			if(robot.hasStopped())
				throw new Exception(getRobotFailureMessage());
			exitDirection=directionOfExit();
		}
		
		System.out.println("at exit: "+Arrays.toString(getRobotPosition()));
		
		//robot.rotate(MazeMath.toTurn(exitDirection));
		faceRobot(exitDirection);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		//System.out.printf("WallFollower drive2Exit: robot sees exit in direction %s (%s, %s)\n",exitDirection,
		//					MazeMath.convertDirs(exitDirection, robot.getCurrentDirection()),
		//					Arrays.toString(MazeMath.convertDirs(exitDirection, robot.getCurrentDirection()).getDirection()));
		
		while(!robot.isAtExit()){
			robot.move(1,false);
			if(robot.hasStopped())
				throw new Exception(getRobotFailureMessage());
		}
		robot.move(1,false);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		return true;
	}*/

}
