/*
	Copyright 2007-2015
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


package edu.uci.ics.luci.utility.webserver.disruptor.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;

public class WebEventVoid_Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		while(Globals.getGlobals() != null){
			Thread.sleep(100);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Globals.setGlobals(null);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	static final String goodVersion = "0.1";
	static final String badVersion = "0.2";

	@Test
	public void test() {
		String s = "thing";
		WebEvent_Void thing1 = new WebEvent_Void();
		WebEvent_Void thing2 = new WebEvent_Void();
		
		assertTrue(!thing1.equals(null));
		assertTrue(!thing1.equals(s));
		assertTrue(!thing1.equals(new WebEvent()));//class != superclass
		assertTrue(thing1.equals(thing1));
		assertTrue(thing1.equals(thing2));
		assertTrue(thing2.equals(thing2));
		
		assertEquals(thing1.hashCode(),thing1.hashCode());
		assertEquals(thing1.hashCode(),thing2.hashCode());
		
		assertEquals(thing1,WebEvent_Void.fromJSON(thing1.toJSON()));
		assertEquals(thing1,WebEvent_Void.fromJSON(thing2.toJSON()));
		
	}

}
