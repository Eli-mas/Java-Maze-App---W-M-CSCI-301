package gui;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.Class;

public class MazeColor {
	//internal color representation
	
	public static enum Colors {white, red, orange, yellow, grey, gray, darkGray, black, green, blue, violet, purple, cyan, brown};
	
	/**
	 * the red, green, blue, alpha color components.
	 */
	int r,g,b,a;
	
	/**
	 * Contains information about r,g,b,a components
	 * which can be extracted via bitwise functions.
	 * Replicates AWT.Color methods for encoding information.
	 */
	int value;
	
	/**
	 * Replicates AWT color model. Uses bitwise functions
	 * to store r,g,b,a values in the {@link #value} parameter.
	 */
	private void setRGBA() {
        value = ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                ((b & 0xFF) << 0);
	}
	
	/**
	 * Set r,g,b values with {@link #a} defaulted to 255.
	 * @param r red value, 0<=r<=255
	 * @param g green value, 0<=g<=255
	 * @param b blue value, 0<=b<=255
	 */
	public MazeColor(int r, int g, int b){
		this.r=r;
		this.g=g;
		this.b=b;
		this.a=255;
		setRGBA();
	}
	
	/**
	 * Set r,g,b values with {@link #a} defaulted to 255.
	 * @param r red value, 0<=r<=255
	 * @param g green value, 0<=g<=255
	 * @param b blue value, 0<=b<=255
	 * @param a alpha value, 0<=r<=255
	 */
	public MazeColor(int r, int g, int b, int a){
		this.r=r;
		this.g=g;
		this.b=b;
		this.a=a;
		setRGBA();
	}
	
	/**
	 * Decode the input integer value
	 * via bitwise functions to yield unique r,g,b,a values,
	 * as in AWT.Color.
	 * @param value
	 */
	public MazeColor(int value) {
		this.r = (value >> 16) & 0xFF;
		this.g = (value >> 8) & 0xFF;
		this.b = (value >> 0) & 0xFF;
		this.a = (value >> 24) & 0xFF;
		this.value=value;
	}
	
	/**
	 * Translate the current MazeColor object into an awt.Color object.
	 * @return awt.Color object
	 */
	public Color export(){
		return MazeColor.export(r, g, b, a);
	}
	
	/**
	 * Return awt.Color object instantiated from input values
	 * 
	 * @param r red value, 0<=r<=255
	 * @param g green value, 0<=g<=255
	 * @param b blue value, 0<=b<=255
	 * @param a alpha value, 0<=r<=255
	 * @return awt.Color object from input values
	 */
	public static Color export(int r, int g, int b, int a){
		//convert to AWT color
		return new Color(r, g, b, a);
	}
	
	/**
	 * Return awt.Color object instantiated from input values
	 * 
	 * @param r red value, 0<=r<=255
	 * @param g green value, 0<=g<=255
	 * @param b blue value, 0<=b<=255
	 * @return awt.Color object from input values
	 */
	public static Color export(int r, int g, int b){
		//convert to AWT color
		return new Color(r, g, b);
	}
	
	/**
	 * Use reflection to return named awt.Color object
	 * from a named #{@link MazeColor} object.
	 * @param c #{@link MazeColor} object
	 * @return awt.Color analog
	 */
	public static Color export(MazeColor.Colors c) {
		try {
			return (Color)Color.class.getDeclaredField(c.name()).get(null);
		} catch (Exception e) {
			// e.printStackTrace()
			return Color.black;
		}
	}
	
	/**
	 * Return integer value representation of color components.
	 * @return color integer value
	 */
	public int getRGB() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
		result = prime * result + g;
		result = prime * result + r;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MazeColor other = (MazeColor) obj;
		if (a != other.a)
			return false;
		if (b != other.b)
			return false;
		if (g != other.g)
			return false;
		if (r != other.r)
			return false;
		return true;
	}
	
	
}
