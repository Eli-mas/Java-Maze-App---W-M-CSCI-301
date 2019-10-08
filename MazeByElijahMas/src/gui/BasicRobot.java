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
	boolean initialized;
	
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
		batteryLevel=3000;
		odometerReading=0;
		sensorFunctionalFlags = new HashMap<Direction,Boolean>();
		
		for(Direction d: Direction.values()) sensorFunctionalFlags.put(d,true);
		
		
		roomSensorIsPresent=true;
		maze=control.getMazeConfiguration();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		dists=distance.getAllDistanceValues();
		initialized=true;
		System.out.println("BasicRobot: instantiateFields completed");
		System.out.printf("BasicRobot: maze dimensions: %d,%d\n",maze.getWidth(),maze.getHeight());
		
		//int directionCounter=0;
		for(Direction d: ForwardRightBackwardLeft) {
			//nonce value added, it is changed by calculateObstacleDistance
			obstacleDistancesForwardRightBackwardLeft.add(-1);
			try {
				calculateObstacleDistance(d);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
		int[] pos=control.getCurrentPosition();
		int p0=pos[0],p1=pos[1];
		int w=maze.getWidth(),h=maze.getHeight();
		if(pos[0]<0 || pos[0]>maze.getWidth() || pos[1]<0 || pos[1]>maze.getHeight())
			throw new Exception(String.format(
					"Exception: %s is an invalid position for a maze of dimensions %d x %d",
					Arrays.toString(pos), maze.getWidth() ,maze.getHeight() ));
		return pos;
	}

	@Override
	public CardinalDirection getCurrentDirection() {
		return control.getCurrentDirection();
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		if(!hasOperationalSensor(direction))
			throw new UnsupportedOperationException("sensor in direction "+direction+" is not operational");
		return obstacleDistancesForwardRightBackwardLeft.get(getDirectionIndex(direction));
	}
	
	//private void 
	
	private void calculateObstacleDistance(Direction direction) throws Exception {
		try {
			int d = getObstacleDistance(direction);
			changeEnergyLevel(energyUsedForDistanceSensing);
			
			obstacleDistancesForwardRightBackwardLeft.set(getDirectionIndex(direction), d);
			
			//return ;
		} catch (Exception e){
			throw new UnsupportedOperationException("distanceToObstacle at direction "+direction+" failed:",e);
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
		if(hasDirectionalSensor(direction))
			sensorFunctionalFlags.put(direction,false);
	}

	@Override
	public boolean repairFailedSensor(Direction direction) {
		if(!hasDirectionalSensor(direction)) return false;
		sensorFunctionalFlags.put(direction,true);
		return true;
	}
	
	@Override
	public void rotate(Turn turn) {
		// TODO change direction
		switch(turn) {
			case RIGHT:
				// moving right is aligned with moving forward in array
				// which means that for what was right to become the new forward,
				// we have to shift things backwards
				System.out.print("robot is rotating RIGHT: ");
				Collections.rotate(obstacleDistancesForwardRightBackwardLeft, -1);
				break;
			case LEFT:
				// opposite of right
				Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
				System.out.print("robot is rotating LEFT: ");
				break;
			case AROUND:
				// two rotations in same direction, which direction does not matter
				System.out.print("robot is rotating AROUND: ");
				Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
				Collections.rotate(obstacleDistancesForwardRightBackwardLeft, 1);
				changeEnergyLevel(energyUsedForRotation);
				break;
		}
		changeEnergyLevel(energyUsedForRotation);
		System.out.println(obstacleDistancesForwardRightBackwardLeft);
	}

	@Override
	public void move(int distance, boolean manual) {
		// TODO account for crash
		// TODO how to factor in manual parameter?
		// TODO allow 'distance' parameter to be more than 1
		// TODO prevent negative 'distance' values (StatePlaying may submit -1 as a value)
		
		
		System.out.printf("robot move: %s   -->   ",obstacleDistancesForwardRightBackwardLeft);
			
		changeEnergyLevel(energyUsedForMove);
		boolean no_move=false;
		if(atForwardWall()) {
			//case of a crash
			no_move=true;
		}
		else {
			//no crash here
			changeDistancesInMoveForward();
		}
		String msg = no_move ? "cannot move here: wall in the way" : "moving as normal";
		System.out.printf("%s  - %s\n",obstacleDistancesForwardRightBackwardLeft,msg);
	}

	@Override
	public void jump() throws Exception {
		// TODO change position, account for possibility that jump was unnecessary and replace with walk operation
		changeEnergyLevel(energyUsedForJump);
		if(atForwardWall()) {
			//case where a jump is not required, a walk suffices
			move(1,control.manualRobotOperation);
			// TODO 'manual' parameter in move--what is proper value?
		}
		else {
			//a jump is required
			
		}
	}
	
	
	
	public Controller getController() {
		return control;
	}
	
	
	
	
	private boolean atForwardWall() {
		return 0==obstacleDistancesForwardRightBackwardLeft.get(0);
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
		try {
			//obstacleDistancesForwardRightBackwardLeft.set(1, calculateObstacleDistance(Direction.RIGHT));
			calculateObstacleDistance(Direction.RIGHT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			//obstacleDistancesForwardRightBackwardLeft.set(3, calculateObstacleDistance(Direction.LEFT));
			calculateObstacleDistance(Direction.LEFT);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void changeEnergyLevel(float amount) {
		setBatteryLevel(getBatteryLevel()+amount);
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
			if(x<0 || x>=maze.getWidth() || y<0 || y>=maze.getHeight()) return Integer.MAX_VALUE;
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

