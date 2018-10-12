package edu.uci.ics.luci.utility.webserver;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.datastructure.Pair;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

class WebUtilTest {


	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		GlobalsForTesting.reset("testSupport/Everything.log4j.xml");
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

	@Test
	void testFetchWebPage() {
		String responseString = null;
		try {
			String apikey = System.getProperty("edu.uci.ics.luci.webclient.googleapikey");
			
			Map<String,String> headerFields = new HashMap<String,String>();
			headerFields.put("hello", "world"); //Dummy headers
			
			Map<String,List<String>> returnFields = new HashMap<String,List<String>>();

			// Fetch with headers and recieves and a 30 second timeout
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("www.googleapis.com")
									.setPath("/customsearch/v1/siterestrict")
									.addParameter("key", apikey)
									.addParameter("prettyPrint","false")
									.addParameter("cx","002236725734790388270:jhl5yb2gyqo") // Custom search engine
									.addParameter("q","djp3");
			responseString = WebUtil.fetchWebPage(uriBuilder, headerFields, returnFields, new Pair<String,String>("djp3","dummypassword"), 30 * 1000);
			//System.out.println(returnFields.toString());
			JSONObject response = null;
			try {
				response = (JSONObject) JSONValue.parse(responseString);
				//System.out.println(response.toJSONString(JSONStyle.NO_COMPRESS);
				assertEquals(response.get("kind"), "customsearch#search");
				assertEquals(((JSONObject)response.get("context")).getAsString("title"),"Github");
				assertTrue(returnFields.containsKey("application/json"));
			} catch (ClassCastException e) {
				fail("Bad JSON Response");
			}
			
			
			
			// Fetch with minimal stuff in the parameters
			responseString = null;
			uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("www.googleapis.com")
									.setPath("/customsearch/v1/siterestrict")
									.addParameter("key", apikey)
									.addParameter("prettyPrint","false")
									.addParameter("cx","002236725734790388270:jhl5yb2gyqo") // Custom search engine
									.addParameter("q","djp3");
			responseString = WebUtil.fetchWebPage(uriBuilder, null,null , null,-1);
			//System.out.println(returnFields.toString());
			response = null;
			try {
				response = (JSONObject) JSONValue.parse(responseString);
				//System.out.println(response.toJSONString(JSONStyle.NO_COMPRESS);
				assertEquals(response.get("kind"), "customsearch#search");
				assertEquals(((JSONObject)response.get("context")).getAsString("title"),"Github");
				assertTrue(returnFields.containsKey("application/json"));
			} catch (ClassCastException e) {
				fail("Bad JSON Response");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}

	}
	
	
	@Test
	void testFetchWebPageHeader() {
		String responseString = null;
		try {
			String apikey = System.getProperty("edu.uci.ics.luci.webclient.googleapikey");
			
			Map<String,String> headerFields = new HashMap<String,String>();
			headerFields.put("hello", "world"); //Dummy headers
			
			Map<String,List<String>> returnFields = new HashMap<String,List<String>>();

			// Fetch with headers and recieves and a 30 second timeout
			URIBuilder uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("www.googleapis.com")
									.setPath("/customsearch/v1/siterestrict")
									.addParameter("key", apikey)
									.addParameter("prettyPrint","false")
									.addParameter("cx","002236725734790388270:jhl5yb2gyqo") // Custom search engine
									.addParameter("q","djp3");
			responseString = WebUtil.fetchWebPageHeader(uriBuilder, headerFields, returnFields, new Pair<String,String>("djp3","dummypassword"), 30 * 1000);
			System.out.println(returnFields.toString());
			assertNull(responseString);
			assertTrue(returnFields.containsKey("application/json"));
			
			
			
			// Fetch with minimal stuff in the parameters
			responseString = null;
			uriBuilder = new URIBuilder()
									.setScheme("https")
									.setHost("www.googleapis.com")
									.setPath("/customsearch/v1/siterestrict")
									.addParameter("key", apikey)
									.addParameter("prettyPrint","false")
									.addParameter("cx","002236725734790388270:jhl5yb2gyqo") // Custom search engine
									.addParameter("q","djp3");
			responseString = WebUtil.fetchWebPageHeader(uriBuilder, null,null , null,-1);
			//System.out.println(returnFields.toString());
			assertTrue(returnFields.containsKey("application/json"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			fail("Bad URL");
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO Exception");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			fail("URISyntaxException");
		}

	}

}
