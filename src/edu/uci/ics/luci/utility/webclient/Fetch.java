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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.datastructure.Pair;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * This class is designed to facilitate getting content from the Internet.
 * Hopefully robustly. 
 * First create a Fetch object with a possible pool and configuration
 * Then execute .fetchWebPage(URIBuilder) 
 * @author djp3
 *
 */
public class Fetch {

	/*** Static  ***/
	

	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Fetch.class);
		}
		return log;
	}
	
	
	public final static Comparator<URIBuilder> uriBuilderComparator = new Comparator<URIBuilder>(){
		@Override
		public int compare(URIBuilder aa, URIBuilder bb) {
			URI a = URI.create("");
			try {
				a = aa.build();
			} catch (URISyntaxException e) {
			}
		
			URI b = URI.create("");
			try {
				b = bb.build();
			} catch (URISyntaxException e) {
			}
			return a.compareTo(b);
		}
	};
	
	
	public final static Comparator<Pair<Long,URIBuilder>> pairQueueComparator = new Comparator<Pair<Long,URIBuilder>>(){
		@Override
		public int compare(Pair<Long,URIBuilder> aaa, Pair<Long,URIBuilder> bbb) {
		//public int compare(E aaa, E bbb) {
			if( (((aaa.getFirst() != null) && (bbb.getFirst()!= null)) && (aaa.getFirst().equals(bbb.getFirst()))) ||
				((aaa.getFirst() == null) && (bbb.getFirst() == null)) ) {
				// only care about prioritizing by first element of pair
				return 0;
			}
			else{
				if(aaa.getFirst() == null) {
					return 1;
				}
				else if (bbb.getFirst() == null) {
					return -1;
				}
				else {
					return aaa.getFirst().compareTo(bbb.getFirst());
				}
			}
		}
	};
	
	
	/**
	 *	Fetch the the webpage or header of a URI on the web.  This code should be almost exactly the same as
	 * 
	 * @param URIBuilder
	 * 			The location of the request
	 * @param sendHeaderFields
	 *          These are sent in the HTTP header
	 * @param receiveHeaderFields
	 *          This is where we put headers that come back
	 * @param username_password
	 *          needs to have an entry for "username" (first) and "password" (second).
	 * @param timeOutMilliSecs
	 *          The read time out in milliseconds. Must be greater than 0
	 * @param justHead
	 * 			true if this is an HTTP_HEAD request otherwise it is an HTTP_GET
	 * @param verifyHostName
	 * 			true if connection should fail if SSL certificate does not match hostname
	 * @return the data retrieved
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws NoSuchProviderException 
	 * @throws KeyStoreException 
	 * @throws UnrecoverableKeyException 
	 */
	private static String fetchWebPageHelper(
			URIBuilder uriBuilder,
			Fetch.Configuration config,
			Fetch.Result result) throws IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException, UnrecoverableKeyException, KeyStoreException {
		
		result.reset();
		config.setResult(result);
		
	
		//If the config doesn't already have an HTTP client, then build one.  Lazy evaluation
		if(config.getHTTPClient() == null){
			/* Set up with authentication credentials */
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			if(config.getUsername_password() != null){
				credsProvider.setCredentials(
						new AuthScope(uriBuilder.getHost(), uriBuilder.getPort()),
						new UsernamePasswordCredentials(config.getUsername_password().getFirst(),config.getUsername_password().getSecond()));
			}
			
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.STANDARD)
					.setConnectionRequestTimeout(config.connectionRequestTimeout)
					.setConnectTimeout(config.connectTimeout)
					.setSocketTimeout(config.socketTimeout)
					.build();
			
			SSLContext ctx = SSLContext.getInstance("TLSv1","BCJSSE");
			
			//TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			//try {
				//KeyStore ki = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType",KeyStore.getDefaultType()));
				//tmf.init(ki);
			//} catch (KeyStoreException e) {
			//	e.printStackTrace();
			//}
		
			SSLConnectionSocketFactory sslsf = null;

			FetchSSLTrustManager fetchSSLTrustManager = new FetchSSLTrustManager(config);
			Globals.getGlobals().addQuittable(fetchSSLTrustManager); // Make sure the caches get shut down
			
			ctx.init(	new KeyManager[0],
						new TrustManager[] {
								fetchSSLTrustManager
						},
						new SecureRandom());
			SSLContext.setDefault(ctx);
			sslsf = new SSLConnectionSocketFactory( ctx,new FetchHostnameVerifier(config));
		
			config.setHTTPClient(HttpClients
							.custom()
							.setSSLSocketFactory(sslsf)
							.setDefaultRequestConfig(defaultRequestConfig)
							.setDefaultCredentialsProvider(credsProvider)
							.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false))
							.build());
		}
		
		//If the config doesn't already have a response handler, then build one lazily
	
		if(config.getResponseHandler() == null) {
			/* Set up a call back for the response */
			config.setResponseHandler(new ResponseHandler<String>() {
				
				@Override
				public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();
					
					if (statusLine.getStatusCode() >= 300) {
						throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
					}

					if (config.getReceiveHeaderFields() != null) {
						HeaderIterator hit = response.headerIterator();
						while (hit.hasNext()) {
							Header elem = hit.nextHeader();
							List<String> list = null;
							if (config.getReceiveHeaderFields().containsKey(elem.getName())) {
								list = config.getReceiveHeaderFields().get(elem.getName());
								list.add(elem.getValue());
							} else {
								list = new ArrayList<String>();
								list.add(elem.getValue());
							}
							config.getReceiveHeaderFields().put(elem.getName(), list);
						}
					}

					ContentType contentType = ContentType.getOrDefault(entity);
					Charset charset = contentType.getCharset();
					if (entity != null) {
						return EntityUtils.toString(entity, charset);
					} else {
						return null;
					}
				}
			});
		}
		
		
		// Build the URI
		URI uri = uriBuilder.build();
	
		//Build the request
		HttpUriRequest httpUriRequest = null;
		if(config.HTTP_GET) {
			httpUriRequest = new HttpGet(uri);
		}
		else if(config.HTTP_HEAD) {
			httpUriRequest = new HttpHead(uri);
		}
		else {
			return null;
		}
	
		/* Add Header Fields if provided */
		if (config.getSendHeaderFields() != null) {
			for (Entry<String, String> p : config.getSendHeaderFields().entrySet()) {
				httpUriRequest.setHeader(p.getKey(), p.getValue());
			}
		}
		config.resetResponseHandlerReceiveFields();
		
		result.FETCH_INITIATED = true;
		
		/* Set an initial result in case there is an exception (e.g., https://null.badssl.com)*/
		if(uri.getScheme().toLowerCase().equals("http")) {
			result.FETCH_BY_HTTP = true;
			result.FETCH_BY_HTTPS = false;
		}
		if(uri.getScheme().toLowerCase().equals("https")) {
			result.FETCH_BY_HTTP = false;
			result.FETCH_BY_HTTPS = true;
		}
		
		HttpClientContext context = HttpClientContext.create();
			
		String data = config.getHTTPClient().execute(httpUriRequest, config.getResponseHandler(),context);
		
		/* Update results if redirected */
        List<URI> redirectLocations = context.getRedirectLocations();
        if(redirectLocations != null) {
        	HttpHost target = context.getTargetHost();
        	URI location = URIUtils.resolve(httpUriRequest.getURI(), target, redirectLocations);
        	if(location.getScheme().toLowerCase().equals("http")) {
        		result.FETCH_BY_HTTP = true;
        		result.FETCH_BY_HTTPS = false;
        	}
        	if(location.getScheme().toLowerCase().equals("https")) {
        		result.FETCH_BY_HTTP = false;
        		result.FETCH_BY_HTTPS = true;
        	}
        }
        
		result.FETCH_SUCCEEDED = true;
		return(data);
	}
	
	
	/*** Non static instance elements ***/

	/* A set of url's that provide equivalent information */
	private transient Object uriPoolLock = new Object();
	private PriorityQueue<Pair<Long,URIBuilder>> uriPool = null;
	private Fetch.Configuration instanceConfig = null;
	
	/**
	 * An object that represents the result of a web page fetch 
	 * @author djp3
	 *
	 */
	public static class Result{
		
		public boolean FETCH_INITIATED;
		public boolean FETCH_SUCCEEDED;
		public boolean FETCH_BY_HTTP;
		public boolean FETCH_BY_HTTPS;
		public boolean SSL_ACCEPTED_BAD_CERT_TYPE;
		public boolean SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT;
		public boolean SSL_ACCEPTED_WITH_BROKEN_CHAIN;
		public boolean SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP;
		public boolean SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL;
		public boolean SSL_ACCEPTED_WITH_FAILED_CT;
		public boolean SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2;
		public boolean SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1;
		public boolean SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0;
		public boolean SSL_CONNECTED_WITH_CIPHER_CBC;
		
		
		public static byte[] toByteArray(Result in) {
			BitSet bitSet = new BitSet(5);
			bitSet.set(0,in.FETCH_INITIATED);
			bitSet.set(1,in.FETCH_SUCCEEDED);
			bitSet.set(2,in.FETCH_BY_HTTP);
			bitSet.set(3,in.FETCH_BY_HTTPS);
			bitSet.set(4,in.SSL_ACCEPTED_BAD_CERT_TYPE);
			bitSet.set(5,in.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT);
			bitSet.set(6,in.SSL_ACCEPTED_WITH_BROKEN_CHAIN);
			bitSet.set(7,in.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP);
			bitSet.set(8,in.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL);
			bitSet.set(9,in.SSL_ACCEPTED_WITH_FAILED_CT);
			bitSet.set(10,in.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2);
			bitSet.set(11,in.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1);
			bitSet.set(12,in.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0);
			bitSet.set(13,in.SSL_CONNECTED_WITH_CIPHER_CBC);
			return bitSet.toByteArray();
		}
		

		public static Result fromByteArray(byte[] in) {
			Result ret = new Result();
			BitSet bitSet = BitSet.valueOf(in);
			ret.FETCH_INITIATED = bitSet.get(0);
			ret.FETCH_SUCCEEDED = bitSet.get(1);
			ret.FETCH_BY_HTTP = bitSet.get(2);
			ret.FETCH_BY_HTTPS = bitSet.get(3);
			ret.SSL_ACCEPTED_BAD_CERT_TYPE = bitSet.get(4);
			ret.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT = bitSet.get(5);
			ret.SSL_ACCEPTED_WITH_BROKEN_CHAIN = bitSet.get(6);
			ret.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP = bitSet.get(7);
			ret.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL = bitSet.get(8);
			ret.SSL_ACCEPTED_WITH_FAILED_CT = bitSet.get(9);
			ret.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2 = bitSet.get(10);
			ret.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 = bitSet.get(11);
			ret.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 = bitSet.get(12);
			ret.SSL_CONNECTED_WITH_CIPHER_CBC = bitSet.get(13);
			return(ret);
		}
		
		
		public void reset() {
			this.FETCH_INITIATED = false;
			this.FETCH_SUCCEEDED = false;
			this.FETCH_BY_HTTP = false;
			this.FETCH_BY_HTTPS = false;
			this.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2 = true;
			this.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 = false;
			this.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 = false;
			this.SSL_CONNECTED_WITH_CIPHER_CBC = false;
			this.SSL_ACCEPTED_BAD_CERT_TYPE = false;
			this.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT = false;
			this.SSL_ACCEPTED_WITH_BROKEN_CHAIN = false;
			this.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP = false;
			this.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL = false;
			this.SSL_ACCEPTED_WITH_FAILED_CT = false;
		}
		
		
		public Result() {
			reset();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (FETCH_BY_HTTP ? 1231 : 1237);
			result = prime * result + (FETCH_BY_HTTPS ? 1231 : 1237);
			result = prime * result + (FETCH_INITIATED ? 1231 : 1237);
			result = prime * result + (FETCH_SUCCEEDED ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_BAD_CERT_TYPE ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_WITH_BROKEN_CHAIN ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_WITH_FAILED_CT ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL ? 1231 : 1237);
			result = prime * result + (SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP ? 1231 : 1237);
			result = prime * result + (SSL_CONNECTED_WITH_CIPHER_CBC ? 1231 : 1237);
			result = prime * result + (SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 ? 1231 : 1237);
			result = prime * result + (SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2 ? 1231 : 1237);
			result = prime * result + (SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 ? 1231 : 1237);
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Result))
				return false;
			Result other = (Result) obj;
			if (FETCH_BY_HTTP != other.FETCH_BY_HTTP)
				return false;
			if (FETCH_BY_HTTPS != other.FETCH_BY_HTTPS)
				return false;
			if (FETCH_INITIATED != other.FETCH_INITIATED)
				return false;
			if (FETCH_SUCCEEDED != other.FETCH_SUCCEEDED)
				return false;
			if (SSL_ACCEPTED_BAD_CERT_TYPE != other.SSL_ACCEPTED_BAD_CERT_TYPE)
				return false;
			if (SSL_ACCEPTED_WITH_BROKEN_CHAIN != other.SSL_ACCEPTED_WITH_BROKEN_CHAIN)
				return false;
			if (SSL_ACCEPTED_WITH_FAILED_CT != other.SSL_ACCEPTED_WITH_FAILED_CT)
				return false;
			if (SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT != other.SSL_ACCEPTED_WITH_HOSTNAME_NOT_MATCHING_CERT)
				return false;
			if (SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL != other.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_CRL)
				return false;
			if (SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP != other.SSL_ACCEPTED_WITH_REVOKED_CERT_BY_OCSP)
				return false;
			if (SSL_CONNECTED_WITH_CIPHER_CBC != other.SSL_CONNECTED_WITH_CIPHER_CBC)
				return false;
			if (SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0 != other.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_0)
				return false;
			if (SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2 != other.SSL_CONNECTED_WITH_PROTOCOL_TLSV1_2)
				return false;
			if (SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1 != other.SSL_CONNNECTED_WITH_PROTOCOL_TLSV1_1)
				return false;
			return true;
		}




		
		
		
		
	}
	
	
	/**
	 * A class that represents the configuration of a reusable fetch object
	 * @author djp3
	 *
	 */
	public static class Configuration{
		
		public boolean HTTP_GET;	//If HTTP_GET is false then HTTP_HEAD will be evaluated. 
									//If HTTP_GET is true then HTTP_HEAD won't be looked at	
		public boolean HTTP_HEAD; // If true and HTTP_GET is false, execute an HTTP HEAD request
		
		/* Example bad certificates
		 *    SSL cert is expired
		 *    A self-signed certificate
		 *    The root of the cert chain is untrusted
		 *    SHA1 is used to hash intermediate cert chain
		 *    a superfish root certificate
		 *    an edellroot root certificate
		 *    a dsdtestprovider root certificate
		 *    a preact-cli root certificate
		 *    a webpack-dev-server root certificate
		 */
		public boolean accept_SSL_bad_cert_types;
		
		public boolean accept_SSL_with_hostname_not_matching_cert;
		
		/* a broken chain means their is not a path back to the root authority */
		public boolean accept_SSL_with_broken_chain;
		
		public boolean check_OCSP;
		public boolean check_CRL;
		public boolean check_CT;
		
		public boolean accept_SSL_with_revoked_cert_by_OCSP;
		public boolean accept_SSL_with_revoked_cert_by_CRL;
		public boolean accept_SSL_with_failed_CT;
		
		public boolean accept_SSL_PROTOCOL_TLSV1_2;
		public boolean accept_SSL_PROTOCOL_TLSV1_1;
		public boolean accept_SSL_PROTOCOL_TLSV1_0;
		
		public boolean accept_SSL_CIPHER_CBC;
		
		
		//https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.html
		//Returns the timeout in milliseconds used when requesting a connection from the connection manager. A timeout value of zero is interpreted as an infinite timeout.
		//A timeout value of zero is interpreted as an infinite timeout.
		private int connectionRequestTimeout;
		//Determines the timeout in milliseconds until a connection is established. A timeout value of zero is interpreted as an infinite timeout.
		//A timeout value of zero is interpreted as an infinite timeout. 
		private int connectTimeout;
		// socketTimeout is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets.
		private int socketTimeout;
		
		private Map<String, String> sendHeaderFields;
		private Map<String, List<String>> receiveHeaderFields;
		private Pair<String,String> username_password;
		private CloseableHttpClient httpClient;
		private ResponseHandler<String> responseHandler;
		
		private Result result;
		
		public Configuration() {
			HTTP_GET = true;
			HTTP_HEAD = true;

			accept_SSL_bad_cert_types = false;

			accept_SSL_with_hostname_not_matching_cert = false;

			accept_SSL_with_broken_chain = false;

			check_OCSP = true;
			check_CRL = true;
			check_CT = true;

			accept_SSL_with_revoked_cert_by_OCSP = false;
			accept_SSL_with_revoked_cert_by_CRL = false;
			accept_SSL_with_failed_CT = false;
			
			accept_SSL_PROTOCOL_TLSV1_2 = true;
			accept_SSL_PROTOCOL_TLSV1_1 = false;
			accept_SSL_PROTOCOL_TLSV1_0 = false;
			
			accept_SSL_CIPHER_CBC = false;

			connectionRequestTimeout = 30 * 1000;
			connectTimeout = 30 * 1000;
			socketTimeout = 30 * 1000;

			sendHeaderFields = null;
			receiveHeaderFields = null;
			username_password = null;
			httpClient = null;
			responseHandler = null;
				
		}
		
		public int getConnectionRequestTimeout() {
			return connectionRequestTimeout;
		}
		
		
		/**
		 *  Package private so it is only called by Fetch
		 */
		void resetResponseHandlerReceiveFields() {
			if(this.receiveHeaderFields != null) {
				this.receiveHeaderFields.clear();
			}
		}


		/**
		 *  Package private so it is only called by Fetch
		 */
		ResponseHandler<String> getResponseHandler() {
			return this.responseHandler;
		}
		
		
		/**
		 *  Package private so it is only called by Fetch
		 */
		void setResponseHandler(ResponseHandler<String> responseHandler) {
			this.responseHandler = responseHandler;
		}
		
		
		/**
		 *  Package private so it is only called by Fetch
		 */
		CloseableHttpClient getHTTPClient() {
			return this.httpClient;
		}

		
		/**
		 *  Package private so it is only called by Fetch
		 */
		void setHTTPClient(CloseableHttpClient httpClient) {
			this.httpClient = httpClient;
		}
		

		/**
		 * 
		 * @param connectionRequestTimeout in milliseconds
		 */
		public void setConnectionRequestTimeout(int connectionRequestTimeout) {
			if(connectionRequestTimeout < 0) {
				throw new InvalidParameterException("Connection Request Timeout can't be negative");
			}
			this.connectionRequestTimeout = connectionRequestTimeout;
		}
		
		
		public int getConnectTimeout() {
			return connectTimeout;
		}
		
		
		/**
		 * 
		 * @param connectTimeout in milliseconds
		 */
		public void setConnectTimeout(int connectTimeout) {
			if(connectTimeout < 0) {
				throw new InvalidParameterException("Connection Timeout can't be negative");
			}
			this.connectTimeout = connectTimeout;
		}
		
		
		public int getSocketTimeout() {
			return socketTimeout;
		}
		
		
		public void setSocketTimeout(int socketTimeout) {
			if(socketTimeout < 0) {
				throw new InvalidParameterException("Socket Timeout can't be negative");
			}
			this.socketTimeout = socketTimeout;
		}

		
		/**
		 *  Package private so it is only called by Fetch
		 */
		Pair<String, String> getUsername_password() {
			return username_password;
		}
		
		
		public Map<String, String> getSendHeaderFields() {
			return sendHeaderFields;
		}
		

		public void setSendHeaderFields(Map<String, String> sendHeaderFields) {
			this.sendHeaderFields = sendHeaderFields;
		}
		

		public Map<String, List<String>> getReceiveHeaderFields() {
			return receiveHeaderFields;
		}
		

		public void setReceiveHeaderFields(Map<String, List<String>> receiveHeaderFields) {
			this.receiveHeaderFields = receiveHeaderFields;
		}
		

		public void setUsername_password(Pair<String, String> username_password) {
			this.username_password = username_password;
		}


		Result getResult() {
			return result;
		}

		
		void setResult(Result result) {
			this.result = result;
		} 
	}
	
	

	/**
	 * Create a basic Fetch object
	 * @return 
	 */
	public Fetch(){
		uriPool = new PriorityQueue<Pair<Long,URIBuilder>>(pairQueueComparator);
		this.instanceConfig = new Fetch.Configuration();
	}

	private void setUpURI(URIBuilder uri) {
		TreeMap<URIBuilder, Long> mapper = new TreeMap<URIBuilder,Long>(uriBuilderComparator);
		mapper.put(uri,0L);
		resetURIPool(mapper);
	}
	
	
	/**
	 * Create a basic Fetch object with a custom configuration
	 * @param config, the configuration that modifies default fetch behavior
	 */
	public Fetch(Fetch.Configuration config){
		this();
		if(config == null) {
			this.instanceConfig = new Fetch.Configuration();
		}
		else {
			this.instanceConfig = config;
		}
	}
	
	
	/**
	 * Create a Fetch object with a URIBuilder that is preloaded with some information
	 * @param server, something like "https://foo.com"
	 */
	public Fetch(URIBuilder uri){
		this();
		setUpURI(uri);
	}


	
	
	/**
	 * Create a Fetch object with a URIBuilder that is preloaded with some information
	 * @param server, something like "https://foo.com"
	 * @param config, the configuration that modifies default fetch behavior
	 */
	public Fetch(URIBuilder uri,Fetch.Configuration config){
		this(config);
		setUpURI(uri);
	}
	

	/**
	 * Create a Fetch object that has a pool of uris that all access the same content.
	 * So this would be something like a content distribution network.  The URI Builder objects are 
	 * expected to only be partially complete, and they are completed when the actual fetch call is made.
	 * 
	 * So a typical use is to indicate several alternate content urls like
	 * ww1.domain.com
	 * ww2.domain.com
	 * ww3.domain.com
	 * 
	 * When fetch is called, the scheme, port and path can all be specified and the Fetch object can be reused with
	 * other ports and path
	 * 
	 * @param uriPool, a set of uri's mapped to a priority (Long).  The lower the priority the earlier the uri is tried.
	 *  If a uri fails it's priority is incremented by one each time and the priorities are resorted with an unstable sort. 
	 */
	public Fetch(Map<URIBuilder,Long> uriPool){
		this();
		resetURIPool(uriPool);
	}
	
	
	/**
	 * Create a Fetch object that has a pool of uris that all access the same content.
	 * So this would be something like a content distribution network.  The URI Builder objects are 
	 * expected to only be partially complete, and they are completed when the actual fetch call is made.
	 * 
	 * So a typical use is to indicate several alternate content urls like
	 * ww1.domain.com
	 * ww2.domain.com
	 * ww3.domain.com
	 * 
	 * When fetch is called, the scheme, port and path can all be specified and the Fetch object can be reused with
	 * other ports and path
	 * 
	 * @param uriPool, a set of uri's mapped to a priority (Long).  The lower the priority the earlier the uri is tried.
	 *  If a uri fails it's priority is incremented by one each time and the priorities are resorted with an unstable sort. 
	 * @param config, the configuration that modifies default fetch behavior
	 */
	public Fetch(Map<URIBuilder,Long> uriPool,Fetch.Configuration config){
		this(config);
		resetURIPool(uriPool);
	}
	

	/**
	 * Shuffle the uris in the uri pool so that uris with the same priority value
	 * are randomly ordered but all uris with a higher number come after all uris with 
	 * a lower number.
	 * @param urlMap
	 */
	protected void resetURIPool(Map<URIBuilder,Long> urlMap) {
		
		/* Get a set ordered by success/priority */
		synchronized(uriPoolLock){
			/* Make a container */
			List<Entry<URIBuilder, Long>> shuffler = new ArrayList<Entry<URIBuilder,Long>>();
			
			/* Fill the container */
			for(Entry<URIBuilder, Long> e: urlMap.entrySet()){
				shuffler.add(e);
			}
			/* Shuffle the container */
			Collections.shuffle(shuffler); 
			
			/*erase old URIs*/
			uriPool.clear();
			
			/* Add URIs to the TreeSet in random order, but because of comparator will be grouped by priority */
			for(Entry<URIBuilder, Long> p:shuffler){
				uriPool.add(new Pair<Long,URIBuilder>(p.getValue(),p.getKey()));
			}
		}
	}
	
	
	public PriorityQueue<Pair<Long,URIBuilder>> getURIPoolCopy(){
		PriorityQueue<Pair<Long, URIBuilder>> ret = null;
		synchronized(uriPoolLock){
			ret = new PriorityQueue<Pair<Long,URIBuilder>>(pairQueueComparator);
			ret.addAll(uriPool);
		}
		return(ret);
	}
	
	
	/**
	 * Update the prioriies of the uriPool to reflect the fact that a URI just failed.
	 * @param s, the uri that just failed
	 * If s is in the uriPool created by the constructor it's priority will be incremented by 1
	 * If s is not in the urlPool it will be added with a priority of 1
	 */
	private void incrementFailCount(URIBuilder s) {
		boolean found = false;
		synchronized(uriPoolLock){
			PriorityQueue<Pair<Long, URIBuilder>> toDelete = new PriorityQueue<Pair<Long,URIBuilder>>(pairQueueComparator);
			PriorityQueue<Pair<Long, URIBuilder>> toAdd = new PriorityQueue<Pair<Long,URIBuilder>>(pairQueueComparator);
			for(Pair<Long, URIBuilder> p:uriPool){
				
				URI poolURL = null;
				try {
					poolURL = p.getSecond().build();
					
					try {
						URI failedURL = s.build();
						
						/* If we match the failing URL with one in our pool
						 *  delete the existing entry and add an incremented entry */
						if(poolURL.equals(failedURL)){
							toDelete.add(p);
							/* Only re-add it one time */
							if(!found){
								toAdd.add(new Pair<Long,URIBuilder>(p.getFirst()+1,s));
								found = true;
							}
						}
					} catch (URISyntaxException e) {
						/* s doesn't build a URI for some reason */
						throw e;
					} 
				} catch (URISyntaxException e) {
					/* p doesn't build a URI for some reason */
					toDelete.add(p);
				} 
			}
			/* Remove all the elements that should be removed */
			for(Pair<Long, URIBuilder> p:toDelete){
				uriPool.remove(p);
			}
			/* Add all the updates */
			for(Pair<Long,URIBuilder> p:toAdd){
				uriPool.add(p);
			}
			/* If we didn't find the failing URL at all, add it for the first time */
			if(!found){
				uriPool.add(new Pair<Long,URIBuilder>(1L,s));
			}
		}
	}
	
	
	
	/**
	 * This is the method that actually executes a fetch
	 * @param fetchMe is the URL to fetch, it is a
	 * @return the text obtained from the http/s fetch
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String fetchWebPage(URIBuilder fetchMe) throws IOException, URISyntaxException {
		return fetchWebPage(fetchMe,new Fetch.Result());
	}
	
	
	/**
	 * 
	 * This is the method that actually executes a fetch, include a 
	 * @param fetchMe is the URL to fetch, it is a
	 * @param result is an object that gets the results of the fetch (optional form of the call)
	 * @return the text obtained from the http/s fetch
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public String fetchWebPage(URIBuilder fetchMe,Fetch.Result result) throws IOException, URISyntaxException {
		
		String responseString = null;
		PriorityQueue<Pair<Long, URIBuilder>> servers = new PriorityQueue<Pair<Long,URIBuilder>>(pairQueueComparator);
		
		synchronized(uriPoolLock){
			servers.addAll(uriPool);
		}
		
		while(servers.size() > 0){
			URIBuilder pool = servers.poll().getSecond();
			try{
				URIBuilder _fetchMe = new URIBuilder(fetchMe.build());
				if(pool.getScheme() != null) {
					_fetchMe.setScheme(pool.getScheme());
				}
				if(pool.getHost() != null) {
					_fetchMe.setHost(pool.getHost());
				}
				if(pool.getPort() != -1) {
					_fetchMe.setPort(pool.getPort());
				}
				if(pool.getPath() != null ) {
					_fetchMe.setPath(pool.getPath());
				}
				if(pool.getQueryParams() != null ) {
					_fetchMe.addParameters(pool.getQueryParams());
				}
				if(pool.getFragment() != null ) {
					_fetchMe.setFragment(pool.getFragment());
				}
				if(pool.getUserInfo() != null ) {
					_fetchMe.setUserInfo(pool.getUserInfo());
				}
				responseString = Fetch.fetchWebPageHelper(_fetchMe, instanceConfig,result);
				/* If there was no exception then check response */
				
				break;
			}
			catch(IOException e){
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw e;
				}
			} catch (KeyManagementException e) {
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw new RuntimeException(e);
				}
			} catch (NoSuchAlgorithmException e) {
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw new RuntimeException(e);
				}
			} catch (NoSuchProviderException e) {
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw new RuntimeException(e);
				}
			} catch (UnrecoverableKeyException e) {
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw new RuntimeException(e);
				}
			} catch (KeyStoreException e) {
				incrementFailCount(pool);
				if(servers.size() == 0){
					throw new RuntimeException(e);
				}
			}
		}
		return responseString;
	}
	
	
	
	/**
	 * Fetch a web page's contents. If the webpage errors out or fails to parse JSON it's considered an error. 
	 * @param fetchMe The URI to fetch
	 * @return the JSON obtained
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public JSONObject fetchJSONObject(URIBuilder fetchMe) throws IOException, URISyntaxException {
				
		return (JSONObject) JSONValue.parse(fetchWebPage(fetchMe));
	}
	
	
	/**
	 * 
	 * Fetch a web page's contents. If the webpage errors out or fails to parse JSON it's considered an error. 
	 * @param fetchMe The URI to fetch
	 * @param result is a result object for feedback about call (optional form)
	 * @return the JSON obtained
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public JSONObject fetchJSONObject(URIBuilder fetchMe,Fetch.Result result) throws IOException, URISyntaxException {
				
		return (JSONObject) JSONValue.parse(fetchWebPage(fetchMe,result));
	}
}