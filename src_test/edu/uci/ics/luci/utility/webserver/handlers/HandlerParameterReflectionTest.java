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

package edu.uci.ics.luci.utility.webserver.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

import net.minidev.json.JSONArray;
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
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;

public class HandlerParameterReflectionTest {
	
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
		boolean secure = false;
		ws = HandlerAbstractTest.startAWebServerSocket(Globals.getGlobals(),port,secure);
		HandlerParameterReflection handler = new HandlerParameterReflection(Globals.getGlobals().getSystemVersion());
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/",handler);
		ws.getRequestDispatcher().updateRequestHandlerRegistry("/reflect",handler);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	
	@Test
	public void testWebServer() {
		
		String responseString = null;
		try {
			new URIBuilder();
			URIBuilder uriBuilder = new URIBuilder()
										.setScheme("http")
										.setHost("localhost")
										.setPort(ws.getInputChannel().getPort())
										.setPath("/")
										.setParameter("a","A")
										.setParameter("b","B")
										.setParameter("c","C");
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
			assertEquals("false",response.get("error"));
			
			JSONObject parameters = (JSONObject) response.get("parameters");
			JSONArray A = (JSONArray) parameters.get("a");
			JSONArray B = (JSONArray) parameters.get("b");
			JSONArray C = (JSONArray) parameters.get("c");
			assertTrue(A.get(0) != null);
			assertTrue(B.get(0) != null);
			assertTrue(C.get(0) != null);
			assertTrue(A.get(0).equals("A"));
			assertTrue(B.get(0).equals("B"));
			assertTrue(C.get(0).equals("C"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}
		
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
										.setScheme("http")
										.setHost("localhost")
										.setPort(ws.getInputChannel().getPort())
										.setPath("/reflect")
										.setParameter("a","A")
										.setParameter("b","B")
										.setParameter("c","C");
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
			
			JSONObject parameters = (JSONObject) response.get("parameters");
			JSONArray A = (JSONArray) parameters.get("a");
			JSONArray B = (JSONArray) parameters.get("b");
			JSONArray C = (JSONArray) parameters.get("c");
			assertTrue(A.get(0) != null);
			assertTrue(B.get(0) != null);
			assertTrue(C.get(0) != null);
			assertTrue(A.get(0).equals("A"));
			assertTrue(B.get(0).equals("B"));
			assertTrue(C.get(0).equals("C"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}

	}



	

}
