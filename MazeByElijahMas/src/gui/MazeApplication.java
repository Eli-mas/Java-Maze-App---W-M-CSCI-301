/**
 * 
 */
package gui;

import generation.Order;
import gui.Robot.Direction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * This class is a wrapper class to startup the Maze game as a Java application
 * 
 * This code is refactored code from Maze.java by Paul Falstad, www.falstad.com, Copyright (C) 1998, all rights reserved
 * Paul Falstad granted permission to modify and use code for teaching purposes.
 * Refactored by Peter Kemper
 * 
 * TODO: use logger for output instead of Sys.out
 */
public class MazeApplication extends JFrame {

	// not used, just to make the compiler, static code checker happy
	private static final long serialVersionUID = 1L;
	
	/**
	 * whether a robot will be initialized
	 */
	private static boolean robotEnabled=true;
	
	public static boolean getRobotEnabled() {
		return robotEnabled;
	}
	
	/**
	 * {@link SensorButtonPanel} (subclass of JPanel}
	 * that holds buttons to trigger sensor failure/repair cycles;
	 * see {@link Controller}.
	 */
	private SensorButtonPanel sensorButtons;

	/**
	 * Default is to load no file.
	 */
	public MazeApplication() {
		init(null);
	}

	/**
	 * Constructor that loads a maze from a given file or uses a particular method to generate a maze
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
     * or a filename that stores an already generated maze that is then loaded, or can be null
	 */
	public MazeApplication(String parameter) {
		init(parameter);
	}

	/**
	 * Instantiates a controller with settings according to the given parameter.
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
	 * or a filename that contains a generated maze that is then loaded,
	 * or can be null
	 * @return the newly instantiated and configured controller
	 */
	 Controller createController(String parameter) {
	    // need to instantiate a controller to return as a result in any case
	    Controller result = new Controller(robotEnabled) ;
	    String msg = null; // message for feedback
	    // Case 1: no input
	    if (parameter == null) {
	        msg = "MazeApplication: maze will be generated with a randomized algorithm."; 
	    }
	    // Case 2: Prim
	    else if ("Prim".equalsIgnoreCase(parameter))
	    {
	        msg = "MazeApplication: generating random maze with Prim's algorithm.";
	        result.setBuilder(Order.Builder.Prim);
	    }
	    // Case 3 a and b: Eller, Kruskal or some other generation algorithm
	    else if ("Kruskal".equalsIgnoreCase(parameter))
	    {
	    	// TODO: for P2 assignment, please add code to set the builder accordingly
	        throw new RuntimeException("Don't know anybody named Kruskal ...");
	    }
	    else if ("Eller".equalsIgnoreCase(parameter))
	    {
	        msg = "MazeApplication: generating random maze with Eller's algorithm.";
	        result.setBuilder(Order.Builder.Eller);
	    }
	    // Case 4: a file
	    else {
	        File f = new File(parameter) ;
	        if (f.exists() && f.canRead())
	        {
	            msg = "MazeApplication: loading maze from file: " + parameter;
	            result.setFileName(parameter);
	            return result;
	        }
	        else {
	            // None of the predefined strings and not a filename either: 
	            msg = "MazeApplication: unknown parameter value: " + parameter + " ignored, operating in default mode.";
	        }
	    }
	    // controller instanted and attributes set according to given input parameter
	    // output message and return controller
	    System.out.println(msg);
	    return result;
	}

