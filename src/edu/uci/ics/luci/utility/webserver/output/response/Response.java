package edu.uci.ics.luci.utility.webserver.output.response;


/** This is a super class for outputs that are going out on a network **/
public abstract class Response {
	
	public enum Status{OK, REDIRECT, PROXY, NOT_FOUND};
	public enum DataType{JSON,HTML,CSS,PNG,JAVASCRIPT};
	
	private String body = "";
	private Status status;
	private DataType dataType;
	
	
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
	
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	
	public DataType getDataType(){
		return dataType;
	}
	
	public void setDataType(DataType dataType){
		this.dataType=dataType;
	}

}
