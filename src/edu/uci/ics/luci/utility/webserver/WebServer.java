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

import static org.junit.Assert.fail;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.event.Event_MiddleWare;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Error;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Shutdown;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_UnstableWrapper;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Version;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapper;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperFactory;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperHandler;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperQueuer;
import edu.uci.ics.luci.utility.webserver.input.channel.Input;
import edu.uci.ics.luci.utility.webserver.input.channel.socket.HTTPInputOverSocket;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;

/**
 * This Web server class is designed to be instantiated then explicitly started with the
 * {@link #start()} or {@link #start(long)} methods.
 * 
 * The structure is designed to be multi-threaded and to separate out the way in which a request comes 
 * in from the handling of the request.
 * 
 * Input does not necessarily have to come in from a standard socket.  It can also come in from other kinds of
 * networks, like a p2p network which is why it was refactored as it is.
 * 
 * After starting, the server has 3 threads, one which takes connections from the input channel and spawns
 * threads to consume the incoming job.  The incoming job is just formatted into a Request in this thread.
 * 
 * The second thread is responsible for dispatching the jobs, once converted.  They are passed to a Request Dispatcher
 * which handles one or more jobs at a time.
 * 
 * The third thread is the Request Dispatcher
 * 
 * This class should be shutdown using the {@link #setQuitting(boolean)} method.  This will perform an orderly shutdown
 * of the threads and channels.
 * @author djp3
 *
 */
