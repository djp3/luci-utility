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

package edu.uci.ics.luci.utility.webserver.handlers.login;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public class HandlerLoginSystemTest extends HandlerAbstract {

	
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
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	@Test
	public void testWebServerSocket() {
		
		String responseString = null;
		try {
			HandlerAbstract handler = new HandlerFileServer(edu.uci.ics.luci.utility.Globals.class,"/www_test/");
			ws.getRequestDispatcher().updateRequestHandlerRegistry(null,handler);
			
			ws.getRequestDispatcher().updateRequestHandlerRegistry("/",handler);

			URIBuilder uriBuilder = new URIBuilder()
										.setScheme("http")
										.setHost("localhost")
										.setPort(ws.getInputChannel().getPort())
										.setPath("/index.html");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception "+e);
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}
		
		assertTrue(responseString.contains("<h1>This is a test html file</h1>"));
		

	}
	
	@Test
	public void testLogin(){
		

		Datastore datastore = new DatastoreSQLite());
    	if(!datastore.isThreadSafe()){
    		throw new RuntimeException("Your Datastore implementation is not thread safe and can't be used as a server");
    	}
    	
		ObjectRelationalMapper orm = new ObjectRelationalMapper(new DatastoreSQLite()));

    	SQLiteQueue q = null;
	    	
	   	q = new SQLiteQueue(new File("localDatabase"));
		q.start();
		if(!SQLiteHelper.databaseExists(q)){
			SQLiteHelper.wipeDatabase(q);
			SQLiteHelper.createDatabase(q);
		}
			
		Globals.setGlobals(new GlobalsTest());
		
		WebServer ws = null;
		HashMap<String, HandlerAbstract> requestHandlerRegistry;
		
		try {
			boolean secure = false;
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);
			requestHandlerRegistry = new HashMap<String,HandlerAbstract>();

			requestHandlerRegistry.put(null,new HandlerError(Globals.getGlobals().getSystemVersion()));
			requestHandlerRegistry.put("",new HandlerReflect());
			requestHandlerRegistry.put("/add_stroke",new HandlerAddStroke(q));
			requestHandlerRegistry.put("/get_data",new HandlerGetData(q));
			requestHandlerRegistry.put("/shutdown",new HandlerShutdown(Globals.getGlobals()));
	
			RequestDispatcher requestDispatcher = new RequestDispatcher(requestHandlerRegistry);
			ws = new WebServer(inputChannel, requestDispatcher, new AccessControl());
			ws.start();
			Globals.getGlobals().addQuittable(ws);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Globals.getGlobals().addQuittable(ws);
			
			Globals.getGlobals().addQuittable(new cleanUpDatabase(q));
			
			SQLiteHelper.addTestData(q);
			
			Collection<String> groupNames = SQLiteHelper.getGroupNames(q);
			System.out.println("Here are the known groups:");
			for(String s: groupNames){
				System.out.println("\t"+s);
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			Globals.getGlobals().setQuitting(true);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Globals.getGlobals().setQuitting(true);
		} catch (RuntimeException e) {
			e.printStackTrace();
			Globals.getGlobals().setQuitting(true);
		}
		
		
	}
	

}

}


