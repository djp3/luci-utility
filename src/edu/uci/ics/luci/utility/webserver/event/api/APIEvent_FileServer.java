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

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_FileServer extends APIEvent implements Cloneable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_FileServer.class);
		}
		return log;
	}


	private Class<?> resourceBaseClass;
	private String resourcePrefix;

	/**
	 * 
	 * @param resourceBaseClass, something like Globals.getGlobals().getClass(), which says which package to look in
	 * @param resourcePrefix, something like "/www/" for finding the relevant files in the package
	 * @param stripPrefix, a file path to remove from the beginning of the incoming request in case the web URL has a prefix that the file system does not
	 */
	public APIEvent_FileServer(Class<?> resourceBaseClass,String resourcePrefix){
		super();
		this.resourceBaseClass = resourceBaseClass;
		this.resourcePrefix = resourcePrefix;
	}


	public String getResourcePrefix() {
		return resourcePrefix;
	}

	public void setResourcePrefix(String resourcePrefix) {
		this.resourcePrefix = resourcePrefix;
	}
	

	public Class<?> getResourceBaseClass() {
		return resourceBaseClass;
	}

	public void setResourceBaseClass(Class<?> resourceBaseClass) {
		this.resourceBaseClass = resourceBaseClass;
	}

	
	@Override
	public void set(Event _incoming) {
		APIEvent_FileServer incoming = null;
		if(_incoming instanceof APIEvent_FileServer){
			incoming = (APIEvent_FileServer) _incoming;
			super.set(incoming);
			this.setResourceBaseClass(incoming.getResourceBaseClass());
			this.setResourcePrefix(incoming.getResourcePrefix());
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
	

	/** From : http://stackoverflow.com/a/5445161
	 * 
	 * @param is
	 * @return
	 */
	protected static String convertStreamToString(java.io.InputStream is) {
	    try {
	        Scanner s = null;
	        try{
	        	s = new java.util.Scanner(is,"UTF-8");
	        	return s.useDelimiter("\\A").next();
	        }
	        finally {
	        	if(s != null) {
	        		s.close();
	        		s = null;
	        	}
	        }
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
		
		String cl = getRequest().getCommandLine();
		String c = getRequest().getCommand();
		String resource = null;
		if(cl.startsWith(c)){
			if( c.equals("/")) {
				resource = resourcePrefix+"/"+cl.substring(c.length());
			}
			else {
				resource = resourcePrefix+cl.substring(c.length());
			}
			getLog().debug("resource Prefix=\""+resourcePrefix+"\" command line = \""+cl+"\" command = \""+c+"\" result = \""+resource+"\"");
		}
		
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


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((resourceBaseClass == null) ? 0 : resourceBaseClass
						.hashCode());
		result = prime * result
				+ ((resourcePrefix == null) ? 0 : resourcePrefix.hashCode());
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
		if (!(obj instanceof APIEvent_FileServer)) {
			return false;
		}
		APIEvent_FileServer other = (APIEvent_FileServer) obj;
		if (resourceBaseClass == null) {
			if (other.resourceBaseClass != null) {
				return false;
			}
		} else if (!resourceBaseClass.equals(other.resourceBaseClass)) {
			return false;
		}
		if (resourcePrefix == null) {
			if (other.resourcePrefix != null) {
				return false;
			}
		} else if (!resourcePrefix.equals(other.resourcePrefix)) {
			return false;
		}
		return true;
	}


	
	
}


