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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public class HandlerFileServer extends HandlerAbstract {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HandlerFileServer.class);
		}
		return log;
	}


	private String resourcePrefix;
	private Class<?> resourceBaseClass;

	/**
	 * 
	 * @param resourcePrefix, something like "/www/" for finding the relevant files in the package
	 * @param resourceBaseClass, something like Globals.getGlobals().getClass(), which says which package to look in
	 */
	public HandlerFileServer(Class<?> resourceBaseClass,String resourcePrefix){
		super();
		this.resourceBaseClass = resourceBaseClass;
		this.resourcePrefix = resourcePrefix;
	}


	@Override
	public HandlerFileServer copy() {
		return new HandlerFileServer(this.resourceBaseClass,this.resourcePrefix);
	}
	
	
	/**
	 * This returns the version number.
	 * @param parameters a map of key and value that was passed through the REST request
	 * @return a pair where the first element is the content type and the bytes are the output bytes to send back
	 */
	@Override
	public Response handle(Request request, Output o) {
		Response response = o.makeOutputChannelResponse();

		String body = null;
		
		InputStream ios = null;
		
		String resource = resourcePrefix+request.getCommand();
		ios = resourceBaseClass.getResourceAsStream(resource);
		
		try{
			if(ios != null){
				if(request.getCommand().endsWith(".css")){
					response.setDataType(Response.DataType.CSS);
					body = convertStreamToString(ios);
				}
				else if(request.getCommand().endsWith(".png")){
					response.setDataType(Response.DataType.PNG);
					body = IOUtils.toString(ios);
				}
				else if(request.getCommand().endsWith(".js")){
					response.setDataType(Response.DataType.JAVASCRIPT);
					body = convertStreamToString(ios);
				}else{
					response.setDataType(Response.DataType.HTML);
					body = convertStreamToString(ios);
				}
			}
			else{
				response.setDataType(Response.DataType.HTML);
				body = ("Resource not found:"+resource);
			}
			response.setStatus(Response.Status.OK);
			response.setBody(body);
			
		} catch (IOException e) {
			getLog().error("Problem serving up content:"+request.getCommand()+"\n"+e);
		}finally{
			try {
				if(ios != null){
					ios.close();
					ios = null;
				}
			} catch (IOException e) {
			}
		}
		
		return response;
	}
}


