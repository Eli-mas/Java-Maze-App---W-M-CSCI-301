package gui;

public class ExplorerTest extends AbstractRobotDriverTest {

	@Override
	void setDriver() {
		Explorer e = new Explorer();
		driver = e;
		e.setStart(maze.getMazedists().getStartPosition());
		e.setDistance(maze.getMazedists());
	}

}
