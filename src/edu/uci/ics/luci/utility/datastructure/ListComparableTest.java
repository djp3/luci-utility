/*
	Copyright 2007-2013
		University of California, Irvine (c/o Donald J. Patterson)
*/
/*
	This file is part of the Laboratory for Ubiquitous Computing java Utility package, i.e. "Utilities"

    Utilities is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Utilities is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Utilities.  If not, see <http://www.gnu.org/licenses/>.
*/
package edu.uci.ics.luci.utility.datastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Vector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ListComparableTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testListComparableBasics() {
		//Make sure we can't initialize with null
		try{
			//Testing getLog();
			ListComparable.getLog().warn("About to intentionally initialize");
			ListComparable.getLog().warn("with null");
			
			new ListComparable<String>(null);
			fail("Should throw an exception");
		}
		catch(NullPointerException e){
			//all is well
		}
		ListComparable<String> a = new ListComparable<String>(new ArrayList<String>());
		
		assertEquals(a.size(),0);
		a.add("World");
		assertEquals(a.size(),1);
		a.add(0,"Hello");
		assertEquals(a.size(),2);
		
		assertEquals(a.get(0),"Hello");
		assertEquals(a.get(1),"World");
		
		ListComparable<String> b = new ListComparable<String>(new Vector<String>());
		b.addAll(a);
		assertEquals(b.get(0),"Hello");
		assertEquals(b.get(1),"World");
		
		b.addAll(1,a);
		assertEquals(b.get(0),"Hello");
		assertEquals(b.get(1),"Hello");
		assertEquals(b.get(2),"World");
		assertEquals(b.get(3),"World");
		
		assertTrue(b.containsAll(a));
		assertTrue(!b.isEmpty());
		assertEquals(b.lastIndexOf("Hello"),1);
		assertEquals(b.lastIndexOf("World"),3);
		
		int count = 0;
		for(ListIterator<String> i = b.listIterator();i.hasNext();i.next()){
			count++;
		}
		assertEquals(count,4);
		
		count = 0;
		for(ListIterator<String> i = b.listIterator(1);i.hasNext();i.next()){
			count++;
		}
		assertEquals(count,3);
		
		assertEquals(a.indexOf("Hello"),0);
		assertEquals(a.indexOf("World"),1);
		
		assertTrue(b.subList(1,3).contains("Hello"));
		assertTrue(b.subList(1,3).contains("World"));
		
		Object[] array = b.toArray();
		assertEquals(array.length,4);
		
		Object[] array2 = b.toArray(array);
		assertEquals(array2.length,4);
		
		
		b.remove("World");
		assertEquals(b.size(),3);
		
		b.remove(0);
		assertEquals(b.size(),2);
		
		b.retainAll(a);
		assertEquals(b.size(),2);
		
		a.remove("World");
		assertEquals(a.size(),1);
		
		b.removeAll(a);
		assertEquals(b.size(),1);
		
		b.set(0, "Frobozz");
		assertEquals(b.size(),1);
		assertEquals(b.get(0),"Frobozz");
		
		a.clear();
		assertEquals(a.size(),0);
		assertTrue(a.isEmpty());
	}
	
	@Test
	public void testBasicComparisons(){
		ListComparable<String> a = new ListComparable<String>(new ArrayList<String>());
		ListComparable<String> b = new ListComparable<String>(new ArrayList<String>(0));
		ListComparable<String> c = new ListComparable<String>(new ArrayList<String>(1));
		
		assertTrue(a.compareTo(null) != 0);
		assertTrue(!a.equals(null));
		assertTrue(!a.equals(Integer.valueOf(1)));
		
		assertEquals(a.compareTo(b),0);
		assertEquals(b.compareTo(c),0);
		assertEquals(a.compareTo(c),0);
		
		assertTrue(a.equals(b));
		assertTrue(b.equals(c));
		assertTrue(a.equals(c));
	}

	@Test
	public void testCompareTo() {
		ListComparable<String> a = new ListComparable<String>(new ArrayList<String>());
		ListComparable<String> b = new ListComparable<String>(new ArrayList<String>(0));
		ListComparable<String> c = new ListComparable<String>(new ArrayList<String>(1));
		
		a.add("Hello");
		c.add("Hello");
		assertTrue(a.compareTo(b) != 0);
		assertTrue(b.compareTo(c) != 0);
		assertTrue(a.compareTo(c) == 0);
		assertTrue(!a.equals(b));
		assertTrue(!b.equals(c));
		assertTrue(a.equals(c));
		
		b.add("World");
		assertTrue(a.compareTo(b) != 0);
		assertTrue(b.compareTo(c) != 0);
		assertTrue(a.compareTo(c) == 0);
		assertTrue(!a.equals(b));
		assertTrue(!b.equals(c));
		assertTrue(a.equals(c));
		
		a.add("World");
		c.add("World");
		assertTrue(a.compareTo(b) != 0);
		assertTrue(b.compareTo(c) != 0);
		assertTrue(a.compareTo(c) == 0);
		assertTrue(!a.equals(b));
		assertTrue(!b.equals(c));
		assertTrue(a.equals(c));
		
		a.add(1,"Cruel");
		assertTrue(a.compareTo(b) != 0);
		assertTrue(b.compareTo(c) != 0);
		assertTrue(a.compareTo(c) != 0);
		assertTrue(!a.equals(b));
		assertTrue(!b.equals(c));
		assertTrue(!a.equals(c));
		
		a.add(1,"Cold");
		c.add(1,"Cold");
		c.add(2,"Cruel");
		assertTrue(a.compareTo(b) != 0);
		assertTrue(b.compareTo(c) != 0);
		assertTrue(a.compareTo(c) == 0);
		assertTrue(!a.equals(b));
		assertTrue(!b.equals(c));
		assertTrue(a.equals(c));
		
		assertFalse(a.equals("Hello"));
		assertFalse(a.equals(null));
		assertFalse("Hello".equals(a));
	}

}
