package generation;

//import generation.Order.Builder;
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
	
	private int updatePrintThreshold=0;
	
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
	
	/**
	 * WARNING: this class does not provide functionality for reading from file
	 */
	@Override
	public void setFileName(String filename) {
		throw new RuntimeException("OrderStub.setFileName: cannot instantiate filename");
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
	
	/**
	 * Lightweight start method providing minimal required functionality
	 * to order a maze from a factory.
	 */
	@Override
	public void start(Controller controller, MazePanel panel) {
		started = true;
		control = controller;
		
		percentdone = 0;
		
		
		assert null != factory : "Controller.init: factory must be present";
		factory.order(this) ;
	}
	
	/**
	 * assigns the {@code mazeConfig} created by the factory
	 * to an internal field, which can be accessed for testing.
	 */
	@Override
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
	
	/**
	 * Print update on progress to standard out.
	 * Not thread-safe, but this is not critical.
	 */
	@Override
	public void updateProgress(int percentage) {
		if (this.percentdone < percentage && percentage < 100) {
			this.percentdone = percentage;
			if(percentage>updatePrintThreshold) {
				System.out.print(percentage+"  ");
				updatePrintThreshold+=5;
			}
		}
	}
	
	/**
	 * Return the generated maze for testing.
	 * @return factory-generated maze
	 */
	public Maze getMaze(){
		return mazeConfig;
	}
}

