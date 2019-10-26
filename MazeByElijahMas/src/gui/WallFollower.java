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
	
	public boolean verbose=false;
	
	public List<int[]> locations;

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
			if(robot.hasStopped())
				throw new Exception(getRobotFailureMessage());
			if(robot.isAtExit()) break;
		}
		
		//System.out.println("at exit: "+Arrays.toString(getRobotPosition()));
		
		CardinalDirection exitDirection = directionOfExit();
		faceRobot(exitDirection);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		robot.move(1,false);
		if(robot.hasStopped())
			throw new Exception(getRobotFailureMessage());
		
		return true;
	}
	
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
	}
	
	protected Integer getDistance(CardinalDirection cd){
		while(true){
			Integer d = __getDistance__(cd);
			// if null, we either had a race condition,
			// or there were no functional sensors
			if(null!=d) return d;
		}
	}
	
	Integer tryDistance(Direction d) {
		try{
			return robot.distanceToObstacle(d);
		}
		catch (UnsupportedOperationException e){
			return null;
		}
	}
	
	private void log(String s) {
		if(verbose) System.out.println(s);
	}
	
	private void log(String s, Object... args) {
		if(verbose) System.out.printf(s,args);
	}
	
	/**
	 * To get the distance to a wall in a given direction,
	 * figure out which of the robot's operational sensors is closest
	 * to that direction, rotate the robot so that this sensor faces the
	 * requested direction, and then take the distance measurement.
	 * 
	 * There is a race condition: between the times when the
	 * operational sensor is identified and when the robot
	 * has rotated, that sensor may have failed
	 * @param requested_cd {@link CardinalDirection} where distance is sought
	 * @return distance in direction if it can be retrieved, null otherwise
	 */
	protected Integer __getDistance__(CardinalDirection requested_cd){
		// cd is the direction for which we seek the distance
		
		log("      __getDistance__: Seeking distance in cd "+requested_cd+"="+Arrays.toString(requested_cd.getDirection()));
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		
		log("      __getDistance__: robot is facing "+robot_cd+"="+Arrays.toString(robot_cd.getDirection())+" at position "+Arrays.toString(getRobotPosition()));
		
		// cardinal direction of closest functioning sensor
		CardinalDirection functional_cd = getClosestFunctionalSensor(requested_cd);
		log("      __getDistance__: closest functional sensor is at cd "+functional_cd);
		if(null==functional_cd) {
			log("      __getDistance__: no functional sensor");
			return null;
		}
		// the direction that the robot equates with 'functional_cd'
		Direction functional_dir = MazeMath.convertDirs(functional_cd, robot_cd);
		log("      __getDistance__: closest functional sensor is at d "+functional_dir);
		
		// the direction that the robot equates with the variable 'cd'
		Direction requested_dir = MazeMath.convertDirs(requested_cd, robot_cd);
		log("      __getDistance__: Seeking distance in dir "+requested_dir);
		
		// turn so that the sensor at functional_dir now points to requested_dir
		Turn turn = MazeMath.toTurn(functional_dir,requested_dir);
		robot.rotate(turn);
		log("      __getDistance__: the computed turn (%s-->%s) is %s\n",functional_cd,requested_cd,turn);
		
		robot_cd = robot.getCurrentDirection();
		
		log("      __getDistance__: functional sensor (%s) is facing cd %s=%s: wall distance=%s\n",
				functional_dir,MazeMath.convertDirs(functional_dir, robot_cd),Arrays.toString(MazeMath.convertDirs(functional_dir, robot_cd).getDirection()),
				tryDistance(functional_dir));
		
		if(!robot.hasStopped()) {
			assert requested_cd == MazeMath.convertDirs(functional_dir, robot_cd):
				"requested: "+requested_cd+", facing: "+MazeMath.convertDirs(functional_dir, robot_cd);
		}
		else log("__getDistance__ !   !   !   !   the robot has stopped");
		
		log("      __getDistance__: dists = %s\n",Arrays.toString(getDists()));
		
		try{
			return robot.distanceToObstacle(functional_dir);
		}
		catch (UnsupportedOperationException e){
			return null;
		}
	}
	
	private Integer[] getDists() {
		Integer[] dists = new Integer[4];
		for(int i=0; i<4; i++) {
			dists[i]=tryDistance(MazeMath.getFrom(Direction.FORWARD, i));
		}
		return dists;
	}
	
	/**
	 * Get the {@link CardinalDirection} of the closest
	 * functional sensor from an input {@link CardinalDirection}.
	 * 
	 * @param cd {@link CardinalDirection} value
	 * @return {@link CardinalDirection} of operational sensor closest to {@code cd}
	 */
	protected CardinalDirection getClosestFunctionalSensor(CardinalDirection cd){
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		
		for(int i: new int[]{0,1,-1,2}){
			CardinalDirection new_cd = MazeMath.getFrom(cd,i);
			Direction d = MazeMath.convertDirs(new_cd,robot_cd);
			if(robot.hasOperationalSensor(d)) return new_cd;
		}
		
		// no functional sensor
		return null;
	}
	
	/**
	 * Face the robot towards a given {@link CardinalDirection},
	 * then move it in that direction by a step.
	 * 
	 * @param cd {@link CardinalDirection} value
	 */
	private void moveRobot(CardinalDirection cd){
		faceRobot(cd);
		log("   moveRobot: the robot is now facing %s=%s at position %s\n, dists: %s",
				robot.getCurrentDirection(),Arrays.toString(robot.getCurrentDirection().getDirection()),
				Arrays.toString(getRobotPosition()),Arrays.toString(getDists()));
		robot.move(1,false);
		assert robot.getCurrentDirection() == cd: "robot should be facing "+cd+", instead facing "+robot.getCurrentDirection();
	}
	
	/**
	 * Face the robot towards a given {@link CardinalDirection}
	 * 
	 * @param cd {@link CardinalDirection} value
	 */
	void faceRobot(CardinalDirection cd){
		CardinalDirection robot_cd = robot.getCurrentDirection();
		Turn t = MazeMath.toTurn(cd,robot_cd);
		robot.rotate(t);
		assert robot.getCurrentDirection() == cd: "robot should be facing "+cd+", instead facing "+robot.getCurrentDirection();
		log("   faceRobot: the robot is now facing "+robot.getCurrentDirection());
	}
	
	/**
	 * Get the {@link Direction} in which the robot can see the exit, if any;
	 * if the robot cannot see the exit, return {@code null}.
	 */
	private CardinalDirection directionOfExit(){
		CardinalDirection exit = null;
		
		int count=0;
		
		for(CardinalDirection cd: CardinalDirection.values()){
			if(__getDistance__(cd)==Integer.MAX_VALUE) {
				//System.out.printf("robot at %s sees exit at %s (%s)\n",Arrays.toString(getRobotPosition()),cd,Arrays.toString(cd.getDirection()));
				exit=cd;//MazeMath.convertDirs(cd, robot.getCurrentDirection());
				count++;
			}
		}
		
		assert (0==count || 1==count);
		
		return exit;
	}

}
