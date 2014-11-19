package edu.uci.ics.luci.utility.webserver.input.request;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.luci.utility.webserver.Channel;
import edu.uci.ics.luci.utility.webserver.Channel.Protocol;

//TODO: fix this comment
	/**
	 * This function should be overridden to actually do something in response to a REST call.  It should call oneMoreJobHandled, so
	 * that getJobCounter is meaningful.
	 * @param ip, The ip address from which the request came 
	 * @param httpRequestType, The type of HTTP Request that was received, like: "GET" 
	 * @param headers, the HTML headers in the request 
	 * @param restFunction, the function that was in the URL that caused this code to be invoked, like: "index.html"
	 * @param parameters, a map of key and value that was passed through the REST request
	 * @return a pair where the first element is the content type and the second element are the output bytes to send back
	 */
public class Request {
	
	String source;
	String command;
	private Map<String, Set<String>> parameters;
	private Channel.Protocol protocol = null;
	private Map<String, List<String>> headers;

	/** Returns the protocol and address of the requestor **/
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}
	
	public Map<String, Set<String>> getParameters(){
		return this.parameters;
	}

	public void setParameters(Map<String, Set<String>> parameters) {
		this.parameters = parameters;
	}

	public void setProtocol(Channel.Protocol protocol) {
		this.protocol = protocol;
	}
	
	public Protocol getProtocol() {
		return this.protocol;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
	
	public Map<String, List<String>> getHeaders(){
		return this.headers;
	}


}
