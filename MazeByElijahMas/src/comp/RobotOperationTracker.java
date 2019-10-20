package comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import generation.CardinalDirection;
import generation.Distance;
import generation.Floorplan;
import generation.Maze;
import gui.Robot;
import gui.RobotDriver;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * This class tracks a series of operations to be performed
 * by a {@link Robot}, and can instruct the robot
 * to perform those operations.
 * 
 * Meant for easy manipulation of robot; accepts
 * a {@link Direction} value to its {@code add} method
 * and from this automatically tracks what operations
 * must be performed by the robot to move in that direction.
 * 
 * Some of this code may prove
 * useful to refactor into a {@link RobotDriver}.
 * 
 * @author Elijah Mas
 *
 */
public class RobotOperationTracker {
	/**
	 * robot that the tracker will operate on
	 */
	private Robot robot;
	
	/**
	 * series of {@link RobotOperation} instances that will steer a robot
	 */
	private ArrayList<RobotOperation> operations;
	
	/**
	 * reference to the current {@link RobotMove} instance handled by the tracker;
	 * can be null if the most recent operation was a {@link RobotRotation}.
	 */
	private RobotMove currentMove;
	
	/**
	 * maze operated on by the tracker and robot
	 */
	private Maze maze;
	
	/**
	 * the maze's {@link generation.Floorplan floorplan}
	 */
	private Floorplan floorplan;
	
	
	/**
	 * the maze's {@link generation.Distance distance}
	 */
	private Distance distance;
	
	
	//private int[][] dists;
	
	/**
	 * current (x,y) position of the tracker
	 */
	private int[] currentPosition;
	
	/**
	 * the (x,y) direction of the next move the tracker will store
	 */
	private int[] deltaNext;
	
	/**
	 * the {@link CardinalDirection} of the next move the tracker will store
	 */
	private CardinalDirection cdNext;
	
	/**
	 * current {@link CardinalDirection} position of the tracker
	 */
	private CardinalDirection currentDirection;
	
	/**
	 * tracks the cumulative distance traveled over all operations
	 */
	private int totalDistance;
	
	/**
	 * standardized direction that the tracker starts in;
	 * established so that it can instruct a robot to turn to this direction
	 * before executing other commands
	 */
	public static final CardinalDirection STARTING_CARDINAL_DIRECTION=CardinalDirection.North;
	
	/**
	 * Provide a {@link #maze} for the tracker to store
	 * @param maze a maze
	 */
	public RobotOperationTracker(Maze maze){
		init(maze);
	}
	
	/*
	 * Provide a {@link #maze} and {@link #robot} for the tracker to store
	 * @param maze a maze
	 * @param robot a robot
	public RobotOperationTracker(Maze maze, Robot robot){
		setRobot(robot);
		init(maze);
	}
	 **/
	
	/*
	 * Set the tracker's robot field.
	 * @param robot a {@link Robot} instance
	public void setRobot(Robot robot) {
		this.robot=robot;
	}
	 **/
	
	/*public void clearRobot() {
		robot=null;
	}*/
	
	public static List<RobotOperation> getOperationsFrom(Maze maze) {
		RobotOperationTracker t = new RobotOperationTracker(maze);
		t.buildExitPath();
		return t.getOperations();
	}
	
	/**
	 * Provide the tracker with a maze and set certain members as fields
	 * @param maze a maze
	 */
	private void init(Maze maze){
		this.maze=maze;
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		//dists=distance.getAllDistanceValues();
		operations=new ArrayList<RobotOperation>(2*distance.getMaxDistance());
		currentPosition=maze.getStartingPosition(); // where the robot will start
		currentDirection=STARTING_CARDINAL_DIRECTION; // arbitrary
	}
	
	public ArrayList<RobotOperation> getOperations(){
		return operations;
	}
	
	void incrementTotalDistance() {
		totalDistance++;
	}
	
	/**
	 * If the most recent operation of {@link #operations} is a {@link RobotMove} instance,
	 * return it; otherwise, create a new {@link RobotMove} instance and add it to
	 * {@link #operations}, then return it
	 * @return a {@link RobotMove} instance which is the current move being handled by the tracker
	 */
	private RobotMove getCurrentMove() {
		if(null==currentMove) {
			currentMove = new RobotMove();
			operations.add(currentMove);
		}
		return currentMove;
	}
	
