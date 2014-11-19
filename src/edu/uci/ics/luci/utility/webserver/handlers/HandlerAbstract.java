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

package edu.uci.ics.luci.utility.webserver.handlers;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;


public abstract class HandlerAbstract {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HandlerAbstract.class);
		}
		return log;
	}
	



	public static byte[] getContentTypeHeader_REDIRECT_UNSPECIFIED() {
		return "redirect ".getBytes();
	}

	public static byte[] getContentTypeHeader_PROXY(){
		return "proxy ".getBytes();
	}

	public HandlerAbstract(){
		super();
	}
	
	static private long jobsHandled = 0;
	protected void oneMoreJobHandled(){
		jobsHandled++;
	}
	
	static public long getJobCounter(){
		return jobsHandled;
	}
	
	/**
	 * This function should be overridden to actually do something in response to a REST call.  It should call oneMoreJobHandled, so
	 * that getJobCounter is meaningful.
	 * @param icr, All the info about the incoming request
	 */
	public abstract Response handle(Request icr,Output oc);
	
	/** This function is called to duplicate a Handler before being
	 * dispatched to handle an incoming request.
	 */
	public abstract HandlerAbstract copy();

	protected String wrapCallback(Map<String, Set<String>> parameters, String string) {
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
	protected String convertStreamToString(java.io.InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	


}
