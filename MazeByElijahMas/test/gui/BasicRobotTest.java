package gui;

import static org.junit.Assert.*;
import org.junit.jupiter.api.Assertions;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import comp.MazeMath;
import comp.RobotMove;
import comp.RobotOperation;
import comp.RobotOperationTracker;
import comp.RobotRotation;
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

import generation.MazeTestGenerator;

import comp.ExtendedList;

/**
 * BasicRobotTest provides a mix of black-box and white-box testing of the
 * BasicRobot class. It maintains a robot field and is aware that this
 * robot is a BasicRobot.
 * 
 * It also instantiates instances of {@link Floorplan}, {@link Distance},
 * and {@link Maze} classes for the robot to navigate, which serves as the
 * basis of testing.
 * 
 * Additional testing via in-line {@code assert} statements are located in
 * {@link BasicRobot}.
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
	 * Maze generation is random, rooms are permissible by way
	 * of the {@code perfect} parameter.
	 * 
	 * @param level level of the maze to be generated
	 * @param perfect whether or not the maze should be perfect
	 */
	private void instantiateApplication(int level, boolean perfect) {
		// for testing we treat the robot as being automatically operated
		//BasicRobot.manualOperation=false;
		
		// we don't want energy running out when not commanded explicitly
		// but if we make this value too large,
		// we will encounter precision-related errors
		Controller.suppressWarnings=true;
		Controller.suppressUpdates=true;
		
		control=MazeTestGenerator.getController(false, false, level);//getController(false, false, level);
		robot=(BasicRobot)control.getRobot();
		control.setInitialRobotEnergyLevel(100000);
		
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
	 * @param perfect whether or not the maze should be perfect
	 */
	private void buildNewMaze(int level, boolean perfect) {
		instantiateApplication(level, perfect);
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
	private void resetFields() {
		//System.out.println("resetting fields");
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
	private void resetState() {
		control=null;
		robot=null;
	}
	
	/**
	 * Using the current {@link #maze} field, set a controller
	 * to start a playing state from this maze.
	 * 
	 * @param justEnoughEnergy whether or not to leave the robot just enough energy to exit the maze
	 */
	private void setController(boolean justEnoughEnergy) {
		setController(maze,justEnoughEnergy);
	}
	
	/**
	 * Using an already established maze, set a controller
	 * to start a playing state from this maze.
	 * 
	 * @param maze the maze of interest
	 */
	private void setController(Maze maze, boolean justEnoughEnergy) {
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
		if(justEnoughEnergy) control.setInitialRobotEnergyLevel(energyUsageToExit+robot.getEnergyForStepForward());
		else control.setInitialRobotEnergyLevel(100000);
		//System.out.println("setting robot energy to "+robot.getBatteryLevel());
	}
	
	/**
	 * <p>Automate the testing of mazes over some number of trials.</p>
	 * 
	 * <p>Mazes are tested from levels 0-7; at each level, two mazes
	 * are randomly generated: one perfect, one imperfect,
	 * and each is subjected to the same test suite, encapsulated
	 * in the {@link #testMaze(int, boolean) testMaze} method.</p>
	 * 
	 * <p>Thus if trials=10, ((7-0)+1)*2+10 = 160 mazes are tested.
	 * On my computer, time ratio is ~150-160 seconds / 10 trials.</p>
	 * 
	 * <p>The reason all tests are combined into one main method instead of
	 * labeling each method as a {@code @Test} method is because
	 * the above permutation of test case (8 levels &#215; 2 perfect values &#215; 10 trials)
	 * is difficult to achieve the latter way.</p>
	 */
	@ParameterizedTest
	@ValueSource(ints = {0,1,2,3,4})//,5,6,7
	public void tests(int level) {
		int trials=10;
		System.out.print("\n: > > >      Tests beginning for level "+level+"      < < < :\nrun ");
		for(int i=0; i<trials; i++) {
			System.out.print(trials-1==i ? (i+1) : (i+1)+", ");
			
			if(((i+1)%30)==0) System.out.println();
			
			//test on imperfect and perfect mazes
			testMaze(level, false);
			testMaze(level, true);
			
		}
		System.out.println("\n: -- -- --      Tests completed for level "+level+"      -- -- -- :\n");
	}
	
	/**
	 * Run the suite of tests to be conducted on each maze.
	 * @param level level of the maze
	 * @param perfect whether or not the maze should be perfect
	 */
	private void testMaze(int level, boolean perfect) {
		buildNewMaze(level, perfect);
		
		energyUsageToExit = walkToExit();
		
		setController(true);
		fullMazeWalk();
		
		setController(true);
		fullMazeWalk_LeaveByJump();
		
		for (Direction d: Direction.values()) {
			setController(true);
			testWallCrash(d, false);
		}
		
		testInsufficientEnergy_RotateStopsRobot();
		
		setController(false);
		testInsufficientEnergy_MoveStopsRobot();
		
		setController(false);
		testInsufficientEnergy_JumpStopsRobot();
		
		setController(false);
		testJumps();
		
		testBadJump();
		
		
		walkToExitWithDisabledSensors();
		
		setController(false);
		testSensorInterruption();
		
		testResetOdometer();
		
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
	 * @param runOutOfEnergyFirst whether or not the robot should run out of energy before crashing
	 */
	private void testWallCrash(Direction d, boolean runOutOfEnergyFirst) {
		// move right in front of the wall
		approachWall(d);
		
		if(runOutOfEnergyFirst) robot.setBatteryLevel(robot.getEnergyForStepForward()-1);
		// walk into the wall
		robot.move(1, false);
		
		// verify that robot has stopped and game has ended
		assertTrue(robot.hasStopped());
		assertTrue(isInStateWinning());
		
		// if robot ran out of energy, crash should not have occurred
		// otherwise check for crash
		assertEquals(
			robot.getFailureMessage(),
			runOutOfEnergyFirst ? Constants.robotFailureMessage__NoEnergy : Constants.robotFailureMessage__BadMove);
	}
	
	/**
	 * Walk right up to a wall without hitting it.
	 * @param d which {@link Direction} to approach
	 */
	private void approachWall(Direction d) {
		int wallDistance = robot.distanceToObstacle(d);
		faceDirection(d,robot);
		robot.move(wallDistance, false);
		assertEquals(0, robot.distanceToObstacle(Direction.FORWARD));
	}
	
	/**
	 * Call {@link #testBadJump(boolean)} for inputs of {@code true) and {@code false}.
	 */
	private void testBadJump() {
		setController(false);
		testBadJump(false);
		setController(false);
		testBadJump(true);
	}
	
	/**
	 * Test that a bad jump (an attempt to jump outside a maze) fails as expected.
	 * There is also the option to have the robot run out of energy before
	 * attempting the bad jump, in which case the robot should stop first.
	 * 
	 * @param runOutOfEnergyFirst	whether to have the robot run out of energy before
	 * 								attempting the jump
	 */
	private void testBadJump(boolean runOutOfEnergyFirst) {
		//System.out.println("testing bad jump");
		boolean[] exitDifference = MazeMath.booleanMask(MazeMath.subArrays(distance.getExitPosition(), getRobotPosition()));
		int[] delta=null;
		for(Direction d: Direction.values()) {
			delta=MazeMath.directionToDelta(d, robot.getCurrentDirection());
			if(!Arrays.equals(
					MazeMath.booleanMask(delta),
					exitDifference
			)) {
				//System.out.println("facing robot "+d);
				faceDirection(d,robot);
				break;
			}
		}
		assertTrue(0==robot.getOdometerReading());
		// get the robot to the edge of the maze
		while(maze.isValidPosition(MazeMath.addArrays(getRobotPosition(), delta))) {
			try{robot.jump();}
			catch (Exception e) {assertTrue(false);}
		}
		
		if(runOutOfEnergyFirst) robot.setBatteryLevel(50-1);
		// assert that jump fails
		try {
			robot.jump();
			// if exception was not thrown, must have been because of no energy
			assertEnergyDepleted();
		} catch (Exception e) {
			// e.printStackTrace();
			// if we get here, robot must have had energy and made bad jump
			assertTrue(robot.getBatteryLevel()>=0);
			assertTrue(robot.hasStopped());
			assertTrue(robot.getFailureMessage()==Constants.robotFailureMessage__BadJump);
		}
	}
	
	/**
	 * Rotate the robot until it is facing the input direction.
	 * @param d a {@link Direction} value
	 * @param robot the robot to rotate
	 */
	private static void faceDirection(Direction d, Robot robot) {
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
	private void fullMazeWalk() {
		// get to the exit
		walkToExit();
		// leave maze
		moveOutExit();
	}
	
	/**
	 * Walk the robot to the exit cell and then leave the maze
	 * by attempting a jump.
	 */
	private void fullMazeWalk_LeaveByJump() {
		// get to the exit
		walkToExit();
		// leave maze by attempting jump
		moveOutExitByJump();
	}
	
	/**
	 * Prepares the robot to walk through the maze in sync with
	 * the {@link #tracker}. Keeps track of initial energy level.
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
	
	/**
	 * Tell whether a jump is possible in a given direction.
	 * True if all conditions are met:
	 *   <ul>
	 *     <li> sufficient energy </li>
	 *     <li> wall immediately in front </li>
	 *     <li> not jumping out of maze </li>
	 *   </ul>
	 * 
	 * 
	 * @param d a {@link Direction} value
	 * @return whether a jump is possible towards {@code d}
	 */
	private boolean jumpPossible(Direction d) {
		int[] neighbor = MazeMath.getNeighbor(getRobotPosition(), d, robot.getCurrentDirection());
		
		return (robot.getBatteryLevel()>=53 && //sufficient battery
				robot.distanceToObstacle(d)==0 && // facing a wall at 0 distance
				maze.isValidPosition(neighbor) // destination is inside maze
				);
	}
	
	/**
	 * Get the distances to obstacles in all directions
	 * as a list in right-moving order starting at forward.
	 */
	private ArrayList<Integer> getDistancesList() {
		ArrayList<Integer> list = new ArrayList<Integer>(4);
		for(Direction d: MazeMath.ForwardRightBackwardLeft) {
			list.add(robot.distanceToObstacle(d));
		}
		return list;
	}
	
	/**
	 * Get the distances to obstacles in all directions
	 * as a map::|direction:distance&#62;
	 */
	private HashMap<Direction,Integer> getDistancesMap() {
		HashMap<Direction,Integer> map = new HashMap<Direction,Integer>(4);
		for(Direction d: Direction.values()) {
			map.put(d, robot.distanceToObstacle(d));
		}
		return map;
	}
	
	/**
	 * Operate a robot from a {@link RobotOperation} instance,
	 * with tests embedded.
	 * 
	 * @param op a {@link RobotOperation} instance
	 * @param tryInterrupt whether or not the operation can be interrupted
	 * @return true if operation proceeds, false if interrupted
	 */
	private boolean operateRobot(RobotOperation op, boolean tryInterrupt) {
		// we have the option to stop the current operation from occurring
		// if we reach a cell where the robot can jump
		// functionality used for testing in other methods
		if(tryInterrupt) {
			for(Direction d: Direction.values()) {
				if(jumpPossible(d)) {
					faceDirection(d, robot);
					return false;
				}
			}
		}
		
		// store robot details now for comparison after operation is performed
		HashMap<Direction,Integer> distances = (op instanceof RobotRotation) ? getDistancesMap() : null;
		int[] position = getRobotPosition();
		CardinalDirection cd = robot.getCurrentDirection();
		boolean exitSight = canSeeExitInAnyDirection();
		boolean insideRoom = robot.isInsideRoom();
		int odometerReading = robot.getOdometerReading();
		
		op.operateRobot(robot);
		assertOrientation();
		
		if(op instanceof RobotRotation) {
			HashMap<Direction,Integer> distancesRotated = getDistancesMap();
			Turn turn = ((RobotRotation) op).getTurn();
			
			// things that should not change with rotation
			assertTrue(Arrays.equals(position,getRobotPosition()));
			assertEquals(exitSight,canSeeExitInAnyDirection());
			assertEquals(insideRoom,robot.isInsideRoom());
			assertTrue(robot.getOdometerReading()==odometerReading);
			
			// absolute direction should have changed as expected
			assertEquals(robot.getCurrentDirection(),MazeMath.getFrom(cd, turn));
			
			for(Direction d: Direction.values()) {
				Direction newDirection = MazeMath.getNewDirectionReferenceOverTurn(d, turn);
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
				 * hence this comparison
				 */
				int dist1=distances.get(d), dist2=distancesRotated.get(newDirection);
				assertEquals(dist1+"!="+dist2,dist1,dist2);
			}
		}
		
		else if(op instanceof RobotMove) {
			// direction should not change with move
			assertEquals(cd,robot.getCurrentDirection());
			
			// distance should change
			int opDistance=((RobotMove)op).getDistance();
			assertTrue(robot.getOdometerReading()==odometerReading+opDistance);
			assertTrue(MazeMath.manhattanDistance(getRobotPosition(), position)==opDistance);
		}
		
		return true;
	}
	
	/**
	 * Wrapper around {@link #walkToExit(boolean)}; default value is false.
	 * @return result of {@link #walkToExit(boolean)}
	 */
	private float walkToExit() {
		return walkToExit(false);
	}
	
	/**
	 * Test whether the robot can see the exit in any direction.
	 * @return true if exit is in sight
	 */
	private boolean canSeeExitInAnyDirection() {
		for(Direction d: Direction.values()) {
			if(robot.canSeeThroughTheExitIntoEternity(d)) return true;
		}
		return false;
	}
	
	private boolean disableAllSensors() {
		boolean disabled = true;
		for(Direction d: Direction.values()) {
			robot.triggerSensorFailure(d);
			disabled = (!robot.hasOperationalSensor(d)) && disabled;
		}
		return disabled;
	}
	
	private boolean repairAllSensors() {
		boolean repaired=true;
		boolean reparation;
		for(Direction d: Direction.values()) {
			reparation=robot.repairFailedSensor(d);
			repaired = reparation && repaired && robot.hasOperationalSensor(d);
		}
		return repaired;
	}
	
	private RobotMove getToFirstMove() {
		preWalk();
		
		RobotMove move=null;
		for(RobotOperation op:operations) {
			if(op instanceof RobotMove) {
				move=(RobotMove)op;
				break;
			}
			op.operateRobot(robot);
		}
		
		assertTrue(null!=move);
		
		return move;
	}
	
	private void testResetOdometer() {
		setController(false);
		RobotMove move = getToFirstMove();
		move.operateRobot(robot);
		robot.rotate(Turn.AROUND);
		move.operateRobot(robot);
		
		// make sure that odometer is correct before resetting
		assertTrue(robot.getOdometerReading()==2*move.getDistance());
		
		robot.resetOdometer();
		assertTrue(robot.getOdometerReading()==0);
	}
	
	private void testInterruptedSensorEnergyUsage() {
		RobotMove move = getToFirstMove();
		
		int[] position = getRobotPosition();
		
		float referenceEnergy = robot.getBatteryLevel(), currentEnergy;
		int moveDistance = move.getDistance();
		float stepEnergy=robot.getEnergyForStepForward();
		
		assertTrue(disableAllSensors());
		move.operateRobot(robot);
		currentEnergy=robot.getBatteryLevel();
		// energy to sense distances is not used, move is sole cause of energy usage
		assertTrue(referenceEnergy - stepEnergy*moveDistance == currentEnergy);
		
		referenceEnergy=currentEnergy;
		robot.rotate(Turn.AROUND);
		currentEnergy=robot.getBatteryLevel();
		
		// rotation should use same energy even with disabled sensors
		assertTrue(referenceEnergy-2*(robot.getEnergyForFullRotation()/4)==currentEnergy);
		
		referenceEnergy=currentEnergy;
		
		assertTrue(repairAllSensors());
		
		// take distance sensing energy back into account
		stepEnergy += 2;
		// test that this extra energy is being used upon re-enabling of sensors
		currentEnergy=robot.getBatteryLevel();
		assertTrue(currentEnergy == referenceEnergy - 4);
		referenceEnergy = currentEnergy;
		
		move.operateRobot(robot);
		currentEnergy=robot.getBatteryLevel();
		assertTrue(currentEnergy == referenceEnergy - stepEnergy*moveDistance);
		
		// tampering with sensors should still lead position to be determined accurately
		Assertions.assertArrayEquals(position, getRobotPosition());
		
	}
	
	private void testSensorInterruption() {
		testInterruptedDistanceSensorExceptions();
		setController(false);
		testInterruptedRoomSensorException();
		setController(false);
		testInterruptedSensorEnergyUsage();
	}
	
	private void testInterruptedDistanceSensorExceptions() {
		assertTrue(disableAllSensors());
		
		// sensors are disabled, trying to see exit should throw exception
		for(Direction d: Direction.values())
			Assertions.assertThrows(UnsupportedOperationException.class, () -> robot.canSeeThroughTheExitIntoEternity(d));
		
		assertTrue(repairAllSensors());
		
		// sensors are enabled, trying to see exit should not throw exception
		for(Direction d: Direction.values())
			Assertions.assertDoesNotThrow(() -> robot.canSeeThroughTheExitIntoEternity(d));
	}
	
	private void roomSensorEnabled() {
		robot.enableRoomSensor();
		assertTrue(robot.hasRoomSensor());
		Assertions.assertDoesNotThrow(() -> robot.isInsideRoom());
	}
	
	private void roomSensorDisabled() {
		robot.nullifyRoomSensor();
		assertFalse(robot.hasRoomSensor());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> robot.isInsideRoom());
	}
	
	private void testInterruptedRoomSensorException() {
		roomSensorEnabled();
		roomSensorDisabled();
		roomSensorEnabled();
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
		
		// walk the robot through maze up to the last operation
		// i.e., stop before the last operation
		for(int i=0; i<operations.size()-1; i++) {
			// if interrupted, return a nonce value
			if(!operateRobot(operations.get(i), tryInterrupt)) {
				return 0;
			}
		}
		
		// the robot's only remaining operation is to move forward through the exit
		// it does not have to be at exit, but it has to be facing the exit
		assertTrue(robot.canSeeThroughTheExitIntoEternity(Direction.FORWARD));
		
		// move distance-1, not full distance
		// this moves it to the exit, not through the exit
		robot.move(((RobotMove)(operations.get(operations.size()-1))).getDistance()-1, false);
		assert robot.isAtExit();
		
		return initialEnergy-robot.getBatteryLevel();
	}
	
	/**
	 * Call {@link #walkToExitWithDisabledSensors(boolean)} for
	 * input values of {@code true} and {@code false}.
	 */
	private void walkToExitWithDisabledSensors() {
		setController(false);
		walkToExitWithDisabledSensors(true);
		setController(false);
		walkToExitWithDisabledSensors(false);
	}
	
	/**
	 * Walk through the maze and exit, but disable certain sensors
	 * and check that things behave as expected.
	 * The side sensors are always disabled for this method;
	 * the front and back sensors can be disabled, and
	 * nothing should change.
	 * 
	 * @param disableForwardBackward whether or not to disable front and back sensors
	 */
	private void walkToExitWithDisabledSensors(boolean disableForwardBackward) {
		preWalk();
		
		// we will want to track how much energy is used;
		// these are setup variables to that end
		float initialEnergy=robot.getBatteryLevel();
		float expectedUsage=0;
		float rotationEnergy=robot.getEnergyForFullRotation()/4;
		
		robot.triggerSensorFailure(Direction.LEFT);
		robot.triggerSensorFailure(Direction.RIGHT);
		
		// should not affect any of subsequent assertions
		if(disableForwardBackward) {
			robot.triggerSensorFailure(Direction.FORWARD);
			robot.triggerSensorFailure(Direction.BACKWARD);
		}
		float expectedDifference=0;
		
		for(int i=0; i<operations.size(); i++) {
			RobotOperation op = operations.get(i);
			op.operateRobot(robot);
			
			if(op instanceof RobotMove) {
				// now we only use move distance, not sensor energy,
				// because sensors are disabled
				expectedDifference=
					((RobotMove) op).getDistance() *
					robot.getEnergyForStepForward();
				// not operational --> throw exception
				Assertions.assertThrows(Exception.class, () -> robot.distanceToObstacle(Direction.LEFT));
				Assertions.assertThrows(Exception.class, () -> robot.distanceToObstacle(Direction.RIGHT));
			}
			else if(op instanceof RobotRotation) {
				//rotations function normally
				expectedDifference=
					rotationEnergy *
					Math.abs(MazeMath.getTurnIndex(((RobotRotation) op).getTurn()));
			}
			expectedUsage+=expectedDifference;
		}
		
		// did we use the energy expected?
		//assertTrue(initialEnergy-robot.getBatteryLevel() == expectedUsage);
		
		// set battery to 0 because that is what assertOutsideMaze expects
		// we have already verified energy usage so this is okay
		robot.setBatteryLevel(0);
		assertOutsideMaze();
		
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
		
		//robot is not at exit, but outside
		assertFalse(robot.isAtExit());
		
		// check that room check returns false, even though room sensor is functional
		assertTrue(robot.hasRoomSensor());
		assertFalse(robot.isInsideRoom());
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
		// if the jump is bad, it throws an exception
		Assertions.assertDoesNotThrow(() -> robot.jump());
		
		// should be outside
		assertOutsideMaze();
	}
	
	/**
	 * From the exit cell, leave the maze by a move operation,
	 * and assert that the robot has indeed left the maze.
	 */
	private void moveOutExit() {	
		// exit the maze
		robot.move(1, false);
		//should be outside
		assertOutsideMaze();
	}
	
	/**
	 * Test that the robot has run out of energy and the game has ended.
	 */
	private void assertEnergyDepleted() {
		assertTrue(isInStateWinning());
		assertTrue(robot.hasStopped());
		assertTrue("robot still has energy: "+robot.getBatteryLevel()+ " : "+robot.getFailureMessage(),
					robot.getFailureMessage()==Constants.robotFailureMessage__NoEnergy);
	}
	
	/**
	 * Test the special case where the robot receives a command to turn around
	 * and has enough energy for only one rotation.
	 */
	private void testAround() {
		float initialEnergy = robot.getBatteryLevel();
		robot.rotate(Turn.LEFT);
		float usedEnergy = initialEnergy-robot.getBatteryLevel();
		
		robot.setBatteryLevel(2*usedEnergy-1);
		
		// distances in every direction before rotation
		List<Integer> distancesBeforeRotation = getDistancesList();
		//System.out.print("before around rotation in testAround: ");
		//robot.printDists();
		robot.rotate(Turn.AROUND);
		
		assertEnergyDepleted();
		
		robot.setBatteryLevel(100);
		robot.clearStopped();
		
		// distances in every direction before rotation
		List<Integer> distancesAfterRotation = getDistancesList();
		//robot.printDists();
		
		// the robot should show the signature of one left rotation, not two
		Collections.rotate(distancesBeforeRotation, 1);
		assertEquals(distancesBeforeRotation,distancesAfterRotation);
	}
	
	/**
	 * Call {@link #testInsufficientEnergy_RotateStopsRobot(Turn)} for each {@link Turn} value,
	 * then call {@link #testAround()}.
	 */
	private void testInsufficientEnergy_RotateStopsRobot() {
		for(Turn t: Turn.values()) {
			setController(false);
			testInsufficientEnergy_RotateStopsRobot(t);
		}
		
		//setController(false);
		//testAround();
	}
	
	/**
	 * Test that the robot does not rotate if it has insufficient energy
	 * for a single rotation.
	 * @param t {@link Turn} value for {@link BasicRobot#rotate(Turn) BasicRobot.rotate}
	 */
	private void testInsufficientEnergy_RotateStopsRobot(Turn t) {
		// set battery just below what is required for turn
		robot.setBatteryLevel(robot.getEnergyForFullRotation()/4-1);
		
		ArrayList<Integer> distances = getDistancesList();
		
		robot.rotate(t);
		assertEnergyDepleted();
		
		//if rotation failed (as expected), distances should not change
		assertTrue(Arrays.equals(distances.toArray(), getDistancesList().toArray()));
		
	}
	
	/**
	 * Test whether a robot stops as expected when it tries to move
	 * without having enough energy.
	 */
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
		robot.move(1,false);
		assertEnergyDepleted();
		
		int[] newPosition = getRobotPosition();
		assertFalse(Arrays.equals(position,newPosition));
		
		// now that the robot is out of energy, a move should have no effect on position
		robot.move(1,false);
		assertTrue(Arrays.equals(newPosition,newPosition));
		
		
	}

	/**
	 * Call {@link #testInsufficientEnergy_JumpStopsRobot(boolean)} with input values
	 * {@code true} and {@code false}.
	 */
	private void testInsufficientEnergy_JumpStopsRobot() {
		testInsufficientEnergy_JumpStopsRobot(true);
		setController(false);
		testInsufficientEnergy_JumpStopsRobot(false);
	}

	/**
	 * Test whether a robot stops after attempting a jump due to insufficient energy.
	 * The robot either has too little energy to jump, or enough energy to jump
	 * but not enough energy to sense distances after the jump.
	 * 
	 * @param withDistanceEnergy	if true, the robot has energy to jump,
	 * 								but not enough to sense distances afterwards
	 */
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
	
	/**
	 * Test that a robot can jump over a wall, turn around,
	 * and jump over the same wall without errors. Calls
	 * the {@link #testJump()} method to perform tests.
	 */
	private void testJumps() {
		// walk the robot to an orientation where a jump is possible
		walkToExit(true);
		
		int[] position = getRobotPosition();
		float energyBeforeJump = robot.getBatteryLevel();
		
		// jump forwards, then return back to original position
		testJump();
		
		// used energy for a jump + sensing distance in three directions
		//assertTrue(energyBeforeJump - 53 == robot.getBatteryLevel());
		
		robot.rotate(Turn.AROUND);
		
		// add on energy of two rotations
		//assertTrue(energyBeforeJump - (53+2*(robot.getEnergyForFullRotation()/4)) == robot.getBatteryLevel());
		
		testJump();
		
		// add on energy of another jump and distance collection
		//assertTrue(energyBeforeJump - (2*53+2*(robot.getEnergyForFullRotation()/4)) == robot.getBatteryLevel());
		
		// net position has not changed
		assertTrue(Arrays.equals(position, getRobotPosition()));
	}
	
	/**
	 * Test that a robot completes a jump without error.
	 */
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
		return robot.tryGetCurrentPosition();
	}
	
	/**
	 * Assert that the robot and controller agree on position and direction.
	 */
	private void assertEqualOrientation() {
		assertTrue(Arrays.equals(getRobotPosition(),control.getCurrentPosition()));
		assertEquals(robot.getCurrentDirection(),control.getCurrentDirection());
		assertEquals(robot.isInsideRoom(),floorplan.isInRoom(getRobotPosition()));
	}
	
	/**
	 * Assert that the method {@link BasicRobot#canSeeThroughTheExitIntoEternity(Direction)}
	 * works as expected.
	 * 
	 * When the method return {@code true},
	 * the robot must be in at least the same row or column as the exit,
	 * and there must be no walls in the direction to the exit.
	 * 
	 * When it returns false for a given direction,
	 * there must be a wall in the given direction at some distance.
	 */
	private void assertExitSight() {
		for(Direction d: Direction.values()) {
			CardinalDirection cd = MazeMath.convertDirs(d, robot.getCurrentDirection());
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
	
	/**
	 * Assert that the robot and controller have equal orientation
	 * (call the {@link #assertEqualOrientation()} method),
	 * and verify that {@link BasicRobot#canSeeThroughTheExitIntoEternity(Direction)}
	 * is functioning properly by calling {@link #assertExitSight()}.
	 */
	private void assertOrientation() {
		assertEqualOrientation();
		assertExitSight();
	}
	
	/*
	 * Sets things in order for a maze to be generated.
	 * OrderStub, Controller, and MazeBuilder instances receive
	 * the parameters required to initialize the maze.
	 * Ensures that the maze is finished generating before tests are performed.
	 * 
	 * @param perfect boolean:{maze is perfect}
	 * @param deterministic boolean:{maze is generated deterministically}
	 * @param level level of the maze
	 * @return the generated Maze instance
	private Controller getController(boolean perfect, boolean deterministic, int level){
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
			if(!Controller.suppressUpdates) System.out.println("100");
			
			controller.switchFromGeneratingToPlaying(order.getMaze());
			
			return controller;
			
		} catch (InterruptedException e) {
			System.out.println("\n--Intteruption--");
			e.printStackTrace();
			return null;
		}
	}
	 **/
}