package edu.uci.ics.luci.utility.webserver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Favicon;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_FileServer;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Shutdown;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Test;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Version;
import edu.uci.ics.luci.utility.webserver.input.channel.socket.HTTPInputOverSocket;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

class BasicWebServerTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}
	
	
	private static WebServer startAWebServer(GlobalsTest globals,int port,boolean secure) {

		WebServer ws = null;
		HashMap<String, APIEvent> requestHandlerRegistry;

		try {
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);

			requestHandlerRegistry = new HashMap<String,APIEvent>();
			
			// Null returns a favicon
			APIEvent_Favicon api = new APIEvent_Favicon(
								new URIBuilder().setScheme("https")
								.setHost("assets-cdn.github.com")
								//.setPort(80)
								.setPath("/favicon.ico"));
			requestHandlerRegistry.put(null, api);
			requestHandlerRegistry.put("", api);
			requestHandlerRegistry.put("/", api);
			requestHandlerRegistry.put("/version", new APIEvent_Version(globals.getSystemVersion()));
			requestHandlerRegistry.put("/content",new APIEvent_FileServer(edu.uci.ics.luci.utility.GlobalsForTesting.class,"/www_test"));
			requestHandlerRegistry.put("/shutdown", new APIEvent_Shutdown(Globals.getGlobals()));

			//	requestHandlerRegistry.put("/version", new WAPIEvent_VersionCheck(VERSION));
			/*requestHandlerRegistry.put("/session/initiate", new QAPIEvent_InitiateSession(VERSION,db));
			requestHandlerRegistry.put("/session/check", new QAPIEvent_CheckSession(VERSION,db));
			requestHandlerRegistry.put("/session/kill", new QAPIEvent_KillSession(VERSION,db));
			requestHandlerRegistry.put("/get/patients", new QAPIEvent_GetPatients(VERSION,db));
			requestHandlerRegistry.put("/get/patient/procedures", new QAPIEvent_GetPatientProcedures(VERSION,db));
			requestHandlerRegistry.put("/add/patient", new QAPIEvent_AddPatient(VERSION,db));
			requestHandlerRegistry.put("/update/patient", new QAPIEvent_UpdatePatient(VERSION,db));
			requestHandlerRegistry.put("/add/procedure", new QAPIEvent_AddProcedure(VERSION,db));
			requestHandlerRegistry.put("/update/procedure", new QAPIEvent_UpdateProcedure(VERSION,db));
			requestHandlerRegistry.put("/add/polyp", new QAPIEvent_AddPolyp(VERSION,db));
			requestHandlerRegistry.put("/get/procedure/polyps", new QAPIEvent_GetProcedurePolyps(VERSION,db));
			requestHandlerRegistry.put("/update/polyp", new QAPIEvent_UpdatePolyp(VERSION,db));
			requestHandlerRegistry.put("/login", new QAPIEvent_Login(VERSION,db));
			requestHandlerRegistry.put("/shutdown", new APIEvent_Shutdown(Globals.getGlobals()));
			*/
							
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
	void testResponse() {
		/* First set up the globals in this convoluted way */
		GlobalsForTesting.reset("testSupport/BasicWebServerTest.log4j.xml");
		GlobalsForTesting g = new GlobalsForTesting();
		Globals.setGlobals(g);
		
		JSONParser p = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
		
		/* Now kickoff the webserver */
		int port = APIEvent_Test.testPortPlusPlus();

		WebServer ws = startAWebServer(g,port,true);
		if(ws == null) {
			fail("Webserver should have started");
		}
		
		/* Test the favicon response */
		String responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
			assertEquals(6518,responseString.length()); //This is github's favicon size
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
		
		
		/* Test the version response*/
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/version");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
			JSONObject j = (JSONObject) p.parse(responseString);
			assertEquals(Globals.getGlobals().getSystemVersion(),j.getAsString("version"));
			assertEquals("false",j.getAsString("error"));
			assertEquals(p.parse("[]"),j.get("errors"));
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
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Webserver did not return valid JSON");
		}
		
		
		/* Test the file server response */
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/content/index.html");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
			//System.out.println(responseString);
			assertTrue(responseString.contains("<h1>This is a test html file</h1>"));
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
		
		/* Test the shutdown response */
		responseString = null;
		try {
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("localhost")
									.setPort(ws.getInputChannel().getPort())
									.setPath("/shutdown");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
			JSONObject j = (JSONObject) p.parse(responseString);
			assertEquals("false",j.getAsString("error"));
			assertEquals(p.parse("[]"),j.get("errors"));
			assertTrue(Globals.getGlobals().isQuitting());
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
		} catch (ParseException e) {
			e.printStackTrace();
			fail("Parse Exception unexpected");
		}
	}


}
