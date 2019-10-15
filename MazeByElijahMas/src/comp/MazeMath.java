package comp;
import gui.Robot.Turn;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import generation.CardinalDirection;
import generation.Maze;
import generation.Floorplan;
import generation.Distance;
import gui.Robot.Direction;
import comp.ExtendedList;

/**
 * 
 * This class provides useful methods to operate on to arrays/lists
 * and convert between the directional systems of
 * {@link generation.Floorplan Floorplan}, {@link generation.Distance Distance},
 * {@link generation.CardinalDirection CardinalDirection},
 * {@link gui.Robot.Direction} and {@link gui.Robot.Turn}, which can
 * be used by other classes without hassle.
 * 
 * @author Elijah Mas
 *
 */
public class MazeMath {

	/**
	 * <p> ArrayList that enumerates cardinal directions
	 * in order of rotating rightwards, starting at West. </p>
	 * 
	 * <p> Instantiated as a list rather than an array
	 * to utilize the List.{@link List#indexOf(Object) indexOf} method. </p>
	 */
	public static final ExtendedList<CardinalDirection> WestSouthEastNorth =
		ExtendedList.from(
			CardinalDirection.West,
			CardinalDirection.South,
			CardinalDirection.East,
			CardinalDirection.North
		)
	;

	/**
	 * <p> The four values of {@link Direction}
	 * starting at forward and rotating rightward. </p>
	 * 
	 * <p> Maintained as a separate array from {@code Direction.values()}
	 * so as to not rely on the order of elements in {@code Direction}. </p>
	 */
	public static final ExtendedList<Direction> ForwardRightBackwardLeft =
		ExtendedList.from(Direction.FORWARD, Direction.RIGHT, Direction.BACKWARD, Direction.LEFT);

	/**
	 * Return the sum of two arrays of equal length.
	 * 
	 * @param a1 an array of type int[]
	 * @param a2 an array of type int[]
	 * @return a1+a2 as a new int[] array
	 */
	public static int[] addArrays(int[] a1, int[] a2) {
		//cannot add if lengths are unequal
		if(a1.length!=a2.length) return null;
		
		int[] result=new int[a1.length];
		for(int i=0; i<a1.length; i++) result[i]=a1[i]+a2[i];
		
		return result;
	}

	/**
	 * Return the difference of two arrays of equal length.
	 * 
	 * @param a1 an array of type int[]
	 * @param a2 an array of type int[]
	 * @return a1-a2 as a new int[] array
	 */
	public static int[] subArrays(int[] a1, int[] a2) {
		//cannot add if lengths are unequal
		if(a1.length!=a2.length) return null;
		
		int[] result=new int[a1.length];
		for(int i=0; i<a1.length; i++) result[i]=a1[i]-a2[i];
		
		return result;
	}

	/**
	 * Get the direction index of the input {@link Direction}
	 * in the {@value #ForwardRightBackwardLeft} field.
	 * @param d a value of {@link Direction}
	 * @return the corresponding position of d in {@value #ForwardRightBackwardLeft}
	 */
	public static int getDirectionIndex(Direction d) {
		/*switch(d) {
			case FORWARD:	return 0;
			case RIGHT:		return 1;
			case BACKWARD:	return 2;
			case LEFT:		return 3;
			
			default:		return null;
		}*/
		return ForwardRightBackwardLeft.indexOf(d);
	}
	
	/**
	 * Translate relative direction to absolute direction given knowledge
	 * of how current direction relates to forward direction. Meant
	 * for usage pertinent to a {@link gui.Robot} instance.
	 * 
	 * @param d a value of {@link Direction}
	 * @param currentDirection	{@link CardinalDirection} corresponding to current forward direction
	 * 							(meant for a robot)
	 * @return {@link CardinalDirection} corresponding to input direction
	 */
	public static CardinalDirection DirectionToCardinalDirection(Direction d, CardinalDirection currentDirection) {
		// because of the parallelism between WestSouthEastNorth and ForwardRightBackwardLeft,
		// the distance between d and forward must be the same between
		// the target CardinalDirection and the current CardinalDirection
		return WestSouthEastNorth.getFrom(currentDirection, getDirectionIndex(d)
				/*get(
				(
					WestSouthEastNorth.indexOf(currentDirection) + getDirectionIndex(d)
				)
				%4*/
			);
	}
	
