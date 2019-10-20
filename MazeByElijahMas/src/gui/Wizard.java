package gui;

import comp.ExtendedList;
import comp.RobotOperation;
import comp.RobotOperationTracker;
import generation.Distance;
import generation.Maze;

public class Wizard implements RobotDriver {
	
	Robot robot;
	int width, height;
	Distance distance;
	
	
	
	
	
	
	public Wizard() {
		
	}
	
	public Wizard(Robot robot) {
		setRobot(robot);
	}
	
	@Override
	public void setRobot(Robot r) {
		robot=r;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public float getEnergyConsumption() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPathLength() {
		// TODO Auto-generated method stub
		return 0;
	}

}
