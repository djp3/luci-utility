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

package edu.uci.ics.luci.utility.database;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnection implements Connection{

    private Connection conn;
    private boolean inuse;
    
	private long timestamp;
	
	private String stackTrace;

	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(DBConnection.class);
		}
		return log;
	}
	
	private synchronized void setInUse(boolean inuse) {
		this.inuse = inuse;
	}

    public synchronized boolean inUse() {
        return inuse;
    }

    public DBConnection(Connection conn) {
        this.conn=conn;
        setInUse(false);
        this.timestamp=0;
    }

    public synchronized boolean lease() {
       if(this.inUse())  {
           return false;
       } else {
    	   setInUse(true);
    	   timestamp=System.currentTimeMillis();
    	   return true;
       }
    }
    
    public synchronized boolean validate() {
    	try {
            conn.getMetaData();
            return(conn.isValid(0));
        }catch (Exception e) {
        	return false;
        }
    }
    
    public synchronized long getLastUse() {
        return timestamp;
    }

    public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	/** soft close **/
    public synchronized void close() throws SQLException {
    	this.expireLease();
    }

    protected synchronized void expireLease() {
        setInUse(false);
    }

    protected synchronized Connection getConnection() {
        return conn;
    }
    
    protected synchronized void setConnection(Connection c) {
        this.conn = c;
    }

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
    public synchronized PreparedStatement prepareStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    public synchronized CallableStatement prepareCall(String sql) throws SQLException {
        return conn.prepareCall(sql);
    }

    public synchronized Statement createStatement() throws SQLException {
        return conn.createStatement();
    }

    public synchronized String nativeSQL(String sql) throws SQLException {
        return conn.nativeSQL(sql);
    }

    public synchronized void setAutoCommit(boolean autoCommit) throws SQLException {
        conn.setAutoCommit(autoCommit);
    }

    public synchronized boolean getAutoCommit() throws SQLException {
        return conn.getAutoCommit();
    }

    public synchronized void commit() throws SQLException {
        conn.commit();
    }

    public synchronized void rollback() throws SQLException {
        conn.rollback();
    }

    public synchronized boolean isClosed() throws SQLException {
        return conn.isClosed();
    }

    public synchronized DatabaseMetaData getMetaData() throws SQLException {
        return conn.getMetaData();
    }

    public synchronized void setReadOnly(boolean readOnly) throws SQLException {
        conn.setReadOnly(readOnly);
    }
  
    public synchronized boolean isReadOnly() throws SQLException {
        return conn.isReadOnly();
    }

    public synchronized void setCatalog(String catalog) throws SQLException {
        conn.setCatalog(catalog);
    }

    public synchronized String getCatalog() throws SQLException {
        return conn.getCatalog();
    }

    public synchronized void setTransactionIsolation(int level) throws SQLException {
        conn.setTransactionIsolation(level);
    }

    public synchronized int getTransactionIsolation() throws SQLException {
        return conn.getTransactionIsolation();
    }

    public synchronized SQLWarning getWarnings() throws SQLException {
        return conn.getWarnings();
    }

    public synchronized void clearWarnings() throws SQLException {
        conn.clearWarnings();
    }

	public synchronized Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return(conn.createStatement(resultSetType,resultSetConcurrency));
	}

	public synchronized Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return(conn.createStatement(resultSetType,resultSetConcurrency,resultSetHoldability));
	}

	public synchronized int getHoldability() throws SQLException {
		return(conn.getHoldability());
	}

	public synchronized Map<String, Class<?>> getTypeMap() throws SQLException {
		return conn.getTypeMap();
	}

	public synchronized CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return conn.prepareCall(sql,resultSetType,resultSetConcurrency);
	}

	public synchronized CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return conn.prepareCall(sql,resultSetType,resultSetConcurrency,resultSetHoldability);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
	public synchronized PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		return conn.prepareStatement(sql, autoGeneratedKeys);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
	public synchronized PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		return conn.prepareStatement(sql, columnIndexes);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
	public synchronized PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		return conn.prepareStatement(sql, columnNames);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
	public synchronized PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return conn.prepareStatement(sql, resultSetType,resultSetConcurrency);
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"}, justification="The warning should be for the calling code")
	public synchronized PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return conn.prepareStatement(sql, resultSetType,resultSetConcurrency,resultSetHoldability);
	}

	public synchronized void releaseSavepoint(Savepoint savepoint) throws SQLException {
		conn.releaseSavepoint(savepoint);
	}

	public synchronized void rollback(Savepoint savepoint) throws SQLException {
		conn.rollback(savepoint);
	}

	public synchronized void setHoldability(int holdability) throws SQLException {
		conn.setHoldability(holdability);
	}

	public synchronized Savepoint setSavepoint() throws SQLException {
		return conn.setSavepoint();
	}

	public synchronized Savepoint setSavepoint(String name) throws SQLException {
		return conn.setSavepoint(name);
	}

	public synchronized void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		conn.setTypeMap(map);
	}
	
	public boolean isValid(int arg0) throws SQLException {
		return conn.isValid(arg0);
	}

	public synchronized Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public synchronized Blob createBlob() throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public synchronized Clob createClob() throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public NClob createNClob() throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public SQLXML createSQLXML() throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public Properties getClientInfo() throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public String getClientInfo(String arg0) throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}


	public void setClientInfo(Properties arg0) throws SQLClientInfoException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
		
	}

	public void setClientInfo(String arg0, String arg1)
			throws SQLClientInfoException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
		
	}

	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		getLog().error("Unimplemented Method");
		throw new RuntimeException("unimplemented method");
	}

	/*
	 * Needed in Java 1.7, but we are standardizing on 1.6 for Android compatibility*/
	/*
	public void setSchema(String schema) throws SQLException {
		conn.setSchema(schema);
	}

	public String getSchema() throws SQLException {
		return(conn.getSchema());
	}

	public void abort(Executor executor) throws SQLException {
		conn.abort(executor);
	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		conn.setNetworkTimeout(executor, milliseconds);
	}

	public int getNetworkTimeout() throws SQLException {
		return conn.getNetworkTimeout();
	}
	*/

}

