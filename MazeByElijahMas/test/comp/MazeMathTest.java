package comp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import generation.CardinalDirection;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * Test methods from {@link comp.MazeMath}.
 * 
 * .
 */
public class MazeMathTest {
	
	static final Direction
		forward=Direction.FORWARD,
		backward=Direction.BACKWARD,
		left=Direction.LEFT,
		right=Direction.RIGHT;
	static final CardinalDirection
		north=CardinalDirection.North,
		south=CardinalDirection.South,
		east=CardinalDirection.East,
		west=CardinalDirection.West;
	
	// convert relative to absolute direction
	@Test
	public void testConvertRelativeToAbsolute() {
		assertEquals(MazeMath.convertDirs(forward,east),east);
		assertEquals(MazeMath.convertDirs(forward,north),north);
		assertEquals(MazeMath.convertDirs(forward,south),south);
		assertEquals(MazeMath.convertDirs(forward,west),west);
		assertEquals(MazeMath.convertDirs(backward,north),south);
		assertEquals(MazeMath.convertDirs(backward,south),north);
		assertEquals(MazeMath.convertDirs(backward,east),west);
		assertEquals(MazeMath.convertDirs(backward,west),east);
		assertEquals(MazeMath.convertDirs(left,north),east);
		assertEquals(MazeMath.convertDirs(left,east),south);
		assertEquals(MazeMath.convertDirs(left,south),west);
		assertEquals(MazeMath.convertDirs(left,west),north);
		assertEquals(MazeMath.convertDirs(right,east),north);
		assertEquals(MazeMath.convertDirs(right,south),east);
		assertEquals(MazeMath.convertDirs(right,west),south);
		assertEquals(MazeMath.convertDirs(right,north),west);
	}
	
	// convert relative to absolute direction in terms of (x,y) delta
	@Test
	public void testConvertRelativeToDelta() {
		assertArrayEquals(MazeMath.directionToDelta(forward,east),east.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(forward,north),north.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(forward,south),south.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(forward,west),west.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(backward,north),south.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(backward,south),north.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(backward,east),west.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(backward,west),east.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(left,north),east.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(left,east),south.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(left,south),west.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(left,west),north.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(right,east),north.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(right,south),east.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(right,west),south.getDirection());
		assertArrayEquals(MazeMath.directionToDelta(right,north),west.getDirection());
	}
	
	// convert absolute to relative direction
	@Test
	public void testConvertAbsoluteToRelative() {
		assertEquals(MazeMath.convertDirs(east,east),forward);
		assertEquals(MazeMath.convertDirs(north,north),forward);
		assertEquals(MazeMath.convertDirs(west,west),forward);
		assertEquals(MazeMath.convertDirs(south,south),forward);
		assertEquals(MazeMath.convertDirs(south,north),backward);
		assertEquals(MazeMath.convertDirs(north,south),backward);
		assertEquals(MazeMath.convertDirs(west,east),backward);
		assertEquals(MazeMath.convertDirs(east,west),backward);
		assertEquals(MazeMath.convertDirs(east,north),left);
		assertEquals(MazeMath.convertDirs(south,east),left);
		assertEquals(MazeMath.convertDirs(west,south),left);
		assertEquals(MazeMath.convertDirs(north,west),left);
		assertEquals(MazeMath.convertDirs(north,east),right);
		assertEquals(MazeMath.convertDirs(east,south),right);
		assertEquals(MazeMath.convertDirs(south,west),right);
		assertEquals(MazeMath.convertDirs(west,north),right);
	}
	
