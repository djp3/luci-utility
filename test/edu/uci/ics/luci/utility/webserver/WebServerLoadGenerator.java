package edu.uci.ics.luci.utility.webserver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.Globals;
import edu.uci.ics.luci.utility.GlobalsForTesting;

public class WebServerLoadGenerator {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(WebServerLoadGenerator.class);
		}
		return log;
	}
	
	/****** Support main which is really a performance test *****/
	
	static final int NUM_THREADS=20;
	static final int NUM_TESTS=250;
	
	private static class HitTheWebsite implements Runnable{
		
		public boolean failed = false;
		public String failureReason = "";
		
		public String path = "/";
		

		@Override
		public void run() {
			try {
				URIBuilder uriBuilder = null;
				uriBuilder = new URIBuilder()
										.setScheme("https")
										.setHost("localhost")
										.setPort(9020)
										.setPath(path);
				
				for(int i = 0; i< NUM_TESTS; i++){
					try{
						WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
					}
					catch(NoHttpResponseException e){
						//It's an expected fail
					}
				}
				
				
			} catch (MalformedURLException e) {
				getLog().fatal("Bad URL");
				failed = true;
				failureReason = e.getLocalizedMessage();
			} catch(SocketTimeoutException e){
				getLog().info("SocketTimeoutException");
				failed = true;
				failureReason = e.getLocalizedMessage();
			} catch(HttpResponseException e){
				getLog().fatal("HttpResponseException");
				failed = true;
				failureReason = e.getLocalizedMessage();
			} catch (IOException e) {
				e.printStackTrace();
				getLog().fatal("IO Exception "+e.getLocalizedMessage());
				failed = true;
				failureReason = e.getLocalizedMessage();
			}
			catch (URISyntaxException e) {
				getLog().fatal("URISyntaxException");
				failed = true;
				failureReason = e.getLocalizedMessage();
			}	
		}
		
	}


	public static void load(List<HitTheWebsite> agents, List<Thread> threads,boolean stopOnFailure) {
		
		threads.clear();
		for(int i = 0 ; i < NUM_THREADS; i++){
			Thread thread = new Thread(agents.get(i));
			thread.setName("Agent "+i);
			thread.setDaemon(false);
			threads.add(thread);
		}
		for(int i = 0 ; i < NUM_THREADS; i++){
			threads.get(i).start();
		}
		
		for(int i = 0 ; i < NUM_THREADS; i++){
			while(threads.get(i).isAlive()){
				try {
					threads.get(i).join();
					if(stopOnFailure) {
						if(agents.get(i).failed){
							throw new RuntimeException("One of the agents failed: Here's why\n"+agents.get(i).failureReason);
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}


	public static void main(String[] args) {
		/* First set up the globals in this convoluted way */
		GlobalsForTesting.reset("testSupport/Everything.log4j.xml");
		GlobalsForTesting g = new GlobalsForTesting();
		Globals.setGlobals(g);
		
		/* Quick test with a single hit for debugging */
		HitTheWebsite test = new HitTheWebsite();
		test.path = "/version";
		Thread t = new Thread(test);
		t.start();
		while(t.isAlive()) {
			try {
				t.join(100);
			} catch (InterruptedException e) {
			}
		}
		/*
		if(!t.isAlive()) {
			return;
		}*/

		/* Battery */
		boolean failed = false;
		
		List<HitTheWebsite> agents = new ArrayList<HitTheWebsite>();
		List<Thread> threads = new ArrayList<Thread>();
		for(int i = 0 ; i < NUM_THREADS; i++){
			HitTheWebsite agent = new HitTheWebsite();
			agents.add(agent);
		}
		
		/* Simple load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).path = "/version";
		}
		long start = System.currentTimeMillis();
		load(agents, threads,true);
		long end = System.currentTimeMillis();
		
		System.out.println("Simple load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		/* Failing response load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).path = "/fail";
		}
		start = System.currentTimeMillis();
		load(agents, threads,false);
		end = System.currentTimeMillis();
		
		System.out.println("Failing response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		/* Latency load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).path = "/latent";
		}
		start = System.currentTimeMillis();
		load(agents, threads,false);
		end = System.currentTimeMillis();
		
		System.out.println("Latency response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		
		
		/* Unstable load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).path = "/unstable";
		}
		start = System.currentTimeMillis();
		load(agents, threads,false);
		end = System.currentTimeMillis();
		
		System.out.println("Unstable response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		
		
		/* Shutdown */
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("https")
			.setHost("localhost")
			.setPort(9020)
			.setPath("/shutdown");

		try {
			WebUtil.fetchWebPage(uriBuilder, null,null, null, 30 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		
		if(failed){
			System.out.println("This run failed");
		}
		g.setQuitting(true);

	}

}
