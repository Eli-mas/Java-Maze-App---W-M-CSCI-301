package gui;

import gui.Constants.UserInput;
import gui.Robot.Direction;
import gui.Robot.Turn;

import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import generation.CardinalDirection;
import generation.Maze;
import generation.Order;
import generation.Order.Builder;

import gui.MazeApplication.MPanel;


/**
 * <p>Class handles the user interaction:
 *   <ul>
 *     <li> It implements an automaton with states for the different stages of the game.</li>
 *     <li> It has state-dependent behavior that controls the display and reacts to key board input from a user.</li> 
 *     <li> At this point user keyboard input is first dealt with a key listener (SimpleKeyListener)
 *          and then handed over to a Controller object by way of the keyDown method.</li>
 *   </ul>
 *
 * <p> The class is part of a state pattern. It has a state object to implement
 * state-dependent behavior.
 * The automaton currently has 4 states: <ol>
 * <li> StateTitle: the starting state where the user can pick the skill-level</li>
 * <li> StateGenerating: the state in which the factory computes the maze to play
 *      and the screen shows a progress bar.</li>
 * <li> StatePlaying: the state in which the user plays the game and
 *      the screen shows the first person view and the map view.</li>
 * <li> StateWinning: the finish screen that shows the winning message.
 *      The class provides a specific method for each possible state transition,
 *      for example switchFromTitleToGenerating contains code to start the maze
 *      generation.</li></ol>
 * </p>
 *
 * <p>This code is refactored code from Maze.java by Paul Falstad, 
 * www.falstad.com, Copyright (C) 1998, all rights reserved.
 * Paul Falstad granted permission to modify and use code for teaching purposes.
 * Refactored by Peter Kemper.</p>
 * 
 * <p>Controller holds the keyDown mechanism, but when a robot is enabled
 * it will defer to the robot with this method, in which case
 * the down button is ignored. The robot then instructs the Controller
 * to update the GUI upon completion of operations or errors.</p>
 * 
 * @author Peter Kemper
 */
public class Controller {
	
	/**
	 * suppress certain warnings from printing, used for testing
	 */
	public static boolean suppressWarnings=false;
	/**
	 * suppress certain updates from printing, used for testing
	 */
	public static boolean suppressUpdates=false;
	
	
	
	/**
	 * The game has a reservoir of 4 states: 
	 * <br>1: show the title screen, wait for user input for skill level
	 * <br>2: show the generating screen with the progress bar during 
	 * maze generation
	 * <br>3: show the playing screen, have the user or robot driver
	 * play the game
	 * <br>4: show the finish screen with the winning/loosing message
	 * 
	 * <br>The array entries are set in the constructor. 
	 * There is no mutator method.
	 */
	State[] states;
	
	/**
	 * The current state of the controller and the game.
	 * All state objects share the same interface and can be
	 * operated in the same way, although the behavior is 
	 * vastly different.
	 * currentState is never null and only updated by 
	 * switchFrom .. To .. methods.
	 */
	State currentState;
	
	/**
	 * The panel is used to draw on the screen for the UI.
	 * It can be set to null for dry-running the controller
	 * for testing purposes but otherwise panel is never null.
	 */
	MazePanel panel;
	
	public MazePanel getPanel() {
		return panel;
	}
	
	/**
	 * The filename is optional, may be null, and tells
	 * if a maze is loaded from this file and not generated.
	 */
	String fileName;
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * The builder algorithm to use for generating a maze.
	 */
	Order.Builder builder;
	
	public void setBuilder(Builder builder) {
		this.builder = builder; 
	}
	
	/**
	 * Specifies if the maze is perfect, i.e., it has
	 * no loops, which is guaranteed by the absence of 
	 * rooms and the way the generation algorithms work.
	 */
	boolean perfect;
	
	public void setPerfect(boolean isPerfect) {
		this.perfect = isPerfect; 
	}
	
	
	
	/**
	 * whether a robot will be instantiated
	 */
	private boolean robotEnabled;
	/**
	 * The robot that interacts with the controller
	 */
	Robot robot;
	/**
	 * @return the robot, may be null
	 */
	public Robot getRobot() {
		return robot;
	}
	
	/**
	 * Whether or not the robot is operated manually
	 */
	boolean manualRobotOperation=true;
	/**
	 * Starting energy level of the robot, used to measure energy consumption
	 */
	float initialRobotEnergyLevel;
	/**
	 * get energy consumed by robot from start to now
	 * @return robot's energy consumption
	 */
	public float getEnergyConsumedByRobotAtPresent() {
		return initialRobotEnergyLevel-robot.getBatteryLevel();
	}
	/**
	 * 
	 * @return starting energy level of the robot
	 */
	public float getInitialRobotEnergyLevel() {
		return initialRobotEnergyLevel;
	}
	
	/**
	 * The driver that interacts with the robot starting from P3
	 */
	RobotDriver driver;
	/**
	 * Used to communicate to Controller what driver should be used.
	 * If no driver to be set, this is null.
	 */
	String driverString="";
	
	public void setDriverString(String s) {
		driverString=s;
	}
	
