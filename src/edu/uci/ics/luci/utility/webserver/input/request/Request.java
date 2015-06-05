package edu.uci.ics.luci.utility.webserver.input.request;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	
	private String source;
	private Channel.Protocol protocol = null;
	private String command;
	private Map<String, List<String>> headers;
	private Map<String, Set<String>> parameters;

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
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		
		sb.append("{\"request\":\n");
		sb.append("\t{\"source\":\""+source+"\",\n");
		sb.append("\t \"command\":\""+command+"\",\n");
		sb.append("\t \"protocol\":\""+protocol.toString()+"\",\n");
		sb.append("\t \"headers\":\n");
		boolean first = true;
		for(Entry<String, List<String>> e: headers.entrySet()){
			if(first){
				sb.append("\t\t{");
				first = false;
			}
			else{
				sb.append(",\n\t\t ");
			}
			sb.append("\""+e.getKey()+"\":\n");
			boolean anotherfirst = true;
			for(String s: e.getValue()){
				if(anotherfirst){
					sb.append("\t\t\t[");
					anotherfirst = false;
				}
				else{
					sb.append(",\n\t\t\t ");
				}
				sb.append("\""+s+"\"");
			}
			sb.append("\n\t\t\t]");
		}
		sb.append("\n\t\t},\n");
		sb.append("\t \"parameters\":\n");
		first = true;
		for(Entry<String, Set<String>> e: parameters.entrySet()){
			if(first){
				sb.append("\t\t{");
				first = false;
			}
			else{
				sb.append(",\n\t\t ");
			}
			sb.append("\""+e.getKey()+"\":\n");
			boolean anotherfirst = true;
			for(String s: e.getValue()){
				if(anotherfirst){
					sb.append("\t\t\t[");
					anotherfirst = false;
				}
				else{
					sb.append(",\n\t\t\t ");
				}
				sb.append("\""+s+"\"");
			}
			sb.append("\n\t\t\t]");
		}
		sb.append("\n\t\t}");
		sb.append("\n\t}");
		sb.append("\n}");
		return(sb.toString());
	}


}
