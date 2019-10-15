package gui;

import static org.junit.jupiter.api.DynamicTest.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * 
 * 
 * ...
 * 
 * Note is is the responsibility of the Robot to tell the GUI to change
 * when an operation (move, rotate, jump) is successfully performed.
 * This includes changing the state of StatePlaying and 
 * 
 * @author HomeFolder
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
	 * width & height of the maze
	 */
	private int width, height;
	
	/**
	 * {@link Floorplan floorplan} of {@link #maze}
	 */
	private Floorplan floorplan;
	
	/**
	 * {@link Distance Distance} object of {@link #maze}
	 */
	private Distance distance;
	
	
	//private int[][] dists;
	
	/**
	 * tells whether the robot has a room sensor installed
	 */
	private boolean roomSensorIsPresent;
	
	/**
	 * tells whether the robot is fully initialized
	 */
	private boolean initialized;
	
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
	
	//private static boolean crashEnabled=true;
	
	/**
	 * failure message when the robot tries to jump out of the maze
	 */
	public static final String badJumpMessage="robot tried bad jump";
	
	/**
	 * failure message when the robot crashes into a wall
	 */
	public static final String badMoveMessage="robot crashed";
	
	/**
	 * failure message when the robot runs out of battery
	 */
	public static final String noEnergyMessage="robot out of energy";
	
	/**
	 * initial battery level before robot begins activity;
	 * public so that it can be modified during testing.
	 */
	public static float initialEnergyLevel=3000f;
	
	//private int[][] visitCounts; //prospective for Project 4
	
	/**
	 * allows for printing information about robot while operating;
	 * disabled during testing.
	 */
	public static boolean VERBOSE=true;
	
	
	//private static HashMap<Integer,Direction> create_obstacleDistancesIndices(){
	//	HashMap<Integer,Direction> map = new HashMap<Integer,Direction>(4);
	//	
	//}
	
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
	
	public Controller getController() {
		return control;
	}
	
	public String getDistanceString() {
		return obstacleDistancesForwardRightBackwardLeft.toString();
	}

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
	
	private boolean hasBattery() {
		return getBatteryLevel()>0;
	}

	@Override
	public boolean hasStopped() {
		return stopped;
	}
	
	private void setStopped() {
		stopped=true;
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
		width=maze.getWidth();
		height=maze.getHeight();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		//dists=distance.getAllDistanceValues();
		stopped=false;
		//VERBOSE=true;
		
		currentPosition = control.getCurrentPosition();
		currentDirection = control.getCurrentDirection();
		
		//
		//nonce value added, it is changed by calculateObstacleDistance
		for(int directionCounter=0; directionCounter<4; directionCounter++)
			obstacleDistancesForwardRightBackwardLeft.add(-1);
		
		calculateDistances();
		
		initialized=true;
		//System.out.println("BasicRobot: instantiateFields completed");
		//System.out.printf("BasicRobot: maze dimensions: %d,%d\n",width,height);
		//System.out.println("BasicRobot: maze exit: "+Arrays.toString(distance.getExitPosition()));
		
		/*
		//prospective for Project 4
		visitCounts=new int[width][height];
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				visitCounts[x][y]=0;
			}
		}
		
		incrementVisitCount(currentPosition);
		*/
	}

	/**
	 * Set the controller and instantiate the robot's other fields.
	 */
	@Override
	public void setMaze(Controller controller) {
		control=controller;
		instantiateFields();
		//System.out.println("robot has set controller");
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
	
	/**
	 * Stop the robot if the battery has run out.
	 */
	private void stopIfNoBattery() {
		if(!hasBattery()) setStopped();
	}

	@Override
	public boolean isAtExit() {
		try {
			// distance maintains the maze's exit position
			return Arrays.equals(getCurrentPosition(), distance.getExitPosition());
		} catch (Exception e) {
			System.out.println("BasicRobot: failed to evaluate 'isAtExit':");
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
			//return false;
			System.out.println("canSeeThroughTheExitIntoEternity failed");
			throw new UnsupportedOperationException(
					"operation 'canSeeThroughTheExitIntoEternity' failed, cannot evaluate presence in room", e);
		}
	}

	@Override
	public boolean isInsideRoom() throws UnsupportedOperationException {
		// must have operational sensor
		if (!hasRoomSensor())
			throw new UnsupportedOperationException("cannot sense presence in room: room sensor not present");
		try {
			int[] pos;
			pos = getCurrentPosition(); // possible Exception
			return floorplan.isInRoom(pos[0], pos[1]);
		} catch (Exception e) {
			//e.printStackTrace();
			//return false;
			throw new UnsupportedOperationException(
					"operation 'getCurrentPosition' failed, cannot evaluate presence in room", e);
		}
	}

	@Override
	public boolean hasRoomSensor() {
		return roomSensorIsPresent;
	}

	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		// requires operational sensor
		if(!hasOperationalSensor(direction))
			throw new UnsupportedOperationException("sensor in direction "+direction+" is not operational");
		
		// obstacleDistancesForwardRightBackwardLeft is updated when operations are performed
		int distance=obstacleDistancesForwardRightBackwardLeft.get(MazeMath.getDirectionIndex(direction));
		
		if(-1==distance) // this should not throw an exception, but to satisfy compiler
			try {
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
			obstacleDistancesForwardRightBackwardLeft.set(MazeMath.getDirectionIndex(direction), -1);
			throw new UnsupportedOperationException(
					"cannot calculate distance: "+direction+" sensor not operational");
		}
		try {
			if(!attemptEnergyDepletion(energyUsedForDistanceSensing)) return -1;
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
		return true;
	}
	
	/**
	 * Rotate the robot rightwards, adjusting required fields.
	 * Cause a failure if energy is depleted.
	 */
	private void rotateRight() {
		if(VERBOSE) {
			System.out.printf("robot is rotating RIGHT from %s: %s",currentDirection,obstacleDistancesForwardRightBackwardLeft);
		}
		
		// check that we don't run out of energy before attempting rotation
		if(!attemptEnergyDepletion(energyUsedForRotation)) return;
		
		// moving right is aligned with moving forward in array
		// which means: for what was right to become the new forward,
		// we have to rotate the array backwards
		Collections.rotate(obstacleDistancesForwardRightBackwardLeft, -1);
		
		// convert a turn to a cardinal direction and set current direction for robot
		currentDirection=MazeMath.turnToCardinalDirection(Turn.RIGHT, getCurrentDirection());
		// sets current direction for controller
		control.keyDown(UserInput.Right, 0);
		
		// check for agreement between robot & controller
		assert control.getCurrentDirection()==currentDirection;
		if(VERBOSE) {
			System.out.printf(" --> %s, now facing %s\n",obstacleDistancesForwardRightBackwardLeft,currentDirection);
		}
	}
	
	/**
	 * Rotate the robot leftwards, adjusting required fields.
	 * Cause a failure if energy is depleted.
	 */
	private void rotateLeft() {
		if(VERBOSE) {
			System.out.printf("robot is rotating LEFT from %s: %s",currentDirection,obstacleDistancesForwardRightBackwardLeft);
		}
		
		// check that we don't run out of energy before attempting rotation
		if(!attemptEnergyDepletion(energyUsedForRotation)) return;
		
		// opposite of right -- see `rotateRight` above for explanation
		Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
		
		// convert a turn to a cardinal direction and set current direction for robot
		currentDirection=MazeMath.turnToCardinalDirection(Turn.LEFT, getCurrentDirection());
		// sets current direction for controller
		control.keyDown(UserInput.Left, 0);
		
		// check for agreement between robot & controller
		assert control.getCurrentDirection()==currentDirection;
		if(VERBOSE) {
			System.out.printf(" --> %s, now facing %s\n",obstacleDistancesForwardRightBackwardLeft,currentDirection);
		}
	}
	
	@Override
	public void rotate(Turn turn) {
		if(hasStopped()) {
			if(VERBOSE) {
				System.out.println("the robot has stopped; cannot perform rotate operation");
			}
			return;
		}
		// we know how much energy should be used, so let's store the value for a test
		Float expectedEnergyDifference, energyBeforeRotation = getBatteryLevel();
		switch(turn) {
			case RIGHT:
				rotateRight();
				expectedEnergyDifference=energyUsedForRotation;
				break;
			case LEFT:
				rotateLeft();
				expectedEnergyDifference=energyUsedForRotation;
				break;
			case AROUND:
				// two rotations in same direction, which direction does not matter
				if(VERBOSE) {
					System.out.println("robot is rotating AROUND: ");
				}
				rotateLeft();
				rotateLeft();
				expectedEnergyDifference=2*energyUsedForRotation;
				break;
			default:
				System.out.println("!   !   !   !   INVALID TURN VALUE SPECIFIED   !   !   !   !");
				expectedEnergyDifference=null;
		}
		
		// if we stopped, we didn't use the expected energy; if not, we can check this
		if(!hasStopped()) assert energyBeforeRotation-getBatteryLevel() == expectedEnergyDifference :
			"energy difference before/after rotation: "+
			(energyBeforeRotation-getBatteryLevel())+
			", exepcted: "+expectedEnergyDifference;
	}

	@Override
	public void move(int distance, boolean manual){
		if(distance<0) {
			if(VERBOSE) {
				System.out.println("BasicRobot.move: cannot move negative distance");
			}
			return;
		}
		
		for(int moveCount=0; moveCount<distance; moveCount++) {
			if(hasStopped()) {
				if(VERBOSE) {
					System.out.println("the robot has stopped; cannot perform move operation");
				}
				return;
			}
			
			// use energies for another usage check
			float energyBeforeMove=getBatteryLevel();
			
			moveSingle(manual);
			
			float energyAfterMove=getBatteryLevel();
			
			// if robot has stopped, no reason to check energies below, as calculations stop
			if(hasStopped()) return;
			
			// branch on whether we have left the maze
			try {
				getCurrentPosition(); //throws exception if out of maze
				
				//incrementVisitCount(currentPosition); //prospective for Project 4
				
				//energy used should be sum of move energy + sensing energy for both side sensors
				assert (energyBeforeMove-energyAfterMove)==energyUsedForMove + 2*energyUsedForDistanceSensing:
					energyBeforeMove+", "+energyAfterMove+" at "+Arrays.toString(currentPosition);
			}
			catch (Exception e) {
				// we get here if the robot moves out of the maze
				// if the robot is outside the maze, there is no reason to calculate distances
				// so the only energy used is that of the move
				assert (energyBeforeMove-energyAfterMove)==energyUsedForMove:
					energyBeforeMove+", "+energyAfterMove+" at "+Arrays.toString(currentPosition);
				
				return;}
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
		control.setRobotFailureMessage(failureMessage);
		control.switchFromPlayingToWinning(odometerReading);
	}
	
	/**
	 * Move the robot one position forward, adjusting required fields.
	 * Cause a failure if energy is depleted or the robot crashes.
	 */
	private void moveSingle(boolean manual){
		// TODO how to factor in manual parameter?
		
		if(VERBOSE) {
			System.out.printf("robot move: %s   -->   ",obstacleDistancesForwardRightBackwardLeft);
		}
			
		// check energy before crash, because the robot cannot crash
		// if it does not have energy to move in the first place
		if(!attemptEnergyDepletion(energyUsedForMove)) return;
		
		if(atForwardWall()) {
			if(VERBOSE) {
				System.out.println();
			}
			endGame(badMoveMessage);
		}
		else {
			//no crash here
			incrementCurrentPosition();
			odometerReading++;
			changeDistancesInMoveForward();
			if(hasStopped()) return;
			
			control.keyDown(UserInput.Up, 0);
			//System.out.println("moveSingle: currentPosition="+Arrays.toString(currentPosition));
			//try {getCurrentPosition();}
			//catch (Exception e) {return;}
			
			assert odometerReading*energyUsedForMove<=control.getEnergyConsumedByRobotAtPresent() :
				"error: the robot cannot have used less energy than was required for odometer reading: "
				+"energy used at present is "+control.getEnergyConsumedByRobotAtPresent()+
				" but lower bound is "+(odometerReading*energyUsedForMove)+
				"; initial energy is "+initialEnergyLevel;
			
			
			assert Arrays.equals(control.getCurrentPosition(), currentPosition);
			if(VERBOSE) {
				System.out.printf("%s  - %s\n",obstacleDistancesForwardRightBackwardLeft,Arrays.toString(currentPosition));
			}
			//"moving as normal"
		}
	}
	
	@Override
	public void jump() throws Exception {
		if(hasStopped()) {
			if(VERBOSE) {
				System.out.println("the robot has stopped; cannot perform jump operation");
			}
			return;
		}
		// TODO change position, account for possibility that jump was unnecessary and replace with walk operation
		if(!atForwardWall()) {
			//case where a jump is not required, a walk suffices
			if(VERBOSE) {
				System.out.println("jump is not required: performing move operation");
			}
			move(1,control.manualRobotOperation);
			// TODO 'manual' parameter in move--what is proper value?
		}
		else { // jump required
			
			float energyBeforeJump = getBatteryLevel();
			
			if(VERBOSE) {
				System.out.println("performing jump operation");
			}
			
			// check if sufficient energy
			if(!attemptEnergyDepletion(energyUsedForJump)) return;
			
			// change position before calculating distances
			incrementCurrentPosition();
			try {
				// if we tried a bad jump, currentPosition is outside maze
				// and this call throws an error
				getCurrentPosition();
				
				// we know that the robot must be in front of a wall
				// i.e. distance to wall backwards is 0
				obstacleDistancesForwardRightBackwardLeft.set(2, 0);
				
				calculateDistances(Direction.BACKWARD);
				// could run out of energy here
				if(hasStopped()) return;
				
				//incrementVisitCount(currentPosition); //prospective for Project 4
				
				// robot is finished, push changes to Controller
				control.keyDown(UserInput.Jump, 0);
				
				assert Arrays.equals(control.getCurrentPosition(), currentPosition);
				
				assert getBatteryLevel()==energyBeforeJump-(energyUsedForJump+3*energyUsedForDistanceSensing):
					"energy difference before/after rotation: "+
					(energyBeforeJump-getBatteryLevel())+
					", exepcted: "+(energyUsedForJump+3*energyUsedForDistanceSensing);
			}
			catch (Exception e) {
				//System.out.println("BasicRobot.jump: exception thrown:\n");
				//e.printStackTrace();
				endGame(badJumpMessage);
				throw new Exception(badJumpMessage);
			}
		}
	}
	
	
	
	/**
	 * Check if there is a wall immediately in front of the robot.
	 * @return true if distance to forward wall is 0
	 */
	private boolean atForwardWall() {
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
				//System.out.println("calculateDistances: skipping "+d);
				continue;
			}
			try {
				calculateObstacleDistance(d);
			} catch (Exception e) {
				// System.out.println("cannot calculate distance in direction "+d);
				// e.printStackTrace();
				// System.out.println(e.getMessage()+"-->\n   "+e.getCause().getMessage());
			}
		}
	}
	
	/**
	 * Increment the robot's backward distance and decrement the forward distance,
	 * as happens when the robot moves forward. Get the left/right distances
	 * by using sensors. Designed to optimize energy usage.
	 * 
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
		if(Integer.MAX_VALUE!=dForward) obstacleDistancesForwardRightBackwardLeft.set(0,dForward-1);
		if(Integer.MAX_VALUE!=dBackward) obstacleDistancesForwardRightBackwardLeft.set(2,dBackward+1);
		
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
			endGame(noEnergyMessage);
			return;
		}
		//System.out.printf("setting battery: change=%d\n",(int)amount);
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
		//if(canSeeThroughTheExitIntoEternity(d)) return Integer.MAX_VALUE;
		
		
		
		CardinalDirection cd = translateCurrentDirectionToCardinalDirection(d);
		int[] dir = cd.getDirection();
		int dx = dir[0], dy = dir[1];
		int[] pos = getCurrentPosition();
		int x = pos[0], y = pos[1];
		
		// if the robot is next to a wall in the given direction,
		// this loop does not iterate and (x,y) do not change
		while(floorplan.hasNoWall(x, y, cd)) {
			x+=dx;
			y+=dy;
			
			// if true, we've reached beyond the maze--we can see through the exit
			if(!maze.isValidPosition(x, y)) return Integer.MAX_VALUE;
		}
		
		
		// pos is unchanged, so computing Manhattan distance
		// between (x,y) and pos gives total distance covered
		return MazeMath.manhattanDistance(x, y, pos);
	}
	
	/**
	 * Translate relative direction to absolute direction given knowledge
	 * of how current direction relates to a forward direction.
	 * @param d a value of {@link Direction}
	 * @return {@link CardinalDirection} corresponding to input direction
	 */
	private CardinalDirection translateCurrentDirectionToCardinalDirection(Direction d) {
		// calculate how far right d is from forward
		// add this to the index of the absolute direction
		// matching the forward direction
		return MazeMath.DirectionToCardinalDirection(d, getCurrentDirection());
	}
	
	
	
	
	/*
	
	//////////////// PLANNING FOR PROJECT 4 ////////////////
	
	//when ready to use, search through this document for comments including the string "Project 4"
	
	private boolean hasWallInDirection(Direction d) {
		return floorplan.hasWall(
				currentPosition[0],currentPosition[1],
				translateCurrentDirectionToCardinalDirection(d));
	}
	
	private int[] getCoordinateDelta(Direction d) {
		return translateCurrentDirectionToCardinalDirection(d).getDirection();
	}
	
	private int[] getNewCoordinateFromDirectionalMove(Direction d) {
		return MazeMath.addArrays(currentPosition,getCoordinateDelta(d));
	}
	
	private List<Direction> getMoveableDirections() {
		ArrayList<Direction> moveableDirections = new ArrayList<Direction>(4);
		for(Direction d: Direction.values()) {
			if(!hasWallInDirection(d)) moveableDirections.add(d);
		}
		moveableDirections.trimToSize();
		return moveableDirections;
	}
	
	private int getVisitCount(int[] coor) {
		return visitCounts[coor[0]][coor[1]];
	}
	
	private void incrementVisitCount(int[] coor) {
		visitCounts[coor[0]][coor[1]]+=1;
	}
	
	private Direction selectMoveableDirection_byMinimalVisits() {
		// get the direction which, when the robot moves in this direction,
		// places the robot at the cell which has the smallest visit count
		// of the cells to which the robot can move
		// this works even if there is only one cell the robot can move to
		return getMoveableDirections()
			.stream()
			.min(
				(d1,d2) ->
					Integer.compare(
						getVisitCount(getNewCoordinateFromDirectionalMove(d1)),
						getVisitCount(getNewCoordinateFromDirectionalMove(d2))
					)
			).get();
		
	}
	
	private void rotateToDirection(Direction d) {
		switch(d) {
		case FORWARD: return;
		case LEFT: rotate(Turn.LEFT); return;
		case RIGHT: rotate(Turn.RIGHT); return;
		case BACKWARD: rotate(Turn.AROUND); return;
		}
	}
	
	private void walkMoveableDirection_byMinimalVisits() {
		rotateToDirection(selectMoveableDirection_byMinimalVisits());
		move(1, false);
	}
	
	private Direction directionToExit() {
		for(Direction d: Direction.values()) {
			if(canSeeThroughTheExitIntoEternity(d)) return d;
		}
		return null;
	}
	
	public void walkOutOfMaze_byMinimalVisits() {
		Direction d=null;
		while(null==d) {
			walkMoveableDirection_byMinimalVisits();
			d=directionToExit();
		}
		rotateToDirection(d);
		while(true) {
			move(1,false);
			try{getCurrentPosition();}
			catch(Exception e) {break;}
		}
		return;
	}
	
	*/

}

