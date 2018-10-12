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

package edu.uci.ics.luci.utility.webserver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.datastructure.Pair;

public class WebUtil {
	
//	private static final String UTF8 = "UTF-8";

	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(WebUtil.class);
		}
		return log;
	}
	

    /**
	 * Close a reader/writer/stream, ignoring any exceptions that result. Also
	 * flushes if there is a flush() method.
	 */
	
//	public static void close(Closeable input) {
//		if (input == null)
//			return;
//		// Flush (annoying that this is not part of Closeable)
//		try {
//			Method m = input.getClass().getMethod("flush");
//			m.invoke(input);
//		} catch (NoSuchMethodException e) {
//			getLog().debug("No Such Method Exception: flush");
//		} catch (IllegalAccessException e) {
//			getLog().error("",e);
//		} catch (InvocationTargetException e) {
//			getLog().error("",e);
//		} catch (RuntimeException e) {
//			getLog().error("",e);
//		}
//		// Close
//		try {
//			input.close();
//		} catch (IOException e) {
//			// Ignore
//		}
//	}
	/**
	 * Use a buffered reader (preferably UTF-8) to extract the contents of the
	 * given stream. A convenience method for {@link #toString(Reader)}.
	 * @throws IOException 
	 */
	
//	public static String toString(InputStream inputStream) throws IOException {
//		InputStreamReader reader;
//		try {
//			reader = new InputStreamReader(inputStream, UTF8);
//		} catch (UnsupportedEncodingException e) {
//			reader = new InputStreamReader(inputStream);
//		}
//		
//		return toString(reader);
//	}

	

	/**
	 * Use a buffered reader to extract the contents of the given reader.
	 *
	 * @param reader
	 * @return The contents of this reader.
	 * @throws IOException 
	 */
//	public static String toString(Reader reader) throws IOException {
//		try {
//			// Buffer if not already buffered
//			reader = reader instanceof BufferedReader ? (BufferedReader) reader
//					: new BufferedReader(reader);
//			StringBuilder output = new StringBuilder();
//	
//			while (true) {
//				int c = reader.read();
//				if (c == -1)
//					break;
//				output.append((char) c);
//			}
//			return output.toString();
//		} catch (IOException ex) {
//			ex.printStackTrace();
//			throw ex;
//		} finally {
//			close(reader);
//		}
//	}


