package gui;

import static org.junit.Assert.*;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import generation.Floorplan;
import generation.Maze;
import generation.MazeBuilder;
import generation.Order;
import generation.OrderStub;
import generation.Order.Builder;
import gui.Constants.UserInput;
import gui.Robot.Direction;
import gui.Robot.Turn;

import comp.ExtendedList;

/**
 * 
 * @author Elijah Mas
 *
 */
public class BasicRobotTest {
	private BasicRobot robot;
	//private Robot robot2;
	private Controller control;
	private Maze maze;
	private Floorplan floorplan;
	private Distance distance;
	private RobotOperationTracker tracker;
	private ArrayList<RobotOperation> operations;
	
	/**
	 * Build a new maze at a given level to be used for testing
	 * and establish the controller that will operate on it.
	 * 
	 * @param level level of the maze to be generated
	 */
	private void instantiateApplication(int level) {
		// we don't want energy running out when not commanded explicitly
		// but if we make this value too large,
		// we will encounter precision-related errors
		BasicRobot.initialEnergyLevel=100000;
		
		BasicRobot.VERBOSE=false;
		control=getController(false, false, level);
		robot=(BasicRobot)control.getRobot();
		
		maze=control.getMazeConfiguration();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
	}
	
	/**
	 * Build a new maze at a given level to be used for testing
	 * and establish the controller that will operate on it.
	 * 
	 * Also create a {@link RobotOperationTracker} to compute
	 * a path out of this maze for testing purposes.
	 * 
	 * @param level level of the maze to be generated
	 */
	public void buildNewMaze(int level) {
		instantiateApplication(level);
		assertTrue(null!=maze);
		
		tracker = new RobotOperationTracker(maze);
		tracker.buildExitPath();
		operations=tracker.getOperations();
	}
	
	/**
	 * Cleanup method: sets values to null so that
	 * the test class has no memory of data from the previous maze.
	 */
	@After
	public void resetFields() {
		System.out.println("resetting fields");
		resetState();
		maze=null;
		floorplan=null;
		distance=null;
		operations=null;
	}
	
	/**
	 * Set the {@link #control} and {@link #robot} fields to null,
	 * allowing a new controller and robot to be established.
	 * 
	 * Used to allow a series of tests to be run on a maze
	 * with a fresh controller/robot pair for each test.
	 */
	public void resetState() {
		control=null;
		robot=null;
	}
	
	/**
	 * Using the current {@link #maze} field, set a controller
	 * to start a playing state from this maze.
	 */
	public void setController() {
		setController(maze);
	}
	
	/**
	 * Using an already established maze, set a controller
	 * to start a playing state from this maze.
	 * 
	 * @param maze the maze of interest
	 */
	public void setController(Maze maze) {
		resetState();
		control = new Controller(true);
		control.turnOffGraphics();
		control.switchFromGeneratingToPlaying(maze);
		robot = (BasicRobot)control.getRobot();
	}
	
	@Test
	public void testMaze() {
		buildNewMaze(4);
		fullMazeWalk();
		setController();
		fullMazeWalk_LeaveByJump();
		setController();
		for (Direction d: Direction.values()) {
			testWallCrash(Direction.FORWARD);
			setController();
		}
		resetFields();
	}
	
	/**
	 * Tell whether the Controller is in the winning state.
	 * @return whether controller is in winning state
	 */
	private boolean isStateWinning() {
		return (control.currentState instanceof StateWinning);
	}
	
	/**
	 * Walk up to a wall and then walk into it.
	 * Assert that the robot stops with the appropriate failure message,
	 * and that the game ends.
	 * @param d
	 */
	public void testWallCrash(Direction d) {
		// move right in front of the wall
		approachWall(d);
		
		// walk into the wall
		robot.move(1, true);
		
		// verify that robot has stopped from crash and game has ended
		assertTrue(robot.hasStopped());
		assertEquals(control.getRobotFailureMessage(),BasicRobot.badMoveMessage);
		assertTrue(isStateWinning());
	}
	
	/**
	 * Walk right up to a wall without hitting it.
	 * @param d which {@link Direction} to approach
	 */
	private void approachWall(Direction d) {
		int wallDistance = robot.distanceToObstacle(d);
		faceDirection(d,robot);
		robot.move(wallDistance, true);
		assertEquals(0, robot.distanceToObstacle(Direction.FORWARD));
	}
	
