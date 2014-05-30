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
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.X509KeyManager;

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
	private Date startDate = new Date();
	private long count = 0;

	private int port = DEFAULT_PORT;
	private Boolean secure = null;
	Thread webServer = null;

	ExecutorService threadExecutor = null;
	
	private boolean quitting = false;

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
	
	public String getLaunchDate(){
		return startDate.toString();
	}

	public long getTotalRequests() {
		return count;
	}
	
	
	public Boolean getSecure() {
		return secure;
	}

	public void setSecure(Boolean secure) {
		this.secure = secure;
	}
	
	public String getHTTPServerHeader() {
		return HTTP_SERVER_HEADER;
	}
	
	public void setHTTPServerHeader(String http) {
		HTTP_SERVER_HEADER = http;
	}
	
	public int getPort(){
		return this.port;
	}
	
	public Thread getWebServer() {
		return webServer;
	}

	private void setWebServer(Thread webServer) {
		this.webServer = webServer;
	}

	public WebServer(RequestDispatcher requestDispatcher,int port,boolean secure,AccessControl accessControl){
		if(requestDispatcher == null){
			throw new InvalidParameterException("The Request Dispatcher can't be null");
		}
		if(accessControl == null){
			throw new InvalidParameterException("The Access Control can't be null");
		}
		
		this.requestDispatcher = requestDispatcher;
		this.requestDispatcher.setWebServer(this);
		
		this.port = port;
		
		threadExecutor = Executors.newFixedThreadPool(threadPoolSize);
		
		this.accessControl = accessControl;
		this.accessControl.setDefaultFilename(null);
		
		if(Globals.getGlobals().isTesting()){
			/*Clear access controls for testing*/
			this.accessControl.setBadGuyTest(new ArrayList<String>());
		}
		
		this.setSecure(secure);
		try{
			if(getSecure()){
				SSLContext sctx1 = null;
				try{
					//to make the keystore example
					// keytool -keysize 2048 -genkey -alias swayr.com -keyalg RSA -keystore ./mySrvKeystore -validity 3650
					// keytool -import -alias cross -keystore ./mySrvKeystore  -trustcacerts -file ./temp/gd_cross_intermediate.crt 
					// keytool -import -alias intermed -keystore ./mySrvKeystore -trustcacerts -file ./temp/gd_intermediate.crt 
	
					// keytool -export -alias swayr.com -keystore ./mySrvKeystore -rfc -file server.cer
					// keytool -import -alias swayr.com -file server.cer -keystore ./myClientKeystore
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
