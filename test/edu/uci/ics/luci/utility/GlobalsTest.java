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
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test also extends the Globals class because it is abstract
 * @author djp3
 *
 */
public class GlobalsTest extends Globals {
	
	private static Random random;
	private static String version;
	private static String log4jFileName;
	
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		random = new Random();
		version = Integer.toString(random.nextInt());
		log4jFileName = "testSupport/GlobalsTest.log4j.xml";
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}
	

	@BeforeEach
	public void setUp() throws Exception {
		while(Globals.getGlobals() != null){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
			}
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		Globals.setGlobals(null);
	}
	
	
	public GlobalsTest(){
		super();
		this.setTesting(true);
	}

	@Override
	public String getSystemVersion() {
		return version;
	}

	@Override
	public String getLog4JPropertyFileName() {
		return log4jFileName;
	}

	@Test
	void testLog() {
		try {
			assertNotNull(Globals.getLog());
			assertNotNull(Globals.getLog());
			Globals.getLog().trace("This is a test logging trace message");
			Globals.getLog().debug("This is a test logging debug message");
			Globals.getLog().warn("This is a test logging warning message");
			Globals.getLog().error("This is a test logging error message");
			Globals.getLog().fatal("This is a test logging fatal message");
		}
		catch(RuntimeException e) {
			fail("This should not thrown an exception");
		}
	}
	
	@Test
	void testRandom() {
		assertNotNull(Globals.getRandom());
		/* I guess this could technically pass and there wouldn't be an error */
		assertTrue(Globals.getRandom().nextInt() != Globals.getRandom().nextInt());
	}
	
	@Test
	public void testSingleton() {
		/* Until you set a Globals there is no singleton */
		assertTrue(Globals.getGlobals() == null);
		Globals.setGlobals(this);
		assertTrue(Globals.getGlobals() == this);
		
	}
	
	
	@Test
	public void testSystemVersion() {
		Globals.setGlobals(this);
		assertEquals(version, getSystemVersion());
	}
	
	@Test
	public void testTesting() {
		Globals.setGlobals(this);
		//Set by the GlobalsTest constructor
		assertEquals(true, isTesting());
		setTesting(false);
		assertEquals(false, isTesting());
		setTesting();
		assertEquals(true, isTesting());
		setTesting(false);
		assertEquals(false, isTesting());
		setTesting(true);
		assertEquals(true, isTesting());
	}
	
	@Test
	public void testPropertyFileNames() {
		Globals.setGlobals(this);
		//This is set through a system property in the test set-up
		assertEquals(log4jFileName,getLog4JPropertyFileName());
	}
	

	/** This is to help test the Globals Quittable manager */
	private static class GlobalsTestQuittableHelper implements Quittable{

		private boolean quitting = false;

		public void setQuitting(boolean quitting) {
			this.quitting = quitting;
		}

		public boolean isQuitting() {
			return this.quitting;
		}
		
	}
	
	
	@Test
	public void testBlockingShutdown(){
		Globals.setGlobals(this);
		GlobalsTestQuittableHelper x = new GlobalsTestQuittableHelper();
		Globals.getGlobals().addQuittable(x);
		assertTrue(!x.isQuitting());
		
		this.setQuitting(false);
		assertEquals(false,isQuitting());
		setQuitting(true);
		assertEquals(true,isQuitting());
		assertTrue(x.isQuitting());
	}
	
	@Test
	public void testNotBlockingShutdown(){
		Globals.setGlobals(this);
		GlobalsTestQuittableHelper x = new GlobalsTestQuittableHelper();
		Globals.getGlobals().addQuittable(x);
		assertTrue(!x.isQuitting());
		
		this.setQuitting(false);
		assertEquals(false,isQuitting());
		
		setQuitting(true,false);
		assertEquals(true,isQuitting());
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
			while (!x.isQuitting()) {
				Thread.sleep(100);
			}
		});
		
	}
	
	
	@Test
	public void testRepeatShutdown(){
		Globals.setGlobals(this);
		GlobalsTestQuittableHelper x = new GlobalsTestQuittableHelper();
		Globals.getGlobals().addQuittable(x);
		assertTrue(!x.isQuitting());
		
		this.setQuitting(false);
		assertEquals(false,isQuitting());
		setQuitting(true);
		assertEquals(true,isQuitting());
		assertTrue(x.isQuitting());
		
		/* Once quitting is set it can't be unset */
		setQuitting(false);
		assertEquals(true,isQuitting());
		assertTrue(x.isQuitting());
		
		/* Once quitting is set setting twice is unnecessary but harmless*/
		setQuitting(true);
		assertEquals(true,isQuitting());
		assertTrue(x.isQuitting());
	}
	
	@Test
	public void testAddQuittableAfterShutdown(){
		Globals.setGlobals(this);
		GlobalsTestQuittableHelper x = new GlobalsTestQuittableHelper();
		Globals.getGlobals().addQuittable(x);
		assertTrue(!x.isQuitting());
		
		GlobalsTestQuittableHelper y = new GlobalsTestQuittableHelper();
		assertTrue(!y.isQuitting());
		
		setQuitting(true);
		assertEquals(true,isQuitting());
		assertTrue(x.isQuitting());
		assertTrue(!y.isQuitting());
		
		Globals.getGlobals().addQuittable(y);
		assertTrue(y.isQuitting());
	}
	
	@Test
	public void testNullQuittable(){
		Globals.setGlobals(this);
		Globals.getGlobals().addQuittable(null);
		assertEquals(false,isQuitting());
		setQuitting(true);
		assertEquals(true,isQuitting());
	}
	
	@Test
	public void testCalendar(){
		Globals.setGlobals(this);
		Calendar calendar = Globals.getGlobals().getCalendar(TimeZone.getTimeZone("UTC"));
		assertEquals(calendar.getTimeInMillis(),System.currentTimeMillis(),100.0);
		
		calendar = Globals.getGlobals().getCalendar("UTC");
		assertEquals(calendar.getTimeInMillis(),System.currentTimeMillis(),100.0);
	}

}
