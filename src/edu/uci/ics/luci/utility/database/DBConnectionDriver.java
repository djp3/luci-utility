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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnectionDriver implements Driver{
    private static final int MAJOR_VERSION = 5;
    private static final int MINOR_VERSION = 6;
    
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(DBConnectionDriver.class);
		}
		return log;
	}
    
    private String URL_PREFIX = null;
    private DBConnectionPool pool;

    public DBConnectionDriver(String driver, String url,  String user, String password,Integer warmItUp,Integer numberOfHotStandbys)  throws ClassNotFoundException,  InstantiationException, IllegalAccessException, SQLException {
    	URL_PREFIX = "jdbc:mysql:pool:"+url;
        DriverManager.registerDriver(this);
        Class.forName(driver).newInstance();
        
        pool = new DBConnectionPool(url, user, password,warmItUp,numberOfHotStandbys).launch();
    }

    public synchronized Connection connect(String url, Properties props)  throws SQLException {
        if(!acceptsURL(url)) {
             return null;
        }
        /*Could occur if shutdown happens before warmup is complete */
        if(pool != null){
        	return pool.getSoftConnection();
        }
        return null;
    }

    public synchronized boolean acceptsURL(String url) {
        return url.startsWith(URL_PREFIX);
    }

    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public DriverPropertyInfo[] getPropertyInfo(String str, Properties props) {
        return new DriverPropertyInfo[0];
    }
    
    public synchronized Integer poolSize(){
    	return pool.poolSize();
    }

    public boolean jdbcCompliant() {
        return false;
    }
    
    public synchronized void shutdown(){
    	try{
    		if(pool != null){
    			pool.shutdown();
    		}
    	}catch(Exception e){
    		getLog().error(e.toString());
    	}
    	finally{
    		pool = null;
    		try {
    			DriverManager.deregisterDriver(this);
    		} catch (SQLException e) {
    			getLog().error(e.toString());
    		}
    		finally{
    			getLog().info("DBConnectionDriver shutdown");
    		}
    	}
    }

	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("getParentLogger() not supported");
	}
}