	/**
	 * Rotate the robot until it is facing the input direction.
	 * @param d a {@link Direction} value
	 * @param robot the robot to rotate
	 */
	public static void faceDirection(Direction d, Robot robot) {
		switch(d) {
			case FORWARD: return;
			case LEFT: robot.rotate(Turn.LEFT); return;
			case RIGHT: robot.rotate(Turn.RIGHT); return;
			case BACKWARD: robot.rotate(Turn.AROUND); return;
		}
	}
	
	/**
	 * Walk the robot to the exit cell and then leave the maze.
	 */
	public void fullMazeWalk() {
		// get to the exit
		walkToExit();
		// leave maze
		moveOutExit();
	}
	
	/**
	 * Walk the robot to the exit cell and then leave the maze
	 * by attempting a jump.
	 */
	public void fullMazeWalk_LeaveByJump() {
		// get to the exit
		walkToExit();
		// leave maze by attempting jump
		moveOutExitByJump();
	}
	
	/**
	 * Using the {@link #operations} list provided by {@link #tracker},
	 * walk the robot to the exit cell of the maze.
	 * 
	 * Along the way tests are performed to ensure that the robot
	 * and the Controller concur on the current position and direction.
	 * 
	 * Also test that at the end of this sequence, the robot
	 * is facing the exit, which is part of the design specification
	 * of {@link RobotOperationTracker}.
	 */
	private void walkToExit() {
		// robot and controller should always share orientation
		assertEqualOrientation();
		
		// tracker and robot must be in alignment
		while(robot.getCurrentDirection()!=RobotOperationTracker.STARTING_CARDINAL_DIRECTION) {
			robot.rotate(Turn.LEFT);
			assertEqualOrientation();
		}
		
		//System.out.printf("Oriented in direction %s at position %s: starting operation sequence\n",
		//		RobotOperationTracker.STARTING_CARDINAL_DIRECTION, Arrays.toString(getRobotPosition()));
		
		for(int i=0; i<operations.size()-1; i++) {
			RobotOperation op = operations.get(i);
			op.operateRobot(robot);
			assertEqualOrientation();
		}
		
		// at this stage the robot's only remaining operation is to move forward through the exit
		// it does not have to be at exit, but it has to be able to see the exit
		assertTrue(robot.canSeeThroughTheExitIntoEternity(Direction.FORWARD));
	}
	
	/**
	 * Assert that after leaving a maze, the robot recognizes it is outside,
	 * and the Controller has switched to the winning state.
	 * 
	 * Called when the robot has left a maze.
	 */
	private void assertOutsideMaze() {
		// has the application responded as expected?
		assertTrue(isStateWinning());
		// robot should be outside maze
		assertEquals(null,getRobotPosition());
	}
	
	/**
	 * From the exit cell, exit the maze by attempting a jump.
	 * 
	 * The robot is programmed to substitute a move operation for a jump
	 * if it recognizes that there is no wall directly in front of it.
	 * 
	 * So this should perform a move operation under the hood,
	 * thus successfully exiting the maze.
	 */
	private void moveOutExitByJump() {	
		boolean successfulExit;
		
		try {
			robot.jump();
			//should be outside
			assertOutsideMaze();
			successfulExit=true;
		} catch (Exception e) {
			successfulExit=false;
		}
		assertTrue(successfulExit);
	}
	
	/**
	 * From the exit cell, leave the maze by a move operation,
	 * and assert that the robot has indeed left the maze.
	 */
	private void moveOutExit() {	
		// exit the maze
		operations.get(operations.size()-1).operateRobot(robot);
		//should be outside
		assertOutsideMaze();
	}
	
	private void testPerfectMaze() {
		
	}
	/**
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	 */
	private int[] getRobotPosition() {
		try {return robot.getCurrentPosition();}
		catch(Exception e) {return null;}
	}
	
	/**
	 * Assert that the robot and controller agree on position and direction.
	 */
	private void assertEqualOrientation() {
		assertTrue(Arrays.equals(getRobotPosition(),control.getCurrentPosition()));
		assertEquals(robot.getCurrentDirection(),control.getCurrentDirection());
	}
	