	public static Direction CardinalDirectionToDirection(CardinalDirection input, CardinalDirection currentDirection) {
		// get the rotational difference from currentDirection to input,
		// then move this distance in ForwardRightBackwardLeft and return the result
		// difference of 0 means input = currentDirection
		
		return ForwardRightBackwardLeft.get(
				WestSouthEastNorth.getDistanceFromToMod(currentDirection, input)
		/*		Math.floorMod(
					WestSouthEastNorth.indexOf(input)
					- WestSouthEastNorth.indexOf(currentDirection)
				, 4)*/
		);
	}

	/**
	 * Get the new absolute direction resulting in a specified
	 * turn from the current absolute direction; intended for
	 * usage pertinent to a {@link gui.Robot Robot} instance.
	 * @param turn a value of {@link Turn}
	 * @param currentDirection the {@link CardinalDirection} that corresponds to the robot's current forward direction
	 * @return the {@link CardinalDirection} that results from the given turn
	 */
	public static CardinalDirection turnToCardinalDirection(Turn turn, CardinalDirection currentDirection) {
		int index = WestSouthEastNorth.indexOf(currentDirection);
		int adjust;
		
		/* array is arranged in left-to-right order
		switch(turn) {
			case LEFT: adjust=-1; break;
			case RIGHT: adjust=1; break;
			case AROUND: adjust=2; break;
			default: return null;
		}*/
		
		// use floorMod to prevent negative index
		return WestSouthEastNorth.get(Math.floorMod(index+getTurnIndex(turn),4));
	}
	
	/**
	 * Convert a {@link Direction} value to a {@link Turn} value;
	 * since both values indicate some form of relative direction,
	 * there is a one-to-one correspondence.
	 * @param d a {@link Direction} value
	 * @return the turn that will put us facing the specified direction
	 */
	public static Turn directionToTurn(Direction d) {
		switch(d) {
			//case FORWARD: return null;
			case BACKWARD: return Turn.AROUND;
			case LEFT: return Turn.LEFT;
			case RIGHT: return Turn.RIGHT;
			default: return null;
		}
	}
	
	/**
	 * <p>Relative to the current forward direction, get the new direction
	 * we will face if we turn by a certain {@link Turn} value.</p>
	 * 
	 * <p><b>Example</b>: consider a robot's LEFT direction. The robot rotates RIGHT.
	 * The direction formerly referenced as LEFT is now referenced as BACKWARDS.</p>
	 * 
	 * <p>Another way of phrasing this: in the above example, say the robot before the turn
	 * is in state 1 and after the turn in state 2. Then the robot's notion of left in state 1
	 * and the robot's notion of backwards in state 2 both point to the same absolute direction.</p>
	 * 
	 * @param d a {@link Direction} value
	 * @param turn a {@link Turn} value
	 * @return the direction resulting from the specified turn
	 */
	public static Direction directionToDirection(Direction d, Turn turn) {
		return ForwardRightBackwardLeft.getFrom(d, -getTurnIndex(turn));
	}
	
	/**
	 * The index of a turn is the distance one would have to move
	 * from the current location in a right-moving ordered array
	 * to reach the location specified by the turn
	 * 
	 * @param t a {@link Turn} value
	 * @return index of the turn
	 */
	public static int getTurnIndex(Turn t) {
		switch(t) {
			case LEFT: return -1;
			case RIGHT: return 1;
			case AROUND: return 2;
			default: return 0;
		}
	}
	
	public static boolean[] booleanMask(int[] a) {
		boolean[] b = new boolean[a.length];
		for(int i=0; i<a.length; i++) b[i]=(0!=a[i]);
		return b;
	}
	
	public static Direction getFrom(Direction source, int distanceFrom) {
		return ForwardRightBackwardLeft.getFrom(source, distanceFrom);
	}
	
	public static CardinalDirection getFrom(CardinalDirection source, int distanceFrom) {
		return WestSouthEastNorth.getFrom(source, distanceFrom);
	}
	
	public static Direction getFrom(Direction source, Turn turn) {
		return ForwardRightBackwardLeft.getFrom(source, getTurnIndex(turn));
	}
	
	public static CardinalDirection getFrom(CardinalDirection source, Turn turn) {
		return WestSouthEastNorth.getFrom(source, getTurnIndex(turn));
	}
	
