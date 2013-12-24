/*
	Copyright 2007-2013
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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class WebUtil {
	
	private static final String UTF8 = "UTF-8";

	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = Logger.getLogger(WebUtil.class);
		}
		return log;
	}
	

    /**
	 * Close a reader/writer/stream, ignoring any exceptions that result. Also
	 * flushes if there is a flush() method.
	 */
	
	public static void close(Closeable input) {
		if (input == null)
			return;
		// Flush (annoying that this is not part of Closeable)
		try {
			Method m = input.getClass().getMethod("flush");
			m.invoke(input);
		} catch (NoSuchMethodException e) {
			getLog().log(Level.DEBUG,"No Such Method Exception: flush");
		} catch (IllegalAccessException e) {
			getLog().log(Level.ERROR,"",e);
		} catch (InvocationTargetException e) {
			getLog().log(Level.ERROR,"",e);
		} catch (RuntimeException e) {
			getLog().log(Level.ERROR,"",e);
		}
		// Close
		try {
			input.close();
		} catch (IOException e) {
			// Ignore
		}
	}
	/**
	 * Use a buffered reader (preferably UTF-8) to extract the contents of the
	 * given stream. A convenience method for {@link #toString(Reader)}.
	 * @throws IOException 
	 */
	
	public static String toString(InputStream inputStream) throws IOException {
		InputStreamReader reader;
		try {
			reader = new InputStreamReader(inputStream, UTF8);
		} catch (UnsupportedEncodingException e) {
			reader = new InputStreamReader(inputStream);
		}
		
		return toString(reader);
	}

	

	/**
	 * Use a buffered reader to extract the contents of the given reader.
	 *
	 * @param reader
	 * @return The contents of this reader.
	 * @throws IOException 
	 */
	public static String toString(Reader reader) throws IOException {
		try {
			// Buffer if not already buffered
			reader = reader instanceof BufferedReader ? (BufferedReader) reader
					: new BufferedReader(reader);
			StringBuilder output = new StringBuilder();
	
			while (true) {
				int c = reader.read();
				if (c == -1)
					break;
				output.append((char) c);
			}
			return output.toString();
		} catch (IOException ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
			close(reader);
		}
	}


	public static String encode(Object x) {
		try {
			return URLEncoder.encode(String.valueOf(x),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return(String.valueOf(x));
		}
	}
	
	

	
	/**
	 * Set a header for basic authentication login.
	 *
	 * @param username
	 * @param password
	 * @param connection
	 */
	
	public static void setBasicAuthentication(String username, String password,URLConnection connection)
	{
		assert ((username != null) && (password != null)) : "Need name and password for this method";
	
		String userPassword = username + ":" + password;
		String encoding = Base64.encodeBase64String(userPassword.getBytes());
	
		connection.setRequestProperty("Authorization", "Basic " + encoding);
	}
	
	
	/**
	 * Fetch a web page's contents. Note that this will change all line breaks
	 * into system line breaks!
	 *
	 * @param uri
	 *            The web-page (or file) to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects.
	 * @param authenticate
	 * 	 		  True if basic authentication should be used.  In which case vars needs to have
	 *            an entry for "username" and "password".           
	 * @param vars
	 * 			  A Map of params to be sent on the uri. "username" and "password" is removed before
	 *            calling the uri.           
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @return The full text of the web page
	 *
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	
	public static String fetchWebPage(String uri, boolean authenticate, Map<String, String> vars, int timeOutMilliSecs) throws  MalformedURLException, IOException
	{
		return fetchWebPage(uri, authenticate, vars, timeOutMilliSecs, null,null);
	}



	/**
	 * Fetch a web page's contents. Note that this will change all line breaks
	 * into system line breaks!
	 * 
	 * @param uri
	 *            The web-page (or file) to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects.
	 * @param authenticate
	 *            True if basic authentication should be used. In which case
	 *            vars needs to have an entry for "username" and "password".
	 * @param vars
	 *            A Map of params to be sent on the uri. "username" and
	 *            "password" is removed before calling the uri.
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @param headerFields
	 *            TODO
	 * @return The full text of the web page
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 */

	public static String fetchWebPage(String uri, boolean authenticate,
			Map<String, String> vars, int timeOutMilliSecs,
			Map<String, String> sendHeaderFields,
			Map<String, List<String>> receiveHeaderFields)
			throws MalformedURLException, IOException {
		assert timeOutMilliSecs > 0;

		/* Get authenticate information */
		String username = null;
		String password = null;
		if (authenticate) {
			if (vars != null) {
				username = vars.get("username");
				password = vars.get("password");
				vars.remove("username");
				vars.remove("password");
			}
		}

		/* Build URL query */
		StringBuffer local = new StringBuffer();
		if(vars != null){
			local.append("?");
			for(Entry<String, String> e : vars.entrySet()){
				if (e.getValue() == null)
					continue;
				local.append(encode(e.getKey()));
				local.append("=");
				local.append(encode(e.getValue()));
				local.append("&");
			}
		}
		uri += local.toString();

		// Setup a connection
		final HttpURLConnection connection;
		InputStream inStream = null;

		connection = (HttpURLConnection) new URL(uri).openConnection();
		if(connection instanceof HttpsURLConnection){
			((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String arg0, SSLSession arg1) {
					if(arg0.equals("localhost")){
						return true;
					}
					else{
						throw new RuntimeException("Unexpected certificate mismatch, host:"+arg0+"\nSession:"+arg1.toString());
					}
				}
			});
		}

		

		// Authenticate
		if (authenticate) {
			setBasicAuthentication(username, password, connection);
		}

		/* Set properties of connection */
		// http://en.wikipedia.org/wiki/User_agent
		connection .setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;)");
		if(sendHeaderFields != null){
			for (Map.Entry<String, String> e : sendHeaderFields.entrySet()) {
				connection.setRequestProperty(e.getKey(),e.getValue());
			}
		}

		connection.setDoInput(true);
		connection.setReadTimeout(timeOutMilliSecs);

		// Open a connection
