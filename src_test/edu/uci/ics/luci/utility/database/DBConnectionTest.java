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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsTest;

public class DBConnectionTest {

	//private static final boolean testing = true;
	private static Random r = new Random();

	static Object countLock = new Object();
	static Integer count = 0;

	static LUCIDBConnectionPool odbcp = null;

	private transient volatile Logger log = null;
	public Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(DBConnectionTest.class);
		}
		return log;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		try{
			Globals.setGlobals(new GlobalsTest());
			odbcp = new LUCIDBConnectionPool(Globals.getGlobals().getDatabaseDomain(), "testDatabase", "testuser", "testuserPassword",null,0);
		}
		catch(RuntimeException e){
			fail("Couldn't make a pool\n"+e);
		}
	}

	@AfterClass
	public static void tearDown() throws Exception {
		if(odbcp != null){
			odbcp.shutdown();
		}
		Globals.setGlobals(null);
	}
	
	private static void incrementCount() {
		synchronized(countLock){
			count++;
		}
	}


	private void statementBattery(DBConnection c, String tableName) {
		if (c != null) {
			int count = -1;

			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				ps = c.prepareStatement("DROP TABLE IF EXISTS " + tableName);
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				fail("SQL Exception");
			}
			finally{
				if(ps != null){
					try{
						ps.close();
					} catch (SQLException e) {
					}
					finally{
						ps = null;
					}
				}
			}

			try {
				ps = c.prepareStatement("CREATE TABLE " + tableName + " ("
						+ "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
						+ "PRIMARY KEY (id),"
						+ "name CHAR(40), category CHAR(40))");
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				fail("SQL Exception");
			}
			finally{
				if(ps != null){
					try{
						ps.close();
					} catch (SQLException e) {
					}
					finally{
						ps = null;
					}
				}
			}

			try {
				ps = c.prepareStatement("INSERT INTO " + tableName
						+ " (name, category)" + " VALUES"
						+ "('snake', 'reptile')," + "('frog', 'amphibian'),"
						+ "('tuna', 'fish')," + "('racoon', 'mammal')");
				count = ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				fail("SQL Exception");
			}
			finally{
				if(ps != null){
					try{
						ps.close();
					} catch (SQLException e) {
					}
					finally{
						ps = null;
					}
				}
			}

			assertTrue(count == 4);

			count = 0;
			try {
				ps = c.prepareStatement("SELECT * FROM " + tableName
						+ " WHERE name = 'tuna';");
				rs = ps.executeQuery();
				while (rs.next()) {
					count++;
				}
			} catch (SQLException e) {
				e.printStackTrace();
				fail("SQL Exception");
			}
			finally{
				if(rs != null){
					try{
						rs.close();
					} catch (SQLException e) {
					}
					finally{
						rs = null;
					}
				}
				if(ps != null){
					try{
						ps.close();
					} catch (SQLException e) {
					}
					finally{
						ps = null;
					};
				}
			}
			
			try {
				ps = c.prepareStatement("DROP TABLE IF EXISTS " + tableName);
				ps.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
				fail("SQL Exception");
			}
			finally{
				if(ps != null){
					try{
						ps.close();
					} catch (SQLException e) {
					}
					finally{
						ps = null;
					}
				}
			}

			assertTrue(count == 1);
		}
	}

	public class Tester implements Runnable {

		private LUCIDBConnectionPool pool = null;
		DBConnection c = null;

		Tester(LUCIDBConnectionPool pool) {
			this.pool = pool;
		}

		DBConnection getDBConnection() {
			return c;
		}

		public void run() {
			synchronized (this) {
				long now = System.currentTimeMillis();
				long delay = r.nextInt(100) + 1;/* We don't want to delay 0! */
				while(delay > 0){
					try {
						wait(delay); /* Pause so everything isn't synchronized */
						delay = 0;
					} catch (InterruptedException e1) {
						delay = delay - (System.currentTimeMillis()-now);
					}
				}
			}

			try{
				c = pool.getConnection();

				synchronized(countLock){
					statementBattery(c, "testtable" + count);
					incrementCount();
				}
			}
			finally{
				if(c != null){
					try {
						c.close();
					} catch (SQLException e) {
						getLog().error(e.toString());
						fail("Shouldn't fail");
					}
				}
			}
		}
		
	}

	@Test
	public void testDBConnect() {
		long tests = LUCIDBConnectionPool.getTotalConnections();

		List<Thread> list = new ArrayList<Thread>();

		long outernumber = 10L;
		long number = 10L;
		int count = 0;

		getLog().info( "Creating and testing " + number + " connection operations. "+tests+" connections have been made already");

		for (int j = 0; j < outernumber; j++) {

			for (int i = 0; i < number ; i++) {
				Thread t = new Thread(new Tester(odbcp));
				count++;
				t.setName("Test thread:" + i);
				t.setDaemon(false);
				list.add(t);
				t.start();
			}

			for (Thread t : list) {
				System.out.print(".");
				try {
					t.join();
				} catch (InterruptedException e) {
					getLog().error(e.toString());
					fail("This shouldn't happen");
				}
			}

			System.out.println(""+count);

			list.clear();
		}

		assertEquals(outernumber*number, (LUCIDBConnectionPool.getTotalConnections() - tests));

	}
	
	
	@Test
	public void testWarmUpAndHotStandby() {
		int number = 3;
		LUCIDBConnectionPool testodbcp = new LUCIDBConnectionPool(Globals.getGlobals().getDatabaseDomain(), "testDatabase", "testuser", "testuserPassword",number,2*number);


		assertEquals(Integer.valueOf(2*number),testodbcp.getPoolSize());

		if(testodbcp != null){
			testodbcp.shutdown();
		}
		
		testodbcp = new LUCIDBConnectionPool(Globals.getGlobals().getDatabaseDomain(), "testDatabase", "testuser", "testuserPassword",2*number,number);

		assertEquals(Integer.valueOf(2*number),testodbcp.getPoolSize());

		if(testodbcp != null){
			testodbcp.shutdown();
		}
		
		testodbcp = new LUCIDBConnectionPool(Globals.getGlobals().getDatabaseDomain(), "testDatabase", "testuser", "testuserPassword",number,number);

		assertEquals(Integer.valueOf(number),testodbcp.getPoolSize());

		if(testodbcp != null){
			testodbcp.shutdown();
		}
		
		testodbcp = new LUCIDBConnectionPool(Globals.getGlobals().getDatabaseDomain(), "testDatabase", "testuser", "testuserPassword",0,0);

		assertEquals(Integer.valueOf(0),testodbcp.getPoolSize());

		if(testodbcp != null){
			testodbcp.shutdown();
		}
	}

	
	@Test
	public void testReaping() {

		long tests = LUCIDBConnectionPool.getTotalConnections();

		Tester x = new Tester(odbcp);
		Thread t = new Thread(x);
		t.setName("Test thread");
		t.setDaemon(false);
		t.start();

		try {
			t.join();
		} catch (InterruptedException e1) {
		}

		/*
		 * Lease the wrapper and close the underlying connection so that the
		 * reaper has something to reap
		 */
		try {
			assertTrue(x.getDBConnection().lease());
			x.getDBConnection().getConnection().close();
			x.getDBConnection().setConnection(null);
		} catch (SQLException e) {
			getLog().error(e.toString());
			fail("This should fail");
		}

		assertEquals(1,LUCIDBConnectionPool.getTotalConnections() - tests);

		try {
			odbcp.shutdown();
		} catch (Exception e) {
			fail("This shouldn't throw an exception");
		}
	}

}
