package gui;

import static org.junit.Assert.*;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	float energyUsageToExit;
	
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
		Controller.suppressWarnings=true;
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
		//System.out.println("operations: "+tracker.operationsString());
	}
	
	/**
	 * Cleanup method: sets values to null so that
	 * the test class has no memory of data from the previous maze.
	 */
	//@After
	public void resetFields() {
		System.out.println("resetting fields");
		resetState();
		maze=null;
		floorplan=null;
		distance=null;
		operations=null;
		energyUsageToExit=0;
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
	public void setController(boolean justEnoughEnergy) {
		setController(maze,justEnoughEnergy);
	}
	
	/**
	 * Using an already established maze, set a controller
	 * to start a playing state from this maze.
	 * 
	 * @param maze the maze of interest
	 */
	public void setController(Maze maze, boolean justEnoughEnergy) {
		resetState();
		control = new Controller(true);
		control.turnOffGraphics();
		control.switchFromGeneratingToPlaying(maze);
		robot = (BasicRobot)control.getRobot();
		
		/*
		 * the robot should be able to exit the maze if when
		 * standing at the exit its remaining energy is that required to move forward
		 * it does not require the extra energy normally used to sense distances,
		 * for it is outside the maze and has no need to do so;
		 * 
		 * when justEnoughEnergy is true, this is tested explicitly
		 */
		if(justEnoughEnergy) robot.setBatteryLevel(energyUsageToExit+robot.getEnergyForStepForward());
		else robot.setBatteryLevel(100000);
		//System.out.println("setting robot energy to "+robot.getBatteryLevel());
	}
	
	@Test
	public void tests() {
		for(int level=0; level<8; level++) {
			for(int i=0; i<20; i++) testMaze(level,i+1);
			
			System.out.println(": -- -- --      Tests completed for level "+level+"      -- -- -- :");
		}
	}
	
	public void testMaze(int level, int iter) {
		System.out.printf("testMaze %d-%d\n",level,iter);
		buildNewMaze(level);
		
		energyUsageToExit = walkToExit();
		
		setController(true);
		fullMazeWalk();
		
		setController(true);
		fullMazeWalk_LeaveByJump();
		
		for (Direction d: Direction.values()) {
			setController(true);
			testWallCrash(d);
		}
		
		testInsufficientEnergy_RotateStopsRobot();
		
		setController(false);
		testInsufficientEnergy_MoveStopsRobot();
		
		setController(false);
		testInsufficientEnergy_JumpStopsRobot();
		
		setController(false);
		testJumps();
		
		setController(true);
		
		resetFields();
	}
	
	/**
	 * Tell whether the Controller is in the winning state.
	 * @return whether controller is in winning state
	 */
	private boolean isInStateWinning() {
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
		assertTrue(isInStateWinning());
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
	
	private void testNonOperationalSensorsDoNotUseEnergy() {
		// TODO if a side sensor is disabled and the robot moves forward,
		// it does not use the energy associated with that sensor
	}
	
	/**
	 * Prepares the robot to walk through the maze.
	 * @return initial energy level of the robot
	 */
	private float preWalk() {
		float initialEnergy = robot.getBatteryLevel();
		
		// robot and controller should always share orientation
		assertOrientation();
		
		// tracker and robot must be in alignment
		while(robot.getCurrentDirection()!=RobotOperationTracker.STARTING_CARDINAL_DIRECTION) {
			robot.rotate(Turn.LEFT);
			assertOrientation();
		}
		return initialEnergy;
	}
	
	private boolean jumpPossible(Direction d) {
		int[] neighbor = MazeMath.getNeighborInDirection(getRobotPosition(), d, robot.getCurrentDirection());
		
		return (robot.getBatteryLevel()>=53 && //sufficient battery
				robot.distanceToObstacle(d)==0 && // facing a wall at 0 distance
				maze.isValidPosition(neighbor[0], neighbor[1]) // destination is inside maze
				);
	}
	
	public ArrayList<Integer> getDistancesList() {
		ArrayList<Integer> list = new ArrayList<Integer>(4);
		for(Direction d: Direction.values()) {
			list.add(robot.distanceToObstacle(d));
		}
		return list;
	}
	/*
	*/
	private HashMap<Direction,Integer> getDistancesMap() {
		HashMap<Direction,Integer> map = new HashMap<Direction,Integer>(4);
		for(Direction d: Direction.values()) {
			map.put(d, robot.distanceToObstacle(d));
		}
		return map;
	}
	
	private boolean operateRobot(RobotOperation op, boolean tryInterrupt) {
		if(tryInterrupt) {
			for(Direction d: Direction.values()) {
				if(jumpPossible(d)) {
					faceDirection(d, robot);
					return false;
				}
			}
		}
		
		HashMap<Direction,Integer> distances = (op instanceof RobotRotation) ? getDistancesMap() : null;
		int[] position = getRobotPosition();
		boolean exitSight = canSeeExitInAnyDirection();
		boolean insideRoom = robot.isInsideRoom();
		
		op.operateRobot(robot);
		assertOrientation();
		
		// check that distances have not changed after rotation, simply rotated
		if(op instanceof RobotRotation) {
			//things that should not change with rotation
			assertTrue(Arrays.equals(position,getRobotPosition()));
			assertEquals(exitSight,canSeeExitInAnyDirection());
			assertEquals(insideRoom,robot.isInsideRoom());
			
			HashMap<Direction,Integer> distancesRotated = getDistancesMap();
			Turn turn = ((RobotRotation) op).getTurn();
			for(Direction d: Direction.values()) {
				Direction newDirection = MazeMath.directionToDirection(d, turn);
				//System.out.printf("%s : %s --> %s\n",d,turn,newDirection);
				/*
				 * what this comparison does:
				 * -------------------------
				 * 
				 * before the rotation, the robot referenced a direction <d>
				 *     that pointed to some absolute direction <A>
				 * then it rotated by turn <t>
				 * after this turn, the robot now equates <A> with <newDirection>
				 * 
				 * since the robot only rotated and did not move,
				 * the distance in the absolute direction <A> should not change over the rotation
				 * 
				 * hence why this comparison
				 */
				int dist1=distances.get(d), dist2=distancesRotated.get(newDirection);
				assertEquals(dist1+"!="+dist2,dist1,dist2);
			}
		}
		
		return true;
	}
	
	private float walkToExit() {
		return walkToExit(false);
	}
	
	private boolean canSeeExitInAnyDirection() {
		for(Direction d: Direction.values()) {
			if(robot.canSeeThroughTheExitIntoEternity(d)) return true;
		}
		return false;
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
	 * 
	 * @param tryInterrupt whether or not to try interrupting the walk process, used when testing jump functionality
	 */
	private float walkToExit(boolean tryInterrupt) {
		float initialEnergy=preWalk();
		
		//System.out.printf("Oriented in direction %s at position %s: starting operation sequence\n",
		//		RobotOperationTracker.STARTING_CARDINAL_DIRECTION, Arrays.toString(getRobotPosition()));
		
		// walk the robot through maze up to the last operation
		// i.e., stop before the last operation
		
		for(int i=0; i<operations.size()-1; i++) {
			if(!operateRobot(operations.get(i), tryInterrupt)) {
				return 0;
			}
		}
		
		// the robot's only remaining operation is to move forward through the exit
		// it does not have to be at exit, but it has to be facing the exit
		assertTrue(robot.canSeeThroughTheExitIntoEternity(Direction.FORWARD));
		
		// this moves it to the exit
		robot.move(((RobotMove)(operations.get(operations.size()-1))).getDistance()-1, true);
		assert robot.isAtExit();
		
		return initialEnergy-robot.getBatteryLevel();
	}
	
	/**
	 * Assert that after leaving a maze, the robot recognizes it is outside,
	 * and the Controller has switched to the winning state.
	 * 
	 * Called when the robot has left a maze.
	 */
	private void assertOutsideMaze() {
		// has the application responded as expected?
		assertTrue(isInStateWinning());
		
		// robot should be outside maze
		assertEquals(null,getRobotPosition());
		
		// by design, robot should have used exactly all energy
		// design motivation: test functionality that robot can exit maze
		// with exactly the right amount of energy, requiring no excess
		assertTrue(0==robot.getBatteryLevel());
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
		robot.move(1, true);
		//should be outside
		assertOutsideMaze();
	}
	
	private void assertEnergyDepleted() {
		assertTrue(isInStateWinning());
		assertTrue("robot still has energy: "+robot.getBatteryLevel()+ " : "+control.getRobotFailureMessage(),
					control.getRobotFailureMessage()==BasicRobot.noEnergyMessage);
	}
	
	private void testInsufficientEnergy_RotateStopsRobot() {
		for(Turn t: Turn.values()) {
			setController(false);
			testInsufficientEnergy_RotateStopsRobot(t);
		}
	}
	
	private void testInsufficientEnergy_RotateStopsRobot(Turn t) {
		robot.setBatteryLevel(robot.getEnergyForFullRotation()/4-1);
		
		ArrayList<Integer> distances = getDistancesList();
		
		robot.rotate(t);
		assertEnergyDepleted();
		
		//if rotation failed (as expected), distances should not change
		assertTrue(Arrays.equals(distances.toArray(), getDistancesList().toArray()));
		
	}
	
	private void testInsufficientEnergy_MoveStopsRobot() {
		// first put the robot in a position where it can move
		while(robot.distanceToObstacle(Direction.FORWARD)==0) robot.rotate(Turn.LEFT);
		
		// move requires move energy + 2 * distance-sensing energy
		// so this is not enough energy to move inside a maze
		robot.setBatteryLevel(BasicRobot.energyUsedForMove+BasicRobot.energyUsedForDistanceSensing);
		
		// doesn't work if robot is at exit because at that cell only
		// robot can leave maze without extra distance-sensing energy
		assert !robot.isAtExit();
		
		int position[] = getRobotPosition();
		
		// the robot has enough energy to move, but not enough to complete distance sensing once moved
		// check that position has changed and that energy is depleted
		robot.move(1,true);
		assertEnergyDepleted();
		
		int[] newPosition = getRobotPosition();
		assertFalse(Arrays.equals(position,newPosition));
		
		// now that the robot is out of energy, a move should have no effect on position
		robot.move(1,true);
		assertTrue(Arrays.equals(newPosition,newPosition));
		
		
	}

	private void testInsufficientEnergy_JumpStopsRobot() {
		testInsufficientEnergy_JumpStopsRobot(true);
		setController(false);
		testInsufficientEnergy_JumpStopsRobot(false);
	}

	private void testInsufficientEnergy_JumpStopsRobot(boolean withDistanceEnergy) {
		// walk the robot to an orientation where a jump is possible
		walkToExit(true);
		assertTrue(jumpPossible(Direction.FORWARD));
		
		robot.setBatteryLevel(BasicRobot.energyUsedForJump-1+(withDistanceEnergy? 3 : 0));
		int[] position = getRobotPosition();
		try {
			robot.jump();
		} catch (Exception e) {
			//e.printStackTrace();
		}
		assertEnergyDepleted();
		
		boolean samePosition=Arrays.equals(position,getRobotPosition());
		
		// if the robot had enough energy to make a jump but not to sense distance once there,
		// its position should still render as different from before
		if(withDistanceEnergy) assertFalse(samePosition);
		// if it did not have the energy to jump, then position should not have changed
		else assertTrue(samePosition);
		
	}
	
	private void testJumps() {
		// walk the robot to an orientation where a jump is possible
		walkToExit(true);
		
		// jump forwards, then return back to original position
		testJump();
		robot.rotate(Turn.AROUND);
		testJump();
	}
	
	private void testJump() {
		int[] pos = getRobotPosition();
		int[][] dists=distance.getAllDistanceValues();
		// robot should be in an orientation where it can jump
		assertTrue(jumpPossible(Direction.FORWARD));
		
		boolean successfulJump=true;
		try {
			robot.jump();
			assertEquals(robot.distanceToObstacle(Direction.BACKWARD),0);
		} catch (Exception e) {
			//e.printStackTrace();
			successfulJump=false;
		}
		
		assertTrue(successfulJump);
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
	
	private void assertExitSight() {
		for(Direction d: Direction.values()) {
			CardinalDirection cd = MazeMath.DirectionToCardinalDirection(d, robot.getCurrentDirection());
			int[] delta = cd.getDirection();
			boolean exitSight = robot.canSeeThroughTheExitIntoEternity(d);
			
			int[] exit = distance.getExitPosition();
			int[] position = getRobotPosition();
			
			// if the exit can be seen, it must share at least a row or a column with the robot
			if(exitSight) assertTrue(position[0]==exit[0] || position[1]==exit[1]);
			
			boolean noWall=true;
			
			// if there is any wall in the direction <d>, noWall becomes false
			// otherwise it remains true
			while(true) {
				noWall = noWall && floorplan.hasNoWall(position[0], position[1], cd);
				if(!noWall) break;
				position = MazeMath.addArrays(position, delta);
				if(!maze.isValidPosition(position[0], position[1])) break;
			}
			
			if(exitSight) { // if exit is visible in this direction, no walls must be in the way
				assertTrue(Arrays.equals(MazeMath.subArrays(position, delta),exit));
			}
			// otherwise, there must be a wall in the way
			else assertFalse(noWall);
		}
	}
	
	private void assertOrientation() {
		assertEqualOrientation();
		assertExitSight();
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
		
		assertEquals(MazeMath.directionToDirection(left, Turn.RIGHT),backward);
		assertEquals(MazeMath.directionToDirection(left, Turn.LEFT),forward);
		assertEquals(MazeMath.directionToDirection(left, Turn.AROUND),right);
		assertEquals(MazeMath.directionToDirection(right, Turn.RIGHT),forward);
		assertEquals(MazeMath.directionToDirection(right, Turn.LEFT),backward);
		assertEquals(MazeMath.directionToDirection(right, Turn.AROUND),left);
		assertEquals(MazeMath.directionToDirection(forward, Turn.RIGHT),left);
		assertEquals(MazeMath.directionToDirection(forward, Turn.LEFT),right);
		assertEquals(MazeMath.directionToDirection(forward, Turn.AROUND),backward);
		assertEquals(MazeMath.directionToDirection(backward, Turn.RIGHT),right);
		assertEquals(MazeMath.directionToDirection(backward, Turn.LEFT),left);
		assertEquals(MazeMath.directionToDirection(backward, Turn.AROUND),forward);
		
		System.out.println("finished directional conversion checks");
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
		setRobot(robot);
		init(maze);
	}
	
	public void setRobot(Robot robot) {
		this.robot=robot;
	}
	
	public void clearRobot() {
		robot=null;
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
	 */
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
	
	/*
	 * Condense a {@link Turn} name to one letter.
	 * Could use reflection to do this, but no reason for such complication.
	 * 
	 * Used in {@link #operationsString()}
	 * 
	 * @param t a {@link Turn} value
	 * @return the first letter of the value's name
	 */
	private String shortTurnName(Turn t) {
		switch(t) {
			case LEFT: return "L";
			case RIGHT: return "R";
			case AROUND: return "A";
			}
		return null;
	}
	
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
		if(null!=turn) {
			robot.rotate(turn);
		}
	}
	
	@Override
	public String toString() {
		return String.format("Rotation<%s>",turn);
	}
}