public class WebServer implements Runnable,Quittable{
	
	
	/*******************************************/
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(WebServer.class);
		}
		return log;
	}
	/*******************************************/
	
	Thread webServerThread = null;
	private ExecutorService threadExecutor = null;
	
	private long startTime = System.currentTimeMillis();
	private long count = 0;
	
	private boolean quitting = false;

	private Input inputChannel = null;
	private AccessControl accessControl;
	
	private EventWrapperQueuer eventPublisher;
	private Map<String, APIEvent> aPIRegistry;
	
	
	
	/*******************************************/
	/**  Code for shutting down **/
	
	
	/**
	 * Shutdown the webserver.  Only needs to be called once with @param q = true.
	 * Ignores attempts to unquit by setting to false once it has been set to true. 
	 */
	public void setQuitting(boolean q){
		if(quitting && q){
			getLog().debug("Ignoring attempt to set webserver quitting to:"+q+" from: "+quitting);
			return;
		}
		if(!quitting && !q){
			getLog().debug("Ignoring attempt to set webserver quitting to:"+q+" from: "+quitting);
			return;
		}
		if(quitting && !q){
			getLog().debug("Ignoring attempt to set webserver quitting to:"+q+" from: "+quitting);
			return;
		}
		quitting = q;
		
		if(threadExecutor != null){
			threadExecutor.shutdown();
		}
		
		if(eventPublisher != null){
			eventPublisher.setQuitting(true);
		}
		
		getLog().info("WebServer shutdown");
	}
	
	public boolean isQuitting() {
		return quitting;
	}
	
	public boolean isTerminated(){
		return(threadExecutor.isTerminated());
	}
	
	/*******************************************/

	public long getLaunchTime() {
		return startTime;
	}
	
	public long getTotalRequests() {
		return count;
	}
	
	public void incrementTotalRequests() {
		count++;
	}
	
	
	public Input getInputChannel(){
		return inputChannel;
	}
	
	public Input setInputChannel(Input inputChannel){
		return this.inputChannel = inputChannel;
	}
	
	
	private AccessControl getAccessControl() {
		return accessControl;
	}

	private void setAccessControl(AccessControl accessControl) {
		this.accessControl = accessControl;
	}
	
	public Map<String, APIEvent> getAPIRegistry() {
		return aPIRegistry;
	}

	public void setAPIRegistry(Map<String, APIEvent> aPIRegistry) {
		this.aPIRegistry = aPIRegistry;
	}
	
	public void updateAPIRegistry(String restCommand, APIEvent webevent){
		if(aPIRegistry == null){
			throw new IllegalStateException("APIRegistry has not been initialized");
		}
		aPIRegistry.put(restCommand, webevent);
	}

	public ExecutorService getThreadExecutor() {
		return threadExecutor;
	}

	public void setThreadExecutor(ExecutorService threadExecutor) {
		this.threadExecutor = threadExecutor;
	}
	
	public EventWrapperQueuer getEventPublisher() {
		return eventPublisher;
	}

	public void setEventPublisher(EventWrapperQueuer eventPublisher) {
		this.eventPublisher = eventPublisher;
	}


	public Thread getWebServerThread() {
		return webServerThread;
	}

	private void setWebServerThread(Thread webServer) {
		this.webServerThread = webServer;
	}
	

	/**
	 * 
	 * @param inputChannel  Where are the REST commands coming from?
	 * @param requestDispatcher What is going to handle them?
	 * @param accessControl Are there any access restrictions?
	 */
	public WebServer(Input inputChannel, Map<String, APIEvent> requestHandlerRegistry,AccessControl accessControl){
		
		if(inputChannel == null){
			throw new InvalidParameterException("The Input Channel can't be null");
		}
		if(requestHandlerRegistry == null){
			throw new InvalidParameterException("The Request Handler Registry can't be null");
		}
		if(accessControl == null){
			throw new InvalidParameterException("The Access Control can't be null");
		}
		
		setInputChannel(inputChannel);
		
		setAPIRegistry(requestHandlerRegistry);
		
		setThreadExecutor(Executors.newCachedThreadPool());
		
		this.setAccessControl(accessControl);
		if(this.accessControl.getDefaultFilename() == null){
			throw new IllegalArgumentException("Initialize accessControl's defaultFilename before passing it to the web server");
			//this.accessControl.setDefaultFilename(null);
		}
		
		setWebServerThread(new Thread(this));
		getWebServerThread().setName("WebServer:"+((Globals.getGlobals().isTesting())?"testing":"not testing"));
		getWebServerThread().setDaemon(false); /* Force a clean shutdown */
	}
	
	
	
	
	/**
	 * Create Event Disruptor Queue
	 * @param shouldLog set to true if log4j system should be used to output events
	 * @return 
	 */
	public EventWrapperQueuer createEventQueue() {
	    // The factory for the event
	    EventWrapperFactory factory = new EventWrapperFactory();
	
	    // Specify the size of the ring buffer, must be power of 2.
	    int bufferSize = 1024;
	
	    // Construct the Disruptor
	    Disruptor<EventWrapper> disruptor = new Disruptor<EventWrapper>(factory, bufferSize, Executors.defaultThreadFactory());
	
	    // Connect the handler
	    disruptor.handleEventsWith(new EventWrapperHandler(getThreadExecutor()));
	        
	    // Start the Disruptor, starts all threads running
	    disruptor.start();
	
	    // Get the ring buffer from the Disruptor to be used for publishing.
	    RingBuffer<EventWrapper> ringBuffer = disruptor.getRingBuffer();
	
	    EventWrapperQueuer localEventPublisher = new EventWrapperQueuer(disruptor,ringBuffer);
	    
	    return(localEventPublisher);
	}
	
	
	
	
	/**
	 * The default start method waits 1 second in order for the Threads to launch and stabilize
	 */
	public void start(){
		if(Globals.getGlobals().isTesting()){
			start(1000);
		}
		else{
			start(0);
		}
	}
	
	
	/**
	 * 
	 * @param wait, After launching the webserver thread wait this many milliseconds before returning
	 */
	public void start(long wait){
		getWebServerThread().start();
		
		if(wait > 0){
			getLog().info("Pausing "+(wait/1000.0)+" seconds after WebServer start");
			long start = System.currentTimeMillis();
			while((System.currentTimeMillis()-start) < wait){
				try {
					Thread.sleep(wait - (System.currentTimeMillis()-start));
				} catch (InterruptedException e) {
				}
			}
			getLog().info("Done Pausing");
		}
	}
	
	
	/**********************  Thread constructs  *************/
	
	
	
	/**
	 * This pulls connections off the InputChannel as fast as possible and 
	 * starts jobs to convert the connections to work orders.
	 * @author djp3
	 *
	 */
	private static class MyIncomingGrabber implements Callable<Void>{
		
		public static final String ERROR_NULL_PARENT = "Parent webserver can't be null";
		
		/*******************************************/
		private static transient volatile Logger log = null;
		public static Logger getLog(){
			if(log == null){
				log = LogManager.getLogger(MyIncomingGrabber.class);
			}
			return log;
		}
		/*******************************************/
		
		private WebServer webserver = null;

		public MyIncomingGrabber(WebServer parent) {
			if(parent == null){
				throw new IllegalArgumentException(ERROR_NULL_PARENT);
			}
			this.webserver = parent;
		}

		@Override
		public Void call() throws Exception {
			try{
				getLog().info("Time:"+System.currentTimeMillis()+",Server is listening on port " + webserver.getInputChannel().getPort());
			
				while(!webserver.isQuitting()){
					/* Wait for work to come in, this often times out and returns null */
					Callable<Pair<Request, Output>> incoming = webserver.getInputChannel().waitForIncomingRequest();
				
					/* Put in a conversion job to pull the input channel request off the channel */
					if(!webserver.isQuitting() && (incoming != null)){
						Event_MiddleWare event = new Event_MiddleWare(webserver,webserver.getThreadExecutor().submit(incoming),webserver.getAccessControl());
						EventWrapper eventWrapper = new EventWrapper(event);
						webserver.incrementTotalRequests();
						webserver.getEventPublisher().onData(eventWrapper);
					}
				}
			}catch(Exception e){
				getLog().error(e);
				e.printStackTrace();
				throw e;
			}
			return null;
		}
	}
	
	public void run(){
		
		setEventPublisher(createEventQueue());
		
		try {
			if (!isQuitting()) {
				/* This won't return until the server is completely quitting */
				Future<Void> result = getThreadExecutor().submit(new MyIncomingGrabber(this));
				try {
					result.get();
				} catch (InterruptedException e) {
					getLog().error("The webserver was interrupted unexpectedly");
				} catch (ExecutionException e) {
					getLog().error("The webserver was interrupted unexpectedly");
				}
			}
		} catch (RuntimeException e) {
			getLog().fatal(e.toString());
		}
	}
	
	
	
	/*************************************************************/
	/* main() is here to test incoming connections.  It is just for measuring performance from WebServerLoadGenerator */
	
	private static class GlobalsDummy extends Globals{
		
		/*******************************************/
		private static transient volatile Logger log = null;
		public static Logger getLog(){
			if(log == null){
				log = LogManager.getLogger(GlobalsDummy.class);
			}
			return log;
		}
		/*******************************************/

		@Override
		public String getSystemVersion() {
			return "1.0";
		}
		
		@Override
		public String getLog4JPropertyFileName() {
			return "luci-utility.log4j.xml";
		}
		
	}
	
	private static WebServer ws = null;	
	
	public static void main(String[] args) {
		
		Globals.setGlobals(new GlobalsDummy());
		Globals.getGlobals().setTesting(false);
		
		int port = 9020;
		boolean secure = false;
		
		try {
			HTTPInputOverSocket inputChannel = new HTTPInputOverSocket(port,secure);
			HashMap<String, APIEvent> requestHandlerRegistry = new HashMap<String,APIEvent>();
			
			// Null is a default Handler
			requestHandlerRegistry.put(null,new APIEvent_Error(Globals.getGlobals().getSystemVersion()));
			requestHandlerRegistry.put("/version",new APIEvent_Version(Globals.getGlobals().getSystemVersion()));
			requestHandlerRegistry.put("/fail",new APIEvent_UnstableWrapper(0.5d,0,new APIEvent_Version(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/latent",new APIEvent_UnstableWrapper(0.0d,100,new APIEvent_Version(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/unstable",new APIEvent_UnstableWrapper(0.5d,100,new APIEvent_Version(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/shutdown",new APIEvent_Shutdown(Globals.getGlobals()));
				
			AccessControl accessControl = new AccessControl();
			accessControl.setDefaultFilename("test/access_control_list_for_testing.properties");
			
			ws = new WebServer(inputChannel, requestHandlerRegistry, accessControl);
			
			Globals.getGlobals().addQuittable(ws);
			
			ws.start();
			
		} catch (RuntimeException e) {
			fail("Couldn't start webserver"+e);
		}
		
		synchronized(ws){
			while(!Globals.getGlobals().isQuitting()){
				try {
					ws.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
