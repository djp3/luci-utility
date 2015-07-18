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

package edu.uci.ics.luci.utility.webserver.event.api;

import java.security.InvalidParameterException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_TimeOut extends APIEvent implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_TimeOut.class);
		}
		return log;
	}
	
	private Thread timeOuter;
	
	public APIEvent_TimeOut(){
		super();
	}
	
	public Thread getTimeOuter() {
		return timeOuter;
	}

	public void setTimeOuter(Thread timeOuter) {
		this.timeOuter = timeOuter;
	}

	@Override
	public void set(Event _incoming) {
		APIEvent_TimeOut incoming = null;
		if(_incoming instanceof APIEvent_TimeOut){
			incoming = (APIEvent_TimeOut) _incoming;
			super.set(incoming);
			this.setTimeOuter(incoming.getTimeOuter());
		}
		else{
			getLog().error(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
			throw new InvalidParameterException(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
		}
	}
	
	@Override
	public Object clone(){
		return(super.clone());
	}
	
	
	/**
	 * This should never return. 
	 * @param parameters a map of key and value that was passed through the REST request
	 * @return a pair where the first element is the content type and the bytes are the output bytes to send back
	 */
	@Override
	public APIEventResult onEvent() {
		APIEventResult response = null;
		
		response = getOutput().makeOutputChannelResponse();
		
		timeOuter = new Thread(new Runnable(){
			public void run() {
				while(timeOuter.isAlive()){ //This is dumb, but it eliminates a FindBugs warning
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				
			}});
		timeOuter.setDaemon(true);
		timeOuter.start();
		
		while(timeOuter.isAlive()){
			try {
				timeOuter.join();
			} catch (InterruptedException e1) {
			}
		}
		
		/* We should never get here */
		getLog().fatal("Something very unexpected has happened");
		return response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((timeOuter == null) ? 0 : timeOuter.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof APIEvent_TimeOut)) {
			return false;
		}
		APIEvent_TimeOut other = (APIEvent_TimeOut) obj;
		if (timeOuter == null) {
			if (other.timeOuter != null) {
				return false;
			}
		} else if (!timeOuter.equals(other.timeOuter)) {
			return false;
		}
		return true;
	}
	
	
	
}


