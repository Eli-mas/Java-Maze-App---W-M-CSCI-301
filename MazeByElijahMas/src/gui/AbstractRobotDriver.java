package gui;

import java.util.Arrays;
import java.util.List;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * 
 * @author Elijah Mas
 *
 */
public abstract class AbstractRobotDriver implements RobotDriver {
	
	/**
	 * robot to be driven
	 */
	Robot robot;
	
	/**
	 * maze dimensions
	 */
	int width, height;
	
	/**
	 * {@link Distance} reference for drivers that use it
	 */
	Distance distance;
	
	/**
	 * starting energy of the robot
	 */
	float robotStartEnergy;
	
	/**
	 * {@link Controller} for the driver to interact with
	 */
	protected Controller controller;
	
	/**
	 * current direction of reference of the driver;
	 * can different from robot's sense of direction
	 */
	CardinalDirection currentDirection;
	
	/**
	 * current position of the driver;
	 * must agree with robot's position
	 */
	int[] currentPosition;
	
	/**
	 * whether the driver has been interrupted by a key command;
	 * this is used to stop the {@link #drive2Exit()} method prematurely
	 */
	boolean interrupted=false;
	
	/**
	 * delay between operations performed on the robot in milliseconds
	 */
	public static int walkDelay = 40;
	
	/**
	 * <i>used in testing</i>: may track positions visited by the robot 
	 */
	public List<int[]> locations;
	
	/**
	 * controls printing options
	 */
	public boolean verbose=false;
	
	
	
	
	@Override
	public void setRobot(Robot r) {
		robot=r;
		currentDirection = robot.getCurrentDirection();
		currentPosition = getRobotPosition();
		robotStartEnergy = robot.getBatteryLevel();
		
		//otherInitialization();
		
		log("setting robot: direction=%s, position=%s\n",robot.getCurrentDirection(),Arrays.toString(currentPosition));
	}
	
	/*
	 * Not used
	void otherInitialization() {
		
	}
	 **/

	@Override
	public void setDimensions(int width, int height) {
		this.width=width;
		this.height=height;
	}

	@Override
	public void setDistance(Distance distance) {
		this.distance=distance;
	}

	@Override
	public void triggerUpdateSensorInformation() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean drive2Exit() throws Exception {
		throw new RuntimeException("AbstractRobotDriver does not implement the method 'drive2Exit'");
	}

	@Override
	public float getEnergyConsumption() {
		return robotStartEnergy-robot.getBatteryLevel();
	}

	@Override
	public int getPathLength() {
		return robot.getOdometerReading();
	}
	
	@Override
	public void interrupt() {
		interrupted = true;
		System.out.println("\nAn invalid key (unrelated to toggling or altering map view)"
				+ " has been pressed while the robot driver was operating; "
				+ "this has caused the driver to interrupt and the current session"
				+ " to terminate."
		);
	}
	
	
	/**
	 * print a string by println if {@link #verbose} is true
	 * @param s a string
	 */
	protected void log(String s) {
		if(verbose) System.out.println(s);
	}
	
	
	/**
	 * print a string by printf if {@link #verbose} is true
	 * @param s a string
	 */
	protected void log(String s, Object... args) {
		if(verbose) System.out.printf(s,args);
	}
	
	public void setController(Controller c) {
		controller=c;
	}
	
	public String getRobotFailureMessage() {
		return controller.getRobotFailureMessage();
	}
	
	public Robot getRobot() {
		return robot;
	}
	
	/**
	 * Tell if the robot has left the maze.
	 * @return true if robot outside maze, false otherwise
	 */
	boolean tellIfOutsideMaze() {
		return null==getRobotPosition();
	}
	
	/**
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	 */
	int[] getRobotPosition() {
		return MazeMath.getRobotPosition(robot);
	}
	
	public CardinalDirection getCurrentDirection() {
		return currentDirection;
	}
	
	
	
	
	/**
	 * Get the robot's distances in all four relative directions,
	 * moving rightwards starting from forwards.
	 * @return distances as an array
	 */
	Integer[] getDists() {
		Integer[] dists = new Integer[4];
		for(int i=0; i<4; i++) {
			// getFrom issues a new direction for every increment of i
			dists[i]=tryDistance(MazeMath.getFrom(Direction.FORWARD, i));
		}
		return dists;
	}
	
