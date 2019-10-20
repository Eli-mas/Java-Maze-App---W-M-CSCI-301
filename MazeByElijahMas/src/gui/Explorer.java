package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import comp.ExtendedList;
import comp.MazeMath;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;
import gui.Constants;

public class Explorer implements RobotDriver {
	
	private int[][] visitCounts;
	private int[] currentPosition;
	private Robot robot;
	private float robotStartEnergy;
	
	private int[] dimensions;
	
	public Explorer() {
	}
	
	@Override
	public void setRobot(Robot r) {
		this.robot=r;
	}

	@Override
	public void setDimensions(int width, int height) {
		dimensions= new int[]{width,height};
	}

	@Override
	public void setDistance(Distance distance) {
		System.out.println("warning: the Explorer class does not support the setDistance method");
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
		return robotStartEnergy-robot.getBatteryLevel();
	}

	@Override
	public int getPathLength() {
		return robot.getOdometerReading();
	}
	
	

}
