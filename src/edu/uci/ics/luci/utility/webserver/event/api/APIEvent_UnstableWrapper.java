package edu.uci.ics.luci.utility.webserver.event.api;

import java.security.InvalidParameterException;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.luci.utility.webserver.event.Event;
import edu.uci.ics.luci.utility.webserver.event.result.api.APIEventResult;
import edu.uci.ics.luci.utility.webserver.input.request.Request;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;

public class APIEvent_UnstableWrapper extends APIEvent implements Cloneable { 
	
	private static transient volatile Logger log = null;
	public static Logger getLog(){
		if(log == null){
			log = LogManager.getLogger(APIEvent_UnstableWrapper.class);
		}
		return log;
	}

	private static transient Random random = new Random(System.currentTimeMillis());
	
	private double failRate;
	private int wait;
	private APIEvent wrapMe = null;
	
	
	public Double getFailRate() {
		return failRate;
	}


	public void setFailRate(Double failRate) {
		this.failRate = failRate;
	}


	public Integer getWait() {
		return wait;
	}


	public void setWait(Integer wait) {
		this.wait = wait;
	}


	public APIEvent getWrapMe() {
		return wrapMe;
	}


	public void setWrapMe(APIEvent wrapMe) {
		this.wrapMe = wrapMe;
	}
	
	@Override
	public void setRequest(Request request){
		super.setRequest(request);
		wrapMe.setRequest(request);
	}
	
	@Override
	public void setOutput(Output output){
		super.setOutput(output);
		wrapMe.setOutput(output);
	}


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
	public void set(Event _incoming) {
		APIEvent_UnstableWrapper incoming = null;
		if(_incoming instanceof APIEvent_UnstableWrapper){
			incoming = (APIEvent_UnstableWrapper) _incoming;
			super.set(incoming);
			this.setFailRate(incoming.getFailRate());
			this.setWait(incoming.getWait());
			this.setWrapMe(incoming.getWrapMe());
		}
		else{
			getLog().error(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
			throw new InvalidParameterException(ERROR_SET_ENCOUNTERED_TYPE_MISMATCH+", incoming:"+_incoming.getClass().getName()+", this:"+this.getClass().getName());
		}
	}

	
	@Override
	public Object clone(){
		return(super.clone());
	}
	
	
	@Override
	public APIEventResult onEvent() {
		
		/* Wait a random amount of time simulating latency */
		if(wait > 0) {
			try {
				Thread.sleep(random.nextInt(wait));
			} catch (InterruptedException e) {
			}
		}
		
		/* Possibly fail, simulating failing */
		if(random.nextDouble()<failRate){
			throw new RuntimeException("Handler intentionally failed");
		}
		else{
			return(this.wrapMe.onEvent());
		}
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(failRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + wait;
		result = prime * result + ((wrapMe == null) ? 0 : wrapMe.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof APIEvent_UnstableWrapper)) {
			return false;
		}
		APIEvent_UnstableWrapper other = (APIEvent_UnstableWrapper) obj;
		if (Double.doubleToLongBits(failRate) != Double
				.doubleToLongBits(other.failRate)) {
			return false;
		}
		if (wait != other.wait) {
			return false;
		}
		if (wrapMe == null) {
			if (other.wrapMe != null) {
				return false;
			}
		} else if (!wrapMe.equals(other.wrapMe)) {
			return false;
		}
		return true;
	}


	
	

}
