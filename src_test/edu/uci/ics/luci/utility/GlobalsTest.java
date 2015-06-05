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

package edu.uci.ics.luci.utility;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GlobalsTest extends Globals {
	
	Random random = new Random();
	String version = Integer.toString(random.nextInt());
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		while(Globals.getGlobals() != null){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
			}
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

	@Override
	public String getSystemVersion() {
		return version;
	}
	
	@Test
	public void testStuff(){
		assertTrue(Globals.getGlobals() == null);
		
		Globals.setGlobals(this);
		assertEquals(Globals.getGlobals(),this);
		assertEquals(this.version, getSystemVersion());
		setTesting(null);
		assertEquals(true, isTesting());
		setTesting(true);
		assertEquals(true, isTesting());
		setTesting(false);
		assertEquals(false, isTesting());
		
		String fakeFileName = "foo_+"+random.nextInt()+".xml";
		setLog4JPropertyFileName(fakeFileName);
		assertEquals(fakeFileName,getLog4JPropertyFileName());
		
		fakeFileName = "foo_+"+random.nextInt()+".xml";
		Globals.setLog4JPropertyFileName(fakeFileName);
		assertEquals(fakeFileName,Globals.getLog4JPropertyFileName());
		
		TestQuittable x = new TestQuittable();
		Globals.getGlobals().addQuittable(x);
		assertTrue(!x.isQuitting());
		
		setQuitting(false);
		assertEquals(false,isQuitting());
		setQuitting(true);
		assertEquals(true,isQuitting());
		
		assertTrue(x.isQuitting());
	}
	
	public GlobalsTest(){
		this(true);
	}
	
	@Override
	public List<String> getBadGuyList(){
		return new ArrayList<String>();
	}
	
	protected GlobalsTest(boolean testing){
		super();
		setTesting(true);
	}
	
	private class TestQuittable implements Quittable{

		private boolean quitting = false;

		public void setQuitting(boolean quitting) {
			this.quitting = quitting;
		}

		public boolean isQuitting() {
			return this.quitting;
		}
		
	}

}
