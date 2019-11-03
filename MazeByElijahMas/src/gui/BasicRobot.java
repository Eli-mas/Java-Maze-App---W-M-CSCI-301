package gui;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Floorplan;
import generation.Distance;
import generation.Maze;
import gui.Constants.UserInput;

/**
 * 
 * <p>Basic implementation of the {@link Robot} interface.
 * Provides functionality for an entity to navigate
 * a maze while being aware of its orientation within the maze.
 * In order for a robot to be present while playing a game,
 * Controller must receive a boolean value true in its constructor
 * to instantiate a robot.</p>
 * 
 * <p>BasicRobot tells the GUI (Controller) to change
 * when an operation (move, rotate, jump) is successfully performed
 * by way of its {@link Controller#keyDown keyDown} method.
 * It receives key commands redirected by the Controller to the robot
 * from {@link gui.SimpleKeyListener}.</p>
 * 
 * <p>The robot may be driven by a {@link RobotDriver}
 * to explore a maze programmatically:
 * {@link WallFollower} &#38; {@link Wizard}.
 * </p>
 * 
 * @author Elijah Mas
 *
 */
public class BasicRobot implements Robot {

	/**
	 * the {@link Controller} running in tandem with the robot
	 */
	private Controller control;
	
	/**
	 * remaining energy level of robot
	 */
	private float batteryLevel;
	
	/**
	 * current distance traveled
	 */
	private int odometerReading;
	
	/**
	 * map to whether sensor in each direction is functional
	 */
	private HashMap<Direction,Boolean> sensorFunctionalFlags;
	
	/**
	 * stores distance to walls in the four relative directions in the order
	 * {@ code FORWARD}, {@code RIGHT}, {@code BACKWARD}, {@code LEFT}
	 */
	private ArrayList<Integer> obstacleDistancesForwardRightBackwardLeft=new ArrayList<Integer>(4);
	
	/**
	 * maze in which the robot is operating
	 */
	private Maze maze;
	
	/**
	 * {@link Floorplan floorplan} of {@link #maze}
	 */
	private Floorplan floorplan;
	
	/**
	 * {@link Distance Distance} object of {@link #maze}
	 */
	private Distance distance;
	
	/**
	 * tells whether the robot has a room sensor installed
	 */
	private boolean roomSensorIsPresent;
	
	/**
	 * tells whether the robot is stopped
	 */
	private boolean stopped;
	
	/**
	 * the current position of the robot; maintained internally
	 */
	private int[] currentPosition;
	
	/**
	 * the current absolute direction of the robot; maintained internally
	 */
	private CardinalDirection currentDirection;
	
	/**
	 * initial battery level before robot begins activity;
	 * public so that it can be modified during testing.
	 */
	private float initialEnergyLevel=3000f;

	/**
	 * If the robot fails, this provides an explanatory message.
	 */
	private String failureMessage;
	
	public float getInitialEnergyLevel() {
		return initialEnergyLevel;
	}
	
	public void setInitialEnergyLevel(float e) {
		initialEnergyLevel = e;
	}
	
	
	/**
	 * a jump consumes 50 battery units.
	 */
	final static float energyUsedForJump=50;
	
	/**
	 * a move consumes 5 battery units.
	 */
	final static float energyUsedForMove=5;
	
	/**
	 * a rotation consumes 3 battery units.
	 */
	final static float energyUsedForRotation=3;
	
	/**
	 * a 360-degree rotation consumes 12 battery units.
	 */
	final static float energyUsedForFullRotation=4*energyUsedForRotation;
	
	/**
	 * sensing direction for a single sensor consumes 1 battery unit.
	 */
	final static float energyUsedForDistanceSensing=1;
	
	@Override
	public CardinalDirection getCurrentDirection() {
		return currentDirection;
	}

	@Override
	public float getBatteryLevel() {
		return batteryLevel;
	}

	@Override
	public void setBatteryLevel(float level) {
		batteryLevel=level;
	}

	@Override
	public boolean hasStopped() {
		return stopped;
	}
	
	private void setStopped() {
		stopped=true;
	}
	
	void clearStopped() {
		stopped=false;
	}

	@Override
	public void resetOdometer() {
		odometerReading=0;
	}

	@Override
	public float getEnergyForFullRotation() {
		return energyUsedForFullRotation;
	}

	@Override
	public float getEnergyForStepForward() {
		return energyUsedForMove;
	}

	@Override
	public int getOdometerReading() {
		return odometerReading;
	}
	
	/**
	 * Constructor does nothing. Instantiation is signaled
	 * at appropriate time from within {@link Controller}.
	 */
	public BasicRobot() {}
	
