package gui;

import comp.MazeMath;
import gui.Robot.Direction;

public class RobotSensorTrigger implements Runnable{
	
	Direction direction;
	RobotDriver driver;
	Robot robot;
	public static int deltaT = 3000;
	
	public RobotSensorTrigger(Direction direction, RobotDriver driver, Robot robot) {
		this.direction=direction;
		this.driver=driver;
		this.robot=robot;
	}
	
	public void start() {
		
	}
	
	@Override
	public void run() {
		while(null!=MazeMath.getRobotPosition(robot) && !robot.hasStopped()) {
			boolean sensorState = robot.hasOperationalSensor(direction);
			if(sensorState) {
				robot.triggerSensorFailure(direction);
				System.out.println("failing sensor in direction "+direction);
			}
			else {
				robot.repairFailedSensor(direction);
				System.out.println("repairing sensor in direction "+direction);
			}
			
			driver.triggerUpdateSensorInformation();
			
			try {
				Thread.sleep(deltaT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}