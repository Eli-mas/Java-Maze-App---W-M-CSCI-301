/**
 * 
 */
package generation;

/**
 * @author HomeFolder
 *
 */
public class MazeBuilderEller extends MazeBuilder implements Runnable {
	
	
	public MazeBuilderEller() {
		super();
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze.");
	}
	
	public MazeBuilderEller(boolean det) {
		super(det);
		System.out.println("MazeBuilderEller uses Eller's algorithm to generate a maze (deterministic enabled).");
	}
	
	@Override
	protected void generatePathways() {
		throw new RuntimeException("MazeBuilderEller: using unimplemented method generatePathways"); 
	}

}

/*
class VerticalWallBoard{
	
	private int[] cellLeft;
	private int[] cellRight;
	
	public VerticalWallBoard(int[] cellLeft, int[] cellRight) {
		this.cellLeft=cellLeft;
		this.cellRight=cellRight;
	}
	
	public int[] getLeft() {
		return cellLeft;
	}
	
	public int[] getRight() {
		return cellRight;
	}
}
*/