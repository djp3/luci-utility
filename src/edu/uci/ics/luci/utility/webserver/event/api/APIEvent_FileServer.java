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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_FileServer extends APIEvent implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_FileServer.class);
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
	public APIEvent_FileServer(Class<?> resourceBaseClass,String resourcePrefix){
		super();
		this.resourceBaseClass = resourceBaseClass;
		this.resourcePrefix = resourcePrefix;
	}


	@Override
	public Object clone(){
		return(super.clone());
	}
	

	/** From : http://stackoverflow.com/a/5445161
	 * 
	 * @param is
	 * @return
	 */
	protected static String convertStreamToString(java.io.InputStream is) {
	    try {
	        return new java.util.Scanner(is).useDelimiter("\\A").next();
	    } catch (java.util.NoSuchElementException e) {
	        return "";
	    }
	}
	
	
	
	@Override
	public APIEventResult onEvent() {
		APIEventResult response = null;
		
		response = getOutput().makeOutputChannelResponse();

		String body = null;
		
		InputStream ios = null;
		
		String resource = resourcePrefix+getRequest().getCommand();
		ios = resourceBaseClass.getResourceAsStream(resource);
		
		try{
			if(ios != null){
				if(getRequest().getCommand().endsWith(".css")){
					response.setDataType(APIEventResult.DataType.CSS);
					body = convertStreamToString(ios);
				}
				else if(getRequest().getCommand().endsWith(".png")){
					response.setDataType(APIEventResult.DataType.PNG);
					body = IOUtils.toString(ios);
				}
				else if(getRequest().getCommand().endsWith(".js")){
					response.setDataType(APIEventResult.DataType.JAVASCRIPT);
					body = convertStreamToString(ios);
				}else{
					response.setDataType(APIEventResult.DataType.HTML);
					body = convertStreamToString(ios);
				}
			}
			else{
				response.setDataType(APIEventResult.DataType.HTML);
				body = ("Resource not found:"+resource);
			}
			response.setStatus(APIEventResult.Status.OK);
			response.setResponseBody(body);
			
		} catch (IOException e) {
			getLog().error("Problem serving up content:"+getRequest().getCommand()+"\n"+e);
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