	/**
	 * <p>Establish a new operation in the tracker.
	 * 
	 * The operation can be either a move ({@link RobotMove}) or rotation ({@link RobotRotation}).</p>
	 * 
	 * <p>Provide a direction {@code d} where the tracker should go next.
	 * 
	 * If {@code d} is forward, then the tracker moves in the current direction,
	 * which is encoded by incrementing the distance of the current move operation
	 * in {@link #operations}.
	 * 
	 * If {@code d} is <b>not</b> forward, then the tracker must rotate to {@code d} before moving,
	 * which is encoded by adding a new rotation operation to {@link #operations}
	 * and then adding a new move operation with distance set to {@code 1}.
	 * </p>
	 * @param d
	 */
	public void add(Direction d) {
		switch(d) {
			case FORWARD:
				// keep moving in current direction
				// if currentMove is null, a Move is added to operations
				// and then returned by getCurrentMove
				getCurrentMove().incrementDistance(this);
				
				//we should have a move operation now
				assert (operations.get(operations.size()-1) instanceof RobotMove);
				
				return;
			default:
				// rotation added --> most recent move operation cannot be extended further
				// reset to null and add a new move operation
				currentMove=null;
				Turn t = MazeMath.directionToTurn(d);
				
				// if we are constantly moving closer to the exit,
				// we should never have to backtrack
				// the exception is in the very beginning, when the robot may be facing the wrong way
				if (operations.size()!=0){
					assert d!=Direction.BACKWARD;
					assert t!=Turn.AROUND;
				}
				
				addRotation(t);
				break;
		}
		
		// whatever direction we are now facing, start moving in that direction
		// by above logic, if currentMove is null (case of rotation),
		// getCurrentMove now refers to a RobotMove with distance=0,
		// which should be incremented to 1
		getCurrentMove().incrementDistance(this);
	}
	
	/**
	 * Add a new rotation for a certain {@link Turn} to {@link #operations}
	 * @param turn a value of {@link Turn}
	 */
	private void addRotation(Turn turn) {
		operations.add(new RobotRotation(turn));
	}
	
	/*
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	private int[] getRobotPosition() {
		try {return robot.getCurrentPosition();}
		catch(Exception e) {return null;}
	}
	 **/
	
	/**
	 * Calculate which cell the tracker should move to next,
	 * and get the {@link Direction} value that corresponds
	 * to moving to this cell from the current cell.
	 * 
	 * @return {@link Direction} value to move to the next cell
	 */
	private Direction getDirectionOfNextMove() {
		// the cell to move to is that which is closer to the exit
		int[] next = MazeMath.getNeighborCloserToExit(currentPosition, maze);
		
		// assert that the calculation of next cell was done correctly
		assert 1==maze.getDistanceToExit(currentPosition[0], currentPosition[1])-maze.getDistanceToExit(next[0], next[1]):
			"cdNext error: new distance is "+maze.getDistanceToExit(next[0], next[1])+
			"while previous distance is "+maze.getDistanceToExit(currentPosition[0], currentPosition[1]);
		
		// the (x,y) difference between the new cell and the current cell
		deltaNext = MazeMath.subArrays(next, currentPosition);
		
		// cells should be adjacent
		assert 1==Math.abs(deltaNext[0] + deltaNext[1]):
			"deltaNext error: "+Arrays.toString(next)+", "+Arrays.toString(currentPosition);
		
		// get the absolute direction of the move
		cdNext = CardinalDirection.getDirection(deltaNext[0], deltaNext[1]);
		
		// convert to relative direction and return
		return MazeMath.convertDirs(cdNext, currentDirection);
	}
	
	private static boolean exitAvailable(int[] xy, Distance distance){
		int
			width = distance.getAllDistanceValues().length,
			height = distance.getAllDistanceValues()[0].length;
		
		return ( 1==distance.getDistanceValue(xy)
				&&
				(
					0==xy[0] ||
					width-1==xy[0] ||
					0==xy[1] ||
					height-1==xy[1]
				)
		);
	}
	
