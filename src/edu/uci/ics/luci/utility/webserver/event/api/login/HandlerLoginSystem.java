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

package edu.uci.ics.luci.utility.webserver.event.api.login;

import java.util.Map.Entry;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.handlers.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public class HandlerLoginSystem extends HandlerAbstract {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HandlerLoginSystem.class);
		}
		return log;
	}
	
	private String version;

	public HandlerLoginSystem(String version) {
		super();
		this.version = version;
	}

	@Override
	public HandlerLoginSystem copy() {
		return new HandlerLoginSystem(this.version);
	}
	
	/**
	 * This returns the version number.
	 * @param parameters a map of key and value that was passed through the REST request
	 * @return a pair where the first element is the content type and the bytes are the output bytes to send back
	 */
	@Override
	public Response handle(Request request, Output o) {
		Response response = o.makeOutputChannelResponse();
		
		JSONObject ret = new JSONObject();
		ret.put("version", version);
		ret.put("error", "false");
		
		JSONObject parameters = new JSONObject();
		
		for(Entry<String, Set<String>> e:request.getParameters().entrySet()){
			JSONArray array = new JSONArray();
			for(String s:e.getValue()){
				array.add(s);
			}
			parameters.put(e.getKey(), array);
		}
		ret.put("parameters", parameters);
		
		response.setStatus(Response.Status.OK);
		response.setDataType(Response.DataType.JSON);
		response.setBody(wrapCallback(request.getParameters(),ret.toString()));
		
		return response;
	}
}