	/**
	 * Initializes some internals and puts the game on display.
	 * 
	 * Creates panels for holding various components of game,
	 * including options to control maze generation on title screen
	 * and sensor buttons on playing screen.
	 * 
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
     * or a filename that contains a generated maze that is then loaded, or can be null
	 */
	private void init(String parameter) {
	    // instantiate a game controller and add it to the JFrame
	    Controller controller = createController(parameter);
	    
	    setLayout(new BorderLayout());
	    
	    
	    int sensorButtonsHeight = 30;
	    
	    
		pack();
		// instantiate a key listener that feeds keyboard input into the controller
		// and add it to the JFrame
		KeyListener kl = new SimpleKeyListener(this, controller) ;
		addKeyListener(kl) ;
		// set the frame to a fixed size for its width and height and put it on display
		setSize(400, 400+sensorButtonsHeight*2) ;
		setVisible(true) ;
		// focus should be on the JFrame of the MazeApplication and not on the maze panel
		// such that the SimpleKeyListener kl is used
		setFocusable(true) ;
		// start the game, hand over control to the game controller
		
		GridLayout layout = new GridLayout(1,4);
		
		// master holds the panels that control maze generation/traversal parameters
		// (maze generation algorithm, difficulty, driver algorithm)
		JPanel optsPanel = new JPanel();
		//arrange in a grid layout
		optsPanel.setLayout(layout);
		
		// panel to hold the start button
		StartPanel start = new StartPanel();
		// start button requires reference to controller
		start.setController(controller);
		
		
		
		// box to select difficulty level
		JComboBox levelOptsBox =
			new JComboBox(IntStream.range(0,Constants.SKILL_X.length).mapToObj(Integer::valueOf).toArray());
		levelOptsBox.setName("Levels");
		
		// box to select maze generation algorithm
		JComboBox mazeOptsBox = new JComboBox(new String[] {"DFS", "Eller", "Prim"});//, "Kruskal"
		mazeOptsBox.setName("Mazes");
		
		// box to select driver algorithm
		JComboBox driverOptsBox = new JComboBox(new String[] {"(None)","Wizard", "WallFollower"});
		
		// panel to hold box for maze generation options and provide descriptive text
		JPanel mazeOptsPanel = new JPanel();
		mazeOptsPanel.setName("mazes panel");
		mazeOptsPanel.add(new JLabel("Type:"));
		mazeOptsPanel.add(mazeOptsBox);
		
		// panel to hold box for difficulty options and provide descriptive text
		JPanel levelOptsPanel = new JPanel();
		levelOptsPanel.add(new JLabel("Difficulty:"));
		levelOptsPanel.setName("levels panel");
		levelOptsPanel.add(levelOptsBox);
		
		// panel to hold box for driver options and provide descriptive text
		JPanel driverOptsPanel = new JPanel();
		driverOptsPanel.add(new JLabel("Driver:"));
		driverOptsPanel.setName("driver panel");
		driverOptsPanel.add(driverOptsBox);
		
		// add everything to master
		optsPanel.add(start) ;
		optsPanel.add(mazeOptsPanel) ;
		optsPanel.add(levelOptsPanel) ;
		optsPanel.add(driverOptsPanel) ;
		
		// link boxes to start so that it can transfer data from these boxes
		// to controller
		start.setMazeBox(mazeOptsBox);
		start.setLevelBox(levelOptsBox);
		start.setDriverBox(driverOptsBox);
		start.setContainer(this);
		
		// master options panel occupies bottom of screen
		optsPanel.setBounds(0, 300, 400, 100);
		
		sensorButtons = new SensorButtonPanel();
		sensorButtons.setController(controller);
		sensorButtons.setBounds(0, 400, 400, sensorButtonsHeight);
		sensorButtons.setLayout(new FlowLayout());
		
		add(sensorButtons);
		System.out.println("playing buttons added");
		
		add(optsPanel);
		
		//leave room on screen for sensorButtons, avoid complete overlap
		controller.getPanel().setSize(400, 400);
		
		add(controller.getPanel());
		controller.setOptionsPanel(optsPanel);
		controller.setSensorButtons(sensorButtons);
		
		revalidate();
		
		
		controller.setContainer(this);
		
		//everything is set, get the game started
		controller.start();
	}
	
	public void actionPerformed(ActionEvent e){
		System.out.println("action performed: "+e);
	}
	
	/**
	 * Main method to launch Maze game as a java application.
	 * The application can be operated in three ways. 
	 * 1) The intended normal operation is to provide no parameters
	 * and the maze will be generated by a randomized DFS algorithm (default). 
	 * 2) If a filename is given that contains a maze stored in xml format. 
	 * The maze will be loaded from that file. 
	 * This option is useful during development to test with a particular maze.
	 * 3) A predefined constant string is given to select a maze
	 * generation algorithm, currently supported is "Prim".
	 * @param args is optional, first string can be a fixed constant like Prim or
	 * the name of a file that stores a maze in XML format
	 */
	public static void main(String[] args) {
	    JFrame app ; 
		switch (args.length) {
			case 1 :
				app = new MazeApplication(args[0]);
				break ;
			case 0 : default :
				app = new MazeApplication() ;
				break ;
		}
		app.repaint() ;
	}

}


