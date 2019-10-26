package generation;

import gui.Controller;
import generation.Order;
import generation.OrderStub;
import generation.Order.Builder;

public class MazeTestGenerator {
	
	/**
	 * Sets things in order for a maze to be generated.
	 * OrderStub, Controller, and MazeBuilder instances receive
	 * the parameters required to initialize the maze.
	 * Ensures that the maze is finished generating before tests are performed.
	 * 
	 * @param perfect boolean:{maze is perfect}
	 * @param deterministic boolean:{maze is generated deterministically}
	 * @param level level of the maze
	 * @return the generated Maze instance
	 */
	public static Controller getController(boolean perfect, boolean deterministic, int level){
		Controller controller = new Controller(true);
		controller.turnOffGraphics();
		
		OrderStub order = new OrderStub();
		order.setSkillLevel(level);
		order.setBuilder(Order.Builder.DFS); 
		order.setPerfect(perfect);
		order.start(controller, null);
		
		MazeBuilder builder = new MazeBuilder(deterministic);
		//deterministicTest(deterministic);
		builder.buildOrder(order);
		Thread buildThread = new Thread(builder);
		buildThread.start();
		try {
			buildThread.join();
			if(!Controller.suppressUpdates) System.out.println("100");
			
			controller.switchFromGeneratingToPlaying(order.getMaze());
			
			Maze maze = controller.getMazeConfiguration();
			int w=maze.getWidth(), h=maze.getHeight();
			boolean rooms = false;
			for(int x=0; x<w; x++) {
				for(int y=0; y<h; y++)
					if(maze.getFloorplan().isInRoom(x,y)) {
						rooms=true;
						break;
					}
			}
			
			if(level!=0) assert rooms!=perfect;
			
			return controller;
			
		} catch (InterruptedException e) {
			System.out.println("\n--Intteruption--");
			e.printStackTrace();
			return null;
		}
	}

}
