package gui;

import comp.MazeMath;
import gui.Robot.Direction;

/**
 * RobotSensorTrigger operates a cyclical process
 * of triggering sensor failures and reparations.
 * 
 * It does this by repeatedly evaluating whether or not
 * the application is still in the playing state.
 * While so, it repeatedly changes the operational
 * state of a sensor, then sleeps its thread for a fixed time interval.
 * 
 * 
 * @author Elijah Mas
 *
 */
public class RobotSensorTrigger implements Runnable{
	
	/**
	 * the direction of the sensor to affect
	 */
	Direction direction;
	
	/**
	 * reference to a {@link RobotDriver}
	 */
	RobotDriver driver;
	
	/**
	 * reference to a {@link Robot}
	 */
	Robot robot;
	
	/**
	 * reference to a {@link Controller}
	 */
	Controller controller;
	
	/**
	 * time interval in milliseconds between sensor failure/repair
	 */
	public static int deltaT = 3000;
	
	public RobotSensorTrigger(Direction direction, RobotDriver driver, Robot robot, Controller controller) {
		this.direction=direction;
		this.driver=driver;
		this.robot=robot;
		this.controller=controller;
	}
	
	/*public void start() {
		
	}*/
	
	/**
	 * Checks to see if maze is in playing state and robot is operational.
	 * If so, change the state of the sensor.
	 * Then sleep the thread for the time {@link #deltaT}.
	 */
	@Override
	public void run() {
		while(null!=MazeMath.getRobotPosition(robot) && !robot.hasStopped() && (controller.currentState instanceof StatePlaying)) {
			boolean sensorState = robot.hasOperationalSensor(direction);
			if(sensorState) {// if operational, set to fail
				robot.triggerSensorFailure(direction);
				System.out.println("failing sensor in direction "+direction);
			}
			else { // if inoperational, set to repair
				robot.repairFailedSensor(direction);
				System.out.println("repairing sensor in direction "+direction);
			}
			
			//let the driver know that something changed
			driver.triggerUpdateSensorInformation();
			
			// wait for some interval to repeat
			try {
				Thread.sleep(deltaT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}