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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

public class DBConnectionPool {
	public static final String URL_PREFIX = "jdbc:mysql:";
	private static final int POOL_SOFT_LIMIT = 400;
	private static final int POOL_HARD_LIMIT = 500;
	private static long timeout = 60*1000; /*One minute*/
	
	private Vector<DBConnection> connections;
	
	private String url, user, password;
	
	private Integer hotStandby;
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(DBConnectionPool.class);
		}
		return log;
	}
	
	private ConnectionReaper reaper;
	
	class ConnectionReaper extends Thread {
	    static private final long delay = 120000L;
	    
	    private DBConnectionPool pool;
	    private boolean quitting = false;

	    ConnectionReaper(DBConnectionPool pool) {
	        this.pool = pool;
	    }
	    
	    public synchronized void setQuitting(boolean q){
	    	quitting = q;
	    	this.notifyAll();
	    }
	    
	    public synchronized void run() {
	        while(!quitting) {
	        	DBConnectionPool.getLog().debug("Connection Reaper reaping connections");
	        	pool.reapConnections();
	        	checkForHotStandby();
	        	try {
        		   if(!quitting){
        			   wait(delay);
	        	   }
	           } catch( InterruptedException e) { }
	        }
	    }
	}



	/**
	 * This needs to be explicitly "launched" after being called
	 * @param url
	 * @param user
	 * @param password
	 * @param warmItUp
	 * @param numberOfHotStandbys
	 */
	public DBConnectionPool(String url, String user, String password,Integer warmItUp,Integer numberOfHotStandbys) {
		
		this.url = url;
		this.user = user;
		this.password = password;
		
		if((numberOfHotStandbys != null) && (numberOfHotStandbys > 0)){
			this.hotStandby = numberOfHotStandbys;
		}
		else{
			this.hotStandby = 0;
		}
		
		connections =  new Vector<DBConnection>(this.hotStandby);
		
		getLog().info("Warming up with:"+warmItUp);
		if((warmItUp != null) && (warmItUp > 0)){
			checkForHotStandby(warmItUp);
		}
		
		getLog().info("Making hot standby's with:"+this.hotStandby);
		checkForHotStandby(this.hotStandby);
		
		reaper = new ConnectionReaper(this);
		reaper.setName("Connection Reaper");
		reaper.setDaemon(false); /*Force an intentional/clean shutdown*/
	}
	
	public DBConnectionPool launch(){
		reaper.start();
		return this;
	}

	protected static synchronized long getTimeout() {
		return timeout;
	}

	protected static synchronized void setTimeout(long timeout) {
		DBConnectionPool.timeout = timeout;
	}
	
	public synchronized Integer poolSize(){
		return(connections.size());
	}

	public synchronized void closeConnections() {
		if(connections != null){
			for(DBConnection conn:connections){
				try {
					if(conn != null){
						conn.close();
					}
				} catch (SQLException e) {
					getLog().error("Problem closing connection",e);
				}
			}
		}
	}
	
	private synchronized void hardCloseConnection(DBConnection conn){
		if(conn != null){
			try {
				/*Soft close the connection */
				conn.close();
			} catch (SQLException e) {
				getLog().error("Database error while soft closing a connection",e);
			}
			finally{
				if(conn.getConnection() != null){
					try{
						/* Hard destroy the underlying connection */
						conn.getConnection().close();
					} catch (SQLException e) {
						getLog().error("Database error while hard closing a connection:",e);
					}
				}
			}
			connections.remove(conn);
		}
	}

	/** Close all open underlying connections in the pool.  Written for shutdown sequence. **/
	protected synchronized void hardCloseConnections(){
		if(connections != null){
			
			int count = connections.size();
			
			/*Avoid a concurrent modification exception */
			Vector<DBConnection> connectionsCopy = new Vector<DBConnection>(connections.size());
			connectionsCopy.addAll(connections);
			for(DBConnection conn:connectionsCopy){
				hardCloseConnection(conn);
			}
			getLog().debug("Hard reaped "+(count-connections.size())+" connections. "+connections.size()+" connections are left.");
		}
	}
	
	/*Remove connections from the pool */
	public synchronized void reapConnections() {
		
		if(connections != null){
			long staleTime = System.currentTimeMillis() - timeout;
			
			int total = 0;
			int inuse = 0;
			int stale = 0;
			int notinuse = 0;
			int notinuse_stale = 0;
			int notinuse_reduced = 0;
			int invalid = 0;
			/*Avoid a concurrent modification exception */
			Vector<DBConnection> connectionsCopy = new Vector<DBConnection>(connections.size());
			connectionsCopy.addAll(connections);
			for(DBConnection conn:connectionsCopy){
				total++;
				if(conn.inUse()){
					inuse++;
					if(staleTime > conn.getLastUse()){
						stale++;
						if(conn.validate()){
							getLog().info("I've got an old connection lying around with this stack trace:\n"+conn.getStackTrace());
						}
						else{
							invalid++;
							hardCloseConnection(conn);
						}
					}
				}
				else{
					notinuse++;
					if(staleTime > conn.getLastUse()){
						notinuse_stale++;
						if(notinuse > this.hotStandby){
							notinuse_reduced++;
							hardCloseConnection(conn);
						}
					}
				}
			}
			getLog().debug(total+" connections found in the pool, inuse:"+inuse+"(stale:"+stale+",invalid/reaped:"+invalid+"),not inuse:"+notinuse+"(stale:"+notinuse_stale+",over hotstandby/reaped:"+notinuse_reduced+"), hot standby:"+this.hotStandby+",ended with:"+connections.size());
		}
		else{
			getLog().fatal("Why is the DBConnection Pool missing a datastructure?");
		}
	}
	
	private synchronized void checkForHotStandby(){
		checkForHotStandby(this.hotStandby);
		
	}
	/** Make sure there are enough connections in the pool **/
	private synchronized void checkForHotStandby(Integer numberToKeepHot){
		if((connections != null) && (numberToKeepHot != null)){
			int inuse = 0;
			//int notinuse=0;
			for(DBConnection conn:connections){
				if(conn.inUse()){
					inuse++;
				}
				else{
			//		notinuse++;
				}
			}
			
			/* Hit the database so we don't time out */
			/*
			if(inuse == 0){
				try {
					Connection c = getSoftConnection();
					PreparedStatement ps = c.prepareStatement("SELECT 1");
					ps.executeQuery();
				} catch (SQLException e) {
				}
			}*/
			
			List<DBConnection> cList = new ArrayList<DBConnection>();
			List<PreparedStatement> psList = new ArrayList<PreparedStatement>();
			try {
				int connectionsNeeded = (numberToKeepHot - (connections.size()-inuse)); 
				while(connectionsNeeded > 0){
					getLog().debug("Required Hot: "+numberToKeepHot+", size: "+connections.size()+", inuse:"+inuse+", needed: "+connectionsNeeded);
					for(int i = 0;i < connectionsNeeded;i++){
						getLog().debug("Trying to make a connection for hot standby, available pool size is "+(connections.size()-inuse)+"/"+connections.size()+" < "+numberToKeepHot);
						//c[i] = getSoftConnection(); //Why soft and not hard?
						DBConnection c = getHardConnection(); 
						if(c != null){
							cList.add(c);
						}
						
						ResultSet dummy=null;
						try{
							if( c == null){
								getLog().error("Unable to make a connection for hot standby, available pool size is "+(connections.size()-inuse)+"/"+connections.size()+" < "+numberToKeepHot);
							}
							else{
								try {
									PreparedStatement ps = c.prepareStatement("SELECT CURRENT_TIMESTAMP");
									psList.add(ps);
									dummy = ps.executeQuery();
								} catch (SQLException e) {
									getLog().error("Unable to execute a statement to make hot standbys, pool size is "+connections.size()+" < "+numberToKeepHot);
								}
							}
						}
						finally{
							if(dummy != null){
								try {
									dummy.close();
								} catch (SQLException e) {
								}
								finally{
									dummy = null;
								}
							}
						}
					}
					connectionsNeeded = (numberToKeepHot - (connections.size()-inuse)); 
				}
			//} catch (SQLException e) {
			//	getLog().log(Level.ERROR, "Unable to make a connection for hot standby, available pool size is "+(connections.size()-inuse)+"/"+connections.size()+" < "+numberToKeepHot);
			} catch (RuntimeException e) {
				getLog().error("Problem in checkForHotStandby:"+e);
			}
			finally{
				for(PreparedStatement ps:psList){
					if(ps != null){
						try {
							ps.close();
						} catch (SQLException e) {
						}
					}
				}
				for(DBConnection c:cList){
					if(c != null){
						try {
							c.close();
						} catch (SQLException e) {
						}
					}
				}
				getLog().debug("Made a required connection for hot standby/warm up, pool size is "+connections.size()+", require: "+numberToKeepHot);
			}
		}
	}
	
	protected synchronized DBConnection getHardConnection(){
		DBConnection dbc = null;
		Properties p = new Properties();
		
		if((connections != null) && (p != null)){
			p.setProperty("user", user);
			p.setProperty("password", password);
	
			if(connections.size() > POOL_SOFT_LIMIT){
				getLog().error("The number of connections in the database pool is "+connections.size()+", greater than "+POOL_SOFT_LIMIT);
			}
	
			if(connections.size() > POOL_HARD_LIMIT){
				getLog().fatal("The number of connections in the database pool is "+connections.size()+", greater than "+POOL_HARD_LIMIT+". Failing to deliver a connection.");
			}
		
			Connection conn = null;
			try {
				conn = DriverManager.getConnection(URL_PREFIX+url, p);
			} catch (CommunicationsException e){
				getLog().error("Is your database running?  Is it accessible? "+URL_PREFIX+url+"\n"+e);
				return(null);
			
			} catch (SQLException e) {
				getLog().error("Unable to get a hard connection from the database: "+e);
			}
			
			if(conn != null){
				dbc  = new DBConnection(conn);
			}
			
			if(dbc != null){
				dbc.lease();
				connections.add(dbc);
			}
		}
		return(dbc);
	}


	public synchronized DBConnection getSoftConnection() throws SQLException {
		DBConnection dbc = null;
		boolean invokereaper = false;
		Vector<DBConnection> removeUs = null;
		
		if(connections != null){
			removeUs = new Vector<DBConnection>(connections.size());
			for(DBConnection c:connections){
				if ((dbc == null) && (c.lease())) {
					if(c.validate()){
						dbc = c;
					}
					else{
						removeUs.add(c);
					}
				}
			}
			
			for(DBConnection c:removeUs){
				hardCloseConnection(c);
				/* Removing bad connections requires making sure there are enough good ones around */
				invokereaper = true;
			}
			
			if(dbc == null){
				dbc = getHardConnection();
				if(this.hotStandby > 0){
					/* We want a hot standby available, but we must be so busy they are taken
					 *  tell the reaper to wake up to make some new ones*/
					invokereaper = true;
				}
			}
		}
		
		if(invokereaper){
			reaper.notifyAll();
		}
		
		return dbc;
	} 

	/*
	public synchronized void returnConnection(DBConnection conn) {
		conn.expireLease();
	}
	*/
	
	public synchronized void shutdown(){
		
		try{
			if(reaper != null){
				reaper.setQuitting(true);
			}
			
			/*Soft close connections */
			closeConnections();
			
			/*Hard close connections */
			hardCloseConnections();
		
			if(connections != null){
				connections.clear();
			}
		}
		finally{
			reaper = null;
			connections = null;
		}
	}

	protected void finalize() throws Throwable{
		try{
			shutdown();
		}
		catch(Exception e){
			getLog().error(e.toString());
		}
		finally{
			super.finalize();
		}
	}
}
