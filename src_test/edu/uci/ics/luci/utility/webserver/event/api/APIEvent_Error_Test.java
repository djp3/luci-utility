package edu.uci.ics.luci.utility.webserver.event.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.WebUtil;

public class APIEvent_Error_Test {

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
	
	private WebServer ws = null;

	HashMap<String,APIEvent> requestHandlerRegistry;

	@Test
	public void test() {
		
		try{
			String version = System.currentTimeMillis()+"";
		
			APIEvent_Error a = new APIEvent_Error(version);
			APIEvent b = (APIEvent) a.clone();
			
			APIEvent_Version c = new APIEvent_Version(version);
			
			assertTrue(!a.equals(null));
			
			assertTrue(!a.equals("Hello world"));
			
			assertTrue(!a.equals(c));
			
			assertTrue(a.equals(a));
			assertTrue(a.hashCode() == a.hashCode());
			
			assertTrue(a.equals(b));
			assertTrue(a.hashCode() == b.hashCode());
			
			assertTrue(b.equals(a));
			assertTrue(b.hashCode() == a.hashCode());
		
		}catch(Exception e){
			fail("Exception make me fail"+e);
		}
	}
	
	
	@Test
	public void testWebServerSocket() {
		String version = System.currentTimeMillis()+"";
		
		int port = APIEvent_Test.testPortPlusPlus();
		boolean secure = false;
		ws = APIEvent_Test.startAWebServerSocket(Globals.getGlobals(),port,secure);
		ws.updateAPIRegistry("/error", new APIEvent_Error(version));

		String responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("http")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/error");
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
		System.out.println(responseString);

		JSONObject response = null;
		try {
			response = (JSONObject) JSONValue.parse(responseString);
			assertEquals("true",response.get("error"));
		} catch (ClassCastException e) {
			fail("Bad JSON Response");
		}
		
		//Globals.getGlobals().setQuitting(true);

	}

}
