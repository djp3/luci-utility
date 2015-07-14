package edu.uci.ics.luci.utility.webserver.event.api;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;

public class APIEvent_UnstableWrapper extends APIEvent implements Cloneable { 
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_UnstableWrapper.class);
		}
		return log;
	}

	private static transient Random random = new Random(System.currentTimeMillis());
	
	private Double failRate = null;
	private Integer wait = null;
	private APIEvent wrapMe = null;
	
	public APIEvent_UnstableWrapper(double failRate,int wait, APIEvent wrapMe){
		super();
		if(failRate < 0.0d){
			throw new IllegalArgumentException("failRate is too low "+failRate+" < 0.0");
		}
		if(failRate > 1.0d){
			throw new IllegalArgumentException("failRate is too high "+failRate+" > 1.0");
		}
		this.failRate = failRate;
		
		if(wait < 0){
			throw new IllegalArgumentException("wait is too low "+wait+" < 0");
		}
		this.wait = wait;
		
		this.wrapMe = wrapMe;
	}
	
	
	@Override
	public Object clone(){
		return(super.clone());
	}
	
	
	@Override
	public APIEventResult onEvent() {
		
		/* Wait a random amount of time simulating latency */
		try {
			Thread.sleep(random.nextInt(wait));
		} catch (InterruptedException e) {
		}
		
		/* Possibly fail, simulating failing */
		if(random.nextDouble()<failRate){
			throw new RuntimeException("Handler intentionally failed");
		}
		else{
			return(this.wrapMe.onEvent());
		}
	}
	

}
