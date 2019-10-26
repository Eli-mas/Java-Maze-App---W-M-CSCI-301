package comp;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends {@link ArrayList} to provide additional
 * useful functionalities.
 * 
 * @author ElijahMas
 *
 * @param <T> the type of the objects contained in the list
 */
public class ExtendedList<T> extends ArrayList<T>{

	/**
	 * not used; auto-generated to handle compiler warning
	 */
	private static final long serialVersionUID = 1L;
	
	public ExtendedList() {
		super();
	}
	
	public ExtendedList(int length) {
		super(length);
	}
	
	public ExtendedList(List<T> l) {
		super(l);
	}

	/**
	 * Get the last element in the list.
	 * Wrapper around {@link #get(int) get} and {@link #size() size} methods.
	 * 
	 * @return last element in the list.
	 */
	public T getLast() {
		return get(size()-1);
	}
	
	/**
	 * Construct a new {@link ExtendedList} from a
	 * variable number of arguments.
	 * 
	 * @param <T> object type
	 * @param args objects to compose the list
	 * @return the new list with the input objects
	 */
	@SafeVarargs
	public static <T> ExtendedList<T> from (T... args) {
		ExtendedList<T> list = new ExtendedList<T>(args.length);
		for(T t: args) list.add(t);
		return list;
	}
	
	/**
	 * Given an object {@code source} and a distance {@code d},
	 * return the object in the list that is distance {@code d} from {@code source}.
	 * 
	 * @param source object of type {@link T}
	 * @param distanceFrom {@code int} value, can be of any sign
	 * @return object that is {@code distanceFrom} steps from {@source} in the list
	 */
	public T getFrom(T source, int distanceFrom) {
		return get(modSize(indexOf(source)+distanceFrom));
	}
	
	/**
	 * Get the distance from {@code T from} to {@code T to}
	 * in this list, if both are present.
	 * 
	 * @param from object of type {@link T}
	 * @param to object of type {@link T}
	 * @return {@code {@link #indexOf(Object) indexOf}(to) - {@link #indexOf(Object) indexOf}(from)}
	 */
	public int getDistanceFromTo(T from, T to) {
		return indexOf(to) - indexOf(from);
	}
	
	/**
	 * Get the distance from {@code T from} to {@code T to}
	 * in this list, if both are present, modulated by the list's size.
	 * 
	 * @param from object of type {@link T}
	 * @param to object of type {@link T}
	 * @return {@code {@link Math.floormod}({@link #indexOf(Object) indexOf}(to) - {@link #indexOf(Object) indexOf}(from), {@link #size()}}
	 */
	public int getDistanceFromToMod(T from, T to) {
		return modSize(getDistanceFromTo(from,to));
	}
	
	/**
	 * Modulate a quantity by the list's size.
	 * Convenience method for internal use only.
	 * @param v any integer
	 * @return {@code v%}{@link #size()}
	 */
	private int modSize(int v) {
		return Math.floorMod(v, size());
	}
	
}