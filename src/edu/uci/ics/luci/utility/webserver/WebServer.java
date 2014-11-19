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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.input.channel.Input;
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
 * After starting, the server has 2 threads, one which takes connections from the input channel and spawns
 * threads to consume the incoming job.  The incoming job is just formatted into an Request in this thread.
 * 
 * The second thread is responsible for dispatching the jobs, once converted.  They are passed to a Request Dispatcher
 * which handles one or more jobs at a time.
 * 
 * This class should be shutdown using the {@link #setQuitting(boolean)} method.  This will perform an orderly shutdown
 * of the threads and channels.
 * @author djp3
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
	
	static public final int threadPoolSize = 10;
	
	private long startTime = System.currentTimeMillis();
	private long count = 0;

	Thread webServerThread = null;

	ExecutorService threadExecutor = null;
	
	private boolean quitting = false;

	private Input inputChannel = null;
	private RequestDispatcher requestDispatcher= null;
	private Future<?> requestDispatcherFuture = null;
	private AccessControl accessControl;
	
	Future<Void> eventGrabberFuture = null;
	Future<Void> eventDispatcherFuture = null;
	
	
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
		while((eventGrabberFuture != null) && !eventGrabberFuture.isDone()){
			getLog().debug("Waiting for webserver requests to complete");
			synchronized(semaphore){
				semaphore.notifyAll();
			}
			try {
				eventGrabberFuture.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
		}
		eventGrabberFuture = null;
		
		while((eventDispatcherFuture != null) && !eventDispatcherFuture.isDone()){
			getLog().debug("Waiting for webserver dispatches to complete");
			synchronized(semaphore){
				semaphore.notifyAll();
			}
			try {
				eventDispatcherFuture.get();
			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
		}
		eventDispatcherFuture = null;
		
		threadExecutor.shutdown();
		threadExecutor=null;
		
		getLog().info("WebServer shutdown");
	}
	
	public boolean isQuitting() {
		return quitting;
	}

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
		
		threadExecutor = Executors.newFixedThreadPool(threadPoolSize);
		
		this.accessControl = accessControl;
		this.accessControl.setDefaultFilename(null);
		
		if(Globals.getGlobals().isTesting()){
			/*Clear access controls for testing*/
			this.accessControl.setBadGuyTest(new ArrayList<String>());
		}
		
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
	
	
	/* Set up a queue to convert the incoming text into input channel requests */
	private Object semaphore = new Object();
	private List<Future<Pair<Request, Output>>> handlerQueue = Collections.synchronizedList(new ArrayList<Future<Pair<Request,Output>>>());
	
	/**
	 * This pulls connections off the InputChannel and starts jobs to convert the connections to work orders
	 * @author djp3
	 *
	 */
	private class MyEventGrabber implements Callable<Void>{
		
		@Override
		public Void call() throws Exception {
			getLog().info("Time:"+System.currentTimeMillis()+",Server is listening on port " + inputChannel.getPort());
			
			try{
				while(!isQuitting()){
					/* Wait for work to come in, this often times out and returns null */
					Callable<Pair<Request, Output>> handleMe = inputChannel.waitForIncomingRequest();
				
					/* Put in a conversion job to pull the input channel request off the channel */
					if(!isQuitting() && (handleMe != null)){
						handlerQueue.add(threadExecutor.submit(handleMe));
					}
					
					/* Let the Event Dispatcher know something is waiting*/
					synchronized(semaphore){
						if(handlerQueue.size() > 0){
							semaphore.notifyAll();
						}
					}
				}
			}
			finally{
				/* Let the Event Dispatcher also check for shutting down*/
				synchronized(semaphore){
					semaphore.notifyAll();
				}
			}
			return null;
		}
	}
	
	/**
	 * This takes works orders from the handlerQueue and dispatchers them
	 * @author djp3
	 *
	 */
	private class MyEventDispatcher implements Callable<Void>{

		@Override
		public Void call() throws Exception {
			while(!isQuitting()){
				/* Wait for conversion jobs to finish*/
				synchronized(semaphore){
					while(!isQuitting() && (handlerQueue.size() == 0 )){
						semaphore.wait();
					}
				}
				if(!isQuitting()){
					/* Check all the conversion jobs to see if they are done */
					for(Iterator<Future<Pair<Request, Output>>> i = handlerQueue.iterator(); i.hasNext();){
						Future<Pair<Request, Output>> f = i.next();
						/* If the conversion job is done */
						if(f.isDone()){
							/* Remove it from the conversion queue */
							i.remove();
							Pair<Request, Output> pair = null;
							try {
								pair = f.get();
					
								Request request = pair.getFirst();
				
								if(!isQuitting() && (request != null)){
									
									/* Check to make sure this connection source is allowed */
									String source = request.getSource();
									if(accessControl.allowSource(source, true, false)){
									
										/* Add the work order to the dispatchers work queue */
										requestDispatcher.addRequest(pair);
										count++;
						
										/* Only restart the dispatcher if it's not running. It stops running
										 * when it runs out of jobs.  There should be no harm running multiple threads of
										 * Dispatchers */
										if((requestDispatcherFuture == null) || (requestDispatcherFuture.isDone() && (requestDispatcher.numRequests() > 0))){
											requestDispatcherFuture  = threadExecutor.submit(requestDispatcher);
										}
									}
									else{
										getLog().warn("Server silently rejected request from " + source);
									}
								}
							} catch (InterruptedException e) {
								//Getting the future result failed
							} catch (ExecutionException e) {
								//Getting the future result failed
							}
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
				/* First set up a thread to pull things off the input channel and convert them to jobs*/
				MyEventGrabber eventGrabber = new MyEventGrabber();
				eventGrabberFuture = threadExecutor.submit(eventGrabber);
				
				/* Second set up a thread to take the incoming jobs and pass them to the RequestDispatcher*/
				/* If the requestDispatcher was smarter it could do this instead */
				MyEventDispatcher eventDispatcher = new MyEventDispatcher();
				eventDispatcherFuture = threadExecutor.submit(eventDispatcher);
				
			}
		} catch (RuntimeException e) {
			getLog().fatal(e.toString());
		}
	}


}
