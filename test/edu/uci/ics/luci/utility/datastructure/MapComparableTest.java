/*
	Copyright 2007-2018
		Donald J. Patterson 
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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;


public class MapComparableTest {

	@Test
	public void testBasics(){
		
		//Make sure we can't initialize with null
		try{
			//Testing getLog();
			MapComparable.getLog().warn("About to intentionally initialize");
			MapComparable.getLog().warn("with null");
			
			new MapComparable<String,Double>(null);
			fail("Should throw an exception");
		}
		catch(NullPointerException e){
			//all is well
		}
		
		
		HashMap<String, Double> hashMap = new HashMap<String,Double>();
		MapComparable<String, Double> mapComparable = new MapComparable<String,Double>(hashMap);
		
		mapComparable.put("Hello", 1.0d);
		mapComparable.put("World", 2.0d);
		
		assertEquals(2,mapComparable.size());
		
		for(Entry<String, Double> e : mapComparable.entrySet()){
			assertTrue(e.getKey().equals("Hello") || e.getKey().equals("World"));
		}
		
		mapComparable.put("Frobozz", 3.0d);
		assertTrue(mapComparable.containsKey("Hello"));
		assertTrue(mapComparable.containsKey("World"));
		assertTrue(mapComparable.containsKey("Frobozz"));
		
		assertTrue(mapComparable.containsValue(1.0d));
		assertTrue(mapComparable.containsValue(2.0d));
		assertTrue(mapComparable.containsValue(3.0d));
		
		assertEquals(mapComparable.get("Hello"),Double.valueOf(1.0d));
		assertEquals(mapComparable.get("World"),Double.valueOf(2.0d));
		assertEquals(mapComparable.get("Frobozz"),Double.valueOf(3.0d));
		
		for(String s : mapComparable.keySet()){
			assertTrue(s.equals("Hello") || s.equals("World") || s.equals("Frobozz"));
		}
		
		mapComparable.remove("Hello");
		
		assertEquals(2,mapComparable.size());
		
		for(Entry<String, Double> e : mapComparable.entrySet()){
			assertTrue(e.getKey().equals("World") || e.getKey().equals("Frobozz"));
		}
		
		mapComparable.put("Hello", 2.0d);
		
		Map<Double,Integer> count = new HashMap<Double,Integer>();
		for(Double v: mapComparable.values()){
			if(count.containsKey(v)){
				count.put(v, count.get(v)+1);
			}
			else{
				count.put(v, 1);
			}
		}
		assertEquals(Integer.valueOf(1),count.get(3.0d));
		assertEquals(Integer.valueOf(2),count.get(2.0d));
		
		MapComparable<String, Double> mapComparable2 = new MapComparable<String,Double>(hashMap);
		
		mapComparable2.putAll(mapComparable);
		count.clear();
		for(Double v: mapComparable2.values()){
			if(count.containsKey(v)){
				count.put(v, count.get(v)+1);
			}
			else{
				count.put(v, 1);
			}
		}
		assertEquals(Integer.valueOf(1),count.get(3.0d));
		assertEquals(Integer.valueOf(2),count.get(2.0d));

		
		mapComparable.clear();
		assertEquals(0,mapComparable.size());
		
		assertTrue(mapComparable.isEmpty());
		
		mapComparable2.clear();
		assertEquals(0,mapComparable2.size());
		
		assertTrue(mapComparable2.isEmpty());
	}
	
	
	@Test
	public void testComparisons(){
		
		
		MapComparable<String, Double> a = new MapComparable<String,Double>(new HashMap<String,Double>());
		
		assertTrue(a.compareTo(null) != 0);
		assertTrue(!a.equals(null));
		
		assertTrue(!a.equals(Integer.valueOf(1)));
		
		assertEquals(a,a);
		assertEquals(a.hashCode(),a.hashCode());
		
		
		
		
		MapComparable<String, Double> b = new MapComparable<String,Double>(new HashMap<String,Double>());
		
		assertTrue(a.compareTo(b) == 0);
		assertTrue(a.equals(b));
		
		a.put(new Pair<String,Double>("Hello", 1.0d));
		assertTrue(a.compareTo(b) != 0);
		assertTrue(!a.equals(b));
		
		b.put("World", 2.0d);
		assertTrue(a.compareTo(b) != 0);
		assertTrue(!a.equals(b));
		b.remove("World");
		
		b.put("Hello", 2.0d);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(!a.equals(b));
		
		b.put("Hello", 0.0d);
		assertTrue(a.compareTo(b) > 0);
		assertTrue(!a.equals(b));
		
		b.put("Hello", null);
		assertTrue(a.compareTo(b) > 0);
		assertTrue(!a.equals(b));
		
		b.put("Hello", 1.0d);
		assertTrue(a.compareTo(b) == 0);
		assertTrue(a.equals(b));
		
		a.put("Hello", null);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(!a.equals(b));
		
		b.put("Hello", null);
		assertTrue(a.compareTo(b) == 0);
		assertTrue(a.equals(b));
	}

}