	/**
	 * Instantiate all the fields of the robot that depend on the maze.
	 * The maze must be fully initialized before this is called. 
	 */
	protected void instantiateFields() {
		assert control.currentState instanceof StatePlaying :
			"robot instantation prerequires that the Controller be in a playing state";
		
		batteryLevel=initialEnergyLevel;
		odometerReading=0;
		sensorFunctionalFlags = new HashMap<Direction,Boolean>();
		
		for(Direction d: Direction.values()) sensorFunctionalFlags.put(d,true);
		
		
		roomSensorIsPresent=true;
		maze=control.getMazeConfiguration();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		stopped=false;
		
		currentPosition = control.getCurrentPosition();
		currentDirection = control.getCurrentDirection();
		
		//nonce value added, it is changed by calculateObstacleDistance
		for(int directionCounter=0; directionCounter<4; directionCounter++)
			obstacleDistancesForwardRightBackwardLeft.add(-1);
		
		calculateDistances();
	}

	/**
	 * Set the controller and instantiate the robot's other fields.
	 */
	@Override
	public void setMaze(Controller controller) {
		control=controller;
		instantiateFields();
	}
	
	
	/**
	 * Get the current position of the robot.
	 * Useful for testing when the robot has exited the maze.
	 */
	@Override
	public int[] getCurrentPosition() throws Exception {
		if(!maze.isValidPosition(currentPosition[0], currentPosition[1]))
			throw new Exception(String.format(
					"getCurrentPosition: %s is an invalid position for a maze of dimensions %d x %d",
					Arrays.toString(currentPosition), maze.getWidth() ,maze.getHeight() ));
		return currentPosition;
	}

