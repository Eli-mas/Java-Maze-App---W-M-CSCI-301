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

public class BasicRobotTest {
	private BasicRobot robot;
	//private Robot robot2;
	private Controller control;
	private Maze maze;
	private Floorplan floorplan;
	private Distance distance;
	private RobotOperationTracker tracker;
	
	private void instantiateApplication(int level) {
		// we don't want energy running out when not commanded explicitly
		// but if we make this value too large,
		// we will encounter precision-related errors
		BasicRobot.initialEnergyLevel=100000;
		
		control=getController(true, true, level);
		robot=(BasicRobot)control.getRobot();
		robot.VERBOSE=false;
		
		maze=control.getMazeConfiguration();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
	}
	
	@Test
	public void samples() {
		for(int i=0; i<10; i++) {
			System.out.println("sample "+i);
			sample();
			resetFields();
		}
	}
	
	public void sample() {
		buildNewMaze(0);
		
		int[] exit = distance.getExitPosition();
		boolean exitFound=false;
		for(CardinalDirection cd: CardinalDirection.values()) {
			if(floorplan.hasNoWall(exit[0], exit[1], cd)) {
				//System.out.printf("--> --> --> Maze has exit at %s in direction %s: new position is %s\n",
				//		Arrays.toString(exit),cd,Arrays.toString(MazeMath.addArrays(exit, cd.getDirection())));
				exitFound=true;
				break;
			}
		}
		assertTrue(exitFound);
		
		assertEqualOrientation();
		
		// tracker and robot must be in alignment
		while(robot.getCurrentDirection()!=RobotOperationTracker.STARTING_CARDINAL_DIRECTION) {
			robot.rotate(Turn.LEFT);
			assertEqualOrientation();
		}
		
		//System.out.printf("Oriented in direction %s at position %s: starting operation sequence\n",
		//		RobotOperationTracker.STARTING_CARDINAL_DIRECTION, Arrays.toString(getRobotPosition()));
		
		ArrayList<RobotOperation> operations=tracker.getOperations();
		for(int i=0; i<operations.size(); i++) {
			operations.get(i).operateRobot(robot);
			if(null!=getRobotPosition()) assertEqualOrientation();
		}
		
		assertTrue(control.currentState instanceof StateWinning);
		
		/*
		int[] robotPosition=getRobotPosition();
		assert floorplan.isExitPosition(robotPosition[0], robotPosition[1]);
		System.out.println("robot distances: "+robot.getDistanceString());
		System.out.printf("robot current direction: %s\n",robot.getCurrentDirection());
		assert robot.canSeeThroughTheExitIntoEternity(Direction.FORWARD);
		*/
	}
	
	private void testPerfectMaze() {
		
	}
	public void buildNewMaze(int level) {
		instantiateApplication(level);
		assertTrue(null!=maze);
		
		tracker = new RobotOperationTracker(maze);
		tracker.buildExitPath();
		
		//distance=
		//System.out.println("forward distance to wall: "+robot.distanceToObstacle(Direction.FORWARD));
		//for()
	}
	
	/**
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	 */
	private int[] getRobotPosition() {
		try {return robot.getCurrentPosition();}
		catch(Exception e) {return null;}
	}
	
	private void assertEqualOrientation() {
		assertTrue(Arrays.equals(getRobotPosition(),control.getCurrentPosition()));
		assertEquals(robot.getCurrentDirection(),control.getCurrentDirection());
	}
	
