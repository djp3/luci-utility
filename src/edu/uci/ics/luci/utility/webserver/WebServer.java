/*
	Copyright 2007-2015
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

import static org.junit.Assert.fail;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerError;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerShutdown;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerUnstableWrapper;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerVersion;
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
 * threads to consume the incoming job.  The incoming job is just formatted into an Request in this thread.
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
	ExecutorService threadExecutor = null;
	
	private long startTime = System.currentTimeMillis();
	private long count = 0;
	
	private boolean quitting = false;

	private Input inputChannel = null;
	private RequestDispatcher requestDispatcher= null;
	private AccessControl accessControl;
	
	/* Set up a queue to convert the incoming text into input channel requests */
	private LinkedBlockingQueue<Future<Pair<Request, Output>>> incomingQueue = new LinkedBlockingQueue<Future<Pair<Request,Output>>>();

	private Future<Void> incomingGrabberFuture;
	private Future<Void> middlewareProcessorFuture;
	private Future<?> requestDispatcherFuture;
	
	
	
	/*******************************************/
	/**  Code for shutting down **/
	
	/**
	 * Used internally to such down the blocking queues
	 * @author djp3
	 *
	 */
	private static class MyHandlerQueuePoisonPill implements Callable<Pair<Request,Output>>{
		@Override
		public Pair<Request, Output> call() throws Exception {
			return new Pair<Request,Output>(null,null);
		}
		
	}
	
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
		
		/* Shutdown the piece that gets formats connections from the outside world */
		if(incomingGrabberFuture != null){
			incomingGrabberFuture.cancel(false);
			while(!incomingGrabberFuture.isDone()){
				incomingGrabberFuture.cancel(true);
			}
			incomingGrabberFuture = null;
		}
		
		/* Shutdown the piece that takes formatted connections and gives them to the Request Dispatcher */
		if(incomingQueue != null){
			Future<Pair<Request, Output>> handlerQueuePoisonPill = threadExecutor.submit(new MyHandlerQueuePoisonPill());
			incomingQueue.add(handlerQueuePoisonPill);
		}
		if(middlewareProcessorFuture != null){
			middlewareProcessorFuture.cancel(false);
			while(!middlewareProcessorFuture.isDone()){
				middlewareProcessorFuture.cancel(true);
			}
			middlewareProcessorFuture = null;
		}
		incomingQueue= null;
		
		/* Shutdown the Request Dispatcher */
		if(requestDispatcher != null){
			requestDispatcher.setQuitting(q);
			Pair<Request, Output> requestDispatcherPoisonPill = new Pair<Request,Output>(null,null);
			requestDispatcher.addRequest(requestDispatcherPoisonPill);
			requestDispatcher = null;
		}
		
		if(requestDispatcherFuture != null){
			requestDispatcherFuture.cancel(false);
			while(!requestDispatcherFuture.isDone()){
				requestDispatcherFuture.cancel(true);
			}
		}
		
		if(threadExecutor != null){
			threadExecutor.shutdown();
			threadExecutor=null;
		}
		
		getLog().info("WebServer shutdown");
	}
	
	public boolean isQuitting() {
		return quitting;
	}
	
	/*******************************************/

	public long getLaunchTime() {
		return startTime;
	}
	
	public long getTotalRequests() {
		return count;
	}
	
	public Input getInputChannel(){
		return inputChannel;
	}
	
	public RequestDispatcher getRequestDispatcher(){
		return requestDispatcher;
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
	 * @param accessControl Are there any access restrictions
	 */
	public WebServer(Input inputChannel, RequestDispatcher requestDispatcher,AccessControl accessControl){
		
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
		
		threadExecutor = Executors.newCachedThreadPool();
		
		this.accessControl = accessControl;
		if(this.accessControl.getDefaultFilename() == null){
			throw new IllegalArgumentException("Initialize accessControl's defaultFilename before passing it to the web server");
			//this.accessControl.setDefaultFilename(null);
		}
		
		//if(Globals.getGlobals().isTesting()){
			/*Clear access controls for testing*/
			//this.accessControl.setBadGuyTest(new ArrayList<String>());
		//}
		
		setWebServerThread(new Thread(this));
		getWebServerThread().setName("WebServer:"+((Globals.getGlobals().isTesting())?"testing":"not testing"));
		getWebServerThread().setDaemon(false); /* Force a clean shutdown */
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
	private class MyIncomingGrabber implements Callable<Void>{
		
		@Override
		public Void call() throws Exception {
			getLog().info("Time:"+System.currentTimeMillis()+",Server is listening on port " + inputChannel.getPort());
			
			while(!isQuitting()){
				/* Wait for work to come in, this often times out and returns null */
				Callable<Pair<Request, Output>> handleMe = inputChannel.waitForIncomingRequest();
				
				/* Put in a conversion job to pull the input channel request off the channel */
				if(!isQuitting() && (handleMe != null)){
					incomingQueue.put(threadExecutor.submit(handleMe));
				}
			}
			return null;
		}
	}
	
	/**
	 * This takes works orders from the incomingQueue and dispatches them
	 * @author djp3
	 *
	 */
	private class MyMiddlewareProcessor implements Callable<Void>{

		@Override
		public Void call() throws Exception {
			
			while (!isQuitting()) {
				/* Check all the conversion jobs to see if they are done */
				getLog().info( "Waiting for a conversion job");
				
				/*Blocking wait for a job to come in */
				Future<Pair<Request, Output>> f = null;
				try{
					f = incomingQueue.take();
				}
				catch(InterruptedException e){
				}
				
				if(f != null){
					/* Make sure we didn't start quitting while we were blocking */
					if( (!f.isDone()) || isQuitting() ){
						/* If the conversion job is not done put it back */
						incomingQueue.put(f);
					}
					else{
						Pair<Request, Output> pair = null;
						try {
							pair = f.get();

							Request request = pair.getFirst();

							if (request != null) {
								/*
								 * Check to make sure this connection
								 * source is allowed
								 */
								String source = request.getSource();
								if (accessControl.allowSource(source, true, false)) {
									/*
									 * Add the work order to the
									 * dispatchers work queue
									 */
									requestDispatcher.addRequest(pair);
									count++;
								} else {
									getLog().warn("Server silently rejected request from " + source);
								}
							}
						} catch (InterruptedException e) {
							// Getting the future result failed
						} catch (ExecutionException e) {
							// Getting the future result failed
						}
					}
				}
			}
			return null;
		}
	}
	
	
	
	public void run(){
		try {
			if (!isQuitting()) {
				/*Set up a thread to dispatch incoming requests to the appropriate handler */
				requestDispatcherFuture = threadExecutor.submit(requestDispatcher);
				
				/* Then set up a thread to take the incoming jobs, check them and pass them to the RequestDispatcher*/
				middlewareProcessorFuture = threadExecutor.submit(new MyMiddlewareProcessor()); 
				
				/* Then set up a thread to pull things off the input channel and convert them to jobs*/
				incomingGrabberFuture = threadExecutor.submit(new MyIncomingGrabber());
			}
		} catch (RuntimeException e) {
			getLog().fatal(e.toString());
		}
	}
	
	
	
	/*************************************************************/
	/* main() is here to test incoming connections.  It is just for measuring performance from WebServerLoadGenerator */
	
	private static class GlobalsDummy extends Globals{
		
		private static transient volatile Logger log = null;
		public static Logger getLog(){
			if(log == null){
				log = LogManager.getLogger(GlobalsDummy.class);
			}
			return log;
		}

		@Override
		public String getSystemVersion() {
			return "1.0";
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
			HashMap<String, HandlerAbstract> requestHandlerRegistry = new HashMap<String,HandlerAbstract>();
			
			// Null is a default Handler
			requestHandlerRegistry.put(null,new HandlerError(Globals.getGlobals().getSystemVersion()));
			requestHandlerRegistry.put("/version",new HandlerVersion(Globals.getGlobals().getSystemVersion()));
			requestHandlerRegistry.put("/fail",new HandlerUnstableWrapper(0.5d,0,new HandlerVersion(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/latent",new HandlerUnstableWrapper(0.0d,100,new HandlerVersion(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/unstable",new HandlerUnstableWrapper(0.5d,100,new HandlerVersion(Globals.getGlobals().getSystemVersion())));
			requestHandlerRegistry.put("/shutdown",new HandlerShutdown(Globals.getGlobals()));
				
			RequestDispatcher requestDispatcher = new RequestDispatcher(requestHandlerRegistry);
			AccessControl accessControl = new AccessControl();
			accessControl.setDefaultFilename("test/access_control_list_for_testing.properties");
			
			ws = new WebServer(inputChannel, requestDispatcher, accessControl);
			
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
