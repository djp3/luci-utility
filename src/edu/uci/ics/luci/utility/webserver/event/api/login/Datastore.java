package edu.uci.ics.luci.utility.webserver.event.api.login;

import java.util.Map;
import java.util.Set;

public interface Datastore {
	
	public boolean isThreadSafe();
	public boolean isStopped();
	public void start();
	public void stop(boolean gracefully);
	
	public boolean createTable(String name,Set<String> uniques,Map<String,ColumnProperty> columns);
	public boolean tableExists(String tableName);
	public boolean deleteTable(String name);
	
	public void addRow(String tablename,Map<String,Object> row);
	public Map<String,Object> getRow(String tablename,Set<String> fields, Map<String,Object> matching);
}
