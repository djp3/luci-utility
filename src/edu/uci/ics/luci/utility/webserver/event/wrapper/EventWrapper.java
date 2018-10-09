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


package edu.uci.ics.luci.utility.webserver.event.wrapper;

import java.util.ArrayList;
import java.util.List;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.EventVoid;
import edu.uci.ics.luci.utility.webserver.event.resultlistener.EventResultListener;

/**
 * This is the wrapper for all the events that the disruptor can handle.  
 * Subclasses include internal events and also user defined server processing
 * events.  
 * @author djp3
 * 
 */
public class EventWrapper {

	/* The basic data encapsulated by the wrapper */
	private long timestamp; // Official time of the event
	private Event event;
	private List<EventResultListener> resultListeners;


	/* Getters and Setters */
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event){
		this.event = event;
	}
	

	public List<EventResultListener> getResultListeners() {
		return this.resultListeners;
	}

	public void addResultListener(EventResultListener rl) {
		if(rl != null){
			List<EventResultListener> resultListeners = getResultListeners();
			if(resultListeners == null){
				resultListeners = new ArrayList<EventResultListener>();
			}
			resultListeners.add(rl);
			setResultListeners(resultListeners);
		}
	}


	public void setResultListeners(List<EventResultListener> rl) {
		this.resultListeners = rl;
	}
	

	/**
	 * This is only helpful for testing
	 * 
	 */

	
	public EventWrapper() {
		this(System.currentTimeMillis(), new EventVoid(), null, null);
	}
	
	public EventWrapper(Event event){
		this(System.currentTimeMillis(), event, null, null);
	}
	
	public EventWrapper(Event event, EventResultListener resultListener) {
		this(System.currentTimeMillis(), event, resultListener, null);
	}
	
	public EventWrapper(Event event, List<EventResultListener> resultListeners) {
		this(System.currentTimeMillis(), event,  null, resultListeners);
	}

	public EventWrapper(long eventTime, Event event, List<EventResultListener> resultListeners) {
		this(eventTime,event, null, resultListeners);
	}
	
	
	public EventWrapper(long eventTime, Event  event, EventResultListener listener) {
		this(eventTime,event, listener,null);
	}
	
	private EventWrapper(long eventTime, Event event, EventResultListener listener, List<EventResultListener> resultListeners) {

		this.setTimestamp(eventTime);

		this.setEvent(event);
		
		if(listener == null){
			if (resultListeners == null) {
				this.setResultListeners(new ArrayList<EventResultListener>());
			} else {
				this.setResultListeners(resultListeners);
			}
		}
		else{
			if (resultListeners == null) {
				List<EventResultListener> rl = new ArrayList<EventResultListener>();
				rl.add(listener);
				this.setResultListeners(rl);
			} else {
				resultListeners.add(listener);
				this.setResultListeners(resultListeners);
			}
		}
	}

	public void set(EventWrapper eventWrapper) {
		this.setTimestamp(eventWrapper.getTimestamp());
		this.setEvent(eventWrapper.getEvent());
		this.getResultListeners().clear();
		this.getResultListeners().addAll(eventWrapper.getResultListeners());
	}


}
