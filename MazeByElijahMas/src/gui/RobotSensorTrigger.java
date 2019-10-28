package gui;

import comp.MazeMath;
import gui.Robot.Direction;

public class RobotSensorTrigger implements Runnable{
	
	Direction direction;
	RobotDriver driver;
	Robot robot;
	Controller controller;
	public static int deltaT = 3000;
	
	public RobotSensorTrigger(Direction direction, RobotDriver driver, Robot robot, Controller controller) {
		this.direction=direction;
		this.driver=driver;
		this.robot=robot;
		this.controller=controller;
	}
	
	public void start() {
		
	}
	
	@Override
	public void run() {
		while(null!=MazeMath.getRobotPosition(robot) && !robot.hasStopped() && (controller.currentState instanceof StatePlaying)) {
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