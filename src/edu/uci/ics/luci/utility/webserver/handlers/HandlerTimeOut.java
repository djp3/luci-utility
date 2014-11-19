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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public class HandlerTimeOut extends HandlerAbstract {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HandlerTimeOut.class);
		}
		return log;
	}
	
	Thread timeOuter;
	
	public HandlerTimeOut(){
		super();
	}
	
	@Override
	public HandlerTimeOut copy() {
		return new HandlerTimeOut();
	}
	
	/**
	 * This should never return. 
	 * @param parameters a map of key and value that was passed through the REST request
	 * @return a pair where the first element is the content type and the bytes are the output bytes to send back
	 */
	@Override
	public Response handle(Request request, Output o) {
		Response response = o.makeOutputChannelResponse();
		
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
		
		response.setStatus(Response.Status.OK);
		response.setDataType(Response.DataType.JSON);
		response.setBody(wrapCallback(request.getParameters(),""));
		
		return response;
	}
}


