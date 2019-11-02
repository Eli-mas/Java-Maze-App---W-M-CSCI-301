package gui;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.Class;

public class MazeColor {
	//internal color representation
	
	public static enum Colors {white, red, orange, yellow, grey, gray, black, green, blue, violet, purple, cyan, brown};
	
	int r,g,b;
	
	public MazeColor(int r, int g, int b){
		this.r=r;
		this.g=g;
		this.b=b;
	}
	
	public Color export(){
		return MazeColor.export(r, g, b);
	}
	
	public static Color export(int r, int g, int b){
		//convert to AWT color
		return new Color(r, g, b);
	}
	
	public static Color export(MazeColor.Colors c) {
		try {
			return (Color)Color.class.getDeclaredField(c.name()).get(null);
		} catch (Exception e) {
			// e.printStackTrace()
			return Color.black;
		}
	}
}
