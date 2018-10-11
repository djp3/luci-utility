package edu.uci.ics.luci.utility.webserver.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent;
import edu.uci.ics.luci.utility.webserver.event.api.APIEvent_Version;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapper;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperFactory;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperHandler;
import edu.uci.ics.luci.utility.webserver.event.wrapper.EventWrapperQueuer;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.socket.Output_Socket_HTTP;

class Event_DispatchTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		//GlobalsForTesting.reset("testSupport/APIEvent_Test.log4j.xml");
		GlobalsForTesting.reset("testSupport/Everything.log4j.xml");
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}
	
	@BeforeEach
	void setUp() throws Exception {
		while(Globals.getGlobals() != null){
			try{
				Thread.sleep(1000);
			}
			catch(InterruptedException e){
			}
		}
		/* First set up the globals in this convoluted way */
		GlobalsForTesting g = new GlobalsForTesting();
		Globals.setGlobals(g);
	
	}

	@AfterEach
	void tearDown() throws Exception {
		Globals.getGlobals().setQuitting(true);
		Globals.setGlobals(null);
	}
	
	@Test
	void testCommandIdentification() {
		// Create an Event_Dispatch object
		Event_Dispatch ed =	null;
		{
			// Create a Event Queue
			// The factory for the event
			EventWrapperFactory factory = new EventWrapperFactory();
			
			// Specify the size of the ring buffer, must be power of 2.
			int bufferSize = 1024;
			
			// Construct the Disruptor
			Disruptor<EventWrapper> disruptor = new Disruptor<EventWrapper>(factory, bufferSize, Executors.defaultThreadFactory());
			
			// Connect the handler
			disruptor.handleEventsWith(new EventWrapperHandler(Executors.newCachedThreadPool()));
			        
			// Start the Disruptor, starts all threads running
			disruptor.start();
			
			// Get the ring buffer from the Disruptor to be used for publishing.
			RingBuffer<EventWrapper> ringBuffer = disruptor.getRingBuffer();
			
			EventWrapperQueuer localEventPublisher = new EventWrapperQueuer(disruptor,ringBuffer);
						
			HashMap<String, APIEvent> registry = new HashMap<String,APIEvent>() ;
			APIEvent_Version a = new APIEvent_Version("a");
			APIEvent_Version b = new APIEvent_Version("b");
			APIEvent_Version c = new APIEvent_Version("c");
			APIEvent_Version d = new APIEvent_Version("d");
			APIEvent_Version e = new APIEvent_Version("e");
			APIEvent_Version f = new APIEvent_Version("f");
			registry.put(null, a);
			registry.put("/", b);
			registry.put("/foo",c);
			registry.put("/foo/bar",d);
			registry.put("/bar",e);
			registry.put("bar",f);
			
			/* test bad constructions */
			assertThrows(IllegalArgumentException.class,() -> {
					new Event_Dispatch(null, localEventPublisher,new Request(),new Output_Socket_HTTP(null));
			});
			assertThrows(IllegalArgumentException.class,() -> {
					new Event_Dispatch(registry, null ,new Request(),new Output_Socket_HTTP(null));
			});
			assertThrows(IllegalArgumentException.class,() -> {
					new Event_Dispatch(registry, localEventPublisher,null,new Output_Socket_HTTP(null));
			});
			assertThrows(IllegalArgumentException.class,() -> {
					new Event_Dispatch(registry, localEventPublisher,new Request(),null);
			});
			/* Good constructor */
			ed =	new Event_Dispatch(registry, localEventPublisher,new Request(),new Output_Socket_HTTP(null));
		}
		
		assertEquals(6,ed.getAPIRegistry().size());
		
		assertNotNull(ed);
		
		/* First just check the identifyCommand function for correctness */
		Long start = System.currentTimeMillis();
		assertNull(ed.identifyCommand(null));
		assertEquals("/",ed.identifyCommand("/baz"));
		assertEquals("/",ed.identifyCommand("/fooby"));
		assertEquals("/foo",ed.identifyCommand("/foo/baz"));
		assertEquals("/foo/bar",ed.identifyCommand("/foo/bar"));
		assertEquals("/foo/bar",ed.identifyCommand("/foo/bar/baz"));
		assertEquals("/bar",ed.identifyCommand("/bar"));
		assertEquals("/bar",ed.identifyCommand("/bar/foo"));
		assertEquals("bar",ed.identifyCommand("bar/foo"));
		assertEquals("/bar",ed.identifyCommand("/bar//foo/"));
		Long end = System.currentTimeMillis();
		
		/* Now check to make sure the cache is helping */
		Long start2 = System.currentTimeMillis();
		assertNull(ed.identifyCommand(null));
		assertEquals("/",ed.identifyCommand("/baz"));
		assertEquals("/",ed.identifyCommand("/fooby"));
		assertEquals("/foo",ed.identifyCommand("/foo/baz"));
		assertEquals("/foo/bar",ed.identifyCommand("/foo/bar"));
		assertEquals("/foo/bar",ed.identifyCommand("/foo/bar/baz"));
		assertEquals("/bar",ed.identifyCommand("/bar"));
		assertEquals("/bar",ed.identifyCommand("/bar/foo"));
		assertEquals("bar",ed.identifyCommand("bar/foo"));
		assertEquals("/bar",ed.identifyCommand("/bar//foo/"));
		Long end2 = System.currentTimeMillis();
		
		assertTrue((end - start) >= (end2-start2)); //Cache should speed things up
		//System.out.println("Cached helped "+((end-start) - (end2-start2))+" milliseconds");
		
		/* Now check to make sure we are getting the right objects*/
		assertEquals("a",((APIEvent_Version)ed.getEvent(null)).getAPIVersion());
		assertEquals("b",((APIEvent_Version)ed.getEvent("/baz")).getAPIVersion());
		assertEquals("b",((APIEvent_Version)ed.getEvent("/fooby")).getAPIVersion());
		assertEquals("c",((APIEvent_Version)ed.getEvent("/foo")).getAPIVersion());
		assertEquals("c",((APIEvent_Version)ed.getEvent("/foo/")).getAPIVersion());
		assertEquals("c",((APIEvent_Version)ed.getEvent("/foo/baz")).getAPIVersion());
		assertEquals("d",((APIEvent_Version)ed.getEvent("/foo/bar")).getAPIVersion());
		assertEquals("d",((APIEvent_Version)ed.getEvent("/foo/bar/baz")).getAPIVersion());
		assertEquals("e",((APIEvent_Version)ed.getEvent("/bar")).getAPIVersion());
		assertEquals("e",((APIEvent_Version)ed.getEvent("/bar/foo")).getAPIVersion());
		assertEquals("e",((APIEvent_Version)ed.getEvent("/bar//foo/")).getAPIVersion());
		assertEquals("f",((APIEvent_Version)ed.getEvent("bar/foo")).getAPIVersion());
		
		/* Now check bad matches */
		assertEquals("a",((APIEvent_Version)ed.getEvent("fudge")).getAPIVersion());
		
		/* Check that the event dispatcher doesn't crash at least */
		ed.getRequest().setCommandLine("/foo/bar/baz");
		assertNotNull(ed.onEvent());
	}

}
