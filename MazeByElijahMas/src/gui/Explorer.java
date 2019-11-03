package gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import comp.ExtendedList;
import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import generation.Maze;
import generation.SingleRandom;
import gui.Robot.Direction;
import gui.Robot.Turn;
import gui.Constants;

/**
 * <b>WARNING: INCOMPLETE</b>
 * 
 * This class is not fully developed and should not be used at this time.
 * 
 * @author Elijah Mas
 *
 */
public class Explorer extends AbstractRobotDriver {
	
	private int[][] visitCounts;
	
	private HashSet<Room> rooms;

	private int[] start;
	
	public Explorer() {
		
	};
	
	
	public void setStart(int[] start) {
		this.start=start;
	}

	@Override
	public void setDistance(Distance distance) {
		this.distance=distance;
		// distance should already be set
		
		int width=distance.getAllDistanceValues().length;
		int height=distance.getAllDistanceValues()[0].length;
		
		visitCounts=new int[width][height];
		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				visitCounts[x][y]=0;
			}
		}
		System.out.println("visitCounts="+visitCounts);
		System.out.println("start="+start);
		currentPosition=start;
		incrementVisitCount(start);
	}

	@Override
	public void triggerUpdateSensorInformation() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean drive2Exit() throws Exception {
		rooms = new HashSet<Room>();
		locations = new ArrayList<int[]>();
		currentDirection  = robot.getCurrentDirection();
		locations.add(currentPosition);
		walkOutOfMaze_byMinimalVisits();
		return true;
	}
	
	
	//////////////// private methods ////////////////
	
	/*private boolean hasWallInDirection(Direction d) {
		return robot.distanceToObstacle(d)==0;
	}
	
	private boolean hasForwardWall() {
		return hasWallInDirection(Direction.FORWARD);
	}
	
	private int[] getCoordinateDelta(Direction d) {
		return MazeMath.directionToDelta(d, robot.getCurrentDirection());
	}
	
	private int[] getNewCoordinateFromDirectionalMove(Direction d) {
		return MazeMath.addArrays(currentPosition,getCoordinateDelta(d));
	}
	
	private void rotateTo(Direction d) {
		switch(d) {
		case FORWARD: return;
		case LEFT: robot.rotate(Turn.LEFT); return;
		case RIGHT: robot.rotate(Turn.RIGHT); return;
		case BACKWARD: robot.rotate(Turn.AROUND); return;
		}
	}
	
	private void rotateTo(CardinalDirection cd) {
		rotateTo(MazeMath.convertDirs(cd, robot.getCurrentDirection()));
	
	private Direction directionToExit() {
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			if(robot.canSeeThroughTheExitIntoEternity(d)) return d;
		}
		return null;
	}
	}*/
	
	private List<CardinalDirection> getMoveableDirections() {
		ArrayList<CardinalDirection> moveableDirections = new ArrayList<CardinalDirection>(4);
		for(CardinalDirection cd: CardinalDirection.values()) {
			if(getDistance(cd)!=0) moveableDirections.add(cd);
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
	
	private CardinalDirection selectMoveableDirection_byMinimalVisits() {
		// get the direction which, when the robot moves in this direction,
		// places the robot at the cell which has the smallest visit count
		// of the cells to which the robot can move
		// this works even if there is only one cell the robot can move to
		
		List<CardinalDirection> moveable_cds = getMoveableDirections();
		//System.out.println("moveable_cds: "+moveable_cds);
		List<int[]> moveableCells =
			moveable_cds.stream().map(cd -> MazeMath.addArrays(cd.getDirection(),currentPosition)).collect(Collectors.toList());
		//System.out.println("moveableCells: "+moveableCells);
		
		//System.out.println(Arrays.toString(moveableCells.get(0)));
		
		int minVisit = (moveableCells.size()==1) ? 
				getVisitCount(moveableCells.get(0)) :
				moveableCells.stream().map(this::getVisitCount).min(Integer::compare).get()
		;
		
		//System.out.println("minVisit: "+minVisit);
		
		List<int[]> minVisitedCells =
				moveableCells
				.stream()
				.filter(cell -> getVisitCount(cell)==minVisit)
				.collect(Collectors.toList());
		
		int[] exit = minVisitedCells.get(
			SingleRandom.getRandom()
			.nextIntWithinInterval(0,minVisitedCells.size()-1)
		);
		
		return CardinalDirection.getDirection(MazeMath.subArrays(exit, currentPosition));
		
	}
	
	/*public Object choose(Object... objects) {
		if(1==objects.length)
			return objects[0];
		
		return objects[nextIntWithinInterval(0,objects.length-1)];
	}*/
	
	private void walkMoveableDirection_byMinimalVisits() throws Exception {
		currentDirection = selectMoveableDirection_byMinimalVisits();
		moveRobotE(currentDirection,1);
		currentPosition = getRobotPosition();
		
		incrementVisitCount(currentPosition);
		locations.add(currentPosition);
	}
	
	private void addRoom(Room room) {
		rooms.add(room);
	}
	
	private Room getContainingRoom(int[] cell) {
		for(Room room: rooms) {
			if(room.contains(cell))
				return room;
		}
		return null;
	}
	
	/**
	 * Walk the robot to a corner of the room when it is already along a wall
	 */
	private void walkUntilCornerOfRoomReached(HashMap<CardinalDirection,HashMap<Integer,Integer>> wallSides) throws Exception {
		//assume robot driver is facing a corner of the room
		Integer distToWall = getDistance(currentDirection);
		
		System.out.printf("walkUntilCornerOfRoomReached: currentPosition=%s, facing %s (%s): distance to wall = %d\n",
			Arrays.toString(currentPosition), currentDirection,Arrays.toString(currentDirection.getDirection()),distToWall);
		
		CardinalDirection side1=MazeMath.getFrom(currentDirection, -1);
		CardinalDirection side2=MazeMath.getFrom(currentDirection, 1);
		
		int invariantIndex,variableIndex;
		
		if(side1==CardinalDirection.West || side1==CardinalDirection.East) {
			invariantIndex=1;
			variableIndex=0;
		}
		else {
			invariantIndex=0;
			variableIndex=1;
		}
		int[] position;
		int variable, invariant;
		for(int i=0; i<distToWall; i++) {
			assert !robot.hasStopped() : robot.getFailureMessage();
			
			if(!robot.isInsideRoom()) {
				System.out.println("robot out of room: "+Arrays.toString(currentPosition));
				moveRobotE(MazeMath.getFrom(currentDirection, 2),1);
				return;
			}
			position = getRobotPosition();
			
			variable = position[variableIndex];
			invariant = position[invariantIndex];
			
			if(!wallSides.get(side1).containsKey(variable)) {
				wallSides.get(side1).put(variable, invariant+getDistance(side1));
			}
			
			if(!wallSides.get(side2).containsKey(variable)) {
				wallSides.get(side2).put(variable, invariant+getDistance(side2));
			}
			
			moveRobotE(currentDirection,1);
		}
		
		
	}
	
	private List<int[]> walkUntilCornerOfRoomReached2(CardinalDirection directionMoving, CardinalDirection directionOfTracingWall) throws Exception {
		List<int[]> exits = new LinkedList<int[]>();
		
		int wallDistance = getDistance(directionMoving);
		faceRobotE(directionMoving);
		for(int i=0; i<wallDistance; i++) {
			moveRobotE(directionMoving,1);
			
			if(!robot.isInsideRoom()) {
				currentDirection=MazeMath.getFrom(currentDirection, 2);
				faceRobotE(currentDirection);
				moveRobotE(1);
			}
			
			if(getDistance(directionOfTracingWall)==0) {
				exits.add(currentPosition);
			}
		}
		
		return exits;
	}
	
	private Room scopeRoomUponEntry2() throws Exception{
		
		int[] roomStartPosition = currentPosition;
		CardinalDirection startDirection = MazeMath.getFrom(currentDirection, 1);
		
		currentDirection = startDirection;
		CardinalDirection exitDirection1 = MazeMath.getFrom(currentDirection, 1);
		List<int[]> exits1 = walkUntilCornerOfRoomReached2(currentDirection, exitDirection1);
		int[] corner1 = currentPosition;
		
		currentDirection = MazeMath.getFrom(currentDirection, -1);
		CardinalDirection exitDirection2 = MazeMath.getFrom(currentDirection, 1);
		List<int[]> exits2 = walkUntilCornerOfRoomReached2(currentDirection, exitDirection2);
		
		currentDirection = MazeMath.getFrom(currentDirection, -1);
		CardinalDirection exitDirection3 = MazeMath.getFrom(currentDirection, 1);
		List<int[]> exits3 = walkUntilCornerOfRoomReached2(currentDirection, exitDirection3);
		int[] corner2 = currentPosition;
		
		currentDirection = MazeMath.getFrom(currentDirection, -1);
		CardinalDirection exitDirection4 = MazeMath.getFrom(currentDirection, 1);
		List<int[]> exits4 = walkUntilCornerOfRoomReached2(currentDirection, exitDirection4);
		
		currentDirection = MazeMath.getFrom(currentDirection, -1);
		assert currentDirection == startDirection;
		List<int[]> exits_1a = new LinkedList<int[]>();
		
		while(!Arrays.equals(roomStartPosition,currentPosition)) {
			moveRobotE(currentDirection,1);
			
			if(getDistance(exitDirection1)==0) {
				exits_1a.add(currentPosition);
			}
		}
		
		Room room = new Room();
		room.setCorners(corner1,corner2);
		room.setVisitCounts(visitCounts);
		
		List<List<int[]>> exits = Arrays.asList(exits1,exits2,exits3,exits4,exits_1a);
		CardinalDirection[] exitDirections = new CardinalDirection[] {exitDirection1,exitDirection2,exitDirection3,exitDirection4,exitDirection1};
		
		for(int i=0; i<5; i++) {
			CardinalDirection cd = exitDirections[i];
			for(int[] exit: exits.get(i)) {
				room.addExit(exit,cd);
			}
		}
		
		return room;
	}
	
	private Room scopeRoomUponEntry() throws Exception {
		
		System.out.printf("robot entered room at %s, facing %s (%s)\n",
			Arrays.toString(currentPosition), currentDirection,Arrays.toString(currentDirection.getDirection()));
		
		HashMap<CardinalDirection,HashMap<Integer,Integer>> wallSides = new HashMap<CardinalDirection,HashMap<Integer,Integer>>(4);
		
		for(CardinalDirection cd: CardinalDirection.values()) {
			wallSides.put(cd,new HashMap<Integer,Integer>());
		}
		
		
		
		//assume robot has just entered the room
		// turn right, walk until corner reached
		currentDirection = MazeMath.getFrom(currentDirection, 1);
		walkUntilCornerOfRoomReached(wallSides);
		
		//keep track of location of this corner
		int[] corner1 = getRobotPosition();
		
		// now walk to the other corner along the current wall
		// opposite the direction just walked
		currentDirection = MazeMath.getFrom(currentDirection, 2);
		walkUntilCornerOfRoomReached(wallSides);
		
		// walk to the opposite corner at the other end of the room
		currentDirection = MazeMath.getFrom(currentDirection, 1);
		walkUntilCornerOfRoomReached(wallSides);
		
		int[] corner2 = getRobotPosition();
		
		Room room = new Room(new int[][] {corner1,corner2});
		
		for(CardinalDirection cd: CardinalDirection.values()) {
			int x,y;
			HashMap<Integer,Integer> potentialExitLocations = wallSides.get(cd);
			for(int k: potentialExitLocations.keySet()) {
				if(cd==CardinalDirection.West || cd==CardinalDirection.East) {
					x=k;
					y=potentialExitLocations.get(x);
				}
				else {
					y=k;
					x=potentialExitLocations.get(y);
				}
				
				if(!room.contains(new int[] {x,y})) {
					System.out.printf("x, y, cd, room: %s, %s, %s, %s",x,y,cd,room);
					room.addExit(x, y, cd);
				}
			}
			
		}
		
		System.out.println("room scoped: exits: "+room.getExitsView().stream().map(Arrays::toString).collect(Collectors.toList()));
		
		return room;
	}
	
	private void faceRobotE(CardinalDirection cd) throws Exception {
		faceRobot(cd);
		if(robot.hasStopped()) throw new Exception(robot.getFailureMessage());
	}
	
	private void moveRobotE(CardinalDirection cd,int distance) throws Exception {
		faceRobotE(cd);
		for(int i=0; i<distance; i++) moveRobotE(1);
		currentPosition = getRobotPosition();
		if(robot.hasStopped()) throw new Exception(robot.getFailureMessage());
	}
	
	private void moveRobotE(int distance) throws Exception {
		robot.move(distance, false);
		currentPosition = getRobotPosition();
		locations.add(currentPosition);
		if(robot.hasStopped()) throw new Exception(robot.getFailureMessage());
	}
	
	public void walkOutOfMaze_byMinimalVisits() throws Exception {
		CardinalDirection cd=null;
		
		// get to the point where the exit is in sight
		while(null==cd) {
			walkMoveableDirection_byMinimalVisits();
			if(robot.isInsideRoom()) {
				Room room = getContainingRoom(currentPosition);
				if(null==room) {
					room=scopeRoomUponEntry2();
					addRoom(room);
				}
				
				List<Object> exit_exitDir = room.getRandomLeastVisitedExit(currentPosition);
				int[] exit = (int[])exit_exitDir.get(0);
				CardinalDirection exitDir = (CardinalDirection)exit_exitDir.get(1);
				
				int[] diff = MazeMath.subArrays(exit, currentPosition);
				
				System.out.printf("currentPosition: %s, exit: %s\n",Arrays.toString(currentPosition),Arrays.toString(exit));
				
				try {
					CardinalDirection cd0 = CardinalDirection.getDirection((int)Math.signum(diff[0]), 0);
					moveRobotE(cd0,Math.abs(diff[0]));
					
				} catch(IllegalArgumentException e) {}
				
				try{
					CardinalDirection cd1 = CardinalDirection.getDirection(0, (int)Math.signum(diff[1]));
					moveRobotE(cd1,Math.abs(diff[1]));
				} catch(IllegalArgumentException e) {}
				
				assert Arrays.equals(exit, currentPosition);
				incrementVisitCount(currentPosition);
				
				moveRobotE(exitDir,1);
			}
			cd=directionOfExit();
		}
		
		System.out.printf("exit %s found in direction %s from %s",Arrays.toString(distance.getExitPosition()),cd,Arrays.toString(currentPosition));
		
		// face towards, walk to and through the exit
		faceRobot(cd);
		while(true) {
			moveRobotE(1);
			
			try{
				robot.getCurrentPosition();
				incrementVisitCount(currentPosition);
			}
			catch(Exception e) {break;}
		}
		return;
	}
	

}


class Room {
	
	//private int[][] corners;
	//private List<int[]> cells;
	private int minX, maxX, minY, maxY;
	private ArrayList<int[]> exits;
	//private List<Integer>exitCounts;
	private ArrayList<CardinalDirection> exitDirections;
	private int[][] visitCounts;
	
	public Room() {
		init();
	}
	
	private int getVisitCount(int[] xy) {
		return visitCounts[xy[0]][xy[1]];
	}
	
	public List<Object> getRandomLeastVisitedExit(int[] currentPosition) {
		int minVisit = exits.stream().map(this::getVisitCount).min(Integer::compare).get();
		
		List<Integer> range = IntStream.range(0, exits.size()).boxed().collect(Collectors.toList());
		System.out.println("range: "+range);
		
		List<Integer> minVisitedIndices =
			range
			.stream()
			.filter(i -> getVisitCount(exits.get(i))==minVisit && !Arrays.equals(exits.get(i), currentPosition))
			.collect(Collectors.toList());
		
		int rand = SingleRandom.getRandom().nextIntWithinInterval(0,minVisitedIndices.size()-1);
		
		int[] exit = exits.get(rand);
		CardinalDirection exitDir = exitDirections.get(rand);
		
		
		return Arrays.asList(exit,exitDir);
	}
	
	public void setVisitCounts(int[][] visitCounts) {
		this.visitCounts=visitCounts;
	}

	public void setCorners(int[] corner1, int[] corner2) {
		orderCorners(corner1,corner2);
	}

	public Room(int[][] corners){
		orderCorners(corners);
		init();
		//this.corners = corners;
		//cells = new ArrayList<int[]>((maxX-minX)*(maxY-minY));
		//addCellsFromCorners();
	}
	
	private void init() {
		exits = new ArrayList<int[]>();
		exitDirections = new ArrayList<CardinalDirection>();
		//exitCounts = new ArrayList<Integer>();//HashMap<Integer,Integer>();
	}
	
	public ArrayList<int[]> getExitsView() {
		return new ArrayList<int[]>(exits);
	}
	
	public boolean addExit(int[] xy, CardinalDirection cd) {
		if(!contains(xy)) {
			return false;
		}
		exits.add(xy);
		exitDirections.add(cd);
		//exitCounts.add(visitCounts[xy[0]][xy[1]]);
		return true;
	}
	
	public void addExit(int x, int y, CardinalDirection cd) {
		//the index that will link to the new exit cell to be added
		//exitCounts.put(exits.size(), 0);
		
		// from the current cell,
		// decide where the wall along the specified direction lies
		switch(cd) {
			case East:
				exits.add(new int[] {maxX,y});
				break;
			case West:
				exits.add(new int[] {minX,y});
				break;
			case North:
				exits.add(new int[] {x,minY});
				break;
			case South:
				exits.add(new int[] {x,maxY});
				break;
			default: break;
		}
	}
	
	public List<int[]> getDirectionOutOfExit(int[] exit) {
		List<int[]> exitDirs = new ArrayList<int[]>(1);
		
		for(CardinalDirection cd: CardinalDirection.values()) {
			int[] newCell=MazeMath.addArrays(exit, cd.getDirection());
			if(!contains(newCell)) {
				exitDirs.add(newCell);
			}
		}
		
		return exitDirs;
	}
	
	private void orderCorners(int[] corner1, int[] corner2) {
		minX = Integer.min(corner1[0], corner2[0]);
		maxX = Integer.max(corner1[0], corner2[0]);
		minY = Integer.min(corner1[1], corner2[1]);
		maxY = Integer.max(corner1[1], corner2[1]);
	}
	
	public void orderCorners(int[][] corners) {
		minX = Integer.min(corners[0][0], corners[1][0]);
		maxX = Integer.max(corners[0][0], corners[1][0]);
		minY = Integer.min(corners[0][1], corners[1][1]);
		maxY = Integer.max(corners[0][1], corners[1][1]);
	}
	
	public boolean contains(int[] cell) {
		return (cell[0]<= maxX && cell[0]>=minX && cell[1]<=maxY && cell[1]>=minY);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxX;
		result = prime * result + maxY;
		result = prime * result + minX;
		result = prime * result + minY;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Room other = (Room) obj;
		if (maxX != other.maxX)
			return false;
		if (maxY != other.maxY)
			return false;
		if (minX != other.minX)
			return false;
		if (minY != other.minY)
			return false;
		return true;
	}
	
	/*
	public void addCellsFromCorners() {
		orderCornerCells();
		
		for(int x = minX; x<maxX+1; x++) {
			for(int y = minY; y<maxY+1; y++) {
				
			}
		}
	}*/
	
	
	
}