	/**
	 * Sets the robot and robot driver
	 * @param robot a {@link Robot}
	 * @param robotdriver a {@link RobotDriver}
	 */
	public void setRobotAndDriver(Robot robot, RobotDriver robotdriver) {
		this.robot = robot;
		driver = robotdriver;
	}
	
	/**
	 * @return the driver, may be null
	 */
	public RobotDriver getDriver() {
		return driver;
	}
	
	
	
	
	
	/**
	 * Holds options components for selecting maze difficulty, maze type, and driver type
	 */
	private MPanel optsPanel;
	/**
	 * holds buttons that allow for triggering {@link RobotSensorTrigger} sensor failure threads
	 */
	private MPanel sensorButtons;
	/**
	 * set {@link #sensorButtons} field
	 * @param sensorButtons {@link MPanel} that holds the start button
	 */
	public void setSensorButtons(MPanel sensorButtons) {
		this.sensorButtons=sensorButtons;
	}
	/**
	 * 
	 * @return {@link #sensorButtons} object
	 */
	public MPanel getSensorButtons() {
		return sensorButtons;
	}
	
	
	
	
	/**
	 * Threads that induces sensor failure cycles by way of {@link RobotSensorTrigger} instances
	 */
	private HashMap<Direction,Thread> sensorThreads;
	/**
	 * default is to not initialize a robot
	 */
	public Controller() {
		init(false);
	}
	
	
	
	
	
	/**
	 * Constructor that allows for enabling robot
	 * @param enableRobot whether a robot should be enabled
	 */
	public Controller(boolean enableRobot) {
		init(enableRobot);
	}
	/**
	 * Set the {@link State} instances of the controller and other parameters
	 * used in maze generation.
	 * @param enableRobot whether a robot is to be initialized
	 */
	private void init(boolean enableRobot) {
		states = new State[4];
		states[0] = new StateTitle();
		states[1] = new StateGenerating();
		states[2] = new StatePlaying();
		states[3] = new StateWinning();
		currentState = states[0];
		panel = new MazePanel(); 
		fileName = null;
		builder = Order.Builder.DFS; // default
		perfect = false; // default
		robotEnabled = enableRobot? true: false;
	}
	
	/**
	 * Starts the controller and begins the game 
	 * with the title screen.
	 */
	public void start() { 
		System.out.println("controller has started");
		currentState = states[0]; // initial state is the title state
		currentState.setFileName(fileName); // can be null
		currentState.start(this, panel);
		fileName = null; // reset after use
	 }
	
	public void setOptionsPanel(MPanel optsPanel) {
		this.optsPanel = optsPanel;
	}
	   
	/**
	 * Switches the controller to the generating screen.
	 * Assumes that builder and perfect fields are already set
	 * with set methods if default settings are not ok.
	 * A maze is generated.
	 * @param skillLevel, 0 <= skillLevel, size of maze to be generated
	 */
	public void switchFromTitleToGenerating(int skillLevel) {
		currentState = states[1];
		currentState.setSkillLevel(skillLevel);
		currentState.setBuilder(builder); 
		currentState.setPerfect(perfect); 
		currentState.start(this, panel);
		optsPanel.setVisibleEnabled(false);//setOptsPanelVisibleEnabled(false);
	}
	
	/**
	 * Switches the controller to the generating screen and
	 * loads maze from file.
	 * @param filename gives file to load maze from
	 */
	public void switchFromTitleToGenerating(String filename) {
		currentState = states[1];
		currentState.setFileName(filename);
		currentState.start(this, panel);
		
		optsPanel.setVisibleEnabled(false);

	}
	
	/**
	 * <p>Switches the controller to the playing screen.
	 * This is where the user or a robot can navigate through
	 * the maze and play the game.</p>
	 * 
	 * <p> New for P3: also sets the robot to a fully initialized state</p>.
	 * @param config contains a maze to play
	 */
	public void switchFromGeneratingToPlaying(Maze config) {
		currentState = states[2];
		currentState.setMazeConfiguration(config);
		//System.out.println("Controller: calling robot.setMaze");
		
		
		if(robotEnabled){
			RobotDriver new_driver=null;
			switch(driverString) {
				case "Wizard":
					new_driver=new Wizard();
					break;
				case "WallFollower":
					new_driver = new WallFollower();
					break;
				default: break;
			}
			Robot new_robot = new BasicRobot();
			setRobotAndDriver(new_robot,new_driver);
			//setupRobot()
			// can't do this here--
			// StatePlaying has not initialized the current position/direction
		}
		currentState.start(this, panel);
	}
	
	/**
	 * Switches the controller to the final screen
	 * @param pathLength gives the length of the path
	 */
	public void switchFromPlayingToWinning(int pathLength) {
		currentState = states[3];
		currentState.setPathLength(pathLength);
		currentState.start(this, panel);
	}
	
