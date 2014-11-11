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

import static org.junit.Assert.assertEquals;
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

public class HandlerFileServerSecureTest {
	
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
		int port = HandlerAbstractTest.testPortPlusPlus();
		boolean secure = true;
		ws = HandlerAbstractTest.startAWebServerSocket(Globals.getGlobals(),port,secure);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	@Test
	public void testWebServerSecureSocket() {
		//The following are just to make sure the testing environment is set up okay.
		//If this test fails the you probably you need to set up the JUnit VM arguments to have these values
		//with a command-line like:
		// -Djavax.net.ssl.keyStore=test/mySrvKeystore
		// -Djavax.net.ssl.keyStorePassword=password
		// -Dedu.uci.ics.luci.webserver.Alias=server
		// -Djavax.net.ssl.trustStore=test/myClientTrustStore
		// -Djavax.net.ssl.trustStorePassword=password
		assertEquals(System.getProperty("javax.net.ssl.trustStore"), "test/myClientTrustStore");
		assertEquals(System.getProperty("javax.net.ssl.trustStorePassword"), "password");
		assertEquals(System.getProperty("javax.net.ssl.keyStore"),"test/mySrvKeystore");
		assertEquals(System.getProperty("javax.net.ssl.keyStorePassword"),"password");
		assertEquals(System.getProperty("edu.uci.ics.luci.webserver.Alias"),"server");
		
		String responseString = null;
		try {
			HandlerAbstract handler = new HandlerFileServer(edu.uci.ics.luci.utility.Globals.class,"/www_test/");
			ws.getRequestDispatcher().updateRequestHandlerRegistry(null,handler);
			
			handler = new HandlerVersion(Globals.getGlobals().getSystemVersion());
			ws.getRequestDispatcher().updateRequestHandlerRegistry("",handler);

			HashMap<String, String> params = new HashMap<String, String>();
			
			responseString = WebUtil.fetchWebPage("https://localhost:" + ws.getInputChannel().getPort() + "/index.html", false, params, 30 * 1000);
		} catch (MalformedURLException e) {
			fail("Bad URL\n"+e);
		} catch (IOException e) {
			fail("IO Exception\n"+e);
		}
		
		assertTrue(responseString.contains("<h1>This is a test html file</h1>"));
		

	}
	

}
