package edu.uci.ics.luci.utility.webserver.handlers;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;
import edu.uci.ics.luci.utility.webserver.output.response.Response;

public class HandlerUnstableWrapper extends HandlerAbstract {
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(HandlerUnstableWrapper.class);
		}
		return log;
	}

	Random random = new Random(System.currentTimeMillis());
	private HandlerAbstract wrapMe;
	private double failRate = 0.0d;
	private int wait = 0;
	
	public HandlerUnstableWrapper(double failRate,int wait, HandlerAbstract wrapMe){
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
	public Response handle(Request icr, Output oc) {
		
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
			return(this.wrapMe.handle(icr, oc));
		}
	}

	@Override
	public HandlerAbstract copy() {
		return(new HandlerUnstableWrapper(this.failRate,this.wait, this.wrapMe.copy()));
	}

}
