/*
	Copyright 2007-2014
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.webserver.handlers.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerVersion;

public class RequestDispatcherTest {
	
	Random random = new Random();

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
	public void testRequestDispatcher() {
		final String testVersion = Integer.toString(random.nextInt());
		
		try {
			Map<String, HandlerAbstract> requestHandlerRegistry = new HashMap<String, HandlerAbstract>();
			
			HandlerVersion versionHandler= new HandlerVersion(testVersion);
			requestHandlerRegistry.put(null,versionHandler);
			requestHandlerRegistry.put("",versionHandler);
			requestHandlerRegistry.put("version",versionHandler);
			
			
			RequestDispatcher dispatcher = new RequestDispatcher(requestHandlerRegistry);
			
			assertEquals(3,dispatcher.getRequestHandlerRegistrySize());
			
			HandlerAbstract handler = dispatcher.getHandler("version");
			assertTrue(handler != null);
			
			while(dispatcher.getNumInstantiatingThreadsInvoked() == 0){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			
			while(dispatcher.getNumLiveInstantiatingThreads() > 0){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			
			assertEquals(dispatcher.getNumInstantiatingThreadsInvoked(),1);
			assertEquals(dispatcher.getNumInstancesToStageMax(),dispatcher.getRequestHandlersSize(HandlerVersion.class));
			
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
	}

}
