package gui;

import java.awt.Color;

public class MazeColor {
	//internal color representation
	
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
}
