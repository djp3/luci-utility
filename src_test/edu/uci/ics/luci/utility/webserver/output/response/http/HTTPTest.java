package edu.uci.ics.luci.utility.webserver.output.response.http;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uci.ics.luci.utility.webserver.output.response.Response;
import edu.uci.ics.luci.utility.webserver.output.response.Response.DataType;
import edu.uci.ics.luci.utility.webserver.output.response.Response.Status;

public class HTTPTest {

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
	public void testSetBody() {
		String arbitrary = UUID.randomUUID().toString();
		HTTP ocrh = new HTTP();
		ocrh.setBody(arbitrary);
		assertEquals(arbitrary,ocrh.getBody());
		
		arbitrary = UUID.randomUUID().toString();
		ocrh.setBody(arbitrary);
		assertEquals(arbitrary,ocrh.getBody());
	}

	@Test
	public void testSetStatus() {
		HTTP ocrh = new HTTP();
		Status[] values = Response.Status.values();
		for(int i=0 ; i < values.length;i++){
			ocrh.setStatus(values[i]);
			assertEquals(values[i],ocrh.getStatus());
		}
	}
	
	@Test
	public void testSetDataType() {
		HTTP ocrh = new HTTP();
		DataType[] values = Response.DataType.values();
		for(int i=0 ; i < values.length;i++){
			ocrh.setDataType(values[i]);
			assertEquals(values[i],ocrh.getDataType());
		}
	}
	
	
	@Test
	public void testSetHttpStatus() {
		HTTP ocrh = new HTTP();
		
		ocrh.setStatus(Response.Status.OK);
		assertEquals(HttpStatus.SC_OK,ocrh.getHttpStatus());
	}

}
