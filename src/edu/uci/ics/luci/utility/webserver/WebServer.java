/*
	Copyright 2007-2014
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

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.CalendarCache;
import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.Quittable;

/**
 * 
 * This Web server class listens for any incoming client request at a specified
 * port. Every incoming request spawns a new thread to process it.
 * 
 * Usage: java MyWebServer [port] where port is any unused port in the system
 * value between 1024 and 65535. Ports less than 1024 needs admin access. if
 * port is 0 or not specified, then any unused port is used
 * 
 */

public class WebServer implements Runnable,Quittable{
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(WebServer.class);
		}
		return log;
	}
	
	static public final int DEFAULT_PORT = 443;
	static public final int threadPoolSize = 100;
	
	static final int databaseConnectionPoolSize = threadPoolSize;
	
	static private CalendarCache calendarCache = new CalendarCache(CalendarCache.TZ_GMT);
	
	private long startTime = System.currentTimeMillis();
	private long count = 0;

	Thread webServer = null;

	ExecutorService threadExecutor = null;
	
	private boolean quitting = false;

	private InputChannel inputChannel = null;
	private RequestDispatcher requestDispatcher= null;
	private AccessControl accessControl;
	
	private ServerSocket serverSoc = null;
	private String HTTP_SERVER_HEADER;

	
	
	
	public void setQuitting(boolean q){
		getLog().debug("Setting webserver quitting to :"+q);
		quitting = q;
		if(quitting){
			while(getWebServer().isAlive()){
				try {
					getLog().debug("Waiting for webserver requests to complete");
					getWebServer().join();
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public boolean isQuitting() {
		return quitting;
	}

	public static CalendarCache getCalendarCache() {
		return calendarCache;
	}

	public static void setCalendarCache(CalendarCache calendarCache) {
		WebServer.calendarCache = calendarCache;
	}

	public long getLaunchTime() {
		return startTime;
	}
	
	public long getTotalRequests() {
		return count;
	}
	
	public InputChannel getInputChannel(){
		return inputChannel;
	}
	
	public RequestDispatcher getRequestDispatcher(){
		return requestDispatcher;
	}
	
	
	public String getHTTPServerHeader() {
		return HTTP_SERVER_HEADER;
	}
	
	public void setHTTPServerHeader(String http) {
		HTTP_SERVER_HEADER = http;
	}
	
	
	public Thread getWebServer() {
		return webServer;
	}

	private void setWebServer(Thread webServer) {
		this.webServer = webServer;
	}

	public WebServer(InputChannel inputChannel, RequestDispatcher requestDispatcher,AccessControl accessControl){
		if(inputChannel == null){
			throw new InvalidParameterException("The Input Channel can't be null");
		}
		if(requestDispatcher == null){
			throw new InvalidParameterException("The Request Dispatcher can't be null");
		}
		if(accessControl == null){
			throw new InvalidParameterException("The Access Control can't be null");
		}
		this.inputChannel = inputChannel;
		this.requestDispatcher = requestDispatcher;
		this.requestDispatcher.setWebServer(this);
		
		threadExecutor = Executors.newFixedThreadPool(threadPoolSize);
		
		this.accessControl = accessControl;
		this.accessControl.setDefaultFilename(null);
		
		if(Globals.getGlobals().isTesting()){
			/*Clear access controls for testing*/
			this.accessControl.setBadGuyTest(new ArrayList<String>());
		}
		
		serverSoc = inputChannel.getServerSocket();
		
		setWebServer(new Thread(this));
		getWebServer().setName("WebServer:"+((Globals.getGlobals().isTesting())?"testing":"not testing"));
		getWebServer().setDaemon(false); /* Force a clean shutdown */
	}
	
	public void start(){
		start(1000);
	}
	
	
	public void start(long wait){
		getWebServer().start();
		
		if(Globals.getGlobals().isTesting()){
			getLog().info("Sleeping "+(wait/1000.0)+" seconds so everything can stabilize for testing");
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
			}
			getLog().info("Done Sleeping");
		}
	}
	
	
	public void run(){
		/* Set up an infinite loop to field requests */
		
		
		try {
			/*Counting requests for stats */
			count = 0;
		
			
			getLog().info("Time:"+System.currentTimeMillis()+",Server is listening on port " + serverSoc.getLocalPort());
			
			while (!quitting) {
				/* Blocks until connection arrives */
				Socket soc;
				try{
					soc = serverSoc.accept();
				}
				catch(java.net.SocketTimeoutException e){
					soc = null;
				}
				
				if(!quitting && (soc != null) ){
					String source = soc.getInetAddress().toString();
					if(accessControl.allowSource(source, true, false)){
						/* When we get a connection handle it and wait for the next one */
						requestDispatcher.addSocket(soc);
						threadExecutor.execute(requestDispatcher);
						count++;
					}
					else{
						getLog().warn("Server silently rejected request from " + source);
					}
				}
			}
		} catch (BindException e) {
			getLog().fatal(e.toString());
		} catch (RuntimeException e) {
			getLog().fatal(e.toString());
		} catch (IOException e) {
			getLog().fatal(e.toString());
		} finally {
			try {
				serverSoc.close();
				serverSoc=null;
			} catch (Exception e) {
				getLog().error(e.toString());
			}
			finally{
				try{
					threadExecutor.shutdown();
					threadExecutor=null;
				} catch (Exception e) {
					getLog().error(e.toString());
				}
			}
		}
		getLog().info("WebServer shutdown");
	}


}
