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

package edu.uci.ics.luci.utility.webserver.event;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.AccessControl;
import edu.uci.ics.luci.utility.webserver.WebServer;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.result.EventResult;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapper;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperQueuer;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;

public class Event_MiddleWare extends Event{
	
	/*******************************************/
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Event_MiddleWare.class);
		}
		return log;
	}
	/*******************************************/

	private static final Random r = new Random(System.currentTimeMillis());
	private static final int MAX_BACKOFF = 60 * 1000; // 1 minute
	private EventWrapperQueuer eventPublisher = null;
	private Future<Pair<Request, Output>> future;
	private AccessControl accessControl;
	private Quittable shutdownChecker;
	private Map<String, APIEvent> aPIRegistry;

	public EventWrapperQueuer getEventPublisher() {
		return eventPublisher;
	}

	private void setEventPublisher(EventWrapperQueuer eventPublisher) {
		this.eventPublisher = eventPublisher;
	}
	
	private void setAPIRegistry( Map<String, APIEvent> apiRegistry) {
		this.aPIRegistry = apiRegistry;
	}
	
	public Map<String, APIEvent> getAPIRegistry(){
		return this.aPIRegistry;
	}

	@Override
	public void set(Event _incoming) {
		Event_MiddleWare incoming = null;
		if(_incoming instanceof Event_MiddleWare){
			incoming = (Event_MiddleWare) _incoming;
			this.setEventPublisher(incoming.getEventPublisher());
			this.setFuture(incoming.getFuture());
			this.setAccessControl(incoming.getAccessControl());
			this.setShutdownChecker(incoming.getShutdownChecker());
			this.setAPIRegistry(incoming.getAPIRegistry());
		}
		else{
			throw new InvalidParameterException(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
		}
	}
	

	public Event_MiddleWare(WebServer webserver, Future<Pair<Request, Output>> incoming,AccessControl accessControl) {
		super();
		this.setEventPublisher(webserver.getEventPublisher());
		this.setFuture(incoming);
		this.setAccessControl(accessControl);
		this.setShutdownChecker(webserver);
		this.setAPIRegistry(webserver.getAPIRegistry());
	}
	

	@Override
	public EventResult onEvent() {
		EventResult ret = new EventResult();
		
		int backoff = 0;

		Future<Pair<Request, Output>> f = getFuture();

		/* Check degenerate case */
		if (f == null) {
			return new EventResult();
		}

		/* Wait for incoming job to be converted */
		while ((!getShutdownChecker().isQuitting()) && (!f.isDone())) {
			/* If the conversion job is not done, wait */
			backoff = backoff * 2 + r.nextInt(10);

			if (backoff > MAX_BACKOFF) {
				backoff -= r.nextInt(100); // This is totally hacky
			}
			if (backoff < 0) {
				backoff = 0;
			}

			try {
				Thread.sleep(backoff);
			} catch (InterruptedException e1) {
			}
		}
			
		/* If we aren't quitting and job is therefore done ... */
		if(f.isDone()){
			Pair<Request, Output> pair = null;
			try {
				pair = f.get();

				Request request = pair.getFirst();

				if (request != null) {
					/*
					 * Check to make sure this connection source is allowed
					 */
					String source = request.getSource();
					if (getAccessControl().allowSource(source, true, false)) {
						/*
						 * Add the work order to the dispatchers work queue
						 */
						Event_Dispatch event = new Event_Dispatch(getAPIRegistry(),getEventPublisher(),pair.getFirst(), pair.getSecond());
						EventWrapper eventWrapper = new EventWrapper(event);
						
						getEventPublisher().onData(eventWrapper);
					} else {
						getLog().warn(
							"Server silently rejected request from "
									+ source);
					}
				}
			} catch (InterruptedException e1) {
				// Getting the future result failed
			} catch (ExecutionException e1) {
				// Getting the future result failed
			}
		}
		return ret;
	}
	
	
	public Future<Pair<Request, Output>> getFuture() {
		return future;
	}
	
	protected void setFuture(Future<Pair<Request, Output>> future) {
		this.future = future;
	}

	
	public AccessControl getAccessControl() {
		return accessControl;
	}

	protected void setAccessControl(AccessControl accessControl) {
		this.accessControl = accessControl;
	}
	

	public Quittable getShutdownChecker() {
		return shutdownChecker;
	}

	protected void setShutdownChecker(Quittable shutdownChecker) {
		this.shutdownChecker = shutdownChecker;
	}

	
}