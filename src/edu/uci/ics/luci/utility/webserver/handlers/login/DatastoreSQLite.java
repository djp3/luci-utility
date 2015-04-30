package edu.uci.ics.luci.utility.webserver.handlers.login;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

public class DatastoreSQLite implements Datastore {

	private static boolean OSX_64 = true;
	private SQLiteQueue queue;

	static {
		if (OSX_64) {
			SQLite.setLibraryPath("/Users/djp3/.m2/repository/com/almworks/sqlite4java/libsqlite4java-osx/1.0.392/");
		}
	}

	private static transient volatile Logger log = null;

	public static Logger getLog() {
		if (log == null) {
			log = LogManager.getLogger(DatastoreSQLite.class);
		}
		return log;
	}

	public DatastoreSQLite(String path) {
		this.queue = new SQLiteQueue(new File(path));
	}

	public boolean isStopped() {
		return this.queue.isStopped();
	}

	public void start() {
		this.queue.start();
	}

	public void stop(boolean gracefully) {
		this.queue.stop(gracefully);
	}

	@Override
	public boolean isThreadSafe() {
		try {
			return SQLite.isThreadSafe();
		} catch (SQLiteException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean createTable(String name, Set<String> uniques,
			Map<String, ColumnProperty> columns) {
		final StringBuffer query = new StringBuffer();

		query.append("CREATE TABLE \"" + name + "\" (");

		boolean first = true;
		for (Entry<String, ColumnProperty> e : columns.entrySet()) {
			// add a comma
			if (!first) {
				query.append(",");
			} else {
				first = false;
			}

			// put in the column name escaped
			query.append("\"" + e.getKey() + "\" ");

			// add the properties
			switch (e.getValue().type) {
			case Integer:
				query.append("INTEGER");
				break;
			case Real:
				query.append("NUMERIC");
				break;
			case Text:
				query.append("TEXT");
				break;
			default:
				break;
			}
		}
		if (uniques != null) {
			if (first) {
				getLog().error("Can't make a column that isn't defined unique");
				return false;
			}
			for (String u : uniques) {
				if (!columns.containsKey(u)) {
					getLog().error(
							"Can't make a column that isn't defined unique");
					return false;
				}
			}
			query.append(", UNIQUE(");
			first = true;
			for (String u : uniques) {
				if (!first) {
					query.append(",");
				} else {
					first = false;
				}
				query.append("\"" + u + "\"");
			}
			query.append(")");
		}

		query.append(" ON CONFLICT FAIL)");
		//getLog().error(query.toString());

		Boolean ret;
		try {
			ret = queue.execute(new SQLiteJob<Boolean>() {
				protected Boolean job(SQLiteConnection connection)
						throws SQLiteException {
					connection.exec(query.toString());
					return true;
				}
			}).get();
		} catch (InterruptedException e1) {
			getLog().error(e1.getLocalizedMessage());
			return false;
		} catch (ExecutionException e1) {
			getLog().error(e1.getLocalizedMessage());
			return false;
		}
		return (ret == null ? false : ret);
	}

	public boolean tableExists(final String name) {

		final StringBuffer query = new StringBuffer();
		query.append("SELECT name FROM sqlite_master WHERE type='table' AND name=?");

		Boolean ret;
		try {
			ret = queue.execute(new SQLiteJob<Boolean>() {
				protected Boolean job(SQLiteConnection connection) {
					SQLiteStatement st = null;
					try {
						st = connection.prepare(query.toString());
						st.bind(1, name);
						if (!st.step()) {
							return false;
						} else {
							st.reset();
						}
					} catch (SQLiteException e) {
						getLog().error(e.getLocalizedMessage());
						return false;
					} finally {
						if (st != null) {
							st.dispose();
						}
					}
					return true;
				}
			}).get();
		} catch (InterruptedException e) {
			getLog().error(e.getLocalizedMessage());
			return false;
		} catch (ExecutionException e) {
			getLog().error(e.getLocalizedMessage());
			return false;
		}

		return ((ret == null) ? false : ret);
	}

	public boolean deleteTable(final String name) {

		final StringBuffer query = new StringBuffer();
		query.append("DROP TABLE IF EXISTS \"" + name + "\"");

		Boolean ret;
		try {
			ret = queue.execute(new SQLiteJob<Boolean>() {
				protected Boolean job(SQLiteConnection connection)
						throws SQLiteException {
					connection.exec(query.toString());
					return true;
				}
			}).get();
		} catch (InterruptedException e1) {
			getLog().error(e1.getLocalizedMessage());
			return false;
		} catch (ExecutionException e1) {
			getLog().error(e1.getLocalizedMessage());
			return false;
		}
		return (ret == null ? false : ret);
	}
	
	
	public void addRow(String tableName, Map<String, Object> row) {
		final StringBuffer query = new StringBuffer();
		query.append("INSERT INTO \"" + tableName + "\" (");
		boolean first = true;
		for (Entry<String, Object> e : row.entrySet()) {
			if (!first) {
				query.append(",");
			} else {
				first = false;
			}
			query.append("\""+e.getKey()+"\"");
		}
		query.append(") VALUES(");
		first = true;
		for (Entry<String, Object> e : row.entrySet()) {
			if (!first) {
				query.append(",");
			} else {
				first = false;
			}
			if(e.getValue().getClass().equals(Integer.class)){
				query.append(e.getValue());
			}
			else if(e.getValue().getClass().equals(Double.class)){
				query.append(e.getValue());
			}
			else if(e.getValue().getClass().equals(String.class)){
				query.append("\""+e.getValue()+"\"");
			}
			else{
				getLog().fatal("problem here");
			}
		}
		query.append(")");
		
		//getLog().error(query.toString());

		try {
			queue.execute(new SQLiteJob<Integer>() {
				protected Integer job(SQLiteConnection connection)
						throws SQLiteException {
					SQLiteStatement st = connection.prepare(query.toString());
					try {
						return st.step() ? st.columnInt(0) : null;
					} finally {
						st.dispose();
					}
				}
			}).get();
		} catch (InterruptedException e1) {
			getLog().error(e1.getLocalizedMessage());
		} catch (ExecutionException e1) {
			getLog().error(e1.getLocalizedMessage());
		}
	}
	
	public Map<String,Object> getRow(String tableName, Set<String> fields, final Map<String, Object> matching) {
		Map<String,Object> ret = new HashMap<String,Object>();
		
		final StringBuffer query = new StringBuffer();
		query.append("SELECT ");
		boolean first = true;
		for (String e : fields) {
			if (!first) {
				query.append(",");
			} else {
				first = false;
			}
			query.append("\""+e+"\"");
		}
		query.append(" FROM \""+tableName+"\"");
		if(matching != null){
			query.append(" WHERE ");
			first = true;
			for (Entry<String, Object> e : matching.entrySet()) {
				if (!first) {
					query.append(" AND ");
				} else {
					first = false;
				}
				query.append("\""+e.getKey()+"\"=?");
			}
		}
		
		//getLog().error(query.toString());

		try {
			ret = queue.execute(new SQLiteJob<Map<String,Object>>() {
				protected Map<String,Object> job(SQLiteConnection connection)
						throws SQLiteException {
					Map<String,Object> ret = new HashMap<String,Object>();
					SQLiteStatement st = connection
							.prepare(query.toString());
					int i = 1;
					for (Entry<String, Object> e : matching.entrySet()) {
						if(e.getValue().getClass().equals(Integer.class)){
							st.bind(i++, (Integer)e.getValue());				
						}
						else if(e.getValue().getClass().equals(Double.class)){
							st.bind(i++, (Double)e.getValue());				
						}
						else if(e.getValue().getClass().equals(String.class)){
							st.bind(i++, (String)e.getValue());				
						}
						else{
							getLog().fatal("aaah");
						}
					}
					try{
						if(st.step()){
							i = -1;
							for (Entry<String, Object> e : matching.entrySet()) {
								i++;
								if(e.getValue().getClass().equals(Integer.class)){
									ret.put(e.getKey(), st.columnInt(i));
								}
								else if(e.getValue().getClass().equals(Double.class)){
									ret.put(e.getKey(), st.columnDouble(i));
								}
								else if(e.getValue().getClass().equals(String.class)){
									ret.put(e.getKey(), st.columnString(i));
								}
								else{
									getLog().fatal("choo");
								}
							}
						}
					} finally {
						st.dispose();
					}
					return ret;
				}
			}).get();
		} catch (InterruptedException e1) {
			getLog().error(e1.getLocalizedMessage());
		} catch (ExecutionException e1) {
			getLog().error(e1.getLocalizedMessage());
		}
		return ret;
	}
}