	/**
	 * Cleanup method: sets values to null so that
	 * the test class has no memory of data from previous tests.
	 */
	@After
	public void resetFields() {
		System.out.println("resetting fields");
		maze=null;
		control=null;
		robot=null;
		floorplan=null;
		distance=null;
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
	
	public Controller getController(boolean perfect, boolean deterministic, int level, Maze maze) {
		Controller controller = new Controller(true);
		controller.turnOffGraphics();
		controller.switchFromGeneratingToPlaying(maze);
		
		return controller;
		
	}
	
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
	private Robot robot;
	private ArrayList<RobotOperation> operations;
	private RobotMove currentMove;
	private Maze maze;
	private Floorplan floorplan;
	private Distance distance;
	private int[][] dists;
	private int[] currentPosition;
	private int[] deltaNext;
	private CardinalDirection cdNext;
	private CardinalDirection currentDirection;
	private int totalDistance;
	
	public static final CardinalDirection STARTING_CARDINAL_DIRECTION=CardinalDirection.North;
	
	public RobotOperationTracker(Maze maze){
		init(maze);
	}
	
	public RobotOperationTracker(Maze maze, Robot robot){
		this.robot=robot;
		init(maze);
	}
	
	public ArrayList<RobotOperation> getOperations(){
		return operations;
	}
	
	void incrementTotalDistance() {
		totalDistance++;
	}
	
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
	
	private void init(Maze maze){
		this.maze=maze;
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		dists=distance.getAllDistanceValues();
		operations=new ArrayList<RobotOperation>(2*distance.getMaxDistance());
		currentPosition=maze.getStartingPosition(); // where the robot will start
		currentDirection=STARTING_CARDINAL_DIRECTION; // arbitrary
	}
	
	private RobotMove getCurrentMove() {
		if(null==currentMove) {
			currentMove = new RobotMove();
			operations.add(currentMove);
		}
		return currentMove;
	}
	
	public void add(Direction d) {
		switch(d) {
			case FORWARD:
				// keep moving in current direction
				// if currentMove is null, a Move is added to operations
				// and then returned by getCurrentMove
				getCurrentMove().incrementDistance(this);
				assert (operations.get(operations.size()-1) instanceof RobotMove);
				return;
			default:
				// rotation added --> most recent move operation cannot be extended further
				// reset to null and add a new move operation
				currentMove=null;
				addRotation(MazeMath.directionToTurn(d));
				break;
		}
		
		// whatever direction we are now facing,
		// start moving in that direction
		// by above logic, currentMove can be null without issue
		getCurrentMove().incrementDistance(this);
	}
	
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
	
	private Direction getDirectionOfNextMove() {
		int[] next = MazeMath.getNeighborCloserToExit(currentPosition, maze);
		deltaNext = MazeMath.subArrays(next, currentPosition);
		
		assert 1==Math.abs(deltaNext[0] + deltaNext[1]):
			"deltaNext error: "+Arrays.toString(next)+", "+Arrays.toString(currentPosition);
		
		cdNext = CardinalDirection.getDirection(deltaNext[0], deltaNext[1]);
		
		assert 1==maze.getDistanceToExit(currentPosition[0], currentPosition[1])-maze.getDistanceToExit(next[0], next[1]):
			"cdNext error: new distance is "+maze.getDistanceToExit(next[0], next[1])+
			"while previous distance is "+maze.getDistanceToExit(currentPosition[0], currentPosition[1]);
		
		return MazeMath.CardinalDirectionToDirection(cdNext, currentDirection);
	}
	
	public void buildExitPath() {
		//System.out.println("buildExitPath starting");
		int x = currentPosition[0], y = currentPosition[1];
		Direction d;
		while(!floorplan.isExitPosition(x, y)) {
			//System.out.printf("<%d, %d>\n",x,y);
			d=getDirectionOfNextMove();
			add(d);
			currentPosition = MazeMath.addArrays(currentPosition,deltaNext);
			assert testManhattanDistanceIsOne(x,y,currentPosition);
			currentDirection = cdNext;
			x=currentPosition[0];
			y=currentPosition[1];
		}
		
		assert floorplan.isExitPosition(currentPosition[0], currentPosition[1]) : "tracker is not at exit position";
		
		//System.out.printf("<%d, %d> (final)\n",x,y);
		
		CardinalDirection cd = MazeMath.getCardinalDirectionOfMazeExit(maze);
		d=MazeMath.CardinalDirectionToDirection(cd, currentDirection);
		//System.out.printf("tracker is at the exit: current direction is %s, exit located towards %s, -->direction:%s\n",currentDirection,cd,d);
		add(d);
		//System.out.printf("the last two operations are: %s, %s\n",operations.get(operations.size()-2),operations.get(operations.size()-1));
		
		operations.trimToSize();
		//System.out.printf("buildExitPath finishing (distance traveled=%d, maze max dist=%d):\n%s\n",
		//		totalDistance, distance.getMaxDistance(), operationsString());
		
		assert distance.getMaxDistance()==totalDistance : 
			String.format("Total distance %d != maze max distance % d", totalDistance, distance.getMaxDistance());
	}
	
	private boolean testManhattanDistanceIsOne(int x, int y, int[] currentPosition) {
		int d=MazeMath.manhattanDistance(x, y, currentPosition);
		if(1==d) return true;
		else System.out.printf(
				"a single move should have manhattan distance 1, but we have: "
				+ "[%d, %d] <--> %s\n", x, y, Arrays.toString(currentPosition));
		return false;
	}
	
	private String shortTurnName(Turn t) {
		switch(t) {
			case LEFT: return "L";
			case RIGHT: return "R";
			case AROUND: return "A";
			}
		return null;
	}
	
}

abstract class RobotOperation{
	
	abstract void operateRobot(Robot robot);
	
}

class RobotMove extends RobotOperation{
	private int distance=0;
	
	public void incrementDistance(RobotOperationTracker tracker) {
		distance++;
		tracker.incrementTotalDistance();
	}
	
	public int getDistance() {
		return distance;
	}
	
	public void RobotMove() {}
	
	public void RobotMove(int distance) {
		this.distance=distance;
	}
	
	public void operateRobot(Robot robot) {
		robot.move(distance, false);
	}
	
	@Override
	public String toString() {
		return String.format("Move(%d)",distance);
	}
}

class RobotRotation extends RobotOperation{
	private Turn turn;
	
	public RobotRotation(Turn turn) {
		this.turn=turn;
	}
	
	public Turn getTurn() {
		return turn;
	}
	
	public void operateRobot(Robot robot) {
		if(null!=turn) robot.rotate(turn);
	}
	
	@Override
	public String toString() {
		return String.format("Rotation<%s>",turn);
	}
}