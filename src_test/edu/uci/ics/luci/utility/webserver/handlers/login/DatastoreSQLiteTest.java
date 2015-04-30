package edu.uci.ics.luci.utility.webserver.handlers.login;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DatastoreSQLiteTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTableFunctions() {
		Datastore ds = new DatastoreSQLite("eraseme");
		assertTrue(ds != null);
		ds.start();
		
		String tableName = "test";
		try{
			ColumnProperty cp  = new ColumnProperty(ColumnType.Integer);
			Map<String, ColumnProperty> columns = new HashMap<String,ColumnProperty>();
			columns.put("column_1", cp);
		
			Set<String> uniques = new HashSet<String>();
			uniques.add("column_1");
		
			assertTrue(ds.createTable(tableName, uniques, columns));
			assertTrue(ds.tableExists(tableName));
		}
		finally{
			assertTrue(ds.deleteTable(tableName));
		}
		
		ds.stop(true);
		
	}
	
	@Test
	public void testRowFunctions() {
		Datastore ds = new DatastoreSQLite("eraseme");
		assertTrue(ds != null);
		ds.start();
		
		String tableName = "test";
		try{
			Map<String, ColumnProperty> columns = new HashMap<String,ColumnProperty>();
			ColumnProperty cp  = new ColumnProperty(ColumnType.Integer);
			columns.put("column_1", cp);
		
			cp  = new ColumnProperty(ColumnType.Real);
			columns.put("column_2", cp);
			
			cp  = new ColumnProperty(ColumnType.Text);
			columns.put("column_3", cp);
		
			Set<String> uniques = new HashSet<String>();
			uniques.add("column_1");
		
			assertTrue(ds.createTable(tableName, uniques, columns));
			assertTrue(ds.tableExists(tableName));
		
			Map<String, Object> row = new HashMap<String,Object>();
			row.put("column_1", Integer.valueOf(1));
			row.put("column_2", Double.valueOf(2.2));
			row.put("column_3", "foobar");
		
			ds.addRow(tableName,row);
			Map<String, Object> answer = ds.getRow(tableName,row.keySet(),row);
			assertEquals(answer.keySet(),row.keySet());
			for(Entry<String, Object> e:row.entrySet()){
				assertEquals(answer.get(e.getKey()),e.getValue());
			}
			
		}
		finally{
			assertTrue(ds.deleteTable(tableName));
		}
		
		ds.stop(true);
		
	}

}
