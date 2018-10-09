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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.HashMap;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;
import edu.uci.ics.luci.utility.webserver.event.EventVoid;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.socket.Output_Socket_HTTP;

public class APIEvent_TimeOut_Test {

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
	
	private WebServer ws = null;

	HashMap<String,APIEvent> requestHandlerRegistry;

	@Test
	public void test() {
		
		try{
			String version = System.currentTimeMillis()+"";
		
			APIEvent_TimeOut a = new APIEvent_TimeOut();
			APIEvent_TimeOut b = (APIEvent_TimeOut) a.clone();
			
			APIEvent_Error c = new APIEvent_Error(version);
			
			assertTrue(!a.equals(null));
			
			assertTrue(!a.equals("Hello world"));
			
			assertTrue(!a.equals(c));
			
			assertTrue(a.equals(a));
			assertTrue(a.hashCode() == a.hashCode());
			
			assertTrue(a.equals(b));
			assertTrue(a.hashCode() == b.hashCode());
			
			assertTrue(b.equals(a));
			assertTrue(b.hashCode() == a.hashCode());
			
			/*parameter equals */
			a.setTimeOuter(new Thread());
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setTimeOuter(new Thread());
			assertTrue(!a.equals(b));
			assertTrue(!b.equals(a));
			assertTrue(a.hashCode() != b.hashCode());
			
			b.setTimeOuter(a.getTimeOuter());
			assertTrue(a.equals(b));
			assertTrue(b.equals(a));
			assertTrue(a.hashCode() == b.hashCode());
			
			
			/*setting */
			b.setRequest(new Request());
			b.setOutput(new Output_Socket_HTTP(null));
			b.setTimeOuter(new Thread());
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
		ws.updateAPIRegistry("/test", new APIEvent_TimeOut());

		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/test");
			WebUtil.fetchWebPage(uriBuilder, null,null, null, 2 * 1000);
			fail("This should time out");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (SocketTimeoutException e) {
			/* This is what we want to happen */
		} catch (IOException e) {
			e.printStackTrace();
			fail("Bad URL");
		}catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}
		
		
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/test");
			WebUtil.fetchWebPage(uriBuilder, null,null, null, 2 * 1000);
			fail("This should time out");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (SocketTimeoutException e) {
			/* This is what we want to happen */
		} catch (IOException e) {
			e.printStackTrace();
			fail("Bad URL");
		}catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}
		

	}

}
