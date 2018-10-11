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

package edu.uci.ics.luci.utility.webserver.event.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.webserver.AccessControl;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;
import edu.uci.ics.luci.utility.webserver.input.channel.socket.HTTPInputOverSocket;

public class APIEvent_FileServerSecure_Test {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		GlobalsForTesting.reset("testSupport/APIEvent_Test.log4j.xml");
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		while(Globals.getGlobals() != null){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
			}
		}
		/* First set up the globals in this convoluted way */
		GlobalsForTesting g = new GlobalsForTesting();
		Globals.setGlobals(g);
	
	}

	@AfterEach
	void tearDown() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}
	
	WebServer ws = null;
	

	public static WebServer startAWebServerSocket(Globals globals,int port,boolean secure) {
		WebServer ws = null;
		try {
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);
			HashMap<String, APIEvent> requestHandlerRegistry = new HashMap<String,APIEvent>();

			
			// Null is a default Handler
			requestHandlerRegistry.put(null,new APIEvent_Error(Globals.getGlobals().getSystemVersion()));
				
			AccessControl accessControl = new AccessControl();
			accessControl.reset();
			ws = new WebServer(inputChannel, requestHandlerRegistry, accessControl);
			
			globals.addQuittable(ws);
			
			ws.start();
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
		return ws;
	}
	
	
	
	
	@Test
	public void testWebServerSocket() {
		//The following are just to make sure the testing environment is set up okay.
		//If this test fails the you probably you need to set up the JUnit VM arguments to have these values
		//with a command-line like:
		// -Djavax.net.ssl.keyStore=testSupport/keystore/mySrvKeystore
		// -Djavax.net.ssl.keyStorePassword=password
		// -Dedu.uci.ics.luci.webserver.Alias=server
		// -Djavax.net.ssl.trustStore=testSupport/keystore/myClientTrustStore
		// -Djavax.net.ssl.trustStorePassword=password
		assertEquals(System.getProperty("javax.net.ssl.trustStore"), "testSupport/keystore/myClientTrustStore");
		assertEquals(System.getProperty("javax.net.ssl.trustStorePassword"), "x3mgbgxF9YcJWg8ITUmnMsx37VyR6v4"); //If these were production passwords they shouldn't be in the code
		assertEquals(System.getProperty("javax.net.ssl.keyStore"),"testSupport/keystore/mySrvKeystore");
		assertEquals(System.getProperty("javax.net.ssl.keyStorePassword"),"D5WOgddFtiV0NSqcpFJ6uLU0I1uWe6o"); //If these were production passwords they shouldn't be in the code
		assertEquals(System.getProperty("edu.uci.ics.luci.webserver.Alias"),"server");
				
		
		int port = APIEvent_Test.testPortPlusPlus();

		boolean secure = true;
		ws = APIEvent_FileServerSecure_Test.startAWebServerSocket(Globals.getGlobals(),port,secure);
		ws.updateAPIRegistry(null, new APIEvent_FileServer(edu.uci.ics.luci.utility.Globals.class,"/www_test/"));

		String responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/index.html");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception");
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}
		//System.out.println(responseString);
		if(responseString == null) {
			fail("Did not get a response");
		}
		else {
			assertTrue(responseString.contains("<h1>This is a test html file</h1>"));
		}


	}

}