	@Override
	public boolean isAtExit() {
		try {
			// distance maintains the maze's exit position
			return Arrays.equals(getCurrentPosition(), distance.getExitPosition());
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean canSeeThroughTheExitIntoEternity(Direction direction) throws UnsupportedOperationException {
		// must have operational sensor
		if (!hasOperationalSensor(direction))
			throw new UnsupportedOperationException(
					"cannot sense whether looking out of maze:"+direction+" sensor not present");
		try {
			// this value indicates that the maze is open in the given direction
			return (Integer.MAX_VALUE==distanceToObstacle(direction));
		} catch (Exception e) {
			//e.printStackTrace();
			throw new UnsupportedOperationException(
					"operation 'canSeeThroughTheExitIntoEternity' failed", e);
		}
	}
	
	@Override
	public boolean isInsideRoom() throws UnsupportedOperationException {
		// must have operational sensor
		if (!hasRoomSensor())
			throw new UnsupportedOperationException("cannot sense presence in room: room sensor not present");
		
		
		int[] pos = tryGetCurrentPosition();
		return (null==pos) ? false : floorplan.isInRoom(pos);
	}

	@Override
	public boolean hasRoomSensor() {
		return roomSensorIsPresent;
	}
	
	public void nullifyRoomSensor() {
		roomSensorIsPresent=false;
	}
	
	public void enableRoomSensor() {
		roomSensorIsPresent=true;
	}
	
	public void printDists() {
		System.out.println("FRBL: "+obstacleDistancesForwardRightBackwardLeft);
	}

	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		// requires operational sensor
		if(!hasOperationalSensor(direction))
			throw new UnsupportedOperationException("sensor in direction "+direction+" is not operational");
		
		// obstacleDistancesForwardRightBackwardLeft is updated when operations are performed
		int distance=obstacleDistancesForwardRightBackwardLeft.get(MazeMath.getDirectionIndex(direction));
		
		// if distance is missing, fill it
		if(-1==distance || hasMissingSensors()) 
			try {// this should not throw an exception, but to satisfy compiler
				distance=calculateObstacleDistance(direction);
			} catch (Exception e) {
				System.out.println("second exception in distanceToObstacle: "+e.getMessage());
				//e.printStackTrace();
			}
		
		return distance;
		
	}
	
	/**
	 * Compute and store the distance to a wall in a given direction.
	 * Returns true if the distance could be calculated successfully.
	 * 
	 * @param direction a value of {@link Direction}
	 * @return true if the distance was calculated successfully
	 * @throws Exception originates from {@link #getCurrentPosition() getCurrentPosition}
	 * called in {@link #getObstacleDistance(gui.Robot.Direction) getObstacleDistance}
	 */
	private int calculateObstacleDistance(Direction direction) throws Exception {
		if(!hasOperationalSensor(direction)) {
			// no sensor --> set to nonce value -1
			obstacleDistancesForwardRightBackwardLeft.set(MazeMath.getDirectionIndex(direction), -1);
			throw new UnsupportedOperationException(
					"cannot calculate distance: "+direction+" sensor not operational");
		}
		try {
			// fail if no energy
			if(!attemptEnergyDepletion(energyUsedForDistanceSensing)) return -1;
			
			//get obstacle distance and assign to appropriate location
			int d = getObstacleDistance(direction);
			obstacleDistancesForwardRightBackwardLeft.set(MazeMath.getDirectionIndex(direction), d);
			
			return d;
			
		} catch (Exception e){
			throw new UnsupportedOperationException(
					"distanceToObstacle at direction "+direction+" failed:",e);
		}
		
	}

	@Override
	public boolean hasOperationalSensor(Direction direction) {
		// if the first term is not true, this short-circuits
		// so the second term cannot inject null into the boolean comparison
		return (hasDirectionalSensor(direction) && sensorFunctionalFlags.get(direction));
	}

	@Override
	public void triggerSensorFailure(Direction direction) {
		// has to have a sensor
		if(hasDirectionalSensor(direction))
			sensorFunctionalFlags.put(direction,false);
	}

	@Override
	public boolean repairFailedSensor(Direction direction) {
		// has to have a sensor
		if(!hasDirectionalSensor(direction)) return false;
		sensorFunctionalFlags.put(direction,true);
		
		// now get the distance that was missing
		try {
			calculateObstacleDistance(direction);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Tells whether the robot has any inoperable sensors.
	 * @return true if any sensor is inoperable, false otherwise
	 */
	private boolean hasMissingSensors() {
		for(Direction d: Direction.values()) {
			if(!hasOperationalSensor(d)) return true;
		}
		return false;
	}
	
	/**
	 * Rotate the robot rightwards, adjusting required fields.
	 * Cause a failure if energy is depleted.
	 */
	private void rotateRight() {
		// check that we don't run out of energy before attempting rotation
		if(!attemptEnergyDepletion(energyUsedForRotation)) return;
		
		/*
		// moving right is aligned with moving forward in array
		// which means: for what was right to become the new forward,
		// we have to rotate the array backwards
		if(hasMissingSensors()) Collections.rotate(obstacleDistancesForwardRightBackwardLeft, -1);
		else {
			if(hasStopped()) return;
		}
		*/
		
		// convert a turn to a cardinal direction and set current direction for robot
		currentDirection=MazeMath.getFrom(getCurrentDirection(), Turn.RIGHT);
		calculateDistances();
		if(hasStopped()) return;
		// sets current direction for controller
		control.keyDown(UserInput.Right, 0);
		
		// check for agreement between robot & controller
		checkOrientation();
		
	}
	
	/**
	 * Rotate the robot leftwards, adjusting required fields.
	 * Cause a failure if energy is depleted.
	 */
	private void rotateLeft() {
		// check that we don't run out of energy before attempting rotation
		if(!attemptEnergyDepletion(energyUsedForRotation)) return;
		
		/*
		// opposite of right -- see `rotateRight` above for explanation
		if(hasMissingSensors()) Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
		else {
			if(hasStopped()) return;
		}*/
		
		// convert a turn to a cardinal direction and set current direction for robot
		currentDirection=MazeMath.getFrom(getCurrentDirection(), Turn.LEFT);
		calculateDistances();
		if(hasStopped()) return;
		// sets current direction for controller
		control.keyDown(UserInput.Left, 0);
		
		// check for agreement between robot & controller
		checkOrientation();
	}
	
	@Override
	public void rotate(Turn turn) {
		if(null==turn) return;
		
		if(hasStopped()) {
			return;
		}
		switch(turn) {
			case RIGHT:
				rotateRight();
				break;
			case LEFT:
				rotateLeft();
				break;
			case AROUND:
				// two rotations in same direction, which direction does not matter
				rotateLeft();
				rotateLeft();
				break;
			default:
				System.out.printf("!   !   !   !   BasicRobot: invalid turn value specified: %s   !   !   !   !", turn);
		}
	}
	
	@Override
	public void move(int distance, boolean manual){
		if(distance<0) {
			return;
		}
		
		for(int moveCount=0; moveCount<distance; moveCount++) {
			if(hasStopped()) {
				return;
			}
			
			int[] position=tryGetCurrentPosition();
			
			moveSingleStep(manual);
			
			// if robot has stopped, no reason to check energies below, as calculations stop
			if(hasStopped()) return;
			
			// branch on whether we have left the maze
			try {
				int[] newPosition = getCurrentPosition(); //throws exception if out of maze
				
				int delta=MazeMath.manhattanDistance(position, newPosition);
				assert (1==delta || 0==delta);
			}
			catch (Exception e) {
				// we get here if the robot moves out of the maze
				return;
			}
		}
	}
	
	/**
	 * Increment the robot's position according to the current forward direction.
	 */
	private void incrementCurrentPosition() {
		// currentDirection is a CardinalDirection
		// so currentDirection.getDirection is the int[] array that
		// corresponds to this direction
		currentPosition=MazeMath.addArrays(currentPosition,currentDirection.getDirection());
	}
	
	/**
	 * Cause the game to end because of a robot failure.
	 * The robot informs the controller about the cause of failure.
	 * @param failureMessage a description of why the robot failed.
	 */
	private void endGame(String failureMessage) {
		setStopped();
		setFailureMessage(failureMessage);
		control.switchFromPlayingToWinning(odometerReading);
	}
	
	private void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}
	
	public String getFailureMessage() {
		return failureMessage;
	}
	
	/**
	 * Move the robot one position forward, adjusting required fields.
	 * Cause a failure if energy is depleted or the robot crashes.
	 */
	private void moveSingleStep(boolean manual){
		// check energy before checking for crash, because the robot cannot crash
		// if it does not have energy to move in the first place
		if(!attemptEnergyDepletion(energyUsedForMove)) return;
		
		if(hasImmediateWallInForwardDirection()) {
			
			if(!manual) {
				//System.out.println("ending game");
				endGame(Constants.robotFailureMessage__BadMove);
			}
		}
		else {
			//no crash here
			incrementCurrentPosition();
			odometerReading++;
			//System.out.printf("odometer, energy, used: %d, %.1f, %.1f\n",getOdometerReading(),getBatteryLevel(),control.getEnergyConsumedByRobotAtPresent());
			
			changeDistancesInMoveForward();
			
			if(hasStopped()) return;
			
			control.keyDown(UserInput.Up, 0);
			
			checkOrientation();
		}
	}
	
	/**
	 * Check that the robot agrees with the controller on position and orientation.
	 * This should always be true; the exception is when a driver is enabled
	 * and the game returns to title screen before the winning screen is reached.
	 * This arises from threading issues.
	 */
	private void checkOrientation() {
		try{
			assert Arrays.equals(control.getCurrentPosition(), currentPosition);
			assert control.getCurrentDirection()==currentDirection;
		}
		catch (AssertionError e){
			System.out.println("controller and robot out of sync");
			//control.switchToTitle();
		}
	}
	
	@Override
	public void jump() throws Exception {
		if(hasStopped()) {
			return;
		}
		
		if(!hasImmediateWallInForwardDirection()) {
			move(1,control.manualRobotOperation);
		}
		
		else { // jump required
			
			float energyBeforeJump = getBatteryLevel();
			
			// check if sufficient energy
			if(!attemptEnergyDepletion(energyUsedForJump)) return;
			
			// change position before calculating distances
			incrementCurrentPosition();
			try {
				// if we tried a bad jump, currentPosition is outside maze
				// and this call throws an error
				getCurrentPosition();
				
				// increment odometer reading
				odometerReading++;
				
				// we know that the robot must be in front of a wall
				// i.e. distance to wall backwards is 0
				obstacleDistancesForwardRightBackwardLeft.set(2, 0);
				
				calculateDistances(Direction.BACKWARD);
				// could run out of energy here
				if(hasStopped()) return;
				
				// robot is finished, push changes to Controller
				control.keyDown(UserInput.Jump, 0);
				
				checkOrientation();
				
				assert getBatteryLevel()==energyBeforeJump-(energyUsedForJump+3*energyUsedForDistanceSensing):
					"energy difference before/after rotation: "+
					(energyBeforeJump-getBatteryLevel())+
					", exepcted: "+(energyUsedForJump+3*energyUsedForDistanceSensing);
			}
			catch (Exception e) {
				endGame(Constants.robotFailureMessage__BadJump);
				throw new Exception(Constants.robotFailureMessage__BadJump);
			}
		}
	}
	
	
	
	/**
	 * Check if there is a wall immediately in front of the robot.
	 * @return true if distance to forward wall is 0
	 */
	private boolean hasImmediateWallInForwardDirection() {
		return 0==obstacleDistancesForwardRightBackwardLeft.get(
			MazeMath.getDirectionIndex(Direction.FORWARD)
		);
	}
	
	/**
	 * Calculate the distances across directions.
	 * @param exclusions
	 */
	private void calculateDistances(Direction... exclusions) {
		List<Direction> exclude = Arrays.asList(exclusions);
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			if(exclude.contains(d)) {
				continue;
			}
			
			// if distance calculation fails, -1 is the default nonce value
			try {
				calculateObstacleDistance(d);
			} catch (Exception e) {
				setDistanceInDirection(d,-1);
			}
		}
	}
	
	/**
	 * Set the distance in a given direction from parameter input (without recomputing it).
	 * @param d the {@link Direction}
	 * @param distanceValue the distance value to be set
	 */
	private void setDistanceInDirection(Direction d, int distanceValue) {
		obstacleDistancesForwardRightBackwardLeft.set(MazeMath.getDirectionIndex(d),distanceValue);
	}
	
	/**
	 * Increment the robot's backward distance and decrement the forward distance,
	 * as happens when the robot moves forward. Get the left/right distances
	 * by using sensors. Designed to optimize energy usage.
	 */
	private void changeDistancesInMoveForward() {
		int	dForward=obstacleDistancesForwardRightBackwardLeft.get(0),
			dBackward=obstacleDistancesForwardRightBackwardLeft.get(2);
		
		/*
		 * if not looking out through exit, a move forward
		 * decrements the forward obstacle distance and
		 * increments the backward obstacle distance
		 * 
		 * if looking through the exit,
		 * we can't shift the infinity value
		*/
		if(Integer.MAX_VALUE!=dForward) setDistanceInDirection(Direction.FORWARD, dForward-1);
		if(Integer.MAX_VALUE!=dBackward) setDistanceInDirection(Direction.BACKWARD, dBackward+1);obstacleDistancesForwardRightBackwardLeft.set(2,dBackward+1);
		
		/*
		 * possible optimization:
		 * if robot is in line of exit forwards,
		 * stop calculating distances on the sides
		 * this can cause trouble, so I might skip it
		
		//if(canSeeThroughTheExitIntoEternity(Direction.FORWARD)) return;
		*/
		try{
			getCurrentPosition();
			calculateDistances(Direction.FORWARD,Direction.BACKWARD);
		} catch(Exception e) {
			return;
		}
	}
	
	/**
	 * Change the energy level of the robot by a given amount.
	 * Cause the robot to stop if there is insufficient battery.
	 * @param amount energy depleted by an operation
	 */
	private void changeEnergyLevel(float amount) {
		float newEnergy=getBatteryLevel()-amount;
		if(newEnergy<0) {
			setStopped();
			setBatteryLevel(0);
			endGame(Constants.robotFailureMessage__NoEnergy);
			return;
		}
		setBatteryLevel(newEnergy);
	}
	
	/**
	 * Wrapper around {@link #changeEnergyLevel(float)}
	 * to test if robot has stopped in attempting an operation.
	 * @param amount energy depleted by an operation
	 * @return whether the robot had enough battery to perform the operation
	 */
	private boolean attemptEnergyDepletion(float amount) {
		changeEnergyLevel(amount);
		return !hasStopped();
	}
	
	/**
	 * Tell whether the robot has a sensor in the given direction.
	 * 
	 * @param direction a value of {@link Direction}
	 * @return whether or not sensor for this direction is installed
	 */
	private boolean hasDirectionalSensor(Direction direction) {
		return sensorFunctionalFlags.containsKey(direction);
	}
	
	/**
	 * Get the distance to a wall in a particular direction.
	 * Makes use of {@link #floorplan}. 
	 * 
	 * @param d a member of {@link Direction}
	 * @return wall distance in specified direction
	 * @throws Exception could originate from {@link #getCurrentPosition()},
	 * or if no sensor is present
	 */
	private int getObstacleDistance(Direction d) throws Exception {
		
		
		CardinalDirection cd = MazeMath.convertDirs(d, currentDirection);//translateCurrentDirectionToCardinalDirection(d);
		
		int[] dir = cd.getDirection();
		int dx = dir[0], dy = dir[1]; // direction of movement
		int[] pos = getCurrentPosition();
		int x = pos[0], y = pos[1]; // current position
		
		// the number of iterations of this loop is equal to the number of
		// moves the robot must make before standing next to a wall
		while(floorplan.hasNoWall(x, y, cd)) {
			x+=dx; // new x position after move
			y+=dy; // new y position after move
			
			// if true, we've reached beyond the maze--we can see through the exit
			if(!maze.isValidPosition(x, y)) return Integer.MAX_VALUE;
		}
		
		// pos is unchanged, so computing Manhattan distance
		// between (x,y) and pos gives total distance covered
		return MazeMath.manhattanDistance(x, y, pos);
	}
	
	
	

}

