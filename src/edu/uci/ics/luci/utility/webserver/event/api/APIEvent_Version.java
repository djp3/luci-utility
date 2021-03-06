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

package edu.uci.ics.luci.utility.webserver.event.api;

import java.security.InvalidParameterException;

import net.minidev.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_Version extends APIEvent implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_Version.class);
		}
		return log;
	}
	
	private String aPIVersion = null;
	
	public String getAPIVersion(){
		return aPIVersion;
	}
	
	public void setAPIVersion(String version){
		aPIVersion = version;
	}
	
	public APIEvent_Version(String version) {
		super();
		setAPIVersion(version);
	}
	

	@Override
	public void set(Event _incoming) {
		APIEvent_Version incoming = null;
		if(_incoming instanceof APIEvent_Version){
			incoming = (APIEvent_Version) _incoming;
			super.set(incoming);
			this.setAPIVersion(incoming.getAPIVersion());
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
		
		ret.put("version", getAPIVersion());
		
		response.setStatus(APIEventResult.Status.OK);
		response.setDataType(APIEventResult.DataType.JSON);
		response.setResponseBody(wrapCallback(getRequest().getParameters(),ret.toString()));
			
		getLog().info("Version is "+getAPIVersion());
		
		return response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((aPIVersion == null) ? 0 : aPIVersion.hashCode());
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
		if (!(obj instanceof APIEvent_Version)) {
			return false;
		}
		APIEvent_Version other = (APIEvent_Version) obj;
		if (aPIVersion == null) {
			if (other.aPIVersion != null) {
				return false;
			}
		} else if (!aPIVersion.equals(other.aPIVersion)) {
			return false;
		}
		return true;
	}
	
	

}


