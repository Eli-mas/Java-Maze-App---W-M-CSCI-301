package generation;

import generation.Order.Builder;
import gui.Controller;
import gui.DefaultState;
import gui.MazePanel;

public class OrderStub extends DefaultState implements Order{
	//SimpleScreens view;
	//MazePanel panel;
	Controller control;
	private String filename;
	
	private int skillLevel;
	private Builder builder;
	private boolean perfect; 
   
	protected Factory factory;

	private int percentdone;

	boolean started;
	
	private Maze mazeConfig;
	
	public OrderStub() {
		filename = null;
		factory = new MazeFactory(true) ;
		skillLevel = 0;
		builder = Order.Builder.DFS;
		perfect = true;
		percentdone = 0;
		started = false;
	}
	
	@Override
	public void setFileName(String filename) {
		this.filename = filename;  
	}
	@Override
	public void setSkillLevel(int skillLevel) {
		this.skillLevel = skillLevel;
	}
	@Override
	public void setBuilder(Builder builder) {
		this.builder = builder;
	}
	@Override
	public void setPerfect(boolean isPerfect) {
		perfect = isPerfect;
	}
	
	/*
	private Maze loadMazeConfigurationFromFile(String filename) {
		// load maze from file
		MazeFileReader mfr = new MazeFileReader(filename) ;
		// obtain MazeConfiguration
		return mfr.getMazeConfiguration();
	}
	*/
	
	public void start(Controller controller, MazePanel panel) {
		started = true;
		control = controller;
		
		percentdone = 0;
		
		
		if (filename != null) {
			//deliver(loadMazeConfigurationFromFile(filename));
			filename = null;  
		} else {
			assert null != factory : "Controller.init: factory must be present";
			//draw();
			factory.order(this) ;
		}
	}
	
	public void deliver(Maze mazeConfig) {
		this.mazeConfig=mazeConfig;
	}
	
	@Override
	public int getSkillLevel() {
		return skillLevel;
	}
	@Override
	public Builder getBuilder() {
		return builder;
	}
	@Override
	public boolean isPerfect() {
		return perfect;
	}
	public int getPercentDone() {
		return percentdone;
	}
	
    @Override
    public void updateProgress(int percentage) {
        /*
        if (this.percentdone < percentage && percentage <= 100) {
            this.percentdone = percentage;
            draw() ;
        }
        */
    }
    
    public Maze getMaze(){
    	return mazeConfig;
    }
	
}

