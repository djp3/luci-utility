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

package edu.uci.ics.luci.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Globals implements Quittable{
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Globals.class);
		}
		return log;
	}
	
	private static final String LOG4J_CONFIG_FILE_DEFAULT = "luci-utility.log4j.xml";
	
	public static String getLog4JPropertyFileName() {
		return System.getProperty("log4j.configurationFile");
	}
	
	public static void setLog4JPropertyFileName(String propertyFileName) {
		System.setProperty("log4j.configurationFile",propertyFileName);
	}
	
	static protected Globals _globals = null;
	
	public static synchronized Globals getGlobals(){
		return _globals;
	}
	
	public static synchronized Globals setGlobals(Globals _globals) {
		return(Globals._globals = _globals);
	}
	
	private String databaseDomain = "localhost";
	
	private Shutdown quittables = null;
	private CalendarCache calendarCache = null;
	
	private boolean shuttingDown = false;
	private boolean testing = true;
	
	protected Globals(){
		super();
		setLog4JPropertyFileName(LOG4J_CONFIG_FILE_DEFAULT);
		getLog().trace("Static Evaluation of "+Globals.class.getCanonicalName()+" complete");
		
		calendarCache = new CalendarCache(CalendarCache.TZ_GMT);
		
		quittables = new Shutdown(new ArrayList<Quittable>());
		Thread t = new Thread(quittables);
		t.setName("ShutdownHook Shutdown Thread");
		t.setDaemon(false);
		Runtime.getRuntime().addShutdownHook(t);
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
	
	public List<String> getBadGuyList() {
		return(new ArrayList<String>());
	}
	
	public synchronized void addQuittable(Quittable q){
		if(q != null){
			if(shuttingDown){
				synchronized(q){
					q.setQuitting(true);
				}
			}
			else{
				synchronized(quittables){
					synchronized(q){
						this.quittables.add(q);
					}
				}
			}
		}
	}
	
	
	/**
	 * Initiate a global shutdown and block till done
	 */
	public synchronized void setQuitting(boolean quitting){
		setQuitting(quitting, true);
	}
	
	/**
	 * Initiate a global shutdown and possibly block 
	 */
	public synchronized void setQuitting(boolean quitting, boolean blockTillDone){
		
		if(shuttingDown == false){
			if(quitting == true){
				
				/* Show who called the shutdown */
				RuntimeException e = new RuntimeException("dummy");
				StackTraceElement[] st = e.getStackTrace();
				StringBuffer sb = new StringBuffer();
				for(int i = 0 ; i< st.length; i++){
					sb.append(st[i].toString()+"\n");
				}
				getLog().trace("Here's who shut us down\n"+sb);
				
				Thread t;
				synchronized(quittables){
					shuttingDown = true;
					t = new Thread(quittables);
					t.setName("Shutdown Thread");
					t.setDaemon(false);
					t.start();
				}
				if(blockTillDone){
					while(t.isAlive()){
						try {
							t.join();
						} catch (InterruptedException e1) {
						}
					}
				}
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
