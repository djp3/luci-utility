package edu.uci.ics.luci.utility.webserver.input.channel.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpServerConnection;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.MyKeyManager;
import edu.uci.ics.luci.utility.webserver.input.channel.Input;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.channel.socket.Output_Socket_HTTP;

public class HTTPInputOverSocket extends Input{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HTTPInputOverSocket.class);
		}
		return log;
	}
	
	private int port;
	private boolean secure;
	private DefaultBHttpServerConnectionFactory connFactory;


	public HTTPInputOverSocket(int port, boolean secure){
		this.port = port;
		this.secure = secure;
        
		this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
	}

	@Override
	public int getPort(){
		return this.port;
	}

	@Override
	public boolean getSecure() {
		return secure;
	}

	public void setSecure(Boolean secure) {
		this.secure = secure;
	}
	
	private ServerSocket localServerSocket = null;
	private synchronized ServerSocket getServerSocket(){
		if(localServerSocket == null){
			return (localServerSocket = initializeServerSocket());
		}
		else{
			return this.localServerSocket;
		}
	}
	
	private ServerSocket initializeServerSocket(){
		ServerSocket serverSoc = null;
		try{
			if(getSecure()){
				SSLContext sctx1 = null;
				try{
					/* See the README.txt in test/keystore for information on how to make the credentials*/
					sctx1 = SSLContext.getInstance("SSLv3");
					sctx1.init(new X509KeyManager[] { 
						new MyKeyManager(
								System.getProperty("javax.net.ssl.keyStore"),
								System.getProperty("javax.net.ssl.keyStorePassword").toCharArray(),
								System.getProperty("edu.uci.ics.luci.webserver.Alias")
								)
						}
					,null,null);
				} catch (NoSuchAlgorithmException e) {
					getLog().fatal("I'm not into this error:\n"+e);
				} catch (KeyManagementException e) {
					getLog().fatal("Problem managing keys:\n"+e);
				} catch (GeneralSecurityException e) {
					getLog().fatal("Security Exception:\n"+e);
				}
			
				//SSLServerSocketFactory ssocketFactory = (SSLServerSocketFactory) sctx1.getServerSocketFactory();
				ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
				serverSoc = ssocketFactory.createServerSocket(port);
				serverSoc.setSoTimeout(1000);
			}
			else{
				serverSoc = new ServerSocket(port);
				serverSoc.setSoTimeout(1000);
			}
		} catch (IOException e) {
			getLog().fatal(e.toString());
		}
		return serverSoc;
	}
	
	/* 
	   private void appendIncomingData(BufferedInputStream bis, StringBuilder request) {
		int nBytes = -1;
		byte[] readBytes = new byte[5120];
		
		try {
			while(bis.available() > 0){
				nBytes = bis.read(readBytes, 0, 5120);
				request.append(new String(readBytes, 0, nBytes));
			}
		} catch (IOException e) {
			getLog().error(e);
		}
		
	}*/

	/**
	    *
	    * Parses a header string passed from the client to the
	    * server and builds a <code>Map</code> object
	    * with key-value pairs. 
	    * The query string should be in the form of a header
	    * packaged by the GET or POST method, that is, it
	    * should have key-value pairs in the form <i>key:value</i>,
	    * with each pair separated from the next by a newline character.
	    *
	    * <p>A key can appear more than once in the query string
	    * with different values. However, the key appears only once in 
	    * the map, with its value being rewritten in the case of multiple values sent
	    * by the query string.
	    * 
	    *
	    * @param s		a string containing the query to be parsed
	    *
	    * @return		a <code>Map</code> object built
	    * 			from the parsed key-value pairs
	    *
	    * @exception IllegalArgumentException	if the query string 
	    *						is invalid
	    *
	    */
	
	   public static Map<String, String> parseHeaderString(String s) {
	
		   if (s == null) {
			   throw new IllegalArgumentException("s is null");
		   }
		
		   Map<String,String> ht = new HashMap<String,String>();
		   StringTokenizer st = new StringTokenizer(s, "\n");
		
		   while (st.hasMoreTokens()) {
			   String pair = st.nextToken();
			   int pos = pair.indexOf(':');
			   String key;
			   String value;
			   if (pos == -1) {
				   key = pair;
				   value = null;
			   }
			   else{
				   key = pair.substring(0, pos).trim();
				   value =pair.substring(pos+1, pair.length()).trim();
			   }
			   if (ht.containsKey(key)) {
				   getLog().warn("http request had repeated keys");
			   }
			   
			   ht.put(key, value);
		   }
		   return ht;
	   }

		/*
		 * Parse a name in the query string.
		 */
		static String parseName(String s, StringBuffer sb) {
			
			sb.setLength(0);
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i); 
				switch (c) {
					case '+':
						sb.append(' ');
						break;
					case '%':
						try {
							sb.append((char) Integer.parseInt(s.substring(i+1, i+3),  16));
							i += 2;
						} catch (NumberFormatException e) {
							// XXX
							// need to be more specific about illegal arg
							throw new IllegalArgumentException();
						} catch (StringIndexOutOfBoundsException e) {
							String rest  = s.substring(i);
							sb.append(rest);
							if (rest.length()==2)
								i++;
						}
						break;
					default:
						sb.append(c);
					break;
				}
			}
			return sb.toString();
		}

		
	   
	   /**
	    *
	    * Parses a query string passed from the client to the
	    * server and builds a <code>Map</code> object
	    * with key-value pairs. 
	    * The query string should be in the form of a string
	    * packaged by the GET or POST method, that is, it
	    * should have key-value pairs in the form <i>key=value</i>,
	    * with each pair separated from the next by a &amp; character.
	    *
	    * <p>A key can appear more than once in the query string
	    * with different values. However, the key appears only once in 
	    * the map, with its value being rewritten in the case of multiple values sent
	    * by the query string.
	    * 
	    * <p>The keys and values in the map are stored in their
	    * decoded form, so
	    * any + characters are converted to spaces, and characters
	    * sent in hexadecimal notation (like <i>%xx</i>) are
	    * converted to ASCII characters.
	    *
	    * @param s		a string containing the query to be parsed
	    *
	    * @return		a <code>Map</code> object built
	    * 			from the parsed key-value pairs
	    *
	    * @exception IllegalArgumentException	if the query string 
	    *						is invalid
	    *
	    */
	
	   public static Map<String, String> parseQueryString(String s) {
	
		   if (s == null) {
			   throw new IllegalArgumentException("s is null");
		   }
		
		   Map<String,String> ht = new HashMap<String,String>();
		   StringBuffer sb = new StringBuffer();
		   StringTokenizer st = new StringTokenizer(s, "&");
		
		   while (st.hasMoreTokens()) {
			   String pair = st.nextToken();
			   int pos = pair.indexOf('=');
			   String key;
			   String value;
			   if (pos == -1) {
				   key = pair;
				   value = null;
			   }
			   else{
				   key = parseName(pair.substring(0, pos), sb);
				   value = parseName(pair.substring(pos+1, pair.length()), sb);
			   }
			   if (ht.containsKey(key)) {
				   getLog().warn("http request had repeated keys");
			   }
			   
			   ht.put(key, value);
		   }
		   return ht;
	   }
	   


	@Override
	public Callable<Pair<Request,Output>> waitForIncomingRequest() {
		java.net.Socket localSocket = null;
		try{
			localSocket = this.getServerSocket().accept();
			HttpServerConnection conn = this.connFactory.createConnection(localSocket);
			
			return new MyHandler(localSocket.getInetAddress().toString(),conn);
		} catch(SocketTimeoutException e){
			getLog().debug("Socket timed out\n"+e);
			return null;
		} catch (IOException e) {
			getLog().debug("Socket timed out\n"+e);
			getLog().error("Problem getting incoming socket connection\n"+e);
			return null;
		}
	}
	

	
	private class MyHandler implements Callable<Pair<Request,Output>>{
		
		private String source;
		private HttpServerConnection conn;

		MyHandler(String source, final HttpServerConnection conn){
			
			this.source = source;
			
			if(conn == null){
				throw new IllegalArgumentException("conn can't be null");
			}
			this.conn = conn;
		}
		
		
		@Override
		public Pair<Request, Output> call() throws Exception {
			
			Request request = new Request();
			/* Set Source */
			request.setSource(source);
			
			HttpRequest httpRequest = conn.receiveRequestHeader();
			
			/* Set Headers */
			Header[] h = httpRequest.getAllHeaders();
			Map<String,List<String>> headers = new HashMap<String,List<String>>();
			for(int i = 0 ; i < h.length;i++){
				String key = h[i].getName();
				String value = h[i].getValue();
				List<String> list = null;
				if(headers.containsKey(key)){
					list = headers.get(key);
				}
				else{
					list = new ArrayList<String>();
				}
				list.add(value);
				headers.put(key,list);
			}
			request.setHeaders(headers);
			
			/* Set Protocol/Method */
			String method = httpRequest.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			if(method.equals("GET")){
				request.setProtocol(Protocol.HTTP_GET);
			}
			else if(method.equals("POST")){
				request.setProtocol(Protocol.HTTP_POST);
			}
			else{
				request.setProtocol(Protocol.UNKNOWN);
			}
			
			/* Set the REST command */
            URIBuilder uri = new URIBuilder(httpRequest.getRequestLine().getUri());
            request.setCommand(uri.getPath());
            
            Map<String, Set<String>> parameters = new HashMap<String,Set<String>>();
            for (NameValuePair nvp : uri.getQueryParams()) {
            	Set<String> set = null;
            	if(parameters.containsKey(nvp.getName())){
            		set = parameters.get(nvp.getName());
            	}
            	else{
            		set = new HashSet<String>();
            	}
            	set.add(nvp.getValue());
            	parameters.put(nvp.getName(), set);
            }
            
            request.setParameters(parameters);
            
            /* Try and get the parameters */
            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);
                System.err.println("Incoming entity content (bytes): " + entityContent.length);
            }

				
			//TODO: Set all the required components of icr
			//TODO: Rebuild an outputChannel object
				
			if (httpRequest instanceof HttpEntityEnclosingRequest) {
				conn.receiveRequestEntity((HttpEntityEnclosingRequest) httpRequest);
				HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
				if (entity != null) {
					// Do something useful with the entity and, when done, ensure all
					// content has been consumed, so that the underlying connection
					// could be re-used
					EntityUtils.consume(entity);
				}
			}
			
			Output_Socket_HTTP oc = new Output_Socket_HTTP(conn);
			
			getLog().info("Received a request on the wire:\n"+request.toString());
			return new Pair<Request,Output>(request,oc);
			
			
		}
	}



	@Override
	public void closeChannel() {
		if(localServerSocket != null){
			try {
				localServerSocket.close();
			} catch (IOException e) {
			}
		}
		localServerSocket = null;
		
	}

	
}
