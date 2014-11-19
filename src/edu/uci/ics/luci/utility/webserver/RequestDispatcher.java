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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minidev.json.JSONArray;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.handlers.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;


public class RequestDispatcher implements Runnable{
	
	/*
	private static final byte[] EOL = {(byte)'\r', (byte)'\n' };
    protected static final String HTTP_OK = "200 OK";
    protected static final String HTTP_REDIRECT_UNSPECIFIED = "302 Found";
    protected static final String HTTP_NOT_FOUND = "404 Not Found";
    */
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(RequestDispatcher.class);
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
	
	
	private WebServer webServer = null;
	private Queue<Pair<Request,Output>> requestQueue = null;
	private Map<String, HandlerAbstract> requestHandlerRegistry = null;
	private Map<Class<? extends HandlerAbstract>,List<HandlerAbstract>> requestHandlers = null;
	private int numInstancesToStage = 10;
	

	private int numLiveInstantiatingThreads;
	private int numInstantiatingThreadsInvoked;
	
	public RequestDispatcher(Map<String,HandlerAbstract> requestHandlerRegistry){
		super();
		setRequestHandlerRegistry(requestHandlerRegistry);
		numLiveInstantiatingThreads = 0;
		numInstantiatingThreadsInvoked = 0;
		requestQueue = new ConcurrentLinkedQueue<Pair<Request,Output>>();
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
	
	public synchronized void updateRequestHandlerRegistry(String restCommand, HandlerAbstract handler){
		this.requestHandlerRegistry.put(restCommand, handler);
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
	
	protected Pair<Request, Output> getRequest() {
		return requestQueue.poll();
	}
	protected void addRequest(Pair<Request, Output> request) {
		requestQueue.add(request);
	}
	
	public int numRequests(){
		return requestQueue.size();
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

	   
	   
	  
	   
	public void run() {
		Pair<Request, Output> pair = null;
		Request request = null;
		Output output = null;
		boolean error;
		List<String> errors = null;
		
		/* Go through the queue until it is empty */
		while((pair = getRequest()) != null){
			try{
				request = pair.getFirst();
				output = pair.getSecond();
				
				error = false;
				errors = new ArrayList<String>();
				
				incrementJobCounter();
				
				HandlerAbstract handler = getHandler(request.getCommand());
				
				Response response = handler.handle(request,output);
			
				if(response != null){
					if( response.getStatus() == Response.Status.OK){
						if(response.getBody() == null){
							errors.add("Request Handler returned null response to this request\n"+request.toString());
							error = true;
						}
						
						if(error){
							response.setDataType(Response.DataType.JSON);
							
							JSONArray jsonArray = new JSONArray();
							jsonArray.addAll(errors);
							response.setBody(jsonArray.toString());
						}
						output.send_OK(response);
					}
					else{
						if(response.getStatus() == Response.Status.REDIRECT){
							output.send_Redirect(response);
						}
						else{
							output.send_Proxy(response);
						}
					}
				}
				else{
					output.send_NotFound();
				}
			} catch (RuntimeException e) {
				getLog().error("RuntimeException with this request:\n"+request+"\n"+e);
				e.printStackTrace();
			} finally {
				try{
					if(output != null){
						if(output.getSocket() != null){
							output.getSocket().close();
						}
					}
				}
				catch (Exception e) {
					getLog().error(e);
				}
			}	
		}
	}

	
}
