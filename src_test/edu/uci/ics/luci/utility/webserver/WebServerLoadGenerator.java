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

public class WebServerLoadGenerator {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(WebServerLoadGenerator.class);
		}
		return log;
	}
	
	/****** Support main which is really a performance test *****/
	
	private static class GlobalsDummy extends Globals{
		
		private static transient volatile Logger log = null;
		public static Logger getLog(){
			if(log == null){
				log = LogManager.getLogger(GlobalsDummy.class);
			}
			return log;
		}

		@Override
		public String getSystemVersion() {
			return "1.0";
		}
		
	}
	
	static final int NUM_THREADS=20;
	static final int NUM_TESTS=250;
	
	private static class HitTheWebsite implements Runnable{
		
		public boolean failed = false;
		public String failureReason = "";
		
		public boolean version = false;
		public boolean fail = false;
		public boolean latent = false;
		public boolean unstable = false;

		@Override
		public void run() {
			try {
				URIBuilder uriBuilder = null;
				if(version){
					uriBuilder = new URIBuilder()
											.setScheme("http")
											.setHost("localhost")
											.setPort(9020)
											.setPath("/version");
				}
				else if(fail){
					uriBuilder = new URIBuilder()
											.setScheme("http")
											.setHost("localhost")
											.setPort(9020)
											.setPath("/fail");
				}
				else if(latent){
					uriBuilder = new URIBuilder()
											.setScheme("http")
											.setHost("localhost")
											.setPort(9020)
											.setPath("/latent");
				}
				else if(unstable){
					uriBuilder = new URIBuilder()
											.setScheme("http")
											.setHost("localhost")
											.setPort(9020)
											.setPath("/unstable");
				}
				else{
					uriBuilder = new URIBuilder()
											.setScheme("http")
											.setHost("localhost")
											.setPort(9020)
											.setPath("/version");
				}
				
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
				getLog().fatal("SocketTimeoutException");
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


	public static void main(String[] args) {
		
		Globals.setGlobals(new GlobalsDummy());
		Globals.getGlobals().setTesting(false);

		boolean failed = false;
		
		List<HitTheWebsite> agents = new ArrayList<HitTheWebsite>();
		List<Thread> threads = new ArrayList<Thread>();
		for(int i = 0 ; i < NUM_THREADS; i++){
			HitTheWebsite agent = new HitTheWebsite();
			agents.add(agent);
		}
		
		/* Simple load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).version = true;
		}
		long start = System.currentTimeMillis();
		load(agents, threads);
		long end = System.currentTimeMillis();
		
		System.out.println("Simple load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		/* Failing response load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).version = false;
			agents.get(i).fail = true;
		}
		start = System.currentTimeMillis();
		load(agents, threads);
		end = System.currentTimeMillis();
		
		System.out.println("Failing response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		/* Latency load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).version = false;
			agents.get(i).fail = false;
			agents.get(i).latent = true;
		}
		start = System.currentTimeMillis();
		load(agents, threads);
		end = System.currentTimeMillis();
		
		System.out.println("Latency response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		
		
		/* Unstable load */
		for(int i = 0 ; i < NUM_THREADS; i++){
			agents.get(i).version = false;
			agents.get(i).fail = false;
			agents.get(i).latent = false;
			agents.get(i).unstable = true;
		}
		start = System.currentTimeMillis();
		load(agents, threads);
		end = System.currentTimeMillis();
		
		System.out.println("Unstable response load:\n\t\t"+(NUM_THREADS *NUM_TESTS)+" tests took "+((end-start)/1000.0)+" seconds,\t"+((end-start)/(NUM_THREADS*NUM_TESTS*1.0d))+" ms each");
		
		
		
		/* Shutdown */
		URIBuilder uriBuilder = new URIBuilder()
			.setScheme("http")
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

	}


	public static void load(List<HitTheWebsite> agents, List<Thread> threads) {
		
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
					if(agents.get(i).failed){
						throw new RuntimeException("One of the agents failed: Here's why\n"+agents.get(i).failureReason);
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

}