	/**
	 * Get the (x,y) adjustment analogous to moving in the specified direction
	 * 
	 * @param d a {@link Direction} value
	 * @param currentDirection the {@link CardinalDirection} that corresponds to forward
	 * 
	 * @return the (x,y) direction corresponding to {@code d}
	 */
	public static int[] getDirectionDelta(Direction d, CardinalDirection currentDirection) {
		return DirectionToCardinalDirection(d, currentDirection).getDirection();
	}
	
	/**
	 * Get the neighboring cell in the specified direction from the current cell
	 * 
	 * @param cell current (x,y) position
	 * @param d a {@link Direction} of reference
	 * @param currentDirection the {@link CardinalDirection} that corresponds to forward
	 * 
	 * @return the (x,y) of the neighbor in the direction {@code d}
	 */
	public static int[] getNeighborInDirection(int[] cell, Direction d, CardinalDirection currentDirection) {
		return addArrays(cell,getDirectionDelta(d, currentDirection));
	}
	
	/**
	 * Get the neighboring cell in the specified cardinal direction from the current cell
	 * 
	 * @param cell current (x,y) position
	 * @param d a {@link Direction} of reference
	 * @param currentDirection the {@link CardinalDirection} that corresponds to forward
	 * 
	 * @return the (x,y) of the neighbor in the cardinal direction {@code currentDirection}
	 */
	public static int[] getNeighborInCardinalDirection(int[] cell, CardinalDirection cd) {
		return addArrays(cell,cd.getDirection());
	}
	
	/**
	 * Get all the cells that can be reached from the current cell
	 * by a standard move operation, i.e. not a jump.
	 * 
	 * @param cell {x,y} location in maze
	 * @param maze {@link Maze} instance
	 * @return list of cells that can be reached from {@code cell} by a move operation
	 */
	public static List<int[]> getMazeCellNeighbors(int[] cell, Maze maze){
		List<int[]> neighbors = new LinkedList<int[]>();
		Floorplan floorplan = maze.getFloorplan();
		
		for(CardinalDirection cd: CardinalDirection.values()) {
			if(floorplan.hasNoWall(cell[0], cell[1], cd))
				neighbors.add(getNeighborInCardinalDirection(cell,cd));
		}
		
		return neighbors;
	}
	
	/**
	 * Get the neighboring cell to the current cell whose distance to exit
	 * is one less than that of the current cell.
	 * 
	 * @param cell {x,y} location in maze
	 * @param maze {@link Maze} instance
	 * @return neighoring cell that is closer to exit
	 */
	public static int[] getNeighborCloserToExit(int[] cell, Maze maze) {
		int targetDistance = maze.getDistanceToExit(cell[0], cell[1])-1;
		for(int[] n: getMazeCellNeighbors(cell, maze)) {
			if(maze.getDistanceToExit(n[0], n[1])==targetDistance) return n;
		}
		return null;
	}
	
	/**
	 * Calculate the Manhattan distance between two coordinates,
	 * defined as the sum of the absolute distances between each
	 * pair of values between the coordinates.
	 * 
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public static int manhattanDistance(int[] cell1, int[] cell2) {
		return Math.abs((cell1[0] - cell2[0])) + Math.abs((cell1[1] - cell2[1]));
	}

	/**
	 * Calculate the Manhattan distance between two coordinates,
	 * defined as the sum of the absolute distances between each
	 * pair of values between the coordinates.
	 * 
	 * @param cell1
	 * @param cell2
	 * @return
	 */
	public static int manhattanDistance(int x, int y, int[] cell) {
		return Math.abs((x - cell[0])) + Math.abs((y - cell[1]));
	}

	/**
	 * Realtive to the exit cell of a maze,
	 * get the {@link CardinalDirection} that leads
	 * from this cell out of the maze.
	 * @param maze a {@link Maze} instance
	 * @return the {@code CardinalDirection} that leads out of the maze from the exit cell
	 */
	public static CardinalDirection getCardinalDirectionOfMazeExit(Maze maze) {
		int[] exit = maze.getMazedists().getExitPosition();
		int x=exit[0], y=exit[1];
		for(CardinalDirection cd: CardinalDirection.values()) {
			int[] cdDelta = cd.getDirection();
			
			// exit means two conditions:
			// no wall and movement leads to position outside maze
			if(maze.getFloorplan().hasNoWall(x, y, cd) && !maze.isValidPosition(x+cdDelta[0], y+cdDelta[1])) {
				return cd;
			}
		}
		return null;
	}

}