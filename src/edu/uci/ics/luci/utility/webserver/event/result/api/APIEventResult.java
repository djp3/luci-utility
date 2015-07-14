package edu.uci.ics.luci.utility.webserver.event.result.api;

import edu.uci.ics.luci.utility.webserver.event.result.EventResult;
import edu.uci.ics.luci.utility.webserver.output.channel.Output;


public class APIEventResult extends EventResult{
	
	public enum Status{OK, REDIRECT, PROXY, NOT_FOUND};
	public enum DataType{JSON,HTML,CSS,PNG,JAVASCRIPT,PROXYSTRING};
	
	Status status = null;
	DataType responseDataType = null;
	String responseBody = null;
	Output output = null;
	
	public APIEventResult(Status status, DataType responseDataType, String responseBody,Output output){
		this.setStatus(status);
		this.setDataType(responseDataType);
		this.setResponseBody(responseBody);
		this.setOutput(output);
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	

	public DataType getDataType() {
		return responseDataType;
	}

	public void setDataType(DataType responseDataType) {
		this.responseDataType = responseDataType;
	}
	
	
	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}
	
}
