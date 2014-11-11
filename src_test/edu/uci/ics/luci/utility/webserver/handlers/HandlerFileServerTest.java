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

package edu.uci.ics.luci.utility.webserver.handlers;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.HandlerAbstractTest;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;

public class HandlerFileServerTest {
	
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
		int port = testPortPlusPlus();
		boolean secure = false;
		ws = HandlerAbstractTest.startAWebServerSocket(Globals.getGlobals(),port,secure);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	@Test
	public void testWebServerSocket() {
		
		String responseString = null;
		try {
			HashMap<String, String> params = new HashMap<String, String>();

			responseString = WebUtil.fetchWebPage("http://localhost:" + ws.getInputChannel().getPort() + "/", false, params, 30 * 1000);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception");
		}
		
		assertTrue(responseString.contains("<h1>This is a test html file</h1>"));
		

	}



	

}
