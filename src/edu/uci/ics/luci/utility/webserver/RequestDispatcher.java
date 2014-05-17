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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minidev.json.JSONArray;

import org.apache.log4j.Logger;

import edu.uci.ics.luci.utility.datastructure.Pair;


public class RequestDispatcher implements Runnable{
	
	private static final byte[] EOL = {(byte)'\r', (byte)'\n' };
    protected static final String HTTP_OK = "200 OK";
    protected static final String HTTP_REDIRECT_UNSPECIFIED = "302 Found";
    protected static final String HTTP_NOT_FOUND = "404 Not Found";
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = Logger.getLogger(RequestDispatcher.class);
		}
		return log;
	}
	
	
    private static int jobCounter = 0;
    
	private static void incrementJobCounter() {
		jobCounter++;
	}
	
	public static int getJobCounter() {
		return jobCounter;
	}

	public static void setJobCounter(int jobCounter) {
		RequestDispatcher.jobCounter = jobCounter;
	}
	
	
	public enum HTTPRequest{GET,POST, UNKNOWN};
	
	private WebServer webServer = null;
	private Queue<Socket> socketQueue = null;
	private Map<String, HandlerAbstract> requestHandlerRegistry = null;
	private Map<Class<? extends HandlerAbstract>,List<HandlerAbstract>> requestHandlers = null;
	private int numInstancesToStage = 10;
	
	private byte[] readBytes = null;
	private int numLiveInstantiatingThreads;
	private int numInstantiatingThreadsInvoked;
	
	public RequestDispatcher(Map<String,HandlerAbstract> requestHandlerRegistry){
		super();
		setRequestHandlerRegistry(requestHandlerRegistry);
		readBytes = new byte[5120];
		numLiveInstantiatingThreads = 0;
		numInstantiatingThreadsInvoked = 0;
		socketQueue = new ConcurrentLinkedQueue<Socket>();
	}
	
	
	/**
	 * Fill up the prestaged instances with the number of required instances for this function 
	 * @param handler, an example of the handler which is copied and prestaged
	 */
	private synchronized void stageNewInstances(HandlerAbstract handler){
		
		if(handler == null){
			return;
		}
		
		/* Make sure the list is sound */
		List<HandlerAbstract> list = requestHandlers.get(handler.getClass());
		if(list == null){
			list = new ArrayList<HandlerAbstract>(numInstancesToStage);
			requestHandlers.put(handler.getClass(), list);
		}
		/* Make sure the list needs new instances */
		if(list.size() >= numInstancesToStage){
			return;
		}
	
		/* Stage the new instances */
		while(list.size() < numInstancesToStage){
			list.add(handler.copy());
		}
		requestHandlers.put(handler.getClass(), list);
	}
	
	
	private void stageNewInstancesInThread(final HandlerAbstract handlerTemplate) {
		Thread instantiationThread = new Thread(new Runnable (){
			public void run() {
				numLiveInstantiatingThreads++;
				numInstantiatingThreadsInvoked++;
				try{
					stageNewInstances(handlerTemplate);
				}
				finally{
					numLiveInstantiatingThreads--;
				}
			};
		}
		);
		instantiationThread.setName("Instantiating new "+handlerTemplate.getClass().getCanonicalName()+" to handle requests");
		instantiationThread.setDaemon(false); //force clean shutdown
		instantiationThread.start();
	}
	
	
	/**
	 * 
	 * @param restFunction
	 * @return Returns the RequestHandlerHelper that is associated with handling the restFunction.
	 *  This is determined based on the registry that was passed to the RequestHandlerFactory and the default RequestHandlerHelper.
	 *  If the RequestHandlerHelper has not been instantiated before then it is instantiated on
	 *  the fly.
	 */
	public synchronized HandlerAbstract getHandler(String restFunction){
		
		/* This is the one we will return */
		HandlerAbstract handler = null;
		
		/* Check error conditions */
		if(requestHandlerRegistry == null){
			getLog().error("Call setRequestHandlerRegistry(requestHandlerRegistry) before trying to get handlers");
			return null;
		}
		
		/* Get the template that the restFunction maps to */
		HandlerAbstract handlerTemplate = requestHandlerRegistry.get(restFunction);
		
		/* If the class isn't present then grab the default template */
		if(handlerTemplate == null){
			handlerTemplate = requestHandlerRegistry.get(null);
		}
		
		/* If we don't have a class there is nothing to instantiate :( */
		if(handlerTemplate == null){
			return null;
		}
		
		/* Check to see if we have instantiated one or more of these and get it */
		synchronized(requestHandlers){
			if(requestHandlers.containsKey(handlerTemplate.getClass())){
				/* Get it */
				List<HandlerAbstract> prestage = requestHandlers.remove(handlerTemplate.getClass());
				if(prestage != null){
					if(prestage.size() > 0){
						handler = prestage.remove(0); 
						/* Put the leftovers back */
						requestHandlers.put(handlerTemplate.getClass(), prestage);
					}
					if((prestage.size() < this.numInstancesToStage) && (getNumLiveInstantiatingThreads() == 0)){
						stageNewInstancesInThread(handlerTemplate);
					}
				}
			}
		}
		
		/* If there was no prestaged handler, then make one on the fly */
		if(handler == null){
			handler = handlerTemplate.copy();
			if((this.numInstancesToStage > 0) && (getNumLiveInstantiatingThreads() == 0)){
				stageNewInstancesInThread(handlerTemplate);
			}
		}
		
		return(handler);
	}

	
	
	public WebServer getWebServer() {
		return webServer;
	}
	public void setWebServer(WebServer webServer) {
		this.webServer = webServer;
	}
	
	public synchronized int getRequestHandlerRegistrySize(){
		return this.requestHandlerRegistry.size();
	}
	
	public synchronized int getRequestHandlersSize(Class<? extends HandlerAbstract> key){
		return requestHandlers.get(key).size();
	}
	
	public int getNumLiveInstantiatingThreads(){
		return this.numLiveInstantiatingThreads;
	}
	
	public int getNumInstantiatingThreadsInvoked(){
		return this.numInstantiatingThreadsInvoked;
	}
	
	protected Socket getSocket() {
		return socketQueue.poll();
	}
	protected void addSocket(Socket soc) {
		socketQueue.add(soc);
	}
	
	public synchronized void setRequestHandlerRegistry(Map<String, HandlerAbstract> newRegistry){
		requestHandlerRegistry = new HashMap<String,HandlerAbstract>(newRegistry);
		requestHandlers = new HashMap<Class<? extends HandlerAbstract>,List<HandlerAbstract>>(); 
	}
	
	public synchronized int getInstancesToStage() {
		return numInstancesToStage;
	}

	public  synchronized void setInstancesToStage(int instancesToStage) {
		this.numInstancesToStage = instancesToStage;
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
				   key = RequestDispatcher.parseName(pair.substring(0, pos), sb);
				   value = RequestDispatcher.parseName(pair.substring(pos+1, pair.length()), sb);
			   }
			   if (ht.containsKey(key)) {
				   getLog().warn("http request had repeated keys");
			   }
			   
			   ht.put(key, value);
		   }
		   return ht;
	   }
	   
	   
	   
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

	public void run() {
		
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		
		int nBytes = -1;
		boolean error = false;
		List<String> errors = new ArrayList<String>();
		byte[] contentTypeHeader;
		byte[] outputBytes;
		String request = "";
		String requestParameters = "";
		String header = "";
		
		Socket localSocket = getSocket();
		
		try {
			incrementJobCounter();
			getLog().debug("----------------------");
			String source = localSocket.getInetAddress().toString();
			getLog().info("Request Handler #:"+jobCounter+" handling request from " + source);

			bis = new BufferedInputStream(localSocket.getInputStream());
			nBytes = bis.read(readBytes, 0, 5120);
			if(nBytes == -1){
				request = "";
				requestParameters = "";
				header = "";
			}
			else{
				while(nBytes != -1){
					request += new String(readBytes, 0, nBytes);
					if(bis.available() > 0){
						nBytes = bis.read(readBytes,0,5120);
					}
					else{
						nBytes = -1;
					}
				}
				requestParameters = new String(request);
				header = new String(request);
			}
				
			getLog().debug("First Part of Full Client Request = \n" + request);


			/* figure out the HTTP method */
			boolean getMethod = false;
			boolean postMethod = false;
			int indexGET = request.indexOf("GET");
			int indexPOST = request.indexOf("POST");
			int indexHTTP = -2;
			if(indexGET != -1){
				indexHTTP = request.indexOf("HTTP",indexGET);
				if(indexGET < indexHTTP){
					getMethod = true;
				}
			}
			else if(indexPOST != -1){
				indexHTTP = request.indexOf("HTTP",indexPOST);
				if(indexPOST < indexHTTP){
					postMethod = true;
				}
			}
				
			if(indexHTTP == -1){
				indexHTTP = request.length();
			}
				
				
			/* Capture the header */
			int start = header.indexOf("\n",indexHTTP);
			if(start == -1){
				start = header.indexOf("\r",indexHTTP);
			}
					
						
			/*If we didn't get the whole post, try for the second piece */
			if(start >=0 ){
				header = header.substring(start).trim();
			}
			if(header.length() == 0){
				nBytes = bis.read(readBytes, 0, 5120);
				if(nBytes == -1){
					header = "";
				}
				else{
					header = new String(readBytes, 0, nBytes);
				}
				if(header.length()==0){
					getLog().warn("No HTTP Headers from <"+source+"> url:<"+request+">");
					header="";
				}
			}
			header=header.trim();
			Map<String, String> headers = RequestDispatcher.parseHeaderString(header);
			//System.out.println("Testing for Header Parameters:\n"+header);

			HandlerAbstract handler = null;
			Map<String, String> parameters = null;
				
			/* Capture the parameters, which either start at a ? or if there is a space before the ? then there are no parameters*/
			/* Find the root of the request */
			start = request.indexOf("/");
			int end = -1;
			if(start != -1){
				int q = request.indexOf("?",start);
				int s = request.indexOf(" ",start);
				if((q != -1) && (s != -1)){
					if(q < s){
						end = q;
					}
					else{
						end = s;
					}
				}
				else{
					if( q == -1 ){
						end = s;
					}
					if( s == -1 ){
						end = q;
					}
				}
			}
			
			if((start >= 0) && (end >=0)){
				request = request.substring(start+1, end).trim();
				
				/*Grab the parameters */
				if(getMethod){
					getLog().debug("URL Client Request (GET)= "+ request);
					start = requestParameters.indexOf("?");
					end = requestParameters.indexOf(" ",start);
					if((start >=0 ) && (end >=0)){
						requestParameters = requestParameters.substring(start+1,end).trim();
					}
					else{
						getLog().warn("No HTTP (GET) parameters from <"+source+"> url:<"+request+">");
						requestParameters="";
					}
						
					requestParameters = requestParameters.trim();
					parameters = RequestDispatcher.parseQueryString(requestParameters);
						
				}
				else if(postMethod){
					getLog().debug("URL Client Request (POST)= "+ request);
					
					start = requestParameters.indexOf("\r\n\r\n");
					
					/*If we didn't get the whole post, try for the second piece */
					if(start >=0 ){
						requestParameters = requestParameters.substring(start+2).trim();
					}
					if(requestParameters.length() == 0){
						nBytes = bis.read(readBytes, 0, 5120);
						requestParameters = new String(readBytes, 0, nBytes);
						if(requestParameters.length()==0){
							getLog().warn("No HTTP (POST) parameters from <"+source+"> url:<"+request+">");
							requestParameters="";
						}
					}

					requestParameters = requestParameters.trim();
					parameters = RequestDispatcher.parseQueryString(requestParameters);
				}
				else{
					getLog().warn("Unhandled HTTP method from <"+source+"> url:<"+request+">");
					requestParameters="";
				}
				getLog().info("Request Handler #:"+(jobCounter)+" handling <"+request+"("+requestParameters+")>");
			
				if(parameters != null){
					getLog().debug("Parameters"+parameters.toString());
				}
					
				handler = getHandler(request);
					
			}
				
			if(handler != null){
				HTTPRequest httpRequestType;
				if(getMethod){
					httpRequestType = HTTPRequest.GET;
				}
				else if(postMethod){
					httpRequestType = HTTPRequest.POST;
				}
				else{
					httpRequestType = HTTPRequest.UNKNOWN;
				}
				Pair<byte[], byte[]> handle = handler.handle(localSocket.getInetAddress(),httpRequestType,headers,request,parameters); 
				if(handle != null){
					if(	(handle.getFirst() != HandlerAbstract.getContentTypeHeader_REDIRECT_UNSPECIFIED()) &&
							(handle.getFirst() != HandlerAbstract.getContentTypeHeader_PROXY())){
						contentTypeHeader = handle.getFirst();
						outputBytes = handle.getSecond();
						if((contentTypeHeader == null) || (outputBytes == null)){
							errors.add("Request Handler for "+request+" returned null response to these parameters "+parameters.toString());
							error = true;
						}
						
						if(error){
							contentTypeHeader = HandlerAbstract.getContentTypeHeader_JSON();
							JSONArray jsonArray = new JSONArray();
							jsonArray.addAll(errors);
							outputBytes=jsonArray.toString().getBytes();
						}
						//getLog().error("Checking what we are sending back:"+handle.getSecond().toString());
						send_OKPage(localSocket,getWebServer().getHTTPServerHeader(),contentTypeHeader,outputBytes);
					}
					else{
						String new_url = Arrays.toString(handle.getSecond());
						if(handle.getFirst() == HandlerAbstract.getContentTypeHeader_REDIRECT_UNSPECIFIED()){
							sendRedirect(new BufferedOutputStream(localSocket.getOutputStream()),HTTP_REDIRECT_UNSPECIFIED,new_url);
						}
						else{
							sendProxy(localSocket,getWebServer().getHTTPServerHeader(),new_url);
						}
					}
				}
				else{
					sendNotFound(new BufferedOutputStream(localSocket.getOutputStream()));
				}
			}
		} catch (IOException e) {
			getLog().error(e);
		} catch (RuntimeException e) {
			getLog().error("RuntimeException with this request:\n"+request+"\n"+e);
			e.printStackTrace();
		} finally {
			try {
				if(bis != null){
					bis.close();
				}
			}
			catch (Exception e) {
				getLog().error(e);
			}
			finally{
				try{
					if(bos != null){
						bos.close();
					}
				}
				catch (Exception e) {
					getLog().error(e);
				}
				finally{
					try{
						if(localSocket != null){
							localSocket.close();
						}
					}
					catch (Exception e) {
						getLog().error(e);
					}
				}
			}
		}
	}


	private void send_OKPage(Socket localSocket, String httpServerHeader, byte[] contentTypeHeader, byte[] outputBytes) {
		
		try {
			BufferedOutputStream bos = new BufferedOutputStream(localSocket.getOutputStream());
			bos.write(("HTTP/1.0 "+ HTTP_OK).getBytes());
			bos.write(EOL);
			bos.write(("Server: "+httpServerHeader+":").getBytes());
			bos.write(EOL);
			bos.write(("Date: "+ (new Date())).getBytes());
			bos.write(EOL);
			bos.write(contentTypeHeader);
			bos.write(EOL);
			bos.write(EOL);
			bos.write(outputBytes, 0, outputBytes.length);
			bos.flush();
		} catch (IOException e) {
			getLog().error(e);
		}
	}

	private void sendRedirect(BufferedOutputStream bos, String httpRedirectCode,String new_url) {
		try {
			bos.write(("HTTP/1.1 "+ httpRedirectCode).getBytes());
			bos.write(EOL);
			bos.write(("Location: "+new_url).getBytes());
			bos.flush();
		} catch (IOException e) {
			getLog().error(e);
		}
	}
	
	public void sendProxy(Socket localSocket, String httpServerHeader,String new_url) {
		try {
			BufferedOutputStream bos = new BufferedOutputStream(localSocket.getOutputStream());
			bos.write(("HTTP/1.0 "+ HTTP_OK).getBytes());
			bos.write(EOL);
			bos.write(("Server: "+ httpServerHeader +":").getBytes());
			bos.write(EOL);
			bos.write(("Date: "+ (new Date())).getBytes());
			bos.write(EOL);
			bos.write(HandlerAbstract.getContentTypeHeader_HTML());
			bos.write(EOL);
			bos.write(EOL);
			String responseString = WebUtil.fetchWebPage(new_url, false, null, 30 * 1000);
			bos.write(responseString.getBytes());
			bos.flush();
		} catch (IOException e) {
			getLog().error(e);
		}
	}

	private void sendNotFound(BufferedOutputStream bos) {
		try {
			bos.write(("HTTP/1.0 "+ HTTP_NOT_FOUND).getBytes());
			bos.write(EOL);
			bos.write(EOL);
			bos.flush();
		} catch (IOException e) {
			getLog().error(e);
		}
	}
	
}
