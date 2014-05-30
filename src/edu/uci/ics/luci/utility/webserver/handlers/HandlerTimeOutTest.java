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

package edu.uci.ics.luci.utility.webserver.handlers;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.AccessControl;
import edu.uci.ics.luci.utility.webserver.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.RequestDispatcher;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;

public class HandlerTimeOutTest {
	
	private static int testPort = 9020;
	private static synchronized int testPortPlusPlus(){
		int x = testPort;
		testPort++;
		return(x);
	}
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		Globals.setGlobals(new GlobalsTest());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}

	private WebServer ws = null;

	HashMap<String,HandlerAbstract> requestHandlerRegistry;
	

	@Before
	public void setUp() throws Exception {
		startAWebServer(testPortPlusPlus());
	}

	@After
	public void tearDown() throws Exception {
	}
	
	

	private void startAWebServer(int port) {
		try {
			requestHandlerRegistry = new HashMap<String,HandlerAbstract>();
			requestHandlerRegistry.put(null,new HandlerTimeOut());
			
			RequestDispatcher requestDispatcher = new RequestDispatcher(requestHandlerRegistry);
			ws = new WebServer(requestDispatcher, port, false, new AccessControl());
			ws.start();
			Globals.getGlobals().addQuittable(ws);
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
	}
	

	
	@Test
	public void testWebServer() {
		
		try {
			HashMap<String, String> params = new HashMap<String, String>();

			WebUtil.fetchWebPage("http://localhost:" + ws.getPort() + "/", false, params, 5 * 1000);
			fail("Shouldn't have returned cleanly");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (SocketTimeoutException e) {
			/* This is what we want to happen */
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception");
		}

	}

}
