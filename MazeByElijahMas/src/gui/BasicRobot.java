package gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

	private Controller control;
	
	private float batteryLevel;
	private int odometerReading;
	private HashMap<Direction,Boolean> sensorFunctionalFlags;
	private HashMap<Direction,int[]> mapDirectionsToFloorplan;
	private ArrayList<Integer> obstacleDistancesForwardRightBackwardLeft=new ArrayList<Integer>(4);
	//private HashMap<Integer,Direction> obstacleDistancesIndices;
	private Maze maze;
	private Floorplan floorplan;
	private Distance distance;
	private int[][] dists;
	private boolean roomSensorIsPresent;
	private boolean initialized;
	private boolean stopped;
	private int[] currentPosition;
	private CardinalDirection currentDirection;
	private static boolean crashEnabled=true;
	
	private static final String badJumpMessage="robot tried bad jump";
	private static final String badMoveMessage="robot crashed";
	private static final String noEnergyMessage="robot out of energy";
	private static final float initialEnergyLevel=3000f;
	
	/**
	 * ArrayList that enumerates cardinal directions
	 * in order of rotating rightwards, starting at West.
	 */
	private static final List<CardinalDirection> WestSouthEastNorth =
		(List<CardinalDirection>)Arrays.asList(
			CardinalDirection.West,
			CardinalDirection.South,
			CardinalDirection.East,
			CardinalDirection.North
		)
	;
	
	protected static final Direction[] ForwardRightBackwardLeft = new Direction[] {
		Direction.FORWARD, Direction.RIGHT, Direction.BACKWARD, Direction.LEFT
	};
	
	//private static HashMap<Integer,Direction> create_obstacleDistancesIndices(){
	//	HashMap<Integer,Direction> map = new HashMap<Integer,Direction>(4);
	//	
	//}
	
	final static float energyUsedForJump=-50;
	final static float energyUsedForMove=-5;
	final static float energyUsedForRotation=-3;
	final static float energyUsedForDistanceSensing=-1;
	
	public BasicRobot() {}
	
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
		dists=distance.getAllDistanceValues();
		stopped=false;
		
		currentPosition = control.getCurrentPosition();
		currentDirection = control.getCurrentDirection();
		
		//
		//nonce value added, it is changed by calculateObstacleDistance
		for(int directionCounter=0; directionCounter<4; directionCounter++)
			obstacleDistancesForwardRightBackwardLeft.add(-1);
		
		calculateDistances();
		
		initialized=true;
		System.out.println("BasicRobot: instantiateFields completed");
		System.out.printf("BasicRobot: maze dimensions: %d,%d\n",maze.getWidth(),maze.getHeight());
	}
	
	private void calculateDistances(Direction... exclusions) {
		List<Direction> exclude = Arrays.asList(exclusions);
		for(Direction d: ForwardRightBackwardLeft) {
			if(exclude.contains(d)) {
				System.out.println("calculateDistances: skipping "+d);
				continue;
			}
			try {
				calculateObstacleDistance(d);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setMaze(Controller controller) {
		control=controller;
		instantiateFields();
		System.out.println("robot has set controller");
	}
	
	
	@Override
	public int[] getCurrentPosition() throws Exception {
		if(!maze.isValidPosition(currentPosition[0], currentPosition[1]))
			throw new Exception(String.format(
					"Exception: %s is an invalid position for a maze of dimensions %d x %d",
					Arrays.toString(currentPosition), maze.getWidth() ,maze.getHeight() ));
		return currentPosition;
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

	@Override
	public int getOdometerReading() {
		return odometerReading;
	}

	@Override
	public void resetOdometer() {
		odometerReading=0;
	}

	@Override
	public float getEnergyForFullRotation() {
		return 12f;
	}

	@Override
	public float getEnergyForStepForward() {
		return 5f;
	}

	@Override
	public boolean isAtExit() {
		try {
			return Arrays.equals(getCurrentPosition(), distance.getExitPosition());
		} catch (Exception e) {
			System.out.println("BasicRobot: failed to evaluate 'isAtExit':");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean canSeeThroughTheExitIntoEternity(Direction direction) throws UnsupportedOperationException {
		if (!hasOperationalSensor(direction))
			throw new UnsupportedOperationException("cannot sense whether looking out of maze:"+direction+" sensor not present");
		try {
			//if(!isAtExit()) return false;
			//int[] pos = getCurrentPosition();
			//return floorplan.hasNoWall(pos[0], pos[1], translateDirectionToCardinalDirection(direction));
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
		if (!hasRoomSensor())
			throw new UnsupportedOperationException("cannot sense presence in room: room sensor not present");
		try {
			int[] pos;
			pos = getCurrentPosition();
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
	public boolean hasStopped() {
		return stopped;
	}

	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		if(!hasOperationalSensor(direction))
			throw new UnsupportedOperationException("sensor in direction "+direction+" is not operational");
		return obstacleDistancesForwardRightBackwardLeft.get(getDirectionIndex(direction));
	}
	
	private boolean calculateObstacleDistance(Direction direction) throws Exception {
		if(hasStopped()) {
			System.out.println("cannot calculate distance: robot stopped");
			return false;
		}
		try {
			changeEnergyLevel(energyUsedForDistanceSensing);
			int d = getObstacleDistance(direction);
			
			obstacleDistancesForwardRightBackwardLeft.set(getDirectionIndex(direction), d);
			
			return true;
		} catch (Exception e){
			throw new UnsupportedOperationException("distanceToObstacle at direction "+direction+" failed:",e);
		}
		
	}
	
	private boolean hasBattery() {
		return getBatteryLevel()>0;
	}
	
	private void setStopped() {
		stopped=true;
	}
	
	private void stopCheck() {
		if(!hasBattery()) setStopped();
	}

	@Override
	public boolean hasOperationalSensor(Direction direction) {
		// if the first term is not true, this short-circuits
		// so the second term cannot inject null into the boolean comparison
		return (hasDirectionalSensor(direction) && sensorFunctionalFlags.get(direction));
	}

	@Override
	public void triggerSensorFailure(Direction direction) {
		if(hasDirectionalSensor(direction))
			sensorFunctionalFlags.put(direction,false);
	}

	@Override
	public boolean repairFailedSensor(Direction direction) {
		if(!hasDirectionalSensor(direction)) return false;
		sensorFunctionalFlags.put(direction,true);
		return true;
	}
	
	private CardinalDirection getNewDirection(Turn turn) {
		int index = WestSouthEastNorth.indexOf(currentDirection);
		int adjust;
		
		// array is arranged in left-to-right order
		adjust = (turn==Turn.LEFT) ? -1 : 1;
		
		// use floorMod to prevent negative index
		return WestSouthEastNorth.get(Math.floorMod(index+adjust,4));
	}
	
	private void rotateLeft() {
		System.out.print("robot is rotating LEFT: ");
		Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
		currentDirection=getNewDirection(Turn.LEFT);
		changeEnergyLevel(energyUsedForRotation);
		if(hasStopped()) return;
		control.keyDown(UserInput.Left, 0);
		assert control.getCurrentDirection()==currentDirection;
	}
	
	private void rotateRight() {
		System.out.print("robot is rotating RIGHT: ");
		Collections.rotate(obstacleDistancesForwardRightBackwardLeft, -1);
		currentDirection=getNewDirection(Turn.RIGHT);
		changeEnergyLevel(energyUsedForRotation);
		if(hasStopped()) return;
		control.keyDown(UserInput.Right, 0);
		assert control.getCurrentDirection()==currentDirection;
	}
	
	@Override
	public void rotate(Turn turn) {
		if(hasStopped()) {
			System.out.println("the robot has stopped; cannot perform rotate operation");
			return;
		}
		switch(turn) {
			case RIGHT:
				// moving right is aligned with moving forward in array
				// which means that for what was right to become the new forward,
				// we have to shift things backwards
				rotateRight();
				break;
			case LEFT:
				// opposite of right
				rotateLeft();
				break;
			case AROUND:
				// two rotations in same direction, which direction does not matter
				System.out.println("robot is rotating AROUND: ");
				rotateLeft();
				rotateLeft();
				break;
		}
		System.out.println(obstacleDistancesForwardRightBackwardLeft);
	}

	@Override
	public void move(int distance, boolean manual){
		if(distance<0) {
			System.out.println("BasicRobot.move: cannot move negative distance");
			return;
		}
		
		for(int moveCount=0; moveCount<distance; moveCount++) {
			if(hasStopped()) {
				System.out.println("the robot has stopped; cannot perform move operation");
				return;
			}
			
			moveSingle(1,manual);
			try {getCurrentPosition();}
			catch (Exception e) {return;}
		}
	}
	
	private void incrementCurrentPosition() {
		currentPosition=addArrays(currentPosition,currentDirection.getDirection());
	}
	
	private void endGame(String failureMessage) {
		setStopped();
		control.setRobotFailureMessage(failureMessage);
		control.switchFromPlayingToWinning(odometerReading);
	}
	
	private void moveSingle(int distance, boolean manual){
		// TODO account for crash
		// TODO how to factor in manual parameter?
		// TODO allow 'distance' parameter to be more than 1
		// TODO prevent negative 'distance' values (StatePlaying may submit -1 as a value)
				// this may mean preventing StatePlaying from moving backwards
		
		System.out.printf("robot move: %s   -->   ",obstacleDistancesForwardRightBackwardLeft);
			
		boolean no_move=false;
		if(atForwardWall()) {
			System.out.println();
			endGame(badMoveMessage);
		}
		else {
			changeEnergyLevel(energyUsedForMove);
			if(hasStopped()) return;
			//no crash here
			incrementCurrentPosition();
			odometerReading++;
			changeDistancesInMoveForward();
			control.keyDown(UserInput.Up, 0);
			//try {getCurrentPosition();}
			//catch (Exception e) {return;}
			
			assert odometerReading*energyUsedForMove<=control.getEnergyConsumedByRobotAtPresent() :
				"error: the robot cannot have used less energy than was required for odometer reading";
			
			
			assert Arrays.equals(control.getCurrentPosition(), currentPosition);
			String msg = no_move ? "cannot move here: wall in the way" : "moving as normal";
			System.out.printf("%s  - %s\n",obstacleDistancesForwardRightBackwardLeft,msg);
		}
	}
	
	private static int[] addArrays(int[] a1, int[] a2) {
		if(a1.length!=a2.length) return null;
		int[] result=new int[a1.length];
		for(int i=0; i<a1.length; i++) result[i]=a1[i]+a2[i];
		return result;
	}

	@Override
	public void jump() throws Exception {
		if(hasStopped()) {
			System.out.println("the robot has stopped; cannot perform jump operation");
			return;
		}
		// TODO change position, account for possibility that jump was unnecessary and replace with walk operation
		if(!atForwardWall()) {
			//case where a jump is not required, a walk suffices
			System.out.println("jump is not required: performing move operation");
			move(1,control.manualRobotOperation);
			// TODO 'manual' parameter in move--what is proper value?
		}
		else {
			//a jump is required
			System.out.println("performing jump operation");
			changeEnergyLevel(energyUsedForJump);
			if(hasStopped()) return;
			
			// change position before calculating distances
			incrementCurrentPosition();
			try {
				getCurrentPosition(); // this line throws exception for bad jump
				calculateDistances();
				control.keyDown(UserInput.Jump, 0);
				assert Arrays.equals(control.getCurrentPosition(), currentPosition);
			}
			catch (Exception e) {endGame(badJumpMessage);}
		}
	}
	
	
	
	public Controller getController() {
		return control;
	}
	
	
	
	
	/**
	 * Check if there is a wall immediately in front of the robot.
	 * @return true if distance to forward wall is 0
	 */
	private boolean atForwardWall() {
		return 0==obstacleDistancesForwardRightBackwardLeft.get(
			getDirectionIndex(Direction.FORWARD)
		);
	}
	
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
		
		try {
			calculateObstacleDistance(Direction.RIGHT);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		try {
			calculateObstacleDistance(Direction.LEFT);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	private void changeEnergyLevel(float amount) {
		float newEnergy=getBatteryLevel()+amount;
		if(newEnergy<0) {
			setStopped();
			setBatteryLevel(0);
			endGame(noEnergyMessage);
			return;
		}
		//System.out.printf("setting battery: change=%d\n",(int)amount);
		setBatteryLevel(newEnergy);
	}
	
	private boolean hasDirectionalSensor(Direction direction) {
		return sensorFunctionalFlags.containsKey(direction);
	}
	
	private int getObstacleDistance(Direction d) throws Exception {
		//if(canSeeThroughTheExitIntoEternity(d)) return Integer.MAX_VALUE;
		CardinalDirection cd = translateDirectionToCardinalDirection(d);
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
		return Math.abs(x-pos[0]) + Math.abs(y-pos[1]);
	}
	
	private CardinalDirection translateDirectionToCardinalDirection(Direction d) {
		return WestSouthEastNorth.get(
				(WestSouthEastNorth.indexOf(getCurrentDirection()) + getDirectionIndex(d))
				%4);
	}
	
	private int getDirectionIndex(Direction d) {
		switch(d) {
			case FORWARD:	return 0;
			case RIGHT:		return 1;
			case BACKWARD:	return 2;
			case LEFT:		return 3;
			
			default:		return -1;
		}
		
	}
	

}

