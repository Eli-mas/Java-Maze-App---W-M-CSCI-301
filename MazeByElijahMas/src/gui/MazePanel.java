package gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
//import java.awt.Panel;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * Add functionality for double buffering to an AWT Panel class.
 * Used for drawing a maze.
 * 
 * @author Peter Kemper
 *
 */
public class MazePanel extends JPanel  {
	/* Panel operates a double buffer see
	 * http://www.codeproject.com/Articles/2136/Double-buffer-in-standard-Java-AWT
	 * for details
	 */
	// bufferImage can only be initialized if the container is displayable,
	// uses a delayed initialization and relies on client class to call initBufferImage()
	// before first use
	private Image bufferImage;  
	private Graphics2D graphics; // obtained from bufferImage, 
	// graphics is stored to allow clients to draw on the same graphics object repeatedly
	// has benefits if color settings should be remembered for subsequent drawing operations
	
	/**
	 * Constructor. Object is not focusable.
	 */
	public MazePanel() {
		setFocusable(false);
		bufferImage = null; // bufferImage initialized separately and later
		graphics = null;	// same for graphics
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	/**
	 * Method to draw the buffer image on a graphics object that is
	 * obtained from the superclass. 
	 * Warning: do not override getGraphics() or drawing might fail. 
	 */
	public void update() {
		paint(getGraphics());
	}
	
	/**
	 * Draws the buffer image to the given graphics object.
	 * This method is called when this panel should redraw itself.
	 * The given graphics object is the one that actually shows 
	 * on the screen.
	 */
	@Override
	public void paint(Graphics g) {
		if (null == g) {
			System.out.println("MazePanel.paint: no graphics object, skipping drawImage operation");
		}
		else {
			g.drawImage(bufferImage,0,0,null);	
		}
	}

	/**
	 * Obtains a graphics object that can be used for drawing.
	 * This MazePanel object internally stores the graphics object 
	 * and will return the same graphics object over multiple method calls. 
	 * The graphics object acts like a notepad where all clients draw 
	 * on to store their contribution to the overall image that is to be
	 * delivered later.
	 * To make the drawing visible on screen, one needs to trigger 
	 * a call of the paint method, which happens 
	 * when calling the update method. 
	 * @return graphics object to draw on, null if impossible to obtain image
	 */
	public Graphics getBufferGraphics() {
		// if necessary instantiate and store a graphics object for later use
		if (null == graphics) { 
			if (null == bufferImage) {
				bufferImage = createImage(Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
				if (null == bufferImage)
				{
					System.out.println("Error: creation of buffered image failed, presumedly container not displayable");
					return null; // still no buffer image, give up
				}		
			}
			graphics = (Graphics2D) bufferImage.getGraphics();
			if (null == graphics) {
				System.out.println("Error: creation of graphics for buffered image failed, presumedly container not displayable");
			}
			else {
				// System.out.println("MazePanel: Using Rendering Hint");
				// For drawing in FirstPersonDrawer, setting rendering hint
				// became necessary when lines of polygons 
				// that were not horizontal or vertical looked ragged
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}
		}
		return graphics;
	}
	
	/**
	 * Fill a rectangle; wrapper around {@link Graphics2D#fillRect(int, int, int, int)}
	 * 
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param width width of rectangle
	 * @param height height of rectangle
	 * 
	 * @see java.awt.Graphics2D#fillRect(int, int, int, int)
	 */
	public void fillRect(int x, int y, int width, int height){
		graphics.fillRect(x, y, width, height);
	}
	
	/**
	 * Draw a line; wrapper around {@link Graphics2D#drawLine(int, int, int, int)}
	 * @param x1 x-coordinate of first point
	 * @param y1 y-coordinate of first point
	 * @param x2 x-coordinate of second point
	 * @param y2 y-coordinate of second point
	 * 
	 * @see Graphics2D#drawLine(int, int, int, int)
	 */
	public void drawLine(int x1, int y1, int x2, int y2){
		graphics.drawLine(x1, y1, x2, y2);
	}
	
	/**
	 * Fill a polygon with the current color of the {@link #graphics} object;
	 * wrapper around {@link Graphics2D#fillPolygon(int[], int[], int).
	 * 
	 * @param xpoints array of x values
	 * @param ypoints array of y values
	 * @param npoints number of points to take from array
	 * 
	 * @see Graphics2D#fillPolygon(int[], int[], int)
	 */
	public void fillPolygon(int[] xpoints, int[] ypoints, int npoints){
		graphics.fillPolygon(xpoints, ypoints, npoints);
	}
	
	/**
	 * Set the color of the {@link #graphics} object.
	 * 
	 * @param c {@link MazeColor} object
	 */
	public void setColor(MazeColor c){
		graphics.setColor(c.export());
	}
	
	
	/**
	 * Set the color of the {@link #graphics} object.
	 * 
	 * @param c {@link MazeColor.Colors} object
	 */
	public void setColor(MazeColor.Colors c){
		graphics.setColor(MazeColor.export(c));
	}
	
	/**
	 * Fill an oval; wrapper around {@link Graphics2D#fillOval(int, int, int, int)}
	 * 
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param width width of rectangle
	 * @param height height of rectangle
	 * 
	 * @see java.awt.Graphics2D#fillOval(int, int, int, int)
	 */
	public void fillOval(int x, int y, int width, int height){
		graphics.fillOval(x, y, width, height);
	}
	
	/**
	 * Boolean to test whether graphics is turned off.
	 * 
	 * @return true if graphics off, false if on
	 */
	public boolean isInNoGraphicsMode() {
		return null==getBufferGraphics();
	}
	
}
