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


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.bouncycastle.tls.TlsNoCloseNotifyException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.cache.CacheStats;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webclient.Fetch.Configuration;
import edu.uci.ics.luci.utility.webclient.Fetch.Result;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class FetchTest {


	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		//GlobalsForTesting.reset("testSupport/JustFatals.log4j.xml");
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
		
		//Reset caches for measurements
		FetchSSLTrustManager.configurationOCSP.useCache = true;
		FetchSSLTrustManager.configurationOCSP.trackCacheStats = true;
		FetchSSLTrustManager.configurationOCSP.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.configurationCRL.useCache = true;
		FetchSSLTrustManager.configurationCRL.trackCacheStats = true;
		FetchSSLTrustManager.configurationCRL.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.configurationCT.useCache = true;
		FetchSSLTrustManager.configurationCT.trackCacheStats = true;
		FetchSSLTrustManager.configurationCT.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.resetCaches();
		//Just execute a call that will trigger the static start-up work so it
		// doesn't affect timing measurements.
		FetchSSLTrustManager.getLog(); 
	}
	

	@AfterEach
	void tearDown() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}
	
	
	@Test
	public void testLog() {
		assertNotNull(Fetch.getLog());
		assertNotNull(Fetch.getLog());
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
		
		Random r = new Random();
		Fetch f = new Fetch();
		
		/* Run 100 experiments */
		for(int i = 0 ;i < 100; i++){
			Map<URIBuilder,Long> urlMap = new HashMap<URIBuilder,Long>();
		
			/* Add 0 - 3 elements that should be in the middle */
			int max = r.nextInt(4);
			for(int j=0;j < max; j++) {
				int port = 1777+ r.nextInt(3);
				urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(port),1L);
			}
			/* Add the lowest element */
			urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
			/* Add some middle elements */
			urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777),1L);
			urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778),1L);
			urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779),2L);
			/* Add the highest element */
			urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780),3L);
			/* Add 0 - 3 elements that should be in the middle */
			max = r.nextInt(4);
			for(int j=0;j < max; j++) {
				int port = 1777+ r.nextInt(3);
				urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(port),2L);
			}
			
			/* Pass in the new pool*/
			f.resetURIPool(urlMap);
		
			TreeSet<Pair<Long, URIBuilder>> servers = new TreeSet<Pair<Long,URIBuilder>>(Fetch.pairQueueComparator);
			servers.addAll(f.getURIPoolCopy());
			assertTrue(servers.pollFirst().getSecond().toString().contains("1776"));
			assertTrue(servers.pollLast().getSecond().toString().contains("1780"));
		}
	}
	
	
	@Test
	public void testFetchResultPlumbing() {
		Result first = new Fetch.Result();
		Result second = new Fetch.Result();
		Result third = new Fetch.Result();
		Result basic = new Fetch.Result();
		assertNotEquals(first,null);
		assertNotEquals(first,"dummy");
		assertEquals(first,first);
		assertEquals(first,second);
		try {
			for(Field f:first.getClass().getFields()) {
				System.out.println(f);
				f.set(first, !f.getBoolean(basic));
				assertNotEquals(first,second);
				assertNotEquals(first.hashCode(),second.hashCode());
				assertNotEquals(Result.toByteArray(first),Result.toByteArray(second));
				
				assertEquals(first,Result.fromByteArray(Result.toByteArray(first)));
				
				f.set(second, !f.getBoolean(basic));
				assertEquals(first,second);
				assertEquals(first.hashCode(),second.hashCode());
				assertArrayEquals(Result.toByteArray(first),Result.toByteArray(second));
				
				f.set(first, !f.getBoolean(basic));
				third.reset();;
				assertEquals(third,basic);
			}
			for(Field f:first.getClass().getFields()) {
				f.set(second, f.getBoolean(basic));
				assertNotEquals(first,second);
				assertNotEquals(first.hashCode(),second.hashCode());
				assertNotEquals(Result.toByteArray(first),Result.toByteArray(second));
				
				assertEquals(first,Result.fromByteArray(Result.toByteArray(first)));
				
				f.set(first, f.getBoolean(basic));
				assertEquals(first,second);
				assertEquals(first.hashCode(),second.hashCode());
				assertArrayEquals(Result.toByteArray(first),Result.toByteArray(second));
			}	
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			fail("Could not test Fetch.Result");
		}
	}
	
	
	@Test
	public void testFetchHTTPSGithub(){
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("raw.github.com"));
		
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"));
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (Exception e) {
			fail(""+e);
		}
	}
	
	
	@Test
	public void testFetchHTTPSLetsEncrypt(){
		try {
			Fetch.Configuration config = new Fetch.Configuration();
			//config.accept_SSL_bad_cert_types = true;
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("letsencrypt.org"),config);
		
			String data = f.fetchWebPage(new URIBuilder().setPath("/"));
			assertTrue(data.contains("<title> Let&#39;s Encrypt - Free SSL/TLS Certificates</title>"));
		} catch (Exception e) {
			fail(""+e);
		}
	}
		
	
	@Test
	public void testFetchHTTPSGithubWithResult(){
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("raw.github.com"));
		
			Result result = new Fetch.Result();
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"),result);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			fail(""+e);
		}
	}
	
	
	@Test
	public void testFetchHTTPLetsEncryptRedirect(){
		try {
			Fetch.Configuration config = new Fetch.Configuration();
			Fetch f = new Fetch(new URIBuilder().setScheme("http").setHost("letsencrypt.org"),config);
		
			Result result = new Fetch.Result();
			String data = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			
			assertTrue(data.contains("<title> Let&#39;s Encrypt - Free SSL/TLS Certificates</title>"));
			
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			fail(""+e);
		}
	}
	
	
	@Test
	public void testFetchHTTPSGithubWithPasswordResponseHeaders(){
		try {
			Configuration config = new Fetch.Configuration();
			config.setUsername_password(new Pair<String,String>("djp3","password"));
			// github doesn't actually authenticate this way, so this isn't a complete test
			
			config.setReceiveHeaderFields(new HashMap<String,List<String>>());
			Map<String,String> sendHeaderFields = new HashMap<String,String>();
			sendHeaderFields.put("From","d_j_p_3@djp3.net");
			config.setSendHeaderFields(sendHeaderFields);
			
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("raw.github.com"),config);
		
			Result result = new Fetch.Result();
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"),result);
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			fail(""+e);
		}
	}
	

	@Test
	public void testFetchJSONWithAllBadServers() throws URISyntaxException{
		Map<URIBuilder,Long> urlMap = new HashMap<URIBuilder,Long>();
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780),3L);
		
		Fetch f = new Fetch(urlMap);
		try {
			f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"));
			fail("This should fail because all servers are bad");
		} catch (HttpHostConnectException e) {
			//This is ultimately what is expected
		} catch (Exception e) {
			fail(""+e);
		}
	}

	
	@Test
	public void testFetchJSONWithMostlyBadServers() throws URISyntaxException{
		Map<URIBuilder,Long> urlMap = new HashMap<URIBuilder,Long>();
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780),3L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("raw.github.com"),4L);
		
		Fetch f = new Fetch(urlMap);
		try {
			JSONObject j = f.fetchJSONObject(new URIBuilder().setPath("/djp3/p2p4java/production/bootstrapMasterList.json"));
			JSONArray ja = (JSONArray) j.get("rendezvous_nodes");
			String s = (String) ja.get(0);
			assertTrue(s.contains("tcp"));
		} catch (Exception e) {
			fail(""+e);
		}
	}
	

	@Test
	public void testFetchWebpageWithAllBadServers() throws URISyntaxException{
		Map<URIBuilder,Long> urlMap = new HashMap<URIBuilder,Long>();
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),0L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780),3L);
		
		Fetch f = new Fetch(urlMap);
		try {
			f.fetchWebPage(new URIBuilder().setPath("/djp3/p2p4java/blob/production/bootstrapMasterList.json"));
			fail("This should have ultimately failed");
		} catch (HttpHostConnectException e) {
			//This is what is expected to happen when all servers are tried
		} catch (Exception e) {
			fail(""+e);
		}
	}
	

	@Test
	public void testFetchWebpageWithMostlyBadServers() throws URISyntaxException{
		Map<URIBuilder,Long> urlMap = new HashMap<URIBuilder,Long>();
		urlMap.put(new URIBuilder().setScheme("https").setHost("github.com"),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1776),1L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1777),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1778),2L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1779),3L);
		urlMap.put(new URIBuilder().setScheme("http").setHost("localhost").setPort(1780),4L);
		
		Fetch f = new Fetch(urlMap,null); //null config is optional, just testing it
		try {
			String s = f.fetchWebPage(new URIBuilder().setPath("/djp3/p2p4java/blob/production/bootstrapMasterList.json"));
			assertTrue(s.contains("tcp"));
		} catch (Exception e) {
			fail(""+e);
		}
	}

	
	@Test
	/* Try and get content from a site with an expired SSL certificate */
	public void testFetchHTTPSBadCertificateTypes(){
		
		String[] ciphers= {	"expired",
							"self-signed",
							"untrusted-root",
							"sha1-intermediate",
							"superfish",
							"edellroot",
							"dsdtestprovider",
							"preact-cli",
							"webpack-dev-server",
				};
		
		for(String c:ciphers) {
			//default configuration rejects 
			Result result = new Fetch.Result();
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c+".badssl.com"));
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				fail("Should not have successfully gotten response: "+response);
			}
			catch(org.bouncycastle.tls.TlsFatalAlert e) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			} catch (java.io.IOException e) {
				e.printStackTrace();
				fail("Unexpected exception: "+e);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Unexpected exception: "+e);
			}
		
			try {
				Configuration config = new Fetch.Configuration();
				config.accept_SSL_bad_cert_types = true;
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c+".badssl.com"),config);
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				assertTrue(response.contains("<title>"+c+".badssl.com</title>")); //We got html back as expected
				assertTrue(result.FETCH_INITIATED);
				assertTrue(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
				assertTrue(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed a bad certificate: "+c+"\n"+e);
			}
		}
	}
	
	
	@Test
	/* Try and get content from a site with an expired SSL certificate */
	public void testFetchHTTPSBadCertificateTransparency(){
		
		String[] ciphers= {	
							"invalid-expected-sct"
				};
		
		for(String c:ciphers) {
			//default configuration rejects 
			Result result = new Fetch.Result();
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c+".badssl.com"));
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				fail("Should not have successfully gotten response: "+response);
			}
			catch(org.bouncycastle.tls.TlsFatalAlert e) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			} catch (java.io.IOException e) {
				e.printStackTrace();
				fail("Unexpected exception: "+e);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Unexpected exception: "+e);
			}
		
			try {
				Configuration config = new Fetch.Configuration();
				//config.accept_SSL_bad_cert_types = true;
				config.accept_SSL_with_failed_CT = true;
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c+".badssl.com"),config);
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				assertTrue(response.contains("<title>"+c+".badssl.com</title>")); //We got html back as expected
				assertTrue(result.FETCH_INITIATED);
				assertTrue(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertTrue(result.SSL_ACCEPTED_WITH_FAILED_CT);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed a bad certificate: "+c+"\n"+e);
			}
		}
	}
	
	
	@Test
	/* Try and get content from a site with a null response*/
	public void testFetchHTTPSNullCertificate(){
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("null.badssl.com"));
			String response = f.fetchWebPage(new URIBuilder().setPath("/"), result);
			fail("Should not have successfully gotten response: "+response);
		}
		catch(org.bouncycastle.tls.TlsFatalAlertReceived e) {
			assertTrue(e.getMessage().equals("handshake_failure(40)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		/*
		 * I don't know a way to disregard the bad ciphers and still get the remote content
		 */
	}
	

	@Test
	/* Try and get content from a site with a host that does not match its ssl certificate*/
	public void testFetchHTTPSWrongHost() throws Exception{
		
		//default configuration rejects expired SSL certificates with the wrong host
		Fetch.Result result = new Fetch.Result();
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("wrong.host.badssl.com"));
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (javax.net.ssl.SSLPeerUnverifiedException e) {
			//This is expected
			assertTrue(e.getLocalizedMessage().contains("Host name"));
			assertTrue(e.getLocalizedMessage().contains("does not match the certificate subject provided by the peer"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			fail(""+e);
		}
		
		try {
			Configuration config = new Fetch.Configuration();
			config.accept_SSL_with_hostname_not_matching_cert= true;
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("wrong.host.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>wrong.host.badssl.com</title>")); //We got html back as expected
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertTrue(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (javax.net.ssl.SSLHandshakeException e) {
			fail("Should have allowed an SSL host mismatch on the certificate");
		} catch (Exception e) {
			throw e;
			//fail(""+e);
		}
	}
	
	
	@Test
	/* Try and get content from a site using rc4 an untrusted root */
	public void testFetchHTTPSBadCiphers() {
		String[] ciphers = { "rc4", "rc4-md5", "dh480", "dh512", "dh1024" };

		for (String c : ciphers) {
			Fetch.Result result = null;
			/* default is to fail */
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c + ".badssl.com"));
				
				result = new Fetch.Result();
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				fail("Should not have successfully gotten response: " + response);
			} catch (java.io.IOException e) {
				if (e instanceof org.bouncycastle.tls.TlsFatalAlertReceived) {
					assertNull(e.getCause());
					assertTrue(e.getMessage().equals("handshake_failure(40)"));
					assertNotNull(result);
					assertTrue(result.FETCH_INITIATED);
					assertFalse(result.FETCH_SUCCEEDED);
					assertFalse(result.FETCH_BY_HTTP);
					assertTrue(result.FETCH_BY_HTTPS);
					assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
					assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
					assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
					assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
					assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
					assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
					assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
					assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
					assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
					assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				} else {
					e.printStackTrace();
					fail("" + e);
				}
			} catch (Exception e) {
				e.printStackTrace();
				fail("" + e);
			}

			/*
			 * I don't know a way to disregard the bad ciphers and still get the remote content
			 */
		}
	}
	
	
	
	@Test
	/**
	 *   Try and get content with legacy protocols
	 */
	public void testFetchHTTPSLegacyProtocols() {
		String[] protocols = {"tls-v1-0", "tls-v1-1"};

		for (String p : protocols) {
			/* default should be to accept*/
			Fetch.Result result = new Fetch.Result();
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(p + ".badssl.com"));
				f.fetchWebPage(new URIBuilder().setPath("/"),result);
				fail("The cypher "+p+" should not be allowed by default");
			}catch(IllegalStateException e){
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed connection:\n"+e);
			}
			
			try {
				Fetch.Configuration config = new Fetch.Configuration();
				config.accept_SSL_PROTOCOL_TLSV1_0 = true;
				config.accept_SSL_PROTOCOL_TLSV1_1 = true;
				config.accept_SSL_PROTOCOL_TLSV1_2 = true;
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(p + ".badssl.com"),config);
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				assertTrue(response.contains("<title>"+p+".badssl.com</title>")); //We got html back as expected
				assertTrue(result.FETCH_INITIATED);
				assertTrue(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 ||
						result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 ||
						result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			}catch(IllegalStateException e){
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed connection:\n"+e);
			}
		}
	}
	
	
	@Test
	/** Try and get content from a site using a legacy cipher
	 *   Note: 3DES is not enabled in Java 11 so we don't support using it
	 */
	public void testFetchHTTPSLegacyCiphers() {
		String[] ciphers = { "cbc"};

		for (String c : ciphers) {
			/* default should be to accept*/
			Fetch.Result result = new Fetch.Result();
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c + ".badssl.com"));
				f.fetchWebPage(new URIBuilder().setPath("/"),result);
				fail("The cypher "+c+" should not be allowed by default");
			}catch(IllegalStateException e){
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed connection:\n"+e);
			}
			
			try {
				Fetch.Configuration config = new Fetch.Configuration();
				config.accept_SSL_PROTOCOL_TLSV1_0 = true;
				config.accept_SSL_PROTOCOL_TLSV1_1 = true;
				config.accept_SSL_PROTOCOL_TLSV1_2 = true;
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c + ".badssl.com"),config);
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				assertTrue(response.contains("<title>"+c+".badssl.com</title>")); //We got html back as expected
				assertTrue(result.FETCH_INITIATED);
				assertTrue(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 ||
						result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 ||
						result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			}catch(IllegalStateException e){
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed connection:\n"+e);
			}
		}
	}
	
	
	@Test
	/** Try and get content from known good SSL settings' sites
	 */
	public void testFetchHTTPSWithGoodSettings() {
		String[] configurations = {
						"tls-v1-2",
						"sha256",
						"sha384",
						"sha512",
						"rsa2048",
						"rsa8192",
						"ecc256",
						"ecc384",
						"extended-validation",
						"mozilla-modern"};

		for (String c : configurations) {
			/* default should be to accept*/
			Fetch.Result result = new Fetch.Result();
			try {
				Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost(c + ".badssl.com"));
				String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
				assertTrue(response.contains("<title>"+c+".badssl.com</title>")); //We got html back as expected
				assertTrue(result.FETCH_INITIATED);
				assertTrue(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
				assertFalse(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
				assertFalse(result.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
				assertTrue(result.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
				assertFalse(result.SSL_CONNECTED_WITH_CIPHER_CBC);

				
			} catch (Exception e) {
				e.printStackTrace();
				fail("Should have allowed connection:\n"+e);
			}
		}
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate */
	public void testFetchHTTPSRevokedCertificatesWithOCSPAndCRL(){
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = true;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[0];
		double b = stats.hitRate();
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		double c = FetchSSLTrustManager.getCacheStats()[0].hitRate();
		
		assertEquals(0.0,b,1E-9);
		assertTrue(c > b);
	}
	
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate */
	public void testFetchHTTPSRevokedCertificatesWithOCSPAndCRLHTTPHead(){
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = true;
		config.HTTP_GET = false;
		config.HTTP_HEAD = true;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[0];
		double b = stats.hitRate();
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			
			config.setReceiveHeaderFields(new HashMap<String,List<String>>());
			
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			
			assertNull(response); //Head request doesn't return data
			
			assertNotNull(config.getReceiveHeaderFields().get("Content-Type"));
			assertNotNull(config.getReceiveHeaderFields().get("Date"));
			assertNotNull(config.getReceiveHeaderFields().get("Last-Modified"));
			assertNotNull(config.getReceiveHeaderFields().get("ETag"));
			
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		double c = FetchSSLTrustManager.getCacheStats()[0].hitRate();
		
		assertEquals(0.0,b,1E-9);
		assertTrue(c > b);
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate save and load cache from disk */
	public void testFetchHTTPSRevokedCertificatesWithOCSPAndCRLWithDiskStorage(){
		
		FetchSSLTrustManager.configurationOCSP.loadCacheFromColdStorageOnStart = true;
		FetchSSLTrustManager.configurationOCSP.loadCacheColdStorageFileName = "testSupport/sslcerts/OCSP.cache";
		FetchSSLTrustManager.configurationOCSP.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationOCSP.storeCacheColdStorageFileName= "testSupport/sslcerts/OCSP.cache";
		
		FetchSSLTrustManager.configurationCRL.loadCacheFromColdStorageOnStart = true;
		FetchSSLTrustManager.configurationCRL.loadCacheColdStorageFileName = "testSupport/sslcerts/CRL.cache";
		FetchSSLTrustManager.configurationCRL.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationCRL.storeCacheColdStorageFileName= "testSupport/sslcerts/CRL.cache";
		FetchSSLTrustManager.resetCaches();
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = true;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		double b = FetchSSLTrustManager.getCacheStats()[0].hitRate();
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			
			config.setReceiveHeaderFields(new HashMap<String,List<String>>());
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			
			assertNotNull(config.getReceiveHeaderFields().get("Content-Type"));
			assertNotNull(config.getReceiveHeaderFields().get("Date"));
			assertNotNull(config.getReceiveHeaderFields().get("Last-Modified"));
			assertNotNull(config.getReceiveHeaderFields().get("ETag"));
			
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		double c = FetchSSLTrustManager.getCacheStats()[0].hitRate();
		
		assertEquals(0.0,b,1E-9);
		assertTrue(c > b);
	}
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate save and load cache from disk */
	public void testFetchHTTPSRevokedCertificatesWithOCSPAndCRLAndCTWithDiskStorage(){
		
		FetchSSLTrustManager.configurationOCSP.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.configurationOCSP.loadCacheColdStorageFileName = "testSupport/sslcerts/OCSP.cache";
		FetchSSLTrustManager.configurationOCSP.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationOCSP.storeCacheColdStorageFileName= "testSupport/sslcerts/OCSP.cache";
		
		FetchSSLTrustManager.configurationCRL.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.configurationCRL.loadCacheColdStorageFileName = "testSupport/sslcerts/CRL.cache";
		FetchSSLTrustManager.configurationCRL.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationCRL.storeCacheColdStorageFileName= "testSupport/sslcerts/CRL.cache";
		
		FetchSSLTrustManager.configurationCT.loadCacheFromColdStorageOnStart = false;
		FetchSSLTrustManager.configurationCT.loadCacheColdStorageFileName = "testSupport/sslcerts/CT.cache";
		FetchSSLTrustManager.configurationCT.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationCT.storeCacheColdStorageFileName= "testSupport/sslcerts/CT.cache";
		
		FetchSSLTrustManager.resetCaches();
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = true;
		config.check_CT = true;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("invalid-expected-sct.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		double b = FetchSSLTrustManager.getCacheStats()[2].hitRate();
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			config.accept_SSL_with_failed_CT = true;
			
			config.setReceiveHeaderFields(new HashMap<String,List<String>>());
			Fetch f = new Fetch(new URIBuilder().setScheme("http").setHost("invalid-expected-sct.badssl.com"),config); //Upgrade redirect from http
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>invalid-expected-sct.badssl.com</title>")); //We got html back as expected
			
			assertNotNull(config.getReceiveHeaderFields().get("Content-Type"));
			assertNotNull(config.getReceiveHeaderFields().get("Date"));
			assertNotNull(config.getReceiveHeaderFields().get("Last-Modified"));
			assertNotNull(config.getReceiveHeaderFields().get("ETag"));
			
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertTrue(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		double c = FetchSSLTrustManager.getCacheStats()[2].hitRate();
		
		assertEquals(0.0,b,1E-9);
		assertTrue(c > b);
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate save and load cache from disk */
	public void testFetchHTTPSRevokedCertificatesWithOCSPWithoutCache(){
		
		FetchSSLTrustManager.configurationOCSP.useCache = false;
		FetchSSLTrustManager.configurationCRL.useCache = false;
		FetchSSLTrustManager.resetCaches();
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = false;
		config.check_OCSP = true;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = false;
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
	}
	 
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate save and load cache from disk */
	public void testFetchHTTPSRevokedCertificatesWithCRLWithoutCache(){
		
		FetchSSLTrustManager.configurationOCSP.useCache = false;
		FetchSSLTrustManager.configurationCRL.useCache = false;
		FetchSSLTrustManager.resetCaches();
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = false;
		
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = false;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate */
	public void testFetchHTTPSRevokedCertificatesWithOCSP(){
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = false;
		config.check_OCSP = true;
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[0];
		assertTrue(stats.hitCount() <= stats.missCount());
		assertTrue(stats.requestCount() > 0);
		double b = stats.hitRate();
		assertEquals(0.0,b,1E-9);
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = true;
			config.accept_SSL_with_revoked_cert_by_CRL = false;
			
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		
		double c = FetchSSLTrustManager.getCacheStats()[0].hitRate();
		
		assertTrue(c > b);
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate */
	public void testFetchHTTPSRevokedCertificatesWithCRL(){
		
		
		//default configuration rejects 
		Fetch.Result result = new Fetch.Result();
		Fetch.Configuration config = new Fetch.Configuration();
		config.check_CRL = true;
		config.check_OCSP = false;
		
		try {
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (org.bouncycastle.tls.TlsFatalAlert e) {
			//This is expected
			assertTrue(e.getMessage().equals("bad_certificate(42)"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[1];
		assertTrue(stats.hitCount() <= stats.missCount());
		assertTrue(stats.requestCount() > 0);
		double b = stats.hitRate();
		assertEquals(0.0,b,1E-9);
		
		try {
			config.accept_SSL_with_revoked_cert_by_OCSP = false;
			config.accept_SSL_with_revoked_cert_by_CRL = true;
			
			Fetch f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			assertTrue(response.contains("<title>revoked.badssl.com</title>")); //We got html back as expected
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
			assertFalse(result.FETCH_BY_HTTP);
			assertTrue(result.FETCH_BY_HTTPS);
			assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
			assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			assertTrue(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Should have allowed a bad certificate:\n"+e);
		}
		
		double c = FetchSSLTrustManager.getCacheStats()[1].hitRate();
		
		assertTrue(c > b);
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate and check that the OCSP cache is working */
	public void testFetchHTTPSRevokedCertificatesCacheOCSP(){
		
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[0];
		assertNotNull(stats);
		assertEquals(0,stats.requestCount());
		assertEquals(0,stats.hitCount());
		assertEquals(0,stats.missCount());
		assertEquals(0,stats.loadCount());
		
		Fetch.Result result = new Fetch.Result();
		
		// Fetch the site with revoked SSL using only OCSP
		long start1 = System.currentTimeMillis();
		Fetch f = null;
		try {
			Configuration config = new Fetch.Configuration();
			config.check_OCSP = true;
			config.check_CRL = false;
			f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (java.io.IOException e) {
			//This is expected
			if(e instanceof org.bouncycastle.tls.TlsFatalAlert) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			}
			else {
				e.printStackTrace();
				fail(""+e);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		long end1 = System.currentTimeMillis();
		
		stats = FetchSSLTrustManager.getCacheStats()[0];
		assertNotNull(stats);
		assertEquals(1,stats.requestCount());
		assertEquals(0,stats.hitCount());
		assertEquals(1,stats.missCount());
		assertEquals(1,stats.loadCount());
		
		
		// Fetch the site with revoked SSL using only OCSP but with a warm cache
		long start2 = System.currentTimeMillis();
		try {
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (java.io.IOException e) {
			//This is expected
			if(e instanceof org.bouncycastle.tls.TlsFatalAlert) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			}
			else {
				e.printStackTrace();
				fail(""+e);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		long end2 = System.currentTimeMillis();
		
		stats = FetchSSLTrustManager.getCacheStats()[0];
		assertNotNull(stats);
		assertEquals(2,stats.requestCount());
		assertEquals(1,stats.hitCount());
		assertEquals(1,stats.missCount());
		assertEquals(1,stats.loadCount());
		
		// The cache should have made it faster
		assertTrue(end1-start1 > end2-start2);
		
	}
	
	
	@Test
	/* Try and get content from a site with a revoked SSL certificate and check that CRL cache is working */
	public void testFetchHTTPSRevokedCertificatesCacheCRL(){
		
				
		CacheStats stats = FetchSSLTrustManager.getCacheStats()[1];
		assertNotNull(stats);
		assertEquals(0,stats.requestCount());
		assertEquals(0,stats.hitCount());
		assertEquals(0,stats.missCount());
		assertEquals(0,stats.loadCount());
		
		Fetch.Result result = new Fetch.Result();

		// Fetch the site with revoked SSL using only OCSP
		Fetch f = null;
		try {
			Configuration config = new Fetch.Configuration();
			config.check_OCSP = false;
			config.check_CRL = true;
			f = new Fetch(new URIBuilder().setScheme("https").setHost("revoked.badssl.com"),config);
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (java.io.IOException e) {
			//This is expected
			if(e instanceof org.bouncycastle.tls.TlsFatalAlert) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			}
			else {
				e.printStackTrace();
				fail(""+e);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		stats = FetchSSLTrustManager.getCacheStats()[1];
		assertNotNull(stats);
		assertEquals(1,stats.requestCount());
		assertTrue(stats.hitCount() <= stats.missCount());
		assertEquals(1,stats.missCount());
		assertEquals(1,stats.loadCount());
		
		
		// Fetch the site with revoked SSL using only OCSP but with a warm cache
		try {
			String response = f.fetchWebPage(new URIBuilder().setPath("/"),result);
			fail("Should not have successfully gotten response: "+response);
		} catch (java.io.IOException e) {
			//This is expected
			if(e instanceof org.bouncycastle.tls.TlsFatalAlert) {
				assertTrue(e.getMessage().equals("bad_certificate(42)"));
				assertTrue(result.FETCH_INITIATED);
				assertFalse(result.FETCH_SUCCEEDED);
				assertFalse(result.FETCH_BY_HTTP);
				assertTrue(result.FETCH_BY_HTTPS);
				assertFalse(result.SSL_ACCEPTED_BAD_CERT_TYPE);
				assertFalse(result.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
				assertFalse(result.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
				assertFalse(result.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
				assertFalse(result.SSL_ACCEPTED_WITH_FAILED_CT);
			}
			else {
				e.printStackTrace();
				fail(""+e);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: "+e);
		}
		
		stats = FetchSSLTrustManager.getCacheStats()[1];
		assertNotNull(stats);
		assertEquals(2,stats.requestCount());
		assertEquals(1,stats.hitCount());
		assertEquals(1,stats.missCount());
		assertEquals(1,stats.loadCount());
	}
	
	
	/**
	 * This is a helper for the tests below
	 * @param f
	 * @param site
	 * @param result 
	 */
	public void testFetchHTTPSCertificatesCacheCleanupHelper(Fetch f, String site, Result result) {
		try {
			f.fetchWebPage(new URIBuilder().setHost(site),result);
			assertTrue(result.FETCH_INITIATED);
			assertTrue(result.FETCH_SUCCEEDED);
		} catch (SocketException e) {
			// Probably blocked by network filtering at work
			System.out.println("Site or client probably being filtered: "+site);
			System.out.println("\""+e.getMessage()+"\"");
			String message = e.getMessage();
			assertTrue(message.equals("Connection reset") ||
					message.equals("Read timed out") || 
					message.contains("Connection refused") ||
					message.contains("Operation timed out") 
					);
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch(ConnectTimeoutException e) {
			// Probably blocked by network filtering at work
			System.out.println("Connection probably being blocked by server at: "+site);
			//System.out.println(e.getMessage());
			String message = e.getMessage();
			assertTrue(message.endsWith("connect timed out"));
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch(SocketTimeoutException e) {
			// Probably blocked by network filtering at work
			System.out.println("Site probably being filtered: "+site);
			//System.out.println(e.getMessage());
			assertEquals("Read timed out",e.getMessage());
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch (TlsNoCloseNotifyException e) {
			System.out.println("Site didn't close Tls connection : "+site);
			assertEquals("No close_notify alert received before connection closed",e.getMessage());
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch(org.bouncycastle.tls.TlsFatalAlert e) {
			System.out.println("bad certificate at "+site);
			assertEquals("bad_certificate(42)",e.getMessage());
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch( HttpResponseException e) {
			System.out.println("Site is upset (maybe) : "+site+"\n"+e+"\n");
			assertTrue(e.getMessage().equals("") || //www.vk.com sent nothing on HTTP HEAD
					e.getMessage().equals("Too Many Requests") ||
					e.getMessage().equals("Bad Gateway") ||
					e.getMessage().equals("MethodNotAllowed") ||
					e.getMessage().equals("Method Not Allowed") ||
					e.getMessage().equals("Not Found") ||
					e.getMessage().equals("Forbidden") ||
					e.getMessage().equals("Server Error") 
					);
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch( ClientProtocolException e) {
			System.out.println("Site can't handle a HEAD request (maybe) : "+site+"\n"+e);
			assertTrue(e.getMessage() == null || e.getMessage().contains("Circular redirect") );
			assertTrue(result.FETCH_INITIATED);
			assertFalse(result.FETCH_SUCCEEDED);
		} catch (java.io.IOException e) {
			e.printStackTrace();
			fail("Something is wrong: "+e);
		} catch (URISyntaxException e) {
			fail("This should have worked");
		}
	}
	

	
	
	@Test
	/** Try and get content from lots of https sites to make sure stats look okay and cache clean up works when triggered */
	public void testFetchHTTPSCertificatesCacheOCSPAndCRLCacheCleanup(){
			
								
		// Make sure everything starts as expected 
		CacheStats[] stats = FetchSSLTrustManager.getCacheStats();
		assertNotNull(stats);
		assertNotNull(stats[0]);
		assertNotNull(stats[1]);
		assertEquals(0,stats[0].requestCount());
		assertEquals(0,stats[0].hitCount());
		assertEquals(0,stats[0].missCount());
		assertEquals(0,stats[0].loadCount());
		assertEquals(0,stats[0].evictionCount());
		assertEquals(0,stats[1].requestCount());
		assertEquals(0,stats[1].hitCount());
		assertEquals(0,stats[1].missCount());
		assertEquals(0,stats[1].loadCount());
		assertEquals(0,stats[1].evictionCount());
		
		
		/* Alexa top 50 in world and top 50 in US*/
		String[] sites = {
				"360.cn", "www.aliexpress.com", "www.alipay.com", "www.amazon.com", "www.amazonaws.com",
				"www.apple.com", "www.baidu.com", "www.bankofamerica.com", "www.bing.com", "www.blogspot.com", "www.chase.com", "www.cnn.com", "www.craigslist.org", "www.csdn.net", "www.dropbox.com", "www.ebay.com",
				"www.espn.com", "www.facebook.com", "www.force.com", "www.github.com", "www.google.ca", "www.google.co.in",
				"www.google.co.jp", "www.google.co.uk", "www.google.com", "www.google.com.br", "www.google.com.hk", "www.google.de",
				"www.google.fr", "www.google.ru", "www.hulu.com", "www.imdb.com", "www.imgur.com", "www.instagram.com",
				"www.instructure.com", "www.jd.com", "www.linkedin.com", "www.live.com", "www.livejasmin.com", "login.tmall.com",
				"www.mail.ru", "www.microsoft.com", "www.microsoftonline.com", "www.netflix.com", "www.nytimes.com", "www.office.com",
				"pages.tmall.com", "www.paypal.com", "www.pinterest.com", "www.pornhub.com", "www.qq.com", "www.quora.com",
				"www.reddit.com", "www.salesforce.com", "www.sina.com.cn", "www.sohu.com", "www.stackoverflow.com", "t.co",
				"www.taobao.com", "www.tmall.com", "www.tumblr.com", "www.twitch.tv", "www.twitter.com", "www.vk.com",
				"www.walmart.com", "www.weather.com", "www.weibo.com", "www.wellsfargo.com", "www.wikia.com", "www.wikipedia.org",
				"www.xvideos.com", "www.yahoo.co.jp", "www.yahoo.com", "www.yandex.ru", "www.yelp.com", "www.youtube.com", "www.zillow.com"
				};
		
		// Only use CRL validation
		Configuration config = new Fetch.Configuration();
		config.check_OCSP = true;
		config.check_CRL = true;
		FetchSSLTrustManager.configurationOCSP.loadCacheFromColdStorageOnStart = true;
		FetchSSLTrustManager.configurationOCSP.loadCacheColdStorageFileName = "testSupport/sslcerts/OCSP.cache";
		FetchSSLTrustManager.configurationOCSP.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationOCSP.storeCacheColdStorageFileName= "testSupport/sslcerts/OCSP.cache";
		
		FetchSSLTrustManager.configurationCRL.loadCacheFromColdStorageOnStart = true;
		FetchSSLTrustManager.configurationCRL.loadCacheColdStorageFileName = "testSupport/sslcerts/CRL.cache";
		FetchSSLTrustManager.configurationCRL.storeCacheToColdStorageOnQuit= true;
		FetchSSLTrustManager.configurationCRL.storeCacheColdStorageFileName= "testSupport/sslcerts/CRL.cache";
		
		// Don't wait too long for sites that don't answer
		config.setConnectTimeout(5*1000); 
		config.setSocketTimeout(5*1000); 
		config.setConnectionRequestTimeout(5*1000);
		
		Fetch.Result result = new Fetch.Result();
		Fetch f = new Fetch(new URIBuilder().setScheme("https").setPath("/"),config);
		long start1 = System.currentTimeMillis();
		for(String site:sites) {
			testFetchHTTPSCertificatesCacheCleanupHelper(f, site,result);
		}
		long end1 = System.currentTimeMillis();
		
		stats = FetchSSLTrustManager.getCacheStats();
		assertNotNull(stats);
		assertNotNull(stats[0]);
		assertNotNull(stats[1]);
		
		long accesses1_OCSP = stats[0].requestCount();
		long hits1_OCSP= stats[0].hitCount();
		long misses1_OCSP= stats[0].missCount();
		long loads1_OCSP= stats[0].loadCount();
		long evictions1_OCSP= stats[0].evictionCount();
		long accesses1_CRL = stats[1].requestCount();
		long hits1_CRL= stats[1].hitCount();
		long misses1_CRL= stats[1].missCount();
		long loads1_CRL= stats[1].loadCount();
		long evictions1_CRL= stats[1].evictionCount();
		
		assertTrue(accesses1_OCSP + accesses1_CRL >= sites.length);
		assertTrue(hits1_OCSP + hits1_CRL > 0);
		assertTrue(misses1_OCSP + misses1_CRL > 0);
		assertTrue(loads1_OCSP + loads1_CRL > 0);
		assertEquals(0,evictions1_OCSP);
		assertEquals(0,evictions1_CRL);
		
		FetchSSLTrustManager.triggerGarbageCollection();
		
		// Now do the same thing after being warmed up
		long start2 = System.currentTimeMillis();
		for(String site:sites) {
			testFetchHTTPSCertificatesCacheCleanupHelper(f, site,result);
		}
		long end2 = System.currentTimeMillis();
		
		// Make sure it is faster after the cache is warmed up
		assertTrue(end2-start2 < end1-start1);
		
		stats = FetchSSLTrustManager.getCacheStats();
		assertNotNull(stats);
		assertNotNull(stats[0]);
		assertNotNull(stats[1]);
		
		long accesses2_OCSP = stats[0].requestCount();
		long hits2_OCSP= stats[0].hitCount();
		long misses2_OCSP= stats[0].missCount();
		long loads2_OCSP= stats[0].loadCount();
		long evictions2_OCSP= stats[0].evictionCount();
		long accesses2_CRL = stats[1].requestCount();
		long hits2_CRL= stats[1].hitCount();
		long misses2_CRL= stats[1].missCount();
		long loads2_CRL= stats[1].loadCount();
		long evictions2_CRL= stats[1].evictionCount();
		
		assertTrue( accesses2_CRL+accesses2_OCSP > accesses1_CRL+accesses2_OCSP);
		assertTrue( hits2_CRL+hits2_OCSP > hits1_CRL+hits1_OCSP);
		assertTrue( misses2_CRL + misses2_OCSP >= misses1_CRL+misses1_OCSP);
		assertTrue( loads2_CRL + loads2_OCSP >= loads1_CRL + loads2_OCSP);
		assertEquals(0,evictions2_OCSP);
		assertEquals(0,evictions2_CRL);
		
		/*
		System.out.println("First:");
		System.out.println("\t               OCSP\t\tCRL");
		System.out.println("\tTotal Accesses: "+accesses1_OCSP+"\t\t"+accesses1_CRL);
		System.out.println("\t          Hits: "+hits1_OCSP+"\t\t"+hits1_CRL);
		System.out.println("\t        Misses: "+misses1_OCSP+"\t\t"+misses1_CRL);
		System.out.println("\t         Loads: "+loads1_OCSP+"\t\t"+loads1_CRL);
		System.out.println("\t     Evictions: "+evictions1_OCSP+"\t\t"+evictions1_CRL);
		System.out.println("Second:");
		System.out.println("\tTotal Accesses: "+accesses2_OCSP+"\t\t"+accesses2_CRL);
		System.out.println("\t          Hits: "+hits2_OCSP+"\t\t"+hits2_CRL);
		System.out.println("\t        Misses: "+misses2_OCSP+"\t\t"+misses2_CRL);
		System.out.println("\t         Loads: "+loads2_OCSP+"\t\t"+loads2_CRL);
		System.out.println("\t     Evictions: "+evictions2_OCSP+"\t\t"+evictions2_CRL);
		*/
	}
}

