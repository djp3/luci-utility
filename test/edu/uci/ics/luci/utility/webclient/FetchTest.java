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

package edu.uci.ics.luci.utility.webclient;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.datastructure.Pair;

public class FetchTest {


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
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
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
	public void testSorting() throws URISyntaxException{
		
		Fetch f = new Fetch();
		Map<URI,Long> urlMap = new HashMap<URI,Long>();
		
		
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776).build(),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779).build(),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780).build(),3L);
		f.resetUrlPool(urlMap);
		
		for(int i = 0 ;i < 100; i++){
			TreeSet<Pair<Long, URI>> servers = new TreeSet<Pair<Long,URI>>(f.getUrlPoolCopy());
			assertTrue(servers.pollFirst().getSecond().toString().contains("1776"));
			assertTrue(servers.pollLast().getSecond().toString().contains("1780"));
		}
	}
	
	
	@Test
	public void testFetch(){
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("raw.github.com").build());
		
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"),null,null,null,30000);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		} catch (URISyntaxException e) {
			fail(""+e);
		}
	}
	
	@Test
	public void testFetchJSONWithBadServers() throws URISyntaxException{
		Map<URI,Long> urlMap = new HashMap<URI,Long>();
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776).build(),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776).build(),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779).build(),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780).build(),3L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("raw.github.com").build(),4L);
		
		Fetch f = new Fetch(urlMap);
		try {
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"),null,null,null,30000);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		} catch (URISyntaxException e) {
			fail(""+e);
		}
	}
	
	
	@Test
	public void testFetchWebpageWithBadServers() throws URISyntaxException{
		Map<URI,Long> urlMap = new HashMap<URI,Long>();
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776).build(),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776).build(),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778).build(),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779).build(),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780).build(),3L);
		urlMap.put(new URIBuilder().setScheme("https").setHost("github.com").build(),4L);
		
		Fetch f = new Fetch(urlMap);
		try {
			String s = f.fetchWebPage(new URIBuilder().setPath("/djp3/p2p4java/blob/production/bootstrapMasterList.json"),null,null,null,30000);
			assertTrue(s.contains("tcp"));
		} catch (MalformedURLException e) {
			fail(""+e);
		} catch (IOException e) {
			fail(""+e);
		} catch (URISyntaxException e) {
			fail(""+e);
		}
	}
	

}

