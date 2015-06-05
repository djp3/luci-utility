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

package edu.uci.ics.luci.utility.webserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;

public class AccessControl {
	protected final String defaultFilenameTest1 = "test/access_control_list_for_testing.properties";
	protected final String defaultFilenameTest2 = "test/access_control_list.properties";
	protected final String defaultFilenameLive = "access_control_list.properties";
	
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(AccessControl.class);
		}
		return log;
	}
	
	public AccessControl(){
	}
	
	public void reset(){
		setDefaultFilename(null);
		setConfig(null);
		setLastLoad(0);
		setAllowedConnections(null);
		setExpirationTime(5*60*1000); //5 minutes
		
		/*This is a random assortment of people who tried to connect to a server */
		/*This is used for a dynamic test at runtime to make sure the configuration file isn't whack*/
		setBadGuyTest(Globals.getGlobals().getBadGuyList());
	}
	
	protected String defaultFilename = null;

	protected Configuration config;
	protected long lastLoad;
	protected List<String> allowedConnections;
	protected long expirationTime;
	protected List<String> badGuyTest = new ArrayList<String>();
	
	protected long getExpirationTime() {
		return expirationTime;
	}

	protected void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	protected synchronized List<String> getAllowedConnections() {
		return allowedConnections;
	}

	protected synchronized void setAllowedConnections(List<String> allowedConnections) {
		this.allowedConnections = allowedConnections;
	}
	
	/**
	 * 
	 * @return A list of bad guys IP addresses as regex's that should never be allowed.  
	 * This is used as a sanity check to make sure the configuration file
	 *  isn't set up wrong.
	 */
	protected List<String> getBadGuyTest() {
		return badGuyTest;
	}

	protected void setBadGuyTest(List<String> newBlock) {
		badGuyTest = new ArrayList<String>();
		badGuyTest.addAll(newBlock);
	}

	protected String getDefaultFilename() {
		return defaultFilename;
	}

	protected void setDefaultFilename(String defaultFilename) {
		if(defaultFilename == null){
			if(Globals.getGlobals().isTesting()){
				this.defaultFilename= defaultFilenameTest1;
			}
			else{
				this.defaultFilename= defaultFilenameLive;
			}
		}
		else{
			this.defaultFilename = defaultFilename;
		}
	}

	protected synchronized Configuration getConfig() {
		return config;
	}

	protected synchronized void setConfig(Configuration config) {
		this.config = config;
	}

	protected synchronized long getLastLoad() {
		return lastLoad;
	}

	protected synchronized void setLastLoad(long lastLoad) {
		this.lastLoad = lastLoad;
	}

	protected synchronized void loadConfiguration() throws ConfigurationException{
		
		setConfig(new PropertiesConfiguration(defaultFilename));
		
		allowedConnections = Arrays.asList(getConfig().getStringArray("allowed_clients"));
		lastLoad = System.currentTimeMillis();
		
		/*Sanity check on allowedConnections*/
		for(int i = 0; i < badGuyTest.size(); i++){
			if(allowSource(badGuyTest.get(i),true,false)){
				allowedConnections = null;
				lastLoad = 0;
				getLog().error("Something is wrong with the access control list.  It is letting a test case through:"+badGuyTest.get(i));
				throw new RuntimeException("Access Control List Sanity Check Failed");
			}
		}
	}
	
	protected synchronized boolean cacheExpired(){
		if((System.currentTimeMillis() - lastLoad) > expirationTime){
			return(true);
		}
		else{
			return(false);
		}
	}

	/**
	 * 
	 * @param source the ip address or dns listing of the source
	 * @param doReverseLookups attempts to do a reverse lookup on source
	 * @return
	 */
	public synchronized boolean allowSource(String source, boolean doReverseLookups,boolean caseSensitive) {
		
		if(source == null){
			return false;
		}
		
		try{
			if(allowedConnections == null){
				loadConfiguration();
			}
		}catch(ConfigurationException e){
			getLog().error("Can't load property file:"+defaultFilename+" from working directory:"+System.getProperty("user.dir")+"\n"+e.toString());
			allowedConnections = null;
		}catch(RuntimeException e){
			getLog().error("Can't load property file:"+defaultFilename+" from working directory:"+System.getProperty("user.dir")+"\n"+e.toString());
			allowedConnections = null;
		}catch(Throwable e){
			getLog().error("Can't load property file:"+defaultFilename+" from working directory:"+System.getProperty("user.dir")+"\n"+e.toString());
			allowedConnections = null;
		}
		finally{
			if(allowedConnections == null){
				getLog().error("Couldn't load allowedConnections");
				return(false);
			}
		}
		
		/*First check to see if the source matches any of the regular expressions*/
		for (String s:allowedConnections){
	        Pattern pattern =  null;
	        if(caseSensitive){
	        	pattern =  Pattern.compile(s);
	        }
	        else{
	        	pattern =  Pattern.compile(s,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	        }
	        Matcher matcher =  pattern.matcher(source);
	
	        if(matcher.find()){
	        	getLog().debug("Access request "+source+" matched pattern:"+s);
	        	return true;
	        }
		}
		
		if(doReverseLookups){
			try{
				InetAddress[] addresses = InetAddress.getAllByName(source);
				for ( int i=0; i<addresses.length; i++ ){
					String hostname = addresses[i].getHostName();
					//log.info("Found hostname for source:"+source);
					//log.info("\t hostname:"+hostname);
					
					/*Recursively test hostnames */
					if(allowSource(hostname,false,caseSensitive)){
						getLog().debug("Access request "+source+" matched pattern:"+hostname);
						return true;
					}
				}
			}
			catch (UnknownHostException e ){
				getLog().warn("The ip address:"+source+" contacted me and couldn't be resolved");
				//Unknown Host shouldn't be allowed
			}
		}
		
		/*At this point the answer should be false, so reload cache just in case*/
		if(cacheExpired()){
			allowedConnections = null;
			return(allowSource(source,doReverseLookups,caseSensitive));
		}
		else{
			return false;
		}
	}
}
