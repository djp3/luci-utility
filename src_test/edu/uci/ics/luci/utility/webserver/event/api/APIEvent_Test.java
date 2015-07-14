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

package edu.uci.ics.luci.utility.webserver.event.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.HashMap;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.AccessControl;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;
import edu.uci.ics.luci.utility.webserver.event.EventVoid;
import edu.uci.ics.luci.utility.webserver.input.channel.socket.HTTPInputOverSocket;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.socket.Output_Socket_HTTP;

public class APIEvent_Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Globals.setGlobals(new GlobalsTest());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	WebServer ws = null;
	
	private static int testPort = 9020;
	public static synchronized int testPortPlusPlus(){
		int x = testPort;
		testPort++;
		return(x);
	}

	public static WebServer startAWebServerSocket(Globals globals,int port,boolean secure) {
		WebServer ws = null;
		try {
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);
			HashMap<String, APIEvent> requestHandlerRegistry = new HashMap<String,APIEvent>();

			
			// Null is a default Handler
			requestHandlerRegistry.put(null,new APIEvent_Error(Globals.getGlobals().getSystemVersion()));
				
			AccessControl accessControl = new AccessControl();
			accessControl.reset();
			ws = new WebServer(inputChannel, requestHandlerRegistry, accessControl,null);
			
			ws.start();
			
			globals.addQuittable(ws);
			
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
		return ws;
	}
	
	@Test
	public void testEquals() {
		
		try{
			String version = System.currentTimeMillis()+"";
		
			APIEvent a = new APIEvent();
			APIEvent b = (APIEvent) a.clone();
			
			APIEvent_Version c = new APIEvent_Version(version);
			
			assertTrue(!a.equals(null));
			
			assertTrue(!a.equals("Hello world"));
			
			assertTrue(a.equals(c));
			
			assertTrue(a.equals(a));
			assertTrue(a.hashCode() == a.hashCode());
			
			assertTrue(a.equals(b));
			assertTrue(a.hashCode() == b.hashCode());
			
			assertTrue(b.equals(a));
			assertTrue(b.hashCode() == a.hashCode());
			
			/*request equals */
			a.setRequest(new Request());
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setRequest(new Request());
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setRequest(a.getRequest());
			assertTrue(a.equals(b));
			assertTrue(b.equals(a));
			assertTrue(a.hashCode() == b.hashCode());
			
			/*output equals */
			a.setOutput(new Output_Socket_HTTP(null));
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setOutput(new Output_Socket_HTTP(null));
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setOutput(a.getOutput());
			assertTrue(a.equals(b));
			assertTrue(b.equals(a));
			assertTrue(a.hashCode() == b.hashCode());
			
			/*setting */
			b.setOutput(new Output_Socket_HTTP(null));
			assertTrue(!a.equals(b));
			a.set(b);
			assertTrue(a.equals(b));
			
			try{
				a.set(new EventVoid());
				fail("This should throw an exception");
			}
			catch(InvalidParameterException e){
				//okay
			}
			
		
		}catch(Exception e){
			fail("Exception make me fail"+e);
		}
	}
	
	
	
	@Test
	public void testWebServerSocket() {
		
		int port = APIEvent_Test.testPortPlusPlus();
		boolean secure = false;
		ws = APIEvent_Test.startAWebServerSocket(Globals.getGlobals(),port,secure);
		ws.updateAPIRegistry("/event", new APIEvent());

		String responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/");
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

		JSONObject response = null;
		try {
			response = (JSONObject) JSONValue.parse(responseString);
			assertEquals("true",response.get("error"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}
		
		
		
		
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/event");
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

		response = null;
		try {
			response = (JSONObject) JSONValue.parse(responseString);
			assertEquals("false",response.get("error"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}
		
		
		
		
		/* Test callback parameter */
		String callbackFunction = "foo"+System.currentTimeMillis();
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/event")
									.setParameter("callback", callbackFunction);
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

		response = null;
		try {
			assertTrue(responseString.contains(callbackFunction));
			/*Strip the callback*/
			responseString = responseString.substring(callbackFunction.length()+1, responseString.length()-1);
			response = (JSONObject) JSONValue.parse(responseString);
			assertEquals("false",response.get("error"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}
		
		//Globals.getGlobals().setQuitting(true);

	}

}