//	public static String encode(Object x) {
//		try {
//			return URLEncoder.encode(String.valueOf(x),"UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			return(String.valueOf(x));
//		}
//	}
	
	/**
	 * This is a class used for testing that will accept any https certificates
	 * @author djp3
	 *
	 */
	private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
	
	

	
	
	/**
	 *	REST Request: Fetch a data from the internet over a socket using http 
	 * 
	 * @param protocolScheme
	 *            "http" or "https" are likely values
	 * @param host
	 *            "www.cnn.com", "localhost" etc.
	 * @param port
	 *            80, 443, 9020, etc.
	 * @param path
	 *            "/", "/index.html", "/path/to/index.html"
	 * @param uriParams
	 *            These are uri encoded parameters as in "?a=foo"
	 * @param sendHeaderFields
	 *            These are sent in the HTTP header
	 * @param receiveHeaderFields
	 *            This is where we put headers that come back
	 * @param authenticate
	 *            True if basic authentication should be used. In which case @param
	 *            uriParams needs to have an entry for "username" and
	 *            "password".
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Must be greater than 0
	 * @return the data
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws NoSuchAlgorithmException 
	 * @throws UnrecoverableKeyException 
	 * @throws KeyManagementException 
	 */
	public static String fetchWebPage(
			URIBuilder uriBuilder,
			Map<String, String> sendHeaderFields,
			final Map<String, List<String>> receiveHeaderFields,
			Pair<String,String> username_password,
			int timeOutMilliSecs) throws IOException, URISyntaxException {

		/* Deal with authentication */
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if(username_password != null){
			credsProvider.setCredentials(
					new AuthScope(uriBuilder.getHost(), uriBuilder.getPort()),
					new UsernamePasswordCredentials(username_password.getFirst(),username_password.getSecond()));
		}

		// Make the URI
		URI uri = uriBuilder.build();

		// Set up the connection
		CloseableHttpClient httpclient = null;
		try {
			// Set the timeouts
			if (timeOutMilliSecs < 0) {
				timeOutMilliSecs = 0;
			}
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(timeOutMilliSecs)
					.setConnectTimeout(timeOutMilliSecs)
					.setConnectionRequestTimeout(timeOutMilliSecs)
					.setStaleConnectionCheckEnabled(true)
					.build();

			if (Globals.getGlobals().isTesting()) {
				// Allow the remote domain to not match the remote certificate
				// when testing or trust the certificate

		        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
		        ctx.init(new KeyManager[0],
		        		new TrustManager[] {
		        				new DefaultTrustManager()
		        		}, new SecureRandom());
		        SSLContext.setDefault(ctx);

		        /*
				SSLContextBuilder builder = new SSLContextBuilder();
				builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
			    builder.loadTrustMaterial(null, new TrustStrategy() {
					@Override
					public boolean isTrusted(
							java.security.cert.X509Certificate[] chain,
							String authType)
							throws java.security.cert.CertificateException {
						return true;
					}
			    });*/
			    //SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory( builder.build(),SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory( ctx,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				httpclient = HttpClients
						.custom()
						.setHostnameVerifier( SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
						.setSSLSocketFactory(sslsf)
						.setDefaultRequestConfig(defaultRequestConfig)
						.setDefaultCredentialsProvider(credsProvider)
						.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false))
						.build();
			} else {
				httpclient = HttpClients.custom()
						.setDefaultRequestConfig(defaultRequestConfig)
						.setDefaultCredentialsProvider(credsProvider)
						.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false))
						.build();
			}

			HttpGet httpget = new HttpGet(uri);

			/* Add Header Fields if provided */
			if (sendHeaderFields != null) {
				for (Entry<String, String> p : sendHeaderFields.entrySet()) {
					httpget.setHeader(p.getKey(), p.getValue());
				}
			}

			/* Set up a call back for the response */
			ResponseHandler<String> rh = new ResponseHandler<String>() {
				@Override
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();

					if (statusLine.getStatusCode() >= 300) {
						throw new HttpResponseException(
								statusLine.getStatusCode(),
								statusLine.getReasonPhrase());
					}

					if (entity == null) {
						throw new ClientProtocolException(
								"Response contains no content");
					} else {

						if (receiveHeaderFields != null) {
							BasicHeaderElementIterator hit = new BasicHeaderElementIterator(response.headerIterator());
							while (hit.hasNext()) {
								HeaderElement elem = hit.nextElement();
								List<String> list = null;
								if (receiveHeaderFields.containsKey(elem.getName())) {
									list = receiveHeaderFields.get(elem.getName());
									list.add(elem.getValue());
								} else {
									list = new ArrayList<String>();
									list.add(elem.getValue());
								}
								receiveHeaderFields.put(elem.getName(), list);
							}
						}

						ContentType contentType = ContentType
								.getOrDefault(entity);
						Charset charset = contentType.getCharset();
						return EntityUtils.toString(entity, charset);
					}
				}
			};

			return httpclient.execute(httpget, rh);

		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			if (httpclient != null) {
				httpclient.close();
			}
		}
		return null;
	}

	/**
	 *	Fetch the http header of a URI on the web.  This code should be almost exactly the same as
	 *  fetchWebPage 
	 * 
	 * @param protocolScheme
	 *            "http" or "https" are likely values
	 * @param host
	 *            "www.cnn.com", "localhost" etc.
	 * @param port
	 *            80, 443, 9020, etc.
	 * @param path
	 *            "/", "/index.html", "/path/to/index.html"
	 * @param uriParams
	 *            These are uri encoded parameters as in "?a=foo"
	 * @param sendHeaderFields
	 *            These are sent in the HTTP header
	 * @param receiveHeaderFields
	 *            This is where we put headers that come back
	 * @param authenticate
	 *            True if basic authentication should be used. In which case @param
	 *            uriParams needs to have an entry for "username" and
	 *            "password".
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Must be greater than 0
	 * @return the data
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String fetchWebPageHeader(
			URIBuilder uriBuilder,
			Map<String, String> sendHeaderFields,
			final Map<String, List<String>> receiveHeaderFields,
			Pair<String,String> username_password,
			int timeOutMilliSecs) throws IOException, URISyntaxException {

		/* Deal with authentication */
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		if(username_password != null){
			credsProvider.setCredentials(
					new AuthScope(uriBuilder.getHost(), uriBuilder.getPort()),
					new UsernamePasswordCredentials(username_password.getFirst(),username_password.getSecond()));
		}

		// Make the URI
		URI uri = uriBuilder.build();

		// Set up the connection
		CloseableHttpClient httpclient = null;
		try {
			// Set the timeouts
			if (timeOutMilliSecs < 0) {
				timeOutMilliSecs = 0;
			}
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(timeOutMilliSecs)
					.setConnectTimeout(timeOutMilliSecs)
					.setConnectionRequestTimeout(timeOutMilliSecs)
					.setStaleConnectionCheckEnabled(true).build();

			if (Globals.getGlobals().isTesting()) {
				// Allow the remote domain to not match the remote certificate
				// when testing or trust the certificate

		        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
		        ctx.init(new KeyManager[0],
		        		new TrustManager[] {
		        				new DefaultTrustManager()
		        		}, new SecureRandom());
		        SSLContext.setDefault(ctx);
		        
		        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory( ctx,SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				httpclient = HttpClients
						.custom()
						.setHostnameVerifier( SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
						.setSSLSocketFactory(sslsf)
						.setDefaultRequestConfig(defaultRequestConfig)
						.setDefaultCredentialsProvider(credsProvider)
						.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false))
						.build();
			} else {
				httpclient = HttpClients.custom()
						.setDefaultRequestConfig(defaultRequestConfig)
						.setDefaultCredentialsProvider(credsProvider)
						.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false))
						.build();
			}

			HttpHead httphead = new HttpHead(uri);

			/* Add Header Fields if provided */
			if (sendHeaderFields != null) {
				for (Entry<String, String> p : sendHeaderFields.entrySet()) {
					httphead.setHeader(p.getKey(), p.getValue());
				}
			}

			/* Set up a call back for the response */
			ResponseHandler<String> rh = new ResponseHandler<String>() {
				@Override
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();

					if (statusLine.getStatusCode() >= 300) {
						throw new HttpResponseException(
								statusLine.getStatusCode(),
								statusLine.getReasonPhrase());
					}

//					if (entity == null) {
//						throw new ClientProtocolException(
//								"Response contains no content");
//					} else {

						if (receiveHeaderFields != null) {
							BasicHeaderElementIterator hit = new BasicHeaderElementIterator(
									response.headerIterator());
							while (hit.hasNext()) {
								HeaderElement elem = hit.nextElement();
								List<String> list = null;
								if (receiveHeaderFields.containsKey(elem .getName())) {
									list = receiveHeaderFields.get(elem .getName());
									list.add(elem.getValue());
								} else {
									list = new ArrayList<String>();
									list.add(elem.getValue());
								}
								receiveHeaderFields.put(elem.getName(), list);
							}
						}

						ContentType contentType = ContentType
								.getOrDefault(entity);
						Charset charset = contentType.getCharset();
						if(entity == null) {
							return null;
						}
						else {
							return EntityUtils.toString(entity, charset);
						}
//					}
				}
			};

			return httpclient.execute(httphead, rh);

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} finally {
			if (httpclient != null) {
				httpclient.close();
			}
		}
		return null;
	}
	
			
}

	
