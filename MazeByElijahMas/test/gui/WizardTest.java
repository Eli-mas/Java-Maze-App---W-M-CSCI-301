package gui;

public class WizardTest extends AbstractRobotDriverTest {

	@Override
	void setDriver() {
		driver = new Wizard();
		((Wizard)driver).setMaze(maze);
	}

}