	/**
	 * Get the distance in an absolute direction {@code cd}.
	 * Calls {@link #__getDistance__(CardinalDirection) __getDistance__(cd)}
	 * until a non-null result is returned.
	 * @param cd a {@link CardinalDirection} value
	 * @return distance in input direction
	 */
	protected Integer getDistance(CardinalDirection cd){
		// while-loop to force a valid result to return
		while(true){
			Integer d = __getDistance__(cd);
			// if null, we either had a race condition,
			// or there were no functional sensors
			if(null!=d) return d;
		}
	}
	
	/**
	 * To get the distance to a wall in a given direction,
	 * figure out which of the robot's operational sensors is closest
	 * to that direction, rotate the robot so that this sensor faces the
	 * requested direction, and then take the distance measurement.
	 * 
	 * There is a race condition: between the times when the
	 * operational sensor is identified and when the robot
	 * has rotated, that sensor may have failed. This possibility
	 * is handled by the while loop in {@link #getDistance(CardinalDirection) getDistance}.
	 * @param requested_cd {@link CardinalDirection} where distance is sought
	 * @return distance in direction if it can be retrieved, null otherwise
	 */
	protected Integer __getDistance__(CardinalDirection requested_cd){
		// cd is the direction for which we seek the distance
		
		log("      __getDistance__: Seeking distance in cd "+requested_cd+"="+Arrays.toString(requested_cd.getDirection()));
		
		CardinalDirection robot_cd = robot.getCurrentDirection();
		
		log("      __getDistance__: robot is facing "+robot_cd+"="+Arrays.toString(robot_cd.getDirection())+" at position "+Arrays.toString(getRobotPosition()));
		
		// functional_cd = cardinal direction of closest functioning sensor
		CardinalDirection functional_cd = getClosestFunctionalSensor(requested_cd);
		log("      __getDistance__: closest functional sensor is at cd "+functional_cd);
		if(null==functional_cd) {
			log("      __getDistance__: no functional sensor");
			return null;
		}
		// functional_dir = the direction that the robot equates with 'functional_cd'
		Direction functional_dir = MazeMath.convertDirs(functional_cd, robot_cd);
		log("      __getDistance__: closest functional sensor is at d "+functional_dir);
		
		// requested_dir = the direction that the robot equates with the variable 'cd'
		Direction requested_dir = MazeMath.convertDirs(requested_cd, robot_cd);
		log("      __getDistance__: Seeking distance in dir "+requested_dir);
		
		// turn robot so that the sensor at functional_dir now points to requested_dir
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
		
		return tryDistance(functional_dir);
	}
	
	/**
	 * Try returning the distance to a wall in a given direction;
	 * if not possible, return null. Convenience method to wrap
	 * around {@link Robot#distanceToObstacle(Direction)}.
	 * @param d a {@link Direction} value
	 * @return distance in {@code d} if obtainable, else {@code null}.
	 */
	Integer tryDistance(Direction d) {
		try{
			return robot.distanceToObstacle(d);
		}
		catch (UnsupportedOperationException e){
			return null;
		}
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
		
		// try the cardinal directions in the order closest to current forward direction:
		// forward, right, left, backwards
		// (right and left are equivalent in distance from forward)
		for(int i: new int[]{0,1,-1,2}){
			CardinalDirection new_cd = MazeMath.getFrom(cd,i);
			Direction d = MazeMath.convertDirs(new_cd,robot_cd);
			if(robot.hasOperationalSensor(d)) return new_cd;
		}
		
		// if we get here, there is no functional sensor
		return null;
	}
	
	/**
	 * Face the robot towards a given {@link CardinalDirection},
	 * then move it in that direction by a single step.
	 * 
	 * @param cd {@link CardinalDirection} value
	 */
	void moveRobot(CardinalDirection cd){
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
	CardinalDirection directionOfExit(){
		if(!robot.isAtExit()) return null;
		
		CardinalDirection exit = null;
		
		int count=0;
		
		for(CardinalDirection cd: CardinalDirection.values()){
			// signature of seeing the exit: distance = max possible integer value
			if(getDistance(cd)==Integer.MAX_VALUE) {
				exit=cd;
				count++;
			}
		}
		
		//there should be only one exit
		assert (0==count || 1==count);
		
		return exit;
	}

}
