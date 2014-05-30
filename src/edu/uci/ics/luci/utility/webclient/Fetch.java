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

package edu.uci.ics.luci.utility.webclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.datastructure.Pair;
import edu.uci.ics.luci.utility.webserver.WebUtil;

/**
 * This class is designed to facilitate getting content from the Internet.
 * Hopefully robustly. 
 * @author djp3
 *
 */
public class Fetch {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Fetch.class);
		}
		return log;
	}
	

	public static void resetLog(){
		log = null;
	}
	
	/* Number of times the URL has failed and the URL like "localhost:1776" */
	transient Object urlPoolLock = new Object();
	/* A set of url's that provide equivalent information */
	private TreeSet<Pair<Long,String>> urlPool = null;
	

	/*
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        resetLog();
        urlPoolLock = new Object();
    }*/
	
	public Fetch(){
		urlPool = new TreeSet<Pair<Long,String>>();
	}
	
	/**
	 * 
	 * @param server, something like "https://foo.com"
	 */
	public Fetch(String server){
		this();
		TreeMap<String, Long> mapper = new TreeMap<String,Long>();
		mapper.put(server,0L);
		resetUrlPool(mapper);
	}
	

	/**
	 * 
	 * @param urlPool, a set of urls (String) mapped to a priority (Long).  The lower the priority the earlier the url is tried.
	 *  If a url fails it's priority is incremented by one each time and the priorities are reordered. 
	 */
	public Fetch(Map<String,Long> urlPool){
		this();
		resetUrlPool(urlPool);
	}
	

	/**
	 * Shuffle the urls in the url pool so that urls with the same priority value
	 * are randomly ordered but all urls with a higher number come after all urls with 
	 * a lower number.
	 * @param urlMap
	 */
	protected void resetUrlPool(Map<String,Long> urlMap) {
		
		/* Get a set ordered by success/priority */
		synchronized(urlPoolLock){
			
			List<Entry<String, Long>> shuffler = new ArrayList<Entry<String,Long>>();
			
			for(Entry<String, Long> e: urlMap.entrySet()){
				shuffler.add(e);
			}
			/* Randomly choose among equal priorities */
			Collections.shuffle(shuffler); 
			
			/*erase old URLs*/
			urlPool.clear();
			
			for(Entry<String, Long> p:shuffler){
				urlPool.add(new Pair<Long,String>(p.getValue(),p.getKey()));
			}
		}
	}
	
	
	
	public TreeSet<Pair<Long,String>> getUrlPoolCopy(){
		TreeSet<Pair<Long,String>> ret = null;
		synchronized(urlPoolLock){
			ret = new TreeSet<Pair<Long,String>>(urlPool);
		}
		return(ret);
	}
	
	

	private void incrementFailCount(String s) {
		boolean found = false;
		synchronized(urlPoolLock){
			TreeSet<Pair<Long, String>> toDelete = new TreeSet<Pair<Long,String>>();
			TreeSet<Pair<Long, String>> toAdd = new TreeSet<Pair<Long,String>>();
			for(Pair<Long, String> p:urlPool){
				if(p.getSecond().equals(s)){
					Long x = p.getFirst();
					toDelete.add(p);
					if(!found){
						toAdd.add(new Pair<Long,String>(x+1,s));
						found = true;
					}
				}
			}
			for(Pair<Long, String> p:toDelete){
				urlPool.remove(p);
			}
			for(Pair<Long,String> p:toAdd){
				urlPool.add(p);
			}
			if(!found){
				urlPool.add(new Pair<Long,String>(1L,s));
			}
		}
	}
	
	
	
	
	/**
	 * Fetch a web page's contents. Note that this will change all line breaks
	 * into system line breaks!  
	 *
	 * @param path
	 *            The web-page (or file) to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects. The path is everything after the ip address
	 *            including the "/".  So the path for http:///www.cnn.com/media/index.html should be "/media/index.html"
	 *            The server locations are taken from the pool
	 * @param authenticate
	 * 	 		  True if basic authentication should be used.  In which case vars needs to have
	 *            an entry for "username" and "password".           
	 * @param vars
	 * 			  A Map of params to be sent on the uri. "username" and "password" is removed before
	 *            calling the uri.           
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @return The full text of the web page
	 *
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	
	public String fetchWebPage(String path, boolean authenticate, Map<String, String> vars, int timeOutMilliSecs) throws  MalformedURLException, IOException
	{
		String responseString = null;
		TreeSet<Pair<Long, String>> servers = new TreeSet<Pair<Long,String>>();
		
		synchronized(urlPoolLock){
			servers.addAll(urlPool);
		}
		
		while(servers.size()>0){
			String s = servers.pollFirst().getSecond();
			try{
				responseString = WebUtil.fetchWebPage(""+s+path, authenticate, vars, timeOutMilliSecs);
				break;
			}
			catch(IOException e){
				incrementFailCount(s);
				if(servers.size() == 0){
					throw e;
				}
			}
		}
		return responseString;

	}
	
	
	
	/**
	 * Fetch a web page's contents. If the webpage errors out or fails to parse JSON it's considered an error. 
	 *
	 * @param path
	 *            The URL to fetch. This method can handle
	 *            permanent redirects, but not javascript or meta redirects. The path is everything after the ip address
	 *            including the "/".  So the path for http:///www.cnn.com/media/index.html should be "/media/index.html"
	 *            The server locations are taken from the pool
	 * @param authenticate
	 * 	 		  True if basic authentication should be used.  In which case vars needs to have
	 *            an entry for "username" and "password".           
	 * @param vars
	 * 			  A Map of params to be sent on the uri. "username" and "password" is removed before
	 *            calling the uri.           
	 * @param timeOutMilliSecs
	 *            The read time out in milliseconds. Zero is not allowed. Note
	 *            that this is not the timeout for the method call, but only for
	 *            the connection. The method call may take longer.
	 * @return A JSON object from the URL
	 *
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	
	public JSONObject fetchJSONObject(String path, boolean authenticate, Map<String, String> vars, int timeOutMilliSecs) throws  MalformedURLException, IOException 
	{
		JSONObject ret = null;
		TreeSet<Pair<Long, String>> servers = new TreeSet<Pair<Long,String>>();
		
		synchronized(urlPoolLock){
			servers.addAll(urlPool);
		}
		
		
		while(servers.size() > 0){
			String s = servers.pollFirst().getSecond();
			try{
				String responseString = WebUtil.fetchWebPage(""+s+path, authenticate, vars, timeOutMilliSecs);
				if(responseString != null){
					try{
						ret = (JSONObject) JSONValue.parse(responseString);
						break;
					}
					catch(ClassCastException e){
						getLog().info("response is not a JSONObject:\n"+responseString);
						if(servers.size() == 0){
							throw new IOException(e);
						}
					}
				}
			}
			catch(IOException e){
				incrementFailCount(s);
				if(servers.size() == 0){
					throw e;
				}
			}
		}
		return ret;
	}

}
