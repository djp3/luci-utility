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

import java.net.URISyntaxException;
import java.security.InvalidParameterException;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_Favicon extends APIEvent implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_Favicon.class);
		}
		return log;
	}

	private URIBuilder favicon;
	

	public URIBuilder getFavicon() {
		return favicon;
	}

	public void setFavicon(URIBuilder favicon) {
		this.favicon = favicon;
	}

	public APIEvent_Favicon(URIBuilder favicon) {
		super();
		
		if(favicon != null){
			try {
				setFavicon(new URIBuilder(favicon.build()));
			} catch (URISyntaxException e) {
				getLog().info(e);
				setFavicon(null);
			}
		}
		else{
			setFavicon(null);
		}
	}
	
	@Override
	public void set(Event _incoming) {
		APIEvent_Favicon incoming = null;
		if(_incoming instanceof APIEvent_Favicon){
			incoming = (APIEvent_Favicon) _incoming;
			super.set(incoming);
			setFavicon(incoming.getFavicon());
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
		
		response.setStatus(APIEventResult.Status.PROXY);
		response.setDataType(APIEventResult.DataType.PROXYSTRING);

		try {
			response.setResponseBody(this.favicon.build().toString());
		} catch (URISyntaxException e) {
			getLog().info(e);
		}
		getLog().info("Favicon response:"+response.getResponseBody());
		return response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((favicon == null) ? 0 : favicon.hashCode());
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
		if (!(obj instanceof APIEvent_Favicon)) {
			return false;
		}
		APIEvent_Favicon other = (APIEvent_Favicon) obj;
		if (favicon == null) {
			if (other.favicon != null) {
				return false;
			}
		} else if (!favicon.equals(other.favicon)) {
			return false;
		}
		return true;
	}
	
	
	

}