/**
 * 
 * @author Elijah Mas
 * 
 * SensorButtonPanel holds buttons to trigger cyclical
 * failure and repair of a robot's sensor
 * when operated by a driver.
 *
 */
class SensorButtonPanel extends JPanel {
	
	
	private Controller controller;

	public SensorButtonPanel() {
		//not active to start; activated in playing state
		setVisible(false);
		setEnabled(false);
		setFocusable(false);
		
		ActionListener a = new ActionListener() {
			// use anonymous class to allow the button press
			// to signal the controller to trigger the failure/repair cycle
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// verify that the event source was a button press
				Object source = e.getSource();
				if(source instanceof dButton) {
					System.out.println("source: direction="+((dButton)source).getDirection());
					controller.communicateSensorTrigger(((dButton)source).getDirection());
				}
			}
			
		};
		
		for(Direction d: Direction.values()) {
			dButton button = new dButton(d);
			// remove focusability to prevent keyboard input
			// from losing effect in playing state
			button.setFocusable(false);
			// add button to SensorButtonPanel instance
			add(button);
			// the action listener can extract direction from the button,
			// so each button gets the same action listener
			button.addActionListener(a);
		}
		
	}

	public void setController(Controller controller) {
		this.controller=controller;
	}
	
	
}

/**
 * Extension of JButton that provides a direction field
 * that can be accessed externally.
 * @author Elijah Mas
 *
 */
class dButton extends JButton {
	private Direction direction;
	
	
	public dButton(Direction d) {
		//button's identifier is the direction name
		super(d.toString());
		direction=d;
	}

	public Direction getDirection() {
		return direction;
	}
}






/**
 * StartPanel holds four components:
 * (I) a panel with a start button;
 * (II) a panel with an option to select a maze generation algorithm
 * (III) a panel with an option to select maze difficulty level
 * (IV) a panel with an option to select a driver algorithm
 * @author ElijahMas
 *
 */
class StartPanel extends JPanel implements ActionListener {
	
	/**
	 * button that starts the maze generation
	 */
	JButton startButton;
	
	/**
	 * link to a {@link Controller}
	 */
	Controller controller;
	
	/**
	 * box for level selection
	 */
	private JComboBox levelOptsBox;
	
	
	/**
	 * box for maze generation algorithm selection
	 */
	private JComboBox mazeOptsBox;
	
	
	/**
	 * box for driver algorithm selection
	 */
	private JComboBox driverOptsBox;
	
	
	/**
	 * reference to JFrame that holds everything
	 */
	private JFrame container;
	
	/**
	 * Set the start button; other panels are received separately.
	 */
	public StartPanel() {
		setLayout(new GridBagLayout()); //centers the button (approximately)
		startButton = new JButton("Start");
		startButton.setBackground(Color.green);
		setBackground(Color.green);
		startButton.addActionListener(this);
		add(startButton);
		setName("Start");
	}
	
	public void setContainer(MazeApplication container) {
		this.container=container;
	}

	public void setDriverBox(JComboBox driverOptsBox) {
		this.driverOptsBox=driverOptsBox;
	}

	public void setController(Controller controller) {
		this.controller=controller;
	}
	
	public JButton getButton() {
		return startButton;
	}
	
	public void setLevelBox(JComboBox levelOptsBox) {
		this.levelOptsBox=levelOptsBox;
	}
	
	public void setMazeBox(JComboBox mazeOptsBox) {
		this.mazeOptsBox=mazeOptsBox;
	}

	/**
	 * Receive information from other panels
	 * and signal the controller to start maze generation.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		//System.out.println("startButton: Action performed: "+e);
		
		String mazeType = (String)mazeOptsBox.getSelectedItem();
		switch(mazeType) {
			case "Eller":
				controller.setBuilder(Order.Builder.Eller);
				break;
			//case "Kruskal": controller.setBuilder(Order.Builder.Kruskal); break;
			case "Prim":
				controller.setBuilder(Order.Builder.Prim);
				break;
			case "DFS":
				controller.setBuilder(Order.Builder.DFS);
				break;
			default: break;
		}
		
		controller.setDriverString((String)driverOptsBox.getSelectedItem());
		
		// everything set, generate maze
		controller.switchFromTitleToGenerating((Integer)levelOptsBox.getSelectedItem());
		
		// don't lose keyboard focus from main application
		container.requestFocusInWindow();
		
	}
}