	/**
	 * Sets things in order for a maze to be generated.
	 * OrderStub, Controller, and MazeBuilder instances receive
	 * the parameters required to initialize the maze.
	 * Ensures that the maze is finished generating before tests are performed.
	 * 
	 * @param perfect boolean:{maze is perfect}
	 * @param deterministic boolean:{maze is generated deterministically}
	 * @param level level of the maze
	 * @return the generated Maze instance
	 */
	public Controller getController(boolean perfect, boolean deterministic, int level){
		Controller controller = new Controller(true);
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(perfect);
		order.start(controller, null);
		
		MazeBuilder builder = new MazeBuilder(deterministic);
		//deterministicTest(deterministic);
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
			System.out.println("100");
			
			//this.maze=order.getMaze();
			//this.control=controller;
			//this.robot=(BasicRobot)control.getRobot();
			//this.floorplan=maze.getFloorplan();
			//this.distance=maze.getMazedists();
			
			controller.switchFromGeneratingToPlaying(order.getMaze());
			
			return controller;
			
		} catch (InterruptedException e) {
			System.out.println("\n--Intteruption--");
			e.printStackTrace();
			return null;
		}
	}
	
	/*public Controller getController(boolean perfect, boolean deterministic, int level, Maze maze) {
		Controller controller = new Controller(true);
		controller.turnOffGraphics();
		controller.switchFromGeneratingToPlaying(maze);
		
		return controller;
		
	}*/
	
	/**
	 * Test the direction conversion methods from {@link comp.MazeMath}.
	 * 
	 * TODO move this into a new test class designed specifically for {@link comp.MazeMath}
	 */
	@Test
	public void mathTest() {
		Direction forward=Direction.FORWARD, backward=Direction.BACKWARD, left=Direction.LEFT, right=Direction.RIGHT;
		CardinalDirection north=CardinalDirection.North, south=CardinalDirection.South, east=CardinalDirection.East, west=CardinalDirection.West;
		assertEquals(MazeMath.DirectionToCardinalDirection(forward,east),east);
		assertEquals(MazeMath.DirectionToCardinalDirection(forward,north),north);
		assertEquals(MazeMath.DirectionToCardinalDirection(forward,south),south);
		assertEquals(MazeMath.DirectionToCardinalDirection(forward,west),west);
		assertEquals(MazeMath.DirectionToCardinalDirection(backward,north),south);
		assertEquals(MazeMath.DirectionToCardinalDirection(backward,south),north);
		assertEquals(MazeMath.DirectionToCardinalDirection(backward,east),west);
		assertEquals(MazeMath.DirectionToCardinalDirection(backward,west),east);
		assertEquals(MazeMath.DirectionToCardinalDirection(left,north),east);
		assertEquals(MazeMath.DirectionToCardinalDirection(left,east),south);
		assertEquals(MazeMath.DirectionToCardinalDirection(left,south),west);
		assertEquals(MazeMath.DirectionToCardinalDirection(left,west),north);
		assertEquals(MazeMath.DirectionToCardinalDirection(right,east),north);
		assertEquals(MazeMath.DirectionToCardinalDirection(right,south),east);
		assertEquals(MazeMath.DirectionToCardinalDirection(right,west),south);
		assertEquals(MazeMath.DirectionToCardinalDirection(right,north),west);
		
		assertEquals(MazeMath.CardinalDirectionToDirection(east,east),forward);
		assertEquals(MazeMath.CardinalDirectionToDirection(north,north),forward);
		assertEquals(MazeMath.CardinalDirectionToDirection(west,west),forward);
		assertEquals(MazeMath.CardinalDirectionToDirection(south,south),forward);
		assertEquals(MazeMath.CardinalDirectionToDirection(south,north),backward);
		assertEquals(MazeMath.CardinalDirectionToDirection(north,south),backward);
		assertEquals(MazeMath.CardinalDirectionToDirection(west,east),backward);
		assertEquals(MazeMath.CardinalDirectionToDirection(east,west),backward);
		assertEquals(MazeMath.CardinalDirectionToDirection(east,north),left);
		assertEquals(MazeMath.CardinalDirectionToDirection(south,east),left);
		assertEquals(MazeMath.CardinalDirectionToDirection(west,south),left);
		assertEquals(MazeMath.CardinalDirectionToDirection(north,west),left);
		assertEquals(MazeMath.CardinalDirectionToDirection(north,east),right);
		assertEquals(MazeMath.CardinalDirectionToDirection(east,south),right);
		assertEquals(MazeMath.CardinalDirectionToDirection(south,west),right);
		assertEquals(MazeMath.CardinalDirectionToDirection(west,north),right);
		
	}
}

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
 * useful to refactor into a RobotDriver.
 * 
 * @author Elijah Mas
 *
 */
