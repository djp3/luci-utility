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

import static org.junit.Assert.*;

import org.junit.Test;


public class PairTest {

	@Test
	public void testPair() {
		Pair<Integer,Integer> p1 = new Pair<Integer,Integer>(1,2);
		assertTrue(p1 != null);
		Pair<Double,Double> p2 = new Pair<Double,Double>();
		assertTrue(p2 != null);
		Pair<Double,String> p3 = new Pair<Double,String>(1.0,"Hello World");
		assertTrue(p3 != null);
	}

	@Test
	public void testGetFirst() {
		Pair<Double,String> p3 = new Pair<Double,String>(1.0,"Hello World");
		assertTrue(p3 != null);
		assertEquals(Double.valueOf(1.0),p3.getFirst());
	}

	@Test
	public void testGetSecond() {
		Pair<Double,String> p3 = new Pair<Double,String>(1.0,"Hello World");
		assertTrue(p3 != null);
		assertEquals("Hello World",p3.getSecond());
	}

	@Test
	public void testSetFirst() {
		Pair<Double,String> p3 = new Pair<Double,String>(1.0,"Hello World");
		assertTrue(p3 != null);
		assertEquals(Double.valueOf(1.0),p3.getFirst());
		p3.setFirst(Double.valueOf(2.2));
		assertEquals(Double.valueOf(2.2),p3.getFirst());
	}

	@Test
	public void testSetSecond() {
		Pair<Double,String> p3 = new Pair<Double,String>(1.0,"Hello World");
		assertTrue(p3 != null);
		assertEquals("Hello World",p3.getSecond());
		p3.setSecond("Foo");
		assertEquals("Foo",p3.getSecond());
	}

	@Test
	public void testCompareTo() {
		Pair<Double,String> p1 = new Pair<Double,String>(1.0,"Hello World");
		
		Pair<Double,String> p2 = new Pair<Double,String>(1.0,"Hello World");
		assertEquals(0,p1.compareTo(p2));
		assertEquals(p1,p2);
		assertEquals(p1.hashCode(),p2.hashCode());
		
		Pair<Double,String> p3 = new Pair<Double,String>(2.0,"Hello World");
		assertTrue(p1.compareTo(p3) < 0);
		assertTrue(p1 != p3);
		assertTrue(p1.hashCode() != p3.hashCode());
		
		Pair<Double,String> p4 = new Pair<Double,String>(0.0,"Hello World");
		assertTrue(p1.compareTo(p4) > 0);
		assertTrue(p1 != p4);
		assertTrue(p1.hashCode() != p4.hashCode());
		
		Pair<Double,String> p5 = new Pair<Double,String>(1.0,"Z");
		assertTrue(p1.compareTo(p5) < 0);
		assertTrue(p1 != p5);
		assertTrue(p1.hashCode() != p5.hashCode());
		
		Pair<Double,String> p6 = new Pair<Double,String>(1.0,"A");
		assertTrue(p1.compareTo(p6) > 0);
		assertTrue(p1 != p6);
		assertTrue(p1.hashCode() != p6.hashCode());
		
		assertEquals(p1,p1);
		assertEquals(p1.hashCode(),p1.hashCode());
		assertEquals(p2,p2);
		assertEquals(p2.hashCode(),p2.hashCode());
		assertEquals(p3,p3);
		assertEquals(p3.hashCode(),p3.hashCode());
		assertEquals(p4,p4);
		assertEquals(p4.hashCode(),p4.hashCode());
		assertEquals(p5,p5);
		assertEquals(p5.hashCode(),p5.hashCode());
		assertEquals(p6,p6);
		assertEquals(p6.hashCode(),p6.hashCode());
		
		assertTrue(!p1.equals(null));
		assertTrue(!p2.equals(null));
		assertTrue(!p3.equals(null));
		assertTrue(!p4.equals(null));
		assertTrue(!p5.equals(null));
		assertTrue(!p6.equals(null));
		
		assertTrue(!p1.equals("a"));
		assertTrue(!p2.equals(0L));
		assertTrue(!p3.equals(0.0d));
		assertTrue(!p4.equals(0));
		assertTrue(!p5.equals(new Object()));
		assertTrue(!p6.equals(true));
		
		Pair<Integer, String> p7 = new Pair<Integer,String>(0,p1.getSecond());
		assertTrue(!p7.equals(p1));
		assertTrue(!p1.equals(p7));
		
		Pair<Double, Integer> p8 = new Pair<Double,Integer>(p1.getFirst(),0);
		assertTrue(!p8.equals(p1));
		assertTrue(!p1.equals(p8));
		
		Pair<Double, String> p9 = new Pair<Double,String>(p1.getFirst()+1.0d,p1.getSecond());
		assertTrue(!p9.equals(p1));
		assertTrue(!p1.equals(p9));
		
		Pair<Double, String> p10 = new Pair<Double,String>(p1.getFirst(),"world");
		assertTrue(!p10.equals(p1));
		assertTrue(!p1.equals(p10));
		
		Pair<Double, String> p11 = new Pair<Double,String>(null,"Hello world");
		assertTrue(p11.equals(p11));
		assertTrue(!p11.equals(p1));
		assertTrue(!p1.equals(p11));
		
		Pair<Double, String> p12 = new Pair<Double,String>(1.0,null);
		assertTrue(p12.equals(p12));
		assertTrue(!p12.equals(p1));
		assertTrue(!p1.equals(p12));
		
		Pair<Double, String> p13 = new Pair<Double,String>(null,null);
		assertTrue(p13.equals(p13));
		assertTrue(!p13.equals(p1));
		assertTrue(!p1.equals(p13));
	}
	

}
