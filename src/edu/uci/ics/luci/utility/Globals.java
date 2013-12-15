/*
	Copyright 2007-2013
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

package edu.uci.ics.luci.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public abstract class Globals implements Quittable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = Logger.getLogger(Globals.class);
		}
		return log;
	}
	
	static protected Globals _globals = null;
	private static final String propertyFileNameDefault = "luci-utility.log4j.properties";
	
	
	public static synchronized Globals getGlobals(){
		return _globals;
	}
	
	public static synchronized Globals setGlobals(Globals _globals) {
		return(Globals._globals = _globals);
	}
	
	protected String propertyFileName = propertyFileNameDefault;
	private String databaseDomain = "localhost";
	
	List<Quittable> quittables = new ArrayList<Quittable>();
	private CalendarCache calendarCache = null;
	
	private boolean shuttingDown = false;
	private boolean testing = true;
	
	protected Globals(){
		this(null);
	}
	
	protected Globals(String fileName){
		super();
		if(fileName == null){
			reloadLog4jProperties();
		}
		else{
			reloadLog4jProperties(fileName);
		}
		calendarCache = new CalendarCache(CalendarCache.TZ_GMT);
	}
	
	public String getDatabaseDomain() {
		return databaseDomain;
	}
	
	public void setDatabaseDomain(String databaseDomain) {
		this.databaseDomain = databaseDomain;
	}
	
	public String getDefaultDatabaseDomain() {
		return databaseDomain;
	}
	

	public void reloadLog4jProperties(){
		File f = new File(getPropertyFileName());
		if(f.exists()){
			PropertyConfigurator.configure(getPropertyFileName());
		}
		else{
			BasicConfigurator.configure();
			getLog().log(Level.INFO,"Unable to locate property file:"+getPropertyFileName());
		}
	}
	
	public void reloadLog4jProperties(String propertyFileName) {
		setPropertyFileName(propertyFileName);
		reloadLog4jProperties();
	}
	
	public String getPropertyFileName() {
		return propertyFileName;
	}
	
	public void setPropertyFileName(String propertyFileName) {
		this.propertyFileName = propertyFileName;
	}
	
	public List<String> getBadGuyList() {
		return(new ArrayList<String>());
	}
	
	public synchronized void addQuittables(Quittable q){
		if(q != null){
			if(shuttingDown){
				q.setQuitting(true);
			}
			else{
				synchronized(quittables){
					synchronized(q){
						this.quittables.add(q);
						ArrayList<Quittable> newq = new ArrayList<Quittable>();
						Shutdown m = new Shutdown(newq);
						Runtime.getRuntime().addShutdownHook(m);
					}
				}
			}
		}
	}
	
	
	/**
	 * Initiate a global shutdown
	 */
	public synchronized void setQuitting(boolean quitting){
		
		/* Show who called the shutdown */
		RuntimeException e = new RuntimeException("dummy");
		StackTraceElement[] st = e.getStackTrace();
		StringBuffer sb = new StringBuffer();
		for(int i = 0 ; i< st.length; i++){
			sb.append(st[i].toString()+"\n");
		}
		getLog().debug("Here's who shut us down\n"+sb);
		
		if(shuttingDown == false){
			if(quitting == true){
				shuttingDown = true;
				for(Quittable q: quittables){
					q.setQuitting(true);
				}
				quittables.clear();
			}
		}
		else{
			if(quitting == false){
				getLog().fatal("Trying to undo a shutdown! Can't do that");
			}
			else{
				getLog().warn("Trying to shutdown twice! Can't do that");
			}
		}
	}
	
	public synchronized boolean isQuitting(){
		return shuttingDown;
	}

	
	public boolean isTesting(){
		return testing;
	}
	
	public void setTesting(Boolean testing){
		if(testing == null){
			this.testing = true;
		}
		else{
			this.testing = testing;
		}
	}
	
	public Calendar getCalendar(TimeZone tz){
		return calendarCache.getCalendar(tz);
	}
	
	public abstract String getSystemVersion();

}
