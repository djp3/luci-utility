package edu.uci.ics.luci.utility.webclient;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;
import edu.uci.ics.luci.utility.datastructure.Pair;

public class FetchTest {

	private static int testPort = 9020;
	private static synchronized int testPortPlusPlus(){
		int x = testPort;
		testPort++;
		return(x);
	}


	@BeforeClass
	public static void setUpClass() throws Exception {
		while(Globals.getGlobals() != null){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
			}
		}
		Globals.setGlobals(new GlobalsTest());
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}
	

	private int workingPort;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testJSONAssumptions(){
		JSONObject parse = (JSONObject)JSONValue.parse("{hello:world}");
		assertTrue(parse != null);
		assertTrue(parse.get("hello").equals("world"));
		assertTrue(parse.get("world") == null);
		parse = (JSONObject)JSONValue.parse("{blah}");
		assertTrue(parse == null);
		
		String testcase = "";
		try{
			parse = (JSONObject)JSONValue.parse(testcase);
			fail("Should parse as string");
		}
		catch(ClassCastException e){
			assertTrue("".equals(JSONValue.parse(testcase)));
		}
		parse = (JSONObject)JSONValue.parse((String)null);
		assertTrue(parse == null);
	}
	
	@Test
	public void testSorting(){
		
		Fetch f = new Fetch();
		Map<String,Long> urlMap = new HashMap<String,Long>();
		
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1777",1L);
		urlMap.put("http://localhost:1778",1L);
		urlMap.put("http://localhost:1779",2L);
		urlMap.put("http://localhost:1780",3L);
		f.resetUrlPool(urlMap);
		
		for(int i = 0 ;i < 100; i++){
			TreeSet<Pair<Long, String>> servers = new TreeSet<Pair<Long,String>>(f.getUrlPoolCopy());
			assertTrue(servers.pollFirst().getSecond().contains("1776"));
			assertTrue(servers.pollLast().getSecond().contains("1780"));
		}
	}
	
	
	@Test
	public void testFetch(){
		Fetch f = new Fetch("https://raw.github.com");
		try {
			JSONObject j = f.fetchJSONObject("/djp3/p2p4java/production/rendezvousNodeMasterList.json",false,null,30000);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		}
	}
	
	@Test
	public void testFetchJSONWithBadServers(){
		Map<String,Long> urlMap = new HashMap<String,Long>();
		
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1777",1L);
		urlMap.put("http://localhost:1778",1L);
		urlMap.put("http://localhost:1779",2L);
		urlMap.put("http://localhost:1780",3L);
		urlMap.put("https://raw.github.com",4L);
		Fetch f = new Fetch(urlMap);
		try {
			JSONObject j = f.fetchJSONObject("/djp3/p2p4java/production/rendezvousNodeMasterList.json",false,null,30000);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		}
	}
	
	
	@Test
	public void testFetchWebpageWithBadServers(){
		Map<String,Long> urlMap = new HashMap<String,Long>();
		
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1776",0L);
		urlMap.put("http://localhost:1777",1L);
		urlMap.put("http://localhost:1778",1L);
		urlMap.put("http://localhost:1779",2L);
		urlMap.put("http://localhost:1780",3L);
		urlMap.put("https://github.com",4L);
		Fetch f = new Fetch(urlMap);
		try {
			String s = f.fetchWebPage("/djp3/p2p4java/blob/production/rendezvousNodeMasterList.json",false,null,30000);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		}
	}
	

}

