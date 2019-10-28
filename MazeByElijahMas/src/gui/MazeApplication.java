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
	
	private static boolean robotEnabled=true;
	
	public static boolean getRobotEnabled() {
		return robotEnabled;
	}

	private SensorButtonPanel sensorButtons;

	/**
	 * Constructor
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
		
		JPanel master = new JPanel();
		master.setLayout(layout);
		
		StartPanel start = new StartPanel();
		start.setController(controller);
		master.add(start) ;
		
		
		//levelStrings.add(0,"(select one...)");
		
		JComboBox levelOptsBox =
			new JComboBox(IntStream.range(0,Constants.SKILL_X.length).mapToObj(Integer::valueOf).toArray());
		levelOptsBox.setName("Levels");
		
		JComboBox mazeOptsBox = new JComboBox(new String[] {"DFS", "Eller", "Prim"});//, "Kruskal"
		mazeOptsBox.setName("Mazes");
		
		JComboBox driverOptsBox = new JComboBox(new String[] {"(None)","Wizard", "WallFollower"});
		
		JPanel mazeOptsPanel = new JPanel();
		mazeOptsPanel.setName("mazes panel");
		mazeOptsPanel.add(new JLabel("Type:"));
		mazeOptsPanel.add(mazeOptsBox);
		
		JPanel levelOptsPanel = new JPanel();
		levelOptsPanel.add(new JLabel("Difficulty:"));
		levelOptsPanel.setName("levels panel");
		levelOptsPanel.add(levelOptsBox);
		
		JPanel driverOptsPanel = new JPanel();
		driverOptsPanel.add(new JLabel("Driver:"));
		driverOptsPanel.setName("driver panel");
		driverOptsPanel.add(driverOptsBox);
		
		master.add(mazeOptsPanel) ;
		master.add(levelOptsPanel) ;
		master.add(driverOptsPanel) ;
		
		start.setMazeBox(mazeOptsBox);
		start.setLevelBox(levelOptsBox);
		start.setDriverBox(driverOptsBox);
		start.setContainer(this);
		
		master.setBounds(0, 300, 400, 100);
		
		sensorButtons = new SensorButtonPanel();
		sensorButtons.setController(controller);
		sensorButtons.setBounds(0, 400, 400, sensorButtonsHeight);
		sensorButtons.setLayout(new FlowLayout());
		
		add(sensorButtons);
		System.out.println("playing buttons added");
		
		add(master);
		controller.getPanel().setSize(400, 400);
		add(controller.getPanel());
		controller.setOptionsPanel(master);
		controller.setSensorButtons(sensorButtons);
		//remove(master);
		
		revalidate();
		
		
		controller.setContainer(this);
		
		controller.start();
		
		
		
		
		/*
		mazeOptsBox.setSize(50, 50);
		
		levelOptsBox.setSize(50, 50);
		
		//controller.getPanel().setSize(100, 200);
		
		start.setSize(100, 100);
		
		
		//controller.getPanel().setFocusable(true);
		controller.getPanel().setVisible(true);
		
		JPanel jp = new JPanel();
		jp.setSize(50, 50);
		jp.setVisible(true);
		jp.setBackground(Color.cyan);
		jp.setLayout(layout);
		
		button.setBounds(150, 40, 100, 30);
		button.setSize(40,20);
		
		jp.add(mazeOptsBox);
		jp.add(levelOptsBox);
		
		controller.getPanel().add(button,0);
		
		master.add(jp);
		//master.add(controller.getPanel());
		//jp.add(start);
		
		//add(jp,0);
		add(start,1);
		
		button.setSize(40, 20);
		add(button,BorderLayout.NORTH);
		revalidate();
		
		System.out.printf("%s, %s\n",levelOptsBox.getSelectedItem(),mazeOptsBox.getSelectedItem());
		*/
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


class SensorButtonPanel extends JPanel {
	
	
	private Controller controller;

	public SensorButtonPanel() {
		setVisible(false);
		setEnabled(false);
		setFocusable(false);
		
		ActionListener a = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				if(source instanceof dButton) {//
					System.out.println("source: direction="+((dButton)source).getDirection());
					controller.communicateSensorTrigger(((dButton)source).getDirection());
				}
			}
			
		};
		
		for(Direction d: Direction.values()) {
			dButton button = new dButton(d);
			button.setFocusable(false);
			add(button);
			button.addActionListener(a);
		}
		
	}

	public void setController(Controller controller) {
		this.controller=controller;
	}
	
	
}

class dButton extends JButton {
	private Direction direction;
	
	
	public dButton(Direction d) {
		super(d.toString());
		direction=d;
	}

	public Direction getDirection() {
		return direction;
	}
}






class StartPanel extends JPanel implements ActionListener {
	
	JButton b;
	Controller controller;
	private JComboBox levelOptsBox;
	private JComboBox mazeOptsBox;
	private JComboBox driverOptsBox;
	private JFrame container;
	
	public StartPanel() {
		setLayout(new GridBagLayout());
		b = new JButton("Start");
		b.setBackground(Color.green);
		setBackground(Color.green);
		b.addActionListener(this);
		add(b);
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
		return b;
	}
	
	public void setLevelBox(JComboBox levelOptsBox) {
		this.levelOptsBox=levelOptsBox;
	}
	
	public void setMazeBox(JComboBox mazeOptsBox) {
		this.mazeOptsBox=mazeOptsBox;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("startButton: Action performed: "+e);
		//b.setEnabled(false);
		//setVisible(false);
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
		controller.switchFromTitleToGenerating((Integer)levelOptsBox.getSelectedItem());
		controller.setDriverString((String)driverOptsBox.getSelectedItem());
		
		container.requestFocusInWindow();
		
	}
}