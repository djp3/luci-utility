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

package edu.uci.ics.luci.utility.webclient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.client.utils.URIBuilder;
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
	private TreeSet<Pair<Long,URI>> urlPool = null;
	

	/*
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        resetLog();
        urlPoolLock = new Object();
    }*/
	
	public Fetch(){
		urlPool = new TreeSet<Pair<Long,URI>>();
	}
	
	/**
	 * 
	 * @param server, something like "https://foo.com"
	 */
	public Fetch(URI uri){
		this();
		TreeMap<URI, Long> mapper = new TreeMap<URI,Long>();
		mapper.put(uri,0L);
		resetUrlPool(mapper);
	}
	

	/**
	 * 
	 * @param urlPool, a set of urls (String) mapped to a priority (Long).  The lower the priority the earlier the url is tried.
	 *  If a url fails it's priority is incremented by one each time and the priorities are reordered. 
	 */
	public Fetch(Map<URI,Long> urlPool){
		this();
		resetUrlPool(urlPool);
	}
	

	/**
	 * Shuffle the urls in the url pool so that urls with the same priority value
	 * are randomly ordered but all urls with a higher number come after all urls with 
	 * a lower number.
	 * @param urlMap
	 */
	protected void resetUrlPool(Map<URI,Long> urlMap) {
		
		/* Get a set ordered by success/priority */
		synchronized(urlPoolLock){
			
			List<Entry<URI, Long>> shuffler = new ArrayList<Entry<URI,Long>>();
			
			for(Entry<URI, Long> e: urlMap.entrySet()){
				shuffler.add(e);
			}
			/* Randomly choose among equal priorities */
			Collections.shuffle(shuffler); 
			
			/*erase old URLs*/
			urlPool.clear();
			
			for(Entry<URI, Long> p:shuffler){
				urlPool.add(new Pair<Long,URI>(p.getValue(),p.getKey()));
			}
		}
	}
	
	
	
	public TreeSet<Pair<Long,URI>> getUrlPoolCopy(){
		TreeSet<Pair<Long,URI>> ret = null;
		synchronized(urlPoolLock){
			ret = new TreeSet<Pair<Long,URI>>(urlPool);
		}
		return(ret);
	}
	
	

	private void incrementFailCount(URI s) {
		boolean found = false;
		synchronized(urlPoolLock){
			TreeSet<Pair<Long, URI>> toDelete = new TreeSet<Pair<Long,URI>>();
			TreeSet<Pair<Long, URI>> toAdd = new TreeSet<Pair<Long,URI>>();
			for(Pair<Long, URI> p:urlPool){
				if(p.getSecond().equals(s)){
					Long x = p.getFirst();
					toDelete.add(p);
					if(!found){
						toAdd.add(new Pair<Long,URI>(x+1,s));
						found = true;
					}
				}
			}
			for(Pair<Long, URI> p:toDelete){
				urlPool.remove(p);
			}
			for(Pair<Long,URI> p:toAdd){
				urlPool.add(p);
			}
			if(!found){
				urlPool.add(new Pair<Long,URI>(1L,s));
			}
		}
	}
	
	
	
	
	public String fetchWebPage(
			URIBuilder uriBuilder,
			Map<String, String> sendHeaderFields,
			final Map<String, List<String>> receiveHeaderFields,
			Pair<String,String> username_password,
			int timeOutMilliSecs) throws IOException, URISyntaxException {
		
		String responseString = null;
		TreeSet<Pair<Long, URI>> servers = new TreeSet<Pair<Long,URI>>();
		
		synchronized(urlPoolLock){
			servers.addAll(urlPool);
		}
		
		while(servers.size()>0){
			URI s = servers.pollFirst().getSecond();
			try{
				URIBuilder copy = new URIBuilder(uriBuilder.build());
				copy.setScheme(s.getScheme());
				copy.setHost(s.getHost());
				copy.setPort(s.getPort());
				responseString = WebUtil.fetchWebPage(copy,sendHeaderFields,receiveHeaderFields,
						username_password,timeOutMilliSecs);
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
	 */
	
	public JSONObject fetchJSONObject(
					URIBuilder uriBuilder,
					Map<String, String> sendHeaderFields,
					final Map<String, List<String>> receiveHeaderFields,
					Pair<String,String> username_password,
					int timeOutMilliSecs) throws IOException, URISyntaxException {
				
		JSONObject ret = null;
		TreeSet<Pair<Long, URI>> servers = new TreeSet<Pair<Long,URI>>();
		
		synchronized(urlPoolLock){
			servers.addAll(urlPool);
		}
		
		
		while(servers.size() > 0){
			URI s = servers.pollFirst().getSecond();
			try{
				URIBuilder copy = new URIBuilder(uriBuilder.build());
				copy.setScheme(s.getScheme());
				copy.setHost(s.getHost());
				copy.setPort(s.getPort());
				String responseString = WebUtil.fetchWebPage(copy,sendHeaderFields,receiveHeaderFields,
								username_password,timeOutMilliSecs);
				
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
