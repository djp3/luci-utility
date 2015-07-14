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

package edu.uci.ics.luci.utility.webserver.event;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.result.EventResult;
import edu.uci.ics.luci.utility.webserver.event.resultlistener.api.APIEventResultListener;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapper;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperQueuer;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;


public class Event_Dispatch extends Event{

	/*******************************************/
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Event_Dispatch.class);
		}
		return log;
	}
	/*******************************************/
    private static volatile int dispatchCounter = 0;
    
	
	public static void setDispatchCounter(int jobCounter) {
		Event_Dispatch.dispatchCounter = jobCounter;
	}
	
	private static void incrementDispatchCounter() {
		dispatchCounter++;
	}
	
	public static int getDispatchCounter() {
		return dispatchCounter;
	}
	
	/*******************************************/
	
	private EventWrapperQueuer eventPublisher = null;
	private Map<String, APIEvent> aPIRegistry = null;
	private Request request = null;
	private Output output = null;
    
	public void setEventPublisher(EventWrapperQueuer newEventPublisher){
		if(newEventPublisher == null){
			throw new IllegalArgumentException("Event Publisher can't be null");
		}
		this.eventPublisher = newEventPublisher;
	}
	
	public EventWrapperQueuer getEventPublisher(){
		return eventPublisher;
	}
    
	
	private void setAPIRegistry(Map<String,APIEvent> newRegistry){
		
		if(newRegistry == null){
			throw new IllegalArgumentException("API Registry can't be null");
		}
		
		if(aPIRegistry == null){
			aPIRegistry = Collections.synchronizedMap(new HashMap<String,APIEvent>(newRegistry));
		}
		else{
			throw new IllegalStateException("APIRegistry is already initialized");
		}	
	}
	
	public Map<String, APIEvent> getAPIRegistry(){
		return aPIRegistry;
	}
	
	
	

	public Request getRequest() {
		return request;
	}
	
	protected void setRequest(Request request) {
		this.request = request;
	}

	public Output getOutput() {
		return output;
	}

	protected void setOutput(Output output) {
		this.output = output;
	}

	@Override
	public void set(Event _incoming) {
		Event_Dispatch incoming = null;
		if(_incoming instanceof Event_Dispatch){
			incoming = (Event_Dispatch) _incoming;
			this.setEventPublisher(incoming.getEventPublisher());
			this.setAPIRegistry(incoming.getAPIRegistry());
			this.setRequest(incoming.getRequest());
			this.setOutput(incoming.getOutput());
		}
		else{
			throw new InvalidParameterException(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
		}
	}
	
	
	public Event_Dispatch(Map<String, APIEvent> aPIRegistry, EventWrapperQueuer eventPublisher, Request r, Output o) {
		super();
		
		if(aPIRegistry == null){
			throw new IllegalArgumentException("API Registry can't be null");
		}
		this.setAPIRegistry(aPIRegistry);
		
		if(eventPublisher == null){
			throw new IllegalArgumentException("Event Publisher can't be null");
		}
		this.setEventPublisher(eventPublisher);
		
		
		if(r == null){
			throw new IllegalArgumentException("Request can't be null");
		}
		this.setRequest(r);
		
		if(o == null){
			throw new IllegalArgumentException("Output can't be null");
		}
		this.setOutput(o);
	}
	
	/*
	public static byte[] getContentTypeHeader_REDIRECT_UNSPECIFIED() {
		return "redirect ".getBytes();
	}

	public static byte[] getContentTypeHeader_PROXY(){
		return "proxy ".getBytes();
	}*/
	


	protected static String wrapCallback(Map<String, Set<String>> parameters, String string) {
		if(parameters != null){
			Set<String> callback = parameters.get("callback");
			if(callback != null){
				for(String s:callback){
					return s+"("+string+")";
				}
				return "unspecifiedCallback("+string+")";
			}
			else{
				return string;
			}
		}
		else{
			return string;
		}
	}
	
	/** From : http://stackoverflow.com/a/5445161
	 * 
	 * @param is
	 * @return
	 */
	protected static String convertStreamToString(java.io.InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	
	
	public APIEvent getEvent(String restFunction){
		
		/* Get the template that the restFunction maps to */
		APIEvent event = getAPIRegistry().get(restFunction);
		
		/* If the class isn't present then grab the default template */
		if(event == null){
			event = getAPIRegistry().get(null);
		}
		
		/* If we don't have a class there is nothing to instantiate :( */
		if(event == null){
			return null;
		}
		
		return ((APIEvent) (event.clone()));
	}
	
	
	@Override
	public EventResult onEvent() {
		
		if((getRequest() != null) && (getOutput() != null)){  //Poison Pill would have this for example
			incrementDispatchCounter();
				
			getLog().info(dispatchCounter+": request.getCommand():"+getRequest().getCommand());
				
			APIEvent apiEvent = getEvent(getRequest().getCommand());
			apiEvent.setRequest(getRequest());
			apiEvent.setOutput(getOutput());
			
			APIEventResultListener aPIEventResultListener = new APIEventResultListener();
			
			EventWrapper eventWrapper = new EventWrapper(apiEvent,aPIEventResultListener);
			
			getEventPublisher().onData(eventWrapper);
		}
		else{
			/* Really strange situation */
			if(output != null){
				output.closeChannel();
			}
		}
		return new EventResult();
	}

	

}
