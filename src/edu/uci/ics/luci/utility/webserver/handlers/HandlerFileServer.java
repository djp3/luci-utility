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
import java.net.InetAddress;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.HandlerAbstract;
import edu.uci.ics.luci.utility.webserver.RequestDispatcher.HTTPRequest;

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
	public Pair<byte[], byte[]> handle(InetAddress ip, HTTPRequest httpRequestType, Map<String, String> headers, String restFunction, Map<String, String> parameters) {
		Pair<byte[], byte[]> pair = null;
		byte[] ret = null;
		byte[] type = null;
		
		InputStream ios = null;
		
		String resource = resourcePrefix+restFunction;
		ios = resourceBaseClass.getResourceAsStream(resource);
		
		try{
			if(ios != null){
				if(restFunction.endsWith(".css")){
					ret = convertStreamToString(ios).getBytes();
					type = HandlerAbstract.getContentTypeHeader_CSS();
				}
				else if(restFunction.endsWith(".png")){
					ret = IOUtils.toByteArray(ios);
					type = HandlerAbstract.getContentTypeHeader_PNG();
				}
				else if(restFunction.endsWith(".js")){
					ret = convertStreamToString(ios).getBytes();
					type = HandlerAbstract.getContentTypeHeader_JS();
				}else{
					ret = convertStreamToString(ios).getBytes();
					type = HandlerAbstract.getContentTypeHeader_HTML();
				}
			}
			else{
				ret = ("Resource not found:"+resource).getBytes();
				type = HandlerAbstract.getContentTypeHeader_HTML();
			}
			pair = new Pair<byte[],byte[]>(type,ret);
			
		} catch (IOException e) {
			getLog().error("Problem serving up content:"+restFunction+"\n"+e);
		}finally{
			try {
				if(ios != null){
					ios.close();
					ios = null;
				}
			} catch (IOException e) {
			}
		}
		
		
		return pair;
	}
}


