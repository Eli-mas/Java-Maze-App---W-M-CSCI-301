package gui;

import java.util.Arrays;
import java.util.HashMap;

import generation.CardinalDirection;
import generation.Floorplan;
import generation.Distance;
import generation.Maze;

public class BasicRobot implements Robot {

	private Controller control;
	
	private float batteryLevel;
	private int odometerReading;
	private HashMap<Direction,Boolean> sensorFunctionalFlags;
	private Maze maze;
	private Floorplan floorplan;
	private Distance distance;
	private int[][] dists;
	private boolean roomSensorIsPresent;
	boolean initialized;
	
	final static float energyUsedForJump=-50;
	final static float energyUsedForMove=-5;
	final static float energyUsedForRotation=-3;
	final static float energyUsedForDistanceSensing=-1;
	
	public BasicRobot() {}

	@Override
	public void setMaze(Controller controller) {
		control=controller;
		instantiateFields();
		System.out.println("robot has set controller");
	}
	
	
	@Override
	public int[] getCurrentPosition() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CardinalDirection getCurrentDirection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getBatteryLevel() {
		return batteryLevel;
	}

	@Override
	public void setBatteryLevel(float level) {
		batteryLevel=level;
	}

	@Override
	public int getOdometerReading() {
		return odometerReading;
	}

	@Override
	public void resetOdometer() {
		odometerReading=0;
	}

	@Override
	public float getEnergyForFullRotation() {
		return 12f;
	}

	@Override
	public float getEnergyForStepForward() {
		return 5f;
	}

	@Override
	public boolean isAtExit() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canSeeThroughTheExitIntoEternity(Direction direction) throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInsideRoom() throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasRoomSensor() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasStopped() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int distanceToObstacle(Direction direction) throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasOperationalSensor(Direction direction) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void triggerSensorFailure(Direction direction) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean repairFailedSensor(Direction direction) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void rotate(Turn turn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(int distance, boolean manual) {
		// TODO change position, account for crash
		changeEnergyLevel(energyUsedForMove);
	}

	@Override
	public void jump() throws Exception {
		// TODO Auto-generated method stub

	}
	
	
	
	
	
	
	
	
	private void changeEnergyLevel(float amount) {
		setBatteryLevel(getBatteryLevel()+amount);
	}
	
	private boolean hasDirectionalSensor(Direction direction) {
		return sensorFunctionalFlags.containsKey(direction);
	}
	
	private int getObstacleDistance(Direction d) {
		return 0;
	}
	
	public Controller getController() {
		return control;
	}
	
	protected void instantiateFields() {
		batteryLevel=3000;
		odometerReading=0;
		sensorFunctionalFlags = new HashMap<Direction,Boolean>();
		for(Direction d: Direction.values()) sensorFunctionalFlags.put(d,true);
		roomSensorIsPresent=true;
		maze=control.getMazeConfiguration();
		floorplan=maze.getFloorplan();
		distance=maze.getMazedists();
		dists=distance.getAllDistanceValues();
		initialized=true;
		System.out.println("BasicRobot: instantiateFields completed");
		System.out.printf("BasicRobot: maze dimensions: %d,%d\n",maze.getWidth(),maze.getHeight());
	}
	

}

