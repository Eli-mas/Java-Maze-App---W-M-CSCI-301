package gui;

/**
 * Very lightweight test class to test {@link Wizard} operation.
 * 
 * Lightweight because the Wizard class itself is very simple;
 * most of the testing for it was done in Project 3,
 * and via assertion statesments in {@link comp.RobotOperationTracker}
 * 
 * @author Elijah Mas
 *
 */
public class WizardTest extends AbstractRobotDriverTest {

	@Override
	void setDriver() {
		driver = new Wizard();
		((Wizard)driver).setMaze(maze);
	}

}
