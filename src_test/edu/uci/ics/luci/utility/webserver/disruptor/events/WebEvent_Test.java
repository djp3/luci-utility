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

import static org.junit.Assert.*;

import org.junit.Test;

public class WebEvent_Test {

	@Test
	public void test() {
		String s = "thing";
		WebEvent thing1 = new WebEvent();
		WebEvent thing2 = new WebEvent();
		
		assertTrue(!thing1.equals(null));
		assertTrue(!thing1.equals(s));
		assertTrue(thing1.equals(thing1));
		
		assertEquals(thing1.hashCode(),thing1.hashCode());
		assertEquals(thing1.hashCode(),thing2.hashCode());
		
		assertEquals(thing1,WebEvent.fromJSON(thing1.toJSON()));
		assertEquals(thing1,WebEvent.fromJSON(thing2.toJSON()));
		
	}

}