class RobotOperationTracker {
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
	
	/**
	 * Provide a {@link #maze} and {@link #robot} for the tracker to store
	 * @param maze a maze
	 * @param robot a robot
	 */
	public RobotOperationTracker(Maze maze, Robot robot){
		this.robot=robot;
		init(maze);
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
				assert t!=Turn.AROUND;
				
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
	
	/**
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	 */
	private int[] getRobotPosition() {
		try {return robot.getCurrentPosition();}
		catch(Exception e) {return null;}
	}
	
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
		return MazeMath.CardinalDirectionToDirection(cdNext, currentDirection);
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
		while(!floorplan.isExitPosition(x, y)) {
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
		// add the operation corresonding to this direction
		CardinalDirection cd = MazeMath.getCardinalDirectionOfMazeExit(maze);
		d=MazeMath.CardinalDirectionToDirection(cd, currentDirection);
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
		else System.out.printf(
				"a single move should have manhattan distance 1, but we have: "
				+ "[%d, %d] <--> %s\n", x, y, Arrays.toString(currentPosition));
		return false;
	}
	
	/*
	 * Return a convenient string representation of the series of {@link #operations}
	 * established by the tracker
	 * @return condensed string representing {@link #operations}
	private String operationsString() {
		String s="";
		String space=" - ";
		for(RobotOperation o: operations) {
			if(o instanceof RobotMove) s+=((RobotMove) o).getDistance()+space;
			else if(o instanceof RobotRotation) s+=shortTurnName(((RobotRotation) o).getTurn())+space;
			else s+="?"+space;
		}
		return s;
	}
	 */
	
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
	 */
	
}

/**
 * This class is extended by classes that operate on a robot.
 * Extensions are utilized in {@link RobotOperationTracker} to
 * construct paths through mazes.
 * 
 * @author Elijah Mas
 *
 */
abstract class RobotOperation{
	
	abstract void operateRobot(Robot robot);
	
}

/**
 * Provides a mechanism to move a robot by a certain distance.
 * Also interacts with {@link RobotOperationTracker} to keep track
 * of the distances traveled through mazes.
 * 
 * @author Elijah Mas
 *
 */
class RobotMove extends RobotOperation{
	/**
	 * distance to move an operated robot
	 */
	private int distance=0;
	
	/**
	 * Indicate that the current move operation proceeds
	 * over another cell without changing direction.
	 * @param tracker a {@link RobotOperationTracker} in which this is embedded
	 */
	public void incrementDistance(RobotOperationTracker tracker) {
		distance++;
		tracker.incrementTotalDistance();
	}
	
	public int getDistance() {
		return distance;
	}
	
	public RobotMove() {}
	
	public RobotMove(int distance) {
		this.distance=distance;
	}
	
	/**
	 * Move a robot by the internally stored {@link #distance}
	 * @param robot a robot to operate on
	 */
	public void operateRobot(Robot robot) {
		robot.move(distance, false);
	}
	
	@Override
	public String toString() {
		return String.format("Move(%d)",distance);
	}
}

/**
 * Provides a mechanism to move a robot by a certain distance.
 * Requires no knowledge of the {@link RobotOperationTracker}
 * in which it is embedded.
 * 
 * @author Elijah Mas
 *
 */
class RobotRotation extends RobotOperation{
	/**
	 * {@link Turn} to apply to rotate an operated robot
	 */
	private Turn turn;
	
	public RobotRotation(Turn turn) {
		this.turn=turn;
	}
	
	public Turn getTurn() {
		return turn;
	}
	
	/**
	 * Rotate a robot by the internally stored {@link #turn}
	 * @param robot a robot to operate on
	 */
	public void operateRobot(Robot robot) {
		if(null!=turn) robot.rotate(turn);
	}
	
	@Override
	public String toString() {
		return String.format("Rotation<%s>",turn);
	}
}