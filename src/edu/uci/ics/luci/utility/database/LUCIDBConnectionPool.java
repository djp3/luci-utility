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

package edu.uci.ics.luci.utility.database;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Quittable;

public class LUCIDBConnectionPool implements Quittable{
	
	public static final Integer DEFAULT_WARM_UP_SIZE = 25;
	private static final Integer DEFAULT_HOT_STANDBY_SIZE = 10;
	//private static Random r = new Random();
	private static long totalCount = 0;

	private DBConnectionDriver dbcd = null;
	private String databaseDomain = null;
	private String database = null;

	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(LUCIDBConnectionPool.class);
		}
		return log;
	}
	
	public synchronized static long getTotalConnections() {
		return totalCount;
	}
	
	private synchronized static void incrementTotalCount() {
		totalCount++;
	}
	
	public Integer getPoolSize(){
		return(dbcd.poolSize());
	}

	public String getDatabaseDomain() {
		return databaseDomain;
	}

	public void setDatabaseDomain(String databaseDomain) {
		this.databaseDomain = databaseDomain;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDatabaseURL() {
		return "//" + databaseDomain + "/" + database;
	}

	public void setDatabaseURL(String databaseURL, String database) {
		this.databaseDomain = databaseURL;
		this.database = database;
	}

	public LUCIDBConnectionPool(String databaseDomain, String database, String username, String password) {
		this(databaseDomain,database,username,password,DEFAULT_WARM_UP_SIZE,DEFAULT_HOT_STANDBY_SIZE);
	}


	/**
	 * 
	 * @param database
	 * @param username
	 * @param password
	 * @param databaseURL
	 */
	public LUCIDBConnectionPool(String databaseDomain, String database, String username, String password,Integer warmItUp,Integer numberOfHotStandbys) {
		
		try {
			setDatabaseURL(databaseDomain, database);
			dbcd = new DBConnectionDriver("com.mysql.jdbc.Driver", getDatabaseURL(), username, password,warmItUp,numberOfHotStandbys);
		} catch (ClassNotFoundException e) {
			getLog().error("Unable to start LUCIDBConnection pool:"+e);
		} catch (InstantiationException e) {
			getLog().error("Unable to start LUCIDBConnection pool:"+e);
		} catch (IllegalAccessException e) {
			getLog().error("Unable to start LUCIDBConnection pool:"+e);
		} catch (SQLException e) {
			getLog().error("Unable to start LUCIDBConnection pool:"+e);
		} catch (RuntimeException e) {
			getLog().error("Unable to start LUCIDBConnection pool:"+e);
		}

	}

	public DBConnection getConnection() {
		incrementTotalCount();
		DBConnection c = null;
		try {
			c = (DBConnection) DriverManager.getConnection("jdbc:mysql:pool:" + getDatabaseURL());
		} catch (SQLException e) {
			getLog().error("Unable to get a connection from the pool,"+e.toString());
		}
		
		if((c != null) && (!c.validate())){
			getLog().fatal("We should never be giving back invalid connections that aren't null");
			c = null;
		}
		
		return(c);
	}
	

	public void shutdown() {
		if (dbcd != null) {
			try {
				dbcd.shutdown();
			} finally {
				dbcd = null;
			}
		}
	}

	private boolean quitting = false;
	public void setQuitting(boolean q) {
		if(q == true){
			quitting = true;
			shutdown();
		}
	}
	
	public boolean isQuitting(){
		return(quitting);
	}

}
