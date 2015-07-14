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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.disruptor.eventhandlers.dispatch.HandlerUnstableWrapper;
import edu.uci.ics.luci.utility.webserver.disruptor.eventhandlers.server.ServerCallHandler_Shutdown;
import edu.uci.ics.luci.utility.webserver.event.api.HandlerAbstractTest;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerVersion;

public class WebserverFailureTest {
	
	static final int NUM_TESTS=1000;
	
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

	@Before
	public void setUp() throws Exception {
		int port = HandlerAbstractTest.testPortPlusPlus();
		boolean secure = false;
		ws = HandlerAbstractTest.startAWebServerSocket(Globals.getGlobals(),port,secure);
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/shutdown",new ServerCallHandler_Shutdown(Globals.getGlobals()));
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	

	@Test
	/** Make sure everything works when the webserver handler is crashing **/
	public void testWebserverFailure() {
		//long start = System.currentTimeMillis();
		
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/",new HandlerUnstableWrapper(1.0d,0,new ServerCallHandler_Version(Globals.getGlobals().getSystemVersion())));

		try {
			URIBuilder uriBuilder = new URIBuilder()
										.setScheme("http")
										.setHost("localhost")
										.setPort(ws.getInputChannel().getPort())
										.setPath("/");
			for(int i = 0; i< NUM_TESTS; i++){
				try{
					WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
					fail("Expecting an exception");
				}
				catch(NoHttpResponseException e){
					//It's an expected fail
				}
			}
				
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
		//long end = System.currentTimeMillis();
		
		//System.err.println(NUM_TESTS+" tests took "+((end-start)/1000.0)+" seconds");
	}
	
	@Test
	/* This test was to help profile the failure case to debug and make sure it was clean */
	//for i in {1..10}; do echo $i;wget -t 1 -O - "http://localhost:9020/" ;done; wget -O - "http://localhost:9020/shutdown"
	public void testWebserverFailureExternalRequests() {
		
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/",new HandlerUnstableWrapper(1.0d,0,new ServerCallHandler_Version(Globals.getGlobals().getSystemVersion())));
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/version",new HandlerUnstableWrapper(1.0d,0,new ServerCallHandler_Version(Globals.getGlobals().getSystemVersion())));
		
		//long start = System.currentTimeMillis();
		
		boolean testingExternally = false;
		
		if(!testingExternally){
		
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		
			try {
				URIBuilder uriBuilder = new URIBuilder()
					.setScheme("http")
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
		//long end = System.currentTimeMillis();
		
		//System.err.println("That took "+((end-start)/1000.0)+" seconds");

	}
	

}
