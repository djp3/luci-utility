package edu.uci.ics.luci.utility.webserver.disruptor.wrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.webserver.disruptor.events.WebEvent;
import edu.uci.ics.luci.utility.webserver.disruptor.results.listener.WebEventHandlerResultListener_Dispatch;

public class WebEventWrapperQueuer_Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		while(Globals.getGlobals() != null){
			Thread.sleep(100);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Globals.setGlobals(null);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
    public void testDisruptor() 
    {
        // Executor that will be used to construct new threads for consumers
        ExecutorService executor = Executors.newCachedThreadPool();

        // The factory for the event
        WebEventWrapperFactory factory = new WebEventWrapperFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<WebEventWrapper> disruptor = new Disruptor<WebEventWrapper>(factory, bufferSize, executor);

        // Connect the handler
        disruptor.handleEventsWith(new WebEventWrapperHandler(executor));

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<WebEventWrapper> ringBuffer = disruptor.getRingBuffer();

        WebEventWrapperQueuer producer = new WebEventWrapperQueuer(disruptor,ringBuffer);
        
        WebEventWrapper event = new WebEventWrapper();
        for (int i = 0 ; i< 25000;i++)
        {
            producer.onData(event);
        }

        event = new WebEventWrapper(new WebEvent());
        for (int i = 0 ; i< 25000;i++)
        {
            producer.onData(event);
        }
        
        WebEventHandlerResultListener_Dispatch l = new WebEventHandlerResultListener_Dispatch();
        event = new WebEventWrapper(new WebEvent(),l);
        for (int i = 0 ; i< 25000;i++)
        {
            producer.onData(event);
        }
    }

}
