package comp;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import org.junit.Test;


/**
 * Test methods from {@link comp.ExtendedList}.
 * 
 * Going forward I might move this into a new test class.
 */
public class ExtendedListTest {
	
	/**
	 * Test that the methods
	 *     {@link ExtendedList#getFrom(Object, int) getFrom},
	 *     {@link ExtendedList#getDistanceFromTo(Object, Object) getDistanceFromTo},
	 *     {@link ExtendedList#getDistanceFromToMod(Object, Object) getDistanceFromToMod}
	 * 
	 * work as expected on an {@code ExtendedList<Integer>} instance.
	 */
	@Test
	public void testExtendedListInt() {
		ExtendedList<Integer> list = ExtendedList.from(0,1,2,3,4);
		for(int i: list) {
			
			assertTrue(list.get(i)==i);
			
			for(int j: list) {
				// because the list is an integer range starting at 0,
				// the elements are equal to the indices of the elements, and so
				// the distance between elements = the distance between element values
				assertTrue( list.getFrom(i, j-i) == j);
				assertTrue( list.getDistanceFromTo(i, j) == j-i);
				assertTrue( list.getDistanceFromToMod(i, j) == Math.floorMod(j-i, list.size()));
			}
		}
		
		assertTrue(list.getLast()==4);
		
		System.out.println("finished ExtendedList checks");
	}
	
	/**
	 * Replicate logic of {@link #testExtendedListInt()} with instances of
	 * {@code List<Integer>}. Does not work with arrays because
	 * the {@code equals} method of arrays work by reference, not value equality.
	 */
	@Test
	public void testExtendedListObject() {
		
		ExtendedList<List<Integer>> list = new ExtendedList<List<Integer>>();
		
		final int max_index=4;
		
		for(int i=0; i<max_index+1; i++) {
			list.add(ExtendedList.from(i,i));
		}
		
		for(int i=0; i<list.size(); i++) {
			
			List<Integer> i_array = ExtendedList.from(i,i);
			
			assertTrue(list.get(i).equals(i_array));
			
			for(int j=0; j<list.size(); j++) {
				List<Integer> j_array = ExtendedList.from(j,j);
				// because the list is an integer range starting at 0,
				// the elements are equal to the indices of the elements, and so
				// the distance between elements = the distance between element values
				assertTrue( list.getFrom(i_array, j-i).equals(j_array));
				assertTrue( list.getDistanceFromTo(i_array, j_array) == j-i);
				assertTrue( list.getDistanceFromToMod(i_array, j_array) == Math.floorMod(j-i, list.size()));
			}
		}
		
		assertTrue(list.getLast().equals(ExtendedList.from(max_index,max_index)));
		
		System.out.println("finished ExtendedList checks");
	}

}
