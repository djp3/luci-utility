package edu.uci.ics.luci.utility.webserver.input.request;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uci.ics.luci.utility.webserver.Channel;
import edu.uci.ics.luci.utility.webserver.Channel.Protocol;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

/**
 * This class represents a REST request, that can be delivered from an http or p2p source
 * @author djp3
 *
 */
public class Request {
	
	private String source;
	private Channel.Protocol protocol = null;
	/* If the full url that was called was http://www.somedomain.com/alpha/beta
	 * and the "command" that was matched was "/alpha"
	 * then command will be set to "/alpha"
	 * and commandLine will be set to "/alpha/beta"
	 * With the same url the command that is matched could also be "/alpha/beta"
	 * in which case command will be "/alpha/beta"
	 * and commandLine will be "/alpha/beta"
	 */
	private String command;
	private String commandLine;
	private Map<String, List<String>> headers;
	private Map<String, Set<String>> parameters;

	/** Returns the protocol and address of the requester **/
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
	
	public String getCommandLine() {
		return commandLine;
	}

	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
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
		JSONObject ret = new JSONObject();
		JSONObject ret_ret = new JSONObject();
		ret_ret.put("source", source);
		ret_ret.put("command", command);
		ret_ret.put("command_line", commandLine);
		ret_ret.put("protocol", protocol.toString());
		
		JSONObject ret_headers = new JSONObject();
		for(Entry<String, List<String>> e: headers.entrySet()){
			JSONArray values = new JSONArray();
			for(String s: e.getValue()){
				values.add(s);
			}
			ret_headers.put(e.getKey(), values);
		}
		ret_ret.put("headers", ret_headers);
		
		JSONObject ret_parameters = new JSONObject();
		for(Entry<String, Set<String>> e: parameters.entrySet()){
			JSONArray values = new JSONArray();
			for(String s: e.getValue()){
				values.add(s);
			}
			ret_parameters.put(e.getKey(), values);
		}
		ret_ret.put("parameters", ret_parameters);
		
		ret.put("request",ret_ret);
		return ret.toJSONString(JSONStyle.LT_COMPRESS);
	}
}
