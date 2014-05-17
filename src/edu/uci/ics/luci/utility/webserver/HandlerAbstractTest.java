package edu.uci.ics.luci.utility.webserver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class HandlerAbstractTest {

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

	/*
	@Test
	public void testEncodeURIComponent() {
		String orig ="\"A\" B ± \"";
		String x = HandlerAbstract.encodeURIComponent(orig);
		assertEquals("%22A%22%20B%20%C2%B1%20%22",x);
		assertEquals(orig, HandlerAbstract.decodeURIComponent(x));
		
		orig ="! ' ( ) + ~";
		x = HandlerAbstract.encodeURIComponent(orig);
		assertEquals(orig, HandlerAbstract.decodeURIComponent(x));
		
	}
	*/
	

}