	// calculate how relative direction reference changes over turn
	@Test
	public void testChangeOfRelativeDirectionalReference() {
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(left, Turn.RIGHT),backward);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(left, Turn.LEFT),forward);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(left, Turn.AROUND),right);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(right, Turn.RIGHT),forward);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(right, Turn.LEFT),backward);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(right, Turn.AROUND),left);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(forward, Turn.RIGHT),left);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(forward, Turn.LEFT),right);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(forward, Turn.AROUND),backward);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(backward, Turn.RIGHT),right);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(backward, Turn.LEFT),left);
		assertEquals(MazeMath.getNewDirectionReferenceOverTurn(backward, Turn.AROUND),forward);
	}
	
	// swapping between relative directions with indices
	@Test
	public void testGetRelativeFromRelativeByIndex() {
		assertEquals(MazeMath.getFrom(forward, 0),forward);
		assertEquals(MazeMath.getFrom(forward, 1),right);
		assertEquals(MazeMath.getFrom(forward, 2),backward);
		assertEquals(MazeMath.getFrom(forward, 3),left);
		assertEquals(MazeMath.getFrom(right, 0),right);
		assertEquals(MazeMath.getFrom(right, 1),backward);
		assertEquals(MazeMath.getFrom(right, 2),left);
		assertEquals(MazeMath.getFrom(right, 3),forward);
		assertEquals(MazeMath.getFrom(backward, 0),backward);
		assertEquals(MazeMath.getFrom(backward, 1),left);
		assertEquals(MazeMath.getFrom(backward, 2),forward);
		assertEquals(MazeMath.getFrom(backward, 3),right);
		assertEquals(MazeMath.getFrom(left, 0),left);
		assertEquals(MazeMath.getFrom(left, 1),forward);
		assertEquals(MazeMath.getFrom(left, 2),right);
		assertEquals(MazeMath.getFrom(left, 3),backward);
	}
	
	// swapping between relative directions with out-of-bound indices to test modulo arithmetic
	@Test
	public void testGetRelativeFromRelativeByIndexModulo() {
		assertEquals(MazeMath.getFrom(forward, 4),forward);
		assertEquals(MazeMath.getFrom(forward, -3),right);
		assertEquals(MazeMath.getFrom(forward, -2),backward);
		assertEquals(MazeMath.getFrom(forward, -1),left);
		assertEquals(MazeMath.getFrom(right, 4),right);
		assertEquals(MazeMath.getFrom(right, -3),backward);
		assertEquals(MazeMath.getFrom(right, -2),left);
		assertEquals(MazeMath.getFrom(right, -1),forward);
		assertEquals(MazeMath.getFrom(backward, 4),backward);
		assertEquals(MazeMath.getFrom(backward, -3),left);
		assertEquals(MazeMath.getFrom(backward, -2),forward);
		assertEquals(MazeMath.getFrom(backward, -1),right);
		assertEquals(MazeMath.getFrom(left, 4),left);
		assertEquals(MazeMath.getFrom(left, -3),forward);
		assertEquals(MazeMath.getFrom(left, -2),right);
		assertEquals(MazeMath.getFrom(left, -1),backward);
	}
	
	// swapping between relative directions with turns
	@Test
	public void testGetRelativeFromRelativeByTurn() {
		assertEquals(MazeMath.getFrom(forward, Turn.RIGHT),right);
		assertEquals(MazeMath.getFrom(forward, Turn.AROUND),backward);
		assertEquals(MazeMath.getFrom(forward, Turn.LEFT),left);
		assertEquals(MazeMath.getFrom(right, Turn.RIGHT),backward);
		assertEquals(MazeMath.getFrom(right, Turn.AROUND),left);
		assertEquals(MazeMath.getFrom(right, Turn.LEFT),forward);
		assertEquals(MazeMath.getFrom(backward, Turn.RIGHT),left);
		assertEquals(MazeMath.getFrom(backward, Turn.AROUND),forward);
		assertEquals(MazeMath.getFrom(backward, Turn.LEFT),right);
		assertEquals(MazeMath.getFrom(left, Turn.RIGHT),forward);
		assertEquals(MazeMath.getFrom(left, Turn.AROUND),right);
		assertEquals(MazeMath.getFrom(left, Turn.LEFT),backward);
	}
	
	// swapping between absolute directions with turns
	@Test
	public void testGeAbsoluteFromAbsoluteByTurn() {
		assertEquals(MazeMath.getFrom(west, Turn.RIGHT),south);
		assertEquals(MazeMath.getFrom(west, Turn.AROUND),east);
		assertEquals(MazeMath.getFrom(west, Turn.LEFT),north);
		assertEquals(MazeMath.getFrom(south, Turn.RIGHT),east);
		assertEquals(MazeMath.getFrom(south, Turn.AROUND),north);
		assertEquals(MazeMath.getFrom(south, Turn.LEFT),west);
		assertEquals(MazeMath.getFrom(east, Turn.RIGHT),north);
		assertEquals(MazeMath.getFrom(east, Turn.AROUND),west);
		assertEquals(MazeMath.getFrom(east, Turn.LEFT),south);
		assertEquals(MazeMath.getFrom(north, Turn.RIGHT),west);
		assertEquals(MazeMath.getFrom(north, Turn.AROUND),south);
		assertEquals(MazeMath.getFrom(north, Turn.LEFT),east);
	}
	
	@Test
	public void testAddArrays() {
		// add arrays: order does not matter
		assertArrayEquals(MazeMath.addArrays(new int[] {1,2}, new int[] {100,200}), new int[] {101,202});
		assertArrayEquals(MazeMath.addArrays(new int[] {100,200}, new int[] {1,2}), new int[] {101,202});
		assertArrayEquals(MazeMath.addArrays(new int[] {100,200}, new int[] {-1,-2}), new int[] {99,198});
		assertArrayEquals(MazeMath.addArrays(new int[] {-1,-2}, new int[] {100,200}), new int[] {99,198});
	}
	
	@Test
	public void testSubArrays() {
		//subtract arrays: order matters
		assertArrayEquals(MazeMath.subArrays(new int[] {1,2}, new int[] {100,200}), new int[] {-99,-198});
		assertArrayEquals(MazeMath.subArrays(new int[] {100,200}, new int[] {1,2}), new int[] {99,198});
		assertArrayEquals(MazeMath.subArrays(new int[] {100,200}, new int[] {-1,-2}), new int[] {101,202});
		assertArrayEquals(MazeMath.subArrays(new int[] {-1,-2}, new int[] {100,200}), new int[] {-101,-202});
	}
	
	// boolean mask: check that any value !=0 yields true, 0 yields false
	@Test
	public void testBooleanMask() {
		assertArrayEquals(MazeMath.booleanMask(new int[] {-1,-2}), new boolean[] {true,true});
		assertArrayEquals(MazeMath.booleanMask(new int[] {1,2}), new boolean[] {true,true});
		assertArrayEquals(MazeMath.booleanMask(new int[] {10,0}), new boolean[] {true,false});
		assertArrayEquals(MazeMath.booleanMask(new int[] {0,100}), new boolean[] {false,true});
		assertArrayEquals(MazeMath.booleanMask(new int[] {0,-123}), new boolean[] {false,true});
		assertArrayEquals(MazeMath.booleanMask(new int[] {-1234,0}), new boolean[] {true,false});
	}

}
