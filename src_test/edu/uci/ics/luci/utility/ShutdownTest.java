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
package edu.uci.ics.luci.utility;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShutdownTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testLog() {
		assertNotNull(Shutdown.getLog());
		assertNotNull(Shutdown.getLog());
	}

	@Test
	public void testConstructors() {
		try {
			new Shutdown();
			List<Quittable> q = new ArrayList<Quittable>();
			new Shutdown(q);
		} catch (RuntimeException e) {
			fail("Should not throw an exception on constructor");
		}
	}
	
	private class ShutdownTestHelper implements Quittable{
		Object lock = new Object();
		boolean hasBeenQuit = false;

		@Override
		public void setQuitting(boolean quitting) {
			synchronized(lock) {
				if(quitting) {
					hasBeenQuit = true;
				}
			}	
		}

		@Override
		public boolean isQuitting() {
			synchronized(lock) {
				return hasBeenQuit;
			}
		}
	}
	
	
	@Test
	public void testAdd() {
		try {
			Shutdown s = new Shutdown();
			s.add(null);
			s.add(new ShutdownTestHelper());
			s.add(new ShutdownTestHelper());
		} catch (RuntimeException e) {
			fail("Should not throw an exception");
		}
	}
	
	@Test
	public void testShutItAllDown() {
		Shutdown s = new Shutdown();
		s.add(null);
		ShutdownTestHelper a = new ShutdownTestHelper();
		ShutdownTestHelper b = new ShutdownTestHelper();
		s.add(a);
		s.add(b);
		/* Run the normal shutdown */
		try {
			Thread t = new Thread(s);
			t.start();
			while(t.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			assertTrue(a.hasBeenQuit);
			assertTrue(b.hasBeenQuit);
		} catch (RuntimeException e) {
			fail("Should not throw an exception");
		}
	}
	
	
	@Test
	public void testShutItAllDownStrangely() {
		List<Quittable> qs = new LinkedList<Quittable>();
		ShutdownTestHelper a = new ShutdownTestHelper();
		ShutdownTestHelper b = new ShutdownTestHelper();
		qs.add(a);
		qs.add(b);
		
		Shutdown s = new Shutdown(qs);
		/* Add a worthless null */
		s.add(null);
		/* Add the same quittables again*/
		s.add(a);
		s.add(b);
		/* Quit b out of scope of Shutdown */
		b.setQuitting(true);
		
		/* Run the normal shutdown on the broken data structure */
		try {
			Thread t = new Thread(s);
			t.start();
			while(t.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			assertTrue(a.hasBeenQuit);
			assertTrue(b.hasBeenQuit);
		} catch (RuntimeException e) {
			fail("Should not throw an exception");
		}
		
		/* Run a second unhelpful shutdown on the broken data structure */
		try {
			Thread t = new Thread(s);
			t.start();
			while(t.isAlive()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			assertTrue(a.hasBeenQuit);
			assertTrue(b.hasBeenQuit);
		} catch (RuntimeException e) {
			fail("Should not throw an exception");
		}
	}

}