	/**
	 * Switches the controller to the initial screen.
	 */
	public void switchToTitle() {
		optsPanel.setVisibleEnabled(true);
		
		sensorButtons.setVisibleEnabled(false);
		
		currentState = states[0];
		currentState.start(this, panel);
	}

	
	
	
	/**
	 * Calls upon {@link #sensorThreads} to begin the sensor failure cycle
	 * for a given sensor.
	 * @param direction the direction of the sensor for which the failure cycle should begin
	 */
	public void communicateSensorTrigger(Direction direction) {
		if(!(currentState instanceof StatePlaying)) return;
		
		Thread t = sensorThreads.get(direction);
		if(!t.isAlive()) {
			t.start();
		}
	}
	
	/**
	 * Set the initial robot energy level.
	 * Call this only before the robot starts operations.
	 * @param amount energy level
	 */
	public void setInitialRobotEnergyLevel(float amount) {
		initialRobotEnergyLevel=amount;
		robot.setBatteryLevel(amount);
	}
	
	/**
	 * Sets the robot and driver up and initializes {@link #sensorThreads} if required.
	 * Called in StatePlaying when current position and direction have been initialized.
	 */
	protected void setupRobot() {
		robot.setMaze(this);
		setInitialRobotEnergyLevel(robot.getBatteryLevel());
		
		RobotDriver driver = getDriver();
		
		if(null != driver) {
			driver.setRobot(robot);
			if(driver instanceof Wizard) ((Wizard)driver).setMaze(getMazeConfiguration());
			
			if(null!=panel) {
				sensorThreads = new HashMap<Direction, Thread>(4);
				
				for(Direction d: Direction.values()) {
					Thread t = new Thread(new RobotSensorTrigger(d,driver,robot,this));
					sensorThreads.put(d, t);
				}
			}
		}
		
	}
	
	
	
	
	/**
	 * <p>Method incorporates all reactions to keyboard input in original code. 
	 * The simple key listener calls this method to communicate input.</p>
	 * 
	 * <p>New for P3:
	 * If a robot is established and the currentState is StatePlaying,
	 * commands that involve operating the robot
	 * are delegated to the robot itself, and the Down key is ignored;
	 * keys that affect the graphical interface are handled normally</p>
	 * 
	 * @param key key input value
	 * @param value integer, not used in playing state
	 */
	public boolean keyDown(UserInput key, int value) {
		// delegated to state object
		return currentState.keyDown(key, value);
	}
	
	/**
	 * Receives keyboard input when a robot is enabled before
	 * {@link currentState#keyDown(UserInput, int) keyDown}. Redirects to
	 * the {@link keyDown} method of the current state when no driver is enabled.
	 * 
	 * If a driver is active, then the only valid keyboard input
	 * concerns adjustment of the maze map visibility.
	 * Other keyboard input causes the driver to interrupt prematurely.
	 * 
	 * @param key key input value
	 * @param value integer, not used in playing state
	 * @return true for valid input, false otherwise
	 */
	public boolean keyDownRobot(UserInput key, int value) {
		if(null!=driver) {
			switch(key) {
			case ToggleLocalMap: case ToggleFullMap: case ToggleSolution:
			case ZoomIn: case ZoomOut:
				return currentState.keyDown(key, value);
			default:
				driver.interrupt();
				switchToTitle();
				return false;
			}
		}
		
		else{
			switch(key) {
				case Left:
					robot.rotate(Turn.LEFT);
					break;
				case Right:
					robot.rotate(Turn.RIGHT);
					break;
				case Up:
					robot.move(1, manualRobotOperation);
					break;
				case Jump:
					try {
						robot.jump();
					} catch (Exception e) {
						// e.printStackTrace();
						System.out.println("invalid jump attempted: game ending");
					}
					break;
				case Down:
					return false;
				default:
					return currentState.keyDown(key, value);
			}
			return true;
		}
	}
	
	
	
	
	/**
	 * Turns of graphics to dry-run controller for testing purposes.
	 * This is irreversible. 
	 */
	public void turnOffGraphics() {
		panel = null;
	}
	
	
	
	
	/**
	 * Provides access to the maze configuration. 
	 * This is needed for a robot to be able to recognize walls
	 * for the distance to walls calculation, to see if it 
	 * is in a room or at the exit. 
	 * Note that the current position is stored by the 
	 * controller. The maze itself is not changed during
	 * the game.
	 * This method should only be called in the playing state.
	 * @return the MazeConfiguration
	 */
	public Maze getMazeConfiguration() {
		return ((StatePlaying)states[2]).getMazeConfiguration();
	}
	/**
	 * Provides access to the current position.
	 * The controller keeps track of the current position
	 * while the maze holds information about walls.
	 * This method should only be called in the playing state.
	 * @return the current position as [x,y] coordinates, 
	 * 0 <= x < width, 0 <= y < height
	 */
	public int[] getCurrentPosition() {
		return ((StatePlaying)states[2]).getCurrentPosition();
	}
	/**
	 * Provides access to the current direction.
	 * The controller keeps track of the current position
	 * and direction while the maze holds information about walls.
	 * This method should only be called in the playing state.
	 * @return the current direction
	 */
	public CardinalDirection getCurrentDirection() {
		return ((StatePlaying)states[2]).getCurrentDirection();
	}
}