	/**
	 * Construct a series of {@link RobotOperation} operations which,
	 * when applied to a robot, will move it from the starting cell
	 * of a maze to and out of the exit.
	 */
	public void buildExitPath() {
		int x = currentPosition[0], y = currentPosition[1];
		Direction d;
		
		// if we repeatedly move from the current cell to the cell
		// which is 1 distance unit closer to the exit,
		// we are guaranteed to eventually reach the exit cell,
		// whose distance to exit is 1
		
		while(!exitAvailable(currentPosition, distance))
		//while(!floorplan.isExitPosition(x, y))
		{
			// get the relative direction of the next move
			d=getDirectionOfNextMove();
			
			// add this operation; the add method automatically determines
			// if a rotation is required
			add(d);
			
			currentPosition = MazeMath.addArrays(currentPosition,deltaNext);
			
			// sanity check that the logic of deltaNext was implemented properly
			// deltaNext was updated in getDirectionOfNextMove
			assert testManhattanDistanceIsOne(x,y,currentPosition);
			
			// update current direction
			// cdNext was updated in getDirectionOfNextMove
			currentDirection = cdNext;
			
			// don't forget to update these so that the while-loop progresses
			x=currentPosition[0];
			y=currentPosition[1];
		}
		
		// make sure currentPosition reference is updated
		assert floorplan.isExitPosition(currentPosition[0], currentPosition[1]) : "tracker is not at exit position";
		
		// get the relative direction required to move out of the maze
		// add the operation corresponding to this direction
		CardinalDirection cd = MazeMath.getCardinalDirectionOfMazeExit(maze);
		d=MazeMath.convertDirs(cd, currentDirection);
		add(d);
		
		// this list will no longer be updated, so release unneeded memory
		operations.trimToSize();
		
		// the exit distance from start specified by the maze
		// should match the distance we have traveled
		assert distance.getMaxDistance()==totalDistance : 
			String.format("Total distance %d != maze max distance % d", totalDistance, distance.getMaxDistance());
		
		/*
		 * moves should alternate with rotations by design
		 * if two like operations are consecutive, there is an error:
		 * they should have been merged into one single command
		 * e.g. (rotate left + rotate left) --> rotate around,
		 *      (move 1 + move 3) --> move 4
		 */
		boolean alternatingCheck = true;
		for(int i=0; i<operations.size()-1; i++) {
			if(operations.get(i).getClass() == operations.get(i+1).getClass()) {
				alternatingCheck = false;
				break;
			}
		}
		assert alternatingCheck;
		
		// the last operation should always be the move that take the robot out of the maze
		assert (operations.get(operations.size()-1) instanceof RobotMove);
		
	}
	
	/**
	 * Test that the Manhattan distance between two coordinates is {@code 1}.
	 * Used in {@link #buildExitPath()}.
	 * 
	 * @param x x-value of first coordinate
	 * @param y y-value of first coordinate
	 * @param currentPosition second coordinate
	 * @return true if Manhattan distance is 1, false otherwise (prints error message)
	 */
	private boolean testManhattanDistanceIsOne(int x, int y, int[] currentPosition) {
		int d=MazeMath.manhattanDistance(x, y, currentPosition);
		if(1==d) return true;
		else
			System.out.printf(
				"a single move should have manhattan distance 1, but we have: "
				+ "[%d, %d] <--> %s\n", x, y, Arrays.toString(currentPosition));
		return false;
	}
	
	/*
	 * Return a convenient string representation of the series of {@link #operations}
	 * established by the tracker
	 * @return condensed string representing {@link #operations}
	public String operationsString() {
		String s="";
		String space=" - ";
		for(RobotOperation o: operations) {
			if(o instanceof RobotMove) s+=((RobotMove) o).getDistance()+space;
			else if(o instanceof RobotRotation) s+=shortTurnName(((RobotRotation) o).getTurn())+space;
			else s+="?"+space;
		}
		return s;
	}
	 **/
	
	/*
	 * Condense a {@link Turn} name to one letter.
	 * Could use reflection to do this, but no reason for such complication.
	 * 
	 * Used in {@link #operationsString()}
	 * 
	 * @param t a {@link Turn} value
	 * @return the first letter of the value's name
	private String shortTurnName(Turn t) {
		switch(t) {
			case LEFT: return "L";
			case RIGHT: return "R";
			case AROUND: return "A";
			}
		return null;
	}
	 **/
	
}