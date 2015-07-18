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

import net.minidev.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Quittable;
import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_Shutdown extends APIEvent { 
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_Shutdown.class);
		}
		return log;
	}

	private Quittable q = null;
	
	public APIEvent_Shutdown(Quittable q) {
		super();
		this.q = q;
	}
	

	public Quittable getQuittable() {
		return q;
	}

	public void setQuittable(Quittable q) {
		this.q = q;
	}

	@Override
	public void set(Event _incoming) {
		APIEvent_Shutdown incoming = null;
		if(_incoming instanceof APIEvent_Shutdown){
			incoming = (APIEvent_Shutdown) _incoming;
			super.set(incoming);
			this.setQuittable(incoming.getQuittable());
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
	

	
	@Override
	public APIEventResult onEvent() {
		
		APIEventResult response = null;
		
		response = getOutput().makeOutputChannelResponse();
		
		JSONObject ret = buildResponseSkeleton();
			
		ret.put("shutdown", "true");
		
		response.setStatus(APIEventResult.Status.OK);
		response.setDataType(APIEventResult.DataType.JSON);
		response.setResponseBody(wrapCallback(getRequest().getParameters(),ret.toString()));
		
		this.q.setQuitting(true);
		
		return response;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((q == null) ? 0 : q.hashCode());
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
		if (!(obj instanceof APIEvent_Shutdown)) {
			return false;
		}
		APIEvent_Shutdown other = (APIEvent_Shutdown) obj;
		if (q == null) {
			if (other.q != null) {
				return false;
			}
		} else if (!q.equals(other.q)) {
			return false;
		}
		return true;
	}
	
	
	
}


