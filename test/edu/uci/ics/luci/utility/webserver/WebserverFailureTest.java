/*
	Copyright 2018
		Donald J. Patterson
*/
/*
	This file is part of the Witness This Media Web Service, i.e. "Witness"

    Witness is free software: you can redistribute it and/or modify
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


import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Shutdown;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_UnstableWrapper;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Version;
import edu.uci.ics.luci.utility.webserver.input.channel.socket.HTTPInputOverSocket;

public class WebserverFailureTest {
	
	static final int NUM_TESTS=10;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		GlobalsForTesting.reset("testSupport/JustFatals.log4j.xml");
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
	
	}
	
	
	private static WebServer startAWebServer(Globals globals,int port,boolean secure) {

		WebServer ws = null;
		HashMap<String, APIEvent> requestHandlerRegistry;

		try {
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);

			requestHandlerRegistry = new HashMap<String,APIEvent>();
			
			requestHandlerRegistry.put("/version", new APIEvent_UnstableWrapper(1.0d,0, new APIEvent_Version(globals.getSystemVersion())));
			requestHandlerRegistry.put("/shutdown", new APIEvent_Shutdown(Globals.getGlobals()));

			AccessControl accessControl = new AccessControl();
			accessControl.reset();
			ws = new WebServer(inputChannel, requestHandlerRegistry, accessControl);
			
			Globals.getGlobals().addQuittable(ws);
			
			ws.start();

		} catch (RuntimeException e) {
			e.printStackTrace();
			Globals.getGlobals().setQuitting(true);
			return null;
			}
		
		return ws;
	}


	@Test
	/** Make sure everything works when the webserver handler is crashing **/
	public void testWebserverFailure() {

		try {
			/* Now kickoff the webserver */
			WebServer ws = startAWebServer(Globals.getGlobals(), 1776, true);
			if (ws == null) {
				fail("Webserver should have started");
			} else {

				try {
					URIBuilder uriBuilder = new URIBuilder()
												.setScheme("https")
												.setHost("localhost")
												.setPort(ws.getInputChannel().getPort())
												.setPath("/version");
					for (int i = 0; i < NUM_TESTS; i++) {
						try {
							WebUtil.fetchWebPage(uriBuilder, null, null, null, 1 * 1000);
							fail("Expecting an exception");
						} catch (SocketTimeoutException e) {
							// It's an expected fail
						}
					}

				} catch (MalformedURLException e) {
					e.printStackTrace();
					fail("Bad URL");
				} catch (HttpResponseException e) {
					fail("HttpResponseException");
				} catch (IOException e) {
					e.printStackTrace();
					fail("IO Exception " + e);
				} catch (URISyntaxException e) {
					e.printStackTrace();
					fail("URISyntaxException");
				}
			}
		} finally {
			Globals.getGlobals().setQuitting(true);
			Globals.setGlobals(null);
		}
	}

	@Test
	/* This test was to help profile the failure case to debug and make sure it was clean */
	//for i in {1..10}; do echo $i;wget -t 1 -O - "http://localhost:9020/" ;done; wget -O - "http://localhost:9020/shutdown"
	public void testWebserverFailureExternalRequests() {
		
		boolean testingExternally = false;

		/* Now kickoff the webserver */
		WebServer ws = startAWebServer(Globals.getGlobals(),1777,true);
		if(ws == null) {
			fail("Webserver should have started");
		}
		
		if(!testingExternally) {
		
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		
			try {
				URIBuilder uriBuilder = new URIBuilder()
					.setScheme("https")
					.setHost("localhost")
					.setPort(ws.getInputChannel().getPort())
					.setPath("/shutdown");
				
				WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
				fail("Bad URL");
			} catch(SocketTimeoutException e){
				fail("SocketTimeoutException");
			} catch(HttpResponseException e){
				fail("HttpResponseException");
			} catch (IOException e) {
				e.printStackTrace();
				fail("IO Exception");
			}
			catch (URISyntaxException e) {
				e.printStackTrace();
				fail("URISyntaxException");
			}
		}

		while(!Globals.getGlobals().isQuitting()){
			try {
				Thread.sleep(100);
				//System.out.println(Globals.getGlobals().isQuitting());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
	

}
