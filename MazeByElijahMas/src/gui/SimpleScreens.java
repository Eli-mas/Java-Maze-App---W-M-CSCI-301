package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
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
		// produce white background
		gc.setColor(Color.white);
		gc.fillRect(0, 0, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
		// write the title 
		gc.setFont(largeBannerFont);
		FontMetrics fm = gc.getFontMetrics();
		gc.setColor(Color.red);
		centerString(gc, fm, "MAZE", 100);
		// write the reference to Paul Falstad
		gc.setColor(Color.blue);
		gc.setFont(smallBannerFont);
		fm = gc.getFontMetrics();
		centerString(gc, fm, "by Paul Falstad", 160);
		centerString(gc, fm, "www.falstad.com", 190);
		// write the instructions
		gc.setColor(Color.black);
		if (filename == null) {
			// default instructions
			centerString(gc, fm, "To start, select a skill level.", 250);
			centerString(gc, fm, "(Press a number from 0 to 9,", 300);
			centerString(gc, fm, "or a letter from A to F)", 320);
		}
		else {
			// message if maze is loaded from file
			centerString(gc, fm, "Loading maze from file:", 250);
			centerString(gc, fm, filename, 300);
		}
		centerString(gc, fm, "Version 4.0", 350);
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
		if(controller.getRobot().hasStopped()) return controller.getRobotFailureMessage();
		else return String.format(
					"Energy used: %d/%d; path length: %d",
					(int)controller.getEnergyConsumedByRobotAtPresent(),
					(int)controller.getInitialRobotEnergyLevel(),
					controller.getRobot().getOdometerReading()
		);
	}
	
	private void redrawFinishWinning(Graphics gc) {
		List robotInput = (!controller.robotEnabled) ? null :
			Arrays.asList(getFinishingRobotString(),250,Color.orange,
					new Font("TimesRoman", Font.BOLD, 24));
		
		drawFinishScreen(gc,
			Arrays.asList("You won!", 100, Color.yellow, largeBannerFont),
			Arrays.asList("Congratulations!", 160, Color.cyan, smallBannerFont),
			Arrays.asList("Hit any key to restart", 300, Color.white),
			robotInput
		);
	}
	
	private void redrawFinishLosing(Graphics gc) {
		drawFinishScreen(gc,
			Arrays.asList("Game is over", 100, Color.yellow, largeBannerFont),
			Arrays.asList("Sorry...", 160, Color.cyan, smallBannerFont),
			Arrays.asList("Hit any key to restart", 300, Color.white),
			Arrays.asList(getFinishingRobotString(),250,Color.orange,mediumBannerFont)
		);
	}
	
	private void drawFinishScreen(Graphics gc, List... fontProp) {
		// produce blue background
		gc.setColor(Color.blue);
		gc.fillRect(0, 0, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
		
		String message;
		int ypos;
			
		for(List p: fontProp) {
			//String message, int ypos, Color c, Font f
			if(null==p) continue;
			message=(String)p.get(0);
			ypos=(int)p.get(1);
			Color c = null;
			Font f = null;
			try{
				c=(Color)p.get(2);
				f=(Font)p.get(3);
			} catch (IndexOutOfBoundsException e) {}
			if(null!=c) gc.setColor(c);
			if(null!=f) gc.setFont(f);
			centerString(gc, gc.getFontMetrics(), message, ypos);
		}
	}
	
	/**
	 * Helper method for redraw to draw final screen, screen is hard coded
	 * @param gc graphics is the off-screen image
	 */
	private void redrawFinish(Graphics gc) {
		if(controller.robotEnabled) {
			if(controller.getRobot().hasStopped()) redrawFinishLosing(gc);
			else redrawFinishWinning(gc);
		}
		else redrawFinishWinning(gc);
	}
	
	/**
	 * Draws the generating screen, screen content is hard coded
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
	 * Helper method for redraw to draw screen during phase of maze generation, screen is hard coded
	 * only attribute percentdone is dynamic
	 * @param gc graphics is the off screen image
	 */
	private void redrawGenerating(Graphics gc) {
		// produce yellow background
		gc.setColor(Color.yellow);
		gc.fillRect(0, 0, Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
		// write the title 
		gc.setFont(largeBannerFont);
		FontMetrics fm = gc.getFontMetrics();
		gc.setColor(Color.red);
		centerString(gc, fm, "Building maze", 150);
		gc.setFont(smallBannerFont);
		fm = gc.getFontMetrics();
		// show progress
		gc.setColor(Color.black);
		if (null != controllerState) 
			centerString(gc, fm, controllerState.getPercentDone()+"% completed", 200);
		else
			centerString(gc, fm, "Error: no controller, no progress", 200);
		// write the instructions
		centerString(gc, fm, "Hit escape to stop", 300);
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
