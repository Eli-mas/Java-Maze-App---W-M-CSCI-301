package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gui.Constants.StateGUI;

/**
 * Implements the screens that are displayed whenever the game is not in 
 * the playing state. The screens shown are the title screen, 
 * the generating screen with the progress bar during maze generation,
 * and the final screen when the game finishes.
 * The only one that is not simple and not covered by this class
 * is the one that shows the first person view of the maze game
 * and the map of the maze when the user really navigates inside the maze.
 * 
 * @author Peter Kemper
 *
 */
public class SimpleScreens {

	private StateGenerating controllerState; // only used for generating screen
	
	/**
	 * Constructor
	 * @param c should provide a reference to the generating state, can be null otherwise
	 */
	public SimpleScreens(StateGenerating c) {
		super() ;
		controllerState = c ;
	}
	
	public Controller controller;
	
	/**
	 * Draws the title screen, screen content is hard coded
	 * @param panel holds the graphics for the off-screen image
	 * @param filename is a string put on display for the file
	 * that contains the maze, can be null
	 */
	public void redrawTitle(MazePanel panel, String filename) {
		Graphics g = panel.getBufferGraphics() ;
		if (null == g) {
			System.out.println("MazeView.redrawTitle: can't get graphics object to draw on, skipping redraw operation") ;
		}
		else {
			redrawTitle(g,filename);
		}
	}
	/**
	 * Helper method for redraw to draw the title screen, screen is hard coded
	 * @param  gc graphics is the off-screen image, can not be null
	 * @param filename is a string put on display for the file
	 * that contains the maze, can be null
	 */
	private void redrawTitle(Graphics gc, String filename) {
		// make a list and add title/attribution
		ArrayList<Object[]> fontProp = new ArrayList<Object[]>(Arrays.asList(
			new Object[] {"MAZE", 100, Color.red, largeBannerFont},
			new Object[] {"by Paul Falstad\nwww.falstad.com", 160, Color.blue, smallBannerFont}
		));
		
		
		// state source file if present,
		// otherwise prompt user for input
		if (null == filename) {
			// default instructions
			fontProp.add(new Object[] {"To start,\nselect maze parameters\nand push the start button.", 270, Color.black, new Font("Times Roman",Font.BOLD,20)});
			//fontProp.add(new Object[] {"(Press a number from 0 to 9\nor a letter from A to F),", 320, null, smallBannerFont});
		}
		else // message if maze is loaded from file
			fontProp.add(new Object[] {
					"Loading maze from file:\n"+filename, 300, Color.black, smallBannerFont
			});
		
		
		
		drawStateScreen_fromList(gc, Color.white, fontProp);
		
	}

	/*
	public void redraw(Graphics gc, StateGUI state, int px, int py, int view_dx,
			int view_dy, int walk_step, int view_offset, RangeSet rset, int ang) {
		//dbg("redraw") ;
		switch (state) {
		case STATE_TITLE:
			redrawTitle(gc,null);
			break;
		case STATE_GENERATING:
			redrawGenerating(gc);
			break;
		case STATE_PLAY:
			// skip this one
			break;
		case STATE_FINISH:
			redrawFinish(gc);
			break;
		}
	}
	*/
	private void dbg(String str) {
		System.out.println("MazeView:" + str);
	}
	/**
	 * Draws the finish screen, screen content is hard coded
	 * @param panel holds the graphics for the off-screen image
	 */
	void redrawFinish(MazePanel panel) {
		Graphics g = panel.getBufferGraphics() ;
		if (null == g) {
			System.out.println("MazeView.redrawFinish: can't get graphics object to draw on, skipping redraw operation") ;
		}
		else {
			redrawFinish(g);
		}
	}
	
	private String getFinishingRobotString() {
		int odometer=controller.getRobot().getOdometerReading();
		if(controller.getDriver()!=null) {
			System.out.println("Simple screens: verifying odometer: "+controller.getDriver().getPathLength()+" "+odometer);
			assert controller.getDriver().getPathLength() == odometer : controller.getDriver().getPathLength()+" "+odometer;
			System.out.println("Simple screens: verifying energy: "+controller.getDriver().getEnergyConsumption()+" "+(int)controller.getEnergyConsumedByRobotAtPresent());
			assert controller.getDriver().getEnergyConsumption() == (int)controller.getEnergyConsumedByRobotAtPresent();
		}
		int maxDist=controller.getMazeConfiguration().getMazedists().getMaxDistance();
		
		if(controller.getRobot().hasStopped())
			return controller.getRobot().getFailureMessage();
		else
			return String.format(
						"Energy used: %d/%d\npathLength efficiency: %.1f%% (%d/%d)",
						(int)controller.getEnergyConsumedByRobotAtPresent(),
						(int)controller.getInitialRobotEnergyLevel(),
						100*(double)maxDist/(double)odometer,
						maxDist,
						odometer
					);
	}
	
