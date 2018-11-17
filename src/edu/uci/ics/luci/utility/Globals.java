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

package edu.uci.ics.luci.utility;

import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import edu.uci.ics.luci.utility.webserver.event.api.login.DatastoreSQLite;


/**
 * This is a Globals class that follows the singleton pattern (sort of) for maintaining
 * global variables for the Utilities.
 * 
 * The main deviation is that this is an abstract class, so the singleton needs to be set by an
 * implementing class before there will be one to get. Something like Globals.setGlobals(new MyGlobals())
 * 
 * This class by default manages
 * 	 whether or not the system is in testing mode
 *   whether or not the system is quitting
 *   the things that need to be quit when the system shuts down.
 *   a global Calendar cache
 *   a system version
 *   a database connection
 * 
 * @author djp3
 *
 */
public abstract class Globals implements Quittable{
	
	/* Class logger */
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(Globals.class);
		}
		return log;
	}
	
	/* Convenience constants */
	static final long ONE_SECOND = 1000;
	static final long ONE_MINUTE = 60 * ONE_SECOND;
	
	/* Source of randomness for the whole app */
	protected static final Random random = new Random(System.currentTimeMillis() - 93845);
	
	/* Singleton variable */
	private static Globals singleton = null;
	
	static{
		DatastoreSQLite.initializeSQLite();
	}
	
	/**
	 * This method provides access to the global singleton
	 * @return the current Globals singleton object or null if it has not been set
	 */
	public static synchronized Globals getGlobals(){
		return singleton;
	}
	
	/**
	 * Replace the current global singleton with a new one
	 * @param g
	 * @return
	 */
	public static synchronized void setGlobals(Globals g) {
		singleton = g;
	}
	
	/**
	 * Access to the global source of randomness
	 * @return a singleton Random object
	 */
	public static synchronized Random getRandom() {
		return random;
	}
	
	
	/*** Class variables ***/
	private boolean shuttingDown = false;
	private boolean testing = true;
	
	private Shutdown shutUsDown = null;
	
	private CalendarCache calendarCache = null;
	
	private String databaseDomain = "localhost";
	
	
	
	/**
	 * Constructor for the singleton class
	 */
	protected Globals(){
		super();
		/* Set up the logging utility information based on abstract methods*/
		setLog4JPropertyFileName(getLog4JPropertyFileName());
		
		/* Set up the calendar cache */
		calendarCache = CalendarCache.getCalendarCache();
		
		/* Create an system for automatically shutting down classes on hard quit */
		shutUsDown = new Shutdown();
		Thread t = new Thread(shutUsDown);
		t.setName("ShutdownHook Shutdown Thread");
		t.setDaemon(false);
		Runtime.getRuntime().addShutdownHook(t);
		
		/* Test that we are using UTC as the default */
		if (!TimeZone.getDefault().equals(CalendarCache.TZ_UTC)) {
			throw new RuntimeException("We are in the wrong timezone:\n" + TimeZone.getDefault()
					+ "\n We want to be in:\n " + CalendarCache.TZ_UTC);
		}

		/* Test that we are using UTF-8 as default */
		String c = java.nio.charset.Charset.defaultCharset().name();
		if (!c.equals("UTF-8")) {
			throw new IllegalArgumentException("The character set is not UTF-8:" + c);
		}
		
		Security.addProvider(new BouncyCastleJsseProvider());		
		

		String s = Security.getProperty("jdk.certpath.disabledAlgorithms");
		if(	s == null || !s.contains("SHA1") ) {
				throw new IllegalArgumentException("Security is not strong enough, add \"jdk.certpath.disabledAlgorithms=SHA1\" to VM Security parameters " + s);
		}
	}
	
	public String getDatabaseHost() {
		return databaseDomain;
	}
	
	public void setDatabaseDomain(String databaseDomain) {
		this.databaseDomain = databaseDomain;
	}
	
	public String getDefaultDatabaseDomain() {
		return databaseDomain;
	}
	
	/**
	 * A list of hosts to block from webserver accesses
	 * @return The list of hosts to block
	 */
	public List<String> getBadGuyList() {
		return(new ArrayList<String>());
	}
	
	
	
	/***   Support for clean global shutdowns ***/
	
	/**
	 * Add something to the list of things to be notified when the system is shutting down
	 * @param q an object to be notified
	 */
	public synchronized void addQuittable(Quittable q){
		if(q != null){
			if(shuttingDown){
				synchronized(q){
					q.setQuitting(true);
				}
			}
			else{
				this.shutUsDown.add(q);
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
					sb.append("\t"+st[i].toString()+"\n");
				}
				getLog().trace("Here's who shut us down\n"+sb);
				
				Thread t;
				synchronized(shutUsDown){
					shuttingDown = true;
					t = new Thread(shutUsDown);
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
				getLog().info("Trying to shutdown twice! No harm done...");
			}
		}
	}
	
	public synchronized boolean isQuitting(){
		return shuttingDown;
	}

	/** Global testing setting management **/
	
	public boolean isTesting(){
		return testing;
	}
	
	public void setTesting() {
		setTesting(true);
	}
	
	public void setTesting(boolean testing){
		this.testing = testing;
	}
	
	/******** Global calendar management *******/
	
	public Calendar getCalendar(String tz){
		return calendarCache.getCalendar(tz);
	}
	
	public Calendar getCalendar(TimeZone tz){
		return calendarCache.getCalendar(tz.getID());
	}
	
	
	/*******  Log4j support *******/
	/**
	 * a method that points the class to the correct file location for the 
	 * logging configuration.
	 * See GlobalsTest for a possible implementation
	 * @return The file name of the log4J configuration file
	 */
	public abstract String getLog4JPropertyFileName();
	
	/**
	 * a method that sets correct file location for the 
	 * log4j logging configuration.
	 * @param propertyFileName The file name of the log4J configuration file
	 */
	protected void setLog4JPropertyFileName(String propertyFileName) {
		/* This sets all loggers to be asynchronous for performance */
		/* See https://logging.apache.org/log4j/2.x/manual/async.html */
		System.setProperty("log4j2.contextSelector","org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
		/* This is where log4j looks for it's configuration file name */
		System.setProperty("log4j.configurationFile",propertyFileName);
	}
	
	/**
	 * a method that returns some representation of the system version
	 * @return the system version
	 */
	public abstract String getSystemVersion();

}
