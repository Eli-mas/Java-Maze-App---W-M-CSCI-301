package gui;

import comp.MazeMath;
import generation.CardinalDirection;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

/*
class RobotState {
	public int distanceLeft() {
		return -1;
	}
	public int distanceRight() {
		return -1;
	}
	public int distanceForward() {
		return -1;
	}
	public int distanceBackward() {
		return -1;
	}
}

class State_0000{
	
}

class State_0001{
	@Override
	public int distanceLeft() {
		
	}
}
*/

class RobotSensorTrigger implements Runnable{
	
	Direction direction;
	AbstractRobotDriver driver;
	Robot robot;
	public static int deltaT = 3000;
	
	public RobotSensorTrigger(AbstractRobotDriver driver, Direction direction) {
		this.direction=direction;
		this.driver=driver;
		this.robot=driver.getRobot();
	}
	
	public void start() {
		
	}
	
	@Override
	public void run() {
		while(null!=driver.getRobotPosition() && !robot.hasStopped()) {
			boolean sensorState = robot.hasOperationalSensor(direction);
			if(sensorState) robot.triggerSensorFailure(direction);
			else robot.repairFailedSensor(direction);
			try {
				Thread.sleep(deltaT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}

public abstract class AbstractRobotDriver implements RobotDriver {
	
	Robot robot;
	int width, height;
	Distance distance;
	float robotStartEnergy;
	Controller controller;
	CardinalDirection currentDirection;
	int[] currentPosition;
	
	public String getRobotFailureMessage() {
		return controller.getRobotFailureMessage();
	}
	
	public Robot getRobot() {
		return robot;
	}
	
	boolean tellIfOutsideMaze() {
		return null==getRobotPosition();
	}
	
	/**
	 * wrapper around {@link Robot#getCurrentPosition()} that handles exception throw
	 * @return position if no exception, otherwise null
	 */
	int[] getRobotPosition() {
		try {
			return robot.getCurrentPosition();
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}
	
	public CardinalDirection getCurrentDirection() {
		return currentDirection;
	}
	
	@Override
	public void setRobot(Robot r) {
		robot=r;
		currentDirection = robot.getCurrentDirection();
		currentPosition = getRobotPosition();
		
		//System.out.println("setRobot called; currentDirection="+currentDirection);
	}

	@Override
	public void setDimensions(int width, int height) {
		this.width=width;
		this.height=height;
	}

	@Override
	public void setDistance(Distance distance) {
		this.distance=distance;
	}

	@Override
	public void triggerUpdateSensorInformation() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean drive2Exit() throws Exception {
		throw new RuntimeException("AbstractRobotDriver does not implement the method 'drive2Exit'");
	}

	@Override
	public float getEnergyConsumption() {
		return robotStartEnergy-robot.getBatteryLevel();
	}

	@Override
	public int getPathLength() {
		return robot.getOdometerReading();
	}
	
	private void move() {
		
	}

}
