package gui;

import static org.junit.Assert.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class BasicRobotTest {
	private BasicRobot robot1;
	private Robot robot2;
	
	
	
	/*
	 * test cases
	 * 
	 * does robot exit maze when it has just enough energy to do so?
	 * 
	 * does robot exit maze when it has more than enough energy?
	 * 
	 * in a rotation, do the distances shift as expected?
	 * 
	 * in a move forward, do forward and backward distances change as expected?
	 * 
	 * Immediately after a jump, is the backwards distance always 0?
	 * 
	 * If distance to wall is 0 in any direction, does rotating to face it and moving into it cause a crash?
	 * 
	 * Operations that could cause energy depletion:
	 *     move
	 *     jump
	 *     rotate
	 *     distance sensing
	 */
}
