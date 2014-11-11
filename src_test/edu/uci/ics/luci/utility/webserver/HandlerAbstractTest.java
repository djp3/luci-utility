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

import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerError;

public class HandlerAbstractTest {

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
	
	private static int testPort = 9020;
	public static synchronized int testPortPlusPlus(){
		int x = testPort;
		testPort++;
		return(x);
	}

	public static WebServer startAWebServerSocket(Globals globals,int port,boolean secure) {
		try {
			InputChannelSocket inputChannel = new InputChannelSocket(port,secure);
			HashMap<String, HandlerAbstract> requestHandlerRegistry = new HashMap<String,HandlerAbstract>();
			requestHandlerRegistry.put(null,new HandlerError(Globals.getGlobals().getSystemVersion()));
				
			RequestDispatcher requestDispatcher = new RequestDispatcher(requestHandlerRegistry);
			WebServer ws = new WebServer(inputChannel, requestDispatcher, new AccessControl());
			ws.start();
			globals.addQuittable(ws);
			return ws;
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
		return null;
	}

	/*
	@Test
	public void testEncodeURIComponent() {
		String orig ="\"A\" B ± \"";
		String x = HandlerAbstract.encodeURIComponent(orig);
		assertEquals("%22A%22%20B%20%C2%B1%20%22",x);
		assertEquals(orig, HandlerAbstract.decodeURIComponent(x));
		
		orig ="! ' ( ) + ~";
		x = HandlerAbstract.encodeURIComponent(orig);
		assertEquals(orig, HandlerAbstract.decodeURIComponent(x));
		
	}
	*/
	

}