	/**
	 * <p> Prepare the ending screen in the case where the game is won--
	 * i.e. the maze has been exited. </p>
	 * 
	 * <p> There are two ways to arrive at this state:
	 * either a robot is enabled and exits the maze,
	 * or the player uses no robot and exits the maze.
	 * If a robot is present, certain robot metrics are displayed,
	 * obtained from
	 * {@link #getFinishingRobotString() getFinishingRobotString}. </p>
	 * 
	 * @param gc the graphics panel on which to draw
	 */
	private void redrawFinishWinning(Graphics gc) {
		// if no robot is enabled, pass null,
		// which ensures that no robot message will be drawn
		Object[] robotInput = (controller.getRobot()==null) ? null :
			new Object[]{getFinishingRobotString(),250,Color.orange,
					new Font("TimesRoman", Font.BOLD, 24)};
		
		// add all the messages
		drawStateScreen_fromVarargs(gc,
			Color.blue,
			new Object[]{"You won!", 100, Color.yellow, largeBannerFont},
			new Object[]{"Congratulations!", 160, Color.cyan, smallBannerFont},
			new Object[]{"Hit any key to restart", 300, Color.white},
			robotInput
		);
	}
	
	/**
	 * Prepare the ending screen in the case where the game is lost--
	 * i.e. the robot has stopped before exiting. Alerts the user
	 * that the game has ended and provides a reason why the robot stopped.
	 * @param gc the graphics panel on which to draw
	 */
	private void redrawFinishLosing(Graphics gc) {
		// no need to check for robotEnabled here,
		// the only way to lose is when a robot is enabled
		drawStateScreen_fromVarargs(gc,Color.blue,
			new Object[]{"Game is over", 100, Color.yellow, largeBannerFont},
			new Object[]{"Sorry...", 160, Color.cyan, smallBannerFont},
			new Object[]{"Hit any key to restart", 300, Color.white},
			new Object[]{getFinishingRobotString(),250,Color.orange,mediumBannerFont}
		);
	}
	
	/**
	 * Wraps around {@link #drawStateScreen_fromList(Graphics, Color, List) drawStateScreen_fromList}.
	 * 
	 * @param gc the graphics panel on which to draw
	 * @param c Color object for background; does <b>not</b> accept {@code null}.
	 * @param fontProp variable number of List arguments conforming to above specification
	 */
	private void drawStateScreen_fromVarargs(Graphics gc, Color c, Object[]... fontProp) {
		drawStateScreen_fromList(gc, c, Arrays.asList(fontProp));
	}
	
