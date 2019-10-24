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
public abstract class AbstractRobotDriver implements RobotDriver {
	
	Robot robot;
	int width, height;
	Distance distance;
	float robotStartEnergy;
	Controller control;
	
	
	
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
	
	/**
	 * Rotate the {@link #robot} until it is facing the input direction.
	 * @param d a {@link Direction} value
	 */
	private void face(Direction d) {
		Turn turn = MazeMath.toTurn(d);
		rotate(turn);
	}
	
	/**
	 * Rotate the {@link #robot} until it is facing the input cardinal direction.
	 * @param cd a {@link CardinalDirection} value
	 */
	private void face(CardinalDirection cd) {
		Turn turn = MazeMath.toTurn(cd, robot.getCurrentDirection());
		rotate(turn);
	}
	
	private boolean[] getFunctionalSensors() {
		boolean[] functional = new boolean[4];
		int pos=0;
		for(Direction d: MazeMath.ForwardRightBackwardLeft)
			functional[pos++]=robot.hasOperationalSensor(d);
		
		return functional;
	}
	
	private Direction getClosestFunctionalSensor(Direction d) {
		boolean[] functional = getFunctionalSensors();
		int index = MazeMath.getDirectionIndex(d);
		
		if(functional[index]) return d;
		
		int[] indices = new int[] {1,-1,2};
		for(int i: indices) {
			if(functional[Math.floorMod(index+i,4)]) return MazeMath.getFrom(d, i);
		}
		
		return null;
	}
	
	private Integer getDistance(Direction directionFacing) {
		Direction directionFunctional = getClosestFunctionalSensor(directionFacing);
		if(null==directionFunctional) return null;
		
		int dist = MazeMath
					.ForwardRightBackwardLeft
					.getDistanceFromTo(directionFacing, directionFunctional);
		
		faceRobot(directionFunctional);
		
		try{
			return robot.distanceToObstacle(Direction.FORWARD);
		}
		catch (UnsupportedOperationException e) {
			return null;
		}
	}
	
	private void walkTo(Direction d) {
		
	}
	
	private void faceRobot(Direction d) {
		Turn turn = MazeMath.toTurn(d);
		robot.rotate(turn);
	}
	
	private void rotate(Turn turn) {
		robot.rotate(turn);
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
