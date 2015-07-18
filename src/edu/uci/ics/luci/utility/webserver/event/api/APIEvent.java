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
import java.util.Map;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;



public class APIEvent extends Event implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent.class);
		}
		return log;
	}
	
	private Request request = null;
	private Output output = null;
	
	public APIEvent(){
		super();
	}
	
	protected Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	protected Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}
	
	@Override
	public void set(Event _incoming) {
		APIEvent incoming = null;
		if(_incoming instanceof APIEvent){
			incoming = (APIEvent) _incoming;
			this.setRequest(incoming.getRequest());
			this.setOutput(incoming.getOutput());
		}
		else{
			getLog().error(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
			throw new InvalidParameterException(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
		}
	}
	
	
	@Override
	public Object clone(){
		try {
			return(super.clone());
		} catch (CloneNotSupportedException e) {
			getLog().error("Um didn't you implement Cloneable?"+e);
			return null;
		}
	}
	

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
	
	

	protected JSONObject buildResponseSkeleton() {
		JSONObject ret = new JSONObject();
		ret.put("error", "false");
		JSONArray errors = new JSONArray();
		ret.put("errors",errors);
		return ret;
	}
	

	@Override
	public APIEventResult onEvent() {
		
		APIEventResult response = null;
		
		response = getOutput().makeOutputChannelResponse();
		
		JSONObject ret = buildResponseSkeleton();
		
		response.setStatus(APIEventResult.Status.OK);
		response.setDataType(APIEventResult.DataType.JSON);
		response.setResponseBody(wrapCallback(getRequest().getParameters(),ret.toString()));
		
		getLog().info(this.getClass().getSimpleName()+" Executed");
		return response;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((output == null) ? 0 : output.hashCode());
		result = prime * result + ((request == null) ? 0 : request.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof APIEvent)) {
			return false;
		}
		APIEvent other = (APIEvent) obj;
		if (output == null) {
			if (other.output != null) {
				return false;
			}
		} else if (!output.equals(other.output)) {
			return false;
		}
		if (request == null) {
			if (other.request != null) {
				return false;
			}
		} else if (!request.equals(other.request)) {
			return false;
		}
		return true;
	}
	
	
	

}