	/**
	 * <p> Establishes a background color for the ending screen
	 * and then draws some number of messages on it.
	 * In addition to graphics panel argument,
	 * A {@code List} is passed whose members are lists;
	 * each of these inner lists has the following elements:
	 * <ol>
	 *   </li> (<i>required</i>) {@code String} the text that will be printed </li>
	 *   </li> (<i>required</i>) {@code int} the vertical position of the text </li>
	 *   </li> (<i>optional</i>) {@link java.awt.Color} a Color to apply to the text </li>
	 *   </li> (<i>optional</i>) {@link java.awt.Font} a Font to apply to the text </li>
	 * </ol>
	 * </p>
	 * 
	 * <p> If the Color and Font elements are not supplied, they are inherited from the
	 * current graphics specifications. Supplying {@code null} is equivalent
	 * to not supplying. Note that the position of arguments determines type,
	 * so if one wants to specify a new font but not a new color,
	 * one must pass {@code null} for the Color element. </p>
	 * 
	 * <p> Messages that do not fit on screen will have their
	 * font sizes automatically reduced. </p>
	 * 
	 * @param gc the graphics panel on which to draw
	 * @param c Color object for background; does <b>not</b> accept {@code null}.
	 * @param fontProp a {@code List} whose members are lists conforming to above specification
	 */
	private void drawStateScreen_fromList(Graphics gc, Color c, List<Object[]> fontProp) {
		// produce colored background
		gc.setColor(c);
		gc.fillRect(0, 0, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
		
		// draw each string
		for(Object[] p: fontProp) drawFinishScreenText(gc, p);
	}
	
	/**
	 * Draw a message on screen.
	 * 
	 * Messages containing return characters (\n) are split
	 * into the appropriate number of lines.
	 * 
	 * Messages wider than the screen are shrunk (but not split)
	 * to fit on screen.
	 * 
	 * @param gc graphics panel on which to draw
	 * @param p	see {@link #drawStateScreen_fromVarargs(Graphics, Color, List...) drawStateScreen_fromVarargs}
	 * 			for a description of what {@code p} contains
	 */
	private void drawFinishScreenText(Graphics gc, Object[] p) {
		String message;
		int ypos;
			
		//String message, int ypos, Color c, Font f
		if(null==p) return;
		message=(String)p[0];
		ypos=(int)p[1];
		Color c = null;
		Font f = null;
		
		// these try/catch and if(null != ...) blocks allow us to
		// keep the current color/font, allowing us not to have
		// to specify these with every call to this method
		try{c=(Color)p[2];}
		catch (ArrayIndexOutOfBoundsException e) {}
		try {f=(Font)p[3];}
		catch (ArrayIndexOutOfBoundsException e) {}
		
		if(null!=c) gc.setColor(c);
		if(null!=f) gc.setFont(f);
		
		// case of multi-line message
		// split into base-case calls to this drawTextForceOnScreen
		if(message.contains("\n")) {
			int lineHeight = gc.getFontMetrics().getHeight();
			String[] lines = message.split("\n");
			int line=0;
			for(int i=lines.length-1; i>=0; i--)
				// the 1.1 factor leaves space between lines
				// the line++ and i-- ensures that successive lines of the message
				// are drawn on successive lines on the graphics panel
				// in the expected order (higher ypos = further down on screen)
				drawTextForceOnScreen(gc, lines[line++], (int)(ypos-i*lineHeight*1.1));
		}
		// single-line message: no special case here
		else drawTextForceOnScreen(gc, message, ypos);
	}
	
	/**
	 * <p> Bottommost method before
	 * {@link #centerString(Graphics, FontMetrics, String, int) centerString}
	 * in chain of calls that draws a state screen. </p>
	 * 
	 * <p><b>Functionality</b>:
	 * if a message is too big to fit comfortably on the screen,
	 * decrement its font size until it fits on screen, then draw it
	 * by supplying it to 
	 * {@link #centerString(Graphics, FontMetrics, String, int) centerString}. </p>
	 * 
	 * @param gc graphics panel to draw on
	 * @param message the message that will be drawn
	 * @param ypos the vertical position of the text
	 */
	private void drawTextForceOnScreen(Graphics gc, String message, int ypos) {
			// .95: leave some room around edges
			while(gc.getFontMetrics().stringWidth(message)>.95*Constants.VIEW_WIDTH) {
				// gc repeatedly modifies its current font
				gc.setFont(new Font(
					"TimesRoman",
					Font.BOLD,
					gc.getFontMetrics().getFont().getSize()-1)
				);
			}
			centerString(gc, gc.getFontMetrics(), message, ypos);
	}
	
	/**
	 * <p>Helper method for redraw to draw final screen, screen is hard coded</p>
	 * 
	 * <p>Updated in P3: this method redirects to
	 * {@link #redrawFinishWinning(Graphics) redrawFinishWinning} or
	 * {@link #redrawFinishWinning(Graphics) redrawFinishWinning}
	 * to draw outcome-dependent output to {@code gc}. </p>
	 * 
	 * @param gc the graphics that is the off-screen image
	 */
	private void redrawFinish(Graphics gc) {
		if(controller.getRobot()!=null) {
			if(controller.getRobot().hasStopped()) redrawFinishLosing(gc);
			else redrawFinishWinning(gc);
		}
		else redrawFinishWinning(gc);
	}
	
	/**
	 * Draws the generating screen, screen content is hard coded.
	 * 
	 * @param panel holds the graphics for the off-screen image
	 */
	public void redrawGenerating(MazePanel panel) {
		Graphics g = panel.getBufferGraphics() ;
		if (null == g) {
			System.out.println("MazeView.redrawGenerating: can't get graphics object to draw on, skipping redraw operation") ;
		}
		else {
			redrawGenerating(g);
		}
	}
	
	/**
	 * Helper method for redraw to draw screen during phase of maze generation.
	 * Screen is hard coded, only attribute {@code percentdone} is dynamic.
	 * @param gc graphics is the off screen image on which to draw
	 */
	private void redrawGenerating(Graphics gc) {
		String progress = (null != controllerState) ?
			controllerState.getPercentDone()+"% completed" :
			"Error: no controller, no progress";
		
		drawStateScreen_fromVarargs(gc,Color.yellow,
				new Object[]{"Building maze", 150, Color.red, largeBannerFont},
				new Object[]{progress, 200, Color.black, smallBannerFont}
			);
		
	}
	
	private void centerString(Graphics g, FontMetrics fm, String str, int ypos) {
		g.drawString(str, (Constants.VIEW_WIDTH-fm.stringWidth(str))/2, ypos);
	}

	final Font largeBannerFont = new Font("TimesRoman", Font.BOLD, 48);
	final Font mediumBannerFont = new Font("TimesRoman", Font.BOLD, 32);
	final Font smallBannerFont = new Font("TimesRoman", Font.BOLD, 16);
	
	public void setController(Controller controller) {
		this.controller=controller;
	}

}
