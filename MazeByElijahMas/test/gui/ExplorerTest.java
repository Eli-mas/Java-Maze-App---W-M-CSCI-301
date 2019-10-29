package gui;

/**
 * <b>WARNING: INCOMPLETE</b>
 * 
 * This class is not fully developed and should not be used at this time.
 * 
 * @author Elijah Mas
 *
 */
public class ExplorerTest extends AbstractRobotDriverTest {

	@Override
	void setDriver() {
		Explorer e = new Explorer();
		driver = e;
		e.setStart(maze.getMazedists().getStartPosition());
		e.setDistance(maze.getMazedists());
	}

}
