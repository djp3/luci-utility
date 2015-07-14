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

package edu.uci.ics.luci.utility.webserver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;

public class AccessControlTest extends AccessControl{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(AccessControlTest.class);
		}
		return log;
	}
	
	
	private static class DummyGlobals extends Globals{
		@Override
		public String getSystemVersion() {
			return "1.0";
		}
	};
	
	public AccessControlTest() {
		super();
		
		while(Globals.getGlobals() != null){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		
		Globals.setGlobals(new DummyGlobals());
		super.reset();
		if(Globals.getGlobals() == null){
			fail("Global set up failed");
		}
		this.setBadGuyTest(Globals.getGlobals().getBadGuyList());
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Globals.setGlobals(null);
	}

	
	@Before
	public void setUp() throws Exception {
		reset();
	}

	@After
	public void tearDown() throws Exception {
		Globals.setGlobals(null);
	}

	@Test
	public void testLoadConfiguration() {
		try{
			setDefaultFilename("test/test.properties");
			loadConfiguration();
		}
		catch(Exception e){
			getLog().error("Failed to load test.properties",e);
			fail("Failed to load test.properties");
		}
		
	}
	
	@Test
	/* This makes sure that our bad guy list sanity checking is working */
	public void testBadGuyTesting() {
		List<String> newBadGuyTest = Arrays.asList("a.b.c.d");
		setBadGuyTest(newBadGuyTest);
		
		try {
			/*Try and allow a bad guy to connect */
			setDefaultFilename("test/test.properties");
			loadConfiguration();
			fail("This should throw an exception for letting a bad guy through");
		} catch (ConfigurationException e) {
			fail("Failed to load test.properties");
		} catch (RuntimeException e) {
		}
	}
	
	@Test
	/* This makes sure that our bad guy list sanity checking is based on regex */
	public void testBadGuyTesting2() {
		List<String> newBadGuyTest = Arrays.asList("aabbccd");
		setBadGuyTest(newBadGuyTest);
		
		try {
			/*Try and allow a bad guy to connect */
			setDefaultFilename("test/test.properties");
			loadConfiguration();
			fail("This should throw an exception for letting a bad guy through");
		} catch (ConfigurationException e) {
			fail("Failed to load test.properties");
		} catch (RuntimeException e) {
		}
	}
	
	@Test
	/* This makes sure that our bad guy list sanity checking is working with dns*/
	public void testBadGuyTesting3() {
		List<String> newBadGuyTest = Arrays.asList("dcortes.net");
		setBadGuyTest(newBadGuyTest);
		try {
			setDefaultFilename("test/test2.properties");
			loadConfiguration();
			fail("This should throw an exception for letting a bad guy through");
		} catch (ConfigurationException e) {
			fail("Failed to load test.properties");
		} catch (RuntimeException e) {
		}
	}
			
	@Test
	public void testAllowSource() {
		try{
			setDefaultFilename("test/test.properties");
			loadConfiguration();
		}
		catch(Exception e){
			getLog().error("Failed to load test.properties",e);
			fail("Failed to load test.properties");
		}
		
		assertTrue(allowSource("a.b.c.d",true,false));
		assertTrue(!allowSource("A.b.c.d",true,true));
		assertTrue(allowSource("aabbccd",true,false));
		assertTrue(allowSource("127.0.0.1",true,false));
		assertTrue(!allowSource("a.b.c",true,false));
		assertTrue(!allowSource("b.c.d",true,false));
		
		assertTrue(!allowSource(null,true,false));
		assertTrue(!allowSource(null,false,false));
		assertTrue(!allowSource(null,true,true));
		assertTrue(!allowSource(null,false,true));
	}
	
	@Test
	public void testAllowSource2() {
		/*This is a random assortment of people who tried to connect to a server */
		/*This is used for a dynamic test at runtime to make sure the configuration file isn't whack*/
		setBadGuyTest(Arrays.asList(
		            "ns2.bomfim.com.br",
		            "dcortes.net",
		            "59-124-107-247.hinet-ip.hinet.net",
		            "59.108.230.218",
		            "59-124-107-247.hinet-ip.hinet.net",
		            "202.153.121.205",
		            "190.208.29.147",
		            "94.75.206.156",
		            "loft2552.serverloft.com",
		            "118.102.25.161",
		            "202.117.3.30",
		            "59.108.230.218",
		            "80.84.236.230",
		            "190.144.35.98",
		            "200-168-188-236.copercana.com.br",
		            "202.153.121.205",
		            "203.125.118.252",
		            "67.223.227.141",
		            "dhcp-059179.ics.uci.edu",
		            "tdh.spree.de",
		            "189.103.205.71",
		            "190.144.35.98",
		            "201.38.252.182",
		            "67.223.227.141",
		            "tdh.spree.de",
		            "80.84.236.230",
		            "host131.200-45-174.telecom.net.ar"));
		try{
			setDefaultFilename(defaultFilenameTest2);
			loadConfiguration();
		}
		catch(Exception e){
			getLog().error("Failed to load access_control_list.properties",e);
			fail("Failed to load test.properties");
		}
		
		assertTrue(allowSource("127.0.0.1",true,false));
		assertTrue(allowSource("173.45.243.9",true,false));
		assertTrue(!allowSource("tdh.spree.de",true,false));
		assertTrue(!allowSource("189.103.205.71",true,false));
		assertTrue(!allowSource("80.84.236.230",true,false));
	}
	

	@Test
	public void testAllowSource3() {
		List<String> allowedConnections = new ArrayList<String>();
		
		setDefaultFilename("test/test4.properties");
		/*Make sure the cache never expires */
		setExpirationTime(System.currentTimeMillis() *2);
		
		setAllowedConnections(allowedConnections);
		assertTrue(!allowSource(null, false,false));
		assertTrue(!allowSource("", false,false));
		assertTrue(!allowSource("127.0.0.1", false,false));
		assertTrue(!allowSource("128.195.58.12", false,false));
		
		assertTrue(!allowSource(null, true,false));
		assertTrue(!allowSource("", true,false));
		assertTrue(!allowSource("127.0.0.1", true,false));
		assertTrue(!allowSource("128.195.58.12", true,false));
		assertTrue(!allowSource("999.999.999.999", true,false));
		
		allowedConnections.add("127.0.0.1");
		setAllowedConnections(allowedConnections);
		assertTrue(allowSource("127.0.0.1", false,false));
		assertTrue(allowSource("127.0.0.1", true,false));
		assertTrue(!allowSource("", false,false));
		assertTrue(!allowSource("", true,false));
		
		allowedConnections.clear();
		allowedConnections.add("localhost");
		setAllowedConnections(allowedConnections);
		assertTrue(!allowSource("127.0.0.1", false,false));
		assertTrue(allowSource("127.0.0.1", true,false));
		
		allowedConnections.add("uci.edu$");
		setAllowedConnections(allowedConnections);
		//128.195.58.12 is supposed to resolve to luci-printer.ics.uci.edu
		assertTrue(!allowSource("128.195.58.12", false,false));
		assertTrue(allowSource("128.195.58.12", true,false));
		
		String www_uci_edu = "128.195.188.232"; //If this is wrong the test might break
		assertTrue(!allowSource(www_uci_edu, false,true));
		assertTrue(!allowSource(www_uci_edu, false,true));
		
		allowedConnections.add("edu$");
		setAllowedConnections(allowedConnections);
		assertTrue(!allowSource(www_uci_edu, false,false));
		assertTrue(allowSource(www_uci_edu, true,false));
		
		allowedConnections.add(".*");
		setAllowedConnections(allowedConnections);
		assertTrue(allowSource("999.999.999.999", true,false));
		
	}
	
	@Test
	public void testBadConfiguration() {
		try{
			setDefaultFilename("test/doesnotexist.properties");
			loadConfiguration();
			fail("Should not find this file");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		
		assertTrue(!allowSource("a.b.c.d",true,false));
		assertTrue(!allowSource("aabbccd",true,false));
		assertTrue(!allowSource("127.0.0.1",true,false));
		assertTrue(!allowSource("a.b.c",true,false));
		assertTrue(!allowSource("b.c.d",true,false));
	}
	
	@Test
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value={"UW_UNCOND_WAIT","WA_NOT_IN_LOOP"}, justification="This is testing code")
	public void testCacheExpiring() {
		final long cacheExpireTime = 5* 1000L; // 5 seconds, 1 second was too short
		
		setExpirationTime(cacheExpireTime);
		try {
			setDefaultFilename("test/test.properties");
			loadConfiguration();
		} catch (ConfigurationException e) {
			fail("Failed to load test.properties");
		} catch (RuntimeException e) {
			fail("Failed to load test.properties");
		}
		assertTrue(!cacheExpired());
		assertTrue(allowSource("127.0.0.1",true,false));
		try {
			getLog().info("Waiting "+(cacheExpireTime+1L)+" milliseconds");
			synchronized(Thread.currentThread()){
				Thread.currentThread().wait(cacheExpireTime+1L);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(cacheExpired());
		setDefaultFilename("test/test3.properties");
		/*Force cache reload*/
		assertTrue(allowSource("128.0.0.1",true,false));
		assertTrue(!allowSource("127.0.0.1",true,false));
	}
}