//		try {
			inStream = connection.getInputStream();
			if (receiveHeaderFields != null) {
				receiveHeaderFields.clear();
				receiveHeaderFields.putAll(connection.getHeaderFields());
			}
//		} catch (FileNotFoundException e1) { // This happens with 404s
//			e1.printStackTrace();
//			getLog().error("404 Error: Page not found " + uri);
//		}
	
		// Read in the web page
		String page = toString(inStream);

		// Done
		return page;
	}
	

	
	/**
	 * Fetch a web page's header.
	 *
	 * @param uri
	 *            The web-page (or file) to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects.
	 * @param authenticate
	 * 	 		  True if basic authentication should be used.  In which case vars needs to have
	 *            an entry for "username" and "password".           
	 * @param vars
	 * 			  A Map of params to be sent on the uri. "username" and "password" is removed before
	 *            calling the uri.           
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @return The header of the web page
	 *
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	
	public static Map<String, List<String>> fetchWebPageHeader(String uri, boolean authenticate, Map<String, String> vars, int timeOutMilliSecs) throws  MalformedURLException, IOException
	{
		return fetchWebPageHeader(uri, authenticate, vars, timeOutMilliSecs, null);
	}



	/**
	 * Fetch a web page's header.
	 * 
	 * @param uri
	 *            The web-page (or file) to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects.
	 * @param authenticate
	 *            True if basic authentication should be used. In which case
	 *            vars needs to have an entry for "username" and "password".
	 * @param vars
	 *            A Map of params to be sent on the uri. "username" and
	 *            "password" is removed before calling the uri.
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @param headerFields
	 *            TODO
	 * @return The header of the web page
	 * 
	 * @throws IOException
	 * @throws MalformedURLException
	 */

	public static Map<String, List<String>> fetchWebPageHeader(String uri, boolean authenticate,
			Map<String, String> vars, int timeOutMilliSecs,
			Map<String, String> sendHeaderFields)
			throws MalformedURLException, IOException {
		assert timeOutMilliSecs > 0;

		/* Get authenticate information */
		String username = null;
		String password = null;
		if (authenticate) {
			if (vars != null) {
				username = vars.get("username");
				password = vars.get("password");
				vars.remove("username");
				vars.remove("password");
			}
		}

		/* Build URL query */
		StringBuffer local = new StringBuffer();
		if(vars != null){
			local.append("?");
			for(Entry<String, String> e : vars.entrySet()){
				if (e.getValue() == null)
					continue;
				local.append(encode(e.getKey()));
				local.append("=");
				local.append(encode(e.getValue()));
				local.append("&");
			}
		}
		uri += local.toString();

		// Setup a connection
		final HttpURLConnection connection;

		connection = (HttpURLConnection) new URL(uri).openConnection();
		connection.setRequestMethod("HEAD");

		// Authenticate
		if (authenticate) {
			setBasicAuthentication(username, password, connection);
		}

		/* Set properties of connection */
		// http://en.wikipedia.org/wiki/User_agent
		connection .setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;)");
		if(sendHeaderFields != null){
			for (Map.Entry<String, String> e : sendHeaderFields.entrySet()) {
				connection.setRequestProperty(e.getKey(),e.getValue());
			}
		}

		connection.setDoInput(true);
		connection.setReadTimeout(timeOutMilliSecs);

		// This is just here for debugging
		/*
		try {
			InputStream inStream = connection.getInputStream();
			// Read in the web page
			String page = toString(inStream);
			if(page.equals("testing")){
				
			}
		} catch (FileNotFoundException e1) { // This happens with 404s
			e1.printStackTrace();
			getLog().error("404 Error: Page not found " + uri);
		}*/
	
		
		// Done
		return connection.getHeaderFields();
	}